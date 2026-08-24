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
        "(\\S*)\\s*" + FronteiraTermoAss.INICIO + "(\\p{Lu}\\p{L}{2,})" + FronteiraTermoAss.FIM);

    private static final Pattern TAG_ASS = Pattern.compile("\\{[^{}]*}");

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
        String novo = dicionario.corrigir(texto, nomesPropriosNoMeioDaFala(texto));
        return novo == null || novo.equals(texto) ? Optional.empty() : Optional.of(novo);
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
    public boolean disponivel() {
        return dicionario.disponivel();
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
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo devolve conjunto vazio; nunca lança.
     */
    static Set<String> nomesPropriosNoMeioDaFala(String texto) {
        Set<String> fora = new LinkedHashSet<>();
        if (texto == null) {
            return fora;
        }
        String visivel = TAG_ASS.matcher(texto).replaceAll(" ")
            .replace("\\N", " ").replace("\\n", " ");
        Matcher m = CAPITALIZADA.matcher(visivel);
        while (m.find()) {
            String antes = m.group(1);
            // Abre frase quando não há nada antes, ou quando o que vem antes termina a anterior.
            boolean abreFrase = antes == null || antes.isBlank()
                || antes.endsWith(".") || antes.endsWith("!") || antes.endsWith("?")
                || antes.endsWith(":") || antes.endsWith("\"") || antes.endsWith("—")
                || antes.endsWith("-") || antes.endsWith("...");
            if (!abreFrase) {
                fora.add(m.group(2));
            }
        }
        return fora;
    }
}
