package org.traducao.projeto.traducao.contexto.evangelion;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

@Component
public class ContextoEvangelionTV implements ProvedorContexto {
    private static final String PROMPT = ContextoPrompt.montar("Neon Genesis Evangelion", "- Obra: Neon Genesis Evangelion.\n- Personagens: Shinji Ikari, Rei Ayanami, Asuka Langley Soryu, Misato Katsuragi, Gendo Ikari, Kaworu Nagisa.\n- Termos: NERV, SEELE, Anjos (Angels), EVA Unit-01, AT Field, Human Instrumentation Project.");
    @Override public String getId() { return "evangelion_tv"; }
    @Override public String getNomeExibicao() { return "Evangelion (Série TV Clássica)"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
