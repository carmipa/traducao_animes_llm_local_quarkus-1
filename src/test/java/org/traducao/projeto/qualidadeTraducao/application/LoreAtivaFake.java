package org.traducao.projeto.qualidadeTraducao.application;

import org.traducao.projeto.qualidadeTraducao.domain.LoreAtivaPort;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: dublê de {@link LoreAtivaPort} para os testes que precisam construir
 * {@link ValidadorTraducaoService} sem subir CDI nem montar um catálogo de contextos.
 *
 * <p>Existe porque o validador passou a consultar a lore ativa (medição de 2026-07-28: a limpeza
 * apagou quatro traduções corretas de Zeta que continham {@code "The O"}), e treze pontos de
 * teste construíam o serviço sem argumento. {@link #vazia()} reproduz EXATAMENTE o estado que
 * esses testes enxergavam antes — nenhum termo protegido — para que a mudança não altere
 * silenciosamente o que eles já provavam.
 *
 * <p>INVARIANTES DO DOMÍNIO: retornos fixos e determinísticos; nenhum estado mutável. É classe
 * de teste e não é varrida pelas fronteiras ArchUnit, que importam com
 * {@code DO_NOT_INCLUDE_TESTS}.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; a porta real também promete não lançar.
 */
public final class LoreAtivaFake implements LoreAtivaPort {

    private static final String LORE_NEUTRA = "";

    private final Set<String> termos;
    private final Set<java.util.List<String>> pares;

    private LoreAtivaFake(Set<String> termos) {
        this(termos, Set.of());
    }

    private LoreAtivaFake(Set<String> termos, Set<java.util.List<String>> pares) {
        this.termos = termos;
        this.pares = pares;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: cenário com pares inconfundíveis declarados, para provar que a
     * troca de uma entidade da obra por outra é acusada.
     * <p>INVARIANTES DO DOMÍNIO: cada par tem exatamente dois termos; o dublê não valida isso —
     * quem consome é que ignora par malformado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     *
     * @param pares pares como declarados em {@code ProvedorContexto.paresInconfundiveis()}
     */
    @SafeVarargs
    public static LoreAtivaFake comPares(java.util.List<String>... pares) {
        return new LoreAtivaFake(Set.of(), Set.of(pares));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: cenário "sem contexto ativo" — o comportamento histórico do validador.
     * <p>INVARIANTES DO DOMÍNIO: conjunto vazio, igual ao que a porta real devolve sem contexto.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    public static LoreAtivaFake vazia() {
        return new LoreAtivaFake(Set.of());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: cenário com obra ativa, para provar que nome próprio declarado na
     * lore deixa de ser lido como resíduo em inglês.
     * <p>INVARIANTES DO DOMÍNIO: o conjunto recebido é copiado; o dublê não compartilha estado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     *
     * @param termosProtegidos termos canônicos da obra, como declarados em {@code termosProtegidos()}
     */
    public static LoreAtivaFake com(String... termosProtegidos) {
        return new LoreAtivaFake(Set.of(termosProtegidos));
    }

    @Override
    public Set<String> termosProtegidosAtivos() {
        return termos;
    }

    @Override
    public String obterLoreAtiva() {
        return LORE_NEUTRA;
    }

    @Override
    public Set<java.util.List<String>> paresInconfundiveisAtivos() {
        return pares;
    }
}
