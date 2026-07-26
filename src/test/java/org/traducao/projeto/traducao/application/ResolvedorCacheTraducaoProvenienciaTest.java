package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.contexto.domain.SnapshotContexto;
import org.traducao.projeto.traducao.infrastructure.config.LlmProperties;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa a FRONTEIRA em que o contexto congelado do job vira carimbo de
 * cache. O hash da proveniência tem uma única fonte — {@link ProvenienciaCache#hashDe(String)}
 * — aplicada aqui, sobre o {@code promptSistema} do snapshot. Enquanto o hash morou no próprio
 * {@code SnapshotContexto}, o mesmo algoritmo existia em DOIS módulos: bastava um deles mudar
 * de formato para todo o cache já gravado em disco passar a ser lido como de outra origem e
 * ser descartado — milhares de traduções perdidas em silêncio.
 *
 * <p>INVARIANTES DO DOMÍNIO: {@code contextoId} e {@code contextoHash} saem do MESMO snapshot
 * recebido por parâmetro (nunca do contexto ativo global); o hash é exatamente
 * {@code ProvenienciaCache.hashDe(snapshot.promptSistema())}; prompts diferentes produzem
 * carimbos que não se reaproveitam entre si.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: snapshot neutro carimba {@code contextoId} nulo e a
 * comparação de proveniência trata nulo como divergência — o cache antigo não é reusado.
 */
class ResolvedorCacheTraducaoProvenienciaTest {

    /**
     * PROPÓSITO DE NEGÓCIO: dublê de lore mínimo, só para produzir um prompt de sistema real.
     * <p>INVARIANTES DO DOMÍNIO: retornos fixos; sem I/O.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    private record LoreFake(String id, String nome, String lore) implements ProvedorContexto {
        @Override public String getId() { return id; }
        @Override public String getNomeExibicao() { return nome; }
        @Override public String obterPromptSistema() { return ContextoPrompt.montar(nome, lore); }
        @Override public Set<String> termosProtegidos() { return Set.of(nome); }
        @Override public Map<String, String> correcoesTerminologia() { return Map.of(); }
    }

    private ResolvedorCacheTraducao resolvedor() {
        TradutorProperties props = new TradutorProperties(
            "entrada", "saida", "cache", 20, List.of(), "en", "pt-BR");
        LlmProperties llmProps = new LlmProperties(
            "http://127.0.0.1:1234/v1", "modelo-teste", 0.3, 2048,
            Duration.ofSeconds(5), Duration.ofSeconds(30));
        // pastasExecucao e resolvedorSaida só participam de resolverArquivoCache; a
        // proveniência não os toca, e passá-los nulos deixa isso explícito.
        return new ResolvedorCacheTraducao(null, null, llmProps, props);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: prova que o carimbo gravado no cache usa o hash do peer
     * {@code cachetraducao}, calculado sobre o prompt do snapshot recebido — e não um hash
     * paralelo mantido pelo peer {@code contexto}.
     */
    @Test
    void provenienciaUsaOHashDeProvenienciaCacheSobreOPromptDoSnapshot() {
        SnapshotContexto contexto = SnapshotContexto.de(
            new LoreFake("gundam_0083", "Gundam 0083", "Lore de 0083."));

        ProvenienciaCache carimbo = resolvedor().provenienciaDe(contexto);

        assertEquals(ProvenienciaCache.SCHEMA_ATUAL, carimbo.schemaVersion());
        assertEquals("gundam_0083", carimbo.contextoId(), "o id vem do snapshot do job");
        assertEquals(ProvenienciaCache.hashDe(contexto.promptSistema()), carimbo.contextoHash(),
            "o hash da proveniência tem uma única fonte: ProvenienciaCache.hashDe sobre o "
                + "promptSistema congelado");
        assertEquals("modelo-teste", carimbo.modeloLlm());
        assertEquals("en", carimbo.idiomaOrigem());
        assertEquals("pt-BR", carimbo.idiomaDestino());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: dois contextos diferentes precisam produzir carimbos que NÃO se
     * reaproveitam — é o que impede uma melhoria de lore (ou a obra errada) reusar traduções
     * antigas em silêncio.
     */
    @Test
    void contextosDiferentesProduzemCarimbosIncompativeis() {
        ResolvedorCacheTraducao resolvedor = resolvedor();

        ProvenienciaCache umaObra = resolvedor.provenienciaDe(
            SnapshotContexto.de(new LoreFake("gundam_0083", "Gundam 0083", "Lore de 0083.")));
        ProvenienciaCache outraObra = resolvedor.provenienciaDe(
            SnapshotContexto.de(new LoreFake("guilty_crown", "Guilty Crown", "Lore de GC.")));

        assertNotEquals(umaObra.contextoHash(), outraObra.contextoHash());
        assertTrue(!umaObra.mesmaProveniencia(outraObra),
            "carimbos de obras diferentes nunca podem ser considerados a mesma proveniência");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: job sem contexto (snapshot neutro) carimba id nulo — o que faz a
     * proveniência divergir de qualquer geração anterior, em vez de reusar cache por engano.
     */
    @Test
    void snapshotNeutroCarimbaIdNuloEHashDoPromptGenerico() {
        ProvenienciaCache carimbo = resolvedor().provenienciaDe(SnapshotContexto.NEUTRO);

        assertEquals(null, carimbo.contextoId());
        assertEquals(ProvenienciaCache.hashDe(SnapshotContexto.PROMPT_NEUTRO), carimbo.contextoHash());
    }
}
