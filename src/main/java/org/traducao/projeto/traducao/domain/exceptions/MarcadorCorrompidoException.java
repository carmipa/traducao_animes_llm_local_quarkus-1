package org.traducao.projeto.traducao.domain.exceptions;

import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;

/**
 * PROPÓSITO DE NEGÓCIO: distingue, entre as alucinações do LLM, o caso específico em que a
 * resposta perdeu/duplicou/inventou marcadores {@code [[TAGn]]} — e carrega a TENTATIVA
 * rejeitada junto da mensagem. Sem esse transporte, o pipeline esgotava as tentativas,
 * devolvia o texto original e a pendência era contabilizada como {@code ECO} ("o modelo
 * devolveu o original"), escondendo a causa real no painel de telemetria: na corrida de
 * 2026-07-22 o log provou 393 corrupções de marcador enquanto o KPI registrou 0.
 *
 * <p>INVARIANTES DO DOMÍNIO: continua sendo uma {@link AlucinacaoDetectadaException} — todo
 * {@code catch} existente da divisão/retry/fallback segue capturando-a sem alteração;
 * {@link #tentativa()} devolve o texto EXATO recusado pelo LLM (podendo ser vazio), nunca o
 * texto original do lote.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: propaga como {@code RuntimeException} não verificada,
 * exatamente como a superclasse; construir com {@code tentativa} nula é aceito e
 * {@link #tentativa()} devolve {@code null}. Não lança.
 */
public class MarcadorCorrompidoException extends AlucinacaoDetectadaException {

    private final String tentativa;

    /**
     * PROPÓSITO DE NEGÓCIO: cria a falha de marcador preservando o texto recusado, para que
     * o caminho de desistência possa devolvê-lo ao desmascaramento e a causa-raiz correta
     * ({@code MARCADORES_CORROMPIDOS}) chegue à telemetria em vez de virar eco.
     *
     * <p>INVARIANTES DO DOMÍNIO: delega a mensagem à superclasse; a tentativa é guardada sem
     * normalização, para que o diagnóstico mostre o que o modelo realmente devolveu.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; mensagem e tentativa nulas são aceitas.
     */
    public MarcadorCorrompidoException(String message, String tentativa) {
        super(message);
        this.tentativa = tentativa;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: expõe o texto que o LLM devolveu com os marcadores corrompidos.
     *
     * <p>INVARIANTES DO DOMÍNIO: é a resposta bruta da última tentativa, não o original.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@code null} se a exceção foi construída
     * sem tentativa; não lança.
     */
    public String tentativa() {
        return tentativa;
    }
}
