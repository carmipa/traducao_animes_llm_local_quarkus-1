package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoMacross2 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross II: Lovers Again.
        - Ambientacao: futuro alternativo/pos-Space War, com a humanidade usando a cultura Minmay Attack contra invasores e enfrentando os Marduk.
        - Termos centrais: Marduk, Emulator, Minmay Attack, Valkyrie, Variable Fighter, Macross Cannon, UN Spacy/UN Forces.
        - Principais nomes: Hibiki Kanzaki, Ishtar, Silvie Gena, Nexx Gilbert, Lord Feff, Ingues, Mash.
        - Mechas/naves: VF-2SS Valkyrie II, Metal Siren, Gigamesh, Macross Cannon.
        - Mantenha Valkyrie, Emulator e Marduk; traduza "songstress" como cantora quando for funcao comum, mas preserve a nocao de Emulator como cargo cultural/militar Marduk.
        - Tom: romance de idol/repórter em guerra espacial; Hibiki e informal e curioso, Silvie e militar direta, Ishtar mistura inocencia e reverencia musical.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross II: Lovers Again", LORE);

    @Override
    public String getId() { return "macross_2"; }
    @Override
    public String getNomeExibicao() { return "Macross II: Lovers Again"; }
    @Override
    public String obterPromptSistema() { return PROMPT; }
}
