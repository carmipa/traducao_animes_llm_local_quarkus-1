package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Macross Dynamite 7.
 *
 * <p>INVARIANTES DO DOMÍNIO: Valkyrie/Zentradi oficiais; sem Veritech/Valquíria.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreMacrossDynamite7 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Macross Dynamite 7.
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Macross Dynamite, OVA, Basara Nekki, Elma, Graham, Liza Hoyly, Termos, Planeta Zola, Baleias Espaciais, Space Whales, Galactic Whales.
        - Personagens: Basara Nekki (homem), Elma (mulher), Graham (homem), Liza Hoyly (mulher).
        - Termos: Planeta Zola, Baleias Espaciais (Space Whales), Galactic Whales.
        - Alertas: Valkyrie nao vira Valquiria/Valquíria; Zentradi grafia oficial; proibido Veritech;
          GERWALK/Battroid/Fighter Mode — nao traduzir nomes dos modos.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "macross_dynamite_7"; }
    @Override public String getNomeExibicao() { return "Macross Dynamite 7 - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico Macross na Opção 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho da Tradução Local.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaMacrossRevisao.comExtras(Map.ofEntries(
            Map.entry("Protodevilns", "Protodeviln"),
            Map.entry("Protodemonios", "Protodeviln"),
            Map.entry("Protodemônios", "Protodeviln"),
            Map.entry("Energia da Canção", "Song Energy"),
            Map.entry("Energia da Cancao", "Song Energy")
        ));
    }
}
