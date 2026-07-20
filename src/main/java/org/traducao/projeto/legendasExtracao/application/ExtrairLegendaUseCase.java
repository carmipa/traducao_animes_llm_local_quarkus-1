package org.traducao.projeto.legendasExtracao.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.legendasExtracao.application.strategy.ExtratorStrategy;
import org.traducao.projeto.legendasExtracao.domain.ExtratorException;
import org.traducao.projeto.legendasExtracao.domain.FaixaLegenda;
import org.traducao.projeto.legendasExtracao.domain.FormatoLegenda;
import org.traducao.projeto.legendasExtracao.domain.ItemExtracao;
import org.traducao.projeto.legendasExtracao.domain.RelatorioExtracao;
import org.traducao.projeto.legendasExtracao.domain.exceptions.ExtracaoTimeoutException;
import org.traducao.projeto.legendasExtracao.domain.ports.ExtratorVideoPort;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * PROPÓSITO DE NEGÓCIO: Orquestra a extração de softsubs de vídeos — recebe um
 * arquivo ou pasta, o formato desejado (ASS/SRT/PGS) e a pasta de saída,
 * localiza a faixa daquele formato e a extrai sem conversão, preservando
 * timestamps, estilos e conteúdo. Delega a leitura do contêiner aos adaptadores
 * ({@link ExtratorVideoPort}) e a escolha da faixa às strategies
 * ({@link ExtratorStrategy}).
 *
 * <p>INVARIANTES DO DOMÍNIO: extrai exatamente o formato pedido, sem fallback
 * para outro; nunca sobrescreve arquivo de saída existente; só publica resultado
 * validado (existe, não-vazio, formato correto); cada vídeo gera um item no
 * relatório e é contabilizado na telemetria.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: falhas por vídeo são isoladas — o item vira
 * {@code FALHA}/{@code TIMEOUT}/{@code FAIXA_NAO_ENCONTRADA} e o loop segue; o
 * parcial ({@code .part}) é removido. Só a ausência da pasta de entrada ou a
 * falta de strategy para o formato abortam a execução com {@link ExtratorException}.
 */
