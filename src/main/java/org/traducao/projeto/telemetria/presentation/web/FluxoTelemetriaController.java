package org.traducao.projeto.telemetria.presentation.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.traducao.projeto.telemetria.FluxoTelemetriaPort;
import org.traducao.projeto.telemetria.StatusFluxoTelemetria;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: expõe o estado do fluxo ao vivo de telemetria para o
 * card do painel inicial, ao lado do orquestrador, do LLM e do cache.
 *
 * <h2>Por que responde 200 mesmo desconectado</h2>
 * Fluxo fora do ar é estado LEGÍTIMO: o KRONOS funciona inteiro sem ele. Devolver
 * 503 aqui faria um painel de monitoramento pintar de vermelho uma aplicação
 * perfeitamente saudável, e treinaria quem opera a ignorar o alerta. O card
 * mostra "desconectado" com o motivo, e a tradução segue.
 *
 * <p>INVARIANTES DO DOMÍNIO: nunca lança e nunca demora — a consulta é feita a
 * cada carga do painel inicial.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: a porta já absorve qualquer erro; este
 * controller só formata.
 */
@RestController
@RequestMapping("/api/fluxo")
public class FluxoTelemetriaController {

    private final FluxoTelemetriaPort fluxo;

    public FluxoTelemetriaController(FluxoTelemetriaPort fluxo) {
        this.fluxo = fluxo;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: estado atual do fluxo, no formato que o card consome.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code eventos} vem {@code -1} quando
     * desconhecido, e a interface trata isso como "—" em vez de exibir um número
     * negativo — zero eventos e estado desconhecido são coisas diferentes.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: 200 com {@code conectado:false}.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        StatusFluxoTelemetria s = fluxo.status();
        return ResponseEntity.ok(Map.of(
            "conectado", s.conectado(),
            "detalhe", s.detalhe(),
            "eventos", s.eventos()
        ));
    }
}
