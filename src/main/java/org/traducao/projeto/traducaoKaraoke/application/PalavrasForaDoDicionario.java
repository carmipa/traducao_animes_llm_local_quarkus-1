package org.traducao.projeto.traducaoKaraoke.application;

import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;
import org.traducao.projeto.core.texto.dicionarioOrtografia.VeredictoPalavra;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: aponta, em cada letra de karaokê que vai para o arquivo, a palavra que
 * nenhum dicionário reconhece e que não veio do inglês — a invenção do modelo, que sai em português
 * fluente em volta dela e por isso passa por toda outra guarda. Casos reais: {@code "I'd act on all
 * of my feelings" => "Açãoaria em todos os meus sentimentos."} (Break Blade 1, teste ponta a ponta
 * de 09/10/2026), {@code "Anjel, venha e leve-me pela mão."} e {@code "sonhos que se desfezera"}
 * (0083) e {@code "Cantando Alchemia, Alchemia."} (86 Part 2).
 *
 * <h2>Por que REGISTRA e não refaz — medido, não suposto (regra A8)</h2>
 * <ul>
 *   <li>Custo do bloqueio, no cache real do karaokê em 09/10/2026: 11.561 letras traduzidas, 1.178
 *       formas que não vêm do inglês, 3 desconhecidas — {@code Anjel}, {@code desfezera},
 *       {@code Alchemia}, as 3 defeito de verdade (0 falso positivo em 3). {@code Möbius} sai como
 *       alemão e não é acusado. Falso negativo no universo que o dicionário aceita: não medido.</li>
 *   <li>Critério declarado antes do ensaio: refazer só se um segundo tiro determinístico
 *       consertasse ao menos 3 dos 4 casos. No modelo real ({@code aya-expanse-8b}, caminho de
 *       produção) a pontuação trocada consertou 1 de 4; reticências, 1 de 4; e {@code desfezera}
 *       virou {@code desfezeram}, que também não existe. Recusar a linha a devolveria ao INGLÊS —
 *       a troca de defeito por inglês que este projeto já pagou. Então a tradução segue, e a
 *       palavra vai ao manifesto para revisão.</li>
 * </ul>
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Palavra presente no original (ignorando caixa) nunca é acusada: nome próprio, romaji e
 *       termo mantido não são invenção.</li>
 *   <li>Uma consulta ao dicionário por ARQUIVO, com todas as palavras de todas as letras — o custo
 *       do hunspell é o arranque do processo.</li>
 *   <li>Só {@link VeredictoPalavra#DESCONHECIDA} acusa. Inglês, alemão, francês, espanhol e romaji
 *       têm guarda própria ou são legítimos na letra.</li>
 *   <li>Não altera texto nenhum: devolve o achado.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem dicionário injetado ou com ele fora do ar, devolve {@code verificado = false} e nenhum achado
 * — NÃO VERIFICADO, que quem chama imprime como tal, nunca como "nenhuma palavra inventada".
 */
final class PalavrasForaDoDicionario {

    /**
     * O resultado da conferência de um arquivo.
     *
     * @param verificado se o dicionário respondeu — {@code false} é o estado 2, não aprovação
     * @param porLetra   original da letra -> palavras desconhecidas da tradução, na ordem
     */
    record Verificacao(boolean verificado, Map<String, List<String>> porLetra) {

        static Verificacao naoVerificada() {
            return new Verificacao(false, Map.of());
        }
    }

    private PalavrasForaDoDicionario() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: confere todas as letras traduzidas de um arquivo de uma vez.
     *
     * <p>INVARIANTES DO DOMÍNIO: a comparação é sobre o texto VISÍVEL dos dois lados
     * ({@link ClassificadorLetraKaraokeService#extrairTextoVisivel}); a palavra é a do dono único
     * {@link CorretorOrtograficoLegenda#palavrasDe}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: dicionário nulo ou indisponível, ou qualquer palavra sem
     * veredicto, devolve {@link Verificacao#naoVerificada()}. Nunca lança.
     *
     * @param dicionario o corretor de produção, ou {@code null} em teste de unidade
     * @param traduzidas original da letra -> tradução que vai para o arquivo
     */
    static Verificacao verificar(CorretorOrtograficoLegenda dicionario, Map<String, String> traduzidas) {
        if (dicionario == null || traduzidas == null) {
            return Verificacao.naoVerificada();
        }
        Map<String, List<String>> novasPorLetra = new LinkedHashMap<>();
        Set<String> todas = new LinkedHashSet<>();
        for (Map.Entry<String, String> e : traduzidas.entrySet()) {
            List<String> novas = palavrasQueNaoVemDoOriginal(e.getKey(), e.getValue());
            if (!novas.isEmpty()) {
                novasPorLetra.put(e.getKey(), novas);
                todas.addAll(novas);
            }
        }
        if (todas.isEmpty()) {
            return new Verificacao(true, Map.of());
        }
        Map<String, VeredictoPalavra> veredicto;
        try {
            veredicto = dicionario.classificarPalavras(todas);
        } catch (RuntimeException e) {
            return Verificacao.naoVerificada();
        }
        if (!dicionario.disponivel() || veredicto == null) {
            return Verificacao.naoVerificada();
        }
        Map<String, List<String>> achados = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : novasPorLetra.entrySet()) {
            List<String> desconhecidas = new ArrayList<>();
            for (String p : e.getValue()) {
                VeredictoPalavra v = veredicto.get(p);
                if (v == null || v == VeredictoPalavra.NAO_VERIFICADO) {
                    return Verificacao.naoVerificada();
                }
                if (v == VeredictoPalavra.DESCONHECIDA && !desconhecidas.contains(p)) {
                    desconhecidas.add(p);
                }
            }
            if (!desconhecidas.isEmpty()) {
                achados.put(e.getKey(), List.copyOf(desconhecidas));
            }
        }
        return new Verificacao(true, achados);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: as palavras da tradução que não estão no original.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nulos viram texto vazio.
     */
    private static List<String> palavrasQueNaoVemDoOriginal(String original, String traduzido) {
        Set<String> doOriginal = new LinkedHashSet<>();
        for (String p : CorretorOrtograficoLegenda.palavrasDe(
                ClassificadorLetraKaraokeService.extrairTextoVisivel(original == null ? "" : original))) {
            doOriginal.add(p.toLowerCase(Locale.ROOT));
        }
        List<String> novas = new ArrayList<>();
        for (String p : CorretorOrtograficoLegenda.palavrasDe(
                ClassificadorLetraKaraokeService.extrairTextoVisivel(traduzido == null ? "" : traduzido))) {
            if (!doOriginal.contains(p.toLowerCase(Locale.ROOT))) {
                novas.add(p);
            }
        }
        return novas;
    }
}
