package org.traducao.projeto.medicao;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.contexto.domain.SnapshotContexto;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;
import org.traducao.projeto.llm.domain.LlmPort;
import org.traducao.projeto.llm.domain.Lote;
import org.traducao.projeto.llm.domain.StatusLlm;
import org.traducao.projeto.llm.domain.TraducaoLote;
import org.traducao.projeto.medicao.LeitorAcervoCache.Acervo;
import org.traducao.projeto.medicao.LeitorAcervoCache.FalaDoAcervo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: comparar o modelo CARREGADO AGORA no LM Studio contra as traduções que
 * já estão no cache, sobre exatamente as mesmas falas, <b>sem retraduzir o acervo</b>.
 *
 * <h2>Por que assim, e não trocando o modelo e mandando traduzir</h2>
 * {@code modeloLlm} é um dos seis campos de {@code ProvenienciaCache.mesmaProveniencia()}. Trocar
 * o LLM e rodar o pipeline faz o acervo inteiro — 60.891 falas — deixar de ser reaproveitado: o
 * {@code CacheTraducaoService} arquiva o anterior em {@code backups/} e retraduz do zero. Para
 * DECIDIR entre dois modelos isso é caro e desnecessário; o cache já guarda o que o modelo antigo
 * respondeu, e o original de cada fala.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li><b>READ-ONLY.</b> Nada aqui escreve em {@code cache/}, e nenhuma legenda é gerada.</li>
 *   <li>Usa o CAMINHO DE PRODUÇÃO: {@link LlmPort#traduzir(Lote, Double, String)} com o prompt
 *       do contexto real. Remontar prompt à mão mediria outra coisa — e o {@code contextoHash}
 *       do cache é justamente o SHA-256 desse prompt.</li>
 *   <li>O contexto vem do {@code contextoId} CARIMBADO na proveniência de cada arquivo, via
 *       {@link GerenciadorContexto#snapshotPorId(String)}, que não mexe no contexto ativo global.
 *       Comparar com a lore errada mediria a lore, não o modelo.</li>
 *   <li>O modelo é o que RESPONDEU ({@link LlmPort#modeloAtivo()}), nunca o configurado —
 *       {@code model: "current"} é proposital no {@code application.yml}.</li>
 *   <li>Recusa comparar se o modelo carregado for o MESMO que gravou o cache: seria medir ruído
 *       de amostragem, não diferença entre modelos.</li>
 *   <li>O limite de falas é IMPRESSO. Amostra truncada em silêncio tem a mesma aparência de
 *       cobertura total.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * LM Studio fora do ar, acervo ausente ou obra sem falas terminam com aviso visível e sem
 * exceção. Lote que falhar é contado e nomeado, não descartado em silêncio.
 *
 * <p>Uso (aspas obrigatórias no PowerShell, senão o {@code -D} é partido no ponto):
 * <pre>
 * gradlew test --tests "*ComparacaoModeloLlmIT*" "-Dkronos.comparacao=true" ^
 *   "-Dkronos.comparacao.obra=Mobile Suit Gundam ZZ" "-Dkronos.comparacao.limite=60"
 * </pre>
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.comparacao", matches = "true")
class ComparacaoModeloLlmIT {

    private static final String CHAVE_OBRA = "kronos.comparacao.obra";
    private static final String CHAVE_LIMITE = "kronos.comparacao.limite";
    private static final int LIMITE_PADRAO = 40;
    private static final int TAMANHO_LOTE = 10;

    @Inject
    LlmPort llm;

    @Inject
    GerenciadorContexto contextos;

    @Test
    @DisplayName("compara o modelo carregado contra o que gravou o cache, nas MESMAS falas")
    void comparar() throws IOException {
        StatusLlm status = llm.verificarDisponibilidade();
        String modeloAgora = llm.modeloAtivo();
        System.out.println("LLM: " + status + " | modelo carregado: " + modeloAgora);
        if (modeloAgora == null) {
            System.out.println("SEM MODELO resolvido — LM Studio no ar? Nada comparado.");
            return;
        }

        Acervo acervo = LeitorAcervoCache.ler(LeitorAcervoCache.raizPadrao());
        if (acervo.vazio()) {
            System.out.println("SEM ACERVO — nada comparado.");
            return;
        }

        String obra = System.getProperty(CHAVE_OBRA);
        int limite = Integer.getInteger(CHAVE_LIMITE, LIMITE_PADRAO);
        List<FalaDoAcervo> amostra = amostrar(acervo, obra, limite);
        if (amostra.isEmpty()) {
            System.out.println("NENHUMA fala elegivel"
                + (obra != null ? " para a obra \"" + obra + "\"" : "")
                + ". Obras disponiveis: " + obrasDisponiveis(acervo));
            return;
        }

        String modeloDoCache = amostra.get(0).proveniencia() != null
            ? amostra.get(0).proveniencia().modeloLlm() : null;
        if (modeloAgora.equals(modeloDoCache)) {
            System.out.println("MESMO MODELO do cache (" + modeloAgora + "). Carregue o outro "
                + "modelo no LM Studio: comparar um modelo consigo mesmo mede amostragem, "
                + "nao qualidade.");
            return;
        }

        System.out.printf("%ncomparando %d falas | cache: %s | agora: %s | contexto: %s%n"
                + "(limite %d — amostra TRUNCADA, nao e a obra inteira)%n%n",
            amostra.size(), modeloDoCache, modeloAgora,
            amostra.get(0).proveniencia() != null
                ? amostra.get(0).proveniencia().contextoId() : "(sem carimbo)",
            limite);

        executar(amostra, modeloDoCache, modeloAgora);
    }

    private void executar(List<FalaDoAcervo> amostra, String modeloDoCache, String modeloAgora) {
        int iguais = 0;
        int diferentes = 0;
        int vaziasAgora = 0;
        int lotesFalhos = 0;
        List<String> exemplos = new ArrayList<>();

        for (int inicio = 0; inicio < amostra.size(); inicio += TAMANHO_LOTE) {
            List<FalaDoAcervo> bloco =
                amostra.subList(inicio, Math.min(inicio + TAMANHO_LOTE, amostra.size()));
            SnapshotContexto contexto = contextoDe(bloco.get(0));
            if (contexto == null) {
                lotesFalhos++;
                System.out.println("  lote pulado: contexto nao resolvido para "
                    + bloco.get(0).arquivo().getFileName());
                continue;
            }
            Lote lote = new Lote(inicio / TAMANHO_LOTE,
                bloco.stream().map(FalaDoAcervo::original).toList());
            TraducaoLote resposta = llm.traduzir(lote, null, contexto.promptSistema());
            if (!resposta.sucesso() || resposta.linhasTraduzidas() == null
                || resposta.linhasTraduzidas().size() != bloco.size()) {
                lotesFalhos++;
                System.out.println("  lote " + lote.idLote() + " FALHOU: " + resposta.mensagemErro());
                continue;
            }
            for (int i = 0; i < bloco.size(); i++) {
                String doCache = bloco.get(i).traduzido().strip();
                String agora = resposta.linhasTraduzidas().get(i) == null
                    ? "" : resposta.linhasTraduzidas().get(i).strip();
                if (agora.isEmpty()) {
                    vaziasAgora++;
                } else if (agora.equals(doCache)) {
                    iguais++;
                } else {
                    diferentes++;
                    if (exemplos.size() < 12) {
                        exemplos.add(String.format("  EN    %s%n  cache %s%n  agora %s%n",
                            recortar(bloco.get(i).original()), recortar(doCache), recortar(agora)));
                    }
                }
            }
            System.out.printf("  lote %d/%d ok%n",
                lote.idLote() + 1, (amostra.size() + TAMANHO_LOTE - 1) / TAMANHO_LOTE);
        }

        int comparadas = iguais + diferentes + vaziasAgora;
        System.out.printf("%n=== %s  vs  %s ===%n", modeloDoCache, modeloAgora);
        System.out.printf("  falas comparadas.... %d%n", comparadas);
        System.out.printf("  identicas........... %d (%.1f%%)%n",
            iguais, 100.0 * iguais / Math.max(comparadas, 1));
        System.out.printf("  divergentes......... %d (%.1f%%)%n",
            diferentes, 100.0 * diferentes / Math.max(comparadas, 1));
        System.out.printf("  VAZIAS agora........ %d%n", vaziasAgora);
        System.out.printf("  lotes falhos........ %d%n%n", lotesFalhos);
        System.out.println("Divergencia NAO e defeito: e o que voce tem de LER. "
            + "Quem julga qualidade e voce, nao a contagem.");
        System.out.println();
        exemplos.forEach(System.out::println);
    }

    /** Contexto carimbado NA PROVENIÊNCIA do arquivo, não o ativo global. */
    private SnapshotContexto contextoDe(FalaDoAcervo fala) {
        if (fala.proveniencia() == null || fala.proveniencia().contextoId() == null) {
            return null;
        }
        try {
            return contextos.snapshotPorId(fala.proveniencia().contextoId());
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Falas de diálogo com original e tradução — descarta cartão, karaokê e entrada vazia. */
    private static List<FalaDoAcervo> amostrar(Acervo acervo, String obra, int limite) {
        return acervo.falas().stream()
            .filter(f -> obra == null || f.obra().equalsIgnoreCase(obra))
            .filter(f -> f.proveniencia() != null)
            .filter(f -> !f.original().isBlank() && !f.traduzido().isBlank())
            .filter(f -> !f.original().contains("\\k") && !f.original().startsWith("{\\fade"))
            .filter(f -> f.original().replaceAll("\\{[^}]*\\}", "").strip().length() >= 12)
            .limit(limite)
            .toList();
    }

    private static String obrasDisponiveis(Acervo acervo) {
        Map<String, Integer> porObra = new LinkedHashMap<>();
        acervo.falas().forEach(f -> porObra.merge(f.obra(), 1, Integer::sum));
        return porObra.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(8)
            .map(e -> e.getKey() + " (" + e.getValue() + ")")
            .reduce((a, b) -> a + ", " + b).orElse("(nenhuma)");
    }

    private static String recortar(String t) {
        return t.length() <= 100 ? t : t.substring(0, 100) + "…";
    }
}
