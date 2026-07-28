package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: lore agregada de os tres filmes Macross Frontier — referência/agregadora,
 * FORA do registro CDI, espelhando a decisão já tomada no lado da Tradução.
 *
 * <h2>Por que também aqui, e não só na Tradução</h2>
 * A Opção 7 não retraduz: ela normaliza termo para "o padrão oficial da obra". Isso parecia
 * torná-la segura contra a lore misturada — e no fluxo EN+PT quase é, porque
 * {@code ValidadorCandidatoLoreService} exige que a sequência nova apareça no ORIGINAL INGLÊS
 * além da lore.
 *
 * <p>No fluxo PT-ONLY esse portão NÃO existe. {@code RevisarLorePtOnlyUseCase} manda o prompt da
 * lore ao LLM com o original inglês VAZIO, e {@code CorretorLoreDeterministico} diz no próprio
 * comentário que "sem o EN não existe o portão 'o original contém o canônico'". Com a lore
 * agregada nesse caminho, o modelo pode normalizar um termo de um título para o canônico de
 * OUTRO — e o trabalho declarado dele é exatamente esse. A linha {@code Nomes/termos} desta
 * classe juntava justamente os nomes exclusivos de cada obra: era a superfície do erro.
 *
 * <p>INVARIANTES DO DOMÍNIO: usar os contextos específicos — {@code macross_frontier_filme1, macross_frontier_filme2, macross_frontier_filme3}.
 * Guardado por {@code CatracaAgregadorasForaDoCdiTest}, que cobre os DOIS catálogos.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
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
