package org.traducao.projeto.contexto.lore.gundam.msteam;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.contexto.lore.gundam.CorrecoesTerminologiaGundamUc;

/**
 * PROPÓSITO DE NEGÓCIO: lore completa de Mobile Suit Gundam: The 08th MS Team (OVA UC 0079) —
 * guerra terrestre na selva, Projeto Apsalus/Apsaras e Kojima Battalion, com roster amplo e
 * aliases da fansub EN (Joseki) para o LLM não inventar grafias em PT-BR.
 *
 * <p>INVARIANTES DO DOMÍNIO: 08th MS Team; Shiro Amada; Aina Sahalin/Sakhalin; Gundam Ez8;
 * Gouf Custom; Apsalus/Apsaras (Mobile Armor); Eledore Massis/Mathis; Miller's Report;
 * nomes próprios e topônimos ficam em inglês; Newtype oficial; realismo anti-guerra.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoGundam08thMSTeam implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam: The 08th MS Team (OVA 1996-1999) — Universal Century U.C. 0079
          (One Year War), paralelo ao primeiro Gundam, foco exclusivo na guerra terrestre.
        - Premissa: Kojima Battalion / 08th MS Team na selva do Southeast Asia vs Zeon e o
          Apsalus Project / Apsaras Project (Ginias Sahalin); romance Shiro Amada x Aina Sahalin.
        - Tom: realismo militar anti-guerra (exaustao, trauma, civis, absurdo da frente).
          Evitar girias modernas; radio curto e tecnico.
        - Grafias: nomes, locais, mecha e orgs ficam em INGLES. A fansub Joseki mistura grafias
          (Sahalin/Sakhalin, Ginias/Ginius, Apsalus/Apsaras, Massis/Mathis, Rosita/Logita,
          Kellerne/Kergeren/Kellarny, Nickard/Nickerd, Bone Abust/Von Abst) — preservar a forma
          que vier no original EN; nao "corrigir" alias da fonte nem traduzir nomes proprios.

        === Nucleo UC ===
        - Newtype / Oldtype; Spacenoid vs Earthnoid; Minovsky particles;
          Mega Particle Cannon; Beam Rifle / Beam Saber.
        - Mobile Suit vs Mobile Armor — Apsalus/Apsaras e Mobile Armor (NUNCA Mobile Suit).
        - Earth Federation / Earth Federation Forces; Principality of Zeon / Zeon; Sieg Zeon;
          One Year War; Antarctic Treaty; Operation Star One quando aparecerem.

        === Roster — 08th MS Team / Federation ===
        - Shiro Amada (m) — Ensign; commander of the 08th MS Team ("Commander Newbie").
        - Karen Joshua (f) — Master Chief; pilot / field medic.
        - Terry Sanders Jr. (m) — Chief Sanders; "Grim Reaper" / Shinigami Sanders.
        - Eledore Massis (m) — hovertruck (radar/sonar/com); fansub tambem: Mathis.
        - Michel Ninorich (m) — hovertruck (navigation/gunner); letters to B.B.
        - Kojima (m) — Commander Kojima; Kojima Battalion.
        - Isan Ryer (m) — Captain Ryer.
        - Jidan Nickard (m) — Lieutenant Nickard; fansub: Nickerd.
        - Sally (f) — Federation support / nurse when named.
        - Alice Miller (f) — Federation intelligence; Miller's Report.
        - Agent Jacob (m) — Miller's Report when named.
        - Rob (m); Pietro (m); Mike (m) — Kojima Battalion support when named.
        - B.B. (f) — Michel's girlfriend (letters) when dialogue brings her.

        === Roster — Zeon ===
        - Aina Sahalin (f) — Apsalus/Apsaras test pilot; fansub: Sakhalin / Rear Admiral Sakhalin line.
        - Ginias Sahalin (m) — Apsalus Project lead; fansub: Ginius / Commander Ginius / Master Ginius.
        - Norris Packard (m) — Captain Norris; ace; Gouf Custom; loyal to Aina.
        - Yuri Kellerne (m) — Admiral Yuri Kellerne; fansub: Kergeren / Kellarny / Rear Admiral Yuri.
        - Cynthia (f) — Zeon staff with Kellerne when named.
        - Bone Abust (m) — fansub: Von Abst.
        - Topp (f) — Zeon MS squad leader (village occupation).
        - Arth (m); Dell (m); Masado (m); Barry (m); Niever (m); Runen / Lunen (m);
          Walter Janowitz (m); Nielba; Zhukov — Zeon ranks when named.
        - Kergerenko — Zeon Gaw-class related officer/callsign when named (fansub: Kergeren).
        - Cameos UC: Gihren Zabi (m); Sovereign Degwin; General Revil; Flanagan Institute when named.

        === Roster — civilians / guerrillas ===
        - Kiki Rosita (f) — local guerrilla; fansub: Kiki Logita.
        - Baresto Rosita (m) — Kiki's father / village elder (often "Dad" in EN).
        - Maria (f) — civilian when named.
        - Chibi; Hige; Noppo — guerrilla nicknames when they appear.

        === Mecha / vehicles / ships ===
        - RX-79[G] Gundam Ground Type / Ground Gundam; RX-79[G] Ez-8 Gundam Ez8 (Shiro);
          RGM-79[G] GM Ground Type; Guntank when named.
        - MS-06J Zaku II Ground Type; MS-07B-3 Gouf Custom; Gouf Flight Type;
          Zaku Tank; Magella Attack; Rick Dom; Ball.
        - Apsalus I / II / III (= Apsaras I / II / III) — Minovsky Craft + mega particle cannon.
        - Hovertruck (Eledore / Michel).
        - Aircraft/ships: Dopp; Gaw; Medea; Musai; Komusai; Zanzibar; Kergeren (ship callsign).

        === Orgs / places / ops / satellite work ===
        - 08th MS Team; Kojima Battalion; Combined Mechanized Battalion;
          Far East Division; Earth Federation Forces; Principality of Zeon.
        - Southeast Asia jungle front; mountain fortress / Apsalus base; village / guerrilla lines;
          Jaburo; Odessa; Europe / European front when mentioned;
          Point Bravo; Point Charlie; Burning Sand (episode title/area cue).
        - Miller's Report — epilogue film; Alice Miller; Agent Jacob.

        === Regras duras ===
        - 08th MS Team NUNCA "8o Time MS" / "Oitava Equipe MS" as title.
        - Apsalus/Apsaras NUNCA "Absalao"/"Absaras" inventados; preferir a grafia do EN da fala.
        - Gouf Custom NUNCA "Gouf Personalizado"; Gundam Ez8 / Gundam Ground Type oficiais.
        - Genero: Shiro/Ginias/Ginius/Norris/Sanders/Eledore/Michel/Kojima/Ryer/Nickard/
          Baresto/Bone Abust/Arth/Dell/Masado/Kellerne/Gihren/Revil = m;
          Aina/Karen/Kiki/Alice/B.B./Sally/Topp/Cynthia/Maria = f.
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
     * PROPÓSITO DE NEGÓCIO: protege elenco 08th/Zeon/civis, aliases da fansub EN, mecha terrestre,
     * naves e topônimos UC da obra para o LLM não localizar nomes próprios.
     *
     * <p>INVARIANTES DO DOMÍNIO: inclui canônico e variantes Joseki (Sakhalin, Ginius, Apsaras,
     * Nickerd, Kergeren, Logita, Mathis, Von Abst); Miller's Report; Ez8.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            // Federation / 08th
            "Shiro Amada", "Karen Joshua", "Terry Sanders Jr.", "Sanders",
            "Eledore Massis", "Eledore Mathis", "Michel Ninorich",
            "Kojima", "Isan Ryer", "Captain Ryer", "Alice Miller", "Agent Jacob",
            "Jidan Nickard", "Nickerd", "Lieutenant Nickerd", "Sally",
            "Rob", "Pietro", "Mike", "B.B.",
            // Zeon
            "Aina Sahalin", "Aina Sakhalin", "Sakhalin",
            "Ginias Sahalin", "Ginius", "Commander Ginius",
            "Norris Packard", "Captain Norris",
            "Yuri Kellerne", "Yuri Kellarny", "Kellerne", "Kergeren", "Admiral Yuri",
            "Cynthia", "Bone Abust", "Von Abst",
            "Topp", "Arth", "Dell", "Masado", "Barry", "Niever", "Runen", "Lunen",
            "Walter Janowitz", "Nielba", "Zhukov", "Kergerenko",
            "Gihren Zabi", "Gihren", "Degwin", "General Revil", "Revil", "Flanagan",
            // Civilians
            "Kiki Rosita", "Kiki Logita", "Baresto Rosita", "Maria",
            "Chibi", "Hige", "Noppo",
            // Mecha / ships
            "Gundam Ez8", "Ez-8", "Ez8", "Ground Gundam", "Gundam Ground Type",
            "GM Ground Type", "Zaku II Ground Type", "Gouf Custom", "Gouf Flight Type",
            "Zaku Tank", "Magella Attack", "Guntank", "Rick Dom", "Ball",
            "Apsalus", "Apsalus I", "Apsalus II", "Apsalus III",
            "Apsaras", "Apsaras I", "Apsaras II", "Apsaras III", "Apsaras Project",
            "Hovertruck", "Dopp", "Gaw", "Medea", "Musai", "Komusai", "Zanzibar",
            // Orgs / places / ops
            "Miller's Report", "Earth Federation", "Earth Federation Forces",
            "Principality of Zeon", "Zeon", "Sieg Zeon",
            "Kojima Battalion", "08th MS Team", "Combined Mechanized Battalion",
            "Far East Division", "Jaburo", "Odessa", "Southeast Asia",
            "Point Bravo", "Point Charlie", "Burning Sand",
            "One Year War", "Antarctic Treaty", "Operation Star One",
            "Newtype", "Oldtype", "Spacenoid", "Earthnoid", "Minovsky",
            "Mega Particle Cannon", "Mobile Suit", "Mobile Armor",
            "Beam Rifle", "Beam Saber"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo UC + formas-ruim próprias do 08th (Gouf, Apsalus, Ground Type,
     * títulos e aliases mal traduzidos).
     *
     * <p>INVARIANTES DO DOMÍNIO: só aplica com canônico no original EN; canônicos dos extras
     * estão em {@link #termosProtegidos()}; Absalão/Absaras → Apsalus (preferência de correção PT).
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
            Map.entry("Absaras", "Apsalus"),
            Map.entry("Projeto Absalão", "Apsalus Project"),
            Map.entry("Projeto Apsaras", "Apsaras Project"),
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
            Map.entry("Caminhao Hover", "Hovertruck"),
            Map.entry("Tratado Antártico", "Antarctic Treaty"),
            Map.entry("Tratado Antartico", "Antarctic Treaty"),
            Map.entry("Operação Star One", "Operation Star One"),
            Map.entry("Operacao Star One", "Operation Star One"),
            Map.entry("Divisão do Extremo Oriente", "Far East Division"),
            Map.entry("Batalhão Kojima", "Kojima Battalion"),
            Map.entry("Batalhao Kojima", "Kojima Battalion")
        ));
    }
}
