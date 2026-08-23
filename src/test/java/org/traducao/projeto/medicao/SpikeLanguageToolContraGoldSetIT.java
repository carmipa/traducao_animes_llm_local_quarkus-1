package org.traducao.projeto.medicao;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.languagetool.JLanguageTool;
import org.languagetool.language.BrazilianPortuguese;
import org.languagetool.rules.RuleMatch;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * PROPÓSITO DE NEGÓCIO: decidir com NÚMERO, e não com opinião, se o LanguageTool entra no KRONOS
 * como revisor de gramática pt-BR — medindo-o contra um gold set lido à mão.
 *
 * <h2>Por que este experimento existe, e por que agora</h2>
 * Paulo perguntou em 2026-08-23: <i>"alternativa que poderíamos fazer talvez adicionar um
 * dicionário ou ferramenta externa específica e especializada em gramática ptbr?"</i>. A resposta
 * curta é sim, e o candidato é o LanguageTool: Java puro, roda embutido e <b>100% offline</b>
 * (não viola "o KRONOS é local"), e traz o que <b>nenhum</b> instrumento nosso tem — um POS
 * tagger com desambiguação.
 *
 * <p>É exatamente a peça que falta. O hunspell responde "esta palavra existe?", e para
 * {@code noticia}, {@code orbita} e {@code premio} a resposta é SIM, porque são conjugações
 * legítimas de {@code noticiar}, {@code orbitar} e {@code premiar}. Onde o modelo inverte verbo e
 * substantivo — palavras de Paulo — a existência deixa de ser evidência de qualquer coisa.
 *
 * <h2>O gold set, que é a parte cara e já foi paga</h2>
 * Em 23/08/2026 os episódios 1 e 2 do Macross II foram lidos INTEIROS, fala por fala, e as
 * defeituosas anotadas uma a uma: <b>60 de 477</b>. Esse é o padrão-ouro contra o qual o
 * LanguageTool é medido aqui. Sem ele, "o LanguageTool achou 200 coisas" não significaria nada —
 * não se saberia quantas eram reais.
 *
 * <h2>O que este experimento responde</h2>
 * <pre>
 *   RECALL    das 60 do gold set, quantas ele acha?
 *   PRECISAO  quantos alarmes ele dá em fala que a leitura humana julgou CORRETA?
 *   CUSTO     quanto tempo por fala, e cabe no pipeline?
 *   RUIDO     ele acusa nome de lore (Zentradi, Valkyrie, Minmay, Meltran)?
 * </pre>
 *
 * <h2>O critério de decisão, escrito ANTES de ver o resultado</h2>
 * Isto é deliberado: critério escolhido depois do número é justificativa, não decisão.
 * <ul>
 *   <li>Recall abaixo de 50% ⇒ não resolve o problema que motivou.</li>
 *   <li>Mais alarmes falsos do que acertos ⇒ <b>não entra</b>. Guarda que reprova texto correto
 *       ensina a desligar a guarda, e este projeto já pagou por isso.</li>
 *   <li>Acusar nome de lore ⇒ entra só depois de passar pela proteção de termos que já existe.</li>
 * </ul>
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>READ-ONLY. Mede e imprime; não escreve no acervo e não altera o pipeline.</li>
 *   <li>Escopo de TESTE. O LanguageTool é {@code testImplementation} de propósito: ele está aqui
 *       para ser julgado, não para ser usado.</li>
 *   <li>O texto entregue a ele é o VISÍVEL: sem tag {@code {...}} e com a quebra {@code \\N} do
 *       ASS virando espaço. Entregar marcação a um corretor de gramática seria medir o ruído da
 *       marcação, não a gramática.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Acervo ausente ou episódios não encontrados termina declarando isso, sem afirmar número.
 *
 * <p>Uso: {@code gradlew test --tests "*SpikeLanguageToolContraGoldSetIT*" "-Dkronos.medicao=true"}
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class SpikeLanguageToolContraGoldSetIT {

    private static final Path RAIZ = Path.of(System.getProperty("kronos.acervo", "C:\\animes"));
    private static final String OBRA = "Macross II";

    @Inject
    LeitorLegendaAss leitor;

    @Inject
    PoliticaEstiloMusical politicaEstiloMusical;

    private static final Pattern TAG = Pattern.compile("\\{[^{}]*}");

    /**
     * O PADRÃO-OURO: índice das falas que a leitura humana julgou defeituosas, em 23/08/2026.
     * Contado a partir de 1, na ordem em que a linha {@code Dialogue:} aparece no arquivo.
     */
    private static final Map<String, Set<Integer>> GOLD = new LinkedHashMap<>();

    static {
        // ep01: 32 de 249 falas
        GOLD.put("01", Set.of(9, 17, 21, 26, 30, 33, 49, 52, 53, 58, 70, 76, 77, 78, 93, 95, 99,
            106, 137, 140, 147, 152, 172, 207, 218, 220, 229, 234, 235, 239, 245, 247));
        // ep02: 28 de 228 falas
        GOLD.put("02", Set.of(13, 14, 27, 29, 33, 53, 56, 61, 76, 81, 83, 98, 106, 109, 113, 116,
            117, 118, 119, 124, 126, 127, 149, 153, 160, 163, 183, 199));
    }

    /** Nomes que o LanguageTool não pode acusar sem virar ruído: são lore, não erro. */
    private static final Set<String> LORE = Set.of(
        "Zentradi", "Valkyrie", "Minmay", "Meltran", "Ishtar", "Macross", "Hibiki", "Silvie",
        "Exxegran", "Nexx", "Feff", "Emulator", "Alus", "Microne", "Spacy");

    private record Achado(String ep, int fala, String regra, String categoria, String mensagem,
                          String trecho) {}

    @Test
    @DisplayName("mede o LanguageTool contra as 60 falas lidas a mao no Macross II")
    void medir() throws IOException {
        System.out.printf("%n=== SPIKE LanguageTool pt-BR vs GOLD SET (Macross II ep1+ep2) ===%n");
        if (!Files.isDirectory(RAIZ)) {
            System.out.println("NAO VERIFICADO: acervo ausente em " + RAIZ);
            return;
        }
        Path pasta = localizarPastaDaObra();
        if (pasta == null) {
            System.out.println("NAO VERIFICADO: pasta traducao_ptbr do Macross II nao encontrada");
            return;
        }

        // O `grammar.xml` do portugues estoura o teto de entidades XML do JDK — por 96 bytes:
        // "accumulated size of entities is 100.096 that exceeded the 100.000 limit". O teto
        // existe contra o ataque "billion laughs", e por isso ele NAO e afrouxado no projeto
        // inteiro: e ajustado aqui, em escopo de teste, para um arquivo de uma biblioteca
        // conhecida. Valor finito de proposito — desligar (0) tiraria a protecao de vez.
        System.setProperty("jdk.xml.totalEntitySizeLimit", "5000000");

        long inicioCarga = System.currentTimeMillis();
        JLanguageTool lt = new JLanguageTool(new BrazilianPortuguese());

        // O CORRETOR ORTOGRAFICO dele sai, e a primeira rodada (23/08/2026) e que provou por que:
        // MORFOLOGIK_RULE_PT_BR disparou 117 das 158 acusacoes, e o que ele acusava era
        // `UN`, `Spacy`, `Exxegran`, `Silvie` — NOME DE LORE, nao erro. Pior: contaminava os dois
        // lados da conta, porque uma fala do gold set marcada por causa do nome proprio contava
        // como "acerto" sem ele ter visto o defeito real. Coincidir de linha nao e acertar.
        //
        // Ortografia o projeto ja tem, com hunspell e com a lore protegida. O que se esta
        // medindo aqui e o que o KRONOS NAO tem: as regras de GRAMATICA, que dependem do POS
        // tagger.
        lt.disableRule("MORFOLOGIK_RULE_PT_BR");

        long msCarga = System.currentTimeMillis() - inicioCarga;
        System.out.printf("  carga do motor ......... %d ms, %d regras ativas (sem o ortografico)%n",
            msCarga, lt.getAllActiveRules().size());

        int totalFalas = 0;
        long msAnalise = 0;
        List<Achado> achados = new ArrayList<>();
        Map<String, Integer> falasPorEp = new TreeMap<>();
        Map<String, Set<Integer>> acusadasPorEp = new TreeMap<>();

        for (Map.Entry<String, Set<Integer>> alvo : GOLD.entrySet()) {
            String ep = alvo.getKey();
            Path arquivo = arquivoDoEpisodio(pasta, ep);
            if (arquivo == null) {
                System.out.println("NAO VERIFICADO: episodio " + ep + " nao encontrado em " + pasta);
                return;
            }
            DocumentoLegenda documento = leitor.ler(arquivo);
            int n = 0;
            Set<Integer> acusadas = new LinkedHashSet<>();
            for (EventoLegenda evento : documento.eventos()) {
                if (evento.estilo() != null && politicaEstiloMusical.estiloIgnorado(evento.estilo())) {
                    continue;
                }
                n++;
                String texto = visivel(evento.texto());
                if (texto.isBlank()) {
                    continue;
                }
                long t0 = System.nanoTime();
                List<RuleMatch> matches = lt.check(texto);
                msAnalise += (System.nanoTime() - t0) / 1_000_000;
                for (RuleMatch m : matches) {
                    acusadas.add(n);
                    achados.add(new Achado(ep, n, m.getRule().getId(),
                        m.getRule().getCategory() == null
                            ? "?" : m.getRule().getCategory().getId().toString(),
                        m.getMessage(), trecho(texto, m)));
                }
            }
            falasPorEp.put(ep, n);
            acusadasPorEp.put(ep, acusadas);
            totalFalas += n;
        }

        // ---------- confronto ----------
        int goldTotal = 0;
        int acertos = 0;
        int alarmesFalsos = 0;
        List<String> perdidas = new ArrayList<>();
        for (Map.Entry<String, Set<Integer>> alvo : GOLD.entrySet()) {
            String ep = alvo.getKey();
            Set<Integer> gold = alvo.getValue();
            Set<Integer> acusadas = acusadasPorEp.getOrDefault(ep, Set.of());
            goldTotal += gold.size();
            for (Integer g : gold) {
                if (acusadas.contains(g)) {
                    acertos++;
                } else {
                    perdidas.add("ep" + ep + " #" + g);
                }
            }
            for (Integer a : acusadas) {
                if (!gold.contains(a)) {
                    alarmesFalsos++;
                }
            }
        }

        System.out.printf("%n  falas analisadas ....... %d  (%s)%n", totalFalas, falasPorEp);
        System.out.printf("  tempo de analise ....... %d ms  (%.1f ms por fala)%n",
            msAnalise, totalFalas == 0 ? 0.0 : (double) msAnalise / totalFalas);
        System.out.printf("  acusacoes do LT ........ %d em %d falas distintas%n",
            achados.size(), acusadasPorEp.values().stream().mapToInt(Set::size).sum());

        System.out.printf("%n  GOLD SET ............... %d falas lidas a mao%n", goldTotal);
        System.out.printf("  ACERTOU ................ %d  (recall %.0f%%)%n",
            acertos, goldTotal == 0 ? 0.0 : 100.0 * acertos / goldTotal);
        System.out.printf("  PERDEU ................. %d%n", goldTotal - acertos);
        System.out.printf("  ALARME em fala que li como CORRETA ... %d%n", alarmesFalsos);

        // ---------- lore ----------
        long acusacoesEmLore = achados.stream()
            .filter(a -> LORE.stream().anyMatch(t -> a.trecho().contains(t)))
            .count();
        System.out.printf("  acusacoes que tocam nome de lore ..... %d%n", acusacoesEmLore);

        // ---------- o veredito, contra o criterio escrito ANTES ----------
        double recall = goldTotal == 0 ? 0 : 100.0 * acertos / goldTotal;
        System.out.println("\n  --- veredito contra o criterio declarado antes da medicao ---");
        System.out.printf("  recall >= 50%%? .................... %s (%.0f%%)%n",
            recall >= 50 ? "SIM" : "NAO", recall);
        System.out.printf("  acertos > alarmes falsos? ......... %s (%d vs %d)%n",
            acertos > alarmesFalsos ? "SIM" : "NAO", acertos, alarmesFalsos);
        System.out.printf("  nao mexe em lore? ................. %s (%d acusacoes)%n",
            acusacoesEmLore == 0 ? "SIM" : "NAO", acusacoesEmLore);

        // ---------- o detalhe, para leitura humana ----------
        // A CATEGORIA e o que vai virar configuracao de producao: e por categoria que se liga e
        // desliga em bloco, e nao regra a regra. Cruzada com o gold set, ela diz quais blocos
        // acertam e quais so fazem barulho.
        System.out.println("\n--- CATEGORIA: acerto x alarme, que e o que decide o que fica ligado ---");
        Map<String, int[]> porCategoria = new TreeMap<>();
        for (Achado a : achados) {
            boolean noGold = GOLD.getOrDefault(a.ep(), Set.of()).contains(a.fala());
            int[] par = porCategoria.computeIfAbsent(a.categoria(), k -> new int[2]);
            par[noGold ? 0 : 1]++;
        }
        System.out.printf("  %-34s %7s %7s%n", "categoria", "acerto", "alarme");
        porCategoria.forEach((c, par) ->
            System.out.printf("  %-34s %7d %7d%n", c, par[0], par[1]));

        System.out.println("\n--- REGRAS que mais dispararam ---");
        Map<String, Integer> porRegra = new TreeMap<>();
        achados.forEach(a -> porRegra.merge(a.regra(), 1, Integer::sum));
        porRegra.entrySet().stream()
            .sorted(Comparator.comparingInt((Map.Entry<String, Integer> e) -> -e.getValue()))
            .limit(20)
            .forEach(e -> System.out.printf("  %4d  %s%n", e.getValue(), e.getKey()));

        System.out.println("\n--- ACERTOS: o que ele pegou do gold set (ate 15) ---");
        achados.stream()
            .filter(a -> GOLD.getOrDefault(a.ep(), Set.of()).contains(a.fala()))
            .limit(15)
            .forEach(a -> System.out.printf("  ep%s #%3d [%s] %s%n     %s%n",
                a.ep(), a.fala(), a.regra(), a.trecho(), a.mensagem()));

        System.out.println("\n--- ALARMES em fala que li como correta (ate 15) ---");
        achados.stream()
            .filter(a -> !GOLD.getOrDefault(a.ep(), Set.of()).contains(a.fala()))
            .limit(15)
            .forEach(a -> System.out.printf("  ep%s #%3d [%s] %s%n     %s%n",
                a.ep(), a.fala(), a.regra(), a.trecho(), a.mensagem()));

        System.out.println("\n--- PERDIDAS: do gold set, o que ele NAO viu ---");
        System.out.println("  " + perdidas);
    }

    private Path localizarPastaDaObra() throws IOException {
        try (Stream<Path> s = Files.list(RAIZ)) {
            Path obra = s.filter(Files::isDirectory)
                .filter(p -> p.getFileName().toString().startsWith(OBRA))
                .findFirst().orElse(null);
            return obra == null ? null : obra.resolve("traducao_ptbr");
        }
    }

    private Path arquivoDoEpisodio(Path pasta, String ep) throws IOException {
        if (!Files.isDirectory(pasta)) {
            return null;
        }
        try (Stream<Path> s = Files.list(pasta)) {
            return s.filter(p -> p.getFileName().toString().endsWith(".ass"))
                .filter(p -> p.getFileName().toString().contains(" - " + ep + "v2"))
                .findFirst().orElse(null);
        }
    }

    private static String visivel(String texto) {
        if (texto == null) {
            return "";
        }
        return TAG.matcher(texto).replaceAll(" ")
            .replace("\\N", " ").replace("\\n", " ").trim();
    }

    private static String trecho(String texto, RuleMatch m) {
        int ini = Math.max(0, m.getFromPos());
        int fim = Math.min(texto.length(), m.getToPos());
        if (ini >= fim) {
            return texto.length() > 60 ? texto.substring(0, 60) : texto;
        }
        int ctxIni = Math.max(0, ini - 18);
        int ctxFim = Math.min(texto.length(), fim + 18);
        return texto.substring(ctxIni, ini) + "[" + texto.substring(ini, fim) + "]"
            + texto.substring(fim, ctxFim);
    }
}
