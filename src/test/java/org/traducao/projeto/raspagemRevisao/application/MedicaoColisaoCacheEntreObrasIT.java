package org.traducao.projeto.raspagemRevisao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.raspagemRevisao.application.ResolvedorArtefatosRevisao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: responder, para o ACERVO INTEIRO e não só para o caso que estourou, a
 * pergunta de Paulo em 08/08/2026 — <i>"o problema foi corrigido para todos os animes?"</i>.
 *
 * <h2>O prejuízo que originou</h2>
 * O Gundam 0083 carregou o cache do Gundam ZZ porque ambos têm um episódio {@code S01E02}, e o
 * critério de casamento olhava só episódio + {@code _ENG}. A correção exige agora afinidade de
 * OBRA — mas a afinidade é decidida por tokens do nome, e nome de arquivo de anime é território
 * hostil: colchetes de fansub, ponto no lugar de espaço, abreviação, numeral romano. Uma
 * heurística que funciona para Gundam pode falhar para DanMachi ou Macross.
 *
 * <p>Por isso a verificação NÃO é uma opinião sobre a heurística: é o produto cartesiano real de
 * <b>toda legenda traduzida × todo cache do acervo</b>, perguntado ao objeto de PRODUÇÃO
 * ({@link ResolvedorArtefatosRevisao#correspondeCache}). Reimplementar o critério aqui repetiria
 * o erro que este projeto já pagou caro.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>READ-ONLY: nada é escrito no acervo nem no cache; a saída vai para {@code build/}.</li>
 *   <li>A obra de cada arquivo vem da PASTA (dois níveis acima da legenda), que é como o resto do
 *       KRONOS identifica obra — não de heurística nova inventada aqui.</li>
 *   <li>Travado por {@code -Dkronos.medicao=true}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Acervo ou cache inacessível faz FALHAR com o caminho — zero colisões por não ter olhado nada
 * seria lido como "está tudo certo", que é o pior falso-verde possível (regra 8).
 */
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoColisaoCacheEntreObrasIT {

    private static final Path ACERVO = Path.of(System.getProperty("kronos.acervo", "C:\\animes"));
    private static final Pattern CODIGO_EPISODIO = Pattern.compile("(?i)(S\\d{1,2}E\\d{1,3})");

    /**
     * PROPÓSITO DE NEGÓCIO: cruza cada legenda traduzida com cada cache do acervo e conta quantos
     * pares de OBRAS DIFERENTES o critério de produção aceitaria.
     */
    @Test
    @DisplayName("nenhum cache de obra ALHEIA casa com legenda de outra obra, no acervo inteiro")
    void nenhumaColisaoEntreObras() throws IOException {
        Path raizCache = Path.of(System.getProperty("kronos.medicao.cache", "cache"));
        assertTrue(Files.isDirectory(ACERVO), "acervo inacessivel em " + ACERVO);
        assertTrue(Files.isDirectory(raizCache), "cache inacessivel em " + raizCache);

        // (obra -> nomes de cache dela), pela PASTA do cache; cache na raiz fica em "(raiz)".
        Map<String, List<String>> cachesPorPasta = new LinkedHashMap<>();
        try (Stream<Path> s = Files.walk(raizCache)) {
            for (Path p : s.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".cache.json")).toList()) {
                String pasta = p.getParent() == null ? "(raiz)" : p.getParent().getFileName().toString();
                cachesPorPasta.computeIfAbsent(pasta, k -> new ArrayList<>())
                    .add(p.getFileName().toString());
            }
        }

        List<String> colisoes = new ArrayList<>();
        int paresTestados = 0;
        int legendas = 0;

        // Alcance pelo DONO UNICO: honra -Dkronos.medicao.obra e declara NAO VERIFICADO
        // quando o filtro nao casa. O veto de "backup" continua sendo desta medicao.
        List<Path> legendasNoAlcance = new ArrayList<>();
        for (Path pastaPt : org.traducao.projeto.medicao.AlcanceDaMedicao.pastasDeTraducao()) {
            for (Path a : org.traducao.projeto.medicao.AlcanceDaMedicao.arquivosEntregues(pastaPt)) {
                if (!a.toString().contains("backup")) {
                    legendasNoAlcance.add(a);
                }
            }
        }
        {
            for (Path legenda : legendasNoAlcance) {

                String obraLegenda = obraDe(legenda);
                String baseMidia = ResolvedorArtefatosRevisao.normalizarBaseLegenda(
                    nomeSemExtensao(legenda.getFileName().toString()));
                Matcher m = CODIGO_EPISODIO.matcher(baseMidia);
                if (!m.find()) {
                    continue;
                }
                String codigo = m.group(1).toUpperCase(Locale.ROOT);
                legendas++;

                for (Map.Entry<String, List<String>> e : cachesPorPasta.entrySet()) {
                    for (String nomeCache : e.getValue()) {
                        paresTestados++;
                        if (!ResolvedorArtefatosRevisao.correspondeCache(nomeCache, baseMidia, codigo)) {
                            continue;
                        }
                        // Casou. É da mesma obra? Cache na raiz nao declara obra pela pasta:
                        // ali o casamento por nome ja e o criterio, e nao ha o que comparar.
                        if (e.getKey().equals("(raiz)") || e.getKey().equals("karaoke")
                            || e.getKey().equals("metadata")) {
                            continue;
                        }
                        if (!mesmaObraPorPasta(obraLegenda, e.getKey())) {
                            colisoes.add(String.format("%s%n      legenda: %s (obra: %s)%n      cache  : %s/%s",
                                "COLISAO", legenda.getFileName(), obraLegenda, e.getKey(), nomeCache));
                        }
                    }
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== COLISAO DE CACHE ENTRE OBRAS (acervo inteiro) ===\n");
        sb.append("legendas traduzidas com codigo de episodio: ").append(legendas).append('\n');
        sb.append("pastas de cache: ").append(cachesPorPasta.size()).append('\n');
        sb.append("pares legenda x cache testados: ").append(paresTestados).append('\n');
        sb.append("COLISOES (cache de obra alheia aceito): ").append(colisoes.size()).append("\n\n");
        colisoes.stream().limit(50).forEach(c -> sb.append("  ").append(c).append('\n'));

        Path saida = Path.of("build", "medicao-colisao-cache-entre-obras.txt");
        Files.createDirectories(saida.getParent());
        Files.writeString(saida, sb.toString());
        System.out.println(sb);

        // Instrumento cego daria zero colisoes sem ter olhado nada.
        assertTrue(paresTestados > 0,
            "zero pares testados — o instrumento nao encontrou legenda ou cache, entao o zero de "
                + "colisoes NAO significa ausencia de colisao");
        assertTrue(colisoes.isEmpty(),
            () -> colisoes.size() + " colisao(oes) de cache entre obras DIFERENTES:\n" + sb);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CALIBRAÇÃO (regra 9). Se o instrumento não reprovar o caso doente
     * conhecido, o "zero colisões" acima não vale nada.
     */
    @Test
    @DisplayName("calibracao: o par 0083 x ZZ que estourou em producao seria detectado")
    void calibracaoComOCasoReal() {
        // Antes da correcao, ESTE par casava — foi o incidente de 08/08/2026.
        assertFalse(ResolvedorArtefatosRevisao.correspondeCache(
                "Mobile.Suit.Gundam.ZZ.S01E02_ENG.cache.json",
                "Mobile Suit Gundam 0083 - Stardust Memory - S01E02_5", "S01E02"),
            "o caso real do incidente tem de ser recusado");
        // E o instrumento precisa saber ACEITAR, senao ele so diz 'nao' e o zero e vazio.
        assertTrue(ResolvedorArtefatosRevisao.correspondeCache(
                "Mobile.Suit.Gundam.ZZ.S01E02_ENG.cache.json",
                "Mobile Suit Gundam ZZ - S01E02", "S01E02"),
            "cache da PROPRIA obra tem de casar — senao o instrumento esta cego, nao correto");
    }

    private static String obraDe(Path legenda) {
        Path p = legenda.getParent();
        return p != null && p.getParent() != null
            ? p.getParent().getFileName().toString() : "(desconhecida)";
    }

    private static String nomeSemExtensao(String nome) {
        int i = nome.lastIndexOf('.');
        return i > 0 ? nome.substring(0, i) : nome;
    }

    /**
     * Duas pastas falam da mesma obra? Compara pelo mesmo critério de tokens que a produção usa,
     * consultando {@link ResolvedorArtefatosRevisao#mesmaObra} — não uma segunda regra local.
     */
    private static boolean mesmaObraPorPasta(String obraLegenda, String pastaCache) {
        return ResolvedorArtefatosRevisao.mesmaObra(pastaCache, obraLegenda);
    }
}
