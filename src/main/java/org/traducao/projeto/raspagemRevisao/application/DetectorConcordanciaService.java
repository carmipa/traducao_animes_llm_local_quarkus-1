package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.raspagemRevisao.application.concordancia.DetectorAgressividadeIntroduzida;
import org.traducao.projeto.raspagemRevisao.application.concordancia.DetectorConcordanciaNominal;
import org.traducao.projeto.raspagemRevisao.application.concordancia.DetectorParentescoInvertido;
import org.traducao.projeto.raspagemRevisao.application.concordancia.LexicoGenero;
import org.traducao.projeto.raspagemRevisao.application.concordancia.RegraDeRevisao;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: detecta erros objetivos de gênero e concordância que
 * tornam uma legenda em português incoerente com a fala original.
 *
 * <p>INVARIANTES DO DOMÍNIO: somente evidências presentes na própria entrada
 * podem gerar suspeita; primeira e segunda pessoas sem identificação do falante
 * não autorizam inferência de gênero; tags ASS não interferem na análise.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: texto traduzido ausente é tratado como
 * limpo por este detector e permanece sob responsabilidade dos validadores de
 * tradução pendente.
 */
@Service
public class DetectorConcordanciaService {

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS;

    private static final String SUBST_FEM =
        "menina|garota|moça|moca|mulher|deusa|princesa|heroina|heroína|rainha|senhora|"
            + "irmã|irma|mãe|mae|filha|avó|tia|amiga|dama|donzela|aventureira|sacerdotisa|"
            + "feiticeira|amazona|ladra|ladrona|deusa|moça|moca";

    private static final String SUBST_MASC =
        "menino|garoto|moço|moco|homem|deus|príncipe|principe|irmão|irmao|pai|filho|avô|"
            + "tio|amigo|rei|herói|heroi|aventureiro|novato|campeão|campeao|rapaz|"
            + "sacerdote|mago|ladrao|ladrão|deus|garoto";

    private static final String ADJ_MASC =
        "novo|velho|pequeno|meu|seu|nosso|pronto|cansado|sozinho|animado|nervoso|"
            + "preocupado|furioso|surpreso|certo|errado|bom|mau|satisfeito|"
            + "irritado|confuso|ansioso|fraco|lindo|feio|bravo|loco|louco|"
            + "assustado|machucado|ferido|ocupado|perdido|vivo|morto|bêbado|bebado|doido";

    private static final String ADJ_FEM =
        "nova|velha|pequena|minha|sua|nossa|pronta|cansada|sozinha|animada|nervosa|"
            + "preocupada|furiosa|surpresa|certa|errada|boa|má|ma|satisfeita|"
            + "irritada|confusa|ansiosa|fraca|linda|feia|brava|loca|louca|"
            + "assustada|machucada|ferida|ocupada|perdida|viva|morta|bêbada|bebada|doida";

    private static final String ADJ_MASC_POSPOSTO =
        "novo|velho|pequeno|pronto|cansado|sozinho|animado|nervoso|preocupado|furioso|"
            + "surpreso|certo|errado|bom|mau|satisfeito|irritado|confuso|ansioso|fraco|"
            + "lindo|feio|bravo|loco|louco|assustado|machucado|ferido|ocupado|perdido|"
            + "vivo|morto|bêbado|bebado|doido";

    private static final String ADJ_FEM_POSPOSTO =
        "nova|velha|pequena|pronta|cansada|sozinha|animada|nervosa|preocupada|furiosa|"
            + "surpresa|certa|errada|boa|má|ma|satisfeita|irritada|confusa|ansiosa|fraca|"
            + "linda|feia|brava|loca|louca|assustada|machucada|ferida|ocupada|perdida|"
            + "viva|morta|bêbada|bebada|doida";

    private static final String PARTIC_MASC =
        "cansado|pronto|preocupado|animado|nervoso|sozinho|furioso|surpreso|certo|errado|"
            + "satisfeito|irritado|confuso|ansioso|loco|louco|assustado|machucado|ferido|"
            + "ocupado|perdido|vivo|morto|bêbado|bebado|doido";

    private static final String PARTIC_FEM =
        "cansada|pronta|preocupada|animada|nervosa|sozinha|furiosa|surpresa|certa|errada|"
            + "satisfeita|irritada|confusa|ansiosa|loca|louca|assustada|machucada|ferida|"
            + "ocupada|perdida|viva|morta|bêbada|bebada|doida";

    private static final String TRATAMENTO_MASC = "senhor|moço|moco|garoto|rapaz|cara|homem|menino";
    private static final String TRATAMENTO_FEM = "senhora|moça|moca|garota|menina|dama|donzela";

    private static final String VERBO_AUX =
        "está|esta|estava|é|era|foi|será|sera|ficou|parece|continua|ficará|ficara|estará|estara|"
            + "estão|estao|foram|eram|serão|serao|ficaram|parecem|continuam";

