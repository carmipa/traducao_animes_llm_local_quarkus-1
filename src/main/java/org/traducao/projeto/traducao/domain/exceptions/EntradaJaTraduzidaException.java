package org.traducao.projeto.traducao.domain.exceptions;

import org.traducao.projeto.legenda.domain.ArquivoLegendaException;

/**
 * PROPÓSITO DE NEGÓCIO: Sinaliza que a entrada aparenta já estar em PT-BR e a
 * retradução não foi confirmada — o arquivo é bloqueado para não retraduzir e
 * sobrescrever trabalho bom. É regra específica do fluxo de tradução e por isso
 * permanece na fatia {@code traducao}.
 *
 * <p>INVARIANTES DO DOMÍNIO: só lançada quando a heurística de caminho já
 * traduzido dispara e {@code permitirRetraducao} é falso.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: é a própria sinalização; herda de
 * {@link ArquivoLegendaException} (módulo {@code legenda}) para o lote registrar o
 * arquivo como BLOQUEADO e seguir para o próximo; a captura específica em
 * {@code TraducaoController} permanece válida.
 */
public class EntradaJaTraduzidaException extends ArquivoLegendaException {
    public EntradaJaTraduzidaException(String message) {
        super(message);
    }
}
