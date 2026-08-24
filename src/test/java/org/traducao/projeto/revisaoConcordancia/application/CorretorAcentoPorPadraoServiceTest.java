package org.traducao.projeto.revisaoConcordancia.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: provar que os padrões curados acertam a fala do acervo e calam onde a
 * palavra sem acento está certa.
 *
 * <h2>Cada caso NEGATIVO aqui é uma fala real que a leitura salvou</h2>
 * A medição de 24/08/2026 rodou sobre 86.147 falas e a amostra foi lida uma a uma. Duas regras
 * foram REFEITAS por causa do que apareceu, e é isso que os testes negativos congelam — sem eles,
 * a próxima "simplificação" reabre o buraco:
 *
 * <pre>
 *   "Entrei em contato com você esta noite"  -> `esta noite` e DEMONSTRATIVO
 *   "Ela desceu para nos salvar"             -> `nos salvar` e pronome obliquo
 *   "Judau, isso e aquilo."                  -> coordenacao, o `e` esta certo
 * </pre>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Reprova nomeando a fala inteira, para quem ler saber o que mudou.
 */
class CorretorAcentoPorPadraoServiceTest {

    private final CorretorAcentoPorPadraoService corretor = new CorretorAcentoPorPadraoService();

    @Test
    @DisplayName("POSITIVO: o `e` depois de demonstrativo vira verbo")
    void demonstrativoMaisE() {
        assertEquals(Optional.of("Essa é uma ótima notícia."),
            corretor.corrigir("Essa e uma ótima notícia."));
        assertEquals(Optional.of("Isso é rápido."), corretor.corrigir("Isso e rápido."));
        assertEquals(Optional.of("Qual é o seu nome?"), corretor.corrigir("Qual e o seu nome?"));
    }

    @Test
    @DisplayName("NEGATIVO: `isso e aquilo` e coordenacao — fica como esta")
    void coordenacaoNaoEVerbo() {
        assertEquals(Optional.empty(), corretor.corrigir("Judau, isso e aquilo."),
            "trocou por verbo uma coordenacao legitima — sao 3 falas assim no acervo");
        assertEquals(Optional.empty(), corretor.corrigir("Isso e aquilo..."));
    }

    @Test
    @DisplayName("POSITIVO: `nao e` e sempre `nao é`")
    void naoMaisE() {
        assertEquals(Optional.of("Não é uma ideia ruim."),
            corretor.corrigir("Não e uma ideia ruim."));
        assertEquals(Optional.of("Isso não é saudável."),
            corretor.corrigir("Isso não e saudável."));
    }

    @Test
    @DisplayName("POSITIVO: `esta` seguido de gerundio, participio ou preposicao e verbo")
    void estaVerbo() {
        assertEquals(Optional.of("Do que você está falando?"),
            corretor.corrigir("Do que você esta falando?"));
        assertEquals(Optional.of("Ela está de serviço de limpeza hoje."),
            corretor.corrigir("Ela esta de serviço de limpeza hoje."));
        assertEquals(Optional.of("Ela está viva?"), corretor.corrigir("Ela esta viva?"));
    }

    /**
     * A fala que refez esta regra. Antes, o padrão era só {@code <pronome> esta} — e casava
     * {@code você esta noite}, onde {@code esta} é o demonstrativo e está CERTO.
     */
    @Test
    @DisplayName("NEGATIVO: `esta noite` e demonstrativo — a fala do acervo que refez a regra")
    void estaDemonstrativoNaoEVerbo() {
        assertEquals(Optional.empty(),
            corretor.corrigir("Entrei em contato com você esta noite para me apresentar."),
            "acentuou o demonstrativo: 'esta noite' esta correto, e a fala e real do acervo");
        assertEquals(Optional.empty(), corretor.corrigir("Ele esta vez nao veio."));
    }

    @Test
    @DisplayName("POSITIVO: `nao ha` e o verbo haver")
    void naoHa() {
        assertEquals(Optional.of("Então não há vitimas."),
            corretor.corrigir("Então não ha vitimas."));
    }

    @Test
    @DisplayName("POSITIVO: `nos` no fim da oracao e o pronome tonico")
    void nosTonico() {
        // A fala tem DOIS defeitos e os dois sao corrigidos na mesma passada — a expectativa
        // errada foi minha, e o teste so ficou honesto depois de eu ler a saida real.
        assertEquals(Optional.of("Este é o fim da linha para nós."),
            corretor.corrigir("Este e o fim da linha para nos."));
        assertEquals(Optional.of("Eles não tem poder contra nós."),
            corretor.corrigir("Eles não tem poder contra nos."));
    }

