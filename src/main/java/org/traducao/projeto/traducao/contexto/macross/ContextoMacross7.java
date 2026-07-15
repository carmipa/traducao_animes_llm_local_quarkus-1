package org.traducao.projeto.traducao.contexto.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

@Component
public class ContextoMacross7 implements ProvedorContexto {
    private static final String PROMPT = ContextoPrompt.montar("Macross 7", "- Obra: Macross 7.\n- Personagens: Basara Nekki, Mylene Flare Jenius, Ray Lovelock, Veffidas Feaze, Gamlin Kizaki, Maximilian Jenius, Milia Fallyna Jenius.\n- Banda: Fire Bomber.\n- Mecha: VF-19 Custom Fire Valkyrie.");
    @Override public String getId() { return "macross_7"; }
    @Override public String getNomeExibicao() { return "Macross 7 (Série TV)"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
