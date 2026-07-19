package org.traducao.projeto.contexto.lore.evangelion;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore de Evangelion 3.33 (Rebuild 3.0) — WILLE / Wunder.
 *
 * <p>INVARIANTES DO DOMÍNIO: Asuka Shikinami Langley; Mari Illustrious Makinami;
 * WILLE; AAA Wunder; Unit-13.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoEvangelion333 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Evangelion: 3.33 You Can (Not) Redo (Evangelion: 3.0) — Rebuild.
        - Premissa: 14 anos após Near Third Impact; WILLE vs NERV; Shinji desperta no AAA Wunder.
        - Personagens (gênero): Shinji Ikari (m), Kaworu Nagisa (m),
          Asuka Shikinami Langley (f) — NÃO "Asuka Langley" solto nem "Soryu";
          Mari Illustrious Makinami (f), Misato Katsuragi (f), Ritsuko Akagi (f),
          Rei Ayanami / Ayanami (f — clones/continuidade Rebuild), Gendo Ikari (m),
          Kozo Fuyutsuki (m), Ryoji Kaji (referências).
        - Termos/orgs: WILLE, AAA Wunder, NERV, SEELE, EVA Unit-13, EVA Unit-08,
          Central Dogma, Spear of Longinus, Spear of Cassius, DSS Choker, AT Field, LCL.
        - Regras: Asuka Shikinami Langley; Mari Illustrious Makinami; Wunder/WILLE oficiais;
          Spears mantêm nome EN; não misturar com TV clássica.
        - Tom: desolação, culpa, guerra fria WILLE/NERV, Kaworu empático.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Evangelion: 3.33 You Can (Not) Redo", LORE);

    @Override
    public String getId() {
        return "evangelion_333";
    }

    @Override
    public String getNomeExibicao() {
        return "Evangelion: 3.33 You Can (Not) Redo";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Shinji Ikari", "Kaworu Nagisa", "Asuka Shikinami Langley",
            "Mari Illustrious Makinami", "Misato Katsuragi", "Ritsuko Akagi",
            "Rei Ayanami", "Gendo Ikari", "Kozo Fuyutsuki",
            "Ryoji Kaji", "WILLE", "AAA Wunder",
            "NERV", "SEELE", "EVA Unit-13",
            "EVA Unit-08", "Central Dogma", "Spear of Longinus",
            "Spear of Cassius", "DSS Choker", "AT Field",
            "LCL", "Near Third Impact", "Evangelion"
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
        return CorrecoesTerminologiaEvangelion.comExtras(Map.ofEntries(
            Map.entry("Asuka Langley Soryu", "Asuka Shikinami Langley"),
            Map.entry("Lança de Longinus", "Spear of Longinus"),
            Map.entry("Lança de Cassius", "Spear of Cassius")
        ));
    }
}
