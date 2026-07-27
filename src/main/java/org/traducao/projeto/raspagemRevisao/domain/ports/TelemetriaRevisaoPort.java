package org.traducao.projeto.raspagemRevisao.domain.ports;

import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: publica o desfecho das revisões desta fatia — telemetria, log e relatório
 * persistido. É a porta da FASE 2 do Plano-Mestre; antes dela, três casos de uso injetavam
 * {@code telemetria.TelemetriaService} direto na camada de aplicação.
 *
 * <h2>Por que TRÊS operações, e não uma</h2>
 * Os três consumidores usam a telemetria de formas realmente distintas, e colapsar tudo numa
 * assinatura só obrigaria dois deles a passar argumentos falsos:
 * <ul>
 *   <li>{@link #registrarComRelatorio} — revisão de cache e de legendas produzem um RELATÓRIO
 *       que vai para disco;</li>
 *   <li>{@link #registrar} — a revisão PT-only publica só os totais, sem relatório;</li>
 *   <li>{@link #pastaDeRelatorios} — consulta, não publicação: a revisão de legendas informa ao
 *       operador ONDE o relatório foi salvo.</li>
 * </ul>
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>NENHUM tipo da fatia {@code telemetria} aparece nestas assinaturas: a aplicação fala o
 *       vocabulário do próprio domínio e o adaptador traduz. É isso que permite testar a
 *       aplicação sem a fatia de telemetria existir.</li>
 *   <li>Uma porta POR FATIA. O contrato proíbe fatia depender de fatia e proíbe {@code shared/}
 *       funcional, então portas quase idênticas em fatias diferentes são a forma prescrita —
 *       duplicação consciente, o preço declarado de manter cada fatia removível.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Implementações ABSORVEM falha de I/O: perder telemetria nunca pode interromper uma revisão nem
 * transformar um lote bem-sucedido em falha.
 */
public interface TelemetriaRevisaoPort {

    /**
     * PROPÓSITO DE NEGÓCIO: registra a operação e PERSISTE o relatório legível.
     * <p>INVARIANTES DO DOMÍNIO: uma vez por execução, no fim; os números são os mesmos que o
     * caso de uso devolve ao chamador.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     *
     * @param operacao nome da operação, como aparece na telemetria
     * @param detalhe resumo curto do desfecho
     * @param prefixoRelatorio prefixo do arquivo de relatório persistido
     * @param pastaAlvo pasta sobre a qual a operação rodou
     * @param duracaoMs duração total
     * @param arquivosProcessados arquivos analisados
     * @param itensDetectados itens que precisavam de tratamento
     * @param itensCorrigidos itens efetivamente tratados
     * @param relatorio texto completo do relatório, já formatado
     */
    void registrarComRelatorio(
        String operacao,
        String detalhe,
        String prefixoRelatorio,
        Path pastaAlvo,
        long duracaoMs,
        int arquivosProcessados,
        int itensDetectados,
        int itensCorrigidos,
        String relatorio);

    /**
     * PROPÓSITO DE NEGÓCIO: registra a operação SEM relatório em disco — só os totais.
     * <p>INVARIANTES DO DOMÍNIO: mesma semântica dos totais da variante com relatório.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     *
     * @param operacao nome da operação
     * @param detalhe resumo curto do desfecho
     * @param duracaoMs duração total
     * @param arquivosProcessados arquivos analisados
     * @param itensDetectados itens que precisavam de tratamento
     * @param itensCorrigidos itens efetivamente tratados
     */
    void registrar(
        String operacao,
        String detalhe,
        long duracaoMs,
        int arquivosProcessados,
        int itensDetectados,
        int itensCorrigidos);

    /**
     * PROPÓSITO DE NEGÓCIO: informa ONDE o relatório desta execução foi salvo, para o caso de uso
     * dizer isso ao operador.
     *
     * <p>INVARIANTES DO DOMÍNIO: é CONSULTA, não publicação — não escreve nada. Está nesta porta
     * porque quem sabe a convenção de pastas de relatório é o adaptador, não a aplicação.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança; na dúvida devolve a própria pasta de entrada.
     *
     * @param pastaEntrada pasta sobre a qual a operação rodou
     * @return pasta onde os relatórios daquela entrada são gravados
     */
    Path pastaDeRelatorios(Path pastaEntrada);
}
