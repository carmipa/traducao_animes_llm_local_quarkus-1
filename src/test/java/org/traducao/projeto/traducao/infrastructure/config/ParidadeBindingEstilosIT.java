package org.traducao.projeto.traducao.infrastructure.config;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PASSO 1 da E3c — caracterizacao de paridade de binding (cenarios PREENCHIDO e AUSENTE).
 * Observa o comportamento REAL de dois bindings sobre a MESMA chave
 * {@code tradutor.estilos-ignorados}:
 * <ul>
 *   <li>Spring {@code @ConfigurationProperties} (TradutorProperties.estilosIgnorados());</li>
 *   <li>SmallRye {@code @ConfigProperty Optional<List<String>>} + fallback do futuro produtor.</li>
 * </ul>
 * O cenario LISTA VAZIA EXPLICITA fica em {@code ParidadeBindingVazioIT} (perfil dedicado).
 */
@QuarkusTest
class ParidadeBindingEstilosIT {

    private static final List<String> FALLBACK = List.of("Song JP");

    @Inject
    TradutorProperties tradutorProperties;

    @ConfigProperty(name = "tradutor.estilos-ignorados")
    Optional<List<String>> estilosRaw;

    @Test
    @DisplayName("PREENCHIDO (yml real): Spring == SmallRye == 8 itens na ordem")
    void preenchido() {
        List<String> esperado = List.of(
            "Song JP", "Mobile Suit Gundam", "Char's Counterattack",
            "OP - Romaji", "OP - English", "ED - Romaji", "ED - English", "ED-ROM");
        List<String> spring = tradutorProperties.estilosIgnorados();
        List<String> produtor = estilosRaw.orElse(FALLBACK);
        System.out.println("[E3c-PARIDADE][PREENCHIDO] spring=" + spring + " | smallrye=" + estilosRaw
            + " | produtor=" + produtor);
        assertEquals(esperado, spring, "Spring binding deve refletir o yml exato");
        assertEquals(esperado, produtor, "SmallRye/produtor deve refletir o yml exato");
        assertEquals(spring, produtor, "PARIDADE preenchido: Spring == produtor");
    }

    @Test
    @DisplayName("AUSENTE: ambos caem no fallback [\"Song JP\"] (spec MicroProfile + default Spring)")
    void ausente() {
        List<String> springAusente = new TradutorProperties().estilosIgnorados();
        List<String> produtorAusente = Optional.<List<String>>empty().orElse(FALLBACK);
        System.out.println("[E3c-PARIDADE][AUSENTE] spring(default)=" + springAusente
            + " | produtor(empty.orElse)=" + produtorAusente);
        assertEquals(List.of("Song JP"), springAusente, "Spring ausente = default do campo");
        assertEquals(List.of("Song JP"), produtorAusente, "produtor ausente = fallback");
        assertEquals(springAusente, produtorAusente, "PARIDADE ausente");
    }
}
