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
 * PROPÓSITO DE NEGÓCIO: manter NOMINAL a lista de lugares da tela <b>3.2 (Revisão de Lore)</b> que
 * reescrevem o texto de uma fala, para que nenhum novo nasça sem responder "e se for música?".
 * É a irmã da {@link CatracaEscritaDeFalaVetaMusicaTest}, que faz o mesmo pela 3.1.
 *
 * <h2>O prejuízo que originou — medido, não hipótese</h2>
 * A catraca da 3.1 varre o prefixo {@code org/traducao/projeto/raspagemRevisao}. A fatia
 * {@code revisaoLore} tem DUAS portas próprias e ficava inteiramente fora dela — a varredura da
 * 3.1 nunca teria como enxergá-las, e ninguém percebeu porque o relatório daquela catraca é
 * verde por construção fora do seu prefixo.
 *
 * <p>A porta descoberta assim foi a aba <b>PT-only</b> ({@code RevisarLorePtOnlyUseCase}, <b>removida
 * em 17/08/2026</b>): laço próprio filtrando só {@code temTexto()}, gravando no {@code .ass} com
 * "Apenas simular" desmarcado por padrão. Ela saiu porque as 23 pastas {@code traducao_ptbr} do
 * acervo TÊM espelho em inglês — atendia a um cenário inexistente. O registro fica: foi ela que
 * revelou que esta fatia não tinha catraca nenhuma. Medido em 17/08/2026 com
 * {@code MedicaoExposicaoMusicalRevisaoLorePtOnlyIT} (22 obras, calibrado contra o próprio caso
 * de uso em dry-run — harness e produção bateram em 22 de 22):
 * <pre>
 *   246.246  linhas de estilo musical ao ALCANCE da aba
 *        0   reescritas pela camada determinística fora do Char's Counterattack
 *       29   no CCA, que é o filme ACHATADO — ali são DIÁLOGO com nome de estilo vetado
 * </pre>
 * Zero dano gravado hoje e superfície inteira aberta é exatamente o estado em que a ponte do
 * cache da 3.1 esteve até morder 687 linhas {@code Song ENG} do Gundam 08th MS Team.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code EventoLegenda.comTexto} é a porta de reescrita — mesma assinatura da catraca da
 *       3.1, e pelo mesmo motivo verificada contra caso-controle.</li>
 *   <li>As duas portas desta fatia VETAM POR SI, perguntando ao {@code AlcanceRevisaoLore}. Não
 *       há aqui a figura "protegido pelo chamador" da 3.1: as duas abas têm laço próprio e
 *       entram por controllers diferentes.</li>
 *   <li>Delegar não pode virar promessa vazia: o próprio {@code AlcanceRevisaoLore} é conferido
 *       para garantir que ele consulta o dono da regra ({@code PoliticaEstiloMusical}).</li>
 *   <li>Reprovar aqui NÃO significa "apague a escrita": significa perguntar ao alcance antes de
 *       escrever, ou declarar em {@link #PORTAS_CONHECIDAS} quem veta no seu lugar.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Lista as portas novas, as que sumiram e o veto ausente. Nunca lança por conta própria.
 */
class CatracaEscritaDeFalaVetaMusicaLoreTest {

    private static final Path RAIZ = Path.of("src/main/java");

    private static final String FATIA = "org/traducao/projeto/revisaoLore";

    private static final String APP = FATIA + "/application/";

    /** O dono ÚNICO da pergunta "esta linha está ao alcance da 3.2?". */
    private static final String ALCANCE = APP + "AlcanceRevisaoLore.java";

    /** Como uma porta pergunta pelo alcance. */
    private static final String VETO_PROPRIO = "alcance.estaNoAlcance";

    /** Como o alcance pergunta ao dono da regra de música, em vez de ter lista própria. */
    private static final String JUIZ_DE_MUSICA = "politicaEstiloMusical.estiloIgnorado";

    /** As portas de reescrita da 3.2, com o motivo de cada uma e QUEM a protege. */
    private static final Map<String, String> PORTAS_CONHECIDAS = new LinkedHashMap<>();

    static {
        PORTAS_CONHECIDAS.put(APP + "RevisarLoreUseCase.java",
            "VETA POR SI. Aba \"Com ingles\": ehEventoAuditavelLore delega ao AlcanceRevisaoLore "
            + "e julga pelo evento ORIGINAL, porque restyle da PT e legitimo.");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a varredura. Recebe raiz e prefixo por parâmetro para que o
     * caso-controle a exercite contra uma árvore montada à mão, sem tocar no código real.
     * <p>INVARIANTES DO DOMÍNIO: devolve caminho relativo com barra normal, para o inventário ser
     * legível igual no Windows e no Linux.
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
    @DisplayName("nenhuma escrita nova de fala nasce sem responder ao veto de musica na 3.2")
    void todaEscritaDeFalaDaLoreRespondeAoVetoDeMusica() throws IOException {
        Set<String> encontradas = mapearEscritas(RAIZ, FATIA);

        assertFalse(encontradas.isEmpty(),
            "instrumento CEGO: nao achou NENHUMA escrita de fala na 3.2, e existe uma. Se "
                + "comTexto mudou de nome, corrija a assinatura — nao apague a catraca.");

        List<String> novas = new ArrayList<>(encontradas);
        novas.removeAll(PORTAS_CONHECIDAS.keySet());
        assertTrue(novas.isEmpty(), () -> """
            Porta NOVA reescrevendo o texto de uma fala na 3.2: %s

            A aba PT-only tinha 246.246 linhas de estilo musical ao alcance antes de ganhar veto,
            e a porta gemea da 3.1 reescreveu 687 linhas Song ENG do Gundam 08th MS Team.

            Pergunte ao AlcanceRevisaoLore antes de escrever, ou declare em PORTAS_CONHECIDAS quem
            veta no seu lugar. O que nao pode e nascer calada.
            """.formatted(novas));

        List<String> sumiram = new ArrayList<>(PORTAS_CONHECIDAS.keySet());
        sumiram.removeAll(encontradas);
        assertTrue(sumiram.isEmpty(),
            "porta conhecida sumiu da varredura: " + sumiram + ". Se deixou de escrever, tire do "
                + "inventario. Se so mudou de lugar, corrija o caminho — nao afrouxe a busca.");
    }

    @Test
    @DisplayName("as duas portas perguntam pelo alcance, e o alcance pergunta ao juiz de musica")
    void asPortasPerguntamEODonoDaPerguntaConsultaOJuiz() throws IOException {
        for (String porta : new TreeSet<>(PORTAS_CONHECIDAS.keySet())) {
            assertTrue(fonteDe(porta).contains(VETO_PROPRIO),
                porta + " perdeu a consulta a " + VETO_PROPRIO + ". As duas abas da 3.2 tem laco "
                    + "proprio e nenhuma auditoria posterior as cobre: sem esta chamada, musica, "
                    + "karaoke e desenho vetorial voltam a entrar na reescrita.");
        }

        assertTrue(fonteDe(ALCANCE).contains(JUIZ_DE_MUSICA),
            ALCANCE + " parou de consultar " + JUIZ_DE_MUSICA + ". Delegar a ele so vale enquanto "
                + "ele proprio perguntar ao dono da regra; lista propria divergiria em silencio no "
                + "dia em que um estilo novo entrasse, e o sinal apareceria numa legenda estragada.");
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
                void gravar(EventoLegenda e) { novos.add(e.comTexto("nao e da 3.2")); }
            }
            """);

        Set<String> achadas = mapearEscritas(temp, FATIA);

        assertEquals(1, achadas.size(),
            "a varredura tinha de achar EXATAMENTE a porta plantada. Achou: " + achadas);
        assertTrue(achadas.contains(FATIA + "/application/PortaNova.java"), "achou: " + achadas);
    }
}
