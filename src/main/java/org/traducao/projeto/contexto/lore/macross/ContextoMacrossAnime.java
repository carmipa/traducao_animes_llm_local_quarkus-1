package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoMacrossAnime implements ProvedorContexto {

    private static final String LORE = """
        - Obra: The Super Dimension Fortress Macross, serie TV classica.
        - Premissa: a nave SDF-1 Macross e a humanidade entram em guerra contra os gigantes Zentradi, enquanto cultura, musica e sentimentos humanos mudam o conflito.
        - Termos centrais: SDF-1 Macross, Zentradi, Meltrandi/Meltran quando aplicavel, Protoculture/Protocultura, Valkyrie, Variable Fighter, Destroid, Space War I.
        - Principais nomes: Hikaru Ichijyo, Lynn Minmay, Misa Hayase, Roy Focker, Maximilian Jenius/Max Jenius, Milia Fallyna, Bruno J. Global, Claudia LaSalle, Exsedol Folmo, Boddole Zer, Kamjin Kravshera.
        - Mechas/naves: VF-1 Valkyrie, VF-1S, VF-1J, VF-1D, Monster, Tomahawk, Defender, Queadluun-Rau, Regult, SDF-1.
        - Use "Valkyrie" e "Variable Fighter" como termos de franquia; "Protocultura" pode ser usado em portugues.
        - Tom: opera espacial, romance triangulado, cultura pop/idol e guerra; Minmay fala jovem e emotiva, Misa mais profissional, Hikaru amadurece de impulsivo para responsavel.
        - Letras de musica devem soar cantaveis quando forem claramente cancoes, sem notas explicativas.
        """;

    private static final String PROMPT = ContextoPrompt.montar("The Super Dimension Fortress Macross", LORE);

    @Override
    public String getId() { return "macross_anime"; }
    @Override
    public String getNomeExibicao() { return "The Super Dimension Fortress Macross"; }
    @Override
    public String obterPromptSistema() { return PROMPT; }
}
