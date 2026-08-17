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
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * PROPÓSITO DE NEGÓCIO: garantir que o espelho do console para o painel web seja instalado UMA
 * vez por processo — cada instalação extra faz toda linha do console aparecer uma vez a mais.
 *
 * <h2>O prejuízo, medido no acervo em 17/08/2026</h2>
 * O método instalava {@code System.setOut(new PrintStream(... System.out ...))} a cada
 * {@link StartupEvent}. No dev mode cada live reload dispara o evento, e o {@code System.out} de
 * então JÁ É o redirecionador anterior — as camadas se empilham e cada linha passa por todas.
 *
 * <p>Numa corrida da tela 3.2 no 86, depois de várias recompilações na mesma sessão:
 * <pre>
 *   14.512 linhas no canal [console] + 7.256 em [revisao-lore] = 3 copias de cada linha
 *   linhas com prefixo "[UTC" (do log.info): 0  -> a duplicacao NAO vinha do logger
 * </pre>
 * A tela 3.1 já tinha registrado o sintoma — "console duplica linha (7×, cresce dentro do
 * processo)" — sem achar a causa. Ela cresce porque cada reload acrescenta uma camada.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O teste conta PUBLICAÇÕES, não camadas: é o número de vezes que a mesma linha chega ao
 *       painel que importa, e contar camadas deixaria passar qualquer outro caminho de duplicação.</li>
 *   <li>Restaura o {@code System.out} e a marca no {@code @AfterEach} — teste que sequestra a
 *       saída padrão e não devolve contamina a suíte inteira.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem diz quantas cópias saíram e relembra que o número cresce a cada reload.
 */
class ConsoleRedirectorNaoEmpilhaTest {

    private PrintStream saidaOriginal;
    private String marcaAnterior;

    @BeforeEach
    void guardarEstado() {
        saidaOriginal = System.out;
        marcaAnterior = System.getProperty(ConsoleRedirector.MARCA_INSTALADO);
        System.clearProperty(ConsoleRedirector.MARCA_INSTALADO);
    }

    @AfterEach
    void restaurarEstado() {
        System.setOut(saidaOriginal);
        if (marcaAnterior == null) {
            System.clearProperty(ConsoleRedirector.MARCA_INSTALADO);
        } else {
            System.setProperty(ConsoleRedirector.MARCA_INSTALADO, marcaAnterior);
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
        PrintStream depoisDoPrimeiro = System.out;

        // O live reload do dev mode: MESMO processo, evento de novo.
        redirector.onStart(new StartupEvent());

        assertSame(depoisDoPrimeiro, System.out,
            "o segundo StartupEvent EMBRULHOU o System.out de novo. Cada camada publica uma vez, "
                + "entao a duplicacao cresce a cada live reload — foi assim que uma corrida no 86 "
                + "saiu com 3 copias de cada linha.");

        System.out.println("linha unica");
        System.out.flush();

        assertEquals(1, painel.publicacoes.get(),
            () -> "a linha chegou ao painel " + painel.publicacoes.get() + " vez(es), e devia ser "
                + "UMA. Recebidas: " + painel.recebidas);
    }

    @Test
    @DisplayName("CONTROLE POSITIVO: a primeira instalacao publica de fato")
    void primeiraInstalacaoPublica() {
        PainelDeMentira painel = new PainelDeMentira();
        new ConsoleRedirector(painel).onStart(new StartupEvent());

        System.out.println("linha unica");
        System.out.flush();

        assertEquals(1, painel.publicacoes.get(),
            "sem esta assercao, o teste acima passaria com o redirecionador NUNCA instalado — "
                + "zero publicacoes tambem satisfaz 'nao duplicou'");
    }
}
