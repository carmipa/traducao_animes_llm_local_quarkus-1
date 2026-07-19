package org.traducao.projeto.analisadorMidia.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.analisadorMidia.domain.AnalisadorException;
import org.traducao.projeto.analisadorMidia.domain.AuditoriaResultado;
import org.traducao.projeto.analisadorMidia.domain.FalhaAnalise;
import org.traducao.projeto.analisadorMidia.domain.LegendaInfo;
import org.traducao.projeto.analisadorMidia.domain.ResultadoAnaliseLote;
import org.traducao.projeto.analisadorMidia.infrastructure.adapters.FfprobeAdapter;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

/**
 * PROPÓSITO DE NEGÓCIO: orquestra a auditoria técnica de um lote de vídeos
 * (Opção 1) — localiza os arquivos, executa o ffprobe, classifica as legendas,
 * coleta sucessos e falhas, alimenta o dataset permanente de telemetria e
 * devolve o resultado ESTRUTURADO ({@link ResultadoAnaliseLote}) que é a fonte
 * única da interface HTML e da exportação TXT manual.
 *
 * <p>INVARIANTES DO DOMÍNIO: nenhum relatório é gravado em disco; a telemetria é
 * persistida internamente e é um dataset permanente (acumula/deduplica). Uma
 * falha em um arquivo não aborta o lote; a barra de progresso é cosmética e
 * nunca aborta a análise. As responsabilidades técnicas (localização,
 * classificação, formatação textual, mapeamento de telemetria) são delegadas a
 * colaboradores dedicados; este use case apenas as coordena.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: lote vazio lança {@link AnalisadorException};
 * falha por arquivo vira {@link FalhaAnalise} no resultado; falhas de telemetria
 * são registradas sem interromper a análise.
 */
@Service
public class AnalisarMidiaUseCase {

