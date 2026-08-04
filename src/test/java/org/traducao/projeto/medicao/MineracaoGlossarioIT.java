package org.traducao.projeto.medicao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.medicao.LeitorAcervoCache.Acervo;
import org.traducao.projeto.medicao.LeitorAcervoCache.FalaDoAcervo;
import org.traducao.projeto.traducao.application.EnforcadorGlossarioFala;

import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * PROPÓSITO DE NEGÓCIO: propor CANDIDATOS a entrada de glossário a partir do acervo, com a
 * contagem do lado — em vez de alguém lembrar de uma fala e embutir um palpite no caminho quente.
 *
 * <h2>De onde veio a ideia</h2>
 * O Javadoc de {@code EnforcadorGlossarioFala.GLOSSARIO} manda: <i>"CRESCE POR MEDIÇÃO: cada
 * entrada precisa de ocorrências contadas no acervo, senão vira palpite embutido no caminho
 * quente."</i> A regra existia; o instrumento para cumpri-la, não — e por isso o glossário tem
 * UMA entrada. Isto é o instrumento.
 *
 * <p>A técnica tem nome na literatura: mineração de padrões de erro sobre corpus de pós-edição
 * (<i>Automatic Post-Editing</i>). A parte que importa aqui é a de REGRA, não a neural: um modelo
 * opaco corrigindo outro modelo opaco destruiria a única coisa que este projeto garante, que é
 * apontar arquivo, linha e número quando algo dá errado.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A chave é calculada por {@link EnforcadorGlossarioFala#chaveDeFala(String)} — a MESMA do
 *       enforcer. Normalizar diferente daria contagem que não vale para quem vai consumir.</li>
 *   <li>NÃO decide nada e não escreve nada: propõe, com o número, para um humano aprovar. Cada
 *       linha da saída é uma hipótese, não um veredito.</li>
 *   <li>Só fala CURTA entra ({@link #MAX_PALAVRAS} palavras): o enforcer só age quando a fala
 *       INTEIRA é o termo, então fala longa nunca seria aplicável e só faria ruído.</li>
 *   <li>Os cortes ({@link #MIN_OCORRENCIAS}, {@link #MAX_PALAVRAS}, o teto de impressão) são
 *       IMPRESSOS. Lista truncada em silêncio tem a mesma aparência de lista completa.</li>
 *   <li>READ-ONLY sobre {@code cache/}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Acervo ausente termina com aviso visível. Não lança.
 *
 * <p>Uso (aspas obrigatórias no PowerShell): {@code gradlew test --tests "*MineracaoGlossarioIT*"
 * "-Dkronos.medicao=true"}
 */
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MineracaoGlossarioIT {

    /** Abaixo disto é coincidência, não padrão. */
    private static final int MIN_OCORRENCIAS = 4;

    /** O enforcer só age quando a fala INTEIRA é o termo; fala longa nunca se aplicaria. */
    private static final int MAX_PALAVRAS = 4;

    private static final int TETO_IMPRESSAO = 25;

    /** Uma fala em inglês vista N vezes, e no que ela foi parar. */
    private record Candidato(String chaveEn, int ocorrencias, int vezesNaoTraduzida,
                             Map<String, Integer> traducoes) {

        /** Quantas grafias PT diferentes a mesma fala EN recebeu. */
        int divergencia() {
            return traducoes.size();
        }
    }

    @Test
    @DisplayName("acervo: candidatos a entrada de glossario, com contagem")
    void minerar() throws IOException {
        Acervo acervo = LeitorAcervoCache.ler(LeitorAcervoCache.raizPadrao());
        if (acervo.vazio()) {
            System.out.println("SEM ACERVO — nada minerado.");
            return;
        }

        Map<String, Map<String, Integer>> porChaveEn = new LinkedHashMap<>();
        for (FalaDoAcervo fala : acervo.falas()) {
            String en = EnforcadorGlossarioFala.chaveDeFala(fala.original());
            String pt = EnforcadorGlossarioFala.chaveDeFala(fala.traduzido());
            if (en.isEmpty() || pt.isEmpty() || contaPalavras(en) > MAX_PALAVRAS) {
                continue;
            }
            porChaveEn.computeIfAbsent(en, k -> new TreeMap<>()).merge(pt, 1, Integer::sum);
        }

        List<Candidato> candidatos = porChaveEn.entrySet().stream()
            .map(e -> new Candidato(e.getKey(),
                e.getValue().values().stream().mapToInt(Integer::intValue).sum(),
                e.getValue().getOrDefault(e.getKey(), 0),
                e.getValue()))
            .filter(c -> c.ocorrencias() >= MIN_OCORRENCIAS)
            .toList();

        System.out.printf("%nacervo: %d arquivos, %d falas | falas EN distintas de ate %d "
                + "palavras com %d+ ocorrencias: %d%n",
            acervo.arquivosLidos(), acervo.falas().size(), MAX_PALAVRAS,
            MIN_OCORRENCIAS, candidatos.size());

        relatarNaoTraduzidas(candidatos);
        relatarDivergentes(candidatos);
        System.out.printf("%nCortes aplicados: minimo %d ocorrencias, maximo %d palavras, "
                + "impressao limitada a %d por secao. NENHUMA linha acima e decisao — sao "
                + "hipoteses com contagem, para aprovacao humana.%n",
            MIN_OCORRENCIAS, MAX_PALAVRAS, TETO_IMPRESSAO);
    }

    /**
     * Falas que o LLM DEIXOU EM INGLÊS repetidas vezes — o caso exato que originou a única entrada
     * do glossário hoje ({@code "roger"}). São os candidatos mais fortes: a chave PT é idêntica à
     * chave EN, então não houve tradução, houve cópia.
     */
    private static void relatarNaoTraduzidas(List<Candidato> candidatos) {
        List<Candidato> ranking = candidatos.stream()
            .filter(c -> c.vezesNaoTraduzida() > 0)
            .sorted(Comparator.comparingInt(Candidato::vezesNaoTraduzida).reversed())
            .limit(TETO_IMPRESSAO)
            .toList();

        System.out.printf("%n[1] FALA CURTA QUE FICOU EM INGLES (candidato forte a glossario)%n"
            + "    %-28s %6s %6s  %s%n", "fala EN", "vezes", "total", "traducoes vistas");
        for (Candidato c : ranking) {
            System.out.printf("    %-28s %6d %6d  %s%n",
                recortar(c.chaveEn()), c.vezesNaoTraduzida(), c.ocorrencias(),
                amostraTraducoes(c));
        }
        if (ranking.isEmpty()) {
            System.out.println("    (nenhuma) — o pipeline nao esta deixando fala curta em ingles");
        }
    }

    /**
     * Mesma fala EN traduzida de várias formas. Não é defeito por si — contexto muda a tradução
     * legitimamente —, mas é onde a inconsistência de terminologia aparece, e é o que uma entrada
     * determinística resolveria se as variantes forem equivalentes.
     */
    private static void relatarDivergentes(List<Candidato> candidatos) {
        List<Candidato> ranking = candidatos.stream()
            .filter(c -> c.divergencia() >= 3)
            .sorted(Comparator.comparingInt(Candidato::ocorrencias).reversed())
            .limit(TETO_IMPRESSAO)
            .toList();

        System.out.printf("%n[2] MESMA FALA EN, MUITAS GRAFIAS PT (inconsistencia de terminologia)%n"
            + "    %-28s %6s %6s  %s%n", "fala EN", "total", "formas", "traducoes vistas");
        for (Candidato c : ranking) {
            System.out.printf("    %-28s %6d %6d  %s%n",
                recortar(c.chaveEn()), c.ocorrencias(), c.divergencia(), amostraTraducoes(c));
        }
        if (ranking.isEmpty()) {
            System.out.println("    (nenhuma com 3+ formas)");
        }
    }

    /** As grafias mais frequentes, com a contagem — sem isso a lista não é julgável. */
    private static String amostraTraducoes(Candidato c) {
        return c.traducoes().entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(4)
            .map(e -> recortar(e.getKey()) + " (" + e.getValue() + ")")
            .reduce((a, b) -> a + " | " + b).orElse("");
    }

    private static int contaPalavras(String chave) {
        return chave.isEmpty() ? 0 : chave.split(" ").length;
    }

    private static String recortar(String t) {
        return t.length() <= 26 ? t : t.substring(0, 26) + "…";
    }
}
