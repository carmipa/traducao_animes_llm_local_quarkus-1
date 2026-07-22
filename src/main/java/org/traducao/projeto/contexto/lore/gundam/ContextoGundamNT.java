package org.traducao.projeto.contexto.lore.gundam;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * PROPÓSITO DE NEGÓCIO: lore completa de Mobile Suit Gundam NT (Narrative) — UC 0097,
 * Operation Phoenix Hunt / Phenex, calibrada no diálogo real do BD.
 *
 * <p>INVARIANTES DO DOMÍNIO: Narrative Gundam ≠ Gundam Narrativo; Phenex ≠ Fênix;
 * Miracle Children; Shezarr; Sleeves ≠ Mangas; Psycho-Frame; Newtype oficial.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoGundamNT implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam NT (Narrative) / Kidou Senshi Gundam NT (filme 2018) —
          Universal Century U.C. 0097, um ano apos Laplace Incident / Unicorn.
        - Premissa: Operation Phoenix Hunt caca o RX-0 Unicorn Gundam 03 Phenex;
          Miracle Children (Jona / Michele / Rita); Shezarr Team vs Sleeves / Zoltan.
        - Tom: drama militar-espiritual, melancolico. Evitar girias modernas.
          Fonte: termos abaixo aparecem no dialogo BD EN — preserve-os.

        === Nucleo UC ===
        - Newtype (NUNCA Novo Tipo); Cyber-Newtype; Oldtype; Spacenoid vs Earthnoid.
        - Psycho-Frame / psycommu / funnel / NT-D; Minovsky particles;
          Mega Particle Cannon; Mobile Suit vs Mobile Armor
          (II Neo Zeong = Mobile Armor; Narrative/Phenex/Sinanju Stein = Mobile Suit).

        === Faccoes / orgs ===
        - Earth Federation; Shezarr Team (callsigns Shezarr 001…007; Jona = Shezarr 007).
        - Luio & Co. (Luio Woomin / Stephanie Luio / Michele Luio).
        - Republic of Zeon; Sleeves (remanescentes Neo Zeon — NUNCA Mangas).

        === Roster ===
        - Jona Basta (m); Michele Luio (f — preferir Michele); Rita Bernal (f);
          Zoltan Akkanen (m); Iago Haakana (m); Brick Teclato (m); Fransson (m);
          Monaghan Bakharo (m); Luio Woomin (m); Stephanie Luio (f);
          Banagher Links (m); Mineva Lao Zabi (f) — continuidade Unicorn.

        === Mecha / lugares ===
        - RX-9 Narrative Gundam (A-Packs / B-Packs / C-Packs) — NUNCA Gundam Narrativo;
          RX-0 Unicorn Gundam 03 Phenex (NUNCA Fenís/Fênix como unidade);
          Sinanju Stein; II Neo Zeong (MA); Silver Bullet Suppressor;
          Unicorn / Banshee (legado).
        - Metis (colonia); Banchi 18; Helium-3 / Minovsky overconcentration quando aparecer.

        === Operacoes / legado Unicorn ===
        - Operation Phoenix Hunt; Laplace's Box; Laplace Incident; Universal Century / U.C.

        === Regras duras ===
        - Narrative / Narrative Gundam / Phenex / Miracle Children / Shezarr oficiais.
        - Radio militar curto ("Shezarr 001 to all units", "no eyes on target").
        - Genero: Jona/Zoltan/Iago/Brick/Banagher/Monaghan/Fransson/Luio Woomin = m;
          Michele/Rita/Mineva/Stephanie = f.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam NT (Narrative)", LORE);

    @Override
    public String getId() {
        return "gundam_nt";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam NT (Narrative)";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege elenco NT, Phenex/Narrative, Shezarr e termos UC do filme.
     *
     * <p>INVARIANTES DO DOMÍNIO: só artefatos NT / continuidade Unicorn confirmados.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Jona Basta", "Michele Luio", "Rita Bernal",
            "Zoltan Akkanen", "Luio Woomin", "Stephanie Luio",
            "Banagher Links", "Mineva Lao Zabi", "Iago Haakana",
            "Brick Teclato", "Monaghan Bakharo", "Fransson",
            "Narrative", "Narrative Gundam", "Phenex",
            "Sinanju Stein", "II Neo Zeong", "Silver Bullet Suppressor",
            "Unicorn", "Banshee", "Unicorn Gundam",
            "Earth Federation", "Shezarr", "Shezarr Team",
            "Luio & Co.", "Republic of Zeon", "Sleeves", "Sieg Zeon",
            "Miracle Children", "Metis", "Banchi 18",
            "Operation Phoenix Hunt", "Laplace's Box", "Laplace Incident",
            "Universal Century", "Helium-3",
            "Newtype", "Cyber-Newtype", "Oldtype", "Spacenoid", "Earthnoid",
            "Minovsky", "Psycho-Frame", "psycommu", "funnel", "NT-D",
            "Mega Particle Cannon", "Mobile Suit", "Mobile Armor",
            "A-Packs", "B-Packs", "C-Packs"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo UC + formas-ruim NT (Phenex, Narrative, Phoenix Hunt…).
     *
     * <p>INVARIANTES DO DOMÍNIO: só aplica com canônico no original EN.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUc.comExtras(Map.ofEntries(
            Map.entry("Operação Caça à Fênix", "Operation Phoenix Hunt"),
            Map.entry("Operacao Caca a Fenix", "Operation Phoenix Hunt"),
            Map.entry("Crianças Milagrosas", "Miracle Children"),
            Map.entry("Criancas Milagrosas", "Miracle Children"),
            Map.entry("Gundam Narrativo", "Narrative Gundam"),
            Map.entry("Fenís", "Phenex"),
            Map.entry("Fênix", "Phenex"),
            Map.entry("Fenix", "Phenex"),
            Map.entry("Mangas", "Sleeves"),
            Map.entry("Manga", "Sleeves"),
            Map.entry("Moldura Psíquica", "Psycho-Frame"),
            Map.entry("Moldura Psiquica", "Psycho-Frame"),
            Map.entry("Caixa de Laplace", "Laplace's Box"),
            Map.entry("Incidente de Laplace", "Laplace Incident"),
            Map.entry("Equipe Shezarr", "Shezarr Team"),
            Map.entry("Supressor Silver Bullet", "Silver Bullet Suppressor"),
            Map.entry("Silver Bullet Supressor", "Silver Bullet Suppressor")
        ));
    }
}
