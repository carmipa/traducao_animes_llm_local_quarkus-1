package org.traducao.projeto.traducao.contexto.gundam;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

@Component
public class ContextoGundamOrigin implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam: The Origin.
        - Personagens: Char Aznable / Casval Rem Deikun (homem), Sayla Mass / Artesia Som Deikun (mulher), Amuro Ray (homem), Degwin Sodo Zabi (homem), Gihren Zabi (homem), Dozle Zabi (homem), Kycilia Zabi (mulher), Ramba Ral (homem), Hamon Crowley (mulher).
        - Termos/Mechas: principado de Zeon, Federação Terrestre, Side 3, Munzo, MS-06S Zaku II, RX-78-02 Gundam, Mobile Worker.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam: The Origin", LORE);

    @Override public String getId() { return "gundam_origin"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam: The Origin"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
