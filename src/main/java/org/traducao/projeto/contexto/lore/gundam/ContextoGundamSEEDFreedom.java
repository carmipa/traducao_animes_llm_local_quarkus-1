package org.traducao.projeto.contexto.lore.gundam;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoGundamSEEDFreedom implements ProvedorContexto {
    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam SEED FREEDOM", "- Obra: Mobile Suit Gundam SEED FREEDOM (Filme).\n- Personagens: Kira Yamato, Lacus Clyne, Athrun Zala, Shinn Asuka, Agnes Giebenrath, Orphee Lam Tao.\n- Mecha: Rising Freedom Gundam, Immortal Justice Gundam, Mighty Strike Freedom Gundam.");
    @Override public String getId() { return "gundam_seed_freedom"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam SEED Freedom"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
