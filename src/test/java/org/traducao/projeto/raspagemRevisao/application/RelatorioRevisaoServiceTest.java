package org.traducao.projeto.raspagemRevisao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.raspagemRevisao.domain.DetalheRevisao;
import org.traducao.projeto.raspagemRevisao.domain.ModoRevisaoLegendas;
import org.traducao.projeto.raspagemRevisao.domain.ports.TelemetriaRevisaoPort;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: trava o TEXTO do relatório de revisão. Ele é o único registro que sobrevive
 * à sessão — semanas depois, é por ele que se responde "por que esta fala mudou?".
 *
 * <h2>Por que os cabeçalhos aparecem literais aqui</h2>
 * A extração da FASE 4 substituiu DOIS blocos de texto quase idênticos (um por modo) por um só,
 * parametrizado. Isso é o tipo de simplificação que passa despercebida quando quebra: o relatório
 * continuaria sendo gerado, só que com um espaço a mais, uma linha fora de ordem ou o rótulo
 * errado, e ninguém notaria até precisar auditar. Os literais abaixo foram copiados do código
 * ANTERIOR à extração e existem para provar que a saída não mudou.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer diferença de texto reprova mostrando as duas versões.
 */
class RelatorioRevisaoServiceTest {

    /** Telemetria em memória: guarda o que foi publicado, sem disco nem fatia de telemetria. */
    private static final class TelemetriaEspia implements TelemetriaRevisaoPort {
        private final List<String> relatorios = new ArrayList<>();
        private String operacao;
        private String prefixo;

        @Override
        public void registrarComRelatorio(String operacao, String detalhe, String prefixoRelatorio,
                                          Path pastaAlvo, long duracaoMs, int arquivosProcessados,
                                          int itensDetectados, int itensCorrigidos, String relatorio) {
            this.operacao = operacao;
            this.prefixo = prefixoRelatorio;
            relatorios.add(relatorio);
        }

        @Override
        public void registrar(String operacao, String detalhe, long duracaoMs,
                              int arquivosProcessados, int itensDetectados, int itensCorrigidos) {
        }

        @Override
        public Path pastaDeRelatorios(Path pastaEntrada) {
            return pastaEntrada.resolve("relatorios");
        }
    }

    private final TelemetriaEspia telemetria = new TelemetriaEspia();
    private final RelatorioRevisaoService servico = new RelatorioRevisaoService(telemetria);

    @Test
    @DisplayName("Modo Google: cabeçalho, rótulo e ordem das linhas idênticos aos de antes da extração")
    void relatorioDoModoGoogle() {
        servico.registrar(Path.of("C:", "animes", "pt"), 90_000,
            3, 12, 7, 400, 5, 5, ModoRevisaoLegendas.GOOGLE, List.of());

        String r = telemetria.relatorios.get(0);
        assertTrue(r.startsWith("""
            REVISÃO DE LEGENDAS (.ass)
            ==========================
            Pasta: """), "cabeçalho e sublinhado do modo Google:\n" + r);
        assertTrue(r.contains("Duração: 1min 30s\n"));
        assertTrue(r.contains("Arquivos analisados: 3\n"));
        assertTrue(r.contains("Falas auditadas: 400\n"));
        assertTrue(r.contains("Falas sem original EN (ignoradas): 5\n"));
        assertTrue(r.contains("Problemas detectados: 12\n"));
        assertTrue(r.contains("Falas corrigidas via Google: 7\n"));
        assertTrue(r.contains("Falas pendentes: 5\n"));
        assertFalse(r.contains("via LLM"), "o rótulo do outro modo não pode vazar");
        assertEquals("Revisão Legendas (.ass Google)", telemetria.operacao);
        assertEquals("revisao_legendas", telemetria.prefixo);

        // `contains` prova que cada linha existe, não que estão na ORDEM certa nem que não sobrou
        // linha nenhuma. A sequência abaixo prova as duas coisas. A linha "Pasta:" é substituída
        // porque seu valor absoluto depende da máquina que roda o teste, e não é o que se afirma.
        List<String> linhas = new ArrayList<>(List.of(r.split("\n", -1)));
        linhas.replaceAll(l -> l.startsWith("Pasta: ") ? "Pasta: <caminho>" : l);
        assertEquals(List.of(
            "REVISÃO DE LEGENDAS (.ass)",
            "==========================",
            "Pasta: <caminho>",
            "Duração: 1min 30s",
            "Arquivos analisados: 3",
            "Falas auditadas: 400",
            "Falas sem original EN (ignoradas): 5",
            "Problemas detectados: 12",
            "Falas corrigidas via Google: 7",
            "Falas pendentes: 5",
            "",
            "DETALHES POR OCORRÊNCIA",
            "=======================",
            "Nenhuma ocorrência detalhada registrada.",
            ""), linhas,
            "o relatório inteiro, linha a linha, como era antes da extração");
    }

