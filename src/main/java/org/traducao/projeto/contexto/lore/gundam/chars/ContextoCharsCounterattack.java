package org.traducao.projeto.contexto.lore.gundam.chars;

import java.util.Map;
import org.traducao.projeto.contexto.lore.gundam.CorrecoesTerminologiaGundamUc;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore de Char's Counterattack (UC 0093 / Axis Shock).
 *
 * <p>INVARIANTES DO DOMÍNIO: Amuro Ray; Char Aznable; Nu Gundam; Sazabi;
 * Londo Bell; Axis; psycho-frame.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoCharsCounterattack implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam: Char's Counterattack — Universal Century 0093.
        - Facções: Londo Bell, Neo Zeon, Earth Federation, Anaheim Electronics.
        - Personagens (gênero): Amuro Ray (m), Char Aznable (m), Bright Noa (m),
          Chan Agi (f), Beltorchika Irma (f), Hathaway Noa (m), Quess Paraya (f),
          Gyunei Guss (m), Nanai Miguel (f), Adenauer Paraya (m), Kayra Su (f).
        - Lugares/eventos: Axis (asteroide Neo Zeon), Torrington Base, Luna II;
          plano de Char de lançar Axis contra a Terra ("Human Purification Theory" /
          Teoria da Purificação Humana); clímax conhecido como Axis Shock.
        - Mobile suits: RX-93 Nu Gundam, MSN-04 Sazabi, Re-GZ, RGM-89 Jegan,
          AMS-119 Geara Doga, MSN-03 Jagd Doga, NZ-333 Alpha Azieru.
        - Termos UC: Newtype (NUNCA "Novo Tipo"), Oldtype, Cyber-Newtype; Spacenoid/Earthnoid;
          Mobile Suit vs Mobile Armor (Alpha Azieru = MA); psycho-frame / psycoframe, psycommu,
          funnel; Minovsky; Londo Bell, Neo Zeon, Axis Shock (manter em inglês se aparecer).
        - Regras: Nu Gundam / Sazabi oficiais; Char e Amuro sem adaptação;
          Hathaway Noa grafia; Quess/Gyunei/Nanai oficiais.
        - Tom: confronto ideológico final Amuro vs Char; melancolia política; Char carismático/frio, Amuro direto/cansado.
        """;

    private static final String PROMPT = ContextoPrompt.montar(
        "Mobile Suit Gundam: Char's Counterattack", LORE);

    @Override
    public String getId() {
        return "gundam_cca";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam: Char's Counterattack";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Amuro Ray", "Char Aznable", "Bright Noa",
            "Chan Agi", "Beltorchika Irma", "Hathaway Noa",
            "Quess Paraya", "Gyunei Guss", "Nanai Miguel",
            "Londo Bell", "Neo Zeon", "Earth Federation",
            "Anaheim Electronics", "Axis", "Luna II",
            "Axis Shock", "Human Purification Theory", "Nu Gundam",
            "Sazabi", "Re-GZ", "Jegan",
            "Geara Doga", "Jagd Doga", "Alpha Azieru",
            "Newtype", "Oldtype", "Cyber-Newtype",
            "Spacenoid", "Earthnoid", "Mobile Suit",
            "Mobile Armor", "psycho-frame", "psycommu",
            "funnel", "Minovsky"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reforço determinístico do núcleo UC (Newtype, Mobile Suit, Beam
     * Saber/Rifle, Mobile Armor, Oldtype) mais os termos próprios desta obra.
     * <p>INVARIANTES DO DOMÍNIO: forma-ruim PT → canônico; só aplica se o EN contém o canônico.
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUc.comExtras(Map.ofEntries(
            Map.entry("Eixo", "Axis"),
            Map.entry("Funil", "funnel")
        ));
    }
}
