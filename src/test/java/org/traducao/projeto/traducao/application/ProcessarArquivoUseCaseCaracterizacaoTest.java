package org.traducao.projeto.traducao.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.llm.domain.Lote;
import org.traducao.projeto.traducao.domain.ResultadoTraducaoArquivo;
import org.traducao.projeto.traducao.domain.StatusArquivoTraducao;
import org.traducao.projeto.llm.domain.StatusLlm;
import org.traducao.projeto.llm.domain.TraducaoLote;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.domain.ArquivoLegendaException;
import org.traducao.projeto.traducao.domain.exceptions.TraducaoParcialException;
import org.traducao.projeto.llm.domain.LlmPort;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.cachetraducao.infrastructure.CacheTraducaoService;
import org.traducao.projeto.cachetraducao.domain.EntradaCache;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.traducao.infrastructure.config.LlmProperties;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaSrt;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaSrt;
import org.traducao.projeto.qualidadeTraducao.application.DetectorTraducaoIdenticaService;
import org.traducao.projeto.traducao.infrastructure.adapters.LoreAtivaContextoAdapter;
import org.traducao.projeto.traducao.infrastructure.config.FallbackOnlineProperties;
import org.traducao.projeto.traducao.domain.ports.FallbackTraducaoOnlinePort;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.NormalizadorAcentosComuns;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.traducao.presentation.ui.ConsoleUILogger;
import org.traducao.projeto.traducao.presentation.ui.PastasExecucao;
import org.traducao.projeto.traducao.domain.TelemetriaTraducao;
import org.traducao.projeto.traducao.domain.ports.TelemetriaTraducaoPort;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: caracteriza o fluxo ponta-a-ponta de
 * {@link ProcessarArquivoUseCase} (ler → planejar cache → traduzir pendências →
 * validar → reconstruir → publicar → persistir cache → telemetria → resultado)
 * antes da decomposição estrutural da Opção 4, travando o comportamento
 * observável para que a refatoração posterior não possa alterá-lo em silêncio.
 *
 * <p>INVARIANTES DO DOMÍNIO: um episódio ASS/SRT sem pendências publica a saída
 * final {@code _PT-BR} e status {@code CONCLUIDO}; uma segunda execução com a
 * mesma proveniência reaproveita o cache sem chamar o LLM; uma fala que o modelo
 * devolve sem traduzir mantém o texto original, gera status {@code PARCIAL} e
 * publica apenas o artefato {@code .parcial}, preservando a saída final.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer desvio nessas invariantes falha a
 * suíte; o LLM é substituído por um dublê determinístico, de modo que a
 * caracterização não depende de um LM Studio ativo nem de rede.
 */
class ProcessarArquivoUseCaseCaracterizacaoTest {

    @TempDir
    Path raiz;

    private final TelemetriaCaptor telemetriaCaptor = new TelemetriaCaptor();

    // Fallback online controlável pelos testes: por padrão DESLIGADO (pipeline 100%
    // local), preservando o comportamento das caracterizações existentes.
    private boolean fallbackOnlineAtivo = false;
    private FallbackTraducaoOnlinePort fallbackPort = original -> Optional.empty();

    /**
     * PROPÓSITO DE NEGÓCIO: captura o último registro de telemetria emitido pelo fluxo, para a
     * caracterização assertar os campos observáveis em vez de descartá-los em um no-op.
     * <p>INVARIANTES DO DOMÍNIO: guarda a referência do último {@link TelemetriaTraducao}; os
     * demais sinais são no-op.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; sem registro, {@code ultima} permanece nula.
     */
    private static final class TelemetriaCaptor implements TelemetriaTraducaoPort {
        TelemetriaTraducao ultima;
        @Override public void registrarTraducao(TelemetriaTraducao t) { ultima = t; }
        @Override public void registrarAlucinacaoPrevenida() { /* não exercitado */ }
        @Override public void registrarRespostaTraducaoRejeitada() { /* não exercitado */ }
        @Override public void registrarFalhaTraducaoRecuperada() { /* não exercitado */ }
        @Override public void registrarFallbackMantido() { /* não exercitado */ }
    }

    private static final String CABECALHO_ASS = """
        [Script Info]
        ScriptType: v4.00+

        [V4+ Styles]
        Format: Name, Fontname, Fontsize
        Style: Default,Arial,48

        [Events]
        Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
        """;

    /**
     * PROPÓSITO DE NEGÓCIO: dublê determinístico do LLM local para a
     * caracterização — traduz o texto visível para um marcador PT-BR fixo,
     * preservando os marcadores {@code [[TAGn]]} de tags; falas contendo o
     * sentinela {@code KEEPME} voltam sem tradução, simulando a resposta que o
     * pipeline classifica como pendente.
     *
     * <p>INVARIANTES DO DOMÍNIO: preserva a contagem de linhas do lote e conta
     * quantas vezes o modelo foi efetivamente chamado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca falha por rede; as respostas são
     * puramente locais e reprodutíveis.
     */
    private static final class FakeLlmPort implements LlmPort {
        private static final Pattern TOKEN = Pattern.compile("\\[\\[[^\\]]*\\]\\]");
        final AtomicInteger chamadas = new AtomicInteger();
        private final boolean interromperNaPrimeira;

        FakeLlmPort() {
            this(false);
        }

        FakeLlmPort(boolean interromperNaPrimeira) {
            this.interromperNaPrimeira = interromperNaPrimeira;
        }

