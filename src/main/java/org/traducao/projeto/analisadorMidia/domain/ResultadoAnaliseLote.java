package org.traducao.projeto.analisadorMidia.domain;

import java.util.List;

/**
 * Resultado de uma execução de auditoria sobre um lote de vídeos: os arquivos
 * analisados com sucesso e as falhas individuais. A análise não grava mais
 * relatório em disco automaticamente — a exportação é manual (via UI).
 */
public record ResultadoAnaliseLote(
    List<AuditoriaResultado> resultados,
    List<FalhaAnalise> falhas
) {}
