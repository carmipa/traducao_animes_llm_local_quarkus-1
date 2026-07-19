package org.traducao.projeto.contexto.lore.gundam;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * PROPÓSITO DE NEGÓCIO: lore excepcional de Gundam Unicorn (UC 0096) —
 * Laplace's Box, Unicorn/Banshee, Sleeves.
 *
 * <p>INVARIANTES DO DOMÍNIO: Unicorn ≠ Unicórnio (nome da unidade); Sleeves ≠ Mangas;
 * Laplace's Box; Psycho-Frame; Newtype; Phenex é de NT (não misturar como unidade desta obra).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoGundamUnicorn implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam Unicorn (OVA) / Unicorn RE:0096 — Universal Century 0096.
        - Premissa: caca a Laplace's Box; Banagher Links e o RX-0 Unicorn Gundam;
          Earth Federation, Sleeves (Neo Zeon remnants), Vist Foundation, Londo Bell / ECOAS.

        === Nucleo UC ===
        - Newtype / Cyber-Newtype / Oldtype: oficiais; NUNCA "Novo Tipo".
        - Spacenoid vs Earthnoid; Minovsky particles; Psycho-Frame; NT-D; Destroy Mode;
          psycommu; funnel; La+ (Laplace Program) quando o dialogo trouxer.
        - Mobile Suit vs Mobile Armor — distinguir rigorosamente.

        === Pessoas (genero) ===
        - Banagher Links (m), Mineva Lao Zabi / Audrey Burne (f), Full Frontal (m),
          Riddhe Marcenas (m), Marida Cruz (f), Suberoa Zinnerman (m), Otto Midas (m),
          Daguza Mackle (m), Martha Vist Carbine (f), Cardeas Vist (m), Alberto Vist (m),
          Syam Vist (m), Angelo Sauper (m), Takuya Irei (m), Micott Bartsch (f),
          Loni Garvey (f) quando aparecer.

        === Mecha / naves / orgs ===
        - RX-0 Unicorn Gundam; Unicorn Gundam 02 Banshee / Banshee Norn;
          MSN-06S Sinanju; NZ-666 Kshatriya; Delta Plus; ReZEL; Jegan; Geara Zulu;
          Rozen Zulu; Shamblo.
        - Nahel Argama; Garencieres; Industrial 7; Palau; Torrington Base.
        - Sleeves; Neo Zeon; Vist Foundation; Anaheim Electronics; Londo Bell; ECOAS.
        - Laplace's Box / Laplace Incident. Phenex NAO e unidade desta obra (aparece em NT).

        === Regras ===
        - Unicorn Gundam NUNCA vira "Gundam Unicornio"; Sleeves NUNCA "Mangas";
          Full Frontal NUNCA "Frontal Completo"; Axis (mencoes) NUNCA "Eixo".
        - Tom: politico-militar UC, melancolico, legado Char / Newtype.
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
     * PROPÓSITO DE NEGÓCIO: protege elenco Unicorn / Sleeves / Laplace.
     *
     * <p>INVARIANTES DO DOMÍNIO: grafias oficiais UC 0096.
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
            "Loni Garvey", "Unicorn Gundam", "Banshee", "Banshee Norn",
            "Sinanju", "Kshatriya", "Delta Plus", "Nahel Argama",
            "Garencieres", "Earth Federation", "Sleeves",
            "Neo Zeon", "Vist Foundation", "Anaheim Electronics",
            "Londo Bell", "ECOAS", "Laplace's Box", "Industrial 7",
            "Newtype", "Cyber-Newtype", "Oldtype", "Spacenoid",
            "Earthnoid", "Minovsky", "Psycho-Frame",
            "NT-D", "psycommu", "funnel", "Mobile Suit", "Mobile Armor", "Axis"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo UC + formas-ruim Unicorn (Sleeves, Laplace, Psycho-Frame).
     *
     * <p>INVARIANTES DO DOMÍNIO: só aplica com canônico no original EN.
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
            Map.entry("Fundação Vist", "Vist Foundation"),
            Map.entry("Fundacao Vist", "Vist Foundation"),
            Map.entry("Gundam Unicórnio", "Unicorn Gundam"),
            Map.entry("Gundam Unicornio", "Unicorn Gundam"),
            Map.entry("Eixo", "Axis"),
            Map.entry("Frontal Completo", "Full Frontal")
        ));
    }
}
