package org.traducao.projeto.medicao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.traducaoKaraoke.application.ClassificadorLetraKaraokeService;
import org.traducao.projeto.traducaoKaraoke.application.PlanoDeClassificacao;
import org.traducao.projeto.traducaoKaraoke.domain.ClasseLinhaKaraoke;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: medir, contra o acervo REAL e pelo MESMO plano de classificação que a
 * produção usa ({@link PlanoDeClassificacao#montar}), o calcanhar de Aquiles do 4.1 relatado por
 * Paulo em 21/09/2026 — a MISTURA romaji + inglês + a linha de karaokê traduzida. Responde a duas
 * perguntas com número:
 * <ol>
 *   <li>quantos FRAGMENTOS (1-2 palavras) o classificador ainda manda ao LLM como
 *       {@code TRADUZIVEL_INGLES} — a fonte do lixo {@code hol}→"Olá", {@code on}→"começando",
 *       {@code dnt}→meta-resposta;</li>
 *   <li>quantas linhas traduzíveis EMPILHARIAM {@code ingles\Nportugues} redundante por não
 *       encontrarem camada romaji no mesmo instante ({@code temOriginalPreservadaNoInstante}).</li>
 * </ol>
 *
 * <h2>Por que pelo PLANO e não pelo {@code classificar} de 2 args</h2>
 * {@link MedicaoLinhaCurtaKaraokeIT} usa o atalho de 2 argumentos, que não recebe os sinais
 * cruzados do arquivo ({@code silabaDeFraseIrma}, {@code comRomaji}). A produção decide pelo plano,
 * então medir pelo atalho mediria uma classe diferente da que sai no {@code .ass}. Aqui a chave da
 * medição é a chave da COISA (regra da medição).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Estritamente READ-ONLY: só lê o acervo e escreve em {@code build/}.</li>
 *   <li>Consulta o classificador, o leitor e o plano de PRODUÇÃO — nada reimplementado.</li>
 *   <li>Alcance pelo dono único {@link AlcanceDaMedicao} (honra {@code -Dkronos.medicao.obra}).</li>
 *   <li>Travado por {@code -Dkronos.medicao=true}: lê o acervo real, ausente no CI.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Acervo ausente ou zero eventos analisados FALHA com o caminho na mensagem — resultado vazio por
 * instrumento cego não é "não há mistura" (regra 8).
 */
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MisturaKaraokeDiagnosticoIT {

    private static final Pattern REMOVE_TAGS = Pattern.compile("\\{[^}]*\\}");

    private final DetectorEfeitoKaraokeService detector = new DetectorEfeitoKaraokeService();
    private final ClassificadorLetraKaraokeService classificador =
        new ClassificadorLetraKaraokeService(detector);
    private final LeitorLegendaAss leitor = new LeitorLegendaAss();

    @Test
    @DisplayName("mede a mistura romaji+ingles+PT do 4.1 pelo plano de producao")
    void medeMisturaPeloPlano() throws IOException {
        List<Path> pastas = AlcanceDaMedicao.pastasDeTraducao();
        assertFalse(pastas.isEmpty(),
            "nenhuma pasta traducao_ptbr no alcance — sem acervo o vazio significaria "
                + "\"nao medi\", nunca \"nao ha mistura\". Passe -Dkronos.acervo e/ou -Dkronos.medicao.obra");

        // Calibracao (regra 9): o instrumento tem de separar ingles de romaji nos casos conhecidos.
        assertTrue(classificador.classificar("Opening", "No matter how hard I wish, nothing ever changes")
                == ClasseLinhaKaraoke.TRADUZIVEL_INGLES,
            "caso-controle SAO (86): se parar de ser traduzivel, o instrumento cegou");
        assertTrue(classificador.classificar("ED Roma L1", "{\\blur3}yasashikatta")
                == ClasseLinhaKaraoke.ORIGINAL_JAPONES,
            "caso-controle DOENTE (Guilty Crown): romaji nao pode virar traduzivel");

        Map<ClasseLinhaKaraoke, long[]> porClasse = new EnumMap<>(ClasseLinhaKaraoke.class);
        // TRADUZIVEL por numero de palavras visiveis: [1, 2, 3, 4+]
        long[] traduzivelPorPalavras = new long[4];
        // TRADUZIVEL que EMPILHARIA (sem romaji no instante) x que troca no lugar (com romaji)
        long empilharia = 0;
        long trocaNoLugar = 0;
        // fragmentos distintos (<=2 palavras) que iriam ao LLM
        Map<String, Integer> fragmentosDistintos = new LinkedHashMap<>();
        Map<String, long[]> porEstilo = new TreeMap<>();
        // TRADUZIVEL com >=3 palavras, distinto, por estilo — para ver se romaji (que deveria ser
        // preservado) esta sendo mandado a traducao (o "romaji que mistura ingles" do Paulo).
        Map<String, java.util.LinkedHashSet<String>> traduzivelLongoPorEstilo = new TreeMap<>();

        long arquivos = 0;
        long ilegiveis = 0;
        long eventos = 0;

        for (Path pasta : pastas) {
            List<Path> assFiles;
            try (Stream<Path> s = Files.list(pasta)) {
                assFiles = s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ass"))
                    .sorted()
                    .toList();
            }
            for (Path arquivo : assFiles) {
                DocumentoLegenda doc;
                try {
                    doc = leitor.ler(arquivo);
                } catch (RuntimeException e) {
                    ilegiveis++;
                    continue;
                }
                arquivos++;
                PlanoDeClassificacao plano = PlanoDeClassificacao.montar(doc, classificador);
                List<EventoLegenda> evs = doc.eventos();
                for (int i = 0; i < evs.size(); i++) {
                    EventoLegenda ev = evs.get(i);
                    if (!ev.isDialogo() || !ev.temTexto()) {
                        continue;
                    }
                    eventos++;
                    ClasseLinhaKaraoke classe = plano.classeNaPosicao(i);
                    porClasse.computeIfAbsent(classe, k -> new long[1])[0]++;
                    if (classe != ClasseLinhaKaraoke.TRADUZIVEL_INGLES) {
                        continue;
                    }
                    int palavras = palavras(ev.texto());
                    traduzivelPorPalavras[Math.min(palavras <= 0 ? 0 : palavras - 1, 3)]++;
                    String estilo = ev.estilo() == null ? "(sem estilo)" : ev.estilo();
                    long[] pe = porEstilo.computeIfAbsent(estilo, k -> new long[2]);
                    if (plano.temOriginalPreservadaNoInstante(ev)) {
                        trocaNoLugar++;
                        pe[0]++;
                    } else {
                        empilharia++;
                        pe[1]++;
                    }
                    if (palavras > 0 && palavras <= 2) {
                        String vis = visivel(ev.texto()).toLowerCase(Locale.ROOT);
                        fragmentosDistintos.merge(vis, 1, Integer::sum);
                    } else if (palavras >= 3) {
                        traduzivelLongoPorEstilo
                            .computeIfAbsent(estilo, k -> new java.util.LinkedHashSet<>())
                            .add(visivel(ev.texto()));
                    }
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== 4.1 MISTURA romaji+ingles+PT — medido pelo PLANO de producao ===\n");
        sb.append("acervo: ").append(AlcanceDaMedicao.RAIZ)
            .append(" | filtro obra: \"").append(AlcanceDaMedicao.FILTRO_OBRA).append("\"\n");
        sb.append("pastas traducao_ptbr: ").append(pastas.size())
            .append(" | arquivos .ass: ").append(arquivos)
            .append(" (ilegiveis ").append(ilegiveis).append(")\n");
        sb.append("eventos de dialogo: ").append(eventos).append("\n\n");

        sb.append("--- distribuicao por CLASSE ---\n");
        for (ClasseLinhaKaraoke c : ClasseLinhaKaraoke.values()) {
            sb.append(String.format(Locale.ROOT, "%-22s %10d%n",
                c.name(), porClasse.getOrDefault(c, new long[1])[0]));
        }

        sb.append("\n--- TRADUZIVEL_INGLES por numero de palavras visiveis ---\n");
        sb.append(String.format(Locale.ROOT, "1 palavra: %d | 2: %d | 3: %d | 4+: %d%n",
            traduzivelPorPalavras[0], traduzivelPorPalavras[1],
            traduzivelPorPalavras[2], traduzivelPorPalavras[3]));

        sb.append("\n--- (1) O VAZAMENTO: fragmentos (<=2 palavras) distintos que iriam ao LLM ---\n");
        sb.append("distintos: ").append(fragmentosDistintos.size()).append('\n');
        fragmentosDistintos.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(120)
            .forEach(e -> sb.append(String.format(Locale.ROOT, "  %5dx  %s%n", e.getValue(),
                e.getKey().length() > 60 ? e.getKey().substring(0, 59) + "…" : e.getKey())));

        sb.append("\n--- (2) EMPILHAMENTO: TRADUZIVEL que empilha ingles\\Npt (sem romaji no instante) ---\n");
        sb.append("EMPILHARIA (redundante/risco de mistura na tela): ").append(empilharia).append('\n');
        sb.append("troca no lugar (tem romaji irmao no instante):    ").append(trocaNoLugar).append("\n\n");
        sb.append("--- por estilo: [empilha, troca-no-lugar] ---\n");
        porEstilo.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue()[1] + b.getValue()[0],
                a.getValue()[1] + a.getValue()[0]))
            .limit(40)
            .forEach(e -> sb.append(String.format(Locale.ROOT, "  %-34s empilha=%-8d troca=%d%n",
                truncar(e.getKey(), 34), e.getValue()[1], e.getValue()[0])));

        sb.append("\n--- (3) TRADUZIVEL >=3 palavras por estilo (romaji aqui = mistranslation) ---\n");
        traduzivelLongoPorEstilo.forEach((est, textos) -> {
            sb.append(String.format(Locale.ROOT, "  [%s] %d distintos:%n", est, textos.size()));
            textos.stream().limit(6).forEach(t -> sb.append("      ")
                .append(t.length() > 80 ? t.substring(0, 79) + "…" : t).append('\n'));
        });

        escrever("mistura-karaoke-diagnostico.txt", sb.toString());
        System.out.println(sb);

        assertFalse(eventos == 0,
            "zero eventos analisados — instrumento cego, nao ausencia de mistura");
    }

    private int palavras(String texto) {
        int n = 0;
        for (String p : visivel(texto).toLowerCase(Locale.ROOT).split("[^a-z]+")) {
            if (!p.isEmpty()) {
                n++;
            }
        }
        return n;
    }

    private static String visivel(String texto) {
        if (texto == null) {
            return "";
        }
        return REMOVE_TAGS.matcher(texto).replaceAll("")
            .replace("\\N", " ").replace("\\n", " ").replace("\\h", " ").strip();
    }

    private static String truncar(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static void escrever(String nome, String conteudo) throws IOException {
        Path saida = Path.of("build", nome);
        Files.createDirectories(saida.getParent());
        Files.writeString(saida, conteudo);
    }
}
