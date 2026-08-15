package org.traducao.projeto.contexto;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.revisaoLore.application.GerenciadorPromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * PROPÓSITO DE NEGÓCIO: congela, entrada por entrada, o mapa de terminologia de lore que os
 * DOIS catálogos da mesma obra têm HOJE — o da Tradução Local ({@link ProvedorContexto}) e o da
 * Revisão de Lore ({@link ProvedorPromptRevisaoLore}) — para que a unificação num dono único
 * não possa perder nenhuma delas sem o build reprovar.
 *
 * <h2>Por que existe (FASE 0 do plano de fonte única de terminologia)</h2>
 * O manifesto que já protegia as lores ({@code /contexto/manifesto-lore.properties}, usado por
 * {@code ProtecaoConteudoLoreTest}) hasheia <b>id + nome + prompt + termos protegidos</b> — e
 * <b>não</b> cobre {@code correcoesTerminologia()}. É justamente o mapa que a unificação vai
 * mexer: hoje são 68 obras nos dois catálogos, 17 divergentes, 18 entradas que só a tradução
 * conhece e 69 que só a revisão conhece, medidas por
 * {@link MedicaoDivergenciaEntreCatalogosDeLoreIT} em 2026-08-15. Sem este congelamento,
 * "ninguém perdeu entrada" seria afirmação, não artefato.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li><b>Catraca de um lado só.</b> O baseline é SUBCONJUNTO obrigatório do estado vivo:
 *       acrescentar entrada é livre (é o que a união faz), perder ou reescrever o canônico de
 *       uma entrada existente reprova. Por isso a unificação não precisa reescrever este
 *       arquivo — ele continua valendo depois dela, que é o teste de que ela não perdeu nada.</li>
 *   <li><b>Total declarado no próprio arquivo.</b> A diretiva {@code #!total=} é conferida
 *       contra as linhas realmente lidas: um baseline truncado teria MENOS o que exigir e
 *       passaria — aprovação por cegueira, que é o modo de falha que esta guarda existe para
 *       não ter.</li>
 *   <li><b>Linha malformada é DEFEITO, nunca linha ignorada.</b></li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Três estados, e o terceiro não é aprovação: baseline ausente/vazio ou catálogo CDI vazio
 * reprovam com {@code NÃO VERIFICADO} — a guarda não pôde medir, e isso não vale por "passou".
 * Entrada perdida ou canônico alterado reprovam nomeando obra, lado, forma-ruim e os dois
 * valores. O snapshot vivo é sempre regravado em {@code build/tmp/} (árvore descartável) para
 * que regenerar o baseline seja copiar um arquivo, nunca digitar entrada à mão.
 */
@QuarkusTest
@DisplayName("FASE 0: baseline do mapa de terminologia de lore (nenhuma entrada pode desaparecer)")
class BaselineTerminologiaLoreIT {

    private static final String BASELINE = "/contexto/baseline-terminologia-lore.tsv";
    private static final String DIRETIVA_TOTAL = "#!total=";
    private static final String LADO_TRADUCAO = "TRADUCAO";
    private static final String LADO_REVISAO = "REVISAO";

    /** Onde o snapshot vivo é gravado a cada execução: árvore descartável, fora do versionamento. */
    private static final Path SNAPSHOT_VIVO =
        Path.of("build", "tmp", "baseline-terminologia-lore.gerado.tsv");

    @Inject
    List<ProvedorContexto> catalogoTraducao;

    @Inject
    GerenciadorPromptRevisaoLore catalogoRevisao;

    /**
     * PROPÓSITO DE NEGÓCIO: o invariante da FASE 0 — toda entrada que existia antes da
     * unificação continua existindo, com o MESMO canônico, no lado em que existia.
     *
     * <p>INVARIANTES DO DOMÍNIO: comparação por (obra, lado, forma-ruim); o valor tem de bater
     * byte a byte. Entrada nova no estado vivo é ignorada de propósito — a união acrescenta.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista cada perda com obra, lado e forma-ruim; baseline
     * ou catálogo indisponível reprova como {@code NÃO VERIFICADO}.
     */
    @Test
    @DisplayName("nenhuma entrada do baseline desapareceu nem trocou de canônico")
    void nenhumaEntradaDoBaselineDesapareceu() throws IOException {
        Map<String, Map<String, String>> vivoTraducao = mapaVivoDaTraducao();
        Map<String, Map<String, String>> vivoRevisao = mapaVivoDaRevisao();
        gravarSnapshotVivo(vivoTraducao, vivoRevisao);

        assertTrue(!vivoTraducao.isEmpty(),
            "NÃO VERIFICADO: nenhum ProvedorContexto no CDI — a guarda não pôde medir");
        assertTrue(!vivoRevisao.isEmpty(),
            "NÃO VERIFICADO: nenhum ProvedorPromptRevisaoLore no CDI — a guarda não pôde medir");

        List<Entrada> esperadas = lerBaseline();

        List<String> perdas = new ArrayList<>();
        for (Entrada e : esperadas) {
            Map<String, Map<String, String>> lado =
                LADO_TRADUCAO.equals(e.lado()) ? vivoTraducao : vivoRevisao;
            Map<String, String> mapa = lado.get(e.obra());
            if (mapa == null) {
                perdas.add(e.obra() + " | " + e.lado() + " | a OBRA sumiu deste catálogo");
                continue;
            }
            String vivo = mapa.get(e.formaRuim());
            if (vivo == null) {
                perdas.add(e.obra() + " | " + e.lado() + " | entrada PERDIDA: \""
                    + e.formaRuim() + "\" -> \"" + e.canonico() + "\"");
            } else if (!vivo.equals(e.canonico())) {
                perdas.add(e.obra() + " | " + e.lado() + " | canônico MUDOU para \""
                    + e.formaRuim() + "\": baseline=\"" + e.canonico() + "\" vivo=\"" + vivo + "\"");
            }
        }

        assertTrue(perdas.isEmpty(),
            () -> "A unificação da terminologia de lore PERDEU entrada que existia antes.\n"
                + "Baseline: " + BASELINE + " (" + esperadas.size() + " entradas)\n"
                + "Snapshot vivo regravado em: " + SNAPSHOT_VIVO + "\n  "
                + String.join("\n  ", perdas));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: lê o baseline versionado, que é a fotografia do estado de antes.
     *
     * <p>INVARIANTES DO DOMÍNIO: quatro campos separados por TAB por linha; {@code #} inicia
     * comentário; a diretiva {@code #!total=} declara quantas entradas o arquivo deve ter e é
     * conferida — truncar o arquivo tem de reprovar, não aliviar a exigência.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: recurso ausente, arquivo sem entradas, linha com número
     * de campos diferente de quatro, lado desconhecido ou total divergente reprovam o teste.
     */
    private List<Entrada> lerBaseline() throws IOException {
        List<Entrada> entradas = new ArrayList<>();
        Integer totalDeclarado = null;

        try (InputStream in = getClass().getResourceAsStream(BASELINE)) {
            if (in == null) {
                fail("NÃO VERIFICADO: baseline não encontrado em " + BASELINE
                    + ". Gere-o copiando " + SNAPSHOT_VIVO + " (produzido por esta própria execução).");
            }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String linha;
                int numero = 0;
                while ((linha = r.readLine()) != null) {
                    numero++;
                    if (linha.startsWith(DIRETIVA_TOTAL)) {
                        totalDeclarado = Integer.parseInt(linha.substring(DIRETIVA_TOTAL.length()).trim());
                        continue;
                    }
                    if (linha.isBlank() || linha.startsWith("#")) {
                        continue;
                    }
                    String[] campos = linha.split("\t", -1);
                    if (campos.length != 4) {
                        fail("Baseline malformado na linha " + numero + " (esperados 4 campos "
                            + "separados por TAB, vieram " + campos.length + "): " + linha);
                    }
                    if (!LADO_TRADUCAO.equals(campos[1]) && !LADO_REVISAO.equals(campos[1])) {
                        fail("Baseline malformado na linha " + numero + ": lado desconhecido \""
                            + campos[1] + "\" (esperado " + LADO_TRADUCAO + " ou " + LADO_REVISAO + ")");
                    }
                    entradas.add(new Entrada(campos[0], campos[1], campos[2], campos[3]));
                }
            }
        }

        if (entradas.isEmpty()) {
            fail("NÃO VERIFICADO: baseline " + BASELINE + " não tem nenhuma entrada — "
                + "arquivo vazio não é aprovação.");
        }
        if (totalDeclarado == null) {
            fail("Baseline sem a diretiva " + DIRETIVA_TOTAL + "N. Sem ela, um arquivo truncado "
                + "exigiria menos e passaria.");
        } else if (totalDeclarado != entradas.size()) {
            fail("Baseline " + BASELINE + " declara " + DIRETIVA_TOTAL + totalDeclarado
                + " mas tem " + entradas.size() + " entradas — arquivo truncado ou editado à mão.");
        }
        return entradas;
    }

    /** Mapa efetivo por obra do catálogo da TRADUÇÃO, lido dos provedores reais pelo CDI. */
    private Map<String, Map<String, String>> mapaVivoDaTraducao() {
        Map<String, Map<String, String>> por = new TreeMap<>();
        for (ProvedorContexto p : catalogoTraducao) {
            Map<String, String> m = p.correcoesTerminologia();
            por.put(p.getId(), m == null ? Map.of() : m);
        }
        return por;
    }

    /**
     * Mapa efetivo por obra do catálogo da REVISÃO, lido pelo agregador que a PRODUÇÃO usa
     * ({@link GerenciadorPromptRevisaoLore}) — e não por uma coleta própria, para que a guarda
     * meça o mesmo que o pipeline enxerga.
     */
    private Map<String, Map<String, String>> mapaVivoDaRevisao() {
        Map<String, Map<String, String>> por = new TreeMap<>();
        for (ProvedorPromptRevisaoLore p : catalogoRevisao.getProvedores()) {
            Map<String, String> m = p.correcoesTerminologia();
            por.put(p.getId(), m == null ? Map.of() : m);
        }
        return por;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: grava o estado vivo no formato exato do baseline, para que
     * regenerar seja copiar um arquivo — nunca digitar entrada à mão, que é como as duas
     * cópias divergiram para começo de conversa.
     *
     * <p>INVARIANTES DO DOMÍNIO: escreve em {@code build/}, árvore descartável e fora do
     * versionamento; ordenação determinística por (obra, lado, forma-ruim); TAB dentro de
     * chave ou valor aborta, porque quebraria o formato em silêncio.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: falha de escrita propaga {@link IOException} — snapshot
     * que não foi gravado não pode passar por gravado.
     */
    private void gravarSnapshotVivo(Map<String, Map<String, String>> traducao,
                                    Map<String, Map<String, String>> revisao) throws IOException {
        List<String> linhas = new ArrayList<>();
        List<String> corpo = new ArrayList<>();
        acumular(corpo, traducao, LADO_TRADUCAO);
        acumular(corpo, revisao, LADO_REVISAO);
        corpo.sort(String::compareTo);

        linhas.add("# BASELINE DA FASE 0 — mapa de terminologia de lore congelado nos DOIS catálogos.");
        linhas.add("# Formato: <obraId>\\t<TRADUCAO|REVISAO>\\t<forma-ruim>\\t<canônico>");
        linhas.add("# Gerado por BaselineTerminologiaLoreIT a partir dos provedores REAIS do CDI.");
        linhas.add("# É catraca de um lado só: acrescentar é livre, perder reprova.");
        linhas.add(DIRETIVA_TOTAL + corpo.size());
        linhas.addAll(corpo);

        Files.createDirectories(SNAPSHOT_VIVO.getParent());
        Files.write(SNAPSHOT_VIVO, linhas, StandardCharsets.UTF_8);
    }

    private void acumular(List<String> destino, Map<String, Map<String, String>> porObra, String lado) {
        porObra.forEach((obra, mapa) -> new TreeMap<>(mapa).forEach((formaRuim, canonico) -> {
            if (obra.indexOf('\t') >= 0 || formaRuim.indexOf('\t') >= 0 || canonico.indexOf('\t') >= 0) {
                fail("TAB dentro de obra/forma-ruim/canônico quebraria o formato do baseline: "
                    + obra + " | " + lado + " | \"" + formaRuim + "\" -> \"" + canonico + "\"");
            }
            destino.add(obra + "\t" + lado + "\t" + formaRuim + "\t" + canonico);
        }));
    }

    private record Entrada(String obra, String lado, String formaRuim, String canonico) {}
}
