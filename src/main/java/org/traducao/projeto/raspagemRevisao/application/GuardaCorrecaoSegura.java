package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.raspagemCorrecao.application.ProtetorTermosLoreService;
import org.traducao.projeto.raspagemRevisao.domain.ContextoRevisao;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;
import org.traducao.projeto.core.presentation.ui.AnsiCores;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: o portão único por onde passa TODA proposta de correção antes de substituir
 * uma fala já publicada. Não importa de onde a proposta veio — corretor determinístico, memória de
 * correções deste arquivo, LLM ou Google —, ela responde às mesmas seis perguntas.
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
     * lista VAZIA é um estado legítimo — três das seis rejeições são silenciosas porque o desfecho
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
     * Piso de letras para uma palavra contar na checagem de repetição introduzida. Abaixo dele estão
     * as palavras que o português repete por gramática — {@code que}, {@code de}, {@code ela} —, cuja
     * repetição não é sinal de defeito nenhum.
     */
    private static final int PISO_PALAVRA_LONGA = 4;

    /**
     * PROPÓSITO DE NEGÓCIO: submete uma proposta às seis perguntas, na ordem de custo crescente —
     * é vazia ou igual? mexeu em termo canônico? é alucinação ou resposta estruturalmente suspeita?
     * repetiu palavra que a fala não repetia? introduziu um problema que não existia? sobrou
     * problema demais?
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
        if (repetiuPalavraQueAFalaNaoRepetia(traducaoAtual, candidata)) {
            return new Veredicto.Rejeitada(List.of("     " + AnsiCores.YELLOW
                + "Correção rejeitada: a proposta repete uma palavra que a fala não repetia."
                + AnsiCores.RESET));
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

    /**
     * PROPÓSITO DE NEGÓCIO: recusa a proposta que conserta o defeito medido e estraga a leitura,
     * repetindo uma palavra que a fala não repetia.
     *
     * <h2>Por que é COMPARATIVA e nunca absoluta</h2>
     * Repetição legítima é comum em fala: medidas <b>197 das 7.022</b> falas de diálogo do 86
     * (2,81%) já repetem uma palavra longa — {@code "Pare, pare!"}, {@code "Certo, certo."},
     * {@code "nossas unidades de combate não tripuladas"} depois de {@code "unidades de tamanho de
     * batalhão"}. Uma régua absoluta reprovaria as 197, e <b>guarda que reprova texto correto ensina
     * a desligar a guarda</b>. Por isso só conta o que a PROPOSTA acrescenta: repetição que já
     * existia na fala atual passa intacta.
     *
     * <h2>Invariantes do domínio</h2>
     * <ul>
     *   <li>O piso de {@value #PISO_PALAVRA_LONGA} letras exclui o que se repete por gramática
     *       ({@code que}, {@code de}, {@code ela}), não por defeito.</li>
     *   <li>Usa {@code protecaoAss.textoVisivel} em vez de regex própria: o texto entre chaves do ASS
     *       não é fala, e escrever aqui um segundo extrator criaria a divergência que a catraca de
     *       regra duplicada entre fatias existe para impedir.</li>
     * </ul>
     *
     * <h2>O prejuízo que a originou — 2026-08-16, 86 Part 1</h2>
     * O detector acusou CERTO ({@code he} no inglês, {@code ela} na tradução) e o LLM devolveu
     * {@code "Provavelmente, ela provavelmente pensa que ele é uma boa cama."}: gênero corrigido,
     * advérbio dobrado. Rodada a mesma opção de menu outra vez, a via "consertou" apagando o sujeito
     * — {@code "Provavelmente, provavelmente pensa que ele é uma boa cama."} As duas foram gravadas
     * porque este portão media concordância e <b>não media regressão de fluência</b>. Nenhuma outra
     * opção do menu enxergava o defeito: a passada Google reauditou as mesmas 3.469 falas e não
     * tocou nelas.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo, vazio ou sem palavra longa devolve
     * {@code false} — na ausência de sinal o portão não inventa veto, e as outras cinco perguntas
     * seguem valendo.
     *
     * @param traducaoAtual a fala como está hoje na legenda
     * @param candidata a proposta de substituição
     * @return {@code true} quando a proposta acrescenta uma repetição inexistente na fala atual
     */
    private boolean repetiuPalavraQueAFalaNaoRepetia(String traducaoAtual, String candidata) {
        Map<String, Integer> antes = contarPalavrasLongas(traducaoAtual);
        Map<String, Integer> depois = contarPalavrasLongas(candidata);
        for (Map.Entry<String, Integer> palavra : depois.entrySet()) {
            if (palavra.getValue() >= 2 && palavra.getValue() > antes.getOrDefault(palavra.getKey(), 0)) {
                return true;
            }
        }
        return !colarPalavrasIguais(traducaoAtual).containsAll(colarPalavrasIguais(candidata));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: as palavras que a fala cola em si mesmas — {@code "Pare, pare!"} —,
     * para a checagem anterior enxergar também o que o piso de {@value #PISO_PALAVRA_LONGA} letras
     * deixa passar.
     *
     * <h2>Por que precisa existir, e por que só na forma ADJACENTE</h2>
     * O piso de 4 letras não é folga: é o que impede a guarda de barrar a correção mais comum desta
     * ferramenta. Uma correção de pronome legitimamente acrescenta o sujeito —
     * {@code "Ele disse que viria"} → {@code "Ele disse que ele viria"} —, e {@code ele} passaria de
     * 1 para 2. Baixar o piso reprovaria exatamente o trabalho que a 3.1 existe para fazer.
     *
     * <p>A adjacência não tem esse problema: pronome acrescentado nunca cola no anterior. E o furo é
     * real, não hipotético — na 2ª rodada o mistral-nemo devolveu
     * {@code "Provavelmente, provavelmente pensa…"}, adjacente; só não escapou porque a palavra é
     * longa. Com palavra curta escaparia.
     *
     * <p>MEDIDO no 86 (2026-08-16): <b>57 das 7.022</b> falas de diálogo (0,81%) já colam palavras
     * iguais, e TODAS legítimas — {@code "Sim, sim."}, {@code "Certo, certo."},
     * {@code "Manhã! Manhã!"}, {@code "Buá! Buá!"}. Elas passam porque a comparação é do que a
     * PROPOSTA acrescenta: o que já estava colado na fala atual continua permitido.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo ou sem texto visível devolve conjunto vazio, e
     * conjunto vazio nunca acusa — {@code containsAll(vazio)} é sempre verdadeiro.
     */
    private Set<String> colarPalavrasIguais(String texto) {
        Set<String> coladas = new HashSet<>();
        if (texto == null) {
            return coladas;
        }
        String visivel = protecaoAss.textoVisivel(texto);
        if (visivel == null || visivel.isBlank()) {
            return coladas;
        }
        String[] palavras = visivel.toLowerCase(Locale.ROOT).split("[^\\p{L}]+");
        for (int i = 1; i < palavras.length; i++) {
            if (!palavras[i].isEmpty() && palavras[i].equals(palavras[i - 1])) {
                coladas.add(palavras[i]);
            }
        }
        return coladas;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: conta quantas vezes cada palavra longa aparece no texto visível da fala.
     *
     * <p>INVARIANTES DO DOMÍNIO: compara em minúsculas para que {@code "Provavelmente"} e
     * {@code "provavelmente"} sejam a MESMA palavra — foi exatamente essa a forma do defeito medido.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo ou sem texto visível devolve mapa vazio.
     */
    private Map<String, Integer> contarPalavrasLongas(String texto) {
        Map<String, Integer> contagem = new HashMap<>();
        if (texto == null) {
            return contagem;
        }
        String visivel = protecaoAss.textoVisivel(texto);
        if (visivel == null || visivel.isBlank()) {
            return contagem;
        }
        for (String palavra : visivel.toLowerCase(Locale.ROOT).split("[^\\p{L}]+")) {
            if (palavra.length() >= PISO_PALAVRA_LONGA) {
                contagem.merge(palavra, 1, Integer::sum);
            }
        }
        return contagem;
    }
}
