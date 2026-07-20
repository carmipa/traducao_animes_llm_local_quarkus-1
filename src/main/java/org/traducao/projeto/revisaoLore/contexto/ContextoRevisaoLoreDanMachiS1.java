package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore (Opção 7) para DanMachi I (1ª temporada).
 *
 * <p>INVARIANTES DO DOMÍNIO: Liliruca Arde ≠ Lilisuka; Bell Cranel ≠ Sino Cranel;
 * Familia/Falna/Dungeon oficiais.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreDanMachiS1 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: DanMachi I (Season 1) — Orario, Hestia Familia, Minotaur.
        - Regra: corrigir APENAS nomenclatura. Familia sem acento; Liliruca Arde NUNCA Lilisuka.
        - Personagens: Bell Cranel, Hestia, Liliruca Arde/Lili, Welf Crozzo, Mikoto Yamato,
          Aiz Wallenstein, Eina Tulle, Syr Flover, Ryu Lion, Finn Deimne, Riveria Ljos Alf.
        - Termos: Dungeon, Familia, Falna, Status, Level, Skill, Excelia, Magic Stone,
          Firebolt, Hestia Knife, Argonaut, Minotaur, Goliath.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override
    public String getId() {
        return "danmachi_s1";
    }

    @Override
    public String getNomeExibicao() {
        return "DanMachi S1 - Revisao de Lore";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo DanMachi + grafias erradas da S1 (Lilisuka, Sino Cranel).
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho da política da temporada na Tradução (sem import).
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
