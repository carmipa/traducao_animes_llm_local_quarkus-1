package org.traducao.projeto.traducaoKaraoke.presentation;

/**
 * Corpo das requisições do painel Tradução de Karaokê: a pasta com as
 * legendas .ass e a obra (contexto de lore) selecionada na UI.
 */
public record TraducaoKaraokeRequest(
    String caminhoOrigem,
    String contextoId
) {
}
