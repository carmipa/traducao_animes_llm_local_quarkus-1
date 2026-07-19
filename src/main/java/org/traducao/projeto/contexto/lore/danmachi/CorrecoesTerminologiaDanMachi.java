package org.traducao.projeto.contexto.lore.danmachi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: mapa canônico COMPARTILHADO das obras DanMachi — formas-ruim (PT)
 * → grafia oficial. A política da franquia mantém a maioria dos termos em PT natural
 * (masmorra genérica, Distrito do Prazer), então o mapa é enxuto: fixa só o que é erro
 * claro (ex.: "Familia" nunca leva acento; grafias erradas de nomes próprios).
 *
 * <p>INVARIANTES DO DOMÍNIO: chave = forma-ruim em PT; valor = canônico; o enforcer só aplica
 * quando o original EN contém o canônico na grafia exata.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mapas imutáveis; sem I/O.
 */
public final class CorrecoesTerminologiaDanMachi {

    private static final Map<String, String> NUCLEO = Map.of(
        "Família", "Familia"
    );

    private CorrecoesTerminologiaDanMachi() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo comum DanMachi (Familia sem acento).
     * <p>INVARIANTES DO DOMÍNIO: mapa imutável.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sempre o mesmo mapa.
     */
    public static Map<String, String> mapa() {
        return NUCLEO;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo combinado com termos específicos da obra (ex.: grafias
     * erradas de nomes de personagens que só aparecem em certas temporadas).
     * <p>INVARIANTES DO DOMÍNIO: extras sobrescrevem o núcleo; resultado imutável.
     * <p>COMPORTAMENTO EM CASO DE FALHA: extras vazios devolvem o núcleo.
     */
    public static Map<String, String> comExtras(Map<String, String> extras) {
        Map<String, String> combinado = new LinkedHashMap<>(NUCLEO);
        combinado.putAll(extras);
        return Collections.unmodifiableMap(combinado);
    }
}
