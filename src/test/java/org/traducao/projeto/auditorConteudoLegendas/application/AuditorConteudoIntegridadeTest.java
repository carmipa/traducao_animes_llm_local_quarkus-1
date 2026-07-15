package org.traducao.projeto.auditorConteudoLegendas.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.auditorConteudoLegendas.domain.ModoAuditoria;
import org.traducao.projeto.auditorConteudoLegendas.domain.RelatorioAuditoriaConteudo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: cobre os problemas estruturais da Opção 3 que o conjunto
 * de testes anterior não pegava — falas ausentes/extras, deslocamento por
 * Comentário, comparação ASS↔SRT, corrupção de parsing, índices duplicados,
 * timestamps ilegíveis, imutabilidade e isolamento dos relatórios.
 * <p>INVARIANTES DO DOMÍNIO: o modo AMBAS nunca declara "limpo" quando há eventos
 * sem par; o modo de arquivo único também audita a integridade de parsing.
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer "limpo" indevido ou exceção reprova.
 */
@QuarkusTest
class AuditorConteudoIntegridadeTest {

    @Inject
    AuditorConteudoUseCase useCase;

    private static final String CABECALHO = String.join("\n",
        "[Script Info]", "ScriptType: v4.00+", "PlayResX: 1920", "PlayResY: 1080", "",
        "[V4+ Styles]",
        "Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, "
            + "Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, "
            + "Shadow, Alignment, MarginL, MarginR, MarginV, Encoding",
        "Style: Default,Arial,20,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,0,2,10,10,10,1",
        "",
        "[Events]",
        "Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text",
        "");

    private Path ass(Path dir, String nome, String... eventos) throws IOException {
        Path p = dir.resolve(nome);
        Files.writeString(p, CABECALHO + String.join("\n", eventos) + "\n", StandardCharsets.UTF_8);
        return p;
    }

    private Path srt(Path dir, String nome, String conteudo) throws IOException {
        Path p = dir.resolve(nome);
        Files.writeString(p, conteudo, StandardCharsets.UTF_8);
        return p;
    }

    private String dlg(String inicio, String fim, String texto) {
        return "Dialogue: 0," + inicio + "," + fim + ",Default,,0,0,0,," + texto;
    }

    private boolean temRegra(RelatorioAuditoriaConteudo r, String trecho) {
        return r.getAnomalias().stream().anyMatch(a -> a.regra().contains(trecho));
    }

    private boolean temDescricao(RelatorioAuditoriaConteudo r, String trecho) {
        return r.getAnomalias().stream().anyMatch(a -> a.descricao().toLowerCase().contains(trecho.toLowerCase()));
    }

    // 1 —
    @Test
    void falaRemovidaDoTraduzidoNaoEhLimpo(@TempDir Path dir) throws IOException {
        Path o = ass(dir, "o.ass", dlg("0:00:01.00", "0:00:03.00", "Um"),
            dlg("0:00:04.00", "0:00:06.00", "Dois"), dlg("0:00:07.00", "0:00:09.00", "Tres"));
        Path t = ass(dir, "t.ass", dlg("0:00:01.00", "0:00:03.00", "Um"),
            dlg("0:00:04.00", "0:00:06.00", "Dois"));

        RelatorioAuditoriaConteudo r = useCase.auditar(ModoAuditoria.AMBAS, o, t);
        assertFalse(r.isLimpo());
        assertTrue(temRegra(r, "Integridade do Pareamento"));
    }

    // 2 —
    @Test
    void falaExtraNoTraduzidoNaoEhLimpo(@TempDir Path dir) throws IOException {
        Path o = ass(dir, "o.ass", dlg("0:00:01.00", "0:00:03.00", "Um"),
            dlg("0:00:04.00", "0:00:06.00", "Dois"));
        Path t = ass(dir, "t.ass", dlg("0:00:01.00", "0:00:03.00", "Um"),
            dlg("0:00:04.00", "0:00:06.00", "Dois"), dlg("0:00:07.00", "0:00:09.00", "Tres"));

        RelatorioAuditoriaConteudo r = useCase.auditar(ModoAuditoria.AMBAS, o, t);
        assertFalse(r.isLimpo());
        assertTrue(temRegra(r, "Integridade do Pareamento"));
    }

