package org.traducao.projeto.contexto.lore.gundam.zz;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: mapa canônico de formas-ruim → grafia oficial de Gundam ZZ,
 * compartilhado entre Tradução Local e (por cópia espelhada) Revisao de Lore.
 *
 * <p>INVARIANTES DO DOMÍNIO: chave = forma-ruim em PT; valor = canônico EN/oficial;
 * o enforcer só aplica se o original EN contém o canônico.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
 */
public final class CorrecoesTerminologiaGundamZz {

    private CorrecoesTerminologiaGundamZz() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve o mapa de restauração determinística do ZZ.
     *
     * <p>INVARIANTES DO DOMÍNIO: Axis/Titans/Newtype/Ple nunca localizados.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sempre o mesmo mapa imutável.
     */
    public static Map<String, String> mapa() {
        return Map.of(
            "Eixo", "Axis",
            "Titãs", "Titans",
            "Titas", "Titans",
            "Novo Tipo", "Newtype",
            "Plê", "Ple",
            "Plee", "Ple"
        );
    }
}
