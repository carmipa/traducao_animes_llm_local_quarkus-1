package org.traducao.projeto.renomearArquivos.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.renomearArquivos.domain.OperacaoRenomeacao;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.core.presentation.web.LogStreamService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RenomeadorUseCaseTest {

    RenomeadorUseCase renomeadorUseCase;
    private Path tempDir;
    private boolean telemetriaChamada = false;

    class MockTelemetriaService extends TelemetriaService {
        @Override
        public void registrarArquivosSanitizados(int quantidade) {
            if (quantidade > 0) {
                telemetriaChamada = true;
            }
        }

        /**
         * PROPÓSITO DE NEGÓCIO: impede que testes temporários contaminem o dataset
         * persistente do projeto.
         * INVARIANTES DO DOMÍNIO: a telemetria de produção continua verificada por
         * testes próprios; este mock não grava em disco.
         * COMPORTAMENTO EM CASO DE FALHA: ignora o evento deliberadamente.
         */
        @Override
        public synchronized void registrarOperacao(OperacaoTelemetria operacao) {
            // Não persiste telemetria durante teste unitário.
        }
    }

    class MockLogStream extends LogStreamService {
        @Override
        public void publicarLog(String canal, String mensagem) {
            // Ignora
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("test_renomeador");
        renomeadorUseCase = new RenomeadorUseCase();
        renomeadorUseCase.telemetriaService = new MockTelemetriaService();
        renomeadorUseCase.logStream = new MockLogStream();
        renomeadorUseCase.objectMapper = new ObjectMapper();
        telemetriaChamada = false;
    }

    @AfterEach
    void tearDown() throws IOException {
        if (renomeadorUseCase != null && tempDir != null) {
            Files.deleteIfExists(renomeadorUseCase.resolverArquivoUndo(tempDir));
        }
        Files.walk(tempDir)
            .sorted((a, b) -> b.compareTo(a))
            .forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
    }

    @Test
    void testSimularRenomeacao() throws IOException {
        Path arquivo1 = tempDir.resolve("[SubsPlease] Anime Teste - 01 (1080p).mkv");
        Files.createFile(arquivo1);

        List<OperacaoRenomeacao.ItemRenomeado> simulados = renomeadorUseCase.simularRenomeacao(tempDir, "Anime Top");

        assertEquals(1, simulados.size());
        assertEquals("[SubsPlease] Anime Teste - 01 (1080p).mkv", simulados.get(0).nomeOriginal());
        assertEquals("Anime Top - S01E01.mkv", simulados.get(0).nomeNovo());
        
        // Verifica que o arquivo não foi alterado de verdade na simulação
        assertTrue(Files.exists(arquivo1));
    }

    @Test
    void renomeiaVideosELegendasIgnorandoOutrosArquivos() throws IOException {
        Path video = tempDir.resolve("[SubsPlease] Anime Teste - 03 (1080p).mp4");
        Path legenda = tempDir.resolve("[SubsPlease] Anime Teste - 03 (1080p).ass");
        Path texto = tempDir.resolve("Anime Teste - 03.txt");
        Files.createFile(video);
        Files.createFile(legenda);
        Files.createFile(texto);

        List<OperacaoRenomeacao.ItemRenomeado> simulados = renomeadorUseCase.simularRenomeacao(tempDir, "Anime Top");

        assertEquals(2, simulados.size());
        assertEquals("[SubsPlease] Anime Teste - 03 (1080p).ass", simulados.get(0).nomeOriginal());
        assertEquals("Anime Top - S01E03.ass", simulados.get(0).nomeNovo());
        assertEquals("[SubsPlease] Anime Teste - 03 (1080p).mp4", simulados.get(1).nomeOriginal());
        assertEquals("Anime Top - S01E03.mp4", simulados.get(1).nomeNovo());
    }

    @Test
    void renomeiaLegendasExtraidasComSufixoDeTrackEIdioma() throws IOException {
        for (int episodio = 1; episodio <= 3; episodio++) {
            Files.createFile(tempDir.resolve(
                String.format("[DB]86_-_%02d_(Dual Audio_10bit_BD1080p_x265)_Track6_PT-BR.ass", episodio)
            ));
        }

        List<OperacaoRenomeacao.ItemRenomeado> simulados = renomeadorUseCase.simularRenomeacao(tempDir, "86");

        assertEquals(3, simulados.size());
        assertEquals("86 - S01E01.ass", simulados.get(0).nomeNovo());
        assertEquals("86 - S01E03.ass", simulados.get(2).nomeNovo());
    }

    @Test
    void ignoraEspeciaisCreditlessSemNumeracaoDeEpisodio() throws IOException {
        Files.createFile(tempDir.resolve("[DB]86_-_NCED01_(10bit_BD1080p_x265)_Track2_PT-BR.ass"));
        Files.createFile(tempDir.resolve("[DB]86_-_NCOP01_(10bit_BD1080p_x265)_Track2_PT-BR.ass"));
        Files.createFile(tempDir.resolve("[DB]86 Special Edition Senya ni Akaku Hinageshi no Saku_-_SP_(10bit_BD1080p_x265)_Track2_PT-BR.ass"));
        Files.createFile(tempDir.resolve("[DB]86_-_NCED01_(10bit_BD1080p_x265).mkv"));

        List<OperacaoRenomeacao.ItemRenomeado> simulados = renomeadorUseCase.simularRenomeacao(tempDir, "86");

        assertTrue(simulados.isEmpty());
    }

    @Test
    /**
     * PROPÓSITO DE NEGÓCIO: comprova que faixas diferentes não são descartadas
     * quando compartilham episódio e extensão.
     * INVARIANTES DO DOMÍNIO: ambos os destinos são únicos e preservam a faixa.
     * COMPORTAMENTO EM CASO DE FALHA: o teste falha se houver colisão silenciosa.
     */
    void preservaDuasLegendasQueGerariamOMesmoNome() throws IOException {
        Files.createFile(tempDir.resolve("[DB]86_-_01_(BD1080p)_Track5_PT-BR.ass"));
        Files.createFile(tempDir.resolve("[DB]86_-_01_(BD1080p)_Track6_PT-BR.ass"));

        List<OperacaoRenomeacao.ItemRenomeado> simulados = renomeadorUseCase.simularRenomeacao(tempDir, "86");

        assertEquals(2, simulados.size());
        assertEquals("86 - S01E01.ass", simulados.get(0).nomeNovo());
        assertEquals("86 - S01E01 - Track6 PT-BR.ass", simulados.get(1).nomeNovo());
    }

    @Test
    void renomeiaFilmeUnicoComLegendaUnicaParaNomePadrao() throws IOException {
        Files.createFile(tempDir.resolve("[2ndfire]Mobile_Suit_Gundam_Narrative[BD][1080p][AV1][10bit].mkv"));
        Files.createFile(tempDir.resolve("[2ndfire]Mobile_Suit_Gundam_Narrative[BD][1080p][AV1][10bit]_Track3.ass"));

        List<OperacaoRenomeacao.ItemRenomeado> simulados =
            renomeadorUseCase.simularRenomeacao(tempDir, "Mobile Suit Gundam Narrative");

        assertEquals(2, simulados.size());
        assertEquals("Mobile Suit Gundam Narrative.mkv", simulados.get(0).nomeNovo());
        assertEquals("Mobile Suit Gundam Narrative.ass", simulados.get(1).nomeNovo());
    }

    @Test
    void simulaNomesDeTrackerComUnderlineSemConfundirTitulo86ComEpisodio() throws IOException {
        for (int episodio = 1; episodio <= 11; episodio++) {
            Files.createFile(tempDir.resolve(
                String.format("[DB]86_-_%02d_(Dual Audio_10bit_BD1080p_x265)_PTBR.mkv", episodio)
            ));
        }

        List<OperacaoRenomeacao.ItemRenomeado> simulados =
            renomeadorUseCase.simularRenomeacao(tempDir, "86 (Eighty-Six) - Temp1");

        assertEquals(11, simulados.size());
        assertEquals("[DB]86_-_01_(Dual Audio_10bit_BD1080p_x265)_PTBR.mkv", simulados.get(0).nomeOriginal());
        assertEquals("86 (Eighty-Six) - Temp1 - S01E01.mkv", simulados.get(0).nomeNovo());
        assertEquals("[DB]86_-_11_(Dual Audio_10bit_BD1080p_x265)_PTBR.mkv", simulados.get(10).nomeOriginal());
        assertEquals("86 (Eighty-Six) - Temp1 - S01E11.mkv", simulados.get(10).nomeNovo());
    }

    @Test
    void renomeiaFilmeUnicoSemEpisodioParaNomePadrao() throws IOException {
        Path filme = tempDir.resolve("[2ndfire]Mobile_Suit_Gundam_Narrative[BD][1080p][AV1][10bit][981A36A1].mkv");
        Files.createFile(filme);

        List<OperacaoRenomeacao.ItemRenomeado> simulados =
            renomeadorUseCase.simularRenomeacao(tempDir, "Mobile Suit Gundam Narrative");

        assertEquals(1, simulados.size());
        assertEquals("[2ndfire]Mobile_Suit_Gundam_Narrative[BD][1080p][AV1][10bit][981A36A1].mkv", simulados.get(0).nomeOriginal());
        assertEquals("Mobile Suit Gundam Narrative.mkv", simulados.get(0).nomeNovo());
    }

    @Test
    void ignoraMultiplosVideosSemEpisodioParaEvitarColisaoDeFilmes() throws IOException {
        Files.createFile(tempDir.resolve("[Grupo] Filme Um [BD 1080p].mkv"));
        Files.createFile(tempDir.resolve("[Grupo] Filme Dois [BD 1080p].mkv"));

        List<OperacaoRenomeacao.ItemRenomeado> simulados =
            renomeadorUseCase.simularRenomeacao(tempDir, "Nome Padrao");

        assertTrue(simulados.isEmpty());
    }

    @Test
    void testAplicarRenomeacaoEBackup() throws IOException {
        Path arquivo1 = tempDir.resolve("[SubsPlease] Anime Teste - 02 (1080p).mkv");
        Files.createFile(arquivo1);

        renomeadorUseCase.aplicarRenomeacao(tempDir, "Anime Top");

        // Verifica que o arquivo antigo não existe e o novo existe
        assertFalse(Files.exists(arquivo1));
        assertTrue(Files.exists(tempDir.resolve("Anime Top - S01E02.mkv")));

        // O manifesto de undo é operacional e deve ficar dentro do projeto,
        // nunca misturado na pasta de mídia que está sendo renomeada.
        Path backupAntigoNaPastaMidia = tempDir.resolve(".kronos_undo_renomeacao.json");
        Path manifestoProjeto = renomeadorUseCase.resolverArquivoUndo(tempDir);
        assertFalse(Files.exists(backupAntigoNaPastaMidia));
        assertTrue(Files.exists(manifestoProjeto));
        assertTrue(manifestoProjeto.startsWith(TelemetriaService.resolverPastaArtefatosOperacionais("renomear-arquivos")));

        assertTrue(telemetriaChamada);
    }

    @Test
    void reverterUsaManifestoSalvoDentroDoProjeto() throws IOException {
        Path arquivoOriginal = tempDir.resolve("[SubsPlease] Anime Teste - 04 (1080p).mkv");
        Path arquivoRenomeado = tempDir.resolve("Anime Top - S01E04.mkv");
        Path manifestoProjeto = renomeadorUseCase.resolverArquivoUndo(tempDir);
        Files.createFile(arquivoOriginal);

        renomeadorUseCase.aplicarRenomeacao(tempDir, "Anime Top");

        assertFalse(Files.exists(arquivoOriginal));
        assertTrue(Files.exists(arquivoRenomeado));
        assertTrue(Files.exists(manifestoProjeto));

        renomeadorUseCase.reverterRenomeacao(tempDir);

        assertTrue(Files.exists(arquivoOriginal));
        assertFalse(Files.exists(arquivoRenomeado));
        assertFalse(Files.exists(manifestoProjeto));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante que uma obra identificada como quarta
     * temporada produza S04, não o antigo S01 fixo.
     * INVARIANTES DO DOMÍNIO: episódio e extensão permanecem preservados.
     * COMPORTAMENTO EM CASO DE FALHA: o teste expõe regressão de temporada.
     */
    @Test
    void usaTemporadaInformadaNoNomeEpisodico() throws IOException {
        Files.createFile(tempDir.resolve("[Grupo] DanMachi - 01 [1080p].mkv"));

        var resultado = renomeadorUseCase.simularComResultado(tempDir, "DanMachi Season 4", 4);

        assertEquals("DanMachi Season 4 - S04E01.mkv", resultado.itens().get(0).nomeNovo());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: evita converter ano ou número do título de filme em
     * episódio quando existe apenas um vídeo sem marcador explícito.
     * INVARIANTES DO DOMÍNIO: filme recebe somente nome padrão e extensão.
     * COMPORTAMENTO EM CASO DE FALHA: o teste captura nomes S01E2024 incorretos.
     */
    @Test
    void filmeUnicoComAnoNaoViraEpisodio() throws IOException {
        Files.createFile(tempDir.resolve("[Grupo] Filme Gundam 2024 [1080p].mkv"));

        List<OperacaoRenomeacao.ItemRenomeado> simulados =
            renomeadorUseCase.simularRenomeacao(tempDir, "Filme Gundam 2024");

        assertEquals(1, simulados.size());
        assertEquals("Filme Gundam 2024.mkv", simulados.get(0).nomeNovo());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: permite usar diretamente títulos oficiais que trazem
     * pontuação editorial incompatível com nomes de arquivo do Windows.
     * INVARIANTES DO DOMÍNIO: dois-pontos e interrogação são normalizados, sem
     * alterar episódio, extensão ou pasta de destino.
     * COMPORTAMENTO EM CASO DE FALHA: regressão volta a lançar validação ou gera
     * um nome contendo caractere proibido pelo Windows.
     */
    @Test
    void normalizaPontuacaoWindowsDoNomePadrao() throws IOException {
        Files.createFile(tempDir.resolve("[Grupo] Gundam - 01 [1080p].mkv"));

        var resultado = renomeadorUseCase.simularComResultado(
            tempDir, "Mobile Suit Gundam: The 08th MS Team?", 1);

        assertEquals("Mobile Suit Gundam - The 08th MS Team - S01E01.mkv",
            resultado.itens().getFirst().nomeNovo());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aceita títulos oficiais com barra sem interpretar o
     * título como subdiretório.
     * INVARIANTES DO DOMÍNIO: o destino permanece filho direto da pasta escolhida.
     * COMPORTAMENTO EM CASO DE FALHA: uma barra não normalizada criaria caminho
     * aninhado ou seria rejeitada desnecessariamente.
     */
    @Test
    void normalizaBarraEditorialSemCriarSubdiretorio() throws IOException {
        Files.createFile(tempDir.resolve("[Grupo] Fate - 01 [1080p].mkv"));

        var resultado = renomeadorUseCase.simularComResultado(tempDir, "Fate/stay night", 1);

        assertEquals("Fate - stay night - S01E01.mkv", resultado.itens().getFirst().nomeNovo());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: bloqueia traversal e caracteres que poderiam mover a
     * mídia para fora da pasta selecionada.
     * INVARIANTES DO DOMÍNIO: arquivo original permanece intacto.
     * COMPORTAMENTO EM CASO DE FALHA: espera erro de validação antes do dry-run.
     */
    @Test
    void rejeitaNomePadraoComTraversal() throws IOException {
        Path original = tempDir.resolve("Anime - 01.mkv");
        Files.createFile(original);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
            () -> renomeadorUseCase.aplicarRenomeacao(tempDir, "..\\fora"));

        assertTrue(erro.getMessage().contains("inválido"));
        assertTrue(Files.exists(original));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: comprova que reversão retomada reconhece itens já no
     * nome original e conclui os demais sem deixar manifesto preso.
     * INVARIANTES DO DOMÍNIO: ao final todos os originais existem e o undo some.
     * COMPORTAMENTO EM CASO DE FALHA: manifesto remanescente caracteriza regressão.
     */
    @Test
    void reversaoParcialPodeSerRetomadaEAutolimpaManifesto() throws IOException {
        Path original1 = tempDir.resolve("Anime - 01.mkv");
        Path original2 = tempDir.resolve("Anime - 02.mkv");
        Files.createFile(original1);
        Files.createFile(original2);
        renomeadorUseCase.aplicarRenomeacao(tempDir, "Anime Top");
        Path novo1 = tempDir.resolve("Anime Top - S01E01.mkv");
        Files.move(novo1, original1);

        var resultado = renomeadorUseCase.reverterRenomeacao(tempDir);

        assertEquals("CONCLUIDO", resultado.status());
        assertTrue(Files.exists(original1));
        assertTrue(Files.exists(original2));
        assertFalse(Files.exists(renomeadorUseCase.resolverArquivoUndo(tempDir)));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: impede que manifesto adulterado use caminhos relativos
     * para movimentar arquivo fora da pasta autorizada.
     * INVARIANTES DO DOMÍNIO: nenhum caminho externo é criado ou alterado.
     * COMPORTAMENTO EM CASO DE FALHA: reversão retorna FALHOU e preserva manifesto.
     */
    @Test
    void rejeitaManifestoComCaminhoForaDaPasta() throws IOException {
        Path manifesto = renomeadorUseCase.resolverArquivoUndo(tempDir);
        Files.createDirectories(manifesto.getParent());
        OperacaoRenomeacao adulterada = new OperacaoRenomeacao(
            "teste", "2026-07-13T00:00:00Z", tempDir.toString(),
            List.of(new OperacaoRenomeacao.ItemRenomeado("../fora.mkv", "Anime.mkv")));
        renomeadorUseCase.objectMapper.writeValue(manifesto.toFile(), adulterada);

        var resultado = renomeadorUseCase.reverterRenomeacao(tempDir);

        assertEquals("FALHOU", resultado.status());
        assertTrue(Files.exists(manifesto));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: comprova que duas operações não podem atuar ao mesmo
     * tempo sobre a mesma pasta.
     * INVARIANTES DO DOMÍNIO: a segunda operação é recusada antes de executar o
     * dry-run e a primeira conclui após a liberação controlada.
     * COMPORTAMENTO EM CASO DE FALHA: o executor é encerrado mesmo se a asserção
     * falhar, evitando thread pendurada na suíte.
     */
    @Test
    void bloqueiaOperacaoConcorrenteNaMesmaPasta() throws Exception {
        Files.createFile(tempDir.resolve("Anime - 01.mkv"));
        CountDownLatch iniciou = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        renomeadorUseCase.logStream = new LogStreamService() {
            /**
             * PROPÓSITO DE NEGÓCIO: pausa a primeira simulação dentro do bloqueio
             * para tornar a concorrência reproduzível.
             * INVARIANTES DO DOMÍNIO: apenas a mensagem inicial realiza a espera.
             * COMPORTAMENTO EM CASO DE FALHA: restaura a interrupção da thread.
             */
            @Override
            public void publicarLog(String canal, String mensagem) {
                if (mensagem.startsWith("Iniciando simulação")) {
                    iniciou.countDown();
                    try {
                        liberar.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        };
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> primeira = executor.submit(() -> renomeadorUseCase.simularRenomeacao(tempDir, "Anime"));
            assertTrue(iniciou.await(2, TimeUnit.SECONDS));

            assertThrows(OperacaoRenomeacaoEmAndamentoException.class,
                () -> renomeadorUseCase.simularRenomeacao(tempDir, "Anime"));

            liberar.countDown();
            primeira.get(5, TimeUnit.SECONDS);
        } finally {
            liberar.countDown();
            executor.shutdownNow();
        }
    }
}
