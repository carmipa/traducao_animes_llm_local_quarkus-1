package org.traducao.projeto.revisaoLore.contexto;

import java.util.Collections;
import java.util.LinkedHashMap;
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
