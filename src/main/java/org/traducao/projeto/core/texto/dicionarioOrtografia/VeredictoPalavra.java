package org.traducao.projeto.core.texto.dicionarioOrtografia;

/**
 * PROPÓSITO DE NEGÓCIO: o que uma palavra É, depois de consultados os dicionários dos idiomas que
 * aparecem numa legenda de anime traduzida do inglês.
 *
 * <h2>Por que não basta "certa ou errada"</h2>
 * Medido no acervo em 13/08/2026: das formas que o português não reconhece, uma parte é inglês
 * legítimo ({@code suit}, {@code cockpit}, {@code shuttle} — 435 ocorrências só no Zeta), outra é
 * termo alemão de lore ({@code Kamille} 950, {@code Sieg}, {@code Nordlicht} — 155 formas, porque
 * anime usa alemão à beça), e outra é erro de verdade. Tratar tudo como erro afogaria os
 * defeitos reais em ruído; tratar tudo como aceitável não corrigiria nada.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só o português DECIDE se está errado. Os demais idiomas ROTULAM o que já não é português
 *       — mudam a AÇÃO, nunca o veredito. A cicatriz é medida: {@code Resonância} (grafia errada;
 *       o certo é <i>ressonância</i>) foi reprovada pelo pt_BR e ACEITA pelo alemão. Se o alemão
 *       pudesse aprovar, o erro passaria.</li>
 *   <li>{@link #NAO_VERIFICADO} existe e não é sinônimo de {@link #PORTUGUES_OK} — é o estado 2
 *       da guarda de três estados aplicado a cada palavra.</li>
 * </ul>
 */
public enum VeredictoPalavra {

    /** O português reconhece. Nada a fazer. */
    PORTUGUES_OK("ok"),

    /** Não é português, mas existe acentuada — é falta de acento, e tem conserto automático. */
    ACENTO_FALTANDO("corrigir"),

    /** Não é português e é inglês válido: resíduo de tradução, ou termo técnico assumido. */
    RESIDUO_INGLES("revisar"),

    /** Não é português e é alemão válido: quase sempre nome ou termo de lore. Preservar. */
    TERMO_ALEMAO("preservar"),

    /** Contém kana ou kanji: japonês de verdade no meio da legenda. */
    JAPONES("revisar"),

    /** Nenhum dicionário reconhece: nome próprio da obra, invenção do autor, ou erro de digitação. */
    DESCONHECIDA("revisar"),

    /** Nenhum dicionário respondeu. NÃO é aprovação. */
    NAO_VERIFICADO("nao verificado");

    private final String acaoSugerida;

    VeredictoPalavra(String acaoSugerida) {
        this.acaoSugerida = acaoSugerida;
    }

    /** O que fazer com uma palavra deste veredicto — para o relatório, não para decidir sozinho. */
    public String acaoSugerida() {
        return acaoSugerida;
    }

    /** Se este veredicto autoriza correção automática. Só um autoriza, e de propósito. */
    public boolean corrigivelAutomaticamente() {
        return this == ACENTO_FALTANDO;
    }
}
