package org.traducao.projeto.auditoria;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: mede o defeito que TODAS as outras métricas do projeto deixam passar —
 * a tradução fluente que diz outra coisa. Especificamente a forma mais grave já observada:
 * <b>pergunta que vira afirmação</b>, que troca o falante da cena.
 *
 * <h2>Por que este instrumento precisou existir</h2>
 * Eco, resíduo em inglês e pendência medem FALHA VISÍVEL, e no Unicorn as três deram
 * praticamente zero (eco 181 de 5.455, e quase todo ele nome próprio; resíduo 0; pendências 2).
 * Um artefato que passa em todas elas ainda pode estar errado: {@code Kamille?} virando
 * "Sim, Kamille." e {@code Are you all right?!} virando "Estou bem!" são português perfeito,
 * passam em qualquer validador, e inverteram quem fala. O piso medido no Zeta foi de
 * <b>297 falas (1,82%)</b>, e era PISO — ninguém sabia o número real porque não havia medida.
 *
 * <h2>O critério, e por que ele é mecânico</h2>
 * O original termina em {@code ?} e a tradução não. Interrogação é estrutura, não estilo: o
 * português tem pergunta para toda pergunta do inglês. Quando ela some, ou a fala virou
 * resposta (o defeito), ou a pontuação foi perdida (defeito menor, mas ainda defeito).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Música, karaokê e estilos da lista nominal ficam de fora, pelos vetos de PRODUÇÃO.</li>
 *   <li>Só conta fala com texto visível — cartela e letreiro sem conteúdo não entram.</li>
 *   <li>Nunca afirma "defeito": afirma "perdeu a interrogação". A leitura de cada caso continua
 *       humana; o instrumento só reduz 5.455 falas a uma lista curta.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem acervo, PULA por {@link Assumptions}. Traz controle positivo: o detector é exercitado
 * contra o caso doente conhecido antes de qualquer contagem valer.
 */
@DisplayName("medição: pergunta do original que virou afirmação na tradução")
class MedicaoPerguntaQueViraAfirmacaoIT {

    private static final Path OBRA = Path.of("C:", "animes",
        "Mobile Suit Gundam Unicorn Re0096 (2016) [Season 1] [BD 1080p HEVC OPUS] [Dual-Audio]",
        "Gundam Unicorn Season 1");
    private static final Path ENTRADA = Path.of("backups", "troca_tipo_legenda_20260812_113344");
    private static final Pattern TAGS = Pattern.compile("\\{[^}]*\\}");

    private record Caso(String ep, String en, String pt) {}

