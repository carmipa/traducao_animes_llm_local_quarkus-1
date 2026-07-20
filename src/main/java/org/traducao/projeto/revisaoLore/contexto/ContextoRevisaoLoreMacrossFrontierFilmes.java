package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Macross Frontier: Filmes (False Songstress / Wings of Farewell).
 *
 * <p>INVARIANTES DO DOMÍNIO: Valkyrie/Zentradi oficiais; sem Veritech/Valquíria.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreMacrossFrontierFilmes implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Macross Frontier: Filmes (False Songstress / Wings of Farewell).
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Macross Frontier, Itsuwari, Utafihme, Itsuka, Tsubasa, Labyrinth, Time, Alto Saotome, Sheryl Nome, Ranka Lee, Michael Blanc, Luca Angeloni, Klan Klang, Ozma Lee, Brera Sterne, Grace, Connor, Naves, Mechas, Messiah, Lucifer, Durandal, Vajra.
        - Personagens: Alto Saotome (homem), Sheryl Nome (mulher), Ranka Lee (mulher), Michael Blanc (homem), Luca Angeloni (homem), Klan Klang (mulher), Ozma Lee (homem), Brera Sterne (homem), Grace O'Connor (mulher).
        - Naves / Mechas: VF-25 Messiah, VF-27 Lucifer, YF-29 Durandal, Vajra.
        - Alertas: Valkyrie nao vira Valquiria/Valquíria; Zentradi grafia oficial; proibido Veritech;
          GERWALK/Battroid/Fighter Mode — nao traduzir nomes dos modos.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "macross_frontier_filmes"; }
    @Override public String getNomeExibicao() { return "Macross Frontier: Filmes (False Songstress / Wings of Farewell) - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico Macross na Opção 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho da Tradução Local.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaMacrossRevisao.comExtras(Map.ofEntries(
            Map.entry("Falha Fold", "Fold Fault"),
            Map.entry("Falha de Fold", "Fold Fault"),
            Map.entry("Vajras", "Vajra")
        ));
    }
}
