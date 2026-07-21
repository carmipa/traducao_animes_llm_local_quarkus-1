package org.traducao.projeto.cachetraducao.domain;

/**
 * PROPÓSITO DE NEGÓCIO: modelo persistido de uma linha do cache de tradução — o
 * texto original e sua tradução, com o índice, o estilo, o par de idiomas a que
 * pertencem e, opcionalmente, a ASSINATURA CONTEXTUAL (usada só pela correção de
 * gênero por contexto de cena). É a unidade que o cache de tradução grava e relê para
 * evitar retraduzir a mesma fala. NÃO é um modelo genérico de legenda.
 *
 * <p>INVARIANTES DO DOMÍNIO: {@code record} imutável; a ordem e os nomes dos
 * componentes ({@code indice, estilo, original, traduzido, idiomaOriginal,
 * idiomaTraduzido, assinaturaContexto}) compõem o schema JSON persistido. O campo
 * {@code assinaturaContexto} é ADITIVO e NULÁVEL: entradas do fluxo legado/desligado o
 * gravam como {@code null} e caches antigos (sem o campo) são lidos com ele {@code null}
 * — compatibilidade retroativa preservada, sem bump de schema. O construtor de seis
 * argumentos mantém todos os call-sites existentes (assinatura {@code null}).
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
 * @param assinaturaContexto assinatura contextual só-fonte (null quando a correção por
 *        contexto de cena não gerou esta entrada)
 */
public record EntradaCache(int indice, String estilo, String original, String traduzido,
        String idiomaOriginal, String idiomaTraduzido, String assinaturaContexto) {

    /**
     * PROPÓSITO DE NEGÓCIO: construtor de compatibilidade do fluxo legado/desligado — cria
     * uma entrada SEM assinatura contextual, preservando todos os call-sites de seis
     * argumentos existentes.
     * <p>INVARIANTES DO DOMÍNIO: {@code assinaturaContexto} fica {@code null}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não valida; delega ao construtor canônico.
     */
    public EntradaCache(int indice, String estilo, String original, String traduzido,
            String idiomaOriginal, String idiomaTraduzido) {
        this(indice, estilo, original, traduzido, idiomaOriginal, idiomaTraduzido, null);
    }
}
