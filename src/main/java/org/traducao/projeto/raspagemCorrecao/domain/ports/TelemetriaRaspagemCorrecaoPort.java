package org.traducao.projeto.raspagemCorrecao.domain.ports;

import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: publica o desfecho de uma correção online de cache — telemetria, log e
 * relatório persistido. Sem isto a operação some do painel e do histórico, e não há como comparar
 * execuções nem saber quando o Google parou de resolver as pendências.
 *
 * <p>É a porta da FASE 2 do Plano-Mestre para esta fatia. Antes dela, {@code application} injetava
 * {@code telemetria.TelemetriaService} direto — dívida medida no diagnóstico e a mesma nas quatro
 * fatias da correção.
 *
 * <h2>Por que uma porta POR FATIA, e não uma compartilhada</h2>
 * O contrato proíbe fatia depender de fatia, e proíbe criar um módulo {@code shared/} funcional.
 * Uma porta de telemetria em cada fatia é, portanto, a forma prescrita: quatro interfaces quase
 * idênticas, DE PROPÓSITO. É o que o projeto chama de duplicação consciente, e o preço declarado
 * de manter cada fatia removível sem arrastar as vizinhas.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>NENHUM tipo da fatia {@code telemetria} aparece na assinatura: a aplicação fala o
 *       vocabulário do próprio domínio e o adaptador traduz. É isso que permite testar a
 *       aplicação sem a fatia de telemetria existir.</li>
 *   <li>Unidirecional: publicar não devolve nada e não pode virar fonte de decisão.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Implementações ABSORVEM falha de I/O — perder telemetria nunca pode interromper a correção nem
 * transformar um lote bem-sucedido em falha.
 */
public interface TelemetriaRaspagemCorrecaoPort {

    /**
     * PROPÓSITO DE NEGÓCIO: registra a operação concluída com seus totais e o relatório legível.
     *
     * <p>INVARIANTES DO DOMÍNIO: chamado UMA vez por execução, no fim; os números são os mesmos
     * que o caso de uso devolve ao chamador, para relatório e retorno nunca discordarem.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     *
     * @param operacao nome da operação, como aparece na telemetria
     * @param detalhe resumo curto do desfecho (status, pendências, falhas)
     * @param prefixoRelatorio prefixo do arquivo de relatório persistido
     * @param pastaAlvo pasta sobre a qual a operação rodou
     * @param duracaoMs duração total da execução
     * @param arquivosProcessados arquivos de cache analisados
     * @param itensDetectados entradas que precisavam de correção
     * @param itensCorrigidos entradas efetivamente corrigidas
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
