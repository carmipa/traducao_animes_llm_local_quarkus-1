package org.traducao.projeto.traducao.domain.contextocena;

/**
 * PROPÓSITO DE NEGÓCIO: representa uma única fala dentro de uma janela contextual de
 * diálogo — o texto, seu índice de ordem e o estilo — para que a correção de gênero por
 * contexto de cena possa dar ao modelo as falas vizinhas como REFERÊNCIA sem confundir a
 * fala-alvo (a que será traduzida) com o contexto (que só ambienta a cena). É dado de
 * domínio da fatia {@code traducao}, autocontido e independente de {@code EventoLegenda}
 * (o mapeamento fica no montador da janela).
 *
 * <p>INVARIANTES DO DOMÍNIO: {@code record} imutável; só JDK, sem dependência de peer ou
 * framework; o {@code indice} preserva a ordem original da legenda para que a janela saiba
 * o que vem antes e o que vem depois da fala-alvo.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: portador de dados puro; não valida nem sanitiza —
 * aceita {@code texto}/{@code estilo} nulos, tratados por quem consome.
 *
 * @param indice posição ordinal da fala na legenda
 * @param estilo nome do estilo ASS da fala (para distinguir diálogo de letreiro/karaokê)
 * @param texto texto visível da fala
 */
public record LinhaAlvoContextual(
    int indice,
    String estilo,
    String texto
) {
}
