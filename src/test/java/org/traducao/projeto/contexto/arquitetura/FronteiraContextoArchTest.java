package org.traducao.projeto.contexto.arquitetura;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela a INDEPENDÊNCIA do peer compartilhado
 * {@code contexto} (E7a domínio/lore + E7b infrastructure). Garante que o peer é
 * consumível por qualquer fatia funcional sem acoplamento reverso.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code contexto} NÃO depende de {@code traducao} nem de outra fatia funcional:
 *       só JDK/libs técnicas, {@code core} e o próprio {@code contexto}.</li>
 *   <li>{@code contexto.domain} é puro: sem {@code contexto.infrastructure} nem framework.</li>
 *   <li>{@code contexto.lore} depende somente de {@code contexto.domain}, JDK e Spring
 *       {@code @Component} — nunca de {@code core}, {@code infrastructure} ou outra fatia.</li>
 *   <li>{@code contexto.infrastructure} é congelado nominalmente: exatamente
 *       {@code GerenciadorContexto} e {@code ContextoBeansConfig}.</li>
 *   <li>{@code contexto.domain} contém os cinco tipos homologados;
 *       {@code contexto.lore} agrega 56 classes.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer dependência proibida, tipo fora do pacote correto ou terceira classe em
 * infrastructure reprova o teste, listando a aresta/desvio exato.
 */
class FronteiraContextoArchTest {

    private static final String RAIZ = "org.traducao.projeto";
    private static final String PREFIXO = RAIZ + ".";
    private static final String FATIA_CONTEXTO = "contexto";
    private static final String FATIA_CORE = "core";
    private static final String PKG_CONTEXTO = RAIZ + ".contexto";
    private static final String PKG_CONTEXTO_DOMAIN = RAIZ + ".contexto.domain";
    private static final String PKG_CONTEXTO_LORE = RAIZ + ".contexto.lore";
    private static final String PKG_CONTEXTO_INFRA = RAIZ + ".contexto.infrastructure";

    private static JavaClasses classesProducao;

