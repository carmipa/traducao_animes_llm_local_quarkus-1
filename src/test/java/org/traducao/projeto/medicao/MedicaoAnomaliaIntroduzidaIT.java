package org.traducao.projeto.medicao;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.auditorConteudoLegendas.application.AuditorConteudoUseCase;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.ModoAuditoria;
import org.traducao.projeto.auditorConteudoLegendas.domain.RelatorioAuditoriaConteudo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * PROPÓSITO DE NEGÓCIO: dar ENDEREÇO às anomalias que o pipeline introduziu. A varredura do
 * acervo ({@code MedicaoAuditoriaAcervoIT}) diz que são 120 em 239 episódios; esta aqui diz
 * QUAIS, com arquivo, linha e o texto dos dois lados — que é o que permite consertar em vez
 * de admirar o número.
 *
 * <h2>Por que separada da varredura</h2>
 * A varredura precisa rodar sobre tudo e caber numa tela; esta precisa despejar detalhe. Juntar
 * as duas produziria um relatório que ninguém lê inteiro, e o detalhe é justamente o que
 * transforma medição em conserto.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>READ-ONLY. Só lista o que a régua de origem classificou como introduzido.</li>
 *   <li>Mostra os DOIS textos: sem o original ao lado, não dá para julgar se o defeito é do
 *       LLM ou herdado por caminho que a régua não viu.</li>
 *   <li>Agrupa por regra, porque conserto é por classe de defeito, não por ocorrência.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Raiz inexistente termina com aviso; arquivo que estoure é contado e a varredura segue.
 *
 * <p>Uso: {@code gradlew test --tests "*MedicaoAnomaliaIntroduzidaIT*" "-Dkronos.medicao=true"}
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoAnomaliaIntroduzidaIT {

    private static final String RAIZ_PADRAO = "C:\\animes";
    private static final List<String> PASTAS_FONTE =
        List.of("legendas_extraidas_ass", "legendas_eng", "en");
    private static final List<String> PASTAS_TRADUZIDA =
        List.of("traducao_ptbr", "legendas_ptbr", "ptbr");
    private static final Pattern PADRAO_SE = Pattern.compile("s(\\d{1,2})[\\s._-]?e(\\d{1,3})");
    private static final Pattern PADRAO_NUM = Pattern.compile("(?<!\\d)(\\d{1,3})(?!\\d)");
    private static final int TETO_POR_REGRA = 12;

    @Inject
    AuditorConteudoUseCase auditor;

    private record Achado(String arquivo, String regra, int indice, String estilo,
                          String original, String traduzido) {
    }

    @Test
    @DisplayName("acervo: ONDE estao as anomalias que a traducao introduziu")
    void medir() throws IOException {
        Path raiz = Path.of(System.getProperty("kronos.medicao.raiz", RAIZ_PADRAO));
        if (!Files.isDirectory(raiz)) {
            System.out.println("RAIZ INEXISTENTE — nada medido.");
            return;
        }

        List<Achado> achados = new ArrayList<>();
        try (Stream<Path> obras = Files.list(raiz)) {
            for (Path obra : obras.filter(Files::isDirectory).sorted().toList()) {
                for (Path[] dupla : paresDePastaIrma(obra)) {
                    coletar(obra.getFileName().toString(), dupla[0], dupla[1], achados);
                }
            }
        }

        System.out.printf("%nANOMALIAS INTRODUZIDAS PELO PIPELINE: %d%n", achados.size());
        achados.stream().collect(java.util.stream.Collectors.groupingBy(Achado::regra))
            .entrySet().stream()
            .sorted((a, b) -> b.getValue().size() - a.getValue().size())
            .forEach(e -> {
                System.out.printf("%n=== %s — %d ocorrencia(s)%n", e.getKey(), e.getValue().size());
                e.getValue().stream().limit(TETO_POR_REGRA).forEach(a -> {
                    System.out.printf("  %s  #%d  [%s]%n", a.arquivo(), a.indice(), a.estilo());
                    System.out.printf("      EN  %s%n", recortar(a.original()));
                    System.out.printf("      PT  %s%n", recortar(a.traduzido()));
                });
                if (e.getValue().size() > TETO_POR_REGRA) {
                    System.out.printf("  ... e mais %d (teto de impressao: %d)%n",
                        e.getValue().size() - TETO_POR_REGRA, TETO_POR_REGRA);
                }
            });
    }

    private void coletar(String obra, Path fonte, Path traduzida, List<Achado> achados)
        throws IOException {
        List<Path> traduzidos;
        try (Stream<Path> s = Files.list(traduzida)) {
            traduzidos = s.filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".ass")).toList();
        }
        try (Stream<Path> s = Files.list(fonte)) {
            for (Path original : s.filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".ass")).sorted().toList()) {
                String base = semExt(original.getFileName().toString());
                String chave = chave(original.getFileName().toString());
                Path par = traduzidos.stream()
                    .filter(t -> semExt(t.getFileName().toString()).startsWith(base))
                    .findFirst()
                    .orElseGet(() -> traduzidos.stream()
                        .filter(t -> chave(t.getFileName().toString()).equals(chave))
                        .findFirst().orElse(null));
                if (par == null) {
                    continue;
                }
                try {
                    RelatorioAuditoriaConteudo r = auditor.auditar(ModoAuditoria.AMBAS, original, par);
                    List<AnomaliaConteudo> todas = r.getAnomalias();
                    for (AnomaliaConteudo a : todas) {
                        if (!introduzida(a, todas)) {
                            continue;
                        }
                        var ev = a.eventoTraduzido() != null ? a.eventoTraduzido() : a.eventoOriginal();
                        achados.add(new Achado(
                            obra + " / " + par.getFileName(), a.regra(), ev.indice(), ev.estilo(),
                            a.eventoOriginal() == null ? "(sem lado original)" : visivel(a.eventoOriginal().texto()),
                            a.eventoTraduzido() == null ? "(sem lado traduzido)" : visivel(a.eventoTraduzido().texto())));
                    }
                } catch (RuntimeException ignorado) {
                    // arquivo problemático não pode cegar o inventário
                }
            }
        }
    }

    /** Mesma régua da tela e da varredura: origem sai de QUAL arquivo foi acusado. */
    private static boolean introduzida(AnomaliaConteudo a, List<AnomaliaConteudo> todas) {
        if (a.eventoOriginal() != null || a.eventoTraduzido() == null) {
            return false;
        }
        return todas.stream().noneMatch(o -> o != a
            && o.regra().equals(a.regra())
            && o.eventoOriginal() != null
            && o.eventoTraduzido() == null
            && o.eventoOriginal().indice() == a.eventoTraduzido().indice());
    }

    private static List<Path[]> paresDePastaIrma(Path obra) throws IOException {
        List<Path[]> pares = new ArrayList<>();
        try (Stream<Path> s = Files.walk(obra, 3)) {
            for (Path fonte : s.filter(Files::isDirectory)
                .filter(p -> PASTAS_FONTE.contains(p.getFileName().toString())).sorted().toList()) {
                Path pai = fonte.getParent();
                if (pai == null) {
                    continue;
                }
                for (String nome : PASTAS_TRADUZIDA) {
                    Path c = pai.resolve(nome);
                    if (Files.isDirectory(c)) {
                        pares.add(new Path[]{fonte, c});
                        break;
                    }
                }
            }
        }
        return pares;
    }

    private static String semExt(String n) {
        int p = n.lastIndexOf('.');
        return p < 0 ? n : n.substring(0, p);
    }

    private static String chave(String nome) {
        String base = semExt(nome).toLowerCase(Locale.ROOT);
        Matcher se = PADRAO_SE.matcher(base);
        if (se.find()) {
            return "s" + Integer.parseInt(se.group(1)) + "e" + Integer.parseInt(se.group(2));
        }
        Matcher n = PADRAO_NUM.matcher(base);
        String ultimo = null;
        while (n.find()) {
            ultimo = n.group(1);
        }
        return ultimo != null ? "e" + Integer.parseInt(ultimo) : base;
    }

    private static String visivel(String t) {
        return t == null ? "" : t.replace("\\N", " ").strip();
    }

    private static String recortar(String t) {
        return t.length() <= 110 ? t : t.substring(0, 110) + "…";
    }
}