    private static final String VERBO_IMPERATIVO =
        "diga|fale|fala|pergunte|pergunte|avise|mande|manda|chame|chama|espere|espera|"
            + "olhe|olha|escute|escuta|veja|ve|ouça|ouca|deixe|deixa";
    /**
     * As famílias já extraídas para o subpacote {@code concordancia}, atrás do contrato
     * {@link RegraDeRevisao}. Duas listas, e a separação NÃO é estética: as internas ao
     * português valem sozinhas, as outras precisam do inglês para ter base. Rodar as segundas
     * com original nulo produziria motivo inventado.
     *
     * <p>Campo estático em vez de injeção CDI: os testes constroem
     * {@code new DetectorConcordanciaService()} sem argumentos, e mudar o construtor por causa
     * de organização interna seria custo sem ganho. As regras são sem estado, então uma
     * instância serve a todas as falas.
     *
     * <p>Regra nova entra AQUI, não numa chamada solta em {@code analisar} — foi somando
     * chamada a chamada que o método de pronomes chegou a 124 linhas.
     */
    private static final List<RegraDeRevisao> REGRAS_INTERNAS_AO_PORTUGUES = List.of(
        new DetectorConcordanciaNominal());

    private static final List<RegraDeRevisao> REGRAS_QUE_COMPARAM_COM_O_ORIGINAL = List.of(
        new DetectorAgressividadeIntroduzida(),
        new DetectorParentescoInvertido());

    private static final Pattern GRACAS_AO_DEUS = Pattern.compile("\\bgraças ao deus\\b", FLAGS);

    private static final Pattern PARTIC_MASC_APOS_VERBO =
        Pattern.compile("\\b(" + VERBO_AUX + "|se sente|me sinto|sinto-me|sinto me)\\s+(" + PARTIC_MASC + ")\\b", FLAGS);

    private static final Pattern PARTIC_FEM_APOS_VERBO =
        Pattern.compile("\\b(" + VERBO_AUX + "|se sente|me sinto|sinto-me|sinto me)\\s+(" + PARTIC_FEM + ")\\b", FLAGS);

    private static final Pattern ELA_COM_PREDICADO_MASC =
        Pattern.compile("\\bela\\s+(" + VERBO_AUX + ")\\s+(" + PARTIC_MASC + ")\\b", FLAGS);

    private static final Pattern ELE_COM_PREDICADO_FEM =
        Pattern.compile("\\bele\\s+(" + VERBO_AUX + ")\\s+(" + PARTIC_FEM + ")\\b", FLAGS);

    private static final Pattern ELAS_COM_PREDICADO_MASC =
        Pattern.compile("\\belas\\s+(" + VERBO_AUX + ")\\s+(" + PARTIC_MASC + ")\\b", FLAGS);

    private static final Pattern ELES_COM_PREDICADO_FEM =
        Pattern.compile("\\beles\\s+(" + VERBO_AUX + ")\\s+(" + PARTIC_FEM + ")\\b", FLAGS);

    private static final String PREPOSICOES_OBJETO = "para|com|de|a|ao|à|pela|pelo";

    private static final String VERBOS_TRANSITIVOS_DIRETOS =
        "vi|vejo|viu|vou ver|viemos ver|viram|amo|amei|odia|odeio|encontrei|encontrou|"
            + "conheci|conhece|ajudei|ajudou|protegi|protegeu";

    private static final Pattern OBJETO_MASC_COM_HER_EN =
        Pattern.compile("\\b(" + PREPOSICOES_OBJETO + ")\\s+(ele|nele|dele)\\b", FLAGS);

    private static final Pattern OBJETO_FEM_COM_HIM_EN =
        Pattern.compile("\\b(" + PREPOSICOES_OBJETO + ")\\s+(ela|nela|dela)\\b", FLAGS);

    private static final Pattern IMPERATIVO_PARA_ELE_COM_HER =
        Pattern.compile("\\b(" + VERBO_IMPERATIVO + ")\\s+(a|para)\\s+ele\\b", FLAGS);

    private static final Pattern IMPERATIVO_PARA_ELA_COM_HIM =
        Pattern.compile("\\b(" + VERBO_IMPERATIVO + ")\\s+(a|para)\\s+ela\\b", FLAGS);

    private static final Pattern VI_ELE_COM_HER =
        Pattern.compile("\\b(" + VERBOS_TRANSITIVOS_DIRETOS + ")\\s+(ele|o|lo)\\b", FLAGS);

    private static final Pattern VI_ELA_COM_HIM =
        Pattern.compile("\\b(" + VERBOS_TRANSITIVOS_DIRETOS + ")\\s+(ela|a|la)\\b", FLAGS);

    private static final String VERBOS_SUJEITO =
        "disse|diz|dizia|falou|fala|falava|gritou|grita|gritava|sussurrou|sussurra|pensou|pensa|pensava|"
            + "riu|ri|chorou|chora|sorriu|sorri|perguntou|pergunta|perguntava|respondeu|responde|respondia|"
            + "replicou|replica|murmurou|murmura|exclamou|exclama|continuou|continua|começou|comecou|começa|comeca|"
            // "para" sozinho fica de fora: é, de longe, mais comum como preposição/marcador
            // de oração final ("ele para esperar" = "for him to wait") do que como o verbo
            // "parar" — incluí-lo causava falso positivo em toda frase com essa construção.
            + "parou|para de|foi|vai|ia|está|esta|estava|é|era|será|sera|ficou|fica|parece|parecia|sabe|sabia|"
            + "quer|queria|pode|podia|mencionou|menciona|afirmou|afirma|contou|conta|explicou|explica|"
            + "prometeu|promete|chamou|chama|viu|vê|ve|ouviu|ouve|escutou|escuta|achou|acha|sentiu|sente|"
            + "olhou|olha|concordou|concorda|trabalhou|trabalha|morou|mora|viveu|vive|fez|faz|faria";

    private static final Pattern SUJEITO_ELE_COM_SHE =
        Pattern.compile("\\bele\\s+(" + VERBOS_SUJEITO + ")\\b", FLAGS);

