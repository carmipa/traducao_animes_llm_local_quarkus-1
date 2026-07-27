package org.traducao.projeto.traducaoCorrige.domain.ports;

import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: publica o que uma operação de manutenção de cache fez — para a telemetria,
 * para o log e para o relatório persistido. Sem isto a operação é INVISÍVEL: roda, muda o acervo e
 * não deixa rastro comparável ao das outras operações do pipeline.
 *
 * <p>A ausência não era acidente. O reforço de terminologia nasceu sem telemetria porque injetar
 * {@code telemetria.TelemetriaService} direto na camada de aplicação criaria a mesma dívida que a
 * FASE 2 do Plano-Mestre existe para remover, e o congelamento de fronteira reprovaria — com razão.
 * Adiar, porém, comprou o defeito oposto: uma operação que reescreve trabalho pronto e não aparece
 * em lugar nenhum. Esta porta é a saída certa para os dois problemas.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>NENHUM tipo da fatia {@code telemetria} aparece nesta assinatura. A aplicação fala o
 *       vocabulário do próprio domínio; traduzir para o formato da telemetria é trabalho do
 *       adaptador. É isso que permite testar a aplicação sem a fatia de telemetria existir.</li>
 *   <li>É unidirecional e não devolve nada: publicar não pode virar fonte de decisão.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Implementações ABSORVEM falha de I/O. Perder uma linha de telemetria nunca pode interromper a
 * manutenção do acervo nem propagar exceção para o caso de uso — o inverso trocaria um problema de
 * observabilidade por um de integridade.
 */
public interface TelemetriaCorrecaoPort {

    /**
     * PROPÓSITO DE NEGÓCIO: registra uma operação concluída, com os totais e o relatório legível.
     *
     * <p>INVARIANTES DO DOMÍNIO: chamado UMA vez por execução, no fim; os números são os mesmos
     * que o caso de uso devolve ao chamador, para relatório e retorno nunca discordarem.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     *
     * @param operacao nome da operação, como aparece na telemetria (ex.: "Reforço de Terminologia")
     * @param detalhe resumo curto do desfecho (status, pendências, falhas)
     * @param prefixoRelatorio prefixo do arquivo de relatório persistido (ex.: "reforco_terminologia")
     * @param pastaAlvo pasta sobre a qual a operação rodou
     * @param duracaoMs duração total da execução
     * @param arquivosProcessados arquivos de cache analisados
     * @param itensDetectados falas em que havia terminologia a restaurar
     * @param itensCorrigidos falas efetivamente alteradas (zero em ensaio)
     * @param relatorio texto completo do relatório, já formatado
     */
    void registrar(
        String operacao,
        String detalhe,
        String prefixoRelatorio,
        Path pastaAlvo,
        long duracaoMs,
        int arquivosProcessados,
        int itensDetectados,
        int itensCorrigidos,
        String relatorio);
}
