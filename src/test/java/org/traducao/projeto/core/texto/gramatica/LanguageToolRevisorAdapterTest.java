package org.traducao.projeto.core.texto.gramatica;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: provar que o revisor gramatical faz exatamente o que a medição prometeu —
 * acha a inversão verbo/substantivo, e cala a boca no resto.
 *
 * <h2>Por que cada caso está aqui</h2>
 * Nenhum destes testes foi inventado: cada um congela uma decisão que a medição de 23/08/2026
 * tomou contra o gold set do Macross II, e que sem teste voltaria na primeira mexida.
 *
 * <ul>
 *   <li><b>Positivo</b> — {@code a milicia} tem de ser acusado. É a classe inteira que motivou
 *       trazer a ferramenta: o dicionário aceita {@code milicia} porque é forma verbal.</li>
 *   <li><b>Negativo, e é o que separa isto de uma lista de palavras</b> — {@code ele milicia}
 *       NÃO pode ser acusado. Um corretor por lista trocaria as duas, e foi para não trocar que
 *       se pagou o preço de um POS tagger.</li>
 *   <li><b>Lore</b> — nome de obra não pode ser acusado. Com o corretor ortográfico ligado eram
 *       83 acusações em nome próprio; ele entra desligado justamente por isso.</li>
 *   <li><b>Estilo</b> — {@code bode expiatório} e {@code shampoo} são achados REAIS do
 *       LanguageTool, e não são defeito de legenda. As categorias de estilo ficam de fora.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Se o motor não carregar (teto de entidade XML sem os {@code -D} do build), os testes declaram
 * isso e não afirmam nada — um verde por motor ausente seria pior que vermelho.
 */
class LanguageToolRevisorAdapterTest {

    private static LanguageToolRevisorAdapter revisor;
    private static boolean motorVivo;

    @BeforeAll
    static void carregar() {
        revisor = new LanguageToolRevisorAdapter();
        motorVivo = revisor.disponivel();
    }

    /** Guarda dos guardas: sem motor, nenhum teste abaixo prova coisa alguma. */
    @Test
    @DisplayName("o motor carrega — sem isto nenhum outro caso deste arquivo vale")
    void motorCarrega() {
        assertTrue(motorVivo, () ->
            "NAO VERIFICADO: o revisor gramatical nao carregou. Motivo: "
            + revisor.motivoDaIndisponibilidade()
            + ". Confira TETOS_XML_DO_LANGUAGETOOL no build.gradle — o grammar.xml do portugues "
            + "estoura o teto de entidade XML do JDK e o motor nasce morto sem os dois -D.");
    }

    @Test
    @DisplayName("POSITIVO: acusa o substantivo escrito como verbo depois de artigo")
    void acusaInversaoVerboSubstantivo() {
        assumirMotor();
        List<AchadoGramatical> achados = revisor.revisar("A milicia ordenou um blackout.");
        assertFalse(achados.isEmpty(),
            "a classe que motivou trazer a ferramenta nao foi acusada — 'A milicia' deveria ser "
            + "'A milícia', e o dicionario nao ve porque 'milicia' e forma verbal valida");
        assertTrue(achados.stream().anyMatch(a -> a.trecho().toLowerCase().contains("milicia")),
            "acusou alguma coisa, mas nao a palavra certa: " + achados);
    }

    @Test
    @DisplayName("NEGATIVO: a MESMA palavra como verbo passa intacta — e isto e o ponto")
    void naoAcusaAMesmaPalavraComoVerbo() {
        assumirMotor();
        List<AchadoGramatical> achados = revisor.revisar("O reporter noticia o caso todo dia.");
        assertTrue(achados.stream().noneMatch(a -> a.trecho().toLowerCase().contains("noticia")),
            "acusou 'noticia' onde ela e VERBO, e nao substantivo. Um corretor por lista de "
            + "palavras faria exatamente este estrago — e evitar isso foi a razao de trazer um "
            + "POS tagger em vez de ampliar a lista. Achados: " + achados);
    }

