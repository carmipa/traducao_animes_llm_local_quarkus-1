package org.traducao.projeto.traducao.domain.fallback;

import java.util.Objects;
import java.util.Optional;

/**
 * PROPÓSITO DE NEGÓCIO: desfecho COMPLETO de uma tentativa de recuperar uma fala pendente —
 * a tradução (quando houve), qual provedor atendeu, a causa e o motivo legível. Existe para
 * que uma recusa nunca seja silenciosa: o chamador consegue logar a razão, contabilizá-la por
 * causa e decidir se aciona o próximo provedor da cadeia, coisas impossíveis com um
 * {@code Optional.empty()} que não diz nada.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@link StatusFallback#RECUPERADA} exige {@code traducao} não vazia; qualquer outro
 *       status exige {@code traducao} nula — não existe "meio recuperada". O construtor
 *       canônico faz valer essa regra.</li>
 *   <li>{@code provedor} e {@code status} nunca são nulos; {@code motivo} nunca é nulo (vazio
 *       vira texto do próprio status), para que o log jamais imprima {@code null} como causa.</li>
 *   <li>Record imutável de domínio: só JDK, sem framework, sem I/O e sem conhecer transporte.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Uma combinação incoerente (sucesso sem texto, ou fracasso com texto) lança
 * {@link IllegalArgumentException} na construção — é erro de programação, não estado de
 * runtime, e falhar cedo evita publicar legenda vazia. {@code provedor}/{@code status} nulos
 * lançam {@link NullPointerException}.
 *
 * @param traducao texto traduzido e aprovado, ou {@code null} quando não houve recuperação
 * @param provedor quem atendeu (ou {@link ProvedorFallback#NENHUM} quando não se tentou)
 * @param status causa canônica do desfecho
 * @param motivo explicação legível para log e relatório; nunca nulo
 */
public record ResultadoFallback(
    String traducao,
    ProvedorFallback provedor,
    StatusFallback status,
    String motivo
) {

    public ResultadoFallback {
        Objects.requireNonNull(provedor, "provedor");
        Objects.requireNonNull(status, "status");
        boolean temTexto = traducao != null && !traducao.isBlank();
        if (status == StatusFallback.RECUPERADA && !temTexto) {
            throw new IllegalArgumentException("RECUPERADA exige tradução não vazia");
        }
        if (status != StatusFallback.RECUPERADA && temTexto) {
            throw new IllegalArgumentException(
                "somente RECUPERADA pode carregar tradução; status recebido: " + status);
        }
        if (status != StatusFallback.RECUPERADA) {
            traducao = null;
        }
        if (motivo == null || motivo.isBlank()) {
            motivo = status.name();
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: desfecho de sucesso — a fala foi recuperada por um provedor e
     * passou por todas as verificações.
     * <p>INVARIANTES DO DOMÍNIO: exige texto não vazio; status fixo {@link StatusFallback#RECUPERADA}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto vazio lança {@link IllegalArgumentException}.
     */
    public static ResultadoFallback recuperada(String traducao, ProvedorFallback provedor) {
        return new ResultadoFallback(traducao, provedor, StatusFallback.RECUPERADA, "recuperada");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: desfecho de recusa, carregando a causa e o motivo legível para que
     * o chamador registre e contabilize em vez de perder a informação.
     * <p>INVARIANTES DO DOMÍNIO: nunca carrega tradução; {@code status} não pode ser
     * {@link StatusFallback#RECUPERADA}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: status de sucesso aqui lança {@link IllegalArgumentException}.
     */
    public static ResultadoFallback recusada(ProvedorFallback provedor, StatusFallback status, String motivo) {
        if (status == StatusFallback.RECUPERADA) {
            throw new IllegalArgumentException("recusada() não aceita o status RECUPERADA");
        }
        return new ResultadoFallback(null, provedor, status, motivo);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: desfecho de quando nem se tentou (modo desligado, entrada inválida).
     * <p>INVARIANTES DO DOMÍNIO: provedor {@link ProvedorFallback#NENHUM}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    public static ResultadoFallback naoTentada(String motivo) {
        return new ResultadoFallback(null, ProvedorFallback.NENHUM, StatusFallback.NAO_TENTADA, motivo);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: informa se há tradução publicável, sem o chamador precisar comparar
     * status manualmente.
     * <p>INVARIANTES DO DOMÍNIO: verdadeiro somente para {@link StatusFallback#RECUPERADA}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    public boolean recuperou() {
        return status == StatusFallback.RECUPERADA;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: expõe a tradução como {@link Optional}, preservando a ergonomia dos
     * call-sites que só querem o texto — sem reintroduzir a perda de causa que motivou este record.
     * <p>INVARIANTES DO DOMÍNIO: presente apenas quando {@link #recuperou()}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    public Optional<String> traducaoOpcional() {
        return Optional.ofNullable(traducao);
    }
}
