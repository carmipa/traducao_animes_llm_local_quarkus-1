package org.traducao.projeto.llm.domain;

import java.util.List;
import java.util.Optional;

/**
 * PROPÓSITO DE NEGÓCIO: contrato genérico de saída para o modelo de linguagem local
 * (servido, por exemplo, via LM Studio). É a porta pela qual qualquer fatia funcional
 * pede tradução de um lote, revisão de concordância, correção de uma fala ou a checagem
 * de disponibilidade do servidor — sem conhecer o cliente HTTP concreto nem o modelo.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Contrato puro de domínio: depende apenas de JDK e dos tipos do próprio peer
 *       {@code llm} ({@link Lote}, {@link TraducaoLote}, {@link StatusLlm}); não conhece
 *       framework, HTTP, contexto nem qualquer fatia funcional.</li>
 *   <li>A tradução opera sobre um {@link Lote} e devolve um {@link TraducaoLote} que
 *       preserva o {@code idLote}; as variantes de temperatura e de prompt congelado
 *       apenas refinam a mesma operação.</li>
 *   <li>As revisões pontuais retornam {@link Optional}, distinguindo "sem correção
 *       aplicável" de uma string vazia.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * A implementação concreta ({@code traducao.infrastructure.adapters.LlmClientAdapter})
 * define o tratamento de rede/timeout. No contrato, {@code revisarConcordancia} e
 * {@code corrigirTraducao} devolvem {@link Optional#empty()} quando o LLM falha ou a
 * resposta é inválida, de modo que o chamador preserve a tradução anterior.
 */
public interface LlmPort {

    /**
     * PROPÓSITO DE NEGÓCIO: traduz um lote de falas usando a temperatura configurada e o
     * prompt do contexto ativo.
     * <p>INVARIANTES DO DOMÍNIO: o resultado preserva o {@code idLote} do lote recebido.
     * <p>COMPORTAMENTO EM CASO DE FALHA: a falha é sinalizada pelo {@link TraducaoLote}
     * retornado ({@code sucesso}/{@code mensagemErro}), conforme a implementação concreta.
     *
     * @param lote lote de linhas originais a traduzir
     * @return o lote traduzido, com o mesmo {@code idLote}
     */
    TraducaoLote traduzir(Lote lote);

    /**
     * PROPÓSITO DE NEGÓCIO: variante com temperatura explícita, usada nas retentativas de
     * uma fala isolada — repetir a MESMA requisição com a mesma temperatura tende a
     * reproduzir a mesma alucinação; subir a temperatura muda a amostragem e dá chance
     * real de recuperação.
     * <p>INVARIANTES DO DOMÍNIO: {@code temperaturaOverride} nulo usa a temperatura
     * configurada; a implementação default delega a {@link #traduzir(Lote)}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: idêntico a {@link #traduzir(Lote)}.
     *
     * @param lote lote de linhas originais a traduzir
     * @param temperaturaOverride temperatura de amostragem, ou {@code null} para a configurada
     * @return o lote traduzido, com o mesmo {@code idLote}
     */
    default TraducaoLote traduzir(Lote lote, Double temperaturaOverride) {
        return traduzir(lote);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: variante que recebe o prompt de sistema CONGELADO no início do
     * job, para que uma troca de contexto (lore) no estado global não vaze para o meio da
     * tradução de um episódio.
     * <p>INVARIANTES DO DOMÍNIO: {@code promptSistemaCongelado} nulo usa o prompt do
     * contexto ativo; a implementação default delega a {@link #traduzir(Lote, Double)}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: idêntico a {@link #traduzir(Lote)}.
     *
     * @param lote lote de linhas originais a traduzir
     * @param temperaturaOverride temperatura de amostragem, ou {@code null} para a configurada
     * @param promptSistemaCongelado prompt de sistema fixado no início do job, ou {@code null}
     * @return o lote traduzido, com o mesmo {@code idLote}
     */
    default TraducaoLote traduzir(Lote lote, Double temperaturaOverride, String promptSistemaCongelado) {
        return traduzir(lote, temperaturaOverride);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: verifica, antes de iniciar a tradução, se o servidor LLM local
     * está online e se o modelo configurado está efetivamente carregado — evita descobrir
     * isso só depois de várias tentativas/timeouts já no meio do primeiro episódio.
     * <p>INVARIANTES DO DOMÍNIO: o resultado reflete o estado observado no momento da
     * chamada (servidor online e modelo carregado são sinais independentes).
     * <p>COMPORTAMENTO EM CASO DE FALHA: o servidor indisponível é reportado no
     * {@link StatusLlm} retornado, não por exceção no contrato.
     *
     * @return o estado de disponibilidade do servidor e do modelo
     */
    StatusLlm verificarDisponibilidade();

    /**
     * PROPÓSITO DE NEGÓCIO: revisa uma fala já traduzida, corrigindo concordância de
     * gênero/pronomes sem retraduzir do zero.
     * <p>INVARIANTES DO DOMÍNIO: opera sobre o texto mascarado (marcadores de tag
     * preservados); os problemas detectados guiam a revisão.
     * <p>COMPORTAMENTO EM CASO DE FALHA: retorna {@link Optional#empty()} se o LLM falhar
     * ou a resposta for inválida, preservando a tradução anterior.
     *
     * @param originalInglesMascarado original em inglês, com marcadores de tag
     * @param traducaoPtMascarada tradução PT-BR atual, com marcadores de tag
     * @param problemasDetectados problemas de concordância a corrigir
     * @return a fala revisada, ou vazio quando não há correção aplicável
     */
    Optional<String> revisarConcordancia(
        String originalInglesMascarado,
        String traducaoPtMascarada,
        List<String> problemasDetectados
    );

    /**
     * PROPÓSITO DE NEGÓCIO: retraduz uma fala cuja tradução existente ficou com resíduo em
     * inglês, incompleta ou alucinada, usando o prompt completo (lore + regras) do contexto
     * ativo.
     * <p>INVARIANTES DO DOMÍNIO: opera sobre o texto mascarado; o motivo detectado orienta
     * a correção.
     * <p>COMPORTAMENTO EM CASO DE FALHA: retorna {@link Optional#empty()} se o LLM falhar
     * ou a resposta for inválida, preservando a tradução anterior.
     *
     * @param originalInglesMascarado original em inglês, com marcadores de tag
     * @param traducaoPtMascarada tradução PT-BR atual, com marcadores de tag
     * @param motivoDetectado motivo que disparou a correção
     * @return a fala corrigida, ou vazio quando não há correção aplicável
     */
    Optional<String> corrigirTraducao(
        String originalInglesMascarado,
        String traducaoPtMascarada,
        String motivoDetectado
    );
}
