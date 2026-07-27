package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.cachetraducao.domain.EntradaCache;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.core.presentation.ui.AnsiCores;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.raspagemRevisao.domain.ContextoRevisao;
import org.traducao.projeto.raspagemRevisao.domain.ModoReferenciaRevisao;
import org.traducao.projeto.raspagemRevisao.domain.PreparacaoReferencia;
import org.traducao.projeto.raspagemRevisao.domain.ReferenciaCacheSegura;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: descobre COM O QUE cada legenda será comparada, e recusa o arquivo quando
 * não há com o que comparar. Toda a revisão depende disto: uma fala só pode ser julgada contra o
 * original inglês certo, e comparar contra o original errado produz "correções" que degradam uma
 * tradução boa.
 *
 * <h2>Os dois modos, e por que existem</h2>
 * No modo <b>Cache</b> a referência vem SÓ do cache, com vínculo confirmado entrada a entrada —
 * sem {@code .ass} inglês irmão e sem casamento por texto não validado. No modo <b>Ambos</b>
 * (histórico) o {@code .ass} inglês manda e o cache preenche as lacunas.
 *
 * <h2>Os dois bloqueios</h2>
 * <ul>
 *   <li>Modo Cache sem cache correspondente: não há referência possível, e "revisar" seria
 *       inventar.</li>
 *   <li>Modo Cache com cache RESOLVIDO que não casa com nenhuma fala: é cache de outra obra, outro
 *       episódio ou defasado. Recusar é melhor que concluir com sucesso silencioso — foi um buraco
 *       real, em que um cache de mesmo código de episódio e obra diferente passava.</li>
 * </ul>
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>IMPRIME os próprios avisos, como o {@link AtivadorContextoRevisao} que ele chama no meio do
 *       caminho. Coletá-los para o chamador imprimir depois faria as mensagens deste serviço
 *       aparecerem FORA DE ORDEM em relação às do ativador — uma inversão que nenhum teste pegaria
 *       e que o operador leria como outra sequência de eventos.</li>
 *   <li>NÃO muta contadores: devolve deltas no resultado. Antes recebia sete {@code int[]} e
 *       incrementava índice zero.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Cache ilegível vira aviso e lista vazia, permitindo o {@code .ass} inglês servir de fallback no
 * modo Ambos. Contexto inválido ou obra incompatível propagam a exceção do ativador, antes de
 * qualquer chamada externa.
 */
@Service
public class PreparadorReferenciaRevisao {

    private final LeitorLegendaAss leitor;
    private final LeitorCacheReferenciaService leitorCache;
    private final ResolvedorArtefatosRevisao resolvedorArtefatos;
    private final FiltroAuditoriaLinha filtroAuditoria;
    private final AtivadorContextoRevisao ativadorContexto;

