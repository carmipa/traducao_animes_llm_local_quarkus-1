package org.traducao.projeto.traducao.contextocena;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: schema de um caso do CONJUNTO-OURO de concordância de gênero (D0) —
 * um caso real de inversão observado (Gundam 08th MS Team) rotulado por PROPRIEDADE
 * semântica, não por tradução literal. Guarda o que é preciso para medir acerto sem
 * verdade-base subjetiva: a fala original, sua janela de vizinhança, sobre quem a flexão
 * deve concordar (papel), qual gênero é esperado, se uma formulação neutra é aceitável e
 * qual foi a saída ERRADA observada hoje (linha-base para o futuro A/B).
 *
 * <p>INVARIANTES DO DOMÍNIO: {@code generoEsperado} é sempre MASCULINO ou FEMININO (o gênero
 * que a lore/cena determina para o {@code papel}); {@code baselineAtual} é a saída PT-BR
 * defeituosa efetivamente observada, preservada como evidência histórica e NUNCA como
 * gabarito de tradução; {@code vizinhasAntes}/{@code vizinhasDepois} são a janela de
 * referência (podem vir vazias em D0, quando ainda não extraímos o original em inglês das
 * linhas adjacentes — o campo existe no schema desde já). Record imutável, só JDK.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: portador de dados puro; não valida nem lança. Coerência
 * (ex.: {@code generoEsperado} marcado) é responsabilidade de quem monta o gold.
 *
 * @param indice posição ordinal aproximada do evento na legenda (referência)
 * @param originalEn fala original em inglês
 * @param vizinhasAntes falas de contexto imediatamente anteriores (referência; pode ser vazia)
 * @param vizinhasDepois falas de contexto imediatamente posteriores (referência; pode ser vazia)
 * @param personagemReferido nome do personagem cujo gênero rege a concordância
 * @param papel se a flexão concorda com o falante, o destinatário ou um referente
 * @param generoEsperado gênero que a flexão deveria ter (MASCULINO ou FEMININO)
 * @param neutroAceitavel se uma formulação neutra em PT-BR é uma saída aceitável
 * @param baselineAtual saída PT-BR ERRADA observada hoje (evidência da inversão)
 */
public record GoldCaseConcordancia(
    int indice,
    String originalEn,
    List<String> vizinhasAntes,
    List<String> vizinhasDepois,
    String personagemReferido,
    PapelSemantico papel,
    GeneroFlexao generoEsperado,
    boolean neutroAceitavel,
    String baselineAtual
) {
    public GoldCaseConcordancia {
        vizinhasAntes = vizinhasAntes == null ? List.of() : List.copyOf(vizinhasAntes);
        vizinhasDepois = vizinhasDepois == null ? List.of() : List.copyOf(vizinhasDepois);
    }
}
