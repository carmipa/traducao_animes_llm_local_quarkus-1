package org.traducao.projeto.auditorConteudoLegendas.application.regras;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaAss;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegraDanoKaraokeTest {

    @TempDir
    Path tempDir;

    private final RegraDanoKaraoke regra = new RegraDanoKaraoke(new DetectorEfeitoKaraokeService());
    private final LeitorLegendaAss leitor = new LeitorLegendaAss();

    @Test
    void acusaCriticalQuandoRomajiProtegidoFoiAlterado() throws IOException {
        // Caso real do 86 T1: romaji em estilo "Opening" com tags leves virou
        // alucinação em PT — expansão de só 1.7x, que a checagem de tamanho
        // não pegaria; a régua tem de ser a proteção de romaji do detector.
        DocumentoLegenda original = doc("original.ass",
            "Dialogue: 2,0:22:10.14,0:22:10.27,Opening,,0,0,0,,{\\pos(1143,40)\\bord0\\blur0.5\\clip(0,70,1920,86.5)}fuminijirareru dake no hana");
        DocumentoLegenda traduzido = doc("traduzido.ass",
            "Dialogue: 2,0:22:10.14,0:22:10.27,Opening,,0,0,0,,{\\pos(1143,40)\\bord0\\blur0.5\\clip(0,70,1920,86.5)}A única flor que floresce para todos os mortos.");

        List<AnomaliaConteudo> anomalias = regra.auditar(original, traduzido);

        assertEquals(1, anomalias.size());
        assertEquals(AnomaliaConteudo.TipoSeveridade.CRITICAL, anomalias.get(0).severidade());
    }

    @Test
    void naoAcusaKaraokeInglesTraduzidoNormalmente() throws IOException {
        DocumentoLegenda original = doc("original2.ass",
            "Dialogue: 0,0:01:00.00,0:01:03.00,Opening,,0,0,0,,{\\k30}Fly {\\k20}me {\\k25}to {\\k25}the {\\k40}moon");
        DocumentoLegenda traduzido = doc("traduzido2.ass",
            "Dialogue: 0,0:01:00.00,0:01:03.00,Opening,,0,0,0,,{\\k30}Leve-me {\\k20}até {\\k25}a {\\k25}{\\k40}lua");

        List<AnomaliaConteudo> anomalias = regra.auditar(original, traduzido);

        assertTrue(anomalias.isEmpty());
    }

    @Test
    void avisaQuandoTagsDeKaraokeSomem() throws IOException {
        DocumentoLegenda original = doc("original3.ass",
            "Dialogue: 0,0:01:00.00,0:01:03.00,Opening,,0,0,0,,{\\k30}Fly {\\k20}me {\\k25}to {\\k25}the {\\k40}moon");
        DocumentoLegenda traduzido = doc("traduzido3.ass",
            "Dialogue: 0,0:01:00.00,0:01:03.00,Opening,,0,0,0,,Leve-me até a lua");

        List<AnomaliaConteudo> anomalias = regra.auditar(original, traduzido);

        assertEquals(1, anomalias.size());
        assertEquals(AnomaliaConteudo.TipoSeveridade.WARNING, anomalias.get(0).severidade());
    }

    private DocumentoLegenda doc(String nome, String linhaDialogo) throws IOException {
        Path arquivo = tempDir.resolve(nome);
        Files.writeString(arquivo, """
            [Script Info]
            Title: Teste

            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            %s
            """.formatted(linhaDialogo));
        return leitor.ler(arquivo);
    }
}
