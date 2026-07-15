package org.traducao.projeto.analisadorMidia.application;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PROPÓSITO DE NEGÓCIO: encapsula a barra de progresso do console da análise de
 * mídia, isolando a dependência de UI e, sobretudo, a política de que a barra é
 * PURAMENTE COSMÉTICA — nunca pode abortar a análise do lote.
 *
 * <p>INVARIANTES DO DOMÍNIO: qualquer falha ao criar/avançar/fechar a barra
 * (ex.: terminal incompatível) é contida; após uma falha a barra se autodesativa
 * e a análise continua normalmente.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: nenhum método propaga exceção; falhas são
 * apenas registradas em log em nível WARN.
 */
public class BarraProgressoAnalise {

    private static final Logger log = LoggerFactory.getLogger(BarraProgressoAnalise.class);

    private ProgressBar barra;

    /** Cria a barra; se o terminal for incompatível, segue sem ela. */
    public void iniciar(int total, String tarefa) {
        try {
            barra = new ProgressBarBuilder()
                .setTaskName(tarefa)
                .setInitialMax(total)
                .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                .build();
        } catch (RuntimeException e) {
            barra = null;
            log.warn("Não foi possível iniciar a barra de progresso (terminal incompatível); continuando sem ela: {}",
                e.getMessage());
        }
    }

    /** Avança um passo; em caso de falha, autodesativa a barra sem interromper. */
    public void passo() {
        if (barra == null) {
            return;
        }
        try {
            barra.step();
        } catch (RuntimeException e) {
            log.warn("Barra de progresso falhou ao avançar durante a análise (ignorada): {}", e.getMessage());
            barra = null;
        }
    }

    /** Fecha a barra; falha ao fechar é ignorada. */
    public void fechar() {
        if (barra == null) {
            return;
        }
        try {
            barra.close();
        } catch (RuntimeException e) {
            log.warn("Falha ao fechar a barra de progresso (ignorada): {}", e.getMessage());
        } finally {
            barra = null;
        }
    }
}
