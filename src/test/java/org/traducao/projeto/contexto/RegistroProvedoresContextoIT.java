package org.traducao.projeto.contexto;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.contexto.domain.ContextoNaoEncontradoException;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: caracteriza a descoberta CDI dos provedores de contexto após a
 * E7a, provando que mover os {@code @Component} para {@code contexto.lore} NÃO alterou o
 * conjunto injetado nem a seleção pelo {@code GerenciadorContexto} (que permanece em
 * {@code traducao}). As 3 classes agregadoras Macross sem {@code @Component} continuam
 * fora do registro, mantendo exatamente 53 provedores.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Exatamente 53 provedores CDI; nenhum id nulo/vazio; nenhum id duplicado.</li>
 *   <li>A lista ordenada de ids é idêntica ao baseline pré-move
 *       ({@code /contexto/manifesto-lore.properties}).</li>
 *   <li>Ids canônicos de Danmachi, Gundam e Macross presentes.</li>
 *   <li>O {@code GerenciadorContexto} recebe os 53 provedores e seleciona um id conhecido;
 *       um id desconhecido lança {@code ContextoNaoEncontradoException}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer divergência de contagem, id duplicado/ausente ou falha de seleção reprova o
 * teste — sinal de que a extração quebrou a descoberta CDI.
 */
@QuarkusTest
@DisplayName("E7a: registro CDI dos provedores de contexto (53, sem duplicatas, seleção viva)")
class RegistroProvedoresContextoIT {

    private static final String MANIFESTO = "/contexto/manifesto-lore.properties";

    @Inject
    List<ProvedorContexto> provedores;

    @Inject
    GerenciadorContexto gerenciador;

    @Test
    @DisplayName("exatamente 53 provedores CDI, sem id nulo/vazio e sem duplicatas")
    void registroTem53ProvedoresSemDuplicatas() {
        assertEquals(53, provedores.size(), "esperados 53 provedores CDI (agregadoras Macross sem @Component ficam fora)");

        for (ProvedorContexto p : provedores) {
            assertNotNull(p.getId(), "id nulo em " + p.getClass().getName());
            assertFalse(p.getId().isBlank(), "id vazio em " + p.getClass().getName());
        }

        Set<String> unicos = provedores.stream().map(ProvedorContexto::getId).collect(Collectors.toCollection(TreeSet::new));
        assertEquals(53, unicos.size(), "ids duplicados no registro CDI");
    }

    @Test
    @DisplayName("lista ordenada de ids é idêntica ao baseline pré-move")
    void idsBatemComBaselinePreMove() throws Exception {
        Properties esperado = new Properties();
        try (InputStream in = getClass().getResourceAsStream(MANIFESTO)) {
            assertNotNull(in, "Manifesto pré-move não encontrado: " + MANIFESTO);
            esperado.load(in);
        }
        String idsVivos = provedores.stream()
            .map(ProvedorContexto::getId)
            .sorted(Comparator.naturalOrder())
            .collect(Collectors.joining(","));
        assertEquals(esperado.getProperty("ids"), idsVivos, "conjunto/ordem de ids divergiu do baseline");
    }

    @Test
    @DisplayName("ids canônicos de Danmachi, Gundam e Macross estão presentes")
    void idsCanonicosPresentes() {
        Set<String> ids = provedores.stream().map(ProvedorContexto::getId).collect(Collectors.toCollection(TreeSet::new));
        assertTrue(ids.contains("danmachi"), "id canônico de Danmachi ausente");
        assertTrue(ids.contains("gundam_0079"), "id canônico de Gundam ausente");
        assertTrue(ids.contains("macross_frontier"), "id canônico de Macross ausente");
    }

    @Test
    @DisplayName("GerenciadorContexto recebe os 53 provedores e seleciona um id conhecido; id desconhecido lança")
    void gerenciadorRecebeESelecionaProvedores() {
        assertEquals(53, gerenciador.getProvedores().size(), "GerenciadorContexto não recebeu os 53 provedores");
        assertTrue(gerenciador.existeContexto("danmachi"), "contexto conhecido deveria existir");
        assertFalse(gerenciador.existeContexto("inexistente_zzz"), "contexto desconhecido não deveria existir");

        ProvedorContexto selecionado = gerenciador.definirContextoAtivo("gundam_0079");
        assertEquals("gundam_0079", selecionado.getId(), "seleção não retornou o provedor esperado");
        assertEquals("gundam_0079", gerenciador.obterIdContextoAtivo(), "id ativo não reflete a seleção");

        assertThrows(ContextoNaoEncontradoException.class,
            () -> gerenciador.definirContextoAtivo("inexistente_zzz"),
            "id desconhecido deve lançar ContextoNaoEncontradoException");
    }
}
