package org.traducao.projeto.contexto.lore.gundam;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * PROPÓSITO DE NEGÓCIO: lore completa de Mobile Suit Gundam: The Origin (OVA / mangá) —
 * origem de Char, ascensão Zabi/Zeon e início da One Year War.
 *
 * <p>INVARIANTES DO DOMÍNIO: Casval/Édouard/Char e Artesia/Sayla; família Zabi completa;
 * Munzo → Principality of Zeon; Mobile Worker → Zaku; Zeon ≠ Zion; Newtype oficial;
 * Black Tri-Stars; grafias Origin (Dozle, Kycilia, RX-78-02).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoGundamOrigin implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam: The Origin (OVA / mangá Yasuhiko Yoshikazu) —
          prequela e releitura UC até o início da One Year War (U.C. 0068–0079).
        - Premissa: assassinato de Zeon Zum Deikun; Casval Rem Deikun e Artesia Som Deikun
          fogem sob os nomes Edouard Mass e Sayla Mass (familia Teabolo Mass);
          Casval torna-se Char Aznable; familia Zabi transforma Munzo (Side 3) no
          Principality of Zeon; Mobile Worker evolui para Mobile Suit (Zaku).

        === Deikun / Mass / Char ===
        - Zeon Zum Deikun (m) — ideologo Spacenoid; Astraia Tor Deikun (f) — mae de Casval/Artesia.
        - Casval Rem Deikun (m) → Edouard Mass / Édouard Mass → Char Aznable (mascara vermelha).
          NUNCA "Shar"/"Xar"; NUNCA traduzir Char.
        - Artesia Som Deikun (f) → Sayla Mass.
        - Don Teabolo Mass / Teabolo Mass (m) — tutor/adotivo em Texas Colony.
        - Jimba Ral (m) — mentor politico Deikun; Ramba Ral (m); Crowley Hamon (f).

        === Familia Zabi (Side 3 / Zeon) ===
        - Degwin Sodo Zabi (m) — soberano; Gihren Zabi (m) — estrategista/ideologo militar;
          Dozle Zabi (m); Kycilia Zabi (f); Garma Zabi (m); Sasro Zabi (m) — assassinado cedo.
        - Torenov Y. Minovsky / T.Y. Minovsky (m) — fisica Minovsky.

        === Federacao / White Base (quando a trama cruza 0079) ===
        - Amuro Ray (m), Bright Noa (m), Mirai Yashima (f), Tem Ray (m),
          Johann Ibrahim Revil (m), Lalah Sune (f) quando aparecerem.
        - Kai Shiden, Hayato Kobayashi, Ryu Jose, Fraw Bow quando o dialogo trouxer.

        === Pilotos / unidades Zeon recorrentes ===
        - Black Tri-Stars: Gaia (m), Ortega (m), Mash (m) — NUNCA "Triangulo Negro" como canonico.
        - M'Quve (m), Gadem (m), Clamp (m), Darcia Bakharov (m) quando aparecerem.
        - Cozun Graham, Flanagan Boone, Challia Bull quando o dialogo trouxer.

        === Faccoes / lugares ===
        - Autonomous Republic of Munzo → Principality of Zeon / Zeon (Side 3).
        - Earth Federation / Federation Forces; Spacenoid vs Earthnoid.
        - Munzo; Side 3; Texas Colony; Loum (Battle of Loum); Granada; Solomon;
          A Baoa Qu; Jaburo; Side 7; Luna II; Von Braun quando aparecerem.
        - White Base (Pegasus-class) na continuidade OYW do Origin.

        === Mecha / termos ===
        - Mobile Worker; MS-04 Bugu; MS-05 Zaku I; MS-06 Zaku II / MS-06S Zaku II (Char's);
          MS-07 Gouf; MS-09 Dom quando aparecerem.
        - RX-78-02 Gundam (designacao Origin; distinta de RX-78-2 em alguns materiais 0079);
          RX-75 Guntank; RX-77 Guncannon; GM quando aparecerem.
        - Mobile Suit vs Mobile Armor; Newtype (NUNCA "Novo Tipo"); Oldtype;
          Minovsky particles; Mega Particle Cannon; Beam Saber / Beam Rifle.
        - One Year War; Battle of Loum; Operation British (quando o dialogo trouxer).

        === Regras duras ===
        - Zeon NUNCA "Zion"; Principality of Zeon NUNCA "Principado de Zion".
        - Edouard/Édouard Mass e Sayla Mass sao identidades de Casval/Artesia — manter grafia.
        - Black Tri-Stars grafia oficial; Dozle (nao Dozzle antigo); Kycilia (grafia Origin).
        - Tom: politico-militar, conspiracao Zabi, Char frio/calculista, tragedia Deikun.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam: The Origin", LORE);

    @Override
    public String getId() {
        return "gundam_origin";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam: The Origin";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege elenco Origin completo, locais UC e mechas canônicos
     * contra localização indevida.
     *
     * <p>INVARIANTES DO DOMÍNIO: grafias oficiais The Origin / UC; Zeon ≠ Zion.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Char Aznable", "Casval Rem Deikun", "Edouard Mass", "Édouard Mass",
            "Sayla Mass", "Artesia Som Deikun", "Zeon Zum Deikun", "Astraia Tor Deikun",
            "Don Teabolo Mass", "Teabolo Mass", "Jimba Ral", "Ramba Ral", "Crowley Hamon",
            "Degwin Sodo Zabi", "Gihren Zabi", "Dozle Zabi", "Kycilia Zabi",
            "Garma Zabi", "Sasro Zabi", "Torenov Y. Minovsky",
            "Amuro Ray", "Bright Noa", "Mirai Yashima", "Tem Ray",
            "Johann Ibrahim Revil", "Lalah Sune",
            "Gaia", "Ortega", "Mash", "Black Tri-Stars",
            "M'Quve", "Gadem", "Clamp", "Darcia Bakharov",
            "Zeon", "Sieg Zeon", "Principality of Zeon", "Autonomous Republic of Munzo",
            "Earth Federation", "Munzo", "Side 3", "Texas Colony", "Loum",
            "Battle of Loum", "Granada", "Solomon", "A Baoa Qu", "Jaburo",
            "Side 7", "White Base", "One Year War",
            "Mobile Worker", "Bugu", "Zaku I", "Zaku II", "Gouf", "Dom",
            "RX-78-02 Gundam", "Gundam", "Guntank", "Guncannon",
            "Mobile Suit", "Mobile Armor", "Newtype", "Oldtype",
            "Minovsky", "Spacenoid", "Earthnoid", "Mega Particle Cannon"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo UC + formas-ruim Origin (Mass, Zeon, Loum, Tri-Stars).
     *
     * <p>INVARIANTES DO DOMÍNIO: só aplica com canônico no original EN; canônicos dos extras
     * estão em {@link #termosProtegidos()}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUc.comExtras(Map.ofEntries(
            Map.entry("Base Branca", "White Base"),
            Map.entry("Trabalhador Móvel", "Mobile Worker"),
            Map.entry("Trabalhador Movel", "Mobile Worker"),
            Map.entry("Zion", "Zeon"),
            Map.entry("Principado de Zion", "Principality of Zeon"),
            Map.entry("Principado de Zeon", "Principality of Zeon"),
            Map.entry("República Autônoma de Munzo", "Autonomous Republic of Munzo"),
            Map.entry("Republica Autonoma de Munzo", "Autonomous Republic of Munzo"),
            Map.entry("Guerra de Um Ano", "One Year War"),
            Map.entry("Batalha de Loum", "Battle of Loum"),
            Map.entry("Triângulo Negro", "Black Tri-Stars"),
            Map.entry("Triangulo Negro", "Black Tri-Stars"),
            Map.entry("Três Estrelas Negras", "Black Tri-Stars"),
            Map.entry("Tres Estrelas Negras", "Black Tri-Stars"),
            Map.entry("Eduardo Mass", "Edouard Mass"),
            Map.entry("Edward Mass", "Edouard Mass")
        ));
    }
}
