package org.traducao.projeto.core.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: garante que o resolver central de diretórios preserva o
 * comportamento de produção (raiz = diretório corrente) e redireciona quando a
 * system property {@code kronos.dir.base} está definida — o mecanismo que
 * impede a suíte de contaminar os diretórios operacionais reais.
 *
 * <p>INVARIANTES DO DOMÍNIO: salva e restaura o valor original da property para
 * não afetar os demais testes do mesmo JVM.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: asserções JUnit falham se a resolução
 * divergir do contrato.
 */
class DiretorioBaseKronosTest {

    private String valorOriginal;

    @BeforeEach
    void guardarPropriedade() {
        valorOriginal = System.getProperty(DiretorioBaseKronos.PROPRIEDADE_BASE);
    }

    @AfterEach
    void restaurarPropriedade() {
        if (valorOriginal == null) {
            System.clearProperty(DiretorioBaseKronos.PROPRIEDADE_BASE);
        } else {
            System.setProperty(DiretorioBaseKronos.PROPRIEDADE_BASE, valorOriginal);
        }
    }

    @Test
    void semPropriedadeResolveContraDiretorioCorrente() {
        System.clearProperty(DiretorioBaseKronos.PROPRIEDADE_BASE);
        assertEquals(Path.of("logs"), DiretorioBaseKronos.resolver("logs"));
        assertEquals(Path.of("relatorios", "operacao"),
            DiretorioBaseKronos.resolver("relatorios", "operacao"));
    }

    @Test
    void propriedadeEmBrancoEquivaleAAusente() {
        System.setProperty(DiretorioBaseKronos.PROPRIEDADE_BASE, "   ");
        assertEquals(Path.of("cache"), DiretorioBaseKronos.resolver("cache"));
    }

    @Test
    void comPropriedadeRedirecionaParaBaseInformada() {
        Path base = Path.of("build", "tmp", "kronos-tests");
        System.setProperty(DiretorioBaseKronos.PROPRIEDADE_BASE, base.toString());

        Path resolvido = DiretorioBaseKronos.resolver("relatorios", "op-x");

        assertEquals(base.resolve("relatorios").resolve("op-x"), resolvido);
        assertTrue(resolvido.startsWith(base),
            "O caminho resolvido deve ficar sob a raiz redirecionada");
    }
}
