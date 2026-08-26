package org.traducao.projeto.medicao;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.revisaoLore.application.AlcanceRevisaoLore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: medir quantas linhas de <b>CARTAZ</b> a tela 3.2 ainda olha — e,
 * portanto, manda ao modelo — depois de todos os vetos que ela já tem.
 *
 * <h2>O achado que originou, visto na corrida do 08th MS Team em 17/08/2026</h2>
 * <pre>
 *   EN: {\fs200\blur1\pos(720,650)\c&amp;HDEDEE3&amp;}NEXT EPISODE
 *   PT: {\fs200\blur1\pos(720,650)\c&amp;HDEDEE3&amp;}Proximo episodio.
 * </pre>
 * Corpo 200, blur, posicionamento absoluto — isso é letreiro, não fala, e a tradução está
 * CERTA. Ainda assim rendeu <b>10 acusações</b> naquela obra, e o mesmo padrão apareceu no 86
 * pela manhã ({@code \an2\pos\fn}) sem que eu tivesse dado nome a ele.
 *
 * <p>A causa está em {@code ProtecaoLegendaAssService.deveBloquearAntesDoLlm}: a última porta
 * exige {@code clipLongo}. A regra nasceu do karaokê com {@code \clip(m … l …)} do Zeta e não
 * cobre cartaz posicionado sem clip.
 *
 * <h2>O critério candidato, e por que ele foi medido antes de virar código</h2>
 * Corpo de fonte grande. Varredura do acervo PT (337.721 linhas {@code Dialogue}):
 * <pre>
 *   \fs &gt;= 100  ->  9.083   "Gundam" · "MOBILE SUIT GUNDAM" · "Proximo episodio"   CARTAZ
 *   \fs 60-99   ->  2.501   "Oh, I can't help believing you" · romaji               MUSICA
 *   \fs &lt; 60    ->  1.813   "Not an anime." · "It's true!"                         DIALOGO
 * </pre>
 * O corte separa limpo: diálogo com fonte customizada fica abaixo de 60. Mas volume bruto não é
 * o que importa — a maioria dessas linhas já é barrada por estilo musical ou karaokê. O número
 * que justifica mexer na regra é quantas <b>sobrevivem a todos os vetos atuais</b>, e é isso que
 * este harness mede, perguntando ao {@link AlcanceRevisaoLore} de PRODUÇÃO.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Acervo ausente REPROVA — resultado vazio por acervo inacessível não pode ser lido como "não há
 * cartaz no alcance". Controle positivo: o total no alcance precisa ser maior que zero, senão o
 * instrumento está cego e o recorte dele não vale.
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoCartazNoAlcanceDaLoreIT {

    private static final Path ACERVO =
        Path.of(System.getProperty("kronos.acervo", "C:\\animes"));

    private static final String PASTA_PT = "traducao_ptbr";

    private static final Pattern CORPO_DE_FONTE = Pattern.compile("\\\\fs(\\d+)");

    @Inject
    LeitorLegendaAss leitor;

    @Inject
    AlcanceRevisaoLore alcance;

    @Test
    @DisplayName("mede o cartaz que sobrevive a todos os vetos e chega na tela 3.2")
    void medeCartazQueChegaNaTela() throws IOException {
        assertTrue(Files.isDirectory(ACERVO),
            "acervo inacessivel em " + ACERVO + " — sem ele o zero significaria \"nao consegui "
                + "medir\", nunca \"nao ha cartaz no alcance\"");

        List<Path> arquivos;
        // Alcance pelo DONO UNICO: honra -Dkronos.medicao.obra e declara NAO VERIFICADO
        // quando o filtro nao casa.
        List<Path> reunidos = new ArrayList<>();
        for (Path pastaPt : org.traducao.projeto.medicao.AlcanceDaMedicao.pastasDeTraducao()) {
            try (Stream<Path> naPasta = Files.list(pastaPt)) {
                naPasta.filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".ass"))
                    .forEach(reunidos::add);
            }
        }
        arquivos = reunidos.stream().sorted().toList();
        assertTrue(!arquivos.isEmpty(), "nenhum .ass em " + PASTA_PT + " — instrumento CEGO");

        long noAlcance = 0;
        long cartazNoAlcance = 0;
        List<String> amostra = new ArrayList<>();

        for (Path arquivo : arquivos) {
            DocumentoLegenda doc;
            try {
                doc = leitor.ler(arquivo);
            } catch (Exception e) {
                continue;
            }
            for (EventoLegenda evento : doc.eventos()) {
                if (!alcance.estaNoAlcance(evento)) {
                    continue;
                }
                noAlcance++;
                Matcher m = CORPO_DE_FONTE.matcher(evento.texto());
                if (m.find() && Integer.parseInt(m.group(1)) >= 100) {
                    cartazNoAlcance++;
                    if (amostra.size() < 8) {
                        amostra.add(arquivo.getFileName() + " [" + evento.estilo() + "]  "
                            + recortar(evento.texto()));
                    }
                }
            }
        }

        System.out.println("\n===== CARTAZ QUE CHEGA NA 3.2 (depois de TODOS os vetos) =====");
        System.out.printf("arquivos PT lidos ...... %d%n", arquivos.size());
        System.out.printf("linhas NO ALCANCE ...... %d%n", noAlcance);
        System.out.printf("delas, com \\fs>=100 .... %d   <- cartaz que a tela olha hoje%n", cartazNoAlcance);
        System.out.println("\n----- amostra -----");
        amostra.forEach(a -> System.out.println("  " + a));
        System.out.println("==============================================================\n");

        assertTrue(noAlcance > 0,
            "CONTROLE POSITIVO REPROVADO: zero linha no alcance em " + arquivos.size()
                + " arquivos. O acervo tem dialogo — este zero acusa o instrumento, nao o acervo.");
    }

    private static String recortar(String texto) {
        String limpo = texto.replace("\n", " ");
        return limpo.length() <= 95 ? limpo : limpo.substring(0, 95) + "…";
    }
}
