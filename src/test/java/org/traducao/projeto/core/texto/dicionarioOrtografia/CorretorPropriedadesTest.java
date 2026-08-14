package org.traducao.projeto.core.texto.dicionarioOrtografia;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: cobre as classes de teste que os casos-exemplo NÃO alcançam — propriedades
 * que valem para QUALQUER entrada, concorrência, e as bordas que aparecem em legenda real.
 *
 * <h2>Por que estes testes, e não mais exemplos</h2>
 * Os testes existentes provam casos conhecidos: {@code fatidico} vira {@code fatídico},
 * {@code gonna} não é tocado. Isso não diz nada sobre a fala nº 5.644 de uma obra nova. As
 * propriedades abaixo valem para toda entrada e por isso pegam o que exemplo nenhum pegaria.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem hunspell, PULA por {@link Assumptions}.
 */
@DisplayName("corretor: propriedades, concorrência e bordas")
class CorretorPropriedadesTest {

    private static CorretorOrtograficoLegenda novo() {
        return new CorretorOrtograficoLegenda();
    }

    private static void exigeDicionario(CorretorOrtograficoLegenda c) {
        c.corrigir("organizacao");
        Assumptions.assumeTrue(c.disponivel(), "hunspell ausente — NÃO VERIFICADO");
    }

    /**
     * IDEMPOTÊNCIA: corrigir o já corrigido não muda mais nada. Sem isto, rodar a revisão duas
     * vezes — que é o que qualquer operador faz quando a tela não confirma — produziria texto
     * diferente a cada passada.
     */
    @Test
    @DisplayName("propriedade: corrigir é IDEMPOTENTE")
    void corrigirDuasVezesDaOmesmo() {
        var c = novo();
        exigeDicionario(c);
        for (String fala : new String[] {
                "A situacao da colonia e critica.", "Um evento fatidico no espaco aereo.",
                "{\\i1}A organizacao{\\i0} e\\Nnecessaria.", "voce nao viu o psycommu?"}) {
            String uma = c.corrigir(fala);
            assertEquals(uma, c.corrigir(uma),
                "NÃO É IDEMPOTENTE: a segunda passada mudou '" + uma + "'. Rodar a revisão duas "
                    + "vezes passaria a produzir textos diferentes.");
        }
    }

    /**
     * O corretor NUNCA pode encurtar o texto visível. Ele repõe acento — troca caractere por
     * caractere acentuado —, então perder conteúdo significa que alguma substituição comeu texto.
     */
    @Test
    @DisplayName("propriedade: nunca PERDE palavra")
    void nuncaPerdePalavra() {
        var c = novo();
        exigeDicionario(c);
        for (String fala : new String[] {
                "A situacao da colonia e critica hoje.", "Nao vamos permitir a destruicao total.",
                "O piloto e o mecanico decidiram a operacao."}) {
            int antes = fala.replaceAll("\\{[^}]*}", "").trim().split("\\s+").length;
            int depois = c.corrigir(fala).replaceAll("\\{[^}]*}", "").trim().split("\\s+").length;
            assertEquals(antes, depois, "contagem de palavras mudou em: " + fala);
        }
    }

    /**
     * FUZZING dirigido: entradas geradas ao acaso a partir de um alfabeto de legenda real. Não
     * verifica correção — verifica que NADA explode e que a estrutura sobrevive. É o teste que
     * pega o caractere que ninguém pensou em escrever num exemplo.
     */
    @Test
    @DisplayName("fuzzing: 500 entradas aleatórias não quebram nem comem tag")
    void fuzzingNaoQuebra() {
        var c = novo();
        exigeDicionario(c);
        // Semente FIXA: teste aleatório que não reproduz é teste que não serve para depurar.
        Random r = new Random(20260813L);
        String[] pecas = {"nao", "voce", "situacao", "{\\i1}", "{\\pos(10,20)}", "\\N", "é", "ção",
            "kimi", "gonna", "psycommu", "  ", "...", "?!", "—", "\"", "'", "123", "ç", "ã"};
        for (int i = 0; i < 500; i++) {
            StringBuilder sb = new StringBuilder();
            int n = 1 + r.nextInt(12);
            for (int j = 0; j < n; j++) {
                sb.append(pecas[r.nextInt(pecas.length)]);
                if (r.nextBoolean()) {
                    sb.append(' ');
                }
            }
            String entrada = sb.toString();
            String saida = c.corrigir(entrada);

            assertTrue(saida != null, "devolveu null para: [" + entrada + "]");
            assertEquals(contar(entrada, "{"), contar(saida, "{"),
                "abriu/fechou tag em: [" + entrada + "] -> [" + saida + "]");
            assertEquals(contar(entrada, "}"), contar(saida, "}"),
                "chave de fechamento perdida em: [" + entrada + "] -> [" + saida + "]");
            assertEquals(contar(entrada, "\\N"), contar(saida, "\\N"),
                "quebra de linha perdida em: [" + entrada + "] -> [" + saida + "]");
        }
    }

    /**
     * CONCORRÊNCIA: o corretor é {@code @ApplicationScoped} — uma instância para a aplicação
     * inteira — e a memória interna é escrita por qualquer thread que traduza. Duas execuções
     * simultâneas (a fila do pipeline e o dry-run do karaokê já rodam em pools diferentes) não
     * podem produzir texto diferente nem corromper o cache.
     */
    @Test
    @DisplayName("concorrência: 8 threads na mesma instância dão o MESMO resultado")
    void concorrenciaNaoCorrompeAmemoria() throws Exception {
        var c = novo();
        exigeDicionario(c);
        String fala = "A situacao da colonia e critica.";
        String esperado = c.corrigir(fala);

        List<Thread> threads = new ArrayList<>();
        List<String> resultados = java.util.Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < 8; i++) {
            Thread t = new Thread(() -> {
                for (int j = 0; j < 50; j++) {
                    resultados.add(c.corrigir(fala));
                }
            });
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            t.join(30_000);
        }

        assertEquals(400, resultados.size(), "alguma thread não terminou");
        assertTrue(resultados.stream().allMatch(esperado::equals),
            "MEMÓRIA CORROMPIDA sob concorrência: resultados divergentes na mesma instância");
    }

    /** Bordas que aparecem em legenda real e derrubam quem só testou frase bonita. */
    @Test
    @DisplayName("bordas: vazio, só tag, só pontuação, texto gigante")
    void bordasNaoQuebram() {
        var c = novo();
        assertEquals("", c.corrigir(""));
        assertEquals("   ", c.corrigir("   "));
        assertEquals(null, c.corrigir(null));
        assertEquals("{\\pos(10,20)}", c.corrigir("{\\pos(10,20)}"));
        assertEquals("...", c.corrigir("..."));
        assertEquals("\\N\\N", c.corrigir("\\N\\N"));

        String gigante = "situacao ".repeat(2000);
        String saida = c.corrigir(gigante);
        assertTrue(saida.length() >= gigante.length(),
            "texto de 2000 palavras encolheu — alguma substituição comeu conteúdo");
    }

    private static int contar(String texto, String agulha) {
        int n = 0;
        int i = texto.indexOf(agulha);
        while (i >= 0) {
            n++;
            i = texto.indexOf(agulha, i + agulha.length());
        }
        return n;
    }
}
