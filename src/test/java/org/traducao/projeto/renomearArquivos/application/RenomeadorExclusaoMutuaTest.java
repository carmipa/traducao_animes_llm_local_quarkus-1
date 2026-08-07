package org.traducao.projeto.renomearArquivos.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: impede o retorno do {@code BLOQUEIOS_POR_PASTA.remove} que
 * quebrava a exclusão mútua do renomeador — a garantia que protege {@code Files.move} e
 * o manifesto de reversão de terem dois donos ao mesmo tempo.
 *
 * <p>A sequência que quebrava:
 *
 * <pre>
 *   A dá unlock  ->  B faz computeIfAbsent, obtém o MESMO lock, tryLock OK, ENTRA
 *   A executa remove(chave, lock)   &lt;- tira do mapa o lock que B está segurando
 *   C faz computeIfAbsent, cria lock NOVO, tryLock OK, ENTRA TAMBÉM
 * </pre>
 *
 * <p>Não era teórico: {@code RenomearArquivosController} é JAX-RS puro e NÃO passa pela
 * {@code FilaExecucaoPipeline} — roda na thread HTTP, com requisições concorrentes reais.
 *
 * <h2>Por que NÃO há teste de corrida com muitas threads</h2>
 * Houve, e foi removido: um teste de 30 threads concorrentes <b>passava igualmente com e
 * sem o defeito</b> (verificado por mutação em 2026-07-29). A janela entre {@code unlock}
 * e {@code remove} é de microssegundos e não se reproduz por pressão de carga; forçá-la
 * exigiria instrumentar o código de produção com um ponto de sincronização que só existe
 * para o teste. Um teste de concorrência que não falha na presença do bug dá confiança
 * falsa — pior que não ter.
 *
 * <p>Os dois casos abaixo foram verificados por mutação: reintroduzir o {@code remove}
 * faz ambos falharem.
 */
class RenomeadorExclusaoMutuaTest {

    private static final Path FONTE = Path.of(
        "src/main/java/org/traducao/projeto/renomearArquivos/application/RenomeadorUseCase.java");

    /** Réplica do padrão CORRIGIDO, para exercitar a propriedade sem montar o pipeline. */
    private static void comBloqueio(ConcurrentMap<String, ReentrantLock> mapa,
                                    String chave, Supplier<Void> acao) {
        ReentrantLock lock = mapa.computeIfAbsent(chave, k -> new ReentrantLock());
        if (!lock.tryLock()) {
            return;
        }
        try {
            acao.get();
        } finally {
            lock.unlock();
        }
    }

    @Test
    @DisplayName("liberar o lock NÃO esvazia o mapa — é o remove que reabre a corrida")
    void mapaMantemAChaveDepoisDeLiberar() {
        ConcurrentMap<String, ReentrantLock> mapa = new ConcurrentHashMap<>();

        String chavePasta = Path.of("animes", "obra").toString();
        comBloqueio(mapa, chavePasta, () -> null);

        assertEquals(1, mapa.size(),
            "a entrada precisa PERMANECER. Removê-la faz a próxima thread criar um lock NOVO "
                + "e entrar em paralelo com quem obteve o antigo entre o unlock e o remove.");
        assertFalse(mapa.get(chavePasta).isLocked(),
            "o lock continua no mapa, mas liberado — a próxima operação na pasta é permitida");
    }

    @Test
    @DisplayName("o código de produção não voltou a remover a chave no finally")
    void producaoNaoRemoveDoMapa() throws Exception {
        // Guarda textual porque a propriedade não é observável de fora: o método é privado,
        // o mapa é estático, e a corrida não se reproduz sob carga (ver Javadoc da classe).
        // Ler o fonte é feio, mas é o que efetivamente falha se o padrão voltar.
        String fonte = Files.readString(FONTE);
        int inicio = fonte.indexOf("private <T> T executarComBloqueio");
        assertTrue(inicio > 0, "o método de bloqueio precisa continuar existindo");
        String corpo = fonte.substring(inicio, fonte.indexOf("\n    }", inicio));

        assertFalse(corpo.contains("BLOQUEIOS_POR_PASTA.remove"),
            "BLOQUEIOS_POR_PASTA.remove voltou ao finally — isso reabre a corrida em que duas "
                + "threads renomeiam a mesma pasta ao mesmo tempo, com Files.move e manifesto "
                + "de reversão tendo dois donos");
        assertTrue(corpo.contains("bloqueio.unlock()"),
            "o lock ainda precisa ser liberado no finally");
    }
}
