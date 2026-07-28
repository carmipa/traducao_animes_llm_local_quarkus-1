package org.traducao.projeto.revisaoLore.contexto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: núcleo Evangelion na fatia {@code revisaoLore} (espelho de
 * {@code CorrecoesTerminologiaEvangelion} — sem import cruzado da fatia {@code contexto}).
 *
 * <h2>Por que duplicar em vez de importar</h2>
 * É a mesma decisão já tomada em {@link CorrecoesTerminologiaGundamUcRevisao} e nas irmãs de
 * Macross: duplicação consciente é preferível a acoplar as duas fatias. O preço é manter os dois
 * lados em dia, e ele é cobrado — {@code ParidadeMapasTerminologiaTest} compara os catálogos e
 * acusa qualquer forma-ruim com destinos diferentes.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Chave = forma-ruim PT; valor = canônico. Extras sobrescrevem o núcleo.</li>
 *   <li>O núcleo tem UMA entrada e isso não é descuido: o que separa as continuidades de
 *       Evangelion ({@code Asuka Langley Soryu} da TV vs {@code Asuka Shikinami Langley} do
 *       Rebuild) NÃO pode morar aqui. Se morasse, a série clássica seria "corrigida" para a
 *       grafia do Rebuild — o oposto do que a lore de cada obra manda. Essa separação vive nos
 *       EXTRAS de cada filme, e só nos filmes.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Mapa imutável; extras vazios devolvem o núcleo. Sem I/O.
 */
public final class CorrecoesTerminologiaEvangelionRevisao {

    private static final Map<String, String> NUCLEO = Map.of(
        "Campo AT", "AT Field"
    );

    private CorrecoesTerminologiaEvangelionRevisao() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o núcleo comum a todas as continuidades de Evangelion.
     * <p>INVARIANTES DO DOMÍNIO: mapa imutável.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sempre o mesmo mapa.
     */
    public static Map<String, String> mapa() {
        return NUCLEO;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo combinado com os termos específicos da obra.
     * <p>INVARIANTES DO DOMÍNIO: extras sobrescrevem o núcleo; resultado imutável. Os extras de
     * cada obra têm de ser IDÊNTICOS aos do lado da Tradução, senão a catraca de paridade acusa.
     * <p>COMPORTAMENTO EM CASO DE FALHA: extras vazios devolvem o núcleo.
     */
    public static Map<String, String> comExtras(Map<String, String> extras) {
        Map<String, String> combinado = new LinkedHashMap<>(NUCLEO);
        combinado.putAll(extras);
        return Collections.unmodifiableMap(combinado);
    }
}
