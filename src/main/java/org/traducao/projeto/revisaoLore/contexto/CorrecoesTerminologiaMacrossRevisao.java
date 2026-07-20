package org.traducao.projeto.revisaoLore.contexto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: mapa determinístico Macross na fatia revisaoLore (espelho da
 * franquia — Valkyrie/Zentradi/Protoculture/Minmay Attack; sem importar contexto.lore).
 *
 * <p>INVARIANTES DO DOMÍNIO: chave = forma-ruim PT; valor = canônico; só ADICIONA pares.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
 */
public final class CorrecoesTerminologiaMacrossRevisao {

    private static final Map<String, String> NUCLEO;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Valquíria", "Valkyrie");
        m.put("Valquiria", "Valkyrie");
        m.put("Veritech", "Valkyrie");
        m.put("Zentraedi", "Zentradi");
        m.put("Zentradii", "Zentradi");
        m.put("Protocultura", "Protoculture");
        m.put("Ataque Minmay", "Minmay Attack");
        m.put("Ataque de Minmay", "Minmay Attack");
        m.put("Meltrandy", "Meltrandi");
        m.put("Meltrandii", "Meltrandi");
        m.put("Zentrades", "Zentradi");
        NUCLEO = Collections.unmodifiableMap(m);
    }

    private CorrecoesTerminologiaMacrossRevisao() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: correções transversais Macross para a Opção 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: Valkyrie/Zentradi/Protoculture oficiais; sem Veritech.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sempre o mesmo mapa.
     */
    public static Map<String, String> mapa() {
        return NUCLEO;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo Macross + extras da obra (Frontier, 7, etc.).
     *
     * <p>INVARIANTES DO DOMÍNIO: extras sobrescrevem; resultado imutável.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: extras nulos devolvem o núcleo.
     */
    public static Map<String, String> comExtras(Map<String, String> extras) {
        Map<String, String> combinado = new LinkedHashMap<>(NUCLEO);
        if (extras != null) {
            combinado.putAll(extras);
        }
        return Collections.unmodifiableMap(combinado);
    }
}
