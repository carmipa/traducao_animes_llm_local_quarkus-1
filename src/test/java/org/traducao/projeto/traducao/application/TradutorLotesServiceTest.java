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
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

        FakeEpisodio() { super(null, null, null, null, null, null); }

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
        return new TradutorLotesService(new MascaradorTags(), props, ui, ep, protecao, telemetria,
            new IsoladorQuebraDialogo());
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

        Map<String, String> r = s.traduzirPendentes(pendentes(), Set.of(), "ep.ass", new ArrayList<>(), null);

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

        Map<String, String> r = s.traduzirPendentes(pendentes("A", "B", "C"), Set.of(), "ep.ass", new ArrayList<>(), null);

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

        Map<String, String> r = s.traduzirPendentes(pendentes("{\\i1}Oi mundo"), Set.of(), "ep.ass", new ArrayList<>(), null);

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

        Map<String, String> r = s.traduzirPendentes(pendentes("{\\i1}Oi"), Set.of(), "ep.ass", avisos, null);

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

        Map<String, String> r = s.traduzirPendentes(pendentes("Oi"), Set.of(), "ep.ass", avisos, null);

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
    /**
     * PROPÓSITO DE NEGÓCIO: o mesmo verso de música (OP/ED) aparece em muitas camadas
     * KFX/clip com tags diferentes mas o MESMO texto mascarado; o LLM deve traduzi-lo
     * uma unica vez e a traducao ser reaplicada a cada camada com suas proprias tags.
     * <p>INVARIANTES DO DOMÍNIO: dedup pelo texto mascarado (nao pelo visivel); cada
     * camada preserva suas tags; nenhuma traducao muda.
     * <p>COMPORTAMENTO EM CASO DE FALHA: mais de uma chamada ao LLM ou tags trocadas reprova.
     */
    @Test
    void dedupMusicaTraduzMascaradoUmaVezEAplicaTagsPorCamada() throws Exception {
        FakeEpisodio ep = new FakeEpisodio();
        TradutorLotesService s = servico(props(20), ep, new FakeUiLogger(), new FakeProtecao(), new FakeTelemetria());
        LinkedHashSet<String> pend = pendentes("{\\clip(0,10)}A flower", "{\\clip(0,20)}A flower");
        Set<String> dedup = Set.of("{\\clip(0,10)}A flower", "{\\clip(0,20)}A flower");

        Map<String, String> r = s.traduzirPendentes(pend, dedup, "ep.ass", new ArrayList<>(), null);

        int linhasEnviadas = ep.lotesRecebidos.stream().mapToInt(l -> l.linhasOriginais().size()).sum();
        assertEquals(1, linhasEnviadas, "o mesmo texto mascarado deve ir ao LLM uma unica vez");
        assertEquals("T:{\\clip(0,10)}A flower", r.get("{\\clip(0,10)}A flower"), "camada 1 com suas tags");
        assertEquals("T:{\\clip(0,20)}A flower", r.get("{\\clip(0,20)}A flower"), "camada 2 com suas tags");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: verso de música com {@code \N} no MEIO da frase deixa de mandar um
     * marcador no meio da tradução. Um {@code [[TAGn]]} mid-sentence é o que o LLM mais erra —
     * o português reordena as palavras e o marcador se perde, derrubando a fala inteira.
     *
     * <p>CASO REAL (Break Blade filme 1, 2026-07-23): das 34 falas, 6 viraram pendência por
     * marcador corrompido, TODAS verso de música e cartela com tag no meio, como
     * {@code {\be1\fad(0,200)\pos(502.5,930)}because I never make\Npromises I cannot keep}. A
     * isolação da quebra já existia, mas era exclusiva do diálogo.
     *
     * <p>INVARIANTES DO DOMÍNIO: o texto que chega ao LLM não tem marcador no meio; a fala
     * traduzida volta com a MESMA quantidade de quebras que tinha.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: se o mascarado voltar a levar marcador mid-sentence,
     * a música volta a virar pendência a cada execução.
     */
    @Test
    void musicaComQuebraNoMeioNaoMandaMarcadorMidSentenceAoLlm() throws Exception {
        FakeEpisodio ep = new FakeEpisodio();
        TradutorLotesService s = servico(props(20), ep, new FakeUiLogger(), new FakeProtecao(), new FakeTelemetria());
        String verso = "{\\be1\\fad(0,200)}because I never make\\Npromises I cannot keep";
        LinkedHashSet<String> pend = pendentes(verso);

        Map<String, String> r = s.traduzirPendentes(
            pend, Set.of(verso), Set.of(verso), "ep.ass", new ArrayList<>(), null);

        String enviado = ep.lotesRecebidos.get(0).linhasOriginais().get(0);
        assertFalse(enviado.matches("(?s).*\\p{L}.*\\[\\[TAG\\d+]].*\\p{L}.*"),
            "nao pode haver marcador ENTRE trechos de texto visivel: " + enviado);
        assertTrue(r.get(verso).contains("\\N"),
            "a quebra visual precisa voltar na traducao: " + r.get(verso));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: KFX e romaji ficam FORA da isolação — ali a quebra acompanha o
     * tempo do efeito, não é decoração visual, e mexer nela desalinharia o karaokê.
     * <p>INVARIANTES DO DOMÍNIO: fala ausente do inventário segue o caminho de mascaramento.
     * <p>COMPORTAMENTO EM CASO DE FALHA: isolar quebra de KFX corrompe o sincronismo.
     */
    @Test
    void falaForaDoInventarioMantemAQuebraMascarada() throws Exception {
        FakeEpisodio ep = new FakeEpisodio();
        TradutorLotesService s = servico(props(20), ep, new FakeUiLogger(), new FakeProtecao(), new FakeTelemetria());
        String kfx = "{\\k30}fu\\Nmi";
        LinkedHashSet<String> pend = pendentes(kfx);

        s.traduzirPendentes(pend, Set.of(kfx), Set.of(), "ep.ass", new ArrayList<>(), null);

        String enviado = ep.lotesRecebidos.get(0).linhasOriginais().get(0);
        assertTrue(enviado.contains("[[TAG"),
            "sem estar no inventario, a quebra segue mascarada: " + enviado);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: fora do subconjunto deduplicavel (ex.: diálogo), cada fala
     * vai ao LLM separadamente — o dedup nunca vaza para o diálogo.
     * <p>INVARIANTES DO DOMÍNIO: {@code textosDeduplicaveis} vazio mantém o comportamento antigo.
     * <p>COMPORTAMENTO EM CASO DE FALHA: deduplicar fora do subconjunto reprova.
     */
    @Test
    void semDedupCadaCamadaVaiSeparadaAoLlm() throws Exception {
        FakeEpisodio ep = new FakeEpisodio();
        TradutorLotesService s = servico(props(20), ep, new FakeUiLogger(), new FakeProtecao(), new FakeTelemetria());
        LinkedHashSet<String> pend = pendentes("{\\clip(0,10)}A flower", "{\\clip(0,20)}A flower");

        Map<String, String> r = s.traduzirPendentes(pend, Set.of(), "ep.ass", new ArrayList<>(), null);

        int linhasEnviadas = ep.lotesRecebidos.stream().mapToInt(l -> l.linhasOriginais().size()).sum();
        assertEquals(2, linhasEnviadas, "sem dedup, as duas camadas vao ao LLM");
        assertEquals("T:{\\clip(0,10)}A flower", r.get("{\\clip(0,10)}A flower"));
        assertEquals("T:{\\clip(0,20)}A flower", r.get("{\\clip(0,20)}A flower"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: um cancelamento no meio propaga TraducaoParcialException reconstruída
     * com o dicionário parcial já DESMASCARADO, e a barra de progresso é finalizada mesmo na falha.
     * <p>INVARIANTES DO DOMÍNIO: o dicionário reconstruído mapeia original→desmascarado; a exceção
     * de nível de arquivo não carrega lotes; {@code finalizar()} roda no bloco finally.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dicionário mascarado, lotes preservados ou barra não
     * finalizada reprova.
     */
    /**
     * PROPÓSITO DE NEGÓCIO (rede de segurança do módulo de isolamento de \N): uma fala de
     * diálogo com quebra {@code \N} NO MEIO da frase — hoje mascarada como {@code [[TAGn]]}
     * mid-sentence — não pode mais virar pendente só porque o LLM não reposiciona o marcador
     * na ordem PT. Isolando o {@code \N} antes de mascarar, o modelo traduz a frase limpa e a
     * quebra é reinserida depois.
     * <p>INVARIANTES DO DOMÍNIO: LLM que "traduz" mas dropa o marcador mid-sentence (falha real
     * observada nas 122 falas-eco) deve resultar em TRADUÇÃO com {@code \N} reinserido, não no
     * original mantido; só diálogo (fora de {@code textosDeduplicaveis}).
     * <p>COMPORTAMENTO EM CASO DE FALHA (antes do fix): o desmascaramento reprova por marcador
     * perdido e a fala é mantida em inglês — este teste falha, caracterizando o bug.
     */
    @Test
    void dialogoComQuebraNoMeioEhTraduzidoAoInvesDeManterOriginal() throws Exception {
        FakeEpisodio ep = new FakeEpisodio();
        // Simula o LLM que traduz o texto visível mas descarta o marcador mid-sentence.
        ep.tradutor = l -> l.linhasOriginais().stream()
            .map(s -> "PT:" + s.replaceAll("\\[\\[TAG\\d+]]", "")).toList();
        TradutorLotesService s = servico(props(20), ep, new FakeUiLogger(), new FakeProtecao(), new FakeTelemetria());

        String original = "Why do we have to put up \\Nwith this";
        Map<String, String> r = s.traduzirPendentes(pendentes(original), Set.of(), "ep.ass", new ArrayList<>(), null);

        String traduzido = r.get(original);
        assertTrue(traduzido.startsWith("PT:"),
            "a fala de diálogo com \\N no meio deve ser TRADUZIDA, não mantida em inglês: " + traduzido);
        assertTrue(traduzido.contains("\\N"),
            "a quebra de linha \\N deve ser reinserida na tradução: " + traduzido);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o módulo de isolamento é gated a diálogo e a {@code \N} mid-sentence;
     * uma camada musical deduplicável com {@code \N} NÃO é tocada (segue mascarada como hoje).
     * <p>INVARIANTES DO DOMÍNIO: fala em {@code textosDeduplicaveis} preserva o comportamento antigo.
     * <p>COMPORTAMENTO EM CASO DE FALHA: isolar \N fora do diálogo reprova.
     */
    @Test
    void quebraEmCamadaMusicalDeduplicavelNaoEhIsolada() throws Exception {
        FakeEpisodio ep = new FakeEpisodio();
        TradutorLotesService s = servico(props(20), ep, new FakeUiLogger(), new FakeProtecao(), new FakeTelemetria());
        String verso = "A flower \\Nblooms";
        LinkedHashSet<String> pend = pendentes(verso);
        Set<String> dedup = Set.of(verso);

        s.traduzirPendentes(pend, dedup, "ep.ass", new ArrayList<>(), null);

        String enviadoAoLlm = ep.lotesRecebidos.get(0).linhasOriginais().get(0);
        assertTrue(enviadoAoLlm.contains("[[TAG0]]"),
            "camada musical deduplicável deve manter o \\N mascarado como marcador: " + enviadoAoLlm);
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
            () -> s.traduzirPendentes(pendentes("{\\i1}Oi"), Set.of(), "ep.ass", new ArrayList<>(), null));

        assertEquals(Map.of("{\\i1}Oi", "{\\i1}Ola"), lancada.getDicionarioParcial(),
            "o dicionário parcial deve estar desmascarado");
        assertNull(lancada.getLotesSalvos(), "a exceção reconstruída usa o dicionário, não os lotes");
        assertEquals(1, ui.finalizacoes, "finalizar() deve ocorrer também no caminho de falha");
    }
}
