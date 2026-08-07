package org.traducao.projeto.telemetria;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: publica eventos de telemetria num fluxo AO VIVO, para
 * que a interface e eventuais consumidores externos vejam a execução acontecendo
 * em vez de esperarem o arquivo fechar no fim do episódio.
 *
 * <h2>Por que é um acréscimo, e não uma troca</h2>
 * O {@code telemetria_execucoes.jsonl} continua sendo a FONTE do dataset. É
 * append em disco, sobrevive a {@code kill -9} e é lido direto por qualquer
 * ferramenta de análise. Um fluxo em memória com persistência assíncrona perde
 * os últimos milissegundos — e, para telemetria destinada a virar dataset,
 * perder evento é perder dado.
 *
 * <p>O que o fluxo acrescenta é o que o arquivo não dá: granularidade por evento
 * (o arquivo só grava ao concluir o episódio, e um episódio são milhares de
 * lotes) e leitura por posição, que fecha o buraco de o console perder todo o
 * histórico num F5.
 *
 * <h2>INVARIANTES DO DOMÍNIO</h2>
 * <ul>
 *   <li><b>Telemetria nunca segura o pipeline.</b> Publicar é best-effort: falha
 *       de publicação não interrompe, não repete e não propaga.</li>
 *   <li>Fluxo indisponível deixa o KRONOS funcionando por inteiro. A degradação
 *       é a garantia central, não um detalhe de implementação.</li>
 * </ul>
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: {@link #publicar} engole qualquer erro e
 * segue; {@link #status} reporta a indisponibilidade de forma explícita, para
 * que "não há evento" e "não consigo falar com o fluxo" nunca se pareçam.
 */
public interface FluxoTelemetriaPort {

    /**
     * PROPÓSITO DE NEGÓCIO: registra um evento no fluxo ao vivo.
     *
     * <p>INVARIANTES DO DOMÍNIO: é best-effort e não bloqueante do ponto de vista
     * do domínio — o chamador nunca precisa tratar falha.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança, não repete, não avisa o
     * chamador. O evento se perde e a execução segue, porque a alternativa seria
     * uma tradução de horas parar por causa de um registro.
     *
     * @param tipo rótulo curto do evento (ex.: {@code "episodio-concluido"})
     * @param campos pares chave/valor já sanitizados para publicação
     */
    void publicar(String tipo, Map<String, String> campos);

    /**
     * PROPÓSITO DE NEGÓCIO: informa se o fluxo está de pé, para a interface poder
     * mostrar o estado real em vez de silêncio.
     *
     * <p>INVARIANTES DO DOMÍNIO: a consulta é barata e nunca lança — pode ser
     * feita a cada carga de página.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve estado desconectado com o motivo
     * legível, jamais uma exceção.
     */
    StatusFluxoTelemetria status();
}
