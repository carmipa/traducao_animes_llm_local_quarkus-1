package org.traducao.projeto.contexto.domain;

/**
 * PROPÓSITO DE NEGÓCIO: desfecho da comparação entre a OBRA reconhecida no caminho do
 * arquivo e o CONTEXTO/lore selecionado para a execução. Existe porque, hoje, "qual
 * arquivo traduzir" e "com qual lore traduzir" são dois campos independentes da UI: um
 * clique errado no combo produz um cache inteiro coerentemente errado, carimbado com a
 * proveniência da obra errada. Este veredicto é o que dá voz ao caminho do arquivo.
 *
 * <p>Mora no peer {@code contexto} porque IDENTIDADE DE OBRA é assunto deste peer: quem
 * sabe quais obras existem e qual pasta pertence a qual lore é o catálogo de contextos.
 * O peer {@code qualidadeTraducao} julga o TEXTO produzido pela tradução, não a identidade
 * da obra — por isso este enum não pode voltar para lá.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>São exatamente QUATRO desfechos e não há um quinto implícito: ou casa, ou diverge
 *       com prova, ou a pasta é reivindicada por mais de uma obra com a mesma precisão, ou
 *       não se sabe. Não existe "provavelmente é outra obra".</li>
 *   <li>{@link #DIVERGENTE} exige PROVA POSITIVA: a pasta resolveu para UMA obra do catálogo
 *       e ela não é o contexto ativo. Nunca é inferido por ausência.</li>
 *   <li>{@link #AMBIGUO} cobre o caso em que a identidade NÃO resolve: duas ou mais obras
 *       reivindicam a pasta com a mesma especificidade. Bloqueia, porque sem uma obra única
 *       não há como provar que o arquivo é da lore selecionada — e adivinhar é exatamente o
 *       que esta guarda existe para impedir.</li>
 *   <li>{@link #INDETERMINADO} é o desfecho seguro e o único que permite seguir: cobre
 *       pasta que nenhum contexto reconhece e execução sem contexto ativo. É o estado normal
 *       da fase de migração, enquanto obras sem identidade cobrindo o nome real da pasta
 *       ainda existem — avisar sem bloquear é o que impede a guarda de parar tradução
 *       legítima.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Enum puro, sem estado nem I/O; não lança. A decisão do que FAZER com cada desfecho
 * (bloquear, avisar, seguir) é do consumidor, não deste tipo.
 */
public enum VeredictoObraContexto {

    /** A pasta do arquivo resolve para o contexto ATIVO — pode traduzir. */
    CASA,

    /**
     * A pasta do arquivo resolve para UMA obra do catálogo, e ela não é a ativa.
     * É a assinatura exata do incidente Gundam 0083 traduzido sob a lore de Guilty Crown.
     */
    DIVERGENTE,

    /**
     * A pasta é reivindicada por DUAS OU MAIS obras com a mesma especificidade, então a
     * identidade não resolve. É um defeito de catálogo (identidades pouco específicas) que só
     * aparece diante de um nome de pasta concreto — por isso é detectado em runtime, e não no
     * startup como a colisão de nome canônico exato.
     */
    AMBIGUO,

    /**
     * Não há base para julgar: nenhum contexto reconhece a pasta, ou não há contexto ativo.
     * Segue com aviso — a guarda nunca adivinha a obra a partir de um caminho desconhecido.
     */
    INDETERMINADO
}