    /**
     * A segunda fala que refez uma regra. {@code nos} entre preposição e verbo é objeto —
     * <i>"para nos salvar"</i> significa "para salvar A NÓS", e trocar ali estraga fala correta.
     */
    @Test
    @DisplayName("NEGATIVO: `para nos salvar` e pronome obliquo — a outra fala que refez a regra")
    void nosObliquoNaoEtonico() {
        assertEquals(Optional.empty(),
            corretor.corrigir("Ela desceu ao mundo dos mortais para nos salvar."),
            "trocou o pronome obliquo por tonico: 'para nos salvar' esta correto");
    }

    @Test
    @DisplayName("POSITIVO: `so` antes de palavra portuguesa e adverbio")
    void soAdverbio() {
        assertEquals(Optional.of("Este veículo só tem dois lugares."),
            corretor.corrigir("Este veículo so tem dois lugares."));
    }

    @Test
    @DisplayName("NEGATIVO: `so` do ingles nao e tocado")
    void soDoInglesFicaIntacto() {
        assertEquals(Optional.empty(), corretor.corrigir("I love you so much, baby"),
            "acentuou o 'so' do ingles — o acervo tem letra de musica e residuo em ingles");
    }

    @Test
    @DisplayName("POSITIVO: futuro sem acento, com a caixa preservada")
    void futuro() {
        assertEquals(Optional.of("Isso terá que servir."), corretor.corrigir("Isso tera que servir."));
        assertEquals(Optional.of("Será que ele vem?"), corretor.corrigir("Sera que ele vem?"),
            "perdeu a maiuscula de inicio de frase");
    }

    @Test
    @DisplayName("duas correcoes na MESMA fala, sem deslocar uma a outra")
    void duasNaMesmaFala() {
        Optional<String> r = corretor.corrigir("Isso e serio, e ela esta indo embora.");
        assertTrue(r.isPresent(), "nao corrigiu nada numa fala com dois defeitos");
        assertTrue(r.get().contains("Isso é") && r.get().contains("está indo"),
            "uma correcao deslocou a outra: " + r.get());
    }

    /**
     * {@code ira} e o unico do padrao do futuro com homografo: substantivo (colera) e verbo. O
     * acervo tem 0 do substantivo e 3 do verbo hoje, e o escudo existe para a traducao de amanha.
     */
    @Test
    @DisplayName("`ira` verbo e acentuado; `a ira` substantivo NAO")
    void iraVerboEsubstantivo() {
        assertEquals(Optional.of("Você só irá cometer mais crimes!"),
            corretor.corrigir("Você so ira cometer mais crimes!"));
        assertEquals(Optional.empty(), corretor.corrigir("A ira dele era visível."),
            "acentuou 'ira' substantivo — 'a ira dele' esta correto sem acento");
        assertEquals(Optional.empty(), corretor.corrigir("Ele falou com ira."),
            "acentuou 'ira' depois de preposicao — continua sendo o substantivo");
    }

    /**
     * A catraca {@code CatracaFronteiraQuebraAssTest} reprovou a primeira versão destes padrões,
     * e com razão: <b>24,6% das falas do acervo têm a quebra {@code \\N}</b>, e ela cai no meio
     * da frase. Com {@code \\s+} entre as palavras, {@code "Isso\\Ne tudo"} não casava — o
     * defeito ficava invisível justamente onde a legenda é mais longa.
     */
    @Test
    @DisplayName("a quebra do ASS no meio do padrao nao esconde o defeito")
    void quebraDoAssNaoEscondeODefeito() {
        assertEquals(Optional.of("Isso\\Né tudo."), corretor.corrigir("Isso\\Ne tudo."),
            "a quebra entre o demonstrativo e o verbo escondeu o defeito");
        assertEquals(Optional.of("Você\\Nestá falando demais."),
            corretor.corrigir("Você\\Nesta falando demais."),
            "a quebra antes de 'esta' escondeu o defeito");
        assertEquals(Optional.of("Não\\Nhá tempo."), corretor.corrigir("Não\\Nha tempo."));
    }

    @Test
    @DisplayName("entrada degenerada nao lanca")
    void degenerada() {
        assertEquals(Optional.empty(), corretor.corrigir(null));
        assertEquals(Optional.empty(), corretor.corrigir(""));
        assertEquals(Optional.empty(), corretor.corrigir("   "));
        assertEquals(Optional.empty(), corretor.corrigir("Uma fala perfeitamente correta."));
    }
}
