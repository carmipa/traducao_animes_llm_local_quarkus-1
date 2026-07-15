package org.traducao.projeto.revisaoLore.domain;

import java.util.List;

/**
 * Registro granular, append-only, de cada fala enviada ao LLM na revisão de lore.
 */
public record EntradaAuditoriaRevisaoLore(
    String instante,
    String contextoId,
    String contextoNome,
    String modo,
    String arquivo,
    int indiceEvento,
    int falaAtual,
    int totalFalas,
    String resultado,
    List<String> motivos,
    String originalEn,
    String traducaoAntes,
    String respostaLlm,
    String traducaoDepois,
    String detalhe
) {}
