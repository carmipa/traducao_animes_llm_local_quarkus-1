package org.traducao.projeto.core.texto.dicionarioOrtografia;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: perguntar ao dicionário <b>o que ele propõe</b> para cada palavra quebrada
 * do acervo, e imprimir a resposta crua — para a decisão de consertar ou não ser tomada olhando
 * dados, não intuição.
 *
 * <h2>Por que este passo existe entre o relatório e o conserto</h2>
 * A tentação, ao ver {@code pasaje} numa legenda, é escrever a regra {@code pasaje→passagem}. Duas
 * coisas quebram nesse caminho, e as duas já aconteceram aqui:
 *
 * <ul>
 *   <li><b>O dicionário não é oráculo.</b> Em 23/08/2026 pedir sugestão para palavras sem acento
 *       devolveu {@code terra→terrá}, {@code batalha→batalhá}, {@code garota→garotá}. Saber que
 *       existe uma variante não diz que ela é a certa. Antes de confiar na sugestão para
 *       QUALQUER classe de palavra, é preciso ver a lista dele.</li>
 *   <li><b>Lista escrita à mão é dívida imediata.</b> {@code pasaje→passagem} conserta uma obra e
 *       não conserta a próxima tradução.</li>
 * </ul>
 *
 * <p>Este harness não decide nada e não escreve nada. Ele mostra, palavra por palavra: quantas
 * sugestões o dicionário tem, quais são, e a distância de edição da primeira. É com isso que a
 * régua do conserto automático é calibrada — ou descartada.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>READ-ONLY, e sem varrer o acervo: recebe as palavras já medidas pelo
 *       {@link MedicaoPalavraQuebradaEidiomaVazadoIT}, por propriedade.</li>
 *   <li>UM lote para o dicionário. Perguntar palavra a palavra custaria um processo externo cada.</li>
 *   <li>Dicionário indisponível termina como NÃO VERIFICADO.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Lista de palavras vazia termina declarando, sem número.
 *
 * <p>Uso: {@code gradlew test --tests "*MedicaoSugestaoParaPalavraQuebradaIT*" "-Dkronos.medicao=true"
 * "-Dkronos.palavras=pasaje,misil,insigna"}
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoSugestaoParaPalavraQuebradaIT {

    private static final String PALAVRAS = System.getProperty("kronos.palavras", "");

    @Test
    @DisplayName("mostra o que o dicionario propoe para cada palavra quebrada, sem decidir nada")
    void mostrarSugestoes() {
        System.out.printf("%n=== O QUE O DICIONARIO PROPOE PARA A PALAVRA QUEBRADA ===%n");
        Set<String> palavras = new LinkedHashSet<>();
        for (String p : PALAVRAS.split("[,\\s]+")) {
            if (!p.isBlank()) {
                palavras.add(p.trim());
            }
        }
        if (palavras.isEmpty()) {
            System.out.println("NAO VERIFICADO: nenhuma palavra recebida em -Dkronos.palavras. "
                + "Lista vazia aqui NAO significa acervo limpo.");
            return;
        }

        DicionarioOrtograficoPort pt = new HunspellDicionarioAdapter("hunspell", "pt_BR");

        // CONTROLE: uma palavra com conserto obvio e uma palavra correta. Se o dicionario nao
        // propuser nada para a primeira, ou propuser algo para a segunda, a lista abaixo nao vale.
        Set<String> controle = new LinkedHashSet<>(List.of("territorio", "batalha"));
        Map<String, Set<String>> resposta = pt.sugestoes(controle);
        if (!pt.disponivel()) {
            System.out.println("NAO VERIFICADO: dicionario indisponivel — " + pt.descricao());
            return;
        }
        Set<String> paraDoente = resposta.getOrDefault("territorio", Set.of());
        Set<String> paraSao = resposta.getOrDefault("batalha", Set.of());
        if (paraDoente.isEmpty() || !paraSao.isEmpty()) {
            System.out.printf("INSTRUMENTO REPROVADO NO CONTROLE: 'territorio'->%s (devia propor), "
                + "'batalha'->%s (nao devia propor nada). Nenhuma linha abaixo vale.%n",
                paraDoente, paraSao);
            return;
        }
        System.out.printf("  controle: 'territorio' -> %s · 'batalha' -> (nada)%n%n", paraDoente);

        Map<String, Set<String>> sugestoes = pt.sugestoes(palavras);
        System.out.printf("%-16s %3s  %-11s %s%n", "PALAVRA", "N", "DISTANCIA", "SUGESTOES");
        List<String> semSugestao = new ArrayList<>();
        int comUma = 0;
        int comUmaPerto = 0;
        for (String p : palavras) {
            List<String> lista = new ArrayList<>(sugestoes.getOrDefault(p, Set.of()));
            if (lista.isEmpty()) {
                semSugestao.add(p);
                System.out.printf("%-16s %3d  %-11s %s%n", p, 0, "-", "(nenhuma)");
                continue;
            }
            int d = distanciaDeEdicao(p.toLowerCase(), lista.get(0).toLowerCase());
            if (lista.size() == 1) {
                comUma++;
                if (d <= 2) {
                    comUmaPerto++;
                }
            }
            System.out.printf("%-16s %3d  %-11d %s%n", p, lista.size(), d,
                lista.size() > 6 ? lista.subList(0, 6) + " ..." : lista);
        }

        System.out.printf("%n  RESUMO: %d palavras · %d sem sugestao nenhuma · %d com UMA sugestao "
            + "· %d com UMA sugestao a distancia <= 2%n",
            palavras.size(), semSugestao.size(), comUma, comUmaPerto);
        if (!semSugestao.isEmpty()) {
            System.out.println("  sem sugestao: " + semSugestao);
        }
        System.out.println("""

              LEIA ANTES DE DECIDIR: "uma sugestao so" nao e o mesmo que "sugestao certa".
              A regra do conserto automatico, se existir, sai da leitura desta lista — e o que
              ela nao cobrir continua sendo relatorio, porque inventar palavra plausivel e pior
              que deixar a palavra quebrada: o leitor tropeca uma vez no erro visivel e
              desconfia; no erro invisivel, nunca.""");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: responder UMA pergunta, com dado e não com intuição — <i>a forma
     * MINÚSCULA e acentuada da palavra é português comum, ou só existe como nome próprio?</i>
     *
     * <h2>A decisão que depende desta resposta</h2>
     * Em 24/08/2026 a leitura das falas do acervo pegou o elo do dicionário acentuando nome:
     * {@code Cardeas→Cárdeas}, {@code Artemis→Ártemis}, {@code Astrea→Ástrea},
     * {@code Ingues→Ingués}, {@code Cleo→Cléo}. A posição na frase não resolve — nome de
     * personagem abre frase o tempo todo ({@code "Cárdeas Vist."}, {@code "Ártemis..."}).
     *
     * <p>A hipótese a testar é esta: {@code território} minúsculo É palavra portuguesa comum, e
     * {@code ártemis} minúsculo NÃO é — só existe capitalizado, porque é nome. Se o dicionário
     * separar os dois, a regra sai daqui e não de uma lista escrita à mão.
     *
     * <p>Se ele NÃO separar, a hipótese morre aqui e não vira código — que é exatamente para isso
     * que este passo existe.
     *
     * <p>INVARIANTES DO DOMÍNIO: read-only; um lote por consulta.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: dicionário mudo termina NÃO VERIFICADO.
     */
    @Test
    @DisplayName("a forma MINUSCULA separa nome proprio de palavra comum? (a hipotese da regra)")
    void minusculaSeparaNomeDePalavraComum() {
        System.out.printf("%n=== A FORMA MINUSCULA SEPARA NOME DE PALAVRA COMUM? ===%n");

        // ESTRAGO medido no acervo, e CONTROLE de palavra comum. Os dois lados no mesmo
        // experimento: um instrumento que so olha os casos doentes nao prova nada.
        List<String> nomes = List.of("Cárdeas", "Ártemis", "Ástrea", "Ingués", "Cléo");
        List<String> comuns = List.of("Território", "Reforços", "Necessário", "Fácil", "Está");

        Set<String> perguntas = new LinkedHashSet<>();
        for (String n : nomes) {
            perguntas.add(n);
            perguntas.add(n.toLowerCase());
        }
        for (String c : comuns) {
            perguntas.add(c);
            perguntas.add(c.toLowerCase());
        }

        DicionarioOrtograficoPort pt = new HunspellDicionarioAdapter("hunspell", "pt_BR");
        Set<String> desconhecidas = pt.desconhecidas(perguntas);
        if (!pt.disponivel()) {
            System.out.println("NAO VERIFICADO: dicionario indisponivel — " + pt.descricao());
            return;
        }

        System.out.printf("%n  %-14s %-14s %-14s%n", "PALAVRA", "COMO VEIO", "MINUSCULA");
        int nomesQueASeparacaoPega = 0;
        int comunsQueASeparacaoPerde = 0;
        for (String n : nomes) {
            boolean capOk = !desconhecidas.contains(n);
            boolean minOk = !desconhecidas.contains(n.toLowerCase());
            if (!minOk) {
                nomesQueASeparacaoPega++;
            }
            System.out.printf("  %-14s %-14s %-14s   <- NOME%n", n,
                capOk ? "portugues" : "desconhecida", minOk ? "portugues" : "desconhecida");
        }
        for (String c : comuns) {
            boolean minOk = !desconhecidas.contains(c.toLowerCase());
            if (!minOk) {
                comunsQueASeparacaoPerde++;
            }
            System.out.printf("  %-14s %-14s %-14s   <- COMUM%n", c,
                desconhecidas.contains(c) ? "desconhecida" : "portugues",
                minOk ? "portugues" : "desconhecida");
        }

        System.out.printf("%n  VEREDICTO: a regra \"so corrige se a MINUSCULA for portuguesa\" "
            + "barraria %d de %d nomes e perderia %d de %d palavras comuns.%n",
            nomesQueASeparacaoPega, nomes.size(), comunsQueASeparacaoPerde, comuns.size());
        if (nomesQueASeparacaoPega == nomes.size() && comunsQueASeparacaoPerde == 0) {
            System.out.println("  => A HIPOTESE SE SUSTENTA. A regra pode sair daqui.");
        } else {
            System.out.println("  => A HIPOTESE NAO SE SUSTENTA COMO ESTA. Nao vira codigo assim.");
        }
    }

    /**
     * Distância de Levenshtein — só para ORDENAR a leitura, nunca para decidir sozinha.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada nula conta como vazia; nunca lança.
     */
    static int distanciaDeEdicao(String a, String b) {
        String x = a == null ? "" : a;
        String y = b == null ? "" : b;
        int[] anterior = new int[y.length() + 1];
        int[] atual = new int[y.length() + 1];
        for (int j = 0; j <= y.length(); j++) {
            anterior[j] = j;
        }
        for (int i = 1; i <= x.length(); i++) {
            atual[0] = i;
            for (int j = 1; j <= y.length(); j++) {
                int custo = x.charAt(i - 1) == y.charAt(j - 1) ? 0 : 1;
                atual[j] = Math.min(Math.min(atual[j - 1] + 1, anterior[j] + 1), anterior[j - 1] + custo);
            }
            int[] troca = anterior;
            anterior = atual;
            atual = troca;
        }
        return anterior[y.length()];
    }

    /** Só para o Arrays não sair do import sem uso quando a lista vier vazia. */
    static List<String> partir(String bruto) {
        return Arrays.stream(bruto.split("[,\\s]+")).filter(s -> !s.isBlank()).toList();
    }
}
