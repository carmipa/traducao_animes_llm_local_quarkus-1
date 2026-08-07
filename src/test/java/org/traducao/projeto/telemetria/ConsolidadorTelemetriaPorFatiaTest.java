package org.traducao.projeto.telemetria;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.core.io.DiretorioBaseKronos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que a telemetria espalhada em {@code relatorios/}
 * é reunida por fatia, sanitizada, sem duplicar e sem destruir a origem.
 *
 * <h2>O prejuízo que originou</h2>
 * Medido no acervo em 06/08/2026: das 6.601 operações registradas, <b>6.488
 * estavam em 21 pastas de relatório que o publicador nunca varreu</b>, e só 85
 * chegaram ao repositório público — 1,3%.
 */
class ConsolidadorTelemetriaPorFatiaTest {

    private final ConsolidadorTelemetriaPorFatia consolidador = new ConsolidadorTelemetriaPorFatia();
    private final ObjectMapper mapper = new ObjectMapper();

    private Path logs;
    private Path relatorios;

    @BeforeEach
    void limparArvore() throws IOException {
        logs = DiretorioBaseKronos.resolver("logs");
        relatorios = DiretorioBaseKronos.resolver("relatorios");
        apagar(logs);
        apagar(relatorios);
        Files.createDirectories(logs);
        Files.createDirectories(relatorios);
    }

