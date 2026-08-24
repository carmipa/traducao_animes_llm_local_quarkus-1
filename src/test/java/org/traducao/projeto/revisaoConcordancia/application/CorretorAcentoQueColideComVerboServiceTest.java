package org.traducao.projeto.revisaoConcordancia.application;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.core.texto.gramatica.AchadoGramatical;
import org.traducao.projeto.core.texto.gramatica.LanguageToolRevisorAdapter;
import org.traducao.projeto.core.texto.gramatica.RevisorGramaticalPort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: provar que a reposição de acento na 3.3 corrige o substantivo, deixa o
 * verbo em paz, e não estraga a marcação do ASS no caminho.
 *
 * <h2>Por que cada caso está aqui</h2>
 * Cada teste congela uma decisão tomada em 23/08/2026, e nenhuma delas sobrevive sozinha:
 *
 * <ul>
 *   <li><b>O par positivo/negativo é o coração.</b> A MESMA palavra tem de ser corrigida depois de
 *       artigo e ignorada depois de sujeito. Uma lista de palavras faria as duas iguais, e foi
 *       para não fazer isso que se trouxe um POS tagger.</li>
 *   <li><b>Tag e quebra</b> — o texto que o revisor vê não é o texto que se grava. As posições só
 *       batem porque tag e {@code \\N} viram espaço em vez de sumir. Se alguém "simplificar" isso
 *       para um replace por nome de palavra, estes testes caem.</li>
 *   <li><b>Caixa</b> — nome próprio é lore, e a decisão de Paulo para esta tela é que lore não
 *       entra. São DUAS condições diferentes fazendo isso, descobertas por mutação: a sugestão
 *       tem de ser minúscula, e a comparação de esqueleto é sensível a caixa. Uma terceira
 *       guarda existia e era código morto — removida em 23/08/2026.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem motor gramatical os casos declaram NÃO VERIFICADO em vez de passar por ausência.
 */
class CorretorAcentoQueColideComVerboServiceTest {

    private static CorretorAcentoQueColideComVerboService corretor;
    private static boolean motorVivo;

    @BeforeAll
    static void montar() {
        LanguageToolRevisorAdapter adapter = new LanguageToolRevisorAdapter();
        motorVivo = adapter.disponivel();
        corretor = new CorretorAcentoQueColideComVerboService(adapter);
    }

    @Test
    @DisplayName("POSITIVO: o substantivo depois de artigo recebe o acento")
    void acentuaSubstantivo() {
        assumirMotor();
        Optional<String> r = corretor.corrigir("A milicia ordenou um blackout de noticias.");
        assertTrue(r.isPresent(), "nao corrigiu a classe que motivou o corretor inteiro");
        assertTrue(r.get().contains("milícia") || r.get().contains("notícias"),
            "corrigiu alguma coisa, mas nao o acento esperado: " + r.get());
    }

    @Test
    @DisplayName("NEGATIVO: a MESMA palavra como verbo fica intacta")
    void naoTocaNoVerbo() {
        assumirMotor();
        assertEquals(Optional.empty(),
            corretor.corrigir("O reporter noticia o caso todo dia."),
            "acentuou 'noticia' onde ela e VERBO. Um corretor por lista faria este estrago, e "
            + "evitar isso foi a razao de trazer um POS tagger.");
    }

    @Test
    @DisplayName("a tag de override sobrevive byte a byte")
    void preservaTag() {
        assumirMotor();
        String entrada = "{\\i1}A milicia{\\i0} ordenou um blackout.";
        Optional<String> r = corretor.corrigir(entrada);
        if (r.isEmpty()) {
            return;   // sem correcao nesta fala, nada a conferir aqui
        }
        assertTrue(r.get().startsWith("{\\i1}") && r.get().contains("{\\i0}"),
            "a tag foi corrompida: " + r.get());
        assertFalse(r.get().contains("  "),
            "sobrou o espaco que substituiu a tag na visao do revisor — o texto GRAVADO tem de "
            + "ser o original com a palavra trocada, e nada mais: " + r.get());
    }

    @Test
    @DisplayName("a quebra \\N sobrevive e nao vira espaco no arquivo")
    void preservaQuebra() {
        assumirMotor();
        String entrada = "A milicia ordenou\\Num blackout de noticias.";
        Optional<String> r = corretor.corrigir(entrada);
        if (r.isEmpty()) {
            return;
        }
        assertTrue(r.get().contains("\\N"),
            "a quebra do ASS sumiu — a legenda perderia a divisao de linha: " + r.get());
    }

    @Test
    @DisplayName("palavra com maiuscula nao e tocada: nome proprio e lore")
    void naoTocaEmMaiuscula() {
        assertNull(CorretorAcentoQueColideComVerboService.propostaSoDeAcento(
            new AchadoGramatical("R", "CONFUSED_WORDS", 0, 6, "Muller",
                "msg", List.of("Müller"))),
            "aceitou acentuar palavra com maiuscula — isso e renomear pessoa");
    }

