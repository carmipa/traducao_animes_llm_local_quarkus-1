package org.traducao.projeto.revisaoLore.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.revisaoLore.domain.ResultadoDeteccaoLore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: prioriza falas com possível erro terminológico antes
 * de chamar o LLM, respeitando a lore específica da obra selecionada.
 * <p>INVARIANTES DO DOMÍNIO: nomes canônicos, equivalências PT-BR autorizadas
 * e termos oficiais preservados não podem virar falsos resíduos em inglês.
 * <p>COMPORTAMENTO EM CASO DE FALHA: entradas insuficientes retornam resultado
 * limpo; suspeitas são somente sinalizadas e nunca modificam a legenda.
 */
@Service
public class DetectorTermosLoreService {

    private static final Pattern NOME_PROPRIO = Pattern.compile(
        "\\b(?:[A-Z][A-Za-z0-9'’.-]{2,}|[A-Z]{2,}(?:-[A-Z0-9]+)?)(?:\\s+(?:[A-Z][A-Za-z0-9'’.-]{2,}|[A-Z]{2,}(?:-[A-Z0-9]+)?))*\\b"
    );
    private static final Pattern PALAVRA_LATINA = Pattern.compile("\\b[A-Za-z]{3,}\\b");
    private static final Map<String, List<String>> TRADUCOES_LITERAIS_SUSPEITAS = criarTraducoesLiteraisSuspeitas();
    private static final Map<String, List<String>> TERMOS_TRADUZIVEIS_ACEITOS = criarTermosTraduziveisAceitos();
    private static final Set<String> TERMOS_LORE_SOLTEIROS_RELEVANTES = Set.of(
        "aeug", "titans", "anaheim", "apsalus", "sahalin", "sakhalin", "char", "amuro",
        "londo", "phenex", "unicorn", "narrative", "banshee", "legion", "handler",
        "processor", "juggernaut", "valkyrie", "macross",
        "zeon", "gundam", "zaku", "gouf", "gelgoog", "dom", "gm", "kampfer", "sazabi",
        "alex", "sinanju", "axis", "jaburo", "odessa", "libot", "albion", "shiro",
        "aina", "karen", "eledore", "michel", "kiki", "ginias", "norris", "packard",
        "kojima", "kou", "gato", "nina", "cima", "bernie", "christina", "chris",
        "bright", "sayla", "lalah", "hathaway", "quess", "gyunei", "nanai", "jona",
        "michelle", "rita", "zoltan",
        "shin", "shinei", "nouzen", "lena", "vladilena", "milize", "raiden", "shuga",
        "anju", "theoto", "theo", "rikka", "kurena", "kukumila", "frederica",
        "rosenfort", "ernst", "zimmerman", "eugene", "rantz", "spearhead",
        "nordlicht", "morpho", "feldress", "para-raid", "san", "magnolia", "giad",
        "alba", "colorata", "undertaker"
    );
    private static final Set<String> PALAVRAS_IGNORADAS = Set.of(
        "the", "and", "you", "your", "that", "this", "with", "from", "what", "when", "where",
        "why", "how", "are", "was", "were", "have", "has", "had", "not", "but", "for", "all",
        "out", "our", "his", "her", "him", "she", "they", "them", "will", "can", "just", "like",
        "get", "got", "one", "two", "now", "yes", "hey", "sir", "miss", "lord", "lady", "man",
        "boy", "girl", "god", "damn", "hell", "okay", "ok", "well", "come", "here", "there",
        "even", "passing", "beginning", "fall", "leave", "these", "if", "let", "yeah",
        "youre", "im", "dont", "cant", "wont", "ill", "ive", "thats", "whats", "base",
        "someone", "anyone", "something", "anything", "forever", "indeed", "maybe", "please",
        "thanks", "thank", "hello", "goodbye", "always", "never", "every", "second", "father",
        "laugh", "heaven", "chairman", "minister", "princess", "commander", "ensign", "adm"
    );
    private static final Set<String> COGNATOS_VALIDOS_PT = Set.of(
        "chance", "cosmos", "crime", "ideal", "item", "monitor", "normal", "real", "superior"
    );

