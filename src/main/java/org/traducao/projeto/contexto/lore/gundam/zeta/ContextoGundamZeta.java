package org.traducao.projeto.contexto.lore.gundam.zeta;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore de Zeta Gundam (Gryps Conflict / UC 0087).
 *
 * <p>INVARIANTES DO DOMÍNIO: Kamille Bidan (masculino); AEUG; Titans; Quattro Bajeena.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoGundamZeta implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Zeta Gundam, Universal Century 0087, Gryps Conflict.
        - Faccao/forcas: AEUG, Titans, Federacao Terrestre, Axis Zeon, Karaba, Anaheim Electronics.
        - Principais nomes: Kamille Bidan (m — apesar do nome soar feminino a alguns falantes; preserve masculino),
          Quattro Bajeena / Char Aznable (m), Amuro Ray (m), Bright Noa (m), Emma Sheen (f),
          Fa Yuiry (f), Reccoa Londe (f), Paptimus Scirocco (m), Haman Karn (f),
          Jerid Messa (m), Four Murasame (f), Katz Kobayashi (m), Wong Lee (m).
        - Naves/lugares: Argama, Alexandria, Gryps, Jaburo, Hong Kong, Dakar, Axis.
        - Mobile suits: Zeta Gundam, Gundam Mk-II, Hyaku Shiki, Rick Dias, Methuss, Psycho Gundam, The O, Qubeley, Marasai, Gaplant.
        - Termos UC: Newtype (NUNCA "Novo Tipo"), Cyber-Newtype, Oldtype; Mobile Suit vs Mobile Armor
          (Psycho Gundam / The O quando forem MA ou MS conforme designacao da obra);
          Minovsky particles; Spacenoid/Earthnoid se o dialogo trouxer; colony laser, beam rifle, beam saber.
        - Kamille Bidan e do sexo masculino; varios personagens o confundem com uma garota (piada recorrente). Preserve pronomes e tratamentos masculinos.
        - Tom: politica militar sombria, radicalizacao, trauma e abuso de autoridade; Kamille sensivel/reativo, Quattro estrategico, Titans autoritarios.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Zeta Gundam", LORE);

    @Override
    public String getId() {
        return "gundam_zeta";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Zeta Gundam";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Kamille Bidan", "Quattro Bajeena", "Char Aznable", "Amuro Ray", "Bright Noa",
            "Emma Sheen", "Haman Karn", "Paptimus Scirocco", "AEUG", "Titans",
            "Zeta Gundam", "Argama", "Newtype", "Cyber-Newtype", "Oldtype", "Minovsky",
            "Spacenoid", "Mobile Suit", "Mobile Armor"
        );
    }
}
