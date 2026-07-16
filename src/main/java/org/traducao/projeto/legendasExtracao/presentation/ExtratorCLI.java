package org.traducao.projeto.legendasExtracao.presentation;

import org.springframework.beans.factory.annotation.Value;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.traducao.projeto.config.ExecucaoCli;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.traducao.projeto.core.presentation.ui.AnsiCores;
import org.traducao.projeto.core.presentation.ui.ConsoleEntrada;
import org.traducao.projeto.legendasExtracao.application.ExtrairLegendaUseCase;
import org.traducao.projeto.legendasExtracao.domain.FormatoLegenda;
import org.traducao.projeto.legendasExtracao.domain.RelatorioExtracao;
import org.traducao.projeto.legendasExtracao.presentation.ui.ConsoleExtratorLogger;
import org.traducao.projeto.legendasExtracao.presentation.ui.TabelaExtracaoRenderer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * PROPÓSITO DE NEGÓCIO: oferece execução local por terminal da esteira de extração
 * de softsubs (vídeo ➔ legenda) da fatia {@code legendasExtracao}, resolvendo o
 * único caminho de que precisa — a pasta de vídeos de entrada — a partir da própria
 * configuração, sem depender da configuração ou do estado da fatia {@code traducao}.
 *
 * <p>INVARIANTES DO DOMÍNIO: usa exclusivamente {@code tradutor.diretorio-entrada};
 * não injeta diretório de saída nem de cache; entrada ausente, vazia ou só com
 * espaços é rejeitada antes de qualquer processamento.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: entrada não configurada encerra sem processar
 * (imprime instrução de saída); pasta inexistente encerra sem produzir extração.
 */
@Component
@ConditionalOnProperty(name = "app.modo", havingValue = "EXTRAIR")
public class ExtratorCLI implements ExecucaoCli {

    private final ExtrairLegendaUseCase extrairLegendaUseCase;
    private final ConsoleExtratorLogger logger;

    @ConfigProperty(name = "tradutor.diretorio-entrada")
    Optional<String> diretorioEntrada;

    @Value("${extrator.formato:ASS}")
    private String formatoConfigurado;

    public ExtratorCLI(ExtrairLegendaUseCase extrairLegendaUseCase, ConsoleExtratorLogger logger) {
        this.extrairLegendaUseCase = extrairLegendaUseCase;
        this.logger = logger;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: coleta a pasta de vídeos configurada, executa a extração
     * no formato solicitado e apresenta o relatório consolidado.
     * <p>INVARIANTES DO DOMÍNIO: só prossegue com uma pasta de entrada válida; o
     * formato vem de {@code extrator.formato} (padrão ASS), independente da traducao.
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada ausente/vazia/blank ou pasta
     * inexistente encerram o fluxo sem anunciar sucesso.
     */
    @Override
    public void executar() {
        FormatoLegenda formato = FormatoLegenda.fromString(formatoConfigurado);
        logger.cabecalho("ESTEIRA DE EXTRAÇÃO INTELIGENTE DE SOFTSUBS: VÍDEO ➔ " + formato.name());

        Path pastaVideos = resolverEntrada(diretorioEntrada);
        if (pastaVideos == null) {
            logger.erro("Pasta de vídeos não configurada.");
            ConsoleEntrada.imprimirErroSaida();
            return;
        }
        logger.info("Analisando vídeos em: " + pastaVideos);

        if (!Files.isDirectory(pastaVideos)) {
            logger.erro("Pasta de vídeos não existe ou não é um diretório: " + pastaVideos);
            return;
        }

        RelatorioExtracao relatorio = extrairLegendaUseCase.executar(pastaVideos, formato);

        String tabela = TabelaExtracaoRenderer.render(relatorio);
        if (!tabela.isBlank()) {
            System.out.println(AnsiCores.colorir(tabela, AnsiCores.CYAN));
        }

        logger.cabecalho("RELATÓRIO DE EXTRAÇÃO");
        System.out.printf("  Total de arquivos de vídeo detectados : %d%n", relatorio.getArquivosDetectados());
        System.out.printf("  Faixas de legenda encontradas         : %d%n", relatorio.getFaixasEncontradas());
        System.out.printf("  Arquivos sem legendas %-3s         : %s%n", formato.name(),
            AnsiCores.colorir(String.valueOf(relatorio.getArquivosSemLegenda()), AnsiCores.YELLOW));
        System.out.printf("  Legendas extraídas com sucesso    : %s%n",
            AnsiCores.colorir(String.valueOf(relatorio.getLegendasExtraidas()), AnsiCores.GREEN));
        if (relatorio.getArquivosJaExistentes() > 0) {
            System.out.printf("  Preservados (já existiam)         : %s%n",
                AnsiCores.colorir(String.valueOf(relatorio.getArquivosJaExistentes()), AnsiCores.YELLOW));
        }
        if (relatorio.getFalhasInesperadas() > 0) {
            System.out.printf("  Falhas de extração                : %s%n",
                AnsiCores.colorir(String.valueOf(relatorio.getFalhasInesperadas()), AnsiCores.RED));
        }
        if (relatorio.getTimeouts() > 0) {
            System.out.printf("  Timeouts                          : %s%n",
                AnsiCores.colorir(String.valueOf(relatorio.getTimeouts()), AnsiCores.RED));
        }
        System.out.println("=".repeat(80));
        logger.sucesso("Processamento finalizado!");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: normaliza a pasta de vídeos configurada exatamente como o
     * fluxo legado fazia (via {@code PastasExecucao}), aplicando {@code trim}.
     * <p>INVARIANTES DO DOMÍNIO: ausente, vazia ou só com espaços ⇒ {@code null}
     * (entrada inválida); valor útil ⇒ {@code Path.of(valor.trim())}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: retorno {@code null} sinaliza ao chamador que
     * a entrada não está configurada, encerrando a execução sem exceção.
     */
    static Path resolverEntrada(Optional<String> diretorioEntrada) {
        return diretorioEntrada
            .filter(valor -> !valor.isBlank())
            .map(valor -> Path.of(valor.trim()))
            .orElse(null);
    }
}
