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
import static org.traducao.projeto.telemetria.FixtureCaminhoWindows.c;
import static org.traducao.projeto.telemetria.FixtureCaminhoWindows.marcadorDrive;

/**
 * PROPÓSITO DE NEGÓCIO: a publicação é a FRONTEIRA do que vira público, e nenhuma linha de
 * nenhum acervo a atravessa sem passar pelo sanitizador.
 *
 * <h2>O prejuízo que originou, medido em 2026-08-20</h2>
 * Varredura da mesma classe de falha depois de consertar o karaokê (regra 5): o acervo do
 * DIÁLOGO tinha <b>402 caminhos {@code C:\animes\<obra>}</b> nos avisos, <b>já publicados</b>.
 * Eles nascem da mensagem do portão obra × contexto, que cita o arquivo. O README do dataset
 * promete "nada de caminhos de máquina" e o dado não honrava — estado-alvo tratado como
 * estado-atual.
 *
 * <p>Medido com o sanitizador de produção sobre os 9.335 avisos reais: <b>402 transformados,
 * zero redigidos</b>. Nenhum diagnóstico se perde, porque de um caminho sobrevivem os dois
 * últimos segmentos — obra e arquivo, que é o que serve a quem estuda a falha.
 *
 * <h2>Invariantes do domínio congelados aqui</h2>
 * <ul>
 *   <li>Vale para os DOIS acervos: diálogo e karaokê.</li>
 *   <li>Vale para linha JÁ publicada, não só para a nova — senão uma publicação sem execução
 *       nova deixaria intacto o caminho que ela acabou de encontrar.</li>
 *   <li>O CSV derivado sai limpo pelo mesmo caminho, sem segunda implementação.</li>
 *   <li>Texto sem nada a limpar atravessa BYTE A BYTE: sanitizar não pode ser "mexer".</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nenhum efeito externo: só {@code @TempDir}. Git, clone e push não são tocados.
 */
class SanitizacaoNaFronteiraDoDatasetTest {

    @TempDir
    Path raiz;

    private String baseAnterior;
    private TelemetriaDatasetService servico;
    private Path metrics;

