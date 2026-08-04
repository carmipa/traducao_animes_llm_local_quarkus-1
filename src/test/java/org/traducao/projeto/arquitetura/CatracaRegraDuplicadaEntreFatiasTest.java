package org.traducao.projeto.arquitetura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: contar as REGRAS declaradas em mais de uma fatia, para que duplicar deixe
 * de ser silencioso. O projeto adota deliberadamente "duplicação consciente &gt; acoplamento" —
 * acoplar {@code novoKaraoke} a {@code qualidadeTraducao} só para reusar uma regex criaria
 * dependência pior que a cópia. Esta catraca NÃO proíbe duplicar; ela exige que a cópia seja
 * DECLARADA, porque cópia não declarada não é decisão, é acidente.
 *
 * <h2>O que motivou a catraca</h2>
 * Em 2026-08-03 três defeitos do acervo tiveram a MESMA assinatura: a regra existia em dois
 * lugares, nenhum sabia do outro, um foi corrigido e o outro ficou.
 * <ul>
 *   <li><b>Fronteira de nome musical</b>: o padrão largo já alcançava {@code OP_S2}, mas
 *       {@code eEstiloDeMusica} usava o de {@code \b}. Dez dos 23 episódios do Guilty Crown
 *       atravessaram a simplificação sem fundir NADA, por nove dias.</li>
 *   <li><b>Pares inconfundíveis</b>: consultados no portão da tradução e não no reforço de cache.
 *       Vinte e duas trocas de entidade ficaram presas no cache do Gundam ZZ.</li>
 *   <li><b>Proporção de romaji</b>: o detector passou a exigir 70% em vez de 100%, e a CÓPIA
 *       dentro do conversor ficou no 100%. Trinta e três versos do Guilty Crown saíram com a
 *       tradução acima do original, porque {@code hitoribocchi} e {@code tsudzuku} reprovam numa
 *       régua que exige sílaba Hepburn pura.</li>
 * </ul>
 * Nos três, o defeito foi LOCALIZÁVEL com precisão — arquivo, linha, palavra — que é a vantagem
 * real da arquitetura por fatias. O que faltou foi o sinal de que a outra cópia existia.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O total é congelado. Duplicar a próxima regra reprova o teste, e quem duplicar tem de
 *       declarar aqui — no código, não em conversa — por que a cópia é melhor que o acoplamento.</li>
 *   <li>Unificar uma regra também reprova, de propósito: o número cair sem alguém confirmar
 *       esconderia acoplamento novo tanto quanto duplicação nova esconde deriva.</li>
 *   <li>Mede só {@code Pattern.compile("...")} com 12+ caracteres. Padrão trivial repetido não é
 *       regra de negócio, e contá-lo afogaria o sinal em ruído.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem nomeia a regra e as fatias onde ela vive, para a correção não exigir nova varredura.
 */
class CatracaRegraDuplicadaEntreFatiasTest {

    private static final Path RAIZ = Path.of("src/main/java/org/traducao/projeto");

    /** Corpo do primeiro literal de {@code Pattern.compile("...")}. */
    private static final Pattern DECLARACAO = Pattern.compile(
        "Pattern\\.compile\\(\\s*\"((?:[^\"\\\\]|\\\\.)*)\"", Pattern.DOTALL);

    /** Abaixo disto o padrão é trivial ({@code \\s+}, {@code ^\\d+}) e não representa regra. */
    private static final int TAMANHO_MINIMO = 12;

    /**
     * Total MEDIDO em 2026-08-04 sobre 142 regras declaradas. Cada uma destas vive em duas ou mais
     * fatias e nenhuma delas declara a outra - é o inventário da dívida, não um alvo aceitável.
     */
    private static final int DUPLICADAS_CONHECIDAS = 11;

