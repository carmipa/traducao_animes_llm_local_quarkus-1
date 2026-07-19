package org.traducao.projeto.contexto.lore.gundam.zz;

import org.traducao.projeto.contexto.lore.gundam.CorrecoesTerminologiaGundamUc;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: mapa canônico de Gundam ZZ — núcleo UC + Axis/Titans/Ple/Quin Mantha.
 *
 * <p>INVARIANTES DO DOMÍNIO: chave = forma-ruim PT; valor = canônico; só aplica com canônico no EN.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
 */
public final class CorrecoesTerminologiaGundamZz {

    private CorrecoesTerminologiaGundamZz() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve o mapa de restauração determinística do ZZ.
     *
     * <p>INVARIANTES DO DOMÍNIO: Axis/Titans/Newtype/Ple/Quin Mantha oficiais.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sempre o mesmo mapa imutável.
     */
    public static Map<String, String> mapa() {
        return CorrecoesTerminologiaGundamUc.comExtras(Map.ofEntries(
            Map.entry("Eixo", "Axis"),
            Map.entry("Titãs", "Titans"),
            Map.entry("Titas", "Titans"),
            Map.entry("Plê", "Ple"),
            Map.entry("Plee", "Ple"),
            Map.entry("Rainha Mansa", "Quin Mantha"),
            Map.entry("Zeta Duplo", "ZZ Gundam")
        ));
    }
}