    private static final Pattern SUJEITO_ELA_COM_HE =
        Pattern.compile("\\bela\\s+(" + VERBOS_SUJEITO + ")\\b", FLAGS);

    // SUJEITO INICIAL da fala, ignorando aspas, travessão e reticências de abertura.
    // A posição é a evidência: quem abre a frase é o sujeito, e sujeito trocado de gênero é
    // divergência objetiva — não depende de o verbo estar numa lista curada.
    private static final Pattern ABERTURA_ELE = Pattern.compile("^[\\p{Punct}\\s]*ele\\b", FLAGS);
    private static final Pattern ABERTURA_ELA = Pattern.compile("^[\\p{Punct}\\s]*ela\\b", FLAGS);
    private static final Pattern ABERTURA_SHE = Pattern.compile("^[\\p{Punct}\\s]*she\\b", FLAGS);
    private static final Pattern ABERTURA_HE = Pattern.compile("^[\\p{Punct}\\s]*he\\b", FLAGS);

    // "ele"/"ela" como objeto direto/oblíquo (vi ele, com ela, para ele...) é uso
    // pronominal correto em PT-BR mesmo quando o original só menciona o outro
    // gênero (ex.: "She told him" -> "Ela disse a ele"). Por isso esses usos são
    // removidos do texto antes de checar o isolado (ver removerObjetoPronominal).
    // Nota: um lookbehind negativo equivalente (?<!prep|verbo\s+) chega a dar
    // falso-negativo no JDK quando a alternância mistura frases com espaço
    // (ex.: "viemos ver") com palavras simples — por isso o "strip primeiro".
    private static final Pattern OBJETO_PRONOMINAL_ELE_ELA = Pattern.compile(
        "\\b(?:" + PREPOSICOES_OBJETO + "|" + VERBOS_TRANSITIVOS_DIRETOS + ")\\s+(?:ele|ela)\\b", FLAGS);
    private static final Pattern ELE_ISOLADO = Pattern.compile("\\bele\\b", FLAGS);
    private static final Pattern ELA_ISOLADA = Pattern.compile("\\bela\\b", FLAGS);

    /** {@code contra eles}, {@code para elas}: pronome regido por preposicao e OBJETO. */
    private static final Pattern PLURAL_APOS_PREPOSICAO = Pattern.compile(
        "\\b(?:de|do|da|dos|das|com|contra|para|por|em|no|na|nos|nas|sobre|entre|sem|ao|aos|à|às|a)"
            + "\\s+(?:eles|elas)\\b", FLAGS);

    /**
     * PROPÓSITO DE NEGÓCIO: apaga o {@code eles/elas} que é objeto de preposição, para a
     * checagem de sujeito-predicado não confundi-lo com o sujeito da oração.
     * <p>INVARIANTES DO DOMÍNIO: cópia para inspeção; o texto persistido não muda.
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança.
     */
    private static String removerPluralAposPreposicao(String texto) {
        return PLURAL_APOS_PREPOSICAO.matcher(texto).replaceAll(" ");
    }

    private static String removerObjetoPronominal(String texto) {
        return OBJETO_PRONOMINAL_ELE_ELA.matcher(texto).replaceAll(" ");
    }

    /**
     * Predicado ligado a sujeito de 1ª ou 2ª pessoa — {@code "Você está certo"},
     * {@code "Eu estava preocupado"}. O gênero desses concorda com quem FALA ou com quem OUVE,
     * e o inglês não marca nenhum dos dois: {@code "You're right"} não diz se o interlocutor é
     * homem ou mulher. Um {@code she} na mesma fala refere-se a terceira pessoa, não a esse
     * predicado.
     *
     * <p>MEDIDO em 12/08/2026, Unicorn E12 linha 200 — 1 dos 3 falsos positivos da varredura:
     * <pre>
     *   EN  "You're right, she is enslaved."
     *   PT  "Você está certo, ela está escravizada."     &lt;- CORRETO
     * </pre>
     * O {@code escravizada} está no feminino, como deve; o {@code certo} concorda com "você".
     * As duas regras de predicado viam só o masculino e o {@code she}.
     */
    private static final Pattern PREDICADO_DE_1A_2A_PESSOA = Pattern.compile(
        "\\b(?:eu|voc[êe]|voc[êe]s|tu|n[óo]s|a\\s+senhora|o\\s+senhor)\\s+(?:" + VERBO_AUX + ")\\s+"
            + "(?:" + PARTIC_MASC + "|" + PARTIC_FEM + ")\\b", FLAGS);

    /**
     * Tira da análise os predicados de 1ª/2ª pessoa, no MESMO molde de
     * {@link #removerObjetoPronominal}: "strip primeiro, depois casa" — lookbehind de largura
     * variável dá falso-negativo no JDK quando a alternância mistura frase e palavra simples.
     */
    private static String removerPredicadoDePrimeiraSegundaPessoa(String texto) {
        return PREDICADO_DE_1A_2A_PESSOA.matcher(texto).replaceAll(" ");
    }

