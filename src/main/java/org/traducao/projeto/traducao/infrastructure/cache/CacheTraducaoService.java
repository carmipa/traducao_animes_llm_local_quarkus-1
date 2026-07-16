package org.traducao.projeto.traducao.infrastructure.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.core.util.ArquivoAtomicoUtil;
import org.traducao.projeto.legenda.domain.ArquivoLegendaException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persiste, por arquivo de legenda, o par (texto original em ingles -> texto
 * traduzido) em JSON. Serve a dois propositos: (1) permitir que o usuario
 * revise/corrija falhas de traducao manualmente editando o JSON e (2) evitar
 * chamar o LLM de novo para falas ja traduzidas em uma execucao anterior -
 * uma correcao manual no cache e respeitada na proxima execucao.
 *
 * <p>Formato versionado ({@link CacheDocumento}): o cache carrega a
 * {@link ProvenienciaCache} (lore/hash/modelo/idiomas) que o gerou. Ao carregar,
 * uma proveniencia divergente NAO e reutilizada — a geracao anterior e arquivada
 * e o episodio e retraduzido com o lore atual. Um JSON ilegivel e preservado
 * (renomeado {@code .corrompido_<ts>.json}) em vez de ignorado e sobrescrito.
 */
@Component
public class CacheTraducaoService {

    private static final Logger log = LoggerFactory.getLogger(CacheTraducaoService.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ObjectMapper objectMapper;

    public CacheTraducaoService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Resultado da carga versionada: o mapa reaproveitavel (original -> traduzido),
     * quantas entradas foram invalidadas por mudanca de proveniencia e se o
     * arquivo veio do formato antigo (lista pura) e sera migrado ao salvar.
     */
    public record ResultadoCarga(Map<String, String> mapa, int invalidadas, boolean migrado) {
        public static ResultadoCarga vazio() {
            return new ResultadoCarga(new HashMap<>(), 0, false);
        }
    }

    /**
     * PROPOSITO DE NEGOCIO: Carrega o cache reaproveitavel apenas quando a
     * proveniencia atual (lore/modelo/idiomas) bate com a que gerou o arquivo.
     *
     * <p>INVARIANTES DO DOMINIO: proveniencia divergente nunca e reutilizada
     * (arquiva a geracao anterior e devolve mapa vazio + contagem invalidada);
     * formato antigo sem cabecalho e assumido compativel nesta migracao e
     * versionado a partir do proximo salvamento; arquivo ilegivel e preservado,
     * nunca lido como vazio-e-sobrescrito.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lanca; devolve
     * {@link ResultadoCarga#vazio()} (mapa vazio) e, quando aplicavel, move o
     * arquivo problematico para o lado antes de retornar.
     */
    public ResultadoCarga carregar(Path arquivoCache, ProvenienciaCache provenienciaAtual) {
        if (!Files.exists(arquivoCache)) {
            return ResultadoCarga.vazio();
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(arquivoCache.toFile());
        } catch (IOException e) {
            log.error("Cache ilegivel/corrompido em {}: {}", arquivoCache, e.getMessage());
            preservarCorrompido(arquivoCache);
            return ResultadoCarga.vazio();
        }

        if (root == null || root.isNull()) {
            preservarCorrompido(arquivoCache);
            return ResultadoCarga.vazio();
        }

        // Formato antigo: lista pura de entradas, sem cabecalho de proveniencia.
        if (root.isArray()) {
            try {
                List<EntradaCache> entradas = objectMapper.convertValue(root,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, EntradaCache.class));
                log.warn("Cache sem proveniencia em {} — assumindo compativel nesta migracao; sera versionado a partir de agora.",
                    arquivoCache);
                return new ResultadoCarga(montarMapa(entradas), 0, true);
            } catch (IllegalArgumentException e) {
                preservarCorrompido(arquivoCache);
                return ResultadoCarga.vazio();
            }
        }

        // Formato novo: objeto com proveniencia + entradas.
        if (root.isObject()) {
            CacheDocumento doc;
            try {
                doc = objectMapper.treeToValue(root, CacheDocumento.class);
            } catch (Exception e) {
                preservarCorrompido(arquivoCache);
                return ResultadoCarga.vazio();
            }
            List<EntradaCache> entradas = doc.entradas() != null ? doc.entradas() : List.of();
            if (!provenienciaAtual.mesmaProveniencia(doc.proveniencia())) {
                arquivarGeracao(arquivoCache, doc.proveniencia());
                log.warn("Proveniencia do cache mudou (lore/modelo) em {} — {} entrada(s) arquivada(s) e NAO reutilizada(s).",
                    arquivoCache, entradas.size());
                return new ResultadoCarga(new HashMap<>(), entradas.size(), false);
            }
            Map<String, String> mapa = montarMapa(entradas);
            log.info("Cache carregado de {} ({} entradas reaproveitaveis, contexto {}, modelo {})",
                arquivoCache, mapa.size(),
                doc.proveniencia() != null ? doc.proveniencia().contextoId() : "?",
                doc.proveniencia() != null ? doc.proveniencia().modeloLlm() : "?");
            return new ResultadoCarga(mapa, 0, false);
        }

        // Escalar/booleano/etc. — nao e um cache valido.
        preservarCorrompido(arquivoCache);
        return ResultadoCarga.vazio();
    }

