package org.traducao.projeto.contexto.lore.evangelion;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore de Evangelion 1.11 (Rebuild 1.0).
 *
 * <p>INVARIANTES DO DOMÍNIO: continuidade Rebuild; Sachiel/Shamshel/Ramiel; NERV.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoEvangelion111 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Evangelion: 1.11 You Are (Not) Alone (Evangelion: 1.0 You Are (Not) Alone) — Rebuild.
        - Continuidade: tetralogia Rebuild (não misturar com a série TV clássica).
        - Personagens (gênero): Shinji Ikari (m), Rei Ayanami (f), Misato Katsuragi (f),
          Gendo Ikari (m), Ritsuko Akagi (f), Kozo Fuyutsuki (m), Ryoji Kaji (m),
          Toji Suzuhara (m), Kensuke Aida (m), Maya Ibuki (f).
        - Mecha/Anjos/termos: NERV, SEELE, EVA Unit-00, EVA Unit-01, AT Field, LCL,
          Sachiel (3rd Angel), Shamshel (4th Angel), Ramiel (5th Angel / Fourth Angel em algumas
          nomenclaturas de marketing — preferir nomes oficiais Sachiel/Shamshel/Ramiel),
          Tokyo-3, GeoFront, Entry Plug, Dummy System.
        - Regras: nomes oficiais EN; NERV/SEELE/AT Field/LCL; não importar Asuka ainda (entra no 2.0);
          não usar "Asuka Langley Soryu" desta continuidade.
        - Tom: reintrodução cinematográfica, escala militar e isolamento de Shinji.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Evangelion: 1.11 You Are (Not) Alone", LORE);

    @Override
    public String getId() {
        return "evangelion_111";
    }

    @Override
    public String getNomeExibicao() {
        return "Evangelion: Filme 1 - 1.11 You Are (Not) Alone";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Shinji Ikari", "Rei Ayanami", "Misato Katsuragi", "Gendo Ikari",
            "NERV", "SEELE", "Ramiel", "Sachiel", "Shamshel", "AT Field", "Tokyo-3"
        );
    }
}