        @Override
        public TraducaoLote traduzir(Lote lote) {
            return traduzir(lote, null, null);
        }

        @Override
        public TraducaoLote traduzir(Lote lote, Double temperaturaOverride, String promptSistemaCongelado) {
            int chamada = chamadas.incrementAndGet();
            List<String> saida = lote.linhasOriginais().stream()
                .map(FakeLlmPort::traduzirLinha)
                .toList();
            if (interromperNaPrimeira && chamada == 1) {
                // Reproduz o clique em "Parar" logo após concluir o primeiro lote:
                // marca a interrupção cooperativa, mas devolve a tradução válida do
                // lote — o cancelamento é detectado pelo laço antes do próximo lote.
                Thread.currentThread().interrupt();
            }
            return new TraducaoLote(lote.idLote(), saida, true, null);
        }

        private static String traduzirLinha(String mascarada) {
            if (mascarada.contains("KEEPME")) {
                return mascarada; // devolve o original: o pipeline marca como pendente
            }
            Matcher m = TOKEN.matcher(mascarada);
            StringBuilder out = new StringBuilder();
            int ultimo = 0;
            boolean houveTexto = false;
            while (m.find()) {
                if (m.start() > ultimo) {
                    out.append("fala traduzida");
                    houveTexto = true;
                }
                out.append(m.group());
                ultimo = m.end();
            }
            if (ultimo < mascarada.length()) {
                out.append("fala traduzida");
                houveTexto = true;
            }
            return houveTexto ? out.toString() : mascarada;
        }

        @Override
        public StatusLlm verificarDisponibilidade() {
            return null; // não exercitado pelo fluxo de processar()
        }

        @Override
        public Optional<String> revisarConcordancia(String o, String t, List<String> p) {
            return Optional.empty();
        }

        @Override
        public Optional<String> corrigirTraducao(String o, String t, String m) {
            return Optional.empty();
        }
    }

    private static final class ContextoTeste implements ProvedorContexto {
        @Override public String getId() { return "caracterizacao"; }
        @Override public String getNomeExibicao() { return "Caracterizacao"; }
        @Override public String obterPromptSistema() { return "Traduza fielmente para PT-BR."; }
        // Reforço determinístico: a saída fixa do dublê ("fala traduzida") é tratada como
        // forma-ruim de "Legion" — só dispara quando o ORIGINAL contém "Legion", então os
        // demais cenários (originais sem "Legion") permanecem no-op.
        @Override public java.util.Map<String, String> correcoesTerminologia() {
            return java.util.Map.of("fala traduzida", "Legion");
        }
    }

    private ProcessarArquivoUseCase montar(FakeLlmPort llm) {
        return montar(llm, new ConsoleUILogger());
    }

    private ProcessarArquivoUseCase montar(FakeLlmPort llm, ConsoleUILogger uiLogger) {
        LeitorLegendaAss leitorAss = new LeitorLegendaAss();
        EscritorLegendaAss escritorAss = new EscritorLegendaAss();
        LeitorLegendaSrt leitorSrt = new LeitorLegendaSrt();
        EscritorLegendaSrt escritorSrt = new EscritorLegendaSrt();
        MascaradorTags mascarador = new MascaradorTags();
        CacheTraducaoService cache = new CacheTraducaoService(new ObjectMapper());
        ValidadorTraducaoService validador = new ValidadorTraducaoService();
        GerenciadorContexto gerenciador = new GerenciadorContexto(List.of(new ContextoTeste()));
        DetectorTraducaoIdenticaService detectorIdentica =
            new DetectorTraducaoIdenticaService(new LoreAtivaContextoAdapter(gerenciador));
        ProtecaoLegendaAssService protecao = new ProtecaoLegendaAssService();
        DetectorEfeitoKaraokeService detectorKaraoke = new DetectorEfeitoKaraokeService();
        TelemetriaTraducaoPort telemetria = telemetriaCaptor;
        // uiLogger chega por parâmetro: o cenário de cancelamento usa um logger
        // que desliga a barra de progresso (que, ativa, consumiria a interrupção).

        TradutorProperties props = new TradutorProperties(
            raiz.resolve("entrada").toString(),
            raiz.resolve("saida").toString(),
            raiz.resolve("cache").toString(),
            20, List.of(), "en", "pt-BR");
        LlmProperties llmProps = new LlmProperties(
            "http://127.0.0.1:1234/v1", "modelo-teste", 0.3, 2048,
            Duration.ofSeconds(5), Duration.ofSeconds(30));
        PastasExecucao pastas = new PastasExecucao();
        pastas.configurar(props.diretorioEntrada(), props.diretorioSaida(), props.diretorioCache(), props);

        ProcessarEpisodioUseCase episodio =
            new ProcessarEpisodioUseCase(llm, validador, uiLogger, telemetria, mascarador);

        ResolvedorSaidaLegenda resolvedorSaida = new ResolvedorSaidaLegenda();
        ResolvedorCacheTraducao resolvedorCache =
            new ResolvedorCacheTraducao(pastas, resolvedorSaida, gerenciador, llmProps, props);
        PoliticaBackupTraducao politicaBackup = new PoliticaBackupTraducao(cache, uiLogger);
        SeletorEventosTraduziveis seletorEventos =
            new SeletorEventosTraduziveis(new PoliticaEstiloMusical(List.of()), detectorKaraoke, protecao, mascarador);
        AvaliadorTraducaoCache avaliadorCache =
            new AvaliadorTraducaoCache(mascarador, detectorIdentica, validador);
        TradutorLotesService tradutorLotes =
            new TradutorLotesService(mascarador, props, uiLogger, episodio, protecao, telemetria,
                new IsoladorQuebraDialogo());
        EnforcadorTermosLore enforcadorTermos = new EnforcadorTermosLore();
        MontadorTelemetriaTraducao montadorTelemetria =
            new MontadorTelemetriaTraducao(llmProps, resolvedorCache);
        ClassificadorPendenciaTelemetria classificadorPendencia =
            new ClassificadorPendenciaTelemetria(detectorKaraoke);
        RecuperarPendenciaGoogleService recuperarPendenciaGoogle =
            new RecuperarPendenciaGoogleService(new FallbackOnlineProperties(fallbackOnlineAtivo), fallbackPort);

        return new ProcessarArquivoUseCase(
            leitorAss, escritorAss, leitorSrt, escritorSrt, cache,
            props, uiLogger,
            pastas, telemetria, protecao, gerenciador, resolvedorSaida, resolvedorCache, politicaBackup, seletorEventos, avaliadorCache, tradutorLotes, montadorTelemetria, classificadorPendencia, recuperarPendenciaGoogle,
            enforcadorTermos, new DetectorIdiomaFonteService(), new NormalizadorAspasService(),
            new NormalizadorAcentosComuns());
    }

