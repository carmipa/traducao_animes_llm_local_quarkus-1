package org.traducao.projeto.contexto.lore.danmachi;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore da 4ª temporada de DanMachi (Deep Floors / Labyrinth).
 *
 * <p>INVARIANTES DO DOMÍNIO: Rivira, Juggernaut, Ryu Lion; Liliruca Arde;
 * Mikoto Yamato (ordem ocidental).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoDanMachiS4 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Is It Wrong to Try to Pick Up Girls in a Dungeon? IV / DanMachi Season 4 (Deep Floors / Labyrinth Arc).
        - Titulo e abreviacao: manter DanMachi, Dungeon, Familia e Orario como termos da obra; nao traduzir nomes proprios de personagens, Familias, andares, monstros especiais ou tecnicas.
        - Personagens principais: Bell Cranel (homem), Hestia (deusa, mulher), Ryu Lion / Ryu Lion (mulher), Liliruca Arde/Lili (mulher), Welf Crozzo (homem), Mikoto Yamato (mulher), Haruhime Sanjouno (mulher), Cassandra Ilion (mulher), Daphne Lauros (mulher), Aisha Belka (mulher), Chigusa Hitachi (mulher), Ouka Kashima (homem), Marie (mulher; nixe/espirito das aguas em Rivira), Jura Halmer (homem).
        - Familias e grupos: Hestia Familia, Hermes Familia, Miach Familia, Takemikazuchi Familia, Loki Familia, Astraea Familia, Xenos. Manter "Familia" como termo da obra.
        - Locais: Orario, Dungeon, Rivira, Water Capital, Great Falls, Deep Floors, Lower Floors, Colosseum. Preserve nomes canonicos de lugar.
        - Monstros/termos: Juggernaut, Moss Huge, Lambton, Amphisbaena, Monster Rex, Irregular, Floor Boss, Magic Stone, Status, Level, Skill, Familia Myth.
        - Tecnicas/equipamentos: Firebolt, Hestia Knife, Hakugen, Crozzo Magic Sword.
        - Regras de nomes: Ryu/Ryu nao vira "dragao"; Bell Cranel nao vira "sino"; Welf Crozzo nao adaptar; Cassandra, Daphne, Aisha, Haruhime, Mikoto e Marie mantem grafia.
        - Tom: sobrevivencia no Dungeon com trauma, exaustao e tensao; Bell humilde, Ryu contida/culpada, Cassandra ansiosa/profetica.
        """;

    private static final String PROMPT = ContextoPrompt.montar("DanMachi (Season 4)", LORE);

    @Override
    public String getId() {
        return "danmachi_s4";
    }

    @Override
    public String getNomeExibicao() {
        return "DanMachi (Season 4)";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege nomes e monstros da S4.
     * <p>INVARIANTES DO DOMÍNIO: só artefatos da temporada.
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Bell Cranel", "Hestia", "Ryu Lion", "Liliruca Arde", "Welf Crozzo",
            "Mikoto Yamato", "Haruhime Sanjouno", "Cassandra Ilion", "Daphne Lauros",
            "Juggernaut", "Rivira", "Orario", "Dungeon", "Firebolt", "Hakugen"
        );
    }
}
