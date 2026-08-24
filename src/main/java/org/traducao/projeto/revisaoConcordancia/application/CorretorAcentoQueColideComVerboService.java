package org.traducao.projeto.revisaoConcordancia.application;

import jakarta.enterprise.context.ApplicationScoped;
import org.traducao.projeto.core.texto.gramatica.AchadoGramatical;
import org.traducao.projeto.core.texto.gramatica.RevisorGramaticalPort;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: repõe o acento do substantivo que o modelo escreveu como forma verbal —
 * <i>"a milicia ordenou um blackout de noticias"</i> → <i>"a milícia ordenou um blackout de
 * notícias"</i>.
 *
 * <h2>O prejuízo que originou (2026-08-23)</h2>
 * Paulo mandou verificar o Macross II: <i>"a concordância está bem ruim naquele anime"</i>. Os
 * episódios 1 e 2 saíram com <b>12,5% das falas com defeito</b>, e a tela 3.3 devolveu, com razão,
 * "NADA A CORRIGIR" — só havia UM caso da classe dela nos dois arquivos. O que estava ruim era
 * acento, e ele atravessava três peneiras:
 *
 * <ul>
 *   <li>a lista nominal de acentos não tem essas palavras (166 pares, quase toda {@code -ao});</li>
 *   <li>o corretor por dicionário só age no que o hunspell REJEITA — e ele ACEITA todas elas;</li>
 *   <li>a 3.3 olha gênero de determinante, não acento.</li>
 * </ul>
 *
 * <p>A razão é uma só, e Paulo a nomeou: <b>"muitas vezes ficou invertido o verbo e substantivo"</b>.
 * {@code noticia}, {@code orbita}, {@code premio}, {@code milicia} e {@code valido} são conjugações
 * legítimas de {@code noticiar}, {@code orbitar}, {@code premiar}, {@code miliciar} e
 * {@code validar}. Onde a inversão acontece, a EXISTÊNCIA da palavra deixa de ser evidência de
 * qualquer coisa, e só a classe gramatical no contexto decide.
 *
 * <h2>A regra que torna isto seguro, e ela é herdada</h2>
 * <b>A correção só pode ACENTUAR, nunca trocar a palavra.</b> Uma proposta do revisor só é aceita
 * se, removidos os acentos, ela for exatamente igual ao trecho original:
 *
 * <pre>
 *   noticia -> notícia   ACEITA   (sem acento: noticia == noticia)
 *   noticia -> noticiar  RECUSA   (sem acento: noticiar != noticia)
 *   Muller  -> Müller    RECUSA   (a caixa alta sai antes; nome e territorio de lore)
 * </pre>
 *
 * É a mesma disciplina do {@code CorretorAcentoPorDicionario}, e é ela que impede uma sugestão
 * bem-intencionada do revisor de virar renomeação.
 *
 * <h2>Por que a posição é preservada em vez de recalculada</h2>
 * O revisor recebe o texto VISÍVEL — sem tag {@code {...}} e sem a quebra {@code \\N} — mas a
 * correção tem de cair no texto REAL. Em vez de mapear posições de um para o outro, o que aqui se
 * faz é apagar tag e quebra <b>trocando cada caractere por um espaço</b>: o texto visível fica com
 * o mesmo comprimento do original, e a posição que o revisor devolve vale nos dois. Substituir por
 * nome de palavra seria mais simples e erraria em <i>"a noticia noticia o caso"</i>, trocando as
 * duas ocorrências quando só a primeira é substantivo.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só acentua. Comprimento em caracteres pode mudar (acento é o mesmo caractere), mas a
 *       palavra sem acentos é sempre a mesma.</li>
 *   <li><b>Não toca em palavra com maiúscula.</b> Nome próprio é lore, e lore não se corrige por
 *       regra — decisão de Paulo para esta tela: <i>"lores não entram aqui"</i>.</li>
 *   <li>Não escreve dentro de {@code {...}}: a tag vira espaço antes de o revisor ver, então
 *       nenhum achado pode nascer lá dentro.</li>
 *   <li>Revisor indisponível devolve {@link Optional#empty()} — igual a "não havia o que
 *       corrigir" no efeito, mas quem chama pergunta {@link #disponivel()} e reporta a diferença.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Texto nulo ou em branco devolve {@link Optional#empty()}. Nunca lança.
 */
@ApplicationScoped
public class CorretorAcentoQueColideComVerboService {

    /**
     * REGRAS QUE NÃO SE APLICA SOZINHO — a família da crase.
     *
     * <h2>Medido no acervo em 23/08/2026, e o veredito é do dado</h2>
     * A crase depende da regência do verbo <b>e</b> de o substantivo seguinte admitir artigo
     * definido. O revisor acerta bastante e erra o suficiente para não escrever sozinho — a MESMA
     * regra {@code CRASE_CONFUSION} produziu as duas coisas:
     *
     * <pre>
     *   "vamos assistir a batalha deles"  -> "à batalha"      CERTO
     *   "Como vão as coisas contigo?"     -> "vão às coisas"  ERRADO: "as coisas" e SUJEITO
     *   "quando nos deu a luz verde"      -> "deu à luz"      ERRADO: nao e parto, e sinal verde
     * </pre>
     *
     * <p>São <b>31 falas</b> em 1.831 (1,7%) que deixam de ser corrigidas, e parte delas estava
     * certa. A perda é declarada: crase errada no acervo custa mais caro que crase faltando,
     * porque crase errada <b>não parece erro</b> na releitura.
     *
     * <p>Isto mora na FATIA e não no adaptador do core, de propósito: o core responde "o que a
     * gramática diz"; quem decide o que ousa gravar sem ninguém lendo é quem grava.
     */
    private static final Set<String> REGRAS_QUE_NAO_SE_APLICA_SOZINHO = Set.of(
        "CRASE_CONFUSION", "DAR_À_LUZ", "PHRASAL_VERB_A");

    private static final Pattern TAG_ASS = Pattern.compile("\\{[^{}]*}");
    private static final Pattern QUEBRA_ASS = Pattern.compile("\\\\[Nn]");

    private final RevisorGramaticalPort revisor;

    public CorretorAcentoQueColideComVerboService(RevisorGramaticalPort revisor) {
        this.revisor = revisor;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve a fala com os acentos de substantivo repostos, ou vazio.
     *
     * <p>INVARIANTES DO DOMÍNIO: só troca palavra por ela mesma acentuada; preserva tag, quebra,
     * espaçamento e tudo o mais byte a byte.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@link Optional#empty()} — texto nulo, texto em branco,
     * revisor indisponível ou nenhuma proposta aceitável.
     */
    public Optional<String> corrigir(String texto) {
        if (texto == null || texto.isBlank() || !revisor.disponivel()) {
            return Optional.empty();
        }
        String visivel = visivelPreservandoPosicao(texto);
        List<AchadoGramatical> achados = revisor.revisar(visivel);
        if (achados.isEmpty()) {
            return Optional.empty();
        }

        // De trás para frente: aplicar da esquerda para a direita deslocaria as posições dos
        // achados seguintes assim que o primeiro mudasse o comprimento do texto.
        List<AchadoGramatical> aceitos = new ArrayList<>();
        for (AchadoGramatical a : achados) {
            if (REGRAS_QUE_NAO_SE_APLICA_SOZINHO.contains(a.regra())) {
                continue;
            }
            String proposta = propostaSoDeAcento(a);
            if (proposta != null) {
                aceitos.add(a);
            }
        }
        if (aceitos.isEmpty()) {
            return Optional.empty();
        }
        aceitos.sort((x, y) -> Integer.compare(y.inicio(), x.inicio()));

        StringBuilder sb = new StringBuilder(texto);
        for (AchadoGramatical a : aceitos) {
            if (a.fim() > sb.length()) {
                continue;
            }
            sb.replace(a.inicio(), a.fim(), propostaSoDeAcento(a));
        }
        String resultado = sb.toString();
        return resultado.equals(texto) ? Optional.empty() : Optional.of(resultado);
    }

    /** Diz se o revisor está de pé — para quem chama separar "limpo" de "não verifiquei". */
    public boolean disponivel() {
        return revisor.disponivel();
    }

    /** O motivo, em português, quando o revisor não está de pé. */
    public String motivoDaIndisponibilidade() {
        return revisor.motivoDaIndisponibilidade();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: filtra as propostas do revisor até sobrar só o que é seguro aplicar
     * sozinho, sem um humano lendo.
     *
     * <p>INVARIANTES DO DOMÍNIO: devolve a sugestão apenas quando ela é o MESMO trecho acentuado,
     * em minúsculas, e realmente ganha acento. Qualquer outra coisa devolve {@code null}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code null}; nunca lança.
     */
    static String propostaSoDeAcento(AchadoGramatical achado) {
        if (achado == null || achado.trecho() == null) {
            return null;
        }
        String original = achado.trecho();
        // MAIUSCULA fica de fora, e a historia desta guarda merece ser lida antes de mexer nela:
        // ela EXISTIU, foi removida em 23/08/2026 por ser codigo morto (a mutacao provou: com ou
        // sem, nenhum caso mudava), e voltou no mesmo dia porque a regra de aceitacao mudou.
        // Sob a regra nova — "so acrescenta acento, caractere a caractere" — `Muller` -> `Müller`
        // passa limpo: e acento puro numa palavra capitalizada. E renomear pessoa.
        //
        // A licao nao e "guarda demais nao faz mal". E que utilidade de guarda depende da regra
        // ao lado dela, e as duas vezes quem decidiu foi a mutacao, nao o meu palpite.
        if (!original.equals(original.toLowerCase())) {
            return null;
        }
        List<String> aceitaveis = new ArrayList<>();
        for (String sugestao : achado.sugestoes()) {
            if (soAcrescentaAcento(original, sugestao)) {
                aceitaveis.add(sugestao);
            }
        }
        // EXATAMENTE UMA proposta aceitavel, e a medicao no acervo mostrou por que: para `avo` o
        // revisor oferece `avó` E `avô`, e escolher a primeira produziu "Meu avó queria consertar"
        // e "os ideais do seu avó Meitzer" — dois homens virando avo mulher. Escolher entre elas
        // exige saber de QUEM se fala, que e lore, e lore nao entra nesta tela por decisao de
        // Paulo. Duas propostas viaveis = ambiguidade real = nao mexe.
        return aceitaveis.size() == 1 ? aceitaveis.get(0) : null;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aceita a proposta apenas quando ela ACRESCENTA acento e não faz mais
     * nada — nem troca letra, nem muda caixa, nem <b>tira</b> acento que já existia.
     *
     * <h2>O prejuízo que originou (2026-08-23), medido no acervo</h2>
     * A primeira versão comparava o trecho INTEIRO: "a forma sem acentos é a mesma?" e "a
     * sugestão tem algum acento?". Passava, e estragava. O revisor acusa trechos de mais de uma
     * palavra, e num deles propôs:
     *
     * <pre>
     *   "nós será"  ->  "nos será"     ACEITO pela regra velha, e ERRADO
     * </pre>
     *
     * O {@code será} carregava o acento que fazia a condição "tem acento" passar, enquanto o
     * {@code nós} — que já estava CERTO no arquivo — perdia o dele. Uma fala correta virava
     * errada. Foi pego na medição do acervo, no Guilty Crown ep02, e o arquivo estava em NFC:
     * não era normalização Unicode, era perda de acento mesmo.
     *
     * <h2>Invariantes do domínio</h2>
     * <ul>
     *   <li>Comparação em NFC e caractere a caractere: mesmo comprimento, mesmas letras.</li>
     *   <li>Onde diferem, o original tem de ser a letra SEM acento e a sugestão a MESMA letra
     *       acentuada. Qualquer outra diferença recusa a proposta inteira.</li>
     *   <li>Como corolário, acento nunca é removido e letra nunca é trocada.</li>
     * </ul>
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code false}; nunca lança.
     */
    static boolean soAcrescentaAcento(String original, String sugestao) {
        if (original == null || sugestao == null || sugestao.equals(original)) {
            return false;
        }
        String o = Normalizer.normalize(original, Normalizer.Form.NFC);
        String s = Normalizer.normalize(sugestao, Normalizer.Form.NFC);
        if (o.length() != s.length()) {
            return false;
        }
        boolean ganhouAlgum = false;
        for (int i = 0; i < o.length(); i++) {
            char co = o.charAt(i);
            char cs = s.charAt(i);
            if (co == cs) {
                continue;
            }
            // UMA condicao, e a mutacao derrubou as outras DUAS que eu tinha escrito ao lado
            // dela — "o original ja tinha acento aqui?" e "a sugestao ganhou acento?". Ambas
            // nunca mudavam resultado, porque as duas SAO CONSEQUENCIA desta:
            //
            //   tirando o acento, a sugestao tem de dar exatamente o caractere original.
            //
            //   - se o original ja fosse acentuado, nenhuma letra simples poderia ser igual a
            //     ele — logo acento nunca e REMOVIDO, de graca;
            //   - se a sugestao nao fosse acentuada, ela seria igual a si mesma sem acento e,
            //     valendo esta condicao, igual ao original — mas os dois diferem aqui.
            //
            // Escrever as tres parecia prudencia. Era ruido: guarda que nao muda resultado
            // nenhum ensina a ignorar as que mudam.
            if (!semAcento(String.valueOf(cs)).equals(String.valueOf(co))) {
                return false;
            }
            ganhouAlgum = true;
        }
        return ganhouAlgum;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: entrega ao revisor o texto que o espectador lê, sem mover nada de
     * lugar.
     *
     * <p>INVARIANTES DO DOMÍNIO: cada caractere de tag e de quebra vira UM espaço, então o texto
     * devolvido tem exatamente o mesmo comprimento do original e as posições valem nos dois.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo devolve string vazia.
     */
    static String visivelPreservandoPosicao(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(texto);
        apagarComEspacos(sb, TAG_ASS);
        apagarComEspacos(sb, QUEBRA_ASS);
        return sb.toString();
    }

    private static void apagarComEspacos(StringBuilder sb, Pattern padrao) {
        Matcher m = padrao.matcher(sb.toString());
        while (m.find()) {
            for (int i = m.start(); i < m.end(); i++) {
                sb.setCharAt(i, ' ');
            }
        }
    }

    private static String semAcento(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }
}
