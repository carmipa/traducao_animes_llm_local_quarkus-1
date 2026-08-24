package org.traducao.projeto.core.texto.dicionarioOrtografia;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: provar as duas propriedades do aquecimento em lote — que ele é
 * <b>gratuito em resultado</b> e <b>decisivo em tempo</b>.
 *
 * <h2>A cicatriz que originou o mecanismo</h2>
 * Em 24/08/2026 o relógio por elo da tela 3.3 mostrou que o dicionário consumia 283 dos 291
 * segundos de uma passada por seis episódios — 97% do tempo — e corrigia ZERO falas. A memória
 * evitava a segunda pergunta sobre cada palavra; nunca evitou a primeira, e é a primeira que
 * custa um processo externo inteiro.
 *
 * <h2>Por que a propriedade "não muda o resultado" vem primeiro</h2>
 * Otimização que muda a saída não é otimização, é bug com desculpa. O caso do resultado idêntico
 * é o portão: se ele cair, o número do relógio não interessa.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Dicionário fora do ar termina como NÃO VERIFICADO (teste pulado, e pular não é aprovar).
 */
class AquecimentoDoDicionarioTest {

    /** Falas com defeito de acento de verdade, e vocabulário que não se repete entre elas. */
    private static final List<String> FALAS = List.of(
        "Chegamos a borda do territorio inimigo.",
        "Os reforcos vao demorar mais do que o previsto.",
        "Levante as maos e nao se mova, soldado.",
        "A comunicacao com a base esta interrompida.",
        "Precisamos de uma decisao rapida sobre a operacao.",
        "O piloto perdeu a consciencia durante a manobra.",
        "A explosao destruiu tres modulos da colonia.",
        "Nenhuma informacao nova chegou pela manha.");

    @Test
    @DisplayName("PORTAO: aquecer NAO muda uma virgula do resultado")
    void resultadoIdenticoComEsemAquecimento() {
        CorretorOrtograficoLegenda frio = new CorretorOrtograficoLegenda();
        CorretorOrtograficoLegenda quente = new CorretorOrtograficoLegenda();

        List<String> semAquecer = FALAS.stream().map(f -> frio.corrigir(f, Set.of())).toList();
        Assumptions.assumeTrue(frio.disponivel(),
            "dicionario fora do ar — NAO VERIFICADO, e nao verificar nao e aprovar");

        quente.aquecerComTextos(FALAS);
        List<String> comAquecimento = FALAS.stream().map(f -> quente.corrigir(f, Set.of())).toList();

        assertEquals(semAquecer, comAquecimento,
            "o aquecimento mudou a legenda — otimizacao que muda a saida e bug com desculpa");
        assertTrue(semAquecer.stream().anyMatch(f -> f.contains("território")),
            "o experimento nao tinha defeito nenhum para corrigir: comparar duas listas iguais a "
                + "entrada nao prova nada. Saida: " + semAquecer);
    }

    /**
     * O aquecimento faz UMA consulta externa; sem ele, cada fala com palavra inédita faz a sua.
     * O contador de não-verificadas não serve aqui, então a prova é o relógio — e o limiar é
     * folgado de propósito: o que se afirma é a ordem de grandeza, não um número exato de máquina.
     */
    @Test
    @DisplayName("o aquecimento derruba o tempo da passada em ordem de grandeza")
    void tempoDespenca() {
        CorretorOrtograficoLegenda frio = new CorretorOrtograficoLegenda();
        long comecoFrio = System.nanoTime();
        FALAS.forEach(f -> frio.corrigir(f, Set.of()));
        long nanosFrio = System.nanoTime() - comecoFrio;
        Assumptions.assumeTrue(frio.disponivel(),
            "dicionario fora do ar — NAO VERIFICADO, e nao verificar nao e aprovar");

        CorretorOrtograficoLegenda quente = new CorretorOrtograficoLegenda();
        quente.aquecerComTextos(FALAS);
        long comecoQuente = System.nanoTime();
        FALAS.forEach(f -> quente.corrigir(f, Set.of()));
        long nanosQuente = System.nanoTime() - comecoQuente;

        System.out.printf("  frio: %.0f ms para %d falas · ja aquecido: %.0f ms%n",
            nanosFrio / 1e6, FALAS.size(), nanosQuente / 1e6);
        assertTrue(nanosQuente * 5 < nanosFrio,
            String.format("depois de aquecido a passada tinha de ser MUITO mais barata, e foi "
                + "%.0f ms contra %.0f ms — o aquecimento nao esta pegando as mesmas palavras "
                + "que a correcao pergunta", nanosQuente / 1e6, nanosFrio / 1e6));
    }

    @Test
    @DisplayName("aquecer duas vezes nao pergunta de novo, e entrada degenerada nao lanca")
    void degenerada() {
        CorretorOrtograficoLegenda c = new CorretorOrtograficoLegenda();
        assertTrue(c.aquecerComTextos(List.of()), "lista vazia nao tem o que perguntar");
        assertTrue(c.aquecerComTextos(null), "nulo nao pode lancar");
        c.aquecerComTextos(FALAS);
        Assumptions.assumeTrue(c.disponivel(),
            "dicionario fora do ar — NAO VERIFICADO, e nao verificar nao e aprovar");

        long comeco = System.nanoTime();
        assertTrue(c.aquecerComTextos(FALAS), "o segundo aquecimento reprovou");
        long nanos = System.nanoTime() - comeco;
        assertTrue(nanos < 200_000_000L,
            String.format("o segundo aquecimento levou %.0f ms: perguntou tudo de novo em vez de "
                + "achar na memoria", nanos / 1e6));
    }
}
