package org.traducao.projeto.traducao.arquitetura;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPOSITO DE NEGOCIO: congela a fronteira funcional INBOUND da fatia vertical
 * Traducao Local (org.traducao.projeto.traducao) — dependencias outra-fatia ->
 * traducao. Contraparte do fitness OUTBOUND (FronteiraTraducaoArchTest); as regras
 * da C2 permanecem separadas la.
 *
 * <p>DUAS MEDIDAS COMPLEMENTARES (baseline auditada da FASE E):
 * <ul>
 *   <li>Fitness principal (ArchUnit/bytecode): pre-E1 = 149, pos-E1 = 147, pos-E2 = 144, pos-E3b = 138, pos-E3c = 134, pos-E4a = 128, pos-E4b = 122, pos-E5a = 83. Mesmo
 *       rigor do OUTBOUND; fonte de verdade da fronteira.</li>
 *   <li>Inventario textual complementar (imports do fonte): pre-E1 = 150, pos-E1 = 148, pos-E2 = 145, pos-E3b = 139, pos-E3c = 135, pos-E4a =
 *       129, pos-E4b = 123, pos-E5a = 85. Impede o surgimento silencioso de novos imports outra-fatia -> traducao,
 *       inclusive tipos usados apenas em clausulas catch (que o ArchUnit 1.4.2 nao
 *       registra no grafo).</li>
 * </ul>
 *
 * <p>POR QUE 83 (bytecode) vs 85 (texto):
 * <ul>
 *   <li>-2 no bytecode: {@link #CATCH_ONLY_CORRECAO} e {@link #CATCH_ONLY_RASPAGEM}
 *       (-> AlucinacaoDetectadaException) sao usadas so em catch; existem como import
 *       no fonte, mas o ArchUnit 1.4.2 nao as coloca no grafo. Lacuna conhecida do
 *       bytecode, a blindar na subfase de AlucinacaoDetectadaException.</li>
 *   <li>Nota E5a: a aresta generica AuditorConteudoUseCase -> EventoLegenda (antes
 *       {@code GENERICA_AUDITOR}, visivel so no bytecode via DocumentoLegenda.eventos())
 *       deixou de existir na fronteira de traducao — ambos os modelos migraram para o
 *       modulo compartilhado legenda.</li>
 * </ul>
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer aresta inesperada ou esperada ausente
 * reprova o teste, listando o desvio exato.
 */
class FronteiraInboundArchTest {

    private static final String RAIZ = "org.traducao.projeto";
    private static final String PREFIXO = RAIZ + ".";
    private static final String FATIA_TRADUCAO = "traducao";
    private static final String PKG_TRADUCAO = RAIZ + ".traducao";

    private static final String CATCH_ONLY_CORRECAO = aresta(
        RAIZ + ".correcaoLegendas.application.CorrigirLegendasUseCase",
        RAIZ + ".traducao.domain.exceptions.AlucinacaoDetectadaException");
    private static final String CATCH_ONLY_RASPAGEM = aresta(
        RAIZ + ".raspagemRevisao.application.RevisarLegendasUseCase",
        RAIZ + ".traducao.domain.exceptions.AlucinacaoDetectadaException");

    /** Inventario TEXTUAL (imports do fonte) INBOUND, por aresta exata. 85 apos E5a. */
    private static final Set<String> INBOUND_TEXTUAL_ESPERADAS = Set.of(
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.AuditorConteudoUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaAss"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.AuditorConteudoUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaSrt"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.RegraDanoKaraoke", "org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.arquivounico.RegraSobreposicaoTempo", "org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorretorTraducaoLlmService", "org.traducao.projeto.traducao.application.ProtecaoLegendaAssService"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorretorTraducaoLlmService", "org.traducao.projeto.traducao.application.ValidadorTraducaoService"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorretorTraducaoLlmService", "org.traducao.projeto.traducao.domain.exceptions.AlucinacaoDetectadaException"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorretorTraducaoLlmService", "org.traducao.projeto.traducao.domain.ports.MistralPort"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorretorTraducaoLlmService", "org.traducao.projeto.traducao.infrastructure.legenda.MascaradorTags"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorrigirLegendasUseCase", "org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorrigirLegendasUseCase", "org.traducao.projeto.traducao.application.ProtecaoLegendaAssService"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorrigirLegendasUseCase", "org.traducao.projeto.traducao.domain.exceptions.AlucinacaoDetectadaException"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorrigirLegendasUseCase", "org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorrigirLegendasUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.EscritorLegendaAss"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorrigirLegendasUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaAss"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorrigirLegendasUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.MascaradorTags"),
        aresta("org.traducao.projeto.correcaoLegendas.presentation.CorrecaoLegendasController", "org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto"),
        aresta("org.traducao.projeto.novoKaraoke.application.ConversorKaraokeUseCase", "org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService"),
        aresta("org.traducao.projeto.raspagemCorrecao.application.CorrigirComGoogleUseCase", "org.traducao.projeto.traducao.infrastructure.cache.CacheManutencaoService"),
        aresta("org.traducao.projeto.raspagemCorrecao.application.CorrigirComGoogleUseCase", "org.traducao.projeto.traducao.infrastructure.cache.ProvenienciaCache"),
        aresta("org.traducao.projeto.raspagemRevisao.RevisorRaspagemCLI", "org.traducao.projeto.traducao.domain.StatusLlm"),
        aresta("org.traducao.projeto.raspagemRevisao.RevisorRaspagemCLI", "org.traducao.projeto.traducao.domain.ports.MistralPort"),
        aresta("org.traducao.projeto.raspagemRevisao.application.AuditorProblemasLegendaService", "org.traducao.projeto.traducao.application.DetectorTraducaoIdenticaService"),
        aresta("org.traducao.projeto.raspagemRevisao.application.AuditorProblemasLegendaService", "org.traducao.projeto.traducao.application.ValidadorTraducaoService"),
        aresta("org.traducao.projeto.raspagemRevisao.application.AuditorProblemasLegendaService", "org.traducao.projeto.traducao.domain.exceptions.AlucinacaoDetectadaException"),
        aresta("org.traducao.projeto.raspagemRevisao.application.LeitorCacheReferenciaService", "org.traducao.projeto.traducao.infrastructure.cache.CacheManutencaoService"),
        aresta("org.traducao.projeto.raspagemRevisao.application.LeitorCacheReferenciaService", "org.traducao.projeto.traducao.infrastructure.cache.EntradaCache"),
        aresta("org.traducao.projeto.raspagemRevisao.application.LeitorCacheReferenciaService", "org.traducao.projeto.traducao.infrastructure.cache.ProvenienciaCache"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarCacheUseCase", "org.traducao.projeto.traducao.application.ProtecaoLegendaAssService"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarCacheUseCase", "org.traducao.projeto.traducao.application.ValidadorTraducaoService"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarCacheUseCase", "org.traducao.projeto.traducao.domain.exceptions.AlucinacaoDetectadaException"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarCacheUseCase", "org.traducao.projeto.traducao.domain.ports.MistralPort"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarCacheUseCase", "org.traducao.projeto.traducao.infrastructure.cache.CacheManutencaoService"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarCacheUseCase", "org.traducao.projeto.traducao.infrastructure.cache.ProvenienciaCache"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarCacheUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.MascaradorTags"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.application.ProtecaoLegendaAssService"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.application.ValidadorTraducaoService"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.domain.exceptions.AlucinacaoDetectadaException"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.domain.ports.MistralPort"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.infrastructure.cache.EntradaCache"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.infrastructure.cache.ProvenienciaCache"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.EscritorLegendaAss"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaAss"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.MascaradorTags"),
        aresta("org.traducao.projeto.raspagemRevisao.application.SincronizadorLegendaCacheService", "org.traducao.projeto.traducao.infrastructure.cache.EntradaCache"),
        aresta("org.traducao.projeto.raspagemRevisao.presentation.web.RevisaoLegendasController", "org.traducao.projeto.traducao.domain.StatusLlm"),
        aresta("org.traducao.projeto.raspagemRevisao.presentation.web.RevisaoLegendasController", "org.traducao.projeto.traducao.domain.ports.MistralPort"),
        aresta("org.traducao.projeto.raspagemRevisao.presentation.web.RevisaoLegendasController", "org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto"),
        aresta("org.traducao.projeto.revisaoLore.application.RevisarLoreUseCase", "org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService"),
        aresta("org.traducao.projeto.revisaoLore.application.RevisarLoreUseCase", "org.traducao.projeto.traducao.application.ProtecaoLegendaAssService"),
        aresta("org.traducao.projeto.revisaoLore.application.RevisarLoreUseCase", "org.traducao.projeto.traducao.application.ValidadorTraducaoService"),
        aresta("org.traducao.projeto.revisaoLore.application.RevisarLoreUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.EscritorLegendaAss"),
        aresta("org.traducao.projeto.revisaoLore.application.RevisarLoreUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaAss"),
        aresta("org.traducao.projeto.revisaoLore.application.RevisarLoreUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.MascaradorTags"),
        aresta("org.traducao.projeto.traducaoCorrige.application.ClassificadorEntradaCacheService", "org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService"),
        aresta("org.traducao.projeto.traducaoCorrige.application.ClassificadorEntradaCacheService", "org.traducao.projeto.traducao.application.DetectorTraducaoIdenticaService"),
        aresta("org.traducao.projeto.traducaoCorrige.application.ClassificadorEntradaCacheService", "org.traducao.projeto.traducao.application.ProtecaoLegendaAssService"),
        aresta("org.traducao.projeto.traducaoCorrige.application.ClassificadorEntradaCacheService", "org.traducao.projeto.traducao.application.ValidadorTraducaoService"),
        aresta("org.traducao.projeto.traducaoCorrige.application.ClassificadorEntradaCacheService", "org.traducao.projeto.traducao.domain.exceptions.AlucinacaoDetectadaException"),
        aresta("org.traducao.projeto.traducaoCorrige.application.ContextoManutencaoCacheService", "org.traducao.projeto.traducao.infrastructure.cache.CacheManutencaoService"),
        aresta("org.traducao.projeto.traducaoCorrige.application.ContextoManutencaoCacheService", "org.traducao.projeto.traducao.infrastructure.cache.ProvenienciaCache"),
        aresta("org.traducao.projeto.traducaoCorrige.application.ContextoManutencaoCacheService", "org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto"),
        aresta("org.traducao.projeto.traducaoCorrige.application.LimparCacheUseCase", "org.traducao.projeto.traducao.infrastructure.cache.CacheManutencaoService"),
        aresta("org.traducao.projeto.traducaoCorrige.application.LimparCacheUseCase", "org.traducao.projeto.traducao.infrastructure.cache.ProvenienciaCache"),
        aresta("org.traducao.projeto.traducaoCorrige.presentation.web.CorrecaoCacheController", "org.traducao.projeto.traducao.domain.StatusLlm"),
        aresta("org.traducao.projeto.traducaoCorrige.presentation.web.CorrecaoCacheController", "org.traducao.projeto.traducao.domain.ports.MistralPort"),
        aresta("org.traducao.projeto.traducaoCorrige.presentation.web.CorrecaoCacheController", "org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.ClassificadorLetraKaraokeService", "org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.application.ValidadorTraducaoService"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.domain.Lote"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.domain.StatusLlm"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.domain.TraducaoLote"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.domain.exceptions.AlucinacaoDetectadaException"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.domain.ports.MistralPort"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.infrastructure.cache.CacheTraducaoService"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.infrastructure.cache.EntradaCache"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.EscritorLegendaAss"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaAss"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.MascaradorTags"),
        aresta("org.traducao.projeto.traducaoKaraoke.presentation.TraducaoKaraokeController", "org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto"),
        aresta("org.traducao.projeto.trocaTipoLegenda.application.TrocaTipoLegendaUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.EscritorLegendaAss"),
        aresta("org.traducao.projeto.trocaTipoLegenda.application.TrocaTipoLegendaUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaAss")
    );

    private static final String ALVO_INBOUND = RAIZ + ".traducao.";

    private static JavaClasses classesProducao;

    @BeforeAll
    static void importar() {
        classesProducao = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RAIZ);
    }

    @Test
    @DisplayName("Fitness principal (ArchUnit/bytecode): outras-fatias -> Traducao Local == 83")
    void inboundBytecodeBateComBaseline() {
        Set<String> reais = new TreeSet<>();
        for (JavaClass classe : classesProducao) {
            if (ehDaTraducao(classe)) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                if (FATIA_TRADUCAO.equals(fatiaDe(dependencia.getTargetClass().getPackageName()))) {
                    reais.add(aresta(origem, topo(dependencia.getTargetClass().getName())));
                }
            }
        }

        Set<String> esperadasBytecode = new TreeSet<>(INBOUND_TEXTUAL_ESPERADAS);
        esperadasBytecode.remove(CATCH_ONLY_CORRECAO);
        esperadasBytecode.remove(CATCH_ONLY_RASPAGEM);

        Set<String> inesperadas = new TreeSet<>(reais);
        inesperadas.removeAll(esperadasBytecode);
        Set<String> ausentes = new TreeSet<>(esperadasBytecode);
        ausentes.removeAll(reais);

        assertTrue(inesperadas.isEmpty() && ausentes.isEmpty(),
            () -> "Divergencia INBOUND bytecode. Esperado=" + esperadasBytecode.size()
                + " Real=" + reais.size() + " | INESPERADAS=" + inesperadas + " | AUSENTES=" + ausentes);
    }

    @Test
    @DisplayName("Inventario textual complementar (imports do fonte): outras-fatias -> traducao == 85 (inclui catch-only)")
    void inboundTextualBateComInventario() {
        Set<String> reais = coletarImportsInboundDoFonte();
        Set<String> inesperadas = new TreeSet<>(reais);
        inesperadas.removeAll(INBOUND_TEXTUAL_ESPERADAS);
        Set<String> ausentes = new TreeSet<>(INBOUND_TEXTUAL_ESPERADAS);
        ausentes.removeAll(reais);
        assertTrue(inesperadas.isEmpty() && ausentes.isEmpty(),
            () -> "Divergencia inventario textual INBOUND. Esperado=" + INBOUND_TEXTUAL_ESPERADAS.size()
                + " Real=" + reais.size() + " | INESPERADOS=" + inesperadas + " | AUSENTES=" + ausentes);
    }

    private static Set<String> coletarImportsInboundDoFonte() {
        Path raizFontes = localizarRaizFontes();
        Set<String> arestas = new TreeSet<>();
        try (Stream<Path> arquivos = Files.walk(raizFontes)) {
            arquivos.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                String origem = fqnDeArquivo(raizFontes, p);
                if (origem.equals(PKG_TRADUCAO) || origem.startsWith(PKG_TRADUCAO + ".")) {
                    return; // origem dentro de traducao nao e inbound
                }
                try {
                    for (String linha : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                        String s = linha.strip();
                        if (!s.startsWith("import ")) {
                            continue;
                        }
                        String corpo = s.substring("import ".length()).strip();
                        if (corpo.startsWith("static ")) {
                            continue;
                        }
                        if (corpo.endsWith(";")) {
                            corpo = corpo.substring(0, corpo.length() - 1).strip();
                        }
                        if (corpo.startsWith(ALVO_INBOUND)) {
                            arestas.add(aresta(origem, corpo));
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return arestas;
    }

    private static Path localizarRaizFontes() {
        Path atual = Path.of("").toAbsolutePath();
        for (int i = 0; i < 6 && atual != null; i++) {
            Path candidato = atual.resolve("src").resolve("main").resolve("java");
            if (Files.isDirectory(candidato)) {
                return candidato;
            }
            atual = atual.getParent();
        }
        throw new IllegalStateException(
            "Raiz de fontes src/main/java nao encontrada a partir de " + Path.of("").toAbsolutePath());
    }

    private static String fqnDeArquivo(Path raizFontes, Path arquivo) {
        String rel = raizFontes.relativize(arquivo).toString();
        rel = rel.replace(java.io.File.separatorChar, '.');
        rel = rel.replace('/', '.');
        if (rel.endsWith(".java")) {
            rel = rel.substring(0, rel.length() - ".java".length());
        }
        return rel;
    }

    private static String aresta(String origemFqn, String destinoFqn) {
        return origemFqn + " -> " + destinoFqn;
    }

    private static boolean ehDaTraducao(JavaClass classe) {
        String pkg = classe.getPackageName();
        return pkg.equals(PKG_TRADUCAO) || pkg.startsWith(PKG_TRADUCAO + ".");
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
