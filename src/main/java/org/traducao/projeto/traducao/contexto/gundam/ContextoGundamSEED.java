package org.traducao.projeto.traducao.contexto.gundam;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

@Component
public class ContextoGundamSEED implements ProvedorContexto {
    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam SEED", "- Obra: Mobile Suit Gundam SEED & SEED Destiny.\n- Personagens: Kira Yamato, Athrun Zala, Lacus Clyne, Cagalli Yula Athha, Shinn Asuka, Mu La Flaga, Rau Le Creuset.\n- Mecha/Termos: ZGMF-X10A Freedom Gundam, ZGMF-X09A Justice Gundam, Strike Gundam, ZAFT, Earth Alliance, PLANTs, Coordinator, Natural.");
    @Override public String getId() { return "gundam_seed"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam SEED"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
