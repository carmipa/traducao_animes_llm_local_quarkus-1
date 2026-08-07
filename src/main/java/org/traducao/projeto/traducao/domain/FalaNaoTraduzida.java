package org.traducao.projeto.traducao.domain;

/**
 * PROPÓSITO DE NEGÓCIO: registra, fala a fala, POR QUE uma linha da legenda saiu igual à
 * origem. É a resposta para a única pergunta que a auditoria de um arquivo traduzido realmente
 * faz — "por que esta fala está em inglês?" — sem que ninguém precise recalcular por fora as
 * regras que o pipeline já aplicou por dentro.
 *
 * <h2>O prejuízo que originou</h2>
 * Na auditoria de 07/08/2026, CINCO conclusões erradas saíram de recalcular por fora:
 * <ul>
 *   <li>18.431 falas dadas como resíduo de tradução — eram letra de música cujo estilo o
 *       achatamento apagou;</li>
 *   <li>2.898 dadas como perdidas no Gundam Unicorn — eram o karaokê {@code OPL2} descartado de
 *       propósito, para não virar 138 legendas piscando uma palavra por vez;</li>
 *   <li>364 "defeitos" no mesmo Unicorn — eram estilos que o {@code application.yml} manda
 *       ignorar.</li>
 * </ul>
 * O carimbo do cabeçalho (mesma data) passou a dar os TOTAIS. Faltava o detalhe: <b>quais</b>
 * falas, e por quê.
 *
 * <h2>Por que só as NÃO traduzidas</h2>
 * O acervo tem 1,7 milhão de falas. Gravar uma linha por fala produziria um dataset que
 * ninguém abre e que custa disco a cada execução. A fala traduzida é o caso normal e o próprio
 * {@code .ass} a mostra; o que não tem resposta em lugar nenhum é a fala que ficou igual.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O {@code instante} é a chave estável: o texto muda ao traduzir, o instante não. É por
 *       ele que uma auditoria externa casa este registro com a linha do {@code .ass} — casar por
 *       índice mente quando o arquivo sai reordenado.</li>
 *   <li>O {@code estiloOriginal} é o da ORIGEM, nunca o da saída: o achatador colapsa
 *       {@code OP}/{@code ED}/{@code Other songs} em {@code Default}, e ler o estilo da saída
 *       faria letra de música parecer diálogo.</li>
 *   <li>{@link Motivo} é fechado. "Não sei por quê" não é um valor — se aparecer caso novo,
 *       ele ganha constante, porque motivo desconhecido silenciosamente agrupado no genérico é
 *       exatamente o tipo de cegueira que este registro existe para acabar.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Record imutável, sem I/O e sem validação: quem monta é o use case, que já tem os dados em
 * mãos. Nunca lança.
 *
 * @param indice índice do evento no arquivo de origem, para leitura humana
 * @param instante {@code inicio,fim} do evento — a chave estável de casamento
 * @param estiloOriginal estilo declarado na ORIGEM, antes de qualquer achatamento
 * @param motivo por que a fala não foi traduzida
 * @param detalhe informação livre do motivo (causa-raiz da pendência, nome da regra); pode ser
 *        vazio, nunca {@code null} por contrato de quem monta
 */
public record FalaNaoTraduzida(
    int indice,
    String instante,
    String estiloOriginal,
    Motivo motivo,
    String detalhe
) {

    /**
     * PROPÓSITO DE NEGÓCIO: as razões pelas quais uma fala sai igual à origem. Conjunto FECHADO
     * — cada valor corresponde a uma decisão real do pipeline, e nenhum significa "não sei".
     */
    public enum Motivo {
        /**
         * O seletor não a considerou traduzível: estilo musical, karaokê, romaji protegido ou
         * estilo em {@code tradutor.estilos-ignorados}. É a maior fatia e é comportamento
         * CORRETO — foi confundida com defeito 18.431 vezes numa auditoria só.
         */
        PRESERVADA_POR_REGRA,

        /**
         * Foi ao LLM e voltou inaproveitável — marcador {@code [[TAGn]]} corrompido, eco do
         * original, estrutura divergente. O original é mantido de propósito: publicar a
         * resposta ruim corromperia a legenda. O arquivo sai como {@code .parcial}.
         */
        PENDENTE,

        /**
         * O LLM devolveu tradução que, depois de validada, era idêntica ao original. Acontece
         * legitimamente em nome próprio ({@code "Rygart!"}) e em fala de uma palavra.
         */
        TRADUCAO_IGUAL_AO_ORIGINAL
    }
}
