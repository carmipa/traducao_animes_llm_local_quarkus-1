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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: mede acentuação obrigatória que faltou no português entregue. Nasceu de
 * um achado da auditoria de 12/08/2026, na MESMA fala do Unicorn E02:
 *
 * <pre>
 *   mistral : "É perigoso. Não deveria estar falando com você nessas circunstâncias."
 *   aya     : "Isso e perigoso. Eu não deveria estar falando com você nessas circunstancias."
 * </pre>
 *
 * <p>Os dois dizem a mesma coisa e os dois passam em eco, resíduo, pendência, pergunta e
 * negação. Um está ortograficamente correto e o outro não. Sem esta medição, a escolha de
 * modelo estava sendo feita cega para um eixo em que os dois <b>não</b> empatam.
 *
 * <h2>Como o critério evita o dicionário</h2>
 * Só entram palavras cuja forma SEM acento não existe em português — {@code nao},
 * {@code voce}, {@code tambem}, {@code circunstancias} — e as terminações inequívocas
 * ({@code -cao}, {@code -oes}). Nada de {@code esta}/{@code está} ou {@code e}/{@code é}, que
 * são pares legítimos e exigiriam analisar a frase: contá-los encheria o resultado de falso
 * positivo, e guarda que reprova o certo ensina a desligar o alarme.
 *
 * <p>Consequência assumida: o número é um PISO. O {@code "Isso e perigoso"} do exemplo acima
 * não é contado, porque {@code e} existe. Piso declarado vale mais que total inflado.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem acervo, PULA por {@link Assumptions}.
 */
@DisplayName("medição: acentuação obrigatória faltando no português entregue")
class MedicaoAcentuacaoFaltanteIT {

    private static final Path OBRA = Path.of("C:", "animes",
        "Mobile Suit Gundam Unicorn Re0096 (2016) [Season 1] [BD 1080p HEVC OPUS] [Dual-Audio]",
        "Gundam Unicorn Season 1");
    private static final Pattern TAGS = Pattern.compile("\\{[^}]*\\}");

    /**
     * Palavras cuja forma sem acento NÃO existe em português. Deliberadamente conservadora:
     * qualquer par ambíguo ({@code esta}/{@code está}, {@code e}/{@code é}, {@code so}/{@code só},
     * {@code ja}/{@code já}) fica de fora, mesmo custando cobertura.
     */
    private static final Pattern SEM_ACENTO = Pattern.compile(
        "(?i)\\b(nao|voce|voces|tambem|entao|alguem|ninguem|porem|alem|atraves|apos|"
            + "possivel|impossivel|dificil|facil|util|inutil|"
            + "ultimo|ultima|proximo|proxima|unico|unica|otimo|otima|pessimo|"
            + "familia|historia|memoria|gloria|vitoria|ilusao|"
            + "circunstancia|circunstancias|experiencia|experiencias|ciencia|consciencia|"
            + "importancia|distancia|substancia|ambulancia|infancia|"
            + "irmao|irmaos|irma|irmas|coracao|razao|situacao|informacao|"
            // "avo" e "ate" saem: "avo" é medida musical e "ate" é do verbo atar. Ficam só as
            // inequívocas. E o "ja[a-z]" da primeira versão era lixo — casou "jaz", que é o
            // verbo jazer, três vezes no mistral. Padrão que eu não sei explicar não entra.
            + "seculo|nucleo|midia|tres)\\b");

    /** Terminações que em português SEMPRE levam til: -ção, -ções, -ões, -ão de substantivo. */
    private static final Pattern TERMINACAO_SEM_TIL = Pattern.compile(
        "(?i)\\b\\w{3,}(?:cao|coes|goes|soes)\\b");

