package org.traducao.projeto.core.texto.dicionarioOrtografia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: garante que o {@code ão} destruído pelo modelo volte a ser
 * {@code ão} — e que a correção nunca invente palavra.
 *
 * <h2>O prejuízo que originou</h2>
 * Tradução do 86 Part 1 em 2026-08-15, DEPOIS de a lore ser unificada e o termo
 * {@code Spearhead} passar a ser restaurado com sucesso. A legenda saiu assim:
 * <pre>
 *   ep01  Como comandante do Esquadroo\NSpearhead, farei o meu melhor.
 *   ep02  Este é o comandante do Esquadroao Spearhead.
 *   ep07  ...substitutos para o Esquadroa Spearhead.
 * </pre>
 * O cache prova que quem escreveu foi o MODELO, e o mesmo modelo grafa {@code Esquadrão}
 * corretamente em outras falas. Dez ocorrências em onze episódios. Não é termo de lore: é
 * palavra comum, e nenhum glossário deveria precisar conhecê-la.
 *
 * <p>Medido no acervo do 86 antes de escrever a regra: 4.012 palavras distintas na saída, 9
 * candidatas pelo padrão, 7 rejeitadas pelo dicionário — <b>6 viram palavra válida</b> e
 * <b>1 não</b> ({@code Npessoa} → {@code Npessão}), que é justamente o falso positivo que o
 * dicionário barra sozinho.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O dicionário valida NAS DUAS PONTAS: só entra o que ele rejeita, só sai o que ele
 *       aceita.</li>
 *   <li>Palavra legítima terminada em {@code -oa}/{@code -oo} ({@code pessoa}, {@code lagoa})
 *       nunca é candidata, porque o dicionário a conhece.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Reprovar no caso doente devolve {@code Esquadroo} à legenda. Reprovar num caso-controle é
 * pior: significa que o corretor passou a inventar palavra, que é o dano que a dupla trava
 * existe para impedir.
 */
@DisplayName("reparo de terminação: o ão que o modelo destrói")
class ReparoDeTerminacaoAoTest {

    /**
     * Dublê determinístico: conhece um punhado de palavras do português e nada mais. Não fala
     * com o hunspell — o teste é da REGRA, não da instalação da máquina.
     */
    private static final class DicionarioDeMentira implements DicionarioOrtograficoPort {
        private static final Set<String> CONHECIDAS = Set.of(
            "esquadrão", "pessoa", "lagoa", "comandante", "melhor", "amigos", "spearhead");

        @Override
        public Set<String> desconhecidas(Collection<String> palavras) {
            Set<String> fora = new LinkedHashSet<>();
            for (String p : palavras) {
                if (!CONHECIDAS.contains(p.toLowerCase(java.util.Locale.ROOT))) {
                    fora.add(p);
                }
            }
            return fora;
        }

        @Override
        public Map<String, Set<String>> sugestoes(Collection<String> palavras) {
            return Map.of(); // nenhuma sugestão: isola a regra de terminação da de acentuação
        }

        @Override
        public boolean disponivel() {
            return true;
        }

        @Override
        public String descricao() {
            return "dublê determinístico do teste";
        }
    }

    private final CorretorAcentoPorDicionario corretor =
        new CorretorAcentoPorDicionario(new DicionarioDeMentira());

    /** As quatro formas REAIS medidas na saída do 86. */
    @Test
    @DisplayName("CASO DOENTE: as quatro formas do 86 voltam a ser Esquadrão")
    void asQuatroFormasDo86SaoConsertadas() {
        assertEquals("Como comandante do Esquadrão Spearhead, farei o meu melhor.",
            corretor.corrigir("Como comandante do Esquadroo Spearhead, farei o meu melhor."));
        assertEquals("Este é o comandante do Esquadrão Spearhead.",
            corretor.corrigir("Este é o comandante do Esquadroao Spearhead."));
        assertEquals("substitutos para o Esquadrão Spearhead.",
            corretor.corrigir("substitutos para o Esquadroa Spearhead."));
        assertEquals("ajudar o Esquadrão Spearhead",
            corretor.corrigir("ajudar o Esquadroe Spearhead"));
    }

    /** A caixa do original manda: minúscula continua minúscula. */
    @Test
    @DisplayName("preserva a caixa: 'esquadroo' vira 'esquadrão', não 'Esquadrão'")
    void preservaACaixa() {
        assertEquals("Ele faz parte do meu esquadrão.",
            corretor.corrigir("Ele faz parte do meu esquadroo."));
    }

    /**
     * O CASO-CONTROLE que a medição do 86 entregou de graça: {@code Npessoa} casa o padrão e é
     * desconhecida, mas {@code Npessão} não existe — e por isso a regra não age.
     */
    @Test
    @DisplayName("CASO SÃO: proposta que não vira palavra é RECUSADA")
    void propostaInvalidaNaoEhAplicada() {
        assertEquals("Npessoa ficou como estava.", corretor.corrigir("Npessoa ficou como estava."));
    }

    /** Palavra legítima terminada em -oa/-oo jamais é candidata: o dicionário a conhece. */
    @Test
    @DisplayName("CASO SÃO: palavra válida terminada em -oa não é tocada")
    void palavraValidaNaoEhTocada() {
        assertEquals("A pessoa chegou na lagoa.", corretor.corrigir("A pessoa chegou na lagoa."));
    }

    /**
     * O piso existe para o romaji: sem ele, uma sílaba como {@code paoa} viraria {@code pão} e
     * o corretor passaria a corromper letra de música.
     */
    @Test
    @DisplayName("CASO SÃO: resultado curto demais é recusado (romaji não vira português)")
    void resultadoCurtoNaoEhAplicado() {
        assertEquals("paoa", corretor.corrigir("paoa"));
    }
}
