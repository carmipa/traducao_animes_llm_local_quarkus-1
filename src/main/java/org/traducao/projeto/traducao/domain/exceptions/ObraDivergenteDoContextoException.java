package org.traducao.projeto.traducao.domain.exceptions;

import org.traducao.projeto.legenda.domain.ArquivoLegendaException;

/**
 * PROPÓSITO DE NEGÓCIO: sinaliza que a obra reconhecida no caminho do arquivo NÃO é a obra
 * cujo contexto/lore está selecionado — e por isso aquele arquivo foi bloqueado antes de
 * qualquer chamada ao LLM e antes de qualquer escrita de cache. É a exceção que faltava no
 * incidente medido nesta árvore: 15 caches de Gundam 0083 com
 * {@code proveniencia.contextoId = "guilty_crown"} e ~4.442 entradas sob a lore errada,
 * produzidas sem que nada no pipeline reclamasse.
 *
 * <p>INVARIANTES DO DOMÍNIO: só é lançada com PROVA POSITIVA de divergência — algum
 * contexto registrado reconheceu a pasta e o contexto ativo não está entre eles. Caminho
 * que nenhum contexto reconhece NUNCA a lança (vira aviso). É por arquivo: bloquear um não
 * derruba o lote.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: é a própria sinalização; herda de
 * {@link ArquivoLegendaException} (módulo {@code legenda}), a mesma raiz de
 * {@link EntradaJaTraduzidaException}, para que o lote registre o arquivo como BLOQUEADO,
 * contabilize a telemetria e siga para o próximo — sem cache gravado e sem legenda publicada.
 */
public class ObraDivergenteDoContextoException extends ArquivoLegendaException {
    public ObraDivergenteDoContextoException(String message) {
        super(message);
    }
}
