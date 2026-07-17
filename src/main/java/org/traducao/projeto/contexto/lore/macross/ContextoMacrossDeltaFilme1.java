package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore do filme Macross Delta — Passionate Walküre
 * (Gekijou no Walküre), alinhada à fila ativa de tradução.
 *
 * <p>INVARIANTES DO DOMÍNIO: Walküre; Windermere; Var Syndrome; mesmos nomes
 * canônicos da série Delta.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoMacrossDeltaFilme1 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross Delta the Movie: Passionate Walküre (劇場版マクロスΔ 激情のワルキューレ /
          Gekijouban Macross Delta: Gekijou no Walküre).
        - Natureza: releitura cinematográfica dos eventos da série TV Macross Delta —
          batalha contra o Reino de Windermere e o plano da Síndrome Var / Fold Waves.
        - Unidade Tática Musical Walküre (todas femininas): Freyja Wion, Mikumo Guynemer,
          Kaname Buccaneer, Makina Nakajima, Reina Prowler.
        - Esquadrão Delta / Delta Flight: Hayate Immelmann (m), Mirage Farina Jenius (f),
          Arad Molders (m), Chuck Mustang (m), Messer Ihlefeld (m).
        - Cavaleiros Aéreos de Windermere: Keith Aero Windermere (m), Heinz Nerich Windermere (m),
          Roid Brehm (m), Bogue Con-Vaart (m).
        - Orgs/termos: Chaos, NUNS, Walküre, Windermere, Var Syndrome (Síndrome Var),
          Fold Waves, Protocultura, Aerial Knights, Planeta Ragna.
        - Mecha/naves: VF-31 Siegfried, Sv-262 Draken III, Macross Elysion.
        - Nucleo Macross: Fighter/GERWALK/Battroid; Overtechnology; Fold Waves;
          musica Walkure = interferencia real (nao OST); PROIBIDO Veritech/Robotech.
        - Regras: Walküre / Windermere / nomes oficiais; Var Syndrome como termo medico-militar;
          cancoes cantaveis sem notas editoriais; nao misturar com Absolute Live (filme 2).
        - Tom: idol + mecha cinematografico; Freyja emotiva, Hayate impulsivo, Keith nobre/hostil.
        """;

    private static final String PROMPT = ContextoPrompt.montar(
        "Macross Delta: Filme 1 - Passionate Walküre", LORE);

    @Override
    public String getId() {
        return "macross_delta_filme1";
    }

    @Override
    public String getNomeExibicao() {
        return "Macross Delta: Filme 1 - Passionate Walküre";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Freyja Wion", "Mikumo Guynemer", "Kaname Buccaneer", "Makina Nakajima",
            "Reina Prowler", "Hayate Immelmann", "Mirage Farina Jenius", "Messer Ihlefeld",
            "Keith Aero Windermere", "Heinz Nerich Windermere", "Walküre", "Windermere",
            "Var Syndrome", "VF-31 Siegfried", "GERWALK", "Battroid", "Overtechnology", "Fold"
        );
    }
}
