package org.traducao.projeto.traducao.infrastructure.adapters;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;
import org.traducao.projeto.qualidadeTraducao.domain.LoreAtivaPort;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: liga o contrato {@link LoreAtivaPort}, exigido pelo peer
 * {@code qualidadeTraducao}, à fonte real de contexto do sistema, o
 * {@link GerenciadorContexto}. É o único ponto de composição dessa inversão: o peer
 * permanece ignorante do {@code contexto} e a fatia {@code traducao} assume a ligação.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Adapter puro de delegação: repassa cada chamada ao {@link GerenciadorContexto}
 *       sem normalizar, filtrar, corrigir ou reinterpretar o retorno.</li>
 *   <li>Único adapter da porta; não há implementação concorrente em {@code contexto},
 *       em {@code qualidadeTraducao.infrastructure} nem por fatia.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Não adiciona tratamento próprio: o comportamento observável é exatamente o do
 * {@link GerenciadorContexto} — que não lança e degrada para conjunto vazio / lore
 * neutra quando não há contexto ativo.
 */
@Component
public class LoreAtivaContextoAdapter implements LoreAtivaPort {

    private final GerenciadorContexto gerenciadorContexto;

    /**
     * PROPÓSITO DE NEGÓCIO: recebe a fonte de contexto do sistema para satisfazer a
     * porta de lore ativa exigida pelo peer de qualidade.
     * <p>INVARIANTES DO DOMÍNIO: guarda a referência recebida sem substituí-la nem
     * criar outra instância de {@link GerenciadorContexto}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não valida o argumento; injeção CDI garante o
     * bean único.
     *
     * @param gerenciadorContexto gerenciador de contexto ativo do sistema
     */
    public LoreAtivaContextoAdapter(GerenciadorContexto gerenciadorContexto) {
        this.gerenciadorContexto = gerenciadorContexto;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: expõe os termos protegidos do contexto ativo ao peer de
     * qualidade, delegando ao gerenciador.
     * <p>INVARIANTES DO DOMÍNIO: retorna o conjunto do gerenciador tal como recebido,
     * sem cópia defensiva, filtragem ou ordenação.
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga o comportamento do gerenciador, que
     * retorna conjunto vazio sem contexto ativo.
     */
    @Override
    public Set<String> termosProtegidosAtivos() {
        return gerenciadorContexto.termosProtegidosAtivos();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: expõe a lore do contexto ativo ao peer de qualidade,
     * delegando ao gerenciador.
     * <p>INVARIANTES DO DOMÍNIO: retorna a string do gerenciador sem strip, reformatação
     * ou substituição.
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga o comportamento do gerenciador, que sem
     * contexto ativo devolve a lore neutra.
     */
    @Override
    public String obterLoreAtiva() {
        return gerenciadorContexto.obterLoreAtiva();
    }
}