    // 3 —
    @Test
    void commentInseridoSoEmUmAssDeslocaEEhDetectado(@TempDir Path dir) throws IOException {
        Path o = ass(dir, "o.ass", "Comment: 0,0:00:00.00,0:00:00.50,Default,,0,0,0,,nota",
            dlg("0:00:01.00", "0:00:03.00", "A"), dlg("0:00:04.00", "0:00:06.00", "B"));
        Path t = ass(dir, "t.ass", dlg("0:00:01.00", "0:00:03.00", "A"),
            dlg("0:00:04.00", "0:00:06.00", "B"));

        RelatorioAuditoriaConteudo r = useCase.auditar(ModoAuditoria.AMBAS, o, t);
        assertFalse(r.isLimpo());
        assertTrue(temRegra(r, "Integridade do Pareamento"));
    }

    // 4 —
    @Test
    void assComparadoComSrtBloqueiaComparacao(@TempDir Path dir) throws IOException {
        Path o = ass(dir, "o.ass", dlg("0:00:01.00", "0:00:03.00", "Hello"));
        Path t = srt(dir, "t.srt", "1\n00:00:01,000 --> 00:00:03,000\nOlá\n");

        RelatorioAuditoriaConteudo r = useCase.auditar(ModoAuditoria.AMBAS, o, t);
        assertFalse(r.isLimpo());
        assertTrue(temRegra(r, "Formatos Incompatíveis"));
    }

    // 5 —
    @Test
    void mesmoTimestampComIndicesDiferentesNaoEhLimpo(@TempDir Path dir) throws IOException {
        Path o = ass(dir, "o.ass", dlg("0:00:01.00", "0:00:03.00", "Fala"));
        Path t = ass(dir, "t.ass", "Comment: 0,0:00:00.00,0:00:00.50,Default,,0,0,0,,x",
            dlg("0:00:01.00", "0:00:03.00", "Fala"));

        RelatorioAuditoriaConteudo r = useCase.auditar(ModoAuditoria.AMBAS, o, t);
        assertFalse(r.isLimpo());
        assertTrue(temRegra(r, "Integridade do Pareamento"));
    }

    // 6 —
    @Test
    void indicesSrtDuplicadosNaoDerrubamAuditoria(@TempDir Path dir) throws IOException {
        Path o = srt(dir, "o.srt",
            "1\n00:00:01,000 --> 00:00:03,000\nUm\n\n1\n00:00:04,000 --> 00:00:06,000\nDois\n");
        Path t = srt(dir, "t.srt",
            "1\n00:00:01,000 --> 00:00:03,000\nUm\n\n2\n00:00:04,000 --> 00:00:06,000\nDois\n");

        RelatorioAuditoriaConteudo r = useCase.auditar(ModoAuditoria.AMBAS, o, t); // não pode lançar
        assertFalse(r.isLimpo());
        assertTrue(temDescricao(r, "duplicad"));
    }

    // 7 —
    @Test
    void blocoSrtTruncadoEhDetectado(@TempDir Path dir) throws IOException {
        Path a = srt(dir, "a.srt", "1\n00:00:01,000 --> 00:00:03,000\nHello\n\n2");

        RelatorioAuditoriaConteudo r = useCase.auditar(ModoAuditoria.ORIGINAL, a, null);
        assertFalse(r.isLimpo());
        assertTrue(temRegra(r, "Integridade de Parsing"));
        assertTrue(temDescricao(r, "trunc"));
    }

    // 8 —
    @Test
    void timestampSrtIlegivelEhDetectado(@TempDir Path dir) throws IOException {
        Path a = srt(dir, "a.srt", "1\n00:00:01,000 -> 00:00:03,000\nHello\n");

        RelatorioAuditoriaConteudo r = useCase.auditar(ModoAuditoria.ORIGINAL, a, null);
        assertFalse(r.isLimpo());
        assertTrue(temRegra(r, "Timestamp"));
    }

