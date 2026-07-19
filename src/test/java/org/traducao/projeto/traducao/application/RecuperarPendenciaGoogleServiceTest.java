package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.traducao.domain.ports.FallbackTraducaoOnlinePort;
import org.traducao.projeto.traducao.infrastructure.config.FallbackOnlineProperties;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa o contrato do {@link RecuperarPendenciaGoogleService} — opt-in,
 * escopo restrito às pendências informadas e guarda de nomes próprios — sem rede.
 *
 * <p>INVARIANTES DO DOMÍNIO: desligado não chama a porta; ligado só aceita respostas que
 * preservam nomes próprios; recusa é segura (fala omitida do resultado).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer desvio de gate, de guarda ou de escopo reprova.
 */
class RecuperarPendenciaGoogleServiceTest {

    private static RecuperarPendenciaGoogleService servico(boolean ativo, FallbackTraducaoOnlinePort porta) {
        return new RecuperarPendenciaGoogleService(new FallbackOnlineProperties(ativo), porta);
    }

    private static LinkedHashSet<String> conjunto(String... itens) {
        return new LinkedHashSet<>(Set.of(itens));
    }

    @Test
    @DisplayName("desligado: não chama a porta e devolve mapa vazio")
    void desligadoNaoChamaPorta() {
        boolean[] chamou = {false};
        FallbackTraducaoOnlinePort porta = o -> { chamou[0] = true; return Optional.of("x"); };

        Map<String, String> r = servico(false, porta).recuperar(conjunto("Hello"));

        assertTrue(r.isEmpty());
        assertTrue(!chamou[0], "porta não pode ser chamada com o modo desligado");
    }

    @Test
    @DisplayName("ligado: devolve a tradução quando a porta responde e os nomes são preservados")
    void ligadoRecuperaComNomePreservado() {
        FallbackTraducaoOnlinePort porta = o -> Optional.of("Eu vi o Lena ontem");

        Map<String, String> r = servico(true, porta).recuperar(conjunto("I saw Lena yesterday"));

        assertEquals("Eu vi o Lena ontem", r.get("I saw Lena yesterday"));
    }

    @Test
    @DisplayName("ligado: recusa (mantém pendente) quando o nome próprio mid-sentence some")
    void ligadoRecusaQuandoNomePropioSome() {
        // "Lena" é mid-sentence e capitalizado; a tradução a perdeu -> recusa segura.
        FallbackTraducaoOnlinePort porta = o -> Optional.of("Eu vi ela ontem");

        Map<String, String> r = servico(true, porta).recuperar(conjunto("I saw Lena yesterday"));

        assertTrue(r.isEmpty(), "nome próprio perdido deve manter a fala pendente");
    }

    @Test
    @DisplayName("ligado: capital de início de frase não é tratado como nome próprio")
    void capitalDeInicioNaoEhNomeProprio() {
        // "Why" abre a frase; a tradução PT não o contém e mesmo assim é aceita.
        FallbackTraducaoOnlinePort porta = o -> Optional.of("Por que temos que aguentar isso");

        Map<String, String> r = servico(true, porta).recuperar(conjunto("Why do we have to put up with this"));

        assertEquals("Por que temos que aguentar isso", r.get("Why do we have to put up with this"));
    }

    @Test
    @DisplayName("ligado: porta vazia (rede/recusa) omite a fala do resultado")
    void portaVaziaOmiteFala() {
        FallbackTraducaoOnlinePort porta = o -> Optional.empty();

        Map<String, String> r = servico(true, porta).recuperar(conjunto("Hello there"));

        assertTrue(r.isEmpty());
    }

    @Test
    @DisplayName("ligado: pontuação isolada encerra a frase; a 1ª palavra da frase seguinte não é nome próprio")
    void pontuacaoIsoladaEncerraFrase() {
        // "London" abre a 2ª frase (após o ponto isolado) e foi legitimamente traduzido para
        // "Londres"; não pode ser tratado como nome próprio obrigatório e reprovar a recuperação.
        FallbackTraducaoOnlinePort porta = o -> Optional.of("Ele saiu . Londres chama");

        Map<String, String> r = servico(true, porta).recuperar(conjunto("He left . London calls"));

        assertEquals("Ele saiu . Londres chama", r.get("He left . London calls"),
            "palavra inicial da frase após pontuação isolada não é nome próprio obrigatório");
    }
}
