package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.llm.domain.Lote;
import org.traducao.projeto.llm.domain.LlmPort;
import org.traducao.projeto.llm.domain.StatusLlm;
import org.traducao.projeto.llm.domain.TraducaoLote;
import org.traducao.projeto.qualidadeTraducao.application.LoreAtivaFake;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.traducao.domain.TelemetriaTraducao;
import org.traducao.projeto.traducao.domain.exceptions.TraducaoParcialException;
import org.traducao.projeto.traducao.domain.ports.TelemetriaTraducaoPort;
import org.traducao.projeto.traducao.presentation.ui.ConsoleUILogger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: separa os dois desfechos que hoje têm o MESMO sinal — o servidor
 * LLM estar fora do ar (o episódio inteiro deve parar) e o servidor RECUSAR uma fala
 * específica (só aquela fala deve ficar pendente).
 *
 * <h2>O prejuízo que originou</h2>
 * 2026-08-11, DanMachi Season 01 E03. Um estilo de karaokê batizado com o nome da canção
 * escapou dos detectores de música, a linha foi mascarada com um {@code [[TAGn]]} por LETRA,
 * o {@code towerinstruct-mistral-7b-v0.2} entrou em loop degenerado, e o LM Studio recusou o
 * próprio output do modelo com <b>HTTP 400</b>. Esgotadas as tentativas do adaptador, o
 * pipeline classificou como falha de comunicação — que é crítica — e <b>abortou o episódio
 * inteiro</b>: <b>373 falas de diálogo perdidas por causa de um verso de música</b>.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Recusa atribuível à REQUISIÇÃO (HTTP 4xx permanente) percorre o mesmo caminho de uma
 *       alucinação: retentativa com outra temperatura, segunda opinião, e por fim a fala
 *       segue pendente com o texto ORIGINAL preservado. Nunca derruba o episódio.</li>
 *   <li>Falha de INFRAESTRUTURA (timeout, 5xx, conexão recusada) continua abortando: insistir
 *       com o servidor fora do ar só gasta tempo, e a saída parcial é o desfecho correto.</li>
 *   <li>Recusa em série é infraestrutura disfarçada — modelo não carregado recusa TODAS as
 *       requisições. Ao atingir o limite de falas consecutivas recusadas, o episódio aborta
 *       com diagnóstico próprio, em vez de produzir um arquivo inteiro de pendências em
 *       silêncio.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Estes testes não tocam rede: o {@link LlmPort} é substituído por fakes que devolvem
 * exatamente o {@link TraducaoLote} que o adaptador real produziria em cada cenário.
 */
@DisplayName("recusa do LLM: fala patológica fica pendente; servidor fora do ar aborta")
class ProcessarEpisodioUseCaseRecusaDoLlmTest {

    private static final class TelemetriaFake implements TelemetriaTraducaoPort {
        final AtomicInteger rejeitadas = new AtomicInteger();
        final AtomicInteger recuperadas = new AtomicInteger();

        @Override public void registrarTraducao(TelemetriaTraducao telemetria) { }
        @Override public void registrarAlucinacaoPrevenida() { }
        @Override public void registrarRespostaTraducaoRejeitada() { rejeitadas.incrementAndGet(); }
        @Override public void registrarFalhaTraducaoRecuperada() { recuperadas.incrementAndGet(); }
        @Override public void registrarFallbackMantido() { }
    }

    /** Reproduz o adaptador diante de um HTTP 400: recusa desta requisição, servidor vivo. */
    private static final class LlmRecusaARequisicao implements LlmPort {
        final AtomicInteger chamadas = new AtomicInteger();
        @Override public TraducaoLote traduzir(Lote lote) {
            chamadas.incrementAndGet();
            return TraducaoLote.recusadaPeloServidor(lote.idLote(),
                "Erro ao traduzir o lote " + lote.idLote() + " apos 3 tentativa(s): HTTP 400");
        }
        @Override public StatusLlm verificarDisponibilidade() { return new StatusLlm(true, true, "ok"); }
        @Override public Optional<String> revisarConcordancia(String a, String b, List<String> c) { return Optional.empty(); }
        @Override public Optional<String> corrigirTraducao(String a, String b, String c) { return Optional.empty(); }
    }

    /** Reproduz o adaptador com o servidor FORA DO AR: timeout/5xx, sem resposta classificável. */
    private static final class LlmForaDoAr implements LlmPort {
        @Override public TraducaoLote traduzir(Lote lote) {
            return new TraducaoLote(lote.idLote(), null, false,
                "Erro ao traduzir o lote " + lote.idLote() + " apos 3 tentativa(s): Connection refused");
        }
        @Override public StatusLlm verificarDisponibilidade() { return new StatusLlm(false, false, "offline"); }
        @Override public Optional<String> revisarConcordancia(String a, String b, List<String> c) { return Optional.empty(); }
        @Override public Optional<String> corrigirTraducao(String a, String b, String c) { return Optional.empty(); }
    }

    /**
     * Recusa só a fala patológica; qualquer outra é traduzida normalmente. As traduções são
     * português de verdade porque o {@code ValidadorTraducaoService} do teste é o REAL — um
     * fake do tipo {@code texto + "-pt"} é reprovado como resíduo em inglês, e o teste passaria
     * a medir o validador em vez do desfecho da recusa.
     */
    private static final class LlmRecusaSomenteAFalaRuim implements LlmPort {
        static final String PATOLOGICA = "[[TAG0]]t[[TAG1]]n[[TAG2]]g";
        static final Map<String, String> DICIONARIO = Map.of(
            "Good morning.", "Bom dia.",
            "Where are we going?", "Para onde estamos indo?",
            "Watch out!", "Cuidado!",
            "I will protect you.", "Eu vou proteger você.");

