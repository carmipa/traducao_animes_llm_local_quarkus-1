package org.traducao.projeto.traducao.infrastructure.telemetria;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.core.io.DiretorioBaseKronos;
import org.traducao.projeto.traducao.domain.TelemetriaTraducao;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: o arquivo canônico da telemetria é uma FOTO — o banco é chaveado pelo
 * nome do episódio, então retraduzir o mesmo episódio apaga a medição anterior. Em 2026-07-23
 * isso foi confirmado nos dados reais: 155 registros para 155 episódios distintos, zero
 * repetições, e as medições do 08th MS Team de 2026-07-22 sumiram quando os episódios foram
 * retraduzidos. Sem histórico não existe a pergunta científica "esta mudança melhorou?", porque
 * o segundo ponto de medida destrói o primeiro.
 *
 * <p>Este teste trava o histórico APPEND-ONLY que passa a existir ao lado da foto: cada execução
 * vira uma linha própria, e retraduzir ACRESCENTA em vez de substituir.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Duas execuções do MESMO episódio geram DUAS linhas, cada uma com seu carimbo.</li>
 *   <li>A foto continua sendo foto: uma entrada por episódio, com a execução mais recente.</li>
 *   <li>O histórico é gravado em {@code logs/}, sob a raiz operacional redirecionável — nunca
 *       no diretório real do usuário durante a suíte.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Se o histórico voltar a ter uma linha por episódio, a série temporal foi perdida de novo e o
 * dataset volta a ser incapaz de comparar execuções.
 */
class HistoricoExecucoesTelemetriaTest {

    @TempDir
    Path raiz;

    private String baseAnterior;

    @BeforeEach
    void redirecionarRaizOperacional() {
        baseAnterior = System.getProperty(DiretorioBaseKronos.PROPRIEDADE_BASE);
        System.setProperty(DiretorioBaseKronos.PROPRIEDADE_BASE, raiz.toString());
    }

    @AfterEach
    void restaurarRaizOperacional() {
        if (baseAnterior == null) {
            System.clearProperty(DiretorioBaseKronos.PROPRIEDADE_BASE);
        } else {
            System.setProperty(DiretorioBaseKronos.PROPRIEDADE_BASE, baseAnterior);
        }
    }

    private static TelemetriaTraducao execucao(String episodio, String quando, String status) {
        return new TelemetriaTraducao(
            episodio, "modelo-teste", 100, 90, 10, 1234L,
            List.of(), "AnimeTeste", "S01", quando, "Lore Teste", status, List.of());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o caso que motivou tudo — o MESMO episódio traduzido duas vezes.
     * <p>INVARIANTES DO DOMÍNIO: a foto guarda a última; o histórico guarda as duas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: histórico com uma linha só = série temporal perdida.
     */
    @Test
    void retraduzirOMesmoEpisodioAcrescentaLinhaEmVezDeSubstituir() throws Exception {
        TelemetriaTraducaoAdapter adapter = new TelemetriaTraducaoAdapter(new ObjectMapper());

        adapter.registrarTraducao(execucao("ep01.ass", "2026-07-22T18:55:00Z", "PARCIAL"));
        adapter.registrarTraducao(execucao("ep01.ass", "2026-07-23T07:35:00Z", "CONCLUIDO"));

        Path historico = raiz.resolve("logs").resolve(TelemetriaTraducaoAdapter.NOME_ARQUIVO_HISTORICO);
        assertTrue(Files.exists(historico), "o historico append-only deve ter sido criado");
        List<String> linhas = Files.readAllLines(historico, StandardCharsets.UTF_8);

        assertEquals(2, linhas.size(),
            "duas execucoes do mesmo episodio = duas linhas; uma so significa que a medicao "
                + "anterior foi destruida, que e exatamente o defeito que este historico corrige");
        assertTrue(linhas.get(0).contains("2026-07-22T18:55:00Z") && linhas.get(0).contains("PARCIAL"),
            "a primeira execucao precisa sobreviver a segunda: " + linhas.get(0));
        assertTrue(linhas.get(1).contains("2026-07-23T07:35:00Z") && linhas.get(1).contains("CONCLUIDO"),
            "a segunda execucao precisa estar registrada: " + linhas.get(1));

        // E a foto continua sendo foto: um registro por episodio, o mais recente.
        String foto = Files.readString(raiz.resolve("logs").resolve("telemetria_traducao.json"),
            StandardCharsets.UTF_8);
        assertTrue(foto.contains("CONCLUIDO"), "a foto guarda a execucao mais recente");
        assertEquals(1, foto.split("\"nomeEpisodio\"", -1).length - 1,
            "a foto NAO pode crescer: ela e o estado atual por episodio, nao a serie temporal");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: episódios diferentes coexistem no histórico sem se atropelar.
     * <p>INVARIANTES DO DOMÍNIO: uma linha por execução, na ordem em que aconteceram.
     * <p>COMPORTAMENTO EM CASO DE FALHA: perda de execução de episódio distinto.
     */
    @Test
    void episodiosDiferentesGeramUmaLinhaCadaNaOrdemDeExecucao() throws Exception {
        TelemetriaTraducaoAdapter adapter = new TelemetriaTraducaoAdapter(new ObjectMapper());

        adapter.registrarTraducao(execucao("ep01.ass", "2026-07-23T07:31:00Z", "PARCIAL"));
        adapter.registrarTraducao(execucao("ep02.ass", "2026-07-23T07:35:00Z", "CONCLUIDO"));
        adapter.registrarTraducao(execucao("ep03.ass", "2026-07-23T07:41:00Z", "FALHOU"));

        List<String> linhas = Files.readAllLines(
            raiz.resolve("logs").resolve(TelemetriaTraducaoAdapter.NOME_ARQUIVO_HISTORICO),
            StandardCharsets.UTF_8);

        assertEquals(3, linhas.size());
        assertTrue(linhas.get(0).contains("ep01.ass"));
        assertTrue(linhas.get(1).contains("ep02.ass"));
        assertTrue(linhas.get(2).contains("ep03.ass"));
    }
}
