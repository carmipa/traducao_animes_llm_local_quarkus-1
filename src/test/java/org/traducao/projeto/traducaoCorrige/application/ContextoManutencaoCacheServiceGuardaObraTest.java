package org.traducao.projeto.traducaoCorrige.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.cachetraducao.infrastructure.CacheManutencaoService;
import org.traducao.projeto.contexto.application.ValidadorCompatibilidadeObraContexto;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prende a guarda obra×contexto no ÚNICO ponto por onde as três fatias de
 * manutenção de cache ({@code traducaoCorrige}, {@code raspagemCorrecao},
 * {@code raspagemRevisao}) escolhem sob qual lore vão reinterpretar um cache — a FASE 1 do
 * Plano-Mestre do corretor.
 *
 * <p>A pergunta aqui é DIFERENTE da que a guarda da fatia {@code traducao} responde. Lá se
 * desconfia do CLIQUE do operador. Aqui se desconfia do PRÓPRIO CARIMBO: o incidente medido nesta
 * árvore gravou 15 caches de Gundam 0083 com {@code contextoId = "guilty_crown"}, e a proveniência
 * registrou a lore errada com toda a fidelidade. Um corretor que confia no carimbo reativaria
 * Guilty Crown sobre um cache de 0083 e "corrigiria" a terminologia para a obra errada —
 * aprofundando o dano em vez de repará-lo. A pasta em que o arquivo mora é a segunda testemunha.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A guarda vale para o contexto RESOLVIDO, venha do carimbo ou do fallback escolhido na UI
 *       para cache legado: os dois caminhos desembocam na mesma ativação.</li>
 *   <li>A obra do cache é a pasta-MÃE ({@code <raiz>/<Obra>/<base>.cache.json}), não a pasta-avó
 *       da legenda de entrada. Reusar a regra da legenda deslocaria a obra em um nível.</li>
 *   <li>Bloqueio NÃO altera o contexto ativo: a lore reprovada não pode vazar para o próximo
 *       arquivo da varredura.</li>
 *   <li>Pasta que nenhuma lore reconhece SEGUE (falha aberta) — fechar aí pararia a manutenção de
 *       todo cache de obra ainda sem vocabulário declarado.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Falha aqui significa que a manutenção de cache voltou a aceitar o carimbo como prova, e a
 * correção passa a reescrever terminologia sob a lore de outra obra — sem erro visível, porque o
 * resultado fica internamente coerente com a lore errada.
 */
