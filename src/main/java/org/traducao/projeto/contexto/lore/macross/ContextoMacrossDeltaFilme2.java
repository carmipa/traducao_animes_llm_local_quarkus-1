package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoMacrossDeltaFilme2 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross Delta O Filme 2: Absolute Live!!!!!! (Zettai Live!!!!!!).
        - Continuidade inédita pós-série TV de Macross Delta. A ameaça da organização militar clandestina Heimdall e a unidade rival cibernética Yami_Q_Ray.
        - Unidade Musical Walküre (Todas Femininas): Freyja Wion, Mikumo Guynemer, Kaname Buccaneer, Makina Nakajima, Reina Prowler.
        - Unidade Rival Yami_Q_Ray (Todas Femininas): Yami Mikumo, Yami Freyja, Yami Kaname, Yami Makina, Yami Reina.
        - Pilotos e Legado: Hayate Immelmann (homem), Mirage Farina Jenius (mulher), Arad Molders (homem), Maximilian Jenius / Max Jenius (homem / capitão da Macross Gigant), Ian Cromwell (homem).
        - Mechas: VF-31AX Kairos-Plus, Sv-301t Kairos-Plus, Macross Gigant.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross Delta: Filme 2 - Absolute Live!!!!!!", LORE);

    @Override public String getId() { return "macross_delta_filme2"; }
    @Override public String getNomeExibicao() { return "Macross Delta: Filme 2 - Absolute Live!!!!!!"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
