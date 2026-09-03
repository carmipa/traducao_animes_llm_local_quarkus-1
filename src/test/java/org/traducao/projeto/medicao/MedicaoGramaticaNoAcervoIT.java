package org.traducao.projeto.medicao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.core.texto.gramatica.AchadoGramatical;
import org.traducao.projeto.core.texto.gramatica.LanguageToolRevisorAdapter;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.raspagemRevisao.application.FiltroAuditoriaLinha;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: medir a GRAMÁTICA da legenda entregue, que é a classe de defeito que
 * Paulo viu assistindo e que nenhum instrumento meu enxergava.
 *
 * <h2>O prejuízo que originou, em 03/09/2026</h2>
 * Paulo assistiu ao ZZ e disse: <i>"a tradução do ZZ tá toda emporcalhada, com idiomas
 * misturados, fora erros grotescos de concordância, como se substantivo e adjetivo do inglês não
 * tivessem sido adaptados"</i>. No mesmo acervo, o meu detector de concordância acusava <b>8
 * falas em 74.427 pares</b> — 0,01%.
 *
 * <p>Os dois números estavam certos e mediam coisas diferentes.
 * {@code DetectorConcordanciaService} pergunta apenas <i>"o gênero da PESSOA bate com o do
 * original inglês?"</i> — ele/ela, senhor/senhora, irmão/irmã. É cego para
 * {@code "diga aos gente da Argama"}, que é concordância de artigo com substantivo e não tem
 * nada a ver com o inglês. Instrumento adequado à classe de defeito: para gramática de
 * português, quem sabe é o LanguageTool, que o projeto já tem.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só mede fala que o filtro da 3.1 AUDITA, e sobre o TEXTO VISÍVEL — tag de override não é
 *       português e envenenaria toda regra.</li>
 *   <li>Usa {@link LanguageToolRevisorAdapter} de produção, com as categorias que o projeto já
 *       decidiu ligar. Não inventa régua nova.</li>
 *   <li>Motor indisponível REPROVA. Resultado vazio por motor ausente não pode ser lido como
 *       "a gramática está boa" — é a diferença entre {@code 0} e {@code NÃO VERIFICADO}.</li>
 *   <li>NÃO escreve nada. Lê e imprime, com amostra por regra para julgamento humano.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Arquivo ilegível é contado e reportado. O harness nunca reprova por conteúdo — quem julga a
 * gravidade é quem lê.
 */
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoGramaticaNoAcervoIT {

    /** Configuração do operador, que o pipeline aplica e o harness precisa espelhar. */
    private static final Set<String> ESTILOS_IGNORADOS = Set.of(
        "Song JP", "Mobile Suit Gundam", "Char's Counterattack", "OP - Romaji", "OP - English",
        "ED - Romaji", "ED - English", "ED-ROM", "OPL2");

    private static final int AMOSTRAS_POR_REGRA = 4;

    @Test
    @DisplayName("mede a gramatica das falas entregues, por obra e por regra")
    void medeAGramatica() throws IOException {
        LanguageToolRevisorAdapter revisor = new LanguageToolRevisorAdapter();
        assertTrue(revisor.disponivel(),
            () -> "LanguageTool indisponivel (" + revisor.motivoDaIndisponibilidade()
                + "): sem ele, zero achado significaria NAO VERIFICADO, nunca 'gramatica boa'");

        // MOTOR PROPRIO com a ORTOGRAFIA LIGADA, ao lado do de producao. A producao desliga
        // MORFOLOGIK_RULE_PT_BR porque ela acusa nome de lore (117 de 158 numa medicao de
        // 23/08). Aqui nada e escrito em legenda nenhuma: o que se mede e o ACERVO, e sem esta
        // regra os defeitos que Paulo viu assistindo ao ZZ passam ilesos —
        //   "e motivo de preocupacao"  (falta o acento em "e")
        //   "essa armadura esta quente" (falta em "esta")
        //   "fazendo a mao"             (falta o til)
        // Nome proprio entra na conta e esta DECLARADO na saida: e o preco de nao ter dicionario
        // de lore aqui, e o numero e teto, nao veredito.
        org.languagetool.JLanguageTool motorOrtografia = null;
        try {
            motorOrtografia = new org.languagetool.JLanguageTool(
                new org.languagetool.language.BrazilianPortuguese());
        } catch (RuntimeException e) {
            System.out.println("NAO VERIFICADO: motor de ortografia nao subiu (" + e + ")");
        }

        FiltroAuditoriaLinha filtro = new FiltroAuditoriaLinha(
            new MascaradorTags(), new PoliticaEstiloMusical(List.of()),
            new DetectorEfeitoKaraokeService(), new ProtecaoLegendaAssService());
        ProtecaoLegendaAssService protecao = new ProtecaoLegendaAssService();
        LeitorLegendaAss leitor = new LeitorLegendaAss();

        org.traducao.projeto.lore.infrastructure.CatalogoLoreYaml catalogo =
            new org.traducao.projeto.lore.infrastructure.CatalogoLoreYaml();
        List<Path> pastas = AlcanceDaMedicao.pastasDeTraducao();
        assertFalse(pastas.isEmpty(), "instrumento cego: nenhuma pasta de traducao");

        // [falas, falasComGramatica, falasComOrtografia]
        Map<String, int[]> porObra = new LinkedHashMap<>();
        Map<String, Integer> porRegra = new TreeMap<>();
        Map<String, List<String>> amostra = new LinkedHashMap<>();
        Map<String, Integer> porOrtografia = new TreeMap<>();
        Map<String, List<String>> amostraOrtografia = new LinkedHashMap<>();
        int ilegiveis = 0;
        java.util.Set<String> semLore = new java.util.LinkedHashSet<>();

        for (Path pasta : pastas) {
            String obra = AlcanceDaMedicao.obraDe(pasta);
            // TERMOS DA LORE FORA DA CONTA. As 22 palavras mais acusadas na primeira
            // versao eram Judau, Argama, Gundam, Zeon, Banagher, Quattro, Newtype —
            // nome de lore, nao erro. E o mesmo tratamento que o
            // ValidadorTraducaoService de producao da: remove os termos ANTES de
            // checar. Obra sem lore casada fica DECLARADA na saida, nunca contada
            // como se estivesse limpa.
            Set<String> termosDaLore = termosDaObra(catalogo, obra);
            if (termosDaLore.isEmpty()) {
                semLore.add(obra);
            }
            int[] contagem = porObra.computeIfAbsent(obra, k -> new int[3]);

            for (Path arquivo : AlcanceDaMedicao.arquivosEntregues(pasta)) {
                List<EventoLegenda> eventos;
                try {
                    eventos = leitor.ler(arquivo).eventos();
                } catch (RuntimeException e) {
                    ilegiveis++;
                    continue;
                }
                for (EventoLegenda evento : eventos) {
                    if (filtro.deveIgnorarLinha(evento)
                        || ESTILOS_IGNORADOS.contains(evento.estilo())) {
                        continue;
                    }
                    String visivel = protecao.textoVisivel(evento.texto());
                    if (visivel == null || visivel.isBlank()) {
                        continue;
                    }
                    contagem[0]++;
                    String semLoreTexto = semOsTermos(visivel, termosDaLore);
                    if (motorOrtografia != null) {
                        try {
                            for (org.languagetool.rules.RuleMatch m
                                    : motorOrtografia.check(semLoreTexto)) {
                                if (!LanguageToolRevisorAdapter.REGRA_ORTOGRAFICA
                                        .equals(m.getRule().getId())) {
                                    continue;
                                }
                                contagem[2]++;
                                String palavra = semLoreTexto.substring(
                                    Math.max(0, m.getFromPos()),
                                    Math.min(semLoreTexto.length(), m.getToPos()));
                                porOrtografia.merge(palavra, 1, Integer::sum);
                                List<String> ex = amostraOrtografia
                                    .computeIfAbsent(palavra, k -> new ArrayList<>());
                                if (ex.size() < 2) {
                                    ex.add(recorte(visivel));
                                }
                                break; // uma fala conta uma vez
                            }
                        } catch (Exception e) {
                            // fala que o motor nao processa nao invalida a medicao inteira
                        }
                    }
                    List<AchadoGramatical> achados = revisor.revisar(visivel);
                    if (achados.isEmpty()) {
                        continue;
                    }
                    contagem[1]++;
                    for (AchadoGramatical a : achados) {
                        porRegra.merge(a.regra(), 1, Integer::sum);
                        List<String> exemplos =
                            amostra.computeIfAbsent(a.regra(), k -> new ArrayList<>());
                        if (exemplos.size() < AMOSTRAS_POR_REGRA) {
                            exemplos.add(recorte(visivel) + "   <<" + a.trecho() + ">>");
                        }
                    }
                }
            }
        }

        System.out.println("=== GRAMATICA DAS FALAS ENTREGUES (LanguageTool de producao) ===");
        System.out.println("raiz: " + AlcanceDaMedicao.RAIZ
            + (AlcanceDaMedicao.FILTRO_OBRA.isBlank() ? "" :
                "  | filtro de obra: " + AlcanceDaMedicao.FILTRO_OBRA)
            + "  | pastas: " + pastas.size());
        if (ilegiveis > 0) {
            System.out.println("arquivos ILEGIVEIS: " + ilegiveis);
        }
        System.out.println();
        System.out.println(String.format("%-42s %7s %7s %6s %7s %6s",
            "obra", "falas", "gram.", "taxa", "ortogr.", "taxa"));
        porObra.entrySet().stream()
            .sorted((a, b) -> Double.compare(taxa(b.getValue()[0], b.getValue()[2]),
                taxa(a.getValue()[0], a.getValue()[2])))
            .forEach(e -> System.out.println(String.format("%-42s %7d %7d %5.1f%% %7d %5.1f%%",
                corta(e.getKey(), 42), e.getValue()[0], e.getValue()[1],
                taxa(e.getValue()[0], e.getValue()[1]), e.getValue()[2],
                taxa(e.getValue()[0], e.getValue()[2]))));
        System.out.println();
        if (!semLore.isEmpty()) {
            System.out.println("ATENCAO — obras SEM lore casada (numero de ortografia "
                + "inflado por nome proprio nelas): " + semLore);
        }
        System.out.println();
        System.out.println("--- ORTOGRAFIA: as 22 palavras mais frequentes "
            + "(inclui nome proprio, que nao tem dicionario aqui):");
        porOrtografia.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(22)
            .forEach(e -> System.out.println("   " + e.getValue() + "x  " + e.getKey()
                + "   |  " + String.join(" / ",
                    amostraOrtografia.getOrDefault(e.getKey(), List.of()))));
        System.out.println();
        System.out.println("--- por regra (as 18 mais frequentes):");
        porRegra.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(18)
            .forEach(e -> {
                System.out.println("### " + e.getValue() + "x  " + e.getKey());
                amostra.getOrDefault(e.getKey(), List.of())
                    .forEach(s -> System.out.println("      " + s));
            });
    }

    /** Os termos protegidos da obra, casando a pasta pela {@code apelidosPasta} da lore. */
    private static Set<String> termosDaObra(
        org.traducao.projeto.lore.infrastructure.CatalogoLoreYaml catalogo, String nomePasta) {
        String alvo = nomePasta.toLowerCase(Locale.ROOT);
        Set<String> melhor = Set.of();
        int tamanho = 0;
        for (org.traducao.projeto.lore.domain.ProvedorContexto o : catalogo.obras()) {
            for (String apelido : o.apelidosPasta()) {
                String a = apelido.toLowerCase(Locale.ROOT);
                if (!a.isBlank() && alvo.contains(a) && a.length() > tamanho) {
                    melhor = o.termosProtegidos();
                    tamanho = a.length();
                }
            }
        }
        return melhor;
    }

    /**
     * Apaga do texto os termos da lore, para o corretor ortografico nao os ver.
     * <p>Frases longas primeiro: "Mobile Suit" antes de "Suit", senao a troca curta come a longa
     * e sobra "Mobile" solto para ser acusado.
     */
    private static String semOsTermos(String texto, Set<String> termos) {
        if (termos.isEmpty()) {
            return texto;
        }
        String saida = texto;
        List<String> ordenados = new ArrayList<>(termos);
        ordenados.sort((a, b) -> Integer.compare(b.length(), a.length()));
        for (String termo : ordenados) {
            if (termo == null || termo.isBlank()) {
                continue;
            }
            saida = java.util.regex.Pattern
                .compile("(?iu)(?<![\\p{L}])" + java.util.regex.Pattern.quote(termo)
                    + "(?![\\p{L}])")
                .matcher(saida).replaceAll("X");
        }
        return saida;
    }
    private static double taxa(int total, int parte) {
        return total == 0 ? 0 : 100.0 * parte / total;
    }

    private static String corta(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n);
    }

    private static String recorte(String texto) {
        String uma = texto.replaceAll("\\s+", " ").trim();
        return uma.length() <= 76 ? uma : uma.substring(0, 76) + "...";
    }

    static {
        Locale.setDefault(Locale.forLanguageTag("pt-BR"));
    }
}
