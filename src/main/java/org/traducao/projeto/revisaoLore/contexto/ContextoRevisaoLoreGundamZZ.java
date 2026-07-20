package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore completa para Gundam ZZ / Double Zeta —
 * Opção 7 alinhada à Tradução Local, sem importar {@code contexto.lore}.
 *
 * <p>INVARIANTES DO DOMÍNIO: Axis ≠ Eixo; Titans ≠ Titãs; Ple ≠ Plê;
 * Quin Mantha ≠ Rainha Mansa; ZZ Gundam ≠ Zeta Duplo; Lady Haman; Newtype ≠ Novo Tipo.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreGundamZZ implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam ZZ / Double Zeta, U.C. 0088, Neo Zeon / Axis.
        - Regra: corrigir APENAS nomenclatura. Sequela direta de Zeta.

        === Roster — Shangri-La / Argama ===
        - Judau Ashta; Leina Ashta; Roux Louka; Elle Vianno; Beecha Oleg; Mondo Agake;
          Iino Abbav; Bright Noa; Fa Yuiry; Kamille Bidan; Wong Lee; Astonaige Medoz;
          Torres; Emary Ounce; Blue Corps.

        === Roster — Neo Zeon / Glemy ===
        - Haman Karn / Lady Haman (NUNCA Senhorita Haman quando for Lady Haman);
          Mashymre Cello; Chara Soon; Glemy Toto; Elpeo Ple (NUNCA Plê); Ple Two;
          Mineva Lao Zabi; Rakan Dahkaran; Gottn Goh; August Guidan; Illia Pazom.

        === Faccoes / naves / lugares ===
        - A.E.U.G. / AEUG; Neo Zeon; Axis (NUNCA Eixo); Titans (NUNCA Titãs);
          Earth Federation; Karaba; Anaheim Electronics; Glemy Faction; Blue Corps.
        - Argama; Nahel Argama; Endra; Sadalahn; Shangri-La; Axis; Core 3; Dublin; Moon Moon.

        === Mecha ===
        - ZZ Gundam (NUNCA Zeta Duplo); Double Zeta nao vira "Zeta Duplo";
          Full Armor ZZ; Zeta Gundam; Gundam Mk-II; Hyaku Shiki;
          Qubeley / Qubeley Mk-II; Quin Mantha (NUNCA Rainha Mansa — Quin Mantha nao vira "Rainha Mansa");
          Queen Mansa (variante EN); Hamma Hamma; R-Jarja; Dreissen; Bawoo; Zaku III;
          Gaza-C; Ga-Zowmn; Geymalk; Psycho Gundam Mk-II.

        === Formas-ruim ===
        - Axis nao vira "Eixo"; Lady Haman nao vira "Senhorita Haman";
          Eixo → Axis; Titãs → Titans; Plê → Ple; Rainha Mansa → Quin Mantha;
          Zeta Duplo → ZZ Gundam; Senhorita Haman → Lady Haman;
          Corpo Azul → Blue Corps; Cubely → Qubeley; Facção Glemy → Glemy Faction;
          Novo Tipo → Newtype.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override
    public String getId() {
        return "gundam_zz";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam ZZ - Revisao de Lore";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico ZZ na Opção 7 (espelho da Tradução Local).
     *
     * <p>INVARIANTES DO DOMÍNIO: sem import cruzado de {@code contexto.lore}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUcRevisao.comExtras(Map.ofEntries(
            Map.entry("Eixo", "Axis"),
            Map.entry("Titãs", "Titans"),
            Map.entry("Titas", "Titans"),
            Map.entry("Plê", "Ple"),
            Map.entry("Plee", "Ple"),
            Map.entry("Rainha Mansa", "Quin Mantha"),
            Map.entry("Zeta Duplo", "ZZ Gundam"),
            Map.entry("Senhorita Haman", "Lady Haman"),
            Map.entry("Corpo Azul", "Blue Corps"),
            Map.entry("Corpos Azuis", "Blue Corps"),
            Map.entry("Cubely", "Qubeley"),
            Map.entry("Qubelei", "Qubeley"),
            Map.entry("Neo Zéon", "Neo Zeon"),
            Map.entry("Neo Zeón", "Neo Zeon"),
            Map.entry("Facção Glemy", "Glemy Faction"),
            Map.entry("Faccao Glemy", "Glemy Faction")
        ));
    }
}
