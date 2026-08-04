package org.traducao.projeto.qualidadeTraducao.application;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * PROPÓSITO DE NEGÓCIO: <b>onde o fansub quebrou a linha não pode mudar coisa alguma.</b> Esta é a
 * invariante do domínio; os testes de exemplo congelam os casos que alguém PENSOU em escrever, e a
 * propriedade sorteia a posição da quebra para achar o caso que ninguém pensou.
 *
 * <h2>Por que teste por propriedade entrou no projeto</h2>
 * Em 2026-08-04 o conserto da quebra {@code \N} foi aplicado a 12 arquivos e a suíte ficou verde.
 * Mutando a constante um a um, MEDIDO no mesmo dia: só 2 dos 12 quebravam algum teste. E o próprio
 * conserto introduziu uma corrupção nova — quebra caindo DENTRO do nome composto — que só apareceu
 * quando alguém foi escrever um gerador e perguntou "e se cair aqui?". Exemplo cobre o que se
 * imagina; propriedade cobre o espaço.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code NormalizadorAcentosComuns}: a quebra em qualquer junta entre palavras não muda
 *       QUAIS palavras recebem acento.</li>
 *   <li>{@code ValidadorTraducaoService}: a quebra em qualquer junta não muda o veredito de troca
 *       de entidade — nem para inventar troca, nem para esconder a verdadeira.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * O jqwik encolhe o contraexemplo até o menor caso que ainda falha e imprime a posição da quebra,
 * que é exatamente o dado que faltava nas investigações manuais.
 */
class QuebraAssPropriedadeTest {

    private static final String QUEBRA = "\\" + "N";

    /**
     * Palavras do sorteio: as 12 formas SEM acento do dicionário do normalizador, mais enchimento
     * que NÃO está no dicionário — inclusive {@code esta}, homógrafo excluído de propósito lá.
     */
    private static final List<String> VOCABULARIO = List.of(
        "nao", "voce", "voces", "tambem", "ate", "alem", "apos", "atras",
        "infancia", "ninguem", "alguem", "porem",
        "esta", "hoje", "capitao", "Bright", "que", "muito", "sempre");

    @Provide
    Arbitrary<List<String>> frase() {
        return Arbitraries.of(VOCABULARIO).list().ofMinSize(2).ofMaxSize(8);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: acentuar não pode depender de onde a linha foi partida.
     *
     * <p>INVARIANTES DO DOMÍNIO: normalizar o texto COM a quebra e depois trocar a quebra por
     * espaço tem de dar o mesmo que normalizar o texto SEM a quebra.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: alguma forma do dicionário ficou sem acento por estar
     * colada à quebra — o defeito medido no acervo (0 sobreviveram soltas, 11 coladas).
     */
    @Property
    void aQuebraNaoMudaQuaisPalavrasRecebemAcento(
            @ForAll("frase") List<String> palavras,
            @ForAll @IntRange(min = 0, max = 512) int escolha) {

        int junta = escolha % (palavras.size() - 1);
        var normalizador = new NormalizadorAcentosComuns();

        String semQuebra = String.join(" ", palavras);
        String comQuebra = juntar(palavras, junta);

        assertEquals(
            normalizador.normalizar(semQuebra),
            normalizador.normalizar(comQuebra).replace(QUEBRA, " "),
            () -> "quebra na junta " + junta + " mudou a acentuacao: " + comQuebra);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o veredito de troca de entidade não pode depender de onde a linha foi
     * partida — nem quando a quebra cai DENTRO do nome composto.
     *
     * <p>INVARIANTES DO DOMÍNIO: original e tradução dizendo a MESMA nave nunca é troca, com a
     * quebra em qualquer junta dos dois lados.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: o reparo inventaria troca e corromperia tradução correta,
     * ora apagando metade do nome, ora duplicando-o.
     */
    @Property
    void aQuebraNaoInventaTrocaDeEntidade(
            @ForAll @IntRange(min = 0, max = 512) int ondeNoOriginal,
            @ForAll @IntRange(min = 0, max = 512) int ondeNaTraducao) {

        List<String> original = List.of(
            "Reading", "heat", "sources", "near", "the", "Nahel", "Argama", "airspace");
        List<String> traduzido = List.of(
            "Fonte", "de", "calor", "perto", "do", "espaço", "aéreo", "da", "Nahel", "Argama");

        String orig = juntar(original, ondeNoOriginal % (original.size() - 1));
        String trad = juntar(traduzido, ondeNaTraducao % (traduzido.size() - 1));

        assertNull(
            new ValidadorTraducaoService(LoreAtivaFake.vazia())
                .repararTrocaDeEntidade(orig, trad, Set.of(List.of("Argama", "Nahel Argama"))),
            () -> "as duas falam da Nahel Argama, mas a quebra virou veredito de troca."
                + System.lineSeparator() + "  original:  " + orig
                + System.lineSeparator() + "  traduzido: " + trad);
    }

    /** Junta as palavras por espaço, exceto a junta escolhida, que recebe a quebra do ASS. */
    private static String juntar(List<String> palavras, int junta) {
        List<String> partes = new ArrayList<>();
        for (int i = 0; i < palavras.size(); i++) {
            if (i > 0) {
                partes.add(i - 1 == junta ? QUEBRA : " ");
            }
            partes.add(palavras.get(i));
        }
        return String.join("", partes);
    }
}
