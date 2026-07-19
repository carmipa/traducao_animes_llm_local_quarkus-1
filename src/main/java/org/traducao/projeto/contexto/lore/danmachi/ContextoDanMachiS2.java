package org.traducao.projeto.contexto.lore.danmachi;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore da 2ª temporada de DanMachi (arco Ishtar / War Game /
 * Haruhime) para tradução fiel de nomes e termos.
 *
 * <p>INVARIANTES DO DOMÍNIO: Liliruca Arde; Haruhime Sanjouno; Ishtar Familia;
 * Pleasure Quarter; não traduzir Bell como "sino".
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoDanMachiS2 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Is It Wrong to Try to Pick Up Girls in a Dungeon? Season 2 (DanMachi II).
        - Arco: Apollo Familia e War Game; conflito com Ishtar Familia no Pleasure Quarter;
          resgate de Haruhime Sanjouno; Freya observa Bell de longe.
        - Locais: Orario, Dungeon, Pleasure Quarter (Distrito do Prazer / Rua do Entretenimento),
          Babel, Hostess of Fertility.
        - Familias/grupos: Hestia Familia, Apollo Familia, Ishtar Familia, Hermes Familia,
          Freya Familia, Loki Familia. Manter "Familia" como termo da obra.
        - Personagens (gênero): Bell Cranel (m), Hestia (f), Liliruca Arde / Lili (f),
          Welf Crozzo (m), Mikoto Yamato (f), Haruhime Sanjouno (f), Aisha Belka (f),
          Phryne Jamil (f), Ishtar (f), Freya (f), Hermes (m), Aiz Wallenstein (f),
          Syr Flover (f), Ryu Lion (f), Ottar (m), Hyakinthos Clio (m).
        - Termos: Falna, Status, Level, War Game, Dungeon, Excelia, Magic Sword, Crozzo Magic Sword.
        - Regras: Bell Cranel ≠ "sino"; Haruhime não adaptar; Ishtar/Freya/Hermes mantêm grafia;
          Lili = apelido de Liliruca Arde.
        - Tom: tensão política entre Familias, humor, romance e combate no War Game.
        """;

    private static final String PROMPT = ContextoPrompt.montar("DanMachi (Season 2)", LORE);

    @Override
    public String getId() {
        return "danmachi_s2";
    }

    @Override
    public String getNomeExibicao() {
        return "DanMachi (Season 2)";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege nomes e termos canônicos da S2.
     * <p>INVARIANTES DO DOMÍNIO: só artefatos confirmados da temporada.
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Bell Cranel", "Hestia", "Liliruca Arde", "Lili", "Welf Crozzo", "Mikoto Yamato",
            "Haruhime Sanjouno", "Aisha Belka", "Ishtar", "Freya", "Hermes", "Syr Flover",
            "Orario", "Dungeon", "Falna", "War Game"
        );
    }
}
