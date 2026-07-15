package org.traducao.projeto.mapaProjeto.presentation.web;

/**
 * PROPÓSITO DE NEGÓCIO: entrega ao painel "Mapa do Projeto" o relatório em
 * markdown, a árvore no formato GitHub e o nome do projeto gerados pelo módulo
 * de mapeamento.
 *
 * <p>INVARIANTES DO DOMÍNIO: os nomes dos campos são contrato JSON público
 * consumido pela SPA; representam o resultado já pronto para renderização.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sendo um record imutável, não há falha de
 * construção; a ausência de conteúdo é responsabilidade do use case que o produz.
 */
public record MapaResponse(String conteudo, String arvoreGithub, String nomeProjeto) {}
