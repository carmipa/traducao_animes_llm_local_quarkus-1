package org.traducao.projeto.remuxer.domain;

import java.util.List;
import java.util.Locale;

/**
 * PROPÓSITO DE NEGÓCIO: dono único, dentro da fatia do Remuxer, da pergunta
 * "onde estão as legendas já traduzidas desta obra?". Existe para que a tela e o
 * CLI respondam a mesma coisa.
 *
 * <p>INVARIANTES DO DOMÍNIO: {@link #PADRAO} é o nome que o pipeline REALMENTE
 * produz — medido no acervo em 2026-09-02: <b>20 pastas {@code traducao_ptbr} e
 * ZERO</b> das que a auto-detecção da web procurava até então
 * ({@code legendas pt}, {@code legendas ptbr}, {@code legendas portugues}).
 * A comparação ignora caixa, espaço, hífen e underscore, porque a mesma pasta
 * aparece no acervo como {@code traducao_ptbr} e {@code legenda_ptbr}.
 *
 * <p>Por que a lista mora AQUI e não é importada de {@code traducao}: a fronteira
 * da fatia proíbe o Remuxer depender da Tradução Local, e a duplicação
 * consciente do nome já era autorizada na subfase E4b. O que NÃO era autorizado
 * era existirem <b>duas</b> respostas divergentes dentro da própria fatia — o
 * CLI caindo em {@code traducao_ptbr} e a web procurando três nomes que não
 * existem. Esta classe fecha esse buraco.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: nome nulo nunca casa; a lista é imutável e
 * a ausência de correspondência devolve {@code false}, fazendo a borda pedir o
 * caminho explícito em vez de adivinhar.
 */
public final class PastaDeLegendaDoRemux {

    /**
     * O nome canônico produzido pela Tradução Local. É também o fallback do
     * {@code RemuxerCLI}, congelado pelo gate de paridade da subfase E4b.
     */
    public static final String PADRAO = "traducao_ptbr";

    /**
     * Nomes aceitos pela auto-detecção, em ordem de preferência. O primeiro é o
     * que o pipeline gera; os demais existem no acervo por histórico ou vieram
     * do contrato antigo da tela e continuam aceitos para não quebrar quem já os
     * usava.
     */
    private static final List<String> ACEITOS = List.of(
        "traducaoptbr", "legendaptbr", "legendasptbr", "legendaspt", "legendasportugues");

    private PastaDeLegendaDoRemux() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se um diretório encontrado ao lado dos vídeos
     * é candidato a conter as legendas traduzidas.
     *
     * <p>INVARIANTES DO DOMÍNIO: normaliza caixa e os três separadores usados no
     * acervo antes de comparar; pastas de experimento
     * ({@code traducao_mistral}, {@code traducao_aya},
     * {@code traducao_ptbr_sem_lore}) NÃO entram — escolher a baseline errada de
     * um confronto de modelos é engano de boa-fé caro, e aqui o operador informa
     * o caminho explicitamente.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nome nulo ou vazio devolve
     * {@code false}.
     */
    public static boolean ehCandidata(String nomeDaPasta) {
        if (nomeDaPasta == null || nomeDaPasta.isBlank()) return false;
        String normalizado = nomeDaPasta.toLowerCase(Locale.ROOT)
            .replace("-", "").replace("_", "").replace(" ", "");
        return ACEITOS.contains(normalizado);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: ordena as candidatas encontradas para que
     * {@code traducao_ptbr} vença qualquer sobra histórica na mesma obra.
     *
     * <p>INVARIANTES DO DOMÍNIO: posição na lista {@link #ACEITOS} é a régua;
     * nome desconhecido vai para o fim.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nome nulo recebe a pior posição.
     */
    public static int preferencia(String nomeDaPasta) {
        if (nomeDaPasta == null) return Integer.MAX_VALUE;
        String normalizado = nomeDaPasta.toLowerCase(Locale.ROOT)
            .replace("-", "").replace("_", "").replace(" ", "");
        int indice = ACEITOS.indexOf(normalizado);
        return indice < 0 ? Integer.MAX_VALUE : indice;
    }
}
