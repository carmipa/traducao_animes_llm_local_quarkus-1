package org.traducao.projeto.contexto;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.contexto.domain.ContextoNaoEncontradoException;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;

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
 * PROPÓSITO DE NEGÓCIO: caracteriza a descoberta e resolução CDI dos provedores de
 * contexto após a E7b, provando que mover o {@code GerenciadorContexto} e o producer
 * {@code todosProvedoresContexto} para o peer {@code contexto} NÃO alterou o conjunto
 * injetado, a resolução do manager, a ordenação nem a seleção. O manager agora reside em
 * {@code contexto.infrastructure} e a lista é produzida por
 * {@code contexto.infrastructure.config.ContextoBeansConfig}. As 3 classes agregadoras
 * Macross sem {@code @Component} continuam fora do registro, mantendo exatamente 59 provedores.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Exatamente 59 provedores CDI; nenhum id nulo/vazio; nenhum id duplicado.</li>
 *   <li>{@code GerenciadorContexto} resolve sem ambiguidade e a {@code List<ProvedorContexto>}
 *       resolve pelo producer sem duplicação (o manager e a injeção direta veem os mesmos 59).</li>
 *   <li>A lista ordenada de ids é idêntica ao baseline
 *       ({@code /contexto/manifesto-lore.properties}).</li>
 *   <li>Ids canônicos de Danmachi, Gundam e Macross presentes.</li>
 *   <li>O {@code GerenciadorContexto} recebe os 59 provedores e seleciona um id conhecido;
 *       um id desconhecido lança {@code ContextoNaoEncontradoException}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer divergência de contagem, id duplicado/ausente, ambiguidade/duplicação de bean
 * ou falha de seleção reprova o teste — sinal de que a migração quebrou a resolução CDI.
 */
@QuarkusTest
@DisplayName("E7b: registro e resolução CDI dos provedores de contexto (59, sem ambiguidade/duplicação, seleção viva)")
class RegistroProvedoresContextoIT {

    private static final String MANIFESTO = "/contexto/manifesto-lore.properties";

    @Inject
    List<ProvedorContexto> provedores;

    @Inject
    GerenciadorContexto gerenciador;

    @Test
    @DisplayName("exatamente 59 provedores CDI, sem id nulo/vazio e sem duplicatas")
    void registroTem59ProvedoresSemDuplicatas() {
        assertEquals(59, provedores.size(), "esperados 59 provedores CDI (agregadoras Macross sem @Component ficam fora)");

        for (ProvedorContexto p : provedores) {
            assertNotNull(p.getId(), "id nulo em " + p.getClass().getName());
            assertFalse(p.getId().isBlank(), "id vazio em " + p.getClass().getName());
        }

        Set<String> unicos = provedores.stream().map(ProvedorContexto::getId).collect(Collectors.toCollection(TreeSet::new));
        assertEquals(59, unicos.size(), "ids duplicados no registro CDI");
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
        assertTrue(ids.contains("break_blade_1"), "id canônico Break Blade filme 1 ausente");
        assertTrue(ids.contains("break_blade_6"), "id canônico Break Blade filme 6 ausente");
    }

    @Test
    @DisplayName("GerenciadorContexto recebe os 59 provedores e seleciona um id conhecido; id desconhecido lança")
    void gerenciadorRecebeESelecionaProvedores() {
        assertEquals(59, gerenciador.getProvedores().size(), "GerenciadorContexto não recebeu os 59 provedores");
        assertTrue(gerenciador.existeContexto("danmachi"), "contexto conhecido deveria existir");
        assertFalse(gerenciador.existeContexto("inexistente_zzz"), "contexto desconhecido não deveria existir");

        ProvedorContexto selecionado = gerenciador.definirContextoAtivo("gundam_0079");
        assertEquals("gundam_0079", selecionado.getId(), "seleção não retornou o provedor esperado");
        assertEquals("gundam_0079", gerenciador.obterIdContextoAtivo(), "id ativo não reflete a seleção");

        assertThrows(ContextoNaoEncontradoException.class,
            () -> gerenciador.definirContextoAtivo("inexistente_zzz"),
            "id desconhecido deve lançar ContextoNaoEncontradoException");
    }

    @Test
    @DisplayName("resolução CDI sem ambiguidade (manager único) nem duplicação (producer único alimenta os 59)")
    void resolucaoCdiSemAmbiguidadeNemDuplicacao() {
        // Ambiguidade de bean falharia no deploy do @QuarkusTest; a injeção bem-sucedida
        // do manager único já prova resolução sem ambiguidade.
        assertNotNull(gerenciador, "GerenciadorContexto deve resolver como bean único (sem ambiguidade)");
        assertNotNull(provedores, "List<ProvedorContexto> deve resolver pelo producer");
        // O manager e a injeção direta devem ver EXATAMENTE o mesmo conjunto de 59 — prova de
        // que há um único producer alimentando ambos, sem duplicação de provedores.
        assertEquals(59, provedores.size(), "injeção direta deve resolver 59 provedores pelo producer");
        assertEquals(provedores.size(), gerenciador.getProvedores().size(),
            "manager e injeção direta devem ver o mesmo número de provedores (producer único, sem duplicação)");
        Set<String> idsManager = gerenciador.getProvedores().stream()
            .map(ProvedorContexto::getId).collect(Collectors.toCollection(TreeSet::new));
        assertEquals(59, idsManager.size(), "manager não pode ter ids duplicados (producer sem duplicação)");
        Set<String> idsInjecao = provedores.stream()
            .map(ProvedorContexto::getId).collect(Collectors.toCollection(TreeSet::new));
        assertEquals(idsInjecao, idsManager,
            "o conjunto de ids visto pelo manager deve ser idêntico ao da injeção direta");
    }
}
