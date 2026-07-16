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
 * {@code contexto}, nascido na subfase E7a com o domínio de contexto/lore de tradução
 * (porta {@code ProvedorContexto}, utilitário {@code ContextoPrompt}, constantes
 * {@code RegrasConcordanciaPtBr}, hierarquia {@code ExcecaoContexto}/
 * {@code ContextoNaoEncontradoException} e as 56 classes de lore por obra). Garante que
 * o peer é consumível por qualquer fatia funcional sem criar acoplamento reverso.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code contexto} NÃO depende de {@code traducao} nem de qualquer outra fatia
 *       funcional (LLM, cache, apresentação inclusos): só JDK/libs técnicas, {@code core}
 *       (base de exceção) e o próprio {@code contexto}.</li>
 *   <li>{@code contexto.domain} é puro: não depende de {@code infrastructure} nem de
 *       framework (Quarkus/CDI/MicroProfile/Spring/Jackson). {@code ContextoPrompt} tem
 *       cache estático interno — pureza aqui é de DEPENDÊNCIAS, não ausência de estado.</li>
 *   <li>{@code contexto.lore} depende somente de {@code contexto.domain}, JDK e da
 *       anotação Spring {@code @Component} — nunca de {@code core}, {@code infrastructure}
 *       ou outra fatia.</li>
 *   <li>Nenhuma lore reside em {@code infrastructure}; na E7a não existe
 *       {@code contexto.infrastructure} (o {@code GerenciadorContexto} permanece em
 *       {@code traducao} e só migra na E7b).</li>
 *   <li>{@code contexto.domain} contém exatamente os cinco tipos homologados e
 *       {@code contexto.lore} agrega as 56 classes de lore.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer dependência proibida, tipo fora do pacote correto ou entrada antecipada de
 * um tipo de E7b/E8 (ex.: {@code GerenciadorContexto} no peer) reprova o teste, listando
 * a aresta/desvio exato.
 */
class FronteiraContextoArchTest {

    private static final String RAIZ = "org.traducao.projeto";
    private static final String PREFIXO = RAIZ + ".";
    private static final String FATIA_CONTEXTO = "contexto";
    private static final String FATIA_CORE = "core";
    private static final String PKG_CONTEXTO = RAIZ + ".contexto";
    private static final String PKG_CONTEXTO_DOMAIN = RAIZ + ".contexto.domain";
    private static final String PKG_CONTEXTO_LORE = RAIZ + ".contexto.lore";

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
                boolean infraContexto = destinoPkg.contains(".infrastructure");
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
                // Permitidos: JDK/libs (null, inclui a anotação Spring) e o próprio contexto
                // (domain ou lore). Proíbe core, traducao e qualquer outra fatia.
                boolean permitido = fatia == null || fatia.equals(FATIA_CONTEXTO);
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
    @DisplayName("nenhuma classe de contexto reside em infrastructure na E7a")
    void nenhumaClasseEmInfrastructure() {
        List<String> emInfra = new ArrayList<>();
        for (JavaClass classe : classesProducao) {
            if (!ehDoContexto(classe)) {
                continue;
            }
            if (classe.getPackageName().contains(".infrastructure")) {
                emInfra.add(topo(classe.getName()));
            }
        }
        assertTrue(emInfra.isEmpty(),
            () -> "Na E7a nenhuma classe de contexto pode estar em infrastructure (o GerenciadorContexto "
                + "permanece em traducao e só migra na E7b):\n" + String.join("\n", new TreeSet<>(emInfra)));
    }

    @Test
    @DisplayName("GerenciadorContexto NÃO entrou no peer contexto (é tipo da E7b) e segue em traducao")
    void gerenciadorContextoAindaNaoEstaNoPeer() {
        boolean gerenciadorNoContexto = classesProducao.stream()
            .anyMatch(c -> c.getName().equals(RAIZ + ".contexto.infrastructure.GerenciadorContexto")
                || (c.getPackageName().startsWith(PKG_CONTEXTO + ".")
                    && c.getSimpleName().equals("GerenciadorContexto")));
        assertFalse(gerenciadorNoContexto,
            "GerenciadorContexto é tipo da E7b e NÃO pode estar no peer contexto na E7a");
        boolean gerenciadorEmTraducao = classesProducao.stream()
            .anyMatch(c -> c.getName().equals(RAIZ + ".traducao.infrastructure.contexto.GerenciadorContexto"));
        assertTrue(gerenciadorEmTraducao,
            "GerenciadorContexto deve permanecer em traducao.infrastructure.contexto na E7a");
    }

    @Test
    @DisplayName("estrutura homologada E7a: 5 tipos em domain e 56 lores em contexto.lore")
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
        assertEquals(56, lores,
            "contexto.lore deve agregar exatamente 56 classes de lore (53 @Component + 3 agregadoras Macross)");
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