    private static final Logger log = LoggerFactory.getLogger(AnalisarMidiaUseCase.class);
    /**
     * Teto de ffprobe simultâneos. Cada ffprobe é I/O de disco independente por arquivo e
     * desacoplado do LLM/GPU; limitar evita saturar disco/CPU numa pasta grande de episódios.
     */
    private static final int LIMITE_FFPROBE_PARALELO =
        Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors()));

    private final FfprobeAdapter ffprobeAdapter;
    private final TelemetriaService telemetriaService;
    private final LocalizadorVideosService localizadorVideos;
    private final ClassificadorLegendaService classificadorLegenda;
    private final TelemetriaMidiaMapper telemetriaMidiaMapper;
    private final RelatorioMidiaTextoFormatter relatorioFormatter;

    public AnalisarMidiaUseCase(
        FfprobeAdapter ffprobeAdapter,
        TelemetriaService telemetriaService,
        LocalizadorVideosService localizadorVideos,
        ClassificadorLegendaService classificadorLegenda,
        TelemetriaMidiaMapper telemetriaMidiaMapper,
        RelatorioMidiaTextoFormatter relatorioFormatter
    ) {
        this.ffprobeAdapter = ffprobeAdapter;
        this.telemetriaService = telemetriaService;
        this.localizadorVideos = localizadorVideos;
        this.classificadorLegenda = classificadorLegenda;
        this.telemetriaMidiaMapper = telemetriaMidiaMapper;
        this.relatorioFormatter = relatorioFormatter;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: audita tecnicamente um lote de vídeos e alimenta o
     * dataset permanente de telemetria; o resultado estruturado volta para a UI.
     * NENHUM relatório é gravado em disco e NENHUMA pasta {@code relatorios/} é
     * criada junto da mídia — a exportação TXT é manual no navegador.
     *
     * <p>INVARIANTES DO DOMÍNIO: a telemetria de mídia acumula e deduplica por
     * arquivo (reanalisar a mesma mídia atualiza, não duplica); mídias de lotes
     * anteriores nunca são apagadas. Uma falha por arquivo não aborta o lote;
     * falha cosmética da barra de progresso também não.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lote vazio lança
     * {@link AnalisadorException}; cada arquivo com erro vira {@link FalhaAnalise}.
     *
     * @param entrada pasta ou arquivo de vídeo a auditar
     * @param saidaEfetiva reservado para compatibilidade; não é utilizado (a
     *                     análise não grava relatório em disco)
     */
    public ResultadoAnaliseLote executar(Path entrada, Path saidaEfetiva) {
        List<Path> arquivosAnalisar = localizadorVideos.localizar(entrada);

        if (arquivosAnalisar.isEmpty()) {
            throw new AnalisadorException("Nenhum arquivo de vídeo suportado encontrado no caminho especificado.");
        }

        log.info("Iniciando auditoria técnica para {} arquivo(s) de vídeo.", arquivosAnalisar.size());

        List<AuditoriaResultado> resultados = new ArrayList<>();
        List<FalhaAnalise> falhas = new ArrayList<>();

        BarraProgressoAnalise barra = new BarraProgressoAnalise();
        barra.iniciar(arquivosAnalisar.size(), "Analisando vídeos");
        // Fan-out do ffprobe em virtual threads com paralelismo limitado (Semaphore). Só o
        // ffprobe — I/O independente por arquivo — roda concorrente; a coleta (telemetria,
        // barra, agregação) acontece ORDENADA na thread principal, sem contenção nem corrida.
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Semaphore limite = new Semaphore(LIMITE_FFPROBE_PARALELO);
            List<Future<AuditoriaResultado>> futuros = new ArrayList<>(arquivosAnalisar.size());
            for (Path arquivo : arquivosAnalisar) {
                futuros.add(executor.submit(() -> {
                    limite.acquire();
                    try {
                        return analisarArquivo(arquivo);
                    } finally {
                        limite.release();
                    }
                }));
            }
            for (int i = 0; i < arquivosAnalisar.size(); i++) {
                Path arquivo = arquivosAnalisar.get(i);
                try {
                    AuditoriaResultado resultado = futuros.get(i).get();
                    resultados.add(resultado);
                    registrarNaTelemetria(resultado, entrada);
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause() != null ? e.getCause() : e;
                    log.error("Falha ao analisar o arquivo {}: {}", arquivo.getFileName(), causa.getMessage(), causa);
                    falhas.add(new FalhaAnalise(arquivo.getFileName().toString(), causa.getMessage()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Análise interrompida no arquivo {}", arquivo.getFileName());
                    falhas.add(new FalhaAnalise(arquivo.getFileName().toString(), "Análise interrompida"));
                } finally {
                    barra.passo();
                }
            }
        } finally {
            barra.fechar();
        }

        // Registra a operação do lote na telemetria: os dados por arquivo já vão
        // via registrarMidia; aqui entram o total analisado e as falhas.
        telemetriaService.registrarOperacao(new OperacaoTelemetria(
            "Analise de Midia",
            "Analisados: " + resultados.size() + " | Falhas: " + falhas.size(),
            null,
            arquivosAnalisar.size(),
            resultados.size(),
            0,
            Instant.now().toString()
        ));

        // A telemetria canônica já foi persistida por registrarMidia() e
        // registrarOperacao() no destino interno (logs/); não há cópia para
        // relatorios/ nem relatório gravado junto da mídia.
        return new ResultadoAnaliseLote(resultados, falhas);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: audita um único arquivo — ffprobe, classificação das
     * legendas e montagem do relatório textual, tudo a partir do domínio
     * estruturado (fonte única).
     * <p>INVARIANTES DO DOMÍNIO: o texto do relatório é derivado das legendas já
     * classificadas, sem reimplementar a classificação.
     * <p>COMPORTAMENTO EM CASO DE FALHA: exceções do ffprobe propagam ao chamador
     * (o lote as converte em {@link FalhaAnalise}).
     */
    private AuditoriaResultado analisarArquivo(Path arquivo) {
        AuditoriaResultado base = ffprobeAdapter.analisarMidia(arquivo);
        List<LegendaInfo> legendasClassificadas = classificarLegendas(base);

        AuditoriaResultado semLogs = new AuditoriaResultado(
            arquivo, base.nomeArquivo(), base.container(), base.videos(), base.audios(),
            legendasClassificadas, base.capitulos(), base.anexos(), new ArrayList<>());

        List<String> logs = relatorioFormatter.formatar(semLogs);

        return new AuditoriaResultado(
            arquivo, base.nomeArquivo(), base.container(), base.videos(), base.audios(),
            legendasClassificadas, base.capitulos(), base.anexos(), logs);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: enriquece cada faixa de legenda bruta do ffprobe com
     * a classificação técnica (tipo/categoria/traduzibilidade) e os indicadores
     * temporais informativos.
     * <p>INVARIANTES DO DOMÍNIO: a diferença de duração é apenas informativa (sem
     * veredito de sincronia); ausência de duração deixa os indicadores nulos.
     * <p>COMPORTAMENTO EM CASO DE FALHA: método puro; não lança exceção própria.
     */
    private List<LegendaInfo> classificarLegendas(AuditoriaResultado base) {
        double duracaoVideoSegundos = base.container().duracaoSegundos();
        List<LegendaInfo> classificadas = new ArrayList<>();
        for (LegendaInfo leg : base.legendas()) {
            String[] classif = classificadorLegenda.classificar(leg.codecId(), leg.formato());
            String tipoCompleto = classif[0];
            String tipoCurto = classif[1];
            String categoria = classificadorLegenda.categoria(tipoCurto);
            boolean texto = classificadorLegenda.ehTexto(tipoCurto);
            boolean bitmap = classificadorLegenda.ehBitmap(tipoCurto);

            Double duracaoLegenda = leg.duracaoSegundos();
            Double diferencaFim = (duracaoVideoSegundos > 0.0 && duracaoLegenda != null)
                ? duracaoVideoSegundos - duracaoLegenda : null;

            classificadas.add(new LegendaInfo(
                leg.index(), leg.indexRelativo(), leg.idioma(), leg.formato(), leg.codecId(), leg.titulo(),
                tipoCompleto, tipoCurto, categoria, texto, texto, bitmap,
                leg.isDefault(), leg.isForced(), leg.acessibilidade(),
                duracaoLegenda, diferencaFim));
        }
        return classificadas;
    }

    private void registrarNaTelemetria(AuditoriaResultado resultado, Path entrada) {
        telemetriaService.registrarMidia(
            telemetriaMidiaMapper.mapear(resultado, entrada, Instant.now().toString()));
    }
}
