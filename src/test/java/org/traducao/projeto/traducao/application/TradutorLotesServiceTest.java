package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.llm.domain.Lote;
import org.traducao.projeto.llm.domain.TraducaoLote;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.traducao.domain.TelemetriaTraducao;
import org.traducao.projeto.traducao.domain.exceptions.TraducaoParcialException;
import org.traducao.projeto.traducao.domain.ports.TelemetriaTraducaoPort;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.traducao.presentation.ui.ConsoleUILogger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa por regressão o coração do fluxo de tradução em lote do
 * {@link TradutorLotesService} (FASE F, R6) — mascaramento, ordenação em lotes, restauração
 * de tags e o caminho de exceção parcial — sem depender de LM Studio.
 *
 * <p>INVARIANTES DO DOMÍNIO: usa dublês determinísticos ({@link ProcessarEpisodioUseCase},
 * UI, proteção e telemetria) e o {@link MascaradorTags} real; nenhum teste usa rede, sleep ou
 * dependência temporal.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer desvio de ordem, mascaramento, fallback ou
 * reconstrução parcial reprova a suíte.
 */
class TradutorLotesServiceTest {

    /** Dublê do episódio: registra os lotes recebidos, ecoa {@code "T:"+linha} por padrão e pode abortar. */
    private static final class FakeEpisodio extends ProcessarEpisodioUseCase {
        final List<Lote> lotesRecebidos = new ArrayList<>();
        int chamadas = 0;
        TraducaoParcialException aLancar = null;
        Function<Lote, List<String>> tradutor = l -> l.linhasOriginais().stream().map(s -> "T:" + s).toList();

        FakeEpisodio() { super(null, null, null, null); }

        @Override
        public List<TraducaoLote> processarEpisodio(List<Lote> lotes, String promptSistemaCongelado) {
            chamadas++;
            lotesRecebidos.addAll(lotes);
            if (aLancar != null) { throw aLancar; }
            List<TraducaoLote> r = new ArrayList<>();
            for (Lote l : lotes) { r.add(new TraducaoLote(l.idLote(), tradutor.apply(l), true, null)); }
            return r;
        }
    }

    /** Dublê da UI: conta {@code finalizar()} e não constrói a barra de progresso. */
    private static final class FakeUiLogger extends ConsoleUILogger {
        int finalizacoes = 0;
        @Override public synchronized void iniciarLotes(int totalLotes, String nomeEpisodio) { /* no-op */ }
        @Override public synchronized void finalizar() { finalizacoes++; }
    }

    /** Dublê da telemetria: conta alucinações prevenidas. */
    private static final class FakeTelemetria implements TelemetriaTraducaoPort {
        int alucinacoesPrevenidas = 0;
        @Override public void registrarTraducao(TelemetriaTraducao t) { /* não exercitado */ }
        @Override public void registrarAlucinacaoPrevenida() { alucinacoesPrevenidas++; }
        @Override public void registrarRespostaTraducaoRejeitada() { /* não exercitado */ }
        @Override public void registrarFalhaTraducaoRecuperada() { /* não exercitado */ }
        @Override public void registrarFallbackMantido() { /* não exercitado */ }
    }

    /** Dublê da proteção ASS: {@code respostaSuspeita} controlado por campo. */
    private static final class FakeProtecao extends ProtecaoLegendaAssService {
        boolean suspeita = false;
        @Override public boolean respostaSuspeita(String original, String traduzido) { return suspeita; }
    }

    private static TradutorProperties props(int tamanhoLote) {
        return new TradutorProperties("e", "s", "c", tamanhoLote, List.of(), "en", "pt-BR");
    }

    private static LinkedHashSet<String> pendentes(String... itens) {
        return new LinkedHashSet<>(List.of(itens));
    }

