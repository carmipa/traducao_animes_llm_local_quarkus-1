package org.traducao.projeto.revisaoLore.contexto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: mapa DYRL na fatia revisaoLore (espelho da Tradução).
 *
 * <p>INVARIANTES DO DOMÍNIO: Protoculture; Meltrandi; Valkyrie; anti-Veritech.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
 */
public final class CorrecoesTerminologiaMacrossDyrlRevisao {

    private CorrecoesTerminologiaMacrossDyrlRevisao() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico DYRL para a Opção 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: idêntico ao mapa da Tradução Local DYRL.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sempre o mesmo mapa.
     */
    public static Map<String, String> mapa() {
        Map<String, String> m = new LinkedHashMap<>();
        m.putAll(CorrecoesTerminologiaMacrossRevisao.mapa());
        m.put("Protocultura", "Protoculture");
        m.put("Meltrandy", "Meltrandi");
        m.put("Meltrandii", "Meltrandi");
        m.put("Ataque Minmay", "Minmay Attack");
        m.put("Ataque de Minmay", "Minmay Attack");
        m.put("Zentrades", "Zentradi");
        return Collections.unmodifiableMap(m);
    }
}
