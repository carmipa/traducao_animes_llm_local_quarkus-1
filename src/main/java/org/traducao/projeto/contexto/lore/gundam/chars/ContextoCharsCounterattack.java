package org.traducao.projeto.contexto.lore.gundam.chars;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoCharsCounterattack implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam: Char's Counterattack, Universal Century 0093.
        - Faccao/forcas: Londo Bell, Neo Zeon, Federacao Terrestre, Anaheim Electronics.
        - Principais nomes: Amuro Ray, Char Aznable, Bright Noa, Chan Agi, Beltorchika Irma, Hathaway Noa, Quess Paraya, Gyunei Guss, Nanai Miguel, Adenaur Paraya.
        - Lugares/eventos: Axis (base-asteroide de Neo Zeon), Torrington Base (base lunar da Federacao atacada na abertura), Luna II; Char planeja arremessar o asteroide Axis contra a Terra ("Teoria da Purificacao Humana") para forcar a humanidade a migrar para o espaco, plano interceptado pela Frota Londo Bell.
        - O climax em que Newtypes combinam psycoframe e sentimentos para deter Axis e conhecido pelos fas como "Axis Shock"; mantenha o termo em ingles se aparecer.
        - Mobile suits: Nu Gundam, Sazabi, Re-GZ, Jegan, Geara Doga, Jagd Doga, Alpha Azieru.
        - Termos UC: Newtype, psycho-frame, psycommu, funnel, mobile suit, mobile armor, beam rifle, beam saber. Mantenha Newtype, psycho-frame e funnel.
        - Tom: confronto ideologico final entre Amuro e Char, melancolia politica e tensao catastrofica; Char fala carismatico e frio, Amuro direto e cansado.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam: Char's Counterattack", LORE);

    @Override
    public String getId() { return "gundam_cca"; }
    @Override
    public String getNomeExibicao() { return "Mobile Suit Gundam: Char's Counterattack"; }
    @Override
    public String obterPromptSistema() { return PROMPT; }
}
