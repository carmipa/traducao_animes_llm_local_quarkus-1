package org.traducao.projeto.revisaoLore.contexto;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: mapa Delta na fatia revisaoLore (espelho da Tradução).
 *
 * <p>INVARIANTES DO DOMÍNIO: Walküre ≠ Valkyrie; chave = forma-ruim PT; valor = canônico.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
 */
public final class CorrecoesTerminologiaMacrossDeltaRevisao {

    private CorrecoesTerminologiaMacrossDeltaRevisao() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico Macross Delta para a Opção 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: idêntico ao mapa da Tradução Local Delta.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sempre o mesmo mapa.
     */
    public static Map<String, String> mapa() {
        return Map.ofEntries(
            Map.entry("Valquíria", "Valkyrie"),
            Map.entry("Valquiria", "Valkyrie"),
            Map.entry("Veritech", "Valkyrie"),
            Map.entry("Zentraedi", "Zentradi"),
            Map.entry("Zentradii", "Zentradi"),
            Map.entry("Walkure", "Walküre"),
            Map.entry("Síndrome Var", "Var Syndrome"),
            Map.entry("Sindrome Var", "Var Syndrome"),
            Map.entry("Síndrome de Var", "Var Syndrome"),
            Map.entry("Ondas Fold", "Fold Waves"),
            Map.entry("Onda Fold", "Fold Waves"),
            Map.entry("Ondas de Fold", "Fold Waves"),
            Map.entry("Esquadrão Delta", "Delta Flight"),
            Map.entry("Esquadrao Delta", "Delta Flight"),
            Map.entry("Cavaleiros Aéreos", "Aerial Knights"),
            Map.entry("Cavaleiros Aereos", "Aerial Knights"),
            Map.entry("Protocultura", "Protoculture"),
            Map.entry("Yami Q Ray", "Yami_Q_Ray"),
            Map.entry("Yami Q-Ray", "Yami_Q_Ray"),
            Map.entry("Heimdal", "Heimdall"),
            Map.entry("Gigante Macross", "Macross Gigant"),
            Map.entry("Macross Gigante", "Macross Gigant")
        );
    }
}
