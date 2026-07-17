package org.traducao.projeto.contexto.lore.evangelion;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore de Evangelion 3.0+1.0 — Village-3 / Additional Impact.
 *
 * <p>INVARIANTES DO DOMÍNIO: Asuka Shikinami Langley; Mari Illustrious Makinami;
 * Village-3; Golgotha Object; não usar "Asuka Langley" incompleto.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoEvangelion3010 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Evangelion: 3.0+1.0 Thrice Upon a Time (também 3.0+1.01) — Rebuild final.
        - Personagens (gênero): Shinji Ikari (m), Rei Ayanami (f),
          Asuka Shikinami Langley (f) — NÃO "Asuka Langley" genérico nem "Soryu";
          Mari Illustrious Makinami (f), Gendo Ikari (m), Misato Katsuragi (f),
          Ritsuko Akagi (f), Kaworu Nagisa (m), Kozo Fuyutsuki (m), Kensuke Aida (m),
          Toji Suzuhara (m), Hikari Horaki (f).
        - Locais/termos: Village-3 (Village 3), WILLE, AAA Wunder, NERV, Anti-Universe,
          Golgotha Object, Additional Impact, Neon Genesis, Spears, AT Field, LCL,
          EVA Units (incl. Unit-08 etc. conforme cena).
        - Regras: grafias Rebuild completas; Village-3 como nome de lugar; não traduzir
          "Neon Genesis" quando for conceito/título do filme; não misturar com TV.
        - Tom: redención, cotidiano em Village-3, confronto final com Gendo, tom adult/cinemático.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Evangelion: 3.0+1.0 Thrice Upon a Time", LORE);

    @Override
    public String getId() {
        return "evangelion_3010";
    }

    @Override
    public String getNomeExibicao() {
        return "Evangelion: 3.0+1.0 Thrice Upon a Time";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Shinji Ikari", "Rei Ayanami", "Asuka Shikinami Langley",
            "Mari Illustrious Makinami", "Gendo Ikari", "Misato Katsuragi",
            "Village-3", "Golgotha Object", "WILLE", "AAA Wunder", "NERV"
        );
    }
}
