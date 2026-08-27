package org.traducao.projeto.revisaoLore.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.qualidadeTraducao.application.EnforcadorTermosLore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: quando o ORIGINAL em inglês usa a própria "forma-ruim", ela não é
 * forma-ruim naquela fala — é a palavra que o roteiro escolheu, e trocá-la apaga a cena.
 *
 * <h2>O prejuízo, medido em 27/08/2026</h2>
 * O corretor determinístico da tela 3.2 ia reescrever uma fala do Zeta Gundam assim:
 *
 * <pre>
 *   EN      I'm Rosamia! Not Rosammy!
 *   ANTES   Eu sou Rosamia! Não Rosammy!
 *   DEPOIS  Eu sou Rosammy! Não Rosammy!
 * </pre>
 *
 * A personagem está insistindo no PRÓPRIO nome contra o apelido que lhe dão. O contraste entre os
 * dois nomes <b>é</b> a fala; achatá-los deixa "Eu sou X! Não X!", que não quer dizer nada.
 *
 * <h2>O documento e o mapa diziam coisas opostas</h2>
 * O {@code lore.yaml} trazia {@code Rosamia: Rosammy} no mapa de terminologia e, na prosa da MESMA
 * lore, o contrário:
 *
 * <blockquote>"Rosammy e o apelido usado por Kamille para Rosamia. Preserve exatamente Rosammy
 * quando o original usar Rosammy; nao normalize o apelido para Rosamia."</blockquote>
 *
 * Quem escreve na legenda é o mapa, não a prosa. As duas entradas foram removidas — {@code Rosamia}
 * nunca foi forma-ruim de coisa alguma.
 *
 * <h2>Por que o teste não fica só na Rosamia</h2>
 * Remover a entrada conserta ESTE caso. A guarda conserta a CLASSE, e ela sai do significado do
 * mapa: forma-ruim é o que o LLM traduziu errado em PORTUGUÊS ({@code "Robô Móvel"} por
 * {@code "Mobile Suit"}) — não tem por que aparecer no inglês. Quando aparece, a entrada não
 * descreve aquela fala. São 5.800 entradas; auditar na mão não é opção.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Se a guarda cair, o primeiro caso abaixo mostra a fala achatada lado a lado com a original.
 */
@DisplayName("corretor de lore: contraste de nomes no ORIGINAL nao e forma-ruim")
class ContrasteDeNomesNaoEhFormaRuimTest {

    private final CorretorLoreDeterministico corretor =
        new CorretorLoreDeterministico(new EnforcadorTermosLore());

    private static Map<String, String> mapa(String formaRuim, String canonico) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(formaRuim, canonico);
        return m;
    }

    /**
     * O CASO DOENTE, com o texto REAL do acervo. Mesmo que a entrada volte ao {@code lore.yaml}
     * por engano, a guarda tem de segurar.
     */
    @Test
    @DisplayName("CASO REAL: 'I'm Rosamia! Not Rosammy!' nao vira 'Eu sou Rosammy! Nao Rosammy!'")
    void naoAchataOsDoisNomesDaFalaDoZeta() {
        String original = "I'm Rosamia! Not Rosammy!";
        String traducao = "Eu sou Rosamia! Não Rosammy!";

        Optional<String> saida = corretor.corrigir(original, traducao, mapa("Rosamia", "Rosammy"));

        assertTrue(saida.isEmpty(), () -> """
            O CORRETOR ACHATOU O CONTRASTE DE NOMES.

              EN      %s
              ANTES   %s
              DEPOIS  %s

            A personagem esta insistindo no proprio nome contra o apelido. O original em ingles
            usa OS DOIS de proposito — quando a forma-ruim aparece no EN, a entrada do mapa nao
            descreve aquela fala.""".formatted(original, traducao, saida.orElse("(vazio)")));
    }

    /**
     * CONTROLE NEGATIVO — sem ele a guarda poderia ser "nunca corrige nada" e passaria igual.
     * Aqui o EN NÃO usa a forma-ruim, então a restauração tem de acontecer.
     */
    @Test
    @DisplayName("CONTROLE: quando o EN nao usa a forma-ruim, o corretor RESTAURA normalmente")
    void continuaRestaurandoQuandoOoriginalNaoUsaAformaRuim() {
        String original = "The Mobile Suit is ready.";
        String traducao = "O Robô Móvel está pronto.";

        Optional<String> saida = corretor.corrigir(original, traducao,
            mapa("Robô Móvel", "Mobile Suit"));

        assertTrue(saida.isPresent(),
            "a guarda desligou o corretor: sem restauracao nenhuma ela nao protege, atrapalha");
        assertEquals("O Mobile Suit está pronto.", saida.get(),
            "o canonico tem de entrar quando o EN NAO usa a forma-ruim");
    }

    /**
     * A guarda filtra por ENTRADA, não pela fala inteira: uma fala com duas correções, uma delas
     * em contraste, mantém a outra. Sem isto, um único contraste desligaria o corretor na linha
     * toda — e o remédio seria pior que a doença.
     */
    @Test
    @DisplayName("a guarda filtra por ENTRADA: a outra correcao da mesma fala continua valendo")
    void filtraPorEntradaEnaoPelaFalaInteira() {
        String original = "Rosamia's Mobile Suit is fast.";
        String traducao = "O Robô Móvel de Rosamia é rápido.";
        Map<String, String> m = mapa("Robô Móvel", "Mobile Suit");
        m.put("Rosamia", "Rosammy");

        Optional<String> saida = corretor.corrigir(original, traducao, m);

        assertTrue(saida.isPresent(), "a correcao legitima da mesma fala foi perdida junto");
        assertTrue(saida.get().contains("Mobile Suit"),
            "o termo que o EN NAO usa como forma-ruim tinha de ser restaurado: " + saida.get());
        assertTrue(saida.get().contains("Rosamia"),
            "o nome que o EN usa de proposito nao pode ter sido trocado: " + saida.get());
    }
}
