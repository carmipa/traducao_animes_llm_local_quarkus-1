package org.traducao.projeto.arquitetura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: nenhuma porta de reescrita de fala pode nascer na 3.3 (Revisão de
 * Concordância) sem responder ao veto de música — nem à pergunta de escopo que a acompanha.
 *
 * <h2>A TERCEIRA irmã, e por que ela faltava</h2>
 * As catracas gêmeas varrem <b>prefixo de fatia</b>: uma cobre {@code raspagemRevisao} (3.1) e
 * outra {@code revisaoLore} (3.2). Fora do próprio prefixo elas são <b>verdes por construção</b> —
 * não existe sinal de "não olhei aqui". A 3.3 estava fora das duas, e foi assim que a fatia
 * chegou a 18/08/2026 sem catraca nenhuma: o veto de música dela existe no código desde 16/08, e
 * nada impedia a próxima porta de nascer sem ele.
 *
 * <p>O prejuízo da classe está medido nas irmãs: a porta PT-only da 3.2 tinha <b>246.246</b>
 * linhas de estilo musical ao alcance, e a da 3.1 reescreveu <b>687</b> linhas {@code Song ENG}
 * do Gundam 08th MS Team. Nesta tela o risco é maior por natureza — ela mexe em GÊNERO, que é
 * onde a heurística mais erra, e antes do veto via <b>85,1%</b> dos eventos do 86 Part 1, quase
 * tudo sílaba solta de karaokê.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Toda porta que chama {@code .comTexto(} dentro da fatia está no inventário nominal, com o
 *       motivo escrito.</li>
 *   <li>Cada porta conhecida consulta o juiz de música ({@code eMusica}) — e o {@code eMusica}
 *       consulta o DONO da regra ({@code politicaEstiloMusical.estiloIgnorado}), em vez de ter
 *       lista própria que divergiria em silêncio.</li>
 *   <li>Toda porta respeita o escopo de arquivo: {@code .parcial} é tradução incompleta e não é
 *       entrega — reescrevê-la é gravar por cima de um arquivo que o pipeline isolou de
 *       propósito.</li>
 *   <li><b>Alvo vazio é instrumento CEGO, não aprovação:</b> zero escrita encontrada reprova.</li>
 * </ul>
 *
 * <h2>Limite declarado</h2>
 * Esta catraca prova que a CHAMADA existe, não que ela funciona — a mutação {@code false &&}
 * passaria por aqui. Quem prova o comportamento é {@code ConsoleDaRevisaoConcordanciaTest} e
 * {@code RevisarConcordanciaUseCaseTest}. As duas são necessárias, e é por isso que a lição foi
 * escrita quando a irmã da 3.2 nasceu.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Lista a porta nova, a que sumiu ou o veto ausente. Nunca lança por conta própria.
 */
class CatracaEscritaDeFalaVetaMusicaConcordanciaTest {

    private static final Path RAIZ = Path.of("src/main/java");

    private static final String FATIA = "org/traducao/projeto/revisaoConcordancia";

    private static final String APP = FATIA + "/application/";

    /** Como a porta pergunta se a fala é música — e portanto não é trabalho desta tela. */
    private static final String VETO_DE_MUSICA = "eMusica(";

    /** Como o juiz local pergunta ao dono da regra, em vez de ter lista própria. */
    private static final String JUIZ_DE_MUSICA = "politicaEstiloMusical.estiloIgnorado";

    /** Como a porta mantém fora do alcance o arquivo que não é entrega. */
    private static final String VETO_DE_PARCIAL = "eParcial(";

    /** As portas de reescrita da 3.3, com o motivo de cada uma. */
    private static final Map<String, String> PORTAS_CONHECIDAS = new LinkedHashMap<>();

    static {
        PORTAS_CONHECIDAS.put(APP + "RevisarConcordanciaUseCase.java",
            "A UNICA porta da fatia. Laco proprio sobre os eventos, com eMusica() por fala e "
            + "eParcial() por arquivo; grava com backup e so quando aplicar=true.");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a varredura. Recebe raiz e prefixo por parâmetro para o caso-controle
     * poder exercitá-la contra uma árvore montada à mão, sem tocar no código real.
     * <p>INVARIANTES DO DOMÍNIO: caminho relativo com barra normal, para o inventário ficar igual
     * no Windows e no Linux.
     * <p>COMPORTAMENTO EM CASO DE FALHA: raiz inexistente propaga {@link IOException}.
     */
    private static Set<String> mapearEscritas(Path raiz, String prefixo) throws IOException {
        Set<String> arquivos = new TreeSet<>();
        try (Stream<Path> caminhos = Files.walk(raiz)) {
            for (Path arquivo : caminhos.filter(p -> p.toString().endsWith(".java")).toList()) {
                String relativo = raiz.relativize(arquivo).toString().replace('\\', '/');
                if (!relativo.startsWith(prefixo)) {
                    continue;
                }
                if (Files.readString(arquivo, StandardCharsets.UTF_8).contains(".comTexto(")) {
                    arquivos.add(relativo);
                }
            }
        }
        return arquivos;
    }

    private static String fonteDe(String relativo) throws IOException {
        return Files.readString(RAIZ.resolve(relativo), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("nenhuma escrita nova de fala nasce sem veto de musica na 3.3")
    void todaEscritaDeFalaDaConcordanciaRespondeAoVetoDeMusica() throws IOException {
        Set<String> encontradas = mapearEscritas(RAIZ, FATIA);

        assertFalse(encontradas.isEmpty(),
            "instrumento CEGO: nao achou NENHUMA escrita de fala na 3.3, e existe uma. Se "
                + "comTexto mudou de nome, corrija a assinatura — nao apague a catraca.");

        List<String> novas = new ArrayList<>(encontradas);
        novas.removeAll(PORTAS_CONHECIDAS.keySet());
        assertTrue(novas.isEmpty(), () -> """
            Porta NOVA reescrevendo o texto de uma fala na 3.3: %s

            Esta tela mexe em GENERO, que e onde a heuristica mais erra, e antes do veto via 85,1%%
            dos eventos do 86 Part 1 — quase tudo silaba solta de karaoke. As irmas ja pagaram a
            conta: 687 linhas Song ENG reescritas na 3.1 e 246.246 ao alcance na 3.2.

            Pergunte a eMusica() por fala e a eParcial() por arquivo antes de escrever, ou declare
            em PORTAS_CONHECIDAS quem veta no seu lugar. O que nao pode e nascer calada.
            """.formatted(novas));

        List<String> sumiram = new ArrayList<>(PORTAS_CONHECIDAS.keySet());
        sumiram.removeAll(encontradas);
        assertTrue(sumiram.isEmpty(),
            "porta conhecida sumiu da varredura: " + sumiram + ". Se deixou de escrever, tire do "
                + "inventario. Se so mudou de lugar, corrija o caminho — nao afrouxe a busca.");
    }

    @Test
    @DisplayName("a porta veta musica e .parcial, e o juiz local consulta o dono da regra")
    void aPortaVetaEODonoDaPerguntaEConsultado() throws IOException {
        for (String porta : new TreeSet<>(PORTAS_CONHECIDAS.keySet())) {
            String fonte = fonteDe(porta);
            assertTrue(fonte.contains(VETO_DE_MUSICA),
                porta + " perdeu a consulta a " + VETO_DE_MUSICA + ". Sem ela, karaoke volta a "
                    + "entrar numa reescrita que mexe em genero.");
            assertTrue(fonte.contains(VETO_DE_PARCIAL),
                porta + " perdeu a consulta a " + VETO_DE_PARCIAL + ". O .parcial e traducao "
                    + "incompleta que o pipeline isolou; reescreve-la e gravar por cima do que "
                    + "nao e entrega. Medido: 38 arquivos .parcial ao alcance no acervo.");
            assertTrue(fonte.contains(JUIZ_DE_MUSICA),
                porta + " parou de consultar " + JUIZ_DE_MUSICA + ". Lista propria de estilo "
                    + "musical divergiria em silencio no dia em que um estilo novo entrasse, e o "
                    + "sinal so apareceria numa legenda estragada.");
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CASO-CONTROLE. A varredura só vale se enxergar — monta uma árvore com
     * um arquivo que reescreve fala, um que só lê e um fora da fatia, e exige que ela separe os
     * três. Sem isto, um erro na assinatura deixaria a catraca verde para sempre.
     */
    @Test
    @DisplayName("instrumento calibrado: acha a escrita plantada, ignora leitura e outra fatia")
    void instrumentoAchaEscritaPlantadaEIgnoraOResto(@TempDir Path temp) throws IOException {
        Path dentro = temp.resolve(FATIA + "/application");
        Files.createDirectories(dentro);
        Files.writeString(dentro.resolve("PortaNova.java"), """
            class PortaNova {
                void gravar(EventoLegenda e) { novos.add(e.comTexto("outro texto")); }
            }
            """);
        Files.writeString(dentro.resolve("SoLeitura.java"), """
            class SoLeitura {
                boolean olhar(EventoLegenda e) { return e.texto().isBlank(); }
            }
            """);
        Path foraDaFatia = temp.resolve("org/traducao/projeto/outraFatia");
        Files.createDirectories(foraDaFatia);
        Files.writeString(foraDaFatia.resolve("EscreveTambem.java"), """
            class EscreveTambem {
                void gravar(EventoLegenda e) { novos.add(e.comTexto("nao e da 3.3")); }
            }
            """);

        Set<String> achadas = mapearEscritas(temp, FATIA);

        assertEquals(1, achadas.size(),
            "a varredura tinha de achar EXATAMENTE a porta plantada. Achou: " + achadas);
        assertTrue(achadas.contains(FATIA + "/application/PortaNova.java"), "achou: " + achadas);
    }
}
