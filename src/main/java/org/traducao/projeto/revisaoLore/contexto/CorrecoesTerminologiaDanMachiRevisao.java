package org.traducao.projeto.revisaoLore.contexto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: mapa canônico DanMachi na fatia revisaoLore (espelho da política
 * de terminologia da franquia — sem import cruzado com {@code contexto.lore}). Fixa erros
 * claros de localização: Familia com acento, Falna/Excelia/Valis acentuados, Dungeon→Masmorra
 * e termos de sistema que o LLM costuma “melhorar” para PT.
 *
 * <p>INVARIANTES DO DOMÍNIO: chave = forma-ruim PT; valor = canônico EN/oficial (caixa exata);
 * extras sobrescrevem; só ADICIONA pares (não remove o núcleo existente).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mapas imutáveis; sem I/O.
 */
public final class CorrecoesTerminologiaDanMachiRevisao {

    private static final Map<String, String> NUCLEO;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Família", "Familia");
        m.put("Fálna", "Falna");
        m.put("Excélia", "Excelia");
        m.put("Vális", "Valis");
        m.put("Masmorra", "Dungeon");
        m.put("Pedra Mágica", "Magic Stone");
        m.put("Pedra Magica", "Magic Stone");
        m.put("Jogo de Guerra", "War Game");
        m.put("Espada Mágica", "Magic Sword");
        m.put("Espada Magica", "Magic Sword");
        NUCLEO = Collections.unmodifiableMap(m);
    }

    private CorrecoesTerminologiaDanMachiRevisao() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo comum DanMachi para todas as temporadas/OVAs/filme.
     *
     * <p>INVARIANTES DO DOMÍNIO: mapa imutável; Familia sem acento.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sempre o mesmo mapa.
     */
    public static Map<String, String> mapa() {
        return NUCLEO;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo combinado com termos próprios da temporada
     * (grafias erradas de nomes, monstro, Familia).
     *
     * <p>INVARIANTES DO DOMÍNIO: extras sobrescrevem o núcleo; resultado imutável.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: extras nulos/vazios devolvem o núcleo.
     */
    public static Map<String, String> comExtras(Map<String, String> extras) {
        Map<String, String> combinado = new LinkedHashMap<>(NUCLEO);
        if (extras != null) {
            combinado.putAll(extras);
        }
        return Collections.unmodifiableMap(combinado);
    }
}
