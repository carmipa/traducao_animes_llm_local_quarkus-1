package org.traducao.projeto.telemetria;

/**
 * PROPÓSITO DE NEGÓCIO: estado do fluxo ao vivo de telemetria, no formato que o
 * painel inicial exibe ao lado do orquestrador, do LLM e do cache.
 *
 * <h2>INVARIANTES DO DOMÍNIO</h2>
 * <ul>
 *   <li>Desconectado é um estado LEGÍTIMO e esperado, não um erro. O KRONOS
 *       funciona inteiro sem o fluxo, e a tela precisa dizer isso sem alarde.</li>
 *   <li>{@code detalhe} nunca fica vazio: um card que diz "desconectado" e nada
 *       mais obriga quem opera a ir caçar log. Ou traz a versão e o tamanho do
 *       fluxo, ou traz o motivo de não ter conseguido falar.</li>
 * </ul>
 *
 * @param conectado se o fluxo respondeu neste instante
 * @param detalhe versão do servidor quando conectado; motivo legível quando não
 * @param eventos quantidade de eventos no fluxo; {@code -1} quando desconhecido
 */
public record StatusFluxoTelemetria(boolean conectado, String detalhe, long eventos) {

    /**
     * PROPÓSITO DE NEGÓCIO: constrói o estado desconectado com motivo obrigatório.
     *
     * <p>INVARIANTES DO DOMÍNIO: motivo nulo ou vazio vira texto genérico, porque
     * o card não pode exibir string vazia — vazio na tela lê-se como bug.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança.
     */
    public static StatusFluxoTelemetria desconectado(String motivo) {
        String texto = (motivo == null || motivo.isBlank()) ? "sem detalhe" : motivo;
        return new StatusFluxoTelemetria(false, texto, -1);
    }
}
