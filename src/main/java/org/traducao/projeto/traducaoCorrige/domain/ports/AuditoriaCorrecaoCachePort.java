package org.traducao.projeto.traducaoCorrige.domain.ports;

import org.traducao.projeto.traducaoCorrige.domain.EntradaAuditoriaCorrecaoCache;

/**
 * PROPÓSITO DE NEGÓCIO: contrato de saída para registrar cada decisão granular da manutenção de
 * cache — o que mudou, sob qual lore e com qual proveniência. É a PRIMEIRA porta das quatro fatias
 * de correção, que até aqui tinham zero: o diagnóstico do Plano-Mestre mediu {@code domain/ports}
 * vazio nas quatro.
 *
 * <p>Existe porque o contrato arquitetural diz que {@code application} depende de
 * {@code domain.ports}, NUNCA de {@code infrastructure} — e o caso de uso de reforço de
 * terminologia nasceria violando isso se injetasse a implementação concreta, repetindo a dívida
 * que a FASE 2 existe para remover. Corrigir na origem custa uma interface; corrigir depois custa
 * mais um item de backlog.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Mora em {@code domain} e só conhece o próprio domínio: nenhum tipo de framework,
 *       Jackson ou I/O aparece na assinatura.</li>
 *   <li>É unidirecional — registra e não devolve nada. Auditoria que informasse o chamador
 *       viraria fonte de decisão, e decisão é da camada de aplicação.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Implementações ABSORVEM falha de escrita: perder uma linha de auditoria nunca pode interromper
 * a manutenção do acervo nem propagar exceção para o caso de uso.
 */
public interface AuditoriaCorrecaoCachePort {

    /**
     * PROPÓSITO DE NEGÓCIO: registra uma decisão da manutenção de cache.
     *
     * <p>INVARIANTES DO DOMÍNIO: a entrada já chega completa e imutável.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; falha de I/O é absorvida pela implementação.
     *
     * @param entrada decisão a registrar
     */
    void registrar(EntradaAuditoriaCorrecaoCache entrada);
}
