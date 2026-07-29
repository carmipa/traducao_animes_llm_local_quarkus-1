package org.traducao.projeto.trocaTipoLegenda.arquitetura;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela o isolamento da fatia {@code trocaTipoLegenda} depois do
 * desacoplamento de 2026-07-29 — a regra de negócio do achatamento não pode voltar a
 * importar serviços de outras fatias.
 *
 * <h2>Por que existe</h2>
 * A fatia acumulou {@code AnsiCores} no {@code application} e serviços do peer
 * {@code legenda} dentro do achatador sem que nada apontasse. Os oito
 * {@code Fronteira*ArchTest} do projeto cobrem apenas os peers; nenhuma das ~15 fatias
 * funcionais tinha teste de fronteira, e é por isso que a erosão passou despercebida.
 *
 * <p>O custo apareceu na prática: o achatador HERDAVA a decisão de karaokê do peer, e com
 * ela herdou o defeito dela — {@code ESTILO_MUSICA_AMPLO_PATTERN} não reconhece o estilo
 * {@code OPL2} (lookaround de letra), então a camada de sílabas do Gundam Unicorn passava
 * intacta. Quem depende da regra alheia recebe o bug alheio.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code application} NÃO importa {@code infrastructure} de nenhuma fatia — I/O
 *       entra por porta.</li>
 *   <li>{@code application} NÃO importa {@code presentation} de ninguém: cor de terminal
 *       não é regra de negócio.</li>
 *   <li>{@code domain} é puro — não conhece {@code infrastructure} da própria fatia nem
 *       framework.</li>
 *   <li>Só {@code infrastructure} pode falar com o peer {@code legenda.application}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Cada violação é listada com a aresta exata. A dívida remanescente conhecida está
 * declarada nominalmente em {@link #EXCECOES_DECLARADAS}: quem a resolver deve APAGAR a
 * linha correspondente, e quem adicionar uma nova precisa justificá-la aqui.
 */
class FronteiraTrocaTipoLegendaArchTest {

    private static final String RAIZ = "org.traducao.projeto";
    private static final String PKG_FATIA = RAIZ + ".trocaTipoLegenda";
    private static final String PKG_APPLICATION = PKG_FATIA + ".application";
    private static final String PKG_DOMAIN = PKG_FATIA + ".domain";

    /**
     * Dívida declarada, medida em 2026-07-29 — NÃO é permissão genérica.
     *
     * <p>Vive no {@code TrocaTipoLegendaUseCase}, que carrega uma {@code SessaoTroca} com
     * cronômetro e prefixo UTC no log. Migrar de passagem quebraria esse comportamento,
     * então ficou para um passo próprio. O {@code AchatarEstilosUseCase} — o que serve à
     * TV, e o que descarta a camada de karaokê — está completamente desacoplado.
     *
     * <p>Formato: {@code ClasseOrigem -> ClasseDestino}.
     */
    private static final Set<String> EXCECOES_DECLARADAS = Set.of(
        "TrocaTipoLegendaUseCase -> TelemetriaService"
    );

    /**
     * LIMITE CONHECIDO DESTA FRONTEIRA — {@code AnsiCores} é invisível para o ArchUnit.
     *
     * <p>Suas cores são {@code public static final String} com valor literal, e o
     * compilador Java faz INLINE de constante String em tempo de compilação: o bytecode do
     * {@code TrocaTipoLegendaUseCase} não guarda referência alguma à classe, apenas o
     * texto {@code "[32m"} embutido. O teste
     * {@link #applicationNaoDependeDePresentation()} passa com 11 usos no fonte.
     *
     * <p>Portanto: PASSAR nesse teste não prova ausência de {@code AnsiCores}. Quem quiser
     * essa garantia precisa checar o código-fonte (import/uso textual), não o bytecode.
     * Nenhuma regra de ArchUnit resolve — a informação já não existe no artefato analisado.
     */
    private static final String LIMITE_CONSTANTES_INLINE =
        "AnsiCores é constante String inlined pelo compilador e não aparece no bytecode";

    /**
     * Dívida visível apenas no fonte, pelo mesmo motivo acima. Some daqui quando o
     * {@code TrocaTipoLegendaUseCase} passar a usar {@code ConsoleTrocaPort} — o que exige
     * decidir o que fazer com o prefixo UTC da {@code SessaoTroca}.
     */
    private static final Set<String> EXCECOES_FONTE = Set.of(
        "TrocaTipoLegendaUseCase -> AnsiCores"
    );

    private static JavaClasses classesProducao;

    @BeforeAll
    static void importar() {
        classesProducao = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RAIZ);
    }

    @Test
    @DisplayName("application não conhece infrastructure de ninguém — I/O entra por porta")
    void applicationNaoDependeDeInfrastructure() {
        List<String> violacoes = new ArrayList<>();
        for (JavaClass classe : classesDe(PKG_APPLICATION)) {
            for (Dependency d : classe.getDirectDependenciesFromSelf()) {
                String destinoPkg = d.getTargetClass().getPackageName();
                if (!destinoPkg.startsWith(RAIZ + ".") || !destinoPkg.contains(".infrastructure")) {
                    continue;
                }
                registrar(violacoes, classe, d);
            }
        }
        assertTrue(violacoes.isEmpty(),
            () -> "application da fatia não pode depender de infrastructure — crie uma porta em "
                + "domain/ports e um adaptador:\n" + String.join("\n", new TreeSet<>(violacoes)));
    }

    @Test
    @DisplayName("application não conhece presentation — cor de terminal não é regra de negócio")
    void applicationNaoDependeDePresentation() {
        List<String> violacoes = new ArrayList<>();
        for (JavaClass classe : classesDe(PKG_APPLICATION)) {
            for (Dependency d : classe.getDirectDependenciesFromSelf()) {
                String destinoPkg = d.getTargetClass().getPackageName();
                if (!destinoPkg.startsWith(RAIZ + ".") || !destinoPkg.contains(".presentation")) {
                    continue;
                }
                registrar(violacoes, classe, d);
            }
        }
        assertTrue(violacoes.isEmpty(),
            () -> "application da fatia não pode depender de presentation — declare a INTENÇÃO "
                + "(sucesso/aviso/erro) via ConsoleTrocaPort:\n" + String.join("\n", new TreeSet<>(violacoes)));
    }

    @Test
    @DisplayName("só infrastructure fala com a application do peer legenda")
    void regraDoPeerFicaAtrasDoAdaptador() {
        List<String> violacoes = new ArrayList<>();
        for (JavaClass classe : classesDe(PKG_APPLICATION)) {
            for (Dependency d : classe.getDirectDependenciesFromSelf()) {
                if (!d.getTargetClass().getPackageName().startsWith(RAIZ + ".legenda.application")) {
                    continue;
                }
                registrar(violacoes, classe, d);
            }
        }
        assertTrue(violacoes.isEmpty(),
            () -> "Regra de decisão do peer legenda deve entrar por ClassificadorCamadaMusicalPort. "
                + "Herdar a decisão herda também o defeito dela (ver OPL2 e o padrão amplo):\n"
                + String.join("\n", new TreeSet<>(violacoes)));
    }

    @Test
    @DisplayName("domain da fatia é puro: sem infrastructure própria e sem framework")
    void domainEhPuro() {
        List<String> violacoes = new ArrayList<>();
        for (JavaClass classe : classesDe(PKG_DOMAIN)) {
            for (Dependency d : classe.getDirectDependenciesFromSelf()) {
                String destino = d.getTargetClass().getName();
                boolean infraDaFatia = d.getTargetClass().getPackageName().startsWith(PKG_FATIA + ".infrastructure");
                boolean framework = destino.startsWith("jakarta.")
                    || destino.startsWith("io.quarkus")
                    || destino.startsWith("io.smallrye")
                    || destino.startsWith("org.eclipse.microprofile")
                    || destino.startsWith("org.springframework");
                if (infraDaFatia || framework) {
                    registrar(violacoes, classe, d);
                }
            }
        }
        assertTrue(violacoes.isEmpty(),
            () -> "domain da fatia deve permanecer puro:\n" + String.join("\n", new TreeSet<>(violacoes)));
    }

    @Test
    @DisplayName("nenhum import de core.presentation no application — checado no FONTE, não no bytecode")
    void applicationNaoImportaPresentationNoCodigoFonte() throws IOException {
        // Complementa applicationNaoDependeDePresentation, que é cego para constante String
        // inlined (ver LIMITE_CONSTANTES_INLINE). Aqui a evidência é textual: o import existe
        // ou não existe no arquivo.
        Path raizApplication = Path.of("src/main/java/org/traducao/projeto/trocaTipoLegenda/application");
        List<String> violacoes = new ArrayList<>();
        try (Stream<Path> arquivos = Files.walk(raizApplication)) {
            for (Path arquivo : arquivos.filter(p -> p.toString().endsWith(".java")).toList()) {
                String nome = arquivo.getFileName().toString().replace(".java", "");
                for (String linha : Files.readAllLines(arquivo)) {
                    String t = linha.strip();
                    if (t.startsWith("import ") && t.contains(".core.presentation.")) {
                        String destino = simples(t.replace("import ", "").replace(";", ""));
                        if (!EXCECOES_FONTE.contains(nome + " -> " + destino)) {
                            violacoes.add(nome + " -> " + destino);
                        }
                    }
                }
            }
        }
        assertTrue(violacoes.isEmpty(),
            () -> "application importa core.presentation (" + LIMITE_CONSTANTES_INLINE + "). "
                + "Declare a INTENÇÃO via ConsoleTrocaPort:\n" + String.join("\n", new TreeSet<>(violacoes)));
    }

    @Test
    @DisplayName("as exceções declaradas ainda existem — some da lista quem já foi resolvido")
    void excecoesDeclaradasNaoViramLetraMorta() {
        // Sem isto, uma exceção resolvida ficaria na lista para sempre, dando a impressão de
        // dívida onde não há mais — e escondendo que a fronteira já poderia ser mais estrita.
        List<String> obsoletas = new ArrayList<>(EXCECOES_DECLARADAS);
        for (JavaClass classe : classesDe(PKG_APPLICATION)) {
            for (Dependency d : classe.getDirectDependenciesFromSelf()) {
                obsoletas.remove(simples(classe.getName()) + " -> " + simples(d.getTargetClass().getName()));
            }
        }
        assertTrue(obsoletas.isEmpty(),
            () -> "Estas exceções não correspondem mais a nenhuma dependência real. "
                + "Apague-as de EXCECOES_DECLARADAS:\n" + String.join("\n", new TreeSet<>(obsoletas)));
    }

    private static List<JavaClass> classesDe(String pacote) {
        List<JavaClass> encontradas = new ArrayList<>();
        for (JavaClass classe : classesProducao) {
            String pkg = classe.getPackageName();
            if (pkg.equals(pacote) || pkg.startsWith(pacote + ".")) {
                encontradas.add(classe);
            }
        }
        return encontradas;
    }

    private static void registrar(List<String> violacoes, JavaClass origem, Dependency d) {
        String aresta = simples(origem.getName()) + " -> " + simples(d.getTargetClass().getName());
        if (!EXCECOES_DECLARADAS.contains(aresta)) {
            violacoes.add(aresta);
        }
    }

    private static String simples(String nomeCompleto) {
        int ponto = nomeCompleto.lastIndexOf('.');
        String nome = ponto < 0 ? nomeCompleto : nomeCompleto.substring(ponto + 1);
        int cifrao = nome.indexOf('$');
        return cifrao < 0 ? nome : nome.substring(0, cifrao);
    }
}
