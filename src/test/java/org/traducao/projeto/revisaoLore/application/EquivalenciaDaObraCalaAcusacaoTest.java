package org.traducao.projeto.revisaoLore.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.revisaoLore.domain.ResultadoDeteccaoLore;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: a obra declara quais traduções PT-BR ela ACEITA, e a tela 3.2 para de
 * acusá-las — sem escrever nada na legenda.
 *
 * <h2>O prejuízo, medido nas duas temporadas do 86 em 17/08/2026</h2>
 * A tela fechou com <b>543 pendências</b> e quase nenhuma era defeito:
 * <pre>
 *   Republic  113x no EN  ->  "Republica" no PT      traducao correta
 *   Federacy   49x        ->  "Federacao" 48 de 49   traducao correta e consistente
 *   Empire     14x        ->  "Imperio"   14 de 14   traducao correta
 *   Reaper     17x        ->  "Ceifador"             EPITETO — decisao de Paulo de 15/08
 * </pre>
 * O único catálogo de equivalências era um mapa HARDCODED no detector, misturando Gundam, 86 e
 * Macross — segunda cópia da lore dentro do código, contra a decisão de 15/08/2026 de que lore é
 * dado num arquivo só.
 *
 * <p><b>Por que declarar, e não mapear:</b> pôr {@code Federação → Federacy} em
 * {@code correcoesTerminologia} ESCREVERIA ~90 falas, trocando português correto por inglês — que
 * é exatamente o erro do {@code Ceifador → Reaper}, revertido no mesmo dia depois de a fonte da
 * obra mostrar que eu tinha errado a categoria do termo.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Mapa vazio reproduz o comportamento anterior — o parâmetro é aditivo, não substitui o
 *       catálogo global.</li>
 *   <li>O contra-teste é obrigatório: a equivalência cala a acusação DAQUELE termo, e o detector
 *       tem de continuar acusando nome de verdade trocado. Sem ele, um detector cego passaria.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem diz qual lado quebrou e quantas pendências aquilo representa no acervo.
 */
class EquivalenciaDaObraCalaAcusacaoTest {

    private final DetectorTermosLoreService detector = new DetectorTermosLoreService();

    private static final Map<String, List<String>> DO_86 = Map.of(
        "reaper", List.of("ceifador"),
        "federacy", List.of("federacao", "federação"));

    /**
     * A fala precisa ter o termo FORA do inicio: nome de uma palavra abrindo a frase ja e
     * ignorado por outra regra (o ingles capitaliza por posicao), entao um exemplo assim passaria
     * verde sem exercitar a equivalencia. Descoberto quando o controle abaixo reprovou.
     */
    @Test
    @DisplayName("com a equivalencia declarada, 'Federacy' -> 'Federacao' deixa de ser acusado")
    void equivalenciaDeclaradaCalaAAcusacao() {
        ResultadoDeteccaoLore r = detector.auditar(
            "We joined the Federacy last year.", "Nos entramos na Federacao ano passado.", null, DO_86);

        assertFalse(r.suspeito(), () ->
            "traducao CORRETA sendo acusada mesmo com a obra declarando que a aceita. No 86 o "
                + "Federacy sozinho valia 30 acusacoes. Motivos: " + r.motivos());
    }

    @Test
    @DisplayName("CONTROLE: sem a declaracao, a MESMA fala continua sendo acusada")
    void semDeclaracaoAAcusacaoPermanece() {
        ResultadoDeteccaoLore r = detector.auditar(
            "We joined the Federacy last year.", "Nos entramos na Federacao ano passado.", null, Map.of());

        assertTrue(r.suspeito(),
            "sem este controle, o teste acima passaria com o detector cego — e o mapa vazio tem "
                + "de reproduzir o comportamento anterior, senao a mudanca vazou para obra que "
                + "nao declarou nada");
    }

    @Test
    @DisplayName("CONTRA-TESTE: equivalencia declarada NAO cala nome de verdade trocado")
    void equivalenciaNaoCalaNomeTrocado() {
        ResultadoDeteccaoLore r = detector.auditar(
            "Captain Nouzen is here.", "Capitao Cha esta aqui.", null, DO_86);

        assertTrue(r.suspeito(),
            "a equivalencia cala o termo DECLARADO, nao a regra inteira: 'Captain Nouzen' com o "
                + "sobrenome trocado tem de continuar acusando");
    }
}
