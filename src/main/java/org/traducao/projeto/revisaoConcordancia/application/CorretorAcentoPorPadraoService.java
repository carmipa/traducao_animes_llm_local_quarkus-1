package org.traducao.projeto.revisaoConcordancia.application;

import jakarta.enterprise.context.ApplicationScoped;
import org.traducao.projeto.core.texto.FronteiraTermoAss;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: repõe o acento nas palavras que <b>também são palavras sem ele</b>, nos
 * contextos em que a leitura é inequívoca — {@code "Isso e tudo"} → {@code "Isso é tudo"},
 * {@code "Você esta falando"} → {@code "Você está falando"}.
 *
 * <h2>Por que existe, se já há um revisor gramatical</h2>
 * O LanguageTool <b>tem</b> as regras ({@code CONFUSÃO_E_É}, {@code CONFUSÃO_ESTA_ESTÁ}) e
 * dispara pouquíssimo: medido em 23/08/2026 no acervo, ele resolveu 4 casos de {@code e → é} e 1
 * de {@code esta → está} no Macross II, onde a leitura à mão encontrou dezenas. Sem estes padrões
 * o defeito fica <b>fora do alcance de todo o projeto</b>: o dicionário aprova as duas formas
 * (é o que {@code CorretorAcentoQueColideComVerboService} explica), e o POS tagger não acusa.
 *
 * <h2>Cada padrão nasceu de leitura, e cada exclusão também</h2>
 * Medição de 24/08/2026 sobre 86.147 falas de diálogo do acervo — 1.008 ocorrências, amostra
 * lida uma a uma. Duas regras foram <b>refeitas</b> depois de a leitura mostrar falso positivo:
 *
 * <pre>
 *   "Entrei em contato com você esta noite"  -> `esta noite` e DEMONSTRATIVO, esta CERTO
 *   "Ela desceu para nos salvar"             -> `nos salvar` e pronome obliquo, esta CERTO
 * </pre>
 *
 * Por isso {@code esta} só é tocado quando vem seguido de particípio, gerúndio ou preposição — e
 * {@code nos} só quando a frase TERMINA ali, que é onde ele não pode ser objeto.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li><b>Só acrescenta acento.</b> Nenhum padrão troca, remove ou reordena letra.</li>
 *   <li>A caixa do original é preservada: {@code Isso} continua {@code Isso}.</li>
 *   <li>{@code Isso e aquilo} fica intocado — é coordenação legítima, e são 3 casos no acervo.</li>
 *   <li>Não toca em tag {@code {...}} nem na quebra {@code \\N}: recebe o texto já visível, com
 *       as posições preservadas por quem chama.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Texto nulo ou em branco devolve {@link Optional#empty()}. Nunca lança.
 */
@ApplicationScoped
public class CorretorAcentoPorPadraoService {

    /**
     * Um padrão curado: onde casar, o grupo {@code alvo} vira {@code acentuado}.
     *
     * @param nome       o que este padrão conserta, para telemetria e leitura humana
     * @param padrao     o casamento; o grupo de nome {@code alvo} é o que será trocado
     * @param acentuado  o texto que substitui o grupo {@code alvo}
     */
    private record Regra(String nome, Pattern padrao, String acentuado) {}

    /**
     * Separador entre as palavras do padrão. É {@link FronteiraTermoAss#SEPARADOR_INTERNO} e não
     * {@code \\s+} por uma razão medida: <b>24,6% das falas do acervo têm a quebra {@code \\N}</b>,
     * e ela pode cair exatamente entre o demonstrativo e o verbo — {@code "Isso\\Ne tudo"}. Com
     * {@code \\s+} o padrão não casaria, e o defeito ficaria invisível justamente onde a legenda
     * é mais longa.
     */
    private static final String SEP = FronteiraTermoAss.SEPARADOR_INTERNO;

    /**
     * Fronteiras do ASS. O {@code \\N} ocupa DOIS caracteres e o {@code N} é letra para
     * {@code \\p{L}} — sem a alternativa, o lookbehind conclui que a palavra colada à quebra é
     * sufixo de outra e o termo some. A catraca do projeto reprova quem não usa a forma canônica.
     */
    private static final String INICIO = FronteiraTermoAss.INICIO;
    private static final String FIM = FronteiraTermoAss.FIM;

    /**
     * O {@code e} que é o verbo SER. Depois de demonstrativo ou interrogativo, {@code e} só pode
     * ser {@code é} — exceto quando o que vem a seguir é outro demonstrativo, e aí é coordenação:
     * <i>"Isso e aquilo"</i>. São 3 casos no acervo, e o lookahead negativo os preserva.
     */
    private static final Pattern DEMONSTRATIVO_E = Pattern.compile(
        INICIO + "(?:Isso|Isto|Essa|Esse|Este|Esta|Aquilo|Qual|Quem|isso|isto|aquilo)"
        + SEP + "(?<alvo>e)" + FIM
        + "(?!" + SEP + "(?:aquilo|aquele|aquela|isso|isto|essa|esse|este|esta)" + FIM + ")",
        Pattern.UNICODE_CASE);

    /** {@code não e} é sempre {@code não é}: a negação não coordena com o que vem depois. */
    private static final Pattern NAO_E = Pattern.compile(
        INICIO + "[Nn][ãa]o" + SEP + "(?<alvo>e)" + FIM,
        Pattern.UNICODE_CASE);

    /**
     * {@code esta} verbo, e SÓ quando o que vem depois prova que é verbo: particípio, gerúndio ou
     * preposição. Antes de substantivo ({@code esta noite}, {@code esta vez}) ele é demonstrativo
     * e está certo — foi a leitura da amostra que mostrou isso, e a regra nasceu já com o recorte.
     */
    private static final Pattern ESTA_VERBO = Pattern.compile(
        INICIO + "(?:Voc[êe]|voc[êe]|Ela|ela|Ele|ele|Aqui|aqui|[Nn][ãa]o|que|Que|Isso|isso)"
        + SEP + "(?<alvo>esta)" + SEP
        + "(?=\\p{L}+ndo" + FIM + "|\\p{L}+[ao]dos?" + FIM + "|"
        + "(?:de|em|no|na|com|sem|para|pronto|pronta|vivo|viva|morto|morta|certo|certa|"
        + "errado|errada|bem|mal|aqui|ali|l[áa])" + FIM + ")",
        Pattern.UNICODE_CASE);

    /** {@code não ha} é sempre {@code não há} — o verbo haver, nunca a nota musical. */
    private static final Pattern NAO_HA = Pattern.compile(
        INICIO + "[Nn][ãa]o" + SEP + "(?<alvo>ha)" + FIM,
        Pattern.UNICODE_CASE);

    /**
     * {@code nos} pronome tônico, e SÓ no fim da oração. No meio ele quase sempre é objeto
     * ({@code para nos salvar}), e trocar ali estraga fala correta — medido na amostra.
     */
    private static final Pattern NOS_TONICO = Pattern.compile(
        INICIO + "(?:de|entre|por|para|com|contra|sobre|at[ée])" + SEP + "(?<alvo>nos)"
        + "(?=\\s*[.,!?;:]|\\s*(?:\\\\N)|\\s*$)",
        Pattern.UNICODE_CASE);

    /**
     * {@code so} advérbio. {@code so} não é palavra do português — mas é palavra do INGLÊS, e o
     * acervo tem letra de música e resíduo. Por isso a troca só acontece em fala provadamente
     * portuguesa; ver {@link #FALA_E_PORTUGUESA}.
     */
    private static final Pattern SO_ADVERBIO = Pattern.compile(
        INICIO + "(?<alvo>so)" + FIM,
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /**
     * PROVA de que a fala é portuguesa, e ela existe por causa do {@code so}.
     *
     * <p>A primeira versão listava as palavras que podiam vir DEPOIS do {@code so}
     * ({@code so um}, {@code so tem}...). Lista de palavra seguinte é frágil por construção: o
     * teste caiu em <i>"Você so ira cometer mais crimes"</i>, porque {@code ira} não estava nela —
     * e a fala seguinte traria outra palavra que também não estaria.
     *
     * <p>O fato de verdade é outro: {@code so} <b>não é palavra do português</b>. O único risco é
     * texto em inglês. Então em vez de adivinhar a palavra seguinte, exige-se prova de que a FALA
     * é portuguesa — um diacrítico que o inglês não usa, ou uma palavra funcional inequívoca.
     */
    /**
     * Prova barata de que a fala e portuguesa: um diacritico proprio do idioma, ou uma das
     * palavras-funcao que so o portugues tem nesta forma.
     *
     * <p>Visivel ao PACOTE de proposito. O {@link CorretorCaractereForaDoPortuguesService}
     * precisa da mesma prova para decidir se pode apagar pontuacao espanhola, e reimplementa-la
     * la seria a segunda implementacao do mesmo criterio — que neste projeto sempre divergiu da
     * primeira. Uma so definicao, dois donos de regra que a consultam.
     */
    static final Pattern FALA_E_PORTUGUESA = Pattern.compile(
        "[áàâãéêíóôõúüçÁÀÂÃÉÊÍÓÔÕÚÜÇ]"
        + "|" + INICIO + "(?:que|n[ãa]o|voc[êe]|uma|isso|isto|ent[ãa]o|para|pelo|pela|"
        + "est[áa]|s[ãa]o|mas|meu|minha|seu|sua|nosso|nossa|aqui|agora|quando|porque|"
        + "todos|todas|muito|muita|ainda|depois|antes|sempre|nunca|tudo|nada)" + FIM,
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /**
     * Futuro do indicativo sem acento. Nenhuma destas formas existe sem acento em português, e
     * por isso a troca não precisa de contexto — a exceção é {@code ira}, que tem padrão próprio.
     */
    private static final Pattern FUTURO = Pattern.compile(
        INICIO + "(?<alvo>sera|tera|havera|estara|fara|dira|podera|devera)" + FIM,
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /**
     * {@code ira} sai do padrão do futuro e ganha guarda própria, porque é o ÚNICO da lista com
     * homógrafo: {@code ira} substantivo é cólera, e <i>"a ira dele"</i> está correto sem acento.
     *
     * <p>O acervo tem hoje <b>0</b> ocorrências do substantivo e 3 do verbo. Zero medido não é
     * zero garantido — tradução nova pode trazer a outra forma —, e o escudo custa nada: basta
     * recusar quando vier logo depois de determinante ou preposição.
     */
    private static final Pattern IRA_VERBO = Pattern.compile(
        INICIO
        + "(?<!\\ba\\s)(?<!\\bA\\s)(?<!\\bsua\\s)(?<!\\bSua\\s)(?<!\\bminha\\s)(?<!\\bMinha\\s)"
        + "(?<!\\bnossa\\s)(?<!\\bNossa\\s)(?<!\\bcom\\s)(?<!\\bCom\\s)(?<!\\bde\\s)(?<!\\bDe\\s)"
        + "(?<!\\bda\\s)(?<!\\bDa\\s)(?<!\\bna\\s)(?<!\\bNa\\s)(?<!\\bpela\\s)(?<!\\bmuita\\s)"
        + "(?<alvo>ira)" + FIM,
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final List<Regra> REGRAS = List.of(
        new Regra("e -> é (verbo ser apos demonstrativo)", DEMONSTRATIVO_E, "é"),
        new Regra("nao e -> nao é", NAO_E, "é"),
        new Regra("esta -> está (verbo)", ESTA_VERBO, "está"),
        new Regra("nao ha -> nao há", NAO_HA, "há"),
        new Regra("nos -> nós (tonico)", NOS_TONICO, "nós"),
        new Regra("so -> só (adverbio)", SO_ADVERBIO, "só"),
        new Regra("futuro sem acento", FUTURO, null),
        new Regra("ira -> irá (verbo, nunca o substantivo)", IRA_VERBO, "irá"));

    /**
     * As formas do futuro, para o padrão único devolver a acentuação certa de cada uma. Não é
     * lista de "palavras a acentuar": é a tabela do MESMO padrão, que casa sete formas.
     */
    private static final List<String> FUTURO_SEM = List.of(
        "sera", "tera", "havera", "estara", "fara", "dira", "podera", "devera");
    private static final List<String> FUTURO_COM = List.of(
        "será", "terá", "haverá", "estará", "fará", "dirá", "poderá", "deverá");

    /**
     * PROPÓSITO DE NEGÓCIO: devolve a fala com os acentos inequívocos repostos, ou vazio.
     *
     * <p>INVARIANTES DO DOMÍNIO: aplica de trás para frente, para que a primeira troca não
     * desloque a posição das seguintes; preserva a caixa e tudo o que não casou.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@link Optional#empty()} para nulo, branco ou sem
     * casamento. Nunca lança.
     */
    public Optional<String> corrigir(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String atual = texto;
        for (Regra regra : REGRAS) {
            atual = aplicar(atual, regra);
        }
        return atual.equals(texto) ? Optional.empty() : Optional.of(atual);
    }

    private static String aplicar(String texto, Regra regra) {
        // O `so` só entra em fala provadamente portuguesa — o motivo está em FALA_E_PORTUGUESA.
        if (regra.padrao() == SO_ADVERBIO && !FALA_E_PORTUGUESA.matcher(texto).find()) {
            return texto;
        }
        Matcher m = regra.padrao().matcher(texto);
        StringBuilder sb = new StringBuilder(texto);
        // De TRÁS para a frente: `está` tem mais caracteres que `esta`, e trocar da esquerda para
        // a direita deslocaria as posições que o matcher já calculou.
        java.util.ArrayDeque<int[]> trocas = new java.util.ArrayDeque<>();
        java.util.ArrayDeque<String> valores = new java.util.ArrayDeque<>();
        while (m.find()) {
            String alvo = m.group("alvo");
            String novo = regra.acentuado() != null ? regra.acentuado() : acentuarFuturo(alvo);
            if (novo == null) {
                continue;
            }
            trocas.push(new int[] {m.start("alvo"), m.end("alvo")});
            valores.push(comCaixaDe(alvo, novo));
        }
        while (!trocas.isEmpty()) {
            int[] pos = trocas.pop();
            sb.replace(pos[0], pos[1], valores.pop());
        }
        return sb.toString();
    }

    /** A forma acentuada do futuro casado, ou {@code null} se não for uma das conhecidas. */
    private static String acentuarFuturo(String sem) {
        int i = FUTURO_SEM.indexOf(sem.toLowerCase());
        return i < 0 ? null : FUTURO_COM.get(i);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve a correção com a caixa do original — {@code Sera} vira
     * {@code Será}, e não {@code será}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada vazia devolve a correção como veio.
     */
    private static String comCaixaDe(String original, String novo) {
        if (original.isEmpty() || novo.isEmpty()) {
            return novo;
        }
        if (Character.isUpperCase(original.charAt(0))) {
            return Character.toUpperCase(novo.charAt(0)) + novo.substring(1);
        }
        return novo;
    }
}
