package org.traducao.projeto.traducaoKaraoke.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.traducao.projeto.cachetraducao.domain.EntradaCache;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.cachetraducao.infrastructure.CacheTraducaoService;
import org.traducao.projeto.core.presentation.web.LogStreamService;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PROPOSITO DE NEGOCIO: dono do cache JSON por arquivo desta fatia — onde ele mora, o que se
 * reaproveita dele e o que se grava de volta. O cache e EDITAVEL a mao de proposito: o operador
 * corrige uma linha no JSON e a reexecucao respeita a correcao.
 *
 * <h2>As duas invariantes que vieram junto</h2>
 * <ul>
 *   <li><b>Proveniencia divergente descarta TUDO.</b> Contexto ou modelo diferentes significam
 *       que aquelas traducoes nasceram sob outra lore — reaproveita-las e propagar o erro. O
 *       cache antigo e PRESERVADO em arquivo, nunca apagado, e o sinal sobe para o manifesto:
 *       descartar cache multiplica tempo e chamadas ao LLM, e sem aviso o pico aparece no
 *       historico sem explicacao.</li>
 *   <li><b>Grava-se so o que foi APLICADO.</b> E por isso que a limpeza do cache e automatica:
 *       o que o criterio novo tornou inalcancavel simplesmente nao volta para o arquivo. Em
 *       2026-08-19 isso passou a ser CONTADO ({@code entradasCacheDescartadas}), porque cache
 *       que encolhe sem numero e indistinguivel de perda de dado.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Dry-run nao le nem escreve cache. Mapa vazio de traducoes nao grava arquivo. Nunca lanca.
 */
@ApplicationScoped
public class CacheDoArquivo {

    static final String CANAL_LOG = TraduzirKaraokeUseCase.CANAL_LOG;
    private static final String SUBPASTA = "karaoke";

    @Inject
    CacheTraducaoService cacheService;

    @Inject
    LogStreamService logStream;

    @ConfigProperty(name = "tradutor.idioma-original")
    Optional<String> idiomaOriginal;

    @ConfigProperty(name = "tradutor.idioma-traduzido")
    Optional<String> idiomaTraduzido;

    // E3b/Opcao A: ausencia e vazio colapsam em "cache"; branco de idioma cai no default.
    @ConfigProperty(name = "tradutor.diretorio-cache")
    Optional<String> diretorioCache;

    public Path arquivoDe(Path arquivo) {
        String nome = arquivo.getFileName().toString();
        String base = nome.replaceFirst("(?i)\\.(ass|ssa)$", "");
        String dirCache = diretorioCache.orElse("cache");
        return Path.of(dirCache, SUBPASTA, base + ".cache.json");
    }

    public Map<String, String> carregar(
        Path arquivoCache, boolean gravar, ProvenienciaCache proveniencia,
        java.util.concurrent.atomic.AtomicBoolean cacheIgnorado
    ) {
        if (!gravar) {
            return Map.of();
        }
        CacheTraducaoService.ResultadoCarga carga = cacheService.carregar(arquivoCache, proveniencia);
        if (carga.migrado()) {
            cacheService.arquivarGeracaoSemProveniencia(arquivoCache);
            // Sobe para o manifesto: descartar o cache multiplica tempo e chamadas ao LLM, e sem
            // esse sinal o pico aparece no histórico sem explicação nenhuma.
            cacheIgnorado.set(true);
            logStream.publicarLog(CANAL_LOG,
                "   [CACHE IGNORADO] cache antigo sem contexto/lore foi preservado; linhas serão retraduzidas e carimbadas.");
            return Map.of();
        }
        return carga.mapa();
    }

    /**
     * Persiste TODAS as traduções aplicadas (novas e reaproveitadas) no cache
     * do arquivo, preservando o fluxo de correção manual: o usuário edita o
     * JSON e a reexecução respeita a edição.
     */
    public void salvar(Path arquivoCache, DocumentoLegenda documento, Map<String, String> traducoes,
                             ProvenienciaCache proveniencia) {
        if (traducoes.isEmpty()) {
            return;
        }
        Map<String, EntradaCache> porOriginal = new LinkedHashMap<>();
        for (EventoLegenda evento : documento.eventos()) {
            String traduzido = evento.temTexto() ? traducoes.get(evento.texto()) : null;
            if (traduzido != null) {
                porOriginal.putIfAbsent(evento.texto(), new EntradaCache(
                    evento.indice(), evento.estilo(), evento.texto(), traduzido,
                    idiomaOriginal.filter(s -> !s.isBlank()).orElse("en"),
                    idiomaTraduzido.filter(s -> !s.isBlank()).orElse("pt-br")));
            }
        }
        cacheService.salvar(arquivoCache, proveniencia, new ArrayList<>(porOriginal.values()));
    }
}
