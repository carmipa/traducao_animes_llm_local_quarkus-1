package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore excepcional para Gundam Unicorn.
 *
 * <p>INVARIANTES DO DOMÍNIO: Unicorn ≠ Unicórnio; Sleeves ≠ Mangas; Laplace's Box;
 * Full Frontal; Psycho-Frame; Newtype.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreGundamUnicorn implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam Unicorn / Unicorn RE:0096, U.C. 0096.
        - Regra: corrigir APENAS nomenclatura. Phenex e de NT — nao forcar aqui.

        === Personagens ===
        - Banagher Links, Audrey Burne / Mineva Lao Zabi,
          Full Frontal nao vira "Frontal Completo",
          Marida Cruz, Riddhe Marcenas, Angelo Sauper, Cardeas Vist, Alberto Vist, Syam Vist,
          Daguza Mackle, Suberoa Zinnerman, Otto Midas, Takuya Irei, Micott Bartsch, Loni Garvey.

        === Orgs / naves / mecha ===
        - Earth Federation; Vist Foundation; Anaheim Electronics; Sleeves (NUNCA Mangas);
          Neo Zeon; Londo Bell; ECOAS.
        - Nahel Argama; Garencieres; Industrial 7; Palau; Torrington; Laplace's Box.
        - Unicorn Gundam (NUNCA Gundam Unicornio); Banshee / Banshee Norn; Sinanju; Kshatriya;
          Delta Plus; ReZEL; Geara Zulu; Rozen Zulu; Shamblo.

        === Termos UC / formas-ruim ===
        - Newtype; Cyber-Newtype; Psycho-Frame; NT-D; Destroy Mode; Minovsky;
          Mobile Suit / Mobile Armor.
        - Mangas → Sleeves; Caixa de Laplace → Laplace's Box; Unicórnio → Unicorn;
          Moldura Psíquica → Psycho-Frame; Eixo → Axis; Novo Tipo → Newtype.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override
    public String getId() {
        return "gundam_unicorn";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam Unicorn - Revisao de Lore";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico Unicorn na Opção 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho da Tradução Local.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUcRevisao.comExtras(Map.ofEntries(
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