    public void salvar(Path arquivoCache, ProvenienciaCache proveniencia, List<EntradaCache> entradas) {
        try {
            Path pasta = arquivoCache.toAbsolutePath().getParent();
            if (pasta != null) {
                Files.createDirectories(pasta);
            }
            // Mesmo padrão do EscritorLegendaAss: escreve num temporário e só
            // substitui o destino com move atômico. Uma queda no meio da
            // escrita não pode corromper o cache — ele guarda horas de
            // tradução via LLM.
            CacheDocumento documento = new CacheDocumento(proveniencia, entradas);
            Path temp = Files.createTempFile(pasta, arquivoCache.getFileName().toString(), ".tmp");
            try {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), documento);
                ArquivoAtomicoUtil.substituirAtomico(temp, arquivoCache);
            } finally {
                Files.deleteIfExists(temp);
            }
            log.info("Cache de traducao salvo em {} ({} entradas, contexto {}, modelo {})",
                arquivoCache, entradas.size(),
                proveniencia != null ? proveniencia.contextoId() : "?",
                proveniencia != null ? proveniencia.modeloLlm() : "?");
        } catch (IOException e) {
            throw new ArquivoLegendaException("Falha ao salvar cache de traducao: " + arquivoCache, e);
        }
    }

    // --- Formato antigo (lista pura), mantido para os fluxos que ainda nao
    // --- versionam o cache (ex.: traducao de karaoke). ---

    public Map<String, String> carregar(Path arquivoCache) {
        if (!Files.exists(arquivoCache)) {
            return new HashMap<>();
        }
        try {
            List<EntradaCache> entradas = objectMapper.readValue(arquivoCache.toFile(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, EntradaCache.class));
            Map<String, String> mapa = montarMapa(entradas);
            log.info("Cache carregado de {} ({} entradas reaproveitaveis)", arquivoCache, mapa.size());
            return mapa;
        } catch (IOException e) {
            log.warn("Falha ao ler cache existente em {}, ignorando e traduzindo do zero. Causa: {}",
                arquivoCache, e.getMessage());
            return new HashMap<>();
        }
    }

    public void salvar(Path arquivoCache, List<EntradaCache> entradas) {
        try {
            Path pasta = arquivoCache.toAbsolutePath().getParent();
            if (pasta != null) {
                Files.createDirectories(pasta);
            }
            Path temp = Files.createTempFile(pasta, arquivoCache.getFileName().toString(), ".tmp");
            try {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), entradas);
                ArquivoAtomicoUtil.substituirAtomico(temp, arquivoCache);
            } finally {
                Files.deleteIfExists(temp);
            }
            log.info("Cache de traducao salvo em {} ({} entradas)", arquivoCache, entradas.size());
        } catch (IOException e) {
            throw new ArquivoLegendaException("Falha ao salvar cache de traducao: " + arquivoCache, e);
        }
    }

    private Map<String, String> montarMapa(List<EntradaCache> entradas) {
        Map<String, String> mapa = new HashMap<>();
        if (entradas == null) {
            return mapa;
        }
        for (EntradaCache entrada : entradas) {
            if (entrada.traduzido() != null && !entrada.traduzido().isBlank()) {
                mapa.put(entrada.original(), entrada.traduzido());
            }
        }
        return mapa;
    }

    private void preservarCorrompido(Path arquivoCache) {
        try {
            Path destino = arquivoCache.resolveSibling(
                arquivoCache.getFileName().toString() + ".corrompido_" + LocalDateTime.now().format(TS) + ".json");
            Files.move(arquivoCache, destino);
            log.error("Cache corrompido preservado em {} (nao sobrescrito). A traducao recomeca do zero.", destino);
        } catch (IOException e) {
            log.error("Falha ao preservar o cache corrompido {}: {}", arquivoCache, e.getMessage());
        }
    }

    private void arquivarGeracao(Path arquivoCache, ProvenienciaCache anterior) {
        try {
            String tag = anterior != null && anterior.contextoHash() != null && anterior.contextoHash().length() >= 8
                ? anterior.contextoHash().substring(0, 8) : "anterior";
            Path destino = arquivoCache.resolveSibling(
                arquivoCache.getFileName().toString() + ".geracao_" + tag + "_" + LocalDateTime.now().format(TS) + ".json");
            Files.move(arquivoCache, destino);
            log.info("Geracao anterior do cache arquivada em {}.", destino);
        } catch (IOException e) {
            log.warn("Falha ao arquivar a geracao anterior do cache {}: {}", arquivoCache, e.getMessage());
        }
    }
}
