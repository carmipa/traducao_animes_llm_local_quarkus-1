package org.traducao.projeto.traducao.contexto.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

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
}
