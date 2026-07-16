package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoMacrossFrontier implements ProvedorContexto {
    private static final String PROMPT = ContextoPrompt.montar("Macross Frontier", "- Obra: Macross Frontier.\n- Personagens: Alto Saotome, Sheryl Nome, Ranka Lee, Michael Blanc, Luca Angeloni, Klan Klang, Ozma Lee.");
    @Override public String getId() { return "macross_frontier"; }
    @Override public String getNomeExibicao() { return "Macross Frontier"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
