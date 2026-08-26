package org.traducao.projeto.raspagemRevisao.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.qualidadeTraducao.application.DetectorTraducaoIdenticaService;
import org.traducao.projeto.qualidadeTraducao.application.LoreAtivaFake;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: responder, com as classes de PRODUÇÃO, a única pergunta que importa no
 * fim — <b>o que ainda falta para a tradução do acervo estar pronta?</b>
 *
 * <h2>Por que este harness existe, e por que nasceu de um erro meu</h2>
 * A primeira tentativa de responder isso foi por regex à mão, em 22/08/2026, e deu dois números
 * absurdos: <b>6.157 "falas vazias"</b> (que eram linhas de efeito visual, sem texto por
 * desenho) e <b>2.460 "meta-respostas do LLM"</b> (que eram diálogo legítimo — "Sim, sim.",
 * "Claro!", "Desculpe."). Número grande demais é sinal de instrumento ingênuo, não de sistema
 * doente. Quem sabe o que é diálogo é o {@link FiltroAuditoriaLinha}; quem sabe o que é recusa
 * do modelo é o {@link ValidadorTraducaoService}. Este harness pergunta a eles.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só conta fala que o filtro da 3.1 AUDITA. Música, karaokê, conteúdo vetorial e efeito
 *       protegido ficam fora — eles não são trabalho desta frente.</li>
 *   <li>"Ainda em inglês" é decidido comparando com o par do CACHE, que é o mesmo espelho que a
 *       3.1 usa. Fala sem par no cache é contada à parte, nunca como aprovada.</li>
 *   <li>NÃO escreve nada. Lê e imprime.</li>
 *   <li>Travado por {@code -Dkronos.medicao=true}; acervo ausente REPROVA, não pula.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Arquivo ilegível é contado e reportado. O harness nunca reprova por conteúdo — quem julga o
 * número é quem lê.
 */
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoProntidaoTraducaoIT {

    private static final Path ACERVO = Path.of(System.getProperty("kronos.acervo", "C:\\animes"));
    private static final Path CACHE = Path.of(System.getProperty("kronos.cache", "cache"));
    private static final String PASTA_PT = "traducao_ptbr";

    /** Decisão de Paulo em 22/08/2026: "danmachi nao foi traduzido esquece ele". */
    private static final String FORA_DO_ACERVO = "DanMachi";

    /**
     * A lista {@code estilos-ignorados} do {@code application.yml}. É CONFIGURAÇÃO do operador,
     * não critério de código, e o pipeline a aplica no {@code SeletorEventosTraduziveis} — mas
     * sem ela aqui o número mente: na primeira medição, <b>825 "recusas do modelo"</b> eram
     * letra de música em camada palavra-a-palavra do estilo {@code "Mobile Suit Gundam"}, que a
     * produção veta e o harness não vetava.
     */
    private static final java.util.Set<String> ESTILOS_IGNORADOS = java.util.Set.of(
        "Song JP", "Mobile Suit Gundam", "Char's Counterattack", "OP - Romaji", "OP - English",
        "ED - Romaji", "ED - English", "ED-ROM", "OPL2");

    @Test
    @DisplayName("mede o que falta para a traducao do acervo estar pronta")
    void medeAProntidao() throws IOException {
        assertTrue(Files.isDirectory(ACERVO), "acervo inacessivel em " + ACERVO);

        FiltroAuditoriaLinha filtro = new FiltroAuditoriaLinha(
            new MascaradorTags(), new PoliticaEstiloMusical(List.of()),
            new DetectorEfeitoKaraokeService(), new ProtecaoLegendaAssService());
        ValidadorTraducaoService validador = new ValidadorTraducaoService(LoreAtivaFake.vazia());
        // QUEM DECIDE se a fala idêntica ao inglês é legítima é ele, e não eu: nome próprio,
        // interjeição e grito são publicados iguais DE PROPÓSITO. Na primeira medição, as
        // 2.156 "ainda em inglês" eram "Shin!", "Anju!", "Daiya...", "Kurena..." — tradução
        // correta contada como pendência.
        DetectorTraducaoIdenticaService detectorIdentica =
            new DetectorTraducaoIdenticaService(LoreAtivaFake.vazia());
        ProtecaoLegendaAssService protecao = new ProtecaoLegendaAssService();
        LeitorLegendaAss leitor = new LeitorLegendaAss();
        ObjectMapper mapper = new ObjectMapper();

        // EN de referência: o mesmo espelho que a 3.1 usa.
        Map<String, String> ptParaEn = new HashMap<>();
        try (Stream<Path> caches = Files.walk(CACHE)) {
            for (Path c : caches.filter(x -> x.getFileName().toString().endsWith(".cache.json"))
                    .toList()) {
                JsonNode raiz;
                try {
                    raiz = mapper.readTree(c.toFile());
                } catch (IOException e) {
                    continue;
                }
                for (JsonNode e : raiz.path("entradas")) {
                    String pt = e.path("traduzido").asText("");
                    String en = e.path("original").asText("");
                    if (!pt.isBlank() && !en.isBlank()) {
                        ptParaEn.putIfAbsent(normalizar(pt), en);
                    }
                }
            }
        }

        List<Path> arquivos = new ArrayList<>();
        // Alcance pelo DONO UNICO: honra -Dkronos.medicao.obra e declara NAO VERIFICADO
        // quando o filtro nao casa. O veto de FORA_DO_ACERVO continua sendo desta medicao.
        for (Path pastaPt : org.traducao.projeto.medicao.AlcanceDaMedicao.pastasDeTraducao()) {
            try (Stream<Path> naPasta = Files.list(pastaPt)) {
                naPasta.filter(Files::isRegularFile)
                    .filter(p -> !p.toString().contains(FORA_DO_ACERVO))
                    .filter(p -> {
                        String n = p.getFileName().toString().toLowerCase();
                        return n.endsWith(".ass") && !n.endsWith(".parcial.ass");
                    })
                    .forEach(arquivos::add);
            }
        }
        assertFalse(arquivos.isEmpty(), "instrumento cego: nenhum .ass sob " + ACERVO);

        int auditaveis = 0;
        int vazias = 0;
        int recusadas = 0;
        int aindaEmIngles = 0;
        int semPar = 0;
        int identicaLegitima = 0;
        int ilegiveis = 0;
        Map<String, Integer> inglesPorObra = new LinkedHashMap<>();
        List<String> amostraIngles = new ArrayList<>();
        List<String> amostraRecusa = new ArrayList<>();
        List<String> amostraVazia = new ArrayList<>();

        for (Path arquivo : arquivos) {
            List<EventoLegenda> eventos;
            try {
                eventos = leitor.ler(arquivo).eventos();
            } catch (RuntimeException e) {
                ilegiveis++;
                continue;
            }
            Path pastaObra = arquivo.getParent().getParent();
            String obra = pastaObra == null ? "?" : pastaObra.getFileName().toString();
            for (EventoLegenda evento : eventos) {
                if (filtro.deveIgnorarLinha(evento)
                    || ESTILOS_IGNORADOS.contains(evento.estilo())) {
                    continue;
                }
                auditaveis++;
                String texto = evento.texto();
                String visivel = protecao.textoVisivel(texto);
                if (visivel == null || visivel.isBlank()) {
                    vazias++;
                    if (amostraVazia.size() < 5) {
                        amostraVazia.add(obra.substring(0, Math.min(18, obra.length()))
                            + " | " + recorte(texto));
                    }
                    continue;
                }
                try {
                    validador.validarFala(texto);
                } catch (AlucinacaoDetectadaException e) {
                    recusadas++;
                    if (amostraRecusa.size() < 8) {
                        amostraRecusa.add(obra.substring(0, Math.min(18, obra.length()))
                            + " | " + recorte(visivel) + "  <- " + e.getMessage());
                    }
                    continue;
                }
                String en = ptParaEn.get(normalizar(texto));
                if (en == null) {
                    semPar++;
                } else if (normalizar(en).equals(normalizar(texto))) {
                    if (detectorIdentica.deveManterIdentico(texto)) {
                        identicaLegitima++;
                        continue;
                    }
                    aindaEmIngles++;
                    inglesPorObra.merge(obra, 1, Integer::sum);
                    if (amostraIngles.size() < 10) {
                        amostraIngles.add(obra.substring(0, Math.min(18, obra.length()))
                            + " | " + recorte(visivel));
                    }
                }
            }
        }

        System.out.println("=== PRONTIDAO DA TRADUCAO (DanMachi fora, decisao do Paulo) ===");
        System.out.println("arquivos lidos          : " + arquivos.size()
            + (ilegiveis > 0 ? "  (" + ilegiveis + " ILEGIVEIS)" : ""));
        System.out.println("falas AUDITAVEIS        : " + auditaveis
            + "   (o filtro da 3.1 ja tirou musica, karaoke, vetorial e efeito)");
        System.out.println("  texto visivel VAZIO   : " + vazias);
        System.out.println("  RECUSA/meta do modelo : " + recusadas);
        System.out.println("  ainda EM INGLES       : " + aindaEmIngles);
        System.out.println("  identica LEGITIMA     : " + identicaLegitima
            + "   (nome proprio, interjeicao e grito — publicados iguais de proposito)");
        System.out.println("  sem par no cache      : " + semPar
            + "   (nao da para afirmar nada sobre estas)");
        imprimir("ainda em ingles, por obra", inglesPorObra);
        amostra("ainda em ingles", amostraIngles);
        amostra("recusa/meta", amostraRecusa);
        amostra("visivel vazio", amostraVazia);
    }

    private static void imprimir(String titulo, Map<String, Integer> mapa) {
        System.out.println("--- " + titulo + ":");
        if (mapa.isEmpty()) {
            System.out.println("   (nenhum)");
            return;
        }
        mapa.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .forEach(e -> System.out.println("   " + e.getValue() + "  " + e.getKey()));
    }

    private static void amostra(String titulo, List<String> itens) {
        if (itens.isEmpty()) {
            return;
        }
        System.out.println("--- amostra de " + titulo + ":");
        itens.forEach(i -> System.out.println("   " + i));
    }

    private static String normalizar(String texto) {
        return texto == null ? "" : texto.replaceAll("\\s+", " ").trim();
    }

    private static String recorte(String texto) {
        String uma = texto.replace("\\N", " ").replaceAll("\\s+", " ").trim();
        return uma.length() <= 80 ? uma : uma.substring(0, 80) + "...";
    }
}
