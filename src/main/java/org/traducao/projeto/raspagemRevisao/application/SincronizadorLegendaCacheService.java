package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;
import org.traducao.projeto.traducao.domain.legenda.EventoLegenda;
import org.traducao.projeto.traducao.infrastructure.cache.EntradaCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: materializa no ASS/SSA as correções confirmadas pela
 * Opção 5 antes de a Opção 6 iniciar sua auditoria linguística.
 *
 * <p>INVARIANTES DO DOMÍNIO: sincroniza somente por índice existente, somente
 * tradução não vazia e nunca modifica cabeçalho, tempos, estilos ou linhas não
 * dialogadas. Uma fala que regrediu exatamente ao original EN pode ser
 * recuperada mesmo quando o timestamp do cache é anterior ao ASS.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: cache vazio devolve o documento original;
 * sem autorização temporal, somente regressões exatas ao original são reparadas.
 */
@Service
public class SincronizadorLegendaCacheService {

    /**
     * PROPÓSITO DE NEGÓCIO: transporta o documento sincronizado e os índices
     * alterados para log, métricas e decisão de persistência.
     * <p>INVARIANTES DO DOMÍNIO: índices são imutáveis e não contêm duplicatas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: record não executa efeitos colaterais.
     */
    public record Resultado(
        DocumentoLegenda documento,
        List<Integer> indicesSincronizados,
        List<Integer> indicesRecuperadosDoOriginal
    ) {
        /**
         * PROPÓSITO DE NEGÓCIO: informa o total materializado pela ponte 5→6.
         * <p>INVARIANTES DO DOMÍNIO: equivale ao tamanho da lista de índices.
         * <p>COMPORTAMENTO EM CASO DE FALHA: lista nula é proibida pela fábrica do serviço.
         */
        public int total() { return indicesSincronizados.size(); }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aplica ao documento as traduções mais recentes do
     * cache quando a comparação temporal autorizou a ponte entre módulos ou
     * quando o ASS regrediu exatamente ao original inglês registrado no cache.
     *
     * <p>INVARIANTES DO DOMÍNIO: valor vazio nunca apaga fala; valor idêntico não
     * conta como mudança; primeira entrada de um índice é a autoridade; cache
     * antigo só vence uma fala já traduzida quando ela voltou ao original EN.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: argumentos ausentes devolvem o
     * documento intacto; autorização temporal falsa restringe a operação à
     * recuperação comprovável de regressões ao original.
     */
    public Resultado sincronizar(
        DocumentoLegenda documento,
        List<EntradaCache> entradas,
        boolean autorizado
    ) {
        return sincronizar(documento, entradas, autorizado, Set.of());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: sincroniza o cache sem confundir uma fala composta
     * somente por nomes canônicos com regressão ao inglês.
     * <p>INVARIANTES DO DOMÍNIO: {@code indicesCanonicosProtegidos} só bloqueia
     * recuperação de cache antigo; cache realmente mais novo continua sendo a
     * autoridade da ponte Opção 5 → Opção 6.
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto nulo é tratado como vazio e
     * os demais contratos conservadores permanecem ativos.
     */
    public Resultado sincronizar(
        DocumentoLegenda documento,
        List<EntradaCache> entradas,
        boolean autorizado,
        Set<Integer> indicesCanonicosProtegidos
    ) {
        return sincronizar(documento, entradas, autorizado, indicesCanonicosProtegidos, null);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: sincroniza o cache limitando a escrita a uma lista de
     * índices previamente validados (modo Cache da Opção 6), impedindo que uma
     * entrada com estilo/texto/proveniência incompatível ou de outro episódio
     * sobrescreva a legenda por coincidência de índice.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code indicesPermitidos} nulo mantém o
     * comportamento histórico (todos os índices elegíveis); um conjunto restringe
     * a escrita a exatamente esses índices, mesmo que o cache seja mais novo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista de permitidos vazia não sincroniza
     * nada; argumentos ausentes devolvem o documento intacto.
     */
    public Resultado sincronizar(
        DocumentoLegenda documento,
        List<EntradaCache> entradas,
        boolean autorizado,
        Set<Integer> indicesCanonicosProtegidos,
        Set<Integer> indicesPermitidos
    ) {
        if (documento == null || entradas == null || entradas.isEmpty()) {
            return new Resultado(documento, List.of(), List.of());
        }
        Set<Integer> protegidos = indicesCanonicosProtegidos != null
            ? indicesCanonicosProtegidos
            : Set.of();
        Map<Integer, EntradaCache> porIndice = new HashMap<>();
        for (EntradaCache entrada : entradas) porIndice.putIfAbsent(entrada.indice(), entrada);

        List<EventoLegenda> atualizados = new ArrayList<>(documento.eventos().size());
        List<Integer> indices = new ArrayList<>();
        List<Integer> recuperadosDoOriginal = new ArrayList<>();
        for (EventoLegenda evento : documento.eventos()) {
            EntradaCache entrada = porIndice.get(evento.indice());
            // Modo Cache: só índices validados como vínculo seguro podem ser escritos.
            boolean permitido = indicesPermitidos == null || indicesPermitidos.contains(evento.indice());
            boolean regrediuAoOriginal = entrada != null && entrada.original() != null
                && entrada.original().equals(evento.texto())
                && !protegidos.contains(evento.indice());
            boolean podeAplicar = permitido && (autorizado || regrediuAoOriginal);
            if (podeAplicar && evento.isDialogo() && entrada != null && entrada.traduzido() != null
                && !entrada.traduzido().isBlank() && !entrada.traduzido().equals(evento.texto())) {
                atualizados.add(evento.comTexto(entrada.traduzido()));
                indices.add(evento.indice());
                if (regrediuAoOriginal) recuperadosDoOriginal.add(evento.indice());
            } else {
                atualizados.add(evento);
            }
        }
        if (indices.isEmpty()) return new Resultado(documento, List.of(), List.of());
        return new Resultado(new DocumentoLegenda(
            documento.cabecalho(), atualizados, documento.quebraDeLinha(), documento.comBom()),
            List.copyOf(indices), List.copyOf(recuperadosDoOriginal));
    }
}
