package org.traducao.projeto.novoKaraoke.domain.ports;

import org.traducao.projeto.novoKaraoke.domain.MedicaoEstiloKaraoke;

import java.nio.file.Path;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: publica o que a simplificação de karaokê fez, com recorte suficiente para
 * localizar um defeito — arquivo e estilo, não só total. Existe porque os defeitos desta fatia são
 * SILENCIOSOS por natureza: ela não falha, ela deixa de agir.
 *
 * <h2>O caso que motivou esta porta</h2>
 * Em 2026-08-03 mediu-se que 10 dos 23 episódios do Guilty Crown atravessavam a simplificação sem
 * fundir UM evento — 392 entrando, 392 saindo — porque a régua de nome musical usava {@code \b} e
 * {@code OP_S2}/{@code ED_S2} não casavam. Nenhum erro foi lançado, nenhum aviso emitido, nenhum
 * contador mexeu. Descobrir exigiu varrer o disco com script; o defeito viveu nove dias.
 *
 * <p>A fatia já emitia diagnóstico — {@code [AVISO] Estilo "OP_S2": 1 eventos sem linha
 * correspondente} —, mas como TEXTO em log: não agrega, não entra em relatório e some na rotação
 * do arquivo. Diagnóstico que não acumula não vira medição.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>NENHUM tipo da fatia {@code telemetria} aparece nesta assinatura. A aplicação fala o
 *       vocabulário do próprio domínio; traduzir é trabalho do adaptador. É o que permite testar
 *       a simplificação sem a fatia de telemetria existir.</li>
 *   <li>Unidirecional: publicar não devolve nada e não pode virar fonte de decisão. Uma medição
 *       que influencia o resultado deixa de medir e passa a participar.</li>
 *   <li>O ENSAIO publica igual: é o que torna a simulação uma previsão fiel em vez de estimativa.
 *       Quem distingue é o nome da operação, não a ausência de dado.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Implementações ABSORVEM falha de I/O. Perder uma linha de telemetria nunca pode interromper a
 * conversão nem propagar exceção — seria trocar problema de observabilidade por problema de
 * integridade sobre o acervo.
 */
public interface TelemetriaKaraokePort {

    /**
     * PROPÓSITO DE NEGÓCIO: registra o desfecho de UM arquivo, com a quebra por estilo que
     * permite dizer ONDE o problema está.
     *
     * <p>INVARIANTES DO DOMÍNIO: chamado uma vez por arquivo processado, inclusive quando nada
     * mudou — arquivo com {@code eventosSaida == eventosEntrada} é justamente o sintoma que
     * precisa aparecer, não o que deve ser omitido por ser "sem novidade".
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     *
     * @param arquivo nome do arquivo de legenda processado
     * @param eventosEntrada eventos lidos
     * @param eventosSaida eventos gravados (ou que seriam gravados, em ensaio)
     * @param camadasPareadas versos em que duas camadas foram mantidas juntas
     * @param camadasInvertidas destes, quantos ficaram com a tradução ACIMA do original
     * @param porEstilo o recorte que localiza o defeito
     */
    void publicarArquivo(
        String arquivo,
        int eventosEntrada,
        int eventosSaida,
        int camadasPareadas,
        int camadasInvertidas,
        List<MedicaoEstiloKaraoke> porEstilo);

    /**
     * PROPÓSITO DE NEGÓCIO: fecha a execução, agregando o que foi publicado por arquivo.
     *
     * <p>INVARIANTES DO DOMÍNIO: chamado UMA vez, no fim; os números são os mesmos devolvidos ao
     * chamador, para relatório e retorno nunca discordarem.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     *
     * @param operacao nome legível, distinguindo ensaio de aplicação
     * @param pastaOrigem pasta lida
     * @param pastaDestino pasta gravada (a mesma em ensaio, onde nada é escrito)
     * @param duracaoMs duração total
     * @param arquivosProcessados quantos arquivos passaram
     */
    void publicarOperacao(
        String operacao,
        Path pastaOrigem,
        Path pastaDestino,
        long duracaoMs,
        int arquivosProcessados);
}
