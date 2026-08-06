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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * PROPÓSITO DE NEGÓCIO: passar o auditor de conteúdo por TODO o acervo e separar, obra a obra,
 * o que a tradução causou do que já vinha do arquivo de origem. É a pergunta que decide onde
 * vale gastar esforço — e a que nenhum relatório individual respondia.
 *
 * <h2>Por que a separação por origem é o número que importa</h2>
 * Medido no Gundam 0080 em 06/08/2026: o auditor acusou 25 anomalias no episódio 1 e a tradução
 * respondia por <b>ZERO</b>. Os 12 eventos vazios e os 8 timestamps inválidos eram os mesmos 6 e
 * 4 do Blu-ray, contados nos dois arquivos; os 5 restantes eram regra de par. Lendo só o total,
 * a conclusão seria "a tradução do 0080 está ruim" — o oposto do que o arquivo mostra.
 *
 * <h2>Como a origem é decidida</h2>
 * O auditor emite uma anomalia POR ARQUIVO, não uma comparação. Então:
 * <ul>
 *   <li>só {@code eventoOriginal} → a anomalia está na FONTE;</li>
 *   <li>só {@code eventoTraduzido} → procura gêmea (mesma regra, mesmo índice) acusada no
 *       original; achou, é herdada; não achou, apareceu na tradução;</li>
 *   <li>os dois → regra de PAR, que avalia original e traduzido juntos. Mudança de texto aí é
 *       esperada, porque a linha foi traduzida.</li>
 * </ul>
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>READ-ONLY sobre as legendas. Chama o use case de produção pelo CDI, não uma cópia.</li>
 *   <li>Pareia por PREFIXO e, na falta, por CHAVE DE EPISÓDIO. Só prefixo não bastou: no ZZ
 *       a fonte é {@code ..._Track2.ass} e a tradução {@code ..._PT-BR.parcial.ass}, e os 49
 *       episódios sumiam do inventário. Par não encontrado é contado, nunca silenciado.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Obra sem par de pastas é listada como pulada. Erro num arquivo é contado e a varredura segue —
 * um arquivo corrompido não pode cegar o inventário inteiro.
 *
 * <p>Uso: {@code gradlew test --tests "*MedicaoAuditoriaAcervoIT*" "-Dkronos.medicao=true"}
 * (opcional: {@code "-Dkronos.medicao.raiz=C:\animes"})
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoAuditoriaAcervoIT {

    private static final String RAIZ_PADRAO = "C:\\animes";
    private static final List<String> PASTAS_FONTE =
        List.of("legendas_extraidas_ass", "legendas_eng", "en");
    private static final List<String> PASTAS_TRADUZIDA =
        List.of("traducao_ptbr", "legendas_ptbr", "ptbr");

    private static final Pattern PADRAO_TEMPORADA_EPISODIO =
        Pattern.compile("s(\\d{1,2})[\\s._-]?e(\\d{1,3})");
    private static final Pattern PADRAO_NUMERO = Pattern.compile("(?<!\\d)(\\d{1,3})(?!\\d)");

    @Inject
    AuditorConteudoUseCase auditor;

    private record Contagem(String obra, int pares, int anomalias,
                            int daFonte, int naTraducao, int dePar, int erros) {
    }

    @Test
    @DisplayName("acervo: anomalias por obra, separando o que a traducao causou")
    void medir() throws IOException {
        Path raiz = Path.of(System.getProperty("kronos.medicao.raiz", RAIZ_PADRAO));
        if (!Files.isDirectory(raiz)) {
            System.out.println("RAIZ INEXISTENTE: " + raiz + " — nada medido.");
            return;
        }

        List<Contagem> linhas = new ArrayList<>();
        List<String> puladas = new ArrayList<>();
        Map<String, int[]> porRegra = new TreeMap<>();

        try (Stream<Path> obras = Files.list(raiz)) {
            for (Path obra : obras.filter(Files::isDirectory).sorted().toList()) {
                // Pastas IRMÃS, não a primeira da árvore inteira. O DanMachi tem um par por
                // temporada (Season 04/legendas_eng + Season 04/legendas_ptbr); pegar a
                // primeira fonte com a primeira tradução casaria temporadas diferentes e
                // devolvia ZERO pares — foi o que a primeira versão desta varredura fez.
                List<Path[]> paresDePasta = paresDePastaIrma(obra);
                if (paresDePasta.isEmpty()) {
                    puladas.add(obra.getFileName().toString());
                    continue;
                }
                int pares = 0, anom = 0, fonte = 0, trad = 0, par = 0, erros = 0;
                for (Path[] dupla : paresDePasta) {
                    Contagem c = auditarObra(obra.getFileName().toString(), dupla[0], dupla[1], porRegra);
                    pares += c.pares();
                    anom += c.anomalias();
                    fonte += c.daFonte();
                    trad += c.naTraducao();
                    par += c.dePar();
                    erros += c.erros();
                }
                linhas.add(new Contagem(obra.getFileName().toString(), pares, anom, fonte, trad, par, erros));
            }
        }

        imprimir(linhas, puladas, porRegra);
    }

    private Contagem auditarObra(String nome, Path fonte, Path traduzida,
                                 Map<String, int[]> porRegra) throws IOException {
        int pares = 0;
        int anomalias = 0;
        int daFonte = 0;
        int naTraducao = 0;
        int dePar = 0;
        int erros = 0;

        List<Path> traduzidos;
        try (Stream<Path> s = Files.list(traduzida)) {
            traduzidos = s.filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".ass")).toList();
        }

        try (Stream<Path> s = Files.list(fonte)) {
            for (Path original : s.filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".ass")).sorted().toList()) {
                String base = semExtensao(original.getFileName().toString());
                String chave = chaveDeEpisodio(original.getFileName().toString());
                Path par = traduzidos.stream()
                    .filter(t -> semExtensao(t.getFileName().toString()).startsWith(base))
                    .findFirst()
                    .orElseGet(() -> traduzidos.stream()
                        .filter(t -> chaveDeEpisodio(t.getFileName().toString()).equals(chave))
                        .findFirst()
                        .orElse(null));
                if (par == null) {
                    // Contado, nunca silenciado: fonte sem par significa episódio fora do
                    // inventário, e some do total sem deixar rastro se ninguém contar.
                    erros++;
                    continue;
                }
                pares++;
                try {
                    RelatorioAuditoriaConteudo r = auditor.auditar(ModoAuditoria.AMBAS, original, par);
                    List<AnomaliaConteudo> todas = r.getAnomalias();
                    anomalias += todas.size();
                    for (AnomaliaConteudo a : todas) {
                        String origem = classificarOrigem(a, todas);
                        switch (origem) {
                            case "fonte" -> daFonte++;
                            case "traducao" -> naTraducao++;
                            default -> dePar++;
                        }
                        int[] c = porRegra.computeIfAbsent(a.regra(), k -> new int[3]);
                        c["fonte".equals(origem) ? 0 : "traducao".equals(origem) ? 1 : 2]++;
                    }
                } catch (RuntimeException e) {
                    erros++;
                }
            }
        }
        return new Contagem(nome, pares, anomalias, daFonte, naTraducao, dePar, erros);
    }

    /** Mesma régua da tela: a origem sai de QUAL arquivo foi acusado, não do texto. */
    private static String classificarOrigem(AnomaliaConteudo a, List<AnomaliaConteudo> todas) {
        boolean temO = a.eventoOriginal() != null;
        boolean temT = a.eventoTraduzido() != null;
        if (temO && temT) {
            return "par";
        }
        if (temO) {
            return "fonte";
        }
        if (temT) {
            boolean gemea = todas.stream().anyMatch(outra -> outra != a
                && outra.regra().equals(a.regra())
                && outra.eventoOriginal() != null
                && outra.eventoTraduzido() == null
                && outra.eventoOriginal().indice() == a.eventoTraduzido().indice());
            return gemea ? "fonte" : "traducao";
        }
        return "par";
    }

    /**
     * Todos os pares fonte↔tradução que sejam pastas IRMÃS (mesmo pai). Uma obra pode ter
     * vários — o DanMachi tem um por temporada.
     */
    private static List<Path[]> paresDePastaIrma(Path obra) throws IOException {
        List<Path[]> pares = new ArrayList<>();
        try (Stream<Path> s = Files.walk(obra, 3)) {
            for (Path fonte : s.filter(Files::isDirectory)
                .filter(p -> PASTAS_FONTE.contains(p.getFileName().toString()))
                .sorted().toList()) {
                Path pai = fonte.getParent();
                if (pai == null) {
                    continue;
                }
                for (String nome : PASTAS_TRADUZIDA) {
                    Path candidata = pai.resolve(nome);
                    if (Files.isDirectory(candidata)) {
                        pares.add(new Path[]{fonte, candidata});
                        break;
                    }
                }
            }
        }
        return pares;
    }

    private static String semExtensao(String nome) {
        int p = nome.lastIndexOf('.');
        return p < 0 ? nome : nome.substring(0, p);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: casar o arquivo traduzido com o original mesmo quando o nome muda
     * de forma entre as duas pastas.
     *
     * <p>Casar por PREFIXO não bastou: no Gundam ZZ a fonte é {@code ..._Track2.ass} e a
     * tradução é {@code ..._PT-BR.parcial.ass}, SEM o {@code _Track2} — e a obra inteira, 49
     * episódios, saiu do inventário com zero pares. A chave passa a ser o episódio.
     *
     * <p>INVARIANTES DO DOMÍNIO: prefere {@code SxxExx}; na falta, o último número de 1 a 3
     * dígitos do nome (que é o número do episódio nos padrões de release). Sem número, devolve
     * o nome inteiro em minúsculas — degrada para o casamento antigo em vez de casar errado.
     */
    private static String chaveDeEpisodio(String nomeArquivo) {
        String base = semExtensao(nomeArquivo).toLowerCase(Locale.ROOT);
        Matcher se = PADRAO_TEMPORADA_EPISODIO.matcher(base);
        if (se.find()) {
            return "s" + Integer.parseInt(se.group(1)) + "e" + Integer.parseInt(se.group(2));
        }
        Matcher num = PADRAO_NUMERO.matcher(base);
        String ultimo = null;
        while (num.find()) {
            ultimo = num.group(1);
        }
        return ultimo != null ? "e" + Integer.parseInt(ultimo) : base;
    }

    private static void imprimir(List<Contagem> linhas, List<String> puladas,
                                 Map<String, int[]> porRegra) {
        System.out.printf("%n%-44s %6s %8s %8s %10s %7s %6s%n",
            "OBRA", "pares", "anomalias", "DA FONTE", "NA TRADUCAO", "de par", "s/par");
        linhas.stream()
            .sorted(Comparator.comparingInt(Contagem::naTraducao).reversed())
            .forEach(c -> System.out.printf("%-44s %6d %8d %8d %10d %7d %6d%n",
                recortar(c.obra()), c.pares(), c.anomalias(), c.daFonte(),
                c.naTraducao(), c.dePar(), c.erros()));

        int pares = linhas.stream().mapToInt(Contagem::pares).sum();
        int tot = linhas.stream().mapToInt(Contagem::anomalias).sum();
        int fonte = linhas.stream().mapToInt(Contagem::daFonte).sum();
        int trad = linhas.stream().mapToInt(Contagem::naTraducao).sum();
        int par = linhas.stream().mapToInt(Contagem::dePar).sum();
        int erros = linhas.stream().mapToInt(Contagem::erros).sum();

        System.out.printf("%n%-44s %6d %8d %8d %10d %7d %6d%n",
            "TOTAL", pares, tot, fonte, trad, par, erros);
        System.out.printf("%nDA FONTE ..... %d  (%.1f%%) — o arquivo original ja vinha assim%n",
            fonte, tot == 0 ? 0 : 100.0 * fonte / tot);
        System.out.printf("NA TRADUCAO .. %d  (%.1f%%) — apareceu no nosso pipeline%n",
            trad, tot == 0 ? 0 : 100.0 * trad / tot);
        System.out.printf("DE PAR ....... %d  (%.1f%%) — regra que avalia os dois juntos%n",
            par, tot == 0 ? 0 : 100.0 * par / tot);

        System.out.printf("%n%-52s %8s %10s %7s%n", "REGRA", "DA FONTE", "NA TRADUCAO", "de par");
        porRegra.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue()[1], a.getValue()[1]))
            .forEach(e -> System.out.printf("%-52s %8d %10d %7d%n",
                recortar52(e.getKey()), e.getValue()[0], e.getValue()[1], e.getValue()[2]));

        if (!puladas.isEmpty()) {
            System.out.printf("%nPULADAS (%d) — sem par de pastas:%n", puladas.size());
            puladas.forEach(p -> System.out.println("  " + p));
        }
        System.out.printf("%n%d pares auditados. Sob teste o DiretorioBaseKronos redireciona a "
            + "telemetria para arvore descartavel, entao esta varredura NAO suja relatorios/ —"
            + " verificado em 06/08/2026, contagem antes e depois igual a 29.%n", pares);
    }

    private static String recortar(String t) {
        return t.length() <= 44 ? t : t.substring(0, 43) + "…";
    }

    private static String recortar52(String t) {
        return t.length() <= 52 ? t : t.substring(0, 51) + "…";
    }
}
