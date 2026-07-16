package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoMacrossFilme2 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: continuidade/filme da fase classica de Macross, incluindo material ligado a Do You Remember Love? e Flash Back 2012 quando aparecer.
        - Preserve a terminologia da franquia classica: SDF-1 Macross, Zentradi, Meltrandi, Protocultura, Valkyrie, Variable Fighter.
        - Principais nomes recorrentes: Hikaru Ichijyo, Lynn Minmay, Misa Hayase, Roy Focker, Max Jenius, Milia Fallyna.
        - Mechas/naves: VF-1 Valkyrie, SDF-1, Queadluun-Rau, Regult.
        - Se houver letras de Lynn Minmay, traduza como letra de musica em portugues natural, mantendo romantismo e simplicidade pop.
        - Tom: memoria, despedida, romance e cultura como forca transformadora; evite linguagem militar dura demais em cenas musicais ou emocionais.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross classico - filme/continuidade", LORE);

    @Override
    public String getId() { return "macross_filme2"; }
    @Override
    public String getNomeExibicao() { return "Macross Flash Back 2012"; }
    @Override
    public String obterPromptSistema() { return PROMPT; }
}
