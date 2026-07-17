package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore enriquecida de Macross 7 (série TV) / Fire Bomber.
 *
 * <p>INVARIANTES DO DOMÍNIO: Basara Nekki; Fire Bomber; Protodeviln; Sound Force;
 * VF-19 Custom Fire Valkyrie.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoMacross7 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross 7 (série TV).
        - Premissa: a frota Macross 7 enfrenta os Protodeviln; Basara Nekki da banda Fire Bomber
          combate com música (Song Energy) em vez de só armas; Sound Force.
        - Personagens (gênero): Basara Nekki (m), Mylene Flare Jenius (f), Ray Lovelock (m),
          Veffidas Feaze (f), Gamlin Kizaki (m), Maximilian Jenius / Max Jenius (m),
          Milia Fallyna Jenius (f), Dr. Chiba (m), Exsedol Folmo (m), Akiko Houjoh (f),
          Physica S. Folgom (f), Gigile (m — Protodeviln), Lord Quamzin (m).
        - Bandas/unidades: Fire Bomber, Sound Force, Diamond Force, Jamming Birds.
        - Termos: Protodeviln, Anima Spiritia, Song Energy, Spiritia, Zentradi, NUNS/UN Spacy era Macross 7,
          City 7, Battle 7.
        - Mecha: VF-19 Custom Fire Valkyrie (Basara), VF-11 Thunderbolt, VF-17 Nightmare,
          Queadluun-Rau, Macross 7 / Battle 7.
        - Regras: Fire Bomber e nomes dos músicos oficiais; Protodeviln não traduzir;
          Basara não "adaptar" o nome; letras de música cantáveis.
        - Tom: rock/idol + mecha; Basara obstinado e idealista; Mylene entre idol e piloto.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross 7", LORE);

    @Override
    public String getId() {
        return "macross_7";
    }

    @Override
    public String getNomeExibicao() {
        return "Macross 7 (Série TV)";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Basara Nekki", "Mylene Flare Jenius", "Ray Lovelock", "Veffidas Feaze",
            "Gamlin Kizaki", "Fire Bomber", "Protodeviln", "Sound Force",
            "VF-19 Custom Fire Valkyrie", "City 7"
        );
    }
}
