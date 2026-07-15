package org.traducao.projeto.traducao.domain.ports;

import org.traducao.projeto.traducao.domain.TelemetriaTraducao;

/**
 * PROPÓSITO DE NEGÓCIO: porta de telemetria própria da Tradução Local. Substitui
 * o acoplamento anterior ao {@code telemetria.TelemetriaService}, permitindo que
 * o pipeline registre traduções e incrementos de qualidade sem importar o módulo
 * de telemetria — a integração passa a ser apenas o arquivo canônico próprio.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Cada registro/incremento é persistido de forma atômica e sincronizada
 *       (dentro da JVM), como uma única alteração lógica coerente.</li>
 *   <li>Os contadores são acumuladores da Tradução Local a partir da adoção do
 *       arquivo próprio (iniciam em zero).</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Falha de I/O ao persistir é registrada; o estado em memória permanece coerente
 * e a próxima escrita bem-sucedida projeta o estado consolidado.
 */
public interface TelemetriaTraducaoPort {

    /**
     * PROPÓSITO DE NEGÓCIO: registra o desfecho da tradução de um episódio,
     * substituindo qualquer registro anterior do mesmo episódio (mais recente vence).
     * <p>INVARIANTES DO DOMÍNIO: chave por nome de episódio normalizado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: erro de I/O é logado; estado em memória preservado.
     */
    void registrarTraducao(TelemetriaTraducao telemetria);

    /**
     * PROPÓSITO DE NEGÓCIO: contabiliza uma resposta suspeita interceptada pela guarda anti-alucinação.
     * <p>INVARIANTES DO DOMÍNIO: acumulador monotônico da fatia.
     * <p>COMPORTAMENTO EM CASO DE FALHA: erro de I/O é logado.
     */
    void registrarAlucinacaoPrevenida();

    /**
     * PROPÓSITO DE NEGÓCIO: contabiliza uma resposta do modelo rejeitada pela validação antes de persistir.
     * <p>INVARIANTES DO DOMÍNIO: acumulador monotônico da fatia.
     * <p>COMPORTAMENTO EM CASO DE FALHA: erro de I/O é logado.
     */
    void registrarRespostaTraducaoRejeitada();

    /**
     * PROPÓSITO DE NEGÓCIO: contabiliza uma tradução recuperada por nova tentativa validada.
     * <p>INVARIANTES DO DOMÍNIO: acumulador monotônico da fatia.
     * <p>COMPORTAMENTO EM CASO DE FALHA: erro de I/O é logado.
     */
    void registrarFalhaTraducaoRecuperada();

    /**
     * PROPÓSITO DE NEGÓCIO: contabiliza uma fala mantida pendente após esgotar tentativas.
     * <p>INVARIANTES DO DOMÍNIO: acumulador monotônico da fatia.
     * <p>COMPORTAMENTO EM CASO DE FALHA: erro de I/O é logado.
     */
    void registrarFallbackMantido();
}
