package org.traducao.projeto.traducaoKaraoke.domain;

/**
 * PROPÓSITO DE NEGÓCIO: registra, no manifesto, o arquivo que estourou e o porquê — para que um
 * arquivo quebrado deixe de simplesmente SUMIR do relatório.
 *
 * <h2>O prejuízo que originou</h2>
 * Até 2026-08-14 o laço de {@code TraduzirKaraokeUseCase} contava {@code falhas++} e publicava
 * uma linha no console, mas o manifesto listava apenas os resultados. Um lote de 25 legendas com
 * 2 falhas produzia manifesto com 23 entradas e <b>nenhum vestígio</b> das outras duas: quem
 * auditasse depois veria 23 e concluiria que 23 era o total. Contagem certa de um conjunto errado
 * é exatamente a armadilha que a regra da medição registra.
 *
 * <h2>Invariantes do domínio</h2>
 * O motivo é o texto real da exceção. Nunca "erro desconhecido" — mensagem genérica manda o
 * próximo leitor recomeçar a investigação do zero.
 *
 * @param arquivo nome do arquivo de legenda que falhou
 * @param motivo  mensagem da exceção que interrompeu aquele arquivo
 */
public record FalhaArquivoKaraoke(String arquivo, String motivo) {
}
