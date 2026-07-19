package org.traducao.projeto.traducao.presentation.web;

/**
 * PROPÓSITO DE NEGÓCIO: representa um contexto de tradução (obra/anime)
 * disponível para seleção na interface, com id técnico, nome de exibição, a
 * franquia ({@code grupo}) para agrupar o select em {@code <optgroup>} e a
 * marcação de qual é o padrão.
 *
 * <p>INVARIANTES DO DOMÍNIO: os nomes dos campos são contrato JSON público
 * consumido pela SPA; {@code grupo} vazio significa título solto (opção fora de
 * qualquer {@code <optgroup>}); {@code padrao} identifica de forma exclusiva o
 * contexto pré-selecionado.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sendo um record imutável, não há falha de
 * construção; a lista vazia é responsabilidade do gerenciador de contexto.
 */
public record ContextoResponse(String id, String nome, String grupo, boolean padrao) {}
