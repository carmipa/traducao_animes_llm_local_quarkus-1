package org.traducao.projeto.legendasExtracao.application.strategy;

import org.traducao.projeto.legendasExtracao.domain.FaixaLegenda;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: reconhece a faixa de legenda REDUZIDA que acompanha lançamento dublado —
 * traduz só placa, cartaz e letra de música, nunca o diálogo — para que nenhuma estratégia de
 * extração a escolha no lugar da faixa completa.
 *
 * <h2>Por que é uma classe só</h2>
 * Até 08/10/2026 o filtro existia apenas na estratégia ASS (consertada em 06/08 no Break Blade).
 * As estratégias SRT e PGS escolhiam "a primeira default/eng/por": um MKV com a faixa "Signs &
 * Songs" (default + forced) antes da "Full Subtitles" extraía só os letreiros, com status de
 * sucesso (auditoria de 08/10/2026, E1, reproduzido). Uma regra, um lugar.
 *
 * <ul>
 *   <li>INVARIANTES DO DOMÍNIO: decide pelo que a faixa DECLARA — flag {@code forced} ou nome com
 *       sign/song/forced/letreiro/s&amp;s —, nunca pelo tamanho ou pela posição. Se TODAS as
 *       candidatas forem reduzidas, elas voltam (melhor uma faixa reduzida que nenhuma).</li>
 *   <li>COMPORTAMENTO EM CASO DE FALHA: nome nulo conta como vazio (a faixa continua candidata —
 *       excluir demais é o defeito pior aqui); lista nula ou vazia devolve lista vazia.</li>
 * </ul>
 */
final class FaixaDeLetreiro {

    private FaixaDeLetreiro() {
    }

    /** Diz se a faixa se declara reduzida (letreiros/músicas/forçada). */
    static boolean reconhece(FaixaLegenda faixa) {
        if (faixa.isForced()) {
            return true;
        }
        String n = faixa.nome() == null ? "" : faixa.nome().toLowerCase();
        return n.contains("sign") || n.contains("song") || n.contains("forced")
            || n.contains("letreiro") || n.contains("s&s");
    }

    /** As candidatas sem as faixas reduzidas; se só houver reduzidas, todas as candidatas. */
    static List<FaixaLegenda> semLetreiros(List<FaixaLegenda> candidatas) {
        if (candidatas == null || candidatas.isEmpty()) {
            return List.of();
        }
        List<FaixaLegenda> principais = candidatas.stream().filter(f -> !reconhece(f)).toList();
        return principais.isEmpty() ? candidatas : principais;
    }
}
