package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

public class ContextoMacrossDYRL implements ProvedorContexto {
    private static final String PROMPT = ContextoPrompt.montar("Macross: Do You Remember Love?", "- Obra: Macross: Do You Remember Love? (Filme 1).\n- Personagens: Hikaru Ichijo, Lynn Minmay, Misa Hayase, Roy Focker, Maximilian Jenius, Quamzin Kravshera.");
    @Override public String getId() { return "macross_dyrl"; }
    @Override public String getNomeExibicao() { return "Macross: Do You Remember Love? (Filme 1)"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
