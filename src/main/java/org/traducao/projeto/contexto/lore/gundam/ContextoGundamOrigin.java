package org.traducao.projeto.contexto.lore.gundam;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * PROPÓSITO DE NEGÓCIO: lore excepcional de Mobile Suit Gundam: The Origin
 * (OVA / prequela Char–Zeon, UC pré-0079).
 *
 * <p>INVARIANTES DO DOMÍNIO: Casval/Char e Artesia/Sayla; família Zabi; Zeon Zum Deikun;
 * Principality of Zeon; Mobile Worker → Zaku; White Base; Newtype; proibido localizar Zeon/Char.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoGundamOrigin implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam: The Origin (OVA / mangá U.C. — origem de Char e da Guerra de Um Ano).
        - Premissa: ascensao de Zeon em Side 3 / Munzo; Casval Rem Deikun torna-se Char Aznable;
          Artesia Som Deikun torna-se Sayla Mass; politica Zabi vs ideais de Zeon Zum Deikun;
          desenvolvimento do Mobile Worker → Mobile Suit (Zaku).

        === Personagens (genero; grafia oficial) ===
        - Casval Rem Deikun / Char Aznable (m) — mascara, estrategista; NUNCA "Shar"/"Xar".
        - Artesia Som Deikun / Sayla Mass (f).
        - Zeon Zum Deikun (m) — ideologo; Amuro Ray (m) — aparicoes/continuidade UC.
        - Familia Zabi: Degwin Sodo Zabi (m), Gihren Zabi (m), Dozle Zabi (m),
          Kycilia Zabi (f), Garma Zabi (m), Sasro Zabi (m) quando aparecer.
        - Ramba Ral (m), Crowley Hamon (f), Torenov Y. Minovsky (m) quando o dialogo trouxer.
        - Jimba Ral (m), Astraia Tor Deikun (f) quando aparecerem.

        === Faccoes / lugares ===
        - Principality of Zeon / Zeon; Autonomous Republic of Munzo; Side 3; Munzo; Earth Federation.
        - Texas Colony; Loum (contexto da guerra); White Base (quando a continuidade cruzar 0079).

        === Mecha / termos UC ===
        - Mobile Worker; MS-04 Bugu; MS-05 Zaku I; MS-06S Zaku II (Char's); RX-78-02 Gundam.
        - Mobile Suit vs Mobile Armor; Newtype (NUNCA "Novo Tipo"); Oldtype; Minovsky particles.
        - Beam Saber / Beam Rifle quando o dialogo trouxer.

        === Regras duras ===
        - Char/Casval/Sayla/Artesia/Zabi/Zeon/Munzo/Zaku/Gundam/White Base — grafia oficial.
        - Principality of Zeon nao vira "Principado de Zion" (Zion mitologico).
        - Tom: politico-militar pre-guerra; Char frio/calculista; Zabi autoritarios; Hamon leal a Ramba.
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
     * PROPÓSITO DE NEGÓCIO: protege elenco Origin / Zabi / Zeon e mechas canônicos.
     *
     * <p>INVARIANTES DO DOMÍNIO: grafias oficiais UC Origin.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Char Aznable", "Casval Rem Deikun", "Sayla Mass", "Artesia Som Deikun",
            "Amuro Ray", "Zeon Zum Deikun", "Degwin Sodo Zabi", "Gihren Zabi",
            "Dozle Zabi", "Kycilia Zabi", "Garma Zabi", "Sasro Zabi",
            "Ramba Ral", "Crowley Hamon", "Jimba Ral", "Astraia Tor Deikun",
            "Torenov Y. Minovsky", "Zeon", "Principality of Zeon", "Earth Federation",
            "Side 3", "Munzo", "Zaku I", "Zaku II", "Bugu", "Gundam",
            "Mobile Worker", "Mobile Suit", "Mobile Armor", "Newtype", "Oldtype",
            "Minovsky", "White Base", "Texas Colony"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo UC + formas-ruim Origin (White Base, Mobile Worker, Zion).
     *
     * <p>INVARIANTES DO DOMÍNIO: só aplica com canônico no original EN.
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
            Map.entry("Principado de Zeon", "Principality of Zeon")
        ));
    }
}
