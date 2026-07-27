package org.traducao.projeto.traducaoCorrige.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducaoCorrige.domain.ports.TelemetriaCorrecaoPort;

import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: liga a manutenção de cache à telemetria compartilhada do projeto, para que
 * o reforço de terminologia apareça no mesmo lugar em que as demais operações do pipeline
 * aparecem — dashboard, logs e relatório persistido.
 *
 * <p>É o ADAPTADOR da {@link TelemetriaCorrecaoPort}: a dependência para a fatia
 * {@code telemetria} vive AQUI, em {@code infrastructure}, e não na camada de aplicação. A
 * diferença não é cosmética — é ela que permite o caso de uso rodar em teste sem a fatia de
 * telemetria, e é a forma que a FASE 2 do Plano-Mestre prescreve para as quatro fatias da
 * correção, hoje todas acopladas direto ao {@code TelemetriaService}.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só TRADUZ: recebe números e texto já prontos e os converte para o formato da telemetria.
 *       Nenhuma decisão, nenhum cálculo, nenhuma regra.</li>
 *   <li>Falha de I/O é ABSORVIDA e registrada em log. Uma operação que alterou o acervo com
 *       sucesso não pode ser reportada como falha porque o relatório não pôde ser escrito.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nunca propaga exceção ao caso de uso.
 */
@Component
public class TelemetriaCorrecaoAdapter implements TelemetriaCorrecaoPort {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaCorrecaoAdapter.class);

    private final TelemetriaService telemetriaService;

    /**
     * PROPÓSITO DE NEGÓCIO: injeta a telemetria compartilhada.
     * <p>INVARIANTES DO DOMÍNIO: guarda a referência recebida.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede o uso do adaptador.
     */
    public TelemetriaCorrecaoAdapter(TelemetriaService telemetriaService) {
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
                operacao, detalhe, duracaoMs,
                arquivosProcessados, itensDetectados, itensCorrigidos);
            telemetriaService.finalizarOperacao(op, pastaAlvo, prefixoRelatorio, relatorio);
        } catch (Exception e) {
            log.error("Falha ao registrar telemetria de \"{}\": {}", operacao, e.getMessage());
        }
    }
}
