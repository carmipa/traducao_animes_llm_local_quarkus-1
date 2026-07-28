package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.raspagemRevisao.domain.ContextoRevisao;
import org.traducao.projeto.raspagemRevisao.domain.DecisaoFala;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;
import org.traducao.projeto.core.presentation.ui.AnsiCores;

import java.util.List;
import java.util.Optional;

/**
 * PROPÓSITO DE NEGÓCIO: responde "esta fala eu já resolvi neste arquivo?" antes que ela custe uma
 * chamada ao LLM ou ao Google. Um episódio repete falas — bordões, nomes de ataque, o mesmo grito
 * em três cenas — e sem esta memória cada repetição pagaria a mesma tradução de novo, em dinheiro e
 * em segundos de espera do operador.
 *
 * <h2>Por que a memória é do ARQUIVO e não global</h2>
 * A chave é o texto ORIGINAL mascarado, sem as tags do ASS. Duas falas com a mesma frase inglesa
 * recebem a mesma correção — dentro do mesmo episódio, onde compartilham contexto, personagens e
 * tratamento. Entre episódios isso deixa de valer: a mesma frase pode ter outro sujeito e outro
 * gênero. A memória morre com o arquivo, e isso é a regra, não uma limitação a ser "otimizada".
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Reaproveitar NÃO pula o portão de segurança. A correção guardada foi aprovada para OUTRA
 *       ocorrência, com outras tags; ao ser remontada sobre esta fala pode virar outra coisa. Ela
 *       passa pelo mesmo {@link GuardaCorrecaoSegura} que uma resposta recém-chegada do LLM.</li>
 *   <li>Registrar "sem alteração" é decisão FINAL para o texto no arquivo: a próxima ocorrência nem
 *       consulta o provedor. É por isso que só se registra depois de uma tentativa real ter
 *       falhado — registrar cedo demais silencia falas que teriam correção.</li>
 *   <li>NÃO imprime e NÃO conta: devolve a decisão e os avisos, e quem narra e contabiliza é o
 *       laço. Ver {@link DecisaoFala}.</li>
 *   <li>{@code Optional.empty()} significa "não sei nada sobre esta fala" — e só isso. Não é
 *       "mantenha": é a autorização para o laço seguir e pagar a chamada externa.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Marcadores de tag incompatíveis ({@link AlucinacaoDetectadaException} na remontagem) são desfecho
 * previsto, não erro: a memória é descartada PARA ESTA FALA, ela fica pendente, e a entrada continua
 * valendo para as demais ocorrências — o defeito está no par tags/texto desta linha, não na correção
 * guardada. Nunca lança.
 */
@Service
public class MemoriaCorrecaoArquivo {

    private final MascaradorTags mascaradorTags;
    private final GuardaCorrecaoSegura guardaCorrecao;

    /**
     * PROPÓSITO DE NEGÓCIO: reúne o mascarador de tags e o portão único de correção.
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede a criação da memória.
     */
    public MemoriaCorrecaoArquivo(
            MascaradorTags mascaradorTags,
            GuardaCorrecaoSegura guardaCorrecao) {
        this.mascaradorTags = mascaradorTags;
        this.guardaCorrecao = guardaCorrecao;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: consulta a memória do arquivo e, se houver resposta, devolve a decisão
     * já pronta para a fala — sem tocar em nenhum provedor externo.
     *
     * <p>INVARIANTES DO DOMÍNIO: a ordem das duas consultas importa. "Já sei que não melhora" é
     * verificado ANTES de "tenho uma correção", porque a primeira é a memória de uma tentativa que
     * JÁ falhou e a segunda é uma proposta a testar; inverter faria uma fala descartada voltar a ser
     * remontada a cada ocorrência.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança; chave nula responde {@code Optional.empty()}
     * naturalmente, porque a sessão trata nulo como "não conheço".
     *
     * @param sessao a memória deste arquivo — lida e, no veto, escrita
     * @param evento a fala, usada só para a mensagem ao operador
     * @param textoMascOriginal a chave: o inglês mascarado; pode ser {@code null}
     * @param originalEn o inglês de referência
     * @param traducaoAtual a fala como está hoje
     * @param auditoria a auditoria da fala atual, base de comparação do portão
     * @param contexto lore e termos protegidos da obra
     * @return a decisão, ou vazio quando a memória nada tem sobre esta fala
     */
    public Optional<DecisaoFala> consultar(
        SessaoRevisaoArquivo sessao,
        EventoLegenda evento,
        String textoMascOriginal,
        String originalEn,
        String traducaoAtual,
        ResultadoDeteccaoConcordancia auditoria,
        ContextoRevisao contexto
    ) {
        if (sessao.jaSabidoSemAlteracao(textoMascOriginal)) {
            return Optional.of(new DecisaoFala.Pendente(List.of()));
        }
        String respostaMascCorrigida = sessao.correcaoConhecida(textoMascOriginal);
        if (respostaMascCorrigida == null) {
            return Optional.empty();
        }

        MascaradorTags.Mascarado mascTraducaoAtual = mascaradorTags.mascarar(traducaoAtual);
        String novaTraducaoCache;
        try {
            novaTraducaoCache = mascaradorTags.desmascarar(respostaMascCorrigida, mascTraducaoAtual.tags());
        } catch (AlucinacaoDetectadaException e) {
            return Optional.of(new DecisaoFala.Pendente(List.of("  " + AnsiCores.YELLOW
                + "Cache local ignorado na linha " + evento.indice()
                + ": marcadores de tags incompatíveis com a tradução atual."
                + AnsiCores.RESET)));
        }

        GuardaCorrecaoSegura.Veredicto veredicto = guardaCorrecao.avaliar(
            originalEn, traducaoAtual, novaTraducaoCache, auditoria, contexto);
        if (veredicto instanceof GuardaCorrecaoSegura.Veredicto.Rejeitada rejeitada) {
            sessao.registrarSemAlteracao(textoMascOriginal);
            return Optional.of(new DecisaoFala.Pendente(rejeitada.avisosAoOperador()));
        }

        return Optional.of(new DecisaoFala.Corrigir(novaTraducaoCache, List.of(
            "  -> Linha " + evento.indice() + " [" + evento.estilo()
                + "] (Reutilizando correção do cache local):",
            "     EN: " + AnsiCores.YELLOW + originalEn + AnsiCores.RESET,
            "     PT: " + AnsiCores.YELLOW + traducaoAtual + AnsiCores.RESET,
            "     PT corrigido: " + AnsiCores.GREEN + novaTraducaoCache + AnsiCores.RESET)));
    }
}
