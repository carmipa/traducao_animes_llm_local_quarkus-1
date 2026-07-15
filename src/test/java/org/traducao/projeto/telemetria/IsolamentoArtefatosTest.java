package org.traducao.projeto.telemetria;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.core.io.DiretorioBaseKronos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova, exercitando o caminho real de persistência de
 * relatório e telemetria de operação (o mesmo usado por revisão, correção,
 * lore etc.), que uma execução sob o perfil de teste NÃO grava nos diretórios
 * operacionais versionados ({@code relatorios/}, {@code logs/}) e sim na árvore
 * descartável redirecionada por {@link DiretorioBaseKronos}. É o guard que
 * impede a reaparição dos resíduos {@code relatorios/junit-*}.
 *
 * <p>INVARIANTES DO DOMÍNIO: a suíte roda com {@code kronos.dir.base} apontando
 * para {@code build/tmp/kronos-tests} (ver build.gradle), portanto os caminhos
 * relativos crus ({@code Path.of("relatorios")}, {@code Path.of("logs")})
 * continuam apontando para os diretórios reais e servem de referência do que
 * NÃO pode ser tocado.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer escrita real dispara asserção
 * JUnit, sinalizando regressão do isolamento.
 */
class IsolamentoArtefatosTest {

    @Test
    void finalizarOperacaoNaoGravaNosDiretoriosReais(@TempDir Path pastaEntrada) throws IOException {
        // Pré-condição: o redirecionamento de teste está ativo.
        assertTrue(System.getProperty(DiretorioBaseKronos.PROPRIEDADE_BASE) != null
                && !System.getProperty(DiretorioBaseKronos.PROPRIEDADE_BASE).isBlank(),
            "Testes devem rodar com kronos.dir.base redirecionado (ver build.gradle)");

        Path relatoriosReal = Path.of("relatorios", pastaEntrada.getFileName().toString());
        Path telemetriaReal = Path.of("logs", "telemetria_compartilhada.json");
        byte[] telemetriaRealAntes = Files.exists(telemetriaReal)
            ? Files.readAllBytes(telemetriaReal) : null;

        TelemetriaService telemetria = new TelemetriaService();
        OperacaoTelemetria operacao = TelemetriaService.criarOperacao(
            "Teste de Isolamento", "detalhe", 10L, 1, 0, 0);

        telemetria.finalizarOperacao(operacao, pastaEntrada, "isolamento", "conteudo do relatorio");

        // 1. Nada foi criado na árvore real de relatorios.
        assertFalse(Files.exists(relatoriosReal),
            "Não deve criar relatorios/<tempdir> real: " + relatoriosReal.toAbsolutePath());

        // 2. A telemetria canônica real permanece byte-a-byte intacta (ou ausente).
        if (telemetriaRealAntes == null) {
            assertFalse(Files.exists(telemetriaReal),
                "Telemetria canônica real não deve ser criada por um teste");
        } else {
            assertArrayEquals(telemetriaRealAntes, Files.readAllBytes(telemetriaReal),
                "Telemetria canônica real não pode ser modificada por um teste");
        }

        // 3. A escrita ocorreu, porém na árvore redirecionada (prova que não foi
        //    silenciosamente ignorada).
        Path relatoriosRedirecionado =
            DiretorioBaseKronos.resolver("relatorios", pastaEntrada.getFileName().toString());
        assertTrue(Files.isDirectory(relatoriosRedirecionado),
            "O relatório deveria ter sido gravado na árvore redirecionada: " + relatoriosRedirecionado);
        Path telemetriaRedirecionada =
            DiretorioBaseKronos.resolver("logs", "telemetria_compartilhada.json");
        assertTrue(Files.exists(telemetriaRedirecionada),
            "A telemetria de teste deveria persistir na árvore redirecionada: " + telemetriaRedirecionada);
    }
}
