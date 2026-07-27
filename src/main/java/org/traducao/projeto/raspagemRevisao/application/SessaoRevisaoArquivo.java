package org.traducao.projeto.raspagemRevisao.application;

import org.traducao.projeto.legenda.domain.EventoLegenda;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: a memória de trabalho da revisão de UM arquivo — a legenda sendo remontada
 * fala a fala, o que já se aprendeu sobre falas repetidas, e a contagem do que aconteceu.
 *
 * <h2>Por que a memória precisa existir</h2>
 * Um episódio repete falas ("Sim.", "Entendido.", bordões). Sem lembrar o que a primeira ocorrência
 * produziu, cada repetição pagaria de novo a chamada ao Google ou ao LLM — o custo aparece como
 * lentidão e cota consumida, nunca como erro. Duas memórias, com papéis opostos:
 * <ul>
 *   <li><b>Correções conhecidas</b>: "este texto eu já corrigi assim" — reaproveita a resposta.</li>
 *   <li><b>Sem alteração</b>: "este texto eu já tentei e não melhora" — evita tentar de novo.</li>
 * </ul>
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>VIDA DE UM ARQUIVO. É criada no início dele e descartada no fim. Guardar este estado num
 *       campo de {@code @Service} (que é singleton) faria o segundo episódio herdar a memória do
 *       primeiro: "já tentei, não melhora" de outra obra, com outra lore.</li>
 *   <li>Toda saída registra o evento — inclusive a fala mantida sem tocar. É isso que garante que o
 *       documento remontado tenha o MESMO número de falas do original. Um caminho que esqueça de
 *       registrar apaga uma legenda em silêncio.</li>
 *   <li>Os contadores são acumulados aqui e lidos no fim, em vez de {@code int[]} mutados de dentro
 *       do laço. Chave repetida numa memória NÃO é sobrescrita: a primeira decisão vale, para o
 *       resultado não depender da ordem de varredura.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Chave nula ou em branco é ignorada pelas memórias — uma fala sem texto mascarável não tem
 * identidade para ser lembrada, e registrá-la sob a chave vazia contaminaria todas as outras.
 */
public class SessaoRevisaoArquivo {

    private final List<EventoLegenda> eventos = new ArrayList<>();
    private final Map<String, String> correcoesConhecidas = new HashMap<>();
    private final Set<String> semAlteracao = new LinkedHashSet<>();

    private int corrigidas;
    private int auditadas;
    private int problemas;
    private int semOriginal;
    private int pendentes;
    private boolean modificado;

    /**
     * PROPÓSITO DE NEGÓCIO: a fala segue como está.
     * <p>INVARIANTES DO DOMÍNIO: registra o evento; não conta nada.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    public void manter(EventoLegenda evento) {
        eventos.add(evento);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a fala tinha problema e continua tendo — nada aplicável foi
     * encontrado.
     * <p>INVARIANTES DO DOMÍNIO: registra o evento intacto E conta a pendência. As duas coisas
     * andavam sempre juntas no laço, em oito lugares; separá-las é como se perde uma.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    public void pendente(EventoLegenda evento) {
        eventos.add(evento);
        pendentes++;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a fala foi corrigida e o arquivo passa a merecer gravação.
     * <p>INVARIANTES DO DOMÍNIO: registra o evento JÁ com o texto novo, conta a correção e marca o
     * arquivo como modificado — o trio nunca acontece pela metade.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    public void corrigir(EventoLegenda evento, String textoNovo) {
        eventos.add(evento.comTexto(textoNovo));
        corrigidas++;
        modificado = true;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: registra a correção de um evento cujo texto já foi trocado antes de
     * chegar aqui (é o caso do saneamento de karaokê, que altera o evento no meio da preparação).
     * <p>INVARIANTES DO DOMÍNIO: conta e marca modificado, SEM registrar o evento — quem o
     * registra é o desfecho final da fala.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    public void contarCorrecaoJaAplicada() {
        corrigidas++;
        modificado = true;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: lembra que este texto, uma vez tentado, não rende melhoria.
     * <p>INVARIANTES DO DOMÍNIO: chave nula ou em branco nunca entra — sem identidade não há o que
     * lembrar, e a chave vazia casaria com falas sem relação nenhuma.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    public void registrarSemAlteracao(String chave) {
        if (chave != null && !chave.isBlank()) {
            semAlteracao.add(chave);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: já se sabe que este texto não melhora?
     * <p>INVARIANTES DO DOMÍNIO: consulta pura; chave nula responde não.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    public boolean jaSabidoSemAlteracao(String chave) {
        return chave != null && semAlteracao.contains(chave);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: guarda a correção já obtida para este texto, para a próxima ocorrência
     * não pagar a chamada externa de novo.
     * <p>INVARIANTES DO DOMÍNIO: a PRIMEIRA decisão vale; chave repetida não é sobrescrita, para o
     * resultado não depender da ordem em que as falas aparecem no arquivo.
     * <p>COMPORTAMENTO EM CASO DE FALHA: chave nula é ignorada.
     */
    public void registrarCorrecao(String chave, String textoMascarado) {
        if (chave != null && !chave.isBlank()) {
            correcoesConhecidas.putIfAbsent(chave, textoMascarado);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a correção já conhecida para este texto, se houver.
     * <p>INVARIANTES DO DOMÍNIO: consulta pura; devolve {@code null} quando não há.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    public String correcaoConhecida(String chave) {
        return chave == null ? null : correcoesConhecidas.get(chave);
    }

    /** Conta uma fala que passou pela auditoria. */
    public void contarAuditada() {
        auditadas++;
    }

    /** Conta uma fala em que a auditoria achou problema. */
    public void contarProblema() {
        problemas++;
    }

    /** Conta uma fala sem original inglês comparável. */
    public void contarSemOriginal() {
        semOriginal++;
    }

    /** Conta uma pendência sem registrar evento — para os desfechos que já registraram o seu. */
    public void contarPendente() {
        pendentes++;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: marca que a sincronização com o cache já alterou o arquivo, antes mesmo
     * da revisão começar.
     * <p>INVARIANTES DO DOMÍNIO: é o que faz um arquivo só sincronizado ser gravado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    public void marcarModificado() {
        modificado = true;
    }

    /** A legenda remontada, na ordem original. */
    public List<EventoLegenda> eventos() {
        return eventos;
    }

    public int corrigidas() {
        return corrigidas;
    }

    public int auditadas() {
        return auditadas;
    }

    public int problemas() {
        return problemas;
    }

    public int semOriginal() {
        return semOriginal;
    }

    public int pendentes() {
        return pendentes;
    }

    public boolean modificado() {
        return modificado;
    }
}
