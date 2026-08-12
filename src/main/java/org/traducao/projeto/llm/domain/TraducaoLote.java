package org.traducao.projeto.llm.domain;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: resultado da tradução de um {@link Lote} pelo LLM — as linhas
 * traduzidas mais o desfecho (sucesso ou falha com diagnóstico), para que o pipeline
 * decida entre publicar, retentar ou preservar o original.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code idLote} espelha o do {@link Lote} de origem, correlacionando pedido e resposta.</li>
 *   <li>{@code linhasTraduzidas} corresponde às linhas originais na mesma ordem.</li>
 *   <li>{@code sucesso} indica se a tradução é utilizável; {@code mensagemErro} traz o
 *       diagnóstico quando não é.</li>
 *   <li>{@code recusaDaRequisicao} separa os dois motivos de {@code sucesso == false} que antes
 *       tinham o MESMO sinal: o servidor RECUSOU este pedido (HTTP 4xx permanente — problema
 *       DESTA fala) ou o servidor está indisponível (timeout, 5xx, conexão recusada — problema
 *       de TODAS). Sem essa distinção, uma fala patológica custava o episódio inteiro: em
 *       2026-08-11, no DanMachi E03, um verso de karaokê fez o LM Studio devolver 400 e
 *       <b>373 falas de diálogo</b> foram perdidas.</li>
 *   <li>Record imutável de domínio: só JDK, sem dependência de framework ou fatia.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Em falha, {@code sucesso} é {@code false} e {@code mensagemErro} descreve a causa; o
 * chamador é quem decide preservar a tradução anterior. Este tipo não lança.
 * {@code recusaDaRequisicao} é {@code false} por omissão — quem não sabe distinguir cai no
 * comportamento conservador de tratar a falha como indisponibilidade.
 *
 * @param idLote identificador do lote, herdado do {@link Lote} de origem
 * @param linhasTraduzidas linhas traduzidas, na ordem das originais
 * @param sucesso {@code true} se a tradução é utilizável
 * @param mensagemErro diagnóstico quando {@code sucesso} é {@code false}
 * @param mascaradosSegundaOpiniao textos MASCARADOS cuja tradução veio de outro modelo
 * @param recusaDaRequisicao {@code true} quando o servidor respondeu e RECUSOU este pedido
 */
public record TraducaoLote(
    int idLote,
    List<String> linhasTraduzidas,
    boolean sucesso,
    String mensagemErro,
    List<String> mascaradosSegundaOpiniao,
    boolean recusaDaRequisicao
) {

    /**
     * PROPÓSITO DE NEGÓCIO: garante que a lista de segunda opinião nunca seja nula nem
     * mutável — quem consome decide o que cachear com base nela, e um {@code null} ali
     * viraria {@code NullPointerException} no meio da gravação do cache.
     */
    public TraducaoLote {
        mascaradosSegundaOpiniao = mascaradosSegundaOpiniao == null
            ? List.of() : List.copyOf(mascaradosSegundaOpiniao);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: forma com segunda opinião declarada, sem precisar classificar o
     * motivo da falha — é o caminho de sucesso, onde não há falha a classificar.
     */
    public TraducaoLote(int idLote, List<String> linhasTraduzidas, boolean sucesso, String mensagemErro,
            List<String> mascaradosSegundaOpiniao) {
        this(idLote, linhasTraduzidas, sucesso, mensagemErro, mascaradosSegundaOpiniao, false);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: forma normal, para o caminho em que TODA a tradução veio do
     * modelo principal — que é a esmagadora maioria dos lotes.
     *
     * <p>Existe para que acrescentar a segunda opinião não obrigasse a reescrever os dez
     * pontos de construção espalhados por produção e testes: quem não sabe da segunda
     * opinião continua construindo como sempre, e recebe lista vazia.
     *
     * <p>Falha construída por aqui NÃO é recusa da requisição: quem não sabe distinguir cai no
     * tratamento conservador de indisponibilidade, que aborta. O caminho que ganha uma fala
     * pendente é sempre o EXPLÍCITO, nunca o omitido.
     */
    public TraducaoLote(int idLote, List<String> linhasTraduzidas, boolean sucesso, String mensagemErro) {
        this(idLote, linhasTraduzidas, sucesso, mensagemErro, List.of(), false);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: falha em que o servidor RESPONDEU e recusou este pedido — o
     * adaptador é o único que enxerga o código HTTP, e é aqui que essa informação entra no
     * domínio.
     *
     * <p>INVARIANTES DO DOMÍNIO: sempre {@code sucesso == false} e sem linhas traduzidas;
     * pedir uma "recusa bem-sucedida" é contradição e não tem construtor.
     */
    public static TraducaoLote recusadaPeloServidor(int idLote, String mensagemErro) {
        return new TraducaoLote(idLote, null, false, mensagemErro, List.of(), true);
    }
}