    private static void apagar(Path raiz) throws IOException {
        if (!Files.exists(raiz)) {
            return;
        }
        try (Stream<Path> s = Files.walk(raiz)) {
            for (Path p : s.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }

    /** Escreve um telemetria_compartilhada.json com as operações informadas. */
    private void escrever(Path pasta, String... tipoDetalheInstante) throws IOException {
        Files.createDirectories(pasta);
        StringBuilder ops = new StringBuilder();
        for (int i = 0; i < tipoDetalheInstante.length; i += 3) {
            if (ops.length() > 0) {
                ops.append(',');
            }
            ops.append("""
                {"tipo":"%s","detalhe":"%s","tempoTotalMs":10,"arquivosProcessados":1,
                 "itensDetectados":2,"itensCorrigidos":3,"registradoEm":"%s"}"""
                .formatted(tipoDetalheInstante[i],
                    tipoDetalheInstante[i + 1].replace("\\", "\\\\"),
                    tipoDetalheInstante[i + 2]));
        }
        Files.writeString(pasta.resolve("telemetria_compartilhada.json"),
            "{\"operacoes\":[" + ops + "]}", StandardCharsets.UTF_8);
    }

    private JsonNode lerFatia(String fatia) throws IOException {
        return mapper.readTree(logs.resolve("telemetria_fatia_" + fatia + ".json").toFile());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o caso central. Operações de pastas diferentes se
     * juntam na fatia certa.
     */
    @Test
    @DisplayName("reune operacoes de varias pastas na fatia correspondente")
    void reuneDeVariasPastas() throws IOException {
        escrever(relatorios.resolve("obra-a"),
            "Auditoria de Conteudo de Legendas", "sem caminho", "2026-08-01T10:00:00");
        escrever(relatorios.resolve("obra-b"),
            "Auditoria de Conteudo (.ass)", "outro", "2026-08-01T11:00:00",
            "Limpeza de Cache", "terceiro", "2026-08-01T12:00:00");

        var r = consolidador.consolidar();

        assertEquals(2, r.arquivosLidos());
        assertEquals(3, r.operacoesLidas());
        assertEquals(3, r.operacoesGravadas());
        assertEquals(2, lerFatia("auditoria").get("operacoes").size());
        assertEquals(1, lerFatia("cache").get("operacoes").size());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: é o motivo de sanitizar na consolidação e não na
     * publicação — consolidar sem limpar seria juntar num só lugar os 4.532
     * caminhos absolutos que os arquivos do acervo carregam.
     */
    @Test
    @DisplayName("o detalhe e sanitizado ao consolidar, nao depois")
    void detalheSanitizadoNaConsolidacao() throws IOException {
        escrever(relatorios.resolve("obra"),
            "Limpeza de Cache", "C:\\animes\\[Joseki] Gundam 0083\\cache | 15 arquivos",
            "2026-08-01T10:00:00");

        consolidador.consolidar();

        String detalhe = lerFatia("cache").get("operacoes").get(0).get("detalhe").asText();
        assertFalse(detalhe.contains("C:\\"), "caminho absoluto nao pode entrar no consolidado");
        assertTrue(detalhe.contains("[Joseki]"), "grupo de release FICA — e o valor do dado");
        assertTrue(detalhe.contains("15 arquivos"), "a medicao nao sai junto com o caminho");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a MESMA operação aparece em mais de um arquivo — a
     * cópia local fica na pasta da obra e a mesma execução também é registrada em
     * {@code logs/}. Consolidar sem deduplicar contaria o trabalho duas vezes, e
     * o dataset publicaria um número que nunca existiu.
     *
     * <p>A primeira versão deste teste tinha UMA operação só e passava verde
     * mesmo com a deduplicação removida — não testava nada. A chave de ocorrência
     * é tipo + instante + detalhe, e é entre ARQUIVOS que ela precisa valer.
     */
    @Test
    @DisplayName("a MESMA operacao em dois arquivos e contada UMA vez")
    void deduplicaEntreArquivos() throws IOException {
        escrever(logs,
            "Limpeza de Cache", "mesmo detalhe", "2026-08-01T10:00:00");
        escrever(relatorios.resolve("obra"),
            "Limpeza de Cache", "mesmo detalhe", "2026-08-01T10:00:00",
            "Limpeza de Cache", "outro detalhe", "2026-08-01T11:00:00");

        var r = consolidador.consolidar();

        assertEquals(3, r.operacoesLidas(), "tres registros brutos foram lidos");
        assertEquals(2, r.operacoesGravadas(), "a repetida tem de colapsar numa so");
        assertEquals(2, lerFatia("cache").get("operacoes").size());
    }

    /** Rodar de novo sobre a mesma origem não pode somar cópia ao destino. */
    @Test
    @DisplayName("consolidar duas vezes deixa o destino igual")
    void rodarDuasVezesNaoInfla() throws IOException {
        escrever(relatorios.resolve("obra"),
            "Limpeza de Cache", "detalhe", "2026-08-01T10:00:00");

        consolidador.consolidar();
        var segunda = consolidador.consolidar();

        assertEquals(1, segunda.operacoesGravadas(), "a segunda passada nao pode somar copia");
        assertEquals(1, lerFatia("cache").get("operacoes").size());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a origem é a cópia local que fica ao lado do
     * resultado, e continua servindo para conferência. Consolidação que apaga a
     * fonte transforma erro de agrupamento em perda de dado.
     */
    @Test
    @DisplayName("NAO destroi o arquivo de origem")
    void naoDestroiOrigem() throws IOException {
        Path obra = relatorios.resolve("obra");
        escrever(obra, "Limpeza de Cache", "detalhe", "2026-08-01T10:00:00");
        Path origem = obra.resolve("telemetria_compartilhada.json");
        long antes = Files.size(origem);

        consolidador.consolidar();

        assertTrue(Files.exists(origem), "a origem tem de continuar existindo");
        assertEquals(antes, Files.size(origem), "a origem nao pode ser reescrita");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: um JSON corrompido numa pasta de obra não pode
     * impedir a publicação das outras vinte — mas também não pode passar
     * despercebido.
     */
    @Test
    @DisplayName("arquivo ilegivel e pulado, contabilizado, e nao derruba o resto")
    void ilegivelEhPuladoEContabilizado() throws IOException {
        escrever(relatorios.resolve("boa"), "Limpeza de Cache", "ok", "2026-08-01T10:00:00");
        Path ruim = relatorios.resolve("ruim");
        Files.createDirectories(ruim);
        Files.writeString(ruim.resolve("telemetria_compartilhada.json"), "{ isto nao e json ");

        var r = consolidador.consolidar();

        assertEquals(1, r.arquivosLidos());
        assertEquals(1, r.arquivosPulados(), "o pulado tem de ser CONTADO, nao esquecido");
        assertEquals(1, r.operacoesGravadas(), "a pasta boa foi consolidada mesmo assim");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: tipo sem fatia mapeada continua no consolidado, em
     * {@code outros}. Descartar por falta de classificação é perder dado em
     * silêncio.
     */
    @Test
    @DisplayName("tipo sem fatia vai para outros e CONTINUA no consolidado")
    void tipoSemFatiaVaiParaOutros() throws IOException {
        escrever(relatorios.resolve("obra"),
            "Operacao Que Ninguem Mapeou", "detalhe", "2026-08-01T10:00:00");

        var r = consolidador.consolidar();

        assertEquals(1, r.operacoesGravadas());
        assertEquals(1, lerFatia(FatiaTelemetria.OUTROS).get("operacoes").size());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: os 113 registros de {@code logs/} também entram —
     * esquecê-los repetiria em menor escala o defeito que motivou tudo isto.
     */
    @Test
    @DisplayName("o telemetria_compartilhada de logs/ tambem e consolidado")
    void incluiOArquivoDeLogs() throws IOException {
        escrever(logs, "Renomear Arquivos", "detalhe", "2026-08-01T10:00:00");

        var r = consolidador.consolidar();

        assertEquals(1, r.arquivosLidos());
        assertEquals(1, lerFatia("arquivos").get("operacoes").size());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: operação vinda de execução de TESTE não pode entrar
     * no dataset público.
     *
     * <p>Medido em 07/08/2026: 285 das 775 operações distintas do acervo (36,8%)
     * apontavam para diretório temporário de JUnit — resíduo de execuções
     * anteriores ao {@code DiretorioBaseKronos}, que existe para impedir que a
     * suíte contamine os diretórios reais. O sanitizador já as redigia, então não
     * vazavam caminho; mas publicar 36,8% de {@code [redigido]} vindo de teste é
     * publicar ruído como se fosse medição.
     *
     * <p>São DESCARTADAS e CONTADAS — nunca descartadas em silêncio.
     */
    @Test
    @DisplayName("residuo de execucao de teste e descartado, e contado")
    void residuoDeTesteEhDescartadoEContado() throws IOException {
        escrever(relatorios.resolve("obra"),
            "Auditoria de Conteudo (.ass)",
            "C:\\Users\\Paulo\\AppData\\Local\\Temp\\junit-10410939766033011663\\ep02_pt.ass | anomalias=1",
            "2026-08-01T10:00:00",
            "Auditoria de Conteudo (.ass)", "trabalho de verdade | anomalias=3", "2026-08-01T11:00:00");

        var r = consolidador.consolidar();

        assertEquals(2, r.operacoesLidas());
        assertEquals(1, r.operacoesDeTeste(), "o residuo tem de ser CONTADO, nao esquecido");
        assertEquals(1, r.operacoesGravadas(), "so o trabalho de verdade entra no consolidado");
        assertTrue(lerFatia("auditoria").get("operacoes").get(0).get("detalhe").asText()
            .contains("trabalho de verdade"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CONTRA-TESTE do filtro. Sem ele, "descartar resíduo
     * de teste" poderia virar "descartar tudo que tem número comprido no caminho"
     * e levar junto trabalho real.
     */
    @Test
    @DisplayName("detalhe comum com numero NAO e confundido com residuo de teste")
    void detalheComumNaoEhConfundidoComTeste() throws IOException {
        escrever(relatorios.resolve("obra"),
            "Limpeza de Cache", "[Joseki] Gundam 0083 (1991) | 15 arquivos | 20260801",
            "2026-08-01T10:00:00");

        var r = consolidador.consolidar();

        assertEquals(0, r.operacoesDeTeste());
        assertEquals(1, r.operacoesGravadas());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CONTRA-TESTE. Acervo sem relatório nenhum é estado
     * legítimo — não pode lançar nem produzir arquivo fantasma.
     */
    @Test
    @DisplayName("sem nenhum arquivo de origem, consolida zero sem lancar")
    void semOrigemNaoLanca() throws IOException {
        var r = consolidador.consolidar();

        assertEquals(0, r.arquivosLidos());
        assertEquals(0, r.operacoesGravadas());
        assertTrue(r.porFatia().isEmpty());
    }
}
