package org.traducao.projeto.cachetraducao.arquitetura;

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
 * PROPÓSITO DE NEGÓCIO: congela a INDEPENDÊNCIA do peer compartilhado
 * {@code cachetraducao}, nascido na subfase E6 com o bloco de cache de tradução
 * (modelos {@code EntradaCache}/{@code ProvenienciaCache}/{@code CacheDocumento} e
 * serviços {@code CacheTraducaoService}/{@code CacheManutencaoService}). Garante que o
 * peer é consumível por qualquer fatia funcional sem criar acoplamento reverso.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code cachetraducao} NÃO depende de {@code traducao} nem de qualquer fatia
 *       funcional (contexto, LLM, apresentação inclusos).</li>
 *   <li>{@code cachetraducao} só pode depender de JDK/bibliotecas técnicas, do
 *       {@code core}, do módulo {@code legenda} e do próprio {@code cachetraducao}.</li>
 *   <li>{@code cachetraducao.domain} é puro: não depende de {@code cachetraducao.infrastructure}
 *       nem de framework (Quarkus/CDI/MicroProfile/Spring/Jackson).</li>
 *   <li>Os três modelos permanecem em {@code domain}; os dois serviços em {@code infrastructure}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer dependência proibida ou tipo fora do pacote correto reprova o teste,
 * listando a aresta/desvio exato.
 */
class FronteiraCacheTraducaoArchTest {

    private static final String RAIZ = "org.traducao.projeto";
    private static final String PREFIXO = RAIZ + ".";
    private static final String FATIA_CACHE = "cachetraducao";
    private static final String FATIA_CORE = "core";
    private static final String FATIA_LEGENDA = "legenda";
    private static final String PKG_CACHE = RAIZ + ".cachetraducao";
    private static final String PKG_CACHE_DOMAIN = RAIZ + ".cachetraducao.domain";
    private static final String PKG_CACHE_INFRA = RAIZ + ".cachetraducao.infrastructure";

    private static JavaClasses classesProducao;

    @BeforeAll
    static void importar() {
        classesProducao = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RAIZ);
    }

    @Test
    @DisplayName("cachetraducao NÃO depende de fatia funcional (só JDK, técnico, core, legenda e o próprio cachetraducao)")
    void cacheNaoDependeDeFatiaFuncional() {
        List<String> violacoes = new ArrayList<>();
        for (JavaClass classe : classesProducao) {
            if (!ehDoCache(classe)) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String fatia = fatiaDe(dependencia.getTargetClass().getPackageName());
                // Permitidos: JDK/libs (null), o próprio cachetraducao, core e legenda.
                if (fatia == null || fatia.equals(FATIA_CACHE) || fatia.equals(FATIA_CORE)
                    || fatia.equals(FATIA_LEGENDA)) {
                    continue;
                }
                violacoes.add(origem + " -> " + topo(dependencia.getTargetClass().getName()));
            }
        }
        assertTrue(violacoes.isEmpty(),
            () -> "O peer cachetraducao só pode depender de JDK, libs técnicas, core, legenda e do próprio "
                + "cachetraducao. Nenhuma dependência cachetraducao -> fatia funcional (incl. traducao/contexto/LLM/apresentação) é permitida.\n"
                + String.join("\n", new TreeSet<>(violacoes)));
    }

    @Test
    @DisplayName("cachetraducao.domain é puro: não depende de infrastructure nem de framework")
    void cacheDomainEhPuro() {
        List<String> violacoes = new ArrayList<>();
        for (JavaClass classe : classesProducao) {
            String pkg = classe.getPackageName();
            boolean ehDominio = pkg.equals(PKG_CACHE_DOMAIN) || pkg.startsWith(PKG_CACHE_DOMAIN + ".");
            if (!ehDominio) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String destinoPkg = dependencia.getTargetClass().getPackageName();
                String destino = dependencia.getTargetClass().getName();
                boolean infraCache = destinoPkg.startsWith(PKG_CACHE_INFRA);
                boolean framework = destino.startsWith("jakarta.")
                    || destino.startsWith("io.quarkus")
                    || destino.startsWith("io.smallrye")
                    || destino.startsWith("org.eclipse.microprofile")
                    || destino.startsWith("org.springframework")
                    || destino.startsWith("com.fasterxml.jackson");
                if (infraCache || framework) {
                    violacoes.add(origem + " -> " + topo(destino));
                }
            }
        }
        assertTrue(violacoes.isEmpty(),
            () -> "cachetraducao.domain deve permanecer puro (sem infrastructure/framework):\n"
                + String.join("\n", new TreeSet<>(violacoes)));
    }

    @Test
    @DisplayName("cachetraducao mantém 3 records em domain e 2 serviços em infrastructure (E6, sem tipo E7/E8)")
    void cacheMantemEstruturaHomologada() {
        TreeSet<String> domain = new TreeSet<>();
        TreeSet<String> infra = new TreeSet<>();
        for (JavaClass classe : classesProducao) {
            String pkg = classe.getPackageName();
            String nome = topo(classe.getName());
            if (nome.contains("$")) {
                continue;
            }
            if (pkg.equals(PKG_CACHE_DOMAIN)) {
                domain.add(nome.substring(nome.lastIndexOf('.') + 1));
            } else if (pkg.equals(PKG_CACHE_INFRA)) {
                infra.add(nome.substring(nome.lastIndexOf('.') + 1));
            }
        }
        assertTrue(domain.equals(new TreeSet<>(List.of("CacheDocumento", "EntradaCache", "ProvenienciaCache"))),
            () -> "cachetraducao.domain deve conter exatamente os 3 modelos. Encontrado: " + domain);
        assertTrue(infra.equals(new TreeSet<>(List.of("CacheManutencaoService", "CacheTraducaoService"))),
            () -> "cachetraducao.infrastructure deve conter exatamente os 2 serviços. Encontrado: " + infra);
    }

    private static boolean ehDoCache(JavaClass classe) {
        String pkg = classe.getPackageName();
        return pkg.equals(PKG_CACHE) || pkg.startsWith(PKG_CACHE + ".");
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
