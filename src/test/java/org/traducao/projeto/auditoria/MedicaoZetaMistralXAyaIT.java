package org.traducao.projeto.auditoria;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.cachetraducao.domain.CacheDocumento;
import org.traducao.projeto.raspagemRevisao.application.DetectorConcordanciaService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: repete no Zeta a comparação mistral × aya que só existia no Unicorn. Duas
 * obras medidas com o MESMO critério é o que separa "a aya é melhor" de "a aya foi melhor naquela
 * obra" — e o Zeta é maior (17.036 falas contra 5.424) e de outra época de tradução.
 *
 * <h2>Por que sobre o CACHE, e não sobre os .ass</h2>
 * A retradução de 13/08 gravou a versão da aya por cima de {@code traducao_ptbr}, então os dois
 * artefatos não coexistem em disco. O cache resolve melhor do que os arquivos resolveriam: ele
 * pareia inglês e português na MESMA entrada, dispensando o casamento por índice — que já produziu
 * 77,7% onde o real era 0,5% quando as contagens divergiam.
 *
 * <p>A geração do mistral vem de {@code backups/cache-zeta-mistral-20260812}, congelada antes da
 * retradução justamente para esta comparação existir.
 *
 * <h2>Os critérios são os MESMOS, importados</h2>
 * {@code perdeuInterrogacao}, {@code perdeuNegacao} e {@code temFalta} são chamados das classes que
 * os definiram para o Unicorn, não recriados aqui. Uma segunda implementação divergiria da
 * primeira, e os números das duas obras deixariam de ser comparáveis — que é o único motivo de
 * medir a segunda.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só entram falas presentes nas DUAS gerações, casadas por arquivo + original em inglês.
 *       Comparar conjuntos diferentes de falas mediria o pareamento, não os modelos.</li>
 *   <li>Controle positivo e negativo antes de qualquer contagem.</li>
 *   <li>Números são PISO: cada eixo só enxerga o que seu critério alcança.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem o cache atual ou sem o backup, PULA por {@link Assumptions} — NÃO VERIFICADO, nunca "os
 * modelos empataram".
 */
@DisplayName("medição: Zeta mistral × aya, os mesmos eixos do Unicorn")
class MedicaoZetaMistralXAyaIT {

    private static final Path CACHE_ATUAL = Path.of("cache", "Mobile Suit Zeta Gundam");
    private static final Path CACHE_MISTRAL = Path.of("backups", "cache-zeta-mistral-20260812");

    private record Par(String arquivo, String en, String mistral, String aya) {
    }

    /** Lê uma pasta de cache e devolve arquivo|original -> traduzido, ignorando as gerações. */
    private static Map<String, String> lerCache(Path pasta, ObjectMapper mapper) throws Exception {
        Map<String, String> mapa = new LinkedHashMap<>();
        try (var s = Files.walk(pasta)) {
            for (Path p : s.filter(x -> x.toString().endsWith(".cache.json"))
                    .filter(x -> !x.getFileName().toString().contains(".geracao_")).toList()) {
                CacheDocumento doc;
                try {
                    doc = mapper.readValue(p.toFile(), CacheDocumento.class);
                } catch (Exception e) {
                    continue;
                }
                if (doc.entradas() == null) {
                    continue;
                }
                String nome = p.getFileName().toString();
                for (var e : doc.entradas()) {
                    if (e.original() != null && e.traduzido() != null) {
                        mapa.putIfAbsent(nome + "|" + e.original(), e.traduzido());
                    }
                }
            }
        }
        return mapa;
    }

