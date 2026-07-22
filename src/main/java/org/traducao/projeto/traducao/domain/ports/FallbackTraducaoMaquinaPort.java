package org.traducao.projeto.traducao.domain.ports;

import org.traducao.projeto.traducao.domain.fallback.ResultadoFallback;

/**
 * PROPÓSITO DE NEGÓCIO: contrato da fatia Tradução Local para a recuperação de ÚLTIMO RECURSO
 * de uma única fala que o LLM local não conseguiu traduzir, por um tradutor de MÁQUINA — local
 * (LibreTranslate em container) ou externo (Google). É uma porta PRÓPRIA da fatia: não
 * reaproveita o módulo manual {@code raspagemCorrecao}, cuja fronteira arquitetural é proibida
 * à Tradução Local; cada implementação vive na infraestrutura desta fatia.
 *
 * <p>Sucedeu a {@code FallbackTraducaoOnlinePort}: o nome "online" deixou de descrever a
 * realidade quando o provedor preferencial passou a ser um container LOCAL, e o retorno
 * {@code Optional<String>} colapsava nove desfechos distintos num único "vazio", apagando a
 * razão da recusa.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Recebe só o texto ORIGINAL de uma fala já dada como pendente NESTA execução; nunca
 *       varre cache de execuções anteriores.</li>
 *   <li>Preserva a formatação (tags ASS e quebras {@code \N}): se o provedor corromper qualquer
 *       marcador, a implementação recusa com {@link org.traducao.projeto.traducao.domain.fallback.StatusFallback#MARCADOR_CORROMPIDO}
 *       em vez de devolver legenda com formatação quebrada.</li>
 *   <li>NUNCA devolve resultado sem causa: toda recusa carrega status e motivo legíveis, para
 *       que o chamador registre, contabilize por causa e decida acionar o próximo provedor.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Falha de rede, provedor fora do ar, resposta inválida, eco do original ou marcador corrompido
 * resultam em um {@link ResultadoFallback} de recusa — a fala permanece pendente, exatamente
 * como sem o fallback. Nunca lança para o chamador.
 */
public interface FallbackTraducaoMaquinaPort {

    /**
     * PROPÓSITO DE NEGÓCIO: tenta traduzir uma única fala pendente preservando tags e quebras.
     *
     * <p>INVARIANTES DO DOMÍNIO: quando o resultado é
     * {@link org.traducao.projeto.traducao.domain.fallback.StatusFallback#RECUPERADA}, a saída
     * mantém exatamente as mesmas tags/quebras do original; caso contrário não há texto.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve resultado de recusa com a causa preenchida
     * (rede, HTTP, vazia, eco, marcador); não lança.
     *
     * @param original o texto original (com tags) da fala pendente
     * @return o desfecho da tentativa, com tradução quando recuperada e sempre com a causa
     */
    ResultadoFallback traduzir(String original);

    /**
     * PROPÓSITO DE NEGÓCIO: identifica qual provedor esta implementação representa, para que o
     * orquestrador rotule contadores e logs por provedor sem conhecer as classes concretas.
     * <p>INVARIANTES DO DOMÍNIO: valor fixo por implementação; nunca {@code null}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     *
     * @return o provedor desta implementação
     */
    org.traducao.projeto.traducao.domain.fallback.ProvedorFallback provedor();
}
