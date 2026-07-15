package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

@Component
public class ContextoRevisaoLoreGundamUnicorn implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam Unicorn / Mobile Suit Gundam Unicorn RE:0096, Universal Century U.C. 0096.
        - Regra central: manter nomes oficiais de personagens, faccoes, naves, mobile suits, tecnologias e termos UC.
        - Nunca traduzir Unicorn Gundam como "Gundam Unicornio"; "Unicorn" so pode virar unicornio quando for animal comum, nao nome proprio.
        - Personagens: Banagher Links, Audrey Burne, Mineva Lao Zabi, Full Frontal, Marida Cruz, Riddhe Marcenas, Angelo Sauper, Cardeas Vist, Alberto Vist, Syam Vist, Daguza Mackle, Suberoa Zinnerman, Takuya Irei, Micott Bartsch.
        - Faccao/organizacoes: Earth Federation, Federation, Vist Foundation, Anaheim Electronics, Sleeves, Neo Zeon, Londo Bell, ECOAS, Zeon Remnants.
        - Naves/lugares/eventos: Nahel Argama, Garencieres, Industrial 7, Palau, Torrington Base, Laplace, Laplace's Box, Caixa de Laplace, Laplace Incident, Universal Century, U.C.
        - Mobile suits/armors: RX-0 Unicorn Gundam, Unicorn Gundam, Unicorn Gundam 02 Banshee, Banshee, Banshee Norn, MSN-06S Sinanju, NZ-666 Kshatriya, Delta Plus, ReZEL, Jegan, Geara Zulu, Rozen Zulu, Shamblo.
        - Termos UC: Newtype, Cyber Newtype, Psycho-Frame, psycho-frame, NT-D System, Destroy Mode, La+, Minovsky particles, beam rifle, beam saber, funnel, psycommu, mobile suit, mobile armor.
        - Alertas: Full Frontal nao vira "Frontal Completo"; Sleeves nao vira "Mangas"; Axis nao vira "Eixo"; Kshatriya, Sinanju, Nahel Argama e Garencieres mantem grafia oficial.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "gundam_unicorn"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam Unicorn - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