    /**
     * Existe por causa de UMA palavra: {@code cara}. Ela e vocativo masculino ("e ai, cara!"),
     * substantivo FEMININO ("na cara dela" = rosto) e adjetivo ("essa roupa e cara"). Medido no
     * acervo em 22/08/2026, a UNICA acusacao desta familia era rosto:
     * <pre>
     * EN: Don't you ever say that to her face, got it?
     * PT: Voce nunca fala isso na cara dela, entendeu?      &lt;- "cara" = face
     * </pre>
     * O sinal que separa os usos e o DETERMINANTE: vocativo nao leva artigo, possessivo nem
     * verbo de ligacao antes ("cara," / ", cara"); rosto e adjetivo levam. Um adjetivo pode
     * ficar no meio ("a propria cara"), entao o padrao aceita UMA palavra intermediaria — e
     * so uma, e sem virgula, porque a virgula e justamente o que marca o vocativo ("falou,
     * cara!"). Estas ocorrencias sao
     * APAGADAS do texto antes da checagem de tratamento — a mesma tecnica de
     * {@link #removerPredicadoDePrimeiraSegundaPessoa}, que a classe ja usava.
     *
     * <p>Nao afrouxa a guarda: "e ai, cara!" continua sendo pego, e o contra-caso do teste
     * prova isso. Guarda que reprova texto correto e pior que guarda nenhuma — alarme falso
     * ensina a desligar o alarme.
     */
    private static final Pattern CARA_QUE_NAO_E_VOCATIVO = Pattern.compile(
        "\\b(?:a|na|da|à|as|nas|das|sua|tua|minha|nossa|uma|essa|esta|aquela|"
            + "é|e|era|estava|fica|ficou|muito|mais|tão|tao)\\s+(?:\\w+\\s+)?cara\\b",
        FLAGS);

    /**
     * PROPÓSITO DE NEGÓCIO: entrega o texto sem os {@code cara} que são rosto ou preço, para a
     * checagem de vocativo enxergar só os que são tratamento.
     * <p>INVARIANTES DO DOMÍNIO: não altera o texto persistido — é cópia para inspeção.
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo propaga NPE (entrada inválida do chamador).
     */
    private static String removerCaraQueNaoEVocativo(String texto) {
        return CARA_QUE_NAO_E_VOCATIVO.matcher(texto).replaceAll(" ");
    }

    private static final Pattern TRATAMENTO_MASC_COM_FEM_EN =
        Pattern.compile("\\b(" + TRATAMENTO_MASC + ")\\b", FLAGS);

    private static final Pattern TRATAMENTO_FEM_COM_MASC_EN =
        Pattern.compile("\\b(" + TRATAMENTO_FEM + ")\\b", FLAGS);

    private static final Pattern DELE_COM_HER =
        Pattern.compile("\\bdele\\b", FLAGS);

    private static final Pattern DELA_COM_HIM =
        Pattern.compile("\\bdela\\b", FLAGS);

