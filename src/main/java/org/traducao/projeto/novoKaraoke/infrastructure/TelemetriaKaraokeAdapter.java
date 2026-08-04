package org.traducao.projeto.novoKaraoke.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.core.presentation.web.LogStreamService;
import org.traducao.projeto.novoKaraoke.domain.MedicaoEstiloKaraoke;
import org.traducao.projeto.novoKaraoke.domain.ports.TelemetriaKaraokePort;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: adaptador da {@link TelemetriaKaraokePort}. A dependência para a fatia
 * {@code telemetria} vive AQUI, em {@code infrastructure}, e não na camada de aplicação — que é
 * exatamente a dívida que as duas fatias de karaokê carregavam e que esta classe começa a pagar.
 *
 * <p>Além de publicar os totais, ele DENUNCIA o que o log antigo só sussurrava: estilo com volume
 * de faixa musical que a régua recusou, e arquivo que atravessou sem mudar um evento. Os dois são
 * o mesmo defeito visto de ângulos diferentes, e nenhum dos dois lançava erro.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Acumula por execução e só publica no fim: uma operação, uma linha de telemetria.</li>
 *   <li>Toda falha é ABSORVIDA — observabilidade nunca derruba conversão de acervo.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Falha de I/O ou de formatação vira WARN e a execução segue.
 */
@Component
public class TelemetriaKaraokeAdapter implements TelemetriaKaraokePort {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaKaraokeAdapter.class);
    /** Mesmo canal do use case: o alerta precisa aparecer junto do resto da conversão. */
    private static final String CANAL_LOG = "novo-karaoke";

    private final TelemetriaService telemetriaService;
    private final LogStreamService logStream;

    private final List<String> alertas = new ArrayList<>();
    private int arquivosSemMudanca;
    private int estilosSuspeitos;
    private int camadasInvertidasTotal;

    public TelemetriaKaraokeAdapter(TelemetriaService telemetriaService, LogStreamService logStream) {
        this.telemetriaService = telemetriaService;
        this.logStream = logStream;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: registra um arquivo e ACUSA na hora os dois sintomas silenciosos.
     *
     * <p>INVARIANTES DO DOMÍNIO: acusar é publicar aviso, nunca alterar decisão — o arquivo já foi
     * processado quando esta chamada acontece.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    @Override
    public void publicarArquivo(
        String arquivo, int eventosEntrada, int eventosSaida,
        int camadasPareadas, int camadasInvertidas, List<MedicaoEstiloKaraoke> porEstilo
    ) {
        try {
            camadasInvertidasTotal += camadasInvertidas;
            if (eventosEntrada > 0 && eventosEntrada == eventosSaida) {
                arquivosSemMudanca++;
                alerta("[ SUSPEITO ] " + arquivo + ": " + eventosEntrada
                    + " eventos entraram e saíram — a simplificação não fundiu NADA neste arquivo.");
            }
            for (MedicaoEstiloKaraoke m : porEstilo) {
                if (m.suspeitoDeReguaEstreita()) {
                    estilosSuspeitos++;
                    alerta("[ SUSPEITO ] " + arquivo + ": estilo \"" + m.estilo() + "\" tem "
                        + m.eventos() + " eventos e NÃO foi reconhecido como música — régua de nome"
                        + " estreita demais? (foi assim que OP_S2/ED_S2 passaram despercebidos)");
                }
            }
            if (camadasInvertidas > 0) {
                alerta("[ AVISO ] " + arquivo + ": " + camadasInvertidas + " de " + camadasPareadas
                    + " versos com a tradução ACIMA do original — a ordem das camadas deveria ser"
                    + " estável na música inteira.");
            }
        } catch (Exception e) {
            log.warn("Falha ao registrar telemetria do arquivo {}: {}", arquivo, e.getMessage());
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: fecha a execução publicando uma operação na telemetria canônica.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code itensDetectados} carrega os sintomas encontrados, não os
     * eventos processados — é o número que o operador precisa ver para saber se deve olhar.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; o estado acumulado é sempre zerado.
     */
    @Override
    public void publicarOperacao(
        String operacao, Path pastaOrigem, Path pastaDestino, long duracaoMs, int arquivosProcessados
    ) {
        try {
            int sintomas = arquivosSemMudanca + estilosSuspeitos + camadasInvertidasTotal;
            String detalhe = "origem=" + pastaOrigem + "; destino=" + pastaDestino
                + "; arquivosSemMudanca=" + arquivosSemMudanca
                + "; estilosNaoReconhecidos=" + estilosSuspeitos
                + "; camadasInvertidas=" + camadasInvertidasTotal;
            OperacaoTelemetria op = TelemetriaService.criarOperacao(
                operacao, detalhe, duracaoMs, arquivosProcessados, sintomas, 0);
            telemetriaService.finalizarOperacao(
                op, pastaDestino, "karaoke_simples", String.join(System.lineSeparator(), alertas));
        } catch (Exception e) {
            log.warn("Falha ao publicar telemetria de \"{}\": {}", operacao, e.getMessage());
        } finally {
            alertas.clear();
            arquivosSemMudanca = 0;
            estilosSuspeitos = 0;
            camadasInvertidasTotal = 0;
        }
    }

    private void alerta(String mensagem) {
        alertas.add(mensagem);
        logStream.publicarLog(CANAL_LOG, mensagem);
    }
}