    @Test
    @DisplayName("conta e lista, nas três versões, as perguntas que perderam a interrogação")
    void perguntasQuePerderamAInterrogacao() {
        Assumptions.assumeTrue(Files.isDirectory(ENTRADA) && Files.isDirectory(OBRA),
            "acervo do Unicorn ausente — NÃO VERIFICADO");

        // CONTROLE POSITIVO com o caso doente REAL, antes de qualquer numero valer.
        assertTrue(perdeuInterrogacao("Kamille?", "Sim, Kamille."),
            "instrumento cego: nao reconheceu o caso que originou a medicao");
        assertTrue(perdeuInterrogacao("Are you all right?!", "Estou bem!"),
            "instrumento cego: nao reconheceu a troca de falante do Zeta");
        // E o controle NEGATIVO: traducao correta de pergunta NAO pode ser acusada.
        assertTrue(!perdeuInterrogacao("Are you all right?", "Você está bem?"),
            "alarme falso: pergunta traduzida como pergunta nao e defeito");
        assertTrue(!perdeuInterrogacao("I will protect you.", "Eu vou proteger você."),
            "alarme falso: fala declarativa nao entra nesta medicao");

        LeitorLegendaAss leitor = new LeitorLegendaAss();
        var detector = new DetectorEfeitoKaraokeService();
        var politica = new PoliticaEstiloMusical(MedicaoUnicornMistralXAyaIT.estilosIgnoradosDoYml());

        Map<String, List<Caso>> achados = new LinkedHashMap<>();

        // Cada versão contra A SUA entrada. A terceira nasceu do arquivo já achatado, que tem
        // 2.898 eventos a menos: medi-la contra o original por índice comparou a fala i com a
        // fala j e produziu 77,7% — um número inteiramente inventado, cujos "defeitos" eram
        // pares que nada tinham a ver entre si ("How is it?" → "Banagher!"). O desalinhamento
        // é convincente justamente porque produz absurdos que PARECEM erro grave de tradução.
        Map<String, Path> ENG_ORIGINAL = porEpisodio(ENTRADA);
        Map<String, Path> ENG_ACHATADA = porEpisodio(OBRA.resolve("legendas_extraidas_ass"));

        for (String versao : new String[] {"traducao_mistral", "traducao_aya", "traducao_ptbr"}) {
            Path pasta = OBRA.resolve(versao);
            if (!Files.isDirectory(pasta)) {
                continue;
            }
            Map<String, Path> eng = "traducao_ptbr".equals(versao) ? ENG_ACHATADA : ENG_ORIGINAL;
            Map<String, Path> saida = porEpisodio(pasta);
            List<Caso> casos = new ArrayList<>();
            int perguntas = 0;
            for (String ep : eng.keySet().stream().filter(saida::containsKey).sorted().toList()) {
                List<EventoLegenda> o = eventos(leitor, eng.get(ep));
                List<EventoLegenda> t = eventos(leitor, saida.get(ep));
                for (int i = 0; i < Math.min(o.size(), t.size()); i++) {
                    String en = o.get(i).texto();
                    String pt = t.get(i).texto();
                    if (en == null || pt == null) {
                        continue;
                    }
                    if (detector.podeSerCamadaMusical(o.get(i).estilo(), en)
                        || detector.eEfeitoKaraoke(en)
                        || politica.estiloIgnorado(o.get(i).estilo())) {
                        continue;
                    }
                    if (visivel(en).contains("?")) {
                        perguntas++;
                    }
                    if (perdeuInterrogacao(en, pt)) {
                        casos.add(new Caso(ep, visivel(en), visivel(pt)));
                    }
                }
            }
            achados.put(versao, casos);
            System.out.printf("%n=== %s: %d de %d perguntas perderam a interrogação (%.1f%%) ===%n",
                versao, casos.size(), perguntas, perguntas == 0 ? 0.0 : 100.0 * casos.size() / perguntas);
            casos.stream().limit(12).forEach(c ->
                System.out.printf("   %s  EN: \"%s\"%n            PT: \"%s\"%n", c.ep(), c.en(), c.pt()));
        }

        // assumeTrue e NAO assertTrue: nenhuma versao presente significa NAO VERIFICADO, e o
        // controle positivo la em cima ja provou que o instrumento enxerga. Reprovar aqui
        // confundiria "as pastas sairam do lugar" com "o detector cegou" — foi o que derrubou a
        // suite em 13/08, quando as saidas do Unicorn foram movidas.
        Assumptions.assumeTrue(!achados.isEmpty(),
            "nenhuma versao traduzida do Unicorn em disco — NAO VERIFICADO");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o original pergunta e a tradução não. Exige texto visível dos dois
     * lados para não acusar cartela vazia, e ignora o caso em que a tradução é idêntica ao
     * original (isso é eco, medido por outro instrumento — contar duas vezes o mesmo defeito
     * em duas métricas infla o diagnóstico).
     */
    // package-private: MedicaoZetaMistralXAyaIT aplica o MESMO critério a outra obra. Reimplementar
    // produziria uma segunda verdade que divergiria desta — o erro que a regra de medição proíbe.
    static boolean perdeuInterrogacao(String en, String pt) {
        String a = visivel(en);
        String b = visivel(pt);
        if (a.isEmpty() || b.isEmpty() || a.equals(b)) {
            return false;
        }
        // CONTÉM, não "termina com": o controle positivo reprovou a primeira versão deste
        // método porque "Are you all right?!" termina em "!" — e é justamente um dos dois casos
        // que originaram a medição. Exigir a interrogação no fim deixaria de fora toda pergunta
        // exclamativa, que em legenda de anime é a maioria das perguntas alteradas.
        return a.contains("?") && !b.contains("?");
    }

    private static String visivel(String texto) {
        return TAGS.matcher(texto == null ? "" : texto).replaceAll("")
            .replace("\\N", " ").replace("\\n", " ").trim();
    }

    private static Map<String, Path> porEpisodio(Path pasta) {
        // Pasta ausente devolve VAZIO em vez de lancar: o acervo e material de trabalho e muda de
        // lugar. Harness de medicao que REPROVA por acervo ausente confunde 'nao verifiquei' com
        // 'esta errado' — e foi o que aconteceu em 13/08, quando as pastas do Unicorn sairam do
        // lugar e dois ITs derrubaram a suite inteira.
        if (pasta == null || !Files.isDirectory(pasta)) {
            return new LinkedHashMap<>();
        }
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

