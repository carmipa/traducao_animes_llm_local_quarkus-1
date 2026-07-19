package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoMacrossZero implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross Zero (OVA).
        - Personagens: Shin Kudo (homem), Sara Nome (mulher), Mao Nome (mulher / avó de Sheryl Nome), Roy Focker (homem), Edgar LaSalle (homem), D.D. Ivanov (homem), Nora Polyansky (mulher).
        - Mechas / Termos: VF-0 Phoenix, SV-51, Ilha Mayan, Bird Human (Homem Pássaro), Protocultura.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross Zero", LORE);

    @Override public String getId() { return "macross_zero"; }
    @Override public String getNomeExibicao() { return "Macross Zero"; }
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