    public ResultadoDeteccaoLore auditar(String originalIngles, String traducaoPt) {
        return auditar(originalIngles, traducaoPt, null);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: audita a fala usando somente as regras pertinentes
     * à lore da obra ativa, reduzindo falsos positivos entre franquias.
     * <p>INVARIANTES DO DOMÍNIO: termos canônicos preservados e traduções PT-BR
     * explicitamente aceitas não são tratados como resíduos; sem lore mantém o
     * comportamento global compatível com chamadas antigas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: entradas vazias produzem resultado
     * limpo e nenhuma alteração é autorizada diretamente por este detector.
     */
    public ResultadoDeteccaoLore auditar(String originalIngles, String traducaoPt, String loreObraAtiva) {
        if (originalIngles == null || originalIngles.isBlank()
            || traducaoPt == null || traducaoPt.isBlank()) {
            return ResultadoDeteccaoLore.limpo();
        }

        List<String> motivos = new ArrayList<>();
        String en = originalIngles.trim();
        String pt = traducaoPt.trim();
        String loreLower = loreObraAtiva == null || loreObraAtiva.isBlank()
            ? null
            : loreObraAtiva.toLowerCase(Locale.ROOT);

        detectarTraducoesLiteraisSuspeitas(en, pt, motivos, loreLower);
        detectarTermosTraduziveisEmIngles(en, pt, motivos);
        detectarNomesInglesRemanescentes(en, pt, motivos, loreLower);
        detectarNomesPropriosDivergentes(en, pt, motivos, loreLower);
        detectarTermosMaiusculosSuspeitos(pt, motivos);

        if (motivos.isEmpty()) {
            return ResultadoDeteccaoLore.limpo();
        }
        return new ResultadoDeteccaoLore(true, List.copyOf(motivos));
    }

    /** Termo canônico vale para a obra ativa? Sem lore informado, vale globalmente. */
    private boolean loreMenciona(String loreLower, String termo) {
        return loreLower == null || contemExpressaoInteira(loreLower, termo);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: identifica palavras inglesas copiadas na tradução
     * sem confundi-las com termos oficiais ou cognatos válidos em português.
     * <p>INVARIANTES DO DOMÍNIO: nomes próprios são tratados em etapa própria e
     * palavras declaradas pela lore ativa permanecem protegidas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não produz motivo quando não existe
     * candidato inequívoco; nunca altera o texto recebido.
     */
    private void detectarNomesInglesRemanescentes(
        String en, String pt, List<String> motivos, String loreLower) {
        Matcher matcher = PALAVRA_LATINA.matcher(en);
        Set<String> candidatos = new LinkedHashSet<>();
        Set<String> tokensNomeProprio = tokensDeNomesProprios(en);
        while (matcher.find()) {
            String palavra = matcher.group().toLowerCase(Locale.ROOT);
            if (palavra.matches("tag\\d+")) {
                continue;
            }
            if (palavra.length() >= 4
                && !PALAVRAS_IGNORADAS.contains(palavra)
                && !COGNATOS_VALIDOS_PT.contains(palavra)
                && !tokensNomeProprio.contains(palavra)) {
                candidatos.add(palavra);
            }
        }

        String ptLower = pt.toLowerCase(Locale.ROOT);
        for (String candidato : candidatos) {
            if (contemPalavraInteira(ptLower, candidato) && !loreMencionaExclusivamente(loreLower, candidato)) {
                motivos.add("Possivel nome/termo em ingles remanescente na traducao: \"" + candidato + "\"");
            }
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reconhece um termo que a obra manda preservar.
     * <p>INVARIANTES DO DOMÍNIO: exige correspondência integral na lore ativa.
     * <p>COMPORTAMENTO EM CASO DE FALHA: lore ausente retorna falso.
     */
    private boolean loreMencionaExclusivamente(String loreLower, String termo) {
        return loreLower != null && contemExpressaoInteira(loreLower, termo);
    }

    private void detectarTermosTraduziveisEmIngles(String en, String pt, List<String> motivos) {
        String enLower = en.toLowerCase(Locale.ROOT);
        String ptLower = pt.toLowerCase(Locale.ROOT);

        for (Map.Entry<String, List<String>> entrada : TERMOS_TRADUZIVEIS_ACEITOS.entrySet()) {
            String termoIngles = entrada.getKey();
            if (!contemExpressaoInteira(enLower, termoIngles) || contemAlgumaExpressao(ptLower, entrada.getValue())) {
                continue;
            }
            if (contemExpressaoInteira(ptLower, termoIngles)) {
                motivos.add("Termo de faccao/organizacao traduzivel permaneceu em ingles: \""
                    + termoIngles + "\"");
            }
        }
    }

    private void detectarTraducoesLiteraisSuspeitas(String en, String pt, List<String> motivos, String loreLower) {
        String enLower = en.toLowerCase(Locale.ROOT);
        String ptLower = pt.toLowerCase(Locale.ROOT);

        for (Map.Entry<String, List<String>> entrada : TRADUCOES_LITERAIS_SUSPEITAS.entrySet()) {
            String termoCanonico = entrada.getKey();
            if (!enLower.contains(termoCanonico) || !loreMenciona(loreLower, termoCanonico)) {
                continue;
            }
            for (String traducaoSuspeita : entrada.getValue()) {
                if (ptLower.contains(traducaoSuspeita)) {
                    motivos.add("Possivel traducao literal de termo/nome canonico: \""
                        + traducaoSuspeita + "\" deveria preservar \"" + termoCanonico + "\" quando for lore");
                    break;
                }
            }
        }
    }

    private void detectarNomesPropriosDivergentes(String en, String pt, List<String> motivos, String loreLower) {
        Matcher matcherEn = NOME_PROPRIO.matcher(en);
        while (matcherEn.find()) {
            String grupo = matcherEn.group();
            // Divide o grupo por quebras de frase reais (. ! ? seguido de espaço),
            // ignorando abreviações comuns (Dr., Lt., U.C., etc.)
            String[] subNomes = grupo.split("(?<!\\b(?:Dr|Lt|Col|Capt|Gen|Mr|Mrs|Ms|St|U\\.C)\\.)(?<=[.!?])\\s+");
            for (String subNomeRaw : subNomes) {
                String nome = limparCandidatoNomeProprio(subNomeRaw);
                int indexNoOriginal = en.indexOf(subNomeRaw);
                if (deveIgnorarNomeProprio(nome, inicioEfetivoDaFala(en, indexNoOriginal >= 0 ? indexNoOriginal : matcherEn.start()), loreLower)
                    || traducaoAceitaParaTermo(nome, pt)) {
                    continue;
                }
                if (pt.contains(nome)) {
                    continue;
                }
                if (contemNomeCompostoParcial(pt, nome)) {
                    motivos.add("Nome proprio composto foi preservado apenas em parte; conferir se algum trecho foi traduzido: \"" + nome + "\"");
                } else if (!contemVarianteAproximada(pt, nome)) {
                    motivos.add("Nome proprio do original pode estar inconsistente na traducao: \"" + nome + "\"");
                }
            }
        }
    }

    private void detectarTermosMaiusculosSuspeitos(String pt, List<String> motivos) {
        Matcher matcher = Pattern.compile("\\b[A-Z]{2,}\\b").matcher(pt);
        while (matcher.find()) {
            String token = matcher.group();
            if (token.length() >= 3 && !token.equals("ASS") && !token.equals("SSA")) {
                motivos.add("Sigla ou termo todo em maiusculas pode indicar lore fora do padrao: \"" + token + "\"");
                break;
            }
        }
    }

    private boolean contemVarianteAproximada(String pt, String nome) {
        String[] partes = nome.split("\\s+");
        if (partes.length == 1) {
            return false;
        }
        int encontrados = 0;
        for (String parte : partes) {
            if (parte.length() >= 3 && pt.toLowerCase(Locale.ROOT).contains(parte.toLowerCase(Locale.ROOT))) {
                encontrados++;
            }
        }
        return encontrados >= Math.max(1, partes.length - 1);
    }

    private Set<String> tokensDeNomesProprios(String en) {
        Set<String> tokens = new LinkedHashSet<>();
        Matcher matcher = NOME_PROPRIO.matcher(en);
        while (matcher.find()) {
            for (String parte : limparCandidatoNomeProprio(matcher.group()).split("\\s+")) {
                String normalizada = normalizarTokenNome(parte);
                if (normalizada.length() >= 3 && !normalizada.matches("tag\\d+")) {
                    tokens.add(normalizada);
                }
            }
        }
        return tokens;
    }

    private String removerArtigoInicialIngles(String nome) {
        return nome.replaceFirst("(?i)^(the|a|an)\\s+", "");
    }

    private String removerPossessivoIngles(String nome) {
        return nome.replaceAll("(?i)['’]s\\b", "");
    }

    private String limparCandidatoNomeProprio(String nome) {
        return removerPrefixoComumIngles(removerArtigoInicialIngles(removerPossessivoIngles(nome))).strip();
    }

    private String removerPrefixoComumIngles(String nome) {
        return nome.replaceFirst("(?i)^(one|two|three|four|five|six|seven|eight|nine|ten|all|these|those|this|that|some|any)\\s+", "");
    }

    private boolean deveIgnorarNomeProprio(String nome, boolean inicioEfetivoDaFala, String loreLower) {
        if (nome.length() < 4 || nome.matches("(?i)TAG\\d+")) {
            return true;
        }

        String[] partes = nome.split("\\s+");
        if (partes.length == 1) {
            String normalizada = normalizarTokenNome(partes[0]);
            if (PALAVRAS_IGNORADAS.contains(normalizada)) {
                return true;
            }
            return inicioEfetivoDaFala && !temIndicadorLoreSolteiro(partes[0], normalizada, loreLower);
        }
        return false;
    }

    private boolean inicioEfetivoDaFala(String texto, int inicioCandidato) {
        if (inicioCandidato <= 0) {
            return true;
        }
        String prefixo = texto.substring(0, Math.max(0, inicioCandidato))
            .replaceAll("(?i)\\[\\[TAG\\d+\\]\\]", "")
            .trim();
        if (prefixo.isEmpty()) {
            return true;
        }
        char ultimo = prefixo.charAt(prefixo.length() - 1);
        return ultimo == '.' || ultimo == '!' || ultimo == '?' || ultimo == '"' || ultimo == '”' || ultimo == '\'' || ultimo == '’';
    }

    private boolean temIndicadorLoreSolteiro(String original, String normalizada, String loreLower) {
        // O termo global só conta se pertencer à obra ativa: "Dom" no início de
        // uma fala do 86 não é o mobile suit do Gundam.
        return (TERMOS_LORE_SOLTEIROS_RELEVANTES.contains(normalizada) && loreMenciona(loreLower, normalizada))
            || original.matches(".*\\d.*")
            || original.matches("[A-Z0-9.-]{2,}");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reconhece nomes e títulos cuja forma PT-BR está
     * explicitamente autorizada pelo catálogo de equivalências.
     * <p>INVARIANTES DO DOMÍNIO: nomes compostos só são aceitos quando cada
     * parte permanece canônica ou possui uma variante traduzida cadastrada.
     * <p>COMPORTAMENTO EM CASO DE FALHA: parte não comprovada retorna falso e
     * mantém a fala sinalizada para análise segura.
     */
    private boolean traducaoAceitaParaTermo(String nome, String pt) {
        String ptLower = pt.toLowerCase(Locale.ROOT);
        List<String> aceitas = TERMOS_TRADUZIVEIS_ACEITOS.get(nome.toLowerCase(Locale.ROOT));
        if (aceitas != null && contemAlgumaExpressao(ptLower, aceitas)) {
            return true;
        }

        String[] partes = nome.split("\\s+");
        if (partes.length < 2) {
            return false;
        }
        for (String parte : partes) {
            String normalizada = normalizarTokenNome(parte);
            if (normalizada.isBlank()) {
                continue;
            }
            List<String> variantes = TERMOS_TRADUZIVEIS_ACEITOS.get(normalizada);
            boolean presenteOriginal = contemExpressaoInteira(ptLower, normalizada);
            boolean presenteTraduzida = variantes != null && contemAlgumaExpressao(ptLower, variantes);
            if (!presenteOriginal && !presenteTraduzida) {
                return false;
            }
        }
        return true;
    }

    private String normalizarTokenNome(String token) {
        return removerPossessivoIngles(token)
            .replaceAll("[^A-Za-z0-9]", "")
            .toLowerCase(Locale.ROOT);
    }

    private boolean contemNomeCompostoParcial(String pt, String nome) {
        String[] partes = nome.split("\\s+");
        if (partes.length < 2) {
            return false;
        }

        String ptLower = pt.toLowerCase(Locale.ROOT);
        int encontrados = 0;
        int relevantes = 0;
        for (String parte : partes) {
            String normalizada = parte.replaceAll("[^A-Za-z0-9]", "");
            if (normalizada.length() < 3) {
                continue;
            }
            relevantes++;
            if (ptLower.contains(normalizada.toLowerCase(Locale.ROOT))) {
                encontrados++;
            }
        }
        return relevantes >= 2 && encontrados > 0 && encontrados < relevantes;
    }

    private boolean contemAlgumaExpressao(String textoLower, List<String> expressoes) {
        return expressoes.stream().anyMatch(expressao -> contemExpressaoInteira(textoLower, expressao));
    }

    private boolean contemPalavraInteira(String textoLower, String palavraLower) {
        return contemExpressaoInteira(textoLower, palavraLower);
    }

    private boolean contemExpressaoInteira(String textoLower, String expressaoLower) {
        return Pattern
            .compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(expressaoLower) + "(?![\\p{L}\\p{N}])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
            .matcher(textoLower)
            .find();
    }

    private static Map<String, List<String>> criarTraducoesLiteraisSuspeitas() {
        Map<String, List<String>> termos = new LinkedHashMap<>();
        termos.put("narrative", List.of("narrativo", "narrativa"));
        termos.put("unicorn", List.of("unicórnio", "unicornio"));
        termos.put("phenex", List.of("fênix", "fenix"));
        termos.put("freedom", List.of("liberdade"));
        termos.put("justice", List.of("justiça", "justica"));
        termos.put("destiny", List.of("destino"));
        termos.put("stargazer", List.of("observador de estrelas", "observadora de estrelas"));
        termos.put("war in the pocket", List.of("guerra no bolso", "guerra de bolso"));
        termos.put("mobile suit", List.of("traje móvel", "traje movel", "roupa móvel", "roupa movel"));
        termos.put("mobile armor", List.of("armadura móvel", "armadura movel"));
        termos.put("newtype", List.of("novo tipo", "nova tipo"));
        termos.put("handler", List.of("manipulador", "manipuladora"));
        termos.put("processor", List.of("processador", "processadora"));
        termos.put("juggernaut", List.of("rolo compressor"));
        termos.put("shin", List.of("canela"));
        termos.put("dud rounds", List.of(
            "rodadas aleatorias",
            "rodadas aleatórias",
            "rodadas fracassadas",
            "rodadas dud",
            "rodadas falsas"
        ));
        return Map.copyOf(termos);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: cataloga equivalências PT-BR legítimas de títulos,
     * organizações e conceitos que não precisam permanecer em inglês.
     * <p>INVARIANTES DO DOMÍNIO: chaves ficam em inglês minúsculo e variantes
     * incluem grafias com e sem diacríticos quando necessário.
     * <p>COMPORTAMENTO EM CASO DE FALHA: o mapa final é imutável.
     */
    private static Map<String, List<String>> criarTermosTraduziveisAceitos() {
        Map<String, List<String>> termos = new LinkedHashMap<>();
        termos.put("earth federation", List.of(
            "federacao terrestre",
            "federação terrestre",
            "federacao da terra",
            "federação da terra"
        ));
        termos.put("federation", List.of("federacao", "federação"));
        termos.put("principality of zeon", List.of("principado de zeon"));
        termos.put("republic of zeon", List.of("republica de zeon", "república de zeon"));
        termos.put("universal century", List.of("seculo universal", "século universal"));
        termos.put("laplace declaration", List.of("declaracao de laplace", "declaração de laplace"));
        termos.put("earth", List.of("terra"));
        termos.put("minister", List.of("ministro", "ministra"));
        termos.put("princess", List.of("princesa"));
        termos.put("commander", List.of("comandante"));
        termos.put("ensign", List.of("alferes"));
        return Map.copyOf(termos);
    }
}
