package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoMacrossFrontierFilme2 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross Frontier O Filme: The Wings of Farewell (Sayonara no Tsubasa).
        - Personagens: Alto Saotome (homem), Sheryl Nome (mulher), Ranka Lee (mulher), Michael Blanc (homem), Luca Angeloni (homem), Klan Klang (mulher), Ozma Lee (homem), Brera Sterne (homem), Grace O'Connor (mulher).
        - Naves / Mechas: YF-29 Durandal, VF-25 Messiah, VF-27 Lucifer, Vajra Queen.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross Frontier: The Wings of Farewell", LORE);

    @Override public String getId() { return "macross_frontier_filme2"; }
    @Override public String getNomeExibicao() { return "Macross Frontier: The Wings of Farewell"; }
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
