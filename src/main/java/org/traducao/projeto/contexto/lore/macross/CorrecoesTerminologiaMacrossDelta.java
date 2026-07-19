package org.traducao.projeto.contexto.lore.macross;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: mapa determinístico de Macross Delta (TV + filmes) —
 * franquia Macross + formas-ruim específicas (Walküre, Var, Fold Waves, Aerial Knights).
 *
 * <p>INVARIANTES DO DOMÍNIO: Walküre (grupo idol) ≠ Valkyrie (mecha); chave = forma-ruim PT;
 * valor = canônico; só aplica se o original EN contém o canônico.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
 */
public final class CorrecoesTerminologiaMacrossDelta {

    private CorrecoesTerminologiaMacrossDelta() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve o mapa completo Delta (base Macross + termos da obra).
     *
     * <p>INVARIANTES DO DOMÍNIO: anti-Robotech; Walküre/Var Syndrome/Delta Flight oficiais.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sempre o mesmo mapa imutável.
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