    private TradutorLotesService servico(TradutorProperties props, FakeEpisodio ep,
                                         FakeUiLogger ui, FakeProtecao protecao, FakeTelemetria telemetria) {
        return new TradutorLotesService(new MascaradorTags(), props, ui, ep, protecao, telemetria);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: sem falas pendentes não há chamada ao LLM.
     * <p>INVARIANTES DO DOMÍNIO: conjunto vazio devolve mapa vazio e não invoca o episódio.
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer chamada ao episódio reprova.
     */
    @Test
    void conjuntoVazioRetornaMapaVazioSemChamarEpisodio() throws Exception {
        FakeEpisodio ep = new FakeEpisodio();
        TradutorLotesService s = servico(props(20), ep, new FakeUiLogger(), new FakeProtecao(), new FakeTelemetria());

        Map<String, String> r = s.traduzirPendentes(pendentes(), "ep.ass", new ArrayList<>(), null);

        assertTrue(r.isEmpty());
        assertEquals(0, ep.chamadas, "episódio não pode ser chamado sem pendências");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: cada fala mapeia para a sua própria tradução, mesmo distribuída em
     * múltiplos lotes.
     * <p>INVARIANTES DO DOMÍNIO: com tamanho de lote 2 e três falas, a ordem posição↔tradução
     * é preservada entre os lotes.
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer troca de mapeamento reprova.
     */
    @Test
    void ordemOriginalDasFalasEhPreservada() throws Exception {
        FakeEpisodio ep = new FakeEpisodio();
        TradutorLotesService s = servico(props(2), ep, new FakeUiLogger(), new FakeProtecao(), new FakeTelemetria());

        Map<String, String> r = s.traduzirPendentes(pendentes("A", "B", "C"), "ep.ass", new ArrayList<>(), null);

        assertEquals("T:A", r.get("A"));
        assertEquals("T:B", r.get("B"));
        assertEquals("T:C", r.get("C"));
        assertEquals(2, ep.lotesRecebidos.size(), "3 falas com tamanho de lote 2 geram 2 lotes");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: as tags de formatação são mascaradas antes do LLM e restauradas na
     * resposta — o modelo nunca vê a sintaxe de estilo.
     * <p>INVARIANTES DO DOMÍNIO: o lote enviado traz {@code [[TAG0]]} no lugar da tag; a saída
     * recupera a tag original.
     * <p>COMPORTAMENTO EM CASO DE FALHA: tag vista pelo LLM ou não restaurada reprova.
     */
    @Test
    void tagsSaoMascaradasAntesDoLlmERestauradasNaResposta() throws Exception {
        FakeEpisodio ep = new FakeEpisodio();
        ep.tradutor = l -> l.linhasOriginais().stream().map(s -> s.replace("Oi mundo", "Ola mundo")).toList();
        TradutorLotesService s = servico(props(20), ep, new FakeUiLogger(), new FakeProtecao(), new FakeTelemetria());

        Map<String, String> r = s.traduzirPendentes(pendentes("{\\i1}Oi mundo"), "ep.ass", new ArrayList<>(), null);

        assertEquals("[[TAG0]]Oi mundo", ep.lotesRecebidos.get(0).linhasOriginais().get(0),
            "o LLM deve receber o texto mascarado");
        assertEquals("{\\i1}Ola mundo", r.get("{\\i1}Oi mundo"),
            "a resposta deve ter a tag restaurada");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: alucinação de tags (marcador perdido) mantém só aquela fala original,
     * registra aviso e contabiliza na telemetria — sem derrubar o restante.
     * <p>INVARIANTES DO DOMÍNIO: o texto sem marcador reprova o desmascaramento e cai no fallback.
     * <p>COMPORTAMENTO EM CASO DE FALHA: fala traduzida à força, aviso ausente ou telemetria não
     * incrementada reprova.
     */
    @Test
    void alucinacaoDeTagsMantemFalaOriginalRegistraAvisoETelemetria() throws Exception {
        FakeEpisodio ep = new FakeEpisodio();
        ep.tradutor = l -> l.linhasOriginais().stream().map(s -> "Ola").toList();
        FakeTelemetria tele = new FakeTelemetria();
        List<String> avisos = new ArrayList<>();
        TradutorLotesService s = servico(props(20), ep, new FakeUiLogger(), new FakeProtecao(), tele);

        Map<String, String> r = s.traduzirPendentes(pendentes("{\\i1}Oi"), "ep.ass", avisos, null);

        assertEquals("{\\i1}Oi", r.get("{\\i1}Oi"), "tags corrompidas mantêm o original");
        assertEquals(1, avisos.size());
        assertEquals(1, tele.alucinacoesPrevenidas);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: resposta suspeita numa linha ASS pesada mantém o original, mesmo com
     * desmascaramento bem-sucedido.
     * <p>INVARIANTES DO DOMÍNIO: proteção suspeita → original preservado + aviso + telemetria.
     * <p>COMPORTAMENTO EM CASO DE FALHA: publicar a resposta suspeita reprova.
     */
    @Test
    void respostaSuspeitaDeLinhaAssPesadaMantemOriginal() throws Exception {
        FakeEpisodio ep = new FakeEpisodio();
        FakeProtecao protecao = new FakeProtecao();
        protecao.suspeita = true;
        FakeTelemetria tele = new FakeTelemetria();
        List<String> avisos = new ArrayList<>();
        TradutorLotesService s = servico(props(20), ep, new FakeUiLogger(), protecao, tele);

        Map<String, String> r = s.traduzirPendentes(pendentes("Oi"), "ep.ass", avisos, null);

        assertEquals("Oi", r.get("Oi"), "resposta suspeita mantém o original");
        assertEquals(1, avisos.size());
        assertEquals(1, tele.alucinacoesPrevenidas);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: um cancelamento no meio propaga TraducaoParcialException reconstruída
     * com o dicionário parcial já DESMASCARADO, e a barra de progresso é finalizada mesmo na falha.
     * <p>INVARIANTES DO DOMÍNIO: o dicionário reconstruído mapeia original→desmascarado; a exceção
     * de nível de arquivo não carrega lotes; {@code finalizar()} roda no bloco finally.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dicionário mascarado, lotes preservados ou barra não
     * finalizada reprova.
     */
    @Test
    void traducaoParcialReconstruidaComDicionarioDesmascaradoEFinalizaBarra() throws Exception {
        FakeEpisodio ep = new FakeEpisodio();
        TraducaoLote loteSalvo = new TraducaoLote(1, List.of("[[TAG0]]Ola"), true, null);
        ep.aLancar = new TraducaoParcialException("parou", List.of(loteSalvo), null);
        FakeUiLogger ui = new FakeUiLogger();
        TradutorLotesService s = servico(props(20), ep, ui, new FakeProtecao(), new FakeTelemetria());

        TraducaoParcialException lancada = assertThrows(TraducaoParcialException.class,
            () -> s.traduzirPendentes(pendentes("{\\i1}Oi"), "ep.ass", new ArrayList<>(), null));

        assertEquals(Map.of("{\\i1}Oi", "{\\i1}Ola"), lancada.getDicionarioParcial(),
            "o dicionário parcial deve estar desmascarado");
        assertNull(lancada.getLotesSalvos(), "a exceção reconstruída usa o dicionário, não os lotes");
        assertEquals(1, ui.finalizacoes, "finalizar() deve ocorrer também no caminho de falha");
    }
}
