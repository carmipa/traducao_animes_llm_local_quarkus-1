package org.traducao.projeto.traducaoKaraoke.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.llm.domain.LlmPort;
import org.traducao.projeto.llm.domain.Lote;
import org.traducao.projeto.llm.domain.StatusLlm;
import org.traducao.projeto.llm.domain.TraducaoLote;
import org.traducao.projeto.qualidadeTraducao.application.LoreAtivaFake;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova, pelo caminho REAL de envio ao LLM, que a letra de dois versos
 * volta com os dois (F5, 24/09/2026). A resposta do dublê reproduz a saída crua medida no aya:
 * o {@code \N} do verso vira quebra de linha real e o adaptador entrega DUAS linhas.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Se o tradutor voltar a pegar só a primeira linha, o segundo verso some e este teste reprova
 * nomeando o verso perdido.
 */
class TradutorDeLetraPerdeVersoTest {

    private static final String N = "\\N";

    /** Devolve as linhas fixas, como o adaptador faz ao quebrar a resposta por linha. */
    private static final class LlmQueQuebraEmLinhas implements LlmPort {
        private final List<String> linhas;

        LlmQueQuebraEmLinhas(List<String> linhas) {
            this.linhas = linhas;
        }

        @Override
        public TraducaoLote traduzir(Lote lote) {
            return new TraducaoLote(lote.idLote(), linhas, true, null);
        }

        @Override
        public StatusLlm verificarDisponibilidade() {
            return new StatusLlm(true, true, "teste");
        }

        @Override
        public Optional<String> revisarConcordancia(String original, String traducao, List<String> problemas) {
            return Optional.empty();
        }

        @Override
        public Optional<String> corrigirTraducao(String original, String traducao, String motivo) {
            return Optional.empty();
        }
    }

    private static TradutorDeLetraKaraoke tradutor(LlmPort llm) {
        TradutorDeLetraKaraoke t = new TradutorDeLetraKaraoke();
        t.llmPort = llm;
        t.mascarador = new MascaradorTags();
        t.validador = new ValidadorTraducaoService(LoreAtivaFake.vazia());
        t.telemetriaService = new TraduzirKaraokeUseCaseTest.MockTelemetria();
        t.logStream = new TraduzirKaraokeUseCaseTest.MockLogStream();
        return t;
    }

    @Test
    @DisplayName("letra de dois versos com moldura: os DOIS versos voltam, unidos por \\N, na moldura original")
    void doisVersosVoltamOsDois() {
        String original = "{\\fad(200,200)\\blur5}We had paced back and forth all that time" + N
            + "You do never know that love I felt" + N;
        List<String> avisos = new ArrayList<>();
        String traduzido = tradutor(new LlmQueQuebraEmLinhas(List.of(
            "Passei todo esse tempo andando para lá e para cá. \\",
            "Você nunca sabe o amor que eu senti.")))
            .traduzirViaLlm(original, avisos, new AtomicInteger(), "prompt");

        assertEquals("{\\fad(200,200)\\blur5}Passei todo esse tempo andando para lá e para cá." + N
            + "Você nunca sabe o amor que eu senti.", traduzido,
            () -> "o segundo verso ('Você nunca sabe...') tinha de voltar, sem barra solta. Avisos: " + avisos);
    }

    @Test
    @DisplayName("A1: letra de UM verso com resposta de UMA linha passa como sempre passou")
    void umVersoUmaLinha() {
        String traduzido = tradutor(new LlmQueQuebraEmLinhas(List.of("Você se sente sozinho?")))
            .traduzirViaLlm("{\\fad(200,200)}Do you feel alone", new ArrayList<>(), new AtomicInteger(), "prompt");
        assertEquals("{\\fad(200,200)}Você se sente sozinho?", traduzido);
    }

    @Test
    @DisplayName("resposta com linha A MAIS (comentario do modelo) e recusada: a letra fica no original, com aviso")
    void linhaAMaisERecusada() {
        List<String> avisos = new ArrayList<>();
        String traduzido = tradutor(new LlmQueQuebraEmLinhas(List.of("Você se sente sozinho?", "Observação: tradução livre.")))
            .traduzirViaLlm("{\\fad(200,200)}Do you feel alone", avisos, new AtomicInteger(), "prompt");
        assertNull(traduzido, "duas linhas para um verso nao podem virar legenda");
        assertTrue(avisos.stream().anyMatch(a -> a.contains("2 linha(s) para 1 verso")),
            () -> "a recusa tem de ficar registrada com as duas contagens (A7): " + avisos);
    }
}
