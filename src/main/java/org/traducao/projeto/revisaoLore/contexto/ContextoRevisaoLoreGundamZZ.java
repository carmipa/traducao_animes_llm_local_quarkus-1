package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore excepcional para Gundam ZZ (Double Zeta).
 *
 * <p>INVARIANTES DO DOMÍNIO: Axis ≠ Eixo; Titans ≠ Titãs; Ple ≠ Plê;
 * Quin Mantha ≠ Rainha Mansa; ZZ Gundam ≠ Zeta Duplo; Newtype ≠ Novo Tipo.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreGundamZZ implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam ZZ / Double Zeta, U.C. 0088, Neo Zeon / Axis.
        - Regra: corrigir APENAS nomenclatura. Sequela direta de Zeta.

        === Personagens ===
        - Judau Ashta (m), Leina Ashta (f), Roux Louka (f), Elle Vianno (f),
          Beecha Oleg (m), Mondo Agake (m), Iino Abbav (m),
          Haman Karn / Lady Haman (f), Mashymre Cello (m), Chara Soon (f),
          Glemy Toto (m), Bright Noa (m), Fa Yuiry (f), Kamille Bidan (m),
          Wong Lee (m), Elpeo Ple (f — NUNCA Plê), Ple Two (f).

        === Faccoes / naves / lugares ===
        - A.E.U.G. / AEUG; Neo Zeon; Axis (NUNCA Eixo); Titans (NUNCA Titãs);
          Earth Federation; Karaba; Anaheim Electronics; Blue Corps; Glemy Faction.
        - Argama; Nahel Argama; Shangri-La; Axis; Endra; Sadalahn; Dublin; Core 3; Moon Moon.

        === Mecha ===
        - ZZ Gundam (NUNCA Zeta Duplo); Double Zeta nao vira "Zeta Duplo";
          Zeta Gundam; Gundam Mk-II; Hyaku Shiki;
          Qubeley / Qubeley Mk-II; Quin Mantha (NUNCA Rainha Mansa — Quin Mantha nao vira "Rainha Mansa");
          Hamma Hamma; R-Jarja; Dreissen; Bawoo; Zaku III.

        === Termos UC / formas-ruim ===
        - Newtype (NUNCA Novo Tipo); Cyber-Newtype; Oldtype; Psycommu; Minovsky;
          Mobile Suit / Mobile Armor.
        - Axis nao vira "Eixo"; Lady Haman nao vira "Senhorita Haman";
          Eixo → Axis; Titãs → Titans; Plê → Ple; Rainha Mansa → Quin Mantha;
          Zeta Duplo → ZZ Gundam; Novo Tipo → Newtype.
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
     * PROPÓSITO DE NEGÓCIO: mapa determinístico ZZ na Opção 7 (núcleo UC + extras ZZ).
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho da Tradução Local ZZ.
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
            Map.entry("Zeta Duplo", "ZZ Gundam")
        ));
    }
}
