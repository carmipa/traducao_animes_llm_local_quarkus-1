package org.traducao.projeto.contexto.lore.gundam.warInpocket;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.contexto.lore.gundam.CorrecoesTerminologiaGundamUc;

/**
 * PROPÓSITO DE NEGÓCIO: lore completa de Mobile Suit Gundam 0080: War in the Pocket
 * (OVA UC 0079/0080) — Cyclops Team, Gundam Alex e tragedia em Side 6 / Libot.
 *
 * <p>INVARIANTES DO DOMÍNIO: War in the Pocket ≠ Guerra no Bolso (titulo); Gundam Alex ≠ Alexandre;
 * Kampfer; Al (menino); Chris (piloto mulher); Cyclops Team; Newtype oficial.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoWarInPocket implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam 0080: War in the Pocket (OVA) — Universal Century
          U.C. 0079/0080, fim da One Year War.
        - Premissa: Cyclops Team (Zeon Special Forces) infiltra Side 6 / Libot colony
          para destruir o RX-78NT-1 Gundam Alex; Al Izuruha ve a guerra de perto;
          Bernie Wiseman e Chris Mackenzie no centro da tragedia.
        - Tom: anti-guerra, inocencia de crianca vs realidade militar. Evitar girias modernas
          e glamourizar combate quando a cena for tragedia.

        === Nucleo UC ===
        - Newtype (NUNCA Novo Tipo); Oldtype; Spacenoid vs Earthnoid; Minovsky particles;
          Beam Rifle / Beam Saber; Mobile Suit vs Mobile Armor; One Year War.

        === Faccoes / locais ===
        - Earth Federation / Federation Forces; Principality of Zeon / Zeon;
          Cyclops Team; Zeon Special Forces.
        - Side 6; Republic of Riah; Libot colony; Antarctic Base.

        === Roster ===
        - Alfred "Al" Izuruha (m, menino — garoto/menino, NUNCA garota);
          Bernard "Bernie" Wiseman (m); Christina "Chris" Mackenzie (f — piloto Federacao;
          1a pessoa feminina quando ela fala);
          Mikhail "Misha" Kaminsky (m); Hardy Steiner (m); Gabriel Ramirez Garcia (m);
          Andy Strauss (m); Colonel Killing (m) quando aparecer;
          Chay (m); Telcott (m) — amigos de Al quando o dialogo trouxer.

        === Mecha ===
        - RX-78NT-1 Gundam Alex (NUNCA Gundam Alexandre);
          MS-06FZ Zaku II Kai; MS-18E Kampfer (NUNCA Kämpfer localizado sem necessidade);
          MSM-03C Hygogg; MSM-07E Z'Gok-E;
          RGM-79G GM Cold Districts Type; RGM-79SP GM Sniper II.

        === Regras duras ===
        - War in the Pocket NUNCA "Guerra no Bolso" como titulo.
        - Gundam Alex / Kampfer / Hygogg / Z'Gok-E / Cyclops Team grafias oficiais.
        - Genero: Al/Bernie/Misha/Steiner/Garcia/Strauss/Killing = m; Chris = f.
        """;

    private static final String PROMPT = ContextoPrompt.montar(
        "Mobile Suit Gundam 0080: War in the Pocket",
        LORE
    );

    @Override
    public String getId() {
        return "gundam_0080";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam 0080: War in the Pocket";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege elenco 0080, Cyclops Team, mecha e colonias canônicas.
     *
     * <p>INVARIANTES DO DOMÍNIO: grafias oficiais War in the Pocket / UC.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Alfred Izuruha", "Al", "Bernard Wiseman", "Bernie",
            "Christina Mackenzie", "Chris", "Mikhail Kaminsky", "Misha",
            "Hardy Steiner", "Gabriel Ramirez Garcia", "Andy Strauss",
            "Colonel Killing", "Killing", "Chay", "Telcott",
            "Cyclops Team", "Zeon Special Forces",
            "Earth Federation", "Principality of Zeon", "Zeon",
            "Side 6", "Republic of Riah", "Libot", "Libot colony", "Antarctic Base",
            "Gundam Alex", "RX-78NT-1", "Zaku II Kai", "Kampfer",
            "Hygogg", "Z'Gok-E", "GM Cold Districts Type", "GM Sniper II",
            "War in the Pocket", "One Year War",
            "Mobile Suit", "Mobile Armor", "Beam Rifle", "Beam Saber",
            "Newtype", "Oldtype", "Spacenoid", "Earthnoid", "Minovsky"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo UC + formas-ruim próprias de 0080 (Alex, Cyclops, titulo).
     *
     * <p>INVARIANTES DO DOMÍNIO: só aplica com canônico no original EN.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUc.comExtras(Map.ofEntries(
            Map.entry("Gundam Alexandre", "Gundam Alex"),
            Map.entry("Kämpfer", "Kampfer"),
            Map.entry("Guerra no Bolso", "War in the Pocket"),
            Map.entry("Equipe Cyclops", "Cyclops Team"),
            Map.entry("Equipe Cíclope", "Cyclops Team"),
            Map.entry("Equipe Ciclope", "Cyclops Team"),
            Map.entry("República de Riah", "Republic of Riah"),
            Map.entry("Republica de Riah", "Republic of Riah"),
            Map.entry("Base Antártica", "Antarctic Base"),
            Map.entry("Base Antartica", "Antarctic Base"),
            Map.entry("Guerra de Um Ano", "One Year War"),
            Map.entry("Principado de Zeon", "Principality of Zeon"),
            Map.entry("GM Distritos Frios", "GM Cold Districts Type")
        ));
    }
}
