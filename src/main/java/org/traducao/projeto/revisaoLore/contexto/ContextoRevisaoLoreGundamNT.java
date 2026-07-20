package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore completa para Gundam NT (Narrative) —
 * Opção 7 alinhada à Tradução Local, sem importar {@code contexto.lore}.
 *
 * <p>INVARIANTES DO DOMÍNIO: Phenex ≠ Fênix; Narrative ≠ Narrativo; Miracle Children;
 * Operation Phoenix Hunt; Psycho-Frame; Shezarr; Sleeves ≠ Mangas.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreGundamNT implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam NT (Narrative), U.C. 0097 — pos-Unicorn / Laplace Incident.
        - Regra: corrigir APENAS nomenclatura. Operation Phoenix Hunt caca o Phenex.

        === Roster ===
        - Jona Basta (m); Michele Luio (f — preferir Michele); Rita Bernal (f);
          Zoltan Akkanen (m); Iago Haakana (m); Brick Teclato (m); Fransson (m);
          Monaghan Bakharo (m); Luio Woomin (m); Stephanie Luio (f);
          Mineva Lao Zabi (f); Banagher Links (m).

        === Orgs / lugares ===
        - Earth Federation; Shezarr / Shezarr Team; Luio & Co.;
          Republic of Zeon; Sleeves (NUNCA Mangas);
          Miracle Children; Metis; Banchi 18.

        === Mecha ===
        - Narrative Gundam (NUNCA Gundam Narrativo); A-Packs / B-Packs / C-Packs;
          Phenex (NUNCA Fenís/Fênix como unidade); Sinanju Stein;
          II Neo Zeong (Mobile Armor); Silver Bullet Suppressor;
          Unicorn / Banshee (legado).

        === Termos UC / formas-ruim ===
        - Newtype; Cyber-Newtype; Oldtype; Psycho-Frame; NT-D; Minovsky; Spacenoid;
          Operation Phoenix Hunt; Laplace's Box; Laplace Incident;
          Mobile Suit / Mobile Armor.
        - Fenís/Fênix → Phenex; Operação Caça à Fênix → Operation Phoenix Hunt;
          Crianças Milagrosas → Miracle Children; Gundam Narrativo → Narrative Gundam;
          Mangas → Sleeves; Moldura Psíquica → Psycho-Frame;
          Caixa de Laplace → Laplace's Box; Equipe Shezarr → Shezarr Team;
          Supressor Silver Bullet → Silver Bullet Suppressor.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override
    public String getId() {
        return "gundam_nt";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam NT (Narrative) - Revisao de Lore";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico NT na Opção 7 (espelho da Tradução Local).
     *
     * <p>INVARIANTES DO DOMÍNIO: sem import cruzado de {@code contexto.lore}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUcRevisao.comExtras(Map.ofEntries(
            Map.entry("Fenís", "Phenex"),
            Map.entry("Fênix", "Phenex"),
            Map.entry("Fenix", "Phenex"),
            Map.entry("Operação Caça à Fênix", "Operation Phoenix Hunt"),
            Map.entry("Operacao Caca a Fenix", "Operation Phoenix Hunt"),
            Map.entry("Crianças Milagrosas", "Miracle Children"),
            Map.entry("Criancas Milagrosas", "Miracle Children"),
            Map.entry("Mangas", "Sleeves"),
            Map.entry("Manga", "Sleeves"),
            Map.entry("Moldura Psíquica", "Psycho-Frame"),
            Map.entry("Moldura Psiquica", "Psycho-Frame"),
            Map.entry("Gundam Narrativo", "Narrative Gundam"),
            Map.entry("Caixa de Laplace", "Laplace's Box"),
            Map.entry("Incidente de Laplace", "Laplace Incident"),
            Map.entry("Equipe Shezarr", "Shezarr Team"),
            Map.entry("Supressor Silver Bullet", "Silver Bullet Suppressor"),
            Map.entry("Silver Bullet Supressor", "Silver Bullet Suppressor")
        ));
    }
}
