package org.traducao.projeto.cachetraducao.domain;

/**
 * PROPÓSITO DE NEGÓCIO: modelo persistido de uma linha do cache de tradução — o
 * texto original e sua tradução, com o índice, o estilo e o par de idiomas a que
 * pertencem. É a unidade que o cache de tradução grava e relê para evitar retraduzir
 * a mesma fala. NÃO é um modelo genérico de legenda.
 *
 * <p>INVARIANTES DO DOMÍNIO: {@code record} imutável; a ordem e os nomes dos
 * componentes ({@code indice, estilo, original, traduzido, idiomaOriginal,
 * idiomaTraduzido}) compõem o schema JSON persistido e não podem mudar sem quebrar a
 * compatibilidade dos arquivos {@code .cache.json} existentes.
 *
 * <p>COMPORTAMENTO EM CASO DE LIMITE: não há validação — qualquer valor, inclusive
 * {@code null} nos campos de texto, é aceito; a coerência é responsabilidade de quem
 * grava/lê o cache.
 *
 * @param indice posição ordinal da fala na legenda
 * @param estilo nome do estilo ASS da fala
 * @param original texto original (idioma de origem)
 * @param traduzido texto traduzido (idioma de destino)
 * @param idiomaOriginal código do idioma de origem
 * @param idiomaTraduzido código do idioma de destino
 */
public record EntradaCache(int indice, String estilo, String original, String traduzido, String idiomaOriginal, String idiomaTraduzido) {
}
