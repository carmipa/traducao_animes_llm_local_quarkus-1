package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoMacrossFrontierFilme1 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross Frontier O Filme: The False Songstress (Itsuwari no Utafihme).
        - Personagens: Alto Saotome (homem), Sheryl Nome (mulher), Ranka Lee (mulher), Michael Blanc (homem), Luca Angeloni (homem), Klan Klang (mulher), Ozma Lee (homem), Brera Sterne (homem).
        - Naves / Mechas: VF-25 Messiah, VF-27 Lucifer, Vajra.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross Frontier: The False Songstress", LORE);

    @Override public String getId() { return "macross_frontier_filme1"; }
    @Override public String getNomeExibicao() { return "Macross Frontier: The False Songstress"; }
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
