package org.traducao.projeto.lore.domain;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: contrato de contexto próprio da Revisão de Lore (Opção 7) —
 * fornece o prompt de sistema da obra e o mapa de terminologia canônica usado no
 * reforço determinístico. É o sistema de contexto SEPARADO da fatia {@code contexto}
 * da tradução (que a revisão de lore não pode importar), com IDs equivalentes.
 *
 * <p>INVARIANTES DO DOMÍNIO: {@link #getId()} é único no catálogo; o mapa de correções
 * usa chave = forma-ruim em PT e valor = termo canônico a restaurar.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: implementações não fazem I/O; o mapa padrão é
 * vazio (nenhum reforço determinístico de terminologia para a obra).
 */
public interface ProvedorPromptRevisaoLore {

    String getId();

    String getNomeExibicao();

    String obterPromptSistema();

    /**
     * PROPÓSITO DE NEGÓCIO: termos de lore que o LLM tende a localizar indevidamente e
     * que a revisão deve restaurar deterministicamente na grafia oficial.
     *
     * <p>INVARIANTES DO DOMÍNIO: chave = forma-ruim em PT (ex.: "Titãs"); valor = canônico
     * (ex.: "Titans"). A restauração só ocorre quando o original EN contém o canônico.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: padrão devolve mapa vazio (sem reforço).
     */
    /**
     * PROPÓSITO DE NEGÓCIO: as traduções PT-BR que a obra ACEITA para um termo do inglês — o
     * oposto de {@link #correcoesTerminologia()}. Aquele diz "esta forma está errada, troque";
     * este diz "esta forma está CERTA, pare de acusar".
     *
     * <h2>O prejuízo que originou — medido em 17/08/2026</h2>
     * A tela 3.2 fechou o 86 com <b>543 pendências</b>, e a medição mostrou que quase nenhuma era
     * defeito: {@code Federacy}→{@code Federação} (49), {@code Republic}→{@code República} (113),
     * {@code Empire}→{@code Império} (14) e {@code Reaper}→{@code Ceifador} (9) são traduções
     * corretas e consistentes. O detector as acusava por não ter onde saber que são aceitas.
     *
     * <p>O conceito já existia no {@code DetectorTermosLoreService}, como mapa <b>hardcoded</b>
     * misturando Gundam, 86 e Macross — uma segunda cópia da lore dentro do código, contra a
     * decisão de 15/08/2026 (<i>"todas as lores devem ficar em um único arquivo"</i>). Aqui ele
     * passa a ser DADO da obra, como o resto.
     *
     * <p><b>Por que isto não vira conserto de legenda:</b> declarar equivalência não escreve nada
     * no {@code .ass}. Mapear {@code Federação → Federacy} escreveria — e seria repetir, em ~90
     * falas, o erro do {@code Ceifador → Reaper} revertido no mesmo dia.
     *
     * <p>INVARIANTES DO DOMÍNIO: chave = termo como aparece no INGLÊS, em minúsculas; valor =
     * formas PT-BR aceitas, também em minúsculas. Mapa vazio significa "esta obra não declara
     * nenhuma", nunca "aceite qualquer coisa".
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: implementação que não declara devolve mapa vazio, e o
     * detector segue com o comportamento anterior.
     */
    default Map<String, java.util.List<String>> equivalenciasAceitas() {
        return Map.of();
    }

    default Map<String, String> correcoesTerminologia() {
        return Map.of();
    }
}
