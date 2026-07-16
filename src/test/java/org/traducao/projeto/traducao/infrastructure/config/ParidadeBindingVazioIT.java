package org.traducao.projeto.traducao.infrastructure.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PASSO 1 da E3c — cenario LISTA VAZIA EXPLICITA. Perfil de teste sobrescreve
 * {@code tradutor.estilos-ignorados} para vazio e OBSERVA o que cada binding produz,
 * para o gate de divergencia (decisao 6: ausente != vazia).
 *
 * <p>Este teste NAO afirma um resultado esperado rigido: ele IMPRIME e registra o
 * estado real de ambos os bindings para ratificacao. As assercoes apenas travam a
 * comparacao Spring-vs-SmallRye (se divergirem, o gate dispara com evidencia).
 */
@QuarkusTest
@TestProfile(ParidadeBindingVazioIT.PerfilVazio.class)
class ParidadeBindingVazioIT {

    public static class PerfilVazio implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("tradutor.estilos-ignorados", "");
        }
    }

    private static final List<String> FALLBACK = List.of("Song JP");

    @Inject
    TradutorProperties tradutorProperties;

    @ConfigProperty(name = "tradutor.estilos-ignorados")
    Optional<List<String>> estilosRaw;

    @Test
    @DisplayName("VAZIO EXPLICITO: registra estado real de Spring e SmallRye (gate de colapso ausente-vs-vazio)")
    void vazioExplicito() {
        List<String> spring = tradutorProperties.estilosIgnorados();
        Optional<List<String>> smallrye = estilosRaw;
        List<String> produtor = smallrye.orElse(FALLBACK);

        boolean smallryeColapsou = smallrye.isEmpty(); // empty => produtor aplica fallback (colapso com ausente)
        boolean springColapsou = FALLBACK.equals(spring); // Spring caiu no default ["Song JP"]

        System.out.println("[E3c-PARIDADE][VAZIO] spring=" + spring
            + " | smallrye=" + smallrye
            + " | produtor(orElse)=" + produtor
            + " | smallryeColapsouComAusente=" + smallryeColapsou
            + " | springColapsouComAusente=" + springColapsou);

        // Trava a comparacao entre os dois bindings sob vazio explicito.
        org.junit.jupiter.api.Assertions.assertEquals(
            spring, produtor,
            "GATE E3c: sob lista vazia explicita, Spring e produtor divergem. "
            + "spring=" + spring + " produtor=" + produtor
            + " (smallryeEmpty=" + smallryeColapsou + ")");
    }
}
