package org.traducao.projeto.traducao.presentation.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.traducao.projeto.core.io.DiretorioBaseKronos;
import org.traducao.projeto.mapaProjeto.application.GeradorMapaProjetoUseCase;

import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: expõe a geração do mapa do projeto (Opção 7) à interface
 * web, produzindo o relatório em markdown e a árvore no formato GitHub a partir
 * da raiz do projeto.
 *
 * <p>INVARIANTES DO DOMÍNIO: a raiz mapeada vem de
 * {@link DiretorioBaseKronos#base()} — em produção é o diretório de trabalho e,
 * sob a suíte de testes, a árvore descartável, evitando reescrever o mapa real;
 * nenhuma URL, código HTTP ou nome de campo de DTO é alterado em relação ao
 * controller monolítico original.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: a geração é síncrona; qualquer falha do use
 * case propaga como erro do endpoint, sem estado parcial retornado ao navegador.
 */
@RestController
@RequestMapping("/api")
public class MapaController {

    private final GeradorMapaProjetoUseCase geradorMapaProjetoUseCase;

    public MapaController(GeradorMapaProjetoUseCase geradorMapaProjetoUseCase) {
        this.geradorMapaProjetoUseCase = geradorMapaProjetoUseCase;
    }

    /**
     * 7. MAPA DO PROJETO
     */
    @PostMapping("/mapa")
    public ResponseEntity<MapaResponse> gerarMapa() {
        // Raiz do projeto a mapear. Via DiretorioBaseKronos: em produção é o
        // diretório de trabalho (raiz do repositório); sob a suíte de testes é
        // a árvore descartável, evitando reescrever o mapa_projeto.md real.
        Path raiz = DiretorioBaseKronos.base().toAbsolutePath();
        GeradorMapaProjetoUseCase.ResultadoMapa resultado = geradorMapaProjetoUseCase.executar(raiz);
        return ResponseEntity.ok(new MapaResponse(
            resultado.relatorio(), resultado.arvoreGithub(), resultado.nomeProjeto()));
    }
}
