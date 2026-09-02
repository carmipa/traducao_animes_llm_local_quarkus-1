package org.traducao.projeto.remuxer.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.remuxer.domain.PastaDeLegendaDoRemux;
import org.traducao.projeto.remuxer.domain.RelatorioRemux;
import org.traducao.projeto.remuxer.domain.RemuxTarefa;
import org.traducao.projeto.remuxer.domain.RemuxerException;
import org.traducao.projeto.remuxer.infrastructure.adapters.MkvmergeAdapter;
import org.traducao.projeto.remuxer.infrastructure.config.RemuxerProperties;
import org.traducao.projeto.remuxer.presentation.ui.ConsoleRemuxerLogger;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que o operador pode escolher onde os MKVs finais são gravados
 * (pedido do Paulo em 2026-09-02) SEM perder o comportamento padrão e sem abrir a porta para o
 * remuxado virar entrada da execução seguinte.
 *
 * <p>INVARIANTES DO DOMÍNIO (INV-REMUX-DESTINO-001): campo vazio ⇒
 * {@code <pasta de vídeos>/mkv_final_ptbr}; campo preenchido ⇒ exatamente a pasta escolhida; e o
 * destino NUNCA é a pasta de vídeos nem a de legendas.
 *
 * <p>Por que o negativo é o teste importante: o dano não é sobrescrita — disso o
 * {@code SaidaRemuxJaExisteException} já cuidava. É o operador escolher a própria pasta de
 * vídeos, achando razoável ("quero o arquivo aqui do lado"), e na execução SEGUINTE o
 * {@code X_PTBR.mkv} ser lido como vídeo de entrada. Nada falha, nada avisa, e o acervo passa a
 * ter remux de remux. É engano de boa-fé no sentido exato da regra 15.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: destino recusado vira erro de infraestrutura no relatório,
 * o lote não começa e nenhum arquivo é criado.
 */
class DestinoEscolhidoDoRemuxTest {

