package org.traducao.projeto.medicao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.core.texto.gramatica.LanguageToolRevisorAdapter;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.lore.domain.ProvedorContexto;
import org.traducao.projeto.lore.infrastructure.CatalogoLoreYaml;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.raspagemRevisao.application.FiltroAuditoriaLinha;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: levantar os nomes de lore que a legenda USA e a lore NÃO declara —
 * o primeiro nome de um termo composto ({@code "Judau"} de {@code "Judau Ashta"}) e o plural de
 * um termo simples ({@code "Newtypes"} de {@code "Newtype"}).
 *
 * <h2>O prejuízo que originou, em 03/09/2026</h2>
 * Paulo assistiu ao ZZ e disse que a tradução estava "toda emporcalhada". Medindo com o
 * corretor ortográfico ligado, <b>18,9% das falas do ZZ</b> tinham acusação — e as 22 palavras
 * mais acusadas eram {@code Judau}, {@code Fa}, {@code Roux}, {@code Beecha}, {@code Bright},
 * {@code Glemy}, {@code Elle}, {@code Iino}, {@code Mashymre}. Todas de lore.
 *
 * <p>A lore do {@code gundam_zz} tem 83 termos protegidos e <b>todos são compostos</b>:
 * {@code "Judau Ashta"}, {@code "Fa Yuiry"}, {@code "Roux Louka"}. Nenhum primeiro nome está
 * solto — e é pelo primeiro nome que personagem se chama em diálogo. É a mesma lacuna do
 * {@code "Gottn"} (a lore tinha só {@code "Gottn Goh"}), achada em 22/08 numa fala só.
 *
 * <p>O dano não é o número na medição: é que nome fora da lore fica exposto ao corretor
 * ortográfico e ao de acento, que já quebrou termo de lore antes ({@code Virus} virando
 * {@code Vírus}).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só propõe nome que APARECE nas falas entregues. Termo que ninguém usa não vira ruído.</li>
 *   <li>Só propõe nome que o corretor ORTOGRÁFICO acusa — ou seja, que não é palavra portuguesa.
 *       É a guarda contra proteger palavra comum, que é o dano que a lore existe para evitar:
 *       {@code "Sol"} de um {@code "Sol Silva"} jamais pode entrar.</li>
 *   <li>NÃO escreve nada. Lê e imprime a proposta para julgamento humano.</li>
 *   <li>Motor ortográfico indisponível REPROVA: sem ele a segunda condição não existe e a
 *       proposta viraria uma lista de palavras comuns.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Obra sem lore casada é reportada à parte, nunca contada como se não tivesse lacuna.
 */
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoNomeCompostoSemPrimeiroNomeIT {

    private static final Set<String> ESTILOS_IGNORADOS = Set.of(
        "Song JP", "Mobile Suit Gundam", "Char's Counterattack", "OP - Romaji", "OP - English",
        "ED - Romaji", "ED - English", "ED-ROM", "OPL2");

    /** Palavra que começa com maiúscula: candidata a nome. */
    private static final Pattern PALAVRA = Pattern.compile("\\b(\\p{Lu}[\\p{L}'-]{1,})\\b");

    @Test
    @DisplayName("levanta os nomes de lore que a legenda usa e a lore nao declara")
    void levantaNomesAusentes() throws IOException {
        org.languagetool.JLanguageTool motor;
        try {
            motor = new org.languagetool.JLanguageTool(
                new org.languagetool.language.BrazilianPortuguese());
        } catch (RuntimeException e) {
            throw new AssertionError("motor ortografico nao subiu: sem ele a proposta viraria "
                + "lista de palavra comum — " + e, e);
        }
        assertTrue(motor.getAllActiveRules().size() > 100, "motor sem regras: nao verificado");

        CatalogoLoreYaml catalogo = new CatalogoLoreYaml();
        FiltroAuditoriaLinha filtro = new FiltroAuditoriaLinha(
            new MascaradorTags(), new PoliticaEstiloMusical(List.of()),
            new DetectorEfeitoKaraokeService(), new ProtecaoLegendaAssService());
        ProtecaoLegendaAssService protecao = new ProtecaoLegendaAssService();
        LeitorLegendaAss leitor = new LeitorLegendaAss();

        List<Path> pastas = AlcanceDaMedicao.pastasDeTraducao();
        assertFalse(pastas.isEmpty(), "instrumento cego: nenhuma pasta de traducao");

        Map<String, Map<String, Integer>> propostaPorObra = new LinkedHashMap<>();
        Set<String> semLore = new LinkedHashSet<>();
        Map<String, Boolean> ehPalavraPt = new java.util.HashMap<>();

        for (Path pasta : pastas) {
            String nomePasta = AlcanceDaMedicao.obraDe(pasta);
            ProvedorContexto obra = casar(catalogo, nomePasta);
            if (obra == null) {
                semLore.add(nomePasta);
                continue;
            }
            Set<String> declarados = new LinkedHashSet<>();
            for (String t : obra.termosProtegidos()) {
                declarados.add(t.toLowerCase(Locale.ROOT));
            }
            // candidatos: primeiro nome de composto, e plural de simples
            Set<String> candidatos = new LinkedHashSet<>();
            for (String termo : obra.termosProtegidos()) {
                String[] partes = termo.trim().split("\\s+");
                if (partes.length > 1 && partes[0].length() >= 2
                    && !declarados.contains(partes[0].toLowerCase(Locale.ROOT))) {
                    candidatos.add(partes[0]);
                }
                if (partes.length == 1 && !declarados.contains(
                        (termo + "s").toLowerCase(Locale.ROOT))) {
                    candidatos.add(termo + "s");
                }
            }
            if (candidatos.isEmpty()) {
                continue;
            }

            Map<String, Integer> usados = new TreeMap<>();
            for (Path arquivo : AlcanceDaMedicao.arquivosEntregues(pasta)) {
                List<EventoLegenda> eventos;
                try {
                    eventos = leitor.ler(arquivo).eventos();
                } catch (RuntimeException e) {
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
                    Matcher m = PALAVRA.matcher(visivel);
                    while (m.find()) {
                        String palavra = m.group(1);
                        if (candidatos.contains(palavra)) {
                            usados.merge(palavra, 1, Integer::sum);
                        }
                    }
                }
            }
            if (usados.isEmpty()) {
                continue;
            }
            Map<String, Integer> proposta = new TreeMap<>();
            for (Map.Entry<String, Integer> e : usados.entrySet()) {
                Boolean pt = ehPalavraPt.computeIfAbsent(e.getKey(),
                    palavra -> ehPortugues(motor, palavra));
                if (Boolean.FALSE.equals(pt)) {
                    proposta.put(e.getKey(), e.getValue());
                }
            }
            if (!proposta.isEmpty()) {
                propostaPorObra.put(obra.getId(), proposta);
            }
        }

        System.out.println("=== NOMES DE LORE QUE A LEGENDA USA E A LORE NAO DECLARA ===");
        System.out.println("raiz: " + AlcanceDaMedicao.RAIZ + "  | pastas: " + pastas.size());
        if (!semLore.isEmpty()) {
            System.out.println("SEM lore casada (nao medidas): " + semLore);
        }
        System.out.println();
        int total = 0;
        for (Map.Entry<String, Map<String, Integer>> e : propostaPorObra.entrySet()) {
            System.out.println("### " + e.getKey() + "  (" + e.getValue().size() + " nomes)");
            List<Map.Entry<String, Integer>> ordenado = new ArrayList<>(e.getValue().entrySet());
            ordenado.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            StringBuilder linha = new StringBuilder("   ");
            for (Map.Entry<String, Integer> n : ordenado) {
                linha.append(n.getKey()).append('(').append(n.getValue()).append(") ");
                total++;
                if (linha.length() > 96) {
                    System.out.println(linha);
                    linha = new StringBuilder("   ");
                }
            }
            if (linha.length() > 3) {
                System.out.println(linha);
            }
        }
        System.out.println();
        System.out.println("TOTAL de nomes propostos: " + total
            + "  (usados na legenda E recusados pelo corretor ortografico)");
    }

    /** Palavra que o corretor ortográfico ACEITA é portuguesa — e não pode entrar na lore. */
    private static boolean ehPortugues(org.languagetool.JLanguageTool motor, String palavra) {
        try {
            for (org.languagetool.rules.RuleMatch m
                    : motor.check("Eu vi " + palavra + " ontem.")) {
                if (LanguageToolRevisorAdapter.REGRA_ORTOGRAFICA.equals(m.getRule().getId())) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return true; // na duvida NAO propoe: falha fechada
        }
    }

    private static ProvedorContexto casar(CatalogoLoreYaml catalogo, String nomePasta) {
        String alvo = nomePasta.toLowerCase(Locale.ROOT);
        ProvedorContexto melhor = null;
        int tamanho = 0;
        for (ProvedorContexto o : catalogo.obras()) {
            for (String apelido : o.apelidosPasta()) {
                String a = apelido.toLowerCase(Locale.ROOT);
                if (!a.isBlank() && alvo.contains(a) && a.length() > tamanho) {
                    melhor = o;
                    tamanho = a.length();
                }
            }
        }
        return melhor;
    }
}
