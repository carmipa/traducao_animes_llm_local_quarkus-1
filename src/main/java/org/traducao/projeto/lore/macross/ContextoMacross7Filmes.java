package org.traducao.projeto.lore.macross;

import org.traducao.projeto.lore.domain.ContextoPrompt;
import org.traducao.projeto.lore.domain.ProvedorContexto;

/**
 * PROPÓSITO DE NEGÓCIO: lore agregada de Macross 7 fora da série (The Galaxy's Calling Me! +
 * Dynamite 7 + Encore) — referência/agregadora, sem {@code @Component} (fora do registro CDI),
 * pelo mesmo motivo de {@link ContextoMacrossDeltaFilmes} e
 * {@link ContextoMacrossFrontierFilmes}.
 *
 * <p>INVARIANTES DO DOMÍNIO: preferir os contextos específicos — {@code macross_7_filme}
 * (Galaxy's Calling Me!), {@code macross_dynamite_7} e {@code macross_7_encore}. Os três
 * existem e cobrem cada obra sozinha.
 *
 * <h2>A colisão que motivou a saída, medida</h2>
 * Esta agregadora oferecia ao LLM, de uma vez, termos que pertencem a UMA das três obras:
 * <ul>
 *   <li>{@code Elma}, {@code Graham}, {@code Baleias Espaciais / Space Whales} — só existem em
 *       Dynamite 7 (planeta Zola, arco das baleias);</li>
 *   <li>{@code Pedro} — só existe em The Galaxy's Calling Me!;</li>
 *   <li>{@code Ray Lovelock}, {@code Veffidas Feaze}, {@code Gamlin Kizaki} — não aparecem no
 *       Dynamite 7, que é praticamente um road movie do Basara sozinho.</li>
 * </ul>
 * Traduzir o Dynamite 7 com esta lore põe Pedro à disposição; traduzir o filme põe as baleias
 * espaciais. É a mesma contaminação que já mantinha Delta e Frontier fora — a diferença é que
 * estas duas foram excluídas desde o início e esta ficou registrada por omissão.
 *
 * <p>SAÍDA DO CDI em 2026-07-27, autorizada após medir o custo real: ZERO arquivos de cache
 * carimbados com {@code contextoId=macross_7_filmes} e nenhuma pasta Macross no acervo. Nenhum
 * artefato existente foi invalidado. Saiu também do manifesto E7a (58 provedores).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; mapa de terminologia imutável.
 */
public class ContextoMacross7Filmes implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross 7 (Filmes e OVAs: Macross 7 The Movie - The Galaxy is Calling Me! / Dynamite 7 / Encore).
        - Personagens: Basara Nekki (homem), Mylene Flare Jenius (mulher), Ray Lovelock (homem), Veffidas Feaze (mulher), Gamlin Kizaki (homem), Pedro (homem), Elma (mulher), Graham (homem).
        - Banda / Musicas: Fire Bomber, canciones de Basara.
        - Mechas / Criaturas: VF-19 Custom Fire Valkyrie, VF-22 Sturmvogel II, Anima Spiritia, Galácticos / Baleias Espaciais (Space Whales / Elma).
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross 7 (Filmes & OVAs)", LORE);

    @Override public String getId() { return "macross_7_filmes"; }
    @Override public String getNomeExibicao() { return "Macross 7: Filmes & OVAs (Dynamite 7 / Encore)"; }
    @Override public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: restaura grafias oficiais Macross (Valkyrie/Zentradi) quando
     * o LLM localiza indevidamente — mapa compartilhado da franquia.
     *
     * <p>INVARIANTES DO DOMÍNIO: só aplica com canônico no original EN.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
     */
    @Override
    public java.util.Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaMacross.mapa();
    }

}