    @Test
    @DisplayName("Modo LLM: cabeçalho e rótulo próprios — os dois modos não se confundem no histórico")
    void relatorioDoModoLlm() {
        servico.registrar(Path.of("C:", "animes", "pt"), 5_000,
            1, 2, 2, 10, 0, 0, ModoRevisaoLegendas.LLM_CONCORDANCIA, List.of());

        String r = telemetria.relatorios.get(0);
        assertTrue(r.startsWith("""
            REVISÃO DE CONCORDÂNCIA PT-BR (.ass via LLM)
            ============================================
            Pasta: """), "cabeçalho e sublinhado do modo LLM:\n" + r);
        assertTrue(r.contains("Falas corrigidas via LLM: 2\n"));
        assertFalse(r.contains("via Google"), "o rótulo do outro modo não pode vazar");
        assertEquals("Revisão Concordância (.ass LLM)", telemetria.operacao);
        assertEquals("revisao_concordancia_legendas", telemetria.prefixo,
            "prefixo distinto é o que separa as duas revisões no histórico de relatórios");
    }

    @Test
    @DisplayName("Sem ocorrências, a seção de detalhes aparece dizendo isso — não some")
    void semDetalhesAsecaoContinuaVisivel() {
        String vazio = RelatorioRevisaoService.formatarDetalhes(List.of());
        String nulo = RelatorioRevisaoService.formatarDetalhes(null);

        assertTrue(vazio.contains("DETALHES POR OCORRÊNCIA"));
        assertTrue(vazio.contains("Nenhuma ocorrência detalhada registrada."),
            "ausência de trilha tem de ser visível; seção sumida parece trilha não registrada");
        assertEquals(vazio, nulo, "nulo e vazio produzem o mesmo relatório");
    }

    @Test
    @DisplayName("A trilha traz antes e depois — sem os dois, uma correção errada não se reconstitui")
    void trilhaTrazAntesEDepois() {
        DetalheRevisao d = new DetalheRevisao("ep01.ass", 42, "Default", "CORRIGIDA",
            List.of("Concordância de gênero"), null, "She is tired.", "ele está cansado.",
            "ela está cansada.");

        String texto = RelatorioRevisaoService.formatarDetalhes(List.of(d));

        assertTrue(texto.contains("Arquivo: ep01.ass"));
        assertTrue(texto.contains("Evento: 42 | Estilo: Default"));
        assertTrue(texto.contains("Resultado: CORRIGIDA"));
        assertTrue(texto.contains("Problemas: Concordância de gênero"));
        assertTrue(texto.contains("Diagnóstico: —"), "campo ausente vira travessão, não vazio");
        assertTrue(texto.contains("PT anterior: ele está cansado."));
        assertTrue(texto.contains("Proposta: ela está cansada."));
    }

    @Test
    @DisplayName("resumirCampo torna a quebra VISÍVEL — sem isso duas falas diferentes ficam iguais")
    void quebraDeLinhaViraSimboloVisivel() {
        assertEquals("Primeira ↵ segunda", RelatorioRevisaoService.resumirCampo("Primeira\nsegunda"));
        assertEquals("Primeira ↵ segunda", RelatorioRevisaoService.resumirCampo("Primeira\r\nsegunda"));
        assertEquals("—", RelatorioRevisaoService.resumirCampo(null));
        assertEquals("—", RelatorioRevisaoService.resumirCampo("   "));
    }

    @Test
    @DisplayName("resumirCampo trunca em 500 e o resultado NÃO passa disso")
    void truncaMantendoOLimite() {
        String longo = "a".repeat(600);

        String r = RelatorioRevisaoService.resumirCampo(longo);

        assertEquals(500, r.length(), "497 caracteres + reticências = exatamente o limite");
        assertTrue(r.endsWith("..."));
        assertEquals(500, RelatorioRevisaoService.resumirCampo("b".repeat(500)).length(),
            "exatamente no limite passa inteiro, sem reticências");
        assertFalse(RelatorioRevisaoService.resumirCampo("b".repeat(500)).endsWith("..."));
    }

    @Test
    @DisplayName("mensagemFalha nunca devolve vazio nem stack trace")
    void mensagemDeFalhaSempreExplica() {
        assertEquals("boom", RelatorioRevisaoService.mensagemFalha(new IllegalStateException("boom")));
        assertEquals("IllegalStateException",
            RelatorioRevisaoService.mensagemFalha(new IllegalStateException()),
            "sem mensagem, o nome da classe é o diagnóstico mínimo");
        assertEquals("IllegalStateException",
            RelatorioRevisaoService.mensagemFalha(new IllegalStateException("   ")),
            "mensagem em branco não explica nada");
    }

    @Test
    @DisplayName("formatarDuracao: segundos abaixo de um minuto, minutos acima")
    void duracaoLegivel() {
        assertEquals("0s", RelatorioRevisaoService.formatarDuracao(500));
        assertEquals("59s", RelatorioRevisaoService.formatarDuracao(59_999));
        assertEquals("1min 0s", RelatorioRevisaoService.formatarDuracao(60_000));
        assertEquals("2min 5s", RelatorioRevisaoService.formatarDuracao(125_000));
    }
}
