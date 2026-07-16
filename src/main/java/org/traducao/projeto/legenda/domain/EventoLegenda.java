package org.traducao.projeto.legenda.domain;

/**
 * PROPÓSITO DE NEGÓCIO: representa, como dado imutável do módulo compartilhado
 * {@code legenda}, um único evento (linha) de uma legenda — seu índice de ordem, o
 * tipo de linha (ex.: {@code Dialogue}), o estilo, o prefixo estrutural e o texto
 * visível — servindo de unidade que leitores, escritores e regras percorrem.
 *
 * <p>INVARIANTES DO DOMÍNIO: é um {@code record} — os cinco componentes são fixados na
 * construção; igualdade e hash derivam de todos eles. Não há validação: qualquer valor,
 * inclusive {@code null} em {@code texto} ou nos demais campos de texto, é aceito.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: o record não lança nem sanitiza; consultas sobre
 * campos nulos são tratadas pelos próprios métodos ({@link #temTexto()} devolve
 * {@code false} para {@code texto} nulo). Nenhuma responsabilidade de I/O ou tradução
 * vive aqui.
 *
 * @param indice posição ordinal do evento dentro da legenda
 * @param tipoLinha tipo da linha (ex.: {@code Dialogue}), base de {@link #isDialogo()}
 * @param estilo nome do estilo associado ao evento
 * @param prefixo prefixo estrutural da linha, preservado na reescrita
 * @param texto texto visível do evento; pode ser {@code null}
 */
public record EventoLegenda(
    int indice,
    String tipoLinha,
    String estilo,
    String prefixo,
    String texto
) {
    /**
     * PROPÓSITO DE NEGÓCIO: informa se o evento carrega texto (mesmo que vazio),
     * distinguindo-o de um evento sem conteúdo textual definido.
     * <p>INVARIANTES DO DOMÍNIO: considera presença por não-nulidade de {@code texto};
     * string vazia {@code ""} conta como presente.
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code texto} nulo devolve {@code false}, sem lançar.
     */
    public boolean temTexto() {
        return texto != null;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: identifica se o evento é uma linha de diálogo, o tipo que
     * concentra o texto traduzível de uma legenda.
     * <p>INVARIANTES DO DOMÍNIO: verdadeiro somente quando {@code tipoLinha} é exatamente
     * {@code "Dialogue"} (comparação sensível a maiúsculas/minúsculas).
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code tipoLinha} nulo devolve {@code false}
     * (a comparação parte da constante literal), sem lançar.
     */
    public boolean isDialogo() {
        return "Dialogue".equals(tipoLinha);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: produz uma cópia do evento com o texto substituído,
     * preservando índice, tipo, estilo e prefixo — usado ao aplicar uma tradução ou
     * correção sem mutar o evento original.
     * <p>INVARIANTES DO DOMÍNIO: retorna sempre uma nova instância; os demais quatro
     * componentes são copiados sem alteração; a instância atual permanece intacta.
     * <p>COMPORTAMENTO EM CASO DE FALHA: aceita {@code novoTexto} nulo sem lançar,
     * gerando um evento cujo {@link #temTexto()} passará a ser {@code false}.
     */
    public EventoLegenda comTexto(String novoTexto) {
        return new EventoLegenda(indice, tipoLinha, estilo, prefixo, novoTexto);
    }
}
