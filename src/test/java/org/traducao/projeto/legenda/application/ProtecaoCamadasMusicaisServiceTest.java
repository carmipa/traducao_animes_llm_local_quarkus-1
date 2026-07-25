package org.traducao.projeto.legenda.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.application.ProtecaoCamadasMusicaisService.ProtecaoCamadas;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O pré-passe visto pela estrutura REAL do arquivo: prefixo de evento ASS como o leitor entrega,
 * duas camadas no mesmo tempo (romaji e tradução) e diálogo comum no meio.
 */
class ProtecaoCamadasMusicaisServiceTest {

    private final ProtecaoCamadasMusicaisService servico =
        new ProtecaoCamadasMusicaisService(new DetectorEfeitoKaraokeService());

    private static EventoLegenda evento(int indice, String estilo, String inicio, String fim, String texto) {
        String prefixo = "Dialogue: 0," + inicio + "," + fim + "," + estilo + ",,0,0,0,,";
        return new EventoLegenda(indice, "Dialogue", estilo, prefixo, texto);
    }

    private static DocumentoLegenda documento(EventoLegenda... eventos) {
        return new DocumentoLegenda("[Script Info]", List.of(eventos), "\n", false);
    }

    @Test
    void protegeACamadaRomajiDoParEDeixaAOutraTraduzivel() {
        ProtecaoCamadas protecao = servico.calcular(documento(
            evento(10, "ED_S2_roma", "0:01:23.45", "0:01:26.78", "Sonna sekai ni nokosareta boku wa"),
            evento(11, "ED_S2", "0:01:23.40", "0:01:26.90", "In this world I was left behind")));

        assertEquals(1, protecao.paresEncontrados());
        assertEquals(0, protecao.paresIndecisos());
        assertTrue(protecao.protege(10), "a camada romaji é a original e não pode ser traduzida");
        assertFalse(protecao.protege(11), "a camada em inglês continua sendo o alvo da tradução");
    }

    @Test
    void dialogoComumNuncaEntraNoPareamento() {
        ProtecaoCamadas protecao = servico.calcular(documento(
            evento(1, "Default", "0:00:10.00", "0:00:12.00", "What are you doing here?!"),
            evento(2, "Default", "0:00:10.10", "0:00:12.10", "Nada, só passando."),
            evento(3, "Default", "0:00:13.00", "0:00:15.00", "Sai daqui!")));

        assertEquals(ProtecaoCamadas.VAZIA, protecao);
        assertTrue(protecao.vazia());
    }

    @Test
    void musicaEmInglesSemContraparteRomajiNaoProtegeNinguem() {
        // Duas camadas em inglês no mesmo tempo (contorno + preenchimento): empate de conteúdo,
        // ninguém é declarado romaji, e o par fica INDECISO em vez de chutar.
        ProtecaoCamadas protecao = servico.calcular(documento(
            evento(20, "Opening", "0:00:30.00", "0:00:33.00", "But no matter how bad a fight we'd have"),
            evento(21, "Opening", "0:00:30.00", "0:00:33.00", "But no matter how bad a fight we would")));

        assertEquals(1, protecao.paresEncontrados());
        assertEquals(1, protecao.paresIndecisos());
        assertTrue(protecao.indicesPreservados().isEmpty());
    }

    @Test
    void eventoSemJanelaDeTempoLegivelEIgnorado() {
        EventoLegenda semTempo = new EventoLegenda(30, "Dialogue", "ED_S2_roma",
            "Dialogue: 0,sem tempo aqui,", "Sonna sekai ni nokosareta boku wa");
        EventoLegenda parceiro = evento(31, "ED_S2", "0:01:23.40", "0:01:26.90", "In this world");

        ProtecaoCamadas protecao = servico.calcular(documento(semTempo, parceiro));

        assertEquals(ProtecaoCamadas.VAZIA, protecao);
    }

    @Test
    void entradasDegeneradasDevolvemProtecaoVazia() {
        assertEquals(ProtecaoCamadas.VAZIA, servico.calcular(null));
        assertEquals(ProtecaoCamadas.VAZIA, servico.calcular(documento()));
        assertEquals(ProtecaoCamadas.VAZIA, servico.calcular(documento(
            evento(1, "ED_S2_roma", "0:01:23.45", "0:01:26.78", "Sonna sekai ni nokosareta"))));
    }

    @Test
    void protecaoVaziaNaoProtegeNenhumIndice() {
        assertFalse(ProtecaoCamadas.VAZIA.protege(0));
        assertFalse(ProtecaoCamadas.VAZIA.protege(999));
        assertTrue(ProtecaoCamadas.VAZIA.vazia());
    }
}
