package org.traducao.projeto.legenda.infrastructure.config;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova o WIRING do {@link PoliticaEstiloMusicalProducer} — o bean
 * {@code @Singleton} {@link PoliticaEstiloMusical} é produzido e injeta a lista real do
 * {@code application.yml} ({@code tradutor.estilos-ignorados}), não apenas o fallback.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Um estilo presente APENAS na lista do yml (ex.: "Mobile Suit Gundam", sem palavra-chave
 *       musical) é reconhecido — prova de que a lista completa foi injetada, não o fallback.</li>
 *   <li>A heurística/regex continuam valendo; um estilo comum não é ignorado.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Falha de produção/injeção do bean, ou lista incorreta, reprova o teste.
 */
@QuarkusTest
class PoliticaEstiloMusicalProducerIT {

    @Inject
    PoliticaEstiloMusical politica;

    @Test
    @DisplayName("produtor injeta a lista real do yml e a política funciona ponta-a-ponta")
    void produtorInjetaListaDoYml() {
        // "Mobile Suit Gundam" está SÓ na lista do yml (sem palavra-chave musical) → prova da lista completa.
        assertTrue(politica.estiloIgnorado("Mobile Suit Gundam"), "lista do yml deve ter sido injetada");
        assertTrue(politica.estiloIgnorado("Song JP"), "item da lista do yml");
        assertTrue(politica.estiloIgnorado("Karaoke"), "heurística independe da lista");
        assertFalse(politica.estiloIgnorado("Default"), "estilo comum não é ignorado");
        assertFalse(politica.estiloIgnorado(null));
    }
}
