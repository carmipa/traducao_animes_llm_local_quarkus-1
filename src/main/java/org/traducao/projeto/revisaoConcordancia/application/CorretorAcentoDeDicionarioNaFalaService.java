package org.traducao.projeto.revisaoConcordancia.application;

import jakarta.enterprise.context.ApplicationScoped;
import org.traducao.projeto.core.texto.FronteiraTermoAss;
import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: repõe o acento das palavras que o dicionário <b>rejeita</b> —
 * {@code territorio}, {@code serao}, {@code reforcos}, {@code maos} — na legenda que já está no
 * acervo.
 *
 * <h2>O mecanismo já existia; o que faltava era quem chamasse</h2>
 * O {@link CorretorOrtograficoLegenda#corrigir(String, Set)} faz o trabalho desde sempre, e já
 * carrega a cicatriz que torna isso perigoso: em 18/08/2026 três termos de lore chegaram
 * acentuados à legenda entregue — {@code Apsaras→Apsarás} (23 falas), {@code Bosnia→Bósnia} (7,
 * e é uma NAVE do Zeta, não o país) e {@code Cardeas→Cárdeas} (2). Por isso ele recebe uma lista
 * de <b>intocáveis</b>.
 *
 * <p>No pipeline de tradução essa lista é a lore da obra. <b>Esta tela não tem lore por decisão
 * de Paulo</b> — e a medição de 24/08/2026 mostrou que ela nem precisa: o dano inteiro estava em
 * palavra <b>capitalizada no meio da frase</b>, que é o que nome próprio é. Filtrar por isso
 * protegeu os três casos sem depender de catálogo nenhum.
 *
 * <h2>E o filtro por lore seria PIOR aqui, também medido</h2>
 * Usar o {@code lore.yaml} como lista de intocáveis custa 16 correções legítimas — {@code mao},
 * {@code satelite}, {@code crianca}, {@code lider}, {@code canhao}, {@code heroi} — porque o
 * arquivo de lore tem prosa, e essas palavras aparecem no meio dela. Ganho: zero, já que a
 * capitalização sozinha barra os nomes.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Quem decide o que é falta de acento é o DICIONÁRIO de produção, pela porta. Nenhuma lista
 *       de palavras escrita à mão aqui.</li>
 *   <li>Palavra <b>capitalizada fora do início de frase</b> é intocável: é nome próprio.</li>
 *   <li>Tags {@code {...}} e a quebra {@code \\N} voltam byte a byte — quem garante isso é o
 *       corretor de produção, que usa a fronteira do ASS.</li>
 *   <li>Dicionário indisponível devolve vazio; quem chama pergunta e reporta a diferença.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Texto nulo ou em branco devolve {@link Optional#empty()}. Nunca lança.
 */
@ApplicationScoped
public class CorretorAcentoDeDicionarioNaFalaService {

    /**
     * Palavra que começa com maiúscula, com o que vem antes dela na mesma fala.
     *
     * <p>A fronteira é a canônica do ASS ({@link FronteiraTermoAss#INICIO}) e não um lookbehind
     * simples: o {@code \\N} ocupa dois caracteres e o {@code N} é letra para {@code \\p{L}}, de
     * modo que um nome colado à quebra — {@code "...\\NApsaras avança"} — passaria por sufixo de
     * outra palavra e <b>escaparia da lista de intocáveis</b>. Seria justamente o nome próprio
     * ficando desprotegido, que é o dano que esta classe existe para impedir.
     */
    private static final Pattern CAPITALIZADA = Pattern.compile(
        FronteiraTermoAss.INICIO + "(\\p{Lu}\\p{L}{2,})" + FronteiraTermoAss.FIM);

    /** Qualquer palavra que comece com maiúscula, com a fronteira canônica do ASS. */
    private static final Pattern MAIUSCULA = Pattern.compile(
        FronteiraTermoAss.INICIO + "(\\p{Lu}[\\p{L}]*)" + FronteiraTermoAss.FIM);

    private static final Pattern TAG_ASS = Pattern.compile("\\{[^{}]*}");

    /**
     * Quantas falas o PORTAO DE IDIOMA barrou <b>tendo correcao para fazer</b>.
     *
     * <p>Nao e curiosidade: e o preco da guarda. Uma guarda cujo custo ninguem mede vira dogma, e
     * dogma nao se revisa quando o acervo muda. Se este numero crescer muito, a regra do idioma
     * esta larga demais e volta para a mesa.
     */
    private final java.util.concurrent.atomic.AtomicInteger barradasPorIdioma =
        new java.util.concurrent.atomic.AtomicInteger();

    /**
     * Quantas falas a guarda de MAIUSCULA barrou tendo correcao para fazer — o preco medido dos
     * ~48 nomes de lore salvos.
     */
    private final java.util.concurrent.atomic.AtomicInteger barradasPorMaiuscula =
        new java.util.concurrent.atomic.AtomicInteger();

    private final CorretorOrtograficoLegenda dicionario;

    public CorretorAcentoDeDicionarioNaFalaService(CorretorOrtograficoLegenda dicionario) {
        this.dicionario = dicionario;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve a fala com os acentos que o dicionário aponta, ou vazio.
     *
     * <p>INVARIANTES DO DOMÍNIO: nomes próprios ficam intactos; o resto do texto volta byte a
     * byte.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@link Optional#empty()} — texto nulo, em branco,
     * dicionário fora do ar ou nada a corrigir.
     */
    public Optional<String> corrigir(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        // NÃO se pergunta `disponivel()` ANTES de chamar, e isso é cicatriz de 19/08/2026: o
        // verificador responde "não disponível" enquanto nunca tiver sido consultado, porque o
        // estado dele nasce indefinido. Perguntar primeiro faria a correção nunca acontecer — e o
        // sintoma seria "acervo limpo", que é a pior forma de errar.
        //
        // O corretor de produção já falha fechado: sem dicionário, devolve o texto como veio.
        // Então chama-se, e a disponibilidade só é consultada DEPOIS, para o relatório.
        Set<String> intocaveis = palavrasComMaiuscula(texto);
        String novo = dicionario.corrigir(texto, intocaveis);
        boolean mudaria = novo != null && !novo.equals(texto);

        // PORTAO DE IDIOMA — a fala nao pode ser INGLESA.
        //
        // O acervo tem fala inglesa inteira: "That might not be a bad idea." O dicionario
        // portugues nao sabe o que e `idea`, ve que `ideá` existe, e devolve "a bad ideá".
        // Acentuar palavra de outro idioma nao e corrigir — e estragar por fora do dominio.
        //
        // A REGUA "a fala e portuguesa?" FOI MEDIDA E RECUSADA. Exigir diacritico ou
        // palavra-funcao barrava "Chegamos a borda do territorio.", que e portugues normal: ~19
        // falas legitimas perdidas para salvar 8. A pergunta certa e a inversa, e ela e do
        // dicionario: esta fala e predominantemente INGLESA?
        //
        // A CORRECAO E CALCULADA ANTES DO PORTAO DE PROPOSITO. So assim da para CONTAR o que o
        // portao custa: sem esse numero, "a guarda e barata" seria opiniao.
        if (dicionario.predominantementeInglesa(texto)) {
            if (mudaria) {
                barradasPorIdioma.incrementAndGet();
            }
            return Optional.empty();
        }
        if (!mudaria) {
            // Contador do que se ABSTEVE por caixa alta: separa "nao havia acento a repor" de
            // "havia, e a guarda do nome proprio segurou".
            String semGuarda = dicionario.corrigir(texto, Set.of());
            if (semGuarda != null && !semGuarda.equals(texto)) {
                barradasPorMaiuscula.incrementAndGet();
            }
        }
        return mudaria ? Optional.of(novo) : Optional.empty();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: avisa o dicionário sobre TODAS as falas de um arquivo antes de a
     * primeira ser corrigida, para que a passada não pague uma consulta externa por fala.
     *
     * <h2>Por que a decisão é desta fatia e a mecânica é do core</h2>
     * O lote em si — juntar as formas e perguntar de uma vez — é do
     * {@link CorretorOrtograficoLegenda}, que é dono do dicionário. O que esta fatia sabe, e o
     * core não, é <b>qual é o lote</b>: aqui a unidade é o arquivo de legenda, porque é o que a
     * tela 3.3 processa por vez.
     *
     * <p>Medido em 24/08/2026: sem isto, 3.518 falas de seis episódios custaram 283 segundos ao
     * elo do dicionário — 97% do tempo da tela inteira, para zero correção.
     *
     * <p>INVARIANTES DO DOMÍNIO: aquecer NÃO muda texto nenhum. Chamar ou não chamar produz a
     * MESMA legenda; muda só o relógio. Por isso pode ser pulado sem risco de resultado errado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: dicionário fora do ar devolve {@code false}; a passada
     * continua e cada fala volta a consultar por conta própria.
     */
    public boolean aquecerCom(java.util.Collection<String> falas) {
        return dicionario.aquecerComTextos(falas);
    }

    /**
     * Diz se o dicionário está de pé — para separar "limpo" de "não verifiquei".
     *
     * <p><b>Só vale depois de pelo menos uma chamada a {@link #corrigir(String)}:</b> o
     * verificador nasce com estado indefinido e só se declara disponível quando responde a
     * primeira consulta. Perguntar antes devolve {@code false} sem que nada esteja errado.
     */
    /** Falas que o portão de idioma barrou tendo o que corrigir. */
    public int barradasPorIdioma() {
        return barradasPorIdioma.get();
    }

    /** Falas que a guarda de maiúscula barrou tendo o que corrigir. */
    public int barradasPorMaiuscula() {
        return barradasPorMaiuscula.get();
    }

    /** Zera os dois contadores — uma passada não pode herdar o placar da anterior. */
    public void zerarPlacarDasGuardas() {
        barradasPorIdioma.set(0);
        barradasPorMaiuscula.set(0);
    }

    public boolean disponivel() {
        return dicionario.disponivel();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: TODA palavra que começa com maiúscula é intocável para este corretor.
     *
     * <h2>Por que a régua ficou tão larga, e o que foi medido para chegar nela</h2>
     * A regra anterior protegia só a capitalizada <b>no meio</b> da fala. Em 24/08/2026 a leitura
     * dos 1.156 pares que o acervo produziria mostrou que isso não basta — nome de personagem abre
     * frase o tempo todo:
     *
     * <pre>
     *   Artemis -> Ártemis   ~19 falas   DanMachi     "Ártemis..." sozinha na linha
     *   Astrea  -> Ástrea      8 falas   DanMachi     "Ástrea Record."
     *   Ingues  -> Ingués      8 falas   Macross II   "Senhor Imperador Ingués"
     *   Cardeas -> Cárdeas     6 falas   Unicorn      "Cárdeas Vist."
     *   Cleo    -> Cléo        4 falas   Break Blade  "Cléo, minha filha..."
     *   Orario  -> Orário      1 fala    DanMachi     e a CIDADE da obra
     *   Loquis, Demeter, Virus                        deuses e termo de lore
     * </pre>
     *
     * <p>Contra isso, o que a régua larga CUSTA no acervo inteiro: <b>três</b> falas de
     * {@code Parabens→Parabéns}. Quarenta e oito nomes salvos por três acentos perdidos, e o
     * acento perdido continua legível enquanto o nome trocado vira outro personagem.
     *
     * <h2>Duas hipóteses mais finas foram MEDIDAS e morreram antes de virar código</h2>
     * <ul>
     *   <li><i>"só corrige se a forma minúscula for portuguesa"</i> — o hunspell aceita
     *       {@code cárdeas}, {@code ástrea} e {@code ingués} em minúscula. Barraria 2 nomes de 5.</li>
     *   <li><i>"protege quem está no meio da frase"</i> — era a regra antiga, e o acervo mostrou
     *       nome abrindo frase em cinco obras.</li>
     * </ul>
     *
     * <p>Também some daqui um BUG: a versão anterior lia o token anterior com {@code (\S*)\s*}
     * <b>dentro da mesma regex</b>, e o casamento anterior já tinha consumido esse token. Em
     * {@code "Você viu a Lady Artemis"}, {@code Lady} casava primeiro, e {@code Artemis} vinha com
     * prefixo VAZIO — lido como início de frase e deixado desprotegido, mesmo estando no meio.
     *
     * <p>INVARIANTES DO DOMÍNIO: só olha o texto recebido; tags {@code {...}} e a quebra
     * {@code \N} não entram.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo devolve conjunto vazio; nunca lança.
     */
    /**
     * PROPÓSITO DE NEGÓCIO: diz se a palavra que começa em {@code posicao} <b>abre uma frase</b>.
     *
     * <h2>O bug que este método corrige</h2>
     * A versão anterior capturava o token anterior <i>dentro da própria regex</i>, com
     * {@code (\S*)\s*}. Só que o casamento anterior já tinha consumido esse token: em
     * {@code "Você viu a Lady Artemis"}, {@code Lady} casava primeiro e, na busca seguinte, o
     * prefixo de {@code Artemis} vinha VAZIO. A conclusão era "abre frase", e o nome ficava
     * desprotegido justamente onde estava mais claramente no meio da fala.
     *
     * <p>Olhar para TRÁS por posição não tem esse problema: o texto continua lá, tenha o matcher
     * consumido o que tiver.
     *
     * <p>INVARIANTES DO DOMÍNIO: função pura da string e da posição.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: posição fora da string conta como início; nunca lança.
     */
    private static boolean abreFrase(String visivel, int posicao) {
        int i = Math.min(posicao, visivel.length()) - 1;
        while (i >= 0 && Character.isWhitespace(visivel.charAt(i))) {
            i--;
        }
        if (i < 0) {
            return true;
        }
        char anterior = visivel.charAt(i);
        return anterior == '.' || anterior == '!' || anterior == '?' || anterior == ':'
            || anterior == '"' || anterior == '\u2014' || anterior == '-';
    }

    static Set<String> palavrasComMaiuscula(String texto) {
        Set<String> fora = new LinkedHashSet<>();
        if (texto == null) {
            return fora;
        }
        String visivel = TAG_ASS.matcher(texto).replaceAll(" ")
            .replace("\\N", " ").replace("\\n", " ");
        Matcher m = MAIUSCULA.matcher(visivel);
        while (m.find()) {
            fora.add(m.group(1));
        }
        return fora;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: junta as palavras que esta tela não pode tocar — as capitalizadas que
     * NÃO abrem frase, porque essas são nome próprio.
     *
     * <h2>Por que "não abrir frase" é o recorte</h2>
     * Palavra em início de frase é capitalizada por regra de escrita, e precisa da correção como
     * qualquer outra: {@code "Necessario que venha"} tem de virar {@code "Necessário"}. Já
     * {@code "o mobile armor Apsaras"} no meio da fala só é maiúsculo porque é nome — e foi
     * exatamente ali que os três danos de 18/08/2026 aconteceram.
     *
     * <p>INVARIANTES DO DOMÍNIO: só olha o texto recebido; não consulta lore nem dicionário.
     *
     * <p>PÚBLICO desde 24/08/2026 porque a medição precisa da MESMA resposta. A alternativa era
     * reimplementar "isto é nome próprio?" no harness, e neste projeto a segunda implementação de
     * um critério sempre divergiu da primeira — sempre depois de já ter estragado alguma coisa.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo devolve conjunto vazio; nunca lança.
     */
    public static Set<String> nomesPropriosNoMeioDaFala(String texto) {
        Set<String> fora = new LinkedHashSet<>();
        if (texto == null) {
            return fora;
        }
        String visivel = TAG_ASS.matcher(texto).replaceAll(" ")
            .replace("\\N", " ").replace("\\n", " ");
        Matcher m = CAPITALIZADA.matcher(visivel);
        while (m.find()) {
            if (!abreFrase(visivel, m.start(1))) {
                fora.add(m.group(1));
            }
        }
        return fora;
    }
}
