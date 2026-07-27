package org.traducao.projeto.correcaoLegendas.domain;

/**
 * PROPÓSITO DE NEGÓCIO: o resumo de UMA execução da correção de legendas — o que foi feito, em
 * quanto tempo e quando. É o bloco {@code operacao} do relatório JSON que o operador abre.
 *
 * <p>Existe para tirar {@code telemetria.OperacaoTelemetria} de dentro do {@code domain}. O record
 * de relatório carregava aquele tipo direto, o que violava DUAS regras ao mesmo tempo — domínio
 * deixa de ser puro, e uma fatia passa a depender de outra. As demais dívidas desta área eram só
 * de camada; esta era a única que contaminava o domínio.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Os nomes dos componentes são IDÊNTICOS aos de {@code OperacaoTelemetria}, e de propósito:
 *       o Jackson serializa record pelo nome do componente, então o JSON gravado em disco fica
 *       byte a byte igual ao de antes. Renomear qualquer um destes campos muda o formato de um
 *       artefato que já existe no acervo do operador — não há leitor no projeto, mas há histórico
 *       e quem inspeciona os arquivos à mão.</li>
 *   <li>{@code registradoEm} é gravado UMA vez, por quem executa. O JSON do módulo e o registro
 *       do painel têm de mostrar o MESMO instante: hoje é o mesmo objeto que vai aos dois lugares,
 *       e recriar o carimbo no adaptador faria os dois divergirem por milissegundos sem que
 *       ninguém percebesse.</li>
 *   <li>Puro: só tipos do JDK. Nenhum framework, nenhum Jackson, nenhuma outra fatia.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Record imutável, sem I/O e sem validação: é um transporte de valores já apurados.
 *
 * @param tipo nome da operação como aparece no painel
 * @param detalhe resumo curto e legível do que foi processado
 * @param tempoTotalMs duração total da execução
 * @param arquivosProcessados arquivos de legenda analisados
 * @param itensDetectados itens que precisavam de correção
 * @param itensCorrigidos itens efetivamente corrigidos
 * @param registradoEm instante da execução, em ISO-8601
 */
public record ResumoOperacaoCorrecaoLegendas(
    String tipo,
    String detalhe,
    Long tempoTotalMs,
    Integer arquivosProcessados,
    Integer itensDetectados,
    Integer itensCorrigidos,
    String registradoEm
) {}
