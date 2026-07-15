package org.traducao.projeto.traducao.presentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.config.ExecucaoCli;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.application.ProcessarArquivoUseCase;
import org.traducao.projeto.traducao.domain.StatusLlm;
import org.traducao.projeto.traducao.domain.exceptions.TradutorException;
import org.traducao.projeto.traducao.domain.exceptions.TraducaoParcialException;
import org.traducao.projeto.traducao.domain.ports.MistralPort;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.traducao.presentation.ui.ConsoleEntrada;
import org.traducao.projeto.traducao.presentation.ui.ConsoleUILogger;
import org.traducao.projeto.traducao.presentation.ui.PastasExecucao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Ponto de entrada da CLI: varre a pasta de entrada por arquivos .ass/.ssa
 * e traduz cada um sequencialmente.
 * <p>
 * O bootstrap da aplicação é implícito, administrado pelo Quarkus/CDI — não há
 * classe {@code main} própria. Quando {@code tradutor.diretorio-entrada} não é
 * informado por configuração, os caminhos são pedidos via {@link ConsoleEntrada}.
 * <p>
 * Arquivos são processados um por vez de propósito: todos compartilham o
 * mesmo LLM local (GPU única). Lotes dentro de cada episódio também são
 * sequenciais (ver {@code ProcessarEpisodioUseCase}).
 */
@Component
@ConditionalOnProperty(name = "app.modo", havingValue = "TRADUZIR", matchIfMissing = true)
public class TradutorCLI implements ExecucaoCli {

    private static final Logger log = LoggerFactory.getLogger(TradutorCLI.class);
    private static final Set<String> EXTENSOES_SUPORTADAS = Set.of(".ass", ".ssa");

    private final ProcessarArquivoUseCase processarArquivoUseCase;
    private final ConsoleUILogger uiLogger;
    private final TradutorProperties propriedades;
    private final PastasExecucao pastasExecucao;
    private final MistralPort mistralPort;

    public TradutorCLI(
        ProcessarArquivoUseCase processarArquivoUseCase,
        ConsoleUILogger uiLogger,
        TradutorProperties propriedades,
        PastasExecucao pastasExecucao,
        MistralPort mistralPort
    ) {
        this.processarArquivoUseCase = processarArquivoUseCase;
        this.uiLogger = uiLogger;
        this.propriedades = propriedades;
        this.pastasExecucao = pastasExecucao;
        this.mistralPort = mistralPort;
    }

    @Override
    public void executar() throws Exception {
        if (!resolverPastas()) {
            return;
        }

        uiLogger.log("Iniciando Tradutor Local...");

        Path diretorioEntrada = pastasExecucao.diretorioEntrada();
        if (!Files.isDirectory(diretorioEntrada)) {
            log.error("Pasta de entrada não existe ou não é um diretório: {}", diretorioEntrada);
            uiLogger.log("❌ Pasta de entrada não existe ou não é um diretório: " + diretorioEntrada);
            return;
        }

        if (!verificarLlmDisponivel()) {
            return;
        }

        List<Path> arquivos;
        try (Stream<Path> stream = Files.list(diretorioEntrada)) {
            arquivos = stream
                .filter(Files::isRegularFile)
                .filter(this::temExtensaoSuportada)
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .toList();
        } catch (IOException e) {
            log.error("Falha ao listar arquivos em {}", diretorioEntrada, e);
            uiLogger.log("❌ Falha ao listar arquivos em " + diretorioEntrada + ": " + e.getMessage());
            return;
        }

        if (arquivos.isEmpty()) {
            log.warn("Nenhum arquivo .ass/.ssa encontrado em {}", diretorioEntrada);
            uiLogger.log("Nenhum arquivo .ass/.ssa encontrado em " + diretorioEntrada);
            return;
        }

        log.info("{} arquivo(s) de legenda encontrado(s) em {}", arquivos.size(), diretorioEntrada);
        uiLogger.log(arquivos.size() + " arquivo(s) encontrado(s). Iniciando tradução...");

        int sucesso = 0;
        int falha = 0;
        List<String> arquivosComFalha = new ArrayList<>();
        for (int i = 0; i < arquivos.size(); i++) {
            Path arquivo = arquivos.get(i);
            uiLogger.tituloEpisodio(arquivo.getFileName().toString(), i + 1, arquivos.size());
            try {
                processarArquivoUseCase.processar(arquivo);
                uiLogger.log("[ OK ] " + arquivo.getFileName() + " traduzido com sucesso.");
                sucesso++;
            } catch (TraducaoParcialException e) {
                int salvas = e.getDicionarioParcial() != null ? e.getDicionarioParcial().size() : 0;
                log.warn("Processamento parcial em {}: {} traduções salvas. {}", arquivo.getFileName(), salvas, e.getMessage());
                uiLogger.log("[ PARCIAL ] " + arquivo.getFileName() + " (Salvas: " + salvas + " antes de abortar)");
                falha++;
                arquivosComFalha.add(arquivo.getFileName().toString() + " (parcial: " + salvas + " salvas)");
            } catch (TradutorException e) {
                log.error("Falha crítica ao processar {}: {}", arquivo.getFileName(), e.getMessage());
                uiLogger.log("[ FAIL ] Falha em " + arquivo.getFileName() + ": " + e.getMessage());
                falha++;
                arquivosComFalha.add(arquivo.getFileName().toString());
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (e.getCause() instanceof TradutorException te) {
                    errorMsg = te.getMessage();
                }
                log.error("Erro ao processar {}: {}", arquivo.getFileName(), errorMsg);
                uiLogger.log("[ FAIL ] Erro em " + arquivo.getFileName() + " (" + errorMsg + ")");
                falha++;
                arquivosComFalha.add(arquivo.getFileName().toString());
            }
        }

        log.info("Processamento finalizado: {} sucesso(s), {} falha(s) de {} arquivo(s)", sucesso, falha, arquivos.size());
        imprimirRelatorioFinal(arquivos.size(), sucesso, falha, arquivosComFalha);
    }

