package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore para DanMachi: Arrow of the Orion (filme).
 *
 * <p>INVARIANTES DO DOMÍNIO: Liliruca Arde ≠ Lilisuka/Liriruca; Artemis; Familia.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreDanMachiOrion implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: DanMachi: Arrow of the Orion (filme).
        - Regra: corrigir APENAS nomenclatura. Liliruca Arde NUNCA Liriruca/Lilisuka.
        - Personagens: Bell Cranel, Hestia, Artemis, Aiz Wallenstein, Hermes,
          Liliruca Arde, Welf Crozzo, Mikoto Yamato, Haruhime Sanjouno.
        - Termos: Familia, Falna, Dungeon, Status, Level, Orario.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override
    public String getId() {
        return "danmachi_movie";
    }

    @Override
    public String getNomeExibicao() {
        return "DanMachi: Arrow of the Orion - Revisao de Lore";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo + extras do filme (Lilisuka/Liriruca).
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho da Tradução Orion (sem import).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaDanMachiRevisao.comExtras(Map.ofEntries(
            Map.entry("Lilisuka", "Liliruca Arde"),
            Map.entry("Liriruca", "Liliruca Arde"),
            Map.entry("Sino Cranel", "Bell Cranel")
        ));
    }
}
