package org.traducao.projeto.novoKaraoke.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.core.presentation.web.LogStreamService;
import org.traducao.projeto.novoKaraoke.domain.ports.TelemetriaKaraokePort;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversorKaraokeUseCaseTest {

    @TempDir
    Path tempDir;

    @Test
    void arquivoSemMusicaEhCopiadoByteIdentico() throws Exception {
        Path origem = tempDir.resolve("sem-musica.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        byte[] original = """
            [Script Info]\r
            PlayResY: 1080\r
            \r
            [V4+ Styles]\r
            Format: Name,Fontname,Fontsize,PrimaryColour,SecondaryColour,OutlineColour,BackColour,Bold,Italic,Underline,StrikeOut,ScaleX,ScaleY,Spacing,Angle,BorderStyle,Outline,Shadow,Alignment,MarginL,MarginR,MarginV,Encoding\r
            Style: Default,Arial,48,&H00FFFFFF,&H000000FF,&H00000000,&H96000000,0,0,0,0,100,100,0,0,1,2,1,2,30,30,30,1\r
            \r
            [Events]\r
            Format: Layer,Start,End,Style,Name,MarginL,MarginR,MarginV,Effect,Text\r
            Comment: 0,0:00:00.00,0:00:01.00,Default,,0,0,0,,template preservado\r
            Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Fala comum.\r
            """.getBytes(StandardCharsets.UTF_8);
        Files.write(origem, original);

        novoConversor().converterArquivo(origem, destino, true);

        assertEquals(new String(original, StandardCharsets.UTF_8),
            Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o ORIGINAL fica em cima e a tradução embaixo — na música inteira, sem
     * trocar de posição no meio. O espectador não pode ver o japonês ora acima ora abaixo.
     *
     * <p>O critério de ordem era booleano e exigia 100% das palavras serem sílaba Hepburn. Romaji
     * REAL reprova nisso: {@code hitoribocchi}, {@code tsudzuku}, {@code kekkyoku} e
     * {@code icchatte} têm geminada/dígrafo fora da regex de sílaba, e uma palavra em seis derruba
     * a linha. As duas camadas empatavam em "não é romaji", o sort ESTÁVEL preservava a ordem do
     * arquivo, e a linha inglesa subia.
     *
     * <p>Medido no Guilty Crown em 2026-08-03: 33 de 508 versos pareados (6%), sempre os MESMOS
     * versos em todos os episódios — o defeito é do texto, não do arquivo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: se voltar a comparar por sim/não, estas linhas invertem.
     */
    @Test
    void romajiComGeminadaFicaAcimaDaTraducaoMesmoNaoSendoSilabaPura() throws Exception {
        Path origem = tempDir.resolve("ordem-camadas.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        // A linha INGLESA vem PRIMEIRO no arquivo de propósito: é o caso real do ED do Guilty
        // Crown e o que fazia o sort estável manter a ordem errada.
        Files.writeString(origem, cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\pos(100,80)}Now I am all alone\n"
            + "Dialogue: 0,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\pos(100,40)}Soshite watashi wa koushite hitoribocchi de\n",
            StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertTrue(saida.contains("Soshite watashi wa koushite hitoribocchi de\\NNow I am all alone"),
            () -> "o romaji tem de ficar ACIMA da traducao mesmo com 'hitoribocchi' fora da regex "
                + "de silaba (83% romaji, nao 100%): " + saida);
    }

    @Test
    void preservaAsDuasCamadasNoMesmoTempoELimpaTagsVisiveis() throws Exception {
        Path origem = tempDir.resolve("karaoke.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\pos(100,40)}aigan shitemo kongan shitemo kawaranai ya, mou\n"
            + "Dialogue: 0,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\pos(100,80)}[]Não importa o quanto eu deseje, nada muda [![TAG1]]\n",
            StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        // REGRA DE NEGÓCIO (Paulo, 2026-07-25): o romaji é a LÍNGUA ORIGINAL e fica; o que se
        // remove é a frescura visual. As duas camadas viram UM evento com \N — original em cima,
        // tradução embaixo — porque a saída usa um único estilo e eventos separados no mesmo tempo
        // imprimiriam um sobre o outro. Antes desta data o romaji era justamente o descartado.
        assertTrue(saida.contains("Dialogue: 0,0:00:01.00,0:00:04.00,Karaoke Simples,,0,0,0,,"
            + "aigan shitemo kongan shitemo kawaranai ya, mou"
            + "\\NNão importa o quanto eu deseje, nada muda"), saida);
        assertFalse(saida.contains("[]"));
        assertFalse(saida.contains("TAG1"));
    }

    @Test
    void preservaEventoCurtoSemCoberturaRealMesmoPertoDaLinhaPrincipal() throws Exception {
        Path origem = tempDir.resolve("curta.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            + "Dialogue: 0,0:00:10.00,0:00:12.00,Opening,,0,0,0,,{\\pos(100,40)}Linha principal da música\n"
            + "Dialogue: 0,0:00:19.00,0:00:20.00,Opening,,0,0,0,,{\\pos(100,80)}Ei\n",
            StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertTrue(saida.contains("Dialogue: 0,0:00:10.00,0:00:12.00,Karaoke Simples,,0,0,0,,Linha principal da música"));
        assertTrue(saida.contains("Dialogue: 0,0:00:19.00,0:00:20.00,Opening,,0,0,0,,{\\pos(100,80)}Ei"));
    }

    @Test
    void kfxApenasSilabicoViraLinhaSimplesENaoArquivoGrande() throws Exception {
        Path origem = tempDir.resolve("kfx-silabico.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            + "Dialogue: 1,0:00:01.00,0:00:01.30,Opening,,0,0,0,,{\\pos(100,40)\\clip(0,0,200,60)\\t(0,100,\\blur4\\fscx120)}fu\n"
            + "Dialogue: 1,0:00:01.02,0:00:01.28,Opening,,0,0,0,,{\\pos(101,40)\\clip(0,0,200,60)\\t(0,100,\\blur4\\fscx120)}fu\n"
            + "Dialogue: 1,0:00:01.30,0:00:01.60,Opening,,0,0,0,,{\\pos(130,40)\\clip(0,0,200,60)\\t(0,100,\\blur4\\fscx120)}mi\n"
            + "Dialogue: 1,0:00:01.60,0:00:01.90,Opening,,0,0,0,,{\\pos(160,40)\\clip(0,0,200,60)\\t(0,100,\\blur4\\fscx120)}ni\n"
            + "Dialogue: 1,0:00:01.90,0:00:02.30,Opening,,0,0,0,,{\\pos(190,40)\\clip(0,0,200,60)\\t(0,100,\\blur4\\fscx120)}hana\n",
            StandardCharsets.UTF_8);

        var resultado = novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertTrue(saida.contains("Dialogue: 0,0:00:01.00,0:00:02.30,Karaoke Simples,,0,0,0,,fu mi ni hana"));
        assertFalse(saida.contains("\\t("));
        assertFalse(saida.contains("\\clip("));
        assertFalse(saida.contains("Style: Opening"));
        assertEquals(5, resultado.getEventosKaraokeRemovidos());
        assertEquals(0, resultado.getEventosPreservadosPorSeguranca());
        assertTrue(resultado.getTamanhoNovoBytes() < resultado.getTamanhoOriginalBytes());
    }

    @Test
    void kfxComLayersDuplicadosPrefereLegendaOcidentalSimples() throws Exception {
        Path origem = tempDir.resolve("kfx-duas-faixas.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            + "Dialogue: 1,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\move(100,30,100,30,0,3000)\\t(0,3000,\\frz1)}ki\n"
            + "Dialogue: 1,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\move(130,30,130,30,0,3000)\\t(0,3000,\\frz1)}mi\n"
            + "Dialogue: 1,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\move(160,30,160,30,0,3000)\\t(0,3000,\\frz1)}no\n"
            + "Dialogue: 2,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\move(100,30,100,30,0,3000)\\t(0,3000,\\frz1)}ki\n"
            + "Dialogue: 2,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\move(130,30,130,30,0,3000)\\t(0,3000,\\frz1)}mi\n"
            + "Dialogue: 2,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\move(160,30,160,30,0,3000)\\t(0,3000,\\frz1)}no\n"
            + "Dialogue: 1,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\move(100,1050,100,1050,0,3000)\\t(0,3000,\\frz1)}O\n"
            + "Dialogue: 1,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\move(130,1050,130,1050,0,3000)\\t(0,3000,\\frz1)}i\n",
            StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        // O romaji pulverizado pelo KFX é reconstruído ("ki mi no") e PRESERVADO acima da
        // tradução, em vez de descartado: continua sendo a língua original.
        assertTrue(saida.contains("Dialogue: 0,0:00:01.00,0:00:04.00,Karaoke Simples,,0,0,0,,ki mi no\\NOi"), saida);
        assertFalse(saida.contains("\\move("));
        assertFalse(saida.contains("\\t("));
    }

    @Test
    void kfxLetraPorLetraContinuoEhCortadoPorFraseComEspacos() throws Exception {
        // KFX real (86): cada letra é um evento, a frase inteira fica na tela ao
        // mesmo tempo e a frase seguinte começa EXATAMENTE quando a anterior
        // termina — o gap nunca separa; o corte tem que vir do vale de concorrência.
        Path origem = tempDir.resolve("kfx-letra-a-letra.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        StringBuilder corpo = new StringBuilder(cabecalho());
        corpo.append(eventosPorLetra("0:00:01.00", "0:00:05.00", "Voce pode"));
        corpo.append(eventosPorLetra("0:00:05.00", "0:00:09.00", "Nada muda"));
        Files.writeString(origem, corpo.toString(), StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertTrue(saida.contains("Dialogue: 0,0:00:01.00,0:00:05.00,Karaoke Simples,,0,0,0,,Voce pode"), saida);
        assertTrue(saida.contains("Dialogue: 0,0:00:05.00,0:00:09.00,Karaoke Simples,,0,0,0,,Nada muda"), saida);
        assertFalse(saida.contains("VocepodeNadamuda"), saida);
    }

    @Test
    void deduplicaCamadasComJanelasQuaseIdenticasENaoSoIguais() throws Exception {
        // romaji e tradução simultâneos raramente terminam no MESMO centésimo;
        // a deduplicação precisa agrupar por sobreposição, não por janela exata
        Path origem = tempDir.resolve("janelas-quase-iguais.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:04.96,Opening,,0,0,0,,{\\pos(100,40)}aigan shitemo kongan shitemo kawaranai ya, mou\n"
            + "Dialogue: 0,0:00:01.00,0:00:05.00,Opening,,0,0,0,,{\\pos(100,80)}Não importa o quanto eu deseje, nada muda\n",
            StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        // Agrupar por sobreposição continua valendo (as janelas diferem em 4 centésimos); o que
        // mudou é o desfecho: as duas camadas são preservadas juntas, não uma descartada.
        assertTrue(saida.contains("Dialogue: 0,0:00:01.00,0:00:05.00,Karaoke Simples,,0,0,0,,"
            + "aigan shitemo kongan shitemo kawaranai ya, mou"
            + "\\NNão importa o quanto eu deseje, nada muda"), saida);
    }

    @Test
    void blocoKfxQueViraLinhaImplausivelEhPreservadoIntacto() throws Exception {
        // sem vale de concorrência não há como separar as frases: melhor manter o
        // efeito original do que emitir uma parede de texto de 29 segundos
        Path origem = tempDir.resolve("kfx-irreconstruivel.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            + eventosPorLetra("0:00:01.00", "0:00:30.00", "Frase longa demais"),
            StandardCharsets.UTF_8);

        var resultado = novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertFalse(saida.contains("Karaoke Simples"), saida);
        assertTrue(saida.contains("{\\pos(100.0,40)\\t(0,100,\\blur4\\fscx120)}F"), saida);
        assertEquals(0, resultado.getEventosKaraokeRemovidos());
        assertTrue(resultado.getEventosPreservadosPorSeguranca() > 0);
    }

    /** Um evento Dialogue por letra visível, todos na janela inteira da frase (KFX letra-por-letra). */
    private static String eventosPorLetra(String inicio, String fim, String frase) {
        StringBuilder eventos = new StringBuilder();
        double x = 100;
        for (char letra : frase.toCharArray()) {
            if (letra == ' ') {
                x += 40; // espaço não vira evento: só o salto em X marca a palavra
                continue;
            }
            eventos.append("Dialogue: 1,").append(inicio).append(',').append(fim)
                .append(",Opening,,0,0,0,,{\\pos(").append(x).append(",40)\\t(0,100,\\blur4\\fscx120)}")
                .append(letra).append('\n');
            x += 20;
        }
        return eventos.toString();
    }

    @Test
    void ignoraArquivosAuxiliaresQuandoHaEpisodiosPrincipais() throws Exception {
        Path origem = Files.createDirectory(tempDir.resolve("origem"));
        Path destino = tempDir.resolve("saida");
        Files.writeString(origem.resolve("[DB]86_-_01_(Dual Audio)_Track6_PT-BR.ass"), cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Fala comum.\n",
            StandardCharsets.UTF_8);
        Files.writeString(origem.resolve("[DB]86_-_NCOP01_(10bit)_Track2_PT-BR.ass"), cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:03.00,Opening,,0,0,0,,Letra auxiliar.\n",
            StandardCharsets.UTF_8);
        Files.writeString(origem.resolve("[DB]86 Special Edition Senya_-_SP_(10bit)_Track2_PT-BR.ass"), cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Especial.\n",
            StandardCharsets.UTF_8);

        List<String> processados = novoConversor().simular(origem, destino).stream()
            .map(r -> r.getArquivoOrigem())
            .toList();

        assertEquals(List.of("[DB]86_-_01_(Dual Audio)_Track6_PT-BR.ass"), processados);
    }

    @Test
    void processaAuxiliaresQuandoPastaTemApenasAuxiliares() throws Exception {
        Path origem = Files.createDirectory(tempDir.resolve("origem"));
        Path destino = tempDir.resolve("saida");
        Files.writeString(origem.resolve("[DB]86_-_NCED01_(10bit)_Track2_PT-BR.ass"), cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:03.00,Ending,,0,0,0,,Letra auxiliar.\n",
            StandardCharsets.UTF_8);

        List<String> processados = novoConversor().simular(origem, destino).stream()
            .map(r -> r.getArquivoOrigem())
            .toList();

        assertEquals(List.of("[DB]86_-_NCED01_(10bit)_Track2_PT-BR.ass"), processados);
    }

    private static ConversorKaraokeUseCase novoConversor() {
        ConversorKaraokeUseCase conversor = new ConversorKaraokeUseCase();
        conversor.detectorKaraoke = new DetectorEfeitoKaraokeService();
        conversor.logStream = new LogStreamSilencioso();
        conversor.telemetriaKaraoke = new TelemetriaKaraokeSilenciosa();
        return conversor;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a medição não pode participar do resultado — a suíte usa uma
     * implementação inerte para provar isso. Se um teste passar a depender do que a telemetria
     * faz, a porta deixou de ser unidirecional.
     *
     * <p>É esta a razão de a medição entrar por PORTA e não pelo serviço concreto: a fatia é
     * testável sem a fatia {@code telemetria} existir.
     */
    private static final class TelemetriaKaraokeSilenciosa implements TelemetriaKaraokePort {
        @Override
        public void publicarArquivo(String arquivo, int eventosEntrada, int eventosSaida,
            int camadasPareadas, int camadasInvertidas,
            java.util.List<org.traducao.projeto.novoKaraoke.domain.MedicaoEstiloKaraoke> porEstilo) {
            // inerte de propósito
        }

        @Override
        public void publicarOperacao(String operacao, java.nio.file.Path pastaOrigem,
            java.nio.file.Path pastaDestino, long duracaoMs, int arquivosProcessados) {
            // inerte de propósito
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a tipografia EMPILHADA (halo + sombra + texto, no mesmo instante) é
     * achatada para o evento legível; fala repetida NA MESMA LAYER continua intacta.
     *
     * <h2>O caso real que originou isto</h2>
     * Cartão de data do 86, três eventos na janela EXATA {@code 0:00:00.00 → 0:00:02.98}:
     * <pre>
     *   layer 0  \1a&amp;HFF&amp;  \blur5      -> preenchimento transparente = halo
     *   layer 1  \c&amp;H000000&amp;  \blur0.5  -> preto = sombra
     *   layer 2  (sem cor)               -> herda a cor do estilo = TEXTO legível
     * </pre>
     * Os três iam ao LLM separadamente e em quatro deles o modelo escreveu {@code 2049} no lugar
     * de {@code 2149}, virando pendência. Sobrevive a de MAIOR layer, que é a convenção do ASS —
     * guardar a primeira renderizaria o halo invisível; a segunda, texto preto.
     *
     * <h2>Invariantes do domínio</h2>
     * <ul>
     *   <li>Layer DIFERENTE é obrigatório: duas pessoas dizendo a mesma coisa no mesmo instante
     *       ficam na MESMA layer, e achatar isso apagaria fala. Medido no 86: com layer distinta,
     *       {@code Default} dá ZERO; sem ela, daria 2.</li>
     *   <li>O evento sobrevivente mantém tags e posição — não se remonta nada.</li>
     * </ul>
     *
     * <h2>Comportamento em caso de falha</h2>
     * Ou o cartão sai triplicado na tela, ou uma fala legítima some.
     */
    @Test
    void achataTipografiaEmpilhadaMasPreservaFalaRepetidaNaMesmaLayer() throws Exception {
        Path origem = tempDir.resolve("empilhado.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            // musica: sem ela o conversor copia o arquivo intacto e nao ha o que medir
            + "Dialogue: 0,0:01:00.00,0:01:04.00,Opening,,0,0,0,,{\\pos(100,40)}kimi no koe ga kikoeru\n"
            // cartao de data: TRES layers, mesma janela, mesmo texto -> colapsa para a layer 2
            + "Dialogue: 0,0:00:00.00,0:00:02.98,Signs,,0,0,0,,{\\1a&HFF&\\blur5}30 de julho, Ano Estelar 2149\n"
            + "Dialogue: 1,0:00:00.00,0:00:02.98,Signs,,0,0,0,,{\\c&H000000&\\blur0.5}30 de julho, Ano Estelar 2149\n"
            + "Dialogue: 2,0:00:00.00,0:00:02.98,Signs,,0,0,0,,{\\fs80}30 de julho, Ano Estelar 2149\n"
            // fala repetida na MESMA layer e no mesmo instante: NAO pode ser achatada
            + "Dialogue: 0,0:00:10.00,0:00:12.00,Default,,0,0,0,,Sim, senhor!\n"
            + "Dialogue: 0,0:00:10.00,0:00:12.00,Default,,0,0,0,,Sim, senhor!\n",
            StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);
        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);

        long cartao = saida.lines().filter(l -> l.contains("Ano Estelar 2149")).count();
        assertEquals(1, cartao,
            () -> "as tres camadas do cartao tinham de virar UMA:\n" + saida);
        assertTrue(saida.contains("{\\fs80}30 de julho, Ano Estelar 2149"),
            () -> "tem de sobreviver a de MAIOR layer (a legivel), nao o halo nem a sombra:\n" + saida);

        long falaRepetida = saida.lines().filter(l -> l.contains("Sim, senhor!")).count();
        assertEquals(2, falaRepetida,
            () -> "mesma layer NAO e empilhamento: as duas falas tinham de sobreviver:\n" + saida);
    }

    private static String cabecalho() {
        return """
            [Script Info]
            PlayResY: 1080

            [V4+ Styles]
            Format: Name,Fontname,Fontsize,PrimaryColour,SecondaryColour,OutlineColour,BackColour,Bold,Italic,Underline,StrikeOut,ScaleX,ScaleY,Spacing,Angle,BorderStyle,Outline,Shadow,Alignment,MarginL,MarginR,MarginV,Encoding
            Style: Default,Arial,48,&H00FFFFFF,&H000000FF,&H00000000,&H96000000,0,0,0,0,100,100,0,0,1,2,1,2,30,30,30,1

            [Events]
            Format: Layer,Start,End,Style,Name,MarginL,MarginR,MarginV,Effect,Text
            """;
    }

    private static final class LogStreamSilencioso extends LogStreamService {
        @Override
        public void publicarLog(String canal, String mensagem) {
            // Testes unitarios nao precisam de SSE nem arquivo de log.
        }
    }
}
