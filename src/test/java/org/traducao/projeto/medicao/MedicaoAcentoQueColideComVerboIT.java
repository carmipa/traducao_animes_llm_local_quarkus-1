package org.traducao.projeto.medicao;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.revisaoConcordancia.application.CorretorAcentoDeDicionarioNaFalaService;
import org.traducao.projeto.revisaoConcordancia.application.CorretorAcentoPorPadraoService;
import org.traducao.projeto.revisaoConcordancia.application.CorretorAcentoQueColideComVerboService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: medir, no acervo, o que a reposição de acento da 3.3 <b>faria</b> se
 * rodasse hoje — quantas falas, em que obras, e trocando o quê por quê.
 *
 * <h2>A primeira versão deste arquivo estava ERRADA, e o erro fica escrito aqui</h2>
 * Em 23/08/2026 esta medição foi escrita perguntando ao <b>dicionário</b>: se a palavra está sem
 * acento e existe uma variante acentuada, seria falta de acento. Rodou no Macross II e produziu
 * lixo, com uma cara muito convincente:
 *
 * <pre>
 *   terra   -> terrá      batalha -> batalhá     garota -> garotá
 *   sistema -> sistemá    combate -> combatê     minha  -> minhã
 * </pre>
 *
 * O hunspell aceita <b>as duas formas de cada par</b> — {@code terra} é o substantivo e
 * {@code terrá} é o futuro de {@code terrar}. Saber que a variante acentuada existe não diz qual
 * das duas é a certa <b>naquela frase</b>. É exatamente a lição que este projeto já tinha
 * registrado em 19/08/2026, reconstruída numa forma nova: a existência da palavra não decide
 * direção; só a classe gramatical no contexto decide.
 *
 * <p>E havia um segundo defeito, este de custo: 2.911 formas levaram <b>240 segundos</b> de
 * dicionário para UMA obra. No acervo inteiro seriam horas — o instrumento nunca foi viável.
 *
 * <h2>O que ele é agora: a produção respondendo por si</h2>
 * Esta versão não decide nada. Ela pergunta ao
 * {@link CorretorAcentoQueColideComVerboService} — o MESMO objeto que a tela 3.3 usa, com o mesmo
 * POS tagger, as mesmas categorias e as mesmas travas — <i>o que você faria com esta fala?</i>.
 * Critério que a produção já implementa é CONSULTADO, nunca copiado: a segunda implementação
 * sempre diverge, e a divergência só aparece quando já estragou arquivo.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>READ-ONLY. Mede e imprime; não escreve no acervo.</li>
 *   <li>Mesmo universo das telas: sem música (pela {@link PoliticaEstiloMusical}, dona da
 *       pergunta) e sem {@code .parcial}.</li>
 *   <li>Revisor gramatical indisponível termina como <b>NÃO VERIFICADO</b> e não afirma número —
 *       zero por motor morto tem a mesma cara de acervo limpo, e essa é a confusão que a
 *       invariante 12 proíbe.</li>
 *   <li>Filtro opcional por obra ({@code -Dkronos.medicao.obra}) e progresso por obra: sem eles,
 *       "trabalhando" e "parada" têm a mesma cara no terminal.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Acervo ausente ou filtro sem casamento termina declarando isso, sem número.
 *
 * <p>Uso: {@code gradlew test --tests "*MedicaoAcentoQueColideComVerboIT*" "-Dkronos.medicao=true"}
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoAcentoQueColideComVerboIT {

    private static final Path RAIZ = Path.of(System.getProperty("kronos.acervo", "C:\\animes"));

    /**
     * Filtro OPCIONAL por obra ({@code -Dkronos.medicao.obra=Macross}). Existe por disciplina:
     * provar o instrumento numa obra antes de soltá-lo em 236 arquivos custa trinta segundos, e
     * foi pular esse passo que fez a primeira versão rodar meia hora produzindo lixo.
     */
    private static final String FILTRO_OBRA = System.getProperty("kronos.medicao.obra", "");
    private static final int AMOSTRAS = 3;

    @Inject
    LeitorLegendaAss leitor;

    @Inject
    PoliticaEstiloMusical politicaEstiloMusical;

    @Inject
    CorretorAcentoQueColideComVerboService corretor;

    /**
     * A CADEIA INTEIRA, e não um elo só. Medir um corretor isolado responde "o que ESTE faria",
     * que não é a pergunta: o operador vai clicar na tela, e a tela roda os quatro em sequência.
     * Um elo pode desfazer ou tornar redundante o trabalho do anterior, e só a cadeia mostra isso.
     */
    @Inject
    CorretorAcentoPorPadraoService corretorPadrao;

    @Inject
    CorretorAcentoDeDicionarioNaFalaService corretorDicionario;

    /** O caso de uso INTEIRO — quem a tela chama, e portanto quem sabe o tempo de verdade. */
    @Inject
    org.traducao.projeto.revisaoConcordancia.application.RevisarConcordanciaUseCase revisarConcordancia;

    /** Uma fala que MUDARIA, com o antes e o depois inteiros para leitura humana. */
    private record Mudanca(String obra, String antes, String depois) {}

    @Test
    @DisplayName("mede o que a reposicao de acento da 3.3 faria no acervo, perguntando a producao")
    void medir() throws IOException {
        System.out.printf("%n=== ACENTO QUE COLIDE COM VERBO — o que a 3.3 faria em %s ===%n", RAIZ);
        if (!Files.isDirectory(RAIZ)) {
            System.out.println("NAO VERIFICADO: acervo ausente em " + RAIZ);
            return;
        }
        if (!corretor.disponivel()) {
            System.out.println("NAO VERIFICADO: revisor gramatical indisponivel — "
                + corretor.motivoDaIndisponibilidade()
                + ". Zero mudancas aqui teria a mesma cara de acervo limpo, entao nenhum numero "
                + "e afirmado.");
            return;
        }

        // CONTROLE, no mesmo experimento: se o corretor nao fizer o obvio, nada abaixo vale.
        Optional<String> positivo = corretor.corrigir("A milicia ordenou um blackout de noticias.");
        Optional<String> negativo = corretor.corrigir("O reporter noticia o caso todo dia.");
        // Controle dos elos NOVOS, no mesmo experimento: se qualquer um deles nao fizer o obvio,
        // nenhum numero do acervo vale.
        Optional<String> padraoOk = corretorPadrao.corrigir("Isso e tudo.");
        Optional<String> padraoNao = corretorPadrao.corrigir("Judau, isso e aquilo.");
        // A fala do controle mudou em 24/08/2026: `Territorio` maiusculo deixou de ser
        // corrigido de proposito (guarda de nome proprio), entao o controle passou a usar a
        // forma minuscula. Controle que exercita um caminho desligado nao prova nada.
        Optional<String> dicOk = corretorDicionario.corrigir("Nao ha territorio inimigo aqui.");
        Optional<String> dicNao = corretorDicionario.corrigir("Aquele e o mobile armor Apsaras.");
        if (padraoOk.isEmpty() || padraoNao.isPresent() || dicOk.isEmpty()
            || (dicNao.isPresent() && dicNao.get().contains("Apsarás"))) {
            System.out.printf("INSTRUMENTO REPROVADO NO CONTROLE DOS ELOS NOVOS — "
                + "padrao(+)=%s padrao(-)=%s dicionario(+)=%s dicionario(-)=%s%n",
                padraoOk, padraoNao, dicOk, dicNao);
            return;
        }
        System.out.printf("  controle dos elos novos: padrao ok (%s) · dicionario ok (%s)%n",
            padraoOk.get(), dicOk.get());
        if (positivo.isEmpty() || negativo.isPresent()) {
            System.out.printf("INSTRUMENTO REPROVADO NO CONTROLE — positivo=%s negativo=%s. "
                + "Nenhum numero do acervo vale.%n", positivo, negativo);
            return;
        }
        System.out.printf("  controle: POSITIVO ok (%s) · NEGATIVO ok (nao tocou no verbo)%n",
            positivo.get());

        List<Path> obras;
        try (Stream<Path> s = Files.list(RAIZ)) {
            obras = s.filter(Files::isDirectory)
                .filter(o -> FILTRO_OBRA.isBlank()
                    || o.getFileName().toString().toLowerCase().contains(FILTRO_OBRA.toLowerCase()))
                .sorted().toList();
        }
        if (obras.isEmpty()) {
            System.out.println("NAO VERIFICADO: nenhuma obra casou o filtro \"" + FILTRO_OBRA
                + "\". Zero por filtro errado tem a mesma cara de acervo limpo.");
            return;
        }
        if (!FILTRO_OBRA.isBlank()) {
            System.out.printf("  FILTRO por obra: \"%s\" -> %d pasta(s)%n", FILTRO_OBRA, obras.size());
        }

        List<Mudanca> mudancas = new ArrayList<>();
        Map<String, Integer> falasPorObra = new TreeMap<>();
        Map<String, Integer> mudancasPorObra = new TreeMap<>();
        int falasTotal = 0;
        long t0 = System.currentTimeMillis();

        for (Path obra : obras) {
            String nome = obra.getFileName().toString();
            List<Path> arquivos;
            try (Stream<Path> s = Files.walk(obra)) {
                arquivos = s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".ass"))
                    .filter(p -> !p.getFileName().toString().toLowerCase().contains(".parcial."))
                    .filter(p -> p.getParent() != null
                        && "traducao_ptbr".equals(p.getParent().getFileName().toString()))
                    .sorted().toList();
            }
            if (arquivos.isEmpty()) {
                continue;
            }
            int falasObra = 0;
            int mudancasObra = 0;
            for (Path arquivo : arquivos) {
                DocumentoLegenda documento;
                try {
                    documento = leitor.ler(arquivo);
                } catch (RuntimeException e) {
                    continue;
                }
                // AQUECIMENTO, uma vez por arquivo — o mesmo que o caso de uso da tela faz.
                //
                // Consertar so a producao e deixar a medicao no caminho lento faz a medicao
                // custar horas e, pior, medir um sistema que ja nao existe. Foi o que aconteceu
                // em 24/08/2026: o use case ja aquecia e este laco continuava pagando um processo
                // externo por fala, e a varredura do acervo travou de novo pelo mesmo motivo que
                // eu acabara de corrigir.
                List<String> falasDoArquivo = documento.eventos().stream()
                    .filter(EventoLegenda::temTexto)
                    .filter(e -> e.estilo() == null || !politicaEstiloMusical.estiloIgnorado(e.estilo()))
                    .map(EventoLegenda::texto)
                    .toList();
                corretorDicionario.aquecerCom(falasDoArquivo);

                for (EventoLegenda evento : documento.eventos()) {
                    if (!evento.temTexto()) {
                        continue;
                    }
                    if (evento.estilo() != null && politicaEstiloMusical.estiloIgnorado(evento.estilo())) {
                        continue;
                    }
                    falasObra++;
                    // MESMA ordem da tela, pelos MESMOS objetos: POS tagger, padroes curados,
                    // dicionario. O corretor de genero fica de fora aqui de proposito — ele ja
                    // rodou no acervo e esta medicao e sobre ACENTO.
                    String base = evento.texto();
                    String passo = corretor.corrigir(base).orElse(base);
                    passo = corretorPadrao.corrigir(passo).orElse(passo);
                    passo = corretorDicionario.corrigir(passo).orElse(passo);
                    Optional<String> nova = passo.equals(base) ? Optional.empty() : Optional.of(passo);
                    if (nova.isPresent()) {
                        mudancasObra++;
                        mudancas.add(new Mudanca(nome, evento.texto(), nova.get()));
                    }
                }
            }
            falasTotal += falasObra;
            falasPorObra.put(nome, falasObra);
            mudancasPorObra.put(nome, mudancasObra);
            // Progresso POR OBRA, com o numero DELA e nao o acumulado: a primeira versao imprimia
            // o total corrente e as tres pastas Macross sairam todas com "1381 falas", o que fez
            // parecer que cada uma tinha o mesmo conteudo.
            System.out.printf("    [%-26s] %6d falas · %4d mudariam%n",
                curto(nome), falasObra, mudancasObra);
        }

        long ms = System.currentTimeMillis() - t0;
        System.out.printf("%n  falas ao alcance ....... %d em %d obra(s)%n", falasTotal, falasPorObra.size());
        System.out.printf("  falas que MUDARIAM ..... %d%n", mudancas.size());
        System.out.printf("  custo .................. %d ms (%.2f ms por fala)%n",
            ms, falasTotal == 0 ? 0.0 : (double) ms / falasTotal);

        System.out.println("\n--- POR OBRA ---");
        mudancasPorObra.entrySet().stream()
            .sorted(Comparator.comparingInt((Map.Entry<String, Integer> e) -> -e.getValue()))
            .forEach(e -> System.out.printf("  %4d de %6d falas   %s%n",
                e.getValue(), falasPorObra.getOrDefault(e.getKey(), 0), curto(e.getKey())));

        // O PAR trocado, que e o que decide se a mudanca e boa: agrupado por palavra, com amostra.
        Map<String, List<Mudanca>> porPar = new TreeMap<>();
        for (Mudanca m : mudancas) {
            porPar.computeIfAbsent(parTrocado(m), k -> new ArrayList<>()).add(m);
        }
        System.out.printf("%n--- PARES TROCADOS (%d distintos), por frequencia ---%n", porPar.size());
        porPar.entrySet().stream()
            .sorted(Comparator.comparingInt((Map.Entry<String, List<Mudanca>> e) -> -e.getValue().size()))
            .forEach(e -> {
                System.out.printf("%n  %4d  %s%n", e.getValue().size(), e.getKey());
                e.getValue().stream().limit(AMOSTRAS).forEach(m ->
                    System.out.printf("          [%s] %s%n", curto(m.obra()), recorte(m.depois())));
            });
    }

    /** As palavras que realmente mudaram, no formato {@code antes -> depois}. */
    private static String parTrocado(Mudanca m) {
        String[] a = m.antes().split("\\s+");
        String[] b = m.depois().split("\\s+");
        if (a.length != b.length) {
            return "(estrutura mudou)";
        }
        List<String> pares = new ArrayList<>();
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(b[i])) {
                pares.add(a[i] + " -> " + b[i]);
            }
        }
        return pares.isEmpty() ? "(sem diferenca de palavra)" : String.join(" · ", pares);
    }

    private static String recorte(String texto) {
        String t = texto.replace("\\N", " ").replaceAll("\\{[^{}]*}", "");
        return t.length() > 84 ? t.substring(0, 84) : t;
    }

    private static String curto(String obra) {
        return obra.length() > 26 ? obra.substring(0, 26) : obra;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: dizer <b>onde vai o tempo</b> da tela 3.3, elo por elo, perguntando ao
     * caso de uso que a tela usa — em modo simulação, sem escrever nada.
     *
     * <h2>A cicatriz</h2>
     * Em 24/08/2026 uma passada sobre SEIS arquivos levou 5min15s, e o detector de travamento do
     * Quarkus disparou apontando o dicionário. Não estava travada: estava lenta. Para quem olha a
     * tela as duas coisas são idênticas, e sem relógio por elo a única forma de descobrir o
     * culpado era desmontar a cadeia na mão. Este caso existe para nunca mais precisar disso.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code aplicar=false} — nada é gravado. O placar vem do caso de
     * uso de PRODUÇÃO, não de um cronômetro reimplementado aqui.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: pasta ausente termina declarando, sem número.
     */
    @Test
    @DisplayName("mede ONDE vai o tempo da 3.3, elo por elo, pelo caso de uso de producao")
    void medirOndeVaiOtempo() throws IOException {
        System.out.printf("%n=== ONDE VAI O TEMPO DA 3.3 (simulacao, nada e gravado) ===%n");
        if (!Files.isDirectory(RAIZ)) {
            System.out.println("NAO VERIFICADO: acervo ausente em " + RAIZ);
            return;
        }
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
            System.out.println("NAO VERIFICADO: nenhuma pasta casou com o filtro '" + FILTRO_OBRA + "'");
            return;
        }
        for (Path pasta : pastas) {
            long comeco = System.nanoTime();
            var r = revisarConcordancia.revisarPasta(pasta, false);
            double segundos = (System.nanoTime() - comeco) / 1_000_000_000.0;
            // As falas vem do PLACAR e nao de um contador proprio: quem viu as falas foram os
            // corretores, e `vistas()` e a soma que eles mesmos reportam.
            int falas = r.porCorretor().isEmpty() ? 0 : r.porCorretor().get(0).vistas();
            System.out.printf("%n--- %s%n    %d arquivos · %d falas · %d corrigidas · %.1fs no total%n",
                pasta.getParent().getFileName(), r.arquivosAnalisados(), falas,
                r.falasCorrigidas(), segundos);
            for (var c : r.porCorretor()) {
                System.out.println("      " + c.linhaDeRelatorio());
            }
        }
    }
    /**
     * A frase EXATA que autoriza escrever no acervo. Não é senha: é uma trava contra o dedo
     * errado. Rodar a medição e rodar a gravação não podem se parecer na linha de comando, porque
     * em 24/08/2026 Paulo marcou "Apenas simular" na tela e leu {@code [APLICADO]} no console —
     * era de outra execução, mas o susto foi real e a lição é a mesma: modo destrutivo tem de ser
     * impossível de acionar por engano.
     */
    private static final String AUTORIZACAO = "SIM-ESCREVER-NO-ACERVO";
    private static final String CHAVE_ESCRITA = "kronos.aplicar.concordancia";

    /**
     * PROPÓSITO DE NEGÓCIO: roda a tela 3.3 no acervo <b>gravando</b>, e prova na mesma execução
     * que a segunda passada não muda mais nada.
     *
     * <h2>Por que este método existe separado, e não como uma flag do outro</h2>
     * Medir e gravar num método só é a armadilha de boa-fé que este projeto já pagou: quem quer
     * simular passa a acionar a gravação por uma letra trocada. São dois métodos, dois nomes, duas
     * propriedades — e sem a autorização exata isto é um ENSAIO que não toca em byte nenhum.
     *
     * <h2>Quem grava é a PRODUÇÃO</h2>
     * O laço aqui não escreve arquivo: ele chama {@code revisarPasta(pasta, true)}, o mesmo caso
     * de uso que o botão da tela chama, com os mesmos backups. Uma segunda porta de escrita para
     * o acervo é dívida conhecida deste projeto — a família {@code Aplicar*} gravou 103 linhas de
     * romaji justamente por ser uma porta paralela que ninguém vigiava.
     *
     * <h2>Invariantes do domínio</h2>
     * <ul>
     *   <li>Sem {@code -Dkronos.aplicar.concordancia=SIM-ESCREVER-NO-ACERVO} nada é gravado.</li>
     *   <li>Backup por arquivo alterado — quem faz é o caso de uso, e o total sai no relatório.</li>
     *   <li><b>Segunda passada obrigatória.</b> Se ela corrigir alguma coisa, a correção não é
     *       idempotente e o teste REPROVA: corretor que muda o próprio resultado a cada rodada
     *       degrada a legenda em silêncio a cada clique.</li>
     * </ul>
     *
     * <h2>Comportamento em caso de falha</h2>
     * Acervo ausente termina declarando. Segunda passada não-vazia reprova nomeando as pastas.
     */
    @Test
    @DisplayName("APLICA a 3.3 no acervo (so com autorizacao) e prova a idempotencia na 2a passada")
    void aplicarNoAcervo() throws IOException {
        String recebida = System.getProperty(CHAVE_ESCRITA);
        boolean escrever = AUTORIZACAO.equals(recebida);
        System.out.printf("%n=== 3.3 NO ACERVO — modo %s ===%n",
            escrever ? "!! ESCRITA !!" : "ENSAIO (nada e gravado)");
        // NAO BASTA DIZER "ENSAIO". Em 25/08/2026 a autorizacao foi passada certa na linha de
        // comando e nao chegou aqui: a lista de propriedades encaminhadas ao JVM de teste e
        // NOMINAL, e a chave nova nao estava nela. O harness disse "ENSAIO" e nada mais, entao
        // "ninguem autorizou" e "a autorizacao se perdeu no caminho" sairam identicos — que e a
        // ambiguidade de saida vazia, aqui aplicada a uma trava de escrita.
        if (!escrever) {
            System.out.printf("  autorizacao: -D%s=%s%n", CHAVE_ESCRITA,
                recebida == null
                    ? "(NAO CHEGOU ao JVM de teste — ou nao foi passada, ou falta declarar a "
                        + "chave na lista nominal do build.gradle)"
                    : "'" + recebida + "' — DIFERENTE do exigido '" + AUTORIZACAO + "'");
        }
        if (!Files.isDirectory(RAIZ)) {
            System.out.println("NAO VERIFICADO: acervo ausente em " + RAIZ);
            return;
        }
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
            System.out.println("NAO VERIFICADO: nenhuma pasta casou com o filtro '" + FILTRO_OBRA + "'");
            return;
        }

        int totalPrimeira = 0;
        int totalBackups = 0;
        Map<String, Integer> naSegunda = new TreeMap<>();
        Map<String, int[]> porElo = new TreeMap<>();

        for (Path pasta : pastas) {
            String obra = pasta.getParent().getFileName().toString();
            var primeira = revisarConcordancia.revisarPasta(pasta, escrever);
            totalPrimeira += primeira.falasCorrigidas();
            totalBackups += primeira.backups().size();
            for (var c : primeira.porCorretor()) {
                porElo.computeIfAbsent(c.nome(), k -> new int[1])[0] += c.agiu();
            }
            System.out.printf("  %-46s %4d arquivos · %4d corrigidas · %d backup(s)%n",
                obra.length() > 46 ? obra.substring(0, 46) : obra,
                primeira.arquivosAnalisados(), primeira.falasCorrigidas(),
                primeira.backups().size());

            if (!escrever) {
                continue;
            }
            // SEGUNDA PASSADA, sempre em SIMULACAO: o que ela acusar e o que a primeira deixou
            // por fazer ou o que ela mesma introduziu. Gravar de novo aqui esconderia o defeito
            // consertando-o.
            var segunda = revisarConcordancia.revisarPasta(pasta, false);
            if (segunda.falasCorrigidas() > 0) {
                naSegunda.put(obra, segunda.falasCorrigidas());
            }
        }

        System.out.printf("%n  TOTAL: %d falas em %d pasta(s) · %d backup(s)%n",
            totalPrimeira, pastas.size(), totalBackups);
        System.out.println("  Por elo da cadeia:");
        porElo.forEach((nome, n) -> System.out.printf("      %-28s %5d%n", nome, n[0]));

        if (!escrever) {
            System.out.printf("%n  ENSAIO: nada foi gravado. Para aplicar de verdade, acrescente%n"
                + "    \"-D%s=%s\"%n", CHAVE_ESCRITA, AUTORIZACAO);
            return;
        }
        System.out.printf("%n  IDEMPOTENCIA (2a passada, simulada): %s%n",
            naSegunda.isEmpty() ? "LIMPA — nada mudaria de novo" : naSegunda.toString());
        assertTrue(naSegunda.isEmpty(),
            "a segunda passada ainda corrigiria falas, entao a correcao NAO e idempotente e cada "
                + "clique muda o arquivo de novo: " + naSegunda);
    }
}
