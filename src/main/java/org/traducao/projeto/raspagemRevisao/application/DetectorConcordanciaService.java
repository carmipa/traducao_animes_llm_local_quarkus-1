package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
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

    private static final Pattern ART_MASC_COM_SUBST_FEM =
        Pattern.compile("\\b(o|um|este|esse|aquele|do|no|ao|pelo|num)\\s+(" + SUBST_FEM + ")\\b", FLAGS);

    private static final Pattern ART_FEM_COM_SUBST_MASC =
        Pattern.compile("\\b(uma|esta|essa|aquela|da|na|à|pela|numa)\\s+(" + SUBST_MASC + ")\\b", FLAGS);

    private static final Pattern ADJ_MASC_COM_SUBST_FEM =
        Pattern.compile("\\b(" + ADJ_MASC + ")\\s+(" + SUBST_FEM + ")\\b", FLAGS);

    private static final Pattern ADJ_FEM_COM_SUBST_MASC =
        Pattern.compile("\\b(" + ADJ_FEM + ")\\s+(" + SUBST_MASC + ")\\b", FLAGS);

    private static final Pattern SUBST_FEM_COM_ADJ_MASC =
        Pattern.compile("\\b(" + SUBST_FEM + ")\\s+(" + ADJ_MASC_POSPOSTO + ")\\b", FLAGS);

    private static final Pattern SUBST_MASC_COM_ADJ_FEM =
        Pattern.compile("\\b(" + SUBST_MASC + ")\\s+(" + ADJ_FEM_POSPOSTO + ")\\b", FLAGS);

    private static final Pattern RELACAO_PAI_EN = Pattern.compile("\\b(father|dad|daddy)\\b", FLAGS);
    private static final Pattern RELACAO_MAE_EN = Pattern.compile("\\b(mother|mom|mommy|mum|mummy)\\b", FLAGS);
    private static final Pattern RELACAO_FILHO_EN = Pattern.compile("\\bson\\b", FLAGS);
    private static final Pattern RELACAO_FILHA_EN = Pattern.compile("\\bdaughter\\b", FLAGS);
    private static final Pattern RELACAO_IRMAO_EN = Pattern.compile("\\bbrother\\b", FLAGS);
    private static final Pattern RELACAO_IRMA_EN = Pattern.compile("\\bsister\\b", FLAGS);
    private static final Pattern PAI_PT = Pattern.compile("\\b(pai|papai)\\b", FLAGS);
    private static final Pattern MAE_PT = Pattern.compile("\\b(mãe|mae|mamãe|mamae)\\b", FLAGS);
    private static final Pattern FILHO_PT = Pattern.compile("\\bfilho\\b", FLAGS);
    private static final Pattern FILHA_PT = Pattern.compile("\\bfilha\\b", FLAGS);
    private static final Pattern IRMAO_PT = Pattern.compile("\\b(irmão|irmao)\\b", FLAGS);
    private static final Pattern IRMA_PT = Pattern.compile("\\b(irmã|irma)\\b", FLAGS);
    private static final Pattern PROFANIDADE_FORTE_PT = Pattern.compile("\\bfilh[oa] da puta\\b", FLAGS);
    private static final Pattern PROFANIDADE_FORTE_EN = Pattern.compile(
        "\\b(son of a (?:bitch|hitch)|motherfucker|fuck(?:er|ing)?|bitch|whore|bastard)\\b"
            + "|\\bson of a\\s*\\.\\.\\.", FLAGS);
    private static final Pattern EUFEMISMO_FILHO_DA_MAE = Pattern.compile("\\bfilho da mãe\\b", FLAGS);
    private static final Pattern GRACAS_AO_DEUS = Pattern.compile("\\bgraças ao deus\\b", FLAGS);

    // "a" sozinho fica fora do segundo grupo: é a preposição invariante em gênero
    // ("disse a ele" / "disse a ela" são ambos corretos), não o artigo feminino —
    // incluí-lo fazia "a ele" (construção comum e correta) ser sinalizado sempre.
    private static final Pattern PRONOME_ARTIGO_ERRADO =
        Pattern.compile("\\b(o|um|do|no|ao|pelo|lo|no)\\s+ela\\b|\\b(uma|da|na|à|pela|la)\\s+ele\\b", FLAGS);

    private static final Pattern PRONOME_FEMININO_EN = Pattern.compile(
        "\\b(she|her|hers|girl|woman|lady|mother|mom|sister|daughter|"
            + "princess|goddess|queen|heroine|miss|mrs|ms|madam|ma'am|female|wife|aunt|"
            + "grandma|grandmother|niece|waitress|actress|hostess)\\b", FLAGS);

    private static final Pattern PRONOME_MASCULINO_EN = Pattern.compile(
        "\\b(he|him|his|boy|man|guy|father|dad|brother|son|prince|god|king|"
            + "hero|mr|sir|male|husband|uncle|grandpa|grandfather|nephew|waiter|actor)\\b", FLAGS);

    private static final Pattern HER_EN = Pattern.compile("\\bher\\b", FLAGS);
    private static final Pattern HIM_EN = Pattern.compile("\\bhim\\b", FLAGS);
    private static final Pattern SHE_EN = Pattern.compile("\\bshe\\b", FLAGS);
    private static final Pattern HE_EN = Pattern.compile("\\bhe\\b", FLAGS);

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

    private static String removerObjetoPronominal(String texto) {
        return OBJETO_PRONOMINAL_ELE_ELA.matcher(texto).replaceAll(" ");
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

        detectarConcordanciaNominal(texto, motivos);
        detectarVerboPredicado(texto, motivos);
        adicionarSeEncontrado(motivos, GRACAS_AO_DEUS, texto,
            "Expressão idiomática inválida; em PT-BR usa-se 'graças a Deus'");

        if (originalIngles != null && !originalIngles.isBlank()) {
            String original = removerTagsAss(originalIngles);
            detectarPronomesECruzamento(original, texto, motivos);
            detectarTratamentos(original, texto, motivos);
            detectarParentesco(original, texto, motivos);
            detectarAgressividadeIntroduzida(original, texto, motivos);
        }

        if (motivos.isEmpty()) {
            return ResultadoDeteccaoConcordancia.limpo();
        }
        return new ResultadoDeteccaoConcordancia(true, List.copyOf(motivos));
    }

    private void detectarConcordanciaNominal(String texto, Set<String> motivos) {
        adicionarSeEncontrado(motivos, ART_MASC_COM_SUBST_FEM, texto,
            "Artigo/pronome demonstrativo masculino antes de substantivo feminino");
        adicionarSeEncontrado(motivos, ART_FEM_COM_SUBST_MASC, texto,
            "Artigo/pronome demonstrativo feminino antes de substantivo masculino");
        adicionarSeEncontrado(motivos, ADJ_MASC_COM_SUBST_FEM, texto,
            "Adjetivo masculino antes de substantivo feminino");
        adicionarSeEncontrado(motivos, ADJ_FEM_COM_SUBST_MASC, texto,
            "Adjetivo feminino antes de substantivo masculino");
        adicionarSeEncontrado(motivos, SUBST_FEM_COM_ADJ_MASC, texto,
            "Substantivo feminino com adjetivo/particípio masculino");
        adicionarSeEncontrado(motivos, SUBST_MASC_COM_ADJ_FEM, texto,
            "Substantivo masculino com adjetivo/particípio feminino");
        adicionarSeEncontrado(motivos, PRONOME_ARTIGO_ERRADO, texto,
            "Artigo/pronome oblíquo incompatível (o ela / a ele / lo ela)");
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
        if (HER_EN.matcher(original).find()) {
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

        if (HIM_EN.matcher(original).find()) {
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

        if (SHE_EN.matcher(original).find()) {
            adicionarSeEncontrado(motivos, SUJEITO_ELE_COM_SHE, texto,
                "Original usa 'she', mas sujeito da tradução é 'ele'");
            if (!HE_EN.matcher(original).find() && ELE_ISOLADO.matcher(removerObjetoPronominal(texto)).find()) {
                motivos.add("Original usa 'she' sem referência masculina, mas a tradução contém o masculino 'ele'");
            }
            if (PARTIC_MASC_APOS_VERBO.matcher(texto).find()
                && !HE_EN.matcher(original).find()) {
                motivos.add("Original indica personagem/falante feminino ('she'), mas predicado está no masculino");
            }
        }

        if (HE_EN.matcher(original).find()) {
            adicionarSeEncontrado(motivos, SUJEITO_ELA_COM_HE, texto,
                "Original usa 'he', mas sujeito da tradução é 'ela'");
            if (!SHE_EN.matcher(original).find() && ELA_ISOLADA.matcher(removerObjetoPronominal(texto)).find()) {
                motivos.add("Original usa 'he' sem referência feminina, mas a tradução contém o feminino 'ela'");
            }
            if (PARTIC_FEM_APOS_VERBO.matcher(texto).find()
                && !SHE_EN.matcher(original).find()) {
                motivos.add("Original indica personagem/falante masculino ('he'), mas predicado está no feminino");
            }
        }

        if (PRONOME_FEMININO_EN.matcher(original).find()
            && PARTIC_MASC_APOS_VERBO.matcher(texto).find()
            && !PRONOME_MASCULINO_EN.matcher(original).find()) {
            motivos.add("Original indica feminino, mas participio/adjetivo predicativo está no masculino");
        }

        if (PRONOME_MASCULINO_EN.matcher(original).find()
            && PARTIC_FEM_APOS_VERBO.matcher(texto).find()
            && !PRONOME_FEMININO_EN.matcher(original).find()) {
            motivos.add("Original indica masculino, mas participio/adjetivo predicativo está no feminino");
        }

    }

    private void detectarTratamentos(String original, String texto, Set<String> motivos) {
        boolean femEn = PRONOME_FEMININO_EN.matcher(original).find();
        boolean mascEn = PRONOME_MASCULINO_EN.matcher(original).find();

        if (femEn && !mascEn) {
            adicionarSeEncontrado(motivos, TRATAMENTO_MASC_COM_FEM_EN, texto,
                "Tratamento/vocativo masculino (senhor/garoto/moço) com referência feminina no original");
        }
        if (mascEn && !femEn) {
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
    private void detectarParentesco(String original, String texto, Set<String> motivos) {
        detectarRelacaoInvertida(original, texto, motivos, RELACAO_PAI_EN, RELACAO_MAE_EN, MAE_PT,
            "Original menciona pai, mas a tradução usa mãe");
        detectarRelacaoInvertida(original, texto, motivos, RELACAO_MAE_EN, RELACAO_PAI_EN, PAI_PT,
            "Original menciona mãe, mas a tradução usa pai");
        detectarRelacaoInvertida(original, texto, motivos, RELACAO_FILHO_EN, RELACAO_FILHA_EN, FILHA_PT,
            "Original menciona filho, mas a tradução usa filha");
        detectarRelacaoInvertida(original, texto, motivos, RELACAO_FILHA_EN, RELACAO_FILHO_EN, FILHO_PT,
            "Original menciona filha, mas a tradução usa filho");
        detectarRelacaoInvertida(original, texto, motivos, RELACAO_IRMAO_EN, RELACAO_IRMA_EN, IRMA_PT,
            "Original menciona irmão, mas a tradução usa irmã");
        detectarRelacaoInvertida(original, texto, motivos, RELACAO_IRMA_EN, RELACAO_IRMAO_EN, IRMAO_PT,
            "Original menciona irmã, mas a tradução usa irmão");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aplica uma comparação de parentesco somente quando
     * a fala inglesa fornece evidência inequívoca da relação.
     * <p>INVARIANTES DO DOMÍNIO: presença simultânea das duas relações bloqueia
     * a heurística para evitar associar pessoas diferentes.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não registra diagnóstico especulativo.
     */
    private void detectarRelacaoInvertida(
        String original,
        String texto,
        Set<String> motivos,
        Pattern esperadaEn,
        Pattern opostaEn,
        Pattern opostaPt,
        String descricao
    ) {
        if (esperadaEn.matcher(original).find()
            && !opostaEn.matcher(original).find()
            && opostaPt.matcher(texto).find()) {
            motivos.add(descricao);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mantém a intensidade do insulto compatível com o
     * original e com a preferência de tradução realista definida por Paulo.
     * <p>INVARIANTES DO DOMÍNIO: palavrão equivalente ou insulto interrompido no
     * inglês permite forma forte; fala neutra não recebe agressividade inventada.
     * <p>COMPORTAMENTO EM CASO DE FALHA: linguagem não cadastrada não é julgada
     * por esta regra conservadora.
     */
    private void detectarAgressividadeIntroduzida(String original, String texto, Set<String> motivos) {
        if (PROFANIDADE_FORTE_PT.matcher(texto).find()
            && !PROFANIDADE_FORTE_EN.matcher(original).find()) {
            motivos.add("Tradução introduziu palavrão forte ausente no original");
        }
        if (PROFANIDADE_FORTE_EN.matcher(original).find()
            && EUFEMISMO_FILHO_DA_MAE.matcher(texto).find()) {
            motivos.add("Insulto forte do original foi suavizado contra a preferência de tradução");
        }
    }

    private void detectarVerboPredicado(String texto, Set<String> motivos) {
        adicionarSeEncontrado(motivos, ELA_COM_PREDICADO_MASC, texto,
            "Sujeito 'ela' com predicado/adjetivo no masculino");
        adicionarSeEncontrado(motivos, ELE_COM_PREDICADO_FEM, texto,
            "Sujeito 'ele' com predicado/adjetivo no feminino");
        adicionarSeEncontrado(motivos, ELAS_COM_PREDICADO_MASC, texto,
            "Sujeito 'elas' com predicado no masculino");
        adicionarSeEncontrado(motivos, ELES_COM_PREDICADO_FEM, texto,
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
