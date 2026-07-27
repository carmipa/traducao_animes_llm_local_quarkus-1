package org.traducao.projeto.raspagemRevisao.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.raspagemRevisao.domain.ports.TelemetriaRevisaoPort;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.nio.file.Path;
import java.time.Instant;

/**
 * PROPÓSITO DE NEGÓCIO: liga as revisões desta fatia à telemetria compartilhada, para que apareçam
 * no mesmo painel, log e relatório das demais operações do pipeline.
 *
 * <p>É o ADAPTADOR da {@link TelemetriaRevisaoPort}: a dependência para a fatia {@code telemetria}
 * vive AQUI, em {@code infrastructure}, e não na camada de aplicação — a diferença entre a dívida
 * medida no diagnóstico e a forma que o contrato prescreve.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só TRADUZ: recebe números e texto prontos e os converte para o formato da telemetria.
 *       Nenhuma decisão, nenhum cálculo, nenhuma regra.</li>
 *   <li>Falha de I/O é ABSORVIDA e registrada em log. Uma revisão que terminou com sucesso não
 *       pode ser reportada como falha porque o relatório não pôde ser escrito.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nunca propaga exceção ao caso de uso; a consulta de pasta degrada para a própria entrada.
 */
@Component
public class TelemetriaRevisaoAdapter implements TelemetriaRevisaoPort {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaRevisaoAdapter.class);

    private final TelemetriaService telemetriaService;

    /**
     * PROPÓSITO DE NEGÓCIO: injeta a telemetria compartilhada.
     * <p>INVARIANTES DO DOMÍNIO: guarda a referência recebida.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede o uso do adaptador.
     */
    public TelemetriaRevisaoAdapter(TelemetriaService telemetriaService) {
        this.telemetriaService = telemetriaService;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: publica a operação e persiste o relatório.
     * <p>INVARIANTES DO DOMÍNIO: os números chegam prontos; não são recalculados.
     * <p>COMPORTAMENTO EM CASO DE FALHA: loga e retorna.
     */
    @Override
    public void registrarComRelatorio(
        String operacao, String detalhe, String prefixoRelatorio, Path pastaAlvo, long duracaoMs,
        int arquivosProcessados, int itensDetectados, int itensCorrigidos, String relatorio
    ) {
        try {
            OperacaoTelemetria op = TelemetriaService.criarOperacao(
                operacao, detalhe, duracaoMs, arquivosProcessados, itensDetectados, itensCorrigidos);
            telemetriaService.finalizarOperacao(op, pastaAlvo, prefixoRelatorio, relatorio);
        } catch (Exception e) {
            log.error("Falha ao registrar telemetria de \"{}\": {}", operacao, e.getMessage());
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: publica só os totais, sem relatório em disco.
     * <p>INVARIANTES DO DOMÍNIO: o carimbo de tempo é gerado aqui, na fronteira — a aplicação não
     * precisa conhecer o formato de instante da telemetria.
     * <p>COMPORTAMENTO EM CASO DE FALHA: loga e retorna.
     */
    @Override
    public void registrar(
        String operacao, String detalhe, long duracaoMs,
        int arquivosProcessados, int itensDetectados, int itensCorrigidos
    ) {
        try {
            telemetriaService.registrarOperacao(new OperacaoTelemetria(
                operacao, detalhe, duracaoMs, arquivosProcessados, itensDetectados, itensCorrigidos,
                Instant.now().toString()));
        } catch (Exception e) {
            log.error("Falha ao registrar telemetria de \"{}\": {}", operacao, e.getMessage());
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: informa a pasta onde os relatórios daquela entrada são gravados.
     * <p>INVARIANTES DO DOMÍNIO: consulta pura; não cria nada.
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve a própria pasta de entrada — a mensagem ao
     * operador fica menos precisa, mas nunca quebra o fim de uma revisão bem-sucedida.
     */
    @Override
    public Path pastaDeRelatorios(Path pastaEntrada) {
        try {
            return TelemetriaService.resolverPastaRelatorios(pastaEntrada);
        } catch (Exception e) {
            log.warn("Não foi possível resolver a pasta de relatórios de {}: {}",
                pastaEntrada, e.getMessage());
            return pastaEntrada;
        }
    }
}
