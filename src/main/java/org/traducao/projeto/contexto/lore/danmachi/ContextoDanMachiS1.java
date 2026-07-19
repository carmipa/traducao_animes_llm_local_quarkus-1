package org.traducao.projeto.contexto.lore.danmachi;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore da 1ª temporada de DanMachi (arco inicial em Orario /
 * Minotaur / formação da Hestia Familia) para o LLM e o detector de tradução idêntica.
 *
 * <p>INVARIANTES DO DOMÍNIO: nomes oficiais EN/JP-romanizados; Liliruca Arde (não
 * "Lilisuka"); ordem ocidental Mikoto Yamato; termos Familia/Falna/Dungeon protegidos.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoDanMachiS1 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Is It Wrong to Try to Pick Up Girls in a Dungeon? Season 1 (DanMachi I).
        - Arco: Bell Cranel chega a Orario, encontra a deusa Hestia, forma a Hestia Familia,
          conhece Aiz Wallenstein e enfrenta o Minotaur no Dungeon.
        - Local central: Orario (cidade-labirinto sobre o Dungeon); Babel; Guilda; Hostess of Fertility.
        - Termos de mundo (manter quando forem lore): Dungeon, Familia, Falna, Status, Level, Skill,
          Magic, Excelia, Adventurer, War Game. "Familia" sem acento como nome de grupo (Hestia Familia).
        - Personagens (gênero): Bell Cranel (m), Hestia (f), Liliruca Arde / Lili (f), Welf Crozzo (m),
          Mikoto Yamato (f), Aiz Wallenstein (f), Eina Tulle (f), Syr Flover (f), Ryu Lion (f),
          Finn Deimne (m), Riveria Ljos Alf (f), Gareth Landrock (m), Bete Loga (m),
          Tiona Hiryute (f), Tione Hiryute (f), Lefiya Viridis (f), Ottar (m), Freya (f).
        - Regras de nomes: Liliruca Arde NUNCA "Lilisuka"; Bell Cranel não vira "sino";
          Aiz Wallenstein = Sword Princess / Princesa Espadachim de forma consistente;
          Mikoto Yamato (ordem ocidental: prenome + sobrenome).
        - Armas/termos: Hestia Knife, Firebolt, Argonaut, Minotaur, Goliath, Black Goliath.
        - Tom: aventura leve, romance tímido, Bell sincero; Hestia carinhosa/ciumenta.
        """;

    private static final String PROMPT = ContextoPrompt.montar("DanMachi (Season 1)", LORE);

    @Override
    public String getId() {
        return "danmachi_s1";
    }

    @Override
    public String getNomeExibicao() {
        return "DanMachi (Season 1)";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege nomes e termos canônicos da S1.
     * <p>INVARIANTES DO DOMÍNIO: só artefatos confirmados da temporada.
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável; detector trata o resto como pendência.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Bell Cranel", "Hestia", "Liliruca Arde", "Lili", "Welf Crozzo", "Mikoto Yamato",
            "Aiz Wallenstein", "Eina Tulle", "Syr Flover", "Ryu Lion", "Firebolt", "Hestia Knife",
            "Orario", "Dungeon", "Falna", "Familia"
        );
    }
}
