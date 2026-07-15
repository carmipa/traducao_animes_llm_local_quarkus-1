package org.traducao.projeto.remuxer.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.remuxer.domain.RemuxTarefa;
import org.traducao.projeto.remuxer.domain.PlanoRemux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapeadorMidiaServiceTest {

    private final MapeadorMidiaService mapeador = new MapeadorMidiaService();

    @Test
    void pareiaCorretamenteVideosELegendasComNomesDeAnime(@TempDir Path tempDir) throws IOException {
        Path pastaVideos = tempDir.resolve("videos");
        Path pastaLegendas = tempDir.resolve("legendas");
        Path pastaSaida = tempDir.resolve("saida");

        Files.createDirectories(pastaVideos);
        Files.createDirectories(pastaLegendas);
        Files.createDirectories(pastaSaida);

        // Criar arquivos de vídeo MKV com padrão "EpsXX" (como nos arquivos de 86 do usuário)
        Files.createFile(pastaVideos.resolve("86-Eighty-Six-Eps01-Ptbr_PTBR.mkv"));
        Files.createFile(pastaVideos.resolve("86-Eighty-Six-Eps02-Ptbr_PTBR.mkv"));

        // Criar arquivos de legenda ASS com padrão "_-_XX" e colchetes
        Files.createFile(pastaLegendas.resolve("[DB]86_-_01_(Dual Audio_10bit_BD1080p_x265)_PTBR_PT-BR.ass"));
        Files.createFile(pastaLegendas.resolve("[DB]86_-_02_(Dual Audio_10bit_BD1080p_x265)_PTBR_PT-BR.ass"));

        List<RemuxTarefa> fila = mapeador.construirFilaProcessamento(pastaVideos, pastaLegendas, pastaSaida);

        assertNotNull(fila);
        assertEquals(2, fila.size());

        // Validar pareamento do episódio 1
        RemuxTarefa tarefa1 = fila.stream()
            .filter(t -> t.nomeVideo().contains("Eps01"))
            .findFirst()
            .orElse(null);
        assertNotNull(tarefa1);
        assertEquals("[DB]86_-_01_(Dual Audio_10bit_BD1080p_x265)_PTBR_PT-BR.ass", tarefa1.caminhoLegenda().getFileName().toString());
        assertEquals("86-Eighty-Six-Eps01-Ptbr_PTBR.mkv", tarefa1.caminhoVideo().getFileName().toString());

        // Validar pareamento do episódio 2
        RemuxTarefa tarefa2 = fila.stream()
            .filter(t -> t.nomeVideo().contains("Eps02"))
            .findFirst()
            .orElse(null);
        assertNotNull(tarefa2);
        assertEquals("[DB]86_-_02_(Dual Audio_10bit_BD1080p_x265)_PTBR_PT-BR.ass", tarefa2.caminhoLegenda().getFileName().toString());
        assertEquals("86-Eighty-Six-Eps02-Ptbr_PTBR.mkv", tarefa2.caminhoVideo().getFileName().toString());
    }

    @Test
    void pareiaPorArquivoUnicoQuandoHouverApenasUmDeCada(@TempDir Path tempDir) throws IOException {
        Path pastaVideos = tempDir.resolve("videos");
        Path pastaLegendas = tempDir.resolve("legendas");
        Path pastaSaida = tempDir.resolve("saida");

        Files.createDirectories(pastaVideos);
        Files.createDirectories(pastaLegendas);
        Files.createDirectories(pastaSaida);

        // Nomes completamente diferentes, mas apenas 1 de cada na pasta
        Files.createFile(pastaVideos.resolve("Mobile_Suit_Gundam_Narrative_720p.mkv"));
        Files.createFile(pastaLegendas.resolve("[Pinkusub]Gundam_Narrative_PT-BR.ass"));

        List<RemuxTarefa> fila = mapeador.construirFilaProcessamento(pastaVideos, pastaLegendas, pastaSaida);

        assertNotNull(fila);
        assertEquals(1, fila.size());
        assertEquals("[Pinkusub]Gundam_Narrative_PT-BR.ass", fila.get(0).caminhoLegenda().getFileName().toString());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: confirma que o filme real do Gundam usa como saída o
     * nome editorial limpo da legenda, não as tags técnicas do vídeo.
     * INVARIANTES DO DOMÍNIO: release group, BD, resolução, codec, bit depth e CRC
     * não aparecem no destino.
     * COMPORTAMENTO EM CASO DE FALHA: o teste expõe regressão no nome final.
     */
    @Test
    void geraNomeFinalLimpoAPartirDaLegendaDoFilme(@TempDir Path tempDir) throws IOException {
        Path videos = Files.createDirectory(tempDir.resolve("videos"));
        Path legendas = Files.createDirectory(tempDir.resolve("legendas"));
        Path saida = Files.createDirectory(tempDir.resolve("saida"));
        Files.createFile(videos.resolve("[2ndfire]Mobile_Suit_Gundam_Narrative[BD][1080p][AV1][10bit][981A36A1].mkv"));
        Files.createFile(legendas.resolve("Mobile Suit Gundam NT (Narrative).ass"));

        PlanoRemux plano = mapeador.construirPlano(videos, legendas, saida);

        assertEquals(1, plano.tarefas().size());
        assertEquals("Mobile Suit Gundam NT (Narrative)_PTBR.mkv",
            plano.tarefas().get(0).caminhoSaida().getFileName().toString());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: impede que comparação por prefixo associe episódio 01
     * à legenda do episódio 010.
     * INVARIANTES DO DOMÍNIO: tags episódicas são comparadas integralmente.
     * COMPORTAMENTO EM CASO DE FALHA: qualquer troca entre legendas falha o teste.
     */
    @Test
    void naoConfundeEpisodio01Com010(@TempDir Path tempDir) throws IOException {
        Path videos = Files.createDirectory(tempDir.resolve("videos"));
        Path legendas = Files.createDirectory(tempDir.resolve("legendas"));
        Path saida = Files.createDirectory(tempDir.resolve("saida"));
        Files.createFile(videos.resolve("Anime - S01E01.mkv"));
        Files.createFile(videos.resolve("Anime - S01E010.mkv"));
        Files.createFile(legendas.resolve("Anime - S01E010_PT-BR.ass"));
        Files.createFile(legendas.resolve("Anime - S01E01_PT-BR.ass"));

        PlanoRemux plano = mapeador.construirPlano(videos, legendas, saida);

        assertEquals(2, plano.tarefas().size());
        assertTrue(plano.tarefas().stream().allMatch(t ->
            t.nomeVideo().contains("E010") == t.caminhoLegenda().getFileName().toString().contains("E010")));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: transforma empate entre duas legendas completas de
     * mesma prioridade em pendência explícita.
     * INVARIANTES DO DOMÍNIO: nenhuma candidata é escolhida pela ordem do disco.
     * COMPORTAMENTO EM CASO DE FALHA: tarefa criada silenciosamente falha o teste.
     */
    @Test
    void bloqueiaPareamentoAmbiguoEmVezDeEscolherPrimeira(@TempDir Path tempDir) throws IOException {
        Path videos = Files.createDirectory(tempDir.resolve("videos"));
        Path legendas = Files.createDirectory(tempDir.resolve("legendas"));
        Path saida = Files.createDirectory(tempDir.resolve("saida"));
        Files.createFile(videos.resolve("Anime - S01E01.mkv"));
        Files.createFile(legendas.resolve("Release A - S01E01_PT-BR.ass"));
        Files.createFile(legendas.resolve("Release B - S01E01_PT-BR.ass"));

        PlanoRemux plano = mapeador.construirPlano(videos, legendas, saida);

        assertTrue(plano.tarefas().isEmpty());
        assertEquals(1, plano.pareamentosAmbiguos());
        assertEquals(1, plano.avisos().size());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: prefere diálogo completo PT-BR a faixa auxiliar
     * Forced/Signs quando ambas têm o mesmo episódio.
     * INVARIANTES DO DOMÍNIO: faixa auxiliar nunca vence a completa.
     * COMPORTAMENTO EM CASO DE FALHA: escolha da faixa Forced falha o teste.
     */
    @Test
    void priorizaLegendaCompletaSobreForced(@TempDir Path tempDir) throws IOException {
        Path videos = Files.createDirectory(tempDir.resolve("videos"));
        Path legendas = Files.createDirectory(tempDir.resolve("legendas"));
        Path saida = Files.createDirectory(tempDir.resolve("saida"));
        Files.createFile(videos.resolve("Anime - S01E01.mkv"));
        Files.createFile(legendas.resolve("Anime - S01E01_PT-BR_Forced.ass"));
        Files.createFile(legendas.resolve("Anime - S01E01_PT-BR_Full.ass"));

        PlanoRemux plano = mapeador.construirPlano(videos, legendas, saida);

        assertEquals(1, plano.tarefas().size());
        assertTrue(plano.tarefas().get(0).caminhoLegenda().getFileName().toString().contains("Full"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: impede que o atalho de filme transforme uma única
     * faixa Forced em legenda completa PT-BR.
     * INVARIANTES DO DOMÍNIO: nenhuma tarefa é gerada e a ausência é didática.
     * COMPORTAMENTO EM CASO DE FALHA: pareamento indevido falha o teste.
     */
    @Test
    void bloqueiaUnicaLegendaForcedNoModoFilme(@TempDir Path tempDir) throws IOException {
        Path videos = Files.createDirectory(tempDir.resolve("videos"));
        Path legendas = Files.createDirectory(tempDir.resolve("legendas"));
        Path saida = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(videos.resolve("Filme.mkv"), "video");
        Files.writeString(legendas.resolve("Filme Forced.ass"), "forced");

        PlanoRemux plano = mapeador.construirPlano(videos, legendas, saida);

        assertTrue(plano.tarefas().isEmpty());
        assertEquals(1, plano.videosSemLegenda());
        assertTrue(plano.avisos().get(0).contains("auxiliar"));
    }
}
