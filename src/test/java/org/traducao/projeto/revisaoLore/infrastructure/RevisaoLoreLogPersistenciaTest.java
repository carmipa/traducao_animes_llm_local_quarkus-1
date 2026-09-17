package org.traducao.projeto.revisaoLore.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: o relatório JSON de uma execução é a evidência daquela sessão; duas
 * execuções não podem produzir o mesmo caminho, senão a segunda apaga a prova da primeira.
 *
 * <h2>O prejuízo</h2>
 * O nome carimbava só o segundo ({@code yyyyMMdd_HHmmss}); duas execuções SEM_ARQUIVOS no MESMO
 * segundo (a fila serializa, mas execuções vazias são rápidas) sobrescreviam o relatório uma da
 * outra. Milissegundos reduzem, mas só a catraca de existência fecha o buraco de vez.
 *
 * <h2>Invariantes do domínio</h2>
 * Nome base quando livre; sufixo sequencial quando ocupado.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Reprova se um caminho já existente for reutilizado (o defeito antigo).
 */
class RevisaoLoreLogPersistenciaTest {

    @Test
    @DisplayName("A1: caminho ocupado ganha sufixo; caminho livre mantem o nome base")
    void caminhoUnicoNaoSobrescreveERespeitaOLivre(@TempDir Path dir) throws IOException {
        // OCUPADO: um relatorio ja existe naquele carimbo -> o proximo NAO pode ser o mesmo nome.
        Files.createFile(dir.resolve("revisao_lore_20260917_120000_000.json"));
        Path ocupado = RevisaoLoreLogPersistencia.caminhoUnico(dir, "20260917_120000_000");
        assertEquals("revisao_lore_20260917_120000_000_2.json", ocupado.getFileName().toString(),
            "carimbo ja existe: o relatorio novo tem de ganhar sufixo, nunca sobrescrever o anterior");

        // e um terceiro no mesmo carimbo continua subindo o sufixo
        Files.createFile(dir.resolve("revisao_lore_20260917_120000_000_2.json"));
        Path terceiro = RevisaoLoreLogPersistencia.caminhoUnico(dir, "20260917_120000_000");
        assertEquals("revisao_lore_20260917_120000_000_3.json", terceiro.getFileName().toString());

        // LIVRE: carimbo novo mantem o nome base (nao infla o nome sem necessidade)
        Path livre = RevisaoLoreLogPersistencia.caminhoUnico(dir, "20260917_130000_000");
        assertEquals("revisao_lore_20260917_130000_000.json", livre.getFileName().toString(),
            "carimbo livre nao pode ganhar sufixo — so quando ha colisao de verdade");
    }
}
