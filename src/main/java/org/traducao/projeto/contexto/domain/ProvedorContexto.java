package org.traducao.projeto.contexto.domain;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: contrato de um provedor de contexto/lore de tradução — cada
 * obra (Gundam, Macross, Danmachi...) implementa esta interface para fornecer o
 * prompt de sistema do LLM, o rótulo de UI e os termos que não devem ser traduzidos.
 * É o ponto de extensão do módulo compartilhado {@code contexto}: novas obras entram
 * apenas adicionando implementações {@code @Component}, sem tocar em quem consome.
 *
 * <p>INVARIANTES DO DOMÍNIO: interface pura (só depende do JDK); {@link #getId()} é o
 * identificador único e estável usado para seleção e para carimbar a proveniência do
 * cache; {@link #obterPromptSistema()} devolve o prompt completo já montado; termos
 * protegidos são um conjunto imutável (por padrão vazio). Nenhum método realiza I/O.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: os métodos não lançam por contrato; um provedor
 * mal formado (id nulo/duplicado) é rejeitado por quem agrega os provedores
 * ({@code GerenciadorContexto}), não por esta interface.
 */
public interface ProvedorContexto {
    /**
     * Retorna o ID único para seleção via UI.
     */
    String getId();

    /**
     * Retorna o nome amigável para exibição no combo box da UI.
     */
    String getNomeExibicao();

    /**
     * Retorna o prompt de sistema completo para o LLM, com regras gerais e lore especifico da midia.
     */
    String obterPromptSistema();

    /**
     * Termos desta obra que NÃO devem ser traduzidos (nomes próprios, facções,
     * patentes, lugares, mecha). Por padrão vazio; cada contexto pode
     * sobrescrever para que o detector de "tradução idêntica" proteja o lore
     * selecionado, em vez de depender só da lista global fixa no detector.
     */
    default Set<String> termosProtegidos() {
        return Set.of();
    }
}
