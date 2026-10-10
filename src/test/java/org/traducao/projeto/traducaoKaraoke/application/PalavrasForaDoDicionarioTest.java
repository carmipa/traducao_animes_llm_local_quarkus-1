package org.traducao.projeto.traducaoKaraoke.application;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que a conferência de palavra inventada na letra de karaokê separa a
 * invenção do modelo do que é legítimo e carrega o MESMO sinal superficial (palavra que o
 * português não reconhece): nome que veio do original e termo de outro idioma real.
 *
 * <h2>Os casos, todos reais</h2>
 * <ul>
 *   <li>{@code Açãoaria} — Break Blade 1, teste ponta a ponta de 09/10/2026. Doente.</li>
 *   <li>{@code Anjel} — 0083, cache real do karaokê. Doente.</li>
 *   <li>{@code Alchemilla} — 86 Part 2, a palavra está no ORIGINAL. Legítima.</li>
 *   <li>{@code Möbius} — o original diz {@code Moebius}; o alemão reconhece. Legítima.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem hunspell instalado o caso real PULA (NÃO VERIFICADO); o caso sem dicionário roda sempre e
 * prova que a ausência não vira aprovação.
 */
@DisplayName("4.1: palavra fora do dicionario na letra traduzida")
class PalavrasForaDoDicionarioTest {

    @Test
    @DisplayName("acusa a invencao e poupa o legitimo com o mesmo sinal")
    void acusaInvencaoEpoupaLegitimo() {
        Map<String, String> traduzidas = new LinkedHashMap<>();
        traduzidas.put("{\\be1}I'd act on all of my feelings", "{\\be1}Açãoaria em todos os meus sentimentos.");
        traduzidas.put("Angel come and take me by the hand", "Anjel, venha e leve-me pela mão.");
        traduzidas.put("Chanting Alchemilla, Alchemilla", "Cantando Alchemilla, Alchemilla.");
        traduzidas.put("Just couldn't find the way out of this Moebius loop",
            "Não consegui encontrar a saída desse loop de Möbius.");
        traduzidas.put("Even if the world ends tomorrow", "Mesmo que o mundo acabe amanhã");

        CorretorOrtograficoLegenda dicionario = new CorretorOrtograficoLegenda();
        PalavrasForaDoDicionario.Verificacao v = PalavrasForaDoDicionario.verificar(dicionario, traduzidas);
        Assumptions.assumeTrue(v.verificado(), "hunspell ausente — NÃO VERIFICADO");

        assertEquals(List.of("Açãoaria"), v.porLetra().get("{\\be1}I'd act on all of my feelings"),
            "a invencao do Break Blade nao foi acusada");
        assertEquals(List.of("Anjel"), v.porLetra().get("Angel come and take me by the hand"),
            "a invencao do 0083 nao foi acusada");
        assertFalse(v.porLetra().containsKey("Chanting Alchemilla, Alchemilla"),
            "alarme falso: o nome esta no original e foi mantido");
        assertFalse(v.porLetra().containsKey("Just couldn't find the way out of this Moebius loop"),
            "alarme falso: Möbius e grafia real, reconhecida pelo alemao");
        assertFalse(v.porLetra().containsKey("Even if the world ends tomorrow"),
            "alarme falso em portugues comum");
        assertEquals(2, v.porLetra().size());
    }

    @Test
    @DisplayName("sem dicionario: NAO VERIFICADO, nunca 'nenhuma palavra inventada'")
    void semDicionarioNaoVerifica() {
        PalavrasForaDoDicionario.Verificacao v = PalavrasForaDoDicionario.verificar(null,
            Map.of("I'd act on all of my feelings", "Açãoaria em todos os meus sentimentos."));
        assertFalse(v.verificado(), "ausencia do dicionario virou aprovacao");
        assertTrue(v.porLetra().isEmpty());
    }
}
