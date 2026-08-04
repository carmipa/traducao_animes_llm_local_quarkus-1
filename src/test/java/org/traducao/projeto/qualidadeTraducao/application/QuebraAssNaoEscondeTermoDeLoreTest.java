package org.traducao.projeto.qualidadeTraducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: a quebra visual do ASS não pode esconder um termo de lore de quem compara
 * termo <b>sem normalizar o texto antes</b>.
 *
 * <h2>O defeito, e onde ele REALMENTE mora</h2>
 * {@code \N} é a quebra de linha do ASS e ocupa DOIS caracteres: contrabarra e a letra {@code N}.
 * Colada no termo, o {@code N} é letra para {@code \p{L}}, o lookbehind
 * {@code (?<![\p{L}\p{N}])} conclui que {@code Argama} é sufixo de um {@code NArgama} inexistente,
 * e o termo fica invisível.
 *
 * <p><b>O portão de tradução ({@code validarPar}) NUNCA esteve cego</b>: ele normaliza a quebra
 * para espaço em {@code visivel()} antes de qualquer comparação. Foi afirmado em análise externa
 * de 2026-08-04 que havia "cegueira completa do ValidadorTraducaoService acionando o fallback
 * injustamente, confirmado nos logs" — não há tal confirmação e o caminho não é cego. Um teste
 * escrito contra {@code validarPar} passa com e sem o conserto, ou seja, não prova nada.
 *
 * <p>Quem sofre é <b>quem chama a comparação direto, sobre o texto cru do cache</b>:
 * {@code repararTrocaDeEntidade(original, traduzido, pares)}, usado pelo reforço de terminologia.
 * Medido no cache do Gundam ZZ em 2026-08-04: o ensaio acusou 2 falas a corrigir e <b>as 2 eram
 * falsas</b>; uma delas reverteria {@code Nahel Argama} para {@code Argama} numa tradução correta.
 *
 * <p>O conserto já existia MEDIDO em {@code EnforcadorTermosLore}: <i>"run completo de Gundam ZZ,
 * 47 episódios, 16.716 pares: 21,5% das falas têm a quebra, 74 trazem um termo canônico colado
 * nela… termo colado na quebra NUNCA foi corrigido; termo solto SEMPRE foi."</i> Estava em UM
 * arquivo e não fora propagado.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só o lado ESQUERDO precisa da alternativa: à direita do termo o caractere da quebra é a
 *       contrabarra, que já não é letra nem dígito.</li>
 *   <li>Onde o fansub quebrou a linha não pode mudar o veredito.</li>
 *   <li>Tratar a quebra NÃO pode cegar o reparo para a troca de verdade.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem a alternativa, o reparo inventa uma troca que não existe e corrompe tradução correta.
 */
class QuebraAssNaoEscondeTermoDeLoreTest {

    /** Quebra do ASS montada em runtime para o literal não se confundir com escape do fonte. */
    private static final String QUEBRA = "\\" + "N";

    private static final Set<List<String>> PARES = Set.of(List.of("Argama", "Nahel Argama"));

    private static ValidadorTraducaoService validador() {
        return new ValidadorTraducaoService(LoreAtivaFake.vazia());
    }

    @Test
    @DisplayName("termo colado na quebra do ORIGINAL nao vira troca falsa — caso do ZZ ep44")
    void quebraNoOriginalNaoInventaTroca() {
        String original = "Reading heat sources moving in the" + QUEBRA + "Nahel Argama's airspace!";
        String traduzido = "Fonte de calor se movendo no" + QUEBRA + "espaço aéreo da Nahel Argama!";

        assertNull(validador().repararTrocaDeEntidade(original, traduzido, PARES),
            "a traducao esta CORRETA: original e traducao dizem Nahel Argama. Sem tratar a "
                + "quebra, o reparo le so 'Argama' no original e reverteria a traducao boa.");
    }

    @Test
    @DisplayName("termo colado na quebra da TRADUCAO nao vira troca falsa — caso do ZZ ep40")
    void quebraNaTraducaoNaoInventaTroca() {
        String original = "The Nahel Argama's crew and" + QUEBRA + "civilians are all to disembark";
        String traduzido = "A tripulação e civis do" + QUEBRA + "Nahel Argama devem desembarcar";

        assertNull(validador().repararTrocaDeEntidade(original, traduzido, PARES),
            "o nome esta na traducao, colado na quebra; nao pode contar como sumido");
    }

    @Test
    @DisplayName("a MESMA fala com e sem quebra recebe o mesmo veredito")
    void quebraNaoMudaOVeredito() {
        String semQuebra = "Reading heat sources moving in the Nahel Argama's airspace!";
        String comQuebra = "Reading heat sources moving in the" + QUEBRA + "Nahel Argama's airspace!";
        String traduzido = "Fonte de calor se movendo no espaço aéreo da Nahel Argama!";

        assertNull(validador().repararTrocaDeEntidade(semQuebra, traduzido, PARES));
        assertNull(validador().repararTrocaDeEntidade(comQuebra, traduzido, PARES),
            "onde o fansub quebrou a linha nao pode mudar o veredito");
    }

    @Test
    @DisplayName("troca de entidade REAL continua sendo reparada, com quebra ou sem")
    void trocaVerdadeiraContinuaReparada() {
        String trocado = "Fonte de calor se movendo no espaço aéreo da Argama!";

        for (String original : List.of(
            "Reading heat sources moving in the" + QUEBRA + "Nahel Argama's airspace!",
            "Reading heat sources moving in the Nahel Argama's airspace!")) {

            String reparado = validador().repararTrocaDeEntidade(original, trocado, PARES);
            assertNotNull(reparado,
                () -> "tratar a quebra nao pode cegar o reparo para a troca de verdade: o "
                    + "original diz Nahel Argama e a traducao diz Argama, que e OUTRA nave — "
                    + original);
            assertTrue(reparado.contains("Nahel Argama"),
                () -> "o reparo tem de restaurar o nome certo: " + reparado);
        }
    }
}
