package org.traducao.projeto.revisaoLore.contexto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: mapa Macross II na fatia revisaoLore (espelho da Tradução).
 *
 * <p>INVARIANTES DO DOMÍNIO: Emulator; Minmay Attack; Marduk; Valkyrie; Song Energy.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
 */
public final class CorrecoesTerminologiaMacross2Revisao {

    private CorrecoesTerminologiaMacross2Revisao() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico Macross II para a Opção 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: idêntico ao mapa da Tradução Local Macross II.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sempre o mesmo mapa.
     */
    public static Map<String, String> mapa() {
        Map<String, String> m = new LinkedHashMap<>();
        m.putAll(CorrecoesTerminologiaMacrossRevisao.mapa());
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
