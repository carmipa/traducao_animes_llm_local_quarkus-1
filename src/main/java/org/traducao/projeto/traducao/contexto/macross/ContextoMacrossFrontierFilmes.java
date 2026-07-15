package org.traducao.projeto.traducao.contexto.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

public class ContextoMacrossFrontierFilmes implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross Frontier (Filmes: Itsuwari no Utafihme / Itsuka no Tsubasa / Labyrinth of Time).
        - Personagens: Alto Saotome (homem), Sheryl Nome (mulher), Ranka Lee (mulher), Michael Blanc (homem), Luca Angeloni (homem), Klan Klang (mulher), Ozma Lee (homem), Brera Sterne (homem), Grace O'Connor (mulher).
        - Naves / Mechas: VF-25 Messiah, VF-27 Lucifer, YF-29 Durandal, Vajra.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross Frontier (Filmes)", LORE);

    @Override public String getId() { return "macross_frontier_filmes"; }
    @Override public String getNomeExibicao() { return "Macross Frontier: Filmes (False Songstress / Wings of Farewell)"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
