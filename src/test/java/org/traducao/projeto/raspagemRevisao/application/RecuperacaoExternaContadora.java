package org.traducao.projeto.raspagemRevisao.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoRecuperacaoExterna;
import org.traducao.projeto.raspagemRevisao.domain.StatusRecuperacaoExterna;
import org.traducao.projeto.raspagemRevisao.domain.ports.RecuperacaoExternaRevisaoPort;

import java.util.ArrayList;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: substitui o tradutor externo nos testes que precisam do caminho REAL da
 * revisão sem tocar a rede — e, mais que isso, permite CONTAR as chamadas e interromper a execução
 * numa fala específica.
 *
 * <h2>Por que uma alternativa CDI e não Mockito</h2>
 * O projeto não tem {@code quarkus-junit5-mockito}, e a porta tem UM método. Um dublê explícito é
 * tipado, legível e não acrescenta dependência ao build. É o mesmo padrão que a fatia já usa em
 * dublês de construtor ({@code LlmStub}, {@code TelemetriaStub}); aqui a forma é CDI porque a
 * caracterização precisa passar pelo bean real do caso de uso.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só entra em cena para os testes que a declararem em {@code getEnabledAlternatives()}. Um
 *       {@code @Mock} global trocaria o bean para TODA a suíte {@code @QuarkusTest} e mudaria
 *       testes que ninguém olhou.</li>
 *   <li>Conta e REGISTRA cada texto pedido: é o que permite afirmar "a segunda fala não chamou de
 *       novo", que nenhum teste do projeto consegue afirmar hoje.</li>
 *   <li>A interrupção programada usa a MESMA thread do caso de uso — é ela que carrega a flag e o
 *       canal SSE.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nunca lança e nunca acessa rede.
 */
// SEM @Priority de propósito: no Arc, alternativa com prioridade é habilitada GLOBALMENTE, o que
// tornaria falsa a primeira linha dos invariantes abaixo — o dublê valeria para a suíte inteira,
// não só para quem o declara. Descoberto em 12/08/2026, quando um segundo bean de LlmPort com
// @Priority fez GrafoCdiTraducaoIT reprovar por ambiguidade no contrato.
@Alternative
@ApplicationScoped
public class RecuperacaoExternaContadora implements RecuperacaoExternaRevisaoPort {

    private final List<String> pedidos = new ArrayList<>();
    private int interromperNaChamada = -1;

    /**
     * PROPÓSITO DE NEGÓCIO: zera o dublê entre testes — o bean é de aplicação e sobrevive a eles.
     * <p>INVARIANTES DO DOMÍNIO: sem isto, um teste herdaria a contagem do anterior.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    public void reiniciar() {
        pedidos.clear();
        interromperNaChamada = -1;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: programa a interrupção para acontecer NO MEIO da varredura de falas.
     * <p>INVARIANTES DO DOMÍNIO: a flag é marcada na thread que executa a revisão, que é a mesma
     * que o laço consulta.
     * <p>COMPORTAMENTO EM CASO DE FALHA: valor não alcançado simplesmente não interrompe.
     *
     * @param n número da chamada (1 = a primeira) em que a thread será interrompida
     */
    public void interromperNaChamada(int n) {
        this.interromperNaChamada = n;
    }

    /** Quantas falas chegaram ao tradutor externo. */
    public int chamadas() {
        return pedidos.size();
    }

    /** Os textos pedidos, na ordem — permite afirmar que um texto repetido NÃO foi pedido duas vezes. */
    public List<String> pedidos() {
        return List.copyOf(pedidos);
    }

    @Override
    public ResultadoRecuperacaoExterna traduzir(String textoOriginal) {
        pedidos.add(textoOriginal);
        if (pedidos.size() == interromperNaChamada) {
            Thread.currentThread().interrupt();
        }
        // Portugues de verdade, nao "[PT] " + ingles: o portao `correcaoEhSegura` REJEITA uma
        // proposta que nao melhora a auditoria, entao um dubla preguicoso faria o teste observar
        // "nada foi corrigido" e nao o que ele quer medir. Descoberto ao ver o teste do dreno
        // reprovar por arquivo nao escrito.
        return new ResultadoRecuperacaoExterna(
            StatusRecuperacaoExterna.SUCESSO,
            "Esta fala foi devidamente traduzida para o português na chamada " + pedidos.size() + ".");
    }
}
