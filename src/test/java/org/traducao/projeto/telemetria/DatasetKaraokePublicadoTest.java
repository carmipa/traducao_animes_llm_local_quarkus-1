package org.traducao.projeto.telemetria;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.core.io.DiretorioBaseKronos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fecha o outro lado do caminho — a fatia escreve o acervo, e AQUI se prova
 * que o publicador o transforma em arquivo do dataset e em tabela CSV.
 *
 * <h2>Por que não bastavam os testes da fatia</h2>
 * Escrever o acervo e publicá-lo são responsabilidades de módulos que, de propósito, não se
 * conhecem: a fatia grava em {@code logs/}, o publicador varre {@code logs/}. Um teste de cada
 * lado deixa exatamente o meio sem prova — foi assim que o karaokê passou a existir em manifesto
 * desde sempre e nunca ter chegado ao dataset. Este teste percorre o meio.
 *
 * <h2>Invariantes do domínio congelados aqui</h2>
 * <ul>
 *   <li>O acervo local vira {@code metrics/kronos-karaoke-execucoes.jsonl}.</li>
 *   <li>Republicar NÃO duplica linha: a chave é {@code registradoEm + arquivo}.</li>
 *   <li>Sai tabela por ARQUIVO e tabela tidy por AVISO, e a segunda liga na primeira.</li>
 *   <li>A tabela principal traz a CONTAGEM de avisos, nunca o texto — a mesma regra do diálogo.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nenhum efeito externo: só {@code @TempDir}. Git, clone e push não são tocados.
 */
class DatasetKaraokePublicadoTest {

    @TempDir
    Path raiz;

    private String baseAnterior;
    private TelemetriaDatasetService servico;
    private Path metrics;

    @BeforeEach
    void preparar() throws IOException {
        baseAnterior = System.getProperty(DiretorioBaseKronos.PROPRIEDADE_BASE);
        System.setProperty(DiretorioBaseKronos.PROPRIEDADE_BASE, raiz.toString());
        // Colaboradores nulos: nenhum dos dois métodos exercitados aqui os usa. Construir o
        // grafo inteiro para testar leitura de arquivo seria acoplar o teste ao que ele não mede.
        servico = new TelemetriaDatasetService(null, null, null, null);
        metrics = raiz.resolve("metrics");
        Files.createDirectories(metrics);
    }

    @AfterEach
    void restaurar() {
        if (baseAnterior == null) {
            System.clearProperty(DiretorioBaseKronos.PROPRIEDADE_BASE);
        } else {
            System.setProperty(DiretorioBaseKronos.PROPRIEDADE_BASE, baseAnterior);
        }
    }

    @Test
    @DisplayName("o acervo local do karaokê vira arquivo do dataset, e republicar não duplica")
    void acervoLocalViraArquivoDoDatasetSemDuplicar() throws IOException {
        gravarAcervoLocal(
            linha("2026-08-20T10:00:00Z", "ep01.ass", "TRADUZIDO", 40, "[]"),
            linha("2026-08-20T10:00:00Z", "ep02.ass", "FALHOU", 0, "[]"));

        assertEquals(2, servico.acumularExecucoesKaraoke(metrics),
            "as linhas do karaoke nao chegaram ao acervo publicado");
        Path publicado = metrics.resolve(TelemetriaDatasetService.NOME_ARQUIVO_KARAOKE);
        assertEquals(2, linhasDe(publicado).size());

        // Republicar sem nada novo: o acervo continua com duas linhas.
        assertEquals(0, servico.acumularExecucoesKaraoke(metrics));
        assertEquals(2, linhasDe(publicado).size(), "republicar duplicou linha do acervo");

        // Uma execução nova ACRESCENTA, nunca substitui.
        gravarAcervoLocal(
            linha("2026-08-20T10:00:00Z", "ep01.ass", "TRADUZIDO", 40, "[]"),
            linha("2026-08-20T10:00:00Z", "ep02.ass", "FALHOU", 0, "[]"),
            linha("2026-08-21T09:00:00Z", "ep01.ass", "TRADUZIDO", 55, "[]"));
        assertEquals(1, servico.acumularExecucoesKaraoke(metrics));
        assertEquals(3, linhasDe(publicado).size());
    }

