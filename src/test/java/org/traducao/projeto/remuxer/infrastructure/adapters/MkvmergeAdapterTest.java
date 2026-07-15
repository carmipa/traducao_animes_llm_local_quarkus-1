package org.traducao.projeto.remuxer.infrastructure.adapters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.core.util.ProcessoExternoUtil;
import org.traducao.projeto.remuxer.domain.RemuxTarefa;
import org.traducao.projeto.remuxer.domain.RemuxerException;
import org.traducao.projeto.remuxer.domain.SaidaRemuxJaExisteException;
import org.traducao.projeto.remuxer.infrastructure.config.RemuxerProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MkvmergeAdapterTest {
    private static final byte[] IDENTIFICACAO_VALIDA = ("{\"tracks\":["
        + "{\"id\":0,\"type\":\"video\",\"properties\":{}},"
        + "{\"id\":1,\"type\":\"audio\",\"properties\":{}},"
        + "{\"id\":2,\"type\":\"subtitles\",\"properties\":{\"language\":\"por\",\"language_ietf\":\"pt-BR\"}}]}")
        .getBytes(StandardCharsets.UTF_8);

    /**
     * PROPÓSITO DE NEGÓCIO: comprova que destino anterior é barreira absoluta e
     * nunca é apagado nem substituído.
     * INVARIANTES DO DOMÍNIO: runner externo não chega a ser chamado.
     * COMPORTAMENTO EM CASO DE FALHA: conteúdo original deve permanecer idêntico.
     */
    @Test
    void preservaDestinoExistenteSemExecutarMkvmerge(@TempDir Path tempDir) throws IOException {
        RemuxTarefa tarefa = criarTarefa(tempDir);
        Files.writeString(tarefa.caminhoSaida(), "MKV_VALIDO_ANTERIOR");
        MkvmergeAdapter adapter = new MkvmergeAdapter(new RemuxerProperties("mkvmerge"),
            (comando, timeout, mesclar) -> { throw new AssertionError("runner não deveria ser chamado"); });

        assertThrows(SaidaRemuxJaExisteException.class, () -> adapter.executarRemux(tarefa, 0, false));

        assertEquals("MKV_VALIDO_ANTERIOR", Files.readString(tarefa.caminhoSaida()));
        assertTrue(listarParciais(tempDir).isEmpty());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: valida o caminho feliz temporário→inspeção→publicação
     * e a metadata genérica PT-BR.
     * INVARIANTES DO DOMÍNIO: comando escreve em .part UUID, não no destino.
     * COMPORTAMENTO EM CASO DE FALHA: ausência de final ou parcial restante falha.
     */
    @Test
    void publicaSomenteDepoisDeValidarTemporario(@TempDir Path tempDir) throws Exception {
        RemuxTarefa tarefa = criarTarefa(tempDir);
        List<List<String>> comandos = new ArrayList<>();
        MkvmergeAdapter adapter = new MkvmergeAdapter(new RemuxerProperties("mkvmerge"),
            (comando, timeout, mesclar) -> executarFake(comando, comandos, 0));

        adapter.executarRemux(tarefa, 350, false);

        assertTrue(Files.exists(tarefa.caminhoSaida()));
        assertTrue(listarParciais(tempDir).isEmpty());
        List<String> remux = localizarComandoRemux(comandos);
        assertTrue(remux.contains("--no-subtitles"));
        assertTrue(remux.contains("0:pt-BR"));
        assertTrue(remux.contains("0:Português (Brasil)"));
        assertTrue(remux.contains("0:350"));
        assertNotEquals(tarefa.caminhoSaida().toString(), remux.get(remux.indexOf("-o") + 1));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: comprova que a opção de preservação remove somente a
     * flag que descartaria legendas originais.
     * INVARIANTES DO DOMÍNIO: nova PT-BR continua padrão e identificada.
     * COMPORTAMENTO EM CASO DE FALHA: presença de --no-subtitles falha o teste.
     */
    @Test
    void preservaLegendasOriginaisQuandoSolicitado(@TempDir Path tempDir) throws Exception {
        RemuxTarefa tarefa = criarTarefa(tempDir);
        List<List<String>> comandos = new ArrayList<>();
        MkvmergeAdapter adapter = new MkvmergeAdapter(new RemuxerProperties("mkvmerge"),
            (comando, timeout, mesclar) -> executarFake(comando, comandos, 0));

        adapter.executarRemux(tarefa, 0, true);

        List<String> remux = localizarComandoRemux(comandos);
        assertFalse(remux.contains("--no-subtitles"));
        assertTrue(remux.contains("2:0"));
        assertTrue(remux.contains("0:1"));
        assertTrue(Files.exists(tarefa.caminhoSaida()));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante cleanup do parcial quando mkvmerge retorna
     * erro e ausência de publicação final.
     * INVARIANTES DO DOMÍNIO: somente temporário desta tentativa é removido.
     * COMPORTAMENTO EM CASO DE FALHA: parcial remanescente falha o teste.
     */
    @Test
    void removeSomenteTemporarioQuandoMkvmergeFalha(@TempDir Path tempDir) throws Exception {
        RemuxTarefa tarefa = criarTarefa(tempDir);
        MkvmergeAdapter adapter = new MkvmergeAdapter(new RemuxerProperties("mkvmerge"),
            (comando, timeout, mesclar) -> executarFake(comando, new ArrayList<>(), 2));

        assertThrows(RemuxerException.class, () -> adapter.executarRemux(tarefa, 0, false));

        assertFalse(Files.exists(tarefa.caminhoSaida()));
        assertTrue(listarParciais(tempDir).isEmpty());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante cleanup cooperativo quando a fila interrompe
     * o processo externo.
     * INVARIANTES DO DOMÍNIO: interrupção da thread é restaurada pelo adaptador.
     * COMPORTAMENTO EM CASO DE FALHA: limpa a flag ao final para não contaminar a suíte.
     */
    @Test
    void cancelamentoRemoveTemporarioERestauraInterrupcao(@TempDir Path tempDir) throws Exception {
        RemuxTarefa tarefa = criarTarefa(tempDir);
        MkvmergeAdapter adapter = new MkvmergeAdapter(new RemuxerProperties("mkvmerge"),
            (comando, timeout, mesclar) -> {
                Path parcial = Path.of(comando.get(comando.indexOf("-o") + 1));
                Files.writeString(parcial, "PARCIAL");
                throw new InterruptedException("cancelado");
            });
        try {
            assertThrows(RemuxerException.class, () -> adapter.executarRemux(tarefa, 0, false));
            assertTrue(Thread.currentThread().isInterrupted());
            assertFalse(Files.exists(tarefa.caminhoSaida()));
            assertTrue(listarParciais(tempDir).isEmpty());
        } finally {
            Thread.interrupted();
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: cria uma tarefa mínima isolada para provar as regras
     * de publicação do adaptador.
     * INVARIANTES DO DOMÍNIO: origem, legenda e destino ficam no diretório temporário.
     * COMPORTAMENTO EM CASO DE FALHA: propaga I/O e interrompe o teste.
     */
    private RemuxTarefa criarTarefa(Path pasta) throws IOException {
        Path video = Files.writeString(pasta.resolve("video.mkv"), "VIDEO");
        Path legenda = Files.writeString(pasta.resolve("Legenda.ass"), "[Script Info]\n[Events]\nDialogue: 0,0:00:00.00,0:00:01.00,Default,,0,0,0,,Teste");
        return new RemuxTarefa(video.getFileName().toString(), video, legenda, pasta.resolve("Legenda_PTBR.mkv"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: simula identificação e geração de MKV sem ferramenta externa.
     * INVARIANTES DO DOMÍNIO: toda chamada é registrada e remux cria somente o parcial.
     * COMPORTAMENTO EM CASO DE FALHA: propaga I/O para o fluxo produtivo tratar.
     */
    private ProcessoExternoUtil.Resultado executarFake(List<String> comando, List<List<String>> comandos, int codigo)
            throws IOException {
        comandos.add(List.copyOf(comando));
        if (comando.contains("-J")) {
            return new ProcessoExternoUtil.Resultado(0, IDENTIFICACAO_VALIDA, new byte[0]);
        }
        Path parcial = Path.of(comando.get(comando.indexOf("-o") + 1));
        Files.writeString(parcial, "MKV_TEMPORARIO");
        return new ProcessoExternoUtil.Resultado(codigo, "saida fake".getBytes(StandardCharsets.UTF_8), new byte[0]);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: detecta resíduos parciais após cada cenário.
     * INVARIANTES DO DOMÍNIO: considera somente nomes exclusivos {@code .part-}.
     * COMPORTAMENTO EM CASO DE FALHA: propaga I/O e falha o teste.
     */
    private List<Path> listarParciais(Path pasta) throws IOException {
        try (var stream = Files.list(pasta)) {
            return stream.filter(p -> p.getFileName().toString().contains(".part-")).toList();
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: separa a chamada de remux das inspeções JSON registradas.
     * INVARIANTES DO DOMÍNIO: comando produtivo contém exatamente a opção {@code -o}.
     * COMPORTAMENTO EM CASO DE FALHA: lança erro de asserção se o remux não ocorreu.
     */
    private List<String> localizarComandoRemux(List<List<String>> comandos) {
        return comandos.stream().filter(comando -> comando.contains("-o")).findFirst()
            .orElseThrow(() -> new AssertionError("comando de remux não registrado"));
    }
}
