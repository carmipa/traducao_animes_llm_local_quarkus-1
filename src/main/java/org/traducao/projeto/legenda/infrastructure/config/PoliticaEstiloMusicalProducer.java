package org.traducao.projeto.legenda.infrastructure.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;

import java.util.List;
import java.util.Optional;

/**
 * PROPÓSITO DE NEGÓCIO: ponte técnica (CDI) que lê a lista configurada de estilos
 * musicais preserváveis e produz a política de domínio pura {@link PoliticaEstiloMusical}.
 * Isola o Quarkus/MicroProfile Config na borda de infraestrutura do módulo {@code legenda},
 * mantendo o domínio livre de framework.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Chave lida: {@code tradutor.estilos-ignorados} (mantida nesta subfase — sem rename).</li>
 *   <li>Produz um bean de PSEUDO-ESCOPO {@code @Singleton}: {@link PoliticaEstiloMusical}
 *       é {@code final} e não admite proxy de escopo normal.</li>
 *   <li>Fallback {@code ["Song JP"]} quando a chave está ausente (Optional.empty).</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha / caracterização de binding (E3c)</h2>
 * Chave ausente → política construída com o fallback {@code ["Song JP"]}, idêntico ao
 * binding histórico do {@code TradutorProperties}. Caracterização comprovada: sob string
 * vazia ({@code ""}), Spring e SmallRye COLAPSAM em ausente (ambos → {@code ["Song JP"]}) —
 * paridade preservada. A sequência YAML vazia ({@code []}) NÃO foi caracterizada e não
 * deve ser assumida como estado distinto.
 */
@ApplicationScoped
public class PoliticaEstiloMusicalProducer {

    private static final List<String> FALLBACK = List.of("Song JP");

    /**
     * PROPÓSITO DE NEGÓCIO: resolve a lista configurada e devolve a política pura,
     * preservando a lista exatamente (ordem/case/duplicatas/vazios) e aplicando o
     * fallback só quando a chave está ausente.
     *
     * <p>FALHA: chave ausente/valor vazio → {@code new PoliticaEstiloMusical(["Song JP"])}.
     */
    @Produces
    @Singleton
    public PoliticaEstiloMusical politicaEstiloMusical(
            @ConfigProperty(name = "tradutor.estilos-ignorados") Optional<List<String>> estilos) {
        return new PoliticaEstiloMusical(estilos.orElse(FALLBACK));
    }
}
