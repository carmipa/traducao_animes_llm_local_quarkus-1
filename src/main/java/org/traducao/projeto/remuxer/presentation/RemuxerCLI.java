package org.traducao.projeto.remuxer.presentation;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.traducao.projeto.config.ExecucaoCli;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.traducao.projeto.remuxer.application.RemuxarLoteUseCase;
import org.traducao.projeto.remuxer.domain.RelatorioRemux;
import org.traducao.projeto.remuxer.presentation.ui.ConsoleRemuxerLogger;
import org.traducao.projeto.core.presentation.ui.AnsiCores;
import org.traducao.projeto.core.presentation.ui.ConsoleEntrada;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

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

    @ConfigProperty(name = "tradutor.diretorio-entrada")
    Optional<String> diretorioEntrada;

    @ConfigProperty(name = "tradutor.diretorio-saida")
    Optional<String> diretorioSaida;

    public RemuxerCLI(RemuxarLoteUseCase remuxarLoteUseCase, ConsoleRemuxerLogger logger) {
        this.remuxarLoteUseCase = remuxarLoteUseCase;
        this.logger = logger;
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

        Path pastaVideos = resolverEntrada(diretorioEntrada);
        if (pastaVideos == null) {
            logger.erro("Pasta de vídeos não configurada.");
            ConsoleEntrada.imprimirErroSaida();
            return;
        }

        Path pastaLegendas = resolverDiretorioSaida(pastaVideos, diretorioSaida); // No remuxer, o que era saída virou legendas

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

    /**
     * PROPÓSITO DE NEGÓCIO: normaliza a pasta de vídeos configurada exatamente como o
     * fluxo legado fazia (via {@code PastasExecucao}), aplicando {@code trim}.
     * <p>INVARIANTES DO DOMÍNIO: ausente, vazia ou só com espaços ⇒ {@code null}
     * (entrada inválida); valor útil ⇒ {@code Path.of(valor.trim())}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: retorno {@code null} sinaliza entrada não
     * configurada, encerrando a execução sem exceção.
     */
    static Path resolverEntrada(Optional<String> diretorioEntrada) {
        return diretorioEntrada
            .filter(valor -> !valor.isBlank())
            .map(valor -> Path.of(valor.trim()))
            .orElse(null);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: resolve a pasta de legendas PTBR do remux reproduzindo,
     * como duplicação consciente e autorizada na E4b, a política legada
     * {@code TradutorProperties.resolverDiretorioSaida()} que o remuxer herdava:
     * usa a saída explícita quando informada, senão cai no fallback
     * {@code entrada/traducao_ptbr}.
     * <p>INVARIANTES DO DOMÍNIO: saída ausente, vazia ou só com espaços ⇒
     * {@code pastaVideos.resolve("traducao_ptbr")}; saída útil ⇒
     * {@code Path.of(valor.trim())}. A normalização por {@code trim} é preservada.
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca devolve {@code null} — sempre há uma
     * pasta de legendas resolvida (explícita ou fallback).
     */
    static Path resolverDiretorioSaida(Path pastaVideos, Optional<String> diretorioSaida) {
        return diretorioSaida
            .filter(valor -> !valor.isBlank())
            .map(valor -> Path.of(valor.trim()))
            .orElseGet(() -> pastaVideos.resolve("traducao_ptbr"));
    }
}
