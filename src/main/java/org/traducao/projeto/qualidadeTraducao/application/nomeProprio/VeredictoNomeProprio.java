package org.traducao.projeto.qualidadeTraducao.application.nomeProprio;

import java.util.List;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: resposta da checagem de nome próprio, com a distinção que um mapa vazio
 * não consegue expressar — "olhei e não há nome perdido" contra "não tive como olhar".
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code verificado == false} significa que o dicionário não respondeu. Nesse estado
 *       {@link #perdidos()} é sempre vazio, e ler esse vazio como "está tudo certo" é o defeito
 *       que este record existe para impedir.</li>
 *   <li>{@code verificado == true} com {@link #perdidos()} vazio é afirmação positiva: as
 *       candidatas foram classificadas e todas sobreviveram à tradução.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem I/O e sem estado. As listas são imutáveis; nenhum método lança.
 */
public record VeredictoNomeProprio(Map<String, List<String>> perdidos, boolean verificado,
                                   int candidatasExaminadas) {

    public VeredictoNomeProprio {
        perdidos = perdidos == null ? Map.of() : Map.copyOf(perdidos);
    }

    /** O dicionário não respondeu: NÃO VERIFICADO, que não é aprovação (regra de três estados). */
    public static VeredictoNomeProprio naoVerificado() {
        return new VeredictoNomeProprio(Map.of(), false, 0);
    }

    /** Examinou e nada se perdeu — afirmação positiva, distinta do estado acima. */
    public static VeredictoNomeProprio limpo(int examinadas) {
        return new VeredictoNomeProprio(Map.of(), true, examinadas);
    }

    /** Quantas falas trouxeram ao menos um nome próprio ausente da tradução. */
    public int falasAfetadas() {
        return perdidos.size();
    }

    /** Total de nomes perdidos somando todas as falas. */
    public int totalPerdidos() {
        return perdidos.values().stream().mapToInt(lista -> lista == null ? 0 : lista.size()).sum();
    }

    public boolean temPerda() {
        return !perdidos.isEmpty();
    }
}
