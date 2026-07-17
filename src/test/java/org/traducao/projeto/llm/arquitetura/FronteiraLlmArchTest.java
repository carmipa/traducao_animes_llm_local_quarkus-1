package org.traducao.projeto.llm.arquitetura;

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
 * PROPÓSITO DE NEGÓCIO: congela a INDEPENDÊNCIA do peer de topo {@code llm} extraído na
 * E8d — o contrato genérico do modelo de linguagem ({@code LlmPort}) e seus records
 * ({@code Lote}, {@code TraducaoLote}, {@code StatusLlm}), todos em {@code llm.domain}.
 * Garante que o peer é consumível por qualquer fatia funcional sem acoplamento reverso e
 * sem arrastar framework, HTTP ou o cliente concreto.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code llm} só depende de JDK e, se houver consumo real e justificado,
 *       {@code core} — nunca de {@code traducao} nem de outra fatia funcional, nem de outro
 *       peer ({@code legenda}, {@code cachetraducao}, {@code contexto},
 *       {@code qualidadeTraducao}), nem de framework (Spring/Quarkus/Jackson), cliente HTTP
 *       ou infraestrutura concreta.</li>
 *   <li>Inventário nominal EXATO por FQN COMPLETO: exatamente os quatro proprietários
 *       top-level ({@code llm.domain.LlmPort}, {@code llm.domain.Lote},
 *       {@code llm.domain.TraducaoLote}, {@code llm.domain.StatusLlm}). Congelar por FQN
 *       (não por simple name) impede que uma classe mude de pacote/camada mantendo o mesmo
 *       nome sem reprovar.</li>
 *   <li>Estrutura: todos os quatro tipos ficam em {@code llm.domain}; NÃO existe
 *       {@code llm.infrastructure} nem {@code llm.application}; não há adapter nem
 *       implementação concreta de {@code LlmPort} dentro de {@code llm} (o
 *       {@code LlmClientAdapter} permanece em {@code traducao.infrastructure}).</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer dependência proibida, tipo fora do inventário ou pacote não-domain sob
 * {@code llm} reprova o teste, listando o desvio exato.
 */
class FronteiraLlmArchTest {

    private static final String RAIZ = "org.traducao.projeto";
    private static final String PREFIXO = RAIZ + ".";
    private static final String FATIA_LLM = "llm";
    private static final String FATIA_CORE = "core";
    private static final String PKG_LLM = RAIZ + ".llm";
    private static final String PKG_LLM_DOMAIN = RAIZ + ".llm.domain";

    private static JavaClasses classesProducao;

