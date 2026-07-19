package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Macross 7: The Galaxy's Calling Me!.
 *
 * <p>INVARIANTES DO DOMÍNIO: Valkyrie/Zentradi oficiais; sem Veritech/Valquíria.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreMacross7Filme implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Macross 7: The Galaxy's Calling Me!.
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Macross, The Galaxy, Calling, Ginga, Ore, Yonde Iru, Basara Nekki, Mylene Flare Jenius, Pedro, Emilia Jenius, Mylene, Naughty, Banda, Fire Bomber.
        - Personagens: Basara Nekki (homem), Mylene Flare Jenius (mulher), Pedro (homem), Emilia Jenius (mulher / irma de Mylene), Naughty (homem).
        - Banda: Fire Bomber.
        - Alertas: Valkyrie nao vira Valquiria/Valquíria; Zentradi grafia oficial; proibido Veritech;
          GERWALK/Battroid/Fighter Mode — nao traduzir nomes dos modos.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "macross_7_filme"; }
    @Override public String getNomeExibicao() { return "Macross 7: The Galaxy's Calling Me! - Revisao de Lore"; }
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
        return CorrecoesTerminologiaMacrossRevisao.mapa();
    }
}
