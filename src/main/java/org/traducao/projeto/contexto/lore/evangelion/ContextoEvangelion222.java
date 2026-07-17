package org.traducao.projeto.contexto.lore.evangelion;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore de Evangelion 2.22 (Rebuild 2.0) com Asuka Shikinami.
 *
 * <p>INVARIANTES DO DOMÍNIO: Asuka Shikinami Langley; Mari Illustrious Makinami;
 * Near Third Impact; Unit-03 / Bardiel.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoEvangelion222 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Evangelion: 2.22 You Can (Not) Advance (Evangelion: 2.0) — Rebuild.
        - Personagens (gênero): Shinji Ikari (m), Asuka Shikinami Langley (f) — NÃO "Soryu";
          Mari Illustrious Makinami (f), Rei Ayanami (f), Misato Katsuragi (f),
          Gendo Ikari (m), Ritsuko Akagi (f), Ryoji Kaji (m), Toji Suzuhara (m).
        - Mecha/termos: EVA Unit-02, EVA Unit-03, Beast Mode, Zeruel, Bardiel,
          Near Third Impact, NERV, SEELE, AT Field, LCL, Entry Plug.
        - Regras: Asuka Shikinami Langley (Rebuild) ≠ Asuka Langley Soryu (TV);
          Mari Illustrious Makinami — nome completo preferível a "Mari Makinami";
          não misturar continidades.
        - Tom: ação ampliada, rivalidade Asuka/Shinji, virada apocalíptica no clímax.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Evangelion: 2.22 You Can (Not) Advance", LORE);

    @Override
    public String getId() {
        return "evangelion_222";
    }

    @Override
    public String getNomeExibicao() {
        return "Evangelion: Filme 2 - 2.22 You Can (Not) Advance";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Shinji Ikari", "Asuka Shikinami Langley", "Mari Illustrious Makinami",
            "Rei Ayanami", "Misato Katsuragi", "Zeruel", "Bardiel", "NERV",
            "Near Third Impact", "AT Field"
        );
    }
}
