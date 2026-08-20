package org.traducao.projeto.traducaoKaraoke.application;

import org.traducao.projeto.traducaoKaraoke.domain.TelemetriaKaraoke;
import org.traducao.projeto.traducaoKaraoke.infrastructure.TelemetriaKaraokeDataset;

import java.util.ArrayList;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: dublê da escritora do acervo que guarda as linhas em MEMÓRIA, para os
 * testes provarem o que a execução publicaria sem escrever no {@code logs/} da máquina.
 *
 * <h2>Por que existe como arquivo próprio</h2>
 * Dois testes precisam dele — o do registro e o do use case —, e {@code null} no lugar dele seria
 * pior que inútil: {@code acrescentarAoDataset} engole {@link RuntimeException} de propósito, então
 * um colaborador ausente faria o caminho inteiro do dataset passar pelo {@code catch} e os testes
 * ficariam verdes sem nunca terem exercitado uma linha. Guarda cega aprova por não enxergar nada.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Não falha: acumula e devolve. É deliberadamente burro — a decisão do que gravar é do código
 * sob teste, não do dublê.
 */
class AcervoKaraokeCapturado extends TelemetriaKaraokeDataset {

    final List<TelemetriaKaraoke> linhas = new ArrayList<>();

    @Override
    public int registrar(List<TelemetriaKaraoke> novas) {
        if (novas == null) {
            return 0;
        }
        linhas.addAll(novas);
        return novas.size();
    }
}
