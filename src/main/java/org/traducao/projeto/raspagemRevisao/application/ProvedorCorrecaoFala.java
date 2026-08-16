package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.core.presentation.ui.AnsiCores;
import org.traducao.projeto.llm.domain.LlmPort;
import org.traducao.projeto.qualidadeTraducao.application.IsoladorQuebraDialogo;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;
import org.traducao.projeto.raspagemCorrecao.application.ProtetorTermosLoreService;
import org.traducao.projeto.raspagemRevisao.domain.ContextoRevisao;
import org.traducao.projeto.raspagemRevisao.domain.ModoRevisaoLegendas;
import org.traducao.projeto.raspagemRevisao.domain.PoliticaRetraducao;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoRecuperacaoExterna;
import org.traducao.projeto.raspagemRevisao.domain.ports.RecuperacaoExternaRevisaoPort;

import java.util.List;
import java.util.Optional;

/**
 * PROPÓSITO DE NEGÓCIO: pede a um provedor externo uma tradução melhor para uma fala com problema —
 * ao LLM local, que conhece a lore, ou ao tradutor de máquina, que é rápido e não conhece. Devolve
 * a candidata ou a RAZÃO de não haver candidata.
 *
 * <h2>Os dois provedores não são intercambiáveis</h2>
 * O LLM recebe qualquer motivo de auditoria e devolve concordância, gênero e tratamento corrigidos.
 * O tradutor de máquina só recebe falhas OBJETIVAS ({@link PoliticaRetraducao}) — mandar-lhe uma
 * questão de concordância devolve nome próprio traduzido, porque ele não tem a lore.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Tudo que sai daqui já passou por desmascaramento de tags e restauração de termos da lore. Se
 *       qualquer marcador se perdeu no caminho, o resultado é RECUSA com código próprio, nunca uma
 *       fala com formatação quebrada.</li>
 *   <li>A pausa entre chamadas ao tradutor externo acontece AQUI, logo após a chamada, como sempre
 *       aconteceu. É o que separa esta operação de um bloqueio por IP, e deslocá-la para o adaptador
 *       dobraria a espera de quem já pausa.</li>
 *   <li>NÃO imprime, NÃO conta e NÃO registra auditoria: devolve a mensagem pronta e os campos do
 *       detalhe, e quem narra é o caso de uso. As mensagens deste bloco saem todas no mesmo ponto do
 *       fluxo, então coletá-las não inverte ordem nenhuma.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nunca lança: indisponibilidade, resposta vazia, marcador perdido e estrutura ASS suspeita viram
 * {@link Resultado.Recusada} com causa legível — a fala fica pendente, exatamente como estava.
 */
@Service
public class ProvedorCorrecaoFala {

    /** Espera entre chamadas ao tradutor externo, para não disparar bloqueio por IP. */
    private static final long PAUSA_GOOGLE_MS = 400;

    private final LlmPort llmPort;
    private final RecuperacaoExternaRevisaoPort recuperacaoExterna;
    private final ProtetorTermosLoreService protetorLore;
    private final MascaradorTags mascaradorTags;
    private final IsoladorQuebraDialogo isoladorQuebra;
    private final ValidadorTraducaoService validador;
    private final ProtecaoLegendaAssService protecaoAss;

