package org.traducao.projeto.legendasExtracao.domain.exceptions;

import org.traducao.projeto.legendasExtracao.domain.ExtratorException;

/**
 * PROPÓSITO DE NEGÓCIO: Sinaliza que a ferramenta externa (mkvextract/ffmpeg)
 * estourou o tempo limite durante a extração, para o use case contabilizar
 * timeouts separadamente das demais falhas na telemetria e na tabela de resultado.
 *
 * <p>INVARIANTES DO DOMÍNIO: só deve ser lançada em caso de {@code TimeoutException}
 * real do processo externo — nunca reaproveitada para erros genéricos.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: é ela própria a falha; herda de
 * {@link ExtratorException} para continuar sendo capturada por quem trata a
 * hierarquia genérica, mas permite {@code catch} específico antes.
 */
public class ExtracaoTimeoutException extends ExtratorException {
    public ExtracaoTimeoutException(String message) {
        super(message);
    }

    public ExtracaoTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
