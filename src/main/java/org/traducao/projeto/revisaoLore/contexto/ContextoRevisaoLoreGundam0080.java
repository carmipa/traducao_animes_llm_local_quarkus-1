package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore completa para Gundam 0080: War in the Pocket —
 * Opção 7 alinhada à Tradução Local, sem importar {@code contexto.lore}.
 *
 * <p>INVARIANTES DO DOMÍNIO: War in the Pocket ≠ Guerra no Bolso; Gundam Alex ≠ Alexandre;
 * Kampfer; Cyclops Team; Al menino; Chris mulher.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreGundam0080 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam 0080: War in the Pocket, Universal Century U.C. 0079/0080.
        - Regra: corrigir APENAS nomenclatura. Tom anti-guerra / tragedia infantil.

        === Roster ===
        - Alfred "Al" Izuruha (m, menino); Bernard "Bernie" Wiseman (m);
          Christina "Chris" Mackenzie (f); Mikhail "Misha" Kaminsky (m);
          Hardy Steiner (m); Gabriel Ramirez Garcia (m); Andy Strauss (m);
          Colonel Killing (m); Chay (m); Telcott (m).

        === Orgs / locais ===
        - Earth Federation; Principality of Zeon / Zeon;
          Cyclops Team; Zeon Special Forces;
          Side 6; Republic of Riah; Libot colony; Antarctic Base.

        === Mecha ===
        - Gundam Alex (NUNCA Gundam Alexandre); Zaku II Kai; Kampfer;
          Hygogg; Z'Gok-E; GM Cold Districts Type; GM Sniper II.

        === Termos UC / formas-ruim ===
        - Newtype; Mobile Suit / Mobile Armor; One Year War; War in the Pocket.
        - War in the Pocket nao deve virar "Guerra no Bolso" quando for titulo;
          Gundam Alex nao vira "Alexandre"; Kampfer/Hygogg/Z'Gok-E grafia oficial.
        - Guerra no Bolso → War in the Pocket; Gundam Alexandre → Gundam Alex;
          Equipe Cyclops → Cyclops Team; República de Riah → Republic of Riah;
          Base Antártica → Antarctic Base; Guerra de Um Ano → One Year War;
          Principado de Zeon → Principality of Zeon.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override
    public String getId() {
        return "gundam_0080";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam 0080: War in the Pocket - Revisao de Lore";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo UC + extras 0080 na Opção 7 (espelho da Tradução Local).
     *
     * <p>INVARIANTES DO DOMÍNIO: sem import cruzado de {@code contexto.lore}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUcRevisao.comExtras(Map.ofEntries(
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
