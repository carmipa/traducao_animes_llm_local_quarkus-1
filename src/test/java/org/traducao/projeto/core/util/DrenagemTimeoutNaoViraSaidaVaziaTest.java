package org.traducao.projeto.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que estourar o tempo de drenagem da saída de um processo
 * externo LANÇA, em vez de devolver array vazio.
 *
 * <p>Array vazio é indistinguível de "o processo não escreveu nada", e quem consome
 * isto INTERPRETA a saída: {@code FfprobeAdapter} e {@code MkvToolNixAdapter} concluiriam
 * "este MKV não tem faixa de legenda" quando o fato é "não consegui ler a resposta". Num
 * lote, vira episódio pulado em silêncio. É a mesma classe de defeito que o
 * {@code build.gradle} do projeto já registra: <i>"'0 testes rodaram' tem a mesma
 * aparência de 'tudo passou'"</i>.
 *
 * <p>NOTA SOBRE O CUSTO: o teste leva ~5s porque o limite de drenagem é constante no
 * método. Aceito de propósito — é barato perto de descobrir o defeito num lote de 22
 * episódios. Se o limite virar parâmetro um dia, este teste fica instantâneo.
 *
 * <p>Usa reflexão porque {@code lerResultado} é privado: o valor está em travar o
 * COMPORTAMENTO, e expor o método só para testá-lo alargaria a superfície pública de um
 * utilitário de processo externo.
 */
class DrenagemTimeoutNaoViraSaidaVaziaTest {

    private static Method lerResultado() throws NoSuchMethodException {
        Method m = ProcessoExternoUtil.class.getDeclaredMethod("lerResultado", Future.class);
        m.setAccessible(true);
        return m;
    }

    @Test
    @DisplayName("drenagem que não completa vira IOException, nunca byte[0]")
    void timeoutLancaEmVezDeDevolverVazio() throws Exception {
        Method ler = lerResultado();
        // Future que nunca completa: reproduz a drenagem travada sem depender de um
        // processo real do sistema operacional.
        CompletableFuture<byte[]> travado = new CompletableFuture<>();

        InvocationTargetException empacotada = assertThrows(
            InvocationTargetException.class, () -> ler.invoke(null, travado));

        Throwable causa = empacotada.getCause();
        assertInstanceOf(IOException.class, causa,
            "timeout de drenagem deve virar IOException; devolver vazio faria o chamador "
                + "concluir 'sem faixas' em vez de 'falhei ao ler'");
        assertTrue(causa.getMessage().contains("INCOMPLETA"),
            "a mensagem precisa distinguir saída incompleta de saída vazia: " + causa.getMessage());
        assertTrue(travado.isCancelled(), "o future travado deve ser cancelado, sem vazar a thread");
    }

    @Test
    @DisplayName("drenagem que completa devolve os bytes normalmente")
    void caminhoFelizIntocado() throws Exception {
        Method ler = lerResultado();
        byte[] esperado = "faixa de legenda".getBytes();

        Object resultado = ler.invoke(null, CompletableFuture.completedFuture(esperado));

        assertEquals("faixa de legenda", new String((byte[]) resultado));
    }
}
