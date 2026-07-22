package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore completa para The 08th MS Team (UC 0079) —
 * Opção 7 alinhada à Tradução Local, sem importar {@code contexto.lore}, com roster amplo e
 * aliases da fansub EN (Joseki).
 *
 * <p>INVARIANTES DO DOMÍNIO: 08th MS Team ≠ 8o Time MS; Apsalus/Apsaras ≠ Absalão; Gouf Custom;
 * Gundam Ez8; Miller's Report; Apsalus/Apsaras é Mobile Armor; nomes/locais em inglês.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreGundam08thMSTeam implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam: The 08th MS Team, Universal Century U.C. 0079 (One Year War).
        - Regra: corrigir APENAS nomenclatura. Apsalus/Apsaras e Mobile Armor — nao Mobile Suit.
        - Nomes, locais, mecha e orgs ficam em INGLES. Preservar a grafia do EN da fala
          (Sahalin/Sakhalin, Ginias/Ginius, Apsalus/Apsaras, Massis/Mathis, Rosita/Logita,
          Kellerne/Kergeren/Kellarny, Nickard/Nickerd, Bone Abust/Von Abst).

        === Roster — 08th / Federation ===
        - Shiro Amada; Karen Joshua; Terry Sanders Jr.; Eledore Massis (Mathis);
          Michel Ninorich; Kojima; Isan Ryer; Jidan Nickard (Nickerd); Sally;
          Alice Miller; Agent Jacob; Rob; Pietro; Mike; B.B. (Michel's letters).

        === Roster — Zeon ===
        - Aina Sahalin (Sakhalin); Ginias Sahalin (Ginius); Norris Packard;
          Yuri Kellerne (Kergeren / Kellarny); Cynthia; Bone Abust (Von Abst);
          Topp; Arth; Dell; Masado; Barry; Niever; Runen/Lunen; Walter Janowitz;
          Nielba; Zhukov; Kergerenko; Gihren Zabi; Degwin; General Revil; Flanagan.

        === Roster — civilians / guerrillas ===
        - Kiki Rosita (Logita); Baresto Rosita; Maria; Chibi; Hige; Noppo.

        === Orgs / places / ops ===
        - 08th MS Team (NUNCA "8o Time MS" / "Oitava Equipe MS" as title);
          Kojima Battalion; Combined Mechanized Battalion; Far East Division;
          Earth Federation / Earth Federation Forces; Principality of Zeon / Zeon;
          Southeast Asia; Jaburo; Odessa; Point Bravo; Point Charlie; Burning Sand;
          Antarctic Treaty; Operation Star One; Miller's Report (epilogue).

        === Mecha / ships ===
        - Gundam Ground Type / Ground Gundam; Gundam Ez8; GM Ground Type; Guntank;
          Zaku II Ground Type; Gouf Custom; Gouf Flight Type; Zaku Tank; Magella Attack;
          Rick Dom; Ball; Apsalus I/II/III (= Apsaras I/II/III); Hovertruck;
          Dopp; Gaw; Medea; Musai; Komusai; Zanzibar; Kergeren.

        === Termos UC / formas-ruim ===
        - Newtype; Mobile Suit / Mobile Armor; One Year War; Minovsky; Mega Particle Cannon.
        - Absalão/Absaras → Apsalus; Gouf Personalizado → Gouf Custom;
          Gundam Tipo Terrestre → Gundam Ground Type; Guerra de Um Ano → One Year War;
          Relatório Miller → Miller's Report; Principado de Zeon → Principality of Zeon;
          Caminhão Hover → Hovertruck; Batalhão Kojima → Kojima Battalion;
          Tratado Antártico → Antarctic Treaty; Operação Star One → Operation Star One.
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