    // 9 —
    @Test
    void dialogueAssMalformadoEhDetectado(@TempDir Path dir) throws IOException {
        Path a = ass(dir, "a.ass", "Dialogue: 0,0:00:01.00,Default,,texto");

        RelatorioAuditoriaConteudo r = useCase.auditar(ModoAuditoria.ORIGINAL, a, null);
        assertFalse(r.isLimpo());
        assertTrue(temRegra(r, "Integridade de Parsing"));
        assertTrue(temDescricao(r, "malformada"));
    }

    // 10 —
    @Test
    void ambasDetectaTagNaoFechadaNoTraduzido(@TempDir Path dir) throws IOException {
        Path o = ass(dir, "o.ass", dlg("0:00:01.00", "0:00:03.00", "{\\i1}ok{\\i0}"));
        Path t = ass(dir, "t.ass", dlg("0:00:01.00", "0:00:03.00", "{\\i1 sem fechar"));

        RelatorioAuditoriaConteudo r = useCase.auditar(ModoAuditoria.AMBAS, o, t);
        assertFalse(r.isLimpo());
        assertTrue(r.getAnomalias().stream().anyMatch(a ->
            a.regra().contains("Override ASS Não Fechado") && a.eventoTraduzido() != null));
    }

    // 12 —
    @Test
    void duasAuditoriasNoMesmoSegundoGeramDoisJson(@TempDir Path dir) throws IOException {
        Path o = ass(dir, "o.ass", dlg("0:00:01.00", "0:00:03.00", "x"));
        Path t = ass(dir, "t.ass", dlg("0:00:01.00", "0:00:03.00", "x"));

        RelatorioAuditoriaConteudo r1 = useCase.auditar(ModoAuditoria.AMBAS, o, t);
        RelatorioAuditoriaConteudo r2 = useCase.auditar(ModoAuditoria.AMBAS, o, t);

        assertNotNull(r1.getCaminhoRelatorioJson());
        assertNotNull(r2.getCaminhoRelatorioJson());
        assertNotEquals(r1.getCaminhoRelatorioJson(), r2.getCaminhoRelatorioJson());
        assertTrue(Files.exists(Path.of(r1.getCaminhoRelatorioJson())));
        assertTrue(Files.exists(Path.of(r2.getCaminhoRelatorioJson())));
    }

    // 13 —
    @Test
    void jsonPersistidoContemModo(@TempDir Path dir) throws IOException {
        Path o = ass(dir, "o.ass", dlg("0:00:01.00", "0:00:03.00", "x"));
        Path t = ass(dir, "t.ass", dlg("0:00:01.00", "0:00:03.00", "x"));

        RelatorioAuditoriaConteudo r = useCase.auditar(ModoAuditoria.AMBAS, o, t);
        String json = Files.readString(Path.of(r.getCaminhoRelatorioJson()), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"modo\""));
        assertTrue(json.contains("AMBAS"));
    }

    // 14 —
    @Test
    void testeNaoGravaEmRelatoriosOperacional(@TempDir Path dir) throws IOException {
        Path o = ass(dir, "o.ass", dlg("0:00:01.00", "0:00:03.00", "x"));
        Path t = ass(dir, "t.ass", dlg("0:00:01.00", "0:00:03.00", "x"));

        RelatorioAuditoriaConteudo r = useCase.auditar(ModoAuditoria.AMBAS, o, t);
        String jsonPath = r.getCaminhoRelatorioJson().replace('\\', '/');
        // No perfil de teste, o JSON vai para a pasta de entrada (@TempDir), não para relatorios/.
        assertTrue(jsonPath.contains(dir.toString().replace('\\', '/')));
        assertFalse(jsonPath.toLowerCase().contains("/relatorios/"));
    }
}
