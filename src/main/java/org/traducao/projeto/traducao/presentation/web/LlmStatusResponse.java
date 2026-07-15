package org.traducao.projeto.traducao.presentation.web;

/**
 * PROPÓSITO DE NEGÓCIO: informa ao card do painel inicial o estado ao vivo do
 * servidor LLM local (online, modelo carregado, nome do modelo e mensagem).
 *
 * <p>INVARIANTES DO DOMÍNIO: os nomes dos campos são contrato JSON público
 * consumido pela SPA; o nome do modelo só é preenchido quando há modelo em
 * memória.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: quando a consulta ao servidor falha, o
 * endpoint constrói uma instância com {@code online=false} e a mensagem do erro.
 */
public record LlmStatusResponse(boolean online, boolean modeloCarregado, String modelo, String mensagem) {}
