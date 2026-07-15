package org.traducao.projeto.analisadorMidia.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.nio.file.Path;
import java.util.List;

public record AuditoriaResultado(
    @JsonIgnore Path caminhoArquivo,
    String nomeArquivo,
    ContainerInfo container,
    List<VideoInfo> videos,
    List<AudioInfo> audios,
    List<LegendaInfo> legendas,
    List<CapituloInfo> capitulos,
    List<AnexoInfo> anexos,
    @JsonIgnore List<String> logsAuditoria
) {}