    @Test
    @DisplayName("saem as duas tabelas CSV do karaokê — por arquivo e tidy por aviso")
    void geraAsDuasTabelasCsv() throws IOException {
        gravarAcervoLocal(
            linha("2026-08-20T10:00:00Z", "ep01.ass", "TRADUZIDO", 40,
                "[\"alucinacao detectada; letra mantida\",\"marcador perdido\"]"),
            linha("2026-08-20T10:00:00Z", "ep02.ass", "FALHOU", 0, "[]"));
        servico.acumularExecucoesKaraoke(metrics);

        servico.publicarCsv(metrics, new ObjectMapper().createObjectNode());

        List<String> porArquivo = linhasDe(metrics.resolve("csv").resolve("kronos-karaoke.csv"));
        assertEquals(3, porArquivo.size(), "esperado cabecalho + 2 arquivos: " + porArquivo);
        assertTrue(porArquivo.get(0).startsWith("registradoEm,arquivo,desfechoArquivo"),
            "cabecalho fora de ordem: " + porArquivo.get(0));
        assertTrue(porArquivo.stream().anyMatch(l -> l.contains("ep02.ass") && l.contains("FALHOU")),
            "o arquivo que FALHOU nao aparece na tabela: " + porArquivo);
        // A tabela principal leva a CONTAGEM, nunca o texto do aviso — regra do dataset.
        assertTrue(porArquivo.stream().anyMatch(l -> l.contains("ep01.ass") && l.endsWith(",2")),
            "quantidadeAvisos nao chegou na linha do ep01: " + porArquivo);
        assertFalse(String.join("", porArquivo).contains("alucinacao detectada"),
            "texto de aviso vazou para a tabela agregada");

        List<String> avisos = linhasDe(metrics.resolve("csv").resolve("kronos-karaoke-avisos.csv"));
        assertEquals(3, avisos.size(), "esperado cabecalho + 2 avisos: " + avisos);
        assertEquals("registradoEm,arquivo,contextoNome,ordem,aviso", avisos.get(0));
        assertTrue(avisos.get(1).contains("ep01.ass") && avisos.get(1).contains(",1,"),
            "a ordem do aviso dentro da execucao se perdeu: " + avisos.get(1));
        assertTrue(avisos.get(2).contains("marcador perdido"));
    }

    /**
     * Caso-controle da regra "saída vazia ambígua é bug": sem acervo nenhum a publicação não pode
     * explodir nem inventar linha — ela gera as tabelas VAZIAS, com cabeçalho. Tabela ausente e
     * tabela sem linhas diriam a mesma coisa para quem consome, e não dizem.
     */
    @Test
    @DisplayName("sem acervo de karaokê, as tabelas saem vazias — com cabeçalho, nunca ausentes")
    void semAcervoAsTabelasSaemVaziasComCabecalho() throws IOException {
        assertEquals(0, servico.acumularExecucoesKaraoke(metrics));
        servico.publicarCsv(metrics, new ObjectMapper().createObjectNode());

        List<String> porArquivo = linhasDe(metrics.resolve("csv").resolve("kronos-karaoke.csv"));
        assertEquals(1, porArquivo.size(), "tabela vazia deveria ter so o cabecalho: " + porArquivo);
        assertTrue(Files.exists(metrics.resolve("csv").resolve("kronos-karaoke-avisos.csv")));
    }

    private void gravarAcervoLocal(String... linhas) throws IOException {
        Path logs = raiz.resolve("logs");
        Files.createDirectories(logs);
        Files.writeString(logs.resolve(TelemetriaDatasetService.NOME_ARQUIVO_KARAOKE_LOCAL),
            String.join(System.lineSeparator(), linhas) + System.lineSeparator(),
            StandardCharsets.UTF_8);
    }

    private static List<String> linhasDe(Path arquivo) throws IOException {
        return Files.readAllLines(arquivo, StandardCharsets.UTF_8).stream()
            .filter(l -> !l.isBlank()).toList();
    }

    private static String linha(String quando, String arquivo, String desfecho, int traduzidas,
            String avisosJson) {
        return "{\"registradoEm\":\"" + quando + "\",\"arquivo\":\"" + arquivo
            + "\",\"desfechoArquivo\":\"" + desfecho + "\",\"motivoFalha\":null,"
            + "\"statusExecucao\":\"COMPLETA\",\"motivoExecucao\":null,"
            + "\"contextoId\":\"eight_six\",\"contextoNome\":\"86 (Eighty-Six)\","
            + "\"contextoHash\":\"abc123\",\"modeloLlm\":\"aya-expanse-8b\","
            + "\"cacheIgnorado\":false,\"estadoDicionario\":\"DISPONIVEL\","
            + "\"duracaoExecucaoMs\":1000,\"arquivosNaExecucao\":2,"
            + "\"eventosTotais\":900,\"efeitosKfxPreservados\":400,"
            + "\"preservadasOriginalJapones\":120,\"jaEmPortugues\":5,\"paraTraduzir\":60,"
            + "\"reaproveitadasCache\":20,\"traduzidas\":" + traduzidas
            + ",\"mantidasSemTraducao\":3,\"acentosRepostos\":7,"
            + "\"entradasCacheDescartadas\":2,\"avisos\":" + avisosJson + "}";
    }
}
