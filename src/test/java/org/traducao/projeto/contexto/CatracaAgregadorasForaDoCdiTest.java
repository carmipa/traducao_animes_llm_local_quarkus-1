package org.traducao.projeto.contexto;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: impedir que alguém "conserte" a ausência de {@code @Component} nas lores
 * agregadas. A ausência é DELIBERADA e o motivo é qualidade de tradução — registrar qualquer uma
 * delas degrada a legenda em silêncio, sem quebrar nada que o build perceba.
 *
 * <h2>Por que a contagem sozinha não bastava</h2>
 * Já existe {@code RegistroProvedoresContextoIT} afirmando o total de provedores. Ele QUEBRA se
 * alguém registrar uma agregadora — mas a mensagem diz "esperava 58, veio 59", e a reação natural
 * de quem lê isso é trocar o 58 por 59 e seguir em frente. O número não conta o porquê, então o
 * caminho inocente para desfazer a decisão continua aberto.
 *
 * <p>Esta catraca fecha esse caminho: a falha NOMEIA a classe que entrou e manda ler o Javadoc dela
 * antes de mexer no teste. É o mesmo padrão de inventário nominal que a área de correção de cache
 * usa para arestas entre fatias, e chegou aqui depois de uma tentativa real de "corrigir" a
 * anotação faltando — o import de {@code @Component} sobrando numa das classes fez a exclusão
 * parecer esquecimento.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A lista é NOMINAL e fechada. Uma agregadora nova só entra aqui junto com o Javadoc que
 *       explica a colisão de termos que justifica a exclusão — a lista não é lugar para "achei
 *       que era melhor assim".</li>
 *   <li>Esta catraca COMPLEMENTA a contagem, não a substitui: o total continua sendo quem pega
 *       uma lore nova que alguém esqueceu de registrar.</li>
 *   <li>O critério é único e escrito: agregadora fica fora quando a união introduz vocabulário
 *       EXCLUSIVO de um título que contamina outro. Não é "toda agregadora fica fora" — é a
 *       colisão medida que decide.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Não faz I/O e não altera nada; só afirma. A falha nomeia o culpado e aponta onde está o motivo.
 */
@QuarkusTest
@DisplayName("catraca: lores agregadas continuam FORA do registro CDI")
class CatracaAgregadorasForaDoCdiTest {

    /**
     * As agregadoras deliberadamente excluídas, com a colisão que justifica cada uma. O texto vai
     * na mensagem de falha: quem quebrar esta catraca lê o motivo sem precisar abrir arquivo.
     */
    private static final Map<String, String> AGREGADORAS_FORA_DO_CDI = Map.of(
        "macross_delta_filmes",
        "Heimdall e Yami_Q_Ray só existem no Absolute Live; o filme 1 não os tem",
        "macross_frontier_filmes",
        "mistura três filmes (Itsuwari no Utahime / Itsuka no Tsubasa / Labyrinth of Time)",
        "macross_7_filmes",
        "Elma, Graham e Baleias Espaciais só existem no Dynamite 7; Pedro só no Galaxy's Calling Me!"
    );

    /**
     * Fora do CDI por outro motivo: a lore ainda não foi escrita. É uma espécie DIFERENTE de
     * exclusão e por isso mora numa lista separada — a agregadora não deve ser registrada nunca,
     * o esqueleto deve ser registrado assim que tiver conteúdo. Misturar os dois casos numa lista
     * só faria a mensagem de falha mentir para um deles.
     */
    private static final Map<String, String> ESQUELETOS_SEM_CONTEUDO = Map.of(
        "macross_frontier_filme3",
        "a lore de Labyrinth of Time ainda está vazia; registrar agora daria ao operador um prompt "
            + "de sistema SEM lore, que é pior que o fallback da série porque ele acreditaria estar "
            + "usando a lore do filme"
    );

    /** Os contextos específicos que devem ser usados NO LUGAR de cada agregadora. */
    private static final Map<String, Set<String>> SUBSTITUTOS = Map.of(
        "macross_delta_filmes", Set.of("macross_delta_filme1", "macross_delta_filme2"),
        "macross_frontier_filmes", Set.of("macross_frontier_filme1", "macross_frontier_filme2"),
        "macross_7_filmes", Set.of("macross_7_filme", "macross_dynamite_7", "macross_7_encore")
    );

    @Inject
    List<ProvedorContexto> provedores;

    @Test
    @DisplayName("nenhuma agregadora entrou no registro CDI")
    void agregadorasContinuamForaDoRegistro() {
        Set<String> registrados = provedores.stream()
            .map(ProvedorContexto::getId)
            .collect(Collectors.toSet());

        List<String> intrusas = AGREGADORAS_FORA_DO_CDI.keySet().stream()
            .filter(registrados::contains)
            .sorted()
            .toList();

        assertTrue(intrusas.isEmpty(), () -> intrusas.stream()
            .map(id -> "\n  A lore agregada \"" + id + "\" ENTROU no registro CDI."
                + "\n    Motivo da exclusão: " + AGREGADORAS_FORA_DO_CDI.get(id)
                + "\n    Use no lugar: " + String.join(", ", SUBSTITUTOS.get(id))
                + "\n    Se a intenção era mesmo registrá-la, o Javadoc da classe explica por que"
                + " isso degrada a tradução. Leia ANTES de alterar este teste.")
            .collect(Collectors.joining()));
    }

    /**
     * O esqueleto tem de continuar fora ATÉ ganhar conteúdo. Quando ganhar, este teste é o
     * primeiro a reprovar — e a mensagem diz o que fazer, em vez de só acusar um número.
     */
    @Test
    @DisplayName("esqueleto sem lore não entrou no registro CDI")
    void esqueletoSemConteudoContinuaForaDoRegistro() {
        Set<String> registrados = provedores.stream()
            .map(ProvedorContexto::getId)
            .collect(Collectors.toSet());

        List<String> intrusos = ESQUELETOS_SEM_CONTEUDO.keySet().stream()
            .filter(registrados::contains)
            .sorted()
            .toList();

        assertTrue(intrusos.isEmpty(), () -> intrusos.stream()
            .map(id -> "\n  O esqueleto \"" + id + "\" ENTROU no registro CDI."
                + "\n    " + ESQUELETOS_SEM_CONTEUDO.get(id)
                + "\n    Se a lore JÁ foi preenchida e validada, este é o teste certo para"
                + " atualizar: tire o id desta lista, e lembre do manifesto E7a e do total em"
                + " RegistroProvedoresContextoIT.")
            .collect(Collectors.joining()));
    }

    /**
     * A exclusão só se sustenta se houver para onde mandar o operador. Uma agregadora fora do CDI
     * cuja obra não tenha contexto específico não é proteção — é uma obra sem lore nenhuma.
     */
    @Test
    @DisplayName("cada agregadora excluída tem contextos específicos registrados no lugar dela")
    void todaAgregadoraExcluidaTemSubstitutoVivo() {
        Set<String> registrados = provedores.stream()
            .map(ProvedorContexto::getId)
            .collect(Collectors.toSet());

        List<String> orfas = SUBSTITUTOS.entrySet().stream()
            .filter(e -> !registrados.containsAll(e.getValue()))
            .map(e -> e.getKey() + " -> faltam " + e.getValue().stream()
                .filter(s -> !registrados.contains(s)).sorted().toList())
            .sorted()
            .toList();

        assertTrue(orfas.isEmpty(),
            () -> "agregadora excluída sem substituto registrado — o operador ficaria sem lore "
                + "para essas obras: " + orfas);
    }
}
