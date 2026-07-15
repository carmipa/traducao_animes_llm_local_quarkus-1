package org.traducao.projeto.sistema.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.traducao.projeto.sistema.application.EncerrarAplicacaoUseCase;

import java.util.Map;

/**
 * Endpoints de controle do processo da aplicação (menu "Sair" da UI).
 * Operações de trabalho do pipeline ficam nos controllers de cada módulo;
 * aqui entra apenas o ciclo de vida do servidor em si.
 */
@RestController
@RequestMapping("/api")
public class SistemaController {

    private final EncerrarAplicacaoUseCase encerrarAplicacaoUseCase;

    public SistemaController(EncerrarAplicacaoUseCase encerrarAplicacaoUseCase) {
        this.encerrarAplicacaoUseCase = encerrarAplicacaoUseCase;
    }

    @PostMapping("/sistema/sair")
    public ResponseEntity<Map<String, String>> sair() {
        return ResponseEntity.ok(Map.of("mensagem", encerrarAplicacaoUseCase.encerrar()));
    }
}
