package org.traducao.projeto.raspagemRevisao.domain;

import org.traducao.projeto.cachetraducao.domain.EntradaCache;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: o desfecho da preparação de um arquivo para revisão — ou ele foi RECUSADO
 * antes de qualquer chamada externa, ou está pronto com as referências que tornam a revisão segura.
 *
 * <h2>Por que é um tipo selado e não um objeto com flag</h2>
 * Os dois desfechos carregam dados completamente diferentes: a recusa tem contadores e nada mais; a
 * aprovação tem cinco peças de estado que o laço de revisão consome. Um único record com campos
 * nulos na recusa convidaria a ler {@code originaisPorIndice} de um arquivo bloqueado — que é
 * exatamente o "revisar com zero referência segura" que os bloqueios existem para impedir. Selado,
 * o compilador cobra o tratamento dos dois casos.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Os contadores da recusa são DELTAS, não totais: quem soma é o acumulador do lote. Antes
 *       disso, a preparação incrementava {@code int[]} recebidos por parâmetro — o contorno estilo
 *       C que a FASE 4 existe para eliminar.</li>
 *   <li>Tipos só de domínio e JDK: sem framework, sem I/O.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Portador de desfecho; não lança.
 */
public sealed interface PreparacaoReferencia {

    /**
     * PROPÓSITO DE NEGÓCIO: o arquivo NÃO será revisado. Acontece quando o modo Cache não encontra
     * cache correspondente, ou encontra um que não casa com segurança com nenhuma fala — cache de
     * outra obra, outro episódio ou defasado.
     *
     * <p>INVARIANTES DO DOMÍNIO: recusar é melhor que "concluir com sucesso" sem referência. Um
     * arquivo revisado às cegas volta com correções inventadas e ninguém percebe.
     *
     * @param problemas quanto somar ao total de problemas do lote
     * @param pendentes quanto somar ao total de pendências do lote
     */
    record Bloqueada(int problemas, int pendentes) implements PreparacaoReferencia {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o arquivo pode ser revisado, e aqui está tudo que a revisão precisa
     * saber sobre ele.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code arquivoEn} é nulo no modo Cache — lá a referência vem só do
     * cache, sem {@code .ass} inglês irmão e sem fallback por texto não validado.
     *
     * @param documento a legenda PT-BR lida do disco
     * @param cachePath o cache que foi pareado com ela
     * @param entradasCache as entradas desse cache
     * @param contexto a lore ativa da obra
     * @param arquivoEn a legenda inglesa de referência, ou {@code null} no modo Cache
     * @param originaisPorIndice original inglês de cada fala, por índice
     * @param originalPorTraduzido original inglês por texto traduzido; vazio no modo Cache
     * @param indicesSemReferenciaSegura falas que ficam pendentes por vínculo não confirmado
     */
    record Pronta(
        DocumentoLegenda documento,
        Path cachePath,
        List<EntradaCache> entradasCache,
        ContextoRevisao contexto,
        Path arquivoEn,
        Map<Integer, String> originaisPorIndice,
        Map<String, String> originalPorTraduzido,
        Set<Integer> indicesSemReferenciaSegura
    ) implements PreparacaoReferencia {
    }
}
