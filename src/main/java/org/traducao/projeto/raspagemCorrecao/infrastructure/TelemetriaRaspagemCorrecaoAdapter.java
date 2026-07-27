package org.traducao.projeto.raspagemCorrecao.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.raspagemCorrecao.domain.ports.TelemetriaRaspagemCorrecaoPort;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: liga a correção online à telemetria compartilhada do projeto, para que ela
 * apareça no mesmo painel, log e relatório que as demais operações do pipeline.
 *
 * <p>É o ADAPTADOR da {@link TelemetriaRaspagemCorrecaoPort}: a dependência para a fatia
 * {@code telemetria} vive AQUI, em {@code infrastructure}, e não na camada de aplicação. A
 * diferença é o que permite o caso de uso rodar em teste sem a fatia de telemetria, e é a forma
 * que a FASE 2 do Plano-Mestre prescreve para as quatro fatias da correção.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só TRADUZ: recebe números e texto prontos e os converte para o formato da telemetria.
 *       Nenhuma decisão, nenhum cálculo, nenhuma regra.</li>
 *   <li>Falha de I/O é ABSORVIDA e registrada em log. Um lote que corrigiu o cache com sucesso
 *       não pode ser reportado como falha porque o relatório não pôde ser escrito.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nunca propaga exceção ao caso de uso.
 */
@Component
public class TelemetriaRaspagemCorrecaoAdapter implements TelemetriaRaspagemCorrecaoPort {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaRaspagemCorrecaoAdapter.class);

    private final TelemetriaService telemetriaService;

    /**
     * PROPÓSITO DE NEGÓCIO: injeta a telemetria compartilhada.
     * <p>INVARIANTES DO DOMÍNIO: guarda a referência recebida.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede o uso do adaptador.
     */
    public TelemetriaRaspagemCorrecaoAdapter(TelemetriaService telemetriaService) {
        this.telemetriaService = telemetriaService;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: converte o resumo da operação para o formato da telemetria e o publica.
     * <p>INVARIANTES DO DOMÍNIO: os números chegam prontos; este método não os recalcula.
     * <p>COMPORTAMENTO EM CASO DE FALHA: registra o erro em log e retorna normalmente.
     */
    @Override
    public void registrar(
        String operacao,
        String detalhe,
        String prefixoRelatorio,
        Path pastaAlvo,
        long duracaoMs,
        int arquivosProcessados,
        int itensDetectados,
        int itensCorrigidos,
        String relatorio
    ) {
        try {
            OperacaoTelemetria op = TelemetriaService.criarOperacao(
                operacao, detalhe, duracaoMs, arquivosProcessados, itensDetectados, itensCorrigidos);
            telemetriaService.finalizarOperacao(op, pastaAlvo, prefixoRelatorio, relatorio);
        } catch (Exception e) {
            log.error("Falha ao registrar telemetria de \"{}\": {}", operacao, e.getMessage());
        }
    }
}
