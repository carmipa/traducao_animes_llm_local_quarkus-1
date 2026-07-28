package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.raspagemCorrecao.application.ProtetorTermosLoreService;
import org.traducao.projeto.raspagemRevisao.domain.ContextoRevisao;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;
import org.traducao.projeto.core.presentation.ui.AnsiCores;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: o portão único por onde passa TODA proposta de correção antes de substituir
 * uma fala já publicada. Não importa de onde a proposta veio — corretor determinístico, memória de
 * correções deste arquivo, LLM ou Google —, ela responde às mesmas cinco perguntas.
 *
 * <h2>Por que é uma peça só</h2>
 * Havia três chamadas ao mesmo método privado dentro do laço de falas. Se cada origem de correção
 * tivesse ganho a sua própria checagem, as regras teriam divergido em silêncio: a proposta do Google
 * passaria por uma régua e a do LLM por outra, e a diferença só apareceria numa legenda estragada.
 * O portão é o lugar onde a régua é única por construção, não por disciplina.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Na dúvida, REJEITA. Rejeitar custa uma correção não aplicada — a fala fica pendente e visível
 *       no relatório. Aceitar errado grava alucinação na legenda, e isso não tem desfazer
 *       automático.</li>
 *   <li>NÃO decide o desfecho da fala (manter/pendente/corrigir) e NÃO conta nada: só diz se a
 *       proposta pode ser gravada. Quem contabiliza é a sessão do arquivo.</li>
 *   <li>NÃO imprime. Devolve os avisos ao operador na ordem em que devem sair, porque a mensagem de
 *       rejeição precisa aparecer ENTRE a narração do problema e a linha seguinte do laço — imprimir
 *       daqui funcionaria hoje e quebraria no dia em que o chamador coletar antes de narrar.</li>
 *   <li>Uma proposta IDÊNTICA à tradução atual é rejeitada: "correção" que não muda nada contaria
 *       como corrigida no relatório e marcaria o arquivo como modificado, forçando regravação e
 *       backup de um arquivo que não mudou.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * {@link AlucinacaoDetectadaException} vinda do validador é desfecho previsto, não erro: vira
 * rejeição silenciosa. Nunca lança.
 */
@Service
public class GuardaCorrecaoSegura {

    private final ProtetorTermosLoreService protetorLore;
    private final ValidadorTraducaoService validador;
    private final ProtecaoLegendaAssService protecaoAss;
    private final AuditorProblemasLegendaService auditor;

    /**
     * PROPÓSITO DE NEGÓCIO: reúne as quatro autoridades que podem vetar uma correção.
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede a criação do portão.
     */
    public GuardaCorrecaoSegura(
            ProtetorTermosLoreService protetorLore,
            ValidadorTraducaoService validador,
            ProtecaoLegendaAssService protecaoAss,
            AuditorProblemasLegendaService auditor) {
        this.protetorLore = protetorLore;
        this.validador = validador;
        this.protecaoAss = protecaoAss;
        this.auditor = auditor;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o veredicto do portão sobre uma proposta de correção.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code Rejeitada} carrega os avisos que o operador precisa ver, e a
     * lista VAZIA é um estado legítimo — três das cinco rejeições são silenciosas porque o desfecho
     * já aparece na linha PENDENTE do relatório. Usar {@code null} para "sem aviso" obrigaria todo
     * chamador a lembrar de checar.
     */
    public sealed interface Veredicto {

        /** A proposta pode substituir a fala. */
        record Aprovada() implements Veredicto {
        }

        /**
         * A proposta foi vetada e a fala atual permanece.
         *
         * @param avisosAoOperador mensagens a imprimir, na ordem; vazia quando a rejeição é silenciosa
         */
        record Rejeitada(List<String> avisosAoOperador) implements Veredicto {

            /** PROPÓSITO DE NEGÓCIO: normaliza a lista para imutável, blindando o chamador. */
            public Rejeitada {
                avisosAoOperador = List.copyOf(avisosAoOperador);
            }
        }
    }

    private static final Veredicto APROVADA = new Veredicto.Aprovada();
    private static final Veredicto REJEITADA_EM_SILENCIO = new Veredicto.Rejeitada(List.of());

    /**
     * PROPÓSITO DE NEGÓCIO: submete uma proposta às cinco perguntas, na ordem de custo crescente —
     * é vazia ou igual? mexeu em termo canônico? é alucinação ou resposta estruturalmente suspeita?
     * introduziu um problema que não existia? sobrou problema demais?
     *
     * <p>INVARIANTES DO DOMÍNIO: a última pergunta é a que dá sentido às outras — aceitar exige
     * MELHORA MENSURÁVEL. Uma proposta que continua suspeita só passa se resolveu mais problemas do
     * que a fala original tinha; empatar não basta, porque trocar um defeito por outro do mesmo
     * tamanho gasta uma chamada externa e um backup para não melhorar a legenda.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança; qualquer exceção prevista vira rejeição.
     *
     * @param original o texto inglês de referência
     * @param traducaoAtual a fala como está hoje na legenda
     * @param candidata a proposta de substituição
     * @param auditoriaAnterior a auditoria da fala ATUAL, base de comparação da melhora
     * @param contexto lore e termos protegidos da obra
     */
    public Veredicto avaliar(
        String original,
        String traducaoAtual,
        String candidata,
        ResultadoDeteccaoConcordancia auditoriaAnterior,
        ContextoRevisao contexto
    ) {
        if (candidata == null || candidata.isBlank() || candidata.equals(traducaoAtual)) {
            return REJEITADA_EM_SILENCIO;
        }
        List<String> termosAlterados = protetorLore.termosCanonicosAlterados(
            original, candidata, contexto.lore(), contexto.termosProtegidos());
        if (!termosAlterados.isEmpty()) {
            return new Veredicto.Rejeitada(List.of("     " + AnsiCores.YELLOW
                + "[LORE] Correção rejeitada: alteraria termo(s) canônico(s): "
                + String.join(", ", termosAlterados) + AnsiCores.RESET));
        }
        try {
            validador.validarFala(candidata);
            if (protecaoAss.respostaSuspeita(original, candidata)) {
                return REJEITADA_EM_SILENCIO;
            }
        } catch (AlucinacaoDetectadaException e) {
            return REJEITADA_EM_SILENCIO;
        }
        ResultadoDeteccaoConcordancia posterior = auditor.auditar(original, candidata);
        boolean introduziuProblemaNovo = posterior.motivos().stream()
            .anyMatch(motivo -> !auditoriaAnterior.motivos().contains(motivo));
        if (introduziuProblemaNovo) {
            return new Veredicto.Rejeitada(List.of("     " + AnsiCores.YELLOW
                + "Correção rejeitada: a proposta introduziu um problema diferente do original."
                + AnsiCores.RESET));
        }
        boolean melhorou = !posterior.suspeito()
            || posterior.motivos().size() < auditoriaAnterior.motivos().size();
        return melhorou ? APROVADA : REJEITADA_EM_SILENCIO;
    }
}
