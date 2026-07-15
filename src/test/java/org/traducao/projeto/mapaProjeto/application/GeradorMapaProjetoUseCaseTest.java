package org.traducao.projeto.mapaProjeto.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.mapaProjeto.domain.exceptions.MapaProjetoException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code Files.list} (usado por {@code executar}, diferente de
 * {@code Files.walk} usado em outros use cases deste projeto) lança
 * {@code NotDirectoryException} quando o caminho informado não é um
 * diretório — e, ao contrário dos demais use cases, {@code executar} aqui
 * não tem nenhuma checagem prévia que intercepte esse caso. Isso o torna o
 * único, entre as lacunas de exceção corrigidas nesta auditoria, em que a
 * falha real é reproduzível de forma determinística e portátil num teste.
 */
@QuarkusTest
class GeradorMapaProjetoUseCaseTest {

    @Inject
    GeradorMapaProjetoUseCase geradorMapaProjetoUseCase;

    @Test
    void executarLancaMapaProjetoExceptionQuandoCaminhoNaoEhDiretorio(@TempDir Path tempDir) throws IOException {
        Path arquivoRegular = tempDir.resolve("nao-e-uma-pasta.txt");
        Files.writeString(arquivoRegular, "conteudo");

        MapaProjetoException exception = assertThrows(MapaProjetoException.class,
            () -> geradorMapaProjetoUseCase.executar(arquivoRegular));

        assertTrue(exception.getMessage().contains(arquivoRegular.toString()));
        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void executarGeraMapaProjetoMdParaUmaPastaValida(@TempDir Path tempDir) throws IOException {
        geradorMapaProjetoUseCase.executar(tempDir);

        Path destino = tempDir.resolve("mapa_projeto.md");
        assertTrue(Files.exists(destino), "mapa_projeto.md deveria ter sido gerado");
        assertFalse(Files.readAllLines(destino).isEmpty(), "mapa_projeto.md gerado não deveria estar vazio");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o mapa deve refletir arquitetura/código-fonte, não
     * infraestrutura, saída operacional volumosa nem resíduos de teste — que
     * antes o contaminavam com centenas de entradas {@code relatorios/junit-*}.
     * INVARIANTES DO DOMÍNIO: pastas de código aparecem; {@code build},
     * {@code .gradle}, {@code relatorios/junit-*}, {@code logs} e {@code backups}
     * não aparecem.
     * COMPORTAMENTO EM CASO DE FALHA: regressão dos filtros reintroduz o ruído no
     * mapa e falha estas asserções.
     */
    @Test
    void executarPodaInfraOperacionalEResiduosDeTeste(@TempDir Path raiz) throws IOException {
        // Estrutura de código legítima que DEVE aparecer.
        Path pacote = raiz.resolve("src").resolve("main").resolve("java").resolve("app");
        Files.createDirectories(pacote);
        Files.writeString(pacote.resolve("Servico.java"),
            "package app;\n/** Serviço de exemplo. */\npublic class Servico {}\n");

        // Ruído que NÃO deve aparecer.
        Files.createDirectories(raiz.resolve("relatorios").resolve("junit-123456"));
        Files.writeString(raiz.resolve("relatorios").resolve("junit-123456").resolve("auditoria_conteudo_1.json"), "{}");
        Files.createDirectories(raiz.resolve("build").resolve("classes"));
        Files.writeString(raiz.resolve("build").resolve("classes").resolve("Servico.class"), "bin");
        Files.createDirectories(raiz.resolve(".gradle"));
        Files.writeString(raiz.resolve(".gradle").resolve("cache.bin"), "x");
        Files.createDirectories(raiz.resolve("logs"));
        Files.writeString(raiz.resolve("logs").resolve("tradutor.log"), "log");
        Files.createDirectories(raiz.resolve("backups").resolve("traducao"));
        Files.writeString(raiz.resolve("backups").resolve("traducao").resolve("x.ass"), "bkp");
        // Diretório junit-* na raiz (não só sob relatorios) para provar o filtro por prefixo.
        Files.createDirectories(raiz.resolve("junit-999"));
        Files.writeString(raiz.resolve("junit-999").resolve("tmp.txt"), "y");

        GeradorMapaProjetoUseCase.ResultadoMapa resultado = geradorMapaProjetoUseCase.executar(raiz);
        String mapa = resultado.relatorio();

        assertTrue(mapa.contains("src/"), "pasta de código src/ deveria aparecer no mapa");
        assertTrue(mapa.contains("Servico.java"), "arquivo-fonte deveria aparecer na taxonomia");

        assertFalse(mapa.contains("junit-123456"), "relatorios/junit-* não deve aparecer no mapa");
        assertFalse(mapa.contains("junit-999"), "diretório junit-* (por prefixo) não deve aparecer no mapa");
        assertFalse(mapa.contains("auditoria_conteudo_1.json"), "resíduo de teste não deve aparecer no mapa");
        assertFalse(mapa.contains(".gradle"), ".gradle não deve aparecer no mapa");
        assertFalse(mapa.contains("build/"), "build não deve aparecer no mapa");
        assertFalse(mapa.contains("backups/"), "backups não deve aparecer no mapa");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante que a decisão de poda é por nome simples e
     * cobre infra, operacional e resíduo de teste sem afetar código.
     * INVARIANTES DO DOMÍNIO: nomes de fonte/config não são podados; nomes de
     * ruído (prefixo junit-, build, logs, relatorios, backups) são.
     * COMPORTAMENTO EM CASO DE FALHA: divergência indica regressão do predicado.
     */
    @Test
    void deveIgnorarClassificaNomesCorretamente() {
        assertTrue(GeradorMapaProjetoUseCase.deveIgnorar("build"));
        assertTrue(GeradorMapaProjetoUseCase.deveIgnorar(".gradle"));
        assertTrue(GeradorMapaProjetoUseCase.deveIgnorar("relatorios"));
        assertTrue(GeradorMapaProjetoUseCase.deveIgnorar("logs"));
        assertTrue(GeradorMapaProjetoUseCase.deveIgnorar("backups"));
        assertTrue(GeradorMapaProjetoUseCase.deveIgnorar("junit-10227280303715500912"));
        assertTrue(GeradorMapaProjetoUseCase.deveIgnorar("telemetria_compartilhada.json.tmp"));

        assertFalse(GeradorMapaProjetoUseCase.deveIgnorar("src"));
        assertFalse(GeradorMapaProjetoUseCase.deveIgnorar("Servico.java"));
        assertFalse(GeradorMapaProjetoUseCase.deveIgnorar("application.properties"));
    }
}
