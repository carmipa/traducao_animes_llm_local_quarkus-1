package org.traducao.projeto.contexto.lore.sidonia;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore da série Knights of Sidonia.
 *
 * <p>INVARIANTES DO DOMÍNIO: Izana Shinatose; Gauna; Garde; Ena; Heigus.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoKnightsOfSidonia implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Knights of Sidonia / Sidonia no Kishi.
        - A humanidade vive na nave-semente Sidonia apos a destruicao da Terra pelos Gauna.
        - Termos centrais: Sidonia, Gauna, Gardes, Kabizashi, Ena, Heigus particles, placenta, gauna core.
        - Use "Guarda" apenas se o contexto for funcao comum; para mechas/pilotos, mantenha Garde/Gardes como termo da obra.
        - "Ena" e o orgao/membrana bioenergetica que cobre certos humanos modificados, permite fotossintese e potencializa o uso de particulas Heigus; nunca traduza como simples "pele" ou "aura".
        - Organizacoes e lugares: Toha Heavy Industries, Immortal Ship Committee, Residential Layer, Photosynthesis Chamber, hangar de Gardes.
        - Principais nomes: Nagate Tanikaze (m), Shizuka Hoshijiro (f), Izana Shinatose (terceiro genero/intersexo conforme a obra — NUNCA "Shinoshinari"), Tsumugi Shiraui (f), Captain Kobayashi (f), Yuhata Midorikawa (f), Norio Kunato (m), Lala Hiyama (f).
        - Conceitos biologicos/sociais: fotossintese humana, clones, terceiro genero de Izana, imortalidade de certos lideres, hibridizacao Gauna.
        - Tom: ficcao cientifica militar e existencial; traduza termos tecnicos de forma clara, mantendo estranhamento biologico e tensao de sobrevivencia.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Knights of Sidonia", LORE);

    @Override
    public String getId() {
        return "sidonia";
    }

    @Override
    public String getNomeExibicao() {
        return "Knights of Sidonia";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege nomes e termos biológicos de Sidonia.
     * <p>INVARIANTES DO DOMÍNIO: Izana Shinatose canônico.
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Nagate Tanikaze", "Izana Shinatose", "Shizuka Hoshijiro",
            "Tsumugi Shiraui", "Norio Kunato", "Yuhata Midorikawa",
            "Kobayashi", "Lala Hiyama", "Ochiai",
            "Sidonia", "Gauna", "Garde",
            "Tsugumori", "Kabizashi", "Ena",
            "Heigus", "placenta", "Gauna core",
            "Toha Heavy Industries", "Immortal Ship Committee", "Residential Layer",
            "Photosynthesis Chamber"
        );
    }
}
