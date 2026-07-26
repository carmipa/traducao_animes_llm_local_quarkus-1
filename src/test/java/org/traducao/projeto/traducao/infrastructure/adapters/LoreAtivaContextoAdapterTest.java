package org.traducao.projeto.traducao.infrastructure.adapters;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.contexto.domain.SnapshotContexto;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;
import org.traducao.projeto.traducao.application.ContextoCongeladoDaExecucao;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * PROPÓSITO DE NEGÓCIO: prova a PRECEDÊNCIA de fontes de {@link LoreAtivaContextoAdapter} —
 * o único ponto de composição que liga a porta {@code LoreAtivaPort} do peer de qualidade à
 * fonte real de contexto. Sem snapshot congelado, o adapter é delegação pura ao
 * {@link GerenciadorContexto} (comportamento histórico, preservado para as rotas que não
 * congelam contexto). Com snapshot congelado, ele entrega o snapshot — e é isso que impede a
 * validação de julgar a segunda metade de um episódio com os termos de outra obra quando o
 * operador troca a lore no combo durante a execução.
 *
 * <p>INVARIANTES DO DOMÍNIO: sem snapshot, a saída do adapter é IGUAL à do gerenciador para
 * qualquer estado dele (sem contexto ou com contexto ativo); com snapshot, a saída é a do
 * snapshot mesmo que o contexto global mude depois; limpar o vínculo devolve o adapter à
 * delegação pura.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer divergência reprova. A comparação do caso sem
 * snapshot é sempre contra a saída do próprio gerenciador, de modo que o teste não congela o
 * fallback interno do {@link ContextoPrompt} como regra.
 */
class LoreAtivaContextoAdapterTest {

    /**
     * PROPÓSITO DE NEGÓCIO: contexto de teste com lore e termos protegidos reais, para
     * exercitar o adapter com um contexto ATIVO não vazio.
     * <p>INVARIANTES DO DOMÍNIO: declara termos protegidos e um prompt montado com lore,
     * espelhando um provedor real de obra.
     * <p>COMPORTAMENTO EM CASO DE FALHA: retornos fixos e determinísticos; não lança.
     */
    private static final class ContextoComLore implements ProvedorContexto {
        private static final String PROMPT = ContextoPrompt.montar("Teste", "Principais nomes: Bell Cranel.");

        @Override
        public String getId() {
            return "danmachi";
        }

        @Override
        public String getNomeExibicao() {
            return "Teste";
        }

        @Override
        public String obterPromptSistema() {
            return PROMPT;
        }

        @Override
        public Set<String> termosProtegidos() {
            return Set.of("Bell Cranel", "Hestia");
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: segunda obra registrada, para provar que trocar o contexto ativo
     * do gerenciador NÃO altera o que o adapter entrega enquanto há snapshot congelado.
     * <p>INVARIANTES DO DOMÍNIO: termos e lore deliberadamente disjuntos dos da primeira obra.
     * <p>COMPORTAMENTO EM CASO DE FALHA: retornos fixos; não lança.
     */
    private static final class ContextoOutraObra implements ProvedorContexto {
        private static final String PROMPT = ContextoPrompt.montar("Outra", "Principais nomes: Anavel Gato.");

        @Override
        public String getId() {
            return "outra_obra";
        }

        @Override
        public String getNomeExibicao() {
            return "Outra Obra";
        }

        @Override
        public String obterPromptSistema() {
            return PROMPT;
        }

        @Override
        public Set<String> termosProtegidos() {
            return Set.of("Anavel Gato");
        }
    }

    @Test
    void semContextoAtivoDelegaTermosVaziosELoreNeutraDoGerenciador() {
        GerenciadorContexto gerenciador = new GerenciadorContexto(List.of());
        LoreAtivaContextoAdapter adapter =
            new LoreAtivaContextoAdapter(gerenciador, new ContextoCongeladoDaExecucao());

        assertEquals(gerenciador.termosProtegidosAtivos(), adapter.termosProtegidosAtivos());
        assertEquals(Set.of(), adapter.termosProtegidosAtivos());
        assertEquals(gerenciador.obterLoreAtiva(), adapter.obterLoreAtiva());
    }

    @Test
    void comContextoAtivoDelegaTermosELoreIdenticosAoGerenciador() {
        GerenciadorContexto gerenciador = new GerenciadorContexto(List.of(new ContextoComLore()));
        LoreAtivaContextoAdapter adapter =
            new LoreAtivaContextoAdapter(gerenciador, new ContextoCongeladoDaExecucao());

        assertEquals(gerenciador.termosProtegidosAtivos(), adapter.termosProtegidosAtivos());
        assertEquals(Set.of("Bell Cranel", "Hestia"), adapter.termosProtegidosAtivos());
        assertEquals(gerenciador.obterLoreAtiva(), adapter.obterLoreAtiva());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: com um snapshot congelado em vigor, trocar o contexto ativo
     * global NO MEIO da execução não muda uma vírgula do que a validação enxerga. É a
     * invariante central da guarda: prompt, proveniência e validação de um mesmo arquivo
     * saem todos da MESMA obra.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: se o adapter voltar a consultar o gerenciador
     * primeiro, os termos viram os de "outra_obra" no meio do arquivo — exatamente o modo
     * de falha que produziu os 15 caches contaminados.
     */
    @Test
    void snapshotCongeladoVenceTrocaDoContextoGlobalNoMeioDaExecucao() {
        GerenciadorContexto gerenciador = new GerenciadorContexto(
            List.of(new ContextoComLore(), new ContextoOutraObra()));
        ContextoCongeladoDaExecucao congelado = new ContextoCongeladoDaExecucao();
        LoreAtivaContextoAdapter adapter = new LoreAtivaContextoAdapter(gerenciador, congelado);

        gerenciador.definirContextoAtivo("danmachi");
        SnapshotContexto snapshot = gerenciador.snapshotAtivo();
        congelado.definir(snapshot);

        // O operador troca a obra no combo enquanto o episódio traduz.
        gerenciador.definirContextoAtivo("outra_obra");

        assertEquals(Set.of("Bell Cranel", "Hestia"), adapter.termosProtegidosAtivos(),
            "com snapshot congelado, a troca do contexto global não pode vazar para a validação");
        assertEquals(snapshot.lore(), adapter.obterLoreAtiva(),
            "a lore consultada continua sendo a do snapshot congelado");
        assertNotEquals(gerenciador.termosProtegidosAtivos(), adapter.termosProtegidosAtivos(),
            "o gerenciador já mudou de obra; o adapter, não — é essa diferença que prova o congelamento");

        // Fim do arquivo: sem vínculo, o adapter volta a ser delegação pura.
        congelado.limpar();
        assertEquals(gerenciador.termosProtegidosAtivos(), adapter.termosProtegidosAtivos(),
            "limpo o vínculo, o adapter volta a refletir o contexto ativo global");
        assertEquals(gerenciador.obterLoreAtiva(), adapter.obterLoreAtiva());
    }
}
