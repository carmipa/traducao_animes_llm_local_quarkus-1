package org.traducao.projeto.traducao.infrastructure.telemetria;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.core.io.DiretorioBaseKronos;
import org.traducao.projeto.traducao.domain.FalaNaoTraduzida;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que o dataset responde "por que esta fala está em inglês?" sem
 * que ninguém precise recalcular por fora as regras que o pipeline aplicou por dentro.
 *
 * <h2>O prejuízo que originou</h2>
 * Na auditoria de 07/08/2026, recalcular por fora produziu cinco conclusões erradas — entre
 * elas 18.431 falas dadas como resíduo de tradução que eram letra de música cujo estilo o
 * achatamento havia apagado, e 2.898 dadas como perdidas que eram karaokê descartado de
 * propósito. O dado existia; faltava o pipeline declarar.
 */
class DatasetFalasNaoTraduzidasTest {

    @TempDir
    Path base;

    private TelemetriaTraducaoAdapter adapter;
    private String baseAnterior;

    @BeforeEach
    void preparar() {
        baseAnterior = System.getProperty(DiretorioBaseKronos.PROPRIEDADE_BASE);
        System.setProperty(DiretorioBaseKronos.PROPRIEDADE_BASE, base.toString());
        adapter = new TelemetriaTraducaoAdapter(new ObjectMapper());
    }

    @AfterEach
    void restaurar() {
        if (baseAnterior == null) {
            System.clearProperty(DiretorioBaseKronos.PROPRIEDADE_BASE);
        } else {
            System.setProperty(DiretorioBaseKronos.PROPRIEDADE_BASE, baseAnterior);
        }
    }

    private Path datasetDe(String obra, String base) {
        return this.base.resolve("logs").resolve("falas-nao-traduzidas").resolve(obra)
            .resolve(base + ".jsonl");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: uma linha JSON por fala, com o instante — que é a chave estável de
     * casamento com o {@code .ass} — e o motivo.
     */
    @Test
    @DisplayName("grava uma linha por fala, com instante e motivo")
    void gravaUmaLinhaPorFala() throws IOException {
        adapter.registrarFalasNaoTraduzidas(
            Path.of("saida", "Break Blade - 01_PT-BR.ass"), "Break Blade 1",
            List.of(
                new FalaNaoTraduzida(41, "0:01:37.00,0:01:39.00", "OPL2",
                    FalaNaoTraduzida.Motivo.PRESERVADA_POR_REGRA, "estilo em estilos-ignorados"),
                new FalaNaoTraduzida(575, "0:44:37.99,0:44:43.04", "Default",
                    FalaNaoTraduzida.Motivo.PENDENTE, "marcador corrompido")));

        Path dataset = datasetDe("Break Blade 1", "Break Blade - 01_PT-BR");
        assertTrue(Files.exists(dataset), "dataset nao foi gravado em " + dataset);

        List<String> linhas = Files.readAllLines(dataset);
        assertEquals(2, linhas.size(), "uma linha JSON por fala");
        assertTrue(linhas.get(0).contains("\"instante\":\"0:01:37.00,0:01:39.00\""),
            "o instante e a chave de casamento com o .ass — casar por indice mente quando o "
                + "arquivo sai reordenado. Veio: " + linhas.get(0));
        assertTrue(linhas.get(0).contains("PRESERVADA_POR_REGRA"), linhas.get(0));
        assertTrue(linhas.get(0).contains("\"estiloOriginal\":\"OPL2\""),
            "o estilo tem de ser o da ORIGEM: o achatador colapsa OP/ED em Default, e ler da "
                + "saida faria letra de musica parecer dialogo. Veio: " + linhas.get(0));
        assertTrue(linhas.get(1).contains("PENDENTE"), linhas.get(1));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: lista vazia grava arquivo VAZIO, não deixa de gravar. "Conferi e
     * nenhuma fala ficou para trás" é diferente de "não conferi", e arquivo ausente não pode
     * significar as duas coisas — é a regra 12 do método: saída vazia ambígua é bug.
     */
    @Test
    @DisplayName("lista vazia grava arquivo vazio — ausente e vazio nao podem ser a mesma coisa")
    void listaVaziaGravaArquivoVazio() throws IOException {
        adapter.registrarFalasNaoTraduzidas(
            Path.of("saida", "Perfeito_PT-BR.ass"), "Obra", List.of());

        Path dataset = datasetDe("Obra", "Perfeito_PT-BR");
        assertTrue(Files.exists(dataset),
            "sem o arquivo, 'nenhuma fala ficou para tras' e 'nao conferi' viram o mesmo sinal");
        assertEquals(0, Files.readAllLines(dataset).size());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a última execução vence, como o próprio {@code .ass}. Um dataset
     * append-only misturaria duas execuções e mentiria sobre o estado atual.
     */
    @Test
    @DisplayName("retraducao substitui o dataset, nao acumula")
    void retraducaoSubstitui() throws IOException {
        Path saida = Path.of("saida", "Ep_PT-BR.ass");
        adapter.registrarFalasNaoTraduzidas(saida, "Obra", List.of(
            new FalaNaoTraduzida(1, "0:00:01.00,0:00:02.00", "Default",
                FalaNaoTraduzida.Motivo.PENDENTE, "primeira execucao")));
        adapter.registrarFalasNaoTraduzidas(saida, "Obra", List.of(
            new FalaNaoTraduzida(2, "0:00:03.00,0:00:04.00", "Default",
                FalaNaoTraduzida.Motivo.PENDENTE, "segunda execucao")));

        List<String> linhas = Files.readAllLines(datasetDe("Obra", "Ep_PT-BR"));
        assertEquals(1, linhas.size(), "acumulou as duas execucoes: " + linhas);
        assertTrue(linhas.get(0).contains("segunda execucao"), linhas.get(0));
        assertFalse(linhas.get(0).contains("primeira execucao"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o {@code .parcial} e o definitivo do MESMO episódio compartilham o
     * dataset — senão a auditoria de um arquivo parcial não encontraria o registro dele.
     */
    @Test
    @DisplayName("parcial e definitivo do mesmo episodio compartilham o dataset")
    void parcialEDefinitivoCompartilham() throws IOException {
        adapter.registrarFalasNaoTraduzidas(
            Path.of("saida", "Ep_PT-BR.parcial.ass"), "Obra", List.of());
        assertTrue(Files.exists(datasetDe("Obra", "Ep_PT-BR")),
            "o sufixo .parcial nao pode gerar um segundo dataset");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o dataset é acessório. Perder o registro jamais pode custar a
     * legenda — por isso a porta engole a própria falha em vez de propagar.
     */
    @Test
    @DisplayName("entrada degenerada nao lanca: o dataset nunca custa a legenda")
    void entradaDegeneradaNaoLanca() {
        adapter.registrarFalasNaoTraduzidas(null, "Obra", List.of());
        adapter.registrarFalasNaoTraduzidas(Path.of("x.ass"), "Obra", null);
        adapter.registrarFalasNaoTraduzidas(Path.of("x.ass"), null, List.of());
        assertTrue(Files.exists(base), "nenhuma das chamadas acima pode lancar");
    }
}
