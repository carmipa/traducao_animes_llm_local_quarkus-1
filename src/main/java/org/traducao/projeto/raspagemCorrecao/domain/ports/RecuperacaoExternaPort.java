package org.traducao.projeto.raspagemCorrecao.domain.ports;

import org.traducao.projeto.raspagemCorrecao.domain.ResultadoRaspagem;

/**
 * PROPÓSITO DE NEGÓCIO: contrato desta fatia para recuperar uma fala num tradutor de máquina
 * EXTERNO quando a tradução em cache está errada. É a porta que a FASE 2 do Plano-Mestre prescreve
 * para o {@code GoogleTranslateScraper}: até aqui o caso de uso injetava a classe concreta de
 * {@code infrastructure}, violando a regra de camada do contrato ("application depende de
 * domain.ports, nunca de infrastructure") dentro da própria fatia.
 *
 * <p>O Plano-Mestre situava esta porta em {@code traducaoCorrige}. Está errado e foi corrigido
 * aqui: nenhum caso de uso daquela fatia aciona tradutor externo. Os consumidores reais são
 * {@code raspagemCorrecao} e {@code raspagemRevisao}, e pendurar a porta numa terceira fatia só
 * criaria um hub artificial de que as duas passariam a depender.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Traduz UMA fala por chamada, já mascarada pelo chamador. A porta não conhece lore, tags
 *       nem cache — recebe texto e devolve desfecho.</li>
 *   <li>NÃO faz pausa entre chamadas. O ritmo é do chamador, que é quem sabe quantas falas vai
 *       pedir em sequência: as duas fatias já pausam por conta ({@code Thread.sleep(400)} na
 *       correção, {@code pausaGoogle()} na revisão). Absorver a pausa aqui dobraria a espera de
 *       quem já pausa e mudaria silenciosamente a taxa efetiva de requisições — que é a única
 *       coisa que separa esta operação de um bloqueio por IP.</li>
 *   <li>O retry de falha TRANSITÓRIA é da implementação, não do chamador: é interno a uma única
 *       tentativa lógica e não altera o desfecho visível.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * NUNCA lança. Rede fora, HTTP de erro, JSON inesperado ou marcador de tag mutilado viram
 * {@link ResultadoRaspagem} de recusa carregando o texto ORIGINAL — a fala continua intacta e o
 * chamador sabe o motivo.
 */
public interface RecuperacaoExternaPort {

    /**
     * PROPÓSITO DE NEGÓCIO: tenta traduzir uma fala preservando tags ASS e quebras {@code \N}.
     *
     * <p>INVARIANTES DO DOMÍNIO: em sucesso, a saída mantém as mesmas tags do original; em
     * qualquer recusa, a saída É o original. Não pausa.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve desfecho de recusa com a causa; não lança.
     *
     * @param textoOriginal a fala a traduzir, já mascarada pelo chamador
     * @return o desfecho da tentativa, sempre com texto utilizável e sempre com a causa
     */
    ResultadoRaspagem traduzir(String textoOriginal);
}
