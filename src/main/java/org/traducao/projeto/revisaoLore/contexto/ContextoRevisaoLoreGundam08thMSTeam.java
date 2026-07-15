package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

@Component
public class ContextoRevisaoLoreGundam08thMSTeam implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam: The 08th MS Team, Universal Century U.C. 0079.
        - Regra central: manter nomes oficiais de equipe, personagens, faccoes, unidades, mobile suits, mobile armors, armas e termos UC.
        - Personagens: Shiro Amada, Aina Sahalin, Karen Joshua, Terry Sanders Jr., Eledore Massis/Mathis, Michel Ninorich, Kiki Rosita, Ginias Sahalin, Norris Packard, Kojima, Isan Ryer, Alice Miller.
        - Faccao/forcas: Earth Federation, Federation, Principality of Zeon, Zeon, 08th MS Team.
        - Mobile suits/armors: RX-79[G] Ground Gundam, Gundam Ground Type, RX-79[G] Ez-8 Gundam Ez8, RGM-79[G] GM Ground Type, MS-06J Zaku II Ground Type, MS-07B-3 Gouf Custom, Gouf Flight Type, Zaku Tank, Magella Attack, Apsalus I, Apsalus II, Apsalus III.
        - Termos: mobile suit, mobile armor, Hovertruck, Miller's Report, One Year War, Universal Century, U.C., Newtype, Minovsky particles, beam rifle, beam saber, mega particle cannon, Jaburo.
        - Alertas: The 08th MS Team nao deve virar "O 8o Time MS" se for titulo; Apsalus nao vira "absalão"; Gouf Custom e Gundam Ez8 mantem grafia oficial.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "gundam_08ms"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam: The 08th MS Team - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
