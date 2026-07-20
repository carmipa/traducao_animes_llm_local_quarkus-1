package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore completa para The 08th MS Team (UC 0079) —
 * Opção 7 alinhada à Tradução Local, sem importar {@code contexto.lore}.
 *
 * <p>INVARIANTES DO DOMÍNIO: 08th MS Team ≠ 8o Time MS; Apsalus ≠ Absalão; Gouf Custom;
 * Gundam Ez8; Miller's Report; Apsalus é Mobile Armor.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreGundam08thMSTeam implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam: The 08th MS Team, Universal Century U.C. 0079 (One Year War).
        - Regra: corrigir APENAS nomenclatura. Apsalus e Mobile Armor — nao Mobile Suit.

        === Roster — 08th / Federacao ===
        - Shiro Amada; Karen Joshua; Terry Sanders Jr.; Eledore Massis; Michel Ninorich;
          Kojima; Isan Ryer; Alice Miller; Yuri Kellarny; B.B. (cartas de Michel).

        === Roster — Zeon / civis ===
        - Aina Sahalin; Ginias Sahalin; Norris Packard; Kiki Rosita; Baresto Rosita.

        === Orgs / lugares ===
        - 08th MS Team (NUNCA "8o Time MS" / "Oitava Equipe MS" como titulo);
          Kojima Battalion; Earth Federation; Principality of Zeon / Zeon; Jaburo;
          Miller's Report (epilogo).

        === Mecha ===
        - Gundam Ground Type / Ground Gundam; Gundam Ez8; GM Ground Type;
          Zaku II Ground Type; Gouf Custom; Gouf Flight Type; Zaku Tank; Magella Attack;
          Apsalus I / II / III; Hovertruck.

        === Termos UC / formas-ruim ===
        - Newtype; Mobile Suit / Mobile Armor; One Year War; Minovsky; Mega Particle Cannon.
        - Absalão → Apsalus; Gouf Personalizado → Gouf Custom;
          Gundam Tipo Terrestre → Gundam Ground Type; Guerra de Um Ano → One Year War;
          Relatório Miller → Miller's Report; Principado de Zeon → Principality of Zeon;
          Caminhão Hover → Hovertruck.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override
    public String getId() {
        return "gundam_08ms";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam: The 08th MS Team - Revisao de Lore";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo UC + extras 08th na Opção 7 (espelho da Tradução Local).
     *
     * <p>INVARIANTES DO DOMÍNIO: sem import cruzado de {@code contexto.lore}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUcRevisao.comExtras(Map.ofEntries(
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
