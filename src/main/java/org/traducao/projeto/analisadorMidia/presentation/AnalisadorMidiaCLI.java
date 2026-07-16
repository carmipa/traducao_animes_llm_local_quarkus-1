package org.traducao.projeto.analisadorMidia.presentation;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.traducao.projeto.config.ExecucaoCli;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.traducao.projeto.analisadorMidia.application.AnalisarMidiaUseCase;
import org.traducao.projeto.analisadorMidia.domain.AuditoriaResultado;
import org.traducao.projeto.analisadorMidia.presentation.ui.ConsoleAnalisadorLogger;
import org.traducao.projeto.core.presentation.ui.ConsoleEntrada;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * PROPÓSITO DE NEGÓCIO: oferece execução local por terminal da esteira de auditoria
 * e análise técnica de mídia da fatia {@code analisadorMidia}, resolvendo os caminhos
 * de que precisa (entrada obrigatória e saída opcional) a partir da própria
 * configuração, sem depender da configuração ou do estado da fatia {@code traducao}.
 *
 * <p>INVARIANTES DO DOMÍNIO: usa {@code tradutor.diretorio-entrada} e
 * {@code tradutor.diretorio-saida}; saída ausente/vazia/blank significa "sem pasta de
 * saída" (nunca aplica o fallback {@code traducao_ptbr}); não injeta cache.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: entrada não configurada ou caminho inexistente
 * encerram o fluxo sem produzir auditoria; exceções durante o processamento são
 * reportadas sem mascarar a causa.
 */
@Component
@ConditionalOnProperty(name = "app.modo", havingValue = "ANALISAR")
public class AnalisadorMidiaCLI implements ExecucaoCli {

    private final AnalisarMidiaUseCase analisarMidiaUseCase;
    private final ConsoleAnalisadorLogger logger;

    @ConfigProperty(name = "tradutor.diretorio-entrada")
    Optional<String> diretorioEntrada;

    @ConfigProperty(name = "tradutor.diretorio-saida")
    Optional<String> diretorioSaida;

    public AnalisadorMidiaCLI(AnalisarMidiaUseCase analisarMidiaUseCase, ConsoleAnalisadorLogger logger) {
        this.analisarMidiaUseCase = analisarMidiaUseCase;
        this.logger = logger;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: coleta o caminho de entrada (e a saída opcional), executa
     * a auditoria de mídia e imprime o relatório técnico por arquivo.
     * <p>INVARIANTES DO DOMÍNIO: só prossegue com entrada válida; a saída passada ao
     * caso de uso é {@code null} quando não configurada, preservando o comportamento
     * legado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada ausente/inexistente encerra sem
     * auditar; falhas de processamento são registradas com a causa.
     */
    @Override
    public void executar() {
        logger.cabecalhoGrande("ESTEIRA DE AUDITORIA E ANÁLISE TÉCNICA DE MÍDIA");

        Path entrada = resolverEntrada(diretorioEntrada);
        if (entrada == null) {
            logger.erro("Caminho de entrada (pasta ou arquivo) não configurado.");
            ConsoleEntrada.imprimirErroSaida();
            return;
        }

        logger.info("Verificando caminho informado: " + entrada.toAbsolutePath());

        if (!Files.exists(entrada)) {
            logger.erro("O caminho de entrada informado não existe: " + entrada);
            return;
        }

        try {
            List<AuditoriaResultado> resultados = analisarMidiaUseCase.executar(
                entrada,
                resolverSaidaOpcional(diretorioSaida)
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

    /**
     * PROPÓSITO DE NEGÓCIO: normaliza o caminho de entrada exatamente como o fluxo
     * legado fazia (via {@code PastasExecucao}), aplicando {@code trim}.
     * <p>INVARIANTES DO DOMÍNIO: ausente, vazia ou só com espaços ⇒ {@code null};
     * valor útil ⇒ {@code Path.of(valor.trim())}.
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
     * PROPÓSITO DE NEGÓCIO: resolve a pasta de saída OPCIONAL da auditoria, reproduzindo
     * o comportamento legado em que saída não informada resultava em {@code null}
     * (sem fallback {@code traducao_ptbr}, ao contrário do remux).
     * <p>INVARIANTES DO DOMÍNIO: ausente, vazia ou só com espaços ⇒ {@code null};
     * valor útil ⇒ {@code Path.of(valor.trim())}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code null} é um resultado válido — indica ao
     * caso de uso que não há pasta de saída dedicada.
     */
    static Path resolverSaidaOpcional(Optional<String> diretorioSaida) {
        return diretorioSaida
            .filter(valor -> !valor.isBlank())
            .map(valor -> Path.of(valor.trim()))
            .orElse(null);
    }
}
