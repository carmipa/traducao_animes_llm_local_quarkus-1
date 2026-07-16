package org.traducao.projeto.auditorConteudoLegendas.domain;

import org.traducao.projeto.legenda.domain.DocumentoLegenda;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: contrato das regras que auditam UM único arquivo de
 * legenda (só original ou só traduzido), sem depender de um par de comparação.
 * Sustenta as abas "Só Original" e "Só Traduzida" do painel de Análise de
 * Conteúdo, onde não existe artefato de referência.
 *
 * <p>INVARIANTES DO DOMÍNIO: implementações são de responsabilidade única e não
 * alteram o documento recebido; a auditoria é 100% leitura. Estas regras vivem
 * numa hierarquia separada da comparativa {@link RegraAuditoriaConteudo} para
 * que os dois conjuntos sejam injetados e contados de forma independente.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: uma regra que não consiga avaliar um evento
 * (ex.: timestamp ilegível) deve ignorá-lo silenciosamente e nunca lançar; a
 * ausência de anomalias é representada por lista vazia.
 */
public interface RegraAuditoriaArquivoUnico {
    String getNome();
    List<AnomaliaConteudo> auditar(DocumentoLegenda documento);
}
