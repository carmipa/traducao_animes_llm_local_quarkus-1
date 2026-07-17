package org.traducao.projeto.traducao.infrastructure.adapters;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: prova que {@link LoreAtivaContextoAdapter} é uma delegação pura
 * ao {@link GerenciadorContexto} — o único ponto de composição que liga a porta
 * {@code LoreAtivaPort} do peer de qualidade à fonte real de contexto, sem alterar o que
 * o gerenciador entrega.
 *
 * <p>INVARIANTES DO DOMÍNIO: para qualquer estado do gerenciador (sem contexto ou com
 * contexto ativo) a saída do adapter é IGUAL à do gerenciador — mesmos termos, mesma
 * lore, sem normalização, filtragem ou substituição.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer divergência entre adapter e gerenciador
 * reprova o teste. A comparação é sempre contra a saída do próprio gerenciador, de modo
 * que o teste não congela o fallback interno do {@link ContextoPrompt} como regra.
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

    @Test
    void semContextoAtivoDelegaTermosVaziosELoreNeutraDoGerenciador() {
        GerenciadorContexto gerenciador = new GerenciadorContexto(List.of());
        LoreAtivaContextoAdapter adapter = new LoreAtivaContextoAdapter(gerenciador);

        assertEquals(gerenciador.termosProtegidosAtivos(), adapter.termosProtegidosAtivos());
        assertEquals(Set.of(), adapter.termosProtegidosAtivos());
        assertEquals(gerenciador.obterLoreAtiva(), adapter.obterLoreAtiva());
    }

    @Test
    void comContextoAtivoDelegaTermosELoreIdenticosAoGerenciador() {
        GerenciadorContexto gerenciador = new GerenciadorContexto(List.of(new ContextoComLore()));
        LoreAtivaContextoAdapter adapter = new LoreAtivaContextoAdapter(gerenciador);

        assertEquals(gerenciador.termosProtegidosAtivos(), adapter.termosProtegidosAtivos());
        assertEquals(Set.of("Bell Cranel", "Hestia"), adapter.termosProtegidosAtivos());
        assertEquals(gerenciador.obterLoreAtiva(), adapter.obterLoreAtiva());
    }
}
