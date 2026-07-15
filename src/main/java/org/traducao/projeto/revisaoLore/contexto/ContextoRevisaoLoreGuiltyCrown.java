package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

@Component
public class ContextoRevisaoLoreGuiltyCrown implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Guilty Crown.
        - Regra central: revisar somente termos de lore, nomes proprios, organizacoes, tecnologias e siglas do universo da obra.
        - Nunca traduzir o titulo Guilty Crown como "Coroa Culpada" quando for nome da obra.
        - Personagens: Shu Ouma, Inori Yuzuriha, Gai Tsutsugami, Ayase Shinomiya, Tsugumi, Yahiro Samukawa, Hare Menjou, Souta Tamadate, Arisa Kuhouin, Mana Ouma, Daryl Yan, Makoto Waltz Segai.
        - Organizacoes/grupos: Funeral Parlor, Undertakers, GHQ, Anti Bodies, Da'ath.
        - Termos centrais: Void, Voids, Void Genome, Apocalypse Virus, Lost Christmas, King's Power, Apocalypse, Endlave, Endlaves, Leukocyte, Genomic Resonance.
        - Alertas: Void nao vira "vazio" quando for poder/arma; Funeral Parlor nao vira "funeraria" quando for nome do grupo; GHQ deve ficar como sigla; Apocalypse Virus deve manter a forma oficial.
        - Nomes sensiveis de correcao: Inori, Shu, Gai, Ayase, Tsugumi, Yahiro, Hare, Souta, Mana, Daryl e Segai devem manter grafia oficial.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "guilty_crown"; }
    @Override public String getNomeExibicao() { return "Guilty Crown - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
