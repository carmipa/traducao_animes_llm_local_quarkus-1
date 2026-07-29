package org.traducao.projeto.trocaTipoLegenda.domain.ports;

/**
 * PROPÓSITO DE NEGÓCIO: dá voz aos casos de uso da fatia {@code trocaTipoLegenda} sem
 * que eles saibam COMO a mensagem aparece — cor de terminal, HTML no navegador ou linha
 * de log.
 *
 * <p>Os use cases montavam a saída concatenando {@code AnsiCores.GREEN + ... +
 * AnsiCores.RESET}, ou seja, {@code application} dependendo de
 * {@code core.presentation.ui}: inversão de camada pura, com regra de negócio decidindo
 * código de escape ANSI. Aqui o caso de uso declara a INTENÇÃO (isto é um sucesso, isto
 * é um aviso) e quem pinta é o adaptador.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Os métodos diferem em SIGNIFICADO, não em cor: {@link #aviso(String)} é algo que
 *       o operador precisa notar mas não impediu o trabalho; {@link #erro(String)} é
 *       trabalho que não aconteceu.</li>
 *   <li>Saída é efeito colateral: nenhum resultado depende dela.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Implementação absorve falhas de escrita; a porta não lança. Mensagem nula é ignorada.
 */
public interface ConsoleTrocaPort {

    /** Abertura de uma etapa — o que vai começar e sobre qual pasta. */
    void titulo(String mensagem);

    /** Andamento neutro: um arquivo pulado, um caminho resolvido. */
    void info(String mensagem);

    /** Trabalho concluído com êxito neste item ou no lote. */
    void sucesso(String mensagem);

    /** Precisa da atenção do operador, mas o lote seguiu. */
    void aviso(String mensagem);

    /** Trabalho que NÃO aconteceu — falha neste item. */
    void erro(String mensagem);
}
