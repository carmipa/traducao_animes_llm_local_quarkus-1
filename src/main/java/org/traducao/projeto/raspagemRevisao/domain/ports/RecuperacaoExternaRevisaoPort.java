package org.traducao.projeto.raspagemRevisao.domain.ports;

import org.traducao.projeto.raspagemRevisao.domain.ResultadoRecuperacaoExterna;

/**
 * PROPÓSITO DE NEGÓCIO: contrato DESTA fatia para recuperar uma fala num tradutor de máquina
 * externo durante a revisão de legendas. Antes desta porta, o caso de uso injetava a classe
 * concreta {@code raspagemCorrecao.infrastructure.GoogleTranslateScraper}: uma camada de aplicação
 * dependendo de {@code infrastructure} — e ainda por cima de OUTRA fatia, as duas coisas que o
 * contrato proíbe.
 *
 * <h2>O que esta porta fecha e o que NÃO fecha</h2>
 * Fecha a violação de CAMADA: a aplicação da revisão não conhece mais nenhuma infraestrutura, nem
 * a própria nem a alheia, e passa a ser testável com um dublê de três linhas.
 *
 * <p>NÃO fecha a dependência entre FATIAS. O adaptador desta porta ainda consome a porta de
 * {@code raspagemCorrecao}, porque as 239 linhas do scraper são um contorno contra um endpoint
 * não documentado do Google, e uma terceira cópia byte-idêntica seria o pior tipo de duplicação:
 * as DUAS cópias que já existem (o scraper e o {@code GoogleFallbackAdapter} da fatia gold) já
 * divergiram em produção. Quem for ler a catraca verde de infra cruzada não deve concluir que a
 * questão de dono acabou — ela foi movida para um lugar honesto (contrato publicado em vez de
 * classe concreta) e continua aberta, esperando a decisão "dono ou peer", que merece plano próprio.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Traduz UMA fala por chamada, já mascarada pelo chamador.</li>
 *   <li>NÃO faz pausa. Quem chama continua responsável pelo ritmo — o caso de uso já chama
 *       {@code pausaGoogle()} depois de cada fala, e absorver a espera aqui dobraria a pausa sem
 *       ninguém notar, além de tornar a taxa de requisições diferente entre as duas fatias que
 *       falam com o mesmo provedor.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * NUNCA lança. Toda falha vira {@link ResultadoRecuperacaoExterna} de recusa carregando o texto
 * ORIGINAL — a legenda publicada continua intacta.
 */
public interface RecuperacaoExternaRevisaoPort {

    /**
     * PROPÓSITO DE NEGÓCIO: tenta traduzir uma fala preservando tags ASS e quebras {@code \N}.
     *
     * <p>INVARIANTES DO DOMÍNIO: em sucesso, a saída mantém as tags do original; em qualquer
     * recusa, a saída É o original. Não pausa.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve desfecho de recusa com a causa; não lança.
     *
     * @param textoOriginal a fala a traduzir, já mascarada pelo chamador
     * @return o desfecho da tentativa, sempre com texto utilizável e sempre com a causa
     */
    ResultadoRecuperacaoExterna traduzir(String textoOriginal);
}
