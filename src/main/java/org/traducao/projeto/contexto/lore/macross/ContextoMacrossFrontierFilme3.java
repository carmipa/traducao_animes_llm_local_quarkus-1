package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * PROPÓSITO DE NEGÓCIO: lore de "Macross Frontier: Labyrinth of Time" (Toki no Meikyū, 2021), o
 * terceiro filme da Frontier. Antes desta classe, traduzir o filme caía em
 * {@code macross_frontier} — a lore da SÉRIE, que acerta o elenco base mas não conhece nada
 * específico do filme. A agregadora {@link ContextoMacrossFrontierFilmes} o cobria no papel, mas
 * está deliberadamente fora do CDI porque mistura os três filmes.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O gênero de cada personagem entre parênteses NÃO é decoração: é o que a revisão de
 *       concordância usa para decidir entre "cansado" e "cansada". Personagem sem gênero
 *       declarado é fala que nunca será corrigida.</li>
 *   <li>A terminologia da franquia (Valkyrie, Zentradi, Protoculture, Veritech) NÃO se repete
 *       aqui — vem do mapa compartilhado em {@link #correcoesTerminologia()}.</li>
 * </ul>
 *
 * <h2>Lacuna conhecida: não há linha de Naves / Mechas</h2>
 * As irmãs {@link ContextoMacrossFrontierFilme1} e {@link ContextoMacrossFrontierFilme2}
 * declaram VF-25 Messiah, VF-27 Lucifer, YF-29 Durandal e Vajra. Esta não declara: não foi
 * possível confirmar aparição de mecha neste filme, que é centrado no concerto e nas fold waves.
 *
 * <p>A omissão NÃO é neutra e vale saber por quê: o mapa de terminologia da franquia cobre
 * {@code Valkyrie}, {@code Zentradi}, {@code Meltrandi}, {@code Protoculture} e
 * {@code Minmay Attack} — e NENHUMA designação de modelo. Quem protege {@code VF-25} e
 * {@code YF-29} nas irmãs é exatamente a linha de mechas da lore. Se um modelo aparecer em
 * diálogo deste filme, hoje nada o declara. Acrescentar a linha é a correção, e ela é aditiva.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoMacrossFrontierFilme3 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross Frontier O Filme: Labyrinth of Time (Toki no Meikyū).
        - Personagens: Alto Saotome (homem), Sheryl Nome (mulher), Ranka Lee (mulher), Michael Blanc (homem), Luca Angeloni (homem), Klan Klang (mulher), Ozma Lee (homem), Nanase Matsuura (mulher).
        - Canções: Toki no Meikyū (Labyrinth of Time), Sacrifice, Hoshi Kira.
        """;

    private static final String PROMPT = ContextoPrompt.montar(
        "Macross Frontier: Labyrinth of Time (Toki no Meikyū)", LORE);

    @Override public String getId() { return "macross_frontier_filme3"; }

    @Override public String getNomeExibicao() {
        return "Macross Frontier: Labyrinth of Time (Toki no Meikyū)";
    }

    @Override public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: restaura grafias oficiais Macross (Valkyrie/Zentradi) quando o LLM
     * localiza indevidamente — mapa compartilhado da franquia — MAIS os três extras de Fold/Vajra.
     *
     * <h2>Por que esta obra nasce SEM a dívida de paridade das irmãs</h2>
     * {@code macross_frontier}, {@code macross_frontier_filme1} e {@code macross_frontier_filme2}
     * estão todas em {@code DIVERGENCIAS_DECLARADAS} do {@code ParidadeMapasTerminologiaTest}: a
     * Revisão de Lore delas corrige {@code Falha Fold -> Fold Fault} e {@code Vajras -> Vajra},
     * e a Tradução não. É dívida antiga — a decisão vive num catálogo só.
     *
     * <p>Aqui os DOIS lados declaram os mesmos extras, então a obra entra com paridade e não
     * engorda a lista. A alternativa era tirar os extras da Revisão para casar por baixo, mas o
     * sentido certo de resolver essa dívida é a Tradução GANHAR a correção, não a Revisão perdê-la.
     * Quando as irmãs forem niveladas, elas saem da lista pelo mesmo caminho.
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho exato de
     * {@code ContextoRevisaoLoreMacrossFrontierFilme3.correcoesTerminologia()} — os dois lados
     * são comparados por teste, então mexer num sem o outro reprova.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
     */
    @Override
    public java.util.Map<String, String> correcoesTerminologia() {
        java.util.Map<String, String> mapa =
            new java.util.LinkedHashMap<>(CorrecoesTerminologiaMacross.mapa());
        mapa.put("Falha Fold", "Fold Fault");
        mapa.put("Falha de Fold", "Fold Fault");
        mapa.put("Vajras", "Vajra");
        return java.util.Collections.unmodifiableMap(mapa);
    }
}
