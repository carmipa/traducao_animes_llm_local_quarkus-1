package org.traducao.projeto.contexto.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: fotografia IMUTÁVEL do contexto/lore escolhido para UM job. Existe
 * porque o {@code GerenciadorContexto} guarda UM contexto ativo global e mutável: entre o
 * início e o fim da tradução de um lote, outra rota (ou o próximo clique do operador) pode
 * trocar a obra ativa e fazer o mesmo arquivo ser traduzido com um prompt, validado com
 * outros termos protegidos e carimbado no cache com uma terceira proveniência. O snapshot
 * elimina essa janela: o job resolve UMA vez, a partir do id EXPLÍCITO pedido, e passa este
 * valor adiante por PARÂMETRO — prompt do LLM, lore da validação, termos protegidos, mapa de
 * terminologia e id da proveniência saem todos daqui, e o global nunca é reconsultado.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Valor imutável: record com cópias defensivas imutáveis de {@code termosProtegidos}
 *       e {@code correcoesTerminologia}. Depois de criado, NADA que aconteça no
 *       {@code GerenciadorContexto} altera este objeto.</li>
 *   <li>Coerência interna: os cinco campos vêm do MESMO {@link ProvedorContexto}, lidos de
 *       uma só vez. É impossível um snapshot ter o prompt de uma obra e os termos de outra.</li>
 *   <li>NÃO carrega hash: o snapshot guarda apenas o {@link #promptSistema()} congelado. Quem
 *       carimba proveniência de cache é a fronteira de integração da fatia {@code traducao}
 *       ({@code ResolvedorCacheTraducao}), que deriva o hash com
 *       {@code ProvenienciaCache.hashDe(promptSistema())}. Duplicar o algoritmo aqui criava
 *       DUAS fontes do mesmo hash em módulos diferentes — e, se divergissem, todo o cache já
 *       gravado passaria a ser lido como de outra origem.</li>
 *   <li>{@link #lore()} é só a lore/terminologia crua (via {@link ContextoPrompt#obterLore})
 *       — sem prioridades nem regras de saída —, que é o que a validação de tradução
 *       idêntica consome.</li>
 *   <li>{@link #NEUTRO} reproduz fielmente o estado "sem contexto ativo" do gerenciador:
 *       id nulo, nome {@code "Padrao"}, prompt genérico, sem termos e sem correções.</li>
 *   <li>Domínio puro: só JDK. Não conhece cache, LLM, criptografia, telemetria nem fatia
 *       funcional.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nenhum método lança. Um provedor que devolva {@code null} em termos protegidos ou no mapa
 * de terminologia degrada para coleção vazia (no-op), nunca para {@code NullPointerException}
 * no meio de uma tradução.
 *
 * @param id identificador estável do contexto (o mesmo carimbado na proveniência do cache)
 * @param nomeExibicao rótulo de UI da obra, usado em log e telemetria
 * @param promptSistema prompt de sistema completo enviado ao LLM
 * @param lore apenas a lore/terminologia crua por trás do prompt
 * @param termosProtegidos termos que não devem ser traduzidos nesta obra
 * @param correcoesTerminologia mapa forma-ruim → termo canônico desta obra
 * @param paresInconfundiveis pares de termos desta obra que NÃO podem ser trocados um pelo
 *        outro; congelados aqui para que quem corrige o cache JÁ GRAVADO os obtenha da obra
 *        DONA do arquivo, e não da lore ativa global — que naquele fluxo é outra, ou nenhuma
 */
public record SnapshotContexto(
    String id,
    String nomeExibicao,
    String promptSistema,
    String lore,
    Set<String> termosProtegidos,
    Map<String, String> correcoesTerminologia,
    Set<List<String>> paresInconfundiveis
) {

    /**
     * Prompt genérico usado quando não há contexto ativo. É a MESMA string devolvida por
     * {@code GerenciadorContexto.obterPromptAtivo()} nesse estado; duplicá-la aqui manteria
     * o snapshot neutro alinhado com o comportamento histórico do gerenciador.
     */
    public static final String PROMPT_NEUTRO = "Voce e um tradutor especialista. Traduza fielmente.";

    /**
     * Rótulo de exibição usado quando não há contexto ativo — idêntico ao que
     * {@code GerenciadorContexto.obterNomeContextoAtivo()} devolve nesse estado.
     */
    public static final String NOME_NEUTRO = "Padrao";

    /**
     * Snapshot do estado "sem contexto ativo". Não é um contexto adivinhado: o {@code id}
     * nulo é o que faz a proveniência do cache divergir de qualquer geração anterior e o
     * que impede a guarda de obra×contexto de acusar divergência sem base.
     */
    public static final SnapshotContexto NEUTRO = new SnapshotContexto(
        null,
        NOME_NEUTRO,
        PROMPT_NEUTRO,
        ContextoPrompt.obterLore(PROMPT_NEUTRO),
        Set.of(),
        Map.of(),
        Set.of()
    );

    /**
     * PROPÓSITO DE NEGÓCIO: garante que o snapshot seja de fato imutável, mesmo recebendo
     * coleções mutáveis de um provedor de lore.
     *
     * <p>INVARIANTES DO DOMÍNIO: as coleções são copiadas para versões imutáveis; alterar a
     * coleção de origem depois da construção não altera o snapshot.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: coleção nula vira coleção vazia em vez de lançar.
     */
    public SnapshotContexto {
        termosProtegidos = termosProtegidos == null ? Set.of() : Set.copyOf(termosProtegidos);
        correcoesTerminologia = correcoesTerminologia == null ? Map.of() : Map.copyOf(correcoesTerminologia);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: congela um provedor de lore em um valor que o job carrega do
     * início ao fim, para que a troca de obra na UI durante uma tradução não vaze para o
     * meio do lote em curso.
     *
     * <p>INVARIANTES DO DOMÍNIO: lê o provedor UMA única vez e deriva a lore do mesmo prompt
     * lido — os campos nunca podem pertencer a obras diferentes.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: provedor nulo devolve {@link #NEUTRO}, o mesmo
     * estado que o gerenciador expõe sem contexto ativo.
     *
     * @param provedor provedor de lore a congelar; {@code null} devolve {@link #NEUTRO}
     * @return snapshot imutável do provedor
     */
    public static SnapshotContexto de(ProvedorContexto provedor) {
        if (provedor == null) {
            return NEUTRO;
        }
        String prompt = provedor.obterPromptSistema();
        return new SnapshotContexto(
            provedor.getId(),
            provedor.getNomeExibicao(),
            prompt,
            ContextoPrompt.obterLore(prompt),
            provedor.termosProtegidos(),
            provedor.correcoesTerminologia(),
            provedor.paresInconfundiveis()
        );
    }
}
