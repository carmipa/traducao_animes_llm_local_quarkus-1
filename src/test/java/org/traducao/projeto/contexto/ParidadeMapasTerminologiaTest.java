package org.traducao.projeto.contexto;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.revisaoLore.application.GerenciadorPromptRevisaoLore;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: acusa quando os DOIS catálogos de terminologia — o da tradução
 * ({@code contexto.lore}) e o espelho da revisão de lore ({@code revisaoLore.contexto}) —
 * discordam sobre a mesma obra.
 *
 * <h2>Por que este teste existe</h2>
 * A duplicação entre os catálogos é DELIBERADA: {@code revisaoLore} é uma fatia e o contrato
 * proíbe fatia importar fatia, então o espelho é o preço declarado de manter a independência —
 * "duplicação consciente > acoplamento". O problema é que ninguém guardava o "consciente".
 *
 * <p>Medido em 2026-07-27: das 42 obras presentes nos dois catálogos, apenas 11 tinham o mapa
 * idêntico. E a deriva era nos DOIS sentidos — as decisões do dono do acervo sobre "terno",
 * Beam Saber e Normal Suit (23 chaves) existiam só na tradução, enquanto toda a riqueza de
 * DanMachi e Macross (até 18 chaves por obra) existia só na revisão. Cada lado corrigia o que o
 * outro deixava passar, e ninguém sabia.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li><b>Conflito de VALOR é proibido, sempre.</b> A mesma forma-ruim não pode apontar para
 *       canônicos diferentes nos dois catálogos: isso não é deriva, é contradição — a tradução
 *       produziria um termo que a revisão desfaria.</li>
 *   <li><b>Ausência é dívida DECLARADA.</b> Chave que existe só de um lado entra na lista
 *       {@link #DIVERGENCIAS_DECLARADAS} pelo id da obra. Obra nova divergindo reprova; obra que
 *       parar de divergir também reprova, para o progresso ser registrado.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem nomeia a obra e as chaves de cada lado, para o conserto ser copiar e colar.
 */
@QuarkusTest
@DisplayName("Paridade entre o catálogo de terminologia da tradução e o espelho da revisão")
class ParidadeMapasTerminologiaTest {

    /**
     * Obras cujos mapas AINDA divergem por ausência, medidas em 2026-07-27. Os núcleos das três
     * franquias já foram unificados; o que resta são EXTRAS por obra/temporada, que serão
     * reconciliados um a um. Cada id aqui é dívida nomeada, não aprovação.
     */
    private static final Set<String> DIVERGENCIAS_DECLARADAS = Set.of(
        "danmachi", "danmachi_movie", "danmachi_s1", "danmachi_s2", "danmachi_s3",
        "danmachi_s4", "danmachi_s5", "danmachi_so",
        "eight_six",
        "gundam_cca",
        "macross_7", "macross_7_encore", "macross_7_filme", "macross_7_filmes",
        "macross_dynamite_7", "macross_frontier", "macross_frontier_filme1",
        "macross_frontier_filme2");

    @Inject
    List<ProvedorContexto> catalogoTraducao;

    @Inject
    GerenciadorPromptRevisaoLore catalogoRevisao;

    /**
     * PROPÓSITO DE NEGÓCIO: o invariante duro — nenhuma forma-ruim pode ter destinos diferentes
     * nos dois catálogos. Deriva por ausência é tolerável e declarada; CONTRADIÇÃO não é, porque
     * significa que a tradução e a revisão discordam sobre qual é o termo oficial da obra.
     */
    @Test
    @DisplayName("nenhuma forma-ruim aponta para canônicos DIFERENTES nos dois catálogos")
    void semConflitoDeValor() {
        TreeSet<String> conflitos = new TreeSet<>();
        porObra().forEach((id, par) -> par.traducao().forEach((formaRuim, canonicoTraducao) -> {
            String canonicoRevisao = par.revisao().get(formaRuim);
            if (canonicoRevisao != null && !canonicoRevisao.equals(canonicoTraducao)) {
                conflitos.add(id + " | \"" + formaRuim + "\" -> tradução=\"" + canonicoTraducao
                    + "\" revisão=\"" + canonicoRevisao + "\"");
            }
        }));

        assertTrue(conflitos.isEmpty(),
            () -> "Os dois catálogos discordam sobre o termo oficial. Isto não é duplicação "
                + "consciente, é contradição: a tradução produz o que a revisão desfaz.\n  "
                + String.join("\n  ", conflitos));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: congela QUAIS obras ainda divergem por ausência, para que a lista só
     * encolha. Sem esta catraca, a próxima decisão de terminologia volta a entrar num lado só —
     * que é exatamente como as 23 chaves de Gundam UC ficaram órfãs.
     */
    @Test
    @DisplayName("a lista de obras que divergem por ausência só pode ENCOLHER")
    void divergenciaPorAusenciaCongelada() {
        TreeMap<String, String> detalhe = new TreeMap<>();
        TreeSet<String> divergentes = new TreeSet<>();
        porObra().forEach((id, par) -> {
            TreeSet<String> soTraducao = new TreeSet<>(par.traducao().keySet());
            soTraducao.removeAll(par.revisao().keySet());
            TreeSet<String> soRevisao = new TreeSet<>(par.revisao().keySet());
            soRevisao.removeAll(par.traducao().keySet());
            if (soTraducao.isEmpty() && soRevisao.isEmpty()) {
                return;
            }
            divergentes.add(id);
            detalhe.put(id, "só na tradução=" + soTraducao + " | só na revisão=" + soRevisao);
        });

        TreeSet<String> novas = new TreeSet<>(divergentes);
        novas.removeAll(DIVERGENCIAS_DECLARADAS);
        TreeSet<String> resolvidas = new TreeSet<>(DIVERGENCIAS_DECLARADAS);
        resolvidas.removeAll(divergentes);

        assertTrue(novas.isEmpty() && resolvidas.isEmpty(),
            () -> "Paridade dos catálogos de terminologia mudou.\n"
                + (novas.isEmpty() ? "" : "\nOBRAS QUE PASSARAM A DIVERGIR (regressão — a decisão "
                    + "entrou num catálogo só):\n  "
                    + novas.stream().map(id -> id + " :: " + detalhe.get(id)).reduce((a, b) -> a + "\n  ").orElse(""))
                + (resolvidas.isEmpty() ? "" : "\nOBRAS QUE PARARAM DE DIVERGIR (progresso — "
                    + "remova-as de DIVERGENCIAS_DECLARADAS):\n  " + String.join("\n  ", resolvidas)));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: cruza os dois catálogos pelo id da obra, que é a chave comum.
     * <p>INVARIANTES DO DOMÍNIO: só entram obras presentes NOS DOIS; obra que existe em um só
     * catálogo é outra questão (cobertura), não paridade.
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança; mapas vazios são válidos.
     */
    private Map<String, ParMapas> porObra() {
        Map<String, Map<String, String>> traducao = new TreeMap<>();
        catalogoTraducao.forEach(p -> traducao.put(p.getId(), p.correcoesTerminologia()));

        Map<String, ParMapas> pares = new TreeMap<>();
        catalogoRevisao.getProvedores().forEach(revisao -> {
            Map<String, String> daTraducao = traducao.get(revisao.getId());
            if (daTraducao != null) {
                pares.put(revisao.getId(), new ParMapas(daTraducao, revisao.correcoesTerminologia()));
            }
        });
        return pares;
    }

    private record ParMapas(Map<String, String> traducao, Map<String, String> revisao) {}
}
