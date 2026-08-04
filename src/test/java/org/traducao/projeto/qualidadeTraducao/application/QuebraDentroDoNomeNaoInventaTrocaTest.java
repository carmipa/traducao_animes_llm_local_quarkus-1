package org.traducao.projeto.qualidadeTraducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * PROPÓSITO DE NEGÓCIO: quando o fansub parte um nome composto NA quebra de linha
 * ({@code "Nahel\NArgama"}), o reparo de entidade não pode ler aquilo como menção à nave menor.
 *
 * <h2>Regressão que este teste congela</h2>
 * Em 2026-08-04 o projeto passou a tratar {@code \N} como FRONTEIRA de termo — conserto correto e
 * medido. Só que o mesmo commit não tratou a quebra DENTRO do termo, e {@code Pattern.quote("Nahel
 * Argama")} procura um ESPAÇO literal que não está lá. Com a fronteira nova, o termo curto passou
 * a ser encontrado onde só existia o longo partido. Sonda do mesmo dia, com a lore do ZZ:
 * <ul>
 *   <li>original {@code "Nahel\NArgama"} + tradução CORRETA → revertia a tradução para
 *       {@code "Argama"}, que é outra nave</li>
 *   <li>tradução {@code "Nahel\NArgama"} + original inteiro → devolvia
 *       {@code "Nahel\NNahel Argama!"}, com o nome DUPLICADO na tela</li>
 * </ul>
 * Com a fronteira ANTIGA os dois davam {@code null}: a corrupção nasceu do conserto, não existia
 * antes dele.
 *
 * <p><b>Escala no acervo</b> (60.891 falas do cache, 24,6% com a quebra): <b>435 campos trazem um
 * nome composto partido pela quebra, em 230 formas distintas</b> — {@code Amuro\NRay},
 * {@code Bask\NOm}, {@code Baund\NDoc}, {@code Anavel\NGato}, {@code Alberto\NVist}. Não é caso de
 * laboratório.
 *
 * <p>O mecanismo do conserto já existia em {@code EnforcadorTermosLore.SEPARADOR_INTERNO}, com
 * medição própria ({@code "Quin Mantha"} preso em 66,7% de preservação). Estava em UM arquivo e
 * não fora propagado — a mesma assinatura dos três defeitos de 2026-08-03.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Onde o fansub quebrou a linha — entre os termos ou DENTRO de um deles — não muda o
 *       veredito.</li>
 *   <li>Aceitar a quebra dentro do termo NÃO pode cegar o reparo para a troca de verdade.</li>
 *   <li>Termo de UMA palavra tem de se comportar exatamente como antes.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem o separador flexível, o reparo inventa troca inexistente e corrompe tradução correta — ora
 * apagando metade do nome, ora duplicando-o.
 */
class QuebraDentroDoNomeNaoInventaTrocaTest {

    /** Quebra do ASS montada em runtime para o literal não se confundir com escape do fonte. */
    private static final String QUEBRA = "\\" + "N";

    private static final Set<List<String>> PARES = Set.of(List.of("Argama", "Nahel Argama"));

    private static ValidadorTraducaoService validador() {
        return new ValidadorTraducaoService(LoreAtivaFake.vazia());
    }

    @Test
    @DisplayName("nome partido no ORIGINAL nao faz reverter traducao correta")
    void nomePartidoNoOriginalNaoReverte() {
        String original = "Reading heat sources near the Nahel" + QUEBRA + "Argama's airspace!";
        String traduzido = "Fonte de calor perto do espaço aéreo da Nahel Argama!";

        assertNull(validador().repararTrocaDeEntidade(original, traduzido, PARES),
            "as DUAS falam da Nahel Argama; o original so esta partido na virada da linha");
    }

    @Test
    @DisplayName("nome partido na TRADUCAO nao duplica o nome")
    void nomePartidoNaTraducaoNaoDuplica() {
        String original = "Reading heat sources near the Nahel Argama's airspace!";
        String traduzido = "Fonte de calor perto do espaço aéreo da Nahel" + QUEBRA + "Argama!";

        assertNull(validador().repararTrocaDeEntidade(original, traduzido, PARES),
            "o nome esta inteiro na traducao, so partido pela quebra — reparar aqui produzia "
                + "\"Nahel" + QUEBRA + "Nahel Argama\" na tela");
    }

    @Test
    @DisplayName("nome partido nos DOIS lados segue sem veredito")
    void nomePartidoNosDoisLados() {
        String original = "Reading heat sources near the Nahel" + QUEBRA + "Argama's airspace!";
        String traduzido = "Fonte de calor perto do espaço aéreo da Nahel" + QUEBRA + "Argama!";

        assertNull(validador().repararTrocaDeEntidade(original, traduzido, PARES));
    }

    @Test
    @DisplayName("troca REAL continua reparada mesmo com o nome partido no original")
    void trocaVerdadeiraContinuaReparada() {
        String original = "Reading heat sources near the Nahel" + QUEBRA + "Argama's airspace!";
        String trocado = "Fonte de calor perto do espaço aéreo da Argama!";

        String reparado = validador().repararTrocaDeEntidade(original, trocado, PARES);
        assertNotNull(reparado,
            "aceitar a quebra dentro do termo nao pode cegar o reparo: o original diz Nahel "
                + "Argama e a traducao diz Argama, que e OUTRA nave");
        assertEquals("Fonte de calor perto do espaço aéreo da Nahel Argama!", reparado);
    }

    @Test
    @DisplayName("termo de UMA palavra se comporta como antes")
    void termoSimplesNaoMuda() {
        Set<List<String>> paresSimples = Set.of(List.of("Argama", "Radish"));
        String original = "The Radish is under attack!";
        String trocado = "A Argama está sob ataque!";

        assertEquals("A Radish está sob ataque!",
            validador().repararTrocaDeEntidade(original, trocado, paresSimples),
            "o separador flexivel so vale ENTRE palavras; nome de uma palavra nao muda de padrao");
    }
}
