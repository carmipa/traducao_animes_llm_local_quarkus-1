package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: lore agregada de os filmes Macross Delta (Passionate Walküre + Absolute Live!!!!!!) — referência/agregadora,
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
 * <p>INVARIANTES DO DOMÍNIO: usar os contextos específicos — {@code macross_delta_filme1, macross_delta_filme2}.
 * Guardado por {@code CatracaAgregadorasForaDoCdiTest}, que cobre os DOIS catálogos.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
public class ContextoRevisaoLoreMacrossDeltaFilmes implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Macross Delta (Filmes) — Passionate Walküre + Absolute Live!!!!!!
        - Preferir macross_delta_filme1 / macross_delta_filme2 quando for so um filme.
        - Walküre ≠ Valkyrie; PROIBIDO Veritech.

        === Passionate Walküre (filme 1) ===
        - Windermere Kingdom / Aerial Knights / Var Syndrome / Fold Waves /
          VF-31 Siegfried / Macross Elysion. SEM Heimdall / Yami_Q_Ray.

        === Absolute Live!!!!!! (filme 2) ===
        - Heimdall (Ian Cromwell); Yami_Q_Ray (+ Yami Mikumo/Freyja/Kaname/Makina/Reina);
          Max Jenius / Macross Gigant; VF-31AX Kairos-Plus; Star Singer.

        === Roster comum ===
        - Walküre: Freyja Wion, Mikumo Guynemer, Kaname Buccaneer, Makina Nakajima, Reina Prowler.
        - Delta Flight: Hayate Immelmann, Mirage Farina Jenius, Arad Molders, Chuck Mustang,
          Messer Ihlefeld.

        === Formas-ruim ===
        - Walkure → Walküre; Síndrome Var → Var Syndrome; Ondas Fold → Fold Waves;
          Esquadrão Delta → Delta Flight; Cavaleiros Aéreos → Aerial Knights;
          Heimdal → Heimdall; Yami Q Ray → Yami_Q_Ray; Gigante Macross → Macross Gigant;
          Valquíria → Valkyrie.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override
    public String getId() {
        return "macross_delta_filmes";
    }

    @Override
    public String getNomeExibicao() {
        return "Macross Delta (Filmes) - Revisao de Lore";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico Delta na Opção 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho da Tradução Local Delta.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaMacrossDeltaRevisao.mapa();
    }
}
