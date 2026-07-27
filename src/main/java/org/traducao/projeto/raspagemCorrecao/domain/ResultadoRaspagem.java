package org.traducao.projeto.raspagemCorrecao.domain;

/**
 * PROPÓSITO DE NEGÓCIO: desfecho tipado de uma tentativa de recuperação externa — o
 * {@link StatusRaspagem} e o texto associado. Existe para o chamador saber o MOTIVO em vez de
 * inferi-lo por igualdade de strings, que era a fonte da leitura inconsistente entre os dois
 * consumidores.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Em {@link StatusRaspagem#SUCESSO}, {@code texto} é a tradução; em <b>qualquer</b> outro
 *       caso é o <b>texto original</b>. Nunca vem vazio por causa de recusa — é essa garantia que
 *       permite ao chamador manter a fala intacta sem ramo especial, e é o motivo de este record
 *       não ter virado um {@code Optional}: trocar o texto por "vazio" reintroduziria a
 *       possibilidade de gravar legenda em branco quando o provedor recusa.</li>
 *   <li>Record imutável de domínio: só JDK, sem framework, sem I/O.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Portador de desfecho; não lança.
 *
 * @param status causa canônica do desfecho
 * @param texto a tradução em caso de sucesso; o texto original em todos os demais
 */
public record ResultadoRaspagem(StatusRaspagem status, String texto) {

    public boolean sucesso() {
        return status == StatusRaspagem.SUCESSO;
    }

    public static ResultadoRaspagem sucesso(String traducao) {
        return new ResultadoRaspagem(StatusRaspagem.SUCESSO, traducao);
    }

    public static ResultadoRaspagem semAlteracao(String original) {
        return new ResultadoRaspagem(StatusRaspagem.SEM_ALTERACAO, original);
    }

    public static ResultadoRaspagem falhaTransitoria(String original) {
        return new ResultadoRaspagem(StatusRaspagem.FALHA_TRANSITORIA, original);
    }

    public static ResultadoRaspagem respostaInvalida(String original) {
        return new ResultadoRaspagem(StatusRaspagem.RESPOSTA_INVALIDA, original);
    }

    public static ResultadoRaspagem tagCorrompida(String original) {
        return new ResultadoRaspagem(StatusRaspagem.TAG_CORROMPIDA, original);
    }
}
