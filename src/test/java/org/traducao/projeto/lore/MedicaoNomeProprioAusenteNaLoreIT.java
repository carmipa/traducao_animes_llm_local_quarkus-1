package org.traducao.projeto.lore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.lore.domain.ProvedorContexto;
import org.traducao.projeto.lore.infrastructure.CatalogoLoreYaml;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;
import org.traducao.projeto.qualidadeTraducao.domain.LoreAtivaPort;
import org.traducao.projeto.raspagemRevisao.application.FiltroAuditoriaLinha;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: descobrir quais NOMES PRÓPRIOS faltam na lore de cada obra e, por
 * faltarem, fazem o validador barrar tradução CORRETA como "idioma incorreto".
 *
 * <h2>O prejuízo que originou, medido em 22/08/2026</h2>
 * A fala do ZZ {@code "If we don't get back to Port Blanc soon,\NGottn's gonna yell at us
 * again."} ficou presa em inglês. A tradução saía correta —
 * {@code "Se não voltarmos logo para Port Blanc,..."} — e o validador a recusava com
 * <i>"Idioma incorreto detectado (não é PT-BR)"</i>. A causa não era a tradução: era
 * {@code "Port Blanc"} não estar em {@code termosProtegidos} do {@code gundam_zz}.
 * {@code Gottn}, {@code Sadalahn}, {@code Roux Louka} e {@code Haman} estavam; ele, não.
 *
 * <p>{@link ValidadorTraducaoService} remove os termos da lore ANTES de checar idioma. Nome que
 * está na lore é invisível para a checagem; nome que falta conta como palavra inglesa.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só reporta nome que aparece no texto PT <b>já traduzido</b> e cuja fala o validador
 *       REPROVA. Nome que não causa dano não vira ruído na lista.</li>
 *   <li>A lore de cada obra é a REAL, carregada do {@code lore.yaml} e casada pela
 *       {@code apelidosPasta} — não é lista minha.</li>
 *   <li>NÃO escreve nada. Lê e imprime a lista para julgamento humano: incluir termo na lore é
 *       decisão, e termo errado ali protege palavra comum de ser traduzida.</li>
 *   <li>Travado por {@code -Dkronos.medicao=true}; acervo ausente REPROVA, não pula.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Arquivo ilegível é contado e reportado; o harness nunca reprova por conteúdo.
 */
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoNomeProprioAusenteNaLoreIT {

    private static final Path ACERVO = Path.of(System.getProperty("kronos.acervo", "C:\\animes"));
    private static final String PASTA_PT = "traducao_ptbr";
    private static final String FORA_DO_ACERVO = "DanMachi";

    /**
     * A lista {@code estilos-ignorados} do {@code application.yml}. Sem ela o resultado mente:
     * a primeira medição deu "And" 105x, "You" 84x e "Why" 42x no Unicorn — LETRA DE MÚSICA em
     * camada palavra-a-palavra, não nome próprio. É a terceira vez que esta mesma configuração
     * faltando produz número absurdo num harness meu.
     */
    private static final Set<String> ESTILOS_IGNORADOS = Set.of(
        "Song JP", "Mobile Suit Gundam", "Char's Counterattack", "OP - Romaji", "OP - English",
        "ED - Romaji", "ED - English", "ED-ROM", "OPL2");

    /** Palavra capitalizada no MEIO da frase: candidata a nome próprio. */
    private static final Pattern CAPITALIZADA =
        Pattern.compile("(?<![.!?\"'\\n]\\s)(?<!^)\\b(\\p{Lu}[\\p{L}'-]{2,})\\b");

    private static final Pattern TAG = Pattern.compile("\\{[^}]*}");

    @Test
    @DisplayName("mede que nomes proprios faltam na lore e barram traducao correta")
    void medeNomesAusentes() throws IOException {
        assertTrue(Files.isDirectory(ACERVO), "acervo inacessivel em " + ACERVO);

        CatalogoLoreYaml catalogo = new CatalogoLoreYaml();
        FiltroAuditoriaLinha filtro = new FiltroAuditoriaLinha(
            new MascaradorTags(), new PoliticaEstiloMusical(List.of()),
            new DetectorEfeitoKaraokeService(), new ProtecaoLegendaAssService());
        LeitorLegendaAss leitor = new LeitorLegendaAss();

        List<Path> pastas = new ArrayList<>();
        // Alcance pelo DONO UNICO: honra -Dkronos.medicao.obra e declara NAO VERIFICADO
        // quando o filtro nao casa. O veto de FORA_DO_ACERVO continua sendo desta medicao — o
        // dono unico responde "onde estao as traducoes", nao "quais interessam a este harness".
        org.traducao.projeto.medicao.AlcanceDaMedicao.pastasDeTraducao().stream()
            .filter(d -> !d.toString().contains(FORA_DO_ACERVO))
            .forEach(pastas::add);
        assertFalse(pastas.isEmpty(), "instrumento cego: nenhuma pasta " + PASTA_PT);

        Map<String, Map<String, Integer>> faltamPorObra = new LinkedHashMap<>();
        Map<String, String> loreDaPasta = new LinkedHashMap<>();
        int semLore = 0;
        int barradas = 0;
        Map<String, String> amostraPorNome = new LinkedHashMap<>();

        for (Path pasta : pastas) {
            Path pastaObra = pasta.getParent();
            String nomePasta = pastaObra == null ? "?" : pastaObra.getFileName().toString();
            ProvedorContexto obra = casarObra(catalogo, nomePasta);
            if (obra == null) {
                semLore++;
                loreDaPasta.put(nomePasta, "(sem lore casada)");
                continue;
            }
            loreDaPasta.put(nomePasta, obra.getId());
            Set<String> protegidos = obra.termosProtegidos();
            ValidadorTraducaoService validador = new ValidadorTraducaoService(new LoreDaObra(obra));
            Map<String, Integer> faltam = faltamPorObra
                .computeIfAbsent(obra.getId(), k -> new TreeMap<>());

            for (Path arquivo : arquivosDe(pasta)) {
                List<EventoLegenda> eventos;
                try {
                    eventos = leitor.ler(arquivo).eventos();
                } catch (RuntimeException e) {
                    continue;
                }
                for (EventoLegenda evento : eventos) {
                    if (filtro.deveIgnorarLinha(evento)
                        || ESTILOS_IGNORADOS.contains(evento.estilo())) {
                        continue;
                    }
                    try {
                        validador.validarFala(evento.texto());
                        continue;
                    } catch (AlucinacaoDetectadaException e) {
                        barradas++;
                    }
                    for (String candidata : capitalizadasDe(evento.texto())) {
                        if (!cobertoPelaLore(candidata, protegidos)) {
                            faltam.merge(candidata, 1, Integer::sum);
                            if (amostraPorNome.size() < 12) {
                                amostraPorNome.put(obra.getId() + " | " + candidata,
                                    evento.texto().replace("\\N", " "));
                            }
                        }
                    }
                }
            }
        }

        System.out.println("=== NOMES PROPRIOS AUSENTES DA LORE (que barram traducao correta) ===");
        System.out.println("pastas lidas : " + pastas.size()
            + (semLore > 0 ? "  (" + semLore + " SEM lore casada)" : ""));
        System.out.println("falas barradas pelo validador : " + barradas);
        System.out.println();
        faltamPorObra.forEach((id, faltam) -> {
            List<Map.Entry<String, Integer>> ordenado = new ArrayList<>(faltam.entrySet());
            ordenado.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            if (ordenado.isEmpty()) {
                return;
            }
            System.out.println("### " + id);
            ordenado.stream().limit(20).forEach(e ->
                System.out.println("   " + e.getValue() + "x  " + e.getKey()));
        });
        System.out.println();
        System.out.println("--- a FALA de cada nome (para julgar sem abrir o acervo):");
        amostraPorNome.forEach((k, v) -> System.out.println("   " + k + "  ->  " + v));
        System.out.println();
        System.out.println("--- pasta -> lore casada:");
        loreDaPasta.forEach((p, id) -> System.out.println("   " + p.substring(0,
            Math.min(46, p.length())) + "  ->  " + id));
    }

    private static List<Path> arquivosDe(Path pasta) throws IOException {
        try (Stream<Path> s = Files.list(pasta)) {
            return s.filter(Files::isRegularFile)
                .filter(p -> {
                    String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                    return n.endsWith(".ass") && !n.endsWith(".parcial.ass");
                })
                .toList();
        }
    }

    /** Casa a pasta com a obra pela {@code apelidosPasta} da própria lore. */
    private static ProvedorContexto casarObra(CatalogoLoreYaml catalogo, String nomePasta) {
        String alvo = nomePasta.toLowerCase(Locale.ROOT);
        ProvedorContexto melhor = null;
        int melhorTamanho = 0;
        for (ProvedorContexto o : catalogo.obras()) {
            for (String apelido : o.apelidosPasta()) {
                String a = apelido.toLowerCase(Locale.ROOT);
                if (!a.isBlank() && alvo.contains(a) && a.length() > melhorTamanho) {
                    melhor = o;
                    melhorTamanho = a.length();
                }
            }
        }
        return melhor;
    }

    private static Set<String> capitalizadasDe(String texto) {
        String visivel = TAG.matcher(texto == null ? "" : texto).replaceAll(" ")
            .replace("\\N", " ").replace("\\h", " ");
        Set<String> saida = new LinkedHashSet<>();
        Matcher m = CAPITALIZADA.matcher(visivel);
        while (m.find()) {
            saida.add(m.group(1));
        }
        return saida;
    }

    /** Coberto quando a lore traz o termo, ou um termo composto que o contém. */
    private static boolean cobertoPelaLore(String candidata, Set<String> protegidos) {
        String c = candidata.toLowerCase(Locale.ROOT);
        for (String termo : protegidos) {
            String t = termo.toLowerCase(Locale.ROOT);
            if (t.equals(c) || t.contains(c) || c.contains(t)) {
                return true;
            }
        }
        return false;
    }

    /** A lore REAL da obra, pela porta que o validador consome. */
    private record LoreDaObra(ProvedorContexto obra) implements LoreAtivaPort {
        @Override
        public Set<String> termosProtegidosAtivos() {
            return obra.termosProtegidos();
        }

        @Override
        public String obterLoreAtiva() {
            return obra.obterPromptSistema();
        }

        @Override
        public Set<List<String>> paresInconfundiveisAtivos() {
            return obra.paresInconfundiveis();
        }
    }
}
