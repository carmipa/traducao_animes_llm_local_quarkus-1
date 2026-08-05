package org.traducao.projeto.medicao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.novoKaraoke.domain.EventoAss;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * PROPÓSITO DE NEGÓCIO: dimensionar o padrão <b>"N eventos no MESMO tempo com o MESMO texto"</b> —
 * as camadas de animação que o fansub empilha para sombra, blur e brilho de um cartaz.
 *
 * <h2>Por que a primeira versão deste harness estava errada</h2>
 * Ela usava "índice consecutivo no cache" como proxy de "evento adjacente". <b>Não é.</b> O cache
 * guarda só as falas traduzíveis, então índices vizinhos podem estar minutos apart. Medido em
 * 05/08/2026: os dois {@code "Morning."} do 86 ep01 eram consecutivos no cache e estão a
 * <b>catorze minutos</b> um do outro no {@code .ass} — 0:03:34 e 0:17:05. Duas pessoas
 * cumprimentando em cenas diferentes, não camadas.
 *
 * <p>A conclusão que aquele número sustentava — "alargar o gatilho do simplificador para outros
 * estilos" — teria <b>apagado diálogo</b>. Camada ocupa a MESMA janela; fala repetida ocupa
 * janelas distintas. Só o tempo separa as duas, e tempo só existe no {@code .ass}.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>READ-ONLY sobre os {@code .ass} de entrada. Mede, não propõe.</li>
 *   <li>Lê com {@link EventoAss#interpretar(String)} — o mesmo parser do conversor de karaokê.
 *       Reimplementar a leitura daria números que não valem para quem vai consumir.</li>
 *   <li>Camada = mesmo texto visível E sobreposição de janela ≥ {@link #SOBREPOSICAO_MINIMA}.
 *       Sobreposição, não igualdade exata: o fansub desloca a camada em centésimos.</li>
 *   <li>Texto vazio depois de tirar tags é ignorado: evento decorativo puro não é camada.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Pasta ausente termina com aviso. Linha que não é evento é pulada pelo próprio parser.
 *
 * <p>Uso: {@code gradlew test --tests "*MedicaoCamadaRepetidaIT*" "-Dkronos.medicao=true"
 * "-Dkronos.medicao.ass=C:\animes\86\86 Part 1\legendas_extraidas_ass"}
 */
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoCamadaRepetidaIT {

    private static final String CHAVE_PASTA = "kronos.medicao.ass";

    /** Fração da janela menor que precisa coincidir para os eventos serem a mesma coisa. */
    private static final double SOBREPOSICAO_MINIMA = 0.80;

    private record Conta(String estilo, int eventos, int emCamada, int grupos,
                         int empilhados, String amostra) {
    }

    @Test
    @DisplayName("ass: eventos no MESMO tempo com o MESMO texto, por estilo")
    void medir() throws IOException {
        String pasta = System.getProperty(CHAVE_PASTA);
        if (pasta == null) {
            System.out.println("Passe -D" + CHAVE_PASTA + "=<pasta com .ass> — este harness le o "
                + "ASS, nao o cache, porque a camada so se distingue pelo TEMPO.");
            return;
        }
        Path raiz = Path.of(pasta);
        if (!Files.isDirectory(raiz)) {
            System.out.println("Pasta inexistente: " + raiz.toAbsolutePath());
            return;
        }

        // estilo -> [eventos, emCamada, grupos, empilhados(layer diferente)]
        Map<String, int[]> contas = new LinkedHashMap<>();
        Map<String, String> amostras = new LinkedHashMap<>();
        int arquivos = 0;

        try (Stream<Path> arqs = Files.list(raiz)) {
            for (Path arq : arqs.filter(p -> p.toString().endsWith(".ass")).toList()) {
                arquivos++;
                List<EventoAss> eventos = new ArrayList<>();
                for (String linha : Files.readAllLines(arq, StandardCharsets.UTF_8)) {
                    EventoAss e = EventoAss.interpretar(linha);
                    if (e != null) {
                        eventos.add(e);
                    }
                }
                eventos.sort(Comparator.comparingLong(EventoAss::inicioCs));
                boolean[] jaContado = new boolean[eventos.size()];
                for (int i = 0; i < eventos.size(); i++) {
                    EventoAss a = eventos.get(i);
                    String estilo = a.estilo() == null || a.estilo().isBlank()
                        ? "(sem estilo)" : a.estilo();
                    contas.computeIfAbsent(estilo, k -> new int[4])[0]++;
                    if (jaContado[i]) {
                        continue;
                    }
                    String visivel = visivel(a);
                    if (visivel.isEmpty()) {
                        continue;
                    }
                    int camadas = 1;
                    boolean layerDiverge = false;
                    for (int j = i + 1; j < eventos.size(); j++) {
                        EventoAss b = eventos.get(j);
                        if (b.inicioCs() > a.fimCs()) {
                            break;   // ordenado por inicio: daqui em diante nao ha sobreposicao
                        }
                        if (!jaContado[j] && visivel(b).equals(visivel)
                            && sobreposicao(a, b) >= SOBREPOSICAO_MINIMA) {
                            camadas++;
                            jaContado[j] = true;
                            if (b.camada() != a.camada()) {
                                layerDiverge = true;
                            }
                        }
                    }
                    if (camadas >= 2) {
                        int[] c = contas.get(estilo);
                        c[1] += camadas;
                        c[2]++;
                        // EMPILHADO = mesmo texto, mesma janela e LAYER do ASS DIFERENTE. E a
                        // assinatura da composicao tipografica: o fansub sobrepoe sombra, blur e
                        // texto em camadas distintas do mesmo instante. Duas pessoas dizendo a
                        // mesma coisa ficariam na MESMA camada — por isso este e o discriminador
                        // seguro, e nao a sobreposicao sozinha.
                        if (layerDiverge) {
                            c[3] += camadas;
                        }
                        amostras.putIfAbsent(estilo, camadas + "x  "
                            + (layerDiverge ? "[layers] " : "[mesma layer] ") + recortar(visivel));
                    }
                }
            }
        }

        List<Conta> linhas = new ArrayList<>();
        contas.forEach((e, c) -> linhas.add(
            new Conta(e, c[0], c[1], c[2], c[3], amostras.getOrDefault(e, ""))));

        System.out.printf("%narquivos lidos: %d | sobreposicao minima: %.0f%%%n%n",
            arquivos, SOBREPOSICAO_MINIMA * 100);
        System.out.printf("%-22s %8s %9s %7s %11s  %s%n",
            "ESTILO", "eventos", "sobrepoe", "grupos", "EMPILHADOS", "amostra");
        linhas.stream()
            .filter(c -> c.emCamada() > 0)
            .sorted(Comparator.comparingInt(Conta::emCamada).reversed())
            .limit(20)
            .forEach(c -> System.out.printf("%-22s %8d %9d %7d %11d  %s%n",
                recortarCurto(c.estilo()), c.eventos(), c.emCamada(), c.grupos(),
                c.empilhados(), c.amostra()));

        int camada = linhas.stream().mapToInt(Conta::emCamada).sum();
        int grupos = linhas.stream().mapToInt(Conta::grupos).sum();
        int empilhados = linhas.stream().mapToInt(Conta::empilhados).sum();
        System.out.printf("%nSOBREPOEM ......... %d eventos em %d grupos%n", camada, grupos);
        System.out.printf("EMPILHADOS ........ %d  (mesmo texto, mesma janela, LAYER diferente)%n",
            empilhados);
        System.out.printf("so-sobrepoem ...... %d  (mesma layer — NAO tocar: pode ser fala real)%n",
            camada - empilhados);
        System.out.println();
        System.out.println("A coluna EMPILHADOS e a unica que autoriza colapso. Sobreposicao "
            + "sozinha nao basta: duas pessoas dizendo a mesma coisa no mesmo instante ficam na "
            + "MESMA layer, e achatar isso apagaria fala.");
    }

    /** Fração da janela MENOR coberta pela interseção. */
    private static double sobreposicao(EventoAss a, EventoAss b) {
        long inicio = Math.max(a.inicioCs(), b.inicioCs());
        long fim = Math.min(a.fimCs(), b.fimCs());
        long comum = Math.max(0, fim - inicio);
        long menor = Math.max(1, Math.min(a.fimCs() - a.inicioCs(), b.fimCs() - b.inicioCs()));
        return (double) comum / menor;
    }

    private static String visivel(EventoAss e) {
        return e.textoVisivel().replaceAll("\\\\[Nnh]", " ").replaceAll("\\s+", " ").strip();
    }

    private static String recortar(String t) {
        return t.length() <= 40 ? t : t.substring(0, 40) + "…";
    }

    private static String recortarCurto(String t) {
        return t.length() <= 24 ? t : t.substring(0, 24) + "…";
    }
}
