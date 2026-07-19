package org.traducao.projeto.revisaoLore.contexto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: núcleo UC na fatia revisaoLore (espelho de
 * CorrecoesTerminologiaGundamUc — sem import cruzado da fatia contexto).
 *
 * <p>INVARIANTES DO DOMÍNIO: chave = forma-ruim PT; valor = canônico; extras sobrescrevem.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mapas imutáveis; sem I/O.
 */
public final class CorrecoesTerminologiaGundamUcRevisao {

    private static final Map<String, String> NUCLEO = Map.ofEntries(
        Map.entry("Novo Tipo", "Newtype"),
        Map.entry("Neotipo", "Newtype"),
        Map.entry("Velho Tipo", "Oldtype"),
        Map.entry("Traje Móvel", "Mobile Suit"),
        Map.entry("Robô Móvel", "Mobile Suit"),
        Map.entry("Terno Móvel", "Mobile Suit"),
        Map.entry("Armadura Móvel", "Mobile Armor"),
        Map.entry("Sabre de Raio", "Beam Saber"),
        Map.entry("Sabre de Feixe", "Beam Saber"),
        Map.entry("Fuzil de Feixe", "Beam Rifle"),
        Map.entry("Rifle de Feixe", "Beam Rifle")
    );

    private CorrecoesTerminologiaGundamUcRevisao() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo UC para Opção 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: mapa imutável.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sempre o mesmo mapa.
     */
    public static Map<String, String> mapa() {
        return NUCLEO;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo UC + extras da obra na Revisao.
     *
     * <p>INVARIANTES DO DOMÍNIO: extras sobrescrevem chaves repetidas; LinkedHashMap.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: extras vazios devolvem o núcleo.
     */
    public static Map<String, String> comExtras(Map<String, String> extras) {
        Map<String, String> combinado = new LinkedHashMap<>(NUCLEO);
        if (extras != null) {
            combinado.putAll(extras);
        }
        return Collections.unmodifiableMap(combinado);
    }
}
