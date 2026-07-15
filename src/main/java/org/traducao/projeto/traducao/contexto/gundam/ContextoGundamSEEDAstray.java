package org.traducao.projeto.traducao.contexto.gundam;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

@Component
public class ContextoGundamSEEDAstray implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam SEED MSV Astray (Manga / Side Story).
        - Personagens: Lowe Guele (homem / mecânico da Junk Guild), Gai Murakumo (homem / mercenário da Serpent Tail), Rondo Gina Sahaku (homem), Rondo Mina Sahaku (mulher), Elijah Kiel (homem).
        - Mechas / Termos: MBF-P02 Gundam Astray Red Frame, MBF-P03 Gundam Astray Blue Frame, MBF-P01 Gundam Astray Gold Frame, Gerbera Straight, Junk Guild, Serpent Tail.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam SEED MSV Astray", LORE);

    @Override public String getId() { return "gundam_seed_astray"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam SEED MSV Astray (Mangá/Side Story)"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
