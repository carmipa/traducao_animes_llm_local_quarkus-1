package org.traducao.projeto.qualidadeTraducao.arquitetura;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela a INDEPENDÊNCIA do peer compartilhado
 * {@code qualidadeTraducao} extraído na E8b ({@code MascaradorTags} em application,
 * {@code ExcecaoQualidadeTraducao} + {@code AlucinacaoDetectadaException} em domain).
 * Garante que o peer é consumível por qualquer fatia funcional sem acoplamento reverso.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code qualidadeTraducao} só depende de JDK/libs técnicas, {@code core} e do
 *       próprio {@code qualidadeTraducao} — nunca de {@code traducao} nem de outra fatia
 *       funcional, nem de outro peer ({@code legenda}, {@code cachetraducao},
 *       {@code contexto}, {@code llm}).</li>
 *   <li>Inventário nominal EXATO por FQN COMPLETO: exatamente os três proprietários
 *       top-level ({@code qualidadeTraducao.application.MascaradorTags},
 *       {@code qualidadeTraducao.domain.ExcecaoQualidadeTraducao},
 *       {@code qualidadeTraducao.domain.AlucinacaoDetectadaException}); o nested
 *       {@code MascaradorTags$Mascarado} normaliza para o FQN de {@code MascaradorTags} e
 *       NÃO conta como quarto top-level. Congelar por FQN (não por simple name) impede
 *       que uma classe mude de pacote/camada mantendo o mesmo nome sem reprovar.</li>
 *   <li>{@code qualidadeTraducao.domain} é puro: só JDK e {@code core}; sem application,
 *       infrastructure ou framework.</li>
 *   <li>{@code qualidadeTraducao.application} depende de domain/core/JDK/Spring técnico;
 *       nunca de infrastructure. Nesta fase NÃO existe pacote infrastructure.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer dependência proibida, tipo fora do inventário ou pacote infrastructure novo
 * reprova o teste, listando o desvio exato.
 */
class FronteiraQualidadeTraducaoArchTest {

    private static final String RAIZ = "org.traducao.projeto";
    private static final String PREFIXO = RAIZ + ".";
    private static final String FATIA_QUALIDADE = "qualidadeTraducao";
    private static final String FATIA_CORE = "core";
    private static final String PKG_QUALIDADE = RAIZ + ".qualidadeTraducao";
    private static final String PKG_QT_DOMAIN = RAIZ + ".qualidadeTraducao.domain";
    private static final String PKG_QT_APPLICATION = RAIZ + ".qualidadeTraducao.application";
    private static final String PKG_QT_INFRA = RAIZ + ".qualidadeTraducao.infrastructure";

    private static JavaClasses classesProducao;

