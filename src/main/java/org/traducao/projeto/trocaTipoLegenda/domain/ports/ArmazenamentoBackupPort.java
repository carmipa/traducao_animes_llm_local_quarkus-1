package org.traducao.projeto.trocaTipoLegenda.domain.ports;

import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: guarda a cópia intacta de cada legenda ANTES de gravá-la
 * alterada, tornando toda operação da fatia reversível — e sem que o
 * {@code application} saiba onde fica a raiz de backups no disco.
 *
 * <p>Os casos de uso chamavam {@code DiretorioBaseKronos.resolver("backups")} dentro do
 * próprio construtor: caminho de filesystem decidido em regra de negócio. A reversão é a
 * garantia mais importante desta fatia — o achatamento DESCARTA a camada de timing do
 * karaokê, e sem backup essa perda seria definitiva —, então merece uma porta explícita
 * em vez de uma chamada estática escondida.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@link #abrirSessao(String)} cria um destino NOVO por execução; execuções
 *       distintas nunca se sobrescrevem.</li>
 *   <li>{@link #preservar} copia o arquivo como está — nunca move, nunca altera o
 *       original.</li>
 *   <li>Falha ao preservar deve IMPEDIR a gravação daquele arquivo: sem backup, não se
 *       grava.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Impossibilidade de criar a sessão propaga exceção antes de qualquer escrita.
 */
public interface ArmazenamentoBackupPort {

    /**
     * PROPÓSITO DE NEGÓCIO: abre um destino de backup exclusivo desta execução.
     *
     * <p>INVARIANTES DO DOMÍNIO: o destino existe ao retornar; é novo a cada chamada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga exceção — sem destino não há operação.
     *
     * @param rotuloOperacao identifica a operação no nome do destino (ex.: "achatar_estilos")
     * @return caminho da sessão, para exibição ao operador
     */
    Path abrirSessao(String rotuloOperacao);

    /**
     * PROPÓSITO DE NEGÓCIO: copia o arquivo original para a sessão aberta, antes de ele
     * ser sobrescrito.
     *
     * <p>INVARIANTES DO DOMÍNIO: o original permanece intocado; sobrescreve cópia
     * homônima na mesma sessão.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga exceção, e o chamador NÃO deve gravar
     * o arquivo alterado.
     */
    void preservar(Path sessao, Path arquivoOriginal);
}
