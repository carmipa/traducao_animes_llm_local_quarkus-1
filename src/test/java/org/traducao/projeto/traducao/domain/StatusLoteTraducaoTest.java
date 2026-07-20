package org.traducao.projeto.traducao.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Cobre a consolidação do status do lote a partir dos status por arquivo —
 * o núcleo do fix "não mostrar sucesso quando houve falhas".
 */
class StatusLoteTraducaoTest {

    private static ResultadoTraducaoArquivo comStatus(StatusArquivoTraducao s) {
        return new ResultadoTraducaoArquivo(null, "ep.ass", "DanMachi", 10, 2, 8, 0, s);
    }

    @Test
    void todosConcluidosOuParciaisViramConcluido() {
        assertEquals(StatusLoteTraducao.CONCLUIDO, StatusLoteTraducao.consolidar(List.of(
            comStatus(StatusArquivoTraducao.CONCLUIDO), comStatus(StatusArquivoTraducao.PARCIAL))));
    }

    @Test
    void misturaViraConcluidoComFalhas() {
        assertEquals(StatusLoteTraducao.CONCLUIDO_COM_FALHAS, StatusLoteTraducao.consolidar(List.of(
            comStatus(StatusArquivoTraducao.CONCLUIDO),
            comStatus(StatusArquivoTraducao.FALHOU),
            comStatus(StatusArquivoTraducao.BLOQUEADO))));
    }

    @Test
    void todasFalhasViramFalhou() {
        assertEquals(StatusLoteTraducao.FALHOU, StatusLoteTraducao.consolidar(List.of(
            comStatus(StatusArquivoTraducao.FALHOU), comStatus(StatusArquivoTraducao.BLOQUEADO))));
    }

    @Test
    void loteVazioViraFalhou() {
        assertEquals(StatusLoteTraducao.FALHOU, StatusLoteTraducao.consolidar(List.of()));
        assertEquals(StatusLoteTraducao.FALHOU, StatusLoteTraducao.consolidar(null));
    }

    @Test
    void loteInteiramenteBloqueadoViraConcluido() {
        // Rerun de um lote 100% já traduzido: todos BLOQUEADO. Nada falhou -> CONCLUIDO,
        // não FALHOU (BLOQUEADO é "já pronto", não é falha).
        assertEquals(StatusLoteTraducao.CONCLUIDO, StatusLoteTraducao.consolidar(List.of(
            comStatus(StatusArquivoTraducao.BLOQUEADO), comStatus(StatusArquivoTraducao.BLOQUEADO))));
    }

    @Test
    void concluidoComBloqueadoViraConcluido() {
        // Um arquivo novo traduzido + um já pronto (bloqueado): não há falha -> CONCLUIDO,
        // não CONCLUIDO_COM_FALHAS.
        assertEquals(StatusLoteTraducao.CONCLUIDO, StatusLoteTraducao.consolidar(List.of(
            comStatus(StatusArquivoTraducao.CONCLUIDO), comStatus(StatusArquivoTraducao.BLOQUEADO))));
    }
}