    @Test
    @DisplayName("os cinco eixos, lado a lado, sobre as falas que as duas gerações têm")
    void compararOsDoisModelosNoZeta() throws Exception {
        Assumptions.assumeTrue(Files.isDirectory(CACHE_ATUAL) && Files.isDirectory(CACHE_MISTRAL),
            "cache do Zeta ou backup do mistral ausente — NÃO VERIFICADO");

        // CONTROLES, com os critérios importados. Se algum deixar de enxergar o caso doente ou
        // passar a acusar o são, os números abaixo não valem nada.
        assertTrue(MedicaoPerguntaQueViraAfirmacaoIT.perdeuInterrogacao("Kamille?", "Sim, Kamille."),
            "instrumento cego: pergunta que virou afirmação");
        assertFalse(MedicaoPerguntaQueViraAfirmacaoIT.perdeuInterrogacao("Kamille?", "Kamille?"),
            "alarme falso: a interrogação foi preservada");
        assertTrue(MedicaoNegacaoPerdidaIT.perdeuNegacao("I can't do it.", "Eu consigo fazer."),
            "instrumento cego: negação perdida");
        assertFalse(MedicaoNegacaoPerdidaIT.perdeuNegacao("I can't do it.", "Eu não consigo fazer."),
            "alarme falso: a negação foi preservada");
        assertTrue(MedicaoAcentuacaoFaltanteIT.temFalta("a familia esta aqui"),
            "instrumento cego: acentuação faltando");
        assertFalse(MedicaoAcentuacaoFaltanteIT.temFalta("a família está aqui"),
            "alarme falso: texto acentuado corretamente");

        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> aya = lerCache(CACHE_ATUAL, mapper);
        Map<String, String> mistral = lerCache(CACHE_MISTRAL, mapper);

        List<Par> pares = new ArrayList<>();
        for (var e : mistral.entrySet()) {
            String traducaoAya = aya.get(e.getKey());
            if (traducaoAya == null) {
                continue;
            }
            String en = e.getKey().substring(e.getKey().indexOf('|') + 1);
            pares.add(new Par(e.getKey().substring(0, e.getKey().indexOf('|')),
                en, e.getValue(), traducaoAya));
        }

        System.out.printf("%n=== ZETA — mistral × aya, sobre o CACHE ===%n");
        System.out.printf("falas no cache do mistral : %d%n", mistral.size());
        System.out.printf("falas no cache da aya     : %d%n", aya.size());
        System.out.printf("PARES comparáveis         : %d%n%n", pares.size());
        Assumptions.assumeTrue(!pares.isEmpty(), "nenhuma fala casou entre as gerações");

        int pergM = 0, pergA = 0, negM = 0, negA = 0, acM = 0, acA = 0, ecoM = 0, ecoA = 0, difs = 0;
        DetectorConcordanciaService detector = new DetectorConcordanciaService();
        int concM = 0, concA = 0;
        List<Par> amostraDivergente = new ArrayList<>();

        for (Par p : pares) {
            if (MedicaoPerguntaQueViraAfirmacaoIT.perdeuInterrogacao(p.en(), p.mistral())) {
                pergM++;
            }
            if (MedicaoPerguntaQueViraAfirmacaoIT.perdeuInterrogacao(p.en(), p.aya())) {
                pergA++;
            }
            if (MedicaoNegacaoPerdidaIT.perdeuNegacao(p.en(), p.mistral())) {
                negM++;
            }
            if (MedicaoNegacaoPerdidaIT.perdeuNegacao(p.en(), p.aya())) {
                negA++;
            }
            String vm = MedicaoAcentuacaoFaltanteIT.visivel(p.mistral());
            String va = MedicaoAcentuacaoFaltanteIT.visivel(p.aya());
            if (MedicaoAcentuacaoFaltanteIT.temFalta(vm)) {
                acM++;
            }
            if (MedicaoAcentuacaoFaltanteIT.temFalta(va)) {
                acA++;
            }
            String ven = MedicaoAcentuacaoFaltanteIT.visivel(p.en());
            if (!ven.isEmpty() && ven.equalsIgnoreCase(vm)) {
                ecoM++;
            }
            if (!ven.isEmpty() && ven.equalsIgnoreCase(va)) {
                ecoA++;
            }
            if (!detector.analisar(p.en(), p.mistral()).motivos().isEmpty()) {
                concM++;
            }
            if (!detector.analisar(p.en(), p.aya()).motivos().isEmpty()) {
                concA++;
            }
            if (!vm.equals(va)) {
                difs++;
                if (amostraDivergente.size() < 12 && !vm.isEmpty() && !va.isEmpty()) {
                    amostraDivergente.add(p);
                }
            }
        }

        System.out.printf("%-28s %10s %10s%n", "eixo", "mistral", "aya");
        System.out.printf("%-28s %10s %10s%n", "----", "-------", "---");
        linha("pergunta -> afirmação", pergM, pergA, pares.size());
        linha("negação perdida", negM, negA, pares.size());
        linha("acentuação faltando", acM, acA, pares.size());
        linha("eco (PT igual ao EN)", ecoM, ecoA, pares.size());
        linha("concordância (detector)", concM, concA, pares.size());
        System.out.printf("%n%-28s %10d  (%.1f%% das falas)%n", "traduções DIFERENTES", difs,
            100.0 * difs / pares.size());

        // O eixo em que a aya perde é o ÚNICO corrigível por máquina. A pergunta que importa não é
        // "quantos faltam", é "quantos o normalizador de PRODUÇÃO já resolveria" — porque o que ele
        // alcança deixa de ser diferença entre os modelos.
        var normalizador = new org.traducao.projeto.qualidadeTraducao.application
            .NormalizadorAcentosComuns();
        int corrigiveis = 0;
        int restantes = 0;
        Map<String, Integer> palavrasQueEscapam = new java.util.TreeMap<>();
        for (Par p : pares) {
            String va = MedicaoAcentuacaoFaltanteIT.visivel(p.aya());
            if (!MedicaoAcentuacaoFaltanteIT.temFalta(va)) {
                continue;
            }
            String depois = normalizador.normalizar(p.aya());
            if (!MedicaoAcentuacaoFaltanteIT.temFalta(MedicaoAcentuacaoFaltanteIT.visivel(depois))) {
                corrigiveis++;
            } else {
                restantes++;
                for (String palavra : MedicaoAcentuacaoFaltanteIT.visivel(depois).split("[^\\p{L}]+")) {
                    if (!palavra.isBlank() && MedicaoAcentuacaoFaltanteIT.temFalta(palavra)) {
                        palavrasQueEscapam.merge(palavra.toLowerCase(), 1, Integer::sum);
                    }
                }
            }
        }
        System.out.printf("%n--- o eixo da aya passado pelo NormalizadorAcentosComuns de produção ---%n");
        System.out.printf("falas com falta de acento : %d%n", acA);
        System.out.printf("o normalizador RESOLVE    : %d%n", corrigiveis);
        System.out.printf("continuam faltando        : %d%n", restantes);
        if (!palavrasQueEscapam.isEmpty()) {
            System.out.printf("%npalavras que escapam (top 15) — candidatas ao dicionário:%n");
            palavrasQueEscapam.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(15)
                .forEach(e -> System.out.printf("   %4d  %s%n", e.getValue(), e.getKey()));
        }

        System.out.printf("%nAmostra de divergência (para leitura humana):%n");
        for (Par p : amostraDivergente) {
            System.out.printf("   EN : %s%n   MIS: %s%n   AYA: %s%n%n",
                MedicaoAcentuacaoFaltanteIT.visivel(p.en()),
                MedicaoAcentuacaoFaltanteIT.visivel(p.mistral()),
                MedicaoAcentuacaoFaltanteIT.visivel(p.aya()));
        }
    }

    private static void linha(String eixo, int m, int a, int total) {
        System.out.printf("%-28s %6d (%.2f%%) %6d (%.2f%%)%n", eixo,
            m, 100.0 * m / total, a, 100.0 * a / total);
    }
}
