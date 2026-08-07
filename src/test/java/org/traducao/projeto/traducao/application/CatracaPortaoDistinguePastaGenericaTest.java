package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: guarda de arquitetura. Impede que o desfecho
 * {@code INDETERMINADO} volte a emitir UM ÚNICO aviso para dois casos que pedem consertos
 * opostos — "obra ainda sem lore cadastrada" (cadastre a lore) e "a pasta não diz que obra é
 * esta" (renomeie a pasta).
 *
 * <h2>O prejuízo que originou</h2>
 * Medido em 07/08/2026: a obra derivada do caminho saía {@code "Season 05"} em metade das
 * pastas do acervo. O aviso mandava "confirme que é a lore certa" — instrução impossível,
 * porque não existe lore chamada "Season 05" — e calava sobre o dano real, que é o cache ir
 * para {@code cache/Season 5/}, já ocupado por 22 arquivos do Gundam Unicorn.
 *
 * <h2>Por que ler o FONTE e não o comportamento</h2>
 * {@link GuardaContextoObraTraducao} depende de quatro colaboradores concretos
 * ({@code GerenciadorContexto}, {@code ResolvedorCacheTraducao}, {@code ConsoleUILogger} e o
 * validador) e o projeto não tem Mockito. Sem esta catraca, apagar o desvio no
 * {@code case INDETERMINADO} deixa toda a suíte VERDE:
 * {@code PastaGenericaNaoIdentificaObraTest} exercita o validador diretamente e não enxerga
 * quem o chama. A regra sobrevive porque um teste vermelho não se ignora.
 *
 * <h2>Calibração</h2>
 * O caso-controle está DENTRO deste arquivo: {@link #instrumentoReprovaFonteSemDesvio()}
 * monta à mão o corpo do {@code case} como ele era ANTES da correção e exige que a mesma
 * verificação o REPROVE. Sem isso, uma verificação que não enxerga nada aprovaria por acidente.
 */
class CatracaPortaoDistinguePastaGenericaTest {

    private static final Path GUARDA = Path.of("src", "main", "java", "org", "traducao",
        "projeto", "traducao", "application", "GuardaContextoObraTraducao.java");

    /**
     * PROPÓSITO DE NEGÓCIO: a verificação em si — o corpo do {@code case INDETERMINADO} precisa
     * consultar {@code pastaGenerica} e ter as DUAS mensagens à disposição.
     *
     * <p>INVARIANTES DO DOMÍNIO: recebe o texto como parâmetro para que o caso-controle possa
     * exercitá-la com um fonte doente montado à mão, sem tocar no arquivo real.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: função pura sobre a string; não lê disco e não lança.
     */
    private static boolean distingueOsDoisCasos(String fonte) {
        int inicio = fonte.indexOf("case INDETERMINADO");
        if (inicio < 0) {
            return false;
        }
        int fim = fonte.indexOf("case CASA", inicio);
        String bloco = fim > inicio ? fonte.substring(inicio, fim) : fonte.substring(inicio);
        return bloco.contains("pastaGenerica")
            && bloco.contains("mensagemDePastaGenerica")
            && bloco.contains("mensagemDeIndeterminacao");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o fonte de produção, hoje, distingue os dois casos.
     */
    @Test
    @DisplayName("o portao distingue pasta generica de obra sem lore")
    void portaoDistingueOsDoisCasos() throws IOException {
        assertTrue(Files.exists(GUARDA),
            "instrumento cego: " + GUARDA.toAbsolutePath() + " nao existe — se a classe mudou "
                + "de lugar, esta catraca precisa apontar para o novo caminho, nao ser apagada");

        assertTrue(distingueOsDoisCasos(Files.readString(GUARDA)),
            "o case INDETERMINADO de GuardaContextoObraTraducao precisa consultar pastaGenerica() "
                + "e escolher entre mensagemDePastaGenerica() e mensagemDeIndeterminacao(). Emitir "
                + "o mesmo texto para \"Season 05\" e para \"Memories (1995)\" manda o operador "
                + "procurar uma lore que nunca vai existir, e cala sobre a colisao de cache.");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CASO-CONTROLE. A verificação acima só vale se ela for capaz de
     * reprovar — este teste monta o corpo do {@code case} como era antes da correção e exige
     * o vermelho.
     */
    @Test
    @DisplayName("instrumento calibrado: reprova o fonte SEM o desvio")
    void instrumentoReprovaFonteSemDesvio() {
        String doente = """
            switch (veredicto) {
                case INDETERMINADO -> {
                    String aviso = validadorCompatibilidade.mensagemDeIndeterminacao(obra, contexto.id());
                    log.warn(aviso);
                    uiLogger.log("[ AVISO ] " + aviso);
                }
                case CASA -> log.info("confere");
            }
            """;
        assertFalse(distingueOsDoisCasos(doente),
            "a verificacao APROVOU o fonte anterior a correcao — ela nao esta enxergando nada "
                + "e aprovaria qualquer regressao");

        assertFalse(distingueOsDoisCasos("class Vazio {}"),
            "fonte sem o case INDETERMINADO nao pode ser aprovado por ausencia");
    }
}