    @Test
    @DisplayName("LORE: nome de obra nao vira erro de ortografia")
    void naoAcusaNomeDeLore() {
        assumirMotor();
        List<AchadoGramatical> achados = revisor.revisar("O Zentradi atacou a nave Macross.");
        assertTrue(achados.stream().noneMatch(a ->
                a.trecho().contains("Zentradi") || a.trecho().contains("Macross")),
            "acusou nome de lore. Com o corretor ortografico ligado eram 83 acusacoes assim na "
            + "medicao de 23/08/2026, e e por isso que ele entra DESLIGADO. Achados: " + achados);
    }

    @Test
    @DisplayName("ESTILO: fica de fora, mesmo quando o LanguageTool tem razao")
    void naoAcusaEstilo() {
        assumirMotor();
        List<AchadoGramatical> achados = revisor.revisar(
            "O bode expiatorio comprou shampoo aos vinte anos de idade.");
        assertTrue(achados.stream().noneMatch(a ->
                "STYLE".equals(a.categoria()) || "REDUNDANCY".equals(a.categoria())
                || "PUNCTUATION".equals(a.categoria())),
            "categoria de estilo vazou. Elas nao erram — 'bode expiatorio' e frase-feita mesmo, e "
            + "'anos de idade' e pleonasmo mesmo. So que nada disso e defeito numa legenda, e a "
            + "medicao mostrou 3 acertos contra 9 alarmes nesse bloco. Achados: " + achados);
    }

    @Test
    @DisplayName("toda categoria devolvida esta na lista que a medicao aprovou")
    void soDevolveCategoriaLigada() {
        assumirMotor();
        List<String> textos = List.of(
            "A milicia ordenou um blackout de noticias ate a conferencia.",
            "O bode expiatorio comprou shampoo aos vinte anos de idade.",
            "Nao ha imagens suas? Por que? Eu nao entendo.",
            "O reporter noticia o caso e orbita o assunto todo dia.");
        for (String t : textos) {
            for (AchadoGramatical a : revisor.revisar(t)) {
                assertTrue(LanguageToolRevisorAdapter.CATEGORIAS_LIGADAS.contains(a.categoria()),
                    "vazou a categoria " + a.categoria() + " em: " + t + " -> " + a);
            }
        }
    }

    @Test
    @DisplayName("texto nulo, vazio e so-espaco devolvem lista vazia sem lancar")
    void entradaDegenerada() {
        assertEquals(List.of(), revisor.revisar(null));
        assertEquals(List.of(), revisor.revisar(""));
        assertEquals(List.of(), revisar("   "));
    }

    @Test
    @DisplayName("o achado carrega posicao, trecho e mensagem utilizaveis")
    void achadoEUtilizavel() {
        assumirMotor();
        String texto = "A milicia ordenou um blackout.";
        List<AchadoGramatical> achados = revisor.revisar(texto);
        assertFalse(achados.isEmpty(), "sem achado nao ha o que conferir");
        AchadoGramatical a = achados.get(0);
        assertTrue(a.inicio() >= 0 && a.fim() <= texto.length() && a.inicio() < a.fim(),
            "posicoes fora do texto entregue: " + a);
        assertEquals(texto.substring(a.inicio(), a.fim()), a.trecho(),
            "o trecho tem de ser exatamente o recorte das posicoes — quem for aplicar depende disso");
        assertNotNull(a.mensagem(), "achado sem mensagem nao ajuda o operador");
        assertNotNull(a.sugestoes(), "sugestoes nunca podem ser nulas; vazia e um caso legitimo");
    }

    private static List<AchadoGramatical> revisar(String t) {
        return revisor.revisar(t);
    }

    /** Sem motor os casos abaixo nao provam nada, e um verde por ausencia seria mentira. */
    private static void assumirMotor() {
        assertTrue(motorVivo, "NAO VERIFICADO: motor gramatical ausente — "
            + revisor.motivoDaIndisponibilidade());
    }
}