    /**
     * PROPÓSITO DE NEGÓCIO: reúne os dois provedores e as proteções que validam o que eles devolvem.
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede a criação do serviço.
     */
    public ProvedorCorrecaoFala(
            LlmPort llmPort,
            RecuperacaoExternaRevisaoPort recuperacaoExterna,
            ProtetorTermosLoreService protetorLore,
            MascaradorTags mascaradorTags,
            IsoladorQuebraDialogo isoladorQuebra,
            ValidadorTraducaoService validador,
            ProtecaoLegendaAssService protecaoAss) {
        this.llmPort = llmPort;
        this.recuperacaoExterna = recuperacaoExterna;
        this.protetorLore = protetorLore;
        this.mascaradorTags = mascaradorTags;
        this.isoladorQuebra = isoladorQuebra;
        this.validador = validador;
        this.protecaoAss = protecaoAss;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o desfecho do pedido a um provedor.
     *
     * <p>É selado porque os dois casos carregam dados incompatíveis: a candidata tem texto e nada
     * mais; a recusa tem mensagem, causa e — às vezes — a proposta rejeitada para a auditoria.
     * Um único objeto com campos nulos convidaria a ler o texto de uma recusa.
     */
    public sealed interface Resultado {

        /** Há uma tradução candidata; ela ainda passará pelo portão de segurança. */
        record Obtida(String texto) implements Resultado {
        }

        /**
         * Não há candidata, e aqui está o porquê.
         *
         * @param mensagem o que dizer ao operador, já formatado
         * @param registrarSemAlteracao se vale lembrar que este texto não rende, para não repetir
         * @param codigo código do detalhe de auditoria. <b>Nunca {@code null}</b> — recusa sem
         *        código não gera {@code DetalheRevisao}, e a fala continua contada em
         *        "Pendentes: N" sem aparecer em lugar nenhum. Medido em 2026-08-16 no 86: a
         *        passada Google reportou <b>2 pendentes e ZERO detalhes</b>, porque os dois
         *        caminhos de recusa deste modo passavam {@code null}. "Nada a fazer" e "não
         *        consegui" produziam o mesmo sinal
         * @param detalhe explicação técnica para o relatório
         * @param proposta o que o provedor devolveu e foi rejeitado, quando houve
         */
        record Recusada(
            String mensagem,
            boolean registrarSemAlteracao,
            String codigo,
            String detalhe,
            String proposta
        ) implements Resultado {
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: obtém uma candidata do provedor correspondente ao modo.
     *
     * <p>INVARIANTES DO DOMÍNIO: no modo Google, a fala só é enviada se o motivo for objetivo; no
     * modo LLM, qualquer motivo serve. Uma resposta idêntica à tradução atual é RECUSA, não sucesso
     * — aplicá-la marcaria o arquivo como modificado sem nada ter mudado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@link Resultado.Recusada}; não lança.
     */
    public Resultado obter(
        ModoRevisaoLegendas modo,
        String originalEn,
        String traducaoAtual,
        List<String> motivos,
        ContextoRevisao contexto
    ) {
        if (modo == ModoRevisaoLegendas.LLM_CONCORDANCIA) {
            return obterDoLlm(originalEn, traducaoAtual, motivos, contexto);
        }
        return obterDoTradutorExterno(originalEn, traducaoAtual, motivos, contexto);
    }

    private Resultado obterDoLlm(
        String originalEn, String traducaoAtual, List<String> motivos, ContextoRevisao contexto) {
        TentativaRevisaoLegenda tentativa = tentarRevisarConcordancia(
            originalEn, traducaoAtual, motivos, contexto);
        if (tentativa.revisado().isEmpty()) {
            return new Resultado.Recusada(
                "     " + AnsiCores.RED + "Revisão não aplicada: " + tentativa.detalhe() + AnsiCores.RESET,
                false, tentativa.codigo(), tentativa.detalhe(), tentativa.proposta());
        }
        String novaTraducao = tentativa.revisado().get();
        if (novaTraducao.equals(traducaoAtual)) {
            return new Resultado.Recusada(
                "     " + AnsiCores.DIM + "LLM manteve o texto original." + AnsiCores.RESET,
                true, "LLM_SEM_ALTERACAO",
                "O modelo respondeu, mas manteve a tradução atual.", novaTraducao);
        }
        return new Resultado.Obtida(novaTraducao);
    }

    private Resultado obterDoTradutorExterno(
        String originalEn, String traducaoAtual, List<String> motivos, ContextoRevisao contexto) {
        if (!PoliticaRetraducao.exigeRetraducaoPeloGoogle(motivos)) {
            return new Resultado.Recusada(
                "     " + AnsiCores.DIM + "Google não acionado: problema reservado à revisão LLM."
                    + AnsiCores.RESET,
                true, "GOOGLE_NAO_ACIONADO",
                "O motivo não é falha objetiva de tradução, e o Google não conhece a lore. "
                    + "A fala é trabalho da passada LLM desta mesma tela.", null);
        }
        ProtetorTermosLoreService.TextoProtegido originalProtegido = protetorLore.mascarar(
            originalEn, contexto.lore(), contexto.termosProtegidos());
        ResultadoRecuperacaoExterna resultado =
            recuperacaoExterna.traduzir(originalProtegido.textoMascarado());
        pausar();

        String restaurada = resultado.sucesso()
            ? protetorLore.restaurar(resultado.texto(), originalProtegido)
            : null;
        if (!resultado.sucesso() || restaurada == null || restaurada.equals(traducaoAtual)) {
            return new Resultado.Recusada(
                "     " + AnsiCores.DIM + "Google sem alteração aplicável ("
                    + resultado.status() + "); mantido." + AnsiCores.RESET,
                true, "GOOGLE_SEM_ALTERACAO",
                "Google respondeu " + resultado.status() + " e não produziu texto aplicável "
                    + "(falha, marcador de lore perdido ou resposta igual à fala atual).",
                restaurada);
        }
        return new Resultado.Obtida(restaurada);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: solicita ao LLM uma revisão pontual sem permitir que nomes e termos
     * oficiais definidos pela lore sejam traduzidos.
     *
     * <p>INVARIANTES DO DOMÍNIO: tags ASS e termos canônicos são mascarados ANTES da chamada e
     * precisam ser restaurados integralmente antes da validação. Marcador perdido é recusa, não
     * "quase certo".
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: resposta vazia, marcador perdido ou proposta
     * estruturalmente inválida devolvem diagnóstico explícito, sem confundir rejeição de conteúdo
     * com indisponibilidade do servidor.
     */
    private TentativaRevisaoLegenda tentarRevisarConcordancia(
        String original, String traduzido, List<String> motivos, ContextoRevisao contexto) {
        String textoOriginal = original != null ? original : "";
        // O inglês é apenas referência semântica. Suas tags não pertencem ao contrato estrutural
        // da legenda PT e, se virassem [[TAGn]], o modelo poderia copiá-las para uma tradução que
        // legitimamente não possui nenhuma tag (caso real: Zeta S01E02, evento 628).
        String originalVisivel = protecaoAss.textoVisivel(textoOriginal);
        ProtetorTermosLoreService.TextoProtegido originalProtegido = protetorLore.mascarar(
            originalVisivel, contexto.lore(), contexto.termosProtegidos());
        IsoladorQuebraDialogo.FalaIsolada traducaoIsolada = isoladorQuebra.isolar(traduzido);
        ProtetorTermosLoreService.TextoProtegido traducaoProtegida = protetorLore.mascarar(
            traducaoIsolada.textoSemQuebra(), contexto.lore(), contexto.termosProtegidos());
        MascaradorTags.Mascarado mascTraduzido = mascaradorTags.mascarar(traducaoProtegida.textoMascarado());

        Optional<String> resposta = PoliticaRetraducao.exigeRetraducaoCompletaPeloLlm(motivos)
            ? llmPort.corrigirTraducao(originalProtegido.textoMascarado(), mascTraduzido.texto(),
                String.join(", ", motivos))
            : llmPort.revisarConcordancia(
                originalProtegido.textoMascarado(), mascTraduzido.texto(), motivos);

        if (resposta.isEmpty()) {
            return TentativaRevisaoLegenda.pendente(
                "LLM_SEM_CONTEUDO_UTILIZAVEL",
                "O LLM não devolveu uma linha final utilizável após as tentativas. A resposta pode "
                    + "estar vazia ou ter estrutura de marcadores incompatível; consulte o log técnico.",
                null);
        }

        String proposta = resposta.get();
        try {
            String desmascarado = mascaradorTags.desmascarar(proposta, mascTraduzido.tags());
            String restaurado = protetorLore.restaurar(desmascarado, traducaoProtegida);
            if (restaurado == null) {
                return TentativaRevisaoLegenda.pendente(
                    "LLM_MARCADOR_LORE_INCOMPATIVEL",
                    "A proposta perdeu ou alterou marcador protegido pela lore.", proposta);
            }
            restaurado = isoladorQuebra.reaplicar(restaurado, traducaoIsolada.quebras());
            if (!mascaradorTags.preservaEstruturaOriginal(traduzido, restaurado)) {
                return TentativaRevisaoLegenda.pendente(
                    "LLM_ESTRUTURA_ASS_SUSPEITA",
                    "A proposta não permitiu restaurar integralmente as tags e quebras da legenda PT.",
                    proposta);
            }
            validador.validarFala(restaurado);
            if (protecaoAss.respostaSuspeita(original, restaurado)) {
                return TentativaRevisaoLegenda.pendente(
                    "LLM_ESTRUTURA_ASS_SUSPEITA",
                    "A proposta alterou a estrutura protegida da legenda ASS.", proposta);
            }
            return TentativaRevisaoLegenda.sucesso(restaurado, proposta);
        } catch (AlucinacaoDetectadaException e) {
            return TentativaRevisaoLegenda.pendente(
                "LLM_VALIDACAO_REJEITADA", RelatorioRevisaoService.mensagemFalha(e), proposta);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: espaça as chamadas ao tradutor externo.
     * <p>INVARIANTES DO DOMÍNIO: acontece DEPOIS da chamada, como sempre aconteceu. É o que separa
     * a operação de um bloqueio por IP.
     * <p>COMPORTAMENTO EM CASO DE FALHA: interrupção restaura a flag e devolve o controle — o laço
     * a consulta e faz o dreno do arquivo.
     */
    private void pausar() {
        try {
            Thread.sleep(PAUSA_GOOGLE_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: transporta o resultado técnico de uma chamada ao LLM para que console e
     * relatório expliquem por que a fala foi mantida.
     * <p>INVARIANTES DO DOMÍNIO: sucesso sempre contém texto revisado; pendência sempre contém
     * código e diagnóstico, podendo conservar a proposta rejeitada.
     * <p>COMPORTAMENTO EM CASO DE FALHA: as fábricas normalizam ausências.
     */
    private record TentativaRevisaoLegenda(
        Optional<String> revisado, String codigo, String detalhe, String proposta) {

        static TentativaRevisaoLegenda sucesso(String revisado, String proposta) {
            return new TentativaRevisaoLegenda(Optional.of(revisado), "LLM_APLICADO", null, proposta);
        }

        static TentativaRevisaoLegenda pendente(String codigo, String detalhe, String proposta) {
            return new TentativaRevisaoLegenda(Optional.empty(), codigo, detalhe, proposta);
        }
    }
}
