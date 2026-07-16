package org.traducao.projeto.analisadorMidia.presentation;

import org.traducao.projeto.config.ExecucaoCli;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.traducao.projeto.analisadorMidia.application.AnalisarMidiaUseCase;
import org.traducao.projeto.analisadorMidia.domain.AuditoriaResultado;
import org.traducao.projeto.analisadorMidia.presentation.ui.ConsoleAnalisadorLogger;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.core.presentation.ui.ConsoleEntrada;
import org.traducao.projeto.traducao.presentation.ui.PastasExecucao;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.modo", havingValue = "ANALISAR")
public class AnalisadorMidiaCLI implements ExecucaoCli {

    private final AnalisarMidiaUseCase analisarMidiaUseCase;
    private final ConsoleAnalisadorLogger logger;
    private final PastasExecucao pastasExecucao;
    private final TradutorProperties propriedades;

    public AnalisadorMidiaCLI(AnalisarMidiaUseCase analisarMidiaUseCase, ConsoleAnalisadorLogger logger,
                              PastasExecucao pastasExecucao, TradutorProperties propriedades) {
        this.analisarMidiaUseCase = analisarMidiaUseCase;
        this.logger = logger;
        this.pastasExecucao = pastasExecucao;
        this.propriedades = propriedades;
    }

    @Override
    public void executar() {
        logger.cabecalhoGrande("ESTEIRA DE AUDITORIA E ANÁLISE TÉCNICA DE MÍDIA");

        if (propriedades.diretorioEntrada() == null || propriedades.diretorioEntrada().isBlank()) {
            logger.erro("Caminho de entrada (pasta ou arquivo) não configurado.");
            ConsoleEntrada.imprimirErroSaida();
            return;
        }

        // Configura pastas da execução
        pastasExecucao.configurar(
            propriedades.diretorioEntrada(),
            propriedades.diretorioSaida(),
            propriedades.diretorioCache(),
            propriedades
        );

        Path entrada = pastasExecucao.diretorioEntrada();
        logger.info("Verificando caminho informado: " + entrada.toAbsolutePath());

        if (!Files.exists(entrada)) {
            logger.erro("O caminho de entrada informado não existe: " + entrada);
            return;
        }

        try {
            // Executa a auditoria
            List<AuditoriaResultado> resultados = analisarMidiaUseCase.executar(
                entrada,
                propriedades.diretorioSaida() != null && !propriedades.diretorioSaida().isBlank()
                    ? pastasExecucao.diretorioSaida()
                    : null
            ).resultados();

            // Imprime o relatório colorido na tela para cada arquivo analisado
            for (AuditoriaResultado res : resultados) {
                logger.imprimirResultado(res);
            }

            logger.sucesso("Auditoria de mídia concluída! Resultado exibido na tela; "
                + "a exportação TXT é manual e a telemetria técnica foi persistida internamente.");

        } catch (Exception e) {
            logger.erro("Falha durante o processamento da auditoria de mídias: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
