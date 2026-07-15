package org.traducao.projeto.novoKaraoke.presentation;

/**
 * Requisição da conversão de karaokê: pasta das legendas .ass de origem e a
 * pasta de destino (obrigatoriamente diferente — o original é preservado).
 */
public record NovoKaraokeRequest(String caminhoOrigem, String caminhoDestino) {
}