    @BeforeEach
    void preparar() throws IOException {
        baseAnterior = System.getProperty(DiretorioBaseKronos.PROPRIEDADE_BASE);
        System.setProperty(DiretorioBaseKronos.PROPRIEDADE_BASE, raiz.toString());
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
    @DisplayName("o caminho no aviso do DIÁLOGO não chega ao acervo publicado nem ao CSV")
    void avisoDoDialogoPerdeOCaminho() throws IOException {
        String caminho = c("animes", "86", "ep01.ass");
        gravar("logs", "telemetria_execucoes.jsonl",
            execucao("2026-08-20T10:00:00Z", "ep01.ass",
                "Obra do arquivo NAO confere com o contexto — BLOQUEADA. Arquivo: " + caminho));

        assertEquals(1, servico.acumularExecucoes(metrics));
        String acervo = Files.readString(metrics.resolve(TelemetriaDatasetService.NOME_ARQUIVO_EXECUCOES),
            StandardCharsets.UTF_8);
        assertFalse(acervo.contains(marcadorDrive('C')),
            "o caminho da maquina chegou ao acervo PUBLICADO: " + acervo);
        // Sanitizar nao e apagar: a obra e o arquivo sobrevivem, que e o que serve a pesquisa.
        assertTrue(acervo.contains("86/ep01.ass"), "a cauda util se perdeu: " + acervo);
        assertTrue(acervo.contains("BLOQUEADA"), "o diagnostico se perdeu: " + acervo);

        servico.publicarCsv(metrics, new ObjectMapper().createObjectNode());
        String csv = Files.readString(metrics.resolve("csv").resolve("kronos-avisos.csv"),
            StandardCharsets.UTF_8);
        assertFalse(csv.contains(marcadorDrive('C')), "o caminho chegou ao CSV publicado: " + csv);
    }

    /**
     * O caso que a decisão de "sanitizar só o que entra" deixaria passar: o acervo já publicado
     * está sujo e NÃO há execução nova. Sem limpar o que já está lá, a publicação seguinte
     * encontraria o caminho e o deixaria intacto — para sempre.
     */
    @Test
    @DisplayName("linha JÁ publicada também é limpa, mesmo sem execução nova")
    void linhaJaPublicadaTambemELimpa() throws IOException {
        String sujo = execucao("2026-08-20T10:00:00Z", "ep01.ass",
            "BLOQUEADA. Arquivo: " + c("animes", "86", "ep01.ass"));
        // Simula o estado real: a linha suja ja esta no acervo publicado, e nao ha historico local.
        Files.writeString(metrics.resolve(TelemetriaDatasetService.NOME_ARQUIVO_EXECUCOES),
            sujo + System.lineSeparator(), StandardCharsets.UTF_8);

        servico.acumularExecucoes(metrics);

        String acervo = Files.readString(metrics.resolve(TelemetriaDatasetService.NOME_ARQUIVO_EXECUCOES),
            StandardCharsets.UTF_8);
        assertFalse(acervo.contains(marcadorDrive('C')),
            "o que ja estava publicado continuou sujo: " + acervo);
    }

    @Test
    @DisplayName("o aviso do KARAOKÊ passa pela mesma fronteira")
    void avisoDoKaraokeTambemPassa() throws IOException {
        String caminho = c("animes", "86", "op01.ass");
        gravar("logs", TelemetriaDatasetService.NOME_ARQUIVO_KARAOKE_LOCAL,
            karaoke("2026-08-20T10:00:00Z", "op01.ass", "Marcador perdido em: " + caminho));

        assertEquals(1, servico.acumularExecucoesKaraoke(metrics));
        String acervo = Files.readString(metrics.resolve(TelemetriaDatasetService.NOME_ARQUIVO_KARAOKE),
            StandardCharsets.UTF_8);
        assertFalse(acervo.contains(marcadorDrive('C')),
            "o caminho chegou ao acervo de karaoke publicado: " + acervo);
        assertTrue(acervo.contains("86/op01.ass"),
            "a cauda util do karaoke se perdeu — sanitizar virou apagar: " + acervo);
    }

    /**
     * Contra-teste indispensável: sem ele, um sanitizador que devolvesse {@code "[redigido]"} para
     * tudo passaria nos três testes acima. "Limpou" e "destruiu" não podem ficar indistinguíveis.
     */
    @Test
    @DisplayName("linha sem nada a limpar atravessa BYTE A BYTE")
    void linhaLimpaAtravessaIntacta() throws IOException {
        String limpa = execucao("2026-08-20T10:00:00Z", "ep02.ass",
            "Fala mantida sem traducao (tags corrompidas pelo LLM): {\\i1}Gundam ZZ:\\N\"Cecilia.\"");
        gravar("logs", "telemetria_execucoes.jsonl", limpa);

        servico.acumularExecucoes(metrics);

        List<String> acervo = Files.readAllLines(
            metrics.resolve(TelemetriaDatasetService.NOME_ARQUIVO_EXECUCOES), StandardCharsets.UTF_8);
        assertEquals(List.of(limpa), acervo,
            "linha sem caminho nenhum foi reescrita — sanitizar virou mexer");
    }

    private void gravar(String pasta, String nome, String... linhas) throws IOException {
        Path destino = raiz.resolve(pasta);
        Files.createDirectories(destino);
        Files.writeString(destino.resolve(nome),
            String.join(System.lineSeparator(), linhas) + System.lineSeparator(),
            StandardCharsets.UTF_8);
    }

    private static String execucao(String quando, String episodio, String aviso) {
        return "{\"nomeEpisodio\":\"" + episodio + "\",\"animeNome\":\"86\",\"temporada\":\"1\","
            + "\"loreNome\":\"86\",\"modeloLlm\":\"aya\",\"statusFinal\":\"CONCLUIDO\","
            + "\"totalLinhas\":10,\"falasTraduzidas\":9,\"falasDoCache\":0,\"tempoTotalMs\":5,"
            + "\"registradoEm\":\"" + quando + "\",\"errosOcorridos\":[\"" + escapar(aviso) + "\"]}";
    }

    private static String karaoke(String quando, String arquivo, String aviso) {
        return "{\"registradoEm\":\"" + quando + "\",\"arquivo\":\"" + arquivo + "\","
            + "\"desfechoArquivo\":\"TRADUZIDO\",\"motivoFalha\":null,"
            + "\"statusExecucao\":\"COMPLETA\",\"motivoExecucao\":null,"
            + "\"contextoId\":\"eight_six\",\"contextoNome\":\"86\",\"contextoHash\":\"h\","
            + "\"modeloLlm\":\"aya\",\"cacheIgnorado\":false,\"estadoDicionario\":\"DISPONIVEL\","
            + "\"duracaoExecucaoMs\":1,\"arquivosNaExecucao\":1,\"eventosTotais\":1,"
            + "\"efeitosKfxPreservados\":0,\"preservadasOriginalJapones\":0,\"jaEmPortugues\":0,"
            + "\"paraTraduzir\":1,\"reaproveitadasCache\":0,\"traduzidas\":1,"
            + "\"mantidasSemTraducao\":0,\"acentosRepostos\":0,\"entradasCacheDescartadas\":0,"
            + "\"avisos\":[\"" + escapar(aviso) + "\"]}";
    }

    /** Escapa para JSON o que o teste monta a mao — barra invertida e aspas. */
    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
