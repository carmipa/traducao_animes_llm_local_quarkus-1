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
 * PROPÓSITO DE NEGÓCIO: manter NOMINAL a lista de lugares da tela 3.1 que reescrevem o texto de
 * uma fala, para que nenhum novo nasça sem responder à pergunta "e se for música?".
 *
 * <h2>O prejuízo que originou — medido, não hipótese</h2>
 * A 3.1 declara que <b>música é veto absoluto</b>, e o {@code FiltroAuditoriaLinha} garante isso
 * na AUDITORIA. Em 17/08/2026 uma corrida no Gundam 08th MS Team reescreveu <b>693 linhas, das
 * quais 687 eram estilo {@code Song ENG}</b> (99,1%): a letra inteira do ED de 13 episódios. A
 * porta era o {@code SincronizadorLegendaCacheService}, que roda ANTES da auditoria e só
 * perguntava {@code evento.isDialogo()} — que responde "a linha é {@code Dialogue:}", e NÃO "o
 * estilo é diálogo". O console anunciava {@code [CACHE/RECUPERADO]}, que soa como coisa boa.
 *
 * <p>A auditoria método a método que se seguiu achou uma segunda porta pelo mesmo critério: o
 * {@code RevisorPtOnlyUseCase} alteraria <b>65 falas no 86 Part 1, todas as 65 estilo
 * {@code Ending}</b>. Ele não estava em menu nenhum — e "inalcançável hoje" foi exatamente o
 * estado da ponte do cache até ela morder.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code EventoLegenda.comTexto} é a ÚNICA porta de reescrita nesta fatia: em 17/08/2026
 *       nenhum {@code new EventoLegenda(...)} existia fora de {@code legenda.infrastructure}
 *       (leitores) e {@code trocaTipoLegenda} (outra fatia). Se isso mudar, esta catraca fica
 *       cega — por isso a assinatura é verificada contra caso-controle.</li>
 *   <li>Reprovar aqui NÃO significa "apague a escrita". Significa: pergunte ao
 *       {@code PoliticaEstiloMusical} antes de escrever, ou declare em {@link #PORTAS_CONHECIDAS}
 *       quem veta no seu lugar. O que não pode é nascer calado.</li>
 *   <li>Proteção pelo CHAMADOR é aceita, mas só declarada: é frágil por natureza, e o
 *       {@code SincronizadorLegendaCacheService} tinha exatamente essa forma no dia em que
 *       falhou. Quem depende dela fica amarrado à guarda do chamador por esta catraca.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Lista as portas novas, as que sumiram e o veto ausente. Nunca lança por conta própria.
 */
class CatracaEscritaDeFalaVetaMusicaTest {

    private static final Path RAIZ = Path.of("src/main/java");

    private static final String FATIA = "org/traducao/projeto/raspagemRevisao";

    private static final String APP = FATIA + "/application/";

    /** Quem pergunta ao juiz de estilo musical por conta própria. */
    private static final String VETO_PROPRIO = "politicaEstiloMusical.estiloIgnorado";

    /** A guarda do chamador de que as portas protegidas dependem. */
    private static final String GUARDA_DO_CHAMADOR = "filtroAuditoria.deveIgnorarLinha(";

    /** O chamador que executa {@link #GUARDA_DO_CHAMADOR} antes do laço de correção. */
    private static final String CHAMADOR = APP + "RevisarLegendasUseCase.java";

    /**
     * As portas de reescrita, com o motivo de cada uma e QUEM a protege.
     *
     * <p>Ordem importa para a leitura humana, não para a asserção — daí o
     * {@link LinkedHashMap}.
     */
    private static final Map<String, String> PORTAS_CONHECIDAS = new LinkedHashMap<>();

    static {
        PORTAS_CONHECIDAS.put(APP + "SincronizadorLegendaCacheService.java",
            "VETA POR SI. A ponte 5->6 roda ANTES da auditoria, entao nao pode depender dela. "
            + "Foi esta a porta dos 687 Song ENG do 08th MS Team em 17/08/2026.");
        PORTAS_CONHECIDAS.put(APP + "RevisorPtOnlyUseCase.java",
            "VETA POR SI. Corretor PT-only: nenhum controller o alcanca hoje, mas tampouco "
            + "alcancava a ponte do cache. Alteraria 65 falas Ending no 86 Part 1.");
        PORTAS_CONHECIDAS.put(APP + "PreparadorFalaRevisao.java",
            "PROTEGIDO PELO CHAMADOR: RevisarLegendasUseCase chama deveIgnorarLinha ANTES de "
            + "preparar. Sanea chave { corrompida; nao traduz.");
        PORTAS_CONHECIDAS.put(APP + "SessaoRevisaoArquivo.java",
            "PROTEGIDO PELO CHAMADOR: aplica a decisao ja tomada, no mesmo laco, depois da "
            + "mesma guarda.");
    }

    /** As que respondem sozinhas — o veto tem de estar no arquivo, não numa promessa. */
    private static final Set<String> VETAM_POR_SI = Set.of(
        APP + "SincronizadorLegendaCacheService.java",
        APP + "RevisorPtOnlyUseCase.java");

    /**
     * PROPÓSITO DE NEGÓCIO: a varredura. Recebe a raiz e o prefixo por parâmetro para que o
     * caso-controle a exercite contra uma árvore montada à mão, sem tocar no código real.
     * <p>INVARIANTES DO DOMÍNIO: devolve caminho relativo com barra normal, para o inventário
     * ser legível igual no Windows e no Linux.
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
    @DisplayName("nenhuma escrita nova de fala nasce sem responder ao veto de música")
    void todaEscritaDeFalaRespondeAoVetoDeMusica() throws IOException {
        Set<String> encontradas = mapearEscritas(RAIZ, FATIA);

        assertFalse(encontradas.isEmpty(),
            "instrumento CEGO: nao achou NENHUMA escrita de fala na 3.1, e existem quatro. "
                + "Se comTexto mudou de nome, corrija a assinatura — nao apague a catraca.");

        List<String> novas = new ArrayList<>(encontradas);
        novas.removeAll(PORTAS_CONHECIDAS.keySet());
        assertTrue(novas.isEmpty(), () -> """
            Porta NOVA reescrevendo o texto de uma fala na 3.1: %s

            Em 17/08/2026 uma porta sem veto reescreveu 693 linhas do Gundam 08th MS Team, das
            quais 687 eram estilo Song ENG — a letra inteira do ED de 13 episodios.

            Pergunte ao legenda.domain.PoliticaEstiloMusical antes de escrever, ou declare em
            PORTAS_CONHECIDAS quem veta no seu lugar e em que linha. O que nao pode e nascer
            calada.
            """.formatted(novas));

        List<String> sumiram = new ArrayList<>(PORTAS_CONHECIDAS.keySet());
        sumiram.removeAll(encontradas);
        assertTrue(sumiram.isEmpty(),
            "porta conhecida sumiu da varredura: " + sumiram + ". Se deixou de escrever, tire do "
                + "inventario. Se so mudou de lugar, corrija o caminho — nao afrouxe a busca.");
    }

    @Test
    @DisplayName("quem escreve antes da auditoria pergunta ao juiz de estilo musical")
    void quemNaoDependeDoChamadorTemOVetoNoProprioArquivo() throws IOException {
        for (String porta : new TreeSet<>(VETAM_POR_SI)) {
            assertTrue(fonteDe(porta).contains(VETO_PROPRIO),
                porta + " perdeu a consulta a " + VETO_PROPRIO + ". Esta porta roda ANTES da "
                    + "auditoria (ou fora dela), entao o FiltroAuditoriaLinha nao a cobre: foi "
                    + "assim que 687 linhas de Song ENG foram reescritas no 08th MS Team.");
        }
    }

    @Test
    @DisplayName("a guarda de que as portas protegidas dependem continua no chamador")
    void aGuardaDoChamadorQueProtegeAsDemaisContinuaViva() throws IOException {
        assertTrue(fonteDe(CHAMADOR).contains(GUARDA_DO_CHAMADOR),
            CHAMADOR + " perdeu a chamada " + GUARDA_DO_CHAMADOR + ", e ela e a UNICA protecao de "
                + "PreparadorFalaRevisao e SessaoRevisaoArquivo — ambos declarados em "
                + "PORTAS_CONHECIDAS como 'PROTEGIDO PELO CHAMADOR'. Removendo-a, as duas passam a "
                + "escrever em karaoke sem veto.");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CASO-CONTROLE. A varredura só vale se enxergar — monta uma árvore com
     * um arquivo que reescreve fala, um que não, e um fora da fatia, e exige que ela separe os
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
                void gravar(EventoLegenda e) { novos.add(e.comTexto("nao e da 3.1")); }
            }
            """);

        Set<String> achadas = mapearEscritas(temp, FATIA);

        assertEquals(1, achadas.size(),
            "a varredura tinha de achar EXATAMENTE a porta plantada. Achou: " + achadas);
        assertTrue(achadas.contains(FATIA + "/application/PortaNova.java"), "achou: " + achadas);
    }
}