@Service
public class ExtrairLegendaUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExtrairLegendaUseCase.class);

    private final List<ExtratorVideoPort> adaptadoresVideo;
    private final List<ExtratorStrategy> strategies;
    private final TelemetriaService telemetriaService;

    public ExtrairLegendaUseCase(
            List<ExtratorVideoPort> adaptadoresVideo,
            List<ExtratorStrategy> strategies,
            TelemetriaService telemetriaService) {
        this.adaptadoresVideo = adaptadoresVideo;
        this.strategies = strategies;
        this.telemetriaService = telemetriaService;
    }

    public RelatorioExtracao executar(Path pastaVideos, FormatoLegenda formato) {
        return executar(pastaVideos, null, formato);
    }

    public RelatorioExtracao executar(Path pastaVideos, Path pastaSaidaCustomizada, FormatoLegenda formato) {
        long inicioMs = System.currentTimeMillis();
        RelatorioExtracao relatorio = new RelatorioExtracao(formato);

        if (!Files.exists(pastaVideos)) {
            throw new ExtratorException("Pasta de vídeos ou arquivo não existe: " + pastaVideos);
        }

        ExtratorStrategy strategy = strategies.stream()
                .filter(s -> s.suporta(formato))
                .findFirst()
                .orElseThrow(() -> new ExtratorException("Nenhuma estratégia suporta o formato " + formato));

        Path pastaSaida = (pastaSaidaCustomizada != null && !pastaSaidaCustomizada.toString().isBlank())
                ? pastaSaidaCustomizada
                : (Files.isDirectory(pastaVideos) 
                    ? pastaVideos.resolve("legendas_extraidas_" + formato.name().toLowerCase())
                    : (pastaVideos.getParent() != null ? pastaVideos.getParent().resolve("legendas_extraidas_" + formato.name().toLowerCase()) : Path.of("legendas_extraidas_" + formato.name().toLowerCase())));
        try {
            Files.createDirectories(pastaSaida);
        } catch (IOException e) {
            throw new ExtratorException("Falha ao criar pasta de saída: " + pastaSaida, e);
        }

        List<Path> videos = encontrarVideos(pastaVideos);

        Set<ExtratorVideoPort> adaptadoresEmUso = new HashSet<>();
        videos.forEach(v -> resolverAdaptador(v).ifPresent(adaptadoresEmUso::add));
        adaptadoresEmUso.forEach(ExtratorVideoPort::validarInfraestrutura);

        int total = videos.size();
        int indice = 0;
        for (Path video : videos) {
            indice++;
            relatorio.registrarDetectado();
            String nomeVideo = video.getFileName().toString();
            // Progresso ARQUIVO A ARQUIVO ao vivo no console web: System.out é
            // redirecionado para o canal SSE 'extracao' (ver ConsoleRedirector +
            // PipelineWebSupport). log.* vai só para o terminal/arquivo do servidor,
            // por isso a tela ficava muda durante o lote.
            System.out.printf(">> [%d/%d] Extraindo legenda: %s%n", indice, total, nomeVideo);
            log.debug("Processando {}", nomeVideo);

            ExtratorVideoPort adaptador = resolverAdaptador(video).orElseThrow();

            try {
                List<FaixaLegenda> faixas = adaptador.identificarFaixas(video);
                relatorio.registrarFaixasEncontradas(faixas.size());
                Optional<FaixaLegenda> faixaAlvo = strategy.selecionarMelhorFaixa(faixas);

                if (faixaAlvo.isEmpty()) {
                    relatorio.registrarSemLegenda();
                    relatorio.adicionarItem(ItemExtracao.semFaixa(nomeVideo, formato.name()));
                    System.out.printf("   [SEM FAIXA %s] %s%n", formato.name(), nomeVideo);
                    log.warn("Nenhuma faixa {} encontrada no vídeo: {}", formato, nomeVideo);
                    continue;
                }

                extrairFaixaSelecionada(adaptador, video, nomeVideo, faixaAlvo.get(), formato, pastaSaida, relatorio);
            } catch (ExtracaoTimeoutException e) {
                relatorio.registrarTimeout();
                relatorio.adicionarItem(ItemExtracao.falha(nomeVideo, formato.name(), null,
                    "Timeout ao identificar faixas"));
                System.out.printf("   [TIMEOUT] %s — ao identificar faixas%n", nomeVideo);
                log.error("Timeout ao identificar faixas em {}: {}", nomeVideo, e.getMessage());
            } catch (ExtratorException e) {
                relatorio.registrarFalha();
                relatorio.adicionarItem(ItemExtracao.falha(nomeVideo, formato.name(), null, e.getMessage()));
                System.out.printf("   [FALHA] %s — %s%n", nomeVideo, e.getMessage());
                log.error("Falha ao processar {}: {}", nomeVideo, e.getMessage());
            } catch (Exception e) {
                relatorio.registrarFalha();
                relatorio.adicionarItem(ItemExtracao.falha(nomeVideo, formato.name(), null, e.getMessage()));
                System.out.printf("   [ERRO] %s — %s%n", nomeVideo, e.getMessage());
                log.error("Erro inesperado em {}: {}", nomeVideo, e.getMessage(), e);
            }
        }

        // Telemetria: vídeos processados (arquivosProcessados), faixas encontradas
        // (itensDetectados) e extraídas com sucesso (itensCorrigidos); formato,
        // falhas e timeouts vão no detalhe para o painel exibir a íntegra.
        String detalheTelemetria = String.format(
            "%s | %s | faixas: %d, extraídas: %d, sem faixa: %d, já existiam: %d, falhas: %d, timeouts: %d",
            pastaVideos.toAbsolutePath(), formato.name(),
            relatorio.getFaixasEncontradas(), relatorio.getLegendasExtraidas(),
            relatorio.getArquivosSemLegenda(), relatorio.getArquivosJaExistentes(),
            relatorio.getFalhasInesperadas(), relatorio.getTimeouts());

        telemetriaService.registrarOperacao(TelemetriaService.criarOperacao(
            "Extracao de Legendas (" + formato.name() + ")",
            detalheTelemetria,
            System.currentTimeMillis() - inicioMs,
            relatorio.getArquivosDetectados(),
            relatorio.getFaixasEncontradas(),
            relatorio.getLegendasExtraidas()
        ));

        return relatorio;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: Extrai a faixa já escolhida para o arquivo final,
     * garantindo entrega íntegra — nunca sobrescreve arquivo existente e nunca
     * publica um resultado que não passou na validação de formato.
     *
     * <p>INVARIANTES DO DOMÍNIO: (1) se o destino final já existe, o vídeo é
     * marcado {@code JA_EXISTE} e nada é gravado; (2) a extração ocorre primeiro
     * num arquivo temporário {@code .part}; (3) só após validar existência,
     * tamanho e formato o temporário é movido (atômico) para o nome final.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer erro/timeout durante a extração
     * ou validação remove o parcial ({@code .part}) e registra o item como
     * {@code FALHA} ou {@code TIMEOUT} — o destino final nunca fica com lixo.
     */
    private void extrairFaixaSelecionada(
            ExtratorVideoPort adaptador, Path video, String nomeVideo, FaixaLegenda faixa,
            FormatoLegenda formato, Path pastaSaida, RelatorioExtracao relatorio) {

        String nomeBase = nomeVideo.replaceFirst("[.][^.]+$", "");
        String arquivoSaida = nomeBase + "_Track" + faixa.id() + "." + formato.getExtensaoSaida();
        Path caminhoFinal = pastaSaida.resolve(arquivoSaida);

        if (Files.exists(caminhoFinal)) {
            relatorio.registrarJaExiste();
            relatorio.adicionarItem(ItemExtracao.jaExiste(nomeVideo, formato.name(), faixa.id(), arquivoSaida));
            System.out.printf("   [JÁ EXISTE] %s -> %s%n", nomeVideo, arquivoSaida);
            log.warn("Arquivo de saída já existe; extração ignorada para não sobrescrever: {}", caminhoFinal);
            return;
        }

        Path caminhoTemp = pastaSaida.resolve(arquivoSaida + ".part");
        limparParcial(caminhoTemp);

        try {
            adaptador.extrairTrilha(video, faixa.id(), caminhoTemp);
            ValidadorSaidaExtracao.validar(caminhoTemp, formato);
            moverParaFinal(caminhoTemp, caminhoFinal);
            relatorio.registrarExtraido();
            relatorio.adicionarItem(ItemExtracao.sucesso(nomeVideo, formato.name(), faixa.id(), arquivoSaida));
            System.out.printf("   [OK] %s -> %s (Track %d)%n", nomeVideo, arquivoSaida, faixa.id());
        } catch (ExtracaoTimeoutException e) {
            limparParcial(caminhoTemp);
            relatorio.registrarTimeout();
            relatorio.adicionarItem(ItemExtracao.timeout(nomeVideo, formato.name(), faixa.id()));
            System.out.printf("   [TIMEOUT] %s (Track %d)%n", nomeVideo, faixa.id());
            log.error("Timeout ao extrair {} (Track {}): {}", nomeVideo, faixa.id(), e.getMessage());
        } catch (ExtratorException e) {
            limparParcial(caminhoTemp);
            relatorio.registrarFalha();
            relatorio.adicionarItem(ItemExtracao.falha(nomeVideo, formato.name(), faixa.id(), e.getMessage()));
            System.out.printf("   [FALHA] %s (Track %d) — %s%n", nomeVideo, faixa.id(), e.getMessage());
            log.error("Falha ao extrair {} (Track {}): {}", nomeVideo, faixa.id(), e.getMessage());
        }
    }

    private void moverParaFinal(Path temp, Path destino) {
        try {
            try {
                Files.move(temp, destino, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, destino);
            }
        } catch (IOException e) {
            throw new ExtratorException("Falha ao mover a legenda extraída para o destino final: " + destino, e);
        }
    }

    private void limparParcial(Path temp) {
        try {
            Files.deleteIfExists(temp);
        } catch (IOException e) {
            log.warn("Não foi possível remover o arquivo parcial {}: {}", temp, e.getMessage());
        }
    }

    private List<Path> encontrarVideos(Path entrada) {
        if (Files.isRegularFile(entrada)) {
            return resolverAdaptador(entrada).isPresent() ? List.of(entrada) : List.of();
        }

        if (!Files.isDirectory(entrada)) {
            throw new ExtratorException("Pasta de vídeos não existe ou não é um diretório: " + entrada);
        }

        try (Stream<Path> walk = Files.walk(entrada)) {
            return walk
                .filter(Files::isRegularFile)
                .filter(p -> resolverAdaptador(p).isPresent())
                .sorted()
                .toList();
        } catch (IOException e) {
            throw new ExtratorException("Falha ao ler o diretório " + entrada, e);
        }
    }

    private Optional<ExtratorVideoPort> resolverAdaptador(Path video) {
        return adaptadoresVideo.stream().filter(a -> a.suporta(video)).findFirst();
    }
}