    /**
     * PROPÓSITO DE NEGÓCIO: reúne evidências objetivas de concordância,
     * parentesco, expressão idiomática e agressividade indevida numa fala PT-BR.
     *
     * <p>INVARIANTES DO DOMÍNIO: tags ASS são invisíveis à análise; gênero ou
     * parentesco só cruza idiomas quando o inglês fornece evidência inequívoca.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: tradução ausente retorna resultado
     * limpo e nenhuma alteração é realizada no conteúdo recebido.
     */
    public ResultadoDeteccaoConcordancia analisar(String originalIngles, String traducaoPt) {
        if (traducaoPt == null || traducaoPt.isBlank()) {
            return ResultadoDeteccaoConcordancia.limpo();
        }

        String texto = removerTagsAss(traducaoPt);
        Set<String> motivos = new LinkedHashSet<>();

        // Regras INTERNAS ao português: valem mesmo sem original, porque a discordância é
        // visível sozinha ("a menino" está errado independentemente do que a fala dizia em
        // inglês). Já extraídas para o subpacote e chamadas pelo contrato RegraDeRevisao — o
        // `null` no primeiro argumento é honesto: elas não olham o original.
        for (RegraDeRevisao regra : REGRAS_INTERNAS_AO_PORTUGUES) {
            regra.detectar(null, texto, motivos);
        }
        detectarVerboPredicado(texto, motivos);
        adicionarSeEncontrado(motivos, GRACAS_AO_DEUS, texto,
            "Expressão idiomática inválida; em PT-BR usa-se 'graças a Deus'");

        if (originalIngles != null && !originalIngles.isBlank()) {
            String original = removerTagsAss(originalIngles);
            detectarPronomesECruzamento(original, texto, motivos);
            detectarTratamentos(original, texto, motivos);
            // Regras que COMPARAM com o inglês. A separação em duas listas não é estética: as
            // de cima rodariam com original nulo e produziriam motivo sem base.
            for (RegraDeRevisao regra : REGRAS_QUE_COMPARAM_COM_O_ORIGINAL) {
                regra.detectar(original, texto, motivos);
            }
        }

        if (motivos.isEmpty()) {
            return ResultadoDeteccaoConcordancia.limpo();
        }
        return new ResultadoDeteccaoConcordancia(true, List.copyOf(motivos));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: compara marcas explícitas de gênero do inglês com
     * pronomes, tratamentos e predicados usados na tradução brasileira.
     *
     * <p>INVARIANTES DO DOMÍNIO: exige evidência masculina ou feminina no
     * original; `I` e `you` isolados nunca revelam o gênero de falante ou alvo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: ausência de evidência explícita não
     * adiciona motivo e preserva a fala para eventual revisão contextual.
     */
    private void detectarPronomesECruzamento(String original, String texto, Set<String> motivos) {
        detectarObjetoComHer(original, texto, motivos);
        detectarObjetoComHim(original, texto, motivos);
        detectarSujeitoInicialTrocado(original, texto, motivos);
        detectarSujeitoEPredicadoComShe(original, texto, motivos);
        detectarSujeitoEPredicadoComHe(original, texto, motivos);
        detectarPredicadoPorEvidenciaDeGenero(original, texto, motivos);
    }

    /**
     * {@code her} no original × masculino na tradução: objeto, imperativo, regência, possessivo.
     *
     * <h2>Por que o {@code him} no MESMO original cancela tudo</h2>
     * Quando a frase inglesa cita os dois, o masculino do português pode estar traduzindo o
     * {@code him} — e aí não há cruzamento nenhum. Medido no acervo em 22/08/2026, as três
     * acusações desta família eram todas isto:
     * <pre>
     * EN: Eve appears intent on making him her Adam.
     * PT: Mana parece determinada a fazer dele seu Adão.        <- "dele" = him, "seu" = her
     * EN: The newsman brought her back with him during our First Contact.
     * PT: O repórter a trouxe de volta com ele...               <- "com ele" = with him
     * EN: ...Beltorchika willingly opened her heart to him.
     * PT: ...Beltorchika abriu o coração para ele...            <- "para ele" = to him
     * </pre>
     * As três traduções estão CERTAS. Guarda que reprova código correto é pior que guarda
     * nenhuma: alarme falso ensina a desligar o alarme.
     *
     * <p>Não é regra nova nem afrouxamento: {@link #detectarTratamentos} e
     * {@link #detectarPredicadoPorEvidenciaDeGenero} já exigiam evidência de UM SÓ gênero
     * ({@code femEn && !mascEn}). Estes dois é que estavam fora do padrão da própria classe.
     */
    private void detectarObjetoComHer(String original, String texto, Set<String> motivos) {
        if (!LexicoGenero.HER_EN.matcher(original).find()
            || LexicoGenero.HIM_EN.matcher(original).find()) {
            return;
        }
        adicionarSeEncontrado(motivos, OBJETO_MASC_COM_HER_EN, texto,
            "Original usa 'her', mas tradução aponta para masculino (ele/o/dele/para ele)");
        adicionarSeEncontrado(motivos, IMPERATIVO_PARA_ELE_COM_HER, texto,
            "Original usa 'her', mas imperativo dirige-se a 'ele'");
        adicionarSeEncontrado(motivos, VI_ELE_COM_HER, texto,
            "Original usa 'her', mas verbo rege pronome/objeto masculino");
        if (DELE_COM_HER.matcher(texto).find() && !contemIndicioFemininoPt(texto)) {
            motivos.add("Original usa 'her', mas tradução usa 'dele' (possessivo masculino)");
        }
    }

    /**
     * Lado espelhado: {@code him} no original × feminino na tradução. O {@code her} no mesmo
     * original cancela pela razão simétrica — ver {@link #detectarObjetoComHer}.
     */
    private void detectarObjetoComHim(String original, String texto, Set<String> motivos) {
        if (!LexicoGenero.HIM_EN.matcher(original).find()
            || LexicoGenero.HER_EN.matcher(original).find()) {
            return;
        }
        adicionarSeEncontrado(motivos, OBJETO_FEM_COM_HIM_EN, texto,
            "Original usa 'him', mas tradução aponta para feminino (ela/a/dela/para ela)");
        adicionarSeEncontrado(motivos, IMPERATIVO_PARA_ELA_COM_HIM, texto,
            "Original usa 'him', mas imperativo dirige-se a 'ela'");
        adicionarSeEncontrado(motivos, VI_ELA_COM_HIM, texto,
            "Original usa 'him', mas verbo rege pronome/objeto feminino");
        if (DELA_COM_HIM.matcher(texto).find() && !contemIndicioFemininoPt(texto)) {
            motivos.add("Original usa 'him', mas tradução usa 'dela' (possessivo feminino)");
        }
    }

    /**
     * SUJEITO INICIAL trocado — regra por POSIÇÃO, não por presença de pronome.
     *
     * <p>Nasceu de uma medição incômoda. Ao fechar o guarda da regra larga (que passou a exigir
     * ausência de QUALQUER referência masculina), o acerto real do run de 2026-07-28 deixava de
     * ser detectado:
     * <pre>
     *   EN "She's got a violent older brother, and I happened to know her, so..."
     *   PT "Ele tem um irmão mais velho violento, ..."     &lt;- "Ele" está errado
     * </pre>
     * "brother" ancora o masculino e cala a regra larga; {@code SUJEITO_ELE_COM_SHE} não pega
     * porque "tem" não está em {@code VERBOS_SUJEITO} — lista curada de propósito, para não
     * acusar meia legenda. Sem esta regra, o conserto trocaria 6 falsos positivos por 1 defeito
     * real invisível, o que não é conserto.
     *
     * <p>Quem abre a fala é o sujeito. Sujeito de gênero trocado é divergência objetiva e não
     * depende de o verbo estar em lista nenhuma.
     */
    private void detectarSujeitoInicialTrocado(String original, String texto, Set<String> motivos) {
        if (ABERTURA_SHE.matcher(original).find() && ABERTURA_ELE.matcher(texto).find()) {
            motivos.add("Fala começa com 'she' no original e com 'ele' na tradução: sujeito trocado");
        }
        if (ABERTURA_HE.matcher(original).find() && ABERTURA_ELA.matcher(texto).find()) {
            motivos.add("Fala começa com 'he' no original e com 'ela' na tradução: sujeito trocado");
        }
    }

    /** {@code she} no original: sujeito, pronome solto e predicado na tradução. */
    private void detectarSujeitoEPredicadoComShe(String original, String texto, Set<String> motivos) {
        if (LexicoGenero.SHE_EN.matcher(original).find()) {
            // A MESMA guarda de referência masculina que a regra 12 linhas abaixo já usa desde
            // 28/07. Ela nasceu da cicatriz descrita ali — "a fala tem DUAS referências de
            // gênero e o detector enxergava só uma" — mas foi aplicada a UMA das três regras
            // irmãs, e esta ficou de fora. Toda correção é auditoria, não conserto pontual.
            //
            // O custo apareceu em 12/08/2026, na revisão do Unicorn E08 (linha 238):
            //
            //   EN  "He's with her, so she'll be fine."
            //   PT  "Ele está com ela, então ela estará bem."      <- CORRETO
            //
            // O original tem "He's" E "her"; o detector via só o "she" e acusava o "Ele está".
            // Foi 1 dos 3 "problemas" que a varredura de 5.620 falas reportou — e os 3 eram
            // falso positivo. Ruído a esse nível ensina a ignorar o relatório inteiro.
            if (!LexicoGenero.PRONOME_MASCULINO_EN.matcher(original).find()) {
                adicionarSeEncontrado(motivos, SUJEITO_ELE_COM_SHE, texto,
                    "Original usa 'she', mas sujeito da tradução é 'ele'");
            }
            // O guarda tem que cobrir TODA referência masculina, não só o pronome "he" — a
            // mensagem promete "sem referência masculina" e precisa ser verdade. Com \bhe\b a
            // fala abaixo disparava, porque "him" não casa com "he":
            //
            //   EN  "Oh, I know about him!  Hiromi said she saw it all."
            //   PT  "Eu sei sobre ele! Hiromi disse que viu tudo."      <- CORRETO
            //   virou "Eu sei sobre ela! ..."                           <- regressão gravada
            //
            // Medido em 2026-07-28 no cache de Guilty Crown (07_Track4, evento 10): a proposta
            // do LLM foi aceita porque o motivo parecia legítimo. A fala tem DUAS referências de
            // gênero e o detector enxergava só uma.
            // Espelho do caso da nave: o "ela" junto no portugues diz que o "she" foi
            // traduzido, e o "ele" e outra coisa.
            if (!LexicoGenero.PRONOME_MASCULINO_EN.matcher(original).find()
                && !ELA_ISOLADA.matcher(removerObjetoPronominal(texto)).find()
                && ELE_ISOLADO.matcher(removerObjetoPronominal(texto)).find()) {
                motivos.add("Original usa 'she' sem referência masculina, mas a tradução contém o masculino 'ele'");
            }
            // Mesma verificacao de sujeito de detectarPredicadoPorEvidenciaDeGenero, pela
            // mesma razao: "Ela tem que lutar, ou o Argama sera perdido!" tem "perdido"
            // concordando com ARGAMA, em outra oracao. Sem isto, a familia consertada la
            // continuava vazando por aqui — a mesma classe de falha em outro fluxo.
            if (concordaComAPessoa(removerPredicadoDePrimeiraSegundaPessoa(texto),
                    PARTIC_MASC_APOS_VERBO)
                && !LexicoGenero.HE_EN.matcher(original).find()) {
                motivos.add("Original indica personagem/falante feminino ('she'), mas predicado está no masculino");
            }
        }

    }

    /** Lado espelhado: {@code he} no original: sujeito, pronome solto e predicado. */
    private void detectarSujeitoEPredicadoComHe(String original, String texto, Set<String> motivos) {
        if (LexicoGenero.HE_EN.matcher(original).find()) {
            // Lado espelhado da guarda acrescentada acima. A varredura de 12/08 não produziu
            // falso positivo por aqui, mas a assimetria seria dívida: a mesma fala com os
            // papéis invertidos ("She's with him, so he'll be fine.") acusaria.
            if (!LexicoGenero.PRONOME_FEMININO_EN.matcher(original).find()) {
                adicionarSeEncontrado(motivos, SUJEITO_ELA_COM_HE, texto,
                    "Original usa 'he', mas sujeito da tradução é 'ela'");
            }
            // Mesma correção do lado espelhado: \bshe\b não casa "her"/"hers", então
            // "He gave it to her" com "ela" na tradução disparava um motivo cuja mensagem
            // afirmava não haver referência feminina no original.
            // O "ele" JUNTO no portugues diz que o "he" FOI traduzido — entao o "ela" e outra
            // coisa, e em legenda quase sempre e coisa mesmo. Medido no acervo em 22/08/2026:
            //   EN: ...he's unable to return to the Argama before it begins its counterattack.
            //   PT: Ele nao consegue voltar para a Argama antes que ela inicie...
            // O "ela" e a NAVE. Acusar isso e mandar ao modelo uma fala impecavel.
            if (!LexicoGenero.PRONOME_FEMININO_EN.matcher(original).find()
                && !ELE_ISOLADO.matcher(removerObjetoPronominal(texto)).find()
                && ELA_ISOLADA.matcher(removerObjetoPronominal(texto)).find()) {
                motivos.add("Original usa 'he' sem referência feminina, mas a tradução contém o feminino 'ela'");
            }
            if (concordaComAPessoa(removerPredicadoDePrimeiraSegundaPessoa(texto),
                    PARTIC_FEM_APOS_VERBO)
                && !LexicoGenero.SHE_EN.matcher(original).find()) {
                motivos.add("Original indica personagem/falante masculino ('he'), mas predicado está no feminino");
            }
        }

    }

    /**
     * Predicado julgado pela evidência de gênero do original — mais largo que os dois acima,
     * porque aceita qualquer referência ({@code her}, {@code sir}, {@code brother}), não só o
     * pronome sujeito.
     *
     * <p>As quatro regras de predicado da classe passam pelo MESMO strip de 1ª/2ª pessoa.
     * Aplicá-lo só nas duas de cima deixaria o Unicorn E12 acusando por aqui — foi o que a
     * primeira execução mostrou: a fala trazia DOIS motivos, um de cada par.
     */
    private void detectarPredicadoPorEvidenciaDeGenero(String original, String texto, Set<String> motivos) {
        String limpo = removerPredicadoDePrimeiraSegundaPessoa(texto);

        if (LexicoGenero.PRONOME_FEMININO_EN.matcher(original).find()
            && concordaComAPessoa(limpo, PARTIC_MASC_APOS_VERBO)
            && !LexicoGenero.PRONOME_MASCULINO_EN.matcher(original).find()) {
            motivos.add("Original indica feminino, mas participio/adjetivo predicativo está no masculino");
        }

        if (LexicoGenero.PRONOME_MASCULINO_EN.matcher(original).find()
            && concordaComAPessoa(limpo, PARTIC_FEM_APOS_VERBO)
            && !LexicoGenero.PRONOME_FEMININO_EN.matcher(original).find()) {
            motivos.add("Original indica masculino, mas participio/adjetivo predicativo está no feminino");
        }

    }

    /** Advérbios que cabem entre o sujeito e o verbo sem serem o sujeito. */
    private static final Pattern INTERCALADA_ANTES_DO_VERBO = Pattern.compile(
        "\\b(?:não|nao|já|ja|ainda|sempre|nunca|também|tambem|só|so|realmente|talvez|quase)\\b",
        FLAGS);

    /** Sujeito que PODE ser a pessoa de quem o original fala. Qualquer outro cancela. */
    private static final Pattern SUJEITO_DE_TERCEIRA =
        Pattern.compile("(?:ele|ela|eles|elas)", FLAGS);

    /** Preposição colada no pronome: ali ele é OBJETO, nunca sujeito. */
    private static final Pattern PREPOSICAO_ANTES_DO_PRONOME = Pattern.compile(
        "\\b(?:de|do|da|com|contra|para|por|em|no|na|sobre|entre|sem|ao|à|a)\\s+$", FLAGS);

    /**
     * PROPÓSITO DE NEGÓCIO: confirma que o adjetivo encontrado fala da PESSOA de quem o original
     * fala, e não de um substantivo qualquer da própria frase portuguesa.
     *
     * <h2>O prejuízo, medido no acervo inteiro em 22/08/2026</h2>
     * O padrão sozinho casa {@code verbo + adjetivo} sem olhar QUEM é o sujeito. Das 23 falas que
     * o detector acusava em 67.178 pares EN/PT, <b>oito vinham daqui e as oito estavam certas</b>
     * — o adjetivo concordava com um substantivo da própria frase:
     * <pre>
     * "Algo está errado com aquela garota?"           errado  &lt;- Algo
     * "...ou o Argama sera perdido!"                  perdido &lt;- Argama
     * "Que nossa guerra contra eles é errada!"        errada  &lt;- guerra ("eles" é objeto de "contra")
     * "A causa de Kou é perdida."                     perdida &lt;- causa
     * "Esperando pela era certa."                     certa   &lt;- era (substantivo, não o verbo)
     * "Aquela pessoa esta morta."                     morta   &lt;- pessoa
     * "É certo tê-la?"                                construção impessoal
     * </pre>
     * Oito de oito falso positivo é alarme que ensina a desligar o alarme.
     *
     * <h2>Invariantes do domínio</h2>
     * <ul>
     *   <li>FALHA FECHADA: na dúvida NÃO acusa. Só segue quando o sujeito é
     *       {@code ele/ela/eles/elas} ou está elíptico no início da oração.</li>
     *   <li>Pronome precedido de preposição é OBJETO — mata o caso "contra eles".</li>
     *   <li>{@code é} abrindo a oração é impessoal ("É certo", "É difícil"). Os demais verbos
     *       abrindo a oração continuam valendo: "Está cansado." sem sujeito é exatamente o caso
     *       que a guarda existe para pegar, e o contra-caso do teste prova que ele sobrevive.</li>
     * </ul>
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem casamento devolve {@code false}; nunca lança.
     */
    private static boolean concordaComAPessoa(String texto, Pattern predicado) {
        java.util.regex.Matcher m = predicado.matcher(texto);
        while (m.find()) {
            if (sujeitoPodeSerAPessoa(texto, m.start(), m.group(1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: olha para trás do verbo e decide se o sujeito daquela oração pode ser
     * a pessoa referida no original.
     * <p>INVARIANTES DO DOMÍNIO: a oração é recortada na pontuação — o que vem antes de vírgula ou
     * ponto pertence a outra oração e não é sujeito desta.
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança; devolve {@code false} quando não reconhece.
     */
    private static boolean sujeitoPodeSerAPessoa(String texto, int inicioDoVerbo, String verbo) {
        String antes = texto.substring(0, inicioDoVerbo);
        int corte = -1;
        for (char pontuacao : new char[] {44, 46, 33, 63, 59, 58}) {
            corte = Math.max(corte, antes.lastIndexOf(pontuacao));
        }
        String oracao = corte >= 0 ? antes.substring(corte + 1) : antes;
        String semAdverbios = INTERCALADA_ANTES_DO_VERBO.matcher(oracao).replaceAll(" ").trim();

        if (semAdverbios.isEmpty()) {
            return !"é".equalsIgnoreCase(verbo.trim());
        }
        int ultimoEspaco = semAdverbios.lastIndexOf(32);
        String candidato = ultimoEspaco < 0
            ? semAdverbios
            : semAdverbios.substring(ultimoEspaco + 1);
        if (!SUJEITO_DE_TERCEIRA.matcher(candidato).matches()) {
            return false;
        }
        String antesDoPronome = semAdverbios.substring(0, Math.max(0, ultimoEspaco + 1));
        return !PREPOSICAO_ANTES_DO_PRONOME.matcher(antesDoPronome).find();
    }

    private void detectarTratamentos(String original, String texto, Set<String> motivos) {
        boolean femEn = LexicoGenero.PRONOME_FEMININO_EN.matcher(original).find();
        boolean mascEn = LexicoGenero.PRONOME_MASCULINO_EN.matcher(original).find();

        // O tratamento do OUTRO genero presente no portugues significa que a referencia do
        // original ja esta representada, e o tratamento acusado fala de outra pessoa. Medido
        // no acervo em 22/08/2026:
        //   EN: The kid with a girl's name?
        //   PT: O garoto com um nome de menina?   <- "menina" traduz "girl's"; "garoto" e o kid
        // A traducao esta impecavel: o feminino do ingles qualifica o NOME, nao a pessoa.
        boolean tratamentoFemNoPt = TRATAMENTO_FEM_COM_MASC_EN.matcher(texto).find();
        boolean tratamentoMascNoPt =
            TRATAMENTO_MASC_COM_FEM_EN.matcher(removerCaraQueNaoEVocativo(texto)).find();

        if (femEn && !mascEn && !tratamentoFemNoPt) {
            adicionarSeEncontrado(motivos, TRATAMENTO_MASC_COM_FEM_EN,
                removerCaraQueNaoEVocativo(texto),
                "Tratamento/vocativo masculino (senhor/garoto/moço) com referência feminina no original");
        }
        if (mascEn && !femEn && !tratamentoMascNoPt) {
            adicionarSeEncontrado(motivos, TRATAMENTO_FEM_COM_MASC_EN, texto,
                "Tratamento/vocativo feminino (senhora/garota/moça) com referência masculina no original");
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: encontra troca objetiva de parentesco entre o inglês
     * e o PT-BR sem inferir gênero a partir de outros substantivos da frase.
     *
     * <p>INVARIANTES DO DOMÍNIO: a regra só dispara quando o original contém um
     * único lado da relação relevante; construções com pai e mãe juntos ficam
     * para revisão contextual.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: referência ambígua não adiciona motivo
     * e a fala permanece inalterada.
     */

    /**
     * PROPÓSITO DE NEGÓCIO: mantém a intensidade do insulto compatível com o
     * original e com a preferência de tradução realista definida por Paulo.
     * <p>INVARIANTES DO DOMÍNIO: palavrão equivalente ou insulto interrompido no
     * inglês permite forma forte; fala neutra não recebe agressividade inventada.
     * <p>COMPORTAMENTO EM CASO DE FALHA: linguagem não cadastrada não é julgada
     * por esta regra conservadora.
     */

    private void detectarVerboPredicado(String texto, Set<String> motivos) {
        adicionarSeEncontrado(motivos, ELA_COM_PREDICADO_MASC, texto,
            "Sujeito 'ela' com predicado/adjetivo no masculino");
        adicionarSeEncontrado(motivos, ELE_COM_PREDICADO_FEM, texto,
            "Sujeito 'ele' com predicado/adjetivo no feminino");
        // O plural sai do texto quando vem depois de PREPOSICAO: ali ele e objeto, nao
        // sujeito, e o adjetivo concorda com outra coisa. Medido no acervo em 22/08/2026:
        //   EN: that our war with them is wrong!
        //   PT: Que nossa guerra contra eles e errada!   <- "errada" concorda com GUERRA
        // A traducao esta certa; quem estava errado era o detector.
        String semPluralObjeto = removerPluralAposPreposicao(texto);
        adicionarSeEncontrado(motivos, ELAS_COM_PREDICADO_MASC, semPluralObjeto,
            "Sujeito 'elas' com predicado no masculino");
        adicionarSeEncontrado(motivos, ELES_COM_PREDICADO_FEM, semPluralObjeto,
            "Sujeito 'eles' com predicado no feminino");
    }

    private static boolean contemIndicioFemininoPt(String texto) {
        return Pattern.compile("\\b(" + SUBST_FEM + "|ela|elas|dela|delas|nela|a ela)\\b", FLAGS)
            .matcher(texto).find();
    }

    private static void adicionarSeEncontrado(
        Set<String> motivos, Pattern pattern, String texto, String descricao
    ) {
        Matcher matcher = pattern.matcher(texto);
        if (matcher.find()) {
            motivos.add(descricao + ": \"" + matcher.group().trim() + "\"");
        }
    }

    private static String removerTagsAss(String texto) {
        return texto.replaceAll("\\{[^{}]*}", " ")
            .replace("\\N", " ")
            .replace("\\n", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }
}
