package org.traducao.projeto.traducao.contexto.danmachi;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

@Component
public class ContextoDanMachiS4 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Is It Wrong to Try to Pick Up Girls in a Dungeon? IV / DanMachi Season 4 (Deep Floors / Labyrinth Arc).
        - Titulo e abreviacao: manter DanMachi, Dungeon, Familia e Orario como termos da obra; nao traduzir nomes proprios de personagens, Familias, andares, monstros especiais ou tecnicas.
        - Personagens principais: Bell Cranel (homem), Hestia (deusa, mulher), Ryuu Lion / Ryu Lion (mulher), Liliruca Arde/Lili (mulher), Welf Crozzo (homem), Mikoto Yamato (mulher), Haruhime Sanjouno (mulher), Cassandra Ilion (mulher), Daphne Lauros (mulher), Aisha Belka (mulher), Chigusa Hitachi (mulher), Ouka Kashima (homem), Marie (mulher), Jura Halmer (homem).
        - Familias e grupos: Hestia Familia, Hermes Familia, Miach Familia, Takemikazuchi Familia, Loki Familia, Astraea Familia, Xenos. Manter "Familia" como termo da obra quando aparecer como nome de grupo.
        - Locais: Orario, Dungeon, Rivira, Water Capital, Great Falls, Deep Floors, Lower Floors, Colosseum. Se usar portugues para descricao, preserve o nome canonico quando for nome de lugar.
        - Monstros/termos: Juggernaut, Moss Huge, Lambton, Amphisbaena, Monster Rex, Irregular, Floor Boss, Magic Stone, Status, Level, Skill, Familia Myth.
        - Tecnicas/equipamentos: Firebolt, Hestia Knife, Hakugen, Crozzo Magic Sword. Manter nomes de tecnicas e armas oficiais.
        - Regras de nomes: Ryuu/Ryu nao deve virar "dragao"; Bell Cranel nao deve virar "sino"; Welf Crozzo nao deve ser adaptado; Cassandra, Daphne, Aisha, Haruhime, Mikoto e Marie mantem grafia original.
        - Tom: aventura de sobrevivencia no Dungeon com trauma, exaustao e tensao crescente; Bell fala com coragem humilde, Ryuu e contida e culpada, Cassandra e ansiosa/profetica.
        """;

    private static final String PROMPT = ContextoPrompt.montar("DanMachi (Season 4)", LORE);

    @Override public String getId() { return "danmachi_s4"; }
    @Override public String getNomeExibicao() { return "DanMachi (Season 4)"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
