package org.traducao.projeto.lore.macross;

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
        // As seis últimas vieram do catálogo ESPELHO da revisão de lore, onde já existiam. A
        // duplicação entre os dois catálogos é deliberada (fatia não importa fatia), mas ela
        // derivou: a tradução tinha cinco entradas e a revisão onze, então Macross era traduzido
        // sem "Protocultura"→"Protoculture" e sem as variantes de Meltrandi/Zentradi que a revisão
        // já corrigia depois. Ver ParidadeMapasTerminologiaTest.
        return Map.ofEntries(
            Map.entry("Valquíria", "Valkyrie"),
            Map.entry("Valquiria", "Valkyrie"),
            Map.entry("Veritech", "Valkyrie"),
            Map.entry("Zentraedi", "Zentradi"),
            Map.entry("Zentradii", "Zentradi"),
            Map.entry("Protocultura", "Protoculture"),
            Map.entry("Ataque Minmay", "Minmay Attack"),
            Map.entry("Ataque de Minmay", "Minmay Attack"),
            Map.entry("Meltrandy", "Meltrandi"),
            Map.entry("Meltrandii", "Meltrandi"),
            Map.entry("Zentrades", "Zentradi")
        );
    }
}
