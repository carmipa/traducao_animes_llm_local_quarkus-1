package org.traducao.projeto.qualidadeTraducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * PROPÓSITO DE NEGÓCIO: sela os DOIS falsos positivos que destruíram tradução correta no cache,
 * medidos em 2026-07-28 durante a operação manual de Guilty Crown e Zeta Gundam.
 *
 * <h2>O que aconteceu</h2>
 * A limpeza de cache classifica cada entrada com {@code ValidadorTraducaoService.validarFala} e
 * ESVAZIA o que ela reprova. Duas regras reprovaram tradução boa:
 * <ul>
 *   <li><b>Resíduo em inglês</b> — quatro falas de Zeta apagadas por conterem {@code "The O"},
 *       o mobile suit do Scirocco. {@code "the"} está na lista de palavras-resíduo, e a regra,
 *       olhando só a saída, não distinguia o artigo do nome próprio. O termo JÁ estava em
 *       {@code termosProtegidos()} da obra — a informação existia e não chegava ao validador.</li>
 *   <li><b>Preâmbulo</b> — {@code "{\be3}Aqui está a ordem."} apagada, tradução correta de
 *       {@code "{\be3}Here's the order."}. A alternativa {@code ^aqui está a} do padrão era
 *       solta e casava com abertura comum de diálogo em português.</li>
 * </ul>
 * Nos dois casos o dano entrava em LAÇO: o passo do Google repunha a fala e a limpeza seguinte
 * apagava de novo, com risco de a fala ficar vazia quando o Google recusasse.
 *
 * <h2>Invariantes do domínio</h2>
 * O conserto não pode enfraquecer a detecção: resíduo real e preâmbulo real continuam
 * reprovando, e sem contexto ativo o validador mantém o comportamento histórico. Por isso cada
 * caso que passa a ser aceito vem acompanhado do caso vizinho que deve continuar reprovando.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: os métodos do validador lançam
 * {@link AlucinacaoDetectadaException}; o chamador preserva a tradução anterior.
 */
class ValidadorNaoCondenaNomeDaObraTest {

    @Nested
    @DisplayName("Nome próprio da obra não é resíduo em inglês")
    class NomeProprioDaObra {

        /** As quatro falas reais que a limpeza de Zeta apagou em 2026-07-28. */
        @Test
        void nomeDeMechaDeclaradoNaLoreNaoEhResiduo() {
            var validador = new ValidadorTraducaoService(LoreAtivaFake.com("The O", "Qubeley"));

            assertDoesNotThrow(() -> validador.validarFala("The O, decolando!"));
            assertDoesNotThrow(() -> validador.validarFala("Nem The O nem o Qubeley se moveram!"));
            assertDoesNotThrow(() -> validador.validarFala("Desta vez, é The O!"));
            assertDoesNotThrow(() -> validador.validarFala("The O!"));
        }

        /**
         * O contraprova: sem a obra ativa, a MESMA fala continua reprovando. Prova que quem
         * absolve é a lore, e não um afrouxamento da regra.
         */
        @Test
        void semLoreAtivaOMesmoTextoContinuaReprovando() {
            var validador = new ValidadorTraducaoService(LoreAtivaFake.vazia());

            assertThrows(AlucinacaoDetectadaException.class,
                () -> validador.validarFala("The O, decolando!"));
        }

        /** Resíduo de verdade não pode ser absolvido por a obra ter termos protegidos. */
        @Test
        void residuoRealContinuaReprovandoMesmoComLoreAtiva() {
            var validador = new ValidadorTraducaoService(LoreAtivaFake.com("The O", "Qubeley"));

            assertThrows(AlucinacaoDetectadaException.class,
                () -> validador.validarFala("Ele vai transform quando chegar"));
            assertThrows(AlucinacaoDetectadaException.class,
                () -> validador.validarFala("Isso é exactly the same coisa"));
        }

        /**
         * Remover o termo protegido substitui por ESPAÇO, não por vazio: colar as vizinhas
         * poderia inventar uma palavra que a regra então acusaria.
         */
        @Test
        void remocaoDoTermoNaoColaAsPalavrasVizinhas() {
            var validador = new ValidadorTraducaoService(LoreAtivaFake.com("Void Genome"));

            assertDoesNotThrow(() -> validador.validarFala("Ela usou o Void Genome ontem"));
        }
    }

    @Nested
    @DisplayName("Preâmbulo exige vocabulário da tarefa")
    class Preambulo {

        /** A fala real do OVA de Guilty Crown que a limpeza apagou, e suas irmãs. */
        @Test
        void aberturaComumDeDialogoNaoEhPreambulo() {
            var validador = new ValidadorTraducaoService(LoreAtivaFake.vazia());

            assertDoesNotThrow(() -> validador.validarFala("{\\be3}Aqui está a ordem."));
            assertDoesNotThrow(() -> validador.validarFala("Aqui está a chave do quarto."));
            assertDoesNotThrow(() -> validador.validarFala("Aqui está o relatório que você pediu."));
        }

        /** Preâmbulo de verdade — o LLM falando DA TAREFA — continua reprovando. */
        @Test
        void preambuloSobreATarefaContinuaReprovando() {
            var validador = new ValidadorTraducaoService(LoreAtivaFake.vazia());

            assertThrows(AlucinacaoDetectadaException.class,
                () -> validador.validarFala("Aqui está a tradução: Atire nele!"));
            assertThrows(AlucinacaoDetectadaException.class,
                () -> validador.validarFala("Esta é a tradução solicitada."));
            assertThrows(AlucinacaoDetectadaException.class,
                () -> validador.validarFala("Tradução: Atire nele!"));
            assertThrows(AlucinacaoDetectadaException.class,
                () -> validador.validarFala("Resposta: Atire nele!"));
        }
    }
}
