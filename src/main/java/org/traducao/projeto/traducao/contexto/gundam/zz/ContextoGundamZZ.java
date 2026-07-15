package org.traducao.projeto.traducao.contexto.gundam.zz;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

@Component
public class ContextoGundamZZ implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam ZZ, Universal Century 0088, pos-Gryps Conflict e ascensao de Neo Zeon.
        - Faccao/forcas: AEUG, Neo Zeon, Axis, Federacao Terrestre, Karaba.
        - Principais nomes: Judau Ashta, Haman Karn, Roux Louka, Elle Vianno, Beecha Oleg, Mondo Agake, Iino Abbav, Leina Ashta, Mashymre Cello, Chara Soon, Glemy Toto, Bright Noa.
        - Lugares/naves: Shangri-La, Argama, Nahel Argama, Axis, Core 3, Dublin.
        - Mobile suits: ZZ Gundam, Zeta Gundam, Gundam Mk-II, Qubeley, Qubeley Mk-II, R-Jarja, Dreissen, Bawoo, Zaku III, Quin Mantha.
        - Termos UC: Newtype, Cyber-Newtype, mobile suit, mobile armor, colony drop, beam rifle, beam saber. Mantenha Newtype/mobile suit.
        - Tom: inicio mais aventureiro e caotico, depois guerra politica e tragedia; Judau e informal e protetor, Haman e fria/imperial, Mashymre e teatral.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam ZZ", LORE);

    @Override
    public String getId() { return "gundam_zz"; }
    @Override
    public String getNomeExibicao() { return "Mobile Suit Gundam ZZ"; }
    @Override
    public String obterPromptSistema() { return PROMPT; }
}