    @Test
    @DisplayName("conta palavras sem acento obrigatório nas três versões")
    void acentuacaoFaltante() {
        Assumptions.assumeTrue(Files.isDirectory(OBRA), "acervo ausente — NÃO VERIFICADO");

        // CONTROLE POSITIVO com o caso REAL que originou a medição.
        assertTrue(temFalta("nessas circunstancias."), "instrumento cego: nao viu 'circunstancias'");
        assertTrue(temFalta("Eu nao sei."), "instrumento cego: nao viu 'nao'");
        assertTrue(temFalta("uma situacao dificil"), "instrumento cego: nao viu terminacao -cao");
        // CONTROLE NEGATIVO: texto CORRETO nao pode ser acusado, e par ambiguo fica fora.
        assertFalse(temFalta("Não deveria estar falando com você nessas circunstâncias."),
            "alarme falso: texto acentuado corretamente");
        assertFalse(temFalta("Esta e a casa dele."),
            "alarme falso: 'esta' e 'e' sao pares ambiguos e ficam FORA de proposito");
        assertTrue(temFalta("Ele foi para a estacao? Nao."),
            "controle: 'estacao' e 'Nao' DEVEM contar");
    }

    @Test
    @DisplayName("varredura das três versões do Unicorn")
    void varrerVersoes() {
        Assumptions.assumeTrue(Files.isDirectory(OBRA), "acervo ausente — NÃO VERIFICADO");

        LeitorLegendaAss leitor = new LeitorLegendaAss();
        var detector = new DetectorEfeitoKaraokeService();
        var politica = new PoliticaEstiloMusical(MedicaoUnicornMistralXAyaIT.estilosIgnoradosDoYml());

        for (String versao : new String[] {"traducao_mistral", "traducao_aya", "traducao_ptbr"}) {
            Path pasta = OBRA.resolve(versao);
            if (!Files.isDirectory(pasta)) {
                continue;
            }
            int falas = 0;
            int comFalta = 0;
            Map<String, Integer> ranking = new TreeMap<>();
            List<Path> arquivos;
            try (var s = Files.list(pasta)) {
                arquivos = s.filter(p -> p.toString().endsWith(".ass"))
                    .filter(p -> !p.getFileName().toString().contains(".parcial.")).toList();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            for (Path arq : arquivos) {
                for (EventoLegenda ev : leitor.ler(arq).eventos()) {
                    if (!ev.isDialogo() || ev.texto() == null) {
                        continue;
                    }
                    if (detector.podeSerCamadaMusical(ev.estilo(), ev.texto())
                        || detector.eEfeitoKaraoke(ev.texto())
                        || politica.estiloIgnorado(ev.estilo())) {
                        continue;
                    }
                    String v = visivel(ev.texto());
                    if (v.isEmpty()) {
                        continue;
                    }
                    falas++;
                    boolean achou = false;
                    Matcher m1 = SEM_ACENTO.matcher(v);
                    while (m1.find()) {
                        ranking.merge(m1.group().toLowerCase(), 1, Integer::sum);
                        achou = true;
                    }
                    Matcher m2 = TERMINACAO_SEM_TIL.matcher(v);
                    while (m2.find()) {
                        ranking.merge(m2.group().toLowerCase(), 1, Integer::sum);
                        achou = true;
                    }
                    if (achou) {
                        comFalta++;
                    }
                }
            }
            System.out.printf("%n=== %s: %d de %d falas com acentuação faltando (%.1f%%) ===%n",
                versao, comFalta, falas, falas == 0 ? 0.0 : 100.0 * comFalta / falas);
            ranking.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue()).limit(12)
                .forEach(e -> System.out.printf("   %-20s %d%n", e.getKey(), e.getValue()));
        }
    }

    private static boolean temFalta(String texto) {
        return SEM_ACENTO.matcher(texto).find() || TERMINACAO_SEM_TIL.matcher(texto).find();
    }

    private static String visivel(String texto) {
        return TAGS.matcher(texto == null ? "" : texto).replaceAll("")
            .replace("\\N", " ").replace("\\n", " ").trim();
    }
}
