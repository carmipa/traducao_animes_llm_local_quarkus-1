package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

@Component
public class ContextoRevisaoLoreGundamZeta implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Zeta Gundam, Universal Century U.C. 0087, Gryps Conflict.
        - Regra central: preservar a nomenclatura oficial de personagens, faccoes, operacoes, locais, mobile suits e tecnologia UC.
        - Personagens: Kamille Bidan, Char Aznable, Quattro Bajeena, Amuro Ray, Bright Noa, Fa Yuiry, Emma Sheen, Reccoa Londe, Katz Kobayashi, Henken Bekkener, Jerid Messa, Paptimus Scirocco, Four Murasame, Rosamia Badam, Haman Karn.
        - Faccao/organizacoes: A.E.U.G., Anti-Earth Union Group, Titans, Earth Federation, Axis Zeon, Karaba, Anaheim Electronics.
        - Mobile suits/armors: MSZ-006 Zeta Gundam, RX-178 Gundam Mk-II, Hyaku Shiki, Rick Dias, Methuss, Nemo, GM II, Marasai, Gabthley, Hambrabi, Gaplant, Psycho Gundam, The O, Palace Athene, Qubeley.
        - Lugares/eventos: Gryps, Gryps Conflict, Jaburo, Dakar, Kilimanjaro, Colony Laser, Axis, Universal Century, U.C.
        - Termos UC: Newtype, Cyber Newtype, Psycommu, Minovsky particles, beam rifle, beam saber, mobile suit, mobile armor.
        - Alertas: A.E.U.G. deve manter os pontos; Hyaku Shiki nao vira "Cem Estilos"; The O nao vira "O"; Axis nao vira "Eixo"; Titans deve ficar como nome de faccao.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "gundam_zeta"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Zeta Gundam - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
