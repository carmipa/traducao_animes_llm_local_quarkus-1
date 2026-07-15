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
import org.traducao.projeto.traducao.infrastructure.cache.ProvenienciaCache;
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
        private final boolean interromperNaPrimeira;

        FakeMistralPort() {
            this(false);
        }

        FakeMistralPort(boolean interromperNaPrimeira) {
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
                .map(FakeMistralPort::traduzirLinha)
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
        TelemetriaTraducaoPort telemetria = new TelemetriaTraducaoPort() {
            @Override public void registrarTraducao(TelemetriaTraducao t) {}
            @Override public void registrarAlucinacaoPrevenida() {}
            @Override public void registrarRespostaTraducaoRejeitada() {}
            @Override public void registrarFalhaTraducaoRecuperada() {}
            @Override public void registrarFallbackMantido() {}
        };
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
        FakeMistralPort llm = new FakeMistralPort();
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
        FakeMistralPort llm = new FakeMistralPort(true);
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
}
