package org.traducao.projeto.contexto.lore.macross;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: mapa determinístico de Macross Delta (TV + filmes) —
 * franquia Macross + formas-ruim específicas (Walküre, Var, Fold Waves, Aerial Knights,
 * Heimdall / Yami_Q_Ray / Macross Gigant no filme 2).
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
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Valquíria", "Valkyrie");
        m.put("Valquiria", "Valkyrie");
        m.put("Veritech", "Valkyrie");
        m.put("Zentraedi", "Zentradi");
        m.put("Zentradii", "Zentradi");
        m.put("Walkure", "Walküre");
        m.put("Síndrome Var", "Var Syndrome");
        m.put("Sindrome Var", "Var Syndrome");
        m.put("Síndrome de Var", "Var Syndrome");
        m.put("Ondas Fold", "Fold Waves");
        m.put("Onda Fold", "Fold Waves");
        m.put("Ondas de Fold", "Fold Waves");
        m.put("Bio-Ondas Fold", "Bio-Fold Waves");
        m.put("Esquadrão Delta", "Delta Flight");
        m.put("Esquadrao Delta", "Delta Flight");
        m.put("Cavaleiros Aéreos", "Aerial Knights");
        m.put("Cavaleiros Aereos", "Aerial Knights");
        m.put("Cavaleiros do Ar", "Aerial Knights");
        m.put("Protocultura", "Protoculture");
        m.put("Reino de Windermere", "Windermere Kingdom");
        m.put("Yami Q Ray", "Yami_Q_Ray");
        m.put("Yami Q-Ray", "Yami_Q_Ray");
        m.put("Heimdal", "Heimdall");
        m.put("Gigante Macross", "Macross Gigant");
        m.put("Macross Gigante", "Macross Gigant");
        return Collections.unmodifiableMap(m);
    }
}
