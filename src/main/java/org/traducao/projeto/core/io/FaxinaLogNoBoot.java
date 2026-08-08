package org.traducao.projeto.core.io;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;

/**
 * PROPÓSITO DE NEGÓCIO: disparar a faxina de logs quando o KRONOS sobe, para que o disco se
 * mantenha limpo sem ninguém precisar lembrar — e ANUNCIAR o que ela fez, para que "não apaguei
 * nada" nunca se confunda com "não consegui apagar".
 *
 * <h2>Por que no BOOT, e não num agendador</h2>
 * O KRONOS é ferramenta de mesa: sobe, trabalha horas, desce. Não há processo perene onde um
 * agendador diário faça sentido, e um agendador que só roda com a aplicação de pé é precisamente
 * o "estado calculado pelo processo vivo" que a regra 13 proíbe — ficaria verde eterno justamente
 * quando ninguém está usando. O boot é o momento em que se sabe, com certeza, que o disco vai
 * começar a receber log novo.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A faxina NUNCA impede a aplicação de subir. Log é serviço de apoio; derrubar o KRONOS
 *       porque um arquivo estava travado seria trocar um problema pequeno por um grande.</li>
 *   <li>O resultado é sempre registrado — inclusive quando nada foi apagado. Faxina silenciosa é
 *       indistinguível de faxina que não rodou.</li>
 *   <li>Impedimento sai em {@code WARN}, remoção em {@code INFO}: quem lê o log precisa
 *       diferenciar rotina de problema sem ler o número.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer exceção é capturada e registrada em {@code WARN}; o boot segue. A aplicação sobe com
 * log sujo, que é o pior cenário aceitável — e ele fica declarado no próprio log.
 */
@ApplicationScoped
public class FaxinaLogNoBoot {

    private static final Logger log = LoggerFactory.getLogger(FaxinaLogNoBoot.class);

    @ConfigProperty(name = "kronos.log.retencao-dias", defaultValue = "7")
    int retencaoDias;

    @ConfigProperty(name = "kronos.log.execucao", defaultValue = "manual")
    String carimboExecucao;

    /**
     * PROPÓSITO DE NEGÓCIO: apaga os logs vencidos assim que a aplicação sobe e informa o saldo.
     *
     * <p>INVARIANTES DO DOMÍNIO: o log DESTA execução é passado à faxina justamente para ser
     * poupado; a raiz vem de {@link DiretorioBaseKronos}, o mesmo ponto único que o resto do
     * projeto usa, de modo que a suíte de testes nunca varre a árvore real.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca propaga — o boot não pode cair por causa de log.
     */
    void limparNoBoot(@Observes StartupEvent evento) {
        try {
            Path raizLogs = DiretorioBaseKronos.base().resolve("logs");
            Path meuLog = raizLogs.resolve(FaxinaLogExecucao.SUBPASTA)
                .resolve(FaxinaLogExecucao.nomeDoArquivo(carimboExecucao));

            FaxinaLogExecucao.Resultado r = FaxinaLogExecucao.limpar(
                raizLogs, Duration.ofDays(retencaoDias), meuLog);

            if (r.houveImpedimento()) {
                log.warn("{} — impedimentos: {}", r.resumo(), r.motivosDeFalha());
            } else if (r.removidos() > 0) {
                log.info("{} (retencao de {} dia(s))", r.resumo(), retencaoDias);
            } else {
                log.info("Faxina de log: nada vencido a remover (retencao de {} dia(s), {} arquivo(s) no prazo).",
                    retencaoDias, r.preservados());
            }
        } catch (RuntimeException e) {
            log.warn("Faxina de log nao pode ser executada nesta subida: {}", e.getMessage());
        }
    }
}
