package org.traducao.projeto.traducao.contexto.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

public class ContextoMacrossDeltaFilmes implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross Delta (Filmes: Gekijouban Macross Delta: Gekjou no Walkuere & Zettai Live!!!!!!).
        - Premissa: Continuidade e releitura cinematográfica dos eventos de Macross Delta, incluindo a batalha contra a unidade cibernética/clonada Yami_Q_Ray e a ameaça de Heimdall em Absolute Live!!!!!!.
        - Unidade Tática Musical Walküre (Todas Femininas): Freyja Wion (mulher), Mikumo Guynemer (mulher), Kaname Buccaneer (mulher), Makina Nakajima (mulher), Reina Prowler (mulher).
        - Unidade Rival Yami_Q_Ray (Absolute Live!!!!!! - Todas Femininas): Yami Mikumo, Yami Freyja, Yami Kaname, Yami Makina, Yami Reina.
        - Pilotos do Esquadrão Delta: Hayate Immelmann (homem), Mirage Farina Jenius (mulher), Arad Molders (homem), Chuck Mustang (homem), Messer Ihlefeld (homem).
        - Personagens de Legado e Especiais: Maximilian Jenius / Max Jenius (homem / capitão lendário da Macross Gigant), Ian Cromwell (homem / líder da Heimdall), Lady M.
        - Organizações e Termos: Chaos, NUNS, Heimdall, Walküre, Yami_Q_Ray, Star Singer, Sv-301t Kairos-Plus, VF-31AX Kairos-Plus, VF-31E Siegfried, Macross Gigant.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross Delta (Filmes)", LORE);

    @Override
    public String getId() { return "macross_delta_filmes"; }

    @Override
    public String getNomeExibicao() { return "Macross Delta (Filmes)"; }

    @Override
    public String obterPromptSistema() { return PROMPT; }
}
