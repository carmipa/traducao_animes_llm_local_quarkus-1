package org.traducao.projeto.traducao.domain.ports;

import org.traducao.projeto.traducao.domain.Lote;
import org.traducao.projeto.traducao.domain.StatusLlm;
import org.traducao.projeto.traducao.domain.TraducaoLote;

import java.util.List;
import java.util.Optional;

public interface MistralPort {
    TraducaoLote traduzir(Lote lote);

    /**
     * Variante com temperatura explícita, usada nas retentativas de uma fala
     * isolada: repetir a MESMA requisição com a mesma temperatura tende a
     * reproduzir a mesma alucinação; subir a temperatura muda a amostragem e
     * dá chance real de recuperação. {@code null} usa a temperatura configurada.
     */
    default TraducaoLote traduzir(Lote lote, Double temperaturaOverride) {
        return traduzir(lote);
    }

    /**
     * Variante que recebe o prompt de sistema CONGELADO no início do job. Assim,
     * uma troca de contexto (lore) no estado global não pode vazar para o meio da
     * tradução de um episódio. {@code null} usa o prompt do contexto ativo.
     */
    default TraducaoLote traduzir(Lote lote, Double temperaturaOverride, String promptSistemaCongelado) {
        return traduzir(lote, temperaturaOverride);
    }

    /**
     * Verifica, antes de iniciar a tradução, se o servidor LLM local está
     * online e se o modelo configurado está efetivamente carregado em
     * memória — evita descobrir isso só depois de várias tentativas/timeouts
     * já no meio da tradução do primeiro episódio.
     */
    StatusLlm verificarDisponibilidade();

    /**
     * Revisa uma fala já traduzida, corrigindo concordância de gênero/pronomes.
     * Retorna vazio se o LLM falhar ou a resposta for inválida.
     */
    Optional<String> revisarConcordancia(
        String originalInglesMascarado,
        String traducaoPtMascarada,
        List<String> problemasDetectados
    );

    /**
     * Retraduz uma fala cuja tradução existente ficou com resíduo em inglês,
     * incompleta ou alucinada, usando o prompt completo (lore + regras) do
     * contexto ativo. Retorna vazio se o LLM falhar ou a resposta for inválida.
     */
    Optional<String> corrigirTraducao(
        String originalInglesMascarado,
        String traducaoPtMascarada,
        String motivoDetectado
    );

    /**
     * Revisa nomes proprios, locais, faccoes e termos de lore na traducao PT-BR
     * usando a lore do contexto ativo. Retorna vazio se o LLM falhar ou a resposta for invalida.
     */
    Optional<String> revisarLore(
        String promptSistemaRevisaoLore,
        String originalInglesMascarado,
        String traducaoPtMascarada,
        List<String> problemasDetectados
    );
}
