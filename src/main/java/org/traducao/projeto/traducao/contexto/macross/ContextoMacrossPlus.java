package org.traducao.projeto.traducao.contexto.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

@Component
public class ContextoMacrossPlus implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross Plus (OVA / Filme).
        - Personagens: Isamu Alva Dyson (homem), Guld Goa Bowman (homem/meio-zentradi), Myung Fang Lone (mulher), Sharon Apple (IA idol virtual), Raymond Marley (homem), Millard Johnson (homem).
        - Mechas / Naves: YF-19, YF-21, Ghost X-9, Sharon Apple System.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross Plus", LORE);

    @Override public String getId() { return "macross_plus"; }
    @Override public String getNomeExibicao() { return "Macross Plus"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
