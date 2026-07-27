package org.traducao.projeto.correcaoLegendas.domain.ports;

import org.traducao.projeto.correcaoLegendas.domain.ResumoOperacaoCorrecaoLegendas;

import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: publica o desfecho de uma correção de legendas na telemetria compartilhada.
 * É a porta da FASE 2 do Plano-Mestre para esta fatia — a última das quatro da área de correção.
 *
 * <h2>Por que DUAS operações, e por que uma delas junta duas chamadas</h2>
 * A aplicação hoje faz {@code registrarOperacao(...)} e, na linha seguinte,
 * {@code salvar(resolverPastaRelatorios(...))}. São duas chamadas que só fazem sentido JUNTAS:
 * a primeira acumula em memória e a segunda descarrega no arquivo. Separá-las na porta convidaria
 * alguém a chamar só uma — o painel mostraria a operação e o arquivo compartilhado ficaria
 * desatualizado, ou o inverso. Por isso {@link #registrarEsalvar} é uma operação só.
 *
 * <p>{@link #pastaDeRelatorios} é CONSULTA, não publicação: quem conhece a convenção de pastas da
 * telemetria é o adaptador. Ela existe para que a persistência do relatório JSON do módulo pare de
 * chamar o estático da fatia {@code telemetria} por conta própria.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>NENHUM tipo da fatia {@code telemetria} aparece na assinatura: o que trafega é o
 *       {@link ResumoOperacaoCorrecaoLegendas}, do próprio domínio, e o adaptador traduz.</li>
 *   <li>Uma porta POR FATIA. O contrato proíbe fatia depender de fatia e proíbe {@code shared/}
 *       funcional — portas quase idênticas entre fatias são a forma prescrita.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Implementações ABSORVEM falha de I/O: perder telemetria não pode transformar uma correção
 * bem-sucedida em falha.
 */
public interface TelemetriaCorrecaoLegendasPort {

    /**
     * PROPÓSITO DE NEGÓCIO: registra a operação no painel E descarrega o arquivo compartilhado,
     * que são passos inseparáveis.
     * <p>INVARIANTES DO DOMÍNIO: uma vez por execução; o {@code registradoEm} do resumo é
     * preservado como veio, não regerado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     *
     * @param resumo totais e carimbo de tempo já apurados pela execução
     * @param pastaAlvo pasta sobre a qual a correção rodou
     */
    void registrarEsalvar(ResumoOperacaoCorrecaoLegendas resumo, Path pastaAlvo);

    /**
     * PROPÓSITO DE NEGÓCIO: informa a pasta onde os relatórios daquela entrada são gravados.
     * <p>INVARIANTES DO DOMÍNIO: consulta pura; não cria nem escreve nada.
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança; na dúvida devolve a própria pasta de entrada.
     *
     * @param pastaEntrada pasta sobre a qual a operação rodou
     * @return pasta onde os relatórios daquela entrada são gravados
     */
    Path pastaDeRelatorios(Path pastaEntrada);
}
