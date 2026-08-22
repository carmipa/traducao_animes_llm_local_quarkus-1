package org.traducao.projeto.raspagemRevisao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.domain.EventoLegenda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: separar o que a revisão CONSERTOU do que ela apenas LIMPOU. São os dois
 * números que o operador lê no fim da corrida, e eles respondem perguntas diferentes.
 *
 * <h2>A cicatriz, de 22/08/2026 — no mesmo dia em que a regra do itálico nasceu</h2>
 * A remoção de itálico chamava {@code contarCorrecaoJaAplicada()} para marcar o arquivo como
 * modificado, e com isso entrava em {@code corrigidas}. Na primeira corrida real, no Guilty
 * Crown, o resumo saiu assim:
 * <pre>
 * Falas com problemas detectados: 3
 * Falas corrigidas via LLM e salvas: 412
 * </pre>
 * As 412 eram itálico, uma a uma — a soma dos {@code [ITALICO]} dos 23 arquivos dá exatamente
 * 412. O LLM não corrigiu NENHUMA fala. Paulo leu o número e perguntou se a tradução tinha sido
 * arrancada da tela; foi o número que mentiu, não a tela que quebrou.
 *
 * <p>É a regra da medição aplicada ao próprio painel: número que engana é pior que número
 * nenhum, porque o operador AGE sobre ele.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code corrigidas} conta só conserto de TRADUÇÃO (LLM, Google, determinístico).</li>
 *   <li>{@code italicoRemovido} conta limpeza de formatação e nada mais.</li>
 *   <li>Os dois marcam o arquivo como modificado — a gravação depende de qualquer um deles.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Se {@code contarItalicoRemovido} voltar a somar em {@code corrigidas}, este teste reprova
 * antes de o número enganado chegar à tela.
 */
class SessaoRevisaoArquivoTest {

    private static EventoLegenda fala(int indice, String texto) {
        return new EventoLegenda(indice, "Dialogue", "Default", "prefixo,", texto);
    }

    @Test
    @DisplayName("italico removido marca o arquivo, mas NAO conta como fala corrigida")
    void italicoRemovidoNaoInflaOContadorDeCorrecao() {
        SessaoRevisaoArquivo sessao = new SessaoRevisaoArquivo();
        assertFalse(sessao.modificado(), "sessao nova nao tem o que gravar");

        sessao.contarItalicoRemovido();
        sessao.contarItalicoRemovido();

        assertEquals(2, sessao.italicoRemovido(), "o contador proprio tem de subir");
        assertEquals(0, sessao.corrigidas(),
            "itálico NÃO é conserto de tradução: somar aqui faz o relatório dizer "
                + "\"Falas corrigidas via LLM: 412\" quando o LLM não corrigiu nenhuma");
        assertTrue(sessao.modificado(), "o arquivo precisa ser gravado mesmo assim");
    }

    /**
     * CONTRA-CASO: sem ele, "corrigidas == 0" passaria como se fosse a regra, e um bug que
     * zerasse o contador de correção de verdade não seria notado.
     */
    @Test
    @DisplayName("conserto de traducao continua contando como fala corrigida")
    void consertoDeTraducaoContinuaContando() {
        SessaoRevisaoArquivo sessao = new SessaoRevisaoArquivo();

        sessao.corrigir(fala(1, "Help!"), "Ajude!");
        sessao.contarCorrecaoJaAplicada();

        assertEquals(2, sessao.corrigidas(), "as duas formas de conserto contam em corrigidas");
        assertEquals(0, sessao.italicoRemovido(), "e nenhuma delas mexe no contador de itálico");
        assertTrue(sessao.modificado());
    }

    /**
     * Os dois juntos, que é o caso do acervo real: o arquivo tem itálico E uma fala consertada.
     * Cada número responde à sua pergunta, sem contaminar o outro.
     */
    @Test
    @DisplayName("os dois no mesmo arquivo nao se misturam")
    void osDoisContadoresConvivemSemSeMisturar() {
        SessaoRevisaoArquivo sessao = new SessaoRevisaoArquivo();

        sessao.contarItalicoRemovido();
        sessao.corrigir(fala(1, "Help!"), "Ajude!");
        sessao.contarItalicoRemovido();
        sessao.contarItalicoRemovido();

        assertEquals(1, sessao.corrigidas(), "uma fala consertada");
        assertEquals(3, sessao.italicoRemovido(), "três itálicos removidos");
    }
}
