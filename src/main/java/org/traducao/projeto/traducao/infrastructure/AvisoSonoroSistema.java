package org.traducao.projeto.traducao.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * PROPÓSITO DE NEGÓCIO: chama o operador de volta quando o lote da Tradução Local termina.
 * O lote leva de meia hora a duas, e quem dispara vai trabalhar em outra coisa — normalmente
 * no editor, com o KRONOS minimizado. O aviso sai pela MÁQUINA, não pelo navegador, porque é
 * a máquina que continua ali quando a aba não está.
 *
 * <h2>Por que não o navegador como via única</h2>
 * O beep do navegador depende de três coisas fora do nosso alcance: a aba estar aberta, o
 * navegador ter liberado o áudio (política de autoplay) e a aba não estar no mudo. Nenhuma
 * delas vale quando o operador fechou o navegador e foi programar. O aviso do navegador
 * continua existindo como SEGUNDA VIA — ver {@code traducao.js} —, acionada só quando este
 * aqui informa que não conseguiu tocar.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Três estados, nunca dois: {@link Resultado#TOCOU}, {@link Resultado#INDISPONIVEL} e
 *       {@link Resultado#FALHOU}. "Não tinha como tocar" (contêiner Linux, sem PowerShell) e
 *       "tentei e deu erro" levam a ações diferentes, e um único "não" esconderia o segundo.
 *       Quem responde INDISPONIVEL está dizendo ao frontend para assumir o aviso.</li>
 *   <li>Nunca lança e nunca segura o chamador além do teto: um aviso sonoro não pode
 *       derrubar nem atrasar o encerramento de um lote de duas horas.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sistema operacional sem o executável, processo que não termina no teto de
 * {@value #TETO_SEGUNDOS}s ou interrupção da thread devolvem {@link Resultado#INDISPONIVEL}
 * ou {@link Resultado#FALHOU}, com registro em log — nunca exceção para fora.
 */
@Component
public class AvisoSonoroSistema {

    private static final Logger log = LoggerFactory.getLogger(AvisoSonoroSistema.class);

    /** Teto de espera pelo processo do beep. Três toques de 250ms cabem folgados. */
    static final int TETO_SEGUNDOS = 10;

    /** Frequência em Hz. 880 (lá4) atravessa ruído de fundo sem ser estridente. */
    static final int FREQUENCIA_HZ = 880;

    /** Duração de cada toque, em milissegundos. */
    static final int DURACAO_MS = 250;

    /** Pausa entre toques: sem ela os três viram um só, longo. */
    static final int PAUSA_MS = 300;

    public enum Resultado {
        /** O processo do beep rodou e saiu com sucesso. */
        TOCOU,
        /** Esta máquina não tem como tocar — o frontend assume o aviso. */
        INDISPONIVEL,
        /** Havia como tocar e a tentativa falhou. Diferente de não ter como. */
        FALHOU
    }

    /**
     * PROPÓSITO DE NEGÓCIO: toca o aviso de fim de lote.
     *
     * <p>INVARIANTES DO DOMÍNIO: bloqueia no máximo {@value #TETO_SEGUNDOS}s; devolve sempre
     * um dos três estados; não propaga exceção.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@link Resultado#INDISPONIVEL} quando o
     * sistema não é Windows ou o executável não existe, e {@link Resultado#FALHOU} quando o
     * processo existe mas termina mal ou estoura o teto.
     *
     * @param toques quantos avisos tocar; valor menor que 1 não toca nada
     * @return o estado real da tentativa
     */
    public Resultado tocar(int toques) {
        if (toques < 1) {
            return Resultado.INDISPONIVEL;
        }
        // Windows é o único ambiente onde este KRONOS roda com placa de som: no contêiner
        // Linux não há dispositivo de áudio, e responder INDISPONIVEL ali é o certo — é o
        // que faz o navegador assumir o aviso em vez de todo mundo ficar em silêncio.
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return Resultado.INDISPONIVEL;
        }

        StringBuilder script = new StringBuilder();
        for (int i = 0; i < toques; i++) {
            if (i > 0) {
                script.append("Start-Sleep -Milliseconds ").append(PAUSA_MS).append("; ");
            }
            script.append("[console]::beep(").append(FREQUENCIA_HZ).append(',').append(DURACAO_MS).append("); ");
        }

        try {
            Process processo = new ProcessBuilder(
                List.of("powershell", "-NoProfile", "-NonInteractive", "-Command", script.toString()))
                .redirectErrorStream(true)
                .start();

            if (!processo.waitFor(TETO_SEGUNDOS, TimeUnit.SECONDS)) {
                processo.destroyForcibly();
                log.warn("Aviso sonoro de fim de lote nao terminou em {}s; processo encerrado.", TETO_SEGUNDOS);
                return Resultado.FALHOU;
            }
            if (processo.exitValue() != 0) {
                log.warn("Aviso sonoro de fim de lote saiu com codigo {}.", processo.exitValue());
                return Resultado.FALHOU;
            }
            return Resultado.TOCOU;

        } catch (java.io.IOException e) {
            // PowerShell ausente do PATH: não há como tocar nesta máquina.
            log.info("Aviso sonoro indisponivel nesta maquina ({}). O navegador assume o aviso.", e.getMessage());
            return Resultado.INDISPONIVEL;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Resultado.FALHOU;
        }
    }
}
