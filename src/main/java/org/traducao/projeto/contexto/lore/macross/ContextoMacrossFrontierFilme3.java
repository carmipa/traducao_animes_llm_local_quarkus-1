package org.traducao.projeto.contexto.lore.macross;

import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * ESQUELETO — NÃO REGISTRAR. Falta o conteúdo de lore, e ele NÃO deve ser preenchido de memória.
 *
 * <h2>Por que esta classe existe vazia</h2>
 * "Macross Frontier: Labyrinth of Time" (Toki no Meikyū, 2021) é o terceiro filme da Frontier e
 * hoje é a única obra do catálogo Macross SEM contexto próprio. Quem o traduzir cai em
 * {@code macross_frontier}, a lore da SÉRIE — que acerta elenco e mechas, mas não conhece nada
 * que seja específico do filme. A agregadora {@link ContextoMacrossFrontierFilmes} o cobre no
 * papel, mas está deliberadamente fora do CDI porque mistura os três filmes.
 *
 * <h2>Por que o conteúdo não foi escrito junto</h2>
 * O que entra em {@code LORE} vira PROMPT DE SISTEMA: o texto é enviado ao LLM em toda fala do
 * episódio. Um nome de personagem trocado, uma grafia de mecha errada ou uma canção inventada
 * aqui não quebra teste nenhum — ela se propaga silenciosamente por toda a legenda, e o
 * manifesto E7a congela o erro junto com o resto. Lore preenchida "de cabeça" por quem não
 * assistiu à obra é pior que lore ausente, porque a ausência é visível e o erro não.
 *
 * <h2>Como completar</h2>
 * <ol>
 *   <li>Preencher {@code LORE} no MESMO formato das irmãs — ver
 *       {@link ContextoMacrossFrontierFilme1} e {@link ContextoMacrossFrontierFilme2}: linha de
 *       Obra, linha de Personagens com o gênero de cada um entre parênteses, linha de
 *       Naves/Mechas, e o que for específico deste filme (canções e insertos, se houver).</li>
 *   <li>O gênero de cada personagem NÃO é decoração: é o que a revisão de concordância usa para
 *       decidir "cansado" ou "cansada". Personagem sem gênero declarado é fala sem correção.</li>
 *   <li>Só então acrescentar {@code @Component}.</li>
 *   <li>Rodar {@code ./gradlew test --rerun-tasks}: o total sobe de 58 para 59 em
 *       {@code RegistroProvedoresContextoIT} e o manifesto E7a precisa ganhar a linha
 *       {@code id.macross_frontier_filme3} e um novo {@code aggregate}.</li>
 *   <li>Acrescentar {@code "macross_frontier_filme3"} à ordenação em {@code CatalogoObras},
 *       ao lado de {@code macross_frontier_filme2}.</li>
 * </ol>
 *
 * <p>INVARIANTES DO DOMÍNIO: enquanto {@code LORE} estiver vazia, esta classe não pode ser
 * registrada — um prompt de sistema sem lore é pior que o fallback da série, porque o operador
 * acreditaria estar usando a lore do filme.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O. Não é descoberta pelo CDI, então não afeta o
 * registro nem o manifesto enquanto estiver assim.
 */
public class ContextoMacrossFrontierFilme3 implements ProvedorContexto {

    /** VAZIA DE PROPÓSITO — ver o Javadoc da classe antes de preencher. */
    private static final String LORE = "";

    private static final String PROMPT = ContextoPrompt.montar(
        "Macross Frontier: Labyrinth of Time (Toki no Meikyū)", LORE);

    @Override public String getId() { return "macross_frontier_filme3"; }

    @Override public String getNomeExibicao() {
        return "Macross Frontier: Labyrinth of Time (Toki no Meikyū)";
    }

    @Override public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: restaura grafias oficiais Macross (Valkyrie/Zentradi) quando o LLM
     * localiza indevidamente — mapa compartilhado da franquia, igual ao das obras irmãs.
     *
     * <p>INVARIANTES DO DOMÍNIO: este mapa é da FRANQUIA e já está correto; é a única parte
     * desta classe que não depende do conteúdo pendente.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
     */
    @Override
    public java.util.Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaMacross.mapa();
    }
}
