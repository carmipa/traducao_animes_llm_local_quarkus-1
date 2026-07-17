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
 * sem arrastar framework, cliente HTTP ou qualquer biblioteca externa.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Allowlist POSITIVA de destinos: {@code llm} só pode depender de pacotes do JDK
 *       ({@code java.}), do próprio {@code org.traducao.projeto.llm} e de
 *       {@code org.traducao.projeto.core} (se houver consumo real). Qualquer outro pacote
 *       — outra fatia, outro peer, framework (Spring/Quarkus/Jackson), cliente HTTP ou
 *       qualquer biblioteca externa — é violação. NÃO se admite um destino só porque não
 *       pertence a uma fatia conhecida.</li>
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
 * Qualquer dependência fora da allowlist positiva (JDK/llm/core), tipo fora do inventário
 * ou pacote não-domain sob {@code llm} reprova o teste, listando o desvio exato.
 */
class FronteiraLlmArchTest {

    private static final String RAIZ = "org.traducao.projeto";
    private static final String PKG_LLM = RAIZ + ".llm";
    private static final String PKG_LLM_DOMAIN = RAIZ + ".llm.domain";
    private static final String PKG_CORE = RAIZ + ".core";

    private static JavaClasses classesProducao;

    @BeforeAll
    static void importar() {
        classesProducao = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RAIZ);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante que o peer llm é fonte, nunca consumidor — pode ser
     * importado por qualquer fatia sem criar acoplamento reverso e sem herdar dependências
     * externas.
     * <p>INVARIANTES DO DOMÍNIO: allowlist POSITIVA — só JDK ({@code java.}), o próprio
     * {@code llm} e {@code core} são destinos permitidos. Qualquer outro pacote (fatia,
     * peer, framework, cliente HTTP, biblioteca externa) é violação, mesmo que não pertença
     * a uma fatia conhecida.
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer aresta fora de JDK/llm/core reprova,
     * listando origem -> destino.
     */
    @Test
    @DisplayName("llm só depende de JDK, do próprio llm e de core (allowlist positiva; sem fatia/peer/framework/lib externa)")
    void llmSoDependeDeJdkLlmECore() {
        List<String> violacoes = new ArrayList<>();
        for (JavaClass classe : classesProducao) {
            if (!ehDoLlm(classe)) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String destinoPkg = dependencia.getTargetClass().getPackageName();
                if (pacotePermitidoNoLlm(destinoPkg)) {
                    continue;
                }
                violacoes.add(origem + " -> " + topo(dependencia.getTargetClass().getName()));
            }
        }
        assertTrue(violacoes.isEmpty(),
            () -> "O peer llm só pode depender de JDK (java.), do próprio llm e de core. "
                + "Qualquer outro destino — fatia funcional, outro peer, framework, cliente HTTP "
                + "ou biblioteca externa — é proibido, ainda que não pertença a uma fatia conhecida.\n"
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
     * biblioteca externa — para que qualquer fatia possa consumi-lo sem herdar dependências
     * técnicas.
     * <p>INVARIANTES DO DOMÍNIO: allowlist POSITIVA — {@code llm.domain} só pode depender de
     * JDK ({@code java.}), do próprio {@code llm} e de {@code core}. Qualquer outro pacote é
     * violação, inclusive frameworks e bibliotecas externas que não pertencem a uma fatia.
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer dependência fora de JDK/llm/core reprova,
     * listando origem -> destino.
     */
    @Test
    @DisplayName("llm.domain é puro: só JDK, o próprio llm e core (allowlist positiva; sem framework/HTTP/lib externa)")
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
                if (pacotePermitidoNoLlm(destinoPkg)) {
                    continue;
                }
                violacoes.add(origem + " -> " + topo(dependencia.getTargetClass().getName()));
            }
        }
        assertTrue(violacoes.isEmpty(),
            () -> "llm.domain deve permanecer puro: só JDK (java.), o próprio llm e core. "
                + "Framework, cliente HTTP ou qualquer biblioteca externa é proibido.\n"
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

    /**
     * Allowlist POSITIVA de pacotes de destino permitidos para o peer llm: JDK ({@code java.}),
     * o próprio {@code llm} e {@code core}. Diferente de uma verificação por fatia, NÃO admite
     * um destino apenas por ele não pertencer a uma fatia conhecida — bibliotecas externas
     * (commons, guava, clientes HTTP, etc.) são explicitamente proibidas.
     */
    private static boolean pacotePermitidoNoLlm(String destinoPkg) {
        if (destinoPkg == null) {
            return false;
        }
        boolean ehJdk = destinoPkg.equals("java") || destinoPkg.startsWith("java.");
        boolean ehLlm = destinoPkg.equals(PKG_LLM) || destinoPkg.startsWith(PKG_LLM + ".");
        boolean ehCore = destinoPkg.equals(PKG_CORE) || destinoPkg.startsWith(PKG_CORE + ".");
        return ehJdk || ehLlm || ehCore;
    }

    private static boolean ehDoLlm(JavaClass classe) {
        String pkg = classe.getPackageName();
        return pkg.equals(PKG_LLM) || pkg.startsWith(PKG_LLM + ".");
    }

    private static String topo(String nomeCompleto) {
        int cifrao = nomeCompleto.indexOf('$');
        return cifrao < 0 ? nomeCompleto : nomeCompleto.substring(0, cifrao);
    }
}
