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
import org.traducao.projeto.traducao.domain.ports.TelemetriaTraducaoPort;
import org.traducao.projeto.traducao.presentation.ui.ConsoleUILogger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: exercita a SEGUNDA OPINIÃO — o caminho em que uma fala dada como perdida
 * pelo modelo principal é oferecida a um segundo modelo antes de virar pendência. O mecanismo
 * existe desde 11/08/2026 e nunca tinha sido exercitado: estava 🔴 no checkpoint, e "desligado por
 * padrão" tornava a ausência de sinal indistinguível de "ligado e sem efeito".
 *
 * <h2>Dois gaps que pareciam um só</h2>
 * "O modelo-recuperacao nunca foi testado" misturava duas perguntas diferentes:
 * <ol>
 *   <li><b>O mecanismo funciona?</b> — a fala reprovada chega ao segundo modelo, a resposta passa
 *       pela MESMA régua, o desfecho é registrado. É o que estes testes provam, sem rede.</li>
 *   <li><b>Qual modelo recupera as 6 do Zeta?</b> — medição de MODELO, que depende do LM Studio no
 *       ar e de uma escolha de qual segundo modelo carregar. Continua aberta.</li>
 * </ol>
 * Separar as duas é o que permite fechar a primeira agora; juntá-las manteria as duas eternamente
 * bloqueadas pela infraestrutura.
 *
 * <h2>A fala do caso-controle é a real</h2>
 * {@code I just said, "You want to meet Char, don't you?"} — discurso citado com aspas internas,
 * a classe que respondeu por 5 das 6 pendências do Zeta em 50 episódios com {@code mistral-nemo},
 * e que o {@code towerinstruct} venceu em 3 na primeira tentativa. O laço de temperatura, que
 * repete com o MESMO modelo, não vencia nenhuma.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Segunda opinião NÃO é passe livre: a resposta do segundo modelo passa pela mesma validação
 *       canônica. Reprovada ali, a fala segue pendente.</li>
 *   <li>Vazio significa DESLIGADO e é o padrão — falha fechada. O pipeline não adota um segundo
 *       modelo só porque ele está carregado no servidor.</li>
 *   <li>Recuperação bem-sucedida registra {@code registrarFalhaTraducaoRecuperada}; manter o
 *       original NÃO é recuperação e não pode contar como tal.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem rede: o {@link LlmPort} é um dublê que distingue a chamada COM {@code modeloOverride} da
 * chamada sem. Se alguém apagar a passagem do override, o teste da recuperação reprova — o dublê
 * passaria a responder como o titular.
 */
@DisplayName("segunda opinião: o modelo de recuperação recebe a fala que o titular perdeu")
class SegundaOpiniaoModeloRecuperacaoTest {

    /** A classe de fala que o laço de temperatura nunca vencia — discurso citado com aspas. */
    private static final String FALA_DO_ZETA = "I just said, \"You want to meet Char, don't you?\"";
    private static final String TRADUCAO_BOA =
        "Eu acabei de dizer: \"Você quer conhecer o Char, não quer?\"";
    private static final String MODELO_SEGUNDO = "towerinstruct-de-mentira";

    private static final class TelemetriaFake implements TelemetriaTraducaoPort {
        final AtomicInteger rejeitadas = new AtomicInteger();
        final AtomicInteger recuperadas = new AtomicInteger();

        @Override public void registrarTraducao(TelemetriaTraducao telemetria) { }
        @Override public void registrarAlucinacaoPrevenida() { }
        @Override public void registrarRespostaTraducaoRejeitada() { rejeitadas.incrementAndGet(); }
        @Override public void registrarFalhaTraducaoRecuperada() { recuperadas.incrementAndGet(); }
        @Override public void registrarFallbackMantido() { }
    }

    /**
     * Titular que devolve o inglês intacto — o {@code ValidadorTraducaoService} REAL o reprova como
     * resíduo, que é como uma fala vira perdida de verdade. O segundo modelo responde certo, e a
     * ÚNICA coisa que os distingue é o {@code modeloOverride} ter chegado.
     */
    private static final class LlmQueSoAcertaComOSegundoModelo implements LlmPort {
        final AtomicInteger chamadasTitular = new AtomicInteger();
        final AtomicInteger chamadasSegundo = new AtomicInteger();
        private final String modeloRecuperacao;
        private final String respostaDoSegundo;

        LlmQueSoAcertaComOSegundoModelo(String modeloRecuperacao, String respostaDoSegundo) {
            this.modeloRecuperacao = modeloRecuperacao;
            this.respostaDoSegundo = respostaDoSegundo;
        }

        @Override public TraducaoLote traduzir(Lote lote) {
            chamadasTitular.incrementAndGet();
            return new TraducaoLote(lote.idLote(), lote.linhasOriginais(), true, null);
        }

        @Override public TraducaoLote traduzir(Lote lote, Double temperatura,
                String promptCongelado, String modeloOverride) {
            if (modeloOverride == null || modeloOverride.isBlank()) {
                return traduzir(lote);
            }
            chamadasSegundo.incrementAndGet();
            return new TraducaoLote(lote.idLote(), List.of(respostaDoSegundo), true, null);
        }