    /**
     * O caso acima e este cobrem coisas DIFERENTES, e so a mutacao mostrou isso.
     *
     * <p>Havia uma terceira guarda no corretor — "se o trecho tem maiuscula, desista" — e ela era
     * <b>codigo morto</b>: desligada, nenhum caso mudava de resultado. Foi removida. O que
     * realmente protege sao duas condicoes distintas, e cada mutacao derruba um teste diferente:
     *
     * <pre>
     *   {@code Muller  -> Müller }   barra porque a SUGESTAO nao e minuscula   -> naoTocaEmMaiuscula
     *   {@code Milicia -> milícia}   barra porque semAcento() compara COM caixa -> este teste
     * </pre>
     *
     * <p>O dano que este caso impede nao e acento: e <b>rebaixar a inicial de uma frase</b>. A
     * palavra ganharia o acento e perderia a maiuscula, num texto que ninguem pediu para
     * minusculizar.
     */
    @Test
    @DisplayName("caixa preservada: nao REBAIXA a inicial de frase para acentuar")
    void naoRebaixaInicialDeFrase() {
        assertNull(CorretorAcentoQueColideComVerboService.propostaSoDeAcento(
            new AchadoGramatical("R", "CONFUSED_WORDS", 0, 7, "Milicia",
                "msg", List.of("milícia"))),
            "aceitou trocar 'Milicia' por 'milícia' — a palavra ganharia o acento e PERDERIA a "
            + "maiuscula de inicio de frase, num texto que ninguem pediu para minusculizar");
    }

    /**
     * O DEFEITO QUE A MEDICAO NO ACERVO ENCONTROU (23/08/2026), e ele estragava fala CORRETA.
     *
     * <p>O revisor acusa trechos de mais de uma palavra. No Guilty Crown ep02 ele propos
     * {@code "nós será"} -> {@code "nos será"}: o {@code será} carregava o acento que fazia a
     * regra velha ("a sugestao tem algum acento?") passar, e o {@code nós} — <b>que ja estava
     * certo no arquivo</b> — perdia o dele. O arquivo estava em NFC: nao era normalizacao
     * Unicode, era perda de acento mesmo.
     */
    @Test
    @DisplayName("NUNCA remove acento que ja existia, nem em trecho de varias palavras")
    void nuncaRemoveAcento() {
        assertNull(CorretorAcentoQueColideComVerboService.propostaSoDeAcento(
            new AchadoGramatical("R", "CONFUSED_WORDS", 0, 8, "nós será",
                "msg", List.of("nos será"))),
            "aceitou tirar o acento de 'nós' porque 'será' mantinha o dele — a fala do acervo "
            + "estava CERTA e viraria errada");
        assertFalse(CorretorAcentoQueColideComVerboService.soAcrescentaAcento("nós", "nos"),
            "aceitou remover acento");
        assertTrue(CorretorAcentoQueColideComVerboService.soAcrescentaAcento("nos", "nós"),
            "recusou o caso legitimo: acrescentar acento");
        assertFalse(CorretorAcentoQueColideComVerboService.soAcrescentaAcento("avo", "avos"),
            "aceitou mudar o comprimento da palavra");
        assertFalse(CorretorAcentoQueColideComVerboService.soAcrescentaAcento("avo", "ave"),
            "aceitou TROCAR uma letra por outra que nao e a mesma acentuada");
        // Este isola a unica condicao que sobrou: a sugestao GANHA acento (passaria por qualquer
        // teste de "acentuou?"), mas a letra e OUTRA. So a comparacao "sem acento, da o caractere
        // original" recusa isto — e foi a mutacao que mostrou que os outros casos nao a exercitavam.
        assertFalse(CorretorAcentoQueColideComVerboService.soAcrescentaAcento("avo", "avé"),
            "aceitou trocar 'o' por 'é': ganhou acento, mas nao e a mesma letra");
    }

    /**
     * Duas propostas viaveis e AMBIGUIDADE, e a medicao mostrou o custo de escolher a primeira:
     * para {@code avo} o revisor oferece {@code avó} e {@code avô}, e o corretor produziu
     * <i>"Meu avó queria consertar"</i> e <i>"os ideais do seu avó Meitzer"</i> — dois homens
     * virando avo mulher. Decidir entre elas exige saber de QUEM se fala, que e lore.
     */
    @Test
    @DisplayName("duas acentuacoes possiveis: nao mexe (o caso avó/avô)")
    void ambiguidadeNaoEscolhe() {
        assertNull(CorretorAcentoQueColideComVerboService.propostaSoDeAcento(
            new AchadoGramatical("R", "CONFUSED_WORDS", 0, 3, "avo",
                "msg", List.of("avó", "avô"))),
            "escolheu uma das duas acentuacoes possiveis — isso e decidir o sexo do personagem "
            + "por sorteio");
        assertEquals("órbita", CorretorAcentoQueColideComVerboService.propostaSoDeAcento(
            new AchadoGramatical("R", "CONFUSED_WORDS", 0, 6, "orbita",
                "msg", List.of("órbita"))),
            "com UMA proposta viavel tem de corrigir normalmente");
    }

