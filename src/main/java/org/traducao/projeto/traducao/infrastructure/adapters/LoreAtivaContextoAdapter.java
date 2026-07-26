package org.traducao.projeto.traducao.infrastructure.adapters;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.SnapshotContexto;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;
import org.traducao.projeto.qualidadeTraducao.domain.LoreAtivaPort;
import org.traducao.projeto.traducao.application.ContextoCongeladoDaExecucao;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: liga o contrato {@link LoreAtivaPort}, exigido pelo peer
 * {@code qualidadeTraducao}, à fonte real de contexto do sistema. É o único ponto de
 * composição dessa inversão: o peer permanece ignorante do {@code contexto} e a fatia
 * {@code traducao} assume a ligação.
 *
 * <p>A fonte é escolhida por PRECEDÊNCIA, e essa precedência é a razão de ser desta classe
 * depois da guarda de contexto: quando há um {@link SnapshotContexto} congelado para a
 * execução em curso, ele vence; só na ausência dele o adapter recai no
 * {@link GerenciadorContexto}. Sem isso, a validação de cada fala continuaria consultando o
 * contexto ATIVO GLOBAL enquanto o prompt e a proveniência já estavam congelados — e uma
 * troca de obra no combo durante um episódio faria a segunda metade das falas ser julgada
 * com os termos protegidos e a lore de outra obra, sem nenhum sinal.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Com snapshot em vigor: entrega os valores DAQUELE snapshot, imutáveis e coerentes
 *       entre si (termos e lore sempre da mesma obra).</li>
 *   <li>Sem snapshot em vigor: delegação pura ao {@link GerenciadorContexto}, repassando
 *       cada chamada sem normalizar, filtrar, corrigir ou reinterpretar o retorno — o
 *       comportamento histórico, preservado para as rotas que não congelam contexto
 *       (correção, revisão, karaokê).</li>
 *   <li>Único adapter da porta; não há implementação concorrente em {@code contexto},
 *       em {@code qualidadeTraducao.infrastructure} nem por fatia.</li>
 *   <li>Nunca escreve: não define, não troca e não limpa contexto — só lê.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Não adiciona tratamento próprio. Sem snapshot, o comportamento observável é exatamente o
 * do {@link GerenciadorContexto} — que não lança e degrada para conjunto vazio / lore neutra
 * quando não há contexto ativo. Com snapshot, os valores já são não nulos por construção do
 * próprio {@link SnapshotContexto}.
 */
@Component
public class LoreAtivaContextoAdapter implements LoreAtivaPort {

    private final GerenciadorContexto gerenciadorContexto;
    private final ContextoCongeladoDaExecucao contextoCongelado;

    /**
     * PROPÓSITO DE NEGÓCIO: recebe as duas fontes de lore — o contexto congelado da execução
     * em curso e o contexto ativo global — para satisfazer a porta exigida pelo peer de
     * qualidade com a precedência correta.
     *
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas sem substituí-las nem criar
     * outra instância; a precedência (congelado antes de global) é decidida em cada leitura,
     * nunca fixada na construção.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não valida os argumentos; injeção CDI garante os
     * beans únicos.
     *
     * @param gerenciadorContexto gerenciador de contexto ativo do sistema (fonte de fallback)
     * @param contextoCongelado vínculo do snapshot congelado da execução em curso
     */
    public LoreAtivaContextoAdapter(
        GerenciadorContexto gerenciadorContexto,
        ContextoCongeladoDaExecucao contextoCongelado
    ) {
        this.gerenciadorContexto = gerenciadorContexto;
        this.contextoCongelado = contextoCongelado;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: expõe ao peer de qualidade os termos protegidos da obra que esta
     * execução está de fato traduzindo.
     *
     * <p>INVARIANTES DO DOMÍNIO: com snapshot em vigor devolve o conjunto imutável dele; sem
     * snapshot, o conjunto do gerenciador tal como recebido, sem cópia defensiva, filtragem
     * ou ordenação.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga o comportamento do gerenciador, que retorna
     * conjunto vazio sem contexto ativo.
     */
    @Override
    public Set<String> termosProtegidosAtivos() {
        SnapshotContexto congelado = contextoCongelado.atual();
        return congelado != null ? congelado.termosProtegidos() : gerenciadorContexto.termosProtegidosAtivos();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: expõe ao peer de qualidade a lore da obra que esta execução está
     * de fato traduzindo.
     *
     * <p>INVARIANTES DO DOMÍNIO: com snapshot em vigor devolve a lore dele — a mesma lore por
     * trás do prompt enviado ao LLM neste arquivo; sem snapshot, a string do gerenciador sem
     * strip, reformatação ou substituição.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga o comportamento do gerenciador, que sem
     * contexto ativo devolve a lore neutra.
     */
    @Override
    public String obterLoreAtiva() {
        SnapshotContexto congelado = contextoCongelado.atual();
        return congelado != null ? congelado.lore() : gerenciadorContexto.obterLoreAtiva();
    }
}