@DisplayName("ContextoManutencaoCacheService: a pasta do cache é testemunha contra o carimbo")
class ContextoManutencaoCacheServiceGuardaObraTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final GerenciadorContexto gerenciador = new GerenciadorContexto(List.of(
        new ContextoFake("gundam_0083", "Mobile Suit Gundam 0083"),
        new ContextoFake("guilty_crown", "Guilty Crown")));

    private final ContextoManutencaoCacheService servico =
        new ContextoManutencaoCacheService(gerenciador, new ValidadorCompatibilidadeObraContexto());

    /**
     * PROPÓSITO DE NEGÓCIO: o caso do incidente. O carimbo diz {@code guilty_crown}, mas o cache
     * mora na pasta de Gundam 0083 — a proveniência está fielmente errada e a manutenção precisa
     * recusá-la.
     */
    @Test
    @DisplayName("carimbo de OUTRA obra: bloqueia e a mensagem nomeia as duas lores")
    void carimboDeOutraObraBloqueia() {
        var documento = documento("Mobile Suit Gundam 0083", "guilty_crown");

        var erro = assertThrows(IllegalArgumentException.class, () -> servico.ativar(documento, null));

        assertTrue(erro.getMessage().contains("NÃO confere com o contexto selecionado"),
            () -> "tem de ser DIVERGÊNCIA, não ambiguidade: os dois consertos são diferentes e as "
                + "duas mensagens citam os mesmos ids, então só o texto distingue. " + erro.getMessage());
        assertTrue(erro.getMessage().contains("gundam_0083"),
            () -> "a mensagem precisa nomear a lore ESPERADA: " + erro.getMessage());
        assertTrue(erro.getMessage().contains("guilty_crown"),
            () -> "a mensagem precisa nomear a lore do CARIMBO: " + erro.getMessage());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: bloquear não pode ser um efeito colateral que contamina o resto da
     * varredura — o contexto ativo tem de ficar exatamente como estava.
     */
    @Test
    @DisplayName("bloqueio NÃO ativa a lore reprovada: nada vaza para o próximo arquivo")
    void bloqueioNaoTrocaOContextoAtivo() {
        gerenciador.definirContextoAtivo("gundam_0083");

        assertThrows(IllegalArgumentException.class,
            () -> servico.ativar(documento("Mobile Suit Gundam 0083", "guilty_crown"), null));

        assertEquals("gundam_0083", gerenciador.obterIdContextoAtivo(),
            "a lore recusada foi ativada mesmo assim — o próximo arquivo da varredura herdaria ela");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: cache legado não tem carimbo e o operador escolhe a obra na UI. O
     * clique errado tem exatamente o mesmo efeito destrutivo que o carimbo errado, então a guarda
     * cobre os dois caminhos.
     */
    @Test
    @DisplayName("cache legado com fallback da obra errada também bloqueia")
    void fallbackDaObraErradaBloqueia() {
        var legado = documento("Mobile Suit Gundam 0083", null);

        var erro = assertThrows(IllegalArgumentException.class,
            () -> servico.ativar(legado, "guilty_crown"));

        assertTrue(erro.getMessage().contains("NÃO confere com o contexto selecionado"), erro::getMessage);
        assertTrue(erro.getMessage().contains("gundam_0083"), erro::getMessage);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o caso correto segue e devolve a lore do carimbo, ativada.
     */
    @Test
    @DisplayName("carimbo compatível com a pasta: ativa e devolve o contexto")
    void carimboCompativelSegue() {
        assertEquals("gundam_0083",
            servico.ativar(documento("Mobile Suit Gundam 0083", "gundam_0083"), null));
        assertEquals("gundam_0083", gerenciador.obterIdContextoAtivo());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: falha ABERTA quando não há prova. Uma pasta que nenhuma lore reconhece
     * não é evidência de erro, e bloquear aí pararia a manutenção de todo cache de obra ainda sem
     * vocabulário declarado — inclusive as pastas de serviço da raiz de cache.
     */
    @Test
    @DisplayName("pasta que nenhuma lore reconhece SEGUE com o carimbo")
    void pastaNaoReconhecidaSegue() {
        assertEquals("guilty_crown",
            servico.ativar(documento("Serie Que Ninguem Declarou", "guilty_crown"), null));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: quando duas obras reivindicam a pasta com a MESMA precisão, o catálogo
     * não consegue provar de quem é o arquivo. Aceitar o carimbo como desempate seria tomar por
     * prova justamente o que está sendo verificado — então bloqueia, mesmo com o carimbo entre as
     * empatadas, e a mensagem endereça quem mantém as lores.
     */
    @Test
    @DisplayName("pasta reivindicada por duas obras com a mesma precisão: bloqueia por AMBIGUIDADE")
    void pastaAmbiguaBloqueia() {
        var empatadas = new ContextoManutencaoCacheService(
            new GerenciadorContexto(List.of(
                new ContextoFake("alfa", "Alfa"),
                new ContextoFake("beta", "Beta"))),
            new ValidadorCompatibilidadeObraContexto());

        var erro = assertThrows(IllegalArgumentException.class,
            () -> empatadas.ativar(documento("Alfa Beta", "alfa"), null));

        assertTrue(erro.getMessage().contains("AMBÍGUA"),
            () -> "ambiguidade e divergência têm consertos diferentes e não podem compartilhar a "
                + "mensagem: " + erro.getMessage());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a obra do CACHE é a pasta-mãe. Um cache solto na raiz não tem pasta de
     * obra, e a ausência de obra é indeterminação — nunca divergência.
     */
    @Test
    @DisplayName("cache sem pasta de obra é indeterminado, não divergente")
    void cacheSemPastaDeObraSegue() {
        var solto = new CacheManutencaoService.DocumentoEditavel(
            Path.of("ep01.cache.json"), null, entradasVazias(),
            carimbo("guilty_crown"), true);

        assertEquals("guilty_crown", servico.ativar(solto, null));
    }

    private static CacheManutencaoService.DocumentoEditavel documento(String pastaObra, String contextoId) {
        return new CacheManutencaoService.DocumentoEditavel(
            Path.of("cache", pastaObra, "ep01.cache.json"),
            null,
            entradasVazias(),
            contextoId == null ? null : carimbo(contextoId),
            contextoId != null);
    }

    private static ProvenienciaCache carimbo(String contextoId) {
        return new ProvenienciaCache(
            ProvenienciaCache.SCHEMA_ATUAL, contextoId, "hash", "gemma", "en", "pt-br");
    }

    private static ArrayNode entradasVazias() {
        return MAPPER.createArrayNode();
    }

    /** Lore mínima: só id e nome de exibição, que já bastam para a identidade canônica. */
    private record ContextoFake(String id, String nome) implements ProvedorContexto {
        @Override public String getId() { return id; }
        @Override public String getNomeExibicao() { return nome; }
        @Override public String obterPromptSistema() { return "prompt de " + id; }
    }
}
