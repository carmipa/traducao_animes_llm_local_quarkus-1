package org.traducao.projeto.analisadorMidia.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.analisadorMidia.domain.AnalisadorException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobre o localizador de vídeos: varredura recursiva por extensão, arquivo
 * único e falha de I/O. Componente extraído do AnalisarMidiaUseCase (Etapa 8).
 */
class LocalizadorVideosServiceTest {

    private final LocalizadorVideosService localizador = new LocalizadorVideosService();

    @Test
    void varreDiretorioRecursivamenteSoRetornandoVideos(@TempDir Path raiz) throws IOException {
        Files.createFile(raiz.resolve("b.mkv"));
        Files.createFile(raiz.resolve("a.mp4"));
        Files.createFile(raiz.resolve("nota.txt"));
        Files.createFile(raiz.resolve("legenda.ass"));
        Path sub = raiz.resolve("sub");
        Files.createDirectories(sub);
        Files.createFile(sub.resolve("c.webm"));

        List<Path> encontrados = localizador.localizar(raiz);

        assertEquals(3, encontrados.size(), encontrados.toString());
        assertTrue(encontrados.stream().allMatch(p -> {
            String n = p.getFileName().toString();
            return n.endsWith(".mkv") || n.endsWith(".mp4") || n.endsWith(".webm");
        }));
        // Ordem estável (alfabética por caminho): a.mp4 antes de b.mkv.
        assertTrue(encontrados.get(0).getFileName().toString().equals("a.mp4"));
    }

    @Test
    void arquivoUnicoDeVideoRetornaEleMesmo(@TempDir Path raiz) throws IOException {
        Path video = raiz.resolve("ep01.mkv");
        Files.createFile(video);

        assertEquals(List.of(video), localizador.localizar(video));
    }

    @Test
    void arquivoUnicoNaoVideoRetornaVazio(@TempDir Path raiz) throws IOException {
        Path arquivo = raiz.resolve("leia.txt");
        Files.createFile(arquivo);

        assertTrue(localizador.localizar(arquivo).isEmpty());
    }

    @Test
    void caminhoInexistenteLancaAnalisadorException(@TempDir Path raiz) {
        Path inexistente = raiz.resolve("nao-existe");
        assertThrows(AnalisadorException.class, () -> localizador.localizar(inexistente));
    }
}
