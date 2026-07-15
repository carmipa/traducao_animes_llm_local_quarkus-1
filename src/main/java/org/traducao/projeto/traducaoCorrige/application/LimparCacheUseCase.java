package org.traducao.projeto.traducaoCorrige.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducao.infrastructure.cache.CacheManutencaoService;
import org.traducao.projeto.traducao.infrastructure.cache.ProvenienciaCache;
import org.traducao.projeto.traducao.presentation.ui.AnsiCores;
import org.traducao.projeto.traducaoCorrige.domain.EntradaAuditoriaCorrecaoCache;
import org.traducao.projeto.traducaoCorrige.domain.ResultadoManutencaoCache;
import org.traducao.projeto.traducaoCorrige.infrastructure.CorrecaoCacheAuditoria;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * PROPÓSITO DE NEGÓCIO: limpa do banco persistente apenas traduções comprovadas
 * como fallback ou inválidas, deixando-as vazias para serem refeitas pela
 * Tradução Local sem apagar nomes e termos legitimamente preservados pela lore.
 *
 * <p>INVARIANTES DO DOMÍNIO: cache versionado/legado é preservado; linhas
 * protegidas não mudam; cada arquivo alterado recebe backup e escrita atômica;
 * cache vazio já representa trabalho pendente e não é regravado.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: a falha é contabilizada e auditada por
 * arquivo, o original permanece no disco e o lote termina com status
 * {@code CONCLUIDO_COM_FALHAS}.
 */
@Service
public class LimparCacheUseCase {

    private static final Logger log = LoggerFactory.getLogger(LimparCacheUseCase.class);

    private final CacheManutencaoService cacheService;
    private final ClassificadorEntradaCacheService classificador;
    private final ContextoManutencaoCacheService contextoService;
    private final CorrecaoCacheAuditoria auditoria;
    private final TelemetriaService telemetriaService;

