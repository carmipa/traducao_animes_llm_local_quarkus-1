package org.traducao.projeto.contexto.lore.evangelion;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: mapa canônico COMPARTILHADO das obras Evangelion — formas-ruim (PT)
 * → grafia oficial. Enxuto por política: a franquia permite "Anjo" e "Instrumentalidade" em
 * PT natural, então o mapa fixa só o que é termo próprio / erro claro (AT Field; nos Rebuild,
 * o erro de continuidade Asuka "Soryu" → "Shikinami" e "Lança" → "Spear").
 *
 * <p>INVARIANTES DO DOMÍNIO: chave = forma-ruim PT; valor = canônico; o enforcer só aplica
 * quando o original EN contém o canônico na grafia exata.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mapas imutáveis; sem I/O.
 */
public final class CorrecoesTerminologiaEvangelion {

    private static final Map<String, String> NUCLEO = Map.of(
        "Campo AT", "AT Field"
    );

    private CorrecoesTerminologiaEvangelion() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo comum Evangelion (AT Field).
     * <p>INVARIANTES DO DOMÍNIO: mapa imutável.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sempre o mesmo mapa.
     */
    public static Map<String, String> mapa() {
        return NUCLEO;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo combinado com os termos específicos do filme (ex.: o erro
     * de continuidade de Asuka nos Rebuild).
     * <p>INVARIANTES DO DOMÍNIO: extras sobrescrevem o núcleo; resultado imutável.
     * <p>COMPORTAMENTO EM CASO DE FALHA: extras vazios devolvem o núcleo.
     */
    public static Map<String, String> comExtras(Map<String, String> extras) {
        Map<String, String> combinado = new LinkedHashMap<>(NUCLEO);
        combinado.putAll(extras);
        return Collections.unmodifiableMap(combinado);
    }
}
