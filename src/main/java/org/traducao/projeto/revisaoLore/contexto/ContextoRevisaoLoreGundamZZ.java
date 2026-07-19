package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore (Opção 7) para Gundam ZZ — nomes canônicos
 * e mapa determinístico alinhados à Tradução Local.
 *
 * <p>INVARIANTES DO DOMÍNIO: Axis ≠ Eixo; Titans ≠ Titãs; Newtype ≠ Novo Tipo;
 * Ple ≠ Plê; Quin Mantha ≠ Rainha Mansa; Double Zeta ≠ Zeta Duplo.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreGundamZZ implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam ZZ / Double Zeta, U.C. 0088, Neo Zeon / Axis.
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Personagens: Judau Ashta, Leina Ashta, Roux Louka, Elle Vianno, Beecha Oleg, Mondo Agake,
          Iino Abbav, Haman Karn, Lady Haman, Mashymre Cello, Chara Soon, Glemy Toto, Bright Noa,
          Fa Yuiry, Kamille Bidan, Wong Lee, Elpeo Ple (NUNCA Plê), Ple Two.
        - Faccoes: A.E.U.G., Neo Zeon, Axis (NUNCA Eixo), Titans (NUNCA Titãs), Earth Federation,
          Karaba, Anaheim Electronics, Blue Corps.
        - Naves/lugares: Argama, Nahel Argama, Shangri-La, Axis, Endra, Sadalahn, Gwanban, Dublin, Core 3.
        - Mecha: ZZ Gundam, Double Zeta, Zeta Gundam, Gundam Mk-II, Hyaku Shiki, Qubeley, Qubeley Mk-II,
          Quin Mantha (NUNCA Rainha Mansa), Hamma-Hamma, R-Jarja, Dreissen, Bawoo, Gaza-C, Gaza-D.
        - Termos: Newtype (NUNCA Novo Tipo), Cyber-Newtype, Psycommu, Minovsky, mobile suit, mobile armor.
        - Alertas: Double Zeta nao vira "Zeta Duplo"; Axis nao vira "Eixo"; Lady Haman nao vira "Senhorita Haman";
          Quin Mantha nao vira "Rainha Mansa"; Ple nao vira "Plê"; A.E.U.G. mantem pontos.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "gundam_zz"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam ZZ - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: mesmo mapa determinístico da tradução ZZ.
     *
     * <p>INVARIANTES DO DOMÍNIO: Eixo/Titãs/Novo Tipo/Plê → canônicos.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return Map.of(
            "Eixo", "Axis",
            "Titãs", "Titans",
            "Titas", "Titans",
            "Novo Tipo", "Newtype",
            "Plê", "Ple",
            "Plee", "Ple"
        );
    }
}
