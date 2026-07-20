package org.traducao.projeto.contexto.lore.macross;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: mapa determinístico de Macross II: Lovers Again
 * (base franquia + Marduk / Emulator / Minmay Attack / Song Energy).
 *
 * <p>INVARIANTES DO DOMÍNIO: Emulator é cargo Marduk (não "emulador" de software);
 * Minmay Attack; Valkyrie oficial; anti-Robotech.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
 */
public final class CorrecoesTerminologiaMacross2 {

    private CorrecoesTerminologiaMacross2() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve o mapa Macross II completo.
     *
     * <p>INVARIANTES DO DOMÍNIO: anti-Robotech + termos Marduk.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sempre o mesmo mapa.
     */
    public static Map<String, String> mapa() {
        Map<String, String> m = new LinkedHashMap<>();
        m.putAll(CorrecoesTerminologiaMacross.mapa());
        m.put("Ataque Minmay", "Minmay Attack");
        m.put("Ataque de Minmay", "Minmay Attack");
        m.put("Emulador", "Emulator");
        m.put("Marduque", "Marduk");
        m.put("Protocultura", "Protoculture");
        m.put("Energia da Canção", "Song Energy");
        m.put("Energia da Cancao", "Song Energy");
        m.put("Sereia de Metal", "Metal Siren");
        m.put("Canhão Macross", "Macross Cannon");
        m.put("Canhao Macross", "Macross Cannon");
        m.put("Amantes de Novo", "Lovers Again");
        m.put("Spacy das Nações Unidas", "UN Spacy");
        m.put("Spacy das Nacoes Unidas", "UN Spacy");
        return Collections.unmodifiableMap(m);
    }
}
