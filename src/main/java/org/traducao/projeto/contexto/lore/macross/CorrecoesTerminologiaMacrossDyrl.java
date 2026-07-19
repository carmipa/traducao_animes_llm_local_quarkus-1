package org.traducao.projeto.contexto.lore.macross;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: mapa determinístico de Macross: Do You Remember Love?
 * (base franquia + Protoculture / Meltrandi / Minmay Attack).
 *
 * <p>INVARIANTES DO DOMÍNIO: Protoculture (NUNCA localizar como mitologia genérica);
 * Meltrandi; Valkyrie ≠ Valquíria; anti-Veritech.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
 */
public final class CorrecoesTerminologiaMacrossDyrl {

    private CorrecoesTerminologiaMacrossDyrl() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve o mapa DYRL completo.
     *
     * <p>INVARIANTES DO DOMÍNIO: frases longas relevantes antes de tokens curtos via enforcer.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sempre o mesmo mapa.
     */
    public static Map<String, String> mapa() {
        Map<String, String> m = new LinkedHashMap<>();
        m.putAll(CorrecoesTerminologiaMacross.mapa());
        m.put("Protocultura", "Protoculture");
        m.put("Meltrandy", "Meltrandi");
        m.put("Meltrandii", "Meltrandi");
        m.put("Ataque Minmay", "Minmay Attack");
        m.put("Ataque de Minmay", "Minmay Attack");
        m.put("Zentrades", "Zentradi");
        return Collections.unmodifiableMap(m);
    }
}
