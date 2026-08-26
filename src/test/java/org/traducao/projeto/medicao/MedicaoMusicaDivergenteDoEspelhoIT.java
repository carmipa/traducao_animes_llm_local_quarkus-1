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
import org.traducao.projeto.raspagemRevisao.application.ResolvedorArtefatosRevisao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: medir o PASSIVO de música no acervo — quantas linhas de estilo musical
 * já gravadas em {@code traducao_ptbr} divergem do espelho em inglês. Responde à pergunta que
 * ficou aberta em 17/08/2026: <i>as obras rodadas ANTES do veto têm música reescrita?</i>
 *
 * <h2>Por que este harness existe</h2>
 * A ponte do cache reescreveu 687 linhas de {@code Song ENG} no Gundam 08th MS Team antes de
 * ganhar o veto. O veto impede que aconteça de novo — <b>mas não desfaz o que já foi gravado</b>,
 * e Paulo tem ~19 obras para passar pela 3.1. Rodar por cima de um passivo não medido é repetir
 * a cegueira, agora de olhos abertos.
 *
 * <h2>O instrumento, e o que ele NÃO decide</h2>
 * <ul>
 *   <li>O critério "este estilo é música?" vem da {@link PoliticaEstiloMusical} de PRODUÇÃO, e o
 *       pareamento PT↔EN vem do {@link ResolvedorArtefatosRevisao} de produção. Reimplementar
 *       qualquer um dos dois aqui produziria o número errado com cara de certo — foi assim que
 *       14 medições saíram furadas em 07/08/2026.</li>
 *   <li>Divergência do espelho <b>não é sinônimo de dano</b>. A tela 4.1 traduz karaokê de
 *       propósito desde 10/08/2026, e o que ela grava também diverge do inglês. Este harness
 *       CONTA e localiza; dizer qual obra passou pela 4.1 é informação que ele não tem.</li>
 *   <li>Só conta linha cujo índice existe nos DOIS arquivos: sem espelho não há divergência
 *       comprovável, e chutar seria inventar passivo.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Acervo ausente REPROVA com o caminho na mensagem — resultado vazio por acervo inacessível não
 * pode ser lido como "não há passivo" (regra 8: zero é hipótese até o instrumento provar que
 * enxerga). Pasta PT sem par em inglês é REPORTADA, nunca contada como zero.
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoMusicaDivergenteDoEspelhoIT {

    private static final Path ACERVO =
        Path.of(System.getProperty("kronos.acervo", "C:\\animes"));

    private static final String PASTA_PT = "traducao_ptbr";

    /** As pastas de entrada em inglês, na ordem em que se procura o espelho. */
    private static final List<String> PASTAS_EN =
        List.of("legendas_extraidas_ass", "legendas_eng", "legendas_originais");

    @Inject
    LeitorLegendaAss leitor;

    @Inject
    PoliticaEstiloMusical politicaEstiloMusical;

    @Inject
    ResolvedorArtefatosRevisao resolvedor;

    /** O que se apurou de uma obra. */
    private record Obra(String nome, int arquivosPt, int arquivosComEspelho,
                        long linhasMusicais, long divergentes, List<String> amostra) {}

    @Test
    @DisplayName("mede a musica divergente do espelho ingles em todo o acervo")
    void medePassivoDeMusica() throws IOException {
        assertTrue(Files.isDirectory(ACERVO),
            "acervo inacessivel em " + ACERVO + " — sem ele o resultado vazio significaria "
                + "\"nao consegui medir\", nunca \"nao ha passivo\"");

        List<Path> pastasPt;
        // Alcance pelo DONO UNICO: honra -Dkronos.medicao.obra e declara NAO VERIFICADO
        // quando o filtro nao casa.
        pastasPt = org.traducao.projeto.medicao.AlcanceDaMedicao.pastasDeTraducao();
        assertTrue(!pastasPt.isEmpty(),
            "nenhuma pasta " + PASTA_PT + " encontrada em " + ACERVO + " — instrumento CEGO");

        List<Obra> obras = new ArrayList<>();
        List<String> semEspelho = new ArrayList<>();

        for (Path pastaPt : pastasPt) {
            Path raizObra = pastaPt.getParent();
            String nome = raizObra == null ? pastaPt.toString() : raizObra.getFileName().toString();

            Path pastaEn = null;
            for (String candidata : PASTAS_EN) {
                Path tentativa = raizObra == null ? null : raizObra.resolve(candidata);
                if (tentativa != null && Files.isDirectory(tentativa)) {
                    pastaEn = tentativa;
                    break;
                }
            }
            if (pastaEn == null) {
                semEspelho.add(nome + "  (nenhuma de " + PASTAS_EN + ")");
                continue;
            }

            List<Path> arquivosPt;
            try (Stream<Path> s = Files.list(pastaPt)) {
                arquivosPt = s.filter(Files::isRegularFile)
                    .filter(resolvedor::temExtensaoSuportada)
                    .sorted()
                    .toList();
            }

            int comEspelho = 0;
            long musicais = 0;
            long divergentes = 0;
            List<String> amostra = new ArrayList<>();

            for (Path arquivoPt : arquivosPt) {
                Path arquivoEn = resolvedor.resolverArquivoOriginal(arquivoPt, pastaEn);
                if (arquivoEn == null || !Files.isRegularFile(arquivoEn)) {
                    continue;
                }
                comEspelho++;

                Map<Integer, String> espelho = new HashMap<>();
                DocumentoLegenda docEn = leitor.ler(arquivoEn);
                for (EventoLegenda e : docEn.eventos()) {
                    if (e.isDialogo() && e.texto() != null) {
                        espelho.put(e.indice(), e.texto());
                    }
                }

                DocumentoLegenda docPt = leitor.ler(arquivoPt);
                for (EventoLegenda e : docPt.eventos()) {
                    if (!e.isDialogo() || e.texto() == null || e.estilo() == null) {
                        continue;
                    }
                    if (!politicaEstiloMusical.estiloIgnorado(e.estilo())) {
                        continue;
                    }
                    String original = espelho.get(e.indice());
                    if (original == null) {
                        continue;   // sem espelho para ESTA linha: nao ha divergencia comprovavel
                    }
                    musicais++;
                    if (!original.equals(e.texto())) {
                        divergentes++;
                        if (amostra.size() < 3) {
                            amostra.add(arquivoPt.getFileName() + " [" + e.estilo() + " #"
                                + e.indice() + "]\n            EN: " + recortar(original)
                                + "\n            PT: " + recortar(e.texto()));
                        }
                    }
                }
            }
            obras.add(new Obra(nome, arquivosPt.size(), comEspelho, musicais, divergentes, amostra));
        }

        imprimir(obras, semEspelho);
    }

    private static String recortar(String texto) {
        String limpo = texto.replace("\n", " ");
        return limpo.length() <= 90 ? limpo : limpo.substring(0, 90) + "…";
    }

    private static void imprimir(List<Obra> obras, List<String> semEspelho) {
        long totalMusicais = obras.stream().mapToLong(Obra::linhasMusicais).sum();
        long totalDivergentes = obras.stream().mapToLong(Obra::divergentes).sum();

        System.out.println("\n===== PASSIVO DE MUSICA — linhas de estilo musical x espelho EN =====");
        System.out.printf("acervo: %s%n", ACERVO);
        System.out.println();
        System.out.printf("%-52s %6s %6s %9s %11s%n",
            "OBRA", "ass", "cEsp", "musicais", "divergentes");
        System.out.println("-".repeat(90));

        Map<String, Obra> ordenadas = new TreeMap<>();
        for (Obra o : obras) ordenadas.put(o.divergentes() + "|" + o.nome(), o);
        List<Obra> desc = new ArrayList<>(ordenadas.values());
        desc.sort((a, b) -> Long.compare(b.divergentes(), a.divergentes()));

        for (Obra o : desc) {
            System.out.printf("%-52s %6d %6d %9d %11d%n",
                o.nome().length() > 52 ? o.nome().substring(0, 52) : o.nome(),
                o.arquivosPt(), o.arquivosComEspelho(), o.linhasMusicais(), o.divergentes());
        }
        System.out.println("-".repeat(90));
        System.out.printf("%-52s %6s %6s %9d %11d%n",
            "TOTAL (" + obras.size() + " obras)", "", "", totalMusicais, totalDivergentes);

        System.out.println("\n----- amostra das obras com divergencia -----");
        for (Obra o : desc) {
            if (o.divergentes() == 0 || o.amostra().isEmpty()) continue;
            System.out.println("\n  " + o.nome() + "  (" + o.divergentes() + " divergentes)");
            for (String linha : o.amostra()) System.out.println("     " + linha);
        }

        if (!semEspelho.isEmpty()) {
            System.out.println("\n----- SEM ESPELHO (nao medidas — NAO sao zero) -----");
            for (String s : semEspelho) System.out.println("  " + s);
        }
        System.out.println("\n=====================================================================\n");
    }
}
