package org.traducao.projeto.arquitetura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: impede que um byte NUL entre num arquivo-fonte Java.
 *
 * <h2>O prejuízo que originou</h2>
 * Um caractere NUL dentro de um literal {@code char} ({@code '\0'} gravado como o byte, não como o
 * escape) COMPILA normalmente — nenhum teste nem o compilador reclamam. O efeito é o arquivo virar
 * "binário" para {@code grep}, diff e revisão: o trecho some das buscas. Aconteceu DUAS vezes neste
 * projeto, as duas por script de edição que interpretou o escape: uma registrada no comentário do
 * {@code TradutorLotesService} ("a primeira versão desta linha gravou dois bytes NUL"), e outra em
 * 25/09/2026 no {@code ProcessarArquivoUseCase}, pega só porque o {@code grep} respondeu "Binary
 * file matches".
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Nenhum {@code .java} de {@code src/main} ou {@code src/test} contém o byte 0x00.</li>
 *   <li>Varredura que não leu arquivo nenhum é NÃO VERIFICADO e falha — nunca aprova por cegueira.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Lista os arquivos com o byte e a posição; o conserto é trocar pelo escape {@code '\u0000'}.
 */
class CatracaFonteSemByteNuloTest {

    static boolean contemByteNulo(byte[] conteudo) {
        for (byte b : conteudo) {
            if (b == 0) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("nenhum fonte Java contem byte NUL")
    void nenhumFonteContemByteNulo() throws IOException {
        List<String> comNulo = new ArrayList<>();
        int lidos = 0;
        for (String raiz : List.of("src/main/java", "src/test/java")) {
            try (Stream<Path> arquivos = Files.walk(Path.of(raiz))) {
                for (Path p : arquivos.filter(a -> a.toString().endsWith(".java")).toList()) {
                    lidos++;
                    byte[] bytes = Files.readAllBytes(p);
                    if (contemByteNulo(bytes)) {
                        comNulo.add(p.toString());
                    }
                }
            }
        }
        assertTrue(lidos > 100, "NAO VERIFICADO: so " + lidos + " fonte(s) lido(s) — a varredura esta cega");
        assertTrue(comNulo.isEmpty(), "byte NUL em fonte Java (troque pelo escape '\\u0000'): " + comNulo);
    }

    /**
     * CASO-CONTROLE DE FRONTEIRA (A1): o MESMO sinal superficial — um separador NUL no código —
     * escrito do jeito certo (o escape de seis caracteres) é aceito; o byte cru é reprovado.
     */
    @Test
    @DisplayName("controle: o escape e aceito, o byte cru e reprovado")
    void escapeAceitoByteCruReprovado() {
        byte[] comEscape = "a + '\\u0000' + b".getBytes(StandardCharsets.UTF_8);
        byte[] comByte = new byte[] {'a', ' ', '+', ' ', '\'', 0, '\'', ' ', '+', ' ', 'b'};
        assertFalse(contemByteNulo(comEscape), "o escape de texto nao pode ser acusado");
        assertTrue(contemByteNulo(comByte), "o byte cru tem de ser acusado");
    }
}
