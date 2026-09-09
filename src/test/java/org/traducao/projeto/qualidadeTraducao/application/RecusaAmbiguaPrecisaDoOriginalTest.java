package org.traducao.projeto.qualidadeTraducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que uma construção portuguesa ambígua — "não tenho acesso", "não
 * recebi", "preciso de mais informações", "pode me enviar" — só é lida como recusa do modelo
 * quando o ORIGINAL não a sustenta, e que a recusa de verdade continua bloqueada.
 *
 * <p>ORIGEM (auditoria de terceiro, 2026-09-09, com o Aya carregado): cinco traduções corretas
 * eram descartadas nas três temperaturas. O modelo produzia a resposta certa e a regra a jogava
 * fora, então nem trocar de modelo nem subir temperatura resolveria.
 *
 * <p>INVARIANTES DO DOMÍNIO: o original absolve, jamais condena. Todo caso que bloqueava sem o
 * original continua bloqueando sem ele — é o que o bloco "sem original nada muda" fixa.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: o validador lança {@link AlucinacaoDetectadaException}.
 */
@DisplayName("Construção ambígua: quem desempata é o original, não o português")
class RecusaAmbiguaPrecisaDoOriginalTest {

    private final ValidadorTraducaoService validador =
        new ValidadorTraducaoService(LoreAtivaFake.vazia());

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource(delimiter = '|', value = {
        "I don't have access to the hangar.        | Não tenho acesso ao hangar.",
        "I need more information about the enemy.  | Preciso de mais informações sobre o inimigo.",
        "Can you send me the coordinates?          | Pode me enviar as coordenadas?",
        "Send the signal again.                    | Envie o sinal novamente.",
        "Here's the answer to your question.       | Aqui está a resposta à sua pergunta.",
        "We haven't received orders to sortie!     | Não recebi ordens para decolar!",
        "There's no way to know that.              | Não tenho como saber disso.",
        "Provide the information to command.      | Forneça as informações ao comando.",
    })
    @DisplayName("tradução correta passa quando o original sustenta a construção")
    void traducaoCorretaPassaQuandoOriginalSustenta(String original, String traduzido) {
        assertDoesNotThrow(() -> validador.validarFala(traduzido, original));
        assertDoesNotThrow(() -> validador.validarPar(original, traduzido));
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource(delimiter = '|', value = {
        "Go!             | Não recebi nada disso.",
        "Eh?             | Não tenho acesso ao material que preciso.",
        "Dunno.          | Preciso de mais informações para continuar.",
        "Hurry!          | Pode me enviar o texto que devo trabalhar?",
        "Fire!           | Aqui está a resposta que você buscava.",
    })
    @DisplayName("CASO-CONTROLE: recusa de verdade continua bloqueada, porque o original não sustenta")
    void recusaDeVerdadeContinuaBloqueada(String original, String traduzido) {
        var erro = assertThrows(AlucinacaoDetectadaException.class,
            () -> validador.validarFala(traduzido, original));
        assertTrue(erro.getMessage().contains("sem apoio no original"),
            "o diagnostico tem de dizer que faltou apoio no original, e disse: " + erro.getMessage());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @CsvSource(delimiter = '|', value = {
        "Não tenho acesso ao hangar.",
        "Preciso de mais informações sobre o inimigo.",
        "Pode me enviar as coordenadas?",
        "Aqui está a resposta à sua pergunta.",
    })
    @DisplayName("sem original nada muda: quem não tem o texto de partida segue com a proteção antiga")
    void semOriginalNadaMuda(String traduzido) {
        assertThrows(AlucinacaoDetectadaException.class, () -> validador.validarFala(traduzido));
    }

    @Test
    @DisplayName("MEDIDO NO ACERVO: a fronteira de palavra impede 'chaMANDO' de virar 'mandar novamente'")
    void chamandoNaoEhMandarNovamente() {
        String original = "And Im calling calling out your name again";
        String traduzido = "E eu estou chamando seu nome novamente.";

        assertDoesNotThrow(() -> validador.validarPar(original, traduzido));

        // Sem o original a fronteira sozinha já resolve: "chamando" não é "mandando".
        assertDoesNotThrow(() -> validador.validarFala(traduzido));
    }

    @Test
    @DisplayName("o diagnóstico nomeia QUAL construção ficou sem apoio, não apenas 'recusa'")
    void diagnosticoNomeiaAConstrucao() {
        var erro = assertThrows(AlucinacaoDetectadaException.class,
            () -> validador.validarFala("Não tenho acesso ao arquivo.", "Move out!"));
        assertTrue(erro.getMessage().contains("não tenho acesso"),
            "o diagnostico tem de nomear a construcao, e disse: " + erro.getMessage());
    }

    @Test
    @DisplayName("recusa classica do LLM e pega pelo PROTOCOLO, antes de a regra ambigua opinar")
    void recusaClassicaCaiNoProtocolo() {
        // "Go!" -> "Nao recebi nenhuma linha para traduzir." foi o bug historico. Ele casa
        // "linha para traduzir", que e vocabulario de protocolo e NUNCA dependeu do original.
        // O teste existe para fixar a ordem: afrouxar a regra ambigua nao reabre este caso.
        var erro = assertThrows(AlucinacaoDetectadaException.class,
            () -> validador.validarFala("Não recebi nenhuma linha para traduzir.", "Go!"));
        assertTrue(erro.getMessage().contains("Recusa/meta-resposta"),
            "esperava a recusa de protocolo, e veio: " + erro.getMessage());
    }

    @Test
    @DisplayName("vocabulário de protocolo continua bloqueando mesmo com original que fala de tradução")
    void protocoloBloqueiaAindaComOriginalParecido() {
        // "translate" no original NÃO licencia o modelo a falar sobre a própria tarefa: esta
        // classe de recusa nunca dependeu do original e segue em PADRAO_RECUSA_META.
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("Sou capaz de traduzir legendas de anime.", "Can you translate this?"));
    }
}
