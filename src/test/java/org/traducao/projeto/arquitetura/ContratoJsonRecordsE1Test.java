package org.traducao.projeto.arquitetura;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legendasExtracao.presentation.web.ExtracaoRequest;
import org.traducao.projeto.remuxer.presentation.web.RemuxRequest;

import java.util.Iterator;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela o contrato JSON público dos dois DTOs de request
 * movidos na Subfase E1 ({@code RemuxRequest} e {@code ExtracaoRequest}). A mudança
 * de pacote (traducao → fatias proprietárias) NÃO pode alterar a serialização/
 * desserialização consumida pela SPA: nomes de campos, tipos e ausência de campos
 * extras permanecem idênticos.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code RemuxRequest} expõe exatamente {@code entrada, saida, syncOffsetMs,
 *       preservarLegendasOriginais}.</li>
 *   <li>{@code ExtracaoRequest} expõe exatamente {@code entrada, saida, formato}.</li>
 *   <li>Round-trip (objeto → JSON → objeto) preserva todos os valores.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer campo renomeado, removido, extra ou com tipo divergente reprova a suíte.
 */
class ContratoJsonRecordsE1Test {

    private final ObjectMapper mapper = new ObjectMapper();

    private static TreeSet<String> campos(JsonNode no) {
        TreeSet<String> nomes = new TreeSet<>();
        for (Iterator<String> it = no.fieldNames(); it.hasNext(); ) {
            nomes.add(it.next());
        }
        return nomes;
    }

    @Test
    @DisplayName("RemuxRequest: campos JSON exatos e round-trip preservado")
    void contratoRemuxRequest() throws Exception {
        RemuxRequest original = new RemuxRequest("E:\\videos", "E:\\saida", 500L, true);
        String json = mapper.writeValueAsString(original);

        assertEquals(new TreeSet<>(java.util.List.of(
                "entrada", "saida", "syncOffsetMs", "preservarLegendasOriginais")),
            campos(mapper.readTree(json)),
            "Campos JSON de RemuxRequest não podem mudar (contrato da SPA): " + json);

        RemuxRequest volta = mapper.readValue(json, RemuxRequest.class);
        assertEquals(original, volta, "Round-trip de RemuxRequest deve preservar todos os valores");

        // Desserialização a partir do contrato canônico enviado pela interface.
        RemuxRequest doContrato = mapper.readValue(
            "{\"entrada\":\"E:\\\\v\",\"saida\":\"E:\\\\s\",\"syncOffsetMs\":250,\"preservarLegendasOriginais\":false}",
            RemuxRequest.class);
        assertEquals("E:\\v", doContrato.entrada());
        assertEquals(250L, doContrato.syncOffsetMs());
        assertEquals(false, doContrato.preservarLegendasOriginais());
    }

    @Test
    @DisplayName("ExtracaoRequest: campos JSON exatos e round-trip preservado")
    void contratoExtracaoRequest() throws Exception {
        ExtracaoRequest original = new ExtracaoRequest("E:\\videos", "E:\\saida", "ASS");
        String json = mapper.writeValueAsString(original);

        assertEquals(new TreeSet<>(java.util.List.of("entrada", "saida", "formato")),
            campos(mapper.readTree(json)),
            "Campos JSON de ExtracaoRequest não podem mudar (contrato da SPA): " + json);

        ExtracaoRequest volta = mapper.readValue(json, ExtracaoRequest.class);
        assertEquals(original, volta, "Round-trip de ExtracaoRequest deve preservar todos os valores");

        ExtracaoRequest doContrato = mapper.readValue(
            "{\"entrada\":\"E:\\\\v\",\"saida\":\"E:\\\\s\",\"formato\":\"SRT\"}", ExtracaoRequest.class);
        assertEquals("SRT", doContrato.formato());
    }

    @Test
    @DisplayName("Campos ausentes chegam como null (tolerância preservada)")
    void camposAusentesViramNull() throws Exception {
        RemuxRequest r = mapper.readValue("{\"entrada\":\"x\"}", RemuxRequest.class);
        assertEquals("x", r.entrada());
        assertNull(r.saida());
        assertNull(r.syncOffsetMs());
        assertNull(r.preservarLegendasOriginais());

        ExtracaoRequest e = mapper.readValue("{\"entrada\":\"x\"}", ExtracaoRequest.class);
        assertEquals("x", e.entrada());
        assertNull(e.formato());
        assertTrue(e.saida() == null);
    }
}
