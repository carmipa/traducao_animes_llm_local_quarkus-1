package org.traducao.projeto.traducao.contexto.evangelion;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

@Component
public class ContextoEvangelion3010 implements ProvedorContexto {
    private static final String PROMPT = ContextoPrompt.montar("Evangelion: 3.0+1.0 Thrice Upon a Time", "- Obra: Evangelion: 3.0+1.0 Thrice Upon a Time (Evangelion 3.0+1.01).\n- Personagens: Shinji Ikari, Rei Ayanami, Asuka Langley, Mari Makinami, Gendo Ikari, Misato Katsuragi.\n- Termos: Village 3, Golgotha Object, Additional Impact, Neon Genesis.");
    @Override public String getId() { return "evangelion_3010"; }
    @Override public String getNomeExibicao() { return "Evangelion: 3.0+1.0 Thrice Upon a Time"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
