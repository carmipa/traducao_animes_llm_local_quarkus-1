package org.traducao.projeto.qualidadeTraducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * PROPÓSITO DE NEGÓCIO: sela a captura de UMA assinatura específica do LLM local — o "Just"
 * enfático do inglês saindo como {@code only} (inglês) ou {@code Juste} (francês) no meio da fala
 * em português.
 *
 * <h2>A medição que justifica cada palavra</h2>
 * Varredura do acervo em 2026-07-28, <b>58.722 falas com tradução</b>:
 * <pre>
 *   "only"  : 33 falas      "juste" : 3 falas
 * </pre>
 * Sempre o mesmo gatilho — {@code Just} abrindo uma segunda oração, quase sempre depois de "!":
 * <pre>
 *   EN "All right! Just try and stop me!"        PT "Tudo bem!only tentem me impedir!"
 *   EN "Never mind the guidance! Just hit them!" PT "Não ligue para as instruções!only bati neles!"
 *   EN "Just have some. You need it."            PT "Juste um pouco. Você precisa disso."
 * </pre>
 * Em 13 delas o modelo larga a oração INTEIRA em inglês ({@code "only watch me!"}), então não é
 * troca de palavra: é abandono de oração. Nenhum falso positivo em 36 casos inspecionados.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Acrescentar palavra à régua de resíduo APAGA tradução — foi assim que 4 falas corretas
 *       com {@code "The O"} (o mobile suit do Scirocco) foram apagadas. Por isso cada caso de
 *       acusação aqui vem com o vizinho que precisa PASSAR.</li>
 *   <li>{@code juste} não colide com português: a fronteira de palavra impede que
 *       {@code ajuste} e {@code reajuste} casem, porque o "a" anterior é caractere de palavra.
 *       É a asserção que separa esta regra de um apagador de conjugações.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem esta captura, a fala defeituosa é GRAVADA no cache e sobrevive a toda reexecução, porque
 * cache aceito não volta para a fila.
 */
class ValidadorResiduoOnlyJusteTest {

    private final ValidadorTraducaoService validador =
        new ValidadorTraducaoService(LoreAtivaFake.vazia());

    @Test
    @DisplayName("\"only\" cru no meio da fala em português é resíduo")
    void onlyEhResiduo() {
        assertThrows(AlucinacaoDetectadaException.class,
            () -> validador.validarFala("Tudo bem!only tentem me impedir!"));
        assertThrows(AlucinacaoDetectadaException.class,
            () -> validador.validarFala("Não vou morrer!only watch me!"));
    }

    @Test
    @DisplayName("\"Juste\" é vazamento de francês, como bleu/toujours")
    void justeEhVazamentoDeFrances() {
        assertThrows(AlucinacaoDetectadaException.class,
            () -> validador.validarFala("Juste um pouco. Você precisa disso."));
        assertThrows(AlucinacaoDetectadaException.class,
            () -> validador.validarFala("Juste aguenta aí..."));
    }

    /**
     * O vizinho obrigatório. {@code ajuste} e {@code reajuste} são português corrente; se a regra
     * os pegasse, ela apagaria fala boa em todo o acervo — exatamente o dano do caso "The O".
     */
    @Test
    @DisplayName("\"ajuste\"/\"reajuste\" NÃO disparam a regra do francês")
    void conjugacaoPortuguesaNaoDispara() {
        assertDoesNotThrow(() -> validador.validarFala("Faça o ajuste fino do sensor."));
        assertDoesNotThrow(() -> validador.validarFala("O reajuste da mira ficou pronto."));
        assertDoesNotThrow(() -> validador.validarFala("Ajuste a rota agora!"));
    }

    /**
     * Fala em português sem resíduo nenhum tem de atravessar. Sem esta asserção, um validador que
     * recusasse TUDO passaria nos dois testes de acusação acima.
     */
    @Test
    @DisplayName("fala limpa em português continua passando")
    void falaLimpaPassa() {
        assertDoesNotThrow(() -> validador.validarFala("Tudo bem, tentem me impedir!"));
        assertDoesNotThrow(() -> validador.validarFala("Tenha um pouco. Você precisa disso."));
    }
}
