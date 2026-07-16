package org.traducao.projeto.cachetraducao.domain;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: Formato persistido do cache de tradução versionado — um
 * cabeçalho de {@link ProvenienciaCache} seguido das entradas. Substitui a lista
 * pura de {@link EntradaCache} para que cada arquivo de cache carregue a origem
 * (lore/modelo) que o gerou.
 *
 * <p>INVARIANTES DO DOMÍNIO: {@code proveniencia} descreve TODAS as entradas do
 * documento (um arquivo de cache = uma geração/proveniência). Entradas de
 * proveniências diferentes nunca convivem no mesmo documento.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: record imutável; a leitura de um JSON que
 * não seja este objeto (ex.: lista pura do formato antigo) falha na
 * desserialização e é tratada pelo {@code CacheTraducaoService} como formato
 * legado ou corrompido.
 *
 * @param proveniencia origem (lore/hash/modelo/idiomas) que gerou TODAS as entradas
 * @param entradas linhas do cache de tradução deste documento
 */
public record CacheDocumento(ProvenienciaCache proveniencia, List<EntradaCache> entradas) {
}
