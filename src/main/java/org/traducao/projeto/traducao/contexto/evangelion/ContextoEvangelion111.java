package org.traducao.projeto.traducao.contexto.evangelion;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

@Component
public class ContextoEvangelion111 implements ProvedorContexto {
    private static final String PROMPT = ContextoPrompt.montar("Evangelion: 1.11 You Are (Not) Alone", "- Obra: Evangelion: 1.11 You Are (Not) Alone (Evangelion: 1.0).\n- Personagens: Shinji Ikari, Rei Ayanami, Misato Katsuragi, Gendo Ikari, Ritsuko Akagi.\n- Mecha/Termos: NERV, EVA Unit-01, EVA Unit-00, Ramiel, Sachiel, Fourth Angel.");
    @Override public String getId() { return "evangelion_111"; }
    @Override public String getNomeExibicao() { return "Evangelion: Filme 1 - 1.11 You Are (Not) Alone"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
