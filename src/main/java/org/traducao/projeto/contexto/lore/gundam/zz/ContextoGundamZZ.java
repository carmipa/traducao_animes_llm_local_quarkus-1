package org.traducao.projeto.contexto.lore.gundam.zz;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore de Mobile Suit Gundam ZZ (UC 0088 / Neo Zeon).
 *
 * <p>INVARIANTES DO DOMÍNIO: Judau Ashta; Haman Karn; Glemy Toto; ZZ Gundam;
 * Axis (NUNCA "Eixo"); Titans (NUNCA "Titãs"); Ple (NUNCA "Plê"); Shangri-La.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa de correção imutáveis.
 */
@Component
public class ContextoGundamZZ implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam ZZ — Universal Century 0088, pós-Gryps Conflict; ascensão de Neo Zeon / Axis.
        - Continuidade: sequela direta de Zeta Gundam; Bright Noa e a Argama; Judau Ashta entra na guerra.
        - Facções: AEUG, Neo Zeon (Axis — NUNCA "Eixo"), Earth Federation, Karaba, Glemy Faction (cisão interna).
          Titans (mencoes historicas — NUNCA "Titãs").
        - Personagens (gênero): Judau Ashta (m) — informal, protetor, matura ao longo da série;
          Leina Ashta (f); Roux Louka (f); Elle Vianno (f); Beecha Oleg (m); Mondo Agake (m);
          Iino Abbav (m); Haman Karn (f) — fria/imperial; Mashymre Cello (m) — teatral;
          Chara Soon (f); Glemy Toto (m); Bright Noa (m); Fa Yuiry (f); Kamille Bidan (m — aparições);
          Wong Lee (m); Elpeo Ple (f — NUNCA "Plê"); Ple Two (f); Rasara Moon / Sarasa Moon conforme créditos.
        - Lugares/naves: Shangri-La (colônia), Argama, Nahel Argama, Axis, Core 3, Dublin, Moon Moon.
        - Mobile suits: MSZ-010 ZZ Gundam, MSZ-006 Zeta Gundam, RX-178 Gundam Mk-II,
          AMX-004 Qubeley, Qubeley Mk-II, AMX-103 Hamma Hamma, R-Jarja, Dreissen, Bawoo,
          Zaku III, NZ-000 Queen Mansa / Quin Mantha.
        - Termos UC: Newtype (NUNCA "Novo Tipo"), Cyber-Newtype, Oldtype; Mobile Suit vs Mobile Armor
          (Quin Mantha / Queen Mansa como MA quando a obra designar); Minovsky; Spacenoid;
          Axis, Neo Zeon, beam rifle, beam saber. Manter Newtype / mobile suit.
        - Regras: Judau/Haman/Glemy/Mashymre/Roux/Ple grafias oficiais; ZZ Gundam — preferir "ZZ Gundam";
          Shangri-La como nome de colonia.
        - Tom: inicio aventureiro/comico → guerra politica e tragedia; Judau de rua para piloto;
          Haman autoritaria; Glemy ambicioso.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam ZZ", LORE);

    @Override
    public String getId() {
        return "gundam_zz";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam ZZ";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege elenco e unidades canônicas de ZZ.
     * <p>INVARIANTES DO DOMÍNIO: só artefatos da série ZZ / UC 0088.
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Judau Ashta", "Leina Ashta", "Haman Karn", "Roux Louka", "Glemy Toto",
            "Mashymre Cello", "Chara Soon", "Bright Noa", "Elpeo Ple", "Ple Two",
            "ZZ Gundam", "Qubeley", "Shangri-La", "Argama", "Neo Zeon", "Axis", "Titans",
            "Newtype", "Cyber-Newtype", "Oldtype", "Minovsky", "Mobile Suit", "Mobile Armor",
            "Spacenoid"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: restaura Axis/Titans/Newtype/Ple quando o LLM localiza indevidamente.
     *
     * <p>INVARIANTES DO DOMÍNIO: mapa de {@link CorrecoesTerminologiaGundamZz}; só aplica
     * com canônico presente no original EN.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamZz.mapa();
    }
}
