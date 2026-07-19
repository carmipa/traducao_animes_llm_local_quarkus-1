package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoMacrossPlus implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross Plus (OVA / Filme).
        - Personagens: Isamu Alva Dyson (homem), Guld Goa Bowman (homem/meio-zentradi), Myung Fang Lone (mulher), Sharon Apple (IA idol virtual), Raymond Marley (homem), Millard Johnson (homem).
        - Mechas / Naves: YF-19, YF-21, Ghost X-9, Sharon Apple System.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross Plus", LORE);

    @Override public String getId() { return "macross_plus"; }
    @Override public String getNomeExibicao() { return "Macross Plus"; }
    @Override public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: restaura grafias oficiais Macross (Valkyrie/Zentradi) quando
     * o LLM localiza indevidamente — mapa compartilhado da franquia.
     *
     * <p>INVARIANTES DO DOMÍNIO: só aplica com canônico no original EN.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
     */
    @Override
    public java.util.Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaMacross.mapa();
    }

}