    @Test
    @DisplayName("nenhuma REGRA nova e duplicada entre fatias sem declaracao")
    void totalDeRegrasDuplicadasEstaCongelado() throws IOException {
        Map<String, Set<String>> fatiasPorRegra = mapearRegras();
        Map<String, Set<String>> duplicadas = new TreeMap<>();
        fatiasPorRegra.forEach((regra, fatias) -> {
            if (fatias.size() > 1) {
                duplicadas.put(regra, fatias);
            }
        });

        assertEquals(DUPLICADAS_CONHECIDAS, duplicadas.size(), () -> """
            O NUMERO DE REGRAS DUPLICADAS ENTRE FATIAS MUDOU.

            Subiu? Uma regra passou a existir em duas fatias. Isso PODE ser certo — o projeto
            prefere duplicacao consciente a acoplamento —, mas a copia tem de ser DECLARADA:
            escreva no Javadoc de cada lado que a outra existe e por que nao foram unificadas.
            Depois atualize DUPLICADAS_CONHECIDAS. Copia nao declarada foi a causa de tres
            defeitos em 2026-08-03, um deles vivo por nove dias.

            Desceu? Alguem unificou uma regra. Confirme que a unificacao nao criou dependencia
            entre fatias que deviam permanecer independentes, e atualize o numero.

            Inventario atual:
            """ + formatar(duplicadas));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: as três regras que causaram defeito medido continuam duplicadas e são
     * nomeadas aqui para que ninguém as "descubra" de novo do zero.
     *
     * <p>INVARIANTES DO DOMÍNIO: este teste FALHA quando uma delas for unificada — e falhar aí é
     * bom: significa que a dívida foi paga e a nota pode sair.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: a mensagem diz qual delas mudou.
     */
    @Test
    @DisplayName("as regras que ja causaram defeito seguem nomeadas enquanto duplicadas")
    void regrasComHistoricoDeDerivaEstaoNomeadas() throws IOException {
        Map<String, Set<String>> fatiasPorRegra = mapearRegras();

        String silabaRomaji = fatiasPorRegra.keySet().stream()
            .filter(r -> r.contains("kgsztdnhbpmyrwfjv"))
            .findFirst()
            .orElse(null);
        assertTrue(silabaRomaji != null, "a regex de silaba romaji sumiu do codigo");
        assertTrue(fatiasPorRegra.get(silabaRomaji).size() >= 2, () -> """
            A regex de SILABA ROMAJI foi unificada — otima noticia.
            Ela vivia em legenda, novoKaraoke e traducaoKaraoke; a copia do conversor ficou no
            criterio de 100% depois que o detector passou para 70%, e 33 versos do Guilty Crown
            sairam com a traducao acima do original. Confirme e ajuste esta catraca.
            """);

        String escritaJaponesa = fatiasPorRegra.keySet().stream()
            .filter(r -> r.contains("IsHiragana"))
            .findFirst()
            .orElse(null);
        assertTrue(escritaJaponesa != null, "a regex de escrita japonesa sumiu do codigo");
        assertTrue(fatiasPorRegra.get(escritaJaponesa).size() >= 2,
            "a regex de escrita japonesa foi unificada — confirme e ajuste esta catraca");
    }

    /** Mapeia corpo da regex → fatias em que ela é declarada. */
    private static Map<String, Set<String>> mapearRegras() throws IOException {
        Map<String, Set<String>> mapa = new LinkedHashMap<>();
        try (Stream<Path> arquivos = Files.walk(RAIZ)) {
            for (Path arquivo : arquivos.filter(p -> p.toString().endsWith(".java")).toList()) {
                String fatia = RAIZ.relativize(arquivo).getName(0).toString();
                Matcher m = DECLARACAO.matcher(Files.readString(arquivo, StandardCharsets.UTF_8));
                while (m.find()) {
                    String corpo = m.group(1);
                    if (corpo.length() >= TAMANHO_MINIMO) {
                        mapa.computeIfAbsent(corpo, k -> new TreeSet<>()).add(fatia);
                    }
                }
            }
        }
        return mapa;
    }

    private static String formatar(Map<String, Set<String>> duplicadas) {
        StringBuilder sb = new StringBuilder();
        duplicadas.entrySet().stream()
            .sorted(Comparator.comparingInt((Map.Entry<String, Set<String>> e) -> -e.getValue().size()))
            .forEach(e -> sb.append("  ").append(e.getValue()).append("  /")
                .append(e.getKey().length() > 64 ? e.getKey().substring(0, 64) + "…" : e.getKey())
                .append("/").append(System.lineSeparator()));
        return sb.toString();
    }
}
