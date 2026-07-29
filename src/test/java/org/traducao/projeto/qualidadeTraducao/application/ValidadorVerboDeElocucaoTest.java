package org.traducao.projeto.qualidadeTraducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * PROPÓSITO DE NEGÓCIO: discurso relatado NÃO é locutor inventado.
 *
 * <h2>O buraco, e os dois casos que o provaram</h2>
 * A guarda de locutor acusa qualquer fala que comece com {@code <prefixo curto>:} e não esteja
 * numa allowlist de conectivos. A allowlist só trazia substantivos e interjeições
 * ({@code repito}, {@code atenção}, {@code aviso}, {@code olha}) — e nenhum VERBO DE ELOCUÇÃO.
 *
 * <p>Em português, discurso relatado EXIGE dois-pontos. Então toda fala em que o inglês diz
 * {@code "X said, '...'"} caía como invenção do modelo. Medido em obras diferentes:
 * <pre>
 *   08th  EN "They said \"Now we're reborn.\""         PT "Eles disseram: \"Agora nascemos...\""
 *   ZZ    EN "He said, \"That can wait till later!\""  PT "Ele disse: \"Isso pode esperar...\""
 * </pre>
 * No 08th custou 1 das 10 falas vazias da rodada de 2026-07-28: a tradução estava correta e foi
 * descartada.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O sujeito vem ANTES do verbo em português ("Eles disseram", "Ele disse"), então o padrão
 *       aceita um pronome opcional. Casar só o verbo deixaria os DOIS casos reais de fora — é o
 *       que o primeiro teste afirma, com as duas formas.</li>
 *   <li>Alargar allowlist CEGA uma guarda. Por isso cada caso liberado aqui vem com o vizinho
 *       que precisa continuar sendo acusado: nome de personagem antes dos dois-pontos segue
 *       sendo locutor inventado, e foi assim que 2 alucinações reais do 08th foram barradas.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Se a allowlist encolher, volta a descartar tradução correta; se crescer demais, o modelo passa
 * a poder atribuir fala a quem quiser.
 */
class ValidadorVerboDeElocucaoTest {

    private final ValidadorTraducaoService validador =
        new ValidadorTraducaoService(LoreAtivaFake.vazia());

    @Test
    @DisplayName("discurso relatado com verbo de elocução passa — os dois casos medidos")
    void discursoRelatadoPassa() {
        assertDoesNotThrow(() -> validador.validarPar(
            "They said \"Now we're reborn.\"",
            "Eles disseram: \"Agora nascemos de novo.\""));

        assertDoesNotThrow(() -> validador.validarPar(
            "He said, \"That can wait till later!\"",
            "Ele disse: \"Isso pode esperar até mais tarde!\""));
    }

    @Test
    @DisplayName("outras formas de elocução, com e sem pronome")
    void outrasFormasDeElocucao() {
        assertDoesNotThrow(() -> validador.validarPar(
            "She answered, \"Not yet.\"", "Ela respondeu: \"Ainda não.\""));
        assertDoesNotThrow(() -> validador.validarPar(
            "Then he shouted, \"Get down!\"", "Gritou: \"Abaixem-se!\""));
        assertDoesNotThrow(() -> validador.validarPar(
            "You asked: is it over?", "Você perguntou: acabou?"));
    }

    /**
     * O vizinho obrigatório. Se a allowlist tivesse ficado larga demais, o modelo poderia
     * atribuir qualquer fala a qualquer personagem. Estas duas alucinações reais do 08th foram
     * barradas pela guarda e PRECISAM continuar sendo.
     */
    @Test
    @DisplayName("nome de personagem antes dos dois-pontos continua sendo locutor inventado")
    void nomeDePersonagemContinuaAcusado() {
        assertThrows(AlucinacaoDetectadaException.class,
            () -> validador.validarPar(
                "Michel...", "Michel: \"Estou preocupado com o Sanders.\""));

        assertThrows(AlucinacaoDetectadaException.class,
            () -> validador.validarPar(
                "Michel, distract him with the hovertruck!",
                "Michel: \"Distraia ele com o hovertruck!\""));
    }

    /**
     * Os conectivos que já estavam na allowlist não podem ter sido perdidos na edição — é o
     * risco de mexer numa alternância de regex grande.
     */
    @Test
    @DisplayName("os conectivos antigos continuam valendo")
    void conectivosAntigosPreservados() {
        assertDoesNotThrow(() -> validador.validarPar("I repeat: fall back!", "Repito: recuar!"));
        assertDoesNotThrow(() -> validador.validarPar("Listen: it's over.", "Escuta: acabou."));
        assertDoesNotThrow(() -> validador.validarPar("Attention: all units.", "Atenção: todas as unidades."));
    }
}
