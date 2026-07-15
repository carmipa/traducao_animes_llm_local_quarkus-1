package org.traducao.projeto.traducao.domain.ports;

import java.util.Set;

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
