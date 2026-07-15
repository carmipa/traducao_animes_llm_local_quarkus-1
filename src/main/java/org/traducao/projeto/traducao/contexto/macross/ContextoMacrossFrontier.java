package org.traducao.projeto.traducao.contexto.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

@Component
public class ContextoMacrossFrontier implements ProvedorContexto {
    private static final String PROMPT = ContextoPrompt.montar("Macross Frontier", "- Obra: Macross Frontier.\n- Personagens: Alto Saotome, Sheryl Nome, Ranka Lee, Michael Blanc, Luca Angeloni, Klan Klang, Ozma Lee.");
    @Override public String getId() { return "macross_frontier"; }
    @Override public String getNomeExibicao() { return "Macross Frontier"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