    private void imprimirRelatorioFinal(int totalArquivos, int sucesso, int falha, List<String> arquivosComFalha) {
        uiLogger.log("");
        uiLogger.log("==================== RELATÓRIO FINAL ====================");
        uiLogger.log(String.format("Concluido: %d sucesso(s), %d falha(s) de %d arquivo(s).", sucesso, falha, totalArquivos));
        uiLogger.log(String.format("Falas traduzidas agora pelo LLM: %d", uiLogger.totalFalasNovas()));
        uiLogger.log(String.format("Falas reaproveitadas do cache: %d", uiLogger.totalFalasCache()));
        uiLogger.log(String.format("Avisos/revisões manuais sinalizadas: %d", uiLogger.totalAvisos()));
        if (!arquivosComFalha.isEmpty()) {
            uiLogger.log("[ WARN ] Arquivos com falha ou tradução parcial: " + String.join(", ", arquivosComFalha));
        }
        uiLogger.log("===========================================================");
    }

    private boolean verificarLlmDisponivel() {
        uiLogger.log("Verificando se o servidor LLM local esta online e com o modelo carregado em memoria...");
        StatusLlm status = mistralPort.verificarDisponibilidade();
        if (!status.modeloCarregado()) {
            uiLogger.log("[ FAIL ] " + status.mensagem());
            uiLogger.log("[ FAIL ] Abortando: inicie o servidor LLM local (ex: LM Studio), carregue o modelo "
                + "configurado em tradutor.llm.model e tente novamente.");
            return false;
        }
        uiLogger.log("[ OK ] " + status.mensagem());
        return true;
    }

    private boolean resolverPastas() {
        String entrada = propriedades.diretorioEntrada();
        String saida = propriedades.diretorioSaida();
        String cache = propriedades.diretorioCache();

        if (entrada == null || entrada.isBlank()) {
            log.error("Pasta de entrada nao configurada (informe tradutor.diretorio-entrada por configuracao ou os caminhos no console)");
            ConsoleEntrada.imprimirErroSaida();
            return false;
        }

        pastasExecucao.configurar(entrada, saida, cache, propriedades);
        log.info("Pastas: entrada={}, saída={}, cache={}",
            pastasExecucao.diretorioEntrada(),
            pastasExecucao.diretorioSaida(),
            pastasExecucao.diretorioCache());
        uiLogger.log("Entrada: " + pastasExecucao.diretorioEntrada());
        uiLogger.log("Saída: " + pastasExecucao.diretorioSaida());
        return true;
    }

    private boolean temExtensaoSuportada(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase();
        return EXTENSOES_SUPORTADAS.stream().anyMatch(nome::endsWith);
    }
}