    /**
     * PROPÓSITO DE NEGÓCIO: compõe leitura segura, classificação, lore,
     * auditoria e telemetria da limpeza.
     * <p>INVARIANTES DO DOMÍNIO: todas as alterações passam por essas dependências canônicas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede criação do caso de uso.
     */
    public LimparCacheUseCase(
        CacheManutencaoService cacheService,
        ClassificadorEntradaCacheService classificador,
        ContextoManutencaoCacheService contextoService,
        CorrecaoCacheAuditoria auditoria,
        TelemetriaService telemetriaService
    ) {
        this.cacheService = cacheService;
        this.classificador = classificador;
        this.contextoService = contextoService;
        this.auditoria = auditoria;
        this.telemetriaService = telemetriaService;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mantém compatibilidade com o modo CLI, usando a
     * proveniência dos caches versionados e exigindo contexto apenas nos legados.
     *
     * <p>INVARIANTES DO DOMÍNIO: não inventa contexto padrão para cache legado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve resultado com falhas, sem
     * ocultá-las como zero correções.
     */
    public ResultadoManutencaoCache executar(Path diretorioCache) {
        return executar(diretorioCache, null);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: varre a pasta histórica de cache e invalida somente
     * entradas que a Tradução Local também recusaria reutilizar.
     *
     * <p>INVARIANTES DO DOMÍNIO: contexto selecionado é fallback exclusivo para
     * formato legado; proveniência por arquivo governa caches novos.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: continua nos demais arquivos, registra
     * telemetria/relatório e devolve status agregado verdadeiro.
     */
    public ResultadoManutencaoCache executar(Path diretorioCache, String contextoFallback) {
        long inicioMs = System.currentTimeMillis();
        out("Iniciando limpeza segura de cache em: " + diretorioCache.toAbsolutePath());
        CacheManutencaoService.Sessao sessao = cacheService.iniciarSessao(diretorioCache, "limpeza");
        Contadores c = new Contadores();

        if (!Files.isDirectory(diretorioCache)) {
            c.falhas++;
            out(AnsiCores.RED + "Pasta de cache não encontrada: " + diretorioCache + AnsiCores.RESET);
            return finalizar(diretorioCache, inicioMs, c);
        }

        try {
            var arquivos = cacheService.listarCachesTraducaoBase(diretorioCache);
            for (Path arquivo : arquivos) {
                if (Thread.currentThread().isInterrupted()) {
                    c.cancelado = true;
                    break;
                }
                processarArquivo(arquivo, sessao, contextoFallback, c);
            }
        } catch (IOException e) {
            c.falhas++;
            log.error("Falha ao varrer a pasta de cache {}", diretorioCache, e);
        }
        return finalizar(diretorioCache, inicioMs, c);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: limpa as entradas defeituosas de um arquivo sem
     * comprometer as demais traduções acumuladas.
     *
     * <p>INVARIANTES DO DOMÍNIO: salva uma única vez por arquivo; backup precede
     * a troca atômica; decisões relevantes entram no dataset.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: contabiliza e audita o arquivo, sem
     * propagar dano ou substituir o original.
     */
    private void processarArquivo(
        Path arquivo,
        CacheManutencaoService.Sessao sessao,
        String contextoFallback,
        Contadores c
    ) {
        c.arquivosAnalisados++;
        try {
            CacheManutencaoService.DocumentoEditavel doc = cacheService.carregar(arquivo);
            String contextoId = contextoService.ativar(doc, contextoFallback);
            ProvenienciaCache prov = doc.proveniencia();
            int alteradas = 0;
            for (JsonNode no : doc.entradas()) {
                if (Thread.currentThread().isInterrupted()) {
                    c.cancelado = true;
                    break;
                }
                if (!(no instanceof ObjectNode entrada)) {
                    c.itensIgnorados++;
                    continue;
                }
                ClassificadorEntradaCacheService.Classificacao cls = classificador.classificar(entrada);
                if (!cls.precisaCorrecao()) continue;
                c.itensDetectados++;
                String antes = ClassificadorEntradaCacheService.texto(entrada, "traduzido");
                if (cls.status() == ClassificadorEntradaCacheService.Status.VAZIA) {
                    c.itensIgnorados++;
                    auditar("limpeza", arquivo, entrada, prov, contextoId, "JA_VAZIA", cls.motivo(), antes, antes, null);
                    continue;
                }
                entrada.put("traduzido", "");
                alteradas++;
                c.itensCorrigidos++;
                auditar("limpeza", arquivo, entrada, prov, contextoId, "INVALIDADA", cls.motivo(), antes, "", null);
            }
            if (alteradas > 0) {
                Path backup = cacheService.salvarAtomico(doc, sessao);
                c.arquivosAlterados++;
                out(AnsiCores.GREEN + "[OK] " + arquivo.getFileName() + ": " + alteradas
                    + " entrada(s) invalidadas; backup em " + backup + AnsiCores.RESET);
            }
        } catch (Exception e) {
            c.falhas++;
            log.error("Falha ao limpar cache {}: {}", arquivo, e.getMessage());
            auditoria.registrar(new EntradaAuditoriaCorrecaoCache(
                Instant.now().toString(), "limpeza", arquivo.toAbsolutePath().toString(), -1, null,
                contextoFallback, null, null, "FALHA_ARQUIVO", e.getMessage(), null, null, null, e.toString()));
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: registra a transformação granular com sua
     * proveniência para auditoria e aprendizado do sistema.
     *
     * <p>INVARIANTES DO DOMÍNIO: antes/depois representam exatamente o valor
     * observado e persistido.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: a infraestrutura de auditoria absorve
     * erro de I/O sem interromper a manutenção.
     */
    private void auditar(
        String operacao, Path arquivo, ObjectNode entrada, ProvenienciaCache prov, String contextoId,
        String resultado, String motivo, String antes, String depois, String detalhe
    ) {
        auditoria.registrar(new EntradaAuditoriaCorrecaoCache(
            Instant.now().toString(), operacao, arquivo.toAbsolutePath().toString(), entrada.path("indice").asInt(-1),
            ClassificadorEntradaCacheService.texto(entrada, "estilo"), contextoId,
            prov != null ? prov.contextoHash() : null, prov != null ? prov.modeloLlm() : null,
            resultado, motivo, ClassificadorEntradaCacheService.texto(entrada, "original"), antes, depois, detalhe));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: encerra a operação com status fiel, telemetria e
     * relatório persistente dentro do projeto.
     *
     * <p>INVARIANTES DO DOMÍNIO: falhas e cancelamento aparecem no detalhe e no
     * relatório; nenhum banner de sucesso é inferido apenas por ausência de exceção.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: a telemetria trata internamente falhas
     * de relatório e o resultado ainda é devolvido ao chamador.
     */
    private ResultadoManutencaoCache finalizar(Path pasta, long inicioMs, Contadores c) {
        ResultadoManutencaoCache r = c.resultado();
        long duracao = System.currentTimeMillis() - inicioMs;
        OperacaoTelemetria op = TelemetriaService.criarOperacao(
            "Limpeza de Cache", "status=" + r.status() + "; falhas=" + r.falhas(), duracao,
            r.arquivosAnalisados(), r.itensDetectados(), r.itensCorrigidos());
        String relatorio = """
            LIMPEZA SEGURA DO CACHE
            =======================
            Pasta: %s
            Status: %s
            Arquivos analisados: %d
            Arquivos alterados: %d
            Entradas problemáticas: %d
            Entradas invalidadas: %d
            Entradas ignoradas: %d
            Falhas: %d
            Cancelado: %s
            Observação: execute novamente a Tradução Local para regenerar o ASS/SRT a partir do cache corrigido.
            """.formatted(pasta.toAbsolutePath(), r.status(), r.arquivosAnalisados(), r.arquivosAlterados(),
            r.itensDetectados(), r.itensCorrigidos(), r.itensIgnorados(), r.falhas(), r.cancelado());
        telemetriaService.finalizarOperacao(op, pasta, "limpeza_cache", relatorio);
        out("Resultado da limpeza: " + r.status() + " | arquivos=" + r.arquivosAnalisados()
            + " alterados=" + r.arquivosAlterados() + " falhas=" + r.falhas());
        return r;
    }

    private void out(String mensagem) {
        System.out.println(mensagem);
        log.info(mensagem);
    }

    /** Estado mutável restrito a uma execução síncrona na fila única. */
    private static final class Contadores {
        int arquivosAnalisados;
        int arquivosAlterados;
        int itensDetectados;
        int itensCorrigidos;
        int itensIgnorados;
        int falhas;
        boolean cancelado;

        /**
         * PROPÓSITO DE NEGÓCIO: congela os totais da limpeza; entrada já vazia é
         * estado resolvido para esta operação e não uma pendência.
         * <p>INVARIANTES DO DOMÍNIO: a limpeza sempre publica zero pendências próprias.
         * <p>COMPORTAMENTO EM CASO DE FALHA: o record normaliza valores negativos.
         */
        ResultadoManutencaoCache resultado() {
            return new ResultadoManutencaoCache(arquivosAnalisados, arquivosAlterados, itensDetectados,
                itensCorrigidos, itensIgnorados, 0, falhas, cancelado);
        }
    }
}
