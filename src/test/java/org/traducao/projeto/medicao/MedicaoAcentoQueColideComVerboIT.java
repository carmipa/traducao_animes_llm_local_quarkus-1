package org.traducao.projeto.medicao;

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

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;
import org.traducao.projeto.core.texto.dicionarioOrtografia.VeredictoPalavra;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * PROPÓSITO DE NEGÓCIO: medir, no acervo inteiro, a classe de falta de acento que TODO
 * instrumento por dicionário é cego para enxergar — a palavra sem acento que, sem o acento,
 * <b>também é uma palavra do português</b>, normalmente uma forma verbal.
 *
 * <h2>O prejuízo que originou (2026-08-23), e a frase do Paulo que nomeia a classe</h2>
 * Paulo pediu a verificação do Macross II: <i>"a concordância está bem ruim naquele anime"</i>.
 * Os episódios 1 e 2 têm <b>12,5% das falas com defeito</b> — e a tela 3.3 devolveu, com razão,
 * "NADA A CORRIGIR": só existe UM caso da classe dela nos dois arquivos. O que está ruim é
 * acento, e ele passa por três peneiras sem ser visto:
 *
 * <pre>
 *   "A milicia ordenou um blackout de noticias"    milicia, noticias
 *   "Estabeleça a linha de frente na orbita"       orbita
 *   "você ganhou o premio para a história"         premio
 *   "Um ponto valido. O corpo pode temer a morte"  valido
 * </pre>
 *
 * O {@code NormalizadorAcentosComuns} não as tem na lista nominal (166 pares, quase toda
 * {@code -ao}/{@code -cao}). E o {@code CorretorAcentoPorDicionario}, que alcançaria qualquer
 * palavra, <b>só age no que o dicionário REJEITA</b> — e o hunspell ACEITA todas elas, porque
 * {@code ele noticia}, {@code o satélite orbita}, {@code eu premio} e {@code eu valido} são
 * conjugações legítimas. Confirmado no {@code pt_BR.dic}: {@code futila/B} está lá.
 *
 * <p>Paulo descreveu exatamente isto: <i>"muitas vezes ficou invertido o verbo e substantivo"</i>.
 * Onde a inversão acontece, a existência da palavra deixa de ser evidência de nada.
 *
 * <h2>A evidência que substitui o dicionário: o DETERMINANTE</h2>
 * {@code a noticia} não pode ser verbo — artigo não rege verbo. É a mesma forma de raciocínio da
 * tela 3.3, que também decide por determinante + substantivo. Onde não há determinante, este
 * instrumento <b>não opina</b>: {@code ele noticia o fato} nunca entra na conta, por construção.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>READ-ONLY. Mede e imprime; não escreve no acervo.</li>
 *   <li>Mesmo universo dos harnesses irmãos: sem música (pela {@link PoliticaEstiloMusical}, que
 *       é a dona da pergunta) e sem {@code .parcial}.</li>
 *   <li><b>Quem decide se a forma acentuada existe é o dicionário de produção</b>, pela porta —
 *       nenhuma lista de acentos escrita à mão aqui.</li>
 *   <li>Só entra a palavra com <b>exatamente UMA</b> forma acentuada conhecida. Duas ou mais é
 *       ambiguidade real e sai em lista separada, para leitura humana, nunca para correção.</li>
 *   <li>Controle positivo e negativo no MESMO experimento; se o instrumento errar qualquer um,
 *       nenhum número do acervo é afirmado.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Dicionário indisponível ou sonda sem discriminar termina declarando isso e sem afirmar número.
 *
 * <p>Uso: {@code gradlew test --tests "*MedicaoAcentoQueColideComVerboIT*" "-Dkronos.medicao=true"}
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoAcentoQueColideComVerboIT {

    private static final Path RAIZ = Path.of(System.getProperty("kronos.acervo", "C:\\animes"));
    private static final int AMOSTRAS = 3;
    private static final int BLOCO = 3000;

    @Inject
    LeitorLegendaAss leitor;

    @Inject
    PoliticaEstiloMusical politicaEstiloMusical;

    @Inject
    CorretorOrtograficoLegenda dicionario;

    /**
     * Determinantes que provam substantivo. O {@code a}/{@code as} fica FORA: também é
     * preposição, e foi por confundir os dois que o corretor de concordância estragou 14 falas
     * de "graças a Deus" em 19/08/2026. O {@code esta}/{@code estas} também fica fora — é o
     * verbo "está" sem acento, que é justamente o defeito que se está medindo.
     */
    private static final Set<String> DETERMINANTES = Set.of(
        "o", "os", "um", "uns", "este", "estes", "esse", "esses", "aquele", "aqueles",
        "do", "dos", "no", "nos", "ao", "aos", "pelo", "pelos", "num", "nuns",
        "meu", "meus", "seu", "seus", "nosso", "nossos",
        "uma", "umas", "essa", "essas", "aquela", "aquelas",
        "da", "das", "na", "nas", "pela", "pelas", "numa", "numas",
        "minha", "minhas", "sua", "suas", "nossa", "nossas", "à", "às");

    private static final Pattern PAR = Pattern.compile(
        "(?<![\\p{L}\\p{N}])(\\p{L}+)\\s+(\\p{L}{4,})(?![\\p{L}\\p{N}])",
        Pattern.UNICODE_CHARACTER_CLASS);

    private static final Pattern TAG = Pattern.compile("\\{[^{}]*}");

    /**
     * As trocas de UMA letra que produzem acento em português. Uma só por palavra: palavra do
     * português tem no máximo um acento gráfico, e permitir duas explodiria o espaço de busca
     * com formas que o dicionário teria de recusar uma a uma.
     */
    private static final Map<Character, String> ACENTOS = new LinkedHashMap<>();

    static {
        ACENTOS.put('a', "áàâã");
        ACENTOS.put('e', "éê");
        ACENTOS.put('i', "í");
        ACENTOS.put('o', "óôõ");
        ACENTOS.put('u', "úü");
        ACENTOS.put('c', "ç");
    }

    /**
     * O TREMA entra por decisão de Paulo (2026-08-23), e o que a medição mostrou fica escrito
     * aqui porque muda o que se deve OLHAR no relatório, não se o {@code ü} entra.
     *
     * <pre>
     *   lingüiça  freqüente  tranqüilo  agüentar   -> o pt_BR.dic REJEITA (Acordo de 1990)
     *   linguiça  frequente  tranquilo  aguentar   -> ACEITA
     *   Müller    Bündchen                         -> ACEITA (nome estrangeiro manteve o trema)
     * </pre>
     *
     * <p>Consequência prática: em palavra comum o {@code ü} é <b>inerte</b> — a forma
     * pré-reforma não está no dicionário, então a variante nasce e morre na mesma consulta.
     * Onde ele age é em <b>nome próprio estrangeiro</b>: {@code Muller} vira {@code Müller},
     * e isso é renomear pessoa, que é o território da lore.
     *
     * <p>Por isso o trema não é proibido aqui, é <b>SEPARADO</b>: toda correção que produz
     * {@code ü} sai numa seção própria do relatório, para leitura humana, e nunca some no meio
     * das correções de palavra comum. Lacuna conhecida é permitida; lacuna silenciosa não.
     *
     * <p>E o caso que faz lembrar do trema já estava coberto por outra letra: {@code linguica}
     * vira {@code linguiça} pelo {@code c -> ç}, sem trema nenhum.
     */
    private static final char TREMA = 'ü';

    private record Ocorrencia(String determinante, String palavra, String obra, String fala) {}

    @Test
    @DisplayName("mede o acento que colide com forma verbal — a classe cega do dicionario")
    void medir() throws IOException {
        System.out.printf("%n=== ACENTO QUE COLIDE COM VERBO — acervo %s ===%n", RAIZ);
        if (!Files.isDirectory(RAIZ)) {
            System.out.println("NAO VERIFICADO: acervo ausente em " + RAIZ);
            return;
        }

        // ---------- sonda: o dicionario discrimina? ----------
        Map<String, VeredictoPalavra> sonda = dicionario.classificar(
            new LinkedHashSet<>(List.of("notícia", "noticia", "xkcdqwzp")));
        boolean discrimina = dicionario.disponivel()
            && sonda.get("notícia") == VeredictoPalavra.PORTUGUES_OK
            && sonda.get("xkcdqwzp") != VeredictoPalavra.PORTUGUES_OK;
        if (!discrimina) {
            System.out.printf("NAO VERIFICADO: dicionario nao discriminou na sonda "
                + "(notícia=%s, noticia=%s, xkcdqwzp=%s). Nenhum numero afirmado.%n",
                sonda.get("notícia"), sonda.get("noticia"), sonda.get("xkcdqwzp"));
            return;
        }
        System.out.printf("  sonda: notícia=%s  noticia=%s  xkcdqwzp=%s%n",
            sonda.get("notícia"), sonda.get("noticia"), sonda.get("xkcdqwzp"));

        // ---------- 1. varredura ----------
        List<Ocorrencia> ocorrencias = new ArrayList<>();
        int falas = 0;
        List<Path> obras;
        try (Stream<Path> s = Files.list(RAIZ)) {
            obras = s.filter(Files::isDirectory).sorted().toList();
        }
        for (Path obra : obras) {
            List<Path> arquivos;
            try (Stream<Path> s = Files.walk(obra)) {
                arquivos = s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".ass"))
                    .filter(p -> !p.getFileName().toString().toLowerCase().contains(".parcial."))
                    .filter(p -> p.getParent() != null
                        && "traducao_ptbr".equals(p.getParent().getFileName().toString()))
                    .sorted().toList();
            }
            for (Path arquivo : arquivos) {
                DocumentoLegenda documento;
                try {
                    documento = leitor.ler(arquivo);
                } catch (RuntimeException e) {
                    continue;
                }
                for (EventoLegenda evento : documento.eventos()) {
                    if (!evento.temTexto()) {
                        continue;
                    }
                    if (evento.estilo() != null && politicaEstiloMusical.estiloIgnorado(evento.estilo())) {
                        continue;
                    }
                    falas++;
                    colher(visivel(evento.texto()), obra.getFileName().toString(), ocorrencias);
                }
            }
        }

        // ---------- 2. uma pergunta em lote: palavras e variantes acentuadas ----------
        Set<String> aPerguntar = new LinkedHashSet<>();
        Map<String, List<String>> variantesDe = new LinkedHashMap<>();
        for (Ocorrencia o : ocorrencias) {
            String p = o.palavra().toLowerCase();
            if (variantesDe.containsKey(p)) {
                continue;
            }
            List<String> vars = variantesAcentuadas(p);
            variantesDe.put(p, vars);
            aPerguntar.add(p);
            aPerguntar.addAll(vars);
        }
        Map<String, VeredictoPalavra> veredicto = classificarEmBlocos(aPerguntar);

        // ---------- 3. controle, no MESMO experimento ----------
        List<String> falhas = new ArrayList<>();
        if (veredicto.get("noticia") != VeredictoPalavra.PORTUGUES_OK) {
            falhas.add("CONTROLE: 'noticia' devia ser aceita pelo dicionario (e forma verbal) e nao foi");
        }
        if (!variantesAcentuadas("noticia").contains("notícia")) {
            falhas.add("CONTROLE: o gerador de variantes nao produziu 'notícia' a partir de 'noticia'");
        }
        if (variantesAcentuadas("noticia").contains("noticiar")) {
            falhas.add("CONTROLE: o gerador inventou palavra ('noticiar') — so pode TROCAR letra, nunca acrescentar");
        }
        boolean semDeterminante = ocorrencias.stream()
            .noneMatch(o -> "ele".equals(o.determinante().toLowerCase()));
        if (!semDeterminante) {
            falhas.add("CONTROLE: 'ele noticia' entrou na colheita — pronome nao e determinante");
        }
        if (!falhas.isEmpty()) {
            System.out.println("\nINSTRUMENTO REPROVADO NO CONTROLE — nenhum numero do acervo vale:");
            falhas.forEach(f -> System.out.println("   " + f));
            assertTrue(false, "controle do instrumento falhou: " + falhas);
        }

        // ---------- 4. classificacao ----------
        Map<String, List<Ocorrencia>> cega = new TreeMap<>();       // o alvo
        Map<String, List<Ocorrencia>> jaCoberta = new TreeMap<>();  // corretor atual ja alcanca
        Map<String, List<Ocorrencia>> ambigua = new TreeMap<>();    // 2+ formas acentuadas
        Map<String, List<Ocorrencia>> comTrema = new TreeMap<>();   // separado: e nome, nao palavra
        for (Ocorrencia o : ocorrencias) {
            String p = o.palavra().toLowerCase();
            List<String> conhecidas = variantesDe.get(p).stream()
                .filter(v -> veredicto.get(v) == VeredictoPalavra.PORTUGUES_OK)
                .toList();
            boolean palavraConhecida = veredicto.get(p) == VeredictoPalavra.PORTUGUES_OK;
            if (conhecidas.isEmpty()) {
                continue;
            }
            // O TREMA sai antes de qualquer outra gaveta: em palavra comum a forma pré-1990 nem
            // está no dicionário, então o que sobrar aqui é nome próprio — alemão, quase sempre.
            // Nome é território de lore, e lore não se corrige por regra automática.
            if (conhecidas.stream().anyMatch(v -> v.indexOf(TREMA) >= 0)) {
                comTrema.computeIfAbsent(p + " -> " + conhecidas, k -> new ArrayList<>()).add(o);
            } else if (!palavraConhecida) {
                jaCoberta.computeIfAbsent(p, k -> new ArrayList<>()).add(o);
            } else if (conhecidas.size() == 1) {
                cega.computeIfAbsent(p + " -> " + conhecidas.get(0), k -> new ArrayList<>()).add(o);
            } else {
                ambigua.computeIfAbsent(p + " -> " + conhecidas, k -> new ArrayList<>()).add(o);
            }
        }

        // ---------- 5. relatorio ----------
        System.out.printf("%n  falas ao alcance (sem musica, sem .parcial) ... %d%n", falas);
        System.out.printf("  pares determinante+palavra colhidos ........... %d%n", ocorrencias.size());
        System.out.printf("  formas distintas perguntadas ao dicionario .... %d%n", aPerguntar.size());

        int totalCega = cega.values().stream().mapToInt(List::size).sum();
        int totalJa = jaCoberta.values().stream().mapToInt(List::size).sum();
        int totalAmb = ambigua.values().stream().mapToInt(List::size).sum();
        System.out.printf("%n  CLASSE CEGA (palavra E verbo valido) ......... %d ocorrencias, %d formas%n",
            totalCega, cega.size());
        System.out.printf("  ja coberta pelo corretor atual ............... %d ocorrencias, %d formas%n",
            totalJa, jaCoberta.size());
        System.out.printf("  ambigua (2+ acentuacoes) — NAO corrigir ...... %d ocorrencias, %d formas%n",
            totalAmb, ambigua.size());
        int totalTrema = comTrema.values().stream().mapToInt(List::size).sum();
        System.out.printf("  TREMA (nome proprio, decidir na lore) ........ %d ocorrencias, %d formas%n",
            totalTrema, comTrema.size());

        System.out.println("\n--- CLASSE CEGA, por frequencia ---");
        cega.entrySet().stream()
            .sorted(Comparator.<Map.Entry<String, List<Ocorrencia>>>comparingInt(e -> -e.getValue().size()))
            .forEach(e -> {
                System.out.printf("%n  %4d  %s%n", e.getValue().size(), e.getKey());
                e.getValue().stream().limit(AMOSTRAS).forEach(o ->
                    System.out.printf("          [%s] ...%s %s...%n",
                        curto(o.obra()), o.determinante(), o.palavra()));
            });

        if (!ambigua.isEmpty()) {
            System.out.println("\n--- AMBIGUA: duas ou mais acentuacoes conhecidas, fica para leitura humana ---");
            ambigua.forEach((k, v) -> System.out.printf("  %4d  %s%n", v.size(), k));
        }

        if (!comTrema.isEmpty()) {
            System.out.println("\n--- TREMA: a forma pre-1990 nao esta no dicionario, entao o que cai aqui e");
            System.out.println("    NOME PROPRIO (alemao, quase sempre). Decisao de lore, nunca regra automatica. ---");
            comTrema.forEach((k, v) -> {
                System.out.printf("  %4d  %s%n", v.size(), k);
                v.stream().limit(AMOSTRAS).forEach(o ->
                    System.out.printf("          [%s] ...%s %s...%n",
                        curto(o.obra()), o.determinante(), o.palavra()));
            });
        }
    }

    /**
     * Todas as trocas de UMA letra por sua forma acentuada. Só TROCA: nunca acrescenta nem
     * remove letra, e por isso não tem como inventar palavra que não seja o mesmo esqueleto.
     */
    static List<String> variantesAcentuadas(String palavra) {
        List<String> fora = new ArrayList<>();
        char[] letras = palavra.toCharArray();
        for (int i = 0; i < letras.length; i++) {
            String opcoes = ACENTOS.get(letras[i]);
            if (opcoes == null) {
                continue;
            }
            for (char nova : opcoes.toCharArray()) {
                char antiga = letras[i];
                letras[i] = nova;
                fora.add(new String(letras));
                letras[i] = antiga;
            }
        }
        return fora;
    }

    private void colher(String texto, String obra, List<Ocorrencia> destino) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        Matcher m = PAR.matcher(texto);
        while (m.find()) {
            String det = m.group(1);
            String palavra = m.group(2);
            if (!DETERMINANTES.contains(det.toLowerCase())) {
                continue;
            }
            if (!palavra.equals(semAcento(palavra))) {
                continue;
            }
            destino.add(new Ocorrencia(det, palavra, obra, texto));
        }
    }

    /** Texto visível: sem tags de override e com a quebra do ASS virando espaço. */
    private static String visivel(String texto) {
        if (texto == null) {
            return "";
        }
        return TAG.matcher(texto).replaceAll(" ").replace("\\N", " ").replace("\\n", " ");
    }

    private static String semAcento(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
    }

    /**
     * Pergunta em blocos: um lote único de dezenas de milhares de formas prendeu o processo do
     * hunspell por 1.159s em 19/08/2026. Três mil por vez foi o tamanho que voltou a responder.
     */
    private Map<String, VeredictoPalavra> classificarEmBlocos(Set<String> formas) {
        Map<String, VeredictoPalavra> todos = new LinkedHashMap<>();
        List<String> lista = new ArrayList<>(formas);
        for (int i = 0; i < lista.size(); i += BLOCO) {
            todos.putAll(dicionario.classificar(
                new LinkedHashSet<>(lista.subList(i, Math.min(i + BLOCO, lista.size())))));
        }
        return todos;
    }

    private static String curto(String obra) {
        return obra.length() > 18 ? obra.substring(0, 18) : obra;
    }
}
