package org.traducao.projeto.telemetria;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetriaServiceRevisaoLoreTest {

    @Test
    void ehRevisaoLoreReconheceTipoOficial() {
        assertTrue(TelemetriaService.ehRevisaoLore("Revisao de Lore (.ass LLM)"));
        assertFalse(TelemetriaService.ehRevisaoLore("Revisão de Legendas"));
    }

    @Test
    void revisaoLoreSemArquivosNaoContaComoSessaoComTrabalho() {
        OperacaoTelemetria vazia = new OperacaoTelemetria(
            "Revisao de Lore (.ass LLM)",
            "cache | promptRevisaoLore=DanMachi",
            48L,
            0,
            0,
            0,
            "2026-07-05T22:31:02Z"
        );

        assertFalse(TelemetriaService.ehRevisaoLoreComTrabalho(vazia));
    }

    @Test
    void revisaoLoreComArquivoContaComoSessaoComTrabalho() {
        OperacaoTelemetria comTrabalho = new OperacaoTelemetria(
            "Revisao de Lore (.ass LLM)",
            "traducao-ptbr | promptRevisaoLore=Gundam",
            120_000L,
            1,
            396,
            87,
            "2026-07-05T22:31:02Z"
        );

        assertTrue(TelemetriaService.ehRevisaoLoreComTrabalho(comTrabalho));
    }
}