    /**
     * PROPÓSITO DE NEGÓCIO: reúne quem lê legenda, quem lê cache, quem pareia arquivos, quem filtra
     * linhas e quem ativa a lore.
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede a criação do preparador.
     */
    public PreparadorReferenciaRevisao(
            LeitorLegendaAss leitor,
            LeitorCacheReferenciaService leitorCache,
            ResolvedorArtefatosRevisao resolvedorArtefatos,
            FiltroAuditoriaLinha filtroAuditoria,
            AtivadorContextoRevisao ativadorContexto) {
        this.leitor = leitor;
        this.leitorCache = leitorCache;
        this.resolvedorArtefatos = resolvedorArtefatos;
        this.filtroAuditoria = filtroAuditoria;
        this.ativadorContexto = ativadorContexto;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: prepara um arquivo para revisão ou o recusa.
     *
     * <p>INVARIANTES DO DOMÍNIO: a lore é ativada DEPOIS do primeiro bloqueio e ANTES da montagem
     * das referências — bloquear primeiro evita ativar uma lore para um arquivo que não será
     * revisado, e ela precisa estar ativa para a montagem reconhecer termos canônicos.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@link PreparacaoReferencia.Bloqueada} com os
     * deltas de contagem; exceção só vem do ativador de contexto.
     *
     * @param arquivoPt legenda traduzida a revisar
     * @param pastaLegendasEn pasta onde procurar o original inglês (modo Ambos)
     * @param cacheDir raiz do cache
     * @param referencia de onde a referência pode vir
     * @param contextoFallback seleção manual da interface, quando existe
     * @return o desfecho da preparação
     */
    public PreparacaoReferencia preparar(
        Path arquivoPt,
        Path pastaLegendasEn,
        Path cacheDir,
        ModoReferenciaRevisao referencia,
        String contextoFallback
    ) {
        DocumentoLegenda documentoPt = leitor.ler(arquivoPt);

        Path cachePath = resolvedorArtefatos.resolverArquivoCache(arquivoPt, cacheDir);
        LeitorCacheReferenciaService.DocumentoReferencia cache = carregarDocumentoCache(cachePath);
        List<EntradaCache> entradasCache = cache.entradas();

        // Modo Cache: sem cache correspondente não há referência possível. O arquivo
        // fica BLOQUEADO/PENDENTE em vez de ser "revisado" com zero referência segura.
        if (referencia == ModoReferenciaRevisao.CACHE && entradasCache.isEmpty()) {
            out(AnsiCores.RED + "  [BLOQUEADO] Modo Cache: nenhum cache correspondente encontrado para "
                + arquivoPt.getFileName() + " em " + cacheDir.toAbsolutePath()
                + " (esperado algo como " + cachePath.getFileName() + "). Arquivo não revisado."
                + AnsiCores.RESET);
            return new PreparacaoReferencia.Bloqueada(1, 1);
        }

        ContextoRevisao contexto = ativadorContexto.ativar(
            cache.proveniencia(), contextoFallback, cachePath);

        Path arquivoEn;
        Map<Integer, String> originaisPorIndice;
        Map<String, String> originalPorTraduzido;
        Set<Integer> indicesSemReferenciaSegura;
        if (referencia == ModoReferenciaRevisao.CACHE) {
            // Modo "Cache": referência vem SÓ do cache, com vínculo seguro por entrada.
            // Sem .ass EN irmão e sem fallback por texto não validado.
            arquivoEn = null;
            ReferenciaCacheSegura referenciaCache = montarReferenciaCacheSegura(
                documentoPt, entradasCache, cache.proveniencia());
            originaisPorIndice = referenciaCache.originaisPorIndice();
            indicesSemReferenciaSegura = referenciaCache.semReferenciaSegura();
            originalPorTraduzido = Map.of();
        } else {
            // Modo "Ambos": .ass EN + cache preenchendo lacunas (comportamento histórico).
            arquivoEn = resolvedorArtefatos.resolverArquivoOriginal(arquivoPt, pastaLegendasEn);
            originaisPorIndice = carregarOriginaisDeLegenda(arquivoEn);
            originalPorTraduzido = indexarOriginalPorTraduzido(entradasCache);
            for (EntradaCache entrada : entradasCache) {
                if (entrada.original() != null && !entrada.original().isBlank()) {
                    originaisPorIndice.putIfAbsent(entrada.indice(), entrada.original());
                }
            }
            indicesSemReferenciaSegura = Set.of();
        }

        if (!entradasCache.isEmpty()) {
            out("  Cache carregado: " + cachePath.getFileName()
                + " (" + entradasCache.size() + " entradas EN)");
        } else {
            out(AnsiCores.YELLOW + "  Aviso: cache EN não encontrado. Procurado em: "
                + cacheDir.toAbsolutePath()
                + " (esperado algo como " + cachePath.getFileName() + ")"
                + AnsiCores.RESET);
        }
        if (arquivoEn != null && Files.isRegularFile(arquivoEn) && !originaisPorIndice.isEmpty()) {
            out("  Legenda .ass EN: " + arquivoEn.getFileName());
        }
        if (referencia == ModoReferenciaRevisao.CACHE) {
            long dialogosAuditaveis = documentoPt.eventos().stream()
                .filter(e -> e.isDialogo() && e.texto() != null && !e.texto().isBlank()
                    && !filtroAuditoria.deveIgnorar(e, e.texto()))
                .count();
            // Cache resolvido (por código de episódio, p.ex.) mas que não casa com
            // NENHUMA fala com segurança = cache de outra obra/episódio ou estale.
            // Bloqueia em vez de "concluir com sucesso" sem nenhuma referência segura.
            if (originaisPorIndice.isEmpty() && dialogosAuditaveis > 0) {
                out(AnsiCores.RED + "  [BLOQUEADO] Modo Cache: o cache resolvido (" + cachePath.getFileName()
                    + ") não corresponde com segurança a nenhuma das " + dialogosAuditaveis
                    + " fala(s) de " + arquivoPt.getFileName()
                    + " (índice/estilo/proveniência/texto divergem). Arquivo não revisado." + AnsiCores.RESET);
                return new PreparacaoReferencia.Bloqueada(1, 1);
            }
            if (!indicesSemReferenciaSegura.isEmpty()) {
                // Fala sem vínculo seguro é PENDÊNCIA: no modo Cache não há "conclusão
                // com sucesso" enquanto restar fala sem referência segura.
                out(AnsiCores.YELLOW + "  [SEM_REFERÊNCIA_SEGURA] " + indicesSemReferenciaSegura.size()
                    + " fala(s) sem vínculo seguro no cache (índice/estilo/proveniência/texto não conferem); "
                    + "marcadas como pendentes, nunca comparadas em silêncio. Índices: "
                    + indicesSemReferenciaSegura + AnsiCores.RESET);
            }
        }

        return new PreparacaoReferencia.Pronta(documentoPt, cachePath, entradasCache, contexto,
            arquivoEn, originaisPorIndice, originalPorTraduzido, indicesSemReferenciaSegura);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: no modo "Cache", monta a referência EN vindo somente do cache,
     * aceitando uma entrada como referência de uma fala apenas quando o vínculo é seguro; o resto
     * fica marcado como SEM_REFERÊNCIA_SEGURA.
     *
     * <p>INVARIANTES DO DOMÍNIO: uma entrada só vira referência se houver proveniência válida no
     * cache e ela casar com a fala em índice, estilo e texto traduzido (normalizado). Placas e
     * karaokê ({@link FiltroAuditoriaLinha#deveIgnorar}) não exigem referência e não são marcados.
     * Falas sem qualquer entrada no índice não são "inseguras" — apenas ficam sem referência.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: proveniência ausente (cache legado) torna toda fala
     * SEM_REFERÊNCIA_SEGURA, protegendo contra vínculo às cegas.
     */
    ReferenciaCacheSegura montarReferenciaCacheSegura(
        DocumentoLegenda documentoPt,
        List<EntradaCache> entradasCache,
        ProvenienciaCache proveniencia
    ) {
        Map<Integer, String> originaisPorIndice = new HashMap<>();
        Set<Integer> semReferenciaSegura = new LinkedHashSet<>();

        Map<Integer, EntradaCache> cachePorIndice = new HashMap<>();
        for (EntradaCache entrada : entradasCache) {
            cachePorIndice.putIfAbsent(entrada.indice(), entrada);
        }
        boolean provenienciaOk = proveniencia != null
            && proveniencia.contextoId() != null && !proveniencia.contextoId().isBlank();

        for (EventoLegenda evento : documentoPt.eventos()) {
            if (!evento.isDialogo() || evento.texto() == null || evento.texto().isBlank()) {
                continue;
            }
            if (filtroAuditoria.deveIgnorar(evento, evento.texto())) {
                continue;
            }
            EntradaCache entrada = cachePorIndice.get(evento.indice());
            if (entrada == null) {
                continue; // sem entrada no índice: fala apenas não referenciada, não "insegura"
            }
            boolean estiloOk = normalizarEstilo(entrada.estilo()).equals(normalizarEstilo(evento.estilo()));
            boolean originalOk = entrada.original() != null && !entrada.original().isBlank();
            // Vínculo seguro coerente com a sincronização: a fala PT atual precisa
            // corresponder à tradução do cache (já correta) OU ao original inglês do
            // cache (regrediu ao EN e será restaurada pela sincronização). Qualquer
            // outro texto indica índice deslocado / outro episódio → inseguro.
            String ptNormalizado = normalizarTexto(evento.texto());
            boolean textoOk = (entrada.traduzido() != null
                    && ptNormalizado.equals(normalizarTexto(entrada.traduzido())))
                || (originalOk && ptNormalizado.equals(normalizarTexto(entrada.original())));
            if (provenienciaOk && estiloOk && textoOk && originalOk) {
                originaisPorIndice.put(evento.indice(), entrada.original());
            } else {
                semReferenciaSegura.add(evento.indice());
            }
        }
        return new ReferenciaCacheSegura(originaisPorIndice, semReferenciaSegura);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: fornece à revisão as referências EN/PT e a proveniência produzidas
     * pelas etapas 4 e 5.
     *
     * <p>INVARIANTES DO DOMÍNIO: entradas e contexto pertencem ao mesmo documento e a leitura nunca
     * modifica o banco de cache.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: registra aviso e devolve lista vazia, permitindo usar uma
     * legenda inglesa externa como fallback.
     */
    private LeitorCacheReferenciaService.DocumentoReferencia carregarDocumentoCache(Path cachePath) {
        if (!Files.isRegularFile(cachePath)) {
            return new LeitorCacheReferenciaService.DocumentoReferencia(List.of(), null);
        }
        try {
            return leitorCache.carregarDocumento(cachePath);
        } catch (IOException e) {
            out(AnsiCores.YELLOW + "  Aviso: não foi possível ler cache "
                + cachePath.getFileName() + ": " + e.getMessage() + AnsiCores.RESET);
            return new LeitorCacheReferenciaService.DocumentoReferencia(List.of(), null);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: permite achar o original inglês a partir do texto traduzido, no modo
     * Ambos, quando o índice não basta.
     * <p>INVARIANTES DO DOMÍNIO: só entradas com os DOIS lados preenchidos entram; a primeira
     * ocorrência vence, para o mapa não depender da ordem do arquivo.
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista vazia devolve mapa vazio.
     */
    private Map<String, String> indexarOriginalPorTraduzido(List<EntradaCache> entradas) {
        Map<String, String> mapa = new HashMap<>();
        for (EntradaCache entrada : entradas) {
            if (entrada.traduzido() == null || entrada.traduzido().isBlank()) {
                continue;
            }
            if (entrada.original() == null || entrada.original().isBlank()) {
                continue;
            }
            mapa.putIfAbsent(normalizarTexto(entrada.traduzido()), entrada.original());
        }
        return mapa;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: lê a legenda inglesa irmã como fonte de original por índice.
     * <p>INVARIANTES DO DOMÍNIO: só diálogos com texto entram.
     * <p>COMPORTAMENTO EM CASO DE FALHA: arquivo ausente devolve mapa vazio; a revisão segue com o
     * que o cache oferecer.
     */
    private Map<Integer, String> carregarOriginaisDeLegenda(Path arquivoEn) {
        Map<Integer, String> mapa = new HashMap<>();
        if (!Files.isRegularFile(arquivoEn)) {
            return mapa;
        }

        DocumentoLegenda docEn = leitor.ler(arquivoEn);
        for (EventoLegenda evento : docEn.eventos()) {
            if (evento.isDialogo() && evento.texto() != null && !evento.texto().isBlank()) {
                mapa.put(evento.indice(), evento.texto());
            }
        }
        return mapa;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: compara textos ignorando espaçamento, que não muda o que se lê.
     * <p>INVARIANTES DO DOMÍNIO: cópia consciente do helper equivalente do caso de uso — é detalhe
     * DESTAS comparações, não um normalizador compartilhado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: nulo degrada para vazio.
     */
    private static String normalizarTexto(String texto) {
        return texto == null ? "" : texto.replaceAll("\\s+", " ").trim();
    }

    private static String normalizarEstilo(String estilo) {
        return estilo == null ? "" : estilo.trim().toLowerCase();
    }

    private void out(String mensagem) {
        System.out.println(mensagem);
    }
}
