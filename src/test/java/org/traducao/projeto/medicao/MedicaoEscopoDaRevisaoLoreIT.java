package org.traducao.projeto.medicao;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.lore.domain.PromptRevisaoLore;
import org.traducao.projeto.lore.infrastructure.GerenciadorContexto;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.raspagemRevisao.application.ResolvedorArtefatosRevisao;
import org.traducao.projeto.revisaoLore.application.AlcanceRevisaoLore;
import org.traducao.projeto.revisaoLore.application.CorretorLoreDeterministico;
import org.traducao.projeto.revisaoLore.application.DetectorTermosLoreService;
import org.traducao.projeto.revisaoLore.application.GerenciadorPromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ResultadoDeteccaoLore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: dar NÚMERO às duas mudanças de escopo da tela 3.2 decididas por Paulo em
 * 17/08/2026, ANTES de elas alterarem o acervo:
 *
 * <ol>
 *   <li><b>o que sai</b> — quantos motivos hoje vêm das regras que NÃO são nome/local/termo de
 *       lore (sigla em caixa alta e resíduo genérico em inglês), contra as que são;</li>
 *   <li><b>o que entra</b> — quantas falas o corretor determinístico corrigiria FORA do que a
 *       heurística sinaliza. Hoje ele nunca as alcança, porque o {@code continue} da heurística
 *       acontece antes dele; passá-lo a rodar em toda fala no alcance é o certo, mas
 *       <b>aumenta o que é gravado</b>, e isso não se troca às cegas.</li>
 * </ol>
 *
 * <h2>O instrumento, e o que ele NÃO decide</h2>
 * Todos os critérios são PERGUNTADOS à produção: {@link DetectorTermosLoreService} (o que é
 * indício de lore), {@link CorretorLoreDeterministico} (o que seria corrigido),
 * {@link AlcanceRevisaoLore} (que linha a tela pode olhar), {@link GerenciadorContexto} (que obra
 * é esta pasta) e {@link ResolvedorArtefatosRevisao} (qual é o espelho em inglês). A
 * classificação por regra usa o PREFIXO da mensagem que o próprio detector emite — nenhuma regra
 * é reimplementada aqui.
 *
 * <p>Pareamento EN↔PT por {@code indice} do evento, como o harness de música. Linha sem espelho
 * é PULADA, nunca contada como limpa: sem o inglês o detector devolve resultado limpo por falta
 * de entrada, e isso viraria um zero por cegueira.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Acervo ausente REPROVA com o caminho na mensagem. Obra que não resolve para UM contexto entra
 * em NÃO MEDIDA, nunca em zero. Controle positivo: o total de motivos precisa ser maior que zero,
 * senão o instrumento está cego e nenhum recorte dele vale.
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoEscopoDaRevisaoLoreIT {

    private static final Path ACERVO =
        Path.of(System.getProperty("kronos.acervo", "C:\\animes"));

    private static final String PASTA_PT = "traducao_ptbr";

    private static final List<String> PASTAS_EN =
        List.of("legendas_extraidas_ass", "legendas_eng", "legendas_originais");

    /**
     * Os cinco geradores de motivo do detector, pelo PREFIXO que cada um emite, e o veredito de
     * escopo que Paulo fechou. A ordem é a da leitura humana.
     */
    private static final Map<String, String> REGRAS = new LinkedHashMap<>();

    static {
        REGRAS.put("Possivel traducao literal", "FICA  (nome canonico traduzido ao pe da letra)");
        REGRAS.put("Nome proprio do original", "FICA  (nome do EN ausente no PT)");
        REGRAS.put("Nome proprio composto", "FICA  (nome composto preservado pela metade)");
        REGRAS.put("Termo de faccao", "FICA  (termo de lore que ficou em ingles)");
        REGRAS.put("Possivel nome/termo em ingles remanescente", "SAI   (B: falta de traducao = 3.1)");
        REGRAS.put("Sigla ou termo todo em maiusculas", "SAI   (A: nao e nome nem local)");
    }

    @Inject LeitorLegendaAss leitor;
    @Inject AlcanceRevisaoLore alcance;
    @Inject MascaradorTags mascarador;
    @Inject DetectorTermosLoreService detector;
    @Inject CorretorLoreDeterministico corretorLore;
    @Inject GerenciadorPromptRevisaoLore promptRevisaoLore;
    @Inject GerenciadorContexto gerenciadorContexto;
    @Inject ResolvedorArtefatosRevisao resolvedor;

    private record Obra(
        String nome, String contextoId, int arquivosComEspelho,
        long falasNoAlcance, long suspeitas,
        long corrigeDentroDaHeuristica, long corrigeForaDaHeuristica,
        Map<String, Long> motivosPorRegra, List<String> amostraForaDaHeuristica
    ) {}

    @Test
    @DisplayName("mede o que sai e o que entra no escopo da 3.2, antes de mexer no acervo")
    void medeEscopoDaRevisaoDeLore() throws IOException {
        assertTrue(Files.isDirectory(ACERVO),
            "acervo inacessivel em " + ACERVO + " — sem ele o resultado vazio significaria "
                + "\"nao consegui medir\", nunca \"nao ha efeito\"");

        List<Path> pastasPt;
        try (Stream<Path> caminhos = Files.walk(ACERVO)) {
            pastasPt = caminhos.filter(Files::isDirectory)
                .filter(p -> PASTA_PT.equals(p.getFileName().toString()))
                .sorted().toList();
        }
        assertTrue(!pastasPt.isEmpty(), "nenhuma pasta " + PASTA_PT + " em " + ACERVO + " — CEGO");

        List<Obra> obras = new ArrayList<>();
        List<String> naoMedidas = new ArrayList<>();

        for (Path pastaPt : pastasPt) {
            Path raizObra = pastaPt.getParent();
            String nome = raizObra == null ? pastaPt.toString() : raizObra.getFileName().toString();

            Set<String> ids = gerenciadorContexto.idsQueReconhecem(nome);
            if (ids.size() != 1 || !promptRevisaoLore.existePrompt(ids.iterator().next())) {
                naoMedidas.add(nome + "  (contextos que reconhecem: " + ids + ")");
                continue;
            }
            String contextoId = ids.iterator().next();

            Path pastaEn = null;
            for (String candidata : PASTAS_EN) {
                Path tentativa = raizObra == null ? null : raizObra.resolve(candidata);
                if (tentativa != null && Files.isDirectory(tentativa)) {
                    pastaEn = tentativa;
                    break;
                }
            }
            if (pastaEn == null) {
                naoMedidas.add(nome + "  (sem espelho em ingles: " + PASTAS_EN + ")");
                continue;
            }

            Map<String, String> correcoes = promptRevisaoLore.correcoesTerminologia(contextoId);
            String lore = PromptRevisaoLore.extrairLoreCanonica(
                promptRevisaoLore.obterPromptSistema(contextoId));

            List<Path> arquivosPt;
            try (Stream<Path> s = Files.list(pastaPt)) {
                arquivosPt = s.filter(Files::isRegularFile)
                    .filter(resolvedor::temExtensaoSuportada).sorted().toList();
            }

            int comEspelho = 0;
            long noAlcance = 0;
            long suspeitas = 0;
            long corrigeDentro = 0;
            long corrigeFora = 0;
            Map<String, Long> porRegra = new LinkedHashMap<>();
            List<String> amostra = new ArrayList<>();

            for (Path arquivoPt : arquivosPt) {
                Path arquivoEn = resolvedor.resolverArquivoOriginal(arquivoPt, pastaEn);
                if (arquivoEn == null || !Files.isRegularFile(arquivoEn)) {
                    continue;
                }
                comEspelho++;

                Map<Integer, EventoLegenda> espelho = new HashMap<>();
                DocumentoLegenda docEn;
                DocumentoLegenda docPt;
                try {
                    docEn = leitor.ler(arquivoEn);
                    docPt = leitor.ler(arquivoPt);
                } catch (Exception e) {
                    continue;
                }
                for (EventoLegenda e : docEn.eventos()) {
                    espelho.put(e.indice(), e);
                }

                for (EventoLegenda pt : docPt.eventos()) {
                    EventoLegenda en = espelho.get(pt.indice());
                    if (en == null || !alcance.estaNoAlcance(en) || !pt.isDialogo() || !pt.temTexto()) {
                        continue;
                    }
                    noAlcance++;

                    String enMasc = mascarador.mascarar(en.texto()).texto();
                    String ptMasc = mascarador.mascarar(pt.texto()).texto();

                    ResultadoDeteccaoLore deteccao = detector.auditar(enMasc, ptMasc, lore);
                    if (deteccao.suspeito()) {
                        suspeitas++;
                        for (String motivo : deteccao.motivos()) {
                            porRegra.merge(classificar(motivo), 1L, Long::sum);
                        }
                    }

                    if (corretorLore.corrigir(enMasc, ptMasc, correcoes).isPresent()) {
                        if (deteccao.suspeito()) {
                            corrigeDentro++;
                        } else {
                            corrigeFora++;
                            if (amostra.size() < 4) {
                                amostra.add(arquivoPt.getFileName() + " #" + pt.indice()
                                    + "\n            EN: " + recortar(en.texto())
                                    + "\n            PT: " + recortar(pt.texto()));
                            }
                        }
                    }
                }
            }
            obras.add(new Obra(nome, contextoId, comEspelho, noAlcance, suspeitas,
                corrigeDentro, corrigeFora, porRegra, amostra));
        }

        imprimir(obras, naoMedidas);

        long totalMotivos = obras.stream()
            .flatMap(o -> o.motivosPorRegra().values().stream()).mapToLong(Long::longValue).sum();
        assertTrue(totalMotivos > 0,
            "CONTROLE POSITIVO REPROVADO: zero motivo em " + obras.size() + " obra(s). O detector "
                + "de producao acusa em qualquer acervo real — este zero acusa o instrumento "
                + "(pareamento, alcance ou mascaramento), nao o acervo.");
    }

    private static String classificar(String motivo) {
        for (String prefixo : REGRAS.keySet()) {
            if (motivo.startsWith(prefixo)) {
                return prefixo;
            }
        }
        return "(motivo NAO classificado)";
    }

    private static String recortar(String texto) {
        String limpo = texto.replace("\n", " ").replace("\\N", " ");
        return limpo.length() <= 88 ? limpo : limpo.substring(0, 88) + "…";
    }

    private static void imprimir(List<Obra> obras, List<String> naoMedidas) {
        System.out.println("\n===== 3.2 — ESCOPO: O QUE SAI E O QUE ENTRA =====");
        System.out.printf("acervo: %s%n%n", ACERVO);

        System.out.printf("%-40s %5s %10s %10s %9s %9s%n",
            "OBRA", "ass", "noAlcance", "suspeitas", "det.DENTRO", "det.FORA");
        System.out.println("-".repeat(92));
        List<Obra> desc = new ArrayList<>(obras);
        desc.sort((a, b) -> Long.compare(b.corrigeForaDaHeuristica(), a.corrigeForaDaHeuristica()));
        for (Obra o : desc) {
            System.out.printf("%-40s %5d %10d %10d %9d %9d%n",
                corta(o.nome(), 40), o.arquivosComEspelho(), o.falasNoAlcance(), o.suspeitas(),
                o.corrigeDentroDaHeuristica(), o.corrigeForaDaHeuristica());
        }
        System.out.println("-".repeat(92));
        System.out.printf("%-40s %5s %10d %10d %9d %9d%n", "TOTAL (" + obras.size() + " obras)", "",
            obras.stream().mapToLong(Obra::falasNoAlcance).sum(),
            obras.stream().mapToLong(Obra::suspeitas).sum(),
            obras.stream().mapToLong(Obra::corrigeDentroDaHeuristica).sum(),
            obras.stream().mapToLong(Obra::corrigeForaDaHeuristica).sum());
        System.out.println("\n  det.FORA = o DELTA: falas que o corretor deterministico passaria a corrigir");
        System.out.println("             se rodasse em toda fala no alcance, e hoje nao alcanca");

        System.out.println("\n----- MOTIVOS POR REGRA (o que SAI do escopo x o que FICA) -----");
        Map<String, Long> total = new LinkedHashMap<>();
        for (Obra o : obras) {
            o.motivosPorRegra().forEach((k, v) -> total.merge(k, v, Long::sum));
        }
        long soma = total.values().stream().mapToLong(Long::longValue).sum();
        System.out.printf("%-46s %10s %7s  %s%n", "REGRA (prefixo do motivo)", "motivos", "%", "veredito");
        for (Map.Entry<String, String> regra : REGRAS.entrySet()) {
            long n = total.getOrDefault(regra.getKey(), 0L);
            System.out.printf("%-46s %10d %6.1f%%  %s%n",
                corta(regra.getKey(), 46), n, soma == 0 ? 0.0 : (100.0 * n / soma), regra.getValue());
        }
        long naoClassificados = total.getOrDefault("(motivo NAO classificado)", 0L);
        if (naoClassificados > 0) {
            System.out.printf("%-46s %10d %6.1f%%  ATENCAO: prefixo desconhecido — o mapa REGRAS "
                + "esta desatualizado%n", "(motivo NAO classificado)", naoClassificados,
                100.0 * naoClassificados / soma);
        }
        System.out.printf("%-46s %10d%n", "TOTAL", soma);

        System.out.println("\n----- amostra do DELTA (deterministico fora da heuristica) -----");
        boolean houve = false;
        for (Obra o : desc) {
            if (o.amostraForaDaHeuristica().isEmpty()) {
                continue;
            }
            houve = true;
            System.out.println("\n  " + o.nome() + "  (" + o.corrigeForaDaHeuristica() + " falas)");
            for (String linha : o.amostraForaDaHeuristica()) {
                System.out.println("     " + linha);
            }
        }
        if (!houve) {
            System.out.println("  (nenhuma — o deterministico nunca age fora do que a heuristica sinaliza)");
        }

        if (!naoMedidas.isEmpty()) {
            System.out.println("\n----- NAO MEDIDAS (NAO sao zero) -----");
            naoMedidas.forEach(s -> System.out.println("  " + s));
        }
        System.out.println("\n=================================================\n");
    }

    private static String corta(String texto, int max) {
        return texto == null ? "" : (texto.length() > max ? texto.substring(0, max) : texto);
    }
}
