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
import org.traducao.projeto.lore.infrastructure.GerenciadorContexto;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.revisaoLore.application.CorretorLoreDeterministico;
import org.traducao.projeto.revisaoLore.application.GerenciadorPromptRevisaoLore;
import org.traducao.projeto.revisaoLore.application.RevisarLorePtOnlyUseCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: medir o que a aba <b>PT-only</b> da tela 3.2 (Revisão de Lore) alcança
 * hoje em ESTILO MUSICAL no acervo. A pergunta é a mesma que custou 687 linhas de {@code Song
 * ENG} no Gundam 08th MS Team em 17/08/2026, feita agora na outra tela: <i>esta porta de escrita
 * pergunta "e se for música?" antes de reescrever?</i>
 *
 * <h2>O que se sabe por leitura, e por isso precisa de número</h2>
 * {@code RevisarLoreUseCase} (aba "Com inglês") consulta a {@link PoliticaEstiloMusical} em
 * {@code ehEventoAuditavelLore}. {@code RevisarLorePtOnlyUseCase} percorre
 * {@code documento.eventos()} filtrando apenas {@code temTexto()} — sem juiz de estilo musical,
 * sem {@code isDialogo()}, sem detector de karaokê. Ela grava no {@code .ass} quando
 * {@code aplicar=true}, e "Apenas simular" nasce DESMARCADO na interface.
 *
 * <p>A catraca {@code CatracaEscritaDeFalaVetaMusicaTest} não cobre isto: ela varre o prefixo
 * {@code org/traducao/projeto/raspagemRevisao} — a fatia da 3.1. A fatia {@code revisaoLore} tem
 * duas portas próprias e nenhuma catraca nominal.
 *
 * <h2>O instrumento, e o que ele NÃO decide</h2>
 * <ul>
 *   <li>"Este estilo é música?" vem da {@link PoliticaEstiloMusical} de PRODUÇÃO. "Esta fala seria
 *       reescrita?" vem do {@link CorretorLoreDeterministico#corrigirPtOnly} de PRODUÇÃO, com o
 *       mapa de terminologia que o {@link GerenciadorPromptRevisaoLore} entrega. "Que obra é
 *       esta pasta?" vem do {@link GerenciadorContexto#idsQueReconhecem(String)} de PRODUÇÃO.
 *       Nenhum dos três critérios é reimplementado aqui.</li>
 *   <li><b>CALIBRAÇÃO CONTRA A PRÓPRIA PRODUÇÃO:</b> o laço deste harness é conferido contra o
 *       {@link RevisarLorePtOnlyUseCase#executar} rodado em dry-run na mesma pasta. A identidade
 *       exigida é {@code hits do harness == falasCorrigidas + falasDescartadas da produção} — a
 *       produção desconta as propostas reprovadas pelo validador, o harness conta antes dele.
 *       Divergência REPROVA: um laço que conta outra coisa produz número errado com cara de
 *       certo, que é como 14 medições saíram furadas em 07/08/2026.</li>
 *   <li>Pasta cuja obra não resolve para UM contexto entra em <b>NÃO MEDIDA</b>, nunca em zero.
 *       Sem identidade não há mapa de terminologia, e chutar o contexto seria repetir o
 *       incidente dos 15 caches de 0083 carimbados como {@code guilty_crown}.</li>
 *   <li>A camada 2 (LLM PT-only) decide por um portão de homógrafo que é privado da produção.
 *       Este harness NÃO o replica: mede a SUPERFÍCIE — quantas linhas musicais estão ao alcance
 *       das duas camadas — e deixa explícito que o recorte da camada 2 dentro dela não foi
 *       medido. Superfície é o que o veto no laço elimina, então é a grandeza que importa.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Acervo ausente REPROVA com o caminho na mensagem: resultado vazio por acervo inacessível não
 * pode ser lido como "não há exposição" (regra 8 — zero é hipótese até o instrumento provar que
 * enxerga). Nenhuma linha musical encontrada em NENHUMA obra também REPROVA, como controle
 * positivo: acervo com 23 obras sem uma linha de música é defeito do instrumento, não do acervo.
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoExposicaoMusicalRevisaoLorePtOnlyIT {

    private static final Path ACERVO =
        Path.of(System.getProperty("kronos.acervo", "C:\\animes"));

    private static final String PASTA_PT = "traducao_ptbr";

    @Inject
    LeitorLegendaAss leitor;

    @Inject
    PoliticaEstiloMusical politicaEstiloMusical;

    @Inject
    MascaradorTags mascarador;

    @Inject
    CorretorLoreDeterministico corretorLore;

    @Inject
    GerenciadorPromptRevisaoLore promptRevisaoLore;

    @Inject
    GerenciadorContexto gerenciadorContexto;

    @Inject
    RevisarLorePtOnlyUseCase revisarLorePtOnly;

    /** O que se apurou de uma obra do acervo. */
    private record Obra(
        String nome,
        String contextoId,
        int arquivos,
        long superficieMusical,
        long superficieNaoMusical,
        long reescreveriaMusical,
        long reescreveriaNaoMusical,
        long producaoCorrigidas,
        long producaoDescartadas,
        List<String> amostra
    ) {}

    @Test
    @DisplayName("mede o que a aba PT-only da 3.2 alcanca e reescreveria em estilo musical")
    void medeExposicaoMusicalDaAbaPtOnly() throws IOException {
        assertTrue(Files.isDirectory(ACERVO),
            "acervo inacessivel em " + ACERVO + " — sem ele o resultado vazio significaria "
                + "\"nao consegui medir\", nunca \"nao ha exposicao\"");

        List<Path> pastasPt;
        try (Stream<Path> caminhos = Files.walk(ACERVO)) {
            pastasPt = caminhos
                .filter(Files::isDirectory)
                .filter(p -> PASTA_PT.equals(p.getFileName().toString()))
                .sorted()
                .toList();
        }
        assertTrue(!pastasPt.isEmpty(),
            "nenhuma pasta " + PASTA_PT + " encontrada em " + ACERVO + " — instrumento CEGO");

        List<Obra> obras = new ArrayList<>();
        List<String> naoMedidas = new ArrayList<>();
        List<String> divergenciasDeCalibracao = new ArrayList<>();

        for (Path pastaPt : pastasPt) {
            Path raizObra = pastaPt.getParent();
            String nome = raizObra == null ? pastaPt.toString() : raizObra.getFileName().toString();

            Set<String> ids = gerenciadorContexto.idsQueReconhecem(nome);
            if (ids.size() != 1) {
                naoMedidas.add(nome + "  (obra resolveu para " + ids.size() + " contexto(s): " + ids + ")");
                continue;
            }
            String contextoId = ids.iterator().next();
            if (!promptRevisaoLore.existePrompt(contextoId)) {
                naoMedidas.add(nome + "  (contexto " + contextoId + " sem prompt de revisao de lore)");
                continue;
            }
            Map<String, String> correcoes = promptRevisaoLore.correcoesTerminologia(contextoId);

            List<Path> arquivosPt;
            try (Stream<Path> s = Files.list(pastaPt)) {
                arquivosPt = s.filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString().toLowerCase();
                        return n.endsWith(".ass") || n.endsWith(".ssa");
                    })
                    .sorted()
                    .toList();
            }

            long superficieMusical = 0;
            long superficieNaoMusical = 0;
            long reescreveriaMusical = 0;
            long reescreveriaNaoMusical = 0;
            List<String> amostra = new ArrayList<>();

            for (Path arquivoPt : arquivosPt) {
                DocumentoLegenda doc;
                try {
                    doc = leitor.ler(arquivoPt);
                } catch (Exception e) {
                    continue;
                }
                for (EventoLegenda evento : doc.eventos()) {
                    // Mesmo recorte do laço de producao: so temTexto(). E exatamente por ser so
                    // isto que a superficie inclui musica, Comment e karaoke.
                    if (!evento.temTexto()) {
                        continue;
                    }
                    boolean musical = evento.estilo() != null
                        && politicaEstiloMusical.estiloIgnorado(evento.estilo());
                    if (musical) {
                        superficieMusical++;
                    } else {
                        superficieNaoMusical++;
                    }

                    String mascarado = mascarador.mascarar(evento.texto()).texto();
                    if (corretorLore.corrigirPtOnly(mascarado, correcoes).isEmpty()) {
                        continue;
                    }
                    if (musical) {
                        reescreveriaMusical++;
                        if (amostra.size() < 3) {
                            amostra.add(arquivoPt.getFileName() + " [" + evento.estilo() + " #"
                                + evento.indice() + "]  " + recortar(evento.texto()));
                        }
                    } else {
                        reescreveriaNaoMusical++;
                    }
                }
            }

            // CONTROLE: a producao roda na MESMA pasta, em dry-run e sem LLM. O laco acima so
            // vale se bater com ela.
            RevisarLorePtOnlyUseCase.ResultadoLorePtOnly producao =
                revisarLorePtOnly.executar(pastaPt, contextoId, false, false);
            long esperado = producao.falasCorrigidas() + producao.falasDescartadas();
            long medido = reescreveriaMusical + reescreveriaNaoMusical;
            if (esperado != medido) {
                divergenciasDeCalibracao.add(nome + ": harness=" + medido
                    + " producao(corrigidas+descartadas)=" + esperado);
            }

            obras.add(new Obra(nome, contextoId, arquivosPt.size(),
                superficieMusical, superficieNaoMusical,
                reescreveriaMusical, reescreveriaNaoMusical,
                producao.falasCorrigidas(), producao.falasDescartadas(), amostra));
        }

        imprimir(obras, naoMedidas, divergenciasDeCalibracao);

        assertTrue(divergenciasDeCalibracao.isEmpty(),
            "o laco deste harness NAO bate com o RevisarLorePtOnlyUseCase de producao na mesma "
                + "pasta — a medicao inteira esta invalidada ate isto fechar: "
                + divergenciasDeCalibracao);

        long totalSuperficieMusical = obras.stream().mapToLong(Obra::superficieMusical).sum();
        assertTrue(totalSuperficieMusical > 0,
            "CONTROLE POSITIVO REPROVADO: zero linha de estilo musical em " + obras.size()
                + " obra(s) medida(s). O acervo tem OP/ED em praticamente toda obra — este zero "
                + "acusa o instrumento (juiz de estilo, leitor ou varredura), nao o acervo.");
    }

    private static String recortar(String texto) {
        String limpo = texto.replace("\n", " ").replace("\\N", " ");
        return limpo.length() <= 80 ? limpo : limpo.substring(0, 80) + "…";
    }

    private static void imprimir(
        List<Obra> obras, List<String> naoMedidas, List<String> divergencias) {

        long superficieMusical = obras.stream().mapToLong(Obra::superficieMusical).sum();
        long reescreveriaMusical = obras.stream().mapToLong(Obra::reescreveriaMusical).sum();
        long reescreveriaDialogo = obras.stream().mapToLong(Obra::reescreveriaNaoMusical).sum();

        System.out.println("\n===== 3.2 PT-ONLY — EXPOSICAO A ESTILO MUSICAL =====");
        System.out.printf("acervo: %s%n%n", ACERVO);
        System.out.printf("%-44s %-22s %5s %11s %11s %11s%n",
            "OBRA", "contexto", "ass", "sup.MUSICA", "esc.MUSICA", "esc.dialogo");
        System.out.println("-".repeat(112));

        List<Obra> desc = new ArrayList<>(obras);
        desc.sort((a, b) -> Long.compare(b.reescreveriaMusical(), a.reescreveriaMusical()));
        for (Obra o : desc) {
            System.out.printf("%-44s %-22s %5d %11d %11d %11d%n",
                corta(o.nome(), 44), corta(o.contextoId(), 22), o.arquivos(),
                o.superficieMusical(), o.reescreveriaMusical(), o.reescreveriaNaoMusical());
        }
        System.out.println("-".repeat(112));
        System.out.printf("%-44s %-22s %5s %11d %11d %11d%n",
            "TOTAL (" + obras.size() + " obras medidas)", "", "",
            superficieMusical, reescreveriaMusical, reescreveriaDialogo);

        System.out.println("\n  sup.MUSICA  = linhas de estilo musical ao ALCANCE da aba (o que o veto elimina)");
        System.out.println("  esc.MUSICA  = destas, quantas a camada DETERMINISTICA reescreveria AGORA");
        System.out.println("  esc.dialogo = trabalho legitimo da tela (controle negativo do veto)");
        System.out.println("  a camada 2 (LLM) decide dentro de sup.MUSICA por portao NAO medido aqui");

        System.out.println("\n----- amostra do que seria reescrito EM MUSICA -----");
        boolean houve = false;
        for (Obra o : desc) {
            if (o.amostra().isEmpty()) {
                continue;
            }
            houve = true;
            System.out.println("\n  " + o.nome() + "  (" + o.reescreveriaMusical() + " em musica)");
            for (String linha : o.amostra()) {
                System.out.println("     " + linha);
            }
        }
        if (!houve) {
            System.out.println("  (nenhuma — a camada deterministica nao acerta musica no acervo de hoje)");
        }

        System.out.println("\n----- calibracao contra a producao (dry-run na mesma pasta) -----");
        for (Obra o : desc) {
            System.out.printf("  %-44s harness=%d  producao: corrigidas=%d descartadas=%d%n",
                corta(o.nome(), 44), o.reescreveriaMusical() + o.reescreveriaNaoMusical(),
                o.producaoCorrigidas(), o.producaoDescartadas());
        }
        if (divergencias.isEmpty()) {
            System.out.println("  => bate em todas as obras medidas");
        }

        if (!naoMedidas.isEmpty()) {
            System.out.println("\n----- NAO MEDIDAS (obra nao resolve para um contexto — NAO sao zero) -----");
            for (String s : naoMedidas) {
                System.out.println("  " + s);
            }
        }
        System.out.println("\n====================================================\n");
    }

    private static String corta(String texto, int max) {
        if (texto == null) {
            return "";
        }
        return texto.length() > max ? texto.substring(0, max) : texto;
    }
}
