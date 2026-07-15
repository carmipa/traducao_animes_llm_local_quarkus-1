package org.traducao.projeto.legendasExtracao.domain.ports;

import org.traducao.projeto.legendasExtracao.domain.FaixaLegenda;

import java.nio.file.Path;
import java.util.List;

/**
 * Abstrai a ferramenta usada para identificar e extrair faixas de legenda de um
 * vídeo. Cada implementação é responsável por um conjunto de contêineres
 * (ex.: MKVToolNix para Matroska, ffmpeg para os demais formatos).
 */
public interface ExtratorVideoPort {

    boolean suporta(Path arquivoVideo);

    void validarInfraestrutura();

    List<FaixaLegenda> identificarFaixas(Path arquivoVideo);

    void extrairTrilha(Path arquivoVideo, int trackId, Path caminhoSaida);
}
