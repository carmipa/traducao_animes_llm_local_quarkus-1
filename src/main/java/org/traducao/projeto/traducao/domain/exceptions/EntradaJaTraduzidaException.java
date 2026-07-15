package org.traducao.projeto.traducao.domain.exceptions;

/**
 * PROPÓSITO DE NEGÓCIO: Sinaliza que a entrada aparenta já estar em PT-BR e a
 * retradução não foi confirmada — o arquivo é bloqueado para não retraduzir e
 * sobrescrever trabalho bom.
 *
 * <p>INVARIANTES DO DOMÍNIO: só lançada quando a heurística de caminho já
 * traduzido dispara e {@code permitirRetraducao} é falso.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: é a própria sinalização; herda de
 * {@link ArquivoLegendaException} para o lote registrar o arquivo como
 * BLOQUEADO e seguir para o próximo.
 */
public class EntradaJaTraduzidaException extends ArquivoLegendaException {
    public EntradaJaTraduzidaException(String message) {
        super(message);
    }
}
