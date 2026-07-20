package org.traducao.projeto.revisaoLore.contexto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: mapa canônico DanMachi na fatia revisaoLore (espelho de
 * {@code CorrecoesTerminologiaDanMachi} da fatia contexto — sem import cruzado entre fatias,
 * como {@code CorrecoesTerminologiaGundamUcRevisao} espelha o UC). A franquia mantém a maioria
 * dos termos em PT natural; o mapa é enxuto e fixa só o erro claro: "Familia" (a organização
 * de uma divindade) NUNCA leva acento — distingue-a de "família" comum.
 *
 * <p>INVARIANTES DO DOMÍNIO: chave = forma-ruim PT; valor = canônico; extras sobrescrevem.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mapas imutáveis; sem I/O.
 */
public final class CorrecoesTerminologiaDanMachiRevisao {

    private static final Map<String, String> NUCLEO = Map.of(
        "Família", "Familia"
    );

    private CorrecoesTerminologiaDanMachiRevisao() {
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
     * PROPÓSITO DE NEGÓCIO: núcleo combinado com termos próprios da temporada (ex.: grafias
     * erradas de nomes de personagens).
     * <p>INVARIANTES DO DOMÍNIO: extras sobrescrevem o núcleo; resultado imutável.
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
