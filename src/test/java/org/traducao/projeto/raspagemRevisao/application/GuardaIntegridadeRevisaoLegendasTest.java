package org.traducao.projeto.raspagemRevisao.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: trava a guarda de integridade da Revisão de Legendas — a ferramenta que
 * corrige o PT-BR direto no arquivo, sem inglês nem cache. Uma correção NUNCA pode apagar uma
 * fala; se apagasse, a legenda sumiria da tela mantendo tempo e estilo.
 *
 * <h2>Caso real (2026-07-25, Guilty Crown ep4)</h2>
 * Um bloco contínuo de diálogo entre 13:58 e 20:52 saiu com texto vazio — as linhas mantiveram
 * Layer, tempo e estilo {@code Default}, mas o campo de texto ficou em branco (ex.: a fala
 * {@code "Gai, eu localizei a Inori."} desapareceu). O saneamento de tags de karaokê aplicava o
 * resultado só por ele "ter mudado", sem checar se tinha esvaziado a fala.
 *
 * <h2>Por que a guarda deixou de ser estática</h2>
 * A noção de "texto que o espectador lê" existia em QUATRO cópias no projeto, e a desta fatia era
 * a divergente: não convertia a quebra {@code \h} nem a {@code \n} minúscula em espaço, e aparava
 * com {@code trim()} em vez de {@code strip()}. A FASE 4 apagou a cópia e passou a consumir o peer
 * {@code qualidadeTraducao}, que já publicava a definição. Medido antes de trocar: em 34.002 falas
 * do acervo real, os dois critérios discordam sobre "tem texto visível" em ZERO casos — o
 * {@code \h} aparece em 61 falas, sempre acompanhado de texto.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Se a guarda parar de acusar o esvaziamento, uma correção volta a poder destruir legendas.
 */
class GuardaIntegridadeRevisaoLegendasTest {

    /**
     * A guarda só precisa do peer que sabe extrair o texto visível; as demais dependências do caso
     * de uso não participam dela e ficam nulas de propósito — se alguma passar a ser exigida, este
     * teste falha alto em vez de mascarar o acoplamento novo.
     */
    private final RevisarLegendasUseCase guarda = new RevisarLegendasUseCase(
        null, null, null, null, null, null, null, null, null, null, null, null,
        new ProtecaoLegendaAssService(),  // o unico colaborador que a guarda usa
        null, null, null, null, null);

    @Test
    void acusaQuandoACorrecaoApagaUmaFalaComTexto() {
        assertTrue(guarda.saneamentoEsvaziariaFala("Gai, eu localizei a Inori.", ""),
            "corrigir para vazio apaga a fala");
        assertTrue(guarda.saneamentoEsvaziariaFala("{\\i1}Aguarda, eu chego.{\\i0}", "{\\i1}{\\i0}"),
            "sobrar só tags, sem texto visível, também é apagar a fala");
        assertTrue(guarda.saneamentoEsvaziariaFala("Certo, pessoal.", "   "),
            "só espaços é vazio visível");
    }

    @Test
    void permiteReescritaQueMantemTexto() {
        assertFalse(guarda.saneamentoEsvaziariaFala("Minha pai precisa disso!", "Meu pai precisa disso!"),
            "correção legítima de concordância continua permitida");
        assertFalse(guarda.saneamentoEsvaziariaFala("{\\an8}Roger.", "{\\an8}Entendido."),
            "troca de palavra preservando tag é permitida");
        assertFalse(guarda.saneamentoEsvaziariaFala("Gai—", "Gai..."),
            "só pontuação, mas ainda há texto");
    }

    @Test
    void naoAcusaQuandoOOriginalJaEraVazio() {
        assertFalse(guarda.saneamentoEsvaziariaFala("", ""),
            "não há fala a proteger");
        assertFalse(guarda.saneamentoEsvaziariaFala("{\\pos(1,2)}", ""),
            "linha só de tags (sem texto visível) não tinha o que proteger");
        assertFalse(guarda.saneamentoEsvaziariaFala(null, null), "nulos degradam para vazio");
    }

    /**
     * A guarda é CONSERVADORA: qualquer conteúdo que sobre após remover tags {@code {...}} conta
     * como fala a proteger — inclusive um desenho vetorial {@code \p}, cujos comandos ficam fora
     * das chaves. Preferimos preservar um desenho a mais do que arriscar apagar diálogo.
     */
    @Test
    void protegeAteConteudoDeDesenhoVetorial() {
        assertTrue(guarda.saneamentoEsvaziariaFala("{\\p1}m 0 0 l 10 10", ""),
            "os comandos de desenho ficam fora das chaves e contam como conteúdo — melhor preservar");
    }
}
