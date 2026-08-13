package org.traducao.projeto.traducaoKaraoke.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.traducaoKaraoke.domain.ClasseLinhaKaraoke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * PROPÓSITO DE NEGÓCIO: guarda as duas decisões de classificação que custaram 22 episódios
 * inteiros no Unicorn, cada uma medida arquivo por arquivo em 13/08/2026.
 *
 * <h2>Por que o E01 e o E22 são os casos certos (indicação do Paulo)</h2>
 * As duas pontas da obra têm estruturas de música DIFERENTES, e é a diferença que expõe os
 * defeitos:
 * <ul>
 *   <li><b>E01</b> — encerramento com DUAS camadas ({@code ED} romaji + {@code ED - EN} inglês).
 *       Aqui traduzir uma e preservar a outra funciona, e mascarou o defeito por 12 episódios.
 *       E o {@code ED} é MISTO: 12 linhas em romaji e 3 em inglês, na mesma faixa.</li>
 *   <li><b>E22</b> — encerramento com UMA camada só ({@code ED2}). Sem irmã para preservar, a
 *       substituição apagava a letra da tela: 22 de 23 linhas, em 10 episódios.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Reprovar aqui significa que uma das duas classes de dano voltou: abertura em inglês na tela,
 * ou letra original desaparecida.
 */
@QuarkusTest
@DisplayName("karaokê: as duas decisões que custaram os 22 episódios do Unicorn")
class PreservacaoCamadaUnicaTest {

    @Inject
    ClassificadorLetraKaraokeService classificador;

    /** Tag de efeito real do OPL2 do Unicorn — TODAS as 155 linhas do estilo a carregam. */
    private static final String EFEITO = "{\\t(0,300,\\fscx110)\\move(100,200,300,400)}";

    /**
     * O defeito de maior alcance: 0 de 69 linhas traduzidas em TODOS os 22 episódios. A abertura
     * ficava em inglês na tela, empilhada com os próprios fragmentos.
     */
    @Test
    @DisplayName("E01/OPL2: FRASE com tag de efeito é letra e vai ao LLM")
    void fraseComEfeitoNaoEhFragmento() {
        assertNotEquals(ClasseLinhaKaraoke.EFEITO_KFX,
            classificador.classificar("OPL2", EFEITO + "And Im calling calling out your name again"),
            "A ABERTURA VOLTOU A FICAR EM INGLES. No Unicorn TODAS as 155 linhas do OPL2 tem tag "
                + "de efeito, inclusive as 17 que sao a letra — decidir so pela tag zerou a "
                + "traducao do estilo nos 22 episodios.");
        assertNotEquals(ClasseLinhaKaraoke.EFEITO_KFX,
            classificador.classificar("OPL2", EFEITO + "Do you feel alone"),
            "quatro palavras é frase, não sílaba");
    }

    /**
     * O outro lado, e é o que mantém o viés de preservar: sílaba solta continua intocável. São
     * 138 das 155 linhas do OPL2 no E01 — traduzir fragmento destrói a animação.
     */
    @Test
    @DisplayName("E01/OPL2: PALAVRA ÚNICA com tag de efeito continua fragmento")
    void palavraUnicaComEfeitoContinuaKfx() {
        for (String silaba : new String[] {"Do", "you", "feel", "lone", "hear"}) {
            assertEquals(ClasseLinhaKaraoke.EFEITO_KFX,
                classificador.classificar("OPL2", EFEITO + silaba),
                "sílaba '" + silaba + "' virou traduzível — são 138 linhas assim só no E01, e "
                    + "traduzir fragmento destrói o karaokê");
        }
    }

    /**
     * O E01 tem o caso mais fino da obra: a MESMA faixa {@code ED} traz 12 linhas em romaji e 3
     * em inglês. O classificador precisa decidir LINHA A LINHA, não por nome de estilo — e a
     * varredura de 13/08 mostrou que ele acerta.
     */
    @Test
    @DisplayName("E01/ED: faixa MISTA — romaji preservado e inglês traduzido na MESMA faixa")
    void faixaMistaDecidePorLinha() {
        assertEquals(ClasseLinhaKaraoke.ORIGINAL_JAPONES,
            classificador.classificar("ED", "Furi dake no kotae to eeru tai de ensou o tsunagu"),
            "romaji na faixa ED tem de ser preservado");
        assertNotEquals(ClasseLinhaKaraoke.ORIGINAL_JAPONES,
            classificador.classificar("ED", "Take off my dress and crown, then I can fall sound asleep"),
            "a MESMA faixa ED tem 3 linhas em ingles no E01 — decidir por nome de estilo as "
                + "deixaria sem traduzir");
    }

    /**
     * O E22 é a outra ponta: {@code ED2}, camada única. Ela é inglês e tem de ser traduzida — o
     * que preserva a original é o empilhamento com {@code \N}, coberto pelo teste seguinte.
     */
    @Test
    @DisplayName("E22/ED2: camada única em inglês é traduzível")
    void camadaUnicaInglesaEhTraduzivel() {
        assertEquals(ClasseLinhaKaraoke.TRADUZIVEL_INGLES,
            classificador.classificar("ED2", "Behind your mask"),
            "o ED2 dos episodios 13-22 e a unica camada, e ela e ingles");
        assertEquals(ClasseLinhaKaraoke.TRADUZIVEL_INGLES,
            classificador.classificar("ED2", "You smile, showing me that grin with greed"));
    }

    /** Diálogo e placa continuam fora do karaokê, com ou sem as mudanças acima. */
    @Test
    @DisplayName("contraprova: diálogo comum não entra no karaokê")
    void dialogoNaoEhKaraoke() {
        assertEquals(ClasseLinhaKaraoke.FORA_DE_MUSICA,
            classificador.classificar("Default", "Banagher, você está bem?"),
            "diálogo classificado como música seria tradução dupla e ruído no relatório");
    }
}
