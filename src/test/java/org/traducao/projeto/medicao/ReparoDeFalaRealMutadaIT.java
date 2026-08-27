package org.traducao.projeto.medicao;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.revisaoConcordancia.application.RevisarConcordanciaUseCase;
import org.traducao.projeto.revisaoConcordancia.domain.ResultadoConcordancia;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: responder <b>"o que a tela 3.3 conserta de verdade numa legenda?"</b> —
 * quebrando fala REAL do acervo de um jeito declarado e conferindo se a cadeia devolve o original.
 *
 * <h2>Por que este harness existe</h2>
 * Depois de 26/08/2026 o acervo está limpo: a 3.3 roda e corrige ZERO falas. Esse zero não
 * distingue <i>"o corretor é bom e já fez o trabalho"</i> de <i>"o corretor não enxerga nada"</i>.
 * É a regra 12 aplicada à própria tela — e a única forma de separar as duas leituras é dar a ela
 * um defeito que se sabe existir e ver se ele volta.
 *
 * <p>O {@code DiagnosticoCorretorConcordanciaIT} já faz isso com 22 frases escritas à mão. Aqui a
 * diferença é o material: <b>fala do acervo, com a tipografia dela</b> — tag {@code {\an8}},
 * quebra {@code \N}, nome de lore, estilo. Fixture escolhida por conveniência já escondeu defeito
 * de acervo nesta mesma tela antes (o veto de cartaz usava estilo {@code Sign} enquanto o acervo
 * usava {@code Titles}).
 *
 * <h2>A mutação, e por que é ESTA</h2>
 * Tirar o diacrítico de palavras que o têm: {@code não} → {@code nao}, {@code missão} →
 * {@code missao}. É o defeito real e dominante do acervo — o {@code aya-expanse-8b} devolve
 * português sem acento o tempo todo, e das 84 retraduções lidas em 26/08 <b>29 perderam acento</b>
 * que a fala anterior tinha. Não é um defeito inventado para o teste passar.
 *
 * <h2>Os dois números, e o segundo é o que importa</h2>
 * <pre>
 *   RECALL     das falas quebradas, quantas voltaram IDENTICAS ao original?
 *   ESTRAGO    das falas INTACTAS, quantas a cadeia mexeu sem precisar?
 * </pre>
 * O grupo intacto viaja no MESMO arquivo, misturado. Sem ele, "consertou 90%" poderia estar
 * acompanhado de estrago silencioso no resto — e é o resto que é a maior parte da legenda.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>NADA é escrito no acervo. A cópia mutada vive em pasta temporária, e é ela que a 3.3
 *       revisa. O acervo é só a fonte de texto.</li>
 *   <li>Quem corrige é o {@link RevisarConcordanciaUseCase} de PRODUÇÃO, pela porta pública, com
 *       arquivo em disco — não os cinco elos chamados um a um. Chamar os elos aqui seria escrever
 *       uma segunda versão da cadeia, e as duas divergiriam.</li>
 *   <li>Música e {@code Comment} ficam fora, como na tela.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Acervo ausente, nenhuma fala acentuada encontrada ou dicionário fora do ar terminam declarando
 * NÃO VERIFICADO — nenhum recall é afirmado.
 *
 * <p>Uso: {@code gradlew test --tests "*ReparoDeFalaRealMutadaIT*" "-Dkronos.medicao=true"
 * "-Dkronos.acervo=C:\animes\ANIMES-TESTES"}
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class ReparoDeFalaRealMutadaIT {

    /** Quantas falas vão para cada grupo. Amostra, não acervo: a cadeia custa ~8 ms por fala. */
    private static final int POR_GRUPO = 300;

    /**
     * O prefixo COMPLETO da linha, {@code "Dialogue: "} inclusive.
     *
     * <p>O {@link EscritorLegendaAss} concatena {@code prefixo + texto} cru, sem recompor nada. A
     * primeira versão deste harness escreveu o prefixo sem {@code "Dialogue: "}: o arquivo saiu
     * malformado, o leitor devolveu ZERO eventos, e os cinco elos da cadeia marcaram
     * <b>0 agiu · 0 intactas</b> — nem sequer viram uma fala. A guarda de casamento 1 para 1
     * pegou e recusou afirmar recall; sem ela o relatório teria dito "recall 0%" sobre um
     * corretor que nunca recebeu nada para corrigir.
     */
    private static final String PREFIXO = "Dialogue: 0,0:00:00.00,0:00:02.00,Default,,0,0,0,,";

    private static final Pattern TAG_ASS = Pattern.compile("\\{[^{}]*}");
    private static final Pattern ACENTUADA = Pattern.compile("[\\p{L}]*[áàâãéêíóôõúüçÁÀÂÃÉÊÍÓÔÕÚÜÇ][\\p{L}]*");

    @Inject
    LeitorLegendaAss leitor;

    @Inject
    EscritorLegendaAss escritor;

    @Inject
    PoliticaEstiloMusical politicaEstiloMusical;

    @Inject
    RevisarConcordanciaUseCase revisor;

    /**
     * PROPÓSITO DE NEGÓCIO: tira o diacrítico das palavras que o têm, preservando tudo o mais.
     *
     * <p>INVARIANTES DO DOMÍNIO: o que está DENTRO de {@code {...}} não é tocado — nome de fonte e
     * cor não são texto, e mutá-los mediria a robustez do parser, não o alcance do corretor.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto sem acento nenhum volta igual, e o chamador o
     * descarta em vez de contar como "quebrado".
     */
    private static String tirarAcento(String texto) {
        StringBuilder fora = new StringBuilder();
        Matcher tags = TAG_ASS.matcher(texto);
        int cursor = 0;
        while (tags.find()) {
            fora.append(semDiacritico(texto.substring(cursor, tags.start())));
            fora.append(tags.group());
            cursor = tags.end();
        }
        fora.append(semDiacritico(texto.substring(cursor)));
        return fora.toString();
    }

    private static String semDiacritico(String trecho) {
        return Normalizer.normalize(trecho, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
    }

    private boolean foraDoAlcance(EventoLegenda evento) {
        if ("Comment".equals(evento.tipoLinha())) {
            return true;
        }
        return evento.estilo() != null && politicaEstiloMusical.estiloIgnorado(evento.estilo());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CASO-CONTROLE (regra 9) da MUTAÇÃO — antes de medir o corretor, é
     * preciso saber que o defeito foi mesmo introduzido.
     *
     * <p>INVARIANTES DO DOMÍNIO: uma fala com acento tem de sair diferente, e o bloco de tags tem
     * de sobreviver. Uma mutação que não mudasse nada faria o recall dar 100% sem o corretor ter
     * feito coisa alguma — o instrumento mediria a si mesmo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: imprime e devolve {@code false}; nenhum recall é afirmado.
     */
    private static boolean mutacaoCalibrada() {
        String original = "{\\an8}Não é a missão que importa.";
        String mutado = tirarAcento(original);
        boolean quebrou = !mutado.equals(original)
            && mutado.contains("Nao e a missao");
        boolean preservaTag = mutado.startsWith("{\\an8}");
        boolean calaSemAcento = "Sem acento aqui.".equals(tirarAcento("Sem acento aqui."));
        if (quebrou && preservaTag && calaSemAcento) {
            System.out.println("  controle da mutacao: quebra a fala com acento · preserva a tag "
                + "· nao mexe na fala que ja nao tem acento");
            return true;
        }
        System.out.printf("MUTACAO REPROVADA NO CONTROLE — quebrou=%s tag=%s cala=%s. Nenhum "
            + "recall e afirmado.%n", quebrou, preservaTag, calaSemAcento);
        return false;
    }

    @Test
    @DisplayName("acervo: o que a 3.3 conserta quando a fala REAL e quebrada de proposito")
    void medir() throws IOException {
        System.out.println("\n=== REPARO DE FALA REAL MUTADA — o que a 3.3 devolve ===");
        if (!mutacaoCalibrada()) {
            return;
        }
        List<Path> pastas = AlcanceDaMedicao.pastasDeTraducao();
        if (pastas.isEmpty()) {
            return;
        }

        // Colhe falas reais: metade vai ser quebrada, metade viaja intacta no mesmo arquivo.
        List<String> comAcento = new ArrayList<>();
        List<String> semAcento = new ArrayList<>();
        DocumentoLegenda molde = null;
        for (Path pasta : pastas) {
            for (Path arquivo : AlcanceDaMedicao.arquivosEntregues(pasta)) {
                DocumentoLegenda doc;
                try {
                    doc = leitor.ler(arquivo);
                } catch (RuntimeException e) {
                    continue;
                }
                if (molde == null) {
                    molde = doc;
                }
                for (EventoLegenda ev : doc.eventos()) {
                    if (!ev.temTexto() || foraDoAlcance(ev) || ev.texto().isBlank()) {
                        continue;
                    }
                    if (ACENTUADA.matcher(ev.texto()).find()) {
                        if (comAcento.size() < POR_GRUPO) {
                            comAcento.add(ev.texto());
                        }
                    } else if (semAcento.size() < POR_GRUPO) {
                        semAcento.add(ev.texto());
                    }
                }
                if (comAcento.size() >= POR_GRUPO && semAcento.size() >= POR_GRUPO) {
                    break;
                }
            }
        }
        if (comAcento.isEmpty() || molde == null) {
            System.out.println("NAO VERIFICADO: nenhuma fala acentuada encontrada no acervo — sem "
                + "material nao ha o que quebrar, e recall 0 aqui seria cegueira.");
            return;
        }
        System.out.printf("  material: %d falas COM acento (serao quebradas) · %d SEM acento "
            + "(controle, viajam intactas)%n", comAcento.size(), semAcento.size());

        // Monta UM arquivo com os dois grupos misturados, em pasta temporaria.
        Path temporaria = Files.createTempDirectory("kronos-reparo-mutado");
        List<EventoLegenda> eventos = new ArrayList<>();
        Map<Integer, String> esperadoPorIndice = new LinkedHashMap<>();
        List<Integer> indicesDeControle = new ArrayList<>();
        int i = 0;
        for (String original : comAcento) {
            eventos.add(new EventoLegenda(i, "Dialogue", "Default",
                PREFIXO,tirarAcento(original)));
            esperadoPorIndice.put(i, original);
            i++;
        }
        for (String intacta : semAcento) {
            eventos.add(new EventoLegenda(i, "Dialogue", "Default",
                PREFIXO,intacta));
            esperadoPorIndice.put(i, intacta);
            indicesDeControle.add(i);
            i++;
        }
        Path arquivo = temporaria.resolve("mutado.ass");
        escritor.escrever(arquivo, new DocumentoLegenda(
            molde.cabecalho(), eventos, molde.quebraDeLinha(), molde.comBom()));

        // A CADEIA DE PRODUCAO, pela porta publica e com arquivo em disco.
        ResultadoConcordancia r = revisor.revisarPasta(temporaria, true);
        System.out.printf("%n  a 3.3 rodou: %d arquivo(s), %d fala(s) corrigida(s)%n",
            r.arquivosAnalisados(), r.falasCorrigidas());
        if (!r.revisorGramaticalDisponivel()) {
            System.out.printf("  AVISO: revisor gramatical indisponivel (%s) — o elo do POS tagger "
                + "nao participou desta medicao.%n", r.motivoRevisorIndisponivel());
        }

        System.out.println("  por elo da cadeia (agiu · absteve-se):");
        r.porCorretor().forEach(c -> System.out.printf("     %-32s %6d agiu · %6d intactas%n",
            c.nome(), c.agiu(), c.absteve()));
        System.out.printf("     %-32s %6d por MAIUSCULA · %6d por IDIOMA%n",
            "barradas pelas guardas", r.barradasPorMaiuscula(), r.barradasPorIdioma());

        // O CASAMENTO E POR POSICAO, e nao por indice. A primeira versao usava ev.indice(), e o
        // leitor REINDEXA ao reler o arquivo: os indices do documento relido nao sao os que eu
        // atribui ao escrever, entao a comparacao pegava a fala errada — e estourou num evento
        // sem texto. Escrevi na ordem, o escritor preserva a ordem, e e a ordem que vale.
        DocumentoLegenda depois = leitor.ler(arquivo);
        List<String> textosDepois = depois.eventos().stream()
            .filter(EventoLegenda::temTexto)
            .filter(ev -> !"Comment".equals(ev.tipoLinha()))
            .map(EventoLegenda::texto)
            .toList();
        if (textosDepois.size() != eventos.size()) {
            System.out.printf("NAO VERIFICADO: escrevi %d falas e reli %d — sem casamento 1 para 1 "
                + "nenhum recall e afirmado.%n", eventos.size(), textosDepois.size());
            return;
        }
        int restauradas = 0;
        int melhorouSemFechar = 0;
        int intocadas = 0;
        int estragadas = 0;
        List<String> amostraRestaurada = new ArrayList<>();
        List<String> amostraFaltando = new ArrayList<>();
        List<String> amostraEstrago = new ArrayList<>();
        Map<String, Integer> palavrasNaoRestauradas = new TreeMap<>();

        for (int pos = 0; pos < textosDepois.size(); pos++) {
            String esperado = esperadoPorIndice.get(pos);
            String saiu = textosDepois.get(pos);
            if (esperado == null || saiu == null) {
                continue;
            }
            if (indicesDeControle.contains(pos)) {
                if (!esperado.equals(saiu)) {
                    estragadas++;
                    if (amostraEstrago.size() < 6) {
                        amostraEstrago.add("      ANTES  " + corte(esperado)
                            + "\n      DEPOIS " + corte(saiu));
                    }
                }
                continue;
            }
            String mutado = tirarAcento(esperado);
            if (esperado.equals(saiu)) {
                restauradas++;
                if (amostraRestaurada.size() < 8) {
                    amostraRestaurada.add("      QUEBRADA " + corte(mutado)
                        + "\n      DEVOLVIDA " + corte(saiu));
                }
            } else if (!mutado.equals(saiu)) {
                melhorouSemFechar++;
            } else {
                intocadas++;
                if (amostraFaltando.size() < 8) {
                    amostraFaltando.add("      " + corte(mutado));
                }
                for (String p : acentuadasDe(esperado)) {
                    palavrasNaoRestauradas.merge(semDiacritico(p), 1, Integer::sum);
                }
            }
        }

        int quebradas = comAcento.size();
        System.out.printf("%n=== RECALL sobre %d falas REAIS quebradas ===%n", quebradas);
        System.out.printf("  devolvidas IDENTICAS ao original ... %4d  (%.1f%%)%n",
            restauradas, 100.0 * restauradas / quebradas);
        System.out.printf("  mexeu, mas nao fechou .............. %4d%n", melhorouSemFechar);
        System.out.printf("  nao tocou .......................... %4d%n", intocadas);
        System.out.printf("%n=== ESTRAGO sobre %d falas INTACTAS (grupo de controle) ===%n",
            indicesDeControle.size());
        System.out.printf("  alteradas sem precisar ............. %4d  <- quanto menor, melhor%n",
            estragadas);

        System.out.println("\n=== AMOSTRA: quebrada -> devolvida ===");
        amostraRestaurada.forEach(System.out::println);
        if (!amostraFaltando.isEmpty()) {
            System.out.println("\n=== AMOSTRA: quebrada e NAO tocada ===");
            amostraFaltando.forEach(System.out::println);
        }
        if (!amostraEstrago.isEmpty()) {
            System.out.println("\n=== AMOSTRA: fala INTACTA que a cadeia mexeu ===");
            amostraEstrago.forEach(System.out::println);
        }
        if (!palavrasNaoRestauradas.isEmpty()) {
            System.out.println("\n=== PALAVRAS QUE ELE NAO REACENTUOU (top 20) ===");
            palavrasNaoRestauradas.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .forEach(e -> System.out.printf("   %4d  %s%n", e.getValue(), e.getKey()));
        }

        // QUANTO FECHARIA se as INAMBIGUAS fossem tratadas. A pergunta importa porque a lista
        // acima mistura duas coisas muito diferentes:
        //
        //   `e`, `esta`, `sao`  -> AMBIGUAS: as duas formas existem em portugues ("e"/"é",
        //                          "esta"/"está"). O corretor se recusa, e faz bem — acentuar
        //                          por frequencia inverteria o sentido de frase correta.
        //   `nao`, `voce`       -> INAMBIGUAS: nao existem em portugues, forma nenhuma. A recusa
        //                          aqui vem da regra de sugestao UNICA do hunspell, que devolve
        //                          `nau`/`nas` junto com `não` e por isso barra.
        //   `magnolia`,`pleiades` -> LORE (San Magnolia, Pleiades). Nunca devem ser acentuadas.
        //
        // O numero abaixo diz quanto custa a segunda linha, e so ela.
        int fechariaSoComInambiguas = 0;
        for (Map.Entry<Integer, String> par : esperadoPorIndice.entrySet()) {
            if (indicesDeControle.contains(par.getKey())) {
                continue;
            }
            String saiu = textosDepois.get(par.getKey());
            if (saiu == null || par.getValue().equals(saiu)) {
                continue;
            }
            if (soFaltaInambigua(par.getValue(), saiu)) {
                fechariaSoComInambiguas++;
            }
        }
        System.out.printf("%n=== GANHO POSSIVEL: falas que fechariam so com as INAMBIGUAS ===%n");
        System.out.printf("  %s%n", String.join(", ", INAMBIGUAS.keySet()));
        System.out.printf("  fechariam .......................... %4d  (recall iria de %.1f%% "
            + "para %.1f%%)%n", fechariaSoComInambiguas,
            100.0 * restauradas / quebradas,
            100.0 * (restauradas + fechariaSoComInambiguas) / quebradas);

        try (var caminhos = Files.walk(temporaria)) {
            caminhos.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignorado) {
                    // pasta temporaria: falha ao limpar nao invalida a medicao
                }
            });
        }
    }

    private static List<String> acentuadasDe(String texto) {
        List<String> fora = new ArrayList<>();
        Matcher m = ACENTUADA.matcher(TAG_ASS.matcher(texto).replaceAll(" "));
        while (m.find()) {
            fora.add(m.group().toLowerCase(Locale.ROOT));
        }
        return fora;
    }

    /**
     * Formas sem acento que NÃO existem em português — nenhuma leitura, nenhum contexto. São as
     * únicas em que acentuar não pode inverter sentido.
     *
     * <p>Ficam DE FORA, de propósito: {@code e}/{@code é}, {@code esta}/{@code está},
     * {@code sao}/{@code são}, {@code a}/{@code à} — as duas formas existem, e escolher por
     * frequência trocaria o sentido de frase correta. E fica de fora tudo que é LORE
     * ({@code magnolia} de "San Magnolia", {@code pleiades}), que jamais se acentua.
     */
    private static final Map<String, String> INAMBIGUAS = Map.of(
        "nao", "não", "voce", "você", "voces", "vocês", "manha", "manhã",
        "ninguem", "ninguém", "alguem", "alguém", "porem", "porém", "tambem", "também");

    /**
     * PROPÓSITO DE NEGÓCIO: a fala fecharia se, e só se, o que falta forem palavras INAMBÍGUAS?
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer divergência fora das inambíguas devolve
     * {@code false} — o método nunca conta como "fecharia" uma fala que precisaria de mais.
     */
    private static boolean soFaltaInambigua(String esperado, String saiu) {
        String corrigido = saiu;
        for (Map.Entry<String, String> e : INAMBIGUAS.entrySet()) {
            corrigido = corrigido.replaceAll("(?<![\\p{L}])" + e.getKey() + "(?![\\p{L}])",
                Matcher.quoteReplacement(e.getValue()));
            String comMaiuscula = Character.toUpperCase(e.getKey().charAt(0)) + e.getKey().substring(1);
            String valorMaiusculo =
                Character.toUpperCase(e.getValue().charAt(0)) + e.getValue().substring(1);
            corrigido = corrigido.replaceAll("(?<![\\p{L}])" + comMaiuscula + "(?![\\p{L}])",
                Matcher.quoteReplacement(valorMaiusculo));
        }
        return esperado.equals(corrigido);
    }

    private static String corte(String t) {
        if (t == null) {
            return "(nulo)";
        }
        String s = t.replace("\n", " ");
        return s.length() > 84 ? s.substring(0, 84) + "..." : s;
    }
}
