package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: prende o bug em que a quebra de linha do ASS desligava silenciosamente a
 * restauração determinística de terminologia. O marcador de quebra é a sequência literal
 * {@code \N} — dois caracteres, e o segundo é a LETRA N. Como as duas checagens de fronteira do
 * {@link EnforcadorTermosLore} eram {@code (?<![\p{L}\p{N}])}, um termo canônico colado na quebra
 * ficava invisível: em {@code "attack\NAxis"} o caractere antes de {@code Axis} é {@code 'N'}.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Os casos deste teste são REAIS, extraídos do run completo de Gundam ZZ (47 episódios,
 *       16.716 pares). A separação medida foi exata e é o que o teste reproduz: das oito falas em
 *       que {@code Axis} virou {@code Eixo}, as cinco com o termo colado na quebra NUNCA foram
 *       corrigidas e as três com o termo solto SEMPRE foram.</li>
 *   <li>A quebra vale como fronteira dos DOIS lados do par: tanto o canônico no original
 *       ({@code contarCanonico}) quanto a forma-ruim na tradução ({@code padraoFormaRuim}).</li>
 *   <li>A correção não pode afrouxar a fronteira normal — {@code "Eixon"} e {@code "aEixo"}
 *       continuam sem casar, senão trocaríamos um falso negativo por corrupção de palavra.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Falha aqui significa que uma fala com quebra de linha volta a escapar da restauração — o defeito
 * é silencioso em produção, porque a tradução fica gramaticalmente perfeita com o termo errado.
 */
@DisplayName("EnforcadorTermosLore: a quebra \\N do ASS é fronteira de palavra")
class EnforcadorTermosLoreQuebraAssTest {

    private final EnforcadorTermosLore enforcador = new EnforcadorTermosLore();

    private static final Map<String, String> MAPA_ZZ = Map.of(
        "Eixo", "Axis",
        "Titãs", "Titans",
        "Rainha Mansa", "Quin Mantha");

