package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: lore agregada de Macross 7 fora da serie (Galaxy's Calling Me! + Dynamite 7 + Encore) — referência/agregadora,
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
 * <p>INVARIANTES DO DOMÍNIO: usar os contextos específicos — {@code macross_7_filme, macross_dynamite_7, macross_7_encore}.
 * Guardado por {@code CatracaAgregadorasForaDoCdiTest}, que cobre os DOIS catálogos.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
public class ContextoRevisaoLoreMacross7Filmes implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Macross 7: Filmes & OVAs (Dynamite 7 / Encore).
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Macross, OVAs, The Movie, The Galaxy, Calling, Dynamite, Encore, Basara Nekki, Mylene Flare Jenius, Ray Lovelock, Veffidas Feaze, Gamlin Kizaki, Pedro, Elma, Graham, Banda, Musicas, Fire Bomber, Basara, Mechas, Criaturas, Custom Fire Valkyrie, Sturmvogel, Anima Spiritia, Baleias Espaciais, Space Whales.
        - Personagens: Basara Nekki (homem), Mylene Flare Jenius (mulher), Ray Lovelock (homem), Veffidas Feaze (mulher), Gamlin Kizaki (homem), Pedro (homem), Elma (mulher), Graham (homem).
        - Banda / Musicas: Fire Bomber, canciones de Basara.
        - Mechas / Criaturas: VF-19 Custom Fire Valkyrie, VF-22 Sturmvogel II, Anima Spiritia, Galácticos / Baleias Espaciais (Space Whales / Elma).
        - Alertas: Valkyrie nao vira Valquiria/Valquíria; Zentradi grafia oficial; proibido Veritech;
          GERWALK/Battroid/Fighter Mode — nao traduzir nomes dos modos.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "macross_7_filmes"; }
    @Override public String getNomeExibicao() { return "Macross 7: Filmes & OVAs (Dynamite 7 / Encore) - Revisao de Lore"; }
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
            Map.entry("Protodevilns", "Protodeviln"),
            Map.entry("Protodemonios", "Protodeviln"),
            Map.entry("Protodemônios", "Protodeviln"),
            Map.entry("Energia da Canção", "Song Energy"),
            Map.entry("Energia da Cancao", "Song Energy")
        ));
    }
}
