package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.raspagemRevisao.domain.ContextoRevisao;
import org.traducao.projeto.raspagemRevisao.domain.DecisaoFala;
import org.traducao.projeto.raspagemRevisao.domain.DetalheRevisao;
import org.traducao.projeto.raspagemRevisao.domain.ModoRevisaoLegendas;
import org.traducao.projeto.raspagemRevisao.domain.PoliticaRetraducao;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.core.presentation.ui.AnsiCores;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PROPÓSITO DE NEGÓCIO: dada uma fala já reconhecida como defeituosa, decide o que fazer com ela
 * tentando as fontes de correção EM ORDEM DE CUSTO — regra determinística, memória deste arquivo,
 * e só então uma chamada externa ao LLM ou ao Google.
 *
 * <h2>A ordem é a economia, e não pode ser trocada por "organização"</h2>
 * A regra determinística é local e instantânea; a memória do arquivo é local e instantânea; a
 * chamada externa custa segundos, cota e — no caso do Google — risco de bloqueio por volume. Num
 * episódio com 400 falas e 60 defeitos, subir uma fonte de lugar na fila muda o tempo do lote de
 * minutos para dezenas de minutos. Cada fonte só é consultada se a anterior não resolveu.
 *
 * <h2>Por que a evidência viaja AO LADO da decisão, e não dentro dela</h2>
 * {@link DecisaoFala} diz o que acontece com a LINHA; {@link DetalheRevisao} é o que vai para o
 * relatório e para o dataset que treina os detectores. São públicos diferentes: manter uma fala
 * correta é uma decisão sem evidência nenhuma a registrar, e enfiar um campo vazio em toda decisão
 * faria parecer que falta alguma coisa quando não falta. Aqui elas andam juntas porque este é o
 * único ponto do fluxo onde as duas nascem do mesmo fato.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>TODA proposta — inclusive a da regra determinística e a reaproveitada da memória — passa
 *       pelo mesmo {@link GuardaCorrecaoSegura}. Não há atalho para fonte "confiável".</li>
 *   <li>Os avisos ACUMULAM na ordem em que aconteceram. Uma proposta determinística vetada pela
 *       lore imprime o veto e a cadeia SEGUE para as fontes seguintes: o operador precisa ver que
 *       houve uma tentativa barrada, senão a linha some do console como se nada tivesse ocorrido.</li>
 *   <li>NÃO imprime, NÃO conta e NÃO grava arquivo. Escreve na memória da sessão — que é justamente
 *       o que evita a próxima chamada externa — e devolve o resto ao laço.</li>
 *   <li>Uma correção reaproveitada da memória NÃO gera {@link DetalheRevisao}. É comportamento
 *       herdado, não descuido: a evidência da PRIMEIRA ocorrência já está no relatório. Se um dia o
 *       dataset precisar contar reaproveitamentos, o lugar é aqui.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Recusa do provedor externo (cota, HTTP 429, tags corrompidas, resposta vazia) é desfecho previsto:
 * vira {@code Pendente} com a mensagem do provedor e, quando ele mandar, a marca de "não insistir"
 * na memória do arquivo. A fala original é preservada. Nunca lança.
 */
@Service
public class CadeiaCorrecaoFala {

    private final CorretorDeterministicoConcordanciaService corretorDeterministico;
    private final MemoriaCorrecaoArquivo memoriaCorrecao;
    private final ProvedorCorrecaoFala provedorCorrecao;
    private final GuardaCorrecaoSegura guardaCorrecao;
    private final MascaradorTags mascaradorTags;

