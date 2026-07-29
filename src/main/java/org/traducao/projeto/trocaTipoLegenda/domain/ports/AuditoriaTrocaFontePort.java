package org.traducao.projeto.trocaTipoLegenda.domain.ports;

import org.traducao.projeto.trocaTipoLegenda.domain.EntradaAuditoriaTrocaFonte;

/**
 * PROPÓSITO DE NEGÓCIO: registra, fonte a fonte, cada substituição efetivamente gravada —
 * a trilha que permite responder depois "quando esta legenda deixou de usar
 * .VnBook-Antiqua, e por quê".
 *
 * <p>Esta porta foi encontrada pelo próprio teste de fronteira, não pela revisão manual:
 * o {@code application} importava {@code TrocaTipoLegendaAuditoriaCache}, que é
 * {@code infrastructure} da própria fatia. Camada interna errada acopla tanto quanto
 * camada de outra fatia — só é mais fácil de não enxergar.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só se registra DEPOIS da gravação física concluída: o registro afirma que a troca
 *       aconteceu, e marcá-lo antes faria a trilha mentir se o I/O falhasse.</li>
 *   <li>Efeito colateral puro: nenhum resultado da operação depende do retorno.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * A implementação decide entre absorver e propagar; a porta não impõe.
 */
public interface AuditoriaTrocaFontePort {

    /**
     * PROPÓSITO DE NEGÓCIO: acrescenta uma entrada à trilha de auditoria de troca de fonte.
     *
     * <p>INVARIANTES DO DOMÍNIO: chamada uma vez por fonte substituída, após a gravação.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conforme a implementação.
     */
    void registrar(EntradaAuditoriaTrocaFonte entrada);

    /**
     * PROPÓSITO DE NEGÓCIO: informa ONDE a trilha ficou gravada, para o relatório final
     * dizer ao operador onde consultá-la.
     *
     * <p>É a única razão de o caminho atravessar a porta: exibição. O caso de uso não lê
     * nem escreve nesse arquivo — quem faz isso é a implementação.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: implementação sem destino em disco devolve um
     * caminho simbólico, nunca {@code null}.
     */
    java.nio.file.Path caminhoCanonico();
}