    @Test
    @DisplayName("proposta que TROCA a palavra e recusada; so acentuar e aceito")
    void soAceitaAcentuacao() {
        assertEquals("notícia", CorretorAcentoQueColideComVerboService.propostaSoDeAcento(
            new AchadoGramatical("R", "CONFUSED_WORDS", 0, 7, "noticia",
                "msg", List.of("noticia", "noticiar", "notícia"))),
            "devia escolher a acentuacao e ignorar 'noticiar', que TROCA a palavra");
        assertNull(CorretorAcentoQueColideComVerboService.propostaSoDeAcento(
            new AchadoGramatical("R", "CONFUSED_WORDS", 0, 7, "noticia",
                "msg", List.of("noticiar", "noticiario"))),
            "aceitou trocar a palavra em vez de acentuar");
        assertNull(CorretorAcentoQueColideComVerboService.propostaSoDeAcento(
            new AchadoGramatical("R", "CONFUSED_WORDS", 0, 7, "noticia",
                "msg", List.of())),
            "achado sem sugestao nao pode virar correcao");
    }

    @Test
    @DisplayName("o texto visivel tem o MESMO comprimento do original")
    void visivelPreservaComprimento() {
        String entrada = "{\\an8}A milicia\\Nordenou{\\i1} tudo{\\i0}.";
        String visivel = CorretorAcentoQueColideComVerboService.visivelPreservandoPosicao(entrada);
        assertEquals(entrada.length(), visivel.length(),
            "o comprimento mudou — as posicoes que o revisor devolve deixariam de valer no "
            + "texto real, e a correcao cairia no lugar errado");
        assertFalse(visivel.contains("{"), "sobrou tag na visao do revisor: " + visivel);
        assertFalse(visivel.contains("\\N"), "sobrou a quebra na visao do revisor: " + visivel);
        assertTrue(visivel.contains("milicia"), "a palavra sumiu junto com a tag: " + visivel);
    }

    @Test
    @DisplayName("revisor indisponivel devolve vazio e DECLARA o motivo")
    void revisorIndisponivel() {
        var mudo = new RevisorGramaticalPort() {
            @Override public List<AchadoGramatical> revisar(String texto) {
                return List.of();
            }

            @Override public boolean disponivel() {
                return false;
            }

            @Override public String motivoDaIndisponibilidade() {
                return "motor de teste desligado";
            }
        };
        var semRevisor = new CorretorAcentoQueColideComVerboService(mudo);
        assertEquals(Optional.empty(), semRevisor.corrigir("A milicia ordenou tudo."));
        assertFalse(semRevisor.disponivel());
        assertEquals("motor de teste desligado", semRevisor.motivoDaIndisponibilidade(),
            "sem o motivo, a tela mostraria zero correcoes com a mesma cara de 'esta tudo limpo'");
    }

    /**
     * A CRASE nao se aplica sozinha, e a medicao no acervo diz por que: a MESMA regra
     * {@code CRASE_CONFUSION} acertou <i>"assistir à batalha"</i> e errou <i>"Como vão às
     * coisas"</i> — onde "as coisas" e SUJEITO e nao admite crase. Crase errada nao parece erro
     * na releitura, e por isso a tela prefere deixar faltando.
     */
    @Test
    @DisplayName("CRASE nao e aplicada sozinha, nem quando o revisor tem razao")
    void naoAplicaCrase() {
        assumirMotor();
        assertEquals(Optional.empty(), corretor.corrigir("Como vão as coisas contigo?"),
            "aplicou crase onde 'as coisas' e SUJEITO — o revisor errou, e a tela escreveu");
        assertEquals(Optional.empty(),
            corretor.corrigir("No minimo, vamos assistir a batalha deles."),
            "aplicou crase CERTA, e isto tambem reprova: a familia inteira fica de fora enquanto "
            + "ninguem le, porque a mesma regra produz as duas coisas");
    }

    @Test
    @DisplayName("entrada degenerada nao lanca")
    void entradaDegenerada() {
        assertEquals(Optional.empty(), corretor.corrigir(null));
        assertEquals(Optional.empty(), corretor.corrigir(""));
        assertEquals(Optional.empty(), corretor.corrigir("   "));
    }

    private static void assumirMotor() {
        assertTrue(motorVivo, "NAO VERIFICADO: motor gramatical ausente — "
            + corretor.motivoDaIndisponibilidade());
    }
}