    private Path escreverAss(String nomeArquivo, String... falas) throws IOException {
        Path pastaAnime = Files.createDirectories(raiz.resolve("AnimeTeste").resolve("legendas_originais"));
        StringBuilder sb = new StringBuilder(CABECALHO_ASS);
        int t = 1;
        for (String fala : falas) {
            sb.append(String.format("Dialogue: 0,0:00:%02d.00,0:00:%02d.00,Default,,0,0,0,,%s%n", t, t + 1, fala));
            t += 2;
        }
        Path arquivo = pastaAnime.resolve(nomeArquivo);
        Files.writeString(arquivo, sb.toString(), StandardCharsets.UTF_8);
        return arquivo;
    }

    private Path escreverSrt(String nomeArquivo, String... falas) throws IOException {
        Path pastaAnime = Files.createDirectories(raiz.resolve("AnimeTeste").resolve("legendas_originais"));
        StringBuilder sb = new StringBuilder();
        int i = 1;
        int t = 1;
        for (String fala : falas) {
            sb.append(i).append('\n')
              .append(String.format("00:00:%02d,000 --> 00:00:%02d,000%n", t, t + 1))
              .append(fala).append("\n\n");
            i++;
            t += 2;
        }
        Path arquivo = pastaAnime.resolve(nomeArquivo);
        Files.writeString(arquivo, sb.toString(), StandardCharsets.UTF_8);
        return arquivo;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: um episódio ASS sem pendências é traduzido, publicado
     * como {@code _PT-BR.ass} e marcado como {@code CONCLUIDO}, com o cache gravado.
     */
    @Test
    void assFluxoCompletoCacheMissConcluido() throws Exception {
        FakeLlmPort llm = new FakeLlmPort();
        ProcessarArquivoUseCase uc = montar(llm);
        Path entrada = escreverAss("ep.ass", "Hello there", "How are you");

        ResultadoTraducaoArquivo r = uc.processar(entrada, false);

        assertEquals(StatusArquivoTraducao.CONCLUIDO, r.status());
        Path saida = raiz.resolve("saida").resolve("ep_PT-BR.ass");
        assertTrue(Files.exists(saida), "saida final _PT-BR.ass deve existir");
        String conteudo = Files.readString(saida, StandardCharsets.UTF_8);
        assertTrue(conteudo.contains("fala traduzida"), "texto traduzido deve aparecer");
        assertFalse(conteudo.contains("Hello there"), "texto original nao deve permanecer");
        assertEquals(1, llm.chamadas.get(), "um lote deve ter sido enviado ao LLM");
        assertTrue(Files.exists(saida.getParent()));

        TelemetriaTraducao tel = telemetriaCaptor.ultima;
        assertNotNull(tel, "a telemetria do episódio deve ter sido registrada");
        assertEquals("CONCLUIDO", tel.statusFinal());
        assertEquals("ep.ass", tel.nomeEpisodio());
        assertEquals("modelo-teste", tel.modeloLlm());
        assertEquals(2, tel.totalLinhas(), "duas falas traduzíveis");
        assertEquals(2, tel.falasTraduzidas(), "ambas traduzidas de novo (cache-miss)");
        assertEquals(0, tel.falasDoCache(), "nenhuma fala veio do cache");
        assertEquals("AnimeTeste", tel.animeNome());
        assertTrue(tel.errosOcorridos().isEmpty(), "cenário concluído não tem avisos");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reexecutar o mesmo episódio, com a mesma proveniência,
     * reaproveita integralmente o cache e não chama o LLM (cache incremental).
     */
    @Test
    void assSegundaExecucaoReutilizaCacheSemChamarLlm() throws Exception {
        Path entrada = escreverAss("ep.ass", "Hello there", "How are you");

        FakeLlmPort primeira = new FakeLlmPort();
        montar(primeira).processar(entrada, false);
        assertEquals(1, primeira.chamadas.get());

        FakeLlmPort segunda = new FakeLlmPort();
        ResultadoTraducaoArquivo r = montar(segunda).processar(entrada, false);

        assertEquals(StatusArquivoTraducao.CONCLUIDO, r.status());
        assertEquals(0, segunda.chamadas.get(), "segunda execucao nao pode chamar o LLM");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: SRT nativo percorre o mesmo pipeline e publica
     * {@code _PT-BR.srt} sem conversão para ASS.
     */
    @Test
    void srtFluxoCompletoConcluido() throws Exception {
        FakeLlmPort llm = new FakeLlmPort();
        ProcessarArquivoUseCase uc = montar(llm);
        Path entrada = escreverSrt("ep.srt", "Hello there", "How are you");

        ResultadoTraducaoArquivo r = uc.processar(entrada, false);

        assertEquals(StatusArquivoTraducao.CONCLUIDO, r.status());
        Path saida = raiz.resolve("saida").resolve("ep_PT-BR.srt");
        assertTrue(Files.exists(saida), "saida final _PT-BR.srt deve existir");
        assertTrue(Files.readString(saida, StandardCharsets.UTF_8).contains("fala traduzida"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: uma fala que o LLM devolve sem traduzir permanece
     * pendente — status {@code PARCIAL}, publicação apenas em {@code .parcial.ass}
     * e preservação do texto original naquela fala, sem sobrescrever a saída final.
     */
    @Test
    void assLinhaNaoTraduzidaGeraParcial() throws Exception {
        FakeLlmPort llm = new FakeLlmPort();
        ProcessarArquivoUseCase uc = montar(llm);
        Path entrada = escreverAss("ep.ass", "Hello there", "KEEPME stays");

        ResultadoTraducaoArquivo r = uc.processar(entrada, false);

        assertEquals(StatusArquivoTraducao.PARCIAL, r.status());
        Path parcial = raiz.resolve("saida").resolve("ep_PT-BR.parcial.ass");
        Path finalPtBr = raiz.resolve("saida").resolve("ep_PT-BR.ass");
        assertTrue(Files.exists(parcial), "resultado incompleto deve ir para .parcial.ass");
        assertFalse(Files.exists(finalPtBr), "saida final nao pode ser publicada com pendencia");
        String conteudo = Files.readString(parcial, StandardCharsets.UTF_8);
        assertTrue(conteudo.contains("KEEPME stays"), "fala pendente mantem o texto original");

        TelemetriaTraducao tel = telemetriaCaptor.ultima;
        assertNotNull(tel, "a telemetria do episódio parcial deve ter sido registrada");
        assertEquals("PARCIAL", tel.statusFinal());
        assertEquals(2, tel.totalLinhas(), "duas falas traduzíveis");
        assertEquals(1, tel.falasTraduzidas(), "só 'Hello there' foi traduzida; KEEPME ficou pendente");
        assertFalse(tel.errosOcorridos().isEmpty(), "cenário parcial deve registrar avisos");

        // KPI estruturado (schema 1.1): a fala 'KEEPME stays' voltou como o original
        // (eco), sob estilo comum -> DIALOGO/ECO, quantidade 1.
        assertEquals(1, tel.pendenciasPorCausa().size(), "uma combinação (categoria,causa) pendente");
        var p = tel.pendenciasPorCausa().get(0);
        assertEquals("DIALOGO", p.categoria());
        assertEquals("ECO", p.causaRaiz());
        assertEquals(1, p.quantidade());
    }

    /**
     * PROPÓSITO DE NEGÓCIO (reforço de terminologia): quando o original contém um termo
     * canônico da lore ("Legion") e o modelo o traduziu (forma-ruim), o reforço
     * determinístico restaura a grafia oficial na saída publicada — sem depender do LLM.
     *
     * <p>INVARIANTES DO DOMÍNIO: o mapa da lore ativa só age quando o original tem o termo
     * canônico; a saída final traz "Legion", não a forma-ruim.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA (antes do wire): a saída manteria a forma-ruim.
     */
    @Test
    void reforcoDeterministicoRestauraTermoCanonicoNaSaida() throws Exception {
        FakeLlmPort llm = new FakeLlmPort();
        ProcessarArquivoUseCase uc = montar(llm);
        // Original com o termo canônico "Legion"; o dublê traduz para "fala traduzida",
        // que o mapa do ContextoTeste restaura para "Legion".
        Path entrada = escreverAss("ep.ass", "The Legion attacks at dawn");

        uc.processar(entrada, false);

        Path saida = raiz.resolve("saida").resolve("ep_PT-BR.ass");
        assertTrue(Files.exists(saida), "saída final deve existir");
        String conteudo = Files.readString(saida, StandardCharsets.UTF_8);
        assertTrue(conteudo.contains("Legion"), "o termo canônico deve ser restaurado na saída");
        assertFalse(conteudo.contains("fala traduzida"), "a forma-ruim não pode permanecer");
    }

    /**
     * PROPÓSITO DE NEGÓCIO (fallback online — rede de segurança): com o modo online LIGADO,
     * uma fala de diálogo que o LLM deixou pendente é recuperada pelo provedor externo,
     * validada canonicamente e publicada — o episódio conclui em vez de ficar PARCIAL.
     *
     * <p>INVARIANTES DO DOMÍNIO: só a fala pendente vai ao provedor; a resposta entra no
     * ASS/cache final; status {@code CONCLUIDO}; sem pendências no KPI.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA (antes do wire): a fala continuaria pendente e o
     * episódio seria PARCIAL — este teste falharia.
     */
    @Test
    void fallbackLigadoRecuperaDialogoPendenteEConclui() throws Exception {
        fallbackOnlineAtivo = true;
        fallbackPort = original -> original.contains("KEEPME")
            ? Optional.of("permanece aqui") : Optional.empty();
        FakeLlmPort llm = new FakeLlmPort();
        ProcessarArquivoUseCase uc = montar(llm);
        Path entrada = escreverAss("ep.ass", "Hello there", "KEEPME stays");

        ResultadoTraducaoArquivo r = uc.processar(entrada, false);

        assertEquals(StatusArquivoTraducao.CONCLUIDO, r.status(),
            "com a fala recuperada pelo fallback, o episódio conclui");
        Path finalPtBr = raiz.resolve("saida").resolve("ep_PT-BR.ass");
        assertTrue(Files.exists(finalPtBr), "saída final deve ser publicada após recuperação");
        String conteudo = Files.readString(finalPtBr, StandardCharsets.UTF_8);
        assertTrue(conteudo.contains("permanece aqui"), "a tradução do fallback deve entrar na legenda");
        assertFalse(conteudo.contains("KEEPME stays"), "a fala original não pode permanecer");

        TelemetriaTraducao tel = telemetriaCaptor.ultima;
        assertEquals("CONCLUIDO", tel.statusFinal());
        assertTrue(tel.pendenciasPorCausa().isEmpty(), "não deve sobrar pendência após recuperação");
    }

    /**
     * PROPÓSITO DE NEGÓCIO (fallback online — falha externa é segura): com o modo LIGADO
     * mas o provedor indisponível (resposta vazia), a fala continua pendente exatamente
     * como sem o fallback — nada é publicado à força.
     *
     * <p>INVARIANTES DO DOMÍNIO: porta vazia ⇒ status {@code PARCIAL}; original preservado
     * no artefato parcial.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: publicar tradução inexistente reprova.
     */
    @Test
    void fallbackLigadoComRedeVaziaMantemPendente() throws Exception {
        fallbackOnlineAtivo = true;
        fallbackPort = original -> Optional.empty(); // simula rede fora/recusa
        FakeLlmPort llm = new FakeLlmPort();
        ProcessarArquivoUseCase uc = montar(llm);
        Path entrada = escreverAss("ep.ass", "Hello there", "KEEPME stays");

        ResultadoTraducaoArquivo r = uc.processar(entrada, false);

        assertEquals(StatusArquivoTraducao.PARCIAL, r.status(),
            "sem resposta do provedor, a fala continua pendente");
        assertFalse(Files.exists(raiz.resolve("saida").resolve("ep_PT-BR.ass")),
            "saída final não pode ser publicada com pendência");
    }

    /**
     * PROPÓSITO DE NEGÓCIO (visibilidade do fallback): com o modo online LIGADO e falas de
     * diálogo pendentes, o pipeline ANUNCIA na saída dinâmica que está enviando ao scraping
     * do Google — para o operador não ficar no escuro durante a recuperação de último recurso.
     *
     * <p>INVARIANTES DO DOMÍNIO: a linha de aviso cita "scraping do Google" e é emitida antes
     * da tentativa externa, apenas quando há diálogo pendente e o fallback está ligado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA (antes do wire): nenhuma narração — este teste falharia.
     */
    @Test
    void fallbackLigadoAnunciaEnvioAoScrapingDoGoogle() throws Exception {
        fallbackOnlineAtivo = true;
        fallbackPort = original -> original.contains("KEEPME")
            ? Optional.of("permanece aqui") : Optional.empty();
        FakeLlmPort llm = new FakeLlmPort();
        LoggerCapturador logger = new LoggerCapturador();
        ProcessarArquivoUseCase uc = montar(llm, logger);
        Path entrada = escreverAss("ep.ass", "Hello there", "KEEPME stays");

        uc.processar(entrada, false);

        assertTrue(logger.mensagens.stream().anyMatch(m -> m.contains("scraping do Google")),
            "deve anunciar o envio ao scraping do Google quando há diálogo pendente e o fallback está ligado");
    }

    /**
     * PROPÓSITO DE NEGÓCIO (fallback desligado é 100% local e silencioso): com o modo online
     * DESLIGADO, mesmo havendo fala pendente, o pipeline NÃO pode anunciar envio ao Google —
     * evita mentir que houve chamada externa quando nenhuma ocorre.
     *
     * <p>INVARIANTES DO DOMÍNIO: nenhuma linha citando "scraping do Google" é emitida.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: um anúncio incondicional (sem checar {@code ativo()})
     * reprova este teste.
     */
    @Test
    void fallbackDesligadoNaoAnunciaEnvioAoGoogle() throws Exception {
        fallbackOnlineAtivo = false;
        fallbackPort = original -> Optional.empty();
        FakeLlmPort llm = new FakeLlmPort();
        LoggerCapturador logger = new LoggerCapturador();
        ProcessarArquivoUseCase uc = montar(llm, logger);
        Path entrada = escreverAss("ep.ass", "Hello there", "KEEPME stays");

        uc.processar(entrada, false);

        assertTrue(logger.mensagens.stream().noneMatch(m -> m.contains("scraping do Google")),
            "com o fallback desligado, nada de scraping do Google deve ser anunciado");
    }

    /**
     * PROPÓSITO DE NEGÓCIO (fonte contaminada — guarda de idioma): uma fala cuja FONTE já está
     * em PT é mantida como está, SEM ir ao LLM, e o episódio conclui — em vez de virar eco/recusa
     * pendente. É a defesa direta contra os arquivos "inglês" meio-traduzidos.
     *
     * <p>INVARIANTES DO DOMÍNIO: a fala já-PT sai verbatim; status {@code CONCLUIDO}; um aviso
     * informa que ela foi mantida sem retradução.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA (antes do wire): a fala já-PT iria ao LLM, voltaria como
     * eco/recusa e o episódio ficaria PARCIAL — este teste falharia.
     */
    @Test
    void fonteJaEmPortuguesEhMantidaSemRetraducao() throws Exception {
        FakeLlmPort llm = new FakeLlmPort();
        LoggerCapturador logger = new LoggerCapturador();
        ProcessarArquivoUseCase uc = montar(llm, logger);
        Path entrada = escreverAss("ep.ass", "Não é que ele fosse desagradável.");

        ResultadoTraducaoArquivo r = uc.processar(entrada, false);

        assertEquals(StatusArquivoTraducao.CONCLUIDO, r.status(),
            "a linha já-PT não vira pendência; o episódio conclui sem chamar o LLM");
        Path finalPtBr = raiz.resolve("saida").resolve("ep_PT-BR.ass");
        String conteudo = Files.readString(finalPtBr, StandardCharsets.UTF_8);
        assertTrue(conteudo.contains("Não é que ele fosse desagradável."),
            "a fala já em PT deve ser mantida verbatim na saída");
        assertTrue(logger.mensagens.stream().anyMatch(m -> m.contains("já no idioma-alvo")),
            "deve anunciar que manteve fala(s) já no idioma-alvo");
    }

    /**
     * PROPÓSITO DE NEGÓCIO (segurança da guarda): uma fala em INGLÊS nunca pode ser classificada
     * como já-no-alvo — deixar inglês sem traduzir é o erro que a guarda precisa evitar.
     *
     * <p>INVARIANTES DO DOMÍNIO: nenhuma mensagem "já no idioma-alvo" é emitida para inglês; a
     * fala segue o caminho normal de tradução.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: um detector agressivo demais (que pulasse inglês)
     * reprova este teste.
     */
    @Test
    void fonteInglesNaoEhTratadaComoJaTraduzida() throws Exception {
        FakeLlmPort llm = new FakeLlmPort();
        LoggerCapturador logger = new LoggerCapturador();
        ProcessarArquivoUseCase uc = montar(llm, logger);
        Path entrada = escreverAss("ep.ass", "It is because the war changed everything.");

        uc.processar(entrada, false);

        assertTrue(logger.mensagens.stream().noneMatch(m -> m.contains("já no idioma-alvo")),
            "linha inglesa não pode ser classificada como já no idioma-alvo");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: uma falha real na SUBSTITUIÇÃO ATÔMICA final (o caminho
     * de destino já está ocupado por um diretório) não pode publicar uma legenda
     * inválida, não pode destruir o que ocupa o destino e não pode deixar
     * temporários órfãos; caracteriza o contrato de erro da publicação atômica.
     *
     * <p>INVARIANTES DO DOMÍNIO: a falha vira {@link ArquivoLegendaException}; o
     * temporário criado pelo escritor é sempre removido; o diretório que ocupa o
     * destino permanece; a entrada permanece byte a byte intacta; o cache não é
     * persistido (a gravação ocorre depois da publicação e nunca é alcançada).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer regressão que publique saída
     * final, deixe {@code .tmp} órfão, altere a entrada ou troque o tipo da
     * exceção falha a suíte.
     */
    @Test
    void falhaNaPublicacaoAtomicaNaoPublicaNemDeixaTemporario() throws Exception {
        FakeLlmPort llm = new FakeLlmPort();
        ProcessarArquivoUseCase uc = montar(llm);
        Path entrada = escreverAss("ep.ass", "Hello there", "How are you");
        String origemAntes = Files.readString(entrada, StandardCharsets.UTF_8);
        // raiz/saida é um diretório NORMAL; o ponto de falha é o move atômico final:
        // o caminho de destino esperado (ep_PT-BR.ass) já está ocupado por um
        // diretório, então o escritor cria/escreve o temporário e falha ao
        // substituir o destino. Sem ACL, lock, antivírus, sleep ou privilégio.
        Path pastaSaida = Files.createDirectories(raiz.resolve("saida"));
        Path destinoOcupado = Files.createDirectory(pastaSaida.resolve("ep_PT-BR.ass"));

        assertThrows(ArquivoLegendaException.class, () -> uc.processar(entrada, false));

        assertTrue(Files.isDirectory(destinoOcupado), "o diretorio que ocupava o destino permanece intacto");
        try (Stream<Path> itens = Files.list(pastaSaida)) {
            boolean sobrouTemporario = itens.anyMatch(p -> {
                String nome = p.getFileName().toString();
                return Files.isRegularFile(p) && nome.startsWith("ep_PT-BR.ass") && nome.endsWith(".tmp");
            });
            assertFalse(sobrouTemporario, "nenhum temporario de publicacao pode permanecer em raiz/saida");
        }
        assertEquals(origemAntes, Files.readString(entrada, StandardCharsets.UTF_8), "entrada byte a byte intacta");
        assertFalse(Files.exists(raiz.resolve("cache").resolve("AnimeTeste").resolve("ep.cache.json")),
            "cache nao e persistido quando a publicacao falha antes da gravacao do cache");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o cancelamento cooperativo NO MEIO do processamento
     * (após o primeiro lote concluir e antes do segundo) encerra o episódio como
     * parcial, preservando no cache exatamente o progresso já traduzido e sem
     * publicar a saída final — caracteriza a semântica de "Parar" da UI.
     *
     * <p>INVARIANTES DO DOMÍNIO: o próprio dublê da LLM dispara a interrupção após
     * a primeira chamada; o laço detecta antes do segundo lote e lança
     * {@link TraducaoParcialException}; exatamente uma chamada ao LLM ocorre; o
     * segundo lote nunca é enviado; o cache parcial contém e só contém as falas do
     * primeiro lote; nenhuma saída final {@code _PT-BR} é publicada; a entrada
     * permanece intacta.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer chamada extra ao LLM, publicação
     * final, divergência de quantidade/conteúdo do cache parcial ou alteração da
     * entrada falha a suíte. Determinístico: a interrupção é marcada pelo dublê
     * (sem sleep nem corrida) e a barra de progresso é desligada para não consumir
     * o flag antes da verificação do laço.
     */
    @Test
    void cancelamentoNoMeioPreservaProgressoParcialDoPrimeiroLote() throws Exception {
        FakeLlmPort llm = new FakeLlmPort(true);
        ProcessarArquivoUseCase uc = montar(llm, new ConsoleUILoggerSilencioso());
        // 21 falas distintas e traduzíveis: com tamanhoLote=20, geram 2 lotes
        // (20 + 1); o primeiro conclui, o segundo nunca é enviado.
        String[] falas = new String[21];
        for (int i = 0; i < falas.length; i++) {
            falas[i] = String.format("Line %02d", i + 1);
        }
        Path entrada = escreverAss("ep.ass", falas);
        String origemAntes = Files.readString(entrada, StandardCharsets.UTF_8);

        try {
            assertThrows(TraducaoParcialException.class, () -> uc.processar(entrada, false));
        } finally {
            Thread.interrupted();
        }

        assertEquals(1, llm.chamadas.get(), "apenas o primeiro lote pode ser enviado ao LLM");
        // Cache parcial: exatamente as 20 falas do primeiro lote (Line 01..Line 20),
        // sem a fala do segundo lote (Line 21). Carregado com a mesma proveniência.
        Path arquivoCache = raiz.resolve("cache").resolve("AnimeTeste").resolve("ep.cache.json");
        assertTrue(Files.exists(arquivoCache), "o progresso parcial deve ter sido persistido no cache");
        ProvenienciaCache prov = new ProvenienciaCache(
            ProvenienciaCache.SCHEMA_ATUAL, "caracterizacao",
            ProvenienciaCache.hashDe("Traduza fielmente para PT-BR."),
            "modelo-teste", "en", "pt-BR");
        var mapa = new CacheTraducaoService(new ObjectMapper()).carregar(arquivoCache, prov).mapa();
        assertEquals(20, mapa.size(), "o cache parcial deve conter as 20 falas do primeiro lote");
        for (int i = 1; i <= 20; i++) {
            String chave = String.format("Line %02d", i);
            assertTrue(mapa.containsKey(chave), "cache parcial deve conter " + chave);
            assertEquals("fala traduzida", mapa.get(chave), "conteudo traduzido do primeiro lote");
        }
        assertFalse(mapa.containsKey("Line 21"), "a fala do segundo lote nao pode estar no cache");

        assertFalse(Files.exists(raiz.resolve("saida").resolve("ep_PT-BR.ass")),
            "nenhuma saida final pode ser publicada apos o cancelamento");
        assertEquals(origemAntes, Files.readString(entrada, StandardCharsets.UTF_8), "entrada intacta");
    }

    /**
     * PROPÓSITO DE NEGÓCIO (F0/R4 — elegibilidade): um evento que é puro desenho
     * vetorial ({@code \p1} do Aegisub) nunca é considerado traduzível — não é
     * enviado ao LLM e permanece idêntico na saída. Fixa a blindagem de
     * elegibilidade antes de extrair {@code isTraduzivel} para um colaborador.
     *
     * <p>INVARIANTES DO DOMÍNIO: com o desenho como único evento, nenhum lote é
     * enviado (LLM chamado zero vezes), o status é {@code CONCLUIDO} e o comando de
     * desenho continua byte a byte na saída publicada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer regressão que envie o desenho ao
     * LLM ou o altere na saída falha a suíte.
     */
    @Test
    void desenhoVetorialNaoEhTraduzidoNemEnviadoAoLlm() throws Exception {
        FakeLlmPort llm = new FakeLlmPort();
        ProcessarArquivoUseCase uc = montar(llm);
        String desenho = "{\\p1}m 5 5 l 40 5 l 40 40 l 5 40{\\p0}";
        Path entrada = escreverAss("ep.ass", desenho);

        ResultadoTraducaoArquivo r = uc.processar(entrada, false);

        assertEquals(StatusArquivoTraducao.CONCLUIDO, r.status());
        assertEquals(0, llm.chamadas.get(), "desenho vetorial nunca deve ir ao LLM");
        Path saida = raiz.resolve("saida").resolve("ep_PT-BR.ass");
        assertTrue(Files.readString(saida, StandardCharsets.UTF_8).contains("m 5 5 l 40 5"),
            "o desenho vetorial deve permanecer intacto na saida");
    }

    /**
     * PROPÓSITO DE NEGÓCIO (F0/R4 — elegibilidade): um letreiro/título animado
     * quadro a quadro (tag de efeito pesada + pouco texto visível + o mesmo texto
     * repetido muitas vezes) é blindado antes do LLM. Fixa a heurística de
     * repetição antes de extrair {@code isTraduzivel}.
     *
     * <p>INVARIANTES DO DOMÍNIO: cinco ocorrências idênticas (>= limiar de
     * repetição) do letreiro não geram nenhuma chamada ao LLM e o texto visível
     * original permanece na saída, sem marcador de tradução.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: se a heurística deixar de bloquear, o LLM
     * é chamado e "fala traduzida" aparece na saída — a suíte falha.
     */
    @Test
    void letreiroAnimadoRepetidoNaoEhTraduzido() throws Exception {
        FakeLlmPort llm = new FakeLlmPort();
        ProcessarArquivoUseCase uc = montar(llm);
        String letreiro = "{\\clip(0,0,300,300)\\t(0,1000,\\frx360)\\pos(20,20)}Hi";
        Path entrada = escreverAss("ep.ass", letreiro, letreiro, letreiro, letreiro, letreiro);

        uc.processar(entrada, false);

        assertEquals(0, llm.chamadas.get(), "letreiro animado repetido nao deve ir ao LLM");
        Path saida = raiz.resolve("saida").resolve("ep_PT-BR.ass");
        String conteudo = Files.readString(saida, StandardCharsets.UTF_8);
        assertFalse(conteudo.contains("fala traduzida"), "nenhuma fala traduzida deve aparecer");
        assertTrue(conteudo.contains("Hi"), "o texto original do letreiro deve permanecer");
    }

    /**
     * PROPÓSITO DE NEGÓCIO (F0/R5 — reuso de cache): uma entrada de cache cujo
     * "traduzido" é o próprio original em inglês (aparência de fala não traduzida)
     * NÃO pode ser reaproveitada — deve ser reenviada ao LLM. Fixa a política de
     * reuso antes de extrair {@code isCacheReaproveitavel}.
     *
     * <p>INVARIANTES DO DOMÍNIO: com o cache semeado apontando "Hello there" para
     * ele mesmo, a execução ainda assim chama o LLM uma vez e publica a tradução
     * ("fala traduzida"), em vez de reusar o conteúdo suspeito.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: se o cache suspeito for reaproveitado, o
     * LLM não é chamado e a saída mantém o inglês — a suíte falha.
     */
    @Test
    void cacheComFalaAparentandoNaoTraduzidaNaoEhReaproveitado() throws Exception {
        Path entrada = escreverAss("ep.ass", "Hello there");
        Path cachePath = raiz.resolve("cache").resolve("AnimeTeste").resolve("ep.cache.json");
        Files.createDirectories(cachePath.getParent());
        ProvenienciaCache prov = new ProvenienciaCache(
            ProvenienciaCache.SCHEMA_ATUAL, "caracterizacao",
            ProvenienciaCache.hashDe("Traduza fielmente para PT-BR."),
            "modelo-teste", "en", "pt-BR");
        new CacheTraducaoService(new ObjectMapper()).salvar(cachePath, prov,
            List.of(new EntradaCache(0, "Default", "Hello there", "Hello there", "en", "pt-BR")));

        FakeLlmPort llm = new FakeLlmPort();
        montar(llm).processar(entrada, false);

        assertEquals(1, llm.chamadas.get(),
            "fala em cache que aparenta nao-traduzida deve ser reenviada ao LLM");
        Path saida = raiz.resolve("saida").resolve("ep_PT-BR.ass");
        assertTrue(Files.readString(saida, StandardCharsets.UTF_8).contains("fala traduzida"),
            "a saida deve conter a retraducao, nao o cache suspeito");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: logger de progresso silencioso para os testes —
     * desliga a barra de progresso de terceiros ({@code me.tongfei:progressbar}),
     * que, quando ativa, consome o flag de interrupção da thread e impediria
     * caracterizar o cancelamento cooperativo disparado pelo dublê da LLM.
     *
     * <p>INVARIANTES DO DOMÍNIO: nunca constrói a barra; os demais métodos do
     * {@link ConsoleUILogger} passam a operar no ramo {@code pb == null}, sem
     * qualquer interação capaz de consumir a interrupção.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; a inicialização de lotes é um
     * no-op deliberado.
     */
    private static final class ConsoleUILoggerSilencioso extends ConsoleUILogger {
        @Override
        public synchronized void iniciarLotes(int totalLotes, String nomeEpisodio) {
            // no-op: não constrói a barra de progresso (evita consumir a interrupção).
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: captura as mensagens emitidas por {@code uiLogger.log(...)}
     * para que os testes possam asserir a narração da saída dinâmica (ex.: o anúncio do
     * fallback Google) sem depender de capturar {@code System.out}.
     *
     * <p>INVARIANTES DO DOMÍNIO: não constrói a barra de progresso e registra cada mensagem
     * exatamente uma vez, na ordem recebida.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; lista thread-safe para tolerar emissões
     * de qualquer thread do pipeline.
     */
    private static final class LoggerCapturador extends ConsoleUILogger {
        final java.util.List<String> mensagens = new java.util.concurrent.CopyOnWriteArrayList<>();

        @Override
        public synchronized void iniciarLotes(int totalLotes, String nomeEpisodio) {
            // no-op: sem barra de progresso nos testes.
        }

        @Override
        public synchronized void log(String mensagem) {
            mensagens.add(mensagem);
        }
    }
}