        @Override public String modeloRecuperacao() { return modeloRecuperacao; }
        @Override public StatusLlm verificarDisponibilidade() { return new StatusLlm(true, true, "ok"); }
        @Override public Optional<String> revisarConcordancia(String a, String b, List<String> c) {
            return Optional.empty();
        }
        @Override public Optional<String> corrigirTraducao(String a, String b, String c) {
            return Optional.empty();
        }
    }

    private static ProcessarEpisodioUseCase useCaseCom(LlmPort llm, TelemetriaFake telemetria) {
        return new ProcessarEpisodioUseCase(
            llm, new ValidadorTraducaoService(LoreAtivaFake.vazia()), new ConsoleUILogger(),
            telemetria, new MascaradorTags(), new ReparadorMarcadoresLlm(new MascaradorTags()));
    }

    private static List<String> traduzir(LlmPort llm, TelemetriaFake telemetria)
            throws InterruptedException, ExecutionException {
        return useCaseCom(llm, telemetria)
            .processarEpisodio(List.of(new Lote(1, List.of(FALA_DO_ZETA))), null)
            .getFirst()
            .linhasTraduzidas();
    }

    @Test
    @DisplayName("a fala que o titular perdeu chega ao segundo modelo e VOLTA traduzida")
    void segundaOpiniaoRecuperaAFalaPerdida() throws InterruptedException, ExecutionException {
        TelemetriaFake telemetria = new TelemetriaFake();
        var llm = new LlmQueSoAcertaComOSegundoModelo(MODELO_SEGUNDO, TRADUCAO_BOA);

        List<String> saida = traduzir(llm, telemetria);

        assertEquals(List.of(TRADUCAO_BOA), saida,
            "A SEGUNDA OPINIÃO NÃO CHEGOU AO ARQUIVO. O titular reprovou, o modelo de recuperação "
                + "estava configurado e respondeu certo, e mesmo assim a fala não foi recuperada.");
        assertTrue(llm.chamadasSegundo.get() >= 1,
            "o segundo modelo precisa ter sido efetivamente chamado com o modeloOverride");
        assertTrue(llm.chamadasTitular.get() > 1,
            "a segunda opinião só entra DEPOIS do laço de temperatura — ela não pode atalhar o "
                + "caminho barato e cobrar uma chamada extra em toda fala");
        assertEquals(1, telemetria.recuperadas.get(),
            "recuperação bem-sucedida tem de aparecer na telemetria; sem isso um modelo de "
                + "recuperação inútil fica indistinguível de um que trabalha");
    }

    /**
     * CONTRAPROVA que dá sentido ao teste acima: com o modelo de recuperação DESLIGADO — o padrão —
     * a mesma fala, o mesmo titular e o mesmo dublê produzem pendência. Sem isto, o teste anterior
     * poderia estar passando por qualquer outro caminho de retentativa.
     */
    @Test
    @DisplayName("contraprova: desligado (vazio), a mesma fala fica pendente com o original")
    void desligadoAFalaSeguePendente() throws InterruptedException, ExecutionException {
        TelemetriaFake telemetria = new TelemetriaFake();
        var llm = new LlmQueSoAcertaComOSegundoModelo("", TRADUCAO_BOA);

        List<String> saida = traduzir(llm, telemetria);

        assertEquals(List.of(FALA_DO_ZETA), saida,
            "desligado, a fala tem de manter o ORIGINAL — é assim que ela vira pendência");
        assertEquals(0, llm.chamadasSegundo.get(),
            "FALHA FECHADA: vazio significa desligado. O pipeline não pode adotar um segundo "
                + "modelo por conta própria só porque ele está carregado no servidor");
        assertEquals(0, telemetria.recuperadas.get(),
            "manter o original NÃO é recuperação");
    }

    /**
     * O invariante que o Javadoc da porta declara: <i>segunda opinião não é passe livre</i>. O
     * segundo modelo responde, mas responde em inglês — a MESMA régua que reprovou o titular tem
     * de reprová-lo, e a fala segue pendente em vez de entrar no arquivo por vir de outro modelo.
     */
    @Test
    @DisplayName("passe livre negado: se o segundo modelo também erra, a fala segue pendente")
    void segundaOpiniaoReprovadaNaMesmaReguaNaoEntraNoArquivo()
            throws InterruptedException, ExecutionException {
        TelemetriaFake telemetria = new TelemetriaFake();
        var llm = new LlmQueSoAcertaComOSegundoModelo(MODELO_SEGUNDO,
            "I just said, \"You want to meet Char, don't you?\"");

        List<String> saida = traduzir(llm, telemetria);

        assertEquals(List.of(FALA_DO_ZETA), saida,
            "resposta do segundo modelo reprovada na régua canônica NÃO pode entrar no arquivo");
        assertTrue(llm.chamadasSegundo.get() >= 1, "o segundo modelo foi consultado");
        assertEquals(0, telemetria.recuperadas.get(),
            "consultar não é recuperar — contar aqui inflaria a taxa de recuperação com fracassos");
    }
}
