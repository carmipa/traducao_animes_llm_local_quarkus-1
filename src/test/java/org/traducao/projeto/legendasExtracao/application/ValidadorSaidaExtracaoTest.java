package org.traducao.projeto.legendasExtracao.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.legendasExtracao.domain.ExtratorException;
import org.traducao.projeto.legendasExtracao.domain.FormatoLegenda;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobre a blindagem de saída: existência, tamanho > 0 e correspondência de
 * formato (ASS/SRT/PGS) do arquivo recém-extraído.
 */
class ValidadorSaidaExtracaoTest {

    @TempDir
    Path dir;

    @Test
    void assValidoPassa() throws IOException {
        Path f = Files.writeString(dir.resolve("a.ass"),
            "[Script Info]\nScriptType: v4.00+\n[Events]\nDialogue: 0,0:00:01.00,0:00:02.00,Default,,0,0,0,,Olá");
        assertDoesNotThrow(() -> ValidadorSaidaExtracao.validar(f, FormatoLegenda.ASS));
    }

    @Test
    void srtValidoPassa() throws IOException {
        Path f = Files.writeString(dir.resolve("a.srt"), "1\n00:00:01,000 --> 00:00:02,000\nOlá\n");
        assertDoesNotThrow(() -> ValidadorSaidaExtracao.validar(f, FormatoLegenda.SRT));
    }

    @Test
    void pgsValidoPassa() throws IOException {
        Path f = dir.resolve("a.sup");
        Files.write(f, new byte[]{'P', 'G', 0x00, 0x01});
        assertDoesNotThrow(() -> ValidadorSaidaExtracao.validar(f, FormatoLegenda.PGS));
    }

    @Test
    void arquivoVazioFalha() throws IOException {
        Path f = Files.createFile(dir.resolve("vazio.ass"));
        ExtratorException ex = assertThrows(ExtratorException.class,
            () -> ValidadorSaidaExtracao.validar(f, FormatoLegenda.ASS));
        assertTrue(ex.getMessage().toLowerCase().contains("vazio"));
    }

    @Test
    void arquivoInexistenteFalha() {
        assertThrows(ExtratorException.class,
            () -> ValidadorSaidaExtracao.validar(dir.resolve("nao_existe.ass"), FormatoLegenda.ASS));
    }

    @Test
    void formatoDivergenteFalha() throws IOException {
        // Conteúdo SRT gravado num arquivo, mas o formato pedido é ASS.
        Path f = Files.writeString(dir.resolve("x.ass"), "1\n00:00:01,000 --> 00:00:02,000\nOlá\n");
        ExtratorException ex = assertThrows(ExtratorException.class,
            () -> ValidadorSaidaExtracao.validar(f, FormatoLegenda.ASS));
        assertTrue(ex.getMessage().contains("não corresponde"));
    }
}
