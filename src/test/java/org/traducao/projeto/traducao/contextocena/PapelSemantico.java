package org.traducao.projeto.traducao.contextocena;

/**
 * PROPÓSITO DE NEGÓCIO: distingue os três papéis semânticos que a concordância de gênero
 * precisa separar para não trocar o gênero errado — o problema central visto no Gundam
 * 08th MS Team ("Thank you, Norris." dito pela Aina, em que "obrigada" concorda com o
 * FALANTE, não com o destinatário Norris). É metadado do conjunto-ouro (D0): cada caso
 * declara sobre QUEM a flexão de gênero deve concordar.
 *
 * <p>INVARIANTES DO DOMÍNIO: enum fechado de exatamente três papéis; a flexão de um
 * adjetivo/particípio concorda com um único papel por caso de teste. Não há papel
 * "desconhecido" aqui — quando a fonte não permite decidir, o caso não entra no gold como
 * flexão marcada, e sim como "neutro aceitável".
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: tipo puro sem estado; não lança.
 */
public enum PapelSemantico {
    /** Quem diz a fala (1ª pessoa): "Estou cansada" concorda com quem fala. */
    FALANTE,
    /** A quem a fala é dirigida (2ª pessoa/vocativo): "Você está louca?" concorda com o interlocutor. */
    DESTINATARIO,
    /** Terceiro citado na fala: "Ela está pronta" concorda com a pessoa referida. */
    REFERENTE
}
