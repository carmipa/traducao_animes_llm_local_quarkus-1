package org.traducao.projeto.traducao.domain.ports;

import org.traducao.projeto.traducao.domain.FalaNaoTraduzida;
import org.traducao.projeto.traducao.domain.TelemetriaTraducao;

import java.nio.file.Path;
import java.util.List;

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
     * PROPÓSITO DE NEGÓCIO: grava, fala a fala, POR QUE cada linha saiu igual à origem. É o que
     * responde "por que esta fala está em inglês?" sem obrigar quem audita a recalcular por fora
     * as regras que o pipeline já aplicou por dentro.
     *
     * <p>Na auditoria de 07/08/2026, recalcular por fora produziu cinco conclusões erradas —
     * 18.431 falas dadas como resíduo de tradução que eram letra de música, 2.898 dadas como
     * perdidas que eram karaokê descartado de propósito, 364 "defeitos" que eram estilos
     * ignorados por configuração. O dado existia; o que faltava era o pipeline declarar.
     *
     * <p>INVARIANTES DO DOMÍNIO: substitui o registro anterior do mesmo arquivo — a última
     * execução é a que vale, como no {@code .ass} de saída. Lista vazia é gravada como vazia, e
     * isso é informação: significa "conferi e nenhuma fala ficou para trás", que é diferente de
     * "não conferi". Ver a regra 12 do método: saída vazia ambígua é bug.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: erro de I/O é logado e NÃO propaga. Este registro é
     * acessório — perder o dataset jamais pode custar a legenda que acabou de ser traduzida.
     *
     * @param arquivoTraduzido caminho do {@code .ass} publicado, que dá nome ao dataset
     * @param obra nome da obra derivado do caminho, para agrupar
     * @param falas as falas que saíram iguais à origem, com o motivo de cada uma
     */
    default void registrarFalasNaoTraduzidas(Path arquivoTraduzido, String obra,
                                             List<FalaNaoTraduzida> falas) {
        // default vazio DE PROPOSITO: este registro e acessorio e nenhuma implementacao e
        // obrigada a te-lo. Os fakes de teste do pipeline nao precisam gravar dataset para
        // exercitar a traducao, e obriga-los a implementar so adicionaria ruido sem invariante.
        // A implementacao real (TelemetriaTraducaoAdapter) sobrescreve.
    }

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
