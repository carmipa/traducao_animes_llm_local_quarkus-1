package org.traducao.projeto.analisadorMidia.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.analisadorMidia.domain.AnalisadorException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * PROPÓSITO DE NEGÓCIO: localiza os arquivos de vídeo a auditar a partir de uma
 * entrada que pode ser um único arquivo ou uma pasta (varredura recursiva),
 * filtrando pelas extensões de contêiner suportadas.
 *
 * <p>INVARIANTES DO DOMÍNIO: só retorna arquivos regulares com extensão de vídeo
 * conhecida; a ordem é estável (alfabética) para tornar a análise determinística.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: erro de I/O ao varrer a pasta lança
 * {@link AnalisadorException} didática; entrada válida sem vídeos retorna lista
 * vazia (o orquestrador decide como reportar).
 */
@Service
public class LocalizadorVideosService {

    private static final List<String> EXTENSOES_VIDEO = List.of(
        ".mkv", ".mp4", ".avi", ".mov", ".flv", ".wmv", ".webm", ".m4v", ".ts", ".m2ts"
    );

    public List<Path> localizar(Path entrada) {
        if (Files.isRegularFile(entrada)) {
            String nome = entrada.getFileName().toString().toLowerCase();
            for (String ext : EXTENSOES_VIDEO) {
                if (nome.endsWith(ext)) {
                    return List.of(entrada);
                }
            }
            return List.of();
        }

        try (Stream<Path> walk = Files.walk(entrada)) {
            return walk
                .filter(Files::isRegularFile)
                .filter(p -> {
                    String nome = p.getFileName().toString().toLowerCase();
                    return EXTENSOES_VIDEO.stream().anyMatch(nome::endsWith);
                })
                .sorted()
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new AnalisadorException("Erro ao escanear diretório de entrada para encontrar vídeos: " + entrada, e);
        }
    }
}
