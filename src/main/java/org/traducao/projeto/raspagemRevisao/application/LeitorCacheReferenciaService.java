package org.traducao.projeto.raspagemRevisao.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.traducao.projeto.cachetraducao.infrastructure.CacheManutencaoService;
import org.traducao.projeto.cachetraducao.domain.EntradaCache;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: entrega à Revisão de Legendas as referências EN/PT do
 * cache produzido pela Tradução Local e atualizado pela Correção de Cache.
 *
 * <p>INVARIANTES DO DOMÍNIO: aceita o formato legado e o envelope versionado;
 * a leitura é somente consulta e não remove proveniência nem campos futuros.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: arquivo inexistente devolve lista vazia;
 * JSON inválido ou entrada incompatível lança {@link IOException} ao chamador.
 */
@Service
public class LeitorCacheReferenciaService {

    /**
     * PROPÓSITO DE NEGÓCIO: transporta juntas as falas e a identidade da lore
     * que produziu o cache usado pela Opção 6.
     * <p>INVARIANTES DO DOMÍNIO: entradas são imutáveis e proveniência pode ser
     * nula somente para cache legado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: record não executa I/O nem normalização.
     */
    public record DocumentoReferencia(List<EntradaCache> entradas, ProvenienciaCache proveniencia) {}

    private final CacheManutencaoService cacheService;
    private final ObjectMapper mapper;

    /**
     * PROPÓSITO DE NEGÓCIO: conecta o leitor da revisão à porta canônica de
     * compatibilidade de cache do projeto.
     * <p>INVARIANTES DO DOMÍNIO: o mesmo mapper interpreta cada entrada aberta.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede a construção.
     */
    public LeitorCacheReferenciaService(CacheManutencaoService cacheService, ObjectMapper mapper) {
        this.cacheService = cacheService;
        this.mapper = mapper;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: converte as entradas persistidas no modelo usado
     * para parear original, tradução e índice durante a revisão.
     *
     * <p>INVARIANTES DO DOMÍNIO: nós não-objeto são ignorados; a ordem do cache
     * é preservada; nenhuma alteração é escrita em disco.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga {@link IOException} para cache
     * corrompido e devolve lista vazia quando o arquivo não existe.
     */
    public List<EntradaCache> carregar(Path arquivo) throws IOException {
        return carregarDocumento(arquivo).entradas();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: lê entradas e proveniência em uma única operação para
     * que a revisão ative automaticamente a lore correta de cada obra.
     *
     * <p>INVARIANTES DO DOMÍNIO: cache versionado preserva seu contexto; cache
     * legado devolve proveniência nula e depende de fallback explícito.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: arquivo ausente produz documento vazio;
     * JSON corrompido propaga {@link IOException} sem alterar o cache.
     */
    public DocumentoReferencia carregarDocumento(Path arquivo) throws IOException {
        if (!Files.isRegularFile(arquivo)) return new DocumentoReferencia(List.of(), null);
        CacheManutencaoService.DocumentoEditavel documento = cacheService.carregar(arquivo);
        List<EntradaCache> entradas = new ArrayList<>();
        for (JsonNode no : documento.entradas()) {
            if (no.isObject()) entradas.add(mapper.treeToValue(no, EntradaCache.class));
        }
        return new DocumentoReferencia(List.copyOf(entradas), documento.proveniencia());
    }
}