    @BeforeAll
    static void importar() {
        classesProducao = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RAIZ);
    }

    @Test
    @DisplayName("contexto NÃO depende de fatia funcional (só JDK, técnico, core e o próprio contexto)")
    void contextoNaoDependeDeFatiaFuncional() {
        List<String> violacoes = new ArrayList<>();
        for (JavaClass classe : classesProducao) {
            if (!ehDoContexto(classe)) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String fatia = fatiaDe(dependencia.getTargetClass().getPackageName());
                // Permitidos: JDK/libs (null), o próprio contexto e core (base de exceção).
                if (fatia == null || fatia.equals(FATIA_CONTEXTO) || fatia.equals(FATIA_CORE)) {
                    continue;
                }
                violacoes.add(origem + " -> " + topo(dependencia.getTargetClass().getName()));
            }
        }
        assertTrue(violacoes.isEmpty(),
            () -> "O peer contexto só pode depender de JDK, libs técnicas, core e do próprio contexto. "
                + "Nenhuma dependência contexto -> fatia funcional (incl. traducao/LLM/cache/apresentação) é permitida.\n"
                + String.join("\n", new TreeSet<>(violacoes)));
    }

    @Test
    @DisplayName("contexto.domain é puro: não depende de infrastructure nem de framework")
    void contextoDomainEhPuro() {
        List<String> violacoes = new ArrayList<>();
        for (JavaClass classe : classesProducao) {
            String pkg = classe.getPackageName();
            boolean ehDominio = pkg.equals(PKG_CONTEXTO_DOMAIN) || pkg.startsWith(PKG_CONTEXTO_DOMAIN + ".");
            if (!ehDominio) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String destinoPkg = dependencia.getTargetClass().getPackageName();
                String destino = dependencia.getTargetClass().getName();
                boolean infraContexto = destinoPkg.equals(PKG_CONTEXTO_INFRA)
                    || destinoPkg.startsWith(PKG_CONTEXTO_INFRA + ".");
                boolean framework = destino.startsWith("jakarta.")
                    || destino.startsWith("io.quarkus")
                    || destino.startsWith("io.smallrye")
                    || destino.startsWith("org.eclipse.microprofile")
                    || destino.startsWith("org.springframework")
                    || destino.startsWith("com.fasterxml.jackson");
                if (infraContexto || framework) {
                    violacoes.add(origem + " -> " + topo(destino));
                }
            }
        }
        assertTrue(violacoes.isEmpty(),
            () -> "contexto.domain deve permanecer puro em DEPENDÊNCIAS (sem infrastructure/framework):\n"
                + String.join("\n", new TreeSet<>(violacoes)));
    }

    @Test
    @DisplayName("contexto.lore depende somente de contexto.domain, JDK e Spring @Component")
    void loreDependeSoDeDomainEJdkESpring() {
        List<String> violacoes = new ArrayList<>();
        for (JavaClass classe : classesProducao) {
            String pkg = classe.getPackageName();
            boolean ehLore = pkg.equals(PKG_CONTEXTO_LORE) || pkg.startsWith(PKG_CONTEXTO_LORE + ".");
            if (!ehLore) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String destinoPkg = dependencia.getTargetClass().getPackageName();
                String fatia = fatiaDe(destinoPkg);
                // Permitidos: JDK/libs (null, inclui Spring) e contexto.domain / contexto.lore.
                // Proíbe core, traducao, contexto.infrastructure e qualquer outra fatia.
                boolean permitido = fatia == null
                    || destinoPkg.equals(PKG_CONTEXTO_DOMAIN)
                    || destinoPkg.startsWith(PKG_CONTEXTO_DOMAIN + ".")
                    || destinoPkg.equals(PKG_CONTEXTO_LORE)
                    || destinoPkg.startsWith(PKG_CONTEXTO_LORE + ".");
                if (permitido) {
                    continue;
                }
                violacoes.add(origem + " -> " + topo(dependencia.getTargetClass().getName()));
            }
        }
        assertTrue(violacoes.isEmpty(),
            () -> "contexto.lore só pode depender de contexto.domain, JDK e Spring @Component:\n"
                + String.join("\n", new TreeSet<>(violacoes)));
    }

    @Test
    @DisplayName("contexto.infrastructure é congelado NOMINALMENTE (E7b): exatamente GerenciadorContexto e ContextoBeansConfig")
    void infraestruturaCongeladaNominalmente() {
        TreeSet<String> infra = new TreeSet<>();
        for (JavaClass classe : classesProducao) {
            if (!ehDoContexto(classe)) {
                continue;
            }
            String pkg = classe.getPackageName();
            String nome = topo(classe.getName());
            if (nome.contains("$")) {
                continue;
            }
            if (pkg.equals(PKG_CONTEXTO_INFRA) || pkg.startsWith(PKG_CONTEXTO_INFRA + ".")) {
                infra.add(nome.substring(nome.lastIndexOf('.') + 1));
            }
        }
        assertEquals(new TreeSet<>(List.of("ContextoBeansConfig", "GerenciadorContexto")), infra,
            "contexto.infrastructure deve conter EXATAMENTE GerenciadorContexto e ContextoBeansConfig "
                + "(sem liberação genérica de infrastructure; qualquer terceira classe reprova). Encontrado: " + infra);
    }

    @Test
    @DisplayName("GerenciadorContexto NÃO está mais em traducao (E7b)")
    void gerenciadorContextoMigradoParaOPeer() {
        // Presença no peer já é coberta por infraestruturaCongeladaNominalmente().
        boolean gerenciadorEmTraducao = classesProducao.stream()
            .anyMatch(c -> c.getSimpleName().equals("GerenciadorContexto")
                && c.getPackageName().startsWith(RAIZ + ".traducao"));
        assertFalse(gerenciadorEmTraducao,
            "GerenciadorContexto NÃO pode mais residir em traducao após a E7b");
    }

    @Test
    @DisplayName("estrutura homologada E7b: 5 tipos em domain e 59 lores em contexto.lore")
    void estruturaHomologada() {
        TreeSet<String> domain = new TreeSet<>();
        int lores = 0;
        for (JavaClass classe : classesProducao) {
            String pkg = classe.getPackageName();
            String nome = topo(classe.getName());
            if (nome.contains("$")) {
                continue;
            }
            if (pkg.equals(PKG_CONTEXTO_DOMAIN)) {
                domain.add(nome.substring(nome.lastIndexOf('.') + 1));
            } else if (pkg.equals(PKG_CONTEXTO_LORE) || pkg.startsWith(PKG_CONTEXTO_LORE + ".")) {
                lores++;
            }
        }
        assertTrue(domain.equals(new TreeSet<>(List.of(
                "ContextoNaoEncontradoException", "ContextoPrompt", "ExcecaoContexto",
                "ProvedorContexto", "RegrasConcordanciaPtBr"))),
            () -> "contexto.domain deve conter exatamente os 5 tipos homologados. Encontrado: " + domain);
        assertEquals(59, lores,
            "contexto.lore deve agregar exatamente 59 classes de lore (53 @Component + 3 agregadoras Macross "
                + "+ 2 mapas de terminologia ZZ/Macross + 1 mapa compartilhado Gundam UC)");
    }

    private static boolean ehDoContexto(JavaClass classe) {
        String pkg = classe.getPackageName();
        return pkg.equals(PKG_CONTEXTO) || pkg.startsWith(PKG_CONTEXTO + ".");
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
