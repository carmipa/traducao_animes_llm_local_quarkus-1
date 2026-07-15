package org.traducao.projeto.core.util;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Substituição atômica de arquivo (temporário -&gt; destino) tolerante ao Windows.
 *
 * <p>No Windows, o move atômico ({@code MoveFileEx}) falha com
 * {@link AccessDeniedException} quando o arquivo de destino está momentaneamente
 * aberto por outro processo sem compartilhamento de exclusão — tipicamente
 * antivírus ou indexador varrendo o arquivo recém-gravado. O travamento dura
 * milissegundos, então algumas tentativas com espera crescente resolvem sem
 * perder a garantia de "nunca deixa o destino truncado".</p>
 */
public final class ArquivoAtomicoUtil {

    private static final int MAX_TENTATIVAS = 4;
    private static final long ESPERA_INICIAL_MS = 50;

    private ArquivoAtomicoUtil() {
    }

    /**
     * Move {@code origem} sobre {@code destino} com {@code ATOMIC_MOVE},
     * repetindo em caso de {@code AccessDeniedException} transitório
     * (esperas de 50/100/200ms entre as 4 tentativas).
     */
    public static void substituirAtomico(Path origem, Path destino) throws IOException {
        long esperaMs = ESPERA_INICIAL_MS;
        for (int tentativa = 1; ; tentativa++) {
            try {
                Files.move(origem, destino,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                return;
            } catch (AtomicMoveNotSupportedException e) {
                // sistema de arquivos sem suporte atômico: substituição simples
                Files.move(origem, destino, StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (AccessDeniedException e) {
                if (tentativa >= MAX_TENTATIVAS) {
                    throw e;
                }
                try {
                    Thread.sleep(esperaMs);
                } catch (InterruptedException interrompida) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
                esperaMs *= 2;
            }
        }
    }
}
