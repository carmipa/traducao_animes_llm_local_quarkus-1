package org.traducao.projeto.medicao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;
import org.traducao.projeto.core.texto.dicionarioOrtografia.VeredictoPalavra;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: medir, no cache REAL do karaokê, quantas letras traduzidas carregam uma
 * palavra que nenhum dicionário reconhece e que não veio do inglês — a classe de defeito de
 * {@code "I'd act on all of my feelings" => "Açãoaria em todos os meus sentimentos."} (Break Blade
 * 1, teste ponta a ponta de 09/10/2026). Existe para a regra A8: antes de uma guarda bloquear ou
 * refazer tradução, o custo dela (o legítimo que ela pegaria) tem de ser um número.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Estritamente READ-ONLY: lê {@code cache/karaoke} pelos tipos do projeto
 *       ({@link LeitorAcervoCache}) e escreve só em {@code build/}.</li>
 *   <li>Consulta o classificador de PRODUÇÃO ({@link CorretorOrtograficoLegenda#classificarPalavras})
 *       e o dono único de "quais palavras se perguntam ao dicionário"
 *       ({@link CorretorOrtograficoLegenda#palavrasDe}). Não reimplementa critério.</li>
 *   <li>Palavra que aparece no ORIGINAL (ignorando caixa) não conta: nome próprio, romaji e termo
 *       que o modelo manteve não são invenção.</li>
 *   <li>O caso-controle vive DENTRO do instrumento: {@code Açãoaria} TEM de sair desconhecida e
 *       {@code sentimentos} TEM de sair português — senão a medição aborta.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Cache ausente, dicionário fora do ar ou zero entradas lidas REPROVAM: zero suspeitas por
 * instrumento cego não pode ser lido como "o modelo não inventa palavra".
 */
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoPalavraInventadaKaraokeIT {

    private static final Pattern TAGS_ASS = Pattern.compile("\\{[^}]*\\}");

    /**
     * PROPÓSITO DE NEGÓCIO: conta, por veredicto, as palavras das traduções que não vêm do
     * original, e lista as DESCONHECIDAS com a letra inteira para julgamento humano.
     */
    @Test
    @DisplayName("mede palavras desconhecidas nas letras traduzidas do cache real do karaoke")
    void medePalavrasDesconhecidas() throws IOException {
        Path raizCache = LeitorAcervoCache.raizPadrao().resolve("karaoke");
        assertTrue(Files.isDirectory(raizCache),
            "cache de karaoke ausente em " + raizCache + " — sem ele zero suspeitas seria NAO VERIFICADO");

        CorretorOrtograficoLegenda corretor = new CorretorOrtograficoLegenda();

        Map<String, VeredictoPalavra> controle =
            corretor.classificarPalavras(List.of("Açãoaria", "sentimentos"));
        assertTrue(corretor.disponivel(), "hunspell pt_BR indisponivel: NAO VERIFICADO");
        assertEquals(VeredictoPalavra.DESCONHECIDA, controle.get("Açãoaria"),
            "caso DOENTE nao saiu desconhecido: o instrumento nao enxerga a classe");
        assertEquals(VeredictoPalavra.PORTUGUES_OK, controle.get("sentimentos"),
            "caso SAO nao saiu portugues: o instrumento acusa o legitimo");

        LeitorAcervoCache.Acervo acervo = LeitorAcervoCache.ler(raizCache);
        assertFalse(acervo.falas().isEmpty(), "zero entradas no cache — instrumento cego");

        // Palavra -> (linha de exemplo). So o que NAO esta no original.
        Map<String, String> novasPorPalavra = new LinkedHashMap<>();
        Map<String, Integer> ocorrencias = new TreeMap<>();
        long letrasTraduzidas = 0;
        for (LeitorAcervoCache.FalaDoAcervo fala : acervo.falas()) {
            String traduzido = visivel(fala.traduzido());
            String original = visivel(fala.original());
            if (traduzido.isBlank() || traduzido.equals(original)) {
                continue;
            }
            letrasTraduzidas++;
            Set<String> doOriginal = new LinkedHashSet<>();
            for (String p : CorretorOrtograficoLegenda.palavrasDe(original)) {
                doOriginal.add(p.toLowerCase(Locale.ROOT));
            }
            for (String p : CorretorOrtograficoLegenda.palavrasDe(traduzido)) {
                if (doOriginal.contains(p.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                novasPorPalavra.putIfAbsent(p, fala.obra() + " | " + original + "  =>  " + traduzido);
                ocorrencias.merge(p, 1, Integer::sum);
            }
        }

        Map<String, VeredictoPalavra> veredicto = corretor.classificarPalavras(novasPorPalavra.keySet());
        Map<VeredictoPalavra, Integer> formasPorVeredicto = new EnumMap<>(VeredictoPalavra.class);
        Map<VeredictoPalavra, Integer> ocorrenciasPorVeredicto = new EnumMap<>(VeredictoPalavra.class);
        List<String> desconhecidas = new ArrayList<>();
        for (Map.Entry<String, String> e : novasPorPalavra.entrySet()) {
            VeredictoPalavra v = veredicto.getOrDefault(e.getKey(), VeredictoPalavra.NAO_VERIFICADO);
            formasPorVeredicto.merge(v, 1, Integer::sum);
            ocorrenciasPorVeredicto.merge(v, ocorrencias.get(e.getKey()), Integer::sum);
            if (v != VeredictoPalavra.PORTUGUES_OK) {
                desconhecidas.add(String.format(Locale.ROOT, "%-16s %-18s x%-3d %s",
                    v.name(), e.getKey(), ocorrencias.get(e.getKey()), e.getValue()));
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== PALAVRAS QUE NAO VIERAM DO ORIGINAL, NAS LETRAS TRADUZIDAS DO KARAOKE ===\n");
        sb.append("cache: ").append(raizCache).append('\n');
        sb.append("arquivos: ").append(acervo.arquivosLidos())
            .append(" (ilegiveis: ").append(acervo.arquivosIlegiveis()).append(")\n");
        sb.append("entradas: ").append(acervo.falas().size())
            .append(" | letras traduzidas (traduzido != original): ").append(letrasTraduzidas).append('\n');
        sb.append("formas distintas novas: ").append(novasPorPalavra.size()).append("\n\n");
        sb.append(String.format(Locale.ROOT, "%-16s %8s %12s%n", "veredicto", "formas", "ocorrencias"));
        for (VeredictoPalavra v : VeredictoPalavra.values()) {
            sb.append(String.format(Locale.ROOT, "%-16s %8d %12d%n", v.name(),
                formasPorVeredicto.getOrDefault(v, 0), ocorrenciasPorVeredicto.getOrDefault(v, 0)));
        }
        sb.append("\n--- FORA DE PORTUGUES_OK (veredicto, forma, ocorrencias, obra | original => traduzido) ---\n");
        desconhecidas.forEach(l -> sb.append(l).append('\n'));

        Path saida = Path.of("build", "medicao-palavra-inventada-karaoke.txt");
        Files.createDirectories(saida.getParent());
        Files.writeString(saida, sb.toString());
        System.out.println(sb);
    }

    private static String visivel(String texto) {
        if (texto == null) {
            return "";
        }
        return TAGS_ASS.matcher(texto).replaceAll("")
            .replace("\\N", " ").replace("\\n", " ").replace("\\h", " ").strip();
    }
}
