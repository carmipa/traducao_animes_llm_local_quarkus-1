package org.traducao.projeto.traducao.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.traducao.domain.Lote;
import org.traducao.projeto.traducao.domain.ResultadoTraducaoArquivo;
import org.traducao.projeto.traducao.domain.StatusArquivoTraducao;
import org.traducao.projeto.traducao.domain.StatusLlm;
import org.traducao.projeto.traducao.domain.TraducaoLote;
import org.traducao.projeto.traducao.domain.exceptions.ArquivoLegendaException;
import org.traducao.projeto.traducao.domain.exceptions.TraducaoParcialException;
import org.traducao.projeto.traducao.domain.ports.MistralPort;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;
import org.traducao.projeto.traducao.infrastructure.cache.CacheTraducaoService;
import org.traducao.projeto.traducao.infrastructure.config.LlmProperties;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto;
import org.traducao.projeto.traducao.infrastructure.legenda.EscritorLegendaAss;
import org.traducao.projeto.traducao.infrastructure.legenda.EscritorLegendaSrt;
import org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaAss;
import org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaSrt;
import org.traducao.projeto.traducao.infrastructure.legenda.MascaradorTags;
import org.traducao.projeto.traducao.presentation.ui.ConsoleUILogger;
import org.traducao.projeto.traducao.presentation.ui.PastasExecucao;
import org.traducao.projeto.telemetria.TelemetriaService;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private static final class FakeMistralPort implements MistralPort {
        private static final Pattern TOKEN = Pattern.compile("\\[\\[[^\\]]*\\]\\]");
        final AtomicInteger chamadas = new AtomicInteger();

        @Override
        public TraducaoLote traduzir(Lote lote) {
            return traduzir(lote, null, null);
        }

        @Override
        public TraducaoLote traduzir(Lote lote, Double temperaturaOverride, String promptSistemaCongelado) {
            chamadas.incrementAndGet();
            List<String> saida = lote.linhasOriginais().stream()
                .map(FakeMistralPort::traduzirLinha)
                .toList();
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

        @Override
        public Optional<String> revisarLore(String s, String o, String t, List<String> p) {
            return Optional.empty();
        }
    }

    private static final class ContextoTeste implements ProvedorContexto {
        @Override public String getId() { return "caracterizacao"; }
        @Override public String getNomeExibicao() { return "Caracterizacao"; }
        @Override public String obterPromptSistema() { return "Traduza fielmente para PT-BR."; }
    }

    private ProcessarArquivoUseCase montar(FakeMistralPort llm) {
        return montar(llm, new ConsoleUILogger());
    }

    private ProcessarArquivoUseCase montar(FakeMistralPort llm, ConsoleUILogger uiLogger) {
        LeitorLegendaAss leitorAss = new LeitorLegendaAss();
        EscritorLegendaAss escritorAss = new EscritorLegendaAss();
        LeitorLegendaSrt leitorSrt = new LeitorLegendaSrt();
        EscritorLegendaSrt escritorSrt = new EscritorLegendaSrt();
        MascaradorTags mascarador = new MascaradorTags();
        CacheTraducaoService cache = new CacheTraducaoService(new ObjectMapper());
        ValidadorTraducaoService validador = new ValidadorTraducaoService();
        GerenciadorContexto gerenciador = new GerenciadorContexto(List.of(new ContextoTeste()));
        DetectorTraducaoIdenticaService detectorIdentica = new DetectorTraducaoIdenticaService(gerenciador);
        ProtecaoLegendaAssService protecao = new ProtecaoLegendaAssService();
        DetectorEfeitoKaraokeService detectorKaraoke = new DetectorEfeitoKaraokeService();
        TelemetriaService telemetria = new TelemetriaService();
        // uiLogger chega por parâmetro: permite injetar a interrupção
        // determinística logo após iniciarLotes, imediatamente antes do laço de lotes.

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
            new ProcessarEpisodioUseCase(llm, validador, uiLogger, telemetria);

        return new ProcessarArquivoUseCase(
            leitorAss, escritorAss, leitorSrt, escritorSrt, mascarador, cache,
            episodio, validador, detectorIdentica, props, llmProps, uiLogger,
            pastas, telemetria, detectorKaraoke, protecao, gerenciador);
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
        FakeMistralPort llm = new FakeMistralPort();
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
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reexecutar o mesmo episódio, com a mesma proveniência,
     * reaproveita integralmente o cache e não chama o LLM (cache incremental).
     */
    @Test
    void assSegundaExecucaoReutilizaCacheSemChamarLlm() throws Exception {
        Path entrada = escreverAss("ep.ass", "Hello there", "How are you");

        FakeMistralPort primeira = new FakeMistralPort();
        montar(primeira).processar(entrada, false);
        assertEquals(1, primeira.chamadas.get());

        FakeMistralPort segunda = new FakeMistralPort();
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
        FakeMistralPort llm = new FakeMistralPort();
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
        FakeMistralPort llm = new FakeMistralPort();
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
    }

    /**
     * PROPÓSITO DE NEGÓCIO: uma falha real de IO na publicação (a pasta de saída
     * é, na verdade, um arquivo comum) não pode publicar uma legenda final inválida
     * nem sobrescrever conteúdo existente; caracteriza o contrato de erro atual.
     *
     * <p>INVARIANTES DO DOMÍNIO: a falha vira {@link ArquivoLegendaException}; a
     * entrada permanece intacta; nenhum {@code _PT-BR} é publicado; o cache não é
     * gravado (a persistência ocorre depois da escrita e nunca é alcançada).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer regressão que publique saída
     * final, corrompa a entrada ou troque o tipo da exceção falha a suíte.
     */
    @Test
    void falhaDeEscritaNaoPublicaSaidaFinalNemCorrompeEntrada() throws Exception {
        FakeMistralPort llm = new FakeMistralPort();
        ProcessarArquivoUseCase uc = montar(llm);
        Path entrada = escreverAss("ep.ass", "Hello there", "How are you");
        String origemAntes = Files.readString(entrada, StandardCharsets.UTF_8);
        // Ponto de falha determinístico e multiplataforma: o destino da publicação
        // é diretorioSaida()/ep_PT-BR.ass; criar diretorioSaida como ARQUIVO faz o
        // Files.createDirectories do escritor lançar IOException na publicação.
        Path saidaComoArquivo = raiz.resolve("saida");
        Files.createDirectories(saidaComoArquivo.getParent());
        Files.writeString(saidaComoArquivo, "isto e um arquivo, nao uma pasta");

        assertThrows(ArquivoLegendaException.class, () -> uc.processar(entrada, false));

        assertTrue(Files.isRegularFile(saidaComoArquivo), "saida continua sendo o arquivo original");
        assertEquals("isto e um arquivo, nao uma pasta",
            Files.readString(saidaComoArquivo, StandardCharsets.UTF_8), "conteudo existente intacto");
        assertEquals(origemAntes, Files.readString(entrada, StandardCharsets.UTF_8), "entrada intacta");
        assertFalse(Files.exists(raiz.resolve("cache").resolve("AnimeTeste").resolve("ep.cache.json")),
            "cache nao e gravado quando a publicacao falha antes da persistencia");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o cancelamento cooperativo (thread interrompida)
     * encerra o processamento sem publicar saída final, sem chamar o LLM e sem
     * tocar arquivos já existentes, caracterizando a semântica de "Parar" da UI.
     *
     * <p>INVARIANTES DO DOMÍNIO: a interrupção detectada no laço de lotes vira
     * {@link TraducaoParcialException}; a saída final anterior permanece intacta;
     * o status de interrupção da thread é preservado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer publicação final, chamada extra
     * ao LLM ou perda do arquivo existente falha a suíte. Determinístico: a
     * interrupção é injetada no filtro {@code isTraduzivel} (após a leitura do
     * cache, antes de qualquer IO sob interrupção), sem sleep nem corrida.
     */
    @Test
    void cancelamentoEncerraSemPublicarNemChamarLlmPreservandoExistentes() throws Exception {
        FakeMistralPort llm = new FakeMistralPort();
        ConsoleUILogger interruptor = new UILoggerQueInterrompe();
        ProcessarArquivoUseCase uc = montar(llm, interruptor);
        Path entrada = escreverAss("ep.ass", "Hello there", "How are you");
        String origemAntes = Files.readString(entrada, StandardCharsets.UTF_8);
        // Saída final PT-BR pré-existente que NÃO pode ser sobrescrita pelo cancelamento.
        Path finalPtBr = Files.createDirectories(raiz.resolve("saida")).resolve("ep_PT-BR.ass");
        Files.writeString(finalPtBr, "VERSAO ANTERIOR PRESERVADA");

        boolean interrompida = false;
        try {
            assertThrows(TraducaoParcialException.class, () -> uc.processar(entrada, false));
            interrompida = Thread.currentThread().isInterrupted();
        } finally {
            // Limpa o status de interrupção para não vazar para os demais testes.
            Thread.interrupted();
        }

        assertTrue(interrompida, "o status de interrupcao da thread deve ser preservado");
        assertEquals(0, llm.chamadas.get(), "cancelamento antes do lote nao pode chamar o LLM");
        assertEquals("VERSAO ANTERIOR PRESERVADA",
            Files.readString(finalPtBr, StandardCharsets.UTF_8), "saida final anterior preservada");
        assertFalse(Files.exists(raiz.resolve("saida").resolve("ep_PT-BR.parcial.ass")),
            "nada e publicado como parcial neste cancelamento sem progresso");
        assertEquals(origemAntes, Files.readString(entrada, StandardCharsets.UTF_8), "entrada intacta");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: dublê do detector de karaokê que dispara uma
     * interrupção cooperativa na primeira avaliação de {@code isTraduzivel},
     * reproduzindo de forma determinística o clique em "Parar" durante a
     * preparação do episódio.
     *
     * <p>INVARIANTES DO DOMÍNIO: interrompe uma única vez e delega o restante ao
     * comportamento real, sem alterar a classificação das falas.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; apenas marca o status de
     * interrupção da thread corrente.
     */
    private static final class KaraokeQueInterrompe extends DetectorEfeitoKaraokeService {
        private boolean interrompeu = false;

        @Override
        public boolean devePreservarKaraokeOriginal(String estilo, String texto) {
            if (!interrompeu) {
                interrompeu = true;
                Thread.currentThread().interrupt();
            }
            return super.devePreservarKaraokeOriginal(estilo, texto);
        }
    }
}
