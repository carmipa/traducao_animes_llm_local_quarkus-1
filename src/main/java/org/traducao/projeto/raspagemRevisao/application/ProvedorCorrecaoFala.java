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

    /**
     * O bloco de tags que ABRE a linha ASS. Só o prefixo, e de propósito: ali mora posicionamento
     * ({@code {\an8}}), cuja perda move a legenda na tela. Tag no meio é ênfase, e a rota do
     * espelho descarta ênfase por decisão de produto.
     */
    private static final java.util.regex.Pattern PREFIXO_TAGS_ASS =
        java.util.regex.Pattern.compile("^(?:\\{[^}]*\\})+");

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
        if (tentativa.revisado().isEmpty() && aFalaAindaEhOOriginal(originalEn, traducaoAtual)) {
            Optional<String> peloEspelho = traduzirPeloEspelhoDoOriginal(originalEn, contexto);
            if (peloEspelho.isPresent() && !peloEspelho.get().equals(traducaoAtual)) {
                return new Resultado.Obtida(peloEspelho.get());
            }
        }
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
     * PROPÓSITO DE NEGÓCIO: a fala continua sendo o próprio inglês? É a condição de entrada do
     * espelho, e ela compara com a legenda ORIGINAL — <i>"fazendo a comparação com a legenda
     * original teremos mais certeza do que foi corrigido ou não"</i> (Paulo, 2026-08-16).
     *
     * <h2>Por que a comparação, e não o motivo da auditoria</h2>
     * Esta guarda nasceu de um defeito MEU, pego pelo
     * {@code ProvedorCorrecaoFalaMarcadoresTest}: eu havia liberado o espelho pelo motivo
     * ({@code exigeRetraducaoCompletaPeloLlm}), que inclui "resíduo gringo". Só que resíduo aparece
     * em fala **já traduzida** — e o espelho retraduz a linha INTEIRA a partir do inglês. Ele
     * jogaria fora uma tradução boa para consertar uma palavra. O motivo diz que há defeito; só a
     * comparação com o original diz que <b>não há tradução nenhuma a preservar</b>.
     *
     * <p>INVARIANTES DO DOMÍNIO: compara o texto VISÍVEL dos dois lados, pelo mesmo extrator da
     * produção — tag e quebra não fazem parte da pergunta "isto está em português?".
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer lado nulo ou vazio devolve {@code false}, e o
     * espelho não é acionado. Na dúvida, não retraduz.
     */
    private boolean aFalaAindaEhOOriginal(String originalEn, String traducaoAtual) {
        if (originalEn == null || traducaoAtual == null) {
            return false;
        }
        String original = protecaoAss.textoVisivel(originalEn);
        String atual = protecaoAss.textoVisivel(traducaoAtual);
        return !original.isBlank() && original.equals(atual);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: traduz a fala usando a legenda ORIGINAL como espelho da estrutura,
     * quando o caminho normal falhou por marcador perdido. É o que faz a tela cumprir a própria
     * promessa — <b>o que não foi traduzido não sai daqui em inglês</b>.
     *
     * <h2>A ideia é de Paulo (2026-08-16) e substituiu a do cache</h2>
     * <i>"e as legendas originais não existem para servir de espelho? o cache falha, não é uma
     * solução muito melhor?"</i> — e é, por três motivos medidos no mesmo dia: o cache guarda
     * <b>LOTES</b> (8 entradas por episódio do Guilty Crown, blocos de 6.511 a 11.315 caracteres),
     * o {@code .ass} original guarda <b>FALAS</b>, e ele já está carregado no fluxo.
     *
     * <h2>Por que não perde marcador</h2>
     * O caminho normal mascara {@code {\i1}} como {@code [[TAG0]]} e depende de o modelo devolver o
     * marcador; quando ele não devolve, a tradução correta é jogada fora. Medido no Guilty Crown em
     * 16/08: 4 falas assim, e o Google recusava as mesmas com {@code TAG_CORROMPIDA}. Aqui o pedido
     * vai com o <b>texto visível</b> — sem marcador nenhum. Não há o que perder.
     *
     * <h2>O que se perde de propósito, por decisão de Paulo</h2>
     * A <b>ênfase inline</b>. O bloco de tags INICIAL é preservado, porque ali mora posicionamento
     * ({@code {\an8}} move a legenda na tela); a ênfase no meio da frase é descartada. O motivo é a
     * régua do projeto — <i>legível, não perfeito</i>: fala em português sem itálico lê-se; fala
     * inteira em inglês, não. Recolocar a ênfase por posição foi recusado porque a palavra
     * enfatizada muda de lugar em português, e italicizar a palavra errada é errar em silêncio.
     *
     * <h2>Invariantes do domínio</h2>
     * <ul>
     *   <li>Só é acionado quando o motivo é retradução completa — fala NÃO traduzida. Fala já
     *       traduzida com tag continua pelo caminho normal, onde a ênfase existe e não se joga
     *       fora.</li>
     *   <li>Termos da lore continuam mascarados e restaurados; alucinação continua barrando.</li>
     * </ul>
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer etapa sem resultado devolve {@code Optional.empty()}
     * e o fluxo segue para a recusa normal. Nunca lança.
     */
    private Optional<String> traduzirPeloEspelhoDoOriginal(
        String originalEn, ContextoRevisao contexto) {
        if (originalEn == null || originalEn.isBlank()) {
            return Optional.empty();
        }
        String visivel = protecaoAss.textoVisivel(originalEn);
        if (visivel == null || visivel.isBlank()) {
            return Optional.empty();
        }
        ProtetorTermosLoreService.TextoProtegido protegido = protetorLore.mascarar(
            visivel, contexto.lore(), contexto.termosProtegidos());
        Optional<String> resposta = llmPort.corrigirTraducao(
            protegido.textoMascarado(), protegido.textoMascarado(),
            PoliticaRetraducao.NAO_TRADUZIDA + " — traduzir integralmente para português do Brasil");
        if (resposta.isEmpty()) {
            return Optional.empty();
        }
        String restaurado = protetorLore.restaurar(resposta.get(), protegido);
        if (restaurado == null || restaurado.isBlank()) {
            return Optional.empty();
        }
        try {
            validador.validarFala(restaurado);
        } catch (AlucinacaoDetectadaException e) {
            return Optional.empty();
        }
        return Optional.of(prefixoDeTags(originalEn) + restaurado.strip());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o bloco de tags que abre a linha — o que carrega posicionamento e
     * estilo, e cuja perda MOVE a legenda na tela.
     * <p>INVARIANTES DO DOMÍNIO: só o prefixo. Tag no meio da frase é ênfase, e ênfase é o que esta
     * rota descarta de propósito.
     * <p>COMPORTAMENTO EM CASO DE FALHA: linha sem prefixo devolve string vazia.
     */
    private static String prefixoDeTags(String texto) {
        java.util.regex.Matcher inicio = PREFIXO_TAGS_ASS.matcher(texto);
        return inicio.find() ? inicio.group() : "";
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