    @BeforeAll
    static void importar() {
        classesProducao = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RAIZ);
    }

    @Test
    @DisplayName("qualidadeTraducao NÃO depende de fatia funcional nem de outro peer (só JDK, técnico, core e o próprio)")
    void qualidadeTraducaoNaoDependeDeFatiaNemDeOutroPeer() {
        List<String> violacoes = new ArrayList<>();
        for (JavaClass classe : classesProducao) {
            if (!ehDoQualidade(classe)) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String fatia = fatiaDe(dependencia.getTargetClass().getPackageName());
                // Permitidos: JDK/libs/Spring (null), o próprio qualidadeTraducao e core.
                if (fatia == null || fatia.equals(FATIA_QUALIDADE) || fatia.equals(FATIA_CORE)) {
                    continue;
                }
                violacoes.add(origem + " -> " + topo(dependencia.getTargetClass().getName()));
            }
        }
        assertTrue(violacoes.isEmpty(),
            () -> "O peer qualidadeTraducao só pode depender de JDK, libs técnicas, core e do próprio "
                + "qualidadeTraducao. Nenhuma dependência para traducao/fatia funcional nem para outro peer "
                + "(legenda/cachetraducao/contexto/llm) é permitida.\n"
                + String.join("\n", new TreeSet<>(violacoes)));
    }

    @Test
    @DisplayName("inventário nominal EXATO por FQN: exatamente os três proprietários top-level do peer qualidadeTraducao")
    void inventarioNominalExato() {
        TreeSet<String> topLevelsFqn = new TreeSet<>();
        for (JavaClass classe : classesProducao) {
            if (!ehDoQualidade(classe)) {
                continue;
            }
            // Normaliza o nested ($) ao FQN do proprietário top-level:
            // ...qualidadeTraducao.application.MascaradorTags$Mascarado ->
            // ...qualidadeTraducao.application.MascaradorTags. Congela FQN COMPLETO
            // (não simple name): mudar de pacote/camada mantendo o mesmo nome reprova.
            topLevelsFqn.add(topo(classe.getName()));
        }
        assertEquals(new TreeSet<>(List.of(
                PKG_QT_APPLICATION + ".MascaradorTags",
                PKG_QT_DOMAIN + ".AlucinacaoDetectadaException",
                PKG_QT_DOMAIN + ".ExcecaoQualidadeTraducao")), topLevelsFqn,
            "qualidadeTraducao deve conter EXATAMENTE os três proprietários top-level homologados, por FQN "
                + "(o nested MascaradorTags$Mascarado normaliza para MascaradorTags e não é um quarto top-level). "
                + "Encontrado: " + topLevelsFqn);
    }

    @Test
    @DisplayName("qualidadeTraducao.domain é puro: só JDK e core (sem application/infrastructure/framework)")
    void domainEhPuro() {
        List<String> violacoes = new ArrayList<>();
        for (JavaClass classe : classesProducao) {
            String pkg = classe.getPackageName();
            if (!(pkg.equals(PKG_QT_DOMAIN) || pkg.startsWith(PKG_QT_DOMAIN + "."))) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String destinoPkg = dependencia.getTargetClass().getPackageName();
                String destino = dependencia.getTargetClass().getName();
                String fatia = fatiaDe(destinoPkg);
                boolean naoDomain = !(destinoPkg.equals(PKG_QT_DOMAIN) || destinoPkg.startsWith(PKG_QT_DOMAIN + "."));
                boolean permitido = fatia == null || fatia.equals(FATIA_CORE) || !naoDomain;
                boolean framework = destino.startsWith("jakarta.")
                    || destino.startsWith("io.quarkus")
                    || destino.startsWith("io.smallrye")
                    || destino.startsWith("org.eclipse.microprofile")
                    || destino.startsWith("org.springframework");
                if (!permitido || framework) {
                    violacoes.add(origem + " -> " + topo(destino));
                }
            }
        }
        assertTrue(violacoes.isEmpty(),
            () -> "qualidadeTraducao.domain deve permanecer puro (só JDK e core; sem application/infrastructure/framework):\n"
                + String.join("\n", new TreeSet<>(violacoes)));
    }

    @Test
    @DisplayName("qualidadeTraducao.application não depende de infrastructure (que não existe nesta fase)")
    void applicationNaoDependeDeInfraeSemPacoteInfra() {
        List<String> violacoes = new ArrayList<>();
        boolean existeInfra = false;
        for (JavaClass classe : classesProducao) {
            String pkg = classe.getPackageName();
            if (pkg.equals(PKG_QT_INFRA) || pkg.startsWith(PKG_QT_INFRA + ".")) {
                existeInfra = true;
            }
            if (!(pkg.equals(PKG_QT_APPLICATION) || pkg.startsWith(PKG_QT_APPLICATION + "."))) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String destinoPkg = dependencia.getTargetClass().getPackageName();
                if (destinoPkg.equals(PKG_QT_INFRA) || destinoPkg.startsWith(PKG_QT_INFRA + ".")) {
                    violacoes.add(origem + " -> " + topo(dependencia.getTargetClass().getName()));
                }
            }
        }
        assertTrue(!existeInfra,
            "qualidadeTraducao NÃO deve ter pacote infrastructure nesta fase (E8b)");
        assertTrue(violacoes.isEmpty(),
            () -> "qualidadeTraducao.application não pode depender de infrastructure:\n"
                + String.join("\n", new TreeSet<>(violacoes)));
    }

    private static boolean ehDoQualidade(JavaClass classe) {
        String pkg = classe.getPackageName();
        return pkg.equals(PKG_QUALIDADE) || pkg.startsWith(PKG_QUALIDADE + ".");
    }

    private static String topo(String nomeCompleto) {
        int cifrao = nomeCompleto.indexOf('$');
        return cifrao < 0 ? nomeCompleto : nomeCompleto.substring(0, cifrao);
    }

    private static String fatiaDe(String pkg) {
        if (pkg == null || !pkg.startsWith(PREFIXO)) {
            return null;
        }
        String resto = pkg.substring(PREFIXO.length());
        int ponto = resto.indexOf('.');
        return ponto < 0 ? resto : resto.substring(0, ponto);
    }
}
