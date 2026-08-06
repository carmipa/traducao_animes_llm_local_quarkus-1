package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: prova que o cartão de data sai na forma canônica independentemente do
 * que o modelo devolveu — e que nada além de cartão de data é tocado.
 *
 * <h2>Os casos NÃO são inventados</h2>
 * Cada entrada defeituosa abaixo foi lida do cache do 86 em 06/08/2026, quando 17 dos 136
 * cartões do acervo estavam fora do padrão. O defeito era visível na tela.
 */
class NormalizadorCartaoDataServiceTest {

    private final NormalizadorCartaoDataService normalizador = new NormalizadorCartaoDataService();

    /**
     * PROPÓSITO DE NEGÓCIO: a ordem invertida — o caso mais comum, e o que Paulo viu assistindo.
     * <p>COMPORTAMENTO EM CASO DE FALHA: manter "Julho de 30" deixa a data ilegível em português.
     */
    @Test
    @DisplayName("ordem invertida do LLM vira a forma canonica")
    void corrigeOrdemInvertida() {
        assertEquals("30 de julho do Ano Estelar 2149",
            normalizador.normalizar("July 30th, Stellar Year 2149", "Julho de 30, Ano Estelar 2149"));
        assertEquals("12 de março do Ano Estelar 2150",
            normalizador.normalizar("March 12th, Stellar Year 2150", "Março 12º, Ano Estelar 2150"));
        assertEquals("15 de junho do Ano Estelar 2148",
            normalizador.normalizar("June 15th, Stellar Year 2148", "Junho 15, Ano Estelar 2148"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o caso mais grave, porque passava por TODAS as guardas — {@code June}
     * não está na lista de resíduo em inglês e a fala não é idêntica ao original.
     */
    @Test
    @DisplayName("cartao meio em ingles e reescrito por inteiro")
    void corrigeMetadeEmIngles() {
        assertEquals("16 de junho do Ano Estelar 2148",
            normalizador.normalizar("June 16th, Stellar Year 2148", "June 16th, Ano Estelar 2148"));
    }

    /** Caixa perdida na era: cosmético, mas é o que faz camadas divergirem entre si. */
    @Test
    @DisplayName("caixa da era e normalizada")
    void corrigeCaixaDaEra() {
        assertEquals("2 de setembro do Ano Estelar 2149",
            normalizador.normalizar("September 2nd, Stellar Year 2149", "2 de setembro do ano estelar 2149"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o efeito que resolve o borrão na tela. O cartão vem em camadas
     * empilhadas e cada uma ia ao LLM sozinha, voltando diferente. Decidir pelo ORIGINAL faz
     * as três convergirem por construção.
     */
    @Test
    @DisplayName("camadas divergentes do mesmo cartao convergem")
    void camadasConvergem() {
        String en = "January 7th, Stellar Year 2149";
        String a = normalizador.normalizar(en, "7 de janeiro, Ano Estelar 2149");
        String b = normalizador.normalizar(en, "7 de janeiro do Ano Estelar 2149");
        String c = normalizador.normalizar(en, "Janeiro 7, Ano Estelar 2149");

        assertEquals(a, b, "camadas do mesmo cartao continuam divergindo");
        assertEquals(b, c, "camadas do mesmo cartao continuam divergindo");
        assertEquals("7 de janeiro do Ano Estelar 2149", a);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o cartão tem posicionamento e fonte próprios — reescrever a fala
     * inteira apagaria o desenho e a data apareceria no lugar errado da tela.
     */
    @Test
    @DisplayName("as tags de posicionamento do inicio sao preservadas")
    void preservaTagsIniciais() {
        String traduzido = "{=0}{\\an2\\pos(960,930)\\fscx90\\fs80}Julho de 30, Ano Estelar 2149";

        assertEquals("{=0}{\\an2\\pos(960,930)\\fscx90\\fs80}30 de julho do Ano Estelar 2149",
            normalizador.normalizar("{=0}{\\an2\\pos(960,930)}July 30th, Stellar Year 2149", traduzido));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o ano vem do ORIGINAL, dígito a dígito. É a blindagem contra o erro
     * que a guarda de identificador numérico pegou no mesmo arquivo — {@code 2149} virando
     * {@code 2049}, um século de diferença no cartão que abre o episódio.
     */
    @Test
    @DisplayName("ano errado do LLM e substituido pelo do original")
    void anoVemDoOriginal() {
        assertEquals("30 de julho do Ano Estelar 2149",
            normalizador.normalizar("July 30th, Stellar Year 2149", "30 de julho do Ano Estelar 2049"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: contra-teste. Sem ele, "normalizar cartão de data" poderia virar
     * "reescrever qualquer fala" e ninguém notaria até a legenda inteira sair errada.
     */
    @Test
    @DisplayName("fala que NAO e cartao de data atravessa intacta")
    void naoTocaOutrasFalas() {
        assertEquals("Undertaker, o que você está fazendo?",
            normalizador.normalizar("Undertaker, what are you doing?", "Undertaker, o que você está fazendo?"));
        assertEquals("Vejo você em 30 de julho.",
            normalizador.normalizar("See you on July 30th.", "Vejo você em 30 de julho."));
        assertEquals("Era o ano de 2149.",
            normalizador.normalizar("It was the year 2149.", "Era o ano de 2149."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: era desconhecida passa intacta em vez de ser inventada. Foi assim
     * que "Stellar Year 2139" virou "2139 d.C." no cache do 86 — o modelo trocou a era do
     * universo ficcional por uma do mundo real.
     */
    @Test
    @DisplayName("era fora do mapa fechado nao e traduzida por conta propria")
    void eraDesconhecidaPassaIntacta() {
        assertEquals("13 de maio do Calendário Antigo 2148",
            normalizador.normalizar("May 13th, Ancient Calendar 2148", "13 de maio do Calendário Antigo 2148"));
    }

    /** Entradas degeneradas não podem produzir fala vazia — viés de preservação. */
    @Test
    @DisplayName("entrada nula ou vazia devolve a traducao como veio")
    void entradaDegenerada() {
        assertEquals("qualquer coisa", normalizador.normalizar(null, "qualquer coisa"));
        assertEquals("", normalizador.normalizar("July 30th, Stellar Year 2149", ""));
        assertEquals(null, normalizador.normalizar("July 30th, Stellar Year 2149", null));
    }
}
