package org.traducao.projeto.traducao.domain.contextocena;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: a "janela contextual de diálogo" de uma fala — a fala-alvo (a que
 * será traduzida) mais suas vizinhas imediatas antes e depois, usadas SÓ como referência
 * para o modelo inferir o falante e o gênero na cena. Resolve o problema-raiz da inversão
 * de gênero do 08th MS Team: a fonte não diz quem fala, então a única pista é o que se diz
 * ao redor. O nome é honesto — é vizinhança, não reconhecimento real de cena (o
 * {@code EventoLegenda} não tem tempo nem fronteira de cena).
 *
 * <p>INVARIANTES DO DOMÍNIO: {@code record} imutável; {@code antes} e {@code depois} são
 * cópias defensivas imutáveis em ordem cronológica (documento), terminando/começando
 * imediatamente adjacentes à fala-alvo; só o {@code alvo} é traduzido — as vizinhas NUNCA
 * são traduzidas nem contadas como saída. Só JDK, sem dependência de peer.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: listas nulas viram listas vazias imutáveis; um elemento
 * {@code null} nas listas faz {@link List#copyOf} lançar {@code NullPointerException}
 * (entrada inválida). {@code alvo} nulo é aceito e tratado por quem consome.
 *
 * @param alvo a fala a traduzir
 * @param antes vizinhas imediatamente anteriores (referência; pode ser vazia)
 * @param depois vizinhas imediatamente posteriores (referência; pode ser vazia)
 */
public record JanelaContextual(
    LinhaAlvoContextual alvo,
    List<LinhaAlvoContextual> antes,
    List<LinhaAlvoContextual> depois
) {
    public JanelaContextual {
        antes = antes == null ? List.of() : List.copyOf(antes);
        depois = depois == null ? List.of() : List.copyOf(depois);
    }
}
