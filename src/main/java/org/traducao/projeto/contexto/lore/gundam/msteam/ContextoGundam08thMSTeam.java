package org.traducao.projeto.contexto.lore.gundam.msteam;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.contexto.lore.gundam.CorrecoesTerminologiaGundamUc;

/**
 * PROPÓSITO DE NEGÓCIO: lore completa de Mobile Suit Gundam: The 08th MS Team (OVA UC 0079) —
 * guerra terrestre na selva, Projeto Apsalus e Kojima Battalion.
 *
 * <p>INVARIANTES DO DOMÍNIO: 08th MS Team; Shiro Amada; Aina Sahalin; Gundam Ez8; Gouf Custom;
 * Apsalus (Mobile Armor); Eledore Massis; Miller's Report; Newtype oficial; realismo anti-guerra.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoGundam08thMSTeam implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam: The 08th MS Team (OVA 1996-1999) — Universal Century U.C. 0079
          (One Year War), paralelo ao primeiro Gundam, foco exclusivo na guerra terrestre.
        - Premissa: Kojima Battalion / 08th MS Team na selva do Sudeste Asiatico vs Zeon e o
          Projeto Apsalus (Ginias Sahalin); romance Shiro Amada x Aina Sahalin.
        - Tom: realismo militar anti-guerra (exaustao, trauma, civis, absurdo da frente).
          Evitar girias modernas; radio curto e tecnico.

        === Nucleo UC ===
        - Newtype / Oldtype; Spacenoid vs Earthnoid; Minovsky particles;
          Mega Particle Cannon; Beam Rifle / Beam Saber.
        - Mobile Suit vs Mobile Armor — Apsalus e Mobile Armor (NUNCA Mobile Suit).
        - Earth Federation / Principality of Zeon; One Year War; Jaburo quando aparecer.

        === Roster — 08th MS Team / Federacao ===
        - Shiro Amada (m) — comandante da 08th MS Team.
        - Karen Joshua (f) — piloto / medica de campo.
        - Terry Sanders Jr. (m) — "Shinigami Sanders".
        - Eledore Massis (m) — hovertruck (radar/com); grafia Massis (Mathis e variante).
        - Michel Ninorich (m) — hovertruck (navegacao/artilharia); cartas com B.B.
        - Kojima (m) — comandante do Kojima Battalion.
        - Isan Ryer (m); Alice Miller (f) — inteligencia / Miller's Report quando trouxer.
        - Yuri Kellarny (m) quando aparecer.

        === Roster — Zeon / civis ===
        - Aina Sahalin (f) — piloto de teste do Apsalus.
        - Ginias Sahalin (m) — Projeto Apsalus; paranoia/megalomania.
        - Norris Packard (m) — ace; Gouf Custom; leal a Aina.
        - Kiki Rosita (f); Baresto Rosita (m) — guerrilha/vila local.
        - B.B. (f) — namorada de Michel (cartas) quando o dialogo trouxer.

        === Mecha / veiculos ===
        - RX-79[G] Gundam Ground Type / Ground Gundam; RX-79[G] Ez-8 Gundam Ez8 (Shiro);
          RGM-79[G] GM Ground Type.
        - MS-06J Zaku II Ground Type; MS-07B-3 Gouf Custom; Gouf Flight Type;
          Zaku Tank; Magella Attack.
        - Apsalus I / Apsalus II / Apsalus III (Minovsky Craft + mega particle cannon).
        - Hovertruck de suporte (Eledore / Michel).

        === Orgs / lugares / obras satelite ===
        - 08th MS Team; Kojima Battalion; Earth Federation; Principality of Zeon / Zeon.
        - Frente de selva (Sudeste Asiatico); bases improvisadas; Jaburo quando cruzar.
        - Miller's Report — epilogo/filme; Alice Miller.

        === Regras duras ===
        - 08th MS Team NUNCA "8o Time MS" / "Oitava Equipe MS" como titulo.
        - Apsalus NUNCA "Absalao"; Gouf Custom NUNCA "Gouf Personalizado";
          Gundam Ez8 / Gundam Ground Type grafias oficiais.
        - Genero: Shiro/Ginias/Norris/Sanders/Eledore/Michel/Kojima/Ryer = m;
          Aina/Karen/Kiki/Alice/B.B. = f.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam: The 08th MS Team", LORE);

    @Override
    public String getId() {
        return "gundam_08ms";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam: The 08th MS Team";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege elenco 08th/Zeon, mecha terrestre e termos UC da obra.
     *
     * <p>INVARIANTES DO DOMÍNIO: Eledore Massis; Ez8; Apsalus; Miller's Report canônicos.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Shiro Amada", "Aina Sahalin", "Karen Joshua",
            "Terry Sanders Jr.", "Eledore Massis", "Michel Ninorich",
            "Kiki Rosita", "Baresto Rosita", "Ginias Sahalin", "Norris Packard",
            "Kojima", "Isan Ryer", "Alice Miller", "Yuri Kellarny", "B.B.",
            "Gundam Ez8", "Ez-8", "Ground Gundam", "Gundam Ground Type",
            "GM Ground Type", "Zaku II Ground Type", "Gouf Custom", "Gouf Flight Type",
            "Zaku Tank", "Magella Attack",
            "Apsalus", "Apsalus I", "Apsalus II", "Apsalus III",
            "Hovertruck", "Miller's Report",
            "Earth Federation", "Principality of Zeon", "Zeon", "Sieg Zeon",
            "Kojima Battalion", "08th MS Team",
            "Jaburo", "One Year War", "Newtype", "Oldtype",
            "Spacenoid", "Earthnoid", "Minovsky", "Mega Particle Cannon",
            "Mobile Suit", "Mobile Armor", "Beam Rifle", "Beam Saber"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo UC + formas-ruim próprias do 08th (Gouf, Apsalus, Ground Type).
     *
     * <p>INVARIANTES DO DOMÍNIO: só aplica com canônico no original EN; canônicos dos extras
     * estão em {@link #termosProtegidos()}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUc.comExtras(Map.ofEntries(
            Map.entry("Gouf Personalizado", "Gouf Custom"),
            Map.entry("Gouf Customizado", "Gouf Custom"),
            Map.entry("Absalão", "Apsalus"),
            Map.entry("Absalao", "Apsalus"),
            Map.entry("Gundam Tipo Terrestre", "Gundam Ground Type"),
            Map.entry("GM Tipo Terrestre", "GM Ground Type"),
            Map.entry("Zaku Tipo Terrestre", "Zaku II Ground Type"),
            Map.entry("Guerra de Um Ano", "One Year War"),
            Map.entry("Relatório Miller", "Miller's Report"),
            Map.entry("Relatorio Miller", "Miller's Report"),
            Map.entry("8º Time MS", "08th MS Team"),
            Map.entry("8o Time MS", "08th MS Team"),
            Map.entry("Oitava Equipe MS", "08th MS Team"),
            Map.entry("Principado de Zeon", "Principality of Zeon"),
            Map.entry("Caminhão Hover", "Hovertruck"),
            Map.entry("Caminhao Hover", "Hovertruck")
        ));
    }
}
