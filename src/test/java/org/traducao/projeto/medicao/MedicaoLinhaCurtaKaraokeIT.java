package org.traducao.projeto.medicao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.traducaoKaraoke.application.ClassificadorLetraKaraokeService;
import org.traducao.projeto.traducaoKaraoke.domain.ClasseLinhaKaraoke;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: medir, contra o acervo REAL, o furo que ainda resta na regra de Paulo
 * <b>"jamais mexe no japonês, só no inglês"</b> — quantas linhas de letra com POUCAS palavras o
 * classificador ainda manda traduzir. É o formato exato que produziu o prejuízo do Guilty Crown
 * em 07/08/2026 (15 de 15 alterações erradas, {@code yasashikatta} → "era legal").
 *
 * <h2>O prejuízo que originou</h2>
 * A guarda {@code tudoSilabavelSemIngles} ({@code 6eeb4b0e}) fechou o caso conhecido, mas a regra
 * 5 do método exige varrer a MESMA CLASSE de falha nos outros fluxos antes de dar por fechado. A
 * classe é "amostra curta decide errado", e o que sobra dela no acervo nunca foi contado. Sem
 * este número, "o algoritmo está bom" seria opinião.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Estritamente READ-ONLY: nada aqui escreve no acervo, só em {@code build/}.</li>
 *   <li>Consulta o classificador de PRODUÇÃO e o leitor de PRODUÇÃO. Reimplementar o critério
 *       aqui repetiria o erro de 8× de {@link MedicaoDivergenciaPadraoMusicalIT} — a estimativa
 *       feita à mão em PowerShell deu 11.165 onde o objeto de produção deu 1.385.</li>
 *   <li>O corte de "palavras" usa o MESMO texto visível que o classificador usa
 *       ({@code extrairTextoVisivel}) e a mesma divisão {@code [^a-z]+} — contar de outro jeito
 *       mediria uma linha diferente da que o classificador decidiu.</li>
 *   <li>Travado por {@code -Dkronos.medicao=true}: lê o acervo real, ausente no CI.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Acervo inacessível faz o teste FALHAR com o caminho na mensagem, e zero falas analisadas
 * também — resultado vazio por instrumento cego não pode ser lido como "não há furo"
 * (regra 8: resultado zero é hipótese, nunca conclusão).
 */
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoLinhaCurtaKaraokeIT {

    private static final Path ACERVO =
        Path.of(System.getProperty("kronos.acervo", "C:\\animes"));

    /** Pastas de legenda que interessam: entrada bruta E saída traduzida (o corretor roda na segunda). */
    private static final Set<String> PASTAS_DE_LEGENDA = Set.of(
        "legendas_eng", "legendas_originais", "legendas_extraidas_ass", "traducao_ptbr");

    /** Mesma remoção de tags do classificador — a linha medida tem de ser a linha decidida. */
    private static final Pattern PADRAO_REMOVE_TAGS_ASS = Pattern.compile("\\{[^}]*\\}");

    private final DetectorEfeitoKaraokeService detector = new DetectorEfeitoKaraokeService();
    private final ClassificadorLetraKaraokeService classificador =
        new ClassificadorLetraKaraokeService(detector);
    private final LeitorLegendaAss leitor = new LeitorLegendaAss();

    /**
     * PROPÓSITO DE NEGÓCIO: percorre o acervo, pergunta ao classificador de produção a classe de
     * cada evento e cruza com o número de palavras visíveis, isolando as linhas curtas que ainda
     * seriam enviadas ao LLM.
     */
    @Test
    @DisplayName("mede quantas linhas CURTAS de musica o classificador ainda manda traduzir")
    void medeLinhasCurtasTraduziveis() throws IOException {
        assertTrue(Files.isDirectory(ACERVO),
            "acervo inacessivel em " + ACERVO + " — sem ele o resultado vazio significaria "
                + "\"nao consegui medir\", nunca \"nao ha furo\"");

        List<Path> arquivos = listarLegendas();

        Map<ClasseLinhaKaraoke, long[]> porClasse = new EnumMap<>(ClasseLinhaKaraoke.class);
        // [1 palavra, 2 palavras, 3 palavras, 4+ palavras]
        Map<String, long[]> curtasTraduziveisPorEstilo = new TreeMap<>();
        Set<String> amostraCurtas = new LinkedHashSet<>();
        List<String> detalhado = new ArrayList<>();

        long totalEventos = 0;
        long arquivosIlegiveis = 0;
        long curtasTraduziveis = 0;

        for (Path arquivo : arquivos) {
            DocumentoLegenda documento;
            try {
                documento = leitor.ler(arquivo);
            } catch (RuntimeException e) {
                arquivosIlegiveis++;
                continue;
            }
            for (EventoLegenda evento : documento.eventos()) {
                if (!evento.isDialogo() || !evento.temTexto()) {
                    continue;
                }
                totalEventos++;
                ClasseLinhaKaraoke classe = classificador.classificar(evento.estilo(), evento.texto());
                int palavras = contarPalavrasVisiveis(evento.texto());
                long[] faixa = porClasse.computeIfAbsent(classe, k -> new long[4]);
                faixa[Math.min(palavras <= 0 ? 0 : palavras - 1, 3)]++;

                if (classe == ClasseLinhaKaraoke.TRADUZIVEL_INGLES && palavras > 0 && palavras <= 2) {
                    curtasTraduziveis++;
                    curtasTraduziveisPorEstilo
                        .computeIfAbsent(evento.estilo() == null ? "(sem estilo)" : evento.estilo(),
                            k -> new long[1])[0]++;
                    String visivel = textoVisivel(evento.texto());
                    if (amostraCurtas.add(visivel.toLowerCase(Locale.ROOT))) {
                        detalhado.add(String.format(Locale.ROOT, "%-22s | %-30s | %s",
                            truncar(evento.estilo(), 22), truncar(visivel, 30),
                            ACERVO.relativize(arquivo)));
                    }
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== LINHAS CURTAS DE MUSICA QUE O CLASSIFICADOR MANDA TRADUZIR ===\n");
        sb.append("acervo: ").append(ACERVO).append('\n');
        sb.append("arquivos .ass lidos: ").append(arquivos.size() - arquivosIlegiveis)
            .append(" (ilegiveis: ").append(arquivosIlegiveis).append(")\n");
        sb.append("eventos de dialogo analisados: ").append(totalEventos).append("\n\n");

        sb.append("--- distribuicao por CLASSE x numero de palavras visiveis ---\n");
        sb.append(String.format(Locale.ROOT, "%-22s %10s %10s %10s %10s%n",
            "classe", "1 palavra", "2", "3", "4+"));
        for (ClasseLinhaKaraoke c : ClasseLinhaKaraoke.values()) {
            long[] f = porClasse.getOrDefault(c, new long[4]);
            sb.append(String.format(Locale.ROOT, "%-22s %10d %10d %10d %10d%n",
                c.name(), f[0], f[1], f[2], f[3]));
        }

        sb.append("\n--- O FURO: TRADUZIVEL_INGLES com 1 ou 2 palavras ---\n");
        sb.append("total de linhas: ").append(curtasTraduziveis).append('\n');
        sb.append("textos distintos: ").append(amostraCurtas.size()).append('\n');
        sb.append("estilos envolvidos: ").append(curtasTraduziveisPorEstilo.size()).append("\n\n");
        curtasTraduziveisPorEstilo.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
            .forEach(e -> sb.append(String.format(Locale.ROOT, "%-32s %8d%n", e.getKey(), e.getValue()[0])));

        sb.append("\n--- amostra (texto distinto, estilo, arquivo) ---\n");
        detalhado.stream().limit(200).forEach(l -> sb.append(l).append('\n'));

        escrever("medicao-linha-curta-karaoke.txt", sb.toString());
        System.out.println(sb);

        assertFalse(totalEventos == 0,
            "zero eventos analisados — o instrumento nao encontrou legenda em " + ACERVO
                + ", entao nao mediu nada e o resultado NAO significa ausencia de furo");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CALIBRAÇÃO do instrumento (regra 9). Um caso-controle montado à mão,
     * cuja resposta é conhecida, tem de sair pelo classificador com a classe esperada — senão a
     * medição acima estaria contando com um instrumento que não enxerga.
     *
     * <p>Este teste NÃO substitui {@code JamaisMexerNoJaponesTest}: lá a pergunta é "o
     * comportamento está correto?"; aqui é "o instrumento de medição está vivo?".
     */
    @Test
    @DisplayName("calibracao: o classificador ainda separa ingles de romaji nos casos conhecidos")
    void calibracaoDoInstrumento() {
        assertTrue(classificador.classificar("ED Roma L1", "{\\blur3\\fad(200,2000)}yasashikatta")
                == ClasseLinhaKaraoke.ORIGINAL_JAPONES,
            "caso-controle DOENTE do Guilty Crown: se este virar traduzivel, a guarda caiu");
        assertTrue(classificador.classificar("Opening", "No matter how hard I wish, nothing ever changes")
                == ClasseLinhaKaraoke.TRADUZIVEL_INGLES,
            "caso-controle SAO do 86: se este parar de ser traduzivel, o instrumento cegou e a "
                + "medicao acima contaria zero por nao enxergar, nao por nao haver");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mede a REGRESSÃO da guarda — quantas linhas que o corretor já traduziu
     * (e portanto classificou como camada inglesa) hoje seriam preservadas. Toda guarda que
     * preserva mais tem um custo, e o custo tem de ser um número, não uma suposição.
     *
     * <p>INVARIANTES DO DOMÍNIO: a fonte da verdade do "foi traduzido antes" é o cache de karaokê
     * gravado pelo próprio use case ({@code cache/karaoke}), lido pelos tipos do projeto via
     * {@link LeitorAcervoCache} — nunca por um parser de JSON montado aqui.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: cache ausente faz o teste falhar em vez de reportar zero.
     */
    @Test
    @DisplayName("mede a regressao: linhas ja traduzidas que hoje seriam preservadas")
    void medeRegressaoDaGuarda() throws IOException {
        Path raizCache = LeitorAcervoCache.raizPadrao().resolve("karaoke");
        assertTrue(Files.isDirectory(raizCache),
            "cache de karaoke ausente em " + raizCache + " — sem ele nao ha como saber o que o "
                + "corretor ja traduziu, e zero regressao seria conclusao sem instrumento");

        LeitorAcervoCache.Acervo acervo = LeitorAcervoCache.ler(raizCache);

        Map<ClasseLinhaKaraoke, Long> hoje = new EnumMap<>(ClasseLinhaKaraoke.class);
        List<String> regredidas = new ArrayList<>();
        Set<String> distintas = new LinkedHashSet<>();

        for (LeitorAcervoCache.FalaDoAcervo fala : acervo.falas()) {
            ClasseLinhaKaraoke classe =
                classificador.classificar(fala.entrada().estilo(), fala.original());
            hoje.merge(classe, 1L, Long::sum);
            if (classe != ClasseLinhaKaraoke.TRADUZIVEL_INGLES) {
                String visivel = textoVisivel(fala.original());
                if (distintas.add(visivel.toLowerCase(Locale.ROOT))) {
                    regredidas.add(String.format(Locale.ROOT, "%-16s | %-24s | %-34s -> %s",
                        classe.name(), truncar(fala.entrada().estilo(), 24),
                        truncar(visivel, 34), truncar(textoVisivel(fala.traduzido()), 40)));
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== REGRESSAO: o que o corretor JA traduziu, reclassificado HOJE ===\n");
        sb.append("cache lido: ").append(raizCache).append('\n');
        sb.append("arquivos: ").append(acervo.arquivosLidos())
            .append(" (ilegiveis: ").append(acervo.arquivosIlegiveis()).append(")\n");
        sb.append("entradas ja traduzidas: ").append(acervo.falas().size()).append("\n\n");
        sb.append("--- classe HOJE de cada entrada que JA foi traduzida ---\n");
        for (ClasseLinhaKaraoke c : ClasseLinhaKaraoke.values()) {
            sb.append(String.format(Locale.ROOT, "%-22s %8d%n", c.name(), hoje.getOrDefault(c, 0L)));
        }
        sb.append("\nNAO seriam mais traduzidas (textos distintos): ").append(distintas.size())
            .append("\nATENCAO: parte disto e o PROPOSITO da guarda — as 15 do Guilty Crown estao aqui\n")
            .append("e sao ACERTO, nao regressao. Regressao e o que aparece com traducao PT-BR boa.\n\n");
        regredidas.stream().limit(300).forEach(l -> sb.append(l).append('\n'));

        escrever("medicao-regressao-guarda-karaoke.txt", sb.toString());
        System.out.println(sb);

        assertFalse(acervo.falas().isEmpty(),
            "zero entradas lidas do cache — instrumento cego, nao ausencia de regressao");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aplica a regra 5 do método — achou um defeito, varra a MESMA CLASSE
     * nos outros fluxos. A fatia {@code traducao} blinda desenho vetorial antes do LLM
     * ({@code SeletorEventosTraduziveis}: "1. Blindagem Contra Lixo Vetorial Absoluto"), usando
     * {@code ProtecaoLegendaAssService.temDesenhoVetorial}. O corretor de karaokê NÃO consulta
     * essa blindagem — e a medição de linha curta já pescou um caso real no 08th MS Team:
     * {@code Song ENG | m 0 970 l 1920 970 1920 1080 …}, que são coordenadas de forma, não texto.
     *
     * <p>Este teste conta o tamanho do buraco. Traduzir coordenada destrói o desenho, e o texto
     * "traduzido" volta como frase — dano irreversível no arquivo de saída.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: acervo ausente faz falhar; zero eventos analisados
     * também, pelo mesmo motivo do teste principal.
     */
    @Test
    @DisplayName("mede desenho vetorial que o corretor de karaoke mandaria ao LLM")
    void medeDesenhoVetorialTraduzivel() throws IOException {
        assertTrue(Files.isDirectory(ACERVO), "acervo inacessivel em " + ACERVO);
        ProtecaoLegendaAssService protecao = new ProtecaoLegendaAssService();

        // CALIBRACAO (regra 9): o instrumento tem de reprovar um caso doente montado a mao,
        // senao "zero desenhos" seria indistinguivel de "nao sei enxergar desenho".
        assertTrue(protecao.temDesenhoVetorial("{\\p1}m 0 970 l 1920 970{\\p0}"),
            "instrumento cego: nao reconhece nem o desenho vetorial explicito");
        assertFalse(protecao.temDesenhoVetorial("{\\blur3}yasashikatta"),
            "instrumento grosseiro: acusa desenho onde ha texto comum");

        Map<ClasseLinhaKaraoke, Long> desenhosPorClasse = new EnumMap<>(ClasseLinhaKaraoke.class);
        List<String> iriamAoLlm = new ArrayList<>();
        long totalDesenhos = 0;

        for (Path arquivo : listarLegendas()) {
            DocumentoLegenda documento;
            try {
                documento = leitor.ler(arquivo);
            } catch (RuntimeException e) {
                continue;
            }
            for (EventoLegenda evento : documento.eventos()) {
                if (!evento.isDialogo() || !evento.temTexto()
                    || !protecao.temDesenhoVetorial(evento.texto())) {
                    continue;
                }
                totalDesenhos++;
                ClasseLinhaKaraoke classe =
                    classificador.classificar(evento.estilo(), evento.texto());
                desenhosPorClasse.merge(classe, 1L, Long::sum);
                if (classe == ClasseLinhaKaraoke.TRADUZIVEL_INGLES && iriamAoLlm.size() < 60) {
                    iriamAoLlm.add(String.format(Locale.ROOT, "%-22s | %-44s | %s",
                        truncar(evento.estilo(), 22), truncar(textoVisivel(evento.texto()), 44),
                        ACERVO.relativize(arquivo)));
                }
            }
        }

        long aoLlm = desenhosPorClasse.getOrDefault(ClasseLinhaKaraoke.TRADUZIVEL_INGLES, 0L);
        StringBuilder sb = new StringBuilder();
        sb.append("=== DESENHO VETORIAL (\\p1) NO CORRETOR DE KARAOKE ===\n");
        sb.append("eventos com desenho vetorial: ").append(totalDesenhos).append("\n\n");
        for (ClasseLinhaKaraoke c : ClasseLinhaKaraoke.values()) {
            sb.append(String.format(Locale.ROOT, "%-22s %8d%n", c.name(),
                desenhosPorClasse.getOrDefault(c, 0L)));
        }
        sb.append("\nIRIAM AO LLM (TRADUZIVEL_INGLES): ").append(aoLlm).append('\n');
        sb.append("A fatia traducao bloqueia estes; o corretor de karaoke nao consulta a blindagem.\n\n");
        iriamAoLlm.forEach(l -> sb.append(l).append('\n'));

        escrever("medicao-desenho-vetorial-karaoke.txt", sb.toString());
        System.out.println(sb);

        assertFalse(totalDesenhos == 0,
            "zero desenhos vetoriais em todo o acervo — improvavel; instrumento cego, "
                + "nao ausencia (a calibracao acima passou, entao suspeite da varredura)");
    }

    private List<Path> listarLegendas() throws IOException {
        try (Stream<Path> arquivos = Files.walk(ACERVO)) {
            return arquivos
                .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ass"))
                .filter(p -> p.getParent() != null
                    && PASTAS_DE_LEGENDA.contains(p.getParent().getFileName().toString()))
                .toList();
        }
    }

    /**
     * Conta palavras da MESMA forma que {@code classificarPorEvidenciaDeTexto}: sobre o texto
     * visível, quebrando em {@code [^a-z]+} depois de minusculizar. Contar de outro jeito mediria
     * uma linha diferente da que o classificador decidiu.
     */
    private static int contarPalavrasVisiveis(String texto) {
        int n = 0;
        for (String p : textoVisivel(texto).toLowerCase(Locale.ROOT).split("[^a-z]+")) {
            if (!p.isEmpty()) {
                n++;
            }
        }
        return n;
    }

    private static String textoVisivel(String texto) {
        if (texto == null) {
            return "";
        }
        return PADRAO_REMOVE_TAGS_ASS.matcher(texto)
            .replaceAll("")
            .replace("\\N", " ")
            .replace("\\n", " ")
            .replace("\\h", " ")
            .strip();
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
