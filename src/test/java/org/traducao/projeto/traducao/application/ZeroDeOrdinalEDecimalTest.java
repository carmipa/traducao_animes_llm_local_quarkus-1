package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * PROPÓSITO DE NEGÓCIO: prova as duas correções que a auditoria de 2026-09-09 pediu no
 * verificador numérico, uma em cada direção.
 *
 * <ul>
 *   <li>FALSO POSITIVO: {@code "04th Team!"} traduzido como {@code "4ª Equipe!"} era reprovado
 *       porque {@code 04} e {@code 4} eram sequências de dígitos diferentes. Reprovar aqui é
 *       caro: a fala inteira volta ao INGLÊS na legenda, e o acervo tem 27 falas expostas.
 *       <p>ADOÇÃO PARCIAL, E DELIBERADA: a auditoria também citou {@code "Equipe 4!"}, com o
 *       algarismo solto. Esse caso continua REPROVANDO, porque o projeto já havia decidido que
 *       perder o zero de {@code 08th} muda o NOME da unidade, e a lore confirma — o mapa de
 *       terminologia leva {@code "8º Time MS"} de volta para {@code "08th MS Team"}. A
 *       absolvição exige que a forma ORDINAL sobreviva nos DOIS lados, que é exatamente o
 *       critério de aceitação escrito pela própria auditoria: {@code 04th→4ª} passa,
 *       {@code 04th→08ª} falha.</li>
 *   <li>FALSO NEGATIVO: {@code 1.5} e {@code 15} produziam a MESMA sequência, porque o separador
 *       entre dígitos era sempre descartado como milhar. Trocar um pelo outro passava ileso.</li>
 * </ul>
 *
 * <p>INVARIANTES DO DOMÍNIO: zero à esquerda só é tolerado onde o ORIGINAL marca ordinal; grupo
 * de três dígitos continua sendo milhar. O caso que criou a guarda — {@code 04th} publicado como
 * {@code 08ª} — tem de continuar reprovando, e é o que os casos-controle fixam.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: {@code divergencia} devolve mensagem; {@code null} é passe.
 */
@DisplayName("Verificador numérico: zero de ordinal e decimal")
class ZeroDeOrdinalEDecimalTest {

    private final VerificadorIdentificadorNumerico verificador = new VerificadorIdentificadorNumerico();

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource(delimiter = '|', value = {
        "04th Team!                          | 4ª Equipe!",
        "08th MS Team                        | 8º Time MS",
        "The guys from the 07th Team!        | Os caras do 7º Time!",
        "We're going out to back up the 06th Team! | Vamos dar apoio à 6ª Equipe!",
        "4th Team, move out!                 | 04ª Equipe, avancem!",
    })
    @DisplayName("zero à esquerda de ordinal não é número trocado")
    void zeroDeOrdinalPassa(String original, String traduzido) {
        assertNull(verificador.divergencia(original, traduzido),
            "tradução correta reprovada: " + original + " -> " + traduzido);
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource(delimiter = '|', value = {
        // O defeito que criou a guarda: outro ordinal, não o mesmo sem zero.
        "04th Team!        | 08ª Equipe!",
        "08th MS Team      | 7º Time MS",
        // Sem marca de ordinal no original, o zero é código técnico e continua significativo.
        "Unit 04, respond! | Unidade 4, responda!",
        // A FORMA ORDINAL TEM DE SOBREVIVER DOS DOIS LADOS. "08th" virando o algarismo solto
        // "8" nao e re-grafia de ordinal: e a perda da designacao da unidade, que a lore deste
        // projeto trata como NOME (correcoesTerminologia mapeia "8o Time MS" -> "08th MS Team").
        // Decisao anterior, fixada em HorarioLocalizadoNaoEhNumeroTrocadoTest, e mantida.
        "Combined Battalion. 08th Mobile Suit Team. | Batalhão Combinado. 8 Time de Mobile Suit.",
        "04th Team!        | Equipe 4!",
        "Block 07 is down. | Bloco 7 caiu.",
    })
    @DisplayName("CASO-CONTROLE: troca de ordinal e código técnico continuam reprovando")
    void trocaDeOrdinalContinuaReprovando(String original, String traduzido) {
        assertNotNull(verificador.divergencia(original, traduzido),
            "defeito passou ileso: " + original + " -> " + traduzido);
    }

    @Test
    @DisplayName("decimal e inteiro deixam de ser o mesmo valor")
    void decimalNaoColapsaEmInteiro() {
        assertNotNull(verificador.divergencia("Altitude 1.5 km", "Altitude 15 km"),
            "1.5 virou 15 e passou: o separador decimal está sendo tratado como milhar");
    }

    @Test
    @DisplayName("grafia local do decimal continua sendo o mesmo valor")
    void pontoEVirgulaSaoOMesmoDecimal() {
        assertNull(verificador.divergencia("Altitude 1.5 km", "Altitude 1,5 km"));
        assertNull(verificador.divergencia("It's 3.14 exactly", "É 3,14 exatamente"));
    }

    @Test
    @DisplayName("CASO-CONTROLE: milhar continua colapsando, senão a correção quebra o que funcionava")
    void milharContinuaColapsando() {
        assertNull(verificador.divergencia("9.500 meters", "9500 metros"));
        assertNull(verificador.divergencia("9500 meters", "9.500 metros"));
        assertNull(verificador.divergencia("12.500 units", "12500 unidades"));
    }

    @Test
    @DisplayName("espaço agrupa milhar e nunca marca decimal")
    void espacoNaoEhDecimal() {
        // Preserva o comportamento medido em 2026-08-21: "Altitude 15 00" e "1500" são o mesmo
        // valor, porque espaço entre dígitos é separador de milhar em várias saídas de tradução.
        assertNull(verificador.divergencia("Altitude 15 00", "Altitude 1500"));
    }

    @Test
    @DisplayName("horário militar continua explicado, sem regressão da correção de 2026-08-21")
    void horarioMilitarSegueExplicado() {
        assertNull(verificador.divergencia("Attack at 1500 hours.", "Ataque às 15h00."));
    }
}
