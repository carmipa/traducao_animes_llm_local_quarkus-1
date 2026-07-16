package org.traducao.projeto.legenda.arquitetura;

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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela a INDEPENDÊNCIA do módulo peer compartilhado
 * {@code legenda}. Garante que o módulo é consumível por qualquer fatia funcional sem
 * criar acoplamento reverso. Evolução da superfície do módulo:
 * <ul>
 *   <li>E3c: {@code PoliticaEstiloMusical}.</li>
 *   <li>E5a: {@code DocumentoLegenda} e {@code EventoLegenda} (modelo puro, movido de
 *       {@code traducao.domain.legenda}).</li>
 * </ul>
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code legenda} NÃO depende de {@code traducao} nem de qualquer fatia funcional.</li>
 *   <li>{@code legenda} só pode depender de JDK/bibliotecas técnicas, do {@code core} e do
 *       próprio {@code legenda}.</li>
 *   <li>{@code legenda.domain} é puro: não depende de {@code legenda.infrastructure} nem de
 *       framework (Quarkus/CDI/MicroProfile/Spring).</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer dependência proibida reprova o teste, listando a aresta exata.
 */
class FronteiraLegendaArchTest {

    private static final String RAIZ = "org.traducao.projeto";
    private static final String PREFIXO = RAIZ + ".";
    private static final String FATIA_LEGENDA = "legenda";
    private static final String FATIA_CORE = "core";
    private static final String PKG_LEGENDA = RAIZ + ".legenda";
    private static final String PKG_LEGENDA_DOMAIN = RAIZ + ".legenda.domain";

    private static JavaClasses classesProducao;

    @BeforeAll
    static void importar() {
        classesProducao = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RAIZ);
    }

    @Test
    @DisplayName("legenda NÃO depende de nenhuma fatia funcional (só JDK, técnico, core e o próprio legenda)")
    void legendaNaoDependeDeFatiaFuncional() {
        List<String> violacoes = new ArrayList<>();
        for (JavaClass classe : classesProducao) {
            if (!ehDaLegenda(classe)) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String fatia = fatiaDe(dependencia.getTargetClass().getPackageName());
                // Permitidos: JDK/libs externas (fatia == null), o próprio legenda e core.
                if (fatia == null || fatia.equals(FATIA_LEGENDA) || fatia.equals(FATIA_CORE)) {
                    continue;
                }
                violacoes.add(origem + " -> " + topo(dependencia.getTargetClass().getName()));
            }
        }
        assertTrue(violacoes.isEmpty(),
            () -> "O módulo legenda só pode depender de JDK, bibliotecas técnicas, core e do próprio "
                + "legenda. Nenhuma dependência legenda -> fatia funcional (incl. traducao) é permitida.\n"
                + String.join("\n", new TreeSet<>(violacoes)));
    }

    @Test
    @DisplayName("legenda.domain é puro: não depende de infrastructure nem de framework")
    void legendaDomainEhPuro() {
        List<String> violacoes = new ArrayList<>();
        for (JavaClass classe : classesProducao) {
            String pkg = classe.getPackageName();
            boolean ehDominio = pkg.equals(PKG_LEGENDA_DOMAIN) || pkg.startsWith(PKG_LEGENDA_DOMAIN + ".");
            if (!ehDominio) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String destinoPkg = dependencia.getTargetClass().getPackageName();
                String destino = dependencia.getTargetClass().getName();
                boolean infraLegenda = destinoPkg.startsWith(PKG_LEGENDA + ".infrastructure");
                boolean framework = destino.startsWith("jakarta.")
                    || destino.startsWith("io.quarkus")
                    || destino.startsWith("io.smallrye")
                    || destino.startsWith("org.eclipse.microprofile")
                    || destino.startsWith("org.springframework");
                if (infraLegenda || framework) {
                    violacoes.add(origem + " -> " + topo(destino));
                }
            }
        }
        assertTrue(violacoes.isEmpty(),
            () -> "legenda.domain deve permanecer puro (sem infrastructure/framework):\n"
                + String.join("\n", new TreeSet<>(violacoes)));
    }

    private static boolean ehDaLegenda(JavaClass classe) {
        String pkg = classe.getPackageName();
        return pkg.equals(PKG_LEGENDA) || pkg.startsWith(PKG_LEGENDA + ".");
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
