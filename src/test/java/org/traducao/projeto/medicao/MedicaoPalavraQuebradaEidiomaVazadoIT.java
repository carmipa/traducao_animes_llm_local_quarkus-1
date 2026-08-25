package org.traducao.projeto.medicao;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;
import org.traducao.projeto.core.texto.dicionarioOrtografia.VeredictoPalavra;
import org.traducao.projeto.revisaoConcordancia.application.CorretorAcentoDeDicionarioNaFalaService;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * PROPÓSITO DE NEGÓCIO: as duas classes de defeito que a tela 3.3 <b>não conserta e nem deve</b> —
 * a palavra que o modelo inventou e o idioma que vazou — contadas por obra, para virarem decisão.
 *
 * <h2>Por que RELATÓRIO e não conserto</h2>
 * <ul>
 *   <li><b>Palavra quebrada</b> ({@code futila}, {@code esmagrado}, {@code capitaneis}): consertar
 *       exige adivinhar o que o modelo quis dizer. Trocar por palpite é inventar legenda, que é
 *       pior do que o defeito — o leitor tropeça uma vez em {@code esmagrado} e desconfia; não
 *       tropeça nunca numa palavra plausível e errada.</li>
 *   <li><b>Idioma vazado</b> ({@code "Seis mươi, setenta"}): precisa de TRADUÇÃO, não de troca de
 *       caractere. É trabalho de modelo, não de regra.</li>
 * </ul>
 *
 * <h2>O que separa nome próprio de palavra quebrada</h2>
 * O dicionário reprova as duas: {@code Kamille} e {@code esmagrado} saem ambas como
 * {@link VeredictoPalavra#DESCONHECIDA}. O discriminador aqui é a <b>capitalização fora do início
 * de frase</b> — o mesmo que protege a lore no corretor de acento da 3.3. Não é perfeito, e por
 * isso os dois baldes são impressos SEPARADOS em vez de um número único: o operador vê o que a
 * régua separou, e não uma soma que esconde a dúvida.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>READ-ONLY. Mede e imprime.</li>
 *   <li>Quem julga a palavra é o dicionário de PRODUÇÃO, pela porta. Nenhuma lista escrita à mão.</li>
 *   <li>Música fora, pela {@link PoliticaEstiloMusical}; {@code .parcial} fora.</li>
 *   <li>Dicionário indisponível termina como NÃO VERIFICADO, sem afirmar número.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Acervo ausente termina declarando, sem número.
 *
 * <p>Uso: {@code gradlew test --tests "*MedicaoPalavraQuebradaEidiomaVazadoIT*" "-Dkronos.medicao=true"}
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoPalavraQuebradaEidiomaVazadoIT {

    private static final Path RAIZ = Path.of(System.getProperty("kronos.acervo", "C:\\animes"));
    /**
     * Filtro OPCIONAL por obra ({@code -Dkronos.medicao.obra=0080}).
     *
     * <p>A primeira versao deste harness NAO tinha filtro, e o custo apareceu na hora: pedi uma
     * prova rapida numa obra de seis arquivos e ele varreu o acervo inteiro, estourando o tempo
     * sem me dizer nada. Provar o instrumento numa obra antes de solta-lo em 222 arquivos custa
     * trinta segundos — e e o passo que este projeto ja pulou uma vez, produzindo meia hora de
     * lixo convincente.
     */
    private static final String FILTRO_OBRA = System.getProperty("kronos.medicao.obra", "");

    private static final Pattern TAG_ASS = Pattern.compile("\\{[^{}]*}");
    private static final Pattern PALAVRA = Pattern.compile("[\\p{L}][\\p{L}'-]*");

    /**
     * Caractere que não pertence a nenhum alfabeto que este acervo usa. Hiragana, katakana e
     * kanji ficam de FORA da lista: eles aparecem em karaokê legítimo, e a política de música já
     * os exclui por outro caminho. O que interessa aqui é o alfabeto latino ESTENDIDO de idiomas
     * que ninguém traduziu — o {@code ư} e o {@code ơ} do vietnamita são o caso conhecido.
     *
     * <p>As vogais com MACRON ({@code ā ē ī ō ū}) ficam de fora por dois motivos somados: são
     * romaji legítimo ({@code Ryūko}, {@code Ōsaka}) e as de {@code a}/{@code o} já têm dono — o
     * quinto elo da 3.3 troca por til quando o dicionário aprova. Marcá-las aqui contaria duas
     * vezes o mesmo defeito e faria o relatório parecer maior do que é.
     */
    private static final Pattern LATINO_ESTRANHO = Pattern.compile(
        "[\\u0100-\\u024F&&[^\\u0100\\u0101\\u0112\\u0113\\u012A\\u012B\\u014C\\u014D\\u016A\\u016B]]");

    @Inject
    LeitorLegendaAss leitor;

    @Inject
    PoliticaEstiloMusical politicaEstiloMusical;

    @Inject
    CorretorOrtograficoLegenda dicionario;

    /** Uma palavra suspeita com uma fala de exemplo, para a leitura humana decidir. */
    private record Suspeita(String palavra, String obra, String fala) {}

    @Test
    @DisplayName("acervo: palavra que o modelo quebrou e idioma que vazou, por obra")
    void medir() throws IOException {
        System.out.printf("%n=== PALAVRA QUEBRADA E IDIOMA VAZADO em %s ===%n", RAIZ);
        if (!Files.isDirectory(RAIZ)) {
            System.out.println("NAO VERIFICADO: acervo ausente em " + RAIZ);
            return;
        }

        // CONTROLE, antes de qualquer numero: o dicionario precisa ser visto REPROVANDO uma
        // palavra inventada e APROVANDO uma correta. Instrumento que nao foi visto reprovando
        // pode estar aprovando por cegueira, e ai o zero do acervo nao significa nada.
        Map<String, VeredictoPalavra> controle =
            dicionario.classificar(new LinkedHashSet<>(List.of("esmagrado", "batalha")));
        VeredictoPalavra doente = controle.get("esmagrado");
        VeredictoPalavra sao = controle.get("batalha");
        if (doente == null || sao == null || doente == VeredictoPalavra.NAO_VERIFICADO
            || doente == VeredictoPalavra.PORTUGUES_OK || sao != VeredictoPalavra.PORTUGUES_OK) {
            System.out.printf("NAO VERIFICADO: instrumento reprovado no controle — "
                + "'esmagrado'=%s (deveria NAO ser portugues), 'batalha'=%s (deveria ser). "
                + "Nenhum numero do acervo vale.%n", doente, sao);
            return;
        }
        System.out.printf("  controle: 'esmagrado'=%s · 'batalha'=%s%n", doente, sao);

        List<Path> pastas = new ArrayList<>();
        try (Stream<Path> s = Files.walk(RAIZ)) {
            s.filter(Files::isDirectory)
                .filter(d -> d.getFileName().toString().equals("traducao_ptbr"))
                .filter(d -> FILTRO_OBRA.isBlank()
                    || d.getParent().getFileName().toString().toLowerCase()
                        .contains(FILTRO_OBRA.toLowerCase()))
                .sorted(Comparator.comparing(Path::toString))
                .forEach(pastas::add);
        }

        if (pastas.isEmpty()) {
            System.out.printf("NAO VERIFICADO: nenhuma pasta casou com o filtro '%s'. "
                + "Zero aqui seria zero por cegueira, entao nenhum numero e afirmado.%n",
                FILTRO_OBRA);
            return;
        }
        if (!FILTRO_OBRA.isBlank()) {
            System.out.printf("  FILTRO por obra: \"%s\" -> %d pasta(s)%n", FILTRO_OBRA, pastas.size());
        }

        Map<String, List<Suspeita>> vazadoPorObra = new TreeMap<>();

        // DUAS PASSADAS SOBRE O MESMO DADO, e a segunda so acontece depois de a primeira acabar.
        //
        // No inicio de frase, maiuscula nao distingue nada: "Impeda que ele seja lancado!" e
        // "Misha, voce pode aquecer" comecam igual, e uma e palavra quebrada e a outra e nome de
        // personagem. Decidir ali, na hora, e cara ou coroa — e foi o que a primeira versao fez,
        // mandando `Impeda` para o balde dos nomes e escondendo o defeito.
        //
        // A evidencia que resolve esta no CORPUS: um nome de personagem aparece capitalizado no
        // MEIO de alguma fala, mais cedo ou mais tarde. Uma palavra quebrada, nunca. Entao a
        // primeira passada junta os nomes confirmados do acervo inteiro, e so depois cada suspeita
        // e julgada contra esse conjunto.
        Set<String> nomesConfirmados = new LinkedHashSet<>();
        List<Suspeita> suspeitas = new ArrayList<>();
        int arquivos = 0;
        int falas = 0;

        for (Path pasta : pastas) {
            String obra = pasta.getParent().getFileName().toString();
            List<Path> arqs;
            try (Stream<Path> s = Files.list(pasta)) {
                arqs = s.filter(a -> a.getFileName().toString().toLowerCase().endsWith(".ass"))
                    .filter(a -> !a.getFileName().toString().toLowerCase().contains(".parcial."))
                    .sorted().toList();
            }
            for (Path arq : arqs) {
                DocumentoLegenda doc;
                try {
                    doc = leitor.ler(arq);
                } catch (RuntimeException e) {
                    System.out.printf("  ILEGIVEL (contado como NAO VERIFICADO): %s — %s%n",
                        arq.getFileName(), e);
                    continue;
                }
                arquivos++;
                // Um lote por ARQUIVO: perguntar palavra a palavra custaria um processo externo
                // por fala, que foi exatamente o defeito medido em 24/08/2026 na tela 3.3.
                Map<String, Suspeita> candidatas = new LinkedHashMap<>();
                Set<String> nomesDoArquivo = new LinkedHashSet<>();
                for (EventoLegenda ev : doc.eventos()) {
                    if (!ev.temTexto() || politicaEstiloMusical.estiloIgnorado(ev.estilo())) {
                        continue;
                    }
                    falas++;
                    String visivel = TAG_ASS.matcher(ev.texto()).replaceAll(" ")
                        .replace("\\N", " ").replace("\\n", " ");
                    if (LATINO_ESTRANHO.matcher(visivel).find()) {
                        vazadoPorObra.computeIfAbsent(obra, k -> new ArrayList<>())
                            .add(new Suspeita(arq.getFileName().toString(), obra, visivel.trim()));
                    }
                    // Quem e nome proprio NESTA fala quem diz e a producao. Olhar so a primeira
                    // letra jogaria "Impeda que ele seja lancado!" no balde dos nomes: e
                    // maiuscula por ABRIR FRASE, e e uma palavra quebrada de verdade.
                    Set<String> nomes = CorretorAcentoDeDicionarioNaFalaService
                        .nomesPropriosNoMeioDaFala(ev.texto());
                    Matcher m = PALAVRA.matcher(visivel);
                    while (m.find()) {
                        String p = m.group();
                        if (p.length() < 4) {
                            continue;
                        }
                        if (nomes.contains(p)) {
                            nomesDoArquivo.add(p);
                        }
                        candidatas.putIfAbsent(p, new Suspeita(p, obra, visivel.trim()));
                    }
                }
                if (candidatas.isEmpty()) {
                    continue;
                }
                Map<String, VeredictoPalavra> vereditos =
                    dicionario.classificar(new LinkedHashSet<>(candidatas.keySet()));
                nomesConfirmados.addAll(nomesDoArquivo);
                for (Map.Entry<String, Suspeita> e : candidatas.entrySet()) {
                    if (vereditos.get(e.getKey()) == VeredictoPalavra.DESCONHECIDA) {
                        suspeitas.add(e.getValue());
                    }
                }
            }
        }

        System.out.printf("%n  alcance ...... %d arquivos · %d falas (sem musica, sem .parcial)%n",
            arquivos, falas);
        System.out.printf("  nomes proprios confirmados pelo corpus .... %d%n",
            nomesConfirmados.size());

        Map<String, List<Suspeita>> quebradasPorObra = new TreeMap<>();
        Map<String, List<Suspeita>> nomesPorObra = new TreeMap<>();
        for (Suspeita sus : suspeitas) {
            boolean eNome = nomesConfirmados.contains(sus.palavra());
            (eNome ? nomesPorObra : quebradasPorObra)
                .computeIfAbsent(sus.obra(), k -> new ArrayList<>()).add(sus);
        }
        imprimir("FRENTE C — PALAVRA QUEBRADA: nenhum dicionario reconhece e o corpus"
            + " nunca a mostrou como nome. Sobra nome raro que so aparece abrindo frase (no 0080, Lumunba).",
            quebradasPorObra, Integer.MAX_VALUE);
        imprimir("FRENTE D — IDIOMA VAZADO (caractere latino de idioma nao traduzido)",
            vazadoPorObra, 6);
        imprimir("NOME PROPRIO confirmado pelo corpus (aparece capitalizado no MEIO de alguma fala)",
            nomesPorObra, 4);
    }

    /** Imprime um balde por obra, com amostra limitada e o total sempre visível. */
    private static void imprimir(String titulo, Map<String, List<Suspeita>> porObra, int amostra) {
        System.out.printf("%n=== %s ===%n", titulo);
        if (porObra.isEmpty()) {
            System.out.println("  nenhuma ocorrencia — e o instrumento passou no controle acima");
            return;
        }
        int total = porObra.values().stream().mapToInt(List::size).sum();
        System.out.printf("  TOTAL: %d ocorrencias em %d obra(s)%n", total, porObra.size());
        for (Map.Entry<String, List<Suspeita>> e : porObra.entrySet()) {
            List<Suspeita> lista = e.getValue();
            System.out.printf("%n  --- %s (%d)%n", e.getKey(), lista.size());
            Set<String> vistas = new LinkedHashSet<>();
            for (Suspeita s : lista) {
                if (vistas.size() >= amostra) {
                    System.out.printf("      ... e mais %d nao listadas%n",
                        lista.size() - vistas.size());
                    break;
                }
                if (!vistas.add(s.palavra())) {
                    continue;
                }
                System.out.printf("      %-22s | %s%n", s.palavra(),
                    s.fala().length() > 96 ? s.fala().substring(0, 96) : s.fala());
            }
        }
    }
}
