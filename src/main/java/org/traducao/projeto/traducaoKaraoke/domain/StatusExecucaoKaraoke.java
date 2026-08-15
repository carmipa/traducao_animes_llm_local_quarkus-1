package org.traducao.projeto.traducaoKaraoke.domain;

/**
 * PROPÓSITO DE NEGÓCIO: diz como a execução de karaokê TERMINOU, para que "deu tudo certo",
 * "foi cancelada no meio" e "morreu antes de começar" deixem de produzir o mesmo silêncio.
 *
 * <h2>O prejuízo que originou</h2>
 * Auditoria de 2026-08-14: o manifesto só era escrito quando havia resultado
 * ({@code if (gravar && !resultados.isEmpty())}). Consequências, lidas no código:
 * <ul>
 *   <li>todos os arquivos falhando ⇒ <b>nenhum artefato</b>. O pior desfecho possível produzia
 *       o mesmo vazio de "não havia nada a fazer";</li>
 *   <li>execução cancelada ⇒ manifesto <b>indistinguível</b> de execução completa, só com menos
 *       arquivos;</li>
 *   <li>LLM fora do ar ⇒ {@code throw} antes do registro, e o desfecho vivia apenas numa linha
 *       de log em texto, num arquivo de 78 MB.</li>
 * </ul>
 * É a regra "saída vazia ambígua é bug" aplicada ao artefato de auditoria.
 *
 * <h2>Invariantes do domínio</h2>
 * Três estados, nunca dois — o terceiro não é firula: {@link #ABORTADA} é o "não verificou" desta
 * fatia e não pode ser confundido nem com sucesso nem com fracasso parcial.
 */
public enum StatusExecucaoKaraoke {

    /** Percorreu todos os arquivos da pasta. Pode ter falha por arquivo — veja a lista delas. */
    COMPLETA,

    /** Cancelamento cooperativo entre arquivos; o que já foi gravado está preservado. */
    INTERROMPIDA,

    /** Morreu antes ou durante o laço por condição de ambiente: LLM fora, destino não criável. */
    ABORTADA
}