    /**
     * PROPÓSITO DE NEGÓCIO: os cinco casos de {@code Axis} que o run entregou errados por terem o
     * termo canônico colado na quebra no ORIGINAL.
     */
    @Test
    @DisplayName("canônico colado na quebra no original: os 5 casos reais que escapavam")
    void canonicoColadoNaQuebraNoOriginal() {
        assertEquals("A Argama vai atacar o\\NAxis sozinha? Impossível!",
            enforcador.reforcar("The Argama's to attack\\NAxis by itself? Impossible!",
                "A Argama vai atacar o\\NEixo sozinha? Impossível!", MAPA_ZZ));

        assertEquals("Isso mesmo. Vamos ficar\\Num tempo longe do Axis.",
            enforcador.reforcar("That's right. We'll be away from\\NAxis for a while.",
                "Isso mesmo. Vamos ficar\\Num tempo longe do Eixo.", MAPA_ZZ));

        assertEquals("Tenente! A linha defensiva\\Ndo Axis está desmoronando!",
            enforcador.reforcar("Deputy Captain!\\NAxis's defense line is collapsing!",
                "Tenente! A linha defensiva\\Ndo Eixo está desmoronando!", MAPA_ZZ));

        assertEquals("Confirmação recebida! O Axis está\\Nvindo em nossa direção desta vez!",
            enforcador.reforcar("Direction, confirmed!\\NAxis is headed for us, this time!",
                "Confirmação recebida! O Eixo está\\Nvindo em nossa direção desta vez!", MAPA_ZZ));

        // O artigo "o" que o modelo pôs antes de "eixo" NÃO é do escopo do enforcer: ele troca o
        // termo, não reescreve a frase. A saída correta preserva o artigo.
        assertEquals("Enviem comandos para infiltrar\\No Axis e alterar seu curso!",
            enforcador.reforcar("Dispatch commandos to infiltrate\\NAxis and change its course!",
                "Enviem comandos para infiltrar\\No eixo e alterar seu curso!", MAPA_ZZ));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: os três casos que já funcionavam continuam funcionando — a correção
     * não pode ser uma troca de um defeito por outro.
     */
    @Test
    @DisplayName("termo solto: os 3 casos reais que já eram corrigidos seguem corrigidos")
    void termoSoltoSegueCorrigido() {
        assertEquals("Eles entraram no Axis?!\\NSó os três?!",
            enforcador.reforcar("They went into Axis?!\\NJust the three of them?!",
                "Eles entraram no eixo?!\\NSó os três?!", MAPA_ZZ));

        assertEquals("Temos que deter o espião do\\NAxis! Sabemos onde ela vai!",
            enforcador.reforcar("We've gotta stop the Axis spy!\\NWe know where she's going!",
                "Temos que deter o espião do\\NEixo! Sabemos onde ela vai!", MAPA_ZZ));

        assertEquals("Se você me levar para o\\NAxis, eu devolvo sua irmã.",
            enforcador.reforcar("If you take me to Axis,\\NI'll return your sister.",
                "Se você me levar para o\\Neixo, eu devolvo sua irmã.", MAPA_ZZ));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a forma-ruim colada na quebra do lado da TRADUÇÃO também era invisível.
     * Caso real do ep24, único {@code Titans -> Titãs} do run.
     */
    @Test
    @DisplayName("forma-ruim colada na quebra na tradução também é alcançada")
    void formaRuimColadaNaQuebraNaTraducao() {
        assertEquals("Agora, só precisamos reunir quaisquer\\NTitans antigos.",
            enforcador.reforcar("Now, we just need to gather up any former\\NTitans...",
                "Agora, só precisamos reunir quaisquer\\NTitãs antigos.", MAPA_ZZ));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a quebra também cai DENTRO de termo composto, e aí tratar só a
     * fronteira não resolve — {@code Pattern.quote("Quin Mantha")} procura um espaço literal que
     * não está no texto. Caso real do ep44, o único {@code Quin Mantha} do run: ficou em 66,7%
     * de preservação nas DUAS gerações, antes e depois de a quebra virar fronteira, porque o
     * furo era este outro.
     */
    @Test
    @DisplayName("termo COMPOSTO partido pela quebra: \"Quin\\NMantha\" é alcançado")
    void termoCompostoPartidoPelaQuebra() {
        assertEquals("Você acha que pode deter a Quin Mantha jogando coisas nela?!",
            enforcador.reforcar("You think you can stop the Quin\\NMantha by throwing things at it?!",
                "Você acha que pode deter a Rainha\\NMansa jogando coisas nela?!", MAPA_ZZ));

        // Quebra só do lado do ORIGINAL: o canônico precisa ser reconhecido lá também.
        assertEquals("A Quin Mantha apareceu!",
            enforcador.reforcar("The Quin\\NMantha has appeared!", "A Rainha Mansa apareceu!", MAPA_ZZ));

        // Termo composto SEM quebra continua funcionando exatamente como antes.
        assertEquals("A Quin Mantha apareceu!",
            enforcador.reforcar("The Quin Mantha has appeared!", "A Rainha Mansa apareceu!", MAPA_ZZ));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a fronteira normal não pode afrouxar. Sem esta prova, a alternativa
     * introduzida para a quebra poderia passar a casar dentro de palavra e corromper o texto.
     */
    @Test
    @DisplayName("a fronteira normal continua estrita: nada casa dentro de palavra")
    void fronteiraNormalContinuaEstrita() {
        // "Eixon" e "aEixo" não são o termo: nenhuma substituição pode ocorrer.
        assertEquals("O Eixonauta partiu.",
            enforcador.reforcar("The Axis pilot left.", "O Eixonauta partiu.", MAPA_ZZ));
        assertEquals("Chegou aoEixo agora.",
            enforcador.reforcar("Arrived at Axis now.", "Chegou aoEixo agora.", MAPA_ZZ));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: sem o canônico no original, a quebra não pode virar desculpa para
     * reescrever — a regra continua sendo restaurar SÓ o que o original sustenta.
     */
    @Test
    @DisplayName("sem o canônico no original, a quebra não autoriza substituição")
    void semCanonicoNoOriginalNaoSubstitui() {
        assertEquals("O caminhão quebrou no\\Neixo traseiro.",
            enforcador.reforcar("The truck broke its\\Nrear axle.",
                "O caminhão quebrou no\\Neixo traseiro.", MAPA_ZZ));
    }
}