    /**
     * PROPÓSITO DE NEGÓCIO: reúne as três fontes de correção, o portão que as julga e o mascarador
     * que produz a chave da memória.
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede a criação da cadeia.
     */
    public CadeiaCorrecaoFala(
            CorretorDeterministicoConcordanciaService corretorDeterministico,
            MemoriaCorrecaoArquivo memoriaCorrecao,
            ProvedorCorrecaoFala provedorCorrecao,
            GuardaCorrecaoSegura guardaCorrecao,
            MascaradorTags mascaradorTags) {
        this.corretorDeterministico = corretorDeterministico;
        this.memoriaCorrecao = memoriaCorrecao;
        this.provedorCorrecao = provedorCorrecao;
        this.guardaCorrecao = guardaCorrecao;
        this.mascaradorTags = mascaradorTags;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a fala defeituosa e a prova do defeito, num parâmetro só.
     *
     * <p>INVARIANTES DO DOMÍNIO: existe para impedir a troca silenciosa de argumentos. Sem ele a
     * cadeia receberia três Strings seguidas — nome do arquivo, inglês e português — e inverter duas
     * delas compila, roda e produz um relatório errado sem nenhum sinal.
     *
     * @param evento a linha da legenda
     * @param originalEn o inglês de referência, ou {@code null} no modo sem original
     * @param traducaoAtual a fala como está hoje
     * @param temOriginalEn se há original utilizável
     * @param auditoria os motivos do defeito, base de comparação de qualquer proposta
     */
    public record FalaSuspeita(
        EventoLegenda evento,
        String originalEn,
        String traducaoAtual,
        boolean temOriginalEn,
        ResultadoDeteccaoConcordancia auditoria
    ) {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o que a cadeia apurou — o destino da linha e a prova para o relatório.
     *
     * @param decisao o que fazer com a fala, com todos os avisos acumulados na ordem
     * @param evidencias registros para o relatório e o dataset; vazia quando não há o que provar
     */
    public record Tentativa(DecisaoFala decisao, List<DetalheRevisao> evidencias) {

        /** PROPÓSITO DE NEGÓCIO: blinda a lista contra mutação depois da decisão tomada. */
        public Tentativa {
            evidencias = List.copyOf(evidencias);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: percorre as fontes de correção e devolve o desfecho da fala.
     *
     * <p>INVARIANTES DO DOMÍNIO: a chave da memória é o INGLÊS mascarado, e é {@code null} quando
     * não há original — sem chave, a memória não guarda nem consulta nada, e é o correto: no modo
     * sem original duas falas portuguesas parecidas não têm por que compartilhar correção.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança; toda recusa vira {@code Pendente}.
     *
     * @param sessao a memória deste arquivo, lida e escrita
     * @param fala a fala defeituosa e sua auditoria
     * @param nomeArquivo o nome do .ass, usado só nos registros de evidência
     * @param modo GOOGLE ou LLM_CONCORDANCIA — define o provedor e o rótulo da evidência
     * @param contexto lore e termos protegidos da obra
     */
    public Tentativa decidir(
        SessaoRevisaoArquivo sessao,
        FalaSuspeita fala,
        String nomeArquivo,
        ModoRevisaoLegendas modo,
        ContextoRevisao contexto
    ) {
        EventoLegenda evento = fala.evento();
        String originalEn = fala.originalEn();
        String traducaoAtual = fala.traducaoAtual();
        ResultadoDeteccaoConcordancia auditoria = fala.auditoria();
        List<String> avisos = new ArrayList<>();

        String textoMascOriginal = fala.temOriginalEn()
            ? mascaradorTags.mascarar(originalEn).texto()
            : null;

        // 1ª fonte: regra local, sem custo. Resolve contradições objetivas de concordância.
        Optional<String> correcaoDeterministica = corretorDeterministico.corrigir(
            originalEn, traducaoAtual);
        if (correcaoDeterministica.isPresent()) {
            String corrigida = correcaoDeterministica.get();
            GuardaCorrecaoSegura.Veredicto veredicto = guardaCorrecao.avaliar(
                originalEn, traducaoAtual, corrigida, auditoria, contexto);
            if (veredicto instanceof GuardaCorrecaoSegura.Veredicto.Rejeitada rejeitada) {
                avisos.addAll(rejeitada.avisosAoOperador());
            } else {
                sessao.registrarCorrecao(textoMascOriginal, mascaradorTags.mascarar(corrigida).texto());
                avisos.add("     PT corrigido por regra segura: "
                    + AnsiCores.GREEN + corrigida + AnsiCores.RESET);
                return new Tentativa(
                    new DecisaoFala.Corrigir(corrigida, avisos),
                    List.of(new DetalheRevisao(nomeArquivo, evento.indice(), evento.estilo(),
                        "CORRIGIDA_REGRA_SEGURA", auditoria.motivos(),
                        "Contradição objetiva corrigida localmente, sem chamar LLM ou Google.",
                        originalEn, traducaoAtual, corrigida)));
            }
        }

        // 2ª fonte: esta mesma fala já foi resolvida antes neste arquivo?
        Optional<DecisaoFala> daMemoria = memoriaCorrecao.consultar(
            sessao, evento, textoMascOriginal, originalEn, traducaoAtual, auditoria, contexto);
        if (daMemoria.isPresent()) {
            return new Tentativa(precedidaDe(avisos, daMemoria.get()), List.of());
        }

        List<DetalheRevisao> evidencias = new ArrayList<>();

        // O ESCOPO DA TELA (Paulo, 2026-08-16): a 3.1 resolve FALTA DE TRADUÇÃO. Concordância tem
        // tela própria, a 3.3.
        //
        // O corte é AQUI, e não na triagem, e isso foi um erro meu que 5 testes ponta-a-ponta
        // pegaram: cortar na entrada tirava junto a correção DETERMINÍSTICA, que é local, grátis e
        // já provada ("Minha mãe" ← "My dad" sai corrigido no .ass). Perder capacidade gratuita
        // para ganhar pureza de escopo é troca ruim. O que a regra local conserta de graça, a tela
        // continua consertando; o que ela gasta REDE para consertar, só dentro do próprio escopo.
        if (!PoliticaRetraducao.ehFalhaDeTraducao(auditoria.motivos())) {
            avisos.add("     " + AnsiCores.DIM
                + "Fora do escopo desta tela: defeito de concordância/estilo pertence à 3.3."
                + AnsiCores.RESET);
            return new Tentativa(
                new DecisaoFala.Pendente(avisos),
                List.of(new DetalheRevisao(nomeArquivo, evento.indice(), evento.estilo(),
                    "FORA_DO_ESCOPO_DA_TELA", auditoria.motivos(),
                    "A 3.1 resolve falta de tradução. Este defeito é de concordância/estilo e "
                        + "pertence à Revisão de Concordância (3.3). A fala foi preservada.",
                    originalEn, traducaoAtual, null)));
        }

        // 3ª fonte: a única que custa. Só chega aqui o que as duas anteriores não resolveram.
        ProvedorCorrecaoFala.Resultado candidata = provedorCorrecao.obter(
            modo, originalEn, traducaoAtual, auditoria.motivos(), contexto);

        // CASCATA (Paulo, 2026-08-16): o LLM é a 1ª etapa porque conhece a lore; o Google é a 2ª,
        // e SÓ quando a 1ª não resolveu. Antes eram dois botões, e "não sai daqui sem tradução"
        // dependia de o operador lembrar a ordem — o que a regra da boa-fé chama de interface que
        // permite errar. O Google se protege sozinho: motivo que não seja falha objetiva volta
        // como GOOGLE_NAO_ACIONADO, porque tradutor sem lore devolve nome próprio traduzido.
        // Qual provedor REALMENTE produziu o texto. Sem isto, uma fala resolvida pelo Google depois
        // de o LLM recusar sairia rotulada como CORRIGIDA_LLM no relatório e no dataset — o rótulo
        // existe justamente para distinguir o que custou rede de quem, e mentir nele contamina toda
        // comparação futura entre provedores.
        ModoRevisaoLegendas modoEfetivo = modo;
        boolean primeiraPediuMemoria = false;
        if (candidata instanceof ProvedorCorrecaoFala.Resultado.Recusada primeira
            && modo == ModoRevisaoLegendas.LLM_CONCORDANCIA) {
            avisos.add(primeira.mensagem());
            if (primeira.codigo() != null) {
                evidencias.add(new DetalheRevisao(nomeArquivo, evento.indice(), evento.estilo(),
                    primeira.codigo(), auditoria.motivos(), primeira.detalhe(),
                    originalEn, traducaoAtual, primeira.proposta()));
            }
            // A memória NÃO é gravada aqui: se o Google resolver, marcar "não rende" agora
            // impediria a próxima ocorrência de aproveitar a correção que existe.
            primeiraPediuMemoria = primeira.registrarSemAlteracao();
            modoEfetivo = ModoRevisaoLegendas.GOOGLE;
            candidata = provedorCorrecao.obter(
                modoEfetivo, originalEn, traducaoAtual, auditoria.motivos(), contexto);
        }

        if (candidata instanceof ProvedorCorrecaoFala.Resultado.Recusada recusada) {
            avisos.add(recusada.mensagem());
            if (recusada.registrarSemAlteracao() || primeiraPediuMemoria) {
                sessao.registrarSemAlteracao(textoMascOriginal);
            }
            if (recusada.codigo() != null) {
                evidencias.add(new DetalheRevisao(nomeArquivo, evento.indice(), evento.estilo(),
                    recusada.codigo(), auditoria.motivos(), recusada.detalhe(),
                    originalEn, traducaoAtual, recusada.proposta()));
            }
            return new Tentativa(new DecisaoFala.Pendente(avisos), evidencias);
        }

        ProvedorCorrecaoFala.Resultado.Obtida obtida =
            (ProvedorCorrecaoFala.Resultado.Obtida) candidata;
        String novaTraducao = obtida.texto();
        if (obtida.dicionarioAjustou()) {
            avisos.add("     " + AnsiCores.DIM + "dicionário ajustou a proposta." + AnsiCores.RESET);
        }

        GuardaCorrecaoSegura.Veredicto veredicto = guardaCorrecao.avaliar(
            originalEn, traducaoAtual, novaTraducao, auditoria, contexto);
        if (veredicto instanceof GuardaCorrecaoSegura.Veredicto.Rejeitada rejeitada) {
            avisos.addAll(rejeitada.avisosAoOperador());
            String provedor = modoEfetivo == ModoRevisaoLegendas.LLM_CONCORDANCIA ? "LLM" : "Google";
            String motivo = "Correção do " + provedor + " descartada pelo portão: "
                + rejeitada.motivo().descricao() + ".";
            avisos.add("     " + AnsiCores.YELLOW + motivo + AnsiCores.RESET);
            sessao.registrarSemAlteracao(textoMascOriginal);
            evidencias.add(new DetalheRevisao(nomeArquivo, evento.indice(), evento.estilo(),
                rejeitada.motivo().codigo(),
                auditoria.motivos(), motivo, originalEn, traducaoAtual, novaTraducao));
            return new Tentativa(new DecisaoFala.Pendente(avisos), evidencias);
        }

        sessao.registrarCorrecao(textoMascOriginal, mascaradorTags.mascarar(novaTraducao).texto());
        avisos.add("     PT corrigido: " + AnsiCores.GREEN + novaTraducao + AnsiCores.RESET);
        evidencias.add(new DetalheRevisao(nomeArquivo, evento.indice(), evento.estilo(),
            modoEfetivo == ModoRevisaoLegendas.LLM_CONCORDANCIA ? "CORRIGIDA_LLM" : "CORRIGIDA_GOOGLE",
            auditoria.motivos(), "Correção validada e persistida.",
            originalEn, traducaoAtual, novaTraducao));
        return new Tentativa(new DecisaoFala.Corrigir(novaTraducao, avisos), evidencias);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: coloca os avisos das tentativas ANTERIORES na frente dos da decisão
     * final, para o console contar a história na ordem em que ela aconteceu.
     *
     * <p>INVARIANTES DO DOMÍNIO: o {@code switch} é exaustivo e sem {@code default} — um quarto
     * desfecho em {@link DecisaoFala} tem de quebrar a compilação aqui, e não perder os avisos
     * anteriores em silêncio.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista anterior vazia devolve a decisão intacta.
     */
    private DecisaoFala precedidaDe(List<String> anteriores, DecisaoFala decisao) {
        if (anteriores.isEmpty()) {
            return decisao;
        }
        List<String> todos = new ArrayList<>(anteriores);
        todos.addAll(decisao.avisosAoOperador());
        return switch (decisao) {
            case DecisaoFala.Manter _ -> new DecisaoFala.Manter(todos);
            case DecisaoFala.Pendente _ -> new DecisaoFala.Pendente(todos);
            case DecisaoFala.Corrigir corrigir -> new DecisaoFala.Corrigir(corrigir.texto(), todos);
        };
    }
}
