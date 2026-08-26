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
import java.util.regex.Pattern;

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
        if (!cortesCalibrados()) {
            return;
        }
        if (acervo.vazio()) {
            System.out.println("NAO VERIFICADO: acervo de cache vazio — 'nenhum candidato a "
                + "glossario' aqui seria cegueira, e nao acervo sem termo recorrente.");
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
        relatarEvidencia(acervo);
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

    /**
     * Chaves EN cujas evidências CRUAS interessam. Contagem prova que o padrão existe; só a fala
     * inteira, com a obra e o arquivo, permite julgar se é DEFEITO ou variação legítima. Sem isto
     * a lista anterior vira palpite com número do lado — que é exatamente o que ela existe para
     * substituir.
     */
    private static final List<String> SOB_SUSPEITA = List.of("fa", "mobile suit");

    /** Para cada chave sob suspeita, falas REAIS onde a tradução divergiu da forma dominante. */
    private static void relatarEvidencia(Acervo acervo) {
        System.out.printf("%n[3] EVIDENCIA CRUA — a fala inteira, para julgar defeito x variacao%n");
        for (String alvo : SOB_SUSPEITA) {
            List<FalaDoAcervo> casos = acervo.falas().stream()
                .filter(f -> alvo.equals(EnforcadorGlossarioFala.chaveDeFala(f.original())))
                .toList();
            if (casos.isEmpty()) {
                continue;
            }
            Map<String, List<FalaDoAcervo>> porTraducao = new LinkedHashMap<>();
            for (FalaDoAcervo f : casos) {
                porTraducao.computeIfAbsent(
                    EnforcadorGlossarioFala.chaveDeFala(f.traduzido()),
                    k -> new java.util.ArrayList<>()).add(f);
            }
            Map<String, Integer> porObra = new TreeMap<>();
            casos.forEach(f -> porObra.merge(f.obra(), 1, Integer::sum));
            System.out.printf("%n  == \"%s\" (%d falas, %d grafias) ==%n     obras: %s%n",
                alvo, casos.size(), porTraducao.size(),
                porObra.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .map(e -> recortarLargo(e.getKey()) + "=" + e.getValue())
                    .reduce((a, b) -> a + ", " + b).orElse(""));
            porTraducao.entrySet().stream()
                .sorted(Map.Entry.<String, List<FalaDoAcervo>>comparingByValue(
                    Comparator.comparingInt(List::size)).reversed())
                .forEach(e -> {
                    FalaDoAcervo amostra = e.getValue().get(0);
                    System.out.printf("     %4dx  EN %-34s PT %-34s [%s]%n",
                        e.getValue().size(), recortarLargo(amostra.original()),
                        recortarLargo(amostra.traduzido()), recortarLargo(amostra.obra()));
                });
        }
        for (String ruim : List.of("Fogo", "Fala", "Fá", "Pá", "Fale")) {
            relatarColisao(acervo, "Fa", ruim);
        }
        for (String ruim : List.of("Móvel de Assalto", "Móvel de Assento",
            "Móvel de Combate", "Móvel de Guerra", "Mobil Suit")) {
            relatarColisao(acervo, "Mobile Suit", ruim);
        }
        relatarCaixaDaFormaRuim(acervo, "Móvel de Combate");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a CAIXA separa cartão de título de diálogo?
     *
     * <p>{@code "Móvel de Combate"} é exclusão deliberada do dono do acervo: em DIÁLOGO a forma é
     * aceitável por contexto. Mas ela também aparece em CARTÃO DE TÍTULO, onde é o nome da
     * franquia na tela e não há contexto que a salve. O mapa de terminologia não distingue os
     * dois — a menos que exista um sinal no próprio texto.
     *
     * <p>A hipótese é a caixa: o cartão traz o inglês em CAIXA ALTA ({@code "MOBILE SUIT"}) e o
     * diálogo não. Isto mede a hipótese antes de qualquer mudança — separar por um sinal que não
     * separa de verdade destruiria as falas de diálogo que a exclusão protege.
     */
    private static void relatarCaixaDaFormaRuim(Acervo acervo, String formaRuim) {
        Map<String, Integer> porCaixa = new TreeMap<>();
        List<String> amostras = new java.util.ArrayList<>();
        for (FalaDoAcervo f : acervo.falas()) {
            String pt = f.traduzido();
            if (!pt.toLowerCase(java.util.Locale.ROOT)
                .contains(formaRuim.toLowerCase(java.util.Locale.ROOT))) {
                continue;
            }
            String en = recortarLargo(f.original());
            String ptVisivel = recortarLargo(pt);
            boolean enMaiusculo = en.equals(en.toUpperCase(java.util.Locale.ROOT));
            boolean ptCapitalizado = pt.contains(formaRuim);
            String chave = (enMaiusculo ? "EN-CAIXA-ALTA" : "en-normal")
                + " / " + (ptCapitalizado ? "PT-Capitalizado" : "pt-minusculo");
            porCaixa.merge(chave, 1, Integer::sum);
            if (amostras.size() < 8) {
                amostras.add(String.format("     %-30s EN %-30s PT %s", chave, en, ptVisivel));
            }
        }
        System.out.printf("%n  >> CAIXA de \"%s\" — a caixa separa cartao de dialogo?%n", formaRuim);
        porCaixa.forEach((k, v) -> System.out.printf("     %-34s %d%n", k, v));
        amostras.forEach(System.out::println);
    }

    /**
     * Tira as tags ASS ANTES de recortar. A primeira versão recortava o texto cru, e em fala de
     * cartão de título — {@code {\blur0.4\fade(480,0)\fscx120…}} — as tags sozinhas estouravam o
     * limite: a evidência saía sem uma letra do texto. Duas conclusões foram tiradas em cima
     * dessa saída vazia antes de alguém reparar. Evidência ilegível é pior que evidência ausente,
     * porque parece evidência.
     */
    /**
     * PROPÓSITO DE NEGÓCIO: risco de COLISÃO de uma correção de terminologia proposta.
     *
     * <p>{@code correcoesTerminologia} é condicional — só age quando o ORIGINAL contém o termo
     * canônico. Mapear {@code "Fogo" -> "Fa"} é seguro para um {@code "Fire!"} qualquer, que
     * nunca traz "Fa" no inglês. O que NÃO é seguro é a fala que traz as DUAS coisas: o nome Fa
     * <b>e</b> fogo de verdade ({@code "Fa, fire!"}). Ali a restauração trocaria a palavra errada.
     *
     * <p>Contar isso ANTES de aplicar é a diferença entre regra medida e palpite: o teto de
     * {@code restaurarLimitado} protege o homógrafo MINÚSCULO por orçamento, não por semântica.
     */
    private static void relatarColisao(Acervo acervo, String canonicoEn, String formaRuimPt) {
        // (?i) NAO e detalhe: sem ele a comparacao com o canonico e sensivel a caixa e a fala
        // "a combat mobile suit" (minusculo no ingles) escapa de "Mobile Suit". Foi assim que
        // este relatorio devolveu ZERO colisao para "Movel de Combate" em 04/08/2026 quando
        // havia UMA — e o zero seria usado para autorizar a regra. Ferramenta de medicao que
        // erra para menos e pior que nenhuma: ela produz confianca.
        List<FalaDoAcervo> colisoes = acervo.falas().stream()
            .filter(f -> f.original().matches("(?si).*\\b" + Pattern.quote(canonicoEn) + "\\b.*"))
            .filter(f -> f.traduzido().toLowerCase(java.util.Locale.ROOT)
                .contains(formaRuimPt.toLowerCase(java.util.Locale.ROOT)))
            .filter(f -> !EnforcadorGlossarioFala.chaveDeFala(f.traduzido())
                .equals(formaRuimPt.toLowerCase(java.util.Locale.ROOT)))
            .toList();
        System.out.printf("%n  >> COLISAO \"%s\"/\"%s\": %d falas trazem o canonico EN e a "
                + "forma-ruim PT SEM a fala inteira ser a forma-ruim%n",
            canonicoEn, formaRuimPt, colisoes.size());
        colisoes.stream().limit(6).forEach(f -> System.out.printf(
            "       EN %-40s PT %-40s [%s]%n",
            recortarLargo(f.original()), recortarLargo(f.traduzido()), f.obra()));
    }

    private static String recortarLargo(String t) {
        String limpo = t.replaceAll("\\{[^}]*}", "")
            .replace("\\N", " ")
            .replace(System.lineSeparator(), " ")
            .strip();
        return limpo.length() <= 34 ? limpo : limpo.substring(0, 34) + "…";
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

    /**
     * PROPÓSITO DE NEGÓCIO: CASO-CONTROLE (regra 9) dos dois cortes que decidem o que vira
     * candidato — {@link #MAX_PALAVRAS} e {@link #MIN_OCORRENCIAS}.
     *
     * <p>INVARIANTES DO DOMÍNIO: o contador tem de separar a expressão curta (candidata) da
     * frase longa (descartada), e a contagem por chave tem de somar. Um {@code contaPalavras}
     * que devolvesse sempre 1 mandaria o acervo inteiro ao glossário; um que devolvesse sempre
     * um número alto devolveria lista vazia — e "nenhum termo recorrente" é uma conclusão que
     * este harness não pode emitir por cegueira.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: imprime e devolve {@code false}; nenhum candidato é
     * afirmado.
     */
    private static boolean cortesCalibrados() {
        boolean deixaPassarOcurto = contaPalavras("mobile suit") <= MAX_PALAVRAS;
        boolean barraOlongo =
            contaPalavras("this is a full sentence that no glossary wants") > MAX_PALAVRAS;
        boolean contaVazioComoZero = contaPalavras("") == 0;
        if (deixaPassarOcurto && barraOlongo && contaVazioComoZero) {
            System.out.printf("  controle dos cortes: 'mobile suit' passa · frase longa e barrada "
                + "· vazio conta 0  (max %d palavras, min %d ocorrencias)%n",
                MAX_PALAVRAS, MIN_OCORRENCIAS);
            return true;
        }
        System.out.printf("CORTES REPROVADOS NO CONTROLE — curto=%s longo=%s vazio=%s. Nenhum "
            + "candidato e afirmado.%n", deixaPassarOcurto, barraOlongo, contaVazioComoZero);
        return false;
    }

    private static String recortar(String t) {
        return t.length() <= 26 ? t : t.substring(0, 26) + "…";
    }
}
