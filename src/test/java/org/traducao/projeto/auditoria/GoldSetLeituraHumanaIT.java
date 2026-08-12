package org.traducao.projeto.auditoria;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: produz a lista CURTA que uma pessoa consegue ler. O único eixo que
 * nenhuma medição resolve é "qual português está melhor", e ele emperrou em 5.455 falas por
 * obra — volume que ninguém lê. Este harness não julga qualidade: ele ORDENA por suspeita e
 * entrega os pares lado a lado, para que a leitura humana caia onde há chance real de erro.
 *
 * <h2>Por que ordenar por divergência</h2>
 * Onde os dois modelos escrevem a mesma coisa, não há o que decidir — e é a maioria. Onde eles
 * divergem MUITO, ou um está errado, ou um está muito melhor; nos dois casos a fala vale a
 * leitura. É o mesmo princípio das 4.249 discordâncias do Guilty Crown que "esperam leitura
 * humana" desde 11/08 sem nunca terem sido priorizadas.
 *
 * <h2>Os dois sinais, e por que dois</h2>
 * <ul>
 *   <li><b>Perda de conteúdo</b> — uma tradução muito mais curta que a outra. Pega o caso
 *       {@code "You can't possibly approve of this situation, Commander!"} → {@code "Sim,
 *       senhor!"}, que é o defeito mais grave que já apareceu e nenhum validador viu.</li>
 *   <li><b>Distância de vocabulário</b> — poucas palavras em comum entre as duas traduções da
 *       MESMA fala. Pega a divergência de sentido sem diferença de tamanho.</li>
 * </ul>
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Música e karaokê saem pelos vetos de PRODUÇÃO, como nas demais medições.</li>
 *   <li>O corte é DECLARADO: o arquivo diz quantas falas divergiram no total e quantas foram
 *       escritas. Truncar em silêncio faria "as 60 piores" parecer "todas".</li>
 *   <li>Nada é escrito no acervo — a saída vai para {@code relatorios/}, que é runtime.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem acervo, PULA por {@link Assumptions}.
 */
@DisplayName("gold set: os pares mais divergentes entre mistral e aya, para leitura humana")
class GoldSetLeituraHumanaIT {

    private static final Path OBRA = Path.of("C:", "animes",
        "Mobile Suit Gundam Unicorn Re0096 (2016) [Season 1] [BD 1080p HEVC OPUS] [Dual-Audio]",
        "Gundam Unicorn Season 1");
    private static final Path ENTRADA = Path.of("backups", "troca_tipo_legenda_20260812_113344");
    private static final Path SAIDA = Path.of("relatorios", "gold-set-unicorn-mistral-x-aya.md");
    private static final Pattern TAGS = Pattern.compile("\\{[^}]*\\}");
    private static final int QUANTOS_ESCREVER = 60;

    private record Par(String ep, String en, String mistral, String aya, double suspeita, String motivo) {}

    @Test
    @DisplayName("escreve o gold set ordenado por suspeita, com o corte declarado")
    void gerarGoldSet() throws Exception {
        Assumptions.assumeTrue(Files.isDirectory(ENTRADA) && Files.isDirectory(OBRA),
            "acervo do Unicorn ausente — NÃO VERIFICADO");

        // CONTROLE POSITIVO: o par mais grave já observado tem de pontuar alto, e duas
        // traduções equivalentes têm de pontuar baixo. Sem isto, um escore quebrado ordenaria
        // por acaso e a "lista das piores" seria uma amostra aleatória com nome bonito.
        double grave = suspeita("You can't possibly approve of this situation, Commander!",
            "Você não pode aprovar esta situação, Comandante!", "Sim, senhor!");
        double equivalente = suspeita("It's dangerous.",
            "É perigoso.", "Isso é perigoso.");
        assertTrue(grave > equivalente * 2,
            "instrumento cego: o par que perdeu a frase inteira (" + grave
                + ") tem de pontuar MUITO acima de duas traduções equivalentes (" + equivalente + ")");

        LeitorLegendaAss leitor = new LeitorLegendaAss();
        var detector = new DetectorEfeitoKaraokeService();
        var politica = new PoliticaEstiloMusical(MedicaoUnicornMistralXAyaIT.estilosIgnoradosDoYml());

        Map<String, Path> eng = porEpisodio(ENTRADA);
        Map<String, Path> mis = porEpisodio(OBRA.resolve("traducao_mistral"));
        Map<String, Path> aya = porEpisodio(OBRA.resolve("traducao_aya"));

        List<Par> pares = new ArrayList<>();
        int comparadas = 0;
        for (String ep : eng.keySet().stream().filter(mis::containsKey).filter(aya::containsKey).sorted().toList()) {
            List<EventoLegenda> o = eventos(leitor, eng.get(ep));
            List<EventoLegenda> m = eventos(leitor, mis.get(ep));
            List<EventoLegenda> a = eventos(leitor, aya.get(ep));
            int n = Math.min(o.size(), Math.min(m.size(), a.size()));
            for (int i = 0; i < n; i++) {
                String en = visivel(o.get(i).texto());
                String tm = visivel(m.get(i).texto());
                String ta = visivel(a.get(i).texto());
                if (en.isEmpty() || tm.isEmpty() || ta.isEmpty() || tm.equals(ta)) {
                    continue;
                }
                if (detector.podeSerCamadaMusical(o.get(i).estilo(), o.get(i).texto())
                    || detector.eEfeitoKaraoke(o.get(i).texto())
                    || politica.estiloIgnorado(o.get(i).estilo())) {
                    continue;
                }
                comparadas++;
                double s = suspeita(en, tm, ta);
                if (s > 0.45) {
                    pares.add(new Par(ep, en, tm, ta, s, motivo(tm, ta)));
                }
            }
        }

        assertFalse(pares.isEmpty(), "instrumento cego: nenhuma divergência encontrada em " + comparadas + " falas");
        pares.sort(Comparator.comparingDouble(Par::suspeita).reversed());

        StringBuilder md = new StringBuilder();
        md.append("# Gold set — Gundam Unicorn: mistral × aya\n\n");
        md.append("Gerado por `GoldSetLeituraHumanaIT`. **Isto não julga qualidade** — ordena por\n");
        md.append("suspeita e entrega os pares lado a lado para leitura humana.\n\n");
        md.append("- falas comparadas (após os vetos de música): **").append(comparadas).append("**\n");
        md.append("- falas em que as duas traduções DIVERGEM: **").append(pares.size()).append("**\n");
        md.append("- escritas abaixo: **").append(Math.min(QUANTOS_ESCREVER, pares.size()))
          .append("** (as de maior suspeita)\n\n");
        md.append("Para cada par, marque qual está melhor. O que sobrar de empate é ruído.\n\n---\n\n");
        int escritos = 0;
        for (Par p : pares) {
            if (escritos++ >= QUANTOS_ESCREVER) {
                break;
            }
            md.append("### ").append(escritos).append(". ").append(p.ep())
              .append("  _(").append(p.motivo()).append(")_\n\n");
            md.append("| | |\n|---|---|\n");
            md.append("| **EN** | ").append(escapar(p.en())).append(" |\n");
            md.append("| mistral | ").append(escapar(p.mistral())).append(" |\n");
            md.append("| aya | ").append(escapar(p.aya())).append(" |\n\n");
        }
        if (pares.size() > QUANTOS_ESCREVER) {
            md.append("---\n\n**").append(pares.size() - QUANTOS_ESCREVER)
              .append(" pares divergentes não entraram nesta lista.** O corte é de ")
              .append(QUANTOS_ESCREVER).append(", e está declarado para que \"as piores\" nunca")
              .append(" seja lido como \"todas\".\n");
        }
        Files.createDirectories(SAIDA.getParent());
        Files.writeString(SAIDA, md.toString(), StandardCharsets.UTF_8);

        System.out.printf("%n=== GOLD SET: %d divergências em %d falas comparadas; %d escritas ===%n",
            pares.size(), comparadas, Math.min(QUANTOS_ESCREVER, pares.size()));
        System.out.println("   " + SAIDA.toAbsolutePath());
        pares.stream().limit(8).forEach(p -> System.out.printf(
            "   [%.2f %s] %s%n      EN : %s%n      mis: %s%n      aya: %s%n",
            p.suspeita(), p.motivo(), p.ep(), corta(p.en()), corta(p.mistral()), corta(p.aya())));
    }

    /**
     * Escore de 0 a ~1: quanto maior, mais as duas traduções discordam. Combina perda de
     * conteúdo (diferença de tamanho) com distância de vocabulário (palavras não comuns).
     */
    private static double suspeita(String en, String a, String b) {
        double maior = Math.max(a.length(), b.length());
        double perdaConteudo = maior == 0 ? 0 : Math.abs(a.length() - b.length()) / maior;

        Set<String> pa = palavras(a);
        Set<String> pb = palavras(b);
        Set<String> uniao = new HashSet<>(pa);
        uniao.addAll(pb);
        Set<String> comuns = new HashSet<>(pa);
        comuns.retainAll(pb);
        double distancia = uniao.isEmpty() ? 0 : 1.0 - ((double) comuns.size() / uniao.size());

        return Math.max(perdaConteudo, distancia * 0.85);
    }

    private static String motivo(String a, String b) {
        double maior = Math.max(a.length(), b.length());
        return maior > 0 && Math.abs(a.length() - b.length()) / maior > 0.5
            ? "perda de conteúdo" : "sentido divergente";
    }

    private static Set<String> palavras(String texto) {
        return new HashSet<>(Arrays.asList(texto.toLowerCase().split("[^\\p{L}\\p{N}]+")));
    }

    private static String escapar(String s) {
        return s.replace("|", "\\|").replace("\n", " ");
    }

    private static String corta(String s) {
        return s.length() > 74 ? s.substring(0, 74) + "…" : s;
    }

    private static String visivel(String texto) {
        return TAGS.matcher(texto == null ? "" : texto).replaceAll("")
            .replace("\\N", " ").replace("\\n", " ").trim();
    }

    private static Map<String, Path> porEpisodio(Path pasta) {
        Map<String, Path> m = new LinkedHashMap<>();
        try (var s = Files.list(pasta)) {
            s.filter(p -> p.toString().endsWith(".ass"))
             .filter(p -> !p.getFileName().toString().contains(".parcial."))
             .forEach(p -> {
                 var mm = Pattern.compile("(S\\d{2}E\\d{2})").matcher(p.getFileName().toString());
                 if (mm.find()) {
                     m.put(mm.group(1), p);
                 }
             });
        } catch (Exception e) {
            throw new IllegalStateException("falha ao listar " + pasta, e);
        }
        return m;
    }

    private static List<EventoLegenda> eventos(LeitorLegendaAss leitor, Path arquivo) {
        return leitor.ler(arquivo).eventos().stream().filter(EventoLegenda::isDialogo).toList();
    }
}
