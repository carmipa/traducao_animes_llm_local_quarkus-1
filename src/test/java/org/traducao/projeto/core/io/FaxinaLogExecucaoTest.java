package org.traducao.projeto.core.io;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que a faxina apaga o log velho e SÓ ele. Cada teste corresponde a
 * um invariante numerado do Javadoc de {@link FaxinaLogExecucao} — sem prova, o invariante é
 * promessa.
 *
 * <p>Isto é ferramenta destrutiva, então o peso dos testes está no que ela NÃO pode apagar. Um
 * teste que só confirma "apagou o velho" deixaria passar a versão que apaga tudo.
 */
class FaxinaLogExecucaoTest {

    private static final Duration SETE_DIAS = Duration.ofDays(7);

    private static Path logComIdade(Path pasta, String nome, int diasAtras) throws IOException {
        Path p = pasta.resolve(nome);
        Files.writeString(p, "conteudo");
        Files.setLastModifiedTime(p, FileTime.from(Instant.now().minus(Duration.ofDays(diasAtras))));
        return p;
    }

    private static Path pastaExecucoes(Path raizLogs) throws IOException {
        return Files.createDirectories(raizLogs.resolve(FaxinaLogExecucao.SUBPASTA));
    }

    @Test
    @DisplayName("apaga o log alem da retencao e preserva o de dentro dela")
    void apagaVelhoPreservaNovo(@TempDir Path raiz) throws IOException {
        Path pasta = pastaExecucoes(raiz);
        Path velho = logComIdade(pasta, "kronos-20260701-120000.log", 30);
        Path recente = logComIdade(pasta, "kronos-20260807-235959.log", 2);

        FaxinaLogExecucao.Resultado r = FaxinaLogExecucao.limpar(raiz, SETE_DIAS, null);

        assertFalse(Files.exists(velho), "log de 30 dias tinha de sair");
        assertTrue(Files.exists(recente), "log de 2 dias tinha de ficar");
        assertEquals(1, r.removidos());
        assertEquals(1, r.preservados());
        assertFalse(r.houveImpedimento());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: LOG-2. É o teste que separa faxina de destruição — um arquivo de
     * outro dono largado na pasta tem de sobreviver, por mais velho que seja.
     */
    @Test
    @DisplayName("LOG-2: arquivo que NAO e log de execucao sobrevive, mesmo antiquissimo")
    void naoTocaArquivoDeOutroDono(@TempDir Path raiz) throws IOException {
        Path pasta = pastaExecucoes(raiz);
        Path[] intrusos = {
            logComIdade(pasta, "relatorio-importante.txt", 400),
            logComIdade(pasta, "kronos-notas.md", 400),
            logComIdade(pasta, "tradutor.log", 400),
            logComIdade(pasta, "kronos-20260101.log.bak", 400),
            logComIdade(pasta, "backup-kronos-20260101.log", 400)
        };
        FaxinaLogExecucao.Resultado r = FaxinaLogExecucao.limpar(raiz, SETE_DIAS, null);

        for (Path intruso : intrusos) {
            assertTrue(Files.exists(intruso),
                "faxina apagou arquivo que nao e dela: " + intruso.getFileName());
        }
        assertEquals(0, r.removidos());
        assertEquals(intrusos.length, r.preservados());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: LOG-3. O processo não pode perder o log que está escrevendo — seria
     * apagar a evidência da execução em curso.
     */
    @Test
    @DisplayName("LOG-3: o log da execucao ATUAL nunca e apagado, mesmo se datado como velho")
    void nuncaApagaOLogDaExecucaoAtual(@TempDir Path raiz) throws IOException {
        Path pasta = pastaExecucoes(raiz);
        Path atual = logComIdade(pasta, "kronos-agora.log", 90);

        FaxinaLogExecucao.Resultado r = FaxinaLogExecucao.limpar(raiz, SETE_DIAS, atual);

        assertTrue(Files.exists(atual), "apagou o log da propria execucao");
        assertEquals(0, r.removidos());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: LOG-4. A idade vem do relógio do arquivo. Um nome com data antiga
     * mas conteúdo novo não pode ser apagado — nome é texto, não evidência.
     */
    @Test
    @DisplayName("LOG-4: idade vem do RELOGIO, nao do nome do arquivo")
    void idadeVemDoRelogioNaoDoNome(@TempDir Path raiz) throws IOException {
        Path pasta = pastaExecucoes(raiz);
        Path nomeVelhoArquivoNovo = logComIdade(pasta, "kronos-19990101-000000.log", 1);
        Path nomeNovoArquivoVelho = logComIdade(pasta, "kronos-20991231-235959.log", 60);

        FaxinaLogExecucao.Resultado r = FaxinaLogExecucao.limpar(raiz, SETE_DIAS, null);

        assertTrue(Files.exists(nomeVelhoArquivoNovo), "nome antigo nao torna o arquivo velho");
        assertFalse(Files.exists(nomeNovoArquivoVelho), "nome futuro nao salva arquivo velho");
        assertEquals(1, r.removidos());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: LOG-1. A faxina não pode sair da pasta dela. Aqui um SUBDIRETÓRIO
     * com nome de log é oferecido — apagar recursivamente seria o desastre clássico.
     */
    @Test
    @DisplayName("LOG-1/LOG-5: diretorio com cara de log nao e apagado")
    void naoApagaDiretorio(@TempDir Path raiz) throws IOException {
        Path pasta = pastaExecucoes(raiz);
        Path disfarce = Files.createDirectory(pasta.resolve("kronos-20260101-000000.log"));
        Files.writeString(disfarce.resolve("dentro.txt"), "nao me apague");
        Files.setLastModifiedTime(disfarce, FileTime.from(Instant.now().minus(Duration.ofDays(90))));

        FaxinaLogExecucao.Resultado r = FaxinaLogExecucao.limpar(raiz, SETE_DIAS, null);

        assertTrue(Files.isDirectory(disfarce), "faxina apagou um DIRETORIO");
        assertTrue(Files.exists(disfarce.resolve("dentro.txt")));
        assertEquals(0, r.removidos());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: falha fechada (regra 11). Desligar a faxina não pode virar apagar
     * tudo — o erro mais caro que uma configuração de retenção pode cometer.
     */
    @Test
    @DisplayName("FALHA FECHADA: retencao zero, negativa ou nula NAO apaga nada")
    void retencaoInvalidaNaoApagaNada(@TempDir Path raiz) throws IOException {
        Path pasta = pastaExecucoes(raiz);
        Path velho = logComIdade(pasta, "kronos-antigo.log", 999);

        for (Duration d : new Duration[]{null, Duration.ZERO, Duration.ofDays(-7)}) {
            FaxinaLogExecucao.Resultado r = FaxinaLogExecucao.limpar(raiz, d, null);
            assertEquals(0, r.removidos(), "retencao " + d + " apagou arquivo");
        }
        assertTrue(Files.exists(velho));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: regra 12 — "nada a fazer" e "não consegui" não podem dar o mesmo
     * sinal. Pasta ausente é rotina, não incidente.
     */
    @Test
    @DisplayName("pasta ausente devolve resultado zerado SEM impedimento")
    void pastaAusenteNaoEhIncidente(@TempDir Path raiz) {
        FaxinaLogExecucao.Resultado r = FaxinaLogExecucao.limpar(raiz, SETE_DIAS, null);
        assertEquals(0, r.removidos());
        assertFalse(r.houveImpedimento(), "pasta inexistente nao e impedimento, e rotina");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o nome gerado tem de ser reconhecido pela própria faxina — senão o
     * arquivo nasce imortal e o disco enche do mesmo jeito. Este teste fecha o ciclo.
     */
    @Test
    @DisplayName("CICLO FECHADO: o nome que geramos e reconhecido pela faxina")
    void nomeGeradoEhReconhecidoPelaFaxina(@TempDir Path raiz) throws IOException {
        Path pasta = pastaExecucoes(raiz);
        for (String carimbo : new String[]{"20260808-091500", "2026-08-08_09.15.00", null, "  ", "a/b\\c:d"}) {
            String nome = FaxinaLogExecucao.nomeDoArquivo(carimbo);
            Path p = pasta.resolve(nome);
            Files.writeString(p, "x");
            Files.setLastModifiedTime(p, FileTime.from(Instant.now().minus(Duration.ofDays(60))));

            FaxinaLogExecucao.Resultado r = FaxinaLogExecucao.limpar(raiz, SETE_DIAS, null);
            assertEquals(1, r.removidos(),
                "a faxina NAO reconheceu o nome que ela mesma gera: " + nome);
            assertFalse(Files.exists(p));
        }
    }

    @Test
    @DisplayName("bytes liberados sao contabilizados")
    void contabilizaBytes(@TempDir Path raiz) throws IOException {
        Path pasta = pastaExecucoes(raiz);
        Path p = pasta.resolve("kronos-grande.log");
        Files.writeString(p, "x".repeat(5000));
        Files.setLastModifiedTime(p, FileTime.from(Instant.now().minus(Duration.ofDays(30))));

        FaxinaLogExecucao.Resultado r = FaxinaLogExecucao.limpar(raiz, SETE_DIAS, null);

        assertEquals(1, r.removidos());
        assertTrue(r.bytesLiberados() >= 5000, "bytes liberados: " + r.bytesLiberados());
        assertTrue(r.resumo().contains("removido"), r.resumo());
    }
}
