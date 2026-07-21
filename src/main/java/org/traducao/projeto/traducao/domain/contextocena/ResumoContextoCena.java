package org.traducao.projeto.traducao.domain.contextocena;

/**
 * PROPÓSITO DE NEGÓCIO: contadores PRÓPRIOS da tradução por contexto de cena de um episódio —
 * o que o A/B do piloto mede para saber se o motor agiu e a que custo, SEM afirmar acerto
 * (acerto só é medível contra o conjunto-ouro manual). É o insumo do relatório A/B append-only.
 *
 * <p>INVARIANTES DO DOMÍNIO: {@code record} imutável, só JDK; mede ATIVIDADE, não correção —
 * {@code falasContextualizadas} = traduzidas via LLM com contexto; {@code reaproveitadasCache}
 * = servidas do cache contextual por assinatura; {@code pendentes} = caíram no fallback
 * "manter original" (alucinação de tags ou reprova na validação); {@code caracteresContextoExtra}
 * = soma de caracteres das falas vizinhas enviadas como referência (proxy de custo/VRAM, já que
 * o LM Studio pode não fornecer contagem exata de tokens).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: portador de dados puro; não valida.
 *
 * @param falasContextualizadas falas de diálogo traduzidas via chamada contextual
 * @param reaproveitadasCache falas servidas do cache contextual (assinatura batendo)
 * @param pendentes falas mantidas no original por alucinação de tags ou reprova de validação
 * @param caracteresContextoExtra soma de caracteres do contexto enviado como referência
 */
public record ResumoContextoCena(
    int falasContextualizadas,
    int reaproveitadasCache,
    int pendentes,
    long caracteresContextoExtra
) {
}
