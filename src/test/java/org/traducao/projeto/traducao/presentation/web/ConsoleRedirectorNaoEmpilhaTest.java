package org.traducao.projeto.traducao.presentation.web;

import io.quarkus.runtime.StartupEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: o espelho do console alimenta o painel web que o operador acompanha
 * durante um lote de horas. Ele tem de sair UMA vez por linha e continuar vivo depois de um live
 * reload — as duas coisas juntas, porque cada uma sozinha já quebrou em produção.
 *
 * <h2>Os dois prejuízos, os dois medidos em 17/08/2026</h2>
 * <ol>
 *   <li><b>Empilhava.</b> {@code System.setOut(new PrintStream(... System.out ...))} a cada
 *       {@link StartupEvent}, e no dev mode cada reload dispara o evento: o {@code System.out} de
 *       então JÁ É o espelho anterior, então as camadas se somam e cada linha passa por todas.
 *       Numa corrida da 3.2 no 86: 14.512 linhas em {@code [console]} + 7.256 em
 *       {@code [revisao-lore]} = <b>3 cópias</b> de cada linha. A 3.1 já registrara o sintoma
 *       ("console duplica 7×, cresce dentro do processo") sem achar a causa.</li>
 *   <li><b>Emudecia.</b> O conserto de "instalar uma vez só", com marca em propriedade de
 *       sistema, matou o painel INTEIRO no primeiro reload: o Quarkus devolve o
 *       {@code System.out} ao console físico ao desligar, e a marca impedia recolocar o espelho.
 *       Mesmo que sobrevivesse, ele apontava para o {@code publicarLog} do bean ANTIGO, cujo
 *       {@code LogStreamService} morreu com o contexto. Medido: {@code console-web.log} recebeu
 *       a última linha às 22:26 e ficou mudo enquanto uma revisão de 15 arquivos rodava.</li>
 * </ol>
 * Silêncio é pior que duplicata: a linha repetida incomoda, a linha ausente engana.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O teste conta PUBLICAÇÕES e olha QUEM recebeu — nunca a identidade do {@code System.out}.
 *       A versão anterior cravava {@code assertSame(System.out)}, que é a implementação, não a
 *       propriedade: ela ficava verde justamente com o painel mudo.</li>
 *   <li>{@code System.out} e a chave do original são restaurados no {@code @AfterEach} — teste
 *       que sequestra a saída padrão e não devolve contamina a suíte inteira.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem diz qual dos dois defeitos voltou e o que o operador veria no painel.
 */
class ConsoleRedirectorNaoEmpilhaTest {

    private PrintStream saidaOriginal;
    private Object originalGuardado;

    @BeforeEach
    void guardarEstado() {
        saidaOriginal = System.out;
        originalGuardado = System.getProperties().get(ConsoleRedirector.CHAVE_STDOUT_ORIGINAL);
        System.getProperties().remove(ConsoleRedirector.CHAVE_STDOUT_ORIGINAL);
    }

    @AfterEach
    void restaurarEstado() {
        System.setOut(saidaOriginal);
        if (originalGuardado == null) {
            System.getProperties().remove(ConsoleRedirector.CHAVE_STDOUT_ORIGINAL);
        } else {
            System.getProperties().put(ConsoleRedirector.CHAVE_STDOUT_ORIGINAL, originalGuardado);
        }
    }

    /** Conta quantas vezes cada linha chega ao painel, que é o que o operador vê duplicado. */
    private static final class PainelDeMentira
        extends org.traducao.projeto.core.presentation.web.LogStreamService {

        final List<String> recebidas = new ArrayList<>();
        final AtomicInteger publicacoes = new AtomicInteger();

        @Override
        public void publicarLog(String mensagem) {
            publicacoes.incrementAndGet();
            recebidas.add(mensagem);
        }
    }

    @Test
    @DisplayName("dois StartupEvent (o 2o e o live reload) NAO fazem a linha sair duas vezes")
    void reloadNaoEmpilhaORedirecionador() {
        PainelDeMentira painel = new PainelDeMentira();
        ConsoleRedirector redirector = new ConsoleRedirector(painel);

        redirector.onStart(new StartupEvent());
        // O live reload do dev mode: MESMO processo, evento de novo.
        redirector.onStart(new StartupEvent());

        System.out.println("linha unica");
        System.out.flush();

        assertEquals(1, painel.publicacoes.get(),
            () -> "a linha chegou ao painel " + painel.publicacoes.get() + " vez(es), e devia ser "
                + "UMA. O espelho voltou a embrulhar o System.out corrente em vez do original, "
                + "entao a duplicacao cresce a cada live reload — foi assim que uma corrida no 86 "
                + "saiu com 3 copias de cada linha. Recebidas: " + painel.recebidas);
    }

    @Test
    @DisplayName("depois do reload o espelho fala com o bean VIVO, nao com o morto")
    void reloadReataOEspelhoAoBeanVivo() {
        PainelDeMentira antesDoReload = new PainelDeMentira();
        new ConsoleRedirector(antesDoReload).onStart(new StartupEvent());

        // O reload constroi um bean NOVO, com um LogStreamService NOVO. O antigo morre junto com
        // o contexto — inclusive o ThreadLocal do canal.
        PainelDeMentira depoisDoReload = new PainelDeMentira();
        new ConsoleRedirector(depoisDoReload).onStart(new StartupEvent());

        System.out.println("linha depois do reload");
        System.out.flush();

        assertEquals(1, depoisDoReload.publicacoes.get(),
            () -> "o painel ficou MUDO depois do live reload: a linha nao chegou ao bean vivo. "
                + "Foi o que aconteceu as 22:26 de 17/08 — o console-web.log parou enquanto uma "
                + "revisao de 15 arquivos rodava normalmente, sem erro nenhum na tela.");
        assertEquals(0, antesDoReload.publicacoes.get(),
            () -> "a linha foi entregue ao bean MORTO (" + antesDoReload.recebidas + "). O "
                + "LogStreamService dele nao tem mais canal nem contexto, entao a linha se perde "
                + "ou vai para o canal errado.");
    }

    @Test
    @DisplayName("CONTROLE POSITIVO: a primeira instalacao publica de fato")
    void primeiraInstalacaoPublica() {
        PainelDeMentira painel = new PainelDeMentira();
        new ConsoleRedirector(painel).onStart(new StartupEvent());

        System.out.println("linha unica");
        System.out.flush();

        assertEquals(1, painel.publicacoes.get(),
            "sem esta assercao, os testes acima passariam com o redirecionador NUNCA instalado — "
                + "zero publicacoes tambem satisfaz 'nao duplicou'");
    }
}
