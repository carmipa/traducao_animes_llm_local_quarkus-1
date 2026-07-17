package org.traducao.projeto.contexto.lore.gundam;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore UC 0079 com núcleo conceitual militar (Minovsky, MS/MA, Newtype).
 *
 * <p>INVARIANTES DO DOMÍNIO: Newtype ≠ "Novo Tipo"; Mobile Suit ≠ Mobile Armor;
 * Spacenoid/Earthnoid; Minovsky Particles; Principality of Zeon vs Earth Federation.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoGundam0079 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam (0079) — One Year War / Universal Century 0079.
        - Premissa: Earth Federation vs Principality of Zeon; White Base; Amuro Ray vs Char Aznable.

        === Nucleo UC (obrigatorio em toda fala tecnica) ===
        - Minovsky Particles / Minovsky physics: bloqueiam radio e radar → combate visual
          e Mobile Suits sao consequencia "juridica" da fisica da obra, nao sci-fi generica.
        - Mega Particle Cannon; Minovsky Ultracompact Fusion Reactor: consistencia tecnica rigida.
        - Mobile Suit: humanoide (Gundam, Zaku, GM, Gouf, Dom…).
        - Mobile Armor: nao-humanoide de grande porte (Zeong, Elmeth…). NUNCA trocar os rotulos.
        - Newtype: NUNCA "Novo Tipo". Cyber-Newtype / Oldtype quando aparecerem — oficiais.
        - Spacenoid (colonias) vs Earthnoid (Terra): subtexto politico de segregacao.
        - Principality of Zeon (militarista/espacial) vs Earth Federation (burocratica/terrestre).

        === Pessoas (genero) ===
        - Amuro Ray (m), Char Aznable / Casval Rem Deikun (m), Bright Noa (m),
          Sayla Mass / Artesia Som Deikun (f), Lalah Sune (f), Kai Shiden (m),
          Hayato Kobayashi (m), Ryu Jose (m), Sleggar Law (m), Mirai Yashima (f), Fraw Bow (f),
          Degwin Sodo Zabi (m), Gihren Zabi (m), Kycilia Zabi (f), Dozle Zabi (m), Garma Zabi (m),
          Ramba Ral (m), M'Quve (m), Crowley Hamon (f).

        === Mecha / lugares ===
        - RX-78-2 Gundam, RX-75 Guntank, RX-77 Guncannon, MS-06 Zaku II, MS-07 Gouf, MS-09 Dom,
          MSN-02 Zeong (MA), MAN-08 Elmeth (MA), White Base (Pegasus-class).
        - Side 7, Loum, Solomon, A Baoa Qu, Jaburo, Side colonies.

        === Regras ===
        - White Base / Gundam / Zaku / Zeon / Federation oficiais; ranks militares consistentes.
        - Tom: space opera UC, amadurecimento de Amuro, tragedia Newtype (Lalah).
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam (0079)", LORE);

    @Override
    public String getId() {
        return "gundam_0079";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam (Série original / Filme Trilogy)";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Amuro Ray", "Char Aznable", "Bright Noa", "Sayla Mass", "Lalah Sune",
            "White Base", "Gundam", "Zaku II", "Zeon", "Newtype", "Oldtype", "Spacenoid",
            "Earthnoid", "One Year War", "Minovsky", "Mobile Suit", "Mobile Armor",
            "Mega Particle Cannon"
        );
    }
}
