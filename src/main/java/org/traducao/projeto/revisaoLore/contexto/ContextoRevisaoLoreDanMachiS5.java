package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

@Component
public class ContextoRevisaoLoreDanMachiS5 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: DanMachi Season 5 (Goddess of Fertility Arc).
        - Regra central: manter DanMachi, Familia e Orario como termos da obra; nao traduzir nomes de personagens, Familias, deuses, titulos, apelidos, locais ou tecnicas.
        - Personagens: Bell Cranel, Hestia, Freya, Syr Flover, Horn, Ottar, Allen Fromel, Hedin Selland, Hogni Ragnar, Heith Velvet, Mia Grand, Ryu/Ryuu Lion, Ais Wallenstein, Liliruca Arde/Lili, Welf Crozzo, Haruhime Sanjouno.
        - Familias/grupos: Hestia Familia, Freya Familia, Loki Familia, Hostess of Fertility, Benevolent Mistress.
        - Locais: Orario, Babel, Folkvangr, Hostess of Fertility, Pleasure Quarter.
        - Titulos/termos: Goddess of Beauty, Goddess of Fertility, War Game, Familia, Level, Status, Skill, Charm.
        - Alertas: Bell Cranel nao vira "sino"; Syr Flover nao vira "flor"; Charm de Freya e poder divino, nao charme casual.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "danmachi_s5"; }
    @Override public String getNomeExibicao() { return "DanMachi S5 - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo DanMachi + extras S5 (Freya Familia, Hostess of Fertility).
     *
     * <p>INVARIANTES DO DOMÍNIO: Família Freya→Freya Familia; Anfitriã da Fertilidade→Hostess.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaDanMachiRevisao.comExtras(Map.ofEntries(
            Map.entry("Lilisuka", "Liliruca Arde"),
            Map.entry("Liriruca", "Liliruca Arde"),
            Map.entry("Sino Cranel", "Bell Cranel"),
            Map.entry("Família Freya", "Freya Familia"),
            Map.entry("Familia Freya", "Freya Familia"),
            Map.entry("Anfitriã da Fertilidade", "Hostess of Fertility"),
            Map.entry("Anfitria da Fertilidade", "Hostess of Fertility"),
            Map.entry("Deusa da Fertilidade", "Goddess of Fertility"),
            Map.entry("Deusa da Beleza", "Goddess of Beauty")
        ));
    }
}
