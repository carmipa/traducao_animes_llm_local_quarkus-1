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
 *       só JDK/libs técnicas, {@code core} e o próprio {@code contexto}. Em particular NÃO
 *       depende de {@code cachetraducao}: o {@code SnapshotContexto} entrega o prompt
 *       congelado e nada mais — quem deriva o hash do carimbo de cache é
 *       {@code traducao.application.ResolvedorCacheTraducao}, com
 *       {@code ProvenienciaCache.hashDe}. Manter uma cópia do algoritmo aqui criaria DUAS
 *       fontes do mesmo hash e, ao divergirem, invalidaria todo o cache já gravado.</li>
 *   <li>{@code contexto.domain} é puro: sem {@code contexto.infrastructure} nem framework.</li>
 *   <li>{@code contexto.lore} depende somente de {@code contexto.domain}, JDK e Spring
 *       {@code @Component} — nunca de {@code core}, {@code infrastructure} ou outra fatia.</li>
 *   <li>{@code contexto.infrastructure} é congelado nominalmente: exatamente
 *       {@code GerenciadorContexto} e {@code ContextoBeansConfig}.</li>
 *   <li>{@code contexto.domain} contém os OITO tipos homologados — os cinco da E7b, mais
 *       {@code SnapshotContexto} (a fotografia imutável do contexto ativo, para que uma
 *       execução pare de reconsultar o contexto global mutável no meio de um arquivo),
 *       {@code VeredictoObraContexto} (o desfecho da comparação obra×contexto, movido de
 *       {@code qualidadeTraducao.domain} porque IDENTIDADE DE OBRA é assunto deste peer) e
 *       {@code IdentidadeObra} (a identidade canônica derivada de id + nome de exibição, que
 *       deu cobertura de reconhecimento a TODAS as obras do catálogo de uma vez, e não só às
 *       que declaram apelidos à mão); {@code contexto.lore} agrega 82 classes.</li>
 *   <li>{@code contexto.application} é congelado nominalmente em exatamente
 *       {@code ValidadorCompatibilidadeObraContexto} — o serviço que julga se o arquivo
 *       pertence à obra cuja lore está selecionada, movido de
 *       {@code qualidadeTraducao.application.GuardaObraContextoService}. O peer
 *       {@code qualidadeTraducao} é dono da validação do TEXTO produzido, não da identidade
 *       da obra; a fatia {@code traducao} apenas consome o veredicto e o traduz em efeito.</li>
 *   <li>{@code contexto.application} NÃO depende de {@code contexto.infrastructure}: o
 *       validador recebe os ids que reconhecem a pasta como DADO, sem consultar o
 *       {@code GerenciadorContexto}, e por isso é testável sem container.</li>
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
    private static final String PKG_CONTEXTO_APPLICATION = RAIZ + ".contexto.application";
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
    @DisplayName("contexto.infrastructure é congelado NOMINALMENTE (E7b): GerenciadorContexto, ContextoBeansConfig e CatalogoLoreYaml")
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
        // CatalogoLoreYaml entrou em 2026-08-15, e a entrada é DELIBERADA: é a ordem de Paulo de
        // pôr todas as lores num arquivo único, e o leitor desse arquivo é infraestrutura do peer
        // — mesma natureza do GerenciadorContexto, que também só orquestra o catálogo.
        //
        // A catraca funcionou como devia e vale registrar: ela reprovou a classe nova ANTES de
        // qualquer commit, obrigando a declaração em vez de deixar o pacote crescer sozinho. É
        // exatamente para isso que o congelamento é NOMINAL e não "infrastructure liberado".
        assertEquals(
            new TreeSet<>(List.of("CatalogoLoreYaml", "ContextoBeansConfig", "GerenciadorContexto")), infra,
            "contexto.infrastructure deve conter EXATAMENTE GerenciadorContexto, ContextoBeansConfig "
                + "e CatalogoLoreYaml (sem liberação genérica de infrastructure; qualquer quarta "
                + "classe reprova). Encontrado: " + infra);
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
    @DisplayName("estrutura homologada: 8 tipos em domain e 5 classes residuais em contexto.lore (eram 82 até a lore virar arquivo)")
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
                "IdentidadeObra", "ProvedorContexto", "RegrasConcordanciaPtBr", "SnapshotContexto",
                "VeredictoObraContexto"))),
            () -> "contexto.domain deve conter exatamente os 8 tipos homologados (VeredictoObraContexto entrou "
                + "vindo de qualidadeTraducao.domain e IdentidadeObra nasceu aqui: identidade de obra é deste "
                + "peer). Encontrado: " + domain);
        assertEquals(5, lores,
            "contexto.lore deve conter exatamente 5 classes. Eram 82 ate 2026-08-15, quando as 69 "
                + "obras do CDI e os 9 mapas de terminologia que elas consumiam viraram o ARQUIVO "
                + "UNICO /lore/lore-traducao.yaml (ordem de Paulo: 'todas as lores devem ficar em "
                + "um unico arquivo'). Sobraram 4, e o motivo de cada uma e o mesmo: elas NAO estao "
                + "no arquivo.\n"
                + "  ContextoMacross7Filmes / MacrossFrontierFilmes / MacrossDeltaFilmes -- as 3 "
                + "agregadoras que ficam FORA do CDI de proposito (ver CatracaAgregadorasForaDoCdiTest). "
                + "O gerador le os provedores REGISTRADOS, entao nunca as viu: apaga-las perderia lore "
                + "que o arquivo nao tem.\n"
                + "  CorrecoesTerminologiaMacross e CorrecoesTerminologiaMacrossDelta -- sao os mapas que as 3\n"
                + "  chamam; saem junto com elas, nao antes. A segunda so apareceu quando o compilador\n"
                + "  reprovou: a primeira lista de preservacao tinha so a primeira, e a agregadora Delta usa outra.\n"
                + "Trazer as 3 para o arquivo e decisao propria, porque exige representar 'obra fora do "
                + "registro' no YAML sem quebrar a contagem de 69 provedores. Ate la, este numero e 4 e "
                + "qualquer quinta classe aqui reprova.");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: congela nominalmente a camada de aplicação do peer, criada neste
     * passo para receber a compatibilidade obra×contexto. Impede que
     * {@code contexto.application} vire depósito de serviços avulsos: hoje ela existe para
     * UMA responsabilidade — julgar se o arquivo pertence à obra cuja lore está selecionada.
     *
     * <p>INVARIANTES DO DOMÍNIO: exatamente {@code ValidadorCompatibilidadeObraContexto},
     * movido de {@code qualidadeTraducao.application.GuardaObraContextoService}. Qualquer
     * segunda classe reprova até autorização explícita.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: reprova listando o conteúdo real do pacote.
     */
    @Test
    @DisplayName("contexto.application é congelado NOMINALMENTE: exatamente ValidadorCompatibilidadeObraContexto")
    void aplicacaoCongeladaNominalmente() {
        TreeSet<String> application = new TreeSet<>();
        for (JavaClass classe : classesProducao) {
            String pkg = classe.getPackageName();
            if (!(pkg.equals(PKG_CONTEXTO_APPLICATION) || pkg.startsWith(PKG_CONTEXTO_APPLICATION + "."))) {
                continue;
            }
            String nome = topo(classe.getName());
            if (nome.contains("$")) {
                continue;
            }
            application.add(nome.substring(nome.lastIndexOf('.') + 1));
        }
        assertEquals(new TreeSet<>(List.of("ValidadorCompatibilidadeObraContexto")), application,
            "contexto.application deve conter EXATAMENTE ValidadorCompatibilidadeObraContexto "
                + "(sem liberação genérica da camada; qualquer segunda classe reprova). Encontrado: " + application);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mantém o {@code ValidadorCompatibilidadeObraContexto} decidível
     * a partir de DADOS, não de estado global — ele recebe os ids que reconhecem a pasta como
     * argumento em vez de consultar o {@code GerenciadorContexto}. É o que o deixa
     * determinístico e testável sem container.
     *
     * <p>INVARIANTES DO DOMÍNIO: nenhuma classe de {@code contexto.application} pode depender
     * de {@code contexto.infrastructure}. A direção permitida é a inversa (infrastructure
     * pode compor application), nunca esta.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: reprova listando a aresta exata.
     */
    @Test
    @DisplayName("contexto.application não depende de contexto.infrastructure (validador julga por DADO, não consulta o gerenciador)")
    void aplicacaoNaoDependeDeInfrastructure() {
        List<String> violacoes = new ArrayList<>();
        for (JavaClass classe : classesProducao) {
            String pkg = classe.getPackageName();
            if (!(pkg.equals(PKG_CONTEXTO_APPLICATION) || pkg.startsWith(PKG_CONTEXTO_APPLICATION + "."))) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String destinoPkg = dependencia.getTargetClass().getPackageName();
                if (destinoPkg.equals(PKG_CONTEXTO_INFRA) || destinoPkg.startsWith(PKG_CONTEXTO_INFRA + ".")) {
                    violacoes.add(origem + " -> " + topo(dependencia.getTargetClass().getName()));
                }
            }
        }
        assertTrue(violacoes.isEmpty(),
            () -> "contexto.application não pode depender de contexto.infrastructure:\n"
                + String.join("\n", new TreeSet<>(violacoes)));
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
