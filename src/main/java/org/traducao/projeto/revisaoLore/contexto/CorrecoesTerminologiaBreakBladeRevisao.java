package org.traducao.projeto.revisaoLore.contexto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: mapa Break Blade / Broken Blade na fatia revisaoLore (espelho
 * da Tradução Local — Opção 7).
 *
 * <p>INVARIANTES DO DOMÍNIO: idêntico ao mapa de {@code CorrecoesTerminologiaBreakBlade};
 * sem import cruzado de {@code contexto.lore}; chave = forma-ruim PT; valor = canônico.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
 */
public final class CorrecoesTerminologiaBreakBladeRevisao {

    private CorrecoesTerminologiaBreakBladeRevisao() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico Break Blade para a Opção 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho da Tradução Local da franquia.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sempre o mesmo mapa.
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
