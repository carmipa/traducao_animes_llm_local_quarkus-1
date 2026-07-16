package org.traducao.projeto.contexto.lore.evangelion;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoEvangelion333 implements ProvedorContexto {
    private static final String PROMPT = ContextoPrompt.montar("Evangelion: 3.33 You Can (Not) Redo", "- Obra: Evangelion: 3.33 You Can (Not) Redo (Evangelion: 3.0).\n- Personagens: Shinji Ikari, Kaworu Nagisa, Asuka Langley, Mari Makinami, Misato Katsuragi.\n- Termos: WILLE, AAA Wunder, EVA Unit-13, Central Dogma, Spear of Longinus, Spear of Cassius.");
    @Override public String getId() { return "evangelion_333"; }
    @Override public String getNomeExibicao() { return "Evangelion: 3.33 You Can (Not) Redo"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
