package org.traducao.projeto.contexto.lore.evangelion;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore da série TV clássica Neon Genesis Evangelion.
 *
 * <p>INVARIANTES DO DOMÍNIO: Asuka Langley Soryu (TV, não Shikinami); NERV; SEELE;
 * Angels; Human Instrumentality Project.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoEvangelionTV implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Neon Genesis Evangelion (série TV clássica, 1995) — NÃO é a tetralogia Rebuild.
        - Premissa: após o Second Impact, adolescentes pilotam Evangelions contra os Angels sob a NERV.
        - Personagens (gênero): Shinji Ikari (m), Rei Ayanami (f), Asuka Langley Soryu (f) — grafia TV;
          Misato Katsuragi (f), Gendo Ikari (m), Ritsuko Akagi (f), Kaworu Nagisa (m),
          Toji Suzuhara (m), Kensuke Aida (m), Hikari Horaki (f), Kozo Fuyutsuki (m),
          Ryoji Kaji (m), Maya Ibuki (f), Makoto Hyuga (m), Shigeru Aoba (m).
        - Organizações/termos: NERV, SEELE, Gehirn, MAGI, Angels (Anjos), Evangelion / EVA,
          EVA Unit-00, EVA Unit-01, EVA Unit-02, AT Field, LCL, Dummy Plug, Entry Plug,
          Second Impact, Third Impact, Human Instrumentality Project (Projeto de Instrumentalidade Humana —
          manter nome oficial; não inventar "Human Instrumentation").
        - Locais: Tokyo-3, GeoFront, NERV HQ, Lake Ashinoko / Mt. Fuji region.
        - Regras: Asuka Langley Soryu (TV) ≠ Asuka Shikinami Langley (Rebuild); não misturar continidades;
          NERV/SEELE/MAGI/LCL/AT Field mantêm forma oficial; Angel pode ser "Anjo" em fala natural se o original permitir, mas nomes de Anjos (Sachiel, Shamshel, Ramiel, etc.) ficam oficiais.
        - Tom: psicológico, militar-tecnocrático, melancólico; Shinji hesitante, Asuka agressiva/orgulhosa, Rei distante.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Neon Genesis Evangelion", LORE);

    @Override
    public String getId() {
        return "evangelion_tv";
    }

    @Override
    public String getNomeExibicao() {
        return "Evangelion (Série TV Clássica)";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Shinji Ikari", "Rei Ayanami", "Asuka Langley Soryu",
            "Misato Katsuragi", "Gendo Ikari", "Ritsuko Akagi",
            "Kaworu Nagisa", "Toji Suzuhara", "Kensuke Aida",
            "Hikari Horaki", "Kozo Fuyutsuki", "Ryoji Kaji",
            "Maya Ibuki", "Makoto Hyuga", "Shigeru Aoba",
            "NERV", "SEELE", "Gehirn",
            "MAGI", "EVA Unit-00", "EVA Unit-01",
            "EVA Unit-02", "AT Field", "LCL",
            "Dummy Plug", "Entry Plug", "Second Impact",
            "Third Impact", "Human Instrumentality Project", "Sachiel",
            "Shamshel", "Ramiel", "Tokyo-3",
            "GeoFront", "Evangelion"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reforço determinístico de terminologia própria da obra
     * (nomes/termos que a lore manda manter no original).
     * <p>INVARIANTES DO DOMÍNIO: forma-ruim PT → canônico; só aplica se o EN contém o canônico.
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaEvangelion.mapa();
    }
}
