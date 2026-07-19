package org.traducao.projeto.contexto.lore.macross;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: mapa canônico compartilhado da franquia Macross — formas-ruim
 * em PT que o LLM tende a inventar e que devem voltar à grafia oficial.
 *
 * <p>INVARIANTES DO DOMÍNIO: chave = forma-ruim PT; valor = canônico; só restaura se o
 * original EN contém o canônico (protege palavras comuns sem o termo na origem).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
 */
public final class CorrecoesTerminologiaMacross {

    private CorrecoesTerminologiaMacross() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: correções transversais Macross (Valkyrie / Zentradi / anti-Robotech).
     *
     * <p>INVARIANTES DO DOMÍNIO: proíbe léxico Robotech (Veritech) e localização de Valkyrie.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sempre o mesmo mapa imutável.
     */
    public static Map<String, String> mapa() {
        return Map.of(
            "Valquíria", "Valkyrie",
            "Valquiria", "Valkyrie",
            "Veritech", "Valkyrie",
            "Zentraedi", "Zentradi",
            "Zentradii", "Zentradi"
        );
    }
}
