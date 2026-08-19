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
import org.traducao.projeto.raspagemRevisao.application.DetectorConcordanciaService;
import org.traducao.projeto.raspagemRevisao.application.ResolvedorArtefatosRevisao;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;
import org.traducao.projeto.revisaoConcordancia.application.CorretorConcordanciaGeneroService;
import org.traducao.projeto.revisaoConcordancia.application.RevisarConcordanciaUseCase;
import org.traducao.projeto.revisaoConcordancia.domain.ResultadoConcordancia;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: medir, no acervo real, o que a tela 3.3 (Revisão de Concordância)
 * MUDARIA se fosse executada hoje — quantas falas, em quais obras, e trocando QUAL palavra por
 * qual. É a resposta em número para "por onde começar a corrigir", e o dimensionamento do dano
 * de cada defeito que o diagnóstico de casos-controle levantou.
 *
 * <h2>Por que o inventário é por PAR de palavras, e não só contagem</h2>
 * Contagem certa não prova conjunto certo. Uma troca {@code a → o} pode ser conserto legítimo
 * ({@code "a menino" → "o menino"}) ou estrago ({@code "graças a Deus" → "graças o Deus"}), e os
 * dois somam no mesmo total. Por isso cada par vem com amostras do texto real: quem decide é o
 * olho sobre a fala, não o número.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>READ-ONLY. O caso de uso é chamado com {@code aplicar=false}; o laço próprio só lê.</li>
 *   <li>Quem responde é a PRODUÇÃO: o corretor, o leitor de ASS, o juiz de estilo musical
 *       ({@link PoliticaEstiloMusical}) e o critério de "isto é legenda traduzida"
 *       ({@link ResolvedorArtefatosRevisao#eLegendaTraduzida}). Nenhum critério é reescrito
 *       aqui — a segunda implementação sempre diverge da primeira.</li>
 *   <li><b>Calibração obrigatória:</b> o total do laço próprio tem de bater, obra a obra, com o
 *       {@code falasCorrigidas} que o caso de uso devolve em dry-run. Divergiu, o instrumento
 *       reprova em vez de imprimir tabela bonita.</li>
 *   <li><b>Controle positivo:</b> se o acervo inteiro devolver zero fala tocada, o instrumento
 *       reprova — "não achei nada" e "não tinha como achar" não podem ter a mesma cara.</li>
 *   <li>O universo é o MESMO que a tela vê: todo {@code .ass}/{@code .ssa} da pasta, inclusive o
 *       original em inglês e o {@code .parcial}. Isso é medido de propósito — a tela não
 *       distingue, e o relatório precisa mostrar essa superfície.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Acervo ausente termina com aviso e sem reprovar; arquivo ilegível é contado como pulado.
 *
 * <p>Uso: {@code gradlew test --tests "*MedicaoConcordanciaAcervoPtIT*" "-Dkronos.medicao=true"}
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoConcordanciaAcervoPtIT {

    private static final Path RAIZ = Path.of(System.getProperty("kronos.acervo", "C:\\animes"));
    private static final int AMOSTRAS_POR_PAR = 4;
    private static final int PARES_NO_RELATORIO = 30;

    @Inject
    RevisarConcordanciaUseCase useCase;

    @Inject
    CorretorConcordanciaGeneroService corretor;

    @Inject
    PoliticaEstiloMusical politicaEstiloMusical;

    @Inject
    LeitorLegendaAss leitor;

    @Inject
    ResolvedorArtefatosRevisao resolvedor;

    @Inject
    DetectorConcordanciaService detector;

    /** Uma troca observada: palavra que saiu, palavra que entrou. */
    private record Par(String de, String para) {
        @Override
        public String toString() {
            return de + " -> " + para;
        }
    }

    private final Map<Par, Integer> contagemPorPar = new LinkedHashMap<>();
    private final Map<Par, List<String>> amostrasPorPar = new LinkedHashMap<>();
    private final Map<String, Integer> motivosSemCorretor = new TreeMap<>();
    private final Map<String, Integer> motivosComCorretor = new TreeMap<>();

    @Test
    @DisplayName("acervo: o que a 3.3 mudaria hoje, por obra e por par de palavras")
    void medir() throws IOException {
        if (!Files.isDirectory(RAIZ)) {
            System.out.println("SEM ACERVO em " + RAIZ + " — nada medido.");
            return;
        }

        List<Path> obras;
        try (Stream<Path> s = Files.list(RAIZ)) {
            obras = s.filter(Files::isDirectory).sorted().toList();
        }

        int totalArquivosPt = 0;
        int totalArquivosOutros = 0;
        int totalParciais = 0;
        int totalEventos = 0;
        int totalAoAlcance = 0;
        int totalMusicaVetada = 0;
        int totalTocadasPt = 0;
        int totalTocadasNaoPt = 0;
        int obrasDivergentes = 0;
        long maiorDuracaoMs = 0;

        System.out.printf("%n%-46s %7s %7s %8s %9s %9s %9s %8s%n",
            "OBRA", "arq PT", "arq EN", "eventos", "alcance", "musica", "TOCADAS", "ms");
        System.out.println("-".repeat(104));

        for (Path obra : obras) {
            List<Path> arquivos;
            try (Stream<Path> s = Files.walk(obra)) {
                arquivos = s.filter(Files::isRegularFile).filter(this::assOuSsa).sorted().toList();
            }
            if (arquivos.isEmpty()) {
                continue;
            }

            int arqPt = 0;
            int arqOutros = 0;
            int parciais = 0;
            int eventos = 0;
            int aoAlcance = 0;
            int musica = 0;
            int tocadasPt = 0;
            int tocadasNaoPt = 0;

            for (Path arquivo : arquivos) {
                boolean pt = resolvedor.eLegendaTraduzida(arquivo);
                boolean parcial = arquivo.getFileName().toString().toLowerCase().contains(".parcial.");
                if (pt) {
                    arqPt++;
                } else {
                    arqOutros++;
                }
                if (parcial) {
                    parciais++;
                    // A PRODUÇÃO pula o .parcial desde 18/08/2026 (não é entrega). O laço daqui
                    // pula junto, senão a calibração compara universos diferentes e só continua
                    // batendo por sorte — enquanto nenhum .parcial tiver fala corrigível.
                    continue;
                }
                DocumentoLegenda documento;
                try {
                    documento = leitor.ler(arquivo);
                } catch (RuntimeException e) {
                    continue;
                }
                for (EventoLegenda evento : documento.eventos()) {
                    eventos++;
                    if (!evento.temTexto()) {
                        continue;
                    }
                    if (evento.estilo() != null && politicaEstiloMusical.estiloIgnorado(evento.estilo())) {
                        musica++;
                        continue;
                    }
                    aoAlcance++;
                    registrarMotivos(evento.texto());
                    Optional<String> corrigida = corretor.corrigir(evento.texto());
                    if (corrigida.isEmpty()) {
                        continue;
                    }
                    if (pt) {
                        tocadasPt++;
                    } else {
                        tocadasNaoPt++;
                    }
                    registrarPares(obra.getFileName().toString(), evento.texto(), corrigida.get());
                }
            }

            // O tempo do caso de uso SOZINHO é dado de engenharia, não curiosidade: o
            // pode-compilar.ps1 trata 90s sem linha no log como "job terminado" e libera a
            // compilação, que dispara live reload e mata o job em curso. É o acidente do
            // Stink Bomb (14/08/2026). Quem decide se esta tela precisa de batimento de
            // progresso é este número, medido na maior pasta do acervo — não o palpite.
            long t0 = System.nanoTime();
            ResultadoConcordancia oficial = useCase.revisarPasta(obra, false);
            long duracaoMs = (System.nanoTime() - t0) / 1_000_000L;
            maiorDuracaoMs = Math.max(maiorDuracaoMs, duracaoMs);
            int meu = tocadasPt + tocadasNaoPt;
            boolean bate = oficial.falasCorrigidas() == meu;
            if (!bate) {
                obrasDivergentes++;
            }

            System.out.printf("%-46s %7d %7d %8d %9d %9d %9d %8d%s%n",
                recortar(obra.getFileName().toString(), 46), arqPt, arqOutros, eventos, aoAlcance,
                musica, meu, duracaoMs,
                bate ? "" : "  <== CALIBRACAO DIVERGIU (uso de caso: " + oficial.falasCorrigidas() + ")");

            totalArquivosPt += arqPt;
            totalArquivosOutros += arqOutros;
            totalParciais += parciais;
            totalEventos += eventos;
            totalAoAlcance += aoAlcance;
            totalMusicaVetada += musica;
            totalTocadasPt += tocadasPt;
            totalTocadasNaoPt += tocadasNaoPt;
        }

        System.out.println("-".repeat(104));
        System.out.printf("%-46s %7d %7d %8d %9d %9d %9d%n%n", "TOTAL", totalArquivosPt,
            totalArquivosOutros, totalEventos, totalAoAlcance, totalMusicaVetada,
            totalTocadasPt + totalTocadasNaoPt);
        System.out.printf("PIOR silencio possivel (obra mais lenta) %d ms   —  limite do pode-compilar.ps1: 90.000 ms%n",
            maiorDuracaoMs);
        System.out.printf("arquivos .parcial PULADOS (fora do alcance) %d%n", totalParciais);
        System.out.printf("falas tocadas em arquivo PT ............. %d%n", totalTocadasPt);
        System.out.printf("falas tocadas em arquivo NAO-PT ......... %d   (a tela nao distingue)%n%n",
            totalTocadasNaoPt);

        System.out.println("INVENTARIO DAS TROCAS (palavra que sai -> palavra que entra):");
        contagemPorPar.entrySet().stream()
            .sorted(Map.Entry.<Par, Integer>comparingByValue(Comparator.reverseOrder()))
            .limit(PARES_NO_RELATORIO)
            .forEach(e -> {
                System.out.printf("%n  %6d  %s%n", e.getValue(), e.getKey());
                amostrasPorPar.getOrDefault(e.getKey(), List.of())
                    .forEach(a -> System.out.printf("          %s%n", a));
            });

        System.out.printf("%n%npares distintos ......................... %d%n%n", contagemPorPar.size());

        System.out.println("MOTIVOS DO DETECTOR PT-ONLY QUE NAO TEM CORRETOR NA 3.3:");
        motivosSemCorretor.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
            .forEach(e -> System.out.printf("  %6d  %s%n", e.getValue(), e.getKey()));
        System.out.println();
        System.out.println("MOTIVOS DO DETECTOR PT-ONLY QUE JA TEM CORRETOR:");
        motivosComCorretor.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
            .forEach(e -> System.out.printf("  %6d  %s%n", e.getValue(), e.getKey()));

        assertTrue(obrasDivergentes == 0,
            "calibracao falhou em " + obrasDivergentes + " obra(s): o laco do harness nao reproduz o caso de uso");
        assertTrue(totalAoAlcance > 0, "controle positivo: nenhuma fala ao alcance — o instrumento esta cego");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: registra qual palavra virou qual, com uma amostra do texto real.
     * <p>INVARIANTES DO DOMÍNIO: o corretor só TROCA palavras, nunca acrescenta nem remove — se a
     * contagem de tokens mudar, isso é registrado como par especial e vira achado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança.
     */
    private void registrarPares(String obra, String antes, String depois) {
        String[] a = antes.split("\\s+");
        String[] d = depois.split("\\s+");
        if (a.length != d.length) {
            anotar(new Par("(ESTRUTURA)", "TOKENS MUDARAM"), obra + " | " + visivel(antes) + " => " + visivel(depois));
            return;
        }
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(d[i])) {
                anotar(new Par(a[i], d[i]), obra + " | " + visivel(antes));
            }
        }
    }

    private void anotar(Par par, String amostra) {
        contagemPorPar.merge(par, 1, Integer::sum);
        List<String> amostras = amostrasPorPar.computeIfAbsent(par, k -> new ArrayList<>());
        if (amostras.size() < AMOSTRAS_POR_PAR) {
            amostras.add(recortar(amostra, 150));
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: separa o que o detector acusa e a 3.3 conserta do que ela apenas
     * deixa passar — o gap que decide a fila de trabalho.
     * <p>INVARIANTES DO DOMÍNIO: o detector é chamado com original {@code null}, que é a
     * condição real da tela PT-only.
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança.
     */
    private void registrarMotivos(String texto) {
        ResultadoDeteccaoConcordancia r = detector.analisar(null, texto);
        if (!r.suspeito()) {
            return;
        }
        boolean temCorretor = corretor.corrigir(texto).isPresent();
        for (String motivo : r.motivos()) {
            String rotulo = rotulo(motivo);
            if (temCorretor) {
                motivosComCorretor.merge(rotulo, 1, Integer::sum);
            } else {
                motivosSemCorretor.merge(rotulo, 1, Integer::sum);
            }
        }
    }

    private boolean assOuSsa(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase();
        return nome.endsWith(".ass") || nome.endsWith(".ssa");
    }

    private static String rotulo(String motivo) {
        int corte = motivo.indexOf(':');
        int aspas = motivo.indexOf('"');
        int fim = corte < 0 ? aspas : (aspas < 0 ? corte : Math.min(corte, aspas));
        return fim < 0 ? motivo : motivo.substring(0, fim).strip();
    }

    private static String visivel(String t) {
        return t == null ? "" : t.replaceAll("\\{[^}]*}", "").replace("\\N", " ").strip();
    }

    private static String recortar(String t, int max) {
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }
}
