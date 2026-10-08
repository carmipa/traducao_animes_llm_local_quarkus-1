package org.traducao.projeto.auditorConteudoLegendas.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.AuditoriaException;
import org.traducao.projeto.auditorConteudoLegendas.domain.ModoAuditoria;
import org.traducao.projeto.auditorConteudoLegendas.domain.RelatorioAuditoriaConteudo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    // Episódio em miniatura com uma linha de efeito pesado no meio: é o desenho que, com o
    // pareamento deslocado, fazia a regra de efeito vazado acusar falas trocadas.
    private String[] episodioOriginal() {
        return new String[] {
            dlg("0:00:01.00", "0:00:03.00", "{\\pos(10,10)}Sign"),
            dlg("0:00:04.00", "0:00:06.00", "Hello there"),
            dlg("0:00:07.00", "0:00:09.00", "But there are dreams"),
            dlg("0:00:10.00", "0:00:12.00", "{\\pos(20,20)}OK"),
            dlg("0:00:13.00", "0:00:15.00", "Goodbye my friend")
        };
    }

    private AnomaliaConteudo contagemDeFalas(RelatorioAuditoriaConteudo r) {
        return r.getAnomalias().stream()
            .filter(a -> a.regra().contains("Integridade do Pareamento")
                && a.descricao().startsWith("Quantidade de diálogos difere"))
            .findFirst().orElseThrow(() -> new AssertionError("sem a anomalia de contagem: " + r.getAnomalias()));
    }

    // 15 — auditoria de 08/10/2026, A2/A3: a fala apagada no MEIO é a apontada (com os dois
    // lados), e as regras fala a fala não acusam o deslocamento como falas trocadas.
    @Test
    void falaApagadaNoMeioEApontadaEPareamentoDeslocadoNaoViraRuido(@TempDir Path dir) throws IOException {
        Path o = ass(dir, "o.ass", episodioOriginal());
        Path t = ass(dir, "t.ass",
            dlg("0:00:01.00", "0:00:03.00", "{\\pos(10,10)}Sign"),
            dlg("0:00:04.00", "0:00:06.00", "Olá"),
            dlg("0:00:10.00", "0:00:12.00", "{\\pos(20,20)}OK"),
            dlg("0:00:13.00", "0:00:15.00", "Adeus, meu amigo"));

        RelatorioAuditoriaConteudo r = useCase.auditar(ModoAuditoria.AMBAS, o, t);

        AnomaliaConteudo contagem = contagemDeFalas(r);
        assertNotNull(contagem.eventoOriginal());
        assertNotNull(contagem.eventoTraduzido(), "sem o lado traduzido a tela chama de 'da fonte'");
        assertEquals("But there are dreams", contagem.eventoOriginal().texto(),
            "tem de apontar a fala apagada, não a última do episódio");
        assertTrue(contagem.descricao().contains("fala nº 3"), contagem.descricao());
        assertTrue(contagem.descricao().contains("[0:00:07.00]"), contagem.descricao());

        assertFalse(temRegra(r, "Efeito Visual Vazado"),
            "regra fala a fala rodou sobre pareamento deslocado: " + r.getAnomalias());
        assertTrue(r.getAnomalias().stream().anyMatch(a -> a.regra().equals("Regras Comparativas Puladas")
            && a.descricao().contains("Efeito Visual Vazado")), "a omissão tem de ser declarada");
    }

    // 16 — fronteira (A1): o MESMO sinal (contagem de falas difere), mas com a estrutura
    // intacta — o tradutor comentou a fala. A posição continua comparável: as regras fala a
    // fala rodam, nada é declarado pulado, e cada anomalia leva os dois eventos.
    @Test
    void falaComentadaMantemAPosicaoEAsRegrasContinuamRodando(@TempDir Path dir) throws IOException {
        Path o = ass(dir, "o.ass", episodioOriginal());
        Path t = ass(dir, "t.ass",
            dlg("0:00:01.00", "0:00:03.00", "{\\pos(10,10)}Sign"),
            dlg("0:00:04.00", "0:00:06.00", "Olá"),
            "Comment: 0,0:00:07.00,0:00:09.00,Default,,0,0,0,,Mas há sonhos",
            dlg("0:00:10.00", "0:00:12.00", "{\\pos(20,20)}OK"),
            dlg("0:00:13.00", "0:00:15.00", "Adeus, meu amigo"));

        RelatorioAuditoriaConteudo r = useCase.auditar(ModoAuditoria.AMBAS, o, t);

        assertFalse(temRegra(r, "Regras Comparativas Puladas"), r.getAnomalias().toString());
        assertFalse(temRegra(r, "Efeito Visual Vazado"), "posição alinhada não produz falso vazamento");
        assertEquals("But there are dreams", contagemDeFalas(r).eventoOriginal().texto());

        AnomaliaConteudo ausente = r.getAnomalias().stream()
            .filter(a -> a.descricao().contains("sem correspondente no traduzido"))
            .findFirst().orElseThrow();
        assertNotNull(ausente.eventoOriginal());
        assertNotNull(ausente.eventoTraduzido(), "o lado traduzido (o Comentário) tem de ir junto");
        assertEquals("Comment", ausente.eventoTraduzido().tipoLinha());
    }

    // 17 — controle (A1): par legítimo, estrutura igual, efeitos preservados — limpo e com
    // todas as regras comparativas executadas.
    @Test
    void parLegitimoRodaTodasAsRegrasESaiLimpo(@TempDir Path dir) throws IOException {
        Path o = ass(dir, "o.ass", episodioOriginal());
        Path t = ass(dir, "t.ass",
            dlg("0:00:01.00", "0:00:03.00", "{\\pos(10,10)}Sign"),
            dlg("0:00:04.00", "0:00:06.00", "Olá"),
            dlg("0:00:07.00", "0:00:09.00", "Mas há sonhos"),
            dlg("0:00:10.00", "0:00:12.00", "{\\pos(20,20)}OK"),
            dlg("0:00:13.00", "0:00:15.00", "Adeus, meu amigo"));

        RelatorioAuditoriaConteudo r = useCase.auditar(ModoAuditoria.AMBAS, o, t);

        assertTrue(r.isLimpo(), r.getAnomalias().toString());
        RelatorioAuditoriaConteudo comPuladas = useCase.auditar(ModoAuditoria.AMBAS, o,
            ass(dir, "t2.ass", dlg("0:00:01.00", "0:00:03.00", "{\\pos(10,10)}Sign")));
        assertTrue(r.getRegrasExecutadas() > comPuladas.getRegrasExecutadas(),
            "par íntegro executa mais regras que o par deslocado");
    }

    // 18 — auditoria de 08/10/2026, A4: arquivo sem fala nenhuma não é "limpo" — nada foi
    // auditado. Nos dois modos e nos dois lados.
    @Test
    void arquivoSemFalaNaoSaiLimpo(@TempDir Path dir) throws IOException {
        Path vazio = srt(dir, "vazio.srt", "");
        Path soComentario = ass(dir, "so-comentario.ass", "Comment: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,nota");
        Path umaFala = srt(dir, "uma.srt", "1\n00:00:01,000 --> 00:00:03,000\nOlá\n");

        RelatorioAuditoriaConteudo soTraduzido = useCase.auditar(ModoAuditoria.TRADUZIDO, null, vazio);
        assertFalse(soTraduzido.isLimpo(), "um .srt de 0 bytes não pode sair limpo");
        assertTrue(temRegra(soTraduzido, "Nenhuma Fala Auditada"));

        assertTrue(temRegra(useCase.auditar(ModoAuditoria.ORIGINAL, soComentario, null), "Nenhuma Fala Auditada"));

        RelatorioAuditoriaConteudo ambas = useCase.auditar(ModoAuditoria.AMBAS, umaFala, vazio);
        assertTrue(ambas.getAnomalias().stream().anyMatch(a -> a.regra().equals("Nenhuma Fala Auditada")
            && a.descricao().contains("traduzido")), ambas.getAnomalias().toString());
    }

    // 19 — controle (A1), a fronteira do mesmo sinal: UMA fala basta para auditar de verdade.
    @Test
    void arquivoComUmaFalaEAuditadoNormalmente(@TempDir Path dir) throws IOException {
        Path umaFala = srt(dir, "uma.srt", "1\n00:00:01,000 --> 00:00:03,000\nOlá\n");

        RelatorioAuditoriaConteudo r = useCase.auditar(ModoAuditoria.TRADUZIDO, null, umaFala);

        assertTrue(r.isLimpo(), r.getAnomalias().toString());
    }

    private Path assUtf16(Path dir, String nome, String conteudo) throws IOException {
        Path p = dir.resolve(nome);
        Files.write(p, ("﻿" + conteudo).getBytes(StandardCharsets.UTF_16LE));
        return p;
    }

    // 20 — auditoria de 08/10/2026, A5: .ass em UTF-16 (comum em BD antigo) que a produção lê
    // sem erro não pode ser acusado de "sem [Events]" pela validação de parsing.
    @Test
    void assUtf16LegivelNaoEAcusadoDeSemEvents(@TempDir Path dir) throws IOException {
        Path a = assUtf16(dir, "utf16.ass", CABECALHO + dlg("0:00:01.00", "0:00:03.00", "Olá") + "\n");

        RelatorioAuditoriaConteudo r = useCase.auditar(ModoAuditoria.ORIGINAL, a, null);

        assertFalse(temDescricao(r, "sem a seção [Events]"), r.getAnomalias().toString());
        assertTrue(r.isLimpo(), r.getAnomalias().toString());
    }

    // 21 — fronteira (A1): o defeito real continua recusado, no MESMO encoding e em UTF-8. Quem
    // recusa um .ass sem [Events] é o leitor de produção, antes do validador: a auditoria falha
    // alto em vez de devolver relatório.
    @Test
    void assSemEventsContinuaRecusadoEmQualquerEncoding(@TempDir Path dir) throws IOException {
        String semEvents = "[Script Info]\nScriptType: v4.00+\n\n[V4+ Styles]\n";
        Path utf16 = assUtf16(dir, "sem-utf16.ass", semEvents);
        Path utf8 = dir.resolve("sem-utf8.ass");
        Files.writeString(utf8, semEvents, StandardCharsets.UTF_8);

        for (Path p : List.of(utf16, utf8)) {
            AuditoriaException e = assertThrows(AuditoriaException.class,
                () -> useCase.auditar(ModoAuditoria.ORIGINAL, p, null), p.getFileName().toString());
            assertTrue(e.getMessage().contains("[Events]"), e.getMessage());
        }
    }

    // 22 — auditoria de 08/10/2026, A8: marcador de capítulo do fansub (linhas reais do 0080 E02,
    // com e sem Name chptr) não é fala — nem timestamp inválido, nem diálogo vazio.
    @Test
    void marcadorDeCapituloNaoEDefeito(@TempDir Path dir) throws IOException {
        Path a = ass(dir, "capitulos.ass",
            "Dialogue: 0,0:00:01.15,0:00:01.15,OP,,0,0,0,,{OP Start}",
            dlg("0:00:02.00", "0:00:04.00", "Hello"),
            "Dialogue: 0,0:01:44.08,0:01:44.08,OP,chptr,0,0,0,,{Episode}");

        RelatorioAuditoriaConteudo r = useCase.auditar(ModoAuditoria.ORIGINAL, a, null);

        assertTrue(r.isLimpo(), r.getAnomalias().toString());
    }

    // 22b — A8, medido no acervo: o quadro de KFX que o template grava com duração zero (sílaba
    // do encerramento do 86, estilo de música) é invisível de propósito, não timestamp inválido.
    // A fronteira — a mesma duração zero com fala em estilo de diálogo — é o teste 23.
    @Test
    void quadroDeKaraokeDeDuracaoZeroNaoETimestampInvalido(@TempDir Path dir) throws IOException {
        Path a = ass(dir, "kfx.ass",
            "Dialogue: 0,0:01:00.56,0:01:00.56,Ending,,0,0,0,,{=56}{\\fad(80,0)\\pos(60,40)\\blur0.6\\bord3}One,",
            dlg("0:00:02.00", "0:00:04.00", "Hello"));

        RelatorioAuditoriaConteudo r = useCase.auditar(ModoAuditoria.ORIGINAL, a, null);

        assertFalse(temRegra(r, "Timestamp"), r.getAnomalias().toString());
    }

    // 22c — A8, medido no acervo: portador de efeito do karaokê (só tags, estilo de música — linha
    // real da abertura do 86) e nota do fansub só de comentário não são fala perdida.
    @Test
    void portadorDeEfeitoENotaDoFansubNaoSaoDialogoVazio(@TempDir Path dir) throws IOException {
        Path a = ass(dir, "portador.ass",
            "Dialogue: 0,0:00:10.00,0:00:12.00,Opening,,0,0,0,,{\\fad(200,0)\\blur0.6\\pos(664.765625,30)\\c&H000F0E0E&}",
            "Dialogue: 0,0:00:20.00,0:00:22.00,main,,0,0,0,,{Intro}",
            dlg("0:00:30.00", "0:00:32.00", "Hello"));

        RelatorioAuditoriaConteudo r = useCase.auditar(ModoAuditoria.ORIGINAL, a, null);

        assertFalse(temRegra(r, "Diálogo Vazio"), r.getAnomalias().toString());
    }

    // 22d — o controle que cobre a isenção acima: no par, a letra que TINHA texto e ficou só com
    // as tags na tradução é verso apagado — e só a comparação consegue ver isso.
    @Test
    void versoApagadoNaTraducaoEAcusadoPeloPar(@TempDir Path dir) throws IOException {
        Path o = ass(dir, "o.ass",
            "Dialogue: 0,0:00:10.00,0:00:12.00,Opening,,0,0,0,,{\\fad(200,0)\\pos(10,10)}Fly me to the moon");
        Path t = ass(dir, "t.ass",
            "Dialogue: 0,0:00:10.00,0:00:12.00,Opening,,0,0,0,,{\\fad(200,0)\\pos(10,10)}");

        RelatorioAuditoriaConteudo r = useCase.auditar(ModoAuditoria.AMBAS, o, t);

        assertTrue(r.getAnomalias().stream().anyMatch(a -> a.severidade() == AnomaliaConteudo.TipoSeveridade.ERROR
            && a.descricao().contains("apagado")), r.getAnomalias().toString());
    }

    // 23 — fronteira (A1): cada sinal do marcador SOZINHO continua defeito. Duração zero com
    // texto visível é fala que não aparece; texto só de tags com duração é fala perdida.
    @Test
    void duracaoZeroComFalaOuLinhaVaziaComDuracaoContinuamAcusadas(@TempDir Path dir) throws IOException {
        Path a = ass(dir, "fronteira.ass",
            dlg("0:00:01.15", "0:00:01.15", "{\\i1}Hello{\\i0}"),
            dlg("0:00:02.00", "0:00:04.00", "{\\an8}"),
            dlg("0:00:05.00", "0:00:04.00", "{OP Start}"));

        RelatorioAuditoriaConteudo r = useCase.auditar(ModoAuditoria.ORIGINAL, a, null);

        long timestamp = r.getAnomalias().stream().filter(x -> x.regra().startsWith("Timestamp")).count();
        long vazio = r.getAnomalias().stream().filter(x -> x.regra().equals("Evento de Diálogo Vazio")).count();
        assertEquals(2, timestamp, "duração zero com fala E fim antes do início: " + r.getAnomalias());
        // A nota {OP Start} com fim antes do início é defeito de TEMPO (acima), não fala perdida:
        // só a linha de diálogo que ficou só com tag ({\an8}) é "diálogo vazio".
        assertEquals(1, vazio, "só tags com duração em estilo de diálogo: " + r.getAnomalias());
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
