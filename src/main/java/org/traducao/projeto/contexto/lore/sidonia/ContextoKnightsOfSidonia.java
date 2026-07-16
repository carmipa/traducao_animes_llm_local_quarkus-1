package org.traducao.projeto.contexto.lore.sidonia;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoKnightsOfSidonia implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Knights of Sidonia / Sidonia no Kishi.
        - A humanidade vive na nave-semente Sidonia apos a destruicao da Terra pelos Gauna.
        - Termos centrais: Sidonia, Gauna, Gardes, Kabizashi, Ena, Heigus particles, placenta, gauna core.
        - Use "Guarda" apenas se o contexto for funcao comum; para mechas/pilotos, mantenha Garde/Gardes como termo da obra.
        - "Ena" e o orgao/membrana bioenergetica que cobre certos humanos modificados, permite fotossintese e potencializa o uso de particulas Heigus; nunca traduza como simples "pele" ou "aura".
        - Organizacoes e lugares: Toha Heavy Industries, Immortal Ship Committee, Residential Layer, Photosynthesis Chamber, hangar de Gardes.
        - Principais nomes: Nagate Tanikaze, Shizuka Hoshijiro, Izana Shinatose, Tsumugi Shiraui, Captain Kobayashi, Yuhata Midorikawa, Norio Kunato, Lala Hiyama.
        - Conceitos biologicos/sociais: fotossintese humana, clones, terceiro genero/intersexo de Izana, imortalidade de certos lideres, hibridizacao Gauna.
        - Tom: ficcao cientifica militar e existencial; traduza termos tecnicos de forma clara, mantendo estranhamento biologico e tensao de sobrevivencia.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Knights of Sidonia", LORE);

    @Override
    public String getId() { return "sidonia"; }
    @Override
    public String getNomeExibicao() { return "Knights of Sidonia"; }
    @Override
    public String obterPromptSistema() { return PROMPT; }
}
