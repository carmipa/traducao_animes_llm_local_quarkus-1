package org.traducao.projeto.traducao.application;

import org.springframework.stereotype.Component;
import org.traducao.projeto.lore.domain.SnapshotContexto;

/**
 * PROPÓSITO DE NEGÓCIO: PONTE TEMPORÁRIA — mantém, enquanto um arquivo é traduzido, qual
 * {@link SnapshotContexto} está em vigor, para o ÚNICO consumidor que ainda não recebe o
 * snapshot por parâmetro: a validação de cada fala, que atravessa a porta
 * {@code qualidadeTraducao.domain.LoreAtivaPort} (implementada pelo
 * {@code LoreAtivaContextoAdapter} e consumida por {@code DetectorTraducaoIdenticaService} e
 * {@code RecuperarPendenciaFallbackService}). Essa porta pertence ao peer de qualidade e não
 * conhece — nem deve conhecer — o tipo {@code SnapshotContexto} do peer {@code contexto};
 * enquanto ela não ganhar um parâmetro de lore, o vínculo por thread é o que impede a
 * validação de recair no contexto ATIVO GLOBAL e julgar a segunda metade de um episódio com
 * os termos protegidos de outra obra.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>NÃO é o contrato principal de propagação de contexto. O contrato é o PARÂMETRO: o job
 *       resolve um {@link SnapshotContexto} uma vez e o passa explicitamente a
 *       {@code ProcessarArquivoUseCase.processar(...)}, que por sua vez o repassa à guarda
 *       obra×contexto, ao carimbo de proveniência, ao prompt do LLM, ao reforço de
 *       terminologia e à telemetria. Nenhum consumidor NOVO pode ler daqui: quem precisa do
 *       contexto recebe-o na assinatura.</li>
 *   <li>PRAZO: esta ponte sai quando {@code LoreAtivaPort} passar a receber a lore/termos por
 *       argumento (ou quando a validação de fala for movida para dentro do escopo que já
 *       carrega o snapshot). Até lá ela é o único uso legítimo — e o {@code ThreadLocal} não
 *       pode ganhar um segundo consumidor a título de conveniência.</li>
 *   <li>O vínculo é POR THREAD ({@link ThreadLocal}): a fila única do pipeline processa um
 *       arquivo por vez em uma thread, e nenhuma execução enxerga o snapshot de outra.</li>
 *   <li>Ausência de snapshot é um estado NORMAL, não um erro: {@link #atual()} devolve
 *       {@code null} e quem consulta recai no contexto ativo global — que é o comportamento
 *       histórico, preservado para todas as rotas que não congelam contexto (correção,
 *       revisão, karaokê).</li>
 *   <li>Quem define é quem limpa, em {@code try/finally}: um snapshot vazado sobreviveria à
 *       thread e contaminaria o próximo job da MESMA fila com a lore de um arquivo anterior
 *       — exatamente a classe de bug que este mecanismo existe para eliminar.</li>
 *   <li>É um vínculo de execução, não uma fonte de verdade: nunca escolhe, valida ou troca
 *       contexto — só guarda o que o job congelou e passou por parâmetro.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nenhum método lança. {@link #definir(SnapshotContexto)} com {@code null} equivale a
 * {@link #limpar()} (não deixa vínculo pela metade) e {@link #limpar()} é idempotente,
 * podendo ser chamado no {@code finally} mesmo quando a definição nunca ocorreu.
 */
@Component
public class ContextoCongeladoDaExecucao {

    private final ThreadLocal<SnapshotContexto> emVigor = new ThreadLocal<>();

    /**
     * PROPÓSITO DE NEGÓCIO: vincula o snapshot congelado à execução em curso, para que
     * todos os colaboradores consultados durante o arquivo vejam a MESMA obra.
     *
     * <p>INVARIANTES DO DOMÍNIO: afeta somente a thread chamadora; substitui um vínculo
     * anterior da mesma thread sem mesclar.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code null} remove o vínculo em vez de guardar um
     * snapshot nulo, para que {@link #atual()} continue significando "não há contexto
     * congelado" e não "há um contexto vazio".
     *
     * @param snapshot contexto congelado da execução; {@code null} desfaz o vínculo
     */
    public void definir(SnapshotContexto snapshot) {
        if (snapshot == null) {
            emVigor.remove();
            return;
        }
        emVigor.set(snapshot);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: desfaz o vínculo ao terminar (ou abortar) o arquivo, para que o
     * próximo job da fila não herde a lore do anterior.
     *
     * <p>INVARIANTES DO DOMÍNIO: idempotente e restrito à thread chamadora; remove a
     * entrada do {@link ThreadLocal} em vez de guardar {@code null}, evitando reter o
     * snapshot em threads de pool de vida longa.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: seguro no {@code finally} mesmo sem definição
     * prévia; não lança.
     */
    public void limpar() {
        emVigor.remove();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: entrega o contexto congelado da execução em curso a quem precisa
     * julgar uma fala com a MESMA lore que produziu o prompt e a proveniência.
     *
     * <p>INVARIANTES DO DOMÍNIO: devolve exatamente o valor imutável vinculado a esta
     * thread; não copia, não mescla e não consulta o contexto global.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem vínculo devolve {@code null} — sinal explícito
     * para o chamador recair no contexto ativo global.
     *
     * @return o snapshot em vigor nesta thread, ou {@code null} se não houver
     */
    public SnapshotContexto atual() {
        return emVigor.get();
    }
}
