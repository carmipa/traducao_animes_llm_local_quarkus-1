package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

@Component
public class ContextoRevisaoLoreDanMachiS4 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: DanMachi Season 4 (Deep Floors / Labyrinth Arc).
        - Regra central: manter DanMachi, Dungeon, Familia e Orario como termos da obra; nao traduzir nomes proprios, Familias, andares, monstros especiais, armas ou tecnicas.
        - Personagens: Bell Cranel, Hestia, Ryuu Lion/Ryu Lion, Liliruca Arde/Lili, Welf Crozzo, Mikoto Yamato, Haruhime Sanjouno, Cassandra Ilion, Daphne Lauros, Aisha Belka, Chigusa Hitachi, Ouka Kashima, Marie, Jura Halmer.
        - Familias/grupos: Hestia Familia, Hermes Familia, Miach Familia, Takemikazuchi Familia, Loki Familia, Astraea Familia, Xenos.
        - Locais: Orario, Dungeon, Rivira, Water Capital, Great Falls, Deep Floors, Lower Floors, Colosseum.
        - Monstros/termos: Juggernaut, Moss Huge, Lambton, Amphisbaena, Monster Rex, Irregular, Floor Boss, Magic Stone, Status, Level, Skill, Familia Myth.
        - Tecnicas/equipamentos: Firebolt, Hestia Knife, Hakugen, Crozzo Magic Sword.
        - Alertas: Ryuu/Ryu nao vira "dragao"; Bell Cranel nao vira "sino"; nomes de monstros e tecnicas devem ficar na forma canonica.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "danmachi_s4"; }
    @Override public String getNomeExibicao() { return "DanMachi S4 - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }

    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaDanMachiRevisao.mapa();
    }
}
