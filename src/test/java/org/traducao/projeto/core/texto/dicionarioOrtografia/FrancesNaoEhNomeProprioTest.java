package org.traducao.projeto.core.texto.dicionarioOrtografia;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * PROPÓSITO DE NEGÓCIO: impede que palavra francesa comum seja tomada por nome próprio inventado
 * da obra quando a tradução parte de uma faixa em francês.
 *
 * <h2>O prejuízo medido, e é recente</h2>
 * No primeiro run do {@code Memories (1995)} a partir da faixa francesa, em 14/08/2026, o
 * detector de nome próprio acusou seis palavras. <b>Cinco eram francês comum</b> e uma só era
 * nome de verdade:
 * <pre>
 *   E01 Magnetic Rose: Californie, Aoshima, Dieu, Octobre, Juillet
 *   E03 Cannon Fodder: Maman
 * </pre>
 * A causa não era a heurística: era o dicionário francês estar instalado na máquina e NÃO ligado
 * ao classificador, que perguntava só a pt/en/de. Sem essa pergunta, {@code Dieu} não pertence a
 * idioma nenhum — que é exatamente a assinatura de nome inventado da obra.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Palavra que o francês reconhece NUNCA é {@link VeredictoPalavra#DESCONHECIDA} — é o
 *       veredicto que autoriza a acusação de nome próprio.</li>
 *   <li>Nome próprio de verdade CONTINUA sendo desconhecido: o dicionário francês não pode
 *       cegar o detector, só refiná-lo.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem hunspell ou sem o dicionário fr_FR, PULA por {@link Assumptions} — nunca passa por ausência
 * de verificação.
 */
@DisplayName("francês comum não é nome próprio da obra")
class FrancesNaoEhNomeProprioTest {

    private static Map<String, VeredictoPalavra> classificar(Set<String> palavras) {
        return new CorretorOrtograficoLegenda().classificar(palavras);
    }

    /** As cinco palavras REAIS que foram acusadas por engano no run de 14/08/2026. */
    @Test
    @DisplayName("CASO DOENTE: as 5 francesas acusadas no Memories deixam de ser DESCONHECIDA")
    void asCincoFrancesasDoRunNaoSaoMaisDesconhecidas() {
        Set<String> acusadas = Set.of("Dieu", "Octobre", "Juillet", "Maman", "Californie");
        var v = classificar(acusadas);
        Assumptions.assumeFalse(v.isEmpty(), "hunspell ausente — NÃO VERIFICADO");
        Assumptions.assumeTrue(
            v.values().stream().noneMatch(x -> x == VeredictoPalavra.NAO_VERIFICADO),
            "algum dicionário indisponível — NÃO VERIFICADO");

        for (String p : acusadas) {
            assertNotEquals(VeredictoPalavra.DESCONHECIDA, v.get(p),
                "'" + p + "' voltou a ser DESCONHECIDA, e DESCONHECIDA é o veredicto que autoriza "
                    + "o detector a acusá-la de nome próprio traduzido. Foi assim que 5 de 6 "
                    + "acusações do run de 14/08/2026 nasceram falsas. Veredicto atual: " + v.get(p));
        }
    }

    /**
     * O CASO-CONTROLE, e é o que impede a correção de virar cegueira: o único nome PRÓPRIO de
     * verdade da lista precisa continuar desconhecido.
     */
    @Test
    @DisplayName("CASO SÃO: Aoshima continua DESCONHECIDA — é nome de verdade")
    void nomeProprioRealContinuaDesconhecido() {
        var v = classificar(Set.of("Aoshima", "Blackwood", "Nirasaki"));
        Assumptions.assumeFalse(v.isEmpty(), "hunspell ausente — NÃO VERIFICADO");
        Assumptions.assumeTrue(
            v.values().stream().noneMatch(x -> x == VeredictoPalavra.NAO_VERIFICADO),
            "algum dicionário indisponível — NÃO VERIFICADO");

        assertEquals(VeredictoPalavra.DESCONHECIDA, v.get("Aoshima"),
            "o dicionário francês cegou o detector: Aoshima é nome próprio de verdade e precisa "
                + "continuar sendo acusável");
        assertEquals(VeredictoPalavra.DESCONHECIDA, v.get("Blackwood"));
    }

    /** O português continua tendo precedência — o francês entrou por último e não atropela. */
    @Test
    @DisplayName("português continua vencendo: palavra PT não vira TERMO_FRANCES")
    void portuguesTemPrecedencia() {
        var v = classificar(Set.of("situação", "colônia", "verdade"));
        Assumptions.assumeFalse(v.isEmpty(), "hunspell ausente — NÃO VERIFICADO");
        for (String p : Set.of("situação", "colônia", "verdade")) {
            assertEquals(VeredictoPalavra.PORTUGUES_OK, v.get(p),
                "'" + p + "' é português e foi rotulada como outra coisa: " + v.get(p));
        }
    }

    /** E a correção de acento, que é a função original do subpacote, não pode ter mudado. */
    @Test
    @DisplayName("o acento continua sendo reposto — o francês não atrapalha a correção")
    void correcaoDeAcentoIntacta() {
        var c = new CorretorOrtograficoLegenda();
        String saida = c.corrigir("A situacao da colonia e critica.");
        Assumptions.assumeTrue(c.disponivel(), "hunspell ausente — NÃO VERIFICADO");
        org.junit.jupiter.api.Assertions.assertTrue(saida.contains("situação"),
            "a correção de acento parou de funcionar: " + saida);
    }
}
