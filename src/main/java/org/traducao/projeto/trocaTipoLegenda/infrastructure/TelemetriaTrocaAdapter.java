package org.traducao.projeto.trocaTipoLegenda.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.trocaTipoLegenda.domain.ports.TelemetriaTrocaPort;

/**
 * PROPÓSITO DE NEGÓCIO: liga a porta de telemetria da fatia ao {@code TelemetriaService}
 * compartilhado, mantendo o {@code application} sem conhecimento do serviço concreto nem
 * do formato gravado.
 *
 * <p>INVARIANTES DO DOMÍNIO: falha de telemetria NUNCA derruba a operação. Perder o
 * registro de que um achatamento aconteceu é aceitável; perder o achatamento porque a
 * telemetria falhou, não. Por isso o {@code catch} largo — é deliberado, não descuido.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: absorve, registra em log e retorna normalmente.
 */
@Component
public class TelemetriaTrocaAdapter implements TelemetriaTrocaPort {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaTrocaAdapter.class);

    private final TelemetriaService telemetriaService;

    public TelemetriaTrocaAdapter(TelemetriaService telemetriaService) {
        this.telemetriaService = telemetriaService;
    }

    @Override
    public void registrar(String tipo, String detalhe, long duracaoMs,
                          int arquivosProcessados, int itensDetectados, int itensCorrigidos) {
        try {
            telemetriaService.registrarOperacao(TelemetriaService.criarOperacao(
                tipo, detalhe, duracaoMs, arquivosProcessados, itensDetectados, itensCorrigidos));
        } catch (Exception e) {
            log.warn("Falha ao registrar telemetria da operação '{}': {}", tipo, e.getMessage());
        }
    }
}
