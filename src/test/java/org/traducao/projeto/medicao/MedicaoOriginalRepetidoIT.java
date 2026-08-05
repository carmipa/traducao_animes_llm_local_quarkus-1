package org.traducao.projeto.medicao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.medicao.LeitorAcervoCache.Acervo;
import org.traducao.projeto.medicao.LeitorAcervoCache.FalaDoAcervo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * PROPÓSITO DE NEGÓCIO: quantas falas de um MESMO arquivo têm o original IDÊNTICO — e em quantas
 * delas o pipeline devolveu traduções DIFERENTES para o mesmo texto.
 *
 * <h2>A assimetria que isto expõe</h2>
 * O cache já garante, ENTRE execuções, que texto igual recebe tradução igual: é o que
 * {@code mesmaProveniencia} protege. DENTRO de uma execução isso não vale — cada fala vai ao LLM
 * por conta própria. Medido no 86 em 05/08/2026, um cartão de data em três camadas:
 * <pre>
 *   layer 0   "July 30th, Stellar Year 2149"  ->  (pendente)
 *   layer 1   "July 30th, Stellar Year 2149"  ->  "Julho de 30, Ano Estelar 2149"
 *   layer 2   "July 30th, Stellar Year 2149"  ->  (pendente)
 * </pre>
 * Mesmo texto, três desfechos, na mesma execução. Foi isso que produziu as pendências
 * {@code 2049}/{@code 2149} e o que impediu o simplificador de achatar o cartão depois — os textos
 * já tinham divergido.
 *
 * <h2>O que o número decide</h2>
 * Se for pouco, deduplicar a PREPARAÇÃO do lote não paga o esforço. Se for o que se suspeita, o
 * ganho é duplo: some a divergência E some o trabalho repetido do LLM. Nada aqui propõe destruir
 * evento: a saída continua com as N camadas; só o que se MANDA TRADUZIR deixa de repetir.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>READ-ONLY. Agrupa por arquivo — repetição entre obras não é redundância de lote.</li>
 *   <li>Compara o original CRU, como o pipeline o envia: é essa a chave que o lote usaria.</li>
 *   <li>Conta separadamente os grupos que DIVERGIRAM (mais de uma tradução distinta), porque
 *       esses são defeito, e não só desperdício.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Acervo ausente termina com aviso; não lança.
 *
 * <p>Uso: {@code gradlew test --tests "*MedicaoOriginalRepetidoIT*" "-Dkronos.medicao=true"}
 */
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoOriginalRepetidoIT {

    private record Obra(String nome, int falas, int repetidas, int grupos,
                        int gruposDivergentes, int falasDivergentes,
                        int divergentesSoPrefixo, int falasSoPrefixo) {
    }

    @Test
    @DisplayName("acervo: original IDENTICO repetido no mesmo arquivo, e quantos divergiram")
    void medir() throws IOException {
        Acervo acervo = LeitorAcervoCache.ler(LeitorAcervoCache.raizPadrao());
        if (acervo.vazio()) {
            System.out.println("SEM ACERVO — nada medido.");
            return;
        }

        Map<String, List<FalaDoAcervo>> porArquivo = new LinkedHashMap<>();
        for (FalaDoAcervo f : acervo.falas()) {
            porArquivo.computeIfAbsent(f.arquivo().toString(), k -> new ArrayList<>()).add(f);
        }

        // [falas, repetidas, grupos, gruposDiv, falasDiv, divSoPrefixo, falasSoPrefixo]
        Map<String, int[]> porObra = new TreeMap<>();
        List<String> exemplos = new ArrayList<>();

        for (List<FalaDoAcervo> falas : porArquivo.values()) {
            String obra = falas.get(0).obra();
            int[] c = porObra.computeIfAbsent(obra, k -> new int[7]);
            c[0] += falas.size();

            // CHAVE = texto VISÍVEL, não o original cru. A primeira versão agrupava pelo cru e
            // devolveu ZERO divergência — porque as três camadas de um cartão de data diferem
            // justamente nas TAGS ({=0} / nada / {=1}) e nunca caíam no mesmo grupo. O que o LLM
            // lê é o visível; é por ele que a preparação do lote deduplicaria.
            Map<String, List<FalaDoAcervo>> porOriginal = new LinkedHashMap<>();
            for (FalaDoAcervo f : falas) {
                String chave = visivel(f.original());
                if (!chave.isBlank()) {
                    porOriginal.computeIfAbsent(chave, k -> new ArrayList<>()).add(f);
                }
            }
            for (Map.Entry<String, List<FalaDoAcervo>> grupo : porOriginal.entrySet()) {
                if (grupo.getValue().size() < 2) {
                    continue;
                }
                c[1] += grupo.getValue().size();
                c[2]++;
                Set<String> traducoes = new HashSet<>();
                grupo.getValue().forEach(f -> traducoes.add(visivel(f.traduzido())));
                if (traducoes.size() > 1) {
                    c[3]++;
                    c[4] += grupo.getValue().size();
                    // SÓ-PREFIXO: todas as tags do grupo estão ANTES do texto, nenhuma no meio.
                    // É a condição que torna seguro deduplicar pelo texto VISÍVEL: reaplicar é
                    // "prefixo próprio + tradução comum". Com tag no MEIO, a tradução de uma
                    // camada não tem onde reencaixar os marcadores da outra — e foi por isso que
                    // a chave do dedup atual é o MASCARADO (texto + estrutura de tags), que é
                    // conservador de propósito e por isso não junta camadas de composição.
                    if (grupo.getValue().stream().allMatch(f -> soPrefixo(f.original()))) {
                        c[5]++;
                        c[6] += grupo.getValue().size();
                    }
                    if (exemplos.size() < 6) {
                        exemplos.add(String.format("  %s%n     EN  %s%n     PT  %s",
                            falas.get(0).arquivo().getFileName(),
                            recortar(visivel(grupo.getKey())),
                            traducoes.stream().map(t -> "'" + recortar(visivel(t)) + "'")
                                .reduce((a, b) -> a + " | " + b).orElse("")));
                    }
                }
            }
        }

        List<Obra> linhas = new ArrayList<>();
        porObra.forEach((o, c) ->
            linhas.add(new Obra(o, c[0], c[1], c[2], c[3], c[4], c[5], c[6])));

        System.out.printf("%n%-40s %8s %10s %8s %9s %9s%n",
            "OBRA", "falas", "repetidas", "grupos", "DIVERGEM", "falasDiv");
        linhas.stream()
            .filter(o -> o.repetidas() > 0)
            .sorted(Comparator.comparingInt(Obra::repetidas).reversed())
            .limit(15)
            .forEach(o -> System.out.printf("%-40s %8d %10d %8d %9d %9d%n",
                recortar40(o.nome()), o.falas(), o.repetidas(), o.grupos(),
                o.gruposDivergentes(), o.falasDivergentes()));

        int rep = linhas.stream().mapToInt(Obra::repetidas).sum();
        int gru = linhas.stream().mapToInt(Obra::grupos).sum();
        int div = linhas.stream().mapToInt(Obra::gruposDivergentes).sum();
        int falasDiv = linhas.stream().mapToInt(Obra::falasDivergentes).sum();
        System.out.printf("%nREPETIDAS ..... %d falas em %d grupos — o lote mandaria %d (economia de %d)%n",
            rep, gru, gru, rep - gru);
        System.out.printf("DIVERGIRAM .... %d grupos, %d falas — MESMO texto, traducoes DIFERENTES%n",
            div, falasDiv);
        int prefGru = linhas.stream().mapToInt(Obra::divergentesSoPrefixo).sum();
        int prefFal = linhas.stream().mapToInt(Obra::falasSoPrefixo).sum();
        System.out.printf("  destes, SO-PREFIXO ... %d grupos, %d falas — tag so ANTES do texto,%n"
            + "                         onde reaplicar e trivial e o conserto e seguro%n",
            prefGru, prefFal);
        System.out.println();
        System.out.println("A segunda linha e defeito, nao desperdicio: o cache ja garante texto "
            + "igual -> traducao igual ENTRE execucoes; dentro de uma, nao.");
        System.out.println();
        exemplos.forEach(System.out::println);
    }

    /** Todas as tags {@code {...}} estão antes do primeiro caractere de texto? */
    private static boolean soPrefixo(String original) {
        String semPrefixo = original.replaceFirst("^(\\{[^}]*})+", "");
        return !semPrefixo.contains("{");
    }

    private static String visivel(String t) {
        return t.replaceAll("\\{[^}]*}", "").replace("\\N", " ").strip();
    }

    private static String recortar(String t) {
        return t.length() <= 54 ? t : t.substring(0, 54) + "…";
    }

    private static String recortar40(String t) {
        return t.length() <= 40 ? t : t.substring(0, 40) + "…";
    }
}
