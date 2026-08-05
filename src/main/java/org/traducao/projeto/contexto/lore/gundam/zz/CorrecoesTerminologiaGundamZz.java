package org.traducao.projeto.contexto.lore.gundam.zz;

import org.traducao.projeto.contexto.lore.gundam.CorrecoesTerminologiaGundamUc;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: mapa canônico de Gundam ZZ — núcleo UC + Axis/Titans/Ple/Quin Mantha/ZZ.
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
     * <p>INVARIANTES DO DOMÍNIO: Axis/Titans/Ple/Quin Mantha/ZZ Gundam oficiais.
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
            Map.entry("Zeta Duplo", "ZZ Gundam"),
            Map.entry("Senhorita Haman", "Lady Haman"),
            Map.entry("Corpo Azul", "Blue Corps"),
            Map.entry("Corpos Azuis", "Blue Corps"),
            Map.entry("Cubely", "Qubeley"),
            Map.entry("Qubelei", "Qubeley"),
            Map.entry("Neo Zéon", "Neo Zeon"),
            Map.entry("Neo Zeón", "Neo Zeon"),
            Map.entry("Facção Glemy", "Glemy Faction"),
            Map.entry("Faccao Glemy", "Glemy Faction"),

            // ---------------------------------------------------------------------------
            // FA YUIRY. Minerado no acervo em 2026-08-04: a fala cujo texto INTEIRO e "Fa"
            // aparece 51 vezes (Zeta 43, ZZ 8) e so 8 preservaram o nome. O modelo le "Fa!"
            // como palavra comum e produz cinco coisas diferentes:
            //
            //   Fogo!  27x     Fa...  ->  Fá...  6x     Pá!   1x
            //   Fala!   7x                             Fale!  1x
            //
            // "Fa Yuiry" JA esta em termosProtegidos desde sempre, e nao adiantou: aquele
            // conjunto isenta o termo da checagem de residuo, nao restaura grafia. Quem
            // restaura e este mapa.
            //
            // SEGURANCA: a restauracao so dispara quando o INGLES contem "Fa", e o canonico
            // e de UMA palavra -- entao a checagem e SENSIVEL A CAIXA e "fa" minusculo nunca
            // casa. Um "Fire!" qualquer jamais e tocado, porque nao traz "Fa" no original.
            //
            // COLISAO MEDIDA no acervo: 6 falas trazem "Fa" no ingles e uma destas formas em
            // PT dentro de fala MAIOR -- e as 6 sao o MESMO defeito, nao colisao legitima:
            //   EN "Fa! Katz!"                -> PT "Fala, Katz!"
            //   EN "Fa, taking off!"          -> PT "Fá, decolando!"
            //   EN "Fa! We're outnumbered!"   -> PT "Fogo! Estamos em menor número!"
            // A regra corrige as seis.
            //
            // RISCO RESIDUAL DECLARADO: "Fala" e "Fale" sao palavras comuns em PT. Se um dia
            // uma fala trouxer "Fa" no ingles E um "fala" MINUSCULO legitimo ("Fa, escute
            // minha fala"), o orcamento de minusculas de restaurarLimitado pode troca-lo.
            // Hoje isso e ZERO no acervo; se aparecer, o sintoma sera "Fa" no meio da frase.
            //
            // "Faça isso" (1x) fica de fora: nao e variante do nome, e outra frase inteira.
            Map.entry("Fogo", "Fa"),
            Map.entry("Fala", "Fa"),
            Map.entry("Fá", "Fa"),
            Map.entry("Pá", "Fa"),
            Map.entry("Fale", "Fa")
        ));
    }
}
