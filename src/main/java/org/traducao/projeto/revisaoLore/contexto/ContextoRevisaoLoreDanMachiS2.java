package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore para DanMachi II (War Game / Ishtar / Haruhime).
 *
 * <p>INVARIANTES DO DOMÍNIO: Haruhime Sanjouno; Liliruca; War Game; Pleasure Quarter.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreDanMachiS2 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: DanMachi II (Season 2) — Apollo Familia, War Game, Ishtar, Haruhime.
        - Regra: corrigir APENAS nomenclatura. Familia sem acento.
        - Personagens: Bell Cranel, Hestia, Liliruca Arde, Welf Crozzo, Mikoto Yamato,
          Haruhime Sanjouno, Aisha Belka, Ishtar, Freya, Hermes, Aiz Wallenstein.
        - Termos: War Game, Dungeon, Familia, Falna, Pleasure Quarter, Crozzo Magic Sword.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override
    public String getId() {
        return "danmachi_s2";
    }

    @Override
    public String getNomeExibicao() {
        return "DanMachi S2 - Revisao de Lore";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo + extras S2 (Liliruca, Haruhime).
     *
     * <p>INVARIANTES DO DOMÍNIO: grafias oficiais da temporada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaDanMachiRevisao.comExtras(Map.ofEntries(
            Map.entry("Lilisuka", "Liliruca Arde"),
            Map.entry("Liriruca", "Liliruca Arde"),
            Map.entry("Sino Cranel", "Bell Cranel"),
            Map.entry("Haruhime Sanjono", "Haruhime Sanjouno"),
            Map.entry("Haruhime Sanjouono", "Haruhime Sanjouno")
        ));
    }
}