        @Override public TraducaoLote traduzir(Lote lote) {
            if (lote.linhasOriginais().contains(PATOLOGICA)) {
                return TraducaoLote.recusadaPeloServidor(lote.idLote(), "HTTP 400");
            }
            return new TraducaoLote(lote.idLote(),
                lote.linhasOriginais().stream().map(DICIONARIO::get).toList(), true, null);
        }
        @Override public StatusLlm verificarDisponibilidade() { return new StatusLlm(true, true, "ok"); }
        @Override public Optional<String> revisarConcordancia(String a, String b, List<String> c) { return Optional.empty(); }
        @Override public Optional<String> corrigirTraducao(String a, String b, String c) { return Optional.empty(); }
    }

    private static ProcessarEpisodioUseCase useCaseCom(LlmPort llm, TelemetriaFake telemetria) {
        return new ProcessarEpisodioUseCase(
            llm, new ValidadorTraducaoService(LoreAtivaFake.vazia()), new ConsoleUILogger(),
            telemetria, new MascaradorTags(), new ReparadorMarcadoresLlm(new MascaradorTags()));
    }

    @Test
    @DisplayName("HTTP 400 numa fala: episódio segue, fala mantém o original")
    void recusaDaRequisicaoNaoDerrubaOEpisodio() throws InterruptedException, ExecutionException {
        TelemetriaFake telemetria = new TelemetriaFake();
        LlmRecusaARequisicao llm = new LlmRecusaARequisicao();

        List<TraducaoLote> resultado = useCaseCom(llm, telemetria).processarEpisodio(
            List.of(new Lote(1, List.of("KEEPME"))), null);

        assertEquals(1, resultado.size(), "o episódio não pode ser abortado por uma fala recusada");
        assertEquals(List.of("KEEPME"), resultado.getFirst().linhasTraduzidas(),
            "a fala recusada deve manter o texto ORIGINAL, como qualquer pendência");
        assertTrue(resultado.getFirst().sucesso(),
            "o lote encerra e a pendência é resolvida adiante, sem derrubar o arquivo");
        assertTrue(llm.chamadas.get() > 1,
            "a recusa deve gastar as retentativas de temperatura, como uma alucinação");
        assertEquals(0, telemetria.recuperadas.get(),
            "manter o original NÃO é recuperação — não pode registrar falha recuperada");
    }

    @Test
    @DisplayName("o dano real: 1 verso patológico não pode levar as falas de diálogo junto")
    void falaPatologicaNaoLevaOsDialogosJunto() throws InterruptedException, ExecutionException {
        List<String> episodio = List.of(
            "Good morning.", "Where are we going?", LlmRecusaSomenteAFalaRuim.PATOLOGICA,
            "Watch out!", "I will protect you.");

        List<TraducaoLote> resultado = useCaseCom(new LlmRecusaSomenteAFalaRuim(), new TelemetriaFake())
            .processarEpisodio(List.of(new Lote(1, episodio)), null);

        List<String> saida = resultado.getFirst().linhasTraduzidas();
        assertEquals(5, saida.size(), "nenhuma fala pode sumir por causa da vizinha");
        assertEquals(
            List.of("Bom dia.", "Para onde estamos indo?", LlmRecusaSomenteAFalaRuim.PATOLOGICA,
                "Cuidado!", "Eu vou proteger você."),
            saida,
            "as 4 falas boas ficam traduzidas; só a patológica fica pendente com o original");
    }

    @Test
    @DisplayName("contraprova: servidor fora do ar continua abortando o episódio")
    void servidorForaDoArContinuaAbortando() {
        ProcessarEpisodioUseCase useCase = useCaseCom(new LlmForaDoAr(), new TelemetriaFake());

        TraducaoParcialException erro = assertThrows(TraducaoParcialException.class,
            () -> useCase.processarEpisodio(List.of(new Lote(1, List.of("KEEPME"))), null),
            "indisponibilidade não é pendência: insistir com o servidor caído só gasta tempo");

        assertTrue(erro.getMessage().contains("falhou na comunicação"),
            "o diagnóstico de indisponibilidade não pode virar o de recusa: " + erro.getMessage());
    }

    @Test
    @DisplayName("disjuntor: recusa em série é servidor recusando tudo, e aborta com diagnóstico próprio")
    void recusaEmSerieAbortaComDiagnosticoProprio() {
        ProcessarEpisodioUseCase useCase = useCaseCom(new LlmRecusaARequisicao(), new TelemetriaFake());

        TraducaoParcialException erro = assertThrows(TraducaoParcialException.class,
            () -> useCase.processarEpisodio(List.of(
                new Lote(1, List.of("uma")), new Lote(2, List.of("duas")), new Lote(3, List.of("tres"))), null),
            "3 falas seguidas recusadas sem nenhum pedido aceito é o servidor, não a fala");

        assertTrue(erro.getMessage().contains("recusou 3 falas seguidas"),
            "o aborto por disjuntor precisa dizer POR QUE parou: " + erro.getMessage());
        assertEquals(2, erro.getLotesSalvos().size(),
            "o que já foi processado antes do disjuntor não pode ser descartado");
    }
}
