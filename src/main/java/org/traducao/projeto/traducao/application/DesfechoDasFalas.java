package org.traducao.projeto.traducao.application;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: diz, fala por fala, o que o PIPELINE decidiu sobre a tradução — e não o
 * que o texto final aparenta. Leva do episódio até a publicação duas informações que antes se
 * perdiam no caminho:
 * <ul>
 *   <li>a CAUSA real de cada fala em que o pipeline desistiu e manteve o original, para o
 *       relatório e o KPI não chamarem de "eco do modelo" uma decisão do sistema (A7);</li>
 *   <li>quais falas foram traduzidas pela SEGUNDA OPINIÃO (outro modelo), para o cache não as
 *       gravar sob o carimbo do modelo principal — a promessa que {@code ProcessarEpisodioUseCase}
 *       declarava desde 11/08/2026 e que não tinha consumidor nenhum.</li>
 * </ul>
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>As chaves são o texto ORIGINAL da fala (não o mascarado), o mesmo do mapa de traduções
 *       que o chamador recebe.</li>
 *   <li>Vive por arquivo: o chamador cria um novo a cada tradução de arquivo.</li>
 *   <li>Só o {@link TradutorLotesService} escreve; os demais leem pelas visões imutáveis.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Não lança. Fala sem registro significa "o pipeline não desistiu dela" — ausência de causa nunca
 * é inventada.
 */
public final class DesfechoDasFalas {

    private final Map<String, String> causaDoOriginalMantido = new LinkedHashMap<>();
    private final Set<String> traduzidasPorSegundaOpiniao = new LinkedHashSet<>();

    void registrarOriginalMantido(String original, String causa) {
        causaDoOriginalMantido.put(original, causa);
    }

    void registrarSegundaOpiniao(String original) {
        traduzidasPorSegundaOpiniao.add(original);
    }

    /** Original → causa real de o pipeline ter mantido o original. */
    public Map<String, String> causaDoOriginalMantido() {
        return Collections.unmodifiableMap(causaDoOriginalMantido);
    }

    /** Originais cuja tradução veio do modelo de recuperação, não do principal. */
    public Set<String> traduzidasPorSegundaOpiniao() {
        return Collections.unmodifiableSet(traduzidasPorSegundaOpiniao);
    }
}
