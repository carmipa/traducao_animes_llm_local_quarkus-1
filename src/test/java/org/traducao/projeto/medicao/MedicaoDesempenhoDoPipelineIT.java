package org.traducao.projeto.medicao;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.revisaoConcordancia.application.RevisarConcordanciaUseCase;
import org.traducao.projeto.revisaoConcordancia.domain.ContagemCorretor;
import org.traducao.projeto.revisaoConcordancia.domain.ResultadoConcordancia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: responder <b>"onde o KRONOS gasta o tempo?"</b> com número por operação,
 * medido sobre fala REAL do acervo e com os objetos de PRODUÇÃO.
 *
 * <h2>Por que este harness existe</h2>
 * O projeto já pagou três vezes por não ter esta medição:
 * <ul>
 *   <li><b>291,1s → 14,4s</b> numa passada da tela 3.3, quando se descobriu que o elo do
 *       dicionário respondia por 97% do tempo e não trazia ganho proporcional. A memória evitava
 *       a SEGUNDA pergunta sobre uma palavra, nunca a primeira — e a primeira arranca um
 *       processo.</li>
 *   <li><b>15 minutos parados</b> num único arquivo (o CCA, 2.743 formas distintas): o dicionário
 *       ia ao processo externo de uma vez só e estourava o timeout, levando junto TODAS as
 *       palavras do arquivo.</li>
 *   <li><b>5 minutos sobre seis arquivos</b>, sem nenhum jeito de saber qual dos cinco elos da
 *       cadeia gastava o tempo — foi o que fez o relógio entrar por elo em {@link
 *       ContagemCorretor}.</li>
 * </ul>
 *
 * <p>Nos três casos o custo apareceu <b>em produção</b>, com o operador esperando. Um número por
 * operação, gravado a cada medição, é o que transforma "está lento" em "está lento AQUI".
 *
 * <h2>O relógio é CONSULTADO, não recriado</h2>
 * A cadeia da 3.3 já cronometra cada elo e devolve isso em {@link ResultadoConcordancia}. Este
 * harness lê aqueles nanos; não abre a cadeia para cronometrar por fora. Segunda implementação de
 * uma medição diverge da primeira — e aqui divergiria em silêncio, porque os dois números teriam
 * a mesma cara.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>READ-ONLY sobre o acervo. A cópia medida vive em pasta temporária.</li>
 *   <li>Grava {@code relatorios/desempenho.json} — é ele que o painel "Desempenho" da aplicação
 *       exibe. Harness que afirma número e não deixa artefato obriga a repetir a corrida para
 *       conferir.</li>
 *   <li>Toda linha traz o custo POR UNIDADE, e não só o total: total cresce com a pasta e não
 *       distingue "operação cara" de "pasta grande".</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Acervo ausente, amostra vazia ou relógio parado terminam declarando NÃO VERIFICADO — nenhum
 * tempo é afirmado.
 *
 * <p>Uso: {@code gradlew test --tests "*MedicaoDesempenhoDoPipelineIT*" "-Dkronos.medicao=true"
 * "-Dkronos.acervo=C:\animes\ANIMES-TESTES"}
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoDesempenhoDoPipelineIT {

    /** Amostra por operação. Grande o bastante para a média valer, pequena para caber num teste. */
    private static final int AMOSTRA_DE_FALAS = 400;
    private static final int AMOSTRA_DE_PALAVRAS = 300;
    private static final Path SAIDA = Path.of("relatorios", "desempenho.json");

    private static final String PREFIXO =
        "Dialogue: 0,0:00:00.00,0:00:02.00,Default,,0,0,0,,";

    @Inject
    LeitorLegendaAss leitor;

    @Inject
    EscritorLegendaAss escritor;

    @Inject
    PoliticaEstiloMusical politicaEstiloMusical;

    @Inject
    CorretorOrtograficoLegenda dicionario;

    @Inject
    RevisarConcordanciaUseCase revisor;

    /** Uma linha do relatório: o que se mediu, quantas unidades, e o custo de cada uma. */
    private record Medida(String operacao, String unidade, long unidades, long nanos,
                          String observacao) {

        double msPorUnidade() {
            return unidades == 0 ? 0.0 : nanos / 1_000_000.0 / unidades;
        }

        double segundos() {
            return nanos / 1_000_000_000.0;
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CASO-CONTROLE (regra 9) do RELÓGIO — antes de afirmar "esta operação
     * custa X ms", é preciso saber que o cronômetro distingue rápido de lento.
     *
     * <p>INVARIANTES DO DOMÍNIO: uma pausa de 40 ms tem de sair maior que uma de 5 ms. Um relógio
     * quebrado devolveria zero para tudo, e o relatório sairia com todas as operações "instantâneas"
     * — que é o modo de falha mais convincente que uma medição de desempenho pode ter.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: imprime e devolve {@code false}; nenhum tempo é afirmado.
     */
    private static boolean relogioCalibrado() {
        long curto = cronometrar(5);
        long longo = cronometrar(40);
        boolean andaParaFrente = curto > 0 && longo > 0;
        boolean separaOsDois = longo > curto * 2;
        if (andaParaFrente && separaOsDois) {
            System.out.printf("  controle do relogio: 5ms -> %.1fms · 40ms -> %.1fms%n",
                curto / 1_000_000.0, longo / 1_000_000.0);
            return true;
        }
        System.out.printf("RELOGIO REPROVADO NO CONTROLE — anda=%s separa=%s. Nenhum tempo e "
            + "afirmado.%n", andaParaFrente, separaOsDois);
        return false;
    }

    private static long cronometrar(long millis) {
        long inicio = System.nanoTime();
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return System.nanoTime() - inicio;
    }

    private boolean foraDoAlcance(EventoLegenda evento) {
        if ("Comment".equals(evento.tipoLinha())) {
            return true;
        }
        return evento.estilo() != null && politicaEstiloMusical.estiloIgnorado(evento.estilo());
    }

    @Test
    @DisplayName("desempenho: onde o pipeline gasta o tempo, por operacao e por unidade")
    void medir() throws IOException {
        System.out.println("\n=== DESEMPENHO DO PIPELINE ===");
        if (!relogioCalibrado()) {
            return;
        }
        List<Path> pastas = AlcanceDaMedicao.pastasDeTraducao();
        if (pastas.isEmpty()) {
            return;
        }

        List<Medida> medidas = new ArrayList<>();

        // 1) LEITURA DO .ass — a porta de entrada de toda tela que toca legenda.
        List<Path> arquivos = new ArrayList<>();
        for (Path pasta : pastas) {
            arquivos.addAll(AlcanceDaMedicao.arquivosEntregues(pasta));
        }
        if (arquivos.isEmpty()) {
            System.out.println("NAO VERIFICADO: nenhum arquivo entregue no alcance.");
            return;
        }
        List<Path> amostraDeArquivos = arquivos.stream().limit(40).toList();
        List<DocumentoLegenda> lidos = new ArrayList<>();
        long inicio = System.nanoTime();
        for (Path a : amostraDeArquivos) {
            try {
                lidos.add(leitor.ler(a));
            } catch (RuntimeException ignorado) {
                // arquivo problemático não pode cegar a medição inteira
            }
        }
        medidas.add(new Medida("leitura do .ass", "arquivo", lidos.size(),
            System.nanoTime() - inicio, "LeitorLegendaAss, arquivos reais do acervo"));

        // Colhe as falas e as palavras que servem de material para o resto.
        List<String> falas = new ArrayList<>();
        Set<String> palavras = new LinkedHashSet<>();
        for (DocumentoLegenda doc : lidos) {
            for (EventoLegenda ev : doc.eventos()) {
                if (!ev.temTexto() || foraDoAlcance(ev) || ev.texto().isBlank()) {
                    continue;
                }
                if (falas.size() < AMOSTRA_DE_FALAS) {
                    falas.add(ev.texto());
                }
                for (String p : ev.texto().split("[^\\p{L}]+")) {
                    if (p.length() > 3 && palavras.size() < AMOSTRA_DE_PALAVRAS) {
                        palavras.add(p);
                    }
                }
            }
        }
        if (falas.isEmpty() || palavras.isEmpty()) {
            System.out.printf("NAO VERIFICADO: amostra vazia (%d falas, %d palavras).%n",
                falas.size(), palavras.size());
            return;
        }

        // 2) DICIONARIO — palavra CONHECIDA contra DESCONHECIDA. A diferenca entre as duas e o
        // que explica o CCA travar: o custo do hunspell e gerar SUGESTAO, nao ler a palavra.
        Set<String> inventadas = new LinkedHashSet<>();
        for (int i = 0; i < 60; i++) {
            inventadas.add("xkroz" + i + "vqp");
        }
        inicio = System.nanoTime();
        dicionario.classificar(new LinkedHashSet<>(palavras));
        medidas.add(new Medida("classificar palavra · 1a vez", "palavra", palavras.size(),
            System.nanoTime() - inicio,
            "6 idiomas (pt/en/de/fr/ja/es) por palavra desconhecida — nao e so ortografia"));

        // A SEGUNDA PASSADA sobre AS MESMAS palavras. E o numero que explica por que a tela 3.3
        // saiu de 291,1s para 14,4s: a memoria do adaptador responde sem arrancar processo. Sem
        // esta linha, a de cima seria lida como "o dicionario custa 37 ms por palavra, sempre".
        inicio = System.nanoTime();
        dicionario.classificar(new LinkedHashSet<>(palavras));
        medidas.add(new Medida("classificar palavra · 2a vez (memoria)", "palavra",
            palavras.size(), System.nanoTime() - inicio,
            "as MESMAS palavras: a memoria responde sem arrancar processo"));

        inicio = System.nanoTime();
        dicionario.classificar(inventadas);
        medidas.add(new Medida("classificar palavra INVENTADA", "palavra", inventadas.size(),
            System.nanoTime() - inicio, "o caso CARO: o hunspell gera SUGESTAO para cada uma"));

        // 3) A CADEIA DA 3.3 — cinco elos, e o relogio vem DELA, nao de um cronometro meu.
        Path temporaria = Files.createTempDirectory("kronos-desempenho");
        List<EventoLegenda> eventos = new ArrayList<>();
        for (int i = 0; i < falas.size(); i++) {
            eventos.add(new EventoLegenda(i, "Dialogue", "Default", PREFIXO, falas.get(i)));
        }
        DocumentoLegenda molde = lidos.get(0);
        escritor.escrever(temporaria.resolve("desempenho.ass"), new DocumentoLegenda(
            molde.cabecalho(), eventos, molde.quebraDeLinha(), molde.comBom()));

        long inicioCadeia = System.nanoTime();
        ResultadoConcordancia r = revisor.revisarPasta(temporaria, false);
        long nanosCadeia = System.nanoTime() - inicioCadeia;
        long nanosDosElos = r.porCorretor().stream().mapToLong(ContagemCorretor::nanos).sum();
        // O TOTAL EMBUTE CUSTO FIXO, e dizer "X ms por fala" sem isso engana: a diferenca entre o
        // total e a soma dos elos e arranque — subir o LanguageTool, aquecer o dicionario do
        // arquivo, ler e reescrever. Esse custo NAO cresce com a fala; ele se dilui. Numa pasta
        // grande o numero que vale e o dos elos.
        medidas.add(new Medida("cadeia 3.3 · TOTAL com arranque", "fala", falas.size(),
            nanosCadeia, String.format(Locale.ROOT,
                "inclui %.1fs de custo FIXO (LanguageTool, aquecimento, I/O) que nao cresce por fala",
                (nanosCadeia - nanosDosElos) / 1_000_000_000.0)));
        medidas.add(new Medida("cadeia 3.3 · so os 5 elos", "fala", falas.size(), nanosDosElos,
            "o custo que REALMENTE cresce com o tamanho da pasta"));

        for (ContagemCorretor c : r.porCorretor()) {
            medidas.add(new Medida("  elo · " + c.nome(), "fala", c.vistas(), c.nanos(),
                c.disponivel() ? "" : "NAO VERIFICADO: o elo nao pode rodar"));
        }

        limpar(temporaria);

        // ---------------------------------------------------------------- relatorio
        medidas.sort(Comparator.comparingDouble(Medida::msPorUnidade).reversed());
        System.out.printf("%n%-34s %10s %9s %12s  %s%n",
            "OPERACAO", "unidades", "total", "por unidade", "observacao");
        System.out.println("-".repeat(110));
        for (Medida m : medidas) {
            System.out.printf("%-34s %10d %8.1fs %9.3f ms  %s%n",
                m.operacao(), m.unidades(), m.segundos(), m.msPorUnidade(), m.observacao());
        }

        Files.createDirectories(SAIDA.getParent());
        Files.writeString(SAIDA, json(medidas), StandardCharsets.UTF_8);
        System.out.printf("%n  artefato: %s%n", SAIDA.toAbsolutePath());
        System.out.println("  o painel 'Desempenho' da aplicacao le exatamente este arquivo.");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o artefato que o painel lê. Sem data e sem máquina, dois relatórios
     * de máquinas diferentes se parecem — e a comparação entre eles não significa nada.
     */
    private static String json(List<Medida> medidas) {
        StringBuilder j = new StringBuilder("{\n");
        j.append("  \"maquina\": \"").append(escapar(System.getenv("COMPUTERNAME"))).append("\",\n");
        j.append("  \"java\": \"").append(escapar(System.getProperty("java.version"))).append("\",\n");
        j.append("  \"processadores\": ").append(Runtime.getRuntime().availableProcessors())
            .append(",\n");
        j.append("  \"medidas\": [\n");
        for (int i = 0; i < medidas.size(); i++) {
            Medida m = medidas.get(i);
            j.append("    {")
                .append("\"operacao\": \"").append(escapar(m.operacao().trim())).append("\", ")
                .append("\"unidade\": \"").append(escapar(m.unidade())).append("\", ")
                .append("\"unidades\": ").append(m.unidades()).append(", ")
                .append(String.format(Locale.ROOT, "\"segundos\": %.3f, ", m.segundos()))
                .append(String.format(Locale.ROOT, "\"msPorUnidade\": %.4f, ", m.msPorUnidade()))
                .append("\"observacao\": \"").append(escapar(m.observacao())).append("\"}")
                .append(i == medidas.size() - 1 ? "\n" : ",\n");
        }
        return j.append("  ]\n}\n").toString();
    }

    private static String escapar(String v) {
        return v == null ? "" : v.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void limpar(Path pasta) throws IOException {
        try (var caminhos = Files.walk(pasta)) {
            caminhos.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignorado) {
                    // pasta temporária: falha ao limpar não invalida a medição
                }
            });
        }
    }
}
