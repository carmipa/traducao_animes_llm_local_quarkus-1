package org.traducao.projeto.contexto.lore.sidonia;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore do filme Sidonia — corrige Izana Shinatose.
 *
 * <p>INVARIANTES DO DOMÍNIO: Izana Shinatose (nunca Shinoshinari); Nagate; Tsumugi;
 * Gauna/Garde.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoSidoniaFilme implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Knights of Sidonia: Love Woven in the Stars (Sidonia no Kishi: Ai Tsumugu Hoshi) — filme.
        - Continuidade: mesma ambientação da série (nave-semente Sidonia vs Gauna).
        - Personagens (gênero): Nagate Tanikaze (m), Tsumugi Shiraui (f — híbrida),
          Izana Shinatose (f/terceiro gênero conforme a obra — NUNCA "Shinoshinari"),
          Yuhata Midorikawa (f), Captain Kobayashi (f), Norio Kunato (m),
          Lala Hiyama (f), Shizuka Hoshijiro (f — quando referenciada).
        - Termos: Sidonia, Gauna, Garde/Gardes, Kabizashi, Ena, Heigus particles,
          Toha Heavy Industries, Immortal Ship Committee.
        - Regras: Izana Shinatose (grafia canônica da série/filme); Garde como mecha;
          Ena não vira "pele"; Gauna core / placenta como termos biológicos da obra.
        - Tom: ficção científica militar e romance existencial; preservar estranhamento biológico.
        """;

    private static final String PROMPT = ContextoPrompt.montar(
        "Knights of Sidonia: Love Woven in the Stars (Filme)", LORE);

    @Override
    public String getId() {
        return "sidonia_movie";
    }

    @Override
    public String getNomeExibicao() {
        return "Knights of Sidonia: Love Woven in the Stars (Filme)";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Nagate Tanikaze", "Tsumugi Shiraui", "Izana Shinatose",
            "Yuhata Midorikawa", "Kobayashi", "Norio Kunato",
            "Lala Hiyama", "Shizuka Hoshijiro", "Sidonia",
            "Gauna", "Garde", "Kabizashi",
            "Ena", "Heigus", "placenta",
            "Gauna core", "Toha Heavy Industries", "Immortal Ship Committee"
        );
    }
}
