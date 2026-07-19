package org.traducao.projeto.contexto.lore.gundam;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore de Gundam NT (Narrative) calibrada no artefato real
 * (legendas EN/PT do BD em C:\\TRACKER-ANIMES\\animes\\Gundam Narrative NT).
 *
 * <p>INVARIANTES DO DOMÍNIO: Newtype/Cyber-Newtype/Oldtype; Spacenoid; Mobile Suit
 * vs Mobile Armor; Phenex; Shezarr; Metis/Banchi 18/Fransson; Minovsky vs psycho-waves.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoGundamNT implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam NT (Narrative) / Kidou Senshi Gundam NT (filme 2018, UC 0097).
        - Continuidade: um ano apos Laplace Incident / Gundam Unicorn. Operation Phoenix Hunt
          caça o RX-0 Unicorn Gundam 03 Phenex.
        - Fonte de calibracao: legendas oficiais do BD (EN Track3 + PT) — termos abaixo
          aparecem no dialogo real; preserve-os.

        === Nucleo UC (NUNCA traduzir literalmente) ===
        - Newtype: NUNCA "Novo Tipo". Cyber-Newtype = humano modificado artificialmente
          (Zoltan e outros; dialogo: "Are you sure he ain't a Cyber-Newtype?").
        - Oldtype: pejorativo para humanos "comuns" (dialogo: "The only thing that Oldtypes
          understand is physical phenomena!"). NUNCA traduzir como "tipo antigo" casual.
        - Spacenoid: habitante das colonias espaciais (dialogo: "fellow Spacenoids",
          "restoration of Spacenoids"). Earthnoid quando aparecer = elite/habitante da Terra.
        - Mobile Suit vs Mobile Armor: distinguir rigorosamente. MS = humanoide (Narrative,
          Phenex, Sinanju Stein). MA = unidade tatica nao-humanoide de grande porte
          (dialogo cita "gigantic mobile armor" / II Neo Zeong como mobile armor).
        - Minovsky particles: bloqueiam radar/radio convencional; dialogo distingue
          "Psycho-waves aren't affected by Minovsky particles" e "Nothing on Minovsky radar".
          Mega Particle Cannon / reactor Minovsky: consistencia tecnica rigida.
        - psycho-frame / Psycho-Frame, psycommu, funnel, NT-D: oficiais. NT-D "will attack
          genuine Newtypes" (dialogo).

        === Conflito e faccoes ===
        - Earth Federation / Shezarr Team (callsigns Shezarr 001…007; Ensign Jona = Shezarr 007).
        - Luio & Co.: Chairman Luio Woomin; President Stephanie Luio; Michele Luio
          (segunda filha / consultora — rumores vs Stephanie no dialogo).
        - Republic of Zeon; Sleeves (remanescentes Neo Zeon).
        - Miracle Children: Jona, Michele, Rita (experimento/trauma Newtype).

        === Pessoas (genero) ===
        - Jona Basta (m), Michele Luio / Michelle (f — preferir Michele), Rita Bernal (f),
          Zoltan Akkanen (m), Iago Haakana (m), Brick Teclato (m), Mineva Lao Zabi (f),
          Banagher Links (m), Monaghan Bakharo (m), Fransson (m — Cyber-Newtype no dialogo),
          Luio Woomin (m), Stephanie Luio (f).

        === Mecha / lugares confirmados no dialogo ===
        - RX-9 Narrative Gundam (A/B/C-Packs); RX-0 Unicorn Gundam 03 Phenex (NUNCA "Fenís"
          como nome da unidade); Sinanju Stein; II Neo Zeong (mobile armor);
          Silver Bullet Suppressor; Unicorn / Banshee (legado).
        - Metis (colonia); Banchi 18 (setor/endereco em Metis); Helium-3 / Minovsky overconcentration.

        === Regras de traducao ===
        - Narrative / Narrative Gundam / Phenex / Operation Phoenix Hunt / Laplace's Box /
          Laplace Incident / Universal Century / U.C. — oficiais.
        - Comunicacao militar curta ("Shezarr 001 to all units", "scatter", "no eyes on target").
        - Tom: drama militar-espiritual, melancolico; Jona ferido; Michele calculista/culpada;
          Zoltan teatral/cruel; Rita eterea.
        - Genero: Jona/Zoltan/Iago/Brick/Banagher/Monaghan/Fransson/Luio Woomin = m |
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
     * PROPÓSITO DE NEGÓCIO: protege identificadores confirmados nas legendas do BD NT.
     * <p>INVARIANTES DO DOMÍNIO: só termos que aparecem no artefato / cânone NT-Unicorn.
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Jona Basta", "Michele Luio", "Rita Bernal",
            "Zoltan Akkanen", "Luio Woomin", "Stephanie Luio",
            "Banagher Links", "Mineva Lao Zabi", "Iago Haakana",
            "Monaghan Bakharo", "Narrative Gundam", "Phenex",
            "Sinanju Stein", "II Neo Zeong", "Silver Bullet Suppressor",
            "Unicorn", "Banshee", "Earth Federation",
            "Shezarr", "Republic of Zeon", "Sleeves",
            "Miracle Children", "Metis", "Banchi 18", "Fransson", "Operation Phoenix Hunt",
            "Laplace's Box", "Universal Century", "Newtype",
            "Cyber-Newtype", "Oldtype", "Spacenoid",
            "Earthnoid", "Minovsky", "Psycho-Frame",
            "psycommu", "funnel", "NT-D",
            "Mega Particle Cannon", "Mobile Suit", "Mobile Armor"
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
            Map.entry("Moldura Psiquica", "Psycho-Frame")
        ));
    }
}
