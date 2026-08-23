package org.traducao.projeto.core.texto.gramatica;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: uma acusação de gramática sobre um trecho de fala — o que o revisor viu,
 * onde viu, e o que ele propõe no lugar.
 *
 * <p>É deliberadamente um ACHADO, não uma correção aplicada: quem decide se a proposta vira texto
 * é a fatia que chamou, depois de passar pelos portões dela (lore, música, régua de qualidade).
 * Foi assim que a revisão de lore aprendeu a não deixar o modelo escrever direto no arquivo.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code inicio} e {@code fim} são posições no texto que foi ENTREGUE ao revisor — o texto
 *       visível, sem tag de override. Quem aplicar precisa mapear de volta.</li>
 *   <li>{@code sugestoes} pode vir vazia: existe regra que acusa sem saber o conserto, e uma
 *       acusação sem proposta continua sendo informação útil para o operador.</li>
 *   <li>{@code categoria} é o que se liga e desliga em bloco na configuração — por isso ela viaja
 *       junto com o achado, e não fica só do lado de dentro do adaptador.</li>
 * </ul>
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: é um record imutável; não valida nem lança. Revisor
 * indisponível devolve lista vazia lá na porta, e não um achado vazio aqui.
 *
 * @param regra      id da regra que disparou (ex.: {@code PARONYM_NOTICIA_224})
 * @param categoria  bloco a que a regra pertence (ex.: {@code CONFUSED_WORDS})
 * @param inicio     posição inicial no texto entregue, contada de 0
 * @param fim        posição final, exclusiva
 * @param trecho     o pedaço exato que foi acusado
 * @param mensagem   a explicação em português, como o revisor a escreve
 * @param sugestoes  substituições propostas, na ordem em que o revisor as ordenou
 */
public record AchadoGramatical(
    String regra,
    String categoria,
    int inicio,
    int fim,
    String trecho,
    String mensagem,
    List<String> sugestoes) {

    public AchadoGramatical {
        sugestoes = sugestoes == null ? List.of() : List.copyOf(sugestoes);
    }

    /** A primeira sugestão, ou {@code null} quando a regra acusa sem propor conserto. */
    public String primeiraSugestao() {
        return sugestoes.isEmpty() ? null : sugestoes.get(0);
    }
}
