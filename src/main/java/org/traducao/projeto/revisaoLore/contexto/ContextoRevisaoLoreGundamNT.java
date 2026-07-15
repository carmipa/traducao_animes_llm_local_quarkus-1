package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

@Component
public class ContextoRevisaoLoreGundamNT implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam NT (Narrative), Universal Century U.C. 0097.
        - Regra central: nomes de mobile suits, faccoes, operacoes, tecnologias e personagens devem ficar na forma oficial da franquia.
        - Nunca traduzir Narrative ou Narrative Gundam como "Narrativo" ou "Gundam Narrativo".
        - Personagens: Jona Basta, Michele Luio/Michelle Luio, Rita Bernal, Zoltan Akkanen, Iago Haakana, Brick Teclato, Mineva Lao Zabi, Banagher Links, Monaghan Bakharo.
        - Faccao/organizacoes: Earth Federation, Earth Federation Forces, Luio & Co., Republic of Zeon, Shezarr Unit, Sleeves, Titans, Newtype Labs.
        - Mechas/unidades: RX-9 Narrative Gundam, A-Packs, B-Packs, C-Packs, RX-0 Unicorn Gundam 03 Phenex, Phenex, Unicorn Gundam, Banshee, Sinanju Stein, II Neo Zeong, Silver Bullet Suppressor.
        - Termos: Psycho-Frame, psycho-frame, NT-D, Newtype, Cyber Newtype, Operation Phoenix Hunt, Laplace Incident, Universal Century, U.C., Minovsky particles, beam rifle, beam saber, funnels, psychowaves.
        - Alertas: Phenex nao vira Fenix; Unicorn nao vira Unicornio; II Neo Zeong nao vira Segundo Neo Zeong; Psycho-Frame nao vira quadro/moldura psicologica.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "gundam_nt"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam NT (Narrative) - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
