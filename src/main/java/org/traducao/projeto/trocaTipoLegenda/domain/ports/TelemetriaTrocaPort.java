package org.traducao.projeto.trocaTipoLegenda.domain.ports;

/**
 * PROPÓSITO DE NEGÓCIO: registra o desfecho de uma operação da fatia
 * {@code trocaTipoLegenda} na telemetria, sem que o {@code application} conheça o
 * serviço concreto nem o formato do arquivo gravado.
 *
 * <p>Assinatura própria da fatia, deliberadamente: os casos de uso montavam um
 * {@code OperacaoTelemetria} chamando {@code TelemetriaService.criarOperacao(...)} com
 * seis parâmetros posicionais, três deles inteiros seguidos — trocar dois de lugar
 * passava silenciosamente. Aqui os nomes dizem o que cada número é.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Chamada de efeito colateral apenas: nada do resultado da operação depende dela.</li>
 *   <li>Falha de telemetria NUNCA derruba a operação — perder o registro é aceitável,
 *       perder o trabalho não.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * A implementação absorve o erro e o registra em log; a porta não lança.
 */
public interface TelemetriaTrocaPort {

    /**
     * PROPÓSITO DE NEGÓCIO: grava uma linha de telemetria descrevendo o que a operação
     * fez.
     *
     * <p>INVARIANTES DO DOMÍNIO: contadores não negativos; {@code tipo} identifica a
     * operação na tela de telemetria.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: absorve e loga; não lança.
     *
     * @param tipo nome da operação (ex.: "Achatar Estilos Decorativos")
     * @param detalhe resumo legível do que foi feito
     * @param duracaoMs tempo total da operação
     * @param arquivosProcessados quantos arquivos foram examinados
     * @param itensDetectados quantos candidatos foram encontrados
     * @param itensCorrigidos quantos foram efetivamente alterados
     */
    void registrar(String tipo, String detalhe, long duracaoMs,
                   int arquivosProcessados, int itensDetectados, int itensCorrigidos);
}
