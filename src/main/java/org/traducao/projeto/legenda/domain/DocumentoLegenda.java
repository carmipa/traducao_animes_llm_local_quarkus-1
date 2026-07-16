package org.traducao.projeto.legenda.domain;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: representa, como dado imutável do módulo compartilhado
 * {@code legenda}, uma legenda inteira já parseada — o cabeçalho bruto, a sequência
 * ordenada de eventos ({@link EventoLegenda}) e os metadados de serialização
 * (marca de quebra de linha e presença de BOM) necessários para reescrever o arquivo
 * fielmente.
 *
 * <p>INVARIANTES DO DOMÍNIO: é um {@code record} — os quatro componentes são fixados
 * na construção e expostos pelos acessores; igualdade e hash derivam de todos eles.
 * A imutabilidade é rasa: a referência da lista {@code eventos} é armazenada como
 * recebida (não há cópia defensiva). Não há validação: qualquer valor, inclusive
 * {@code null} em qualquer componente, é aceito.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: o próprio record não lança nem sanitiza nada;
 * entradas inválidas (ex.: {@code eventos} nulo) só se manifestam quando um consumidor
 * as percorre. Nenhuma responsabilidade de I/O, parsing ou tradução vive aqui.
 *
 * @param cabecalho bloco bruto de cabeçalho da legenda, preservado para reescrita fiel
 * @param eventos sequência ordenada de eventos (linhas) da legenda
 * @param quebraDeLinha marca de quebra de linha original do arquivo, usada na serialização
 * @param comBom indica se o arquivo original tinha BOM, a ser reproduzido na escrita
 */
public record DocumentoLegenda(
    String cabecalho,
    List<EventoLegenda> eventos,
    String quebraDeLinha,
    boolean comBom
) {
}
