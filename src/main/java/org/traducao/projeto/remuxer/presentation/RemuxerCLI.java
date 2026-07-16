package org.traducao.projeto.remuxer.presentation;

import org.traducao.projeto.config.ExecucaoCli;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.traducao.projeto.remuxer.application.RemuxarLoteUseCase;
import org.traducao.projeto.remuxer.domain.RelatorioRemux;
import org.traducao.projeto.remuxer.presentation.ui.ConsoleRemuxerLogger;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.core.presentation.ui.AnsiCores;
import org.traducao.projeto.core.presentation.ui.ConsoleEntrada;
import org.traducao.projeto.traducao.presentation.ui.PastasExecucao;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: oferece execução local por terminal da mesma etapa de
 * remux usada na interface web.
 *
 * <p>INVARIANTES DO DOMÍNIO: valida pastas antes do lote e imprime o status real
 * consolidado, sem anunciar sucesso quando existem pendências ou falhas.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: configuração/pasta inválida encerra sem
 * criar saída; falhas do lote permanecem no relatório final.
 */
@Component
@ConditionalOnProperty(name = "app.modo", havingValue = "REMUXAR")
public class RemuxerCLI implements ExecucaoCli {

    private final RemuxarLoteUseCase remuxarLoteUseCase;
    private final ConsoleRemuxerLogger logger;
    private final PastasExecucao pastasExecucao;
    private final TradutorProperties propriedades;

    public RemuxerCLI(RemuxarLoteUseCase remuxarLoteUseCase, ConsoleRemuxerLogger logger,
                       PastasExecucao pastasExecucao, TradutorProperties propriedades) {
        this.remuxarLoteUseCase = remuxarLoteUseCase;
        this.logger = logger;
        this.pastasExecucao = pastasExecucao;
        this.propriedades = propriedades;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: coleta configuração CLI, executa o lote e apresenta
     * contadores e status final.
     * INVARIANTES DO DOMÍNIO: o fluxo CLI usa as mesmas proteções do caso de uso.
     * COMPORTAMENTO EM CASO DE FALHA: retorna ao processo após imprimir causa e
     * não força código de sucesso textual.
     */
    @Override
    public void executar() {
        logger.cabecalho("PROCESSAMENTO MULTIPLEXAR EM SEGUNDO PLANO");

        if (propriedades.diretorioEntrada() == null || propriedades.diretorioEntrada().isBlank()) {
            logger.erro("Pasta de vídeos não configurada.");
            ConsoleEntrada.imprimirErroSaida();
            return;
        }
        pastasExecucao.configurar(propriedades.diretorioEntrada(), propriedades.diretorioSaida(), propriedades.diretorioCache(), propriedades);

        Path pastaVideos = pastasExecucao.diretorioEntrada();
        Path pastaLegendas = pastasExecucao.diretorioSaida(); // No remuxer, o que era saída virou legendas

        if (!Files.isDirectory(pastaVideos)) {
            logger.erro("Pasta de vídeos não existe ou não é um diretório: " + pastaVideos);
            return;
        }
        if (!Files.isDirectory(pastaLegendas)) {
            logger.erro("Pasta de legendas PTBR não existe ou não é um diretório: " + pastaLegendas);
            return;
        }

        logger.info("Pasta de vídeos: " + pastaVideos);
        logger.info("Pasta de legendas: " + pastaLegendas);

        RelatorioRemux relatorio = remuxarLoteUseCase.executar(pastaVideos, pastaLegendas);

        logger.cabecalho("RELATÓRIO FINAL DE MULTIPLEXAÇÃO INDUSTRIAL");
        System.out.printf("  Arquivos MKV Detectados     : %d%n", relatorio.getMkvDetectados());
        System.out.printf("  Legendas PTBR Encontradas   : %d%n", relatorio.getLegendasPareadas());
        System.out.printf("  Multiplexados com Sucesso   : %s%n",
            AnsiCores.colorir(String.valueOf(relatorio.getMkvProcessadosSucesso()), AnsiCores.GREEN));
        System.out.printf("  Arquivos Ignorados (Sem Sub): %s%n",
            AnsiCores.colorir(String.valueOf(relatorio.getArquivosIgnorados()), AnsiCores.YELLOW));
        System.out.printf("  Volume de Dados Gravados    : %.3f GB%n", relatorio.getBytesMkvGeradosTotal() / (1024.0 * 1024.0 * 1024.0));
        System.out.println("================================================================================");
        System.out.println(AnsiCores.colorir(String.format("  Erros de Infraestrutura     : %d", relatorio.getErrosInfraestrutura()), AnsiCores.RED));
        System.out.println(AnsiCores.colorir(String.format("  Erros de Mkvmerge Runtime   : %d", relatorio.getErrosMkvmergeRuntime()), AnsiCores.RED));
        System.out.println(AnsiCores.colorir(String.format("  Erros de Permissão de I/O   : %d", relatorio.getErrosPermissaoIo()), AnsiCores.RED));
        System.out.println(AnsiCores.colorir(String.format("  Erros Inesperados/Hardware  : %d", relatorio.getErrosInesperados()), AnsiCores.RED));
        System.out.printf("  Vídeos sem legenda          : %d%n", relatorio.getVideosSemLegenda());
        System.out.printf("  Pareamentos ambíguos        : %d%n", relatorio.getPareamentosAmbiguos());
        System.out.printf("  Saídas existentes preservadas: %d%n", relatorio.getSaidasJaExistentes());
        System.out.printf("  Status Final                : %s%n", relatorio.getStatusFinal());
        System.out.println("================================================================================");
        if ("CONCLUIDO".equals(relatorio.getStatusFinal())) {
            logger.sucesso("Esteira finalizada e validada com sucesso!");
        } else if ("CONCLUIDO_COM_PENDENCIAS".equals(relatorio.getStatusFinal())
                || "SEM_ARQUIVOS".equals(relatorio.getStatusFinal())) {
            logger.aviso("Esteira encerrada com pendências; revise os contadores acima.");
        } else {
            logger.erro("Esteira encerrada sem sucesso completo: " + relatorio.getStatusFinal());
        }
    }
}
