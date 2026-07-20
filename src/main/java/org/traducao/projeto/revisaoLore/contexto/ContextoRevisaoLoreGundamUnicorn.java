package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore completa para Gundam Unicorn (UC 0096).
 *
 * <p>INVARIANTES DO DOMÍNIO: Unicorn Gundam ≠ Gundam Unicórnio; Sleeves ≠ Mangas;
 * Laplace's Box; Full Frontal; Psycho-Frame; NT-D; Phenex é de NT.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreGundamUnicorn implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam Unicorn / Unicorn RE:0096, U.C. 0096.
        - Papel: corrigir APENAS nomenclatura. Phenex e de NT — nao forcar aqui.

        === Protagonistas / Sleeves ===
        - Banagher Links; Mineva Lao Zabi / Audrey Burne;
          Full Frontal nao vira "Frontal Completo"; Marida Cruz; Suberoa Zinnerman;
          Angelo Sauper; Gilboa Sant; Tikva Sant; Flaste Schole; Aaron Terzieff.

        === Federacao / Londo Bell / ECOAS ===
        - Riddhe Marcenas; Otto Midas; Daguza Mackle; Conroy Haagensen; Nigel Garrett;
          Hill Dawson; Liam Borrinea; Mihiro Oiwakken; Bright Noa / Ra Cailum.

        === Vist / civis ===
        - Syam Vist; Cardeas Vist; Alberto Vist; Martha Vist Carbine;
          Takuya Irei; Micott Bartsch; Loni Garvey.

        === Orgs / naves / lugares ===
        - Sleeves (NUNCA Mangas); Neo Zeon; Vist Foundation; Anaheim Electronics;
          Londo Bell; ECOAS; Earth Federation.
        - Nahel Argama; Garencieres; Ra Cailum; Rewloola; Musaka; Magallanica;
          Industrial 7; Palau; Torrington Base; Dakar; Laplace's Box / Laplace Incident.

        === Mecha ===
        - Unicorn Gundam (NUNCA Gundam Unicornio); Banshee / Banshee Norn; Sinanju;
          Kshatriya; Delta Plus; ReZEL; Jegan; Geara Zulu; Rozen Zulu; Shamblo;
          Byarlant Custom; Stark Jegan; Anksha.

        === Termos UC / formas-ruim ===
        - Newtype; Cyber-Newtype; Psycho-Frame; NT-D; Destroy Mode; Minovsky;
          Mobile Suit / Mobile Armor; Axis (NUNCA Eixo).
        - Mangas → Sleeves; Caixa de Laplace → Laplace's Box;
          Incidente de Laplace → Laplace Incident; Fundação Vist → Vist Foundation;
          Gundam Unicórnio → Unicorn Gundam; Frontal Completo → Full Frontal;
          Modo Destruição → Destroy Mode; Moldura Psíquica → Psycho-Frame;
          Novo Tipo → Newtype.
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
     * <p>INVARIANTES DO DOMÍNIO: espelho da Tradução Local (sem import cruzado).
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
