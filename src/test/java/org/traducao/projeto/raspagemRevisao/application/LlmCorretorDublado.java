package org.traducao.projeto.raspagemRevisao.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import org.traducao.projeto.llm.domain.LlmPort;
import org.traducao.projeto.llm.domain.Lote;
import org.traducao.projeto.llm.domain.StatusLlm;
import org.traducao.projeto.llm.domain.TraducaoLote;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PROPÓSITO DE NEGÓCIO: substitui o LLM no caminho REAL da revisão de concordância, para provar o
 * elo que o corretor determinístico não alcança — detectar, pedir ao modelo, julgar a resposta na
 * guarda e GRAVAR no arquivo. Sem rede e sem LM Studio.
 *
 * <h2>Por que corrige o texto RECEBIDO em vez de devolver um literal</h2>
 * A fala chega aqui mascarada: tags viram {@code [[TAGn]]} e termos de lore viram marcadores
 * próprios. Um dublê que devolvesse uma string fixa perderia esses marcadores, e a proposta seria
 * recusada por {@code LLM_MARCADOR_LORE_INCOMPATIVEL} ou {@code LLM_ESTRUTURA_ASS_SUSPEITA} — o
 * teste observaria "não corrigiu" e estaria medindo o dublê, não o pipeline. Aplicando a troca
 * SOBRE o texto recebido, tudo que não é a palavra trocada volta byte a byte.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li><b>{@code @Alternative} SEM {@code @Priority}</b>, e isso não é descuido. No Arc, uma
 *       alternativa com prioridade é habilitada GLOBALMENTE — e um segundo bean de
 *       {@link LlmPort} no container faz {@code GrafoCdiTraducaoIT} reprovar, porque o baseline
 *       de arquitetura exige que o contrato resolva como bean ÚNICO
 *       ({@code LlmClientAdapter}). A catraca pegou exatamente isso na primeira execução. Sem
 *       prioridade, quem habilita é só o {@code getEnabledAlternatives()} do perfil de teste.</li>
 *   <li>Só entra em cena para os testes que a declararem em {@code getEnabledAlternatives()}.</li>
 *   <li>Registra cada pedido, para o teste poder afirmar que o LLM foi (ou não foi) consultado.</li>
 *   <li>{@link #corrigirNada} produz um modelo que responde SEM alterar nada — é o controle que
 *       distingue "o pipeline não chamou" de "o pipeline chamou e o modelo não resolveu".</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nunca lança e nunca acessa rede. Sem substituições configuradas, devolve o texto intacto — que o
 * provedor trata como recusa {@code LLM_SEM_ALTERACAO}, exatamente como faria com o modelo real.
 */
@Alternative
@ApplicationScoped
public class LlmCorretorDublado implements LlmPort {

    private final List<String> pedidos = new ArrayList<>();
    private final List<String[]> substituicoes = new ArrayList<>();
    private boolean corrigirNada;

    /**
     * PROPÓSITO DE NEGÓCIO: zera o dublê entre testes — o bean é de aplicação e sobrevive a eles.
     * <p>INVARIANTES DO DOMÍNIO: sem isto, um teste herdaria as substituições do anterior.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    public void reiniciar() {
        pedidos.clear();
        substituicoes.clear();
        corrigirNada = false;
    }

    /** Ensina o modelo dublado a trocar {@code de} por {@code para} no texto que receber. */
    public void ensinar(String de, String para) {
        substituicoes.add(new String[] {de, para});
    }

    /** O modelo responde, mas devolve o texto como está — recusa por ausência de melhoria. */
    public void responderSemAlterar() {
        corrigirNada = true;
    }

    /** Quantas falas chegaram ao LLM. */
    public int chamadas() {
        return pedidos.size();
    }

    /** Os textos pedidos, na ordem — permite afirmar o que o pipeline mandou revisar. */
    public List<String> pedidos() {
        return List.copyOf(pedidos);
    }

    @Override
    public Optional<String> revisarConcordancia(
            String originalInglesMascarado, String traducaoPtMascarada, List<String> problemas) {
        return responder(traducaoPtMascarada);
    }

    @Override
    public Optional<String> corrigirTraducao(
            String originalInglesMascarado, String traducaoPtMascarada, String motivo) {
        return responder(traducaoPtMascarada);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aplica as trocas ensinadas sobre o texto recebido.
     * <p>INVARIANTES DO DOMÍNIO: tudo que não é a palavra trocada volta byte a byte, preservando
     * marcadores de tag e de lore.
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo devolve vazio, como um modelo sem resposta.
     */
    private Optional<String> responder(String traducaoPtMascarada) {
        if (traducaoPtMascarada == null) {
            return Optional.empty();
        }
        pedidos.add(traducaoPtMascarada);
        if (corrigirNada) {
            return Optional.of(traducaoPtMascarada);
        }
        String resposta = traducaoPtMascarada;
        for (String[] par : substituicoes) {
            resposta = resposta.replace(par[0], par[1]);
        }
        return Optional.of(resposta);
    }

    @Override
    public TraducaoLote traduzir(Lote lote) {
        return new TraducaoLote(lote.idLote(), lote.linhasOriginais(), true, null);
    }

    @Override
    public StatusLlm verificarDisponibilidade() {
        return new StatusLlm(true, true, "dublê de teste");
    }
}
