package org.traducao.projeto.contexto.lore.gundam;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoGundamSEEDDestiny implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam SEED Destiny.
        - Personagens: Shinn Asuka (homem), Kira Yamato (homem), Athrun Zala (homem), Lacus Clyne (mulher), Cagalli Yula Athha (mulher), Lunamaria Hawke (mulher), Stella Loussier (mulher), Rey Za Burrel (homem), Gilbert Durandal (homem), Neo Roanoke / Mu La Flaga (homem).
        - Mechas / Termos: ZGMF-X42S Destiny Gundam, ZGMF-X20A Strike Freedom Gundam, ZGMF-X19A Infinite Justice Gundam, ZGMF-X56S Impulse Gundam, Minerva, ZAFT, Alliance, LOGOS, Plano Destiny.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam SEED Destiny", LORE);

    @Override public String getId() { return "gundam_seed_destiny"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam SEED Destiny"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
