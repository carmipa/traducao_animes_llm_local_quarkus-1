package org.traducao.projeto.trocaTipoLegenda.domain.ports;

import org.traducao.projeto.legenda.domain.DocumentoLegenda;

import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: leitura e escrita de arquivos de legenda para a fatia
 * {@code trocaTipoLegenda}, sem que os casos de uso conheçam o parser concreto.
 *
 * <p>Antes, {@code AchatarEstilosUseCase} e {@code TrocaTipoLegendaUseCase} recebiam
 * {@code LeitorLegendaAss}/{@code EscritorLegendaAss} do peer {@code legenda} direto no
 * construtor: I/O concreto dentro do {@code application}, e nenhum teste de regra podia
 * rodar sem tocar disco.
 *
 * <p>{@link DocumentoLegenda} atravessa de propósito — é o formato ASS em si, o dado que
 * trafega, não regra de decisão.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@link #ler(Path)} não altera o arquivo; {@link #escrever(Path, DocumentoLegenda)}
 *       substitui o conteúdo do caminho dado.</li>
 *   <li>Quem chama {@code escrever} é responsável pelo backup prévio — a porta não decide
 *       política de preservação.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Falha de I/O ou de parsing propaga como exceção não verificada da implementação; o
 * laço do caso de uso a captura por arquivo, sem abortar o lote.
 */
public interface LegendaIoPort {

    /**
     * PROPÓSITO DE NEGÓCIO: carrega uma legenda do disco no modelo de domínio.
     *
     * <p>INVARIANTES DO DOMÍNIO: leitura pura; o arquivo permanece intacto.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga a exceção da implementação.
     */
    DocumentoLegenda ler(Path arquivo);

    /**
     * PROPÓSITO DE NEGÓCIO: grava a legenda transformada no caminho indicado.
     *
     * <p>INVARIANTES DO DOMÍNIO: preserva quebra de linha e BOM conforme o documento;
     * NÃO faz backup — essa decisão é do caso de uso.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga a exceção da implementação.
     */
    void escrever(Path arquivo, DocumentoLegenda documento);
}