    /**
     * PROPÓSITO DE NEGÓCIO: quem não mexe no campo novo não vê diferença nenhuma.
     * INVARIANTES DO DOMÍNIO: a subpasta padrão é criada dentro da pasta de vídeos.
     * COMPORTAMENTO EM CASO DE FALHA: MKV em outro lugar reprova.
     */
    @Test
    void semEscolhaPublicaNaSubpastaPadraoDentroDosVideos(@TempDir Path tempDir) throws IOException {
        Cenario cenario = montar(tempDir);

        RelatorioRemux relatorio = useCase(cenario).executar(cenario.videos, cenario.legendas, 0, null);

        Path esperado = cenario.videos.resolve(RemuxarLoteUseCase.PASTA_SAIDA_PADRAO)
            .resolve("Anime - S01E01_PTBR.mkv");
        assertEquals("CONCLUIDO", relatorio.getStatusFinal());
        assertTrue(Files.exists(esperado), "o padrão histórico tem de continuar valendo: " + esperado);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: pasta escolhida no Explorer é honrada tal como veio.
     * INVARIANTES DO DOMÍNIO: nada é gravado na subpasta padrão quando há escolha.
     * COMPORTAMENTO EM CASO DE FALHA: arquivo nos dois lugares, ou em nenhum, reprova.
     */
    @Test
    void escolhaExplicitaPublicaNaPastaEscolhida(@TempDir Path tempDir) throws IOException {
        Cenario cenario = montar(tempDir);
        Path destino = Files.createDirectories(tempDir.resolve("HD-externo").resolve("prontos"));

        RelatorioRemux relatorio = useCase(cenario).executar(cenario.videos, cenario.legendas, 0, destino);

        assertEquals("CONCLUIDO", relatorio.getStatusFinal());
        assertTrue(Files.exists(destino.resolve("Anime - S01E01_PTBR.mkv")));
        assertFalse(Files.exists(cenario.videos.resolve(RemuxarLoteUseCase.PASTA_SAIDA_PADRAO)),
            "com destino escolhido, a subpasta padrão não deve nem ser criada");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CASO-CONTROLE do invariante. A pasta de vídeos é o diretório varrido
     * em busca de entrada; publicar nela é plantar a entrada da próxima execução.
     * INVARIANTES DO DOMÍNIO: o lote nem começa — o adaptador não é chamado.
     * COMPORTAMENTO EM CASO DE FALHA: qualquer MKV criado reprova.
     */
    @Test
    void recusaDestinoIgualAPastaDeVideos(@TempDir Path tempDir) throws IOException {
        Cenario cenario = montar(tempDir);

        RelatorioRemux relatorio = useCase(cenario).executar(cenario.videos, cenario.legendas, 0, cenario.videos);

        assertEquals("CONCLUIDO_COM_FALHAS", relatorio.getStatusFinal());
        assertEquals(0, relatorio.getMkvProcessadosSucesso());
        assertTrue(cenario.adapter.chamadas.isEmpty(), "o adaptador não pode ser acionado");
        assertFalse(Files.exists(cenario.videos.resolve("Anime - S01E01_PTBR.mkv")));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mesma recusa quando o destino é a pasta das legendas — misturar MKV
     * com os .ass curados confunde o operador e a auto-detecção da própria tela.
     * INVARIANTES DO DOMÍNIO: falha fechada antes de qualquer escrita.
     * COMPORTAMENTO EM CASO DE FALHA: sucesso registrado reprova.
     */
    @Test
    void recusaDestinoIgualAPastaDeLegendas(@TempDir Path tempDir) throws IOException {
        Cenario cenario = montar(tempDir);

        RelatorioRemux relatorio = useCase(cenario).executar(cenario.videos, cenario.legendas, 0, cenario.legendas);

        assertEquals("CONCLUIDO_COM_FALHAS", relatorio.getStatusFinal());
        assertTrue(cenario.adapter.chamadas.isEmpty());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a recusa não pode ser driblada por caminho equivalente escrito de
     * outro jeito — {@code C:\a\b} e {@code C:\a\.\b} são o mesmo diretório.
     * INVARIANTES DO DOMÍNIO: a comparação é por caminho absoluto normalizado.
     * COMPORTAMENTO EM CASO DE FALHA: aceitar o caminho com "." reprova.
     */
    @Test
    void recusaDestinoEquivalenteEscritoComPontoNoMeio(@TempDir Path tempDir) throws IOException {
        Cenario cenario = montar(tempDir);
        Path disfarcado = cenario.videos.resolve(".").resolve("subpasta").resolve("..");

        RelatorioRemux relatorio = useCase(cenario).executar(cenario.videos, cenario.legendas, 0, disfarcado);

        assertEquals("CONCLUIDO_COM_FALHAS", relatorio.getStatusFinal());
        assertTrue(cenario.adapter.chamadas.isEmpty());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: congela o nome de pasta que a auto-detecção da tela procura, medido
     * no acervo em 2026-09-02 (20 {@code traducao_ptbr} contra ZERO {@code legendas pt}).
     * INVARIANTES DO DOMÍNIO: o nome que o pipeline gera é candidato; pasta de experimento não é.
     * COMPORTAMENTO EM CASO DE FALHA: aceitar {@code traducao_aya} reprova — escolher a baseline
     * de um confronto de modelos por acaso é o dano que esta linha impede.
     */
    @Test
    void autoDeteccaoConheceONomeQueOPipelineRealmenteGera() {
        assertTrue(PastaDeLegendaDoRemux.ehCandidata("traducao_ptbr"));
        assertTrue(PastaDeLegendaDoRemux.ehCandidata("TRADUCAO-PTBR"));
        assertTrue(PastaDeLegendaDoRemux.ehCandidata("legenda_ptbr"));
        assertTrue(PastaDeLegendaDoRemux.ehCandidata("legendas pt"),
            "o contrato antigo da tela continua aceito");

        assertFalse(PastaDeLegendaDoRemux.ehCandidata("traducao_aya"));
        assertFalse(PastaDeLegendaDoRemux.ehCandidata("traducao_mistral"));
        assertFalse(PastaDeLegendaDoRemux.ehCandidata("traducao_ptbr_sem_lore"));
        assertFalse(PastaDeLegendaDoRemux.ehCandidata("legendas_eng"));
        assertFalse(PastaDeLegendaDoRemux.ehCandidata(null));

        assertTrue(PastaDeLegendaDoRemux.preferencia("traducao_ptbr")
                < PastaDeLegendaDoRemux.preferencia("legendas pt"),
            "traducao_ptbr tem de vencer quando as duas existem na mesma obra");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: monta uma obra mínima com um vídeo e uma legenda válida.
     * INVARIANTES DO DOMÍNIO: pastas separadas para vídeos e legendas.
     * COMPORTAMENTO EM CASO DE FALHA: propaga I/O e interrompe o teste.
     */
    private Cenario montar(Path tempDir) throws IOException {
        Path videos = Files.createDirectories(tempDir.resolve("obra"));
        Path legendas = Files.createDirectories(videos.resolve(PastaDeLegendaDoRemux.PADRAO));
        Files.writeString(videos.resolve("Anime - S01E01.mkv"), "VIDEO");
        Files.writeString(legendas.resolve("Anime - S01E01.ass"),
            "[Script Info]\n[Events]\nDialogue: 0,0:00:00.00,0:00:01.00,Default,,0,0,0,,Teste");
        return new Cenario(videos, legendas, new AdapterEspiao());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: monta o caso de uso com o espião no lugar do mkvmerge.
     * INVARIANTES DO DOMÍNIO: telemetria não interfere no resultado do lote.
     * COMPORTAMENTO EM CASO DE FALHA: nenhuma — construção pura.
     */
    private RemuxarLoteUseCase useCase(Cenario cenario) {
        TelemetriaService telemetria = new TelemetriaService() {
            /** PROPÓSITO DE NEGÓCIO: descarta a telemetria do teste. INVARIANTES DO DOMÍNIO: não persiste. COMPORTAMENTO EM CASO DE FALHA: não lança. */
            @Override
            public void registrarOperacao(OperacaoTelemetria operacao) {
            }
        };
        return new RemuxarLoteUseCase(cenario.adapter, new MapeadorMidiaService(),
            new ConsoleRemuxerLogger(), telemetria);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: registra se o adaptador chegou a ser acionado e simula publicação.
     * INVARIANTES DO DOMÍNIO: escreve exatamente no destino planejado pelo caso de uso.
     * COMPORTAMENTO EM CASO DE FALHA: converte I/O em erro de domínio.
     */
    private static class AdapterEspiao extends MkvmergeAdapter {
        private final List<Path> chamadas = new ArrayList<>();

        AdapterEspiao() {
            super(new RemuxerProperties("mkvmerge"));
        }

        /** PROPÓSITO DE NEGÓCIO: mkvmerge sempre disponível no teste. INVARIANTES DO DOMÍNIO: não executa processo. COMPORTAMENTO EM CASO DE FALHA: não lança. */
        @Override
        public void validarInfraestrutura() {
        }

        /** PROPÓSITO DE NEGÓCIO: publica onde o caso de uso mandou. INVARIANTES DO DOMÍNIO: registra a chamada. COMPORTAMENTO EM CASO DE FALHA: erro de domínio. */
        @Override
        public void executarRemux(RemuxTarefa tarefa, long sincronismoMs) {
            chamadas.add(tarefa.caminhoSaida());
            try {
                Files.writeString(tarefa.caminhoSaida(), "MKV_VALIDADO");
            } catch (IOException e) {
                throw new RemuxerException("falha fake", e);
            }
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: agrupa as pastas e o espião de um cenário.
     * INVARIANTES DO DOMÍNIO: os três campos nascem preenchidos.
     * COMPORTAMENTO EM CASO DE FALHA: N/A.
     */
    private record Cenario(Path videos, Path legendas, AdapterEspiao adapter) {
    }
}
