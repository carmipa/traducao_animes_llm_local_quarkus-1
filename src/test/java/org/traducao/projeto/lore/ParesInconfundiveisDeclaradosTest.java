package org.traducao.projeto.lore;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.lore.domain.ProvedorContexto;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: catraca de INVENTÁRIO NOMINAL dos pares inconfundíveis. Cada par aqui é a
 * única coisa que impede a repetição de um defeito JÁ MEDIDO no acervo — e as quatro obras que o
 * declaram tinham, até 2026-07-28, uma linha de lore que ensinava a RELAÇÃO entre dois nomes e que
 * o modelo leu como licença para SUBSTITUIR um pelo outro.
 *
 * <h2>O que foi medido, e é o que este teste protege</h2>
 * <pre>
 *   gundam_zeta    172 falas   EN "Four"        -> PT "Quatro" e, em 15, "Quattro"
 *   gundam_zz       55 falas   EN "Zeta Gundam" -> PT "ZZ Gundam"     (mecha diferente)
 *   gundam_zz       31 falas   EN "Argama"      -> PT "Nahel Argama"  (nave diferente)
 *   guilty_crown    20 falas   EN "Inori"       -> PT "Crow"          ("Crow": 0 vezes no inglês)
 *   gundam_08ms      4 falas   EN "Sanders"     -> PT "Shinigami"     ("Shinigami": 0 no inglês)
 * </pre>
 *
 * <h2>Por que uma catraca, e não confiança na revisão</h2>
 * Os três primeiros pares foram declarados sem teste nenhum. Um par apagado por engano não quebra
 * compilação, não quebra teste e não aparece em log: volta a valer a lore antiga, o modelo volta a
 * substituir, e o defeito só reaparece na próxima auditoria manual do acervo — que foi como estes
 * cinco casos foram descobertos, um a um.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O inventário é NOMINAL e fechado: acrescentar par a uma obra listada exige editar este
 *       arquivo, e é aí que se pergunta "qual foi a medição?". Um par declarado sem medição é a
 *       heurística genérica que foi testada e DESCARTADA — "termo protegido no PT ausente do EN"
 *       disparava 1447 vezes em 59.625 falas, quase tudo normalização legítima.</li>
 *   <li>Obra FORA da lista não é obrigada a declarar par: a maioria das 68 não tem alias que o
 *       modelo confunda, e exigir o campo de todas transformaria a catraca em ruído.</li>
 *   <li>{@code paresInconfundiveis()} NÃO entra no hash do manifesto de lore (ver
 *       {@link ProtecaoConteudoLoreTest}), então este teste é a ÚNICA proteção do campo.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Par removido, renomeado ou com grafia trocada reprova o teste nomeando a obra e o par.
 */
@QuarkusTest
@DisplayName("catraca: os pares inconfundíveis medidos continuam declarados")
class ParesInconfundiveisDeclaradosTest {

    /** Obra → pares que a medição no acervo exigiu. Ordem interna do par é irrelevante. */
    private static final Map<String, Set<Set<String>>> ESPERADO = Map.of(
        "gundam_zeta", Set.of(
            Set.of("Four", "Quattro"),
            Set.of("Zeta Gundam", "Gundam Mk-II"),
            // O único par do catálogo que protege ENREDO e não grafia: Char Aznable se
            // apresenta como Quattro Bajeena, e a obra esconde isso de propósito. Medido:
            // 34 falas com EN "Char" -> PT "Quattro", e ZERO na direção inversa.
            Set.of("Char", "Quattro")),
        "gundam_zz", Set.of(
            Set.of("Zeta Gundam", "ZZ Gundam"),
            Set.of("Argama", "Nahel Argama")),
        "guilty_crown", Set.of(
            Set.of("Inori", "Crow")),
        // Três pares para UM personagem, e cada um guarda uma direção diferente do mesmo
        // invariante — o REGISTRO SOCIAL que a obra usa de propósito: companheiro chama pelo
        // nome, os de fora usam o apelido, e ele próprio rejeita o apelido.
        "gundam_08ms", Set.of(
            Set.of("Sanders", "Ceifador"),   // tratamento do esquadrão não vira apelido
            Set.of("Reaper", "Sanders"),     // apelido não vira nome ("Sniper 2!" saiu "Sanders!")
            Set.of("Sanders", "Shinigami"))  // e o japonês não vaza para a legenda PT
    );

    @Inject
    List<ProvedorContexto> provedores;

    @Test
    @DisplayName("cada obra medida declara exatamente os pares que a medição exigiu")
    void paresMedidosContinuamDeclarados() {
        Map<String, ProvedorContexto> porId = provedores.stream()
            .collect(Collectors.toMap(ProvedorContexto::getId, p -> p, (a, b) -> a));

        ESPERADO.forEach((id, esperado) -> {
            ProvedorContexto p = porId.get(id);
            assertTrue(p != null, "obra sumiu do catálogo: " + id);

            Set<Set<String>> declarado = p.paresInconfundiveis().stream()
                .map(Set::copyOf)
                .collect(Collectors.toSet());

            assertEquals(esperado, declarado,
                () -> "os pares de " + id + " mudaram. Cada par aqui corresponde a um defeito "
                    + "contado no acervo; se a mudança é intencional, atualize este inventário "
                    + "junto com a medição que a justifica.");
        });
    }

    /**
     * Um par com um termo só, ou com o mesmo termo dos dois lados, não proíbe nada — e passaria
     * despercebido, porque o portão simplesmente nunca dispararia. Vale para o catálogo inteiro,
     * não só para as obras do inventário acima.
     */
    @Test
    @DisplayName("nenhum par degenerado no catálogo inteiro")
    void nenhumParDegenerado() {
        provedores.forEach(p -> p.paresInconfundiveis().forEach(par -> {
            assertEquals(2, par.size(),
                () -> "par tem de ter exatamente dois termos em " + p.getId() + ": " + par);
            assertTrue(!par.get(0).equalsIgnoreCase(par.get(1)),
                () -> "par com o mesmo termo dos dois lados não proíbe nada em " + p.getId()
                    + ": " + par);
            par.forEach(termo -> assertTrue(termo != null && !termo.isBlank(),
                () -> "termo em branco no par de " + p.getId() + ": " + par));
        }));
    }
}
