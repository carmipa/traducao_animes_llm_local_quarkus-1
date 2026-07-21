package org.traducao.projeto.traducao.domain.contextocena;

/**
 * PROPÓSITO DE NEGÓCIO: uma linha de MEDIÇÃO do experimento A/B da correção de gênero por
 * contexto de cena — o que uma execução de um episódio produziu, marcada pelo braço
 * ({@code A_BASELINE} = pipeline de hoje, flag DESLIGADA; {@code B_CONTEXTO} = motor
 * contextual, flag LIGADA). É o insumo do relatório append-only que a telemetria canônica
 * NÃO consegue guardar (ela deduplica por episódio, então o braço B sobrescreveria o A). Com
 * as duas linhas do MESMO episódio lado a lado, Paulo compara ATIVIDADE e CUSTO dos dois braços;
 * o acerto de gênero em si é medido à parte, pela régua sobre as saídas.
 *
 * <p>INVARIANTES DO DOMÍNIO: {@code record} imutável, só JDK (sem framework/Jackson) — é
 * domínio puro. Mede a contabilidade PRÓPRIA de cada braço, não uma comparação apples-to-apples
 * por categoria: no braço A, {@code traduzidasNovas}/{@code reaproveitadasCache}/{@code pendentes}
 * somam TODAS as categorias (a via de hoje não separa diálogo); no braço B refletem o diálogo
 * tratado por contexto mais o resto pela via de hoje. O sinal diretamente comparável entre os
 * braços é {@code caracteresContextoExtra} (A é sempre 0; B carrega o custo do contexto enviado),
 * além de {@code pendentes}, {@code tempoMs} e {@code status}. Não afirma acerto de gênero.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: portador de dados puro; não valida nem lança. O carimbo de
 * execução (runId, instante) é responsabilidade do adaptador que grava, não desta medição.
 *
 * @param episodio nome do arquivo de episódio medido
 * @param variante braço do experimento: {@code A_BASELINE} ou {@code B_CONTEXTO}
 * @param politicaVersao {@code baseline} no braço A; {@code contexto-cena:<versao>} no braço B
 * @param modelo identificador do modelo LLM que produziu a execução
 * @param diretorioCache raiz de cache usada (identidade do isolamento A/B)
 * @param linhasTraduziveis total de falas elegíveis à tradução no episódio
 * @param traduzidasNovas falas novas validadas nesta execução (contagem própria do braço)
 * @param reaproveitadasCache falas servidas do cache nesta execução (contagem própria do braço)
 * @param pendentes falas mantidas no original (alucinação de tags ou reprova de validação)
 * @param caracteresContextoExtra custo de contexto enviado como referência (0 no braço A)
 * @param tempoMs duração total da execução do episódio em milissegundos
 * @param status desfecho do episódio ({@code CONCLUIDO} ou {@code PARCIAL})
 */
public record RegistroExecucaoContextoCena(
    String episodio,
    String variante,
    String politicaVersao,
    String modelo,
    String diretorioCache,
    int linhasTraduziveis,
    int traduzidasNovas,
    int reaproveitadasCache,
    int pendentes,
    long caracteresContextoExtra,
    long tempoMs,
    String status
) {

    /** Braço de controle: pipeline atual com a correção contextual DESLIGADA. */
    public static final String VARIANTE_BASELINE = "A_BASELINE";

    /** Braço de tratamento: motor de correção de gênero por contexto de cena LIGADO. */
    public static final String VARIANTE_CONTEXTO = "B_CONTEXTO";
}
