package org.traducao.projeto.trocaTipoLegenda.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.trocaTipoLegenda.domain.AuditoriaLegendaResultado;
import org.traducao.projeto.trocaTipoLegenda.domain.EntradaAuditoriaTrocaFonte;
import org.traducao.projeto.trocaTipoLegenda.domain.ResultadoGeralAuditoria;
import org.traducao.projeto.trocaTipoLegenda.domain.ResultadoTrocaFonte;
import org.traducao.projeto.trocaTipoLegenda.infrastructure.TrocaTipoLegendaAuditoriaCache;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: valida auditoria e substituição de fontes sem acessar
 * backups ou relatórios reais do projeto.
 * <p>INVARIANTES DO DOMÍNIO: todos os artefatos ficam sob diretórios temporários.
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer acesso à raiz real reprova os testes.
 */
class TrocaTipoLegendaUseCaseTest {

    private TrocaTipoLegendaUseCase useCase;
    private Path tempDirBackups;

    private static class TelemetriaServiceStub extends TelemetriaService {
        boolean operacaoRegistrada = false;

        @Override
        public void init() {
        }

        @Override
        public synchronized void registrarOperacao(OperacaoTelemetria operacao) {
            this.operacaoRegistrada = true;
        }
    }

    private static class AuditoriaCacheStub extends TrocaTipoLegendaAuditoriaCache {
        int registros = 0;

        public AuditoriaCacheStub() {
            super(new ObjectMapper());
        }

        @Override
        public synchronized void registrar(EntradaAuditoriaTrocaFonte entrada) {
            registros++;
        }
    }

    private final TelemetriaServiceStub telemetriaStub = new TelemetriaServiceStub();
    private final AuditoriaCacheStub cacheStub = new AuditoriaCacheStub();

    /**
     * PROPÓSITO DE NEGÓCIO: compõe cada teste com backup isolado pelo JUnit.
     * <p>INVARIANTES DO DOMÍNIO: nenhuma sessão usa `Path.of("backups")`.
     * <p>COMPORTAMENTO EM CASO DE FALHA: erro de preparação interrompe o teste.
     */
    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        LeitorLegendaAss leitor = new LeitorLegendaAss();
        EscritorLegendaAss escritor = new EscritorLegendaAss();
        AuditoriaFontesService auditoriaService = new AuditoriaFontesService();
        
