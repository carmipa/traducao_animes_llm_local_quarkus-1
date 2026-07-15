package org.traducao.projeto.traducao.contexto.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

@Component
public class ContextoMacrossFrontierFilme2 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross Frontier O Filme: The Wings of Farewell (Sayonara no Tsubasa).
        - Personagens: Alto Saotome (homem), Sheryl Nome (mulher), Ranka Lee (mulher), Michael Blanc (homem), Luca Angeloni (homem), Klan Klang (mulher), Ozma Lee (homem), Brera Sterne (homem), Grace O'Connor (mulher).
        - Naves / Mechas: YF-29 Durandal, VF-25 Messiah, VF-27 Lucifer, Vajra Queen.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross Frontier: The Wings of Farewell", LORE);

    @Override public String getId() { return "macross_frontier_filme2"; }
    @Override public String getNomeExibicao() { return "Macross Frontier: The Wings of Farewell"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
