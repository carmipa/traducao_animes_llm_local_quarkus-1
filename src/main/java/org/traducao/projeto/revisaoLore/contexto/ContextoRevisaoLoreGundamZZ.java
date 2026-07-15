package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

@Component
public class ContextoRevisaoLoreGundamZZ implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam ZZ / Double Zeta, Universal Century U.C. 0088, pos-Gryps Conflict e ascensao de Neo Zeon.
        - Regra central: preservar nomes oficiais de personagens, faccoes, naves, mobile suits, tecnologias e eventos UC.
        - Personagens: Judau Ashta, Roux Louka, Beecha Oleg, Elle Vianno, Mondo Agake, Iino Abbav, Leina Ashta, Elpeo Ple, Ple Two, Haman Karn, Lady Haman, Mashymre Cello, Chara Soon, Glemy Toto, Bright Noa, Fa Yuiry.
        - Faccao/organizacoes: A.E.U.G., Neo Zeon, Axis Zeon, Earth Federation, Blue Corps, Corpo Azul, Anaheim Electronics.
        - Naves/lugares/eventos: Argama, Nahel Argama, Endra, Sadalahn, Gwanban, Shangri-La, Axis, Dublin, Colony Drop, Universal Century, U.C.
        - Mobile suits/armors: ZZ Gundam, Double Zeta, Zeta Gundam, Gundam Mk-II, Hyaku Shiki, Core Top, Core Base, Core Fighter, Mega Rider, Qubeley, Qubeley Mk-II, Bawoo, Dreissen, Doven Wolf, Quin Mantha, Geymalk, Zssa, Hamma-Hamma, R-Jarja, Gaza-C, Gaza-D, Galluss-J, Jamru Fin, Psycho Gundam Mk-II.
        - Termos UC: Newtype, Cyber Newtype, Psycommu, Minovsky particles, beam rifle, beam saber, mobile suit, mobile armor, funnel.
        - Alertas: Double Zeta nao vira "Zeta Duplo"; Axis nao vira "Eixo"; Lady Haman nao vira "Senhorita Haman"; Quin Mantha nao vira "Rainha Mansa"; A.E.U.G. deve manter os pontos.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "gundam_zz"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam ZZ - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