    @BeforeAll
    static void importar() {
        classesProducao = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RAIZ);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante que o peer llm é fonte, nunca consumidor de fatia —
     * pode ser importado por qualquer uma sem criar acoplamento reverso.
     * <p>INVARIANTES DO DOMÍNIO: só JDK, libs técnicas, core e o próprio llm são destinos
     * permitidos.
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer aresta para traducao/fatia/outro peer
     * reprova, listando origem -> destino.
     */
    @Test
    @DisplayName("llm NÃO depende de fatia funcional nem de outro peer (só JDK, técnico, core e o próprio)")
    void llmNaoDependeDeFatiaNemDeOutroPeer() {
        List<String> violacoes = new ArrayList<>();
        for (JavaClass classe : classesProducao) {
            if (!ehDoLlm(classe)) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String fatia = fatiaDe(dependencia.getTargetClass().getPackageName());
                // Permitidos: JDK/libs (null), o próprio llm e core.
                if (fatia == null || fatia.equals(FATIA_LLM) || fatia.equals(FATIA_CORE)) {
                    continue;
                }
                violacoes.add(origem + " -> " + topo(dependencia.getTargetClass().getName()));
            }
        }
        assertTrue(violacoes.isEmpty(),
            () -> "O peer llm só pode depender de JDK, libs técnicas, core e do próprio llm. "
                + "Nenhuma dependência para traducao/fatia funcional nem para outro peer "
                + "(legenda/cachetraducao/contexto/qualidadeTraducao) é permitida.\n"
                + String.join("\n", new TreeSet<>(violacoes)));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: congela a superfície de produção do peer nos exatos quatro
     * proprietários homologados na E8d.
     * <p>INVARIANTES DO DOMÍNIO: igualdade EXATA por FQN completo; nenhuma quinta classe de
     * produção pode entrar silenciosamente.
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer FQN a mais ou a menos reprova.
     */
    @Test
    @DisplayName("inventário nominal EXATO por FQN: exatamente os quatro proprietários top-level do peer llm")
    void inventarioNominalExato() {
        TreeSet<String> topLevelsFqn = new TreeSet<>();
        for (JavaClass classe : classesProducao) {
            if (!ehDoLlm(classe)) {
                continue;
            }
            topLevelsFqn.add(topo(classe.getName()));
        }
        assertEquals(new TreeSet<>(List.of(
                PKG_LLM_DOMAIN + ".LlmPort",
                PKG_LLM_DOMAIN + ".Lote",
                PKG_LLM_DOMAIN + ".StatusLlm",
                PKG_LLM_DOMAIN + ".TraducaoLote")), topLevelsFqn,
            "llm deve conter EXATAMENTE os quatro proprietários top-level homologados, por FQN. "
                + "Encontrado: " + topLevelsFqn);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mantém o peer como domínio puro — sem framework, HTTP ou
     * infraestrutura — para que qualquer fatia possa consumi-lo sem herdar dependências
     * técnicas.
     * <p>INVARIANTES DO DOMÍNIO: {@code llm.domain} só pode depender de JDK e core; nada de
     * jakarta/quarkus/smallrye/microprofile/spring/jackson/cliente HTTP.
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer dependência técnica ou de outra fatia
     * reprova, listando origem -> destino.
     */
    @Test
    @DisplayName("llm.domain é puro: só JDK e core (sem framework/HTTP/infraestrutura)")
    void domainEhPuro() {
        List<String> violacoes = new ArrayList<>();
        for (JavaClass classe : classesProducao) {
            String pkg = classe.getPackageName();
            if (!(pkg.equals(PKG_LLM_DOMAIN) || pkg.startsWith(PKG_LLM_DOMAIN + "."))) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String destinoPkg = dependencia.getTargetClass().getPackageName();
                String destino = dependencia.getTargetClass().getName();
                String fatia = fatiaDe(destinoPkg);
                boolean naoLlm = !(destinoPkg.equals(PKG_LLM) || destinoPkg.startsWith(PKG_LLM + "."));
                boolean permitido = fatia == null || fatia.equals(FATIA_CORE) || !naoLlm;
                boolean tecnicoProibido = destino.startsWith("jakarta.")
                    || destino.startsWith("io.quarkus")
                    || destino.startsWith("io.smallrye")
                    || destino.startsWith("org.eclipse.microprofile")
                    || destino.startsWith("org.springframework")
                    || destino.startsWith("com.fasterxml.jackson");
                if (!permitido || tecnicoProibido) {
                    violacoes.add(origem + " -> " + topo(destino));
                }
            }
        }
        assertTrue(violacoes.isEmpty(),
            () -> "llm.domain deve permanecer puro (só JDK e core; sem framework/HTTP/infraestrutura):\n"
                + String.join("\n", new TreeSet<>(violacoes)));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante que o peer é só contrato de domínio — sem adapter nem
     * implementação concreta dentro dele (o cliente vive em traducao.infrastructure).
     * <p>INVARIANTES DO DOMÍNIO: sob {@code llm} só existe o pacote {@code domain}; não há
     * {@code llm.infrastructure} nem {@code llm.application}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer classe de produção do peer fora de
     * {@code llm.domain} reprova.
     */
    @Test
    @DisplayName("llm contém somente o pacote domain (sem infrastructure/application, sem adapter)")
    void llmContemSomenteDomain() {
        List<String> foraDoDomain = new ArrayList<>();
        for (JavaClass classe : classesProducao) {
            if (!ehDoLlm(classe)) {
                continue;
            }
            String pkg = classe.getPackageName();
            if (!(pkg.equals(PKG_LLM_DOMAIN) || pkg.startsWith(PKG_LLM_DOMAIN + "."))) {
                foraDoDomain.add(topo(classe.getName()) + " (pacote " + pkg + ")");
            }
        }
        assertTrue(foraDoDomain.isEmpty(),
            () -> "O peer llm só pode ter classes em llm.domain nesta fase — sem infrastructure/"
                + "application, sem adapter nem implementação concreta de LlmPort:\n"
                + String.join("\n", new TreeSet<>(foraDoDomain)));
    }

    private static boolean ehDoLlm(JavaClass classe) {
        String pkg = classe.getPackageName();
        return pkg.equals(PKG_LLM) || pkg.startsWith(PKG_LLM + ".");
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
