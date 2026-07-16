package org.traducao.projeto.cachetraducao.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: caracteriza a regra de compatibilidade de proveniência do
 * cache de tradução, garantindo que uma tradução só é reutilizada quando os SEIS
 * campos canônicos batem exatamente — incluindo {@code schemaVersion}, cuja omissão
 * histórica permitia reutilizar cache de schema desconhecido.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Igualdade exata dos seis campos (schemaVersion, contextoId, contextoHash,
 *       modeloLlm, idiomaOrigem, idiomaDestino) autoriza a reutilização.</li>
 *   <li>Diferença isolada em qualquer um dos seis campos torna incompatível.</li>
 *   <li>{@code schemaVersion} 0 (valor materializado quando o campo está ausente no
 *       JSON) nunca é considerado igual ao {@code SCHEMA_ATUAL}: sem normalização.</li>
 *   <li>Comparar com {@code null} é sempre "diferente".</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer regressão que volte a ignorar {@code schemaVersion} ou a normalizar 0 para
 * a versão atual reprova estes testes.
 */
@DisplayName("ProvenienciaCache: compatibilidade exige igualdade exata dos seis campos")
class ProvenienciaCacheTest {

    private static ProvenienciaCache base() {
        return new ProvenienciaCache(ProvenienciaCache.SCHEMA_ATUAL, "danmachi", "hash", "gemma", "en", "pt-BR");
    }

    @Test
    @DisplayName("seis campos iguais → mesma proveniência")
    void seisCamposIguaisSaoCompativeis() {
        assertTrue(base().mesmaProveniencia(base()));
    }

    @Test
    @DisplayName("comparar com null → diferente")
    void nullEhSempreDiferente() {
        assertFalse(base().mesmaProveniencia(null));
    }

    @Test
    @DisplayName("schemaVersion diferente → incompatível")
    void schemaVersionDiferenteEhIncompativel() {
        ProvenienciaCache outra = new ProvenienciaCache(
            ProvenienciaCache.SCHEMA_ATUAL + 1, "danmachi", "hash", "gemma", "en", "pt-BR");
        assertFalse(base().mesmaProveniencia(outra));
    }

    @Test
    @DisplayName("schemaVersion 0 (campo ausente no JSON) versus atual → incompatível, sem normalização")
    void schemaVersionZeroVersusAtualEhIncompativel() {
        ProvenienciaCache ausente = new ProvenienciaCache(
            0, "danmachi", "hash", "gemma", "en", "pt-BR");
        assertFalse(base().mesmaProveniencia(ausente), "0 não pode ser normalizado para SCHEMA_ATUAL");
        assertFalse(ausente.mesmaProveniencia(base()), "a comparação é simétrica");
    }

    @Test
    @DisplayName("contextoId diferente → incompatível")
    void contextoIdDiferenteEhIncompativel() {
        ProvenienciaCache outra = new ProvenienciaCache(
            ProvenienciaCache.SCHEMA_ATUAL, "gundam_0079", "hash", "gemma", "en", "pt-BR");
        assertFalse(base().mesmaProveniencia(outra));
    }

    @Test
    @DisplayName("contextoHash diferente → incompatível")
    void contextoHashDiferenteEhIncompativel() {
        ProvenienciaCache outra = new ProvenienciaCache(
            ProvenienciaCache.SCHEMA_ATUAL, "danmachi", "hash-outro", "gemma", "en", "pt-BR");
        assertFalse(base().mesmaProveniencia(outra));
    }

    @Test
    @DisplayName("modeloLlm diferente → incompatível")
    void modeloLlmDiferenteEhIncompativel() {
        ProvenienciaCache outra = new ProvenienciaCache(
            ProvenienciaCache.SCHEMA_ATUAL, "danmachi", "hash", "mistral", "en", "pt-BR");
        assertFalse(base().mesmaProveniencia(outra));
    }

    @Test
    @DisplayName("idiomaOrigem diferente → incompatível")
    void idiomaOrigemDiferenteEhIncompativel() {
        ProvenienciaCache outra = new ProvenienciaCache(
            ProvenienciaCache.SCHEMA_ATUAL, "danmachi", "hash", "gemma", "ja", "pt-BR");
        assertFalse(base().mesmaProveniencia(outra));
    }

    @Test
    @DisplayName("idiomaDestino diferente → incompatível")
    void idiomaDestinoDiferenteEhIncompativel() {
        ProvenienciaCache outra = new ProvenienciaCache(
            ProvenienciaCache.SCHEMA_ATUAL, "danmachi", "hash", "gemma", "en", "es");
        assertFalse(base().mesmaProveniencia(outra));
    }
}
