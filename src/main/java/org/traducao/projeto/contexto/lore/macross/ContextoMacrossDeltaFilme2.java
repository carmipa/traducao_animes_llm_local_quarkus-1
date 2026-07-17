package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore do filme Macross Delta 2 — Absolute Live!!!!!!
 *
 * <p>INVARIANTES DO DOMÍNIO: Heimdall; Yami_Q_Ray; VF-31AX Kairos-Plus; Max Jenius;
 * continuidade pós-série (não confundir com Passionate Walküre).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoMacrossDeltaFilme2 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross Delta the Movie: Absolute Live!!!!!! (劇場版マクロスΔ 絶対LIVE!!!!!! /
          Zettai Live!!!!!!).
        - Continuidade: inédita pós-série TV de Macross Delta (não é só condensação do filme 1).
        - Ameaça: organização clandestina Heimdall; unidade rival cibernética/idol Yami_Q_Ray.
        - Walküre (femininas): Freyja Wion, Mikumo Guynemer, Kaname Buccaneer, Makina Nakajima, Reina Prowler.
        - Yami_Q_Ray (femininas — espelhos/rivais): Yami Mikumo, Yami Freyja, Yami Kaname,
          Yami Makina, Yami Reina. Manter o prefixo "Yami" e o nome Yami_Q_Ray.
        - Pilotos/legado: Hayate Immelmann (m), Mirage Farina Jenius (f), Arad Molders (m),
          Maximilian Jenius / Max Jenius (m — capitão da Macross Gigant), Ian Cromwell (m).
        - Mecha/naves: VF-31AX Kairos-Plus, Sv-262 variants conforme cena, Macross Gigant.
        - Regras: Heimdall e Yami_Q_Ray oficiais; não fundir com Passionate Walküre;
          Max Jenius grafia consistente; canções cantáveis.
        - Tom: show ao vivo + combate; rivalidade idol Walküre vs Yami_Q_Ray; legado Jenius.
        """;

    private static final String PROMPT = ContextoPrompt.montar(
        "Macross Delta: Filme 2 - Absolute Live!!!!!!", LORE);

    @Override
    public String getId() {
        return "macross_delta_filme2";
    }

    @Override
    public String getNomeExibicao() {
        return "Macross Delta: Filme 2 - Absolute Live!!!!!!";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Freyja Wion", "Mikumo Guynemer", "Hayate Immelmann", "Mirage Farina Jenius",
            "Maximilian Jenius", "Heimdall", "Yami_Q_Ray", "Yami Mikumo", "Yami Freyja",
            "VF-31AX Kairos-Plus", "Macross Gigant", "Walküre"
        );
    }
}
