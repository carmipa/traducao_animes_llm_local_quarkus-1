package org.traducao.projeto.contexto.lore.gundam;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * PROPÓSITO DE NEGÓCIO: lore completa de Gundam Unicorn / Unicorn RE:0096 (UC 0096) —
 * Laplace's Box, Unicorn/Banshee, Sleeves, Londo Bell/ECOAS.
 *
 * <p>INVARIANTES DO DOMÍNIO: Unicorn Gundam ≠ Gundam Unicórnio; Sleeves ≠ Mangas;
 * Laplace's Box; Psycho-Frame; NT-D; Full Frontal; Phenex é de NT (não misturar aqui).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoGundamUnicorn implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam Unicorn (OVA) / Mobile Suit Gundam Unicorn RE:0096 —
          Universal Century 0096.
        - Premissa: caca a Laplace's Box; Banagher Links e o RX-0 Unicorn Gundam;
          Earth Federation / Londo Bell / ECOAS vs Sleeves (Neo Zeon remnants) e Vist Foundation.

        === Nucleo UC ===
        - Newtype / Cyber-Newtype / Oldtype: oficiais; NUNCA "Novo Tipo".
        - Spacenoid vs Earthnoid; Minovsky particles; Psycho-Frame; NT-D; Destroy Mode /
          Unicorn Mode; psycommu; funnel; La+ (Laplace Program) quando o dialogo trouxer.
        - Mobile Suit vs Mobile Armor — distinguir rigorosamente.
        - Axis (mencoes historicas / Laplace) — NUNCA "Eixo".

        === Protagonistas / Sleeves ===
        - Banagher Links (m); Mineva Lao Zabi / Audrey Burne (f);
          Full Frontal (m — NUNCA "Frontal Completo"); Marida Cruz (f — Purge / "Sleeves");
          Suberoa Zinnerman (m); Angelo Sauper (m); Gilboa Sant (m); Tikva Sant (m) quando aparecer;
          Flaste Schole (m); Aaron Terzieff (m); Tomura (m) quando aparecer.

        === Federacao / Londo Bell / ECOAS / Nahel Argama ===
        - Riddhe Marcenas (m); Otto Midas (m); Daguza Mackle (m); Conroy Haagensen (m);
          Nigel Garrett (m); Hill Dawson (m); Liam Borrinea (f); Mihiro Oiwakken (f) quando aparecer;
          Bright Noa (m) / Ra Cailum quando a continuidade trouxer.

        === Vist / Anaheim / civis ===
        - Syam Vist (m); Cardeas Vist (m); Alberto Vist (m); Martha Vist Carbine (f);
          Takuya Irei (m); Micott Bartsch (f); Loni Garvey (f); Kai Shiden (m) cameo quando aparecer;
          Besson (m) quando aparecer.

        === Mecha ===
        - RX-0 Unicorn Gundam; Unicorn Gundam 02 Banshee / Banshee Norn;
          MSN-06S Sinanju; NZ-666 Kshatriya; MSN-001A1 Delta Plus;
          RGZ-95 ReZEL; RGM-89 Jegan; AMS-129 Geara Zulu; NZ-666 Kshatriya (Bustloff etc.);
          YAMS-132 Rozen Zulu; AMA-X7 Shamblo; Byarlant Custom; Stark Jegan; Anksha
          quando aparecerem.
        - Phenex (RX-0 Unicorn Gundam 03) NAO e unidade desta obra — aparece em NT.

        === Naves / lugares ===
        - Nahel Argama; Garencieres; Ra Cailum; Rewloola; Musaka; Magallanica;
          Industrial 7; Palau; Torrington Base; Dakar; Side colonies; Laplace Memorial /
          Laplace's Box / Laplace Incident.

        === Orgs ===
        - Sleeves (NUNCA "Mangas"); Neo Zeon; Vist Foundation; Anaheim Electronics;
          Londo Bell; ECOAS; Earth Federation / Federation Forces.

        === Regras ===
        - Unicorn Gundam NUNCA "Gundam Unicornio"; Full Frontal NUNCA "Frontal Completo";
          Sleeves NUNCA "Mangas"; Psycho-Frame NUNCA "moldura psicologica" generica.
        - Tom: politico-militar UC, melancolico, legado Char / Newtype / Laplace.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam Unicorn", LORE);

    @Override
    public String getId() {
        return "gundam_unicorn";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam Unicorn";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege elenco Unicorn / Sleeves / Londo Bell / Laplace.
     *
     * <p>INVARIANTES DO DOMÍNIO: grafias oficiais UC 0096; Phenex fora desta obra.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Banagher Links", "Mineva Lao Zabi", "Audrey Burne",
            "Full Frontal", "Marida Cruz", "Riddhe Marcenas",
            "Suberoa Zinnerman", "Otto Midas", "Daguza Mackle",
            "Martha Vist Carbine", "Cardeas Vist", "Alberto Vist",
            "Syam Vist", "Angelo Sauper", "Takuya Irei", "Micott Bartsch",
            "Loni Garvey", "Gilboa Sant", "Tikva Sant", "Flaste Schole",
            "Aaron Terzieff", "Conroy Haagensen", "Nigel Garrett",
            "Hill Dawson", "Liam Borrinea", "Mihiro Oiwakken", "Bright Noa",
            "Unicorn Gundam", "Banshee", "Banshee Norn",
            "Sinanju", "Kshatriya", "Delta Plus", "ReZEL", "Jegan",
            "Geara Zulu", "Rozen Zulu", "Shamblo", "Byarlant Custom",
            "Stark Jegan", "Anksha",
            "Nahel Argama", "Garencieres", "Ra Cailum", "Rewloola",
            "Musaka", "Magallanica",
            "Earth Federation", "Sleeves", "Sieg Zeon", "Neo Zeon", "Vist Foundation",
            "Anaheim Electronics", "Londo Bell", "ECOAS",
            "Laplace's Box", "Laplace Incident", "Industrial 7", "Palau",
            "Torrington Base", "Dakar",
            "Newtype", "Cyber-Newtype", "Oldtype", "Spacenoid",
            "Earthnoid", "Minovsky", "Psycho-Frame",
            "NT-D", "Destroy Mode", "psycommu", "funnel",
            "Mobile Suit", "Mobile Armor", "Axis", "La+"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo UC + formas-ruim Unicorn (Sleeves, Laplace, naves, modos).
     *
     * <p>INVARIANTES DO DOMÍNIO: só aplica com canônico no original EN; canônicos dos extras
     * estão em {@link #termosProtegidos()}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUc.comExtras(Map.ofEntries(
            Map.entry("Mangas", "Sleeves"),
            Map.entry("Manga", "Sleeves"),
            Map.entry("Moldura Psíquica", "Psycho-Frame"),
            Map.entry("Moldura Psiquica", "Psycho-Frame"),
            Map.entry("Caixa de Laplace", "Laplace's Box"),
            Map.entry("Incidente de Laplace", "Laplace Incident"),
            Map.entry("Fundação Vist", "Vist Foundation"),
            Map.entry("Fundacao Vist", "Vist Foundation"),
            Map.entry("Gundam Unicórnio", "Unicorn Gundam"),
            Map.entry("Gundam Unicornio", "Unicorn Gundam"),
            Map.entry("Eixo", "Axis"),
            Map.entry("Frontal Completo", "Full Frontal"),
            Map.entry("Modo Destruição", "Destroy Mode"),
            Map.entry("Modo Destruicao", "Destroy Mode")
        ));
    }
}
