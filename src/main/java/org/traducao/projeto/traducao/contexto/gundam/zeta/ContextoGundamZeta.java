package org.traducao.projeto.traducao.contexto.gundam.zeta;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

@Component
public class ContextoGundamZeta implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Zeta Gundam, Universal Century 0087, Gryps Conflict.
        - Faccao/forcas: AEUG, Titans, Federacao Terrestre, Axis Zeon, Karaba, Anaheim Electronics.
        - Principais nomes: Kamille Bidan, Quattro Bajeena, Char Aznable, Amuro Ray, Bright Noa, Emma Sheen, Fa Yuiry, Reccoa Londe, Paptimus Scirocco, Haman Karn, Jerid Messa, Four Murasame.
        - Naves/lugares: Argama, Alexandria, Gryps, Jaburo, Hong Kong, Dakar, Axis.
        - Mobile suits: Zeta Gundam, Gundam Mk-II, Hyaku Shiki, Rick Dias, Methuss, Psycho Gundam, The O, Qubeley, Marasai, Gaplant.
        - Termos UC: Newtype, Cyber-Newtype, mobile suit, mobile armor, colony laser, beam rifle, beam saber, Minovsky particles. Mantenha Newtype e Cyber-Newtype em ingles.
        - Kamille Bidan e do sexo masculino apesar do nome parecer feminino; varios personagens o confundem com uma garota na trama (piada recorrente). Preserve pronomes e tratamentos masculinos para ele.
        - Tom: politica militar sombria, radicalizacao, trauma e abuso de autoridade; Kamille e sensivel/reativo, Quattro e estrategico, Titans soam autoritarios.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Zeta Gundam", LORE);

    @Override
    public String getId() { return "gundam_zeta"; }
    @Override
    public String getNomeExibicao() { return "Mobile Suit Zeta Gundam"; }
    @Override
    public String obterPromptSistema() { return PROMPT; }
}
