package org.traducao.projeto.correcaoLegendas.domain.ports;

import org.traducao.projeto.correcaoLegendas.domain.CorrecaoLegendasRelatorioJson;

import java.io.IOException;
import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: contrato de saída para persistir o relatório JSON de uma sessão de
 * correção de legendas — o artefato que o operador abre depois para conferir fala a fala o que foi
 * curado, corrigido pelo LLM ou deixado sem par.
 *
 * <p>É a ÚLTIMA porta da FASE 2 do Plano-Mestre. A aresta que ela remove era a única que restava do
 * inventário de {@code application} alcançando {@code infrastructure}: o caso de uso injetava o
 * {@code CorrecaoLegendasLogPersistencia} concreto, e o contrato manda depender de
 * {@code domain.ports}.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Recebe a pasta JÁ RESOLVIDA. Quem conhece a convenção de caminho dos relatórios é o
 *       adaptador de telemetria, consultado pelo caso de uso — este contrato só grava onde
 *       mandarem, e é isso que impede a convenção de existir em dois lugares.</li>
 *   <li>Só tipos do próprio domínio e do JDK na assinatura: nenhum framework, nenhum Jackson.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Diferente das demais portas desta área, esta PROPAGA {@link IOException} em vez de absorver — e
 * é deliberado. O caso de uso já envolve a chamada num {@code catch} que degrada para aviso em log
 * e segue com a correção; absorver aqui não mudaria o desfecho, só apagaria a causa antes de ela
 * chegar a esse aviso. A regra continua valendo onde importa: falha ao gravar o relatório nunca
 * interrompe a correção das legendas.
 */
public interface RelatorioCorrecaoLegendasPort {

    /**
     * PROPÓSITO DE NEGÓCIO: grava o relatório da sessão e devolve onde ficou, para o operador ver
     * o caminho no painel.
     *
     * <p>INVARIANTES DO DOMÍNIO: a pasta chega resolvida; o nome do arquivo é da implementação.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga {@link IOException}; quem chama decide.
     *
     * @param pastaRelatorios pasta de destino, já resolvida pelo chamador
     * @param relatorio conteúdo completo da sessão
     * @return caminho absoluto do arquivo gravado
     * @throws IOException se a pasta não puder ser criada ou o arquivo não puder ser escrito
     */
    Path salvarRelatorioJson(Path pastaRelatorios, CorrecaoLegendasRelatorioJson relatorio)
        throws IOException;
}
