package org.traducao.projeto.novoKaraoke.domain;

/**
 * Uma linha de letra de música reconstruída a partir do bloco de eventos KFX.
 * O tempo é herdado literalmente dos eventos de origem: {@code inicioCs} é o
 * menor início e {@code fimCs} o maior fim do grupo — nenhum deslocamento é
 * introduzido, a legenda simples ocupa exatamente a janela do efeito original.
 *
 * @param texto            texto visível da linha (sem tags)
 * @param inicioCs         menor início do grupo, em centésimos
 * @param fimCs            maior fim do grupo, em centésimos
 * @param eventosOrigem    quantos eventos KFX foram colapsados nesta linha
 * @param variantesTexto   variantes divergentes encontradas na mesma janela (>1 indica voto majoritário)
 */
public record LinhaSimplesKaraoke(
    String texto,
    long inicioCs,
    long fimCs,
    int eventosOrigem,
    int variantesTexto
) {

    public String inicioAss() {
        return EventoAss.csParaTempo(inicioCs);
    }

    public String fimAss() {
        return EventoAss.csParaTempo(fimCs);
    }

    /** Emite o evento Dialogue simples (camada 0, estilo dedicado, sem tags). */
    public String paraEventoAss(String nomeEstilo) {
        return "Dialogue: 0," + inicioAss() + "," + fimAss() + "," + nomeEstilo + ",,0,0,0,," + texto;
    }
}
