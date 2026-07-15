package org.traducao.projeto.traducao.contexto.gundam;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

@Component
public class ContextoGundam0079 implements ProvedorContexto {
    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam (0079)", "- Obra: Mobile Suit Gundam (0079 Original Series).\n- Personagens: Amuro Ray, Char Aznable, Bright Noa, Sayla Mass, Lalah Sune, Kai Shiden, Hayato Kobayashi, Degwin Sodo Zabi, Gihren Zabi.\n- Mecha/Termos: RX-78-2 Gundam, MS-06 Zaku II, White Base, Zeon, Federation, One Year War.");
    @Override public String getId() { return "gundam_0079"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam (Série original / Filme Trilogy)"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
