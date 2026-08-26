package org.traducao.projeto.core.texto.dicionarioOrtografia;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * PROPÓSITO DE NEGÓCIO: o espanhol vazado ganha rótulo próprio — e a correção de acento do
 * português NÃO pode ser desligada por causa disso.
 *
 * <h2>O prejuízo, medido em 26/08/2026</h2>
 * A medição do alcance da tela 3.3 sobre 85.503 falas encontrou <b>54 ocorrências</b> de espanhol
 * na tradução — {@code misil}, {@code pasaje}, {@code francotirador}, {@code desplegada},
 * {@code matanza}. Todas saíam como {@link VeredictoPalavra#DESCONHECIDA}, no MESMO balde de
 * {@code psycommu} (termo da franquia) e {@code Kitchman} (nome de personagem). Três coisas
 * diferentes com o mesmo rótulo não permitem decisão nenhuma.
 *
 * <p>A proveniência responde de onde vem: {@code aya-expanse-8b} traduziu as 22 obras, e o
 * vazamento aparece em todas, na mesma ordem de grandeza. Não é uma execução ruim — é o modelo
 * derivando para a língua vizinha.
 *
 * <h2>O RISCO que este arquivo existe para congelar</h2>
 * O espanhol aceita {@code territorio}, {@code capitulo} e {@code servicio}, que em português são
 * a mesma palavra <b>sem o acento</b>. Se o espanhol fosse consultado antes da checagem de acento,
 * {@code territorio} sairia como {@code TERMO_ESPANHOL} e o corretor pararia de escrever
 * {@code território} — trocando um defeito de 54 falas por um de centenas.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Dicionário ausente termina como NÃO VERIFICADO, e pular não é aprovar.
 */
class EspanholVazadoTest {

    private static CorretorOrtograficoLegenda corretor;
    private static boolean dePe;

    @BeforeAll
    static void montar() {
        corretor = new CorretorOrtograficoLegenda();
        corretor.corrigir("Uma sonda qualquer para acordar o dicionario.");
        dePe = corretor.disponivel();
    }

    private static Map<String, VeredictoPalavra> vereditos(String... palavras) {
        return corretor.classificar(new LinkedHashSet<>(List.of(palavras)));
    }

    @Test
    @DisplayName("as palavras reais do acervo saem como TERMO_ESPANHOL, e nao como DESCONHECIDA")
    void espanholGanhaRotulo() {
        Assumptions.assumeTrue(dePe, "dicionario fora do ar — NAO VERIFICADO, e pular nao e aprovar");
        Map<String, VeredictoPalavra> v = vereditos(
            "misil", "pasaje", "francotirador", "desplegada", "matanza", "secuestrar",
            // `servicio` entrou aqui depois de o PORTAO abaixo me corrigir: o portugues dele e
            // `servico` com cedilha, entao ele nunca foi "portugues sem acento".
            "servicio");
        for (String p : v.keySet()) {
            assertEquals(VeredictoPalavra.TERMO_ESPANHOL, v.get(p),
                "'" + p + "' e espanhol de verdade e saiu como " + v.get(p)
                    + " — sem rotulo proprio ele volta para o balde de nome de personagem");
        }
    }

    /**
     * O CASO QUE CONGELA A ORDEM. Se alguém mover a consulta do espanhol para antes da checagem
     * de acento, este caso reprova — e o motivo está na mensagem, para não ser "consertado".
     */
    @Test
    @DisplayName("PORTAO: palavra portuguesa SEM ACENTO nao vira espanhol — a ordem esta congelada")
    void acentoVemAntesDoEspanhol() {
        Assumptions.assumeTrue(dePe, "dicionario fora do ar — NAO VERIFICADO");
        Map<String, VeredictoPalavra> v =
            vereditos("territorio", "capitulo", "servicio", "necessario", "opiniao");

        // Estas o espanhol aceita E sao portugues sem acento — nenhuma pode sair rotulada
        // espanhol, senao o corretor de acento para de trabalhar nelas.
        //
        // `servicio` NAO esta aqui, e o teste me corrigiu pela segunda vez: o portugues dele e
        // `servico` com CEDILHA, nao com acento. Ele e espanhol de verdade, e o corretor de
        // acento nunca o corrigiria de qualquer forma. Poe-lo aqui seria exigir que o
        // classificador mentisse.
        for (String p : List.of("territorio", "capitulo", "necessario", "opiniao")) {
            assertNotEquals(VeredictoPalavra.TERMO_ESPANHOL, v.get(p),
                "'" + p + "' foi rotulado espanhol. O espanhol tem de ser consultado DEPOIS do "
                    + "acento: senao o corretor para de escrever essas palavras com acento, e "
                    + "troca 54 falas ruins por centenas.");
        }

        // E as que sao portugues SEM ACENTO tem de sair como ACENTO_FALTANDO, para continuarem
        // sendo corrigidas. `capitulo` fica FORA desta lista de proposito: ele e portugues
        // legitimo — o verbo capitular na primeira pessoa, "eu capitulo" — e o teste me corrigiu,
        // nao ao codigo. Exigir ACENTO_FALTANDO dele seria congelar um erro meu.
        for (String p : List.of("territorio", "necessario", "opiniao")) {
            assertEquals(VeredictoPalavra.ACENTO_FALTANDO, v.get(p),
                "'" + p + "' tinha de sair como ACENTO_FALTANDO e saiu " + v.get(p));
        }
    }

    @Test
    @DisplayName("o que ja era portugues, ingles ou desconhecido NAO muda de rotulo")
    void osOutrosRotulosSeguemIguais() {
        Assumptions.assumeTrue(dePe, "dicionario fora do ar — NAO VERIFICADO");
        Map<String, VeredictoPalavra> v = vereditos("batalha", "menino", "esmagrado", "psycommu");
        assertEquals(VeredictoPalavra.PORTUGUES_OK, v.get("batalha"),
            "palavra portuguesa comum mudou de rotulo por causa do espanhol");
        assertEquals(VeredictoPalavra.PORTUGUES_OK, v.get("menino"),
            "'menino' existe nas DUAS linguas e o portugues tem de ganhar — ele e perguntado antes");
        assertNotEquals(VeredictoPalavra.TERMO_ESPANHOL, v.get("esmagrado"),
            "palavra QUEBRADA virou espanhol: o dicionario espanhol nao pode servir de desculpa");
        assertNotEquals(VeredictoPalavra.TERMO_ESPANHOL, v.get("psycommu"),
            "termo da franquia virou espanhol");
    }

    /**
     * {@code estibor} aparece 4 vezes no acervo e NÃO é espanhol correto — a palavra é
     * {@code estribor}. O rótulo tem de continuar DESCONHECIDA, senão o relatório diria "isto é
     * espanhol" sobre uma palavra que nenhuma língua reconhece.
     */
    @Test
    @DisplayName("palavra que NENHUMA lingua aceita continua DESCONHECIDA — 'estibor' do acervo")
    void oQueNinguemAceitaSegueDesconhecida() {
        Assumptions.assumeTrue(dePe, "dicionario fora do ar — NAO VERIFICADO");
        assertEquals(VeredictoPalavra.DESCONHECIDA, vereditos("estibor").get("estibor"),
            "'estibor' nao e espanhol (o certo e 'estribor') e nem portugues — chamar de espanhol "
                + "seria dar nome a um defeito que continua sendo palavra inventada");
    }
}
