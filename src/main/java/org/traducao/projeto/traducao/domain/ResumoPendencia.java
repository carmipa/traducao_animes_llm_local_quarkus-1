package org.traducao.projeto.traducao.domain;

/**
 * PROPÓSITO DE NEGÓCIO: linha agregada do KPI de pendência gravada por episódio na
 * telemetria da Tradução Local — quantas falas ficaram pendentes por cada combinação
 * de {@link CategoriaConteudo} e {@link CausaRaizPendencia}. É a métrica estruturada
 * que substitui a contagem por texto livre em {@code errosOcorridos}, permitindo
 * comparar rodadas sem re-interpretar strings.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code quantidade} conta FALAS pendentes distintas, já com a precedência de
 *       causa aplicada (1 fala = 1 causa-raiz); nunca soma o mesmo evento duas vezes.</li>
 *   <li>{@code categoria} e {@code causaRaiz} são os nomes dos enums correspondentes,
 *       compondo o schema JSON persistido (schemaVersion 1.1).</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * {@code record} imutável, sem validação; a coerência é responsabilidade de quem monta.
 *
 * @param categoria balde de conteúdo ({@link CategoriaConteudo})
 * @param causaRaiz causa-raiz da pendência ({@link CausaRaizPendencia})
 * @param quantidade número de falas pendentes distintas nessa combinação
 */
public record ResumoPendencia(String categoria, String causaRaiz, int quantidade) {
}
