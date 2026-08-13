package org.traducao.projeto.auditoria;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.cachetraducao.domain.CacheDocumento;
import org.traducao.projeto.raspagemRevisao.application.DetectorConcordanciaService;
import org.traducao.projeto.raspagemRevisao.application.concordancia.LexicoGenero;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: mede quanto o detector de concordância deixa de ver quando há um ADVÉRBIO
 * entre o verbo de ligação e o particípio. {@code "Ela está cansado"} é acusada;
 * {@code "Ela está muito cansado"} não — o padrão exige o particípio imediatamente após o verbo
 * ({@code \bela\s+(VERBO_AUX)\s+(PARTIC_MASC)\b}), e uma palavra no meio quebra o casamento.
 *
 * <h2>Como isto foi descoberto</h2>
 * Não por leitura de código: por um teste REPROVANDO. Ao montar o caso-controle de
 * {@code CorrecaoViaLlmChegaAoArquivoTest} escolhi "Ela está muito cansado" como defeito plantado,
 * e o LLM nunca foi consultado — zero chamadas. A explicação mais simples era a certa: a fala não
 * estava sendo detectada. Tirar o "muito" fez o caminho inteiro funcionar.
 *
 * <h2>Por que rodar o detector de produção</h2>
 * O critério de "isto é defeito de concordância" já existe e é grande. Reimplementá-lo aqui criaria
 * uma segunda verdade que divergiria da primeira. Este harness chama {@code analisar} DUAS vezes na
 * mesma fala — como ela está, e com o advérbio removido de entre o verbo e a palavra seguinte — e
 * conta os motivos que só aparecem na segunda. A diferença é o que o advérbio escondeu.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O verbo vem de {@link LexicoGenero#VERBO_AUX} — CONSULTADO, nunca copiado.</li>
 *   <li>A lista de advérbios é do experimento, não da produção: é a variável sendo testada.</li>
 *   <li>Controle positivo e negativo antes de qualquer contagem.</li>
 *   <li>Número é PISO: só conta o que o detector saberia acusar se enxergasse.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem pasta de cache, PULA por {@link Assumptions} — NÃO VERIFICADO, nunca ausência de defeito.
 */
@DisplayName("medição: o que um advérbio entre verbo e particípio esconde do detector")
class MedicaoAdverbioEntreVerboEParticipioIT {

    private static final Path CACHE = Path.of("cache");

    /**
     * Advérbios e locuções que aparecem entre o verbo de ligação e o predicativo em legenda. Não
     * pretende ser exaustiva: é a amostra que define o PISO da medição.
     */
    private static final String ADVERBIOS =
        "muito|bem|tão|tao|mais|menos|meio|super|realmente|mesmo|sempre|ainda|já|ja|"
            + "completamente|totalmente|absolutamente|extremamente|bastante|demais|tudo|todo|toda";

    /** {@code (verbo) (advérbio) } → {@code (verbo) } ; o verbo sai do léxico de produção. */
    private static final Pattern VERBO_MAIS_ADVERBIO = Pattern.compile(
        "\\b(" + LexicoGenero.VERBO_AUX + ")\\s+(?:" + ADVERBIOS + ")\\s+",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

    private record Achado(String arquivo, int indice, String en, String pt, List<String> motivos) {
    }

    private static String semAdverbio(String texto) {
        return VERBO_MAIS_ADVERBIO.matcher(texto).replaceAll("$1 ");
    }

    @Test
    @DisplayName("conta as falas cujo defeito só aparece sem o advérbio no meio")
    void quantoOAdverbioEsconde() throws Exception {
        Assumptions.assumeTrue(Files.isDirectory(CACHE), "pasta de cache ausente — NÃO VERIFICADO");

        DetectorConcordanciaService detector = new DetectorConcordanciaService();

        // CONTROLE POSITIVO: o mesmo defeito, com e sem o advérbio. O de cima tem de ser visto e o
        // de baixo NÃO — é essa diferença que a medição conta.
        assertFalse(motivos(detector, "She is tired.", "Ela está cansado.").isEmpty(),
            "instrumento cego: o defeito SEM advérbio tem de ser acusado");
        assertTrue(motivos(detector, "She is tired.", "Ela está muito cansado.").isEmpty(),
            "premissa caiu: o detector JÁ enxerga com o advérbio no meio, então não há o que medir");
        // CONTROLE NEGATIVO: fala CORRETA não pode ganhar motivo por causa da remoção.
        assertFalse(ganhaSemAdverbio(detector, "She is tired.", "Ela está muito cansada."),
            "alarme falso: fala correta acusada só porque o advérbio foi removido");

        ObjectMapper mapper = new ObjectMapper();
        List<Achado> achados = new ArrayList<>();
        Map<String, Integer> porMotivo = new TreeMap<>();
        int falas = 0;
        int comAdverbio = 0;

        List<Path> arquivos;
        try (var s = Files.walk(CACHE)) {
            arquivos = s.filter(p -> p.toString().endsWith(".cache.json")).sorted().toList();
        }

        for (Path arquivo : arquivos) {
            CacheDocumento doc;
            try {
                doc = mapper.readValue(arquivo.toFile(), CacheDocumento.class);
            } catch (Exception e) {
                continue;
            }
            if (doc.entradas() == null) {
                continue;
            }
            for (var entrada : doc.entradas()) {
                String pt = entrada.traduzido();
                String en = entrada.original();
                if (pt == null || pt.isBlank()) {
                    continue;
                }
                falas++;
                String limpo = semAdverbio(pt);
                if (limpo.equals(pt)) {
                    continue;
                }
                comAdverbio++;
                List<String> antes = motivos(detector, en, pt);
                List<String> novos = new ArrayList<>(motivos(detector, en, limpo));
                novos.removeAll(Set.copyOf(antes));
                if (!novos.isEmpty()) {
                    achados.add(new Achado(arquivo.getFileName().toString(), entrada.indice(), en, pt, novos));
                    novos.forEach(m -> porMotivo.merge(m, 1, Integer::sum));
                }
            }
        }

        System.out.printf("%n=== ADVÉRBIO ENTRE VERBO E PARTICÍPIO — detector de produção no cache ===%n");
        System.out.printf("falas traduzidas               : %d%n", falas);
        System.out.printf("com verbo + advérbio           : %d%n", comAdverbio);
        System.out.printf("DEFEITOS QUE O ADVÉRBIO ESCONDE: %d%n", achados.size());
        if (!porMotivo.isEmpty()) {
            System.out.printf("%nPor motivo:%n");
            porMotivo.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> System.out.printf("   %4d  %s%n", e.getValue(), e.getKey()));
            System.out.printf("%nAmostra (até 15):%n");
            achados.stream().limit(15).forEach(a -> System.out.printf(
                "   %s #%d%n      EN: %s%n      PT: %s%n      -> %s%n",
                a.arquivo(), a.indice(), a.en(), a.pt(), String.join(" | ", a.motivos())));
        }
    }

    private static List<String> motivos(DetectorConcordanciaService detector, String en, String pt) {
        ResultadoDeteccaoConcordancia r = detector.analisar(en == null ? "" : en, pt);
        return r.motivos() == null ? List.of() : List.copyOf(r.motivos());
    }

    private static boolean ganhaSemAdverbio(
            DetectorConcordanciaService detector, String en, String pt) {
        List<String> novos = new ArrayList<>(motivos(detector, en, semAdverbio(pt)));
        novos.removeAll(Set.copyOf(motivos(detector, en, pt)));
        return !novos.isEmpty();
    }
}
