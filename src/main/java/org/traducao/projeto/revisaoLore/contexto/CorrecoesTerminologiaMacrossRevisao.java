package org.traducao.projeto.revisaoLore.contexto;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: mapa determinístico Macross na fatia revisaoLore (espelho do
 * mapa da Tradução; sem importar {@code contexto.lore}).
 *
 * <p>INVARIANTES DO DOMÍNIO: chave = forma-ruim PT; valor = canônico.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
 */
public final class CorrecoesTerminologiaMacrossRevisao {

    private CorrecoesTerminologiaMacrossRevisao() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: correções transversais Macross para a Opção 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: Valkyrie/Zentradi oficiais; sem Veritech.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sempre o mesmo mapa.
     */
    public static Map<String, String> mapa() {
        return Map.of(
            "Valquíria", "Valkyrie",
            "Valquiria", "Valkyrie",
            "Veritech", "Valkyrie",
            "Zentraedi", "Zentradi",
            "Zentradii", "Zentradi"
        );
    }
}