        tempDirBackups = tempDir.resolve("backups");
        Files.createDirectories(tempDirBackups);
        useCase = TrocaTipoLegendaUseCase.criarParaTeste(
            leitor, escritor, auditoriaService, telemetriaStub, cacheStub, tempDirBackups);
    }

    @Test
    void escanearEIdentificarProblemasNoLote(@TempDir Path tempDir) throws IOException {
        criarLegendaDeTeste(tempDir, "legenda1.ass", ".VnBook-Antiqua");
        criarLegendaDeTeste(tempDir, "legenda2.ass", "Arial");

        ResultadoGeralAuditoria resultado = useCase.escanear(tempDir);

        assertEquals(2, resultado.totalArquivosAnalisados());
        assertEquals(1, resultado.totalComProblemas());
        
        List<AuditoriaLegendaResultado> arquivos = resultado.arquivos();
        assertEquals(2, arquivos.size());

        AuditoriaLegendaResultado arq1 = arquivos.stream()
            .filter(a -> a.arquivo().equals("legenda1.ass"))
            .findFirst().orElseThrow();
        assertTrue(arq1.temProblemas());
        assertEquals(".VnBook-Antiqua", arq1.fontes().get(1).fonteAtual());

        AuditoriaLegendaResultado arq2 = arquivos.stream()
            .filter(a -> a.arquivo().equals("legenda2.ass"))
            .findFirst().orElseThrow();
        assertFalse(arq2.temProblemas());
    }

    @Test
    void aplicarSubstituicaoComBackupERelatorio(@TempDir Path tempDir) throws IOException {
        criarLegendaDeTeste(tempDir, "legenda1.ass", ".VnBook-Antiqua");
        criarLegendaDeTeste(tempDir, "legenda2.ass", "Arial");

        ResultadoTrocaFonte resultado = useCase.aplicar(tempDir);

        assertEquals(2, resultado.totalAnalisados());
        assertEquals(1, resultado.totalAlterados());
        assertEquals(1, resultado.totalSubstituicoes());
        assertNotNull(resultado.pastaBackup());
        assertNotNull(resultado.caminhoRelatorioJson());

        // Verificar que o arquivo corrigido agora contém a nova fonte (Arial como padrão para animes)
        String conteudoCorrigido = Files.readString(tempDir.resolve("legenda1.ass"), StandardCharsets.UTF_8);
        assertTrue(conteudoCorrigido.contains("Arial"));
        assertFalse(conteudoCorrigido.contains(".VnBook-Antiqua"));

        // Verificar que o backup existe na pasta de backup
        Path pastaBackup = Path.of(resultado.pastaBackup());
        assertTrue(Files.exists(pastaBackup.resolve("legenda1.ass")));
        String conteudoBackup = Files.readString(pastaBackup.resolve("legenda1.ass"), StandardCharsets.UTF_8);
        assertTrue(conteudoBackup.contains(".VnBook-Antiqua"));

        // Verificar telemetria e cache
        assertTrue(telemetriaStub.operacaoRegistrada);
        assertEquals(1, cacheStub.registros);

        Path relatorioMarkdown = Path.of(resultado.caminhoRelatorioJson().replace(".json", ".md"));
        String markdown = Files.readString(relatorioMarkdown, StandardCharsets.UTF_8);
        assertTrue(markdown.contains("# Troca de Fontes ASS"));
        assertTrue(markdown.contains("| Arquivos analisados | 2 |"));
        assertTrue(markdown.contains("Auditoria granular"));
        assertFalse(markdown.contains("Logs de Execução"));
        assertFalse(markdown.contains("[UTC "));
    }

    @Test
    void aplicarSubstituicaoContaCadaEstiloProblematico(@TempDir Path tempDir) throws IOException {
        String cabecalho = "[Script Info]\n" +
            "Title: Test Legend\n" +
            "Script Type: v4.00+\n\n" +
            "[V4+ Styles]\n" +
            "Format: Name, Fontname\n" +
            "Style: Dialogue,.VnBook-Antiqua\n" +
            "Style: Title,.VnBook-Antiqua\n\n" +
            "[Events]\n" +
            "Format: Layer, Start, End, Style, Text\n" +
            "Dialogue: 0,0:00:01.00,0:00:03.00,Dialogue,Olá Mundo\n";
        Files.writeString(tempDir.resolve("legenda-multi.ass"), cabecalho, StandardCharsets.UTF_8);

        ResultadoTrocaFonte resultado = useCase.aplicar(tempDir);

        assertEquals(1, resultado.totalAnalisados());
        assertEquals(1, resultado.totalAlterados());
        assertEquals(2, resultado.totalSubstituicoes());
        assertEquals(2, cacheStub.registros);

        String conteudoCorrigido = Files.readString(tempDir.resolve("legenda-multi.ass"), StandardCharsets.UTF_8);
        assertTrue(conteudoCorrigido.contains("Style: Dialogue,Arial"));
        assertTrue(conteudoCorrigido.contains("Style: Title,Arial"));
        assertFalse(conteudoCorrigido.contains(".VnBook-Antiqua"));
    }

    private void criarLegendaDeTeste(Path pasta, String nome, String fonte) throws IOException {
        String cabecalho = "[Script Info]\n" +
            "Title: Test Legend\n" +
            "Script Type: v4.00+\n\n" +
            "[V4+ Styles]\n" +
            "Format: Name, Fontname\n" +
            "Style: Default,Arial\n" +
            "Style: Dialogue," + fonte + "\n\n" +
            "[Events]\n" +
            "Format: Layer, Start, End, Style, Text\n" +
            "Dialogue: 0,0:00:01.00,0:00:03.00,Dialogue,Olá Mundo\n";

        Files.writeString(pasta.resolve(nome), cabecalho, StandardCharsets.UTF_8);
    }
}
