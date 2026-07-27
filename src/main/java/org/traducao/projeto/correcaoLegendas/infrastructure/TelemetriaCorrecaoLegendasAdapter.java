package org.traducao.projeto.correcaoLegendas.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.correcaoLegendas.domain.ResumoOperacaoCorrecaoLegendas;
import org.traducao.projeto.correcaoLegendas.domain.ports.TelemetriaCorrecaoLegendasPort;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: liga a correção de legendas à telemetria compartilhada, para que ela
 * apareça no mesmo painel e no mesmo arquivo que as demais operações do pipeline.
 *
 * <p>É o ADAPTADOR da {@link TelemetriaCorrecaoLegendasPort}: a dependência para a fatia
 * {@code telemetria} vive AQUI, em {@code infrastructure}. Com isto, as três arestas que esta
 * fatia tinha da aplicação e do DOMÍNIO para {@code telemetria} deixam de existir.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só TRADUZ. A conversão é campo a campo, com os MESMOS nomes — é o que mantém o JSON do
 *       relatório do módulo idêntico ao de antes.</li>
 *   <li>NÃO regera o {@code registradoEm}: usa o que veio no resumo. O JSON do módulo e o registro
 *       do painel têm de mostrar o mesmo instante, e recriar o carimbo aqui os faria divergir por
 *       milissegundos sem ninguém perceber.</li>
 *   <li>Falha de I/O é ABSORVIDA e registrada em log — uma correção que terminou bem não pode
 *       virar falha porque a telemetria não pôde ser escrita.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nunca propaga exceção ao caso de uso; a consulta de pasta degrada para a própria entrada.
 */
@Component
public class TelemetriaCorrecaoLegendasAdapter implements TelemetriaCorrecaoLegendasPort {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaCorrecaoLegendasAdapter.class);

    private final TelemetriaService telemetriaService;

    /**
     * PROPÓSITO DE NEGÓCIO: injeta a telemetria compartilhada.
     * <p>INVARIANTES DO DOMÍNIO: guarda a referência recebida.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede o uso do adaptador.
     */
    public TelemetriaCorrecaoLegendasAdapter(TelemetriaService telemetriaService) {
        this.telemetriaService = telemetriaService;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: publica no painel e descarrega o arquivo compartilhado.
     * <p>INVARIANTES DO DOMÍNIO: os dois passos acontecem juntos, sempre — era assim que a
     * aplicação fazia em duas linhas consecutivas, e a porta existe para que não se possa
     * esquecer um deles.
     * <p>COMPORTAMENTO EM CASO DE FALHA: loga e retorna.
     */
    @Override
    public void registrarEsalvar(ResumoOperacaoCorrecaoLegendas resumo, Path pastaAlvo) {
        try {
            telemetriaService.registrarOperacao(new OperacaoTelemetria(
                resumo.tipo(), resumo.detalhe(), resumo.tempoTotalMs(),
                resumo.arquivosProcessados(), resumo.itensDetectados(), resumo.itensCorrigidos(),
                resumo.registradoEm()));
            telemetriaService.salvar(TelemetriaService.resolverPastaRelatorios(pastaAlvo));
        } catch (Exception e) {
            log.error("Falha ao registrar telemetria de \"{}\": {}", resumo.tipo(), e.getMessage());
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: informa onde os relatórios daquela entrada são gravados.
     * <p>INVARIANTES DO DOMÍNIO: consulta pura.
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve a própria pasta de entrada.
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
