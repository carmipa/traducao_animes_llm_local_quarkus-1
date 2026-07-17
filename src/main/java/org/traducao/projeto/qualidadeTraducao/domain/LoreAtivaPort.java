package org.traducao.projeto.qualidadeTraducao.domain;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: porta de saída pela qual o peer {@code qualidadeTraducao}
 * obtém a terminologia e a lore da obra atualmente selecionada, para decidir se uma
 * fala idêntica ao original é um termo canônico legítimo (nome, facção, patente) ou
 * uma tradução que o LLM simplesmente não fez. Inverte a dependência que antes ligava
 * o detector diretamente ao {@code contexto}: o peer declara o contrato de que precisa
 * e a fatia que possui o contexto fornece a implementação.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Contrato mínimo: exatamente as duas leituras que o detector consome — nada de
 *       escrita, seleção de contexto ou exposição do prompt de tradução completo.</li>
 *   <li>É a lore do contexto ATIVO no momento da consulta; trocar o contexto ativo muda
 *       o que estes métodos retornam, sem que o consumidor precise ser reconfigurado.</li>
 *   <li>Pertence ao domínio do peer: depende apenas de JDK, para não reintroduzir
 *       acoplamento a {@code contexto} nem a qualquer outra fatia.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nenhum método lança: a ausência de contexto ativo é um estado normal, não um erro.
 * {@link #termosProtegidosAtivos()} degrada para conjunto vazio e {@link #obterLoreAtiva()}
 * para a lore neutra que a implementação adotar — o detector, diante de qualquer um dos
 * dois, apenas deixa de reconhecer termos de lore e recai nas heurísticas globais.
 */
public interface LoreAtivaPort {

    /**
     * PROPÓSITO DE NEGÓCIO: lista os termos que não devem ser traduzidos na obra ativa
     * (nomes próprios, facções, codinomes), para que uma fala idêntica a um deles seja
     * preservada em vez de reenviada como suspeita de não-tradução.
     * <p>INVARIANTES DO DOMÍNIO: reflete o contexto ativo no instante da chamada; a
     * comparação de pertencimento é responsabilidade do consumidor, não da porta.
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança; sem contexto ativo ou sem termos
     * declarados retorna conjunto vazio.
     *
     * @return conjunto de termos protegidos do contexto ativo, possivelmente vazio
     */
    Set<String> termosProtegidosAtivos();

    /**
     * PROPÓSITO DE NEGÓCIO: entrega o texto de lore/terminologia da obra ativa, sobre o
     * qual o detector procura um termo inteiro antes de julgar uma fala como não traduzida.
     * <p>INVARIANTES DO DOMÍNIO: é apenas a lore do contexto ativo, sem as demais seções
     * do prompt de tradução (prioridades, regras de saída).
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança; sem contexto ativo retorna a lore
     * neutra definida pela implementação.
     *
     * @return a lore do contexto ativo, ou a lore neutra da implementação quando não há contexto
     */
    String obterLoreAtiva();
}
