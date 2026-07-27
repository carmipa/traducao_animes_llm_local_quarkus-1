package org.traducao.projeto.raspagemRevisao.domain;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: as regras que decidem QUANDO uma fala merece ser retraduzida do zero, em
 * vez de apenas ajustada. São três decisões distintas que a revisão toma lendo os mesmos motivos de
 * auditoria, e mantê-las juntas é o que impede que uma seja alterada sem que se olhe as outras.
 *
 * <h2>O vocabulário de motivos é um contrato por SUBSTRING</h2>
 * Os motivos chegam como texto livre, produzidos por dois lugares diferentes —
 * {@code qualidadeTraducao.ValidadorTraducaoService} e
 * {@code raspagemRevisao.AuditorProblemasLegendaService} — e são consumidos por comparação de
 * substring em três fatias ({@code raspagemRevisao}, {@code revisaoLore}). Não existe nenhum
 * vínculo de compilação entre quem escreve a frase e quem a procura: se o produtor reescrever a
 * mensagem, o consumidor para de reconhecê-la em silêncio, sem erro de compilação e sem
 * necessariamente quebrar um teste.
 *
 * <p>As constantes abaixo não resolvem isso — o produtor continua livre. O que elas fazem é reunir
 * num lugar só os trechos procurados, para que a mudança seja notada aqui em vez de caçada em seis
 * arquivos. A unificação de verdade exigiria um tipo de motivo compartilhado, e isso atravessa
 * fatias e peers: é decisão própria, não um efeito colateral desta extração.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Puro: só JDK, sem framework, sem I/O, sem conhecer legenda, cache ou provedor.</li>
 *   <li>{@link #exigeRetraducaoPeloGoogle(List)} e {@link #exigeRetraducaoCompletaPeloLlm(List)}
 *       são decisões DIFERENTES sobre provedores diferentes, e por isso leem conjuntos diferentes
 *       de motivos. Estavam a 350 linhas de distância uma da outra, uma como método nomeado e a
 *       outra como {@code anyMatch} solto no meio de um método de 71 linhas, o que fazia parecerem
 *       cópias divergentes. Não são — e uni-las mudaria qual provedor atende quais falas.</li>
 *   <li>O trecho que a decisão do LLM procura ({@value #NAO_TRADUZIDA_AMPLO}) é mais largo que o
 *       que a do Google procura ({@value #NAO_TRADUZIDA}). Preservado exatamente como estava:
 *       estreitá-lo mandaria falas hoje retraduzidas por inteiro para a revisão pontual, e isso é
 *       mudança de comportamento do pipeline, não limpeza.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Lista nula ou vazia devolve {@code false} em todas as decisões — sem motivo conhecido, não se
 * retraduz. Nunca lança.
 */
public final class PoliticaRetraducao {

    /** Palavra ou construção em inglês que sobreviveu à tradução. */
    public static final String RESIDUO_GRINGO = "Resíduo gringo";

    /** A fala saiu idêntica ao original em inglês. Grafia emitida pelo auditor. */
    public static final String NAO_TRADUZIDA = "Fala não traduzida";

    /**
     * Variante LARGA do anterior, usada só na decisão do LLM. Casa qualquer motivo que contenha
     * "não traduzida", inclusive o acima. Hoje nenhum produtor emite outra frase com esse trecho —
     * a diferença é latente, não ativa —, mas está preservada porque estreitá-la mudaria o provedor
     * escolhido para falas reais.
     */
    public static final String NAO_TRADUZIDA_AMPLO = "não traduzida";

    /** A tradução não está em português. */
    public static final String IDIOMA_INCORRETO = "Idioma incorreto";

    /** O modelo respondeu com conversa em vez de tradução. */
    public static final String PREAMBULO = "Preâmbulo";

    /** O provedor devolveu um marcador de erro no lugar do texto. */
    public static final String MARCADOR_ERRO = "Marcador de erro de tradução";

    /** Falas não traduzidas necessárias para suspeitar de retradução em massa. */
    private static final int LIMIAR_ABSOLUTO = 20;

    /** O mesmo limite em proporção: 1/10 das falas auditáveis. */
    private static final int DIVISOR_PROPORCAO = 10;

    private PoliticaRetraducao() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: restringe o GOOGLE a falhas objetivas de tradução, deixando
     * concordância e estilo para o LLM local, que conhece a lore.
     *
     * <p>INVARIANTES DO DOMÍNIO: gênero, pronome e tratamento isolados NUNCA provocam retradução
     * completa pelo Google — mandar uma questão de concordância para um tradutor sem lore devolve
     * nomes próprios traduzidos.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: motivo desconhecido devolve {@code false} e a fala é
     * preservada para inspeção; lista nula devolve {@code false}.
     *
     * @param motivos motivos apurados pela auditoria da fala
     * @return {@code true} se a falha é objetiva o bastante para o Google
     */
    public static boolean exigeRetraducaoPeloGoogle(List<String> motivos) {
        if (motivos == null) {
            return false;
        }
        return motivos.stream().anyMatch(motivo ->
            motivo.contains(RESIDUO_GRINGO)
                || motivo.contains(NAO_TRADUZIDA)
                || motivo.contains(IDIOMA_INCORRETO)
                || motivo.contains(PREAMBULO)
                || motivo.contains(MARCADOR_ERRO));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: escolhe, DENTRO do LLM local, entre retraduzir a fala inteira e apenas
     * revisar a concordância. É uma decisão sobre profundidade de intervenção, não sobre provedor.
     *
     * <p>INVARIANTES DO DOMÍNIO: só resíduo em inglês e fala não traduzida justificam refazer o
     * texto do zero; qualquer outro motivo recebe intervenção pontual, que preserva mais do
     * trabalho já feito.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista nula devolve {@code false} — na dúvida, a
     * intervenção menor.
     *
     * @param motivos motivos apurados pela auditoria da fala
     * @return {@code true} para refazer a tradução inteira; {@code false} para revisão pontual
     */
    public static boolean exigeRetraducaoCompletaPeloLlm(List<String> motivos) {
        if (motivos == null) {
            return false;
        }
        return motivos.stream().anyMatch(
            m -> m.contains(RESIDUO_GRINGO) || m.contains(NAO_TRADUZIDA_AMPLO));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aplica o limite operacional que impede a etapa de revisão de assumir
     * silenciosamente o trabalho da Tradução Local. Uma legenda que chega quase inteira em inglês
     * não é um caso de revisão — é um arquivo que não foi traduzido, e processá-lo aqui gastaria a
     * GPU refazendo o pipeline errado.
     *
     * <p>INVARIANTES DO DOMÍNIO: exige SIMULTANEAMENTE {@value #LIMIAR_ABSOLUTO} falas e um décimo
     * do material auditável. Só a proporção deixaria um arquivo de 30 falas bloquear por causa de
     * 3; só o número absoluto deixaria um arquivo de 2.000 falas passar com 19 falhas.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: contagens inválidas (zero auditáveis, negativos) devolvem
     * {@code false} — não se bloqueia por falta de dados.
     *
     * @param falasAuditaveis falas com original comparável
     * @param falasNaoTraduzidas quantas delas saíram idênticas ao inglês
     * @return {@code true} se o arquivo deve ser bloqueado como retradução em massa
     */
    public static boolean excedeLimiarRetraducaoEmMassa(int falasAuditaveis, int falasNaoTraduzidas) {
        if (falasAuditaveis <= 0 || falasNaoTraduzidas < 0) {
            return false;
        }
        return falasNaoTraduzidas >= LIMIAR_ABSOLUTO
            && falasNaoTraduzidas * DIVISOR_PROPORCAO >= falasAuditaveis;
    }
}
