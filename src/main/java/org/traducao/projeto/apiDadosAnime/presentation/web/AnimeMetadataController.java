package org.traducao.projeto.apiDadosAnime.presentation.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.traducao.projeto.apiDadosAnime.application.ObterMetadataAnimeUseCase;
import org.traducao.projeto.apiDadosAnime.domain.model.AnimeMetadata;

import java.util.Optional;

@RestController
@RequestMapping("/api/metadata")
public class AnimeMetadataController {

    private final ObterMetadataAnimeUseCase obterMetadataAnimeUseCase;

    public AnimeMetadataController(ObterMetadataAnimeUseCase obterMetadataAnimeUseCase) {
        this.obterMetadataAnimeUseCase = obterMetadataAnimeUseCase;
    }

    @GetMapping
    public ResponseEntity<AnimeMetadata> obterMetadata(@RequestParam(name = "caminho") String caminho) {
        Optional<AnimeMetadata> metadataOpt = obterMetadataAnimeUseCase.executar(caminho);
        return metadataOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
