package org.traducao.projeto.traducao.presentation.web;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.lore.domain.ProvedorContexto;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: torna VISÍVEL a dívida de obras que existem no catálogo de apresentação
 * e não têm lore. Hoje o operador não as vê no seletor — o id resolve para nada e a entrada de
 * ordenação fica morta —, e a única coisa que registrava a pendência eram dois comentários
 * soltos no meio de {@code CatalogoObras}. Comentário não reprova build.
 *
 * <h2>Por que uma catraca e não apagar os ids</h2>
 * Os ids cadastrados são um CONTRATO: o nome de exibição completo já está escrito
 * ("RC 1014 - Reconguista in G I: Go! Core Fighter"), e o próprio código declara
 * {@code // Contrato p/ lore: ids gundam_greco_1..5}. Apagá-los perderia a intenção e a
 * ordenação pensada. Mantê-los sem nada que os observe é como estavam: dívida silenciosa.
 * A lista nominal abaixo é o meio-termo — a pendência existe, tem nome, e não pode crescer sem
 * alguém escrever o motivo aqui.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Todo id do catálogo sem provedor de tradução tem de estar declarado em UMA das duas
 *       listas — {@link #SLOTS_LORE_PENDENTE} ou {@link #FORA_DO_SELETOR_POR_DECISAO}. Um id
 *       novo sem lore e sem declaração reprova.</li>
 *   <li>A recíproca também vale: id declarado como pendente que JÁ ganhou lore reprova, para
 *       o progresso sair da lista. Sem isso a dívida vira ficção e a lista só cresce.</li>
 *   <li>Esta catraca fala de APRESENTAÇÃO. As agregadoras têm a sua própria, por motivo
 *       diferente — ver {@code CatracaAgregadorasForaDoCdiTest}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Não faz I/O e não altera nada; a falha nomeia o id e diz de que lado está o problema.
 */
@QuarkusTest
@DisplayName("catraca: ids do catálogo sem lore são declarados, não esquecidos")
class CatracaSlotsReservadosLoreTest {

    /**
     * Obras com id, nome de exibição e posição já cadastrados, e lore AINDA não escrita. Sair
     * daqui é o "pronto" auditável de cada uma.
     *
     * <p>VAZIA desde 2026-07-28: todo id do catálogo tem lore. Nasceu com 7 (gundam_ms_igloo,
     * gundam_thunderbolt e gundam_greco_1..5), ganhou 2 na divisão de IGLOO/Thunderbolt, e as 9
     * saíram. A lista permanece porque a dívida vai voltar — obra nova entra no catálogo antes de
     * ter lore, e é aqui que isso fica visível em vez de virar comentário solto.
     */
    private static final Map<String, String> SLOTS_LORE_PENDENTE = Map.of();

    /**
     * Ids que ficam fora do seletor de propósito e NÃO são dívida. Não confundir com o de cima:
     * estes não devem ganhar lore de tradução nunca — o que falta neles é nada.
     */
    private static final Map<String, String> FORA_DO_SELETOR_POR_DECISAO = Map.of(
        "macross_7_filmes", "agregadora — ver CatracaAgregadorasForaDoCdiTest",
        "macross_delta_filmes", "agregadora — ver CatracaAgregadorasForaDoCdiTest",
        "macross_frontier_filmes", "agregadora — ver CatracaAgregadorasForaDoCdiTest",
        "macross_dyrl",
        "alias VIVO só no lado da Revisão (legado de UI); a tradução do DYRL é coberta por "
            + "macross_filme1"
    );

    @Inject
    CatalogoObras catalogo;

    @Inject
    List<ProvedorContexto> provedores;

    private Set<String> idsComLore() {
        return provedores.stream().map(ProvedorContexto::getId).collect(Collectors.toSet());
    }

    @Test
    @DisplayName("todo id do catálogo sem lore está declarado como pendente ou como decisão")
    void idSemLoreTemDeEstarDeclarado() {
        Set<String> comLore = idsComLore();

        List<String> naoDeclarados = catalogo.idsCadastrados().stream()
            .filter(id -> !comLore.contains(id))
            .filter(id -> !SLOTS_LORE_PENDENTE.containsKey(id))
            .filter(id -> !FORA_DO_SELETOR_POR_DECISAO.containsKey(id))
            .sorted()
            .toList();

        assertTrue(naoDeclarados.isEmpty(),
            () -> "\n  Estes ids estão no catálogo de apresentação e NÃO têm lore, e ninguém"
                + " declarou por quê: " + naoDeclarados
                + "\n  O operador não os vê no seletor — a entrada de ordenação é morta."
                + "\n  Ou escreva a lore, ou declare em SLOTS_LORE_PENDENTE (dívida, com o"
                + " motivo) ou em FORA_DO_SELETOR_POR_DECISAO (não é dívida).");
    }

    /**
     * O outro sentido da catraca. Sem ele a lista de pendências viraria decoração: obras já
     * resolvidas continuariam listadas como dívida e ninguém saberia quanto falta de verdade.
     */
    @Test
    @DisplayName("id declarado como pendente que já ganhou lore tem de sair da lista")
    void pendenciaResolvidaSaiDaLista() {
        Set<String> comLore = idsComLore();

        List<String> jaResolvidos = SLOTS_LORE_PENDENTE.keySet().stream()
            .filter(comLore::contains)
            .sorted()
            .toList();

        assertTrue(jaResolvidos.isEmpty(),
            () -> "\n  PROGRESSO: estes ids ganharam lore e continuam declarados como pendentes: "
                + jaResolvidos
                + "\n  Remova-os de SLOTS_LORE_PENDENTE — a lista serve para dizer quanto falta,"
                + " e com item resolvido dentro ela mente.");
    }

    /**
     * Uma decisão de "fora do seletor" que passe a ter lore de tradução é contradição: ou a
     * decisão mudou e a declaração tem de sair, ou alguém registrou o que não devia.
     */
    @Test
    @DisplayName("id declarado fora do seletor não pode ter ganhado lore de tradução")
    void decisaoDeExclusaoNaoFoiContrariada() {
        Set<String> comLore = idsComLore();

        List<String> contraditos = FORA_DO_SELETOR_POR_DECISAO.keySet().stream()
            .filter(comLore::contains)
            .sorted()
            .toList();

        assertTrue(contraditos.isEmpty(), () -> contraditos.stream()
            .map(id -> "\n  \"" + id + "\" foi declarado FORA do seletor ("
                + FORA_DO_SELETOR_POR_DECISAO.get(id) + ") mas TEM lore de tradução registrada."
                + "\n    Ou a decisão mudou e esta declaração sai, ou alguém registrou o que não"
                + " devia. Leia o Javadoc da classe antes de escolher.")
            .collect(Collectors.joining()));
    }
}
