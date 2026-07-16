package org.traducao.projeto.traducao.infrastructure.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: caracteriza a resolução das quatro chaves de valor simples
 * migradas na subfase E3b (tradutor.diretorio-entrada, tradutor.idioma-original,
 * tradutor.idioma-traduzido, tradutor.diretorio-cache) via {@code @ConfigProperty}
 * {@code Optional<String>}, blindando o novo acoplamento antes de remover
 * {@code TradutorProperties} dos seis consumidores.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Chave presente → {@code Optional.of(valor)}; a sobrescrita de perfil vence o
 *       {@code application.yml} (prova de override).</li>
 *   <li>Chave ausente ou vazia ("") → {@code Optional.empty()} (SmallRye colapsa valor
 *       vazio), e o fallback de domínio local (.orElse) aplica o default.</li>
 *   <li>Valor só com espaços → o filtro {@code !isBlank()} força o default de domínio
 *       (idioma/entrada), sem depender de trimming do SmallRye.</li>
 *   <li>Nenhum {@code defaultValue} é usado na injeção; o default é sempre do consumidor.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer divergência na resolução ou no fallback reprova o teste, sinalizando a
 * quebra de paridade com o comportamento pré-E3b.
 */
@QuarkusTest
@TestProfile(ConfiguracaoSimplesE3bIT.PerfilE3b.class)
class ConfiguracaoSimplesE3bIT {

    public static class PerfilE3b implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "tradutor.diretorio-entrada", "entrada-cfg",
                "tradutor.idioma-original", "es",
                "tradutor.idioma-traduzido", "fr",
                "tradutor.diretorio-cache", "cache-cfg",
                "e3b.teste.vazio", "",
                "e3b.teste.espacos", "   "
            );
        }
    }

    @ConfigProperty(name = "tradutor.diretorio-entrada")
    Optional<String> diretorioEntrada;
    @ConfigProperty(name = "tradutor.idioma-original")
    Optional<String> idiomaOriginal;
    @ConfigProperty(name = "tradutor.idioma-traduzido")
    Optional<String> idiomaTraduzido;
    @ConfigProperty(name = "tradutor.diretorio-cache")
    Optional<String> diretorioCache;
    @ConfigProperty(name = "e3b.teste.ausente")
    Optional<String> chaveAusente;
    @ConfigProperty(name = "e3b.teste.vazio")
    Optional<String> chaveVazia;
    @ConfigProperty(name = "e3b.teste.espacos")
    Optional<String> chaveEspacos;

    @Test
    @DisplayName("presente/override: valores configurados resolvem exatamente e vencem o yml")
    void presenteEOverride() {
        assertEquals("entrada-cfg", diretorioEntrada.orElse(null));
        assertEquals("es", idiomaOriginal.filter(s -> !s.isBlank()).orElse("en"));
        assertEquals("fr", idiomaTraduzido.filter(s -> !s.isBlank()).orElse("pt-br"));
        assertEquals("cache-cfg", diretorioCache.orElse("cache"));
    }

    @Test
    @DisplayName("ausente: Optional.empty() e fallback de domínio local")
    void ausente() {
        assertTrue(chaveAusente.isEmpty(), "chave ausente deve resolver Optional.empty()");
        assertEquals("cache", chaveAusente.orElse("cache"));
        assertNull(chaveAusente.orElse(null));
    }

    @Test
    @DisplayName("vazio (\"\"): SmallRye colapsa em empty; fallback aplica")
    void vazio() {
        assertTrue(chaveVazia.isEmpty(), "valor vazio deve colapsar em Optional.empty()");
        assertEquals("cache", chaveVazia.orElse("cache"));
        assertEquals("en", chaveVazia.filter(s -> !s.isBlank()).orElse("en"));
    }

    @Test
    @DisplayName("branco (espaços): filtro isBlank força o default de domínio")
    void espacos() {
        // Robusto a eventual trimming do SmallRye: com o filtro isBlank, valores só
        // com espaços sempre caem no default de domínio (idioma/entrada).
        assertEquals("en", chaveEspacos.filter(s -> !s.isBlank()).orElse("en"));
        assertNull(chaveEspacos.filter(s -> !s.isBlank()).orElse(null));
    }
}
