package org.traducao.projeto.contexto.lore.gundam;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: mapa canônico COMPARTILHADO das obras Gundam da Universal Century —
 * formas-ruim (PT) → grafia oficial (EN) que o LLM tende a localizar indevidamente (Newtype,
 * Mobile Suit, Beam Saber). Obras com termos próprios (ex.: Axis em Char's Counterattack,
 * Sleeves em Unicorn/NT) combinam este núcleo com seus extras via {@link #comExtras(Map)}.
 *
 * <p>INVARIANTES DO DOMÍNIO: chave = forma-ruim em PT; valor = canônico EN/oficial; o enforcer
 * só aplica quando o original EN contém o canônico na grafia exata (ex.: só troca "Eixo"→"Axis"
 * se a fala tinha "Axis"), nunca tocando a palavra comum fora de contexto.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mapas imutáveis; sem I/O.
 */
public final class CorrecoesTerminologiaGundamUc {

    private static final Map<String, String> NUCLEO = Map.ofEntries(
        Map.entry("Novo Tipo", "Newtype"),
        Map.entry("Neotipo", "Newtype"),
        Map.entry("Velho Tipo", "Oldtype"),
        Map.entry("Traje Móvel", "Mobile Suit"),
        Map.entry("Robô Móvel", "Mobile Suit"),
        Map.entry("Terno Móvel", "Mobile Suit"),
        Map.entry("Armadura Móvel", "Mobile Armor"),
        // Compostos MEIO-traduzidos (o LLM localiza "Mobile"->"Móvel"/"Móveis" mas mantém "Suit"),
        // e os plurais das formas-ruim acima — o canônico plural "Mobile Suits" casa o EN
        // "Mobile suits" (checagem multi-palavra é case-insensitive no EnforcadorTermosLore).
        Map.entry("Móvel Suit", "Mobile Suit"),
        Map.entry("Suit Móvel", "Mobile Suit"),
        Map.entry("Móveis Suits", "Mobile Suits"),
        Map.entry("Suits Móveis", "Mobile Suits"),
        Map.entry("Trajes Móveis", "Mobile Suits"),
        Map.entry("Robôs Móveis", "Mobile Suits"),
        Map.entry("Ternos Móveis", "Mobile Suits"),
        Map.entry("Armaduras Móveis", "Mobile Armors"),
        Map.entry("Sabre de Raio", "Beam Saber"),
        Map.entry("Sabre de Feixe", "Beam Saber"),
        Map.entry("Fuzil de Feixe", "Beam Rifle"),
        Map.entry("Rifle de Feixe", "Beam Rifle")
    );

    private CorrecoesTerminologiaGundamUc() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o núcleo comum de terminologia UC, sem extras da obra.
     * <p>INVARIANTES DO DOMÍNIO: mapa imutável; forma-ruim → canônico.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sempre o mesmo mapa.
     */
    public static Map<String, String> mapa() {
        return NUCLEO;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o núcleo UC combinado com os termos específicos de uma obra
     * (ex.: {@code Eixo→Axis} em Char's Counterattack).
     * <p>INVARIANTES DO DOMÍNIO: os extras sobrescrevem o núcleo em chave repetida; o resultado
     * é imutável.
     * <p>COMPORTAMENTO EM CASO DE FALHA: extras vazios devolvem o núcleo.
     */
    public static Map<String, String> comExtras(Map<String, String> extras) {
        Map<String, String> combinado = new LinkedHashMap<>(NUCLEO);
        combinado.putAll(extras);
        return Collections.unmodifiableMap(combinado);
    }
}
