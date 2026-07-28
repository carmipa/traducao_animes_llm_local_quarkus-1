package org.traducao.projeto.traducaoCorrige.domain;

/**
 * PROPÓSITO DE NEGÓCIO: o desfecho de MODO de uma passada de reforço de terminologia — o que a
 * operação era, e não quantos arquivos ela mexeu.
 *
 * <h2>Por que não é um booleano</h2>
 * Era. E o booleano colapsava dois estados que o operador precisa distinguir: o ensaio deliberado
 * (read-only, é o instrumento de decisão) e a APLICAÇÃO RECUSADA porque a escrita no acervo está
 * desligada em configuração.
 *
 * <p>Medido em 2026-07-28: o operador clicou "Aplicar" duas vezes, e o console web respondeu as duas
 * vezes com <i>"REFORÇO DE TERMINOLOGIA — ENSAIO (nada foi escrito) — CONCLUIDO_COM_FALHAS —
 * arquivos: 0 — falhas: 1"</i>. O relatório gravado em disco dizia o mesmo: {@code Modo: ENSAIO}.
 * A recusa construía o acumulador com {@code aplicar=false}, então o desfecho vestia a roupa do
 * ensaio. O motivo real — o nome da propriedade a ligar — existia apenas no log de arquivo, que não
 * aparece na tela.
 *
 * <p>Um relatório de manutenção de acervo tem exatamente um trabalho: dizer a verdade sobre o que
 * foi feito no acervo. Era o único campo que ele errava.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@link #ENSAIO} e {@link #RECUSADO_POR_CONFIG} não escreveram nada — mas por razões opostas:
 *       um porque não era para escrever, o outro porque não pôde.</li>
 *   <li>{@link #RECUSADO_POR_CONFIG} NÃO é falha de processamento. Falha é arquivo que quebrou;
 *       recusa por configuração é a trava funcionando como projetada. Contá-la em {@code falhas}
 *       misturava "o acervo tem um arquivo corrompido" com "você não ligou a flag".</li>
 * </ul>
 */
public enum ModoReforcoTerminologia {

    /** Passada read-only: mede o que a lore mudaria, sem tocar um byte. */
    ENSAIO,

    /** Passada real: o disco foi escrito, com backup por sessão e troca atômica. */
    APLICADO,

    /**
     * Pedido de aplicação recusado antes de abrir qualquer arquivo, porque
     * {@code correcao-cache.reforco-terminologia-aplicar-habilitado} está desligada.
     */
    RECUSADO_POR_CONFIG
}
