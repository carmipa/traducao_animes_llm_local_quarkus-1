package org.traducao.projeto.contexto.lore.breakblade;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: mapa determinístico dos 6 filmes Break Blade / Broken Blade —
 * Golem, Quartz, un-sorcerer, Kingdom of Krisna, Athens Commonwealth, Delphine e
 * Valkyrie Squadron (unidade de Zess; NÃO confundir com Macross Valkyrie).
 *
 * <p>INVARIANTES DO DOMÍNIO: chave = forma-ruim PT (ou grafia errada); valor = canônico EN;
 * só aplica se o original EN contém o canônico.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
 */
public final class CorrecoesTerminologiaBreakBlade {

    private CorrecoesTerminologiaBreakBlade() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve o mapa completo da franquia Break Blade (6 filmes).
     *
     * <p>INVARIANTES DO DOMÍNIO: Delphine/Under-Golem/un-sorcerer oficiais; Krisna ≠ Krishna
     * como forma preferida nos materiais da adaptação.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sempre o mesmo mapa imutável.
     */
    public static Map<String, String> mapa() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Goleme", "Golem");
        m.put("Golems mágicos", "Golems");
        m.put("Quartzo", "Quartz");
        m.put("Não-feiticeiro", "un-sorcerer");
        m.put("Nao-feiticeiro", "un-sorcerer");
        m.put("Sem-magia", "un-sorcerer");
        m.put("Infecundo", "un-sorcerer");
        m.put("Sub-Golem", "Under-Golem");
        m.put("Golem Ancestral", "Under-Golem");
        m.put("Delfine", "Delphine");
        m.put("Delfíng", "Delphine");
        m.put("Delfing", "Delphine");
        m.put("Delphing", "Delphine");
        m.put("Cavaleiro Pesado", "Heavy Knight");
        m.put("Reino de Krisna", "Kingdom of Krisna");
        m.put("Reino de Krishna", "Kingdom of Krisna");
        m.put("Comunidade de Atenas", "Athens Commonwealth");
        m.put("Federação de Atenas", "Athens Commonwealth");
        m.put("Federacao de Atenas", "Athens Commonwealth");
        m.put("Império de Orlando", "Orlando Empire");
        m.put("Imperio de Orlando", "Orlando Empire");
        m.put("Esquadrão Valquíria", "Valkyrie Squadron");
        m.put("Esquadrao Valquiria", "Valkyrie Squadron");
        m.put("Esquadrão Valquiria", "Valkyrie Squadron");
        m.put("Lâmina Quebrada", "Broken Blade");
        m.put("Lamina Quebrada", "Broken Blade");
        m.put("Cruzão", "Cruzon");
        m.put("Cruzao", "Cruzon");
        m.put("Hykélion", "Hykelion");
        m.put("Hykelión", "Hykelion");
        m.put("Eltemus", "Eltemis");
        m.put("Eltemús", "Eltemis");
        return Collections.unmodifiableMap(m);
    }
}
