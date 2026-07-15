package org.traducao.projeto.raspagemCorrecao.infrastructure;

/**
 * Resultado tipado de {@link GoogleTranslateScraper#traduzir(String)}: o
 * {@link StatusRaspagem} do desfecho e o texto associado.
 * <p>
 * Em {@link StatusRaspagem#SUCESSO}, {@code texto} é a tradução; em qualquer
 * outro caso é o <b>texto original</b> (o chamador mantém a fala intacta), agora
 * sabendo o MOTIVO em vez de inferir por igualdade de strings.
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
