package org.traducao.projeto.revisaoLore.infrastructure.adapters;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.revisaoLore.infrastructure.config.RevisaoLoreLlmProperties;
import org.traducao.projeto.revisaoLore.infrastructure.dtos.RevisaoLoreLlmDtos.ChatRequest;
import org.traducao.projeto.revisaoLore.infrastructure.dtos.RevisaoLoreLlmDtos.RespostaLlm;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: provar que o botão "Parar" para de verdade quando a thread é interrompida
 * DURANTE a chamada ao LLM — a janela dominante, porque a revisão passa quase todo o tempo esperando
 * a rede (read-timeout de até 180s por tentativa).
 *
 * <h2>O prejuízo que originou</h2>
 * {@code FilaExecucaoPipeline.parar()} faz {@code future.cancel(true)}, que interrompe a thread única.
 * O {@code HttpClient.send} do JDK lança {@link InterruptedException} e LIMPA o flag de interrupção.
 * Antes do conserto essa exceção caía no {@code catch (Exception)} do adapter, que a engolia, dormia
 * a pausa e RE-TENTAVA a POST; de volta ao laço do {@code RevisarLoreUseCase},
 * {@code Thread.isInterrupted()} dava {@code false} e a parada cooperativa não disparava — o job
 * seguia sobrescrevendo {@code .ass} DEPOIS de o operador mandar parar.
 *
 * <h2>Invariantes do domínio</h2>
 * Interrompida a chamada, o adapter restaura o flag e encerra na mesma tentativa: nada de re-tentar.
 *
 * <h2>Comportamento em caso de falha</h2>
 * A revisão devolve {@link Optional#empty()} e o flag volta setado, para o laço parar no próximo
 * ponto seguro preservando o que já foi gravado.
 */
class RevisorLoreLlmAdapterInterrupcaoTest {

    @Test
    @DisplayName("interrupcao durante a chamada ao LLM restaura o flag e encerra sem re-tentar")
    void interrupcaoDuranteChamadaLlmRestauraFlagEEncerra() {
        AtomicInteger chamadas = new AtomicInteger();
        RevisorLoreLlmAdapter adapter = new RevisorLoreLlmAdapter(
            new RevisaoLoreLlmProperties(), new ObjectMapper(), new NormalizadorRespostaRevisaoLore()) {
            @Override
            protected RespostaLlm postarChat(ChatRequest request) throws InterruptedException {
                chamadas.incrementAndGet();
                // Fiel ao JDK: HttpClient.send lanca InterruptedException com o flag JA LIMPO.
                throw new InterruptedException("interrompido durante o send");
            }
        };

        Optional<String> resultado = adapter.revisar(
            "system", "[[TAG0]] Titan", "[[TAG0]] Titã", List.of("motivo de lore"));

        boolean flagRestaurado = Thread.interrupted(); // le E limpa, para nao vazar para outros testes
        assertTrue(flagRestaurado,
            "o flag de interrupcao tem de voltar SETADO. Sem isso o RevisarLoreUseCase nao para no "
                + "proximo ponto seguro e segue sobrescrevendo .ass depois do Parar.");
        assertEquals(1, chamadas.get(),
            "apos a interrupcao a revisao encerra na mesma tentativa; NAO re-tenta a POST (eram 2 "
                + "chamadas antes do conserto, com uma pausa engolida no meio).");
        assertTrue(resultado.isEmpty(),
            "interrompida, a revisao devolve vazio (a fala fica pendente/sem-resposta, nao gravada).");
    }
}
