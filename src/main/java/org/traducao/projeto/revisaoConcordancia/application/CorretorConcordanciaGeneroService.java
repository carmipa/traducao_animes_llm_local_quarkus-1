package org.traducao.projeto.revisaoConcordancia.application;

import org.traducao.projeto.core.texto.FronteiraTermoAss;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: corrige DETERMINISTICAMENTE erros de concordância de GÊNERO inequívocos
 * numa legenda em português, sem depender do inglês nem do LLM. É o coração do menu "Revisão de
 * Concordância": o detector já enxergava esses erros, mas nada os consertava PT-only — aqui a
 * detecção vira correção. Cobre os casos objetivos e seguros: (1) artigo/determinante de um
 * gênero antes de substantivo de gênero conhecido oposto ("o menina" → "a menina", "uma menino"
 * → "um menino"); (2) sujeito "ela"/"ele" com predicativo adjetivo no gênero errado ("ela está
 * cansado" → "ela está cansada").
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só age sobre substantivos/adjetivos de gênero INEQUÍVOCO (listas curadas); nada ambíguo
 *       é tocado.</li>
 *   <li>Preserva a caixa inicial da palavra trocada ("O menina" → "A menina"); tags ASS e o
 *       restante da fala ficam intactos; nunca deixa a linha pior.</li>
 *   <li>Serviço sem estado; só JDK + Spring; não conhece cache, LLM nem inglês.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Texto {@code null}/vazio ou sem erro inequívoco devolve {@link Optional#empty()} (mantém o
 * texto); nunca lança.
 */
@Service
public class CorretorConcordanciaGeneroService {

    /**
     * Fronteira de início do termo, com a quebra {@code \N} do ASS tratada como separador — igual à
     * de {@code NormalizadorAcentosComuns}, onde o efeito está MEDIDO.
     *
     * <p><b>Aqui o efeito é NULO no acervo de hoje, e isso está declarado de propósito.</b> Medido
     * em 2026-08-04 sobre 60.891 falas traduzidas do cache, com os padrões INTEIROS (artigo +
     * substantivo, pronome + verbo de ligação + adjetivo), não com o fragmento do artigo:
     * <ul>
     *   <li>artigo MASC + substantivo FEM: 0 antes, 0 depois</li>
     *   <li>artigo FEM + substantivo MASC: 11 antes, 11 depois</li>
     *   <li>{@code ela} + adjetivo MASC: 0 antes, 0 depois</li>
     *   <li>{@code ele} + adjetivo FEM: 0 antes, 0 depois</li>
     * </ul>
     * O fansub quebra a linha 24,6% das vezes, mas nunca ENTRE o artigo e o núcleo de gênero
     * inequívoco — a quebra cai em outro ponto da fala. Contar só o fragmento {@code artigo + \s+}
     * dá +375 e +268 e sugere ganho que não existe: o par completo é que decide.
     *
     * <p>Mantida por consistência e porque a quebra pode cair ali em qualquer acervo novo; NÃO
     * conta como correção demonstrada. Se alguém precisar reduzir superfície, este é o candidato —
     * não o de acentos.
     */
    private static final String INICIO_DE_TERMO = FronteiraTermoAss.INICIO;

    /**
     * Substantivos de gênero INEQUÍVOCO. Ambíguo fica de fora, sempre.
     *
     * <h2>A segunda leva veio de MEDIÇÃO, não de intuição (19/08/2026)</h2>
     * A lista original tinha 24 palavras, todas de pessoa, e por isso a tela enxergava
     * <b>1 erro em 332.545 falas</b>. O {@code MedicaoConcordanciaPorDicionarioIT} perguntou ao
     * dicionário pt_BR em vez de a uma lista — inferindo gênero por par mínimo — e devolveu 210
     * pares distintos discordantes. Lendo os 210 um a um, o que apareceu foi sempre a mesma
     * forma: determinante + substantivo COMUM, fora da lista curada.
     *
     * <pre>
     *   um isca (8) · o isca (3) · o cortina (3) · um arma (3) · o mochila (2)
     *   nossa orgulho · sua destino · minha afeto · essa luxo · no casa · um baleia · uma gato
     * </pre>
     *
     * <p><b>Cada palavra abaixo foi vista numa fala real do acervo</b> — nenhuma entrou por
     * palpite. E as ambíguas ficaram de fora com o mesmo critério, também medido: {@code guia},
     * {@code caça}, {@code soldado} e {@code figura} servem aos dois gêneros ({@code o figura} é
     * uso corrente), {@code cinza} muda de sentido, {@code pirata}, {@code profeta},
     * {@code papa}, {@code alerta}, {@code mecha}, {@code diagrama}, {@code emblema},
     * {@code enigma}, {@code poeta}, {@code parasita}, {@code genoma}, {@code plasma},
     * {@code data}, {@code chapa} e {@code foto} têm gênero fixo que contraria a terminação.
     * Todas essas apareceram na lista de candidatos e foram RECUSADAS.
     */
    private static final String SUBST_FEM =
        "menina|garota|moça|moca|mulher|deusa|princesa|rainha|senhora|irmã|irma|mãe|mae|filha|"
            + "tia|amiga|dama|donzela|aventureira|sacerdotisa|feiticeira|amazona|ladra|heroína|heroina|"
            // Segunda leva — medida no acervo em 19/08/2026:
            + "isca|cortina|mochila|arma|bandeira|ponta|alavanca|catapulta|arena|flecha|"
            + "bagunça|bagunca|baleia|batalha|carta|desculpa|faixa|mentira|silhueta|trincheira|"
            + "água|agua|"
            // Terceira leva — revelada pela medição do PLURAL, 19/08/2026:
            + "criança|crianca|perna|bota|marca|pessoa|vítima|vitima|testemunha";
    private static final String SUBST_MASC =
        "menino|garoto|moço|moco|homem|deus|príncipe|principe|rei|senhor|irmão|irmao|pai|filho|"
            + "tio|amigo|rapaz|herói|heroi|aventureiro|sacerdote|mago|ladrão|ladrao|príncipe|"
            // Segunda leva — medida no acervo em 19/08/2026:
            + "orgulho|destino|afeto|encontro|respeito|egoísmo|egoismo|luxo|desespero|casco|"
            + "avanço|avanco|pulso|circuito|gato|partido|reparo|selo|testemunho|"
            // Terceira leva — revelada pela medição do PLURAL, 19/08/2026:
            + "cogumelo|reforço|reforco|buraco|compromisso|pensamento|plano|campo";

    // Artigos/determinantes/contrações masculinos e o feminino correspondente (índice a índice).
    // Servem ao MAPA de troca; quem entra no PADRÃO é decidido logo abaixo, e não é a mesma lista.
    // Os POSSESSIVOS entraram em 19/08/2026 pelo mesmo motivo que a segunda leva de substantivos:
    // a medição os encontrou errando no acervo — "nossa orgulho", "minha afeto", "sua destino",
    // "seu catapulta". Sem eles, metade dos erros medidos ficava fora de alcance, porque o
    // determinante errado não era artigo.
    private static final String[] ART_MASC = {
        "o", "um", "este", "esse", "aquele", "do", "no", "ao", "pelo", "num",
        "meu", "seu", "nosso", "meus", "seus", "nossos",
        // Indefinidos — entraram em 19/08/2026 quando a conferencia do acervo JA CORRIGIDO
        // mostrou "algumas reparos" ainda de pe: a familia estava fora da lista, e o erro
        // sobrevivia por falta de determinante, nao por falta de substantivo.
        //
        // E a familia entrou PODADA, porque a medicao mostrou os falsos positivos ANTES de
        // qualquer escrita. Ficaram de fora, com o motivo:
        //   muito/pouco/tanto no SINGULAR .. sao ADVERBIO invariavel antes de substantivo:
        //                                    "Voce e muito crianca" esta CERTO, e virar
        //                                    "muita crianca" seria estragar fala boa.
        //   todo/toda/todos/todas .......... concordam com o ANTECEDENTE, nao com a palavra
        //                                    seguinte: "esses alvos sao todos iscas" esta certo.
        //   certo/certa .................... adjetivo tao comum quanto determinante.
        // No PLURAL o advérbio nao existe ("muitos criancas" so pode ser determinante), entao
        // muitos/muitas e poucos/poucas ficam.
        "algum", "outro"};
    private static final String[] ART_FEM  = {
        "a", "uma", "esta", "essa", "aquela", "da", "na", "à", "pela", "numa",
        "minha", "sua", "nossa", "minhas", "suas", "nossas",
        "alguma", "outra"};

    /**
     * Determinantes femininos que podem ser ACUSADOS antes de substantivo masculino — a lista do
     * padrão, deliberadamente MENOR que a do mapa: <b>o {@code a} sozinho fica de fora</b>.
     *
     * <h2>O prejuízo que originou (medido em 18/08/2026, acervo inteiro)</h2>
     * {@code a} não é só artigo feminino: é a preposição invariante em gênero, e antes de
     * substantivo masculino ela está CERTA. Com o {@code a} no padrão, esta tela reescrevia
     * <pre>
     *   "Graças a Deus, você está vivo."   ->   "Graças o Deus, você está vivo."
     *   "Ore a Deus, não a mim."           ->   "Ore o Deus, não a mim."
     * </pre>
     * Medição sobre 726 legendas de {@code C:\animes} (2.142.264 eventos, 380.697 falas ao
     * alcance): a tela mudaria <b>15 falas</b> — <b>14 delas eram "a Deus" correto</b>, contra
     * <b>1</b> conserto real ({@code "Aquela garoto" -> "Aquele garoto"}, Gundam ZZ). Ou seja,
     * rodar a 3.3 no acervo hoje causaria 14 estragos para 1 acerto.
     *
     * <p>O denominador encolheu no mesmo dia, e o achado não: com os 38 {@code .parcial} fora do
     * alcance, o acervo passou a 2.050.958 eventos e 332.545 falas — as 15 falas e o único acerto
     * continuam os mesmos, porque nenhum deles estava num parcial.
     *
     * <p>A exceção não é descoberta nova: o detector da 3.1
     * ({@code DetectorConcordanciaNominal}) já a documenta e já deixa o {@code a} de fora do
     * lado feminino. O corretor desta fatia nasceu de uma segunda escrita da mesma ideia e
     * perdeu a exceção no caminho — a divergência que a regra da medição prevê para toda
     * segunda implementação.
     *
     * <h2>Por que a assimetria é correta, e não um remendo</h2>
     * O lado masculino continua com o {@code o}: {@code "o"} nunca é preposição em português, e
     * {@code "Vi o menina"} segue sendo corrigido. Tirar os dois "por simetria" mataria a única
     * família que a medição mostrou funcionando.
     *
     * <p><b>Perda declarada:</b> {@code "a menino"} (artigo feminino de verdade antes de
     * substantivo masculino) deixa de ser corrigido. No acervo inteiro, essa construção aparece
     * <b>zero</b> vez — a única ocorrência de {@code a} + substantivo masculino é {@code a Deus}.
     */
    private static final String[] ART_FEM_NO_PADRAO = {
        "uma", "esta", "essa", "aquela", "da", "na", "à", "pela", "numa",
        "minha", "sua", "nossa", "minhas", "suas", "nossas",
        "alguma", "muita", "outra", "toda", "pouca", "certa", "tanta"};

    /**
     * Os mesmos determinantes no PLURAL, índice a índice com os singulares acima.
     *
     * <h2>Por que o plural tem padrão PRÓPRIO, e não um {@code s?} solto</h2>
     * Casar número e gênero na mesma alternância deixaria {@code "o meninas"} entrar — e trocar
     * só o gênero devolveria {@code "a meninas"}, que troca um erro por outro. A discordância de
     * NÚMERO é outro defeito, e esta tela não o corrige: quando número e gênero divergem juntos,
     * ela não toca. Determinante singular só casa com substantivo singular, e plural com plural.
     *
     * <p>MEDIDO em 19/08/2026, e foi o número que decidiu incluir o plural: dos 238 pares
     * distintos discordantes do acervo, <b>27 eram de plural</b>, e a leitura um a um separou
     * <b>17 ocorrências de erro real</b> — {@code aqueles crianças}, {@code essas cogumelos},
     * {@code suas reforços}, {@code os botas}, {@code nas reparos}, {@code minhas planos} — de
     * um ruído com dono conhecido: {@code os caras} (86 ocorrências, correto) e {@code as fotos}.
     */
    private static final String[] ART_MASC_PLUR = {
        "os", "uns", "estes", "esses", "aqueles", "dos", "nos", "aos", "pelos", "nuns",
        "meus", "seus", "nossos",
        "alguns", "muitos", "outros", "poucos", "varios", "vários"};
    private static final String[] ART_FEM_PLUR = {
        "as", "umas", "estas", "essas", "aquelas", "das", "nas", "às", "pelas", "numas",
        "minhas", "suas", "nossas",
        "algumas", "muitas", "outras", "poucas", "varias", "várias"};

    /** O sufixo do plural regular — o irregular ({@code mulheres}, {@code irmãos}) fica de fora. */
    private static final String PLURAL = "s";

    private static final Pattern ART_MASC_COM_SUBST_FEM =
        Pattern.compile(INICIO_DE_TERMO + "(" + String.join("|", ART_MASC) + ")(\\s+)(" + SUBST_FEM + ")(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ART_FEM_COM_SUBST_MASC =
        Pattern.compile(INICIO_DE_TERMO + "(" + String.join("|", ART_FEM_NO_PADRAO) + ")(\\s+)(" + SUBST_MASC + ")(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern ART_MASC_PLUR_COM_SUBST_FEM =
        Pattern.compile(INICIO_DE_TERMO + "(" + String.join("|", ART_MASC_PLUR) + ")(\\s+)(?:"
            + SUBST_FEM + ")" + PLURAL + "(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ART_FEM_PLUR_COM_SUBST_MASC =
        Pattern.compile(INICIO_DE_TERMO + "(" + String.join("|", ART_FEM_PLUR) + ")(\\s+)(?:"
            + SUBST_MASC + ")" + PLURAL + "(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    // Adjetivos/particípios masc ↔ fem (paralelos): base para trocar o predicativo de "ela/ele".
    private static final String[] ADJ_MASC = {
        "cansado", "pronto", "preocupado", "animado", "nervoso", "sozinho", "furioso", "surpreso",
        "certo", "errado", "satisfeito", "irritado", "confuso", "ansioso", "assustado", "machucado",
        "ferido", "ocupado", "perdido", "vivo", "morto", "bêbado", "bebado", "novo", "velho",
        "lindo", "feio", "bravo", "louco", "fraco"};
    private static final String[] ADJ_FEM = {
        "cansada", "pronta", "preocupada", "animada", "nervosa", "sozinha", "furiosa", "surpresa",
        "certa", "errada", "satisfeita", "irritada", "confusa", "ansiosa", "assustada", "machucada",
        "ferida", "ocupada", "perdida", "viva", "morta", "bêbada", "bebada", "nova", "velha",
        "linda", "feia", "brava", "louca", "fraca"};

    private static final String VERBO_LIGACAO = "está|esta|estava|é|era|foi|fica|ficou|parece|continua|se sente";
    private static final Pattern ELA_COM_ADJ_MASC =
        Pattern.compile(INICIO_DE_TERMO + "(ela)(\\s+(?:" + VERBO_LIGACAO + ")\\s+)(" + String.join("|", ADJ_MASC) + ")(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ELE_COM_ADJ_FEM =
        Pattern.compile(INICIO_DE_TERMO + "(ele)(\\s+(?:" + VERBO_LIGACAO + ")\\s+)(" + String.join("|", ADJ_FEM) + ")(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    // ------------------------------------------------------------------------------------
    // CÓPIA CONSCIENTE dos tratamentos PT-only do corretor da 3.1
    // (raspagemRevisao.CorretorDeterministicoConcordanciaService), por ordem de Paulo em
    // 18/08/2026: "todos os tratamentos de concordância que tem dentro de tradução e funcionam
    // podemos fazer cópia aqui, já que essa é uma área que não permitimos acoplamento".
    //
    // O QUE VEIO, e por que só isto: aquele corretor recebe (originalIngles, traducaoAtual) e a
    // maior parte das regras dele EXIGE o inglês — parentesco por father/mother, insulto forte,
    // artigo de mobile suit. Esta tela é PT-only por definição: não tem com o que comparar.
    // Chamando o objeto de produção dele com original nulo sobra exatamente o que segue abaixo.
    //
    // GANHO MEDIDO HOJE: ZERO. Nas 332.545 falas do acervo (18/08/2026), os dois tratamentos não
    // mudariam nenhuma — o instrumento foi calibrado no mesmo experimento e corrige os casos
    // plantados. Isto está escrito porque zero declarado é honestidade; zero escondido é o que
    // faz alguém confiar numa rede que não pegou nada. Eles existem para a tradução de amanhã,
    // não para o acervo de ontem.
    // ------------------------------------------------------------------------------------

    private static final String FIM_DE_TERMO = FronteiraTermoAss.FIM;

    /** Espaço OU a quebra {@code \N} entre as duas palavras — e ele volta como estava. */
    private static final String SEPARADOR = FronteiraTermoAss.SEPARADOR_INTERNO;

    private static final int FLAGS_COPIA = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS;

    /**
     * {@code graças ao deus} → {@code graças a Deus}.
     *
     * <p><b>Divergência DELIBERADA em relação ao original:</b> a versão da 3.1 exige a forma
     * acentuada ({@code graças}), e a aya-expanse produz texto sem acento — foram 197 acentos
     * faltando medidos no Unicorn. A cópia aceita {@code gracas} também, e devolve sempre a forma
     * correta e acentuada. Duplicação consciente permite divergir; o que ela proíbe é divergir
     * sem dizer.
     */
    private static final Pattern GRACAS_AO_DEUS = Pattern.compile(
        INICIO_DE_TERMO + "gra[çc]as ao deus" + FIM_DE_TERMO, FLAGS_COPIA);

    /**
     * Possessivo feminino antes de parentesco masculino: {@code minha pai} → {@code meu pai}.
     *
     * <p>Três grupos, e o do MEIO é o separador: ele volta como estava. Sem isso, corrigir
     * {@code "Minha\Npai"} devolveria {@code "Meu pai"} numa linha só — consertar a gramática
     * quebrando a diagramação é dano, não correção.
     */
    private static final Pattern POSSESSIVO_FEM_COM_PARENTE_MASC = Pattern.compile(
        INICIO_DE_TERMO + "(minha|sua|nossa)(" + SEPARADOR + ")(pai|filho|irmão|irmao)" + FIM_DE_TERMO,
        FLAGS_COPIA);

    /** O espelho: {@code meu mãe} → {@code minha mãe}. */
    private static final Pattern POSSESSIVO_MASC_COM_PARENTE_FEM = Pattern.compile(
        INICIO_DE_TERMO + "(meu|seu|nosso)(" + SEPARADOR + ")(mãe|mae|filha|irmã|irma)" + FIM_DE_TERMO,
        FLAGS_COPIA);

    private static final Map<String, String> FLIP_ART_M2F = mapaFlip(
        juntar(ART_MASC, ART_MASC_PLUR), juntar(ART_FEM, ART_FEM_PLUR));
    private static final Map<String, String> FLIP_ART_F2M = mapaFlip(
        juntar(ART_FEM, ART_FEM_PLUR), juntar(ART_MASC, ART_MASC_PLUR));
    private static final Map<String, String> FLIP_ADJ_M2F = mapaFlip(ADJ_MASC, ADJ_FEM);
    private static final Map<String, String> FLIP_ADJ_F2M = mapaFlip(ADJ_FEM, ADJ_MASC);

    /**
     * PROPÓSITO DE NEGÓCIO: devolve a fala com os erros de gênero inequívocos corrigidos.
     * <p>INVARIANTES DO DOMÍNIO: só troca artigo/adjetivo de gênero conhecido; preserva a caixa.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem erro corrigível devolve {@link Optional#empty()}.
     */
    public Optional<String> corrigir(String pt) {
        if (pt == null || pt.isBlank()) {
            return Optional.empty();
        }
        String r = pt;
        r = flipPrimeiroGrupo(r, ART_MASC_COM_SUBST_FEM, FLIP_ART_M2F);
        r = flipPrimeiroGrupo(r, ART_FEM_COM_SUBST_MASC, FLIP_ART_F2M);
        r = flipDeterminantePlural(r, ART_MASC_PLUR_COM_SUBST_FEM, FLIP_ART_M2F);
        r = flipDeterminantePlural(r, ART_FEM_PLUR_COM_SUBST_MASC, FLIP_ART_F2M);
        r = flipTerceiroGrupo(r, ELA_COM_ADJ_MASC, FLIP_ADJ_M2F);
        r = flipTerceiroGrupo(r, ELE_COM_ADJ_FEM, FLIP_ADJ_F2M);
        r = corrigirExpressaoIdiomatica(r);
        r = ajustarPossessivosDeParentesco(r);
        return r.equals(pt) ? Optional.empty() : Optional.of(r);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: troca a 1ª captura (artigo) pelo gênero oposto, mantendo o espaço e
     * o substantivo (grupos 2 e 3) intactos.
     * <p>INVARIANTES DO DOMÍNIO: só substitui o artigo mapeado; preserva a caixa inicial.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem casamento devolve o texto igual.
     */
    private String flipPrimeiroGrupo(String texto, Pattern pat, Map<String, String> flip) {
        Matcher m = pat.matcher(texto);
        return m.replaceAll(res -> {
            String palavra = res.group(1);
            if (POSSESSIVOS.contains(palavra.toLowerCase()) && precedidoPorArtigo(texto, res.start(1))) {
                // MEIA-CORREÇÃO É PIOR: em "a nossa orgulho" trocar só o possessivo devolve
                // "a nosso orgulho", que acrescenta uma discordância nova entre artigo e
                // possessivo. E o artigo não pode ser trocado junto porque o "a" também é
                // preposição ("entreguei a meu pai" está certo). Então a fala inteira fica
                // como está — a tela prefere não mexer a deixar a linha pior.
                return Matcher.quoteReplacement(res.group());
            }
            String novo = flip.get(palavra.toLowerCase());
            return Matcher.quoteReplacement(preservarCaixa(palavra, novo) + res.group(2) + res.group(3));
        });
    }

    /** Os determinantes possessivos, que só entram no flip quando não há artigo antes deles. */
    private static final java.util.Set<String> POSSESSIVOS = java.util.Set.of(
        "meu", "minha", "seu", "sua", "nosso", "nossa",
        "meus", "minhas", "seus", "suas", "nossos", "nossas");

    /**
     * PROPÓSITO DE NEGÓCIO: diz se a palavra imediatamente anterior à posição é um artigo.
     * <p>INVARIANTES DO DOMÍNIO: olha só o token colado antes, ignorando espaços; não usa regex
     * com retrovisor, que em alternância de larguras diferentes já deu falso-negativo no JDK.
     * <p>COMPORTAMENTO EM CASO DE FALHA: início do texto devolve {@code false}.
     */
    private boolean precedidoPorArtigo(String texto, int inicio) {
        int i = inicio - 1;
        while (i >= 0 && Character.isWhitespace(texto.charAt(i))) {
            i--;
        }
        int fim = i + 1;
        while (i >= 0 && Character.isLetter(texto.charAt(i))) {
            i--;
        }
        if (fim <= i + 1) {
            return false;
        }
        String anterior = texto.substring(i + 1, fim).toLowerCase();
        return ARTIGOS_SIMPLES.contains(anterior);
    }

    private static final java.util.Set<String> ARTIGOS_SIMPLES = java.util.Set.of(
        "o", "a", "os", "as", "um", "uma", "uns", "umas");

    /**
     * PROPÓSITO DE NEGÓCIO: troca a 3ª captura (adjetivo predicativo) pelo gênero oposto,
     * mantendo o pronome e o verbo de ligação (grupos 1 e 2).
     * <p>INVARIANTES DO DOMÍNIO: só substitui o adjetivo mapeado; preserva a caixa inicial.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem casamento devolve o texto igual.
     */
    private String flipTerceiroGrupo(String texto, Pattern pat, Map<String, String> flip) {
        Matcher m = pat.matcher(texto);
        return m.replaceAll(res -> {
            String palavra = res.group(3);
            String novo = flip.get(palavra.toLowerCase());
            return Matcher.quoteReplacement(res.group(1) + res.group(2) + preservarCaixa(palavra, novo));
        });
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve a expressão idiomática à forma correta — em PT-BR se diz
     * "graças a Deus", nunca "graças ao deus", que é decalque do inglês.
     *
     * <p>INVARIANTES DO DOMÍNIO: substituição por string FIXA, e por isso a fronteira é a do
     * termo inteiro, não o separador interno: flexibilizar o miolo faria
     * {@code "graças ao\Ndeus"} virar uma linha só e estourar a caixa na tela. A forma partida no
     * MEIO da expressão segue sem conserto — lacuna declarada, herdada do original.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem casamento devolve o texto igual.
     */
    private String corrigirExpressaoIdiomatica(String texto) {
        return GRACAS_AO_DEUS.matcher(texto).replaceAll(res ->
            Matcher.quoteReplacement(Character.isUpperCase(res.group().charAt(0))
                ? "Graças a Deus" : "graças a Deus"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: acerta o possessivo que acompanha o parentesco — {@code meu pai} e
     * não {@code minha pai}. É concordância de gênero pura, interna ao português, e por isso vive
     * nesta tela sem precisar do inglês.
     *
     * <p>INVARIANTES DO DOMÍNIO: só o possessivo IMEDIATAMENTE anterior a pai/mãe, filho/filha ou
     * irmão/irmã; a caixa inicial é preservada; o separador (espaço ou {@code \N}) volta como
     * estava, para a quebra da legenda não sumir.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: frase sem essa combinação permanece byte a byte igual.
     */
    private String ajustarPossessivosDeParentesco(String texto) {
        String ajustado = POSSESSIVO_FEM_COM_PARENTE_MASC.matcher(texto).replaceAll(res -> {
            String novo = switch (res.group(1).toLowerCase()) {
                case "minha" -> "meu";
                case "sua" -> "seu";
                default -> "nosso";
            };
            return Matcher.quoteReplacement(
                preservarCaixa(res.group(1), novo) + res.group(2) + res.group(3));
        });
        return POSSESSIVO_MASC_COM_PARENTE_FEM.matcher(ajustado).replaceAll(res -> {
            String novo = switch (res.group(1).toLowerCase()) {
                case "meu" -> "minha";
                case "seu" -> "sua";
                default -> "nossa";
            };
            return Matcher.quoteReplacement(
                preservarCaixa(res.group(1), novo) + res.group(2) + res.group(3));
        });
    }

    /**
     * PROPÓSITO DE NEGÓCIO: troca o determinante PLURAL discordante, preservando o substantivo.
     *
     * <p>INVARIANTES DO DOMÍNIO: o padrão do plural não captura o substantivo (ele é grupo
     * não-capturante), porque nada nele muda — só o determinante. E ele nunca casa determinante
     * singular com substantivo plural: número divergente é OUTRO defeito, e corrigir o gênero
     * ali devolveria {@code "a meninas"}, trocando um erro por outro.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem casamento devolve o texto igual.
     */
    private String flipDeterminantePlural(String texto, Pattern pat, Map<String, String> flip) {
        Matcher m = pat.matcher(texto);
        return m.replaceAll(res -> {
            String palavra = res.group(1);
            if (POSSESSIVOS.contains(palavra.toLowerCase()) && precedidoPorArtigo(texto, res.start(1))) {
                return Matcher.quoteReplacement(res.group());
            }
            String novo = flip.get(palavra.toLowerCase());
            String resto = res.group().substring(palavra.length());
            return Matcher.quoteReplacement(preservarCaixa(palavra, novo) + resto);
        });
    }

    /** Concatena singular e plural mantendo a ordem — os mapas de troca são índice a índice. */
    private static String[] juntar(String[] a, String[] b) {
        String[] r = new String[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    private static Map<String, String> mapaFlip(String[] de, String[] para) {
        Map<String, String> m = new java.util.HashMap<>();
        for (int i = 0; i < de.length; i++) {
            m.put(de[i].toLowerCase(), para[i]);
        }
        return Map.copyOf(m);
    }

    private static String preservarCaixa(String original, String substituto) {
        if (!original.isEmpty() && Character.isUpperCase(original.charAt(0))) {
            return Character.toUpperCase(substituto.charAt(0)) + substituto.substring(1);
        }
        return substituto;
    }
}
