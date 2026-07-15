package org.traducao.projeto.raspagemCorrecao.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.raspagemCorrecao.infrastructure.GoogleTranslateScraper;
import org.traducao.projeto.raspagemCorrecao.infrastructure.ResultadoRaspagem;
import org.traducao.projeto.raspagemCorrecao.infrastructure.StatusRaspagem;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducao.infrastructure.cache.CacheManutencaoService;
import org.traducao.projeto.traducao.infrastructure.cache.ProvenienciaCache;
import org.traducao.projeto.traducao.presentation.ui.AnsiCores;
import org.traducao.projeto.traducaoCorrige.application.ClassificadorEntradaCacheService;
import org.traducao.projeto.traducaoCorrige.application.ContextoManutencaoCacheService;
import org.traducao.projeto.traducaoCorrige.domain.EntradaAuditoriaCorrecaoCache;
import org.traducao.projeto.traducaoCorrige.domain.ResultadoManutencaoCache;
import org.traducao.projeto.traducaoCorrige.infrastructure.CorrecaoCacheAuditoria;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * PROPÓSITO DE NEGÓCIO: preenche por contingência online as lacunas e falhas do
 * banco de tradução que a Tradução Local não pode reutilizar.
 *
 * <p>INVARIANTES DO DOMÍNIO: somente candidatos do classificador canônico são
 * enviados ao Google; nomes/termos protegidos vêm da lore do próprio cache;
 * tags e efeitos protegidos não são tocados; toda gravação tem backup e troca
 * atômica.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: falhas de rede permanecem pendentes no
 * cache, são auditadas e não impedem salvar correções válidas já obtidas.
 */
@Service
public class CorrigirComGoogleUseCase {

    private static final Logger log = LoggerFactory.getLogger(CorrigirComGoogleUseCase.class);

    private final CacheManutencaoService cacheService;
    private final ClassificadorEntradaCacheService classificador;
    private final ContextoManutencaoCacheService contextoService;
    private final ProtetorTermosLoreService protetorLore;
    private final GoogleTranslateScraper googleScraper;
    private final CorrecaoCacheAuditoria auditoria;
    private final TelemetriaService telemetriaService;

    /**
     * PROPÓSITO DE NEGÓCIO: compõe o reparo online com as proteções do cache.
     * <p>INVARIANTES DO DOMÍNIO: rede, lore, auditoria e persistência ficam explícitas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede criação do caso de uso.
     */
    public CorrigirComGoogleUseCase(
        CacheManutencaoService cacheService,
        ClassificadorEntradaCacheService classificador,
        ContextoManutencaoCacheService contextoService,
        ProtetorTermosLoreService protetorLore,
        GoogleTranslateScraper googleScraper,
        CorrecaoCacheAuditoria auditoria,
        TelemetriaService telemetriaService
    ) {
        this.cacheService = cacheService;
        this.classificador = classificador;
        this.contextoService = contextoService;
        this.protetorLore = protetorLore;
        this.googleScraper = googleScraper;
        this.auditoria = auditoria;
        this.telemetriaService = telemetriaService;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mantém o modo CLI compatível com caches versionados.
     *
     * <p>INVARIANTES DO DOMÍNIO: cache legado sem contexto não é processado com
     * uma lore arbitrária.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve resultado agregado com falha.
     */
    public ResultadoManutencaoCache executar(Path diretorioCache) {
        return executar(diretorioCache, null);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: corrige todas as entradas pendentes da pasta cache,
     * incluindo as que já estavam vazias após uma limpeza anterior.
     *
     * <p>INVARIANTES DO DOMÍNIO: proveniência por arquivo prevalece sobre o
     * fallback da interface; arquivos são processados deterministicamente.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: continua nos demais arquivos e registra
     * status real em relatório, auditoria e telemetria.
     */
    public ResultadoManutencaoCache executar(Path diretorioCache, String contextoFallback) {
        long inicioMs = System.currentTimeMillis();
        out("Iniciando correção online do cache via Google Translate: " + diretorioCache.toAbsolutePath());
        CacheManutencaoService.Sessao sessao = cacheService.iniciarSessao(diretorioCache, "google");
        Contadores c = new Contadores();
        if (!Files.isDirectory(diretorioCache)) {
            c.falhas++;
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
     * PROPÓSITO DE NEGÓCIO: repara candidatos de um arquivo preservando as
     * correções anteriores quando uma chamada posterior falha ou é cancelada.
     *
     * <p>INVARIANTES DO DOMÍNIO: resposta só substitui a entrada com status
     * {@code SUCESSO}; idioma de destino é normalizado para pt-br.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: audita o motivo, mantém o valor anterior
     * e salva atomicamente qualquer progresso válido do mesmo arquivo.
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
            int atual = 0;
            int totalCandidatos = contarCandidatos(doc);
            Path backup = null;
            out("[ARQUIVO] " + arquivo.getFileName() + " — " + totalCandidatos
                + " entrada(s) pendente(s) para correção.");
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
                atual++;
                c.itensDetectados++;
                String original = ClassificadorEntradaCacheService.texto(entrada, "original");
                String antes = ClassificadorEntradaCacheService.texto(entrada, "traduzido");
                int indice = entrada.path("indice").asInt(-1);
                out("[CORRIGINDO " + atual + "/" + totalCandidatos + "] Evento " + indice
                    + " — " + resumirParaPainel(original));

                ProtetorTermosLoreService.TextoProtegido protegido = protetorLore.mascarar(
                    original, contextoService.loreAtiva(), contextoService.termosProtegidosAtivos());
                ResultadoRaspagem resposta = googleScraper.traduzir(protegido.textoMascarado());
                if (resposta.status() == StatusRaspagem.SUCESSO) {
                    String restaurado = protetorLore.restaurar(resposta.texto(), protegido);
                    if (restaurado == null) {
                        c.itensIgnorados++;
                        c.itensPendentes++;
                        auditar(arquivo, entrada, prov, contextoId, "NAO_CORRIGIDA", cls.motivo(), antes,
                            antes, "TERMO_LORE_CORROMPIDO");
                        out(AnsiCores.YELLOW + "[PENDENTE " + atual + "/" + totalCandidatos
                            + "] Evento " + indice + " — termo da lore não pôde ser restaurado."
                            + AnsiCores.RESET);
                        continue;
                    }
                    entrada.put("traduzido", restaurado);
                    entrada.put("idiomaTraduzido", "pt-br");
                    // Cada resposta válida vira checkpoint antes de ser anunciada
                    // na auditoria. Uma interrupção perde no máximo a chamada em curso.
                    backup = cacheService.salvarAtomico(doc, sessao);
                    if (alteradas == 0) {
                        c.arquivosAlterados++;
                    }
                    alteradas++;
                    c.itensCorrigidos++;
                    auditar(arquivo, entrada, prov, contextoId, "CORRIGIDA", cls.motivo(), antes,
                        restaurado, resposta.status().name());
                    out(AnsiCores.GREEN + "[CORRIGIDA " + atual + "/" + totalCandidatos
                        + "] Evento " + indice + " — " + resumirParaPainel(restaurado)
                        + AnsiCores.RESET);
                } else {
                    c.itensIgnorados++;
                    c.itensPendentes++;
                    auditar(arquivo, entrada, prov, contextoId, "NAO_CORRIGIDA", cls.motivo(), antes,
                        antes, resposta.status().name());
                    out(AnsiCores.YELLOW + "[PENDENTE " + atual + "/" + totalCandidatos
                        + "] Evento " + indice + " — " + resposta.status().name()
                        + AnsiCores.RESET);
                }

                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    c.cancelado = true;
                }
            }
            if (alteradas > 0) {
                out(AnsiCores.GREEN + "[OK] " + arquivo.getFileName() + ": " + alteradas
                    + " entrada(s) corrigidas; backup em " + backup + AnsiCores.RESET);
            }
        } catch (Exception e) {
            c.falhas++;
            log.error("Falha na correção Google do cache {}: {}", arquivo, e.getMessage());
            auditoria.registrar(new EntradaAuditoriaCorrecaoCache(
                Instant.now().toString(), "google", arquivo.toAbsolutePath().toString(), -1, null,
                contextoFallback, null, null, "FALHA_ARQUIVO", e.getMessage(), null, null, null, e.toString()));
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: acrescenta ao dataset a decisão da contingência
     * online, incluindo falhas e respostas sem alteração.
     *
     * <p>INVARIANTES DO DOMÍNIO: proveniência original e antes/depois são
     * preservados no evento.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: auditoria emite warning sem interromper.
     */
    private void auditar(
        Path arquivo, ObjectNode entrada, ProvenienciaCache prov, String contextoId,
        String resultado, String motivo, String antes, String depois, String detalhe
    ) {
        auditoria.registrar(new EntradaAuditoriaCorrecaoCache(
            Instant.now().toString(), "google", arquivo.toAbsolutePath().toString(), entrada.path("indice").asInt(-1),
            ClassificadorEntradaCacheService.texto(entrada, "estilo"), contextoId,
            prov != null ? prov.contextoHash() : null, prov != null ? prov.modeloLlm() : null,
            resultado, motivo, ClassificadorEntradaCacheService.texto(entrada, "original"), antes, depois, detalhe));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: calcula o denominador exibido no progresso do painel
     * antes de iniciar chamadas demoradas ao Google.
     *
     * <p>INVARIANTES DO DOMÍNIO: usa o mesmo classificador da execução real e
     * conta somente entradas objeto que exigem correção.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nós desconhecidos são ignorados e não
     * interrompem o arquivo.
     */
    private int contarCandidatos(CacheManutencaoService.DocumentoEditavel doc) {
        int total = 0;
        for (JsonNode no : doc.entradas()) {
            if (no instanceof ObjectNode entrada && classificador.classificar(entrada).precisaCorrecao()) {
                total++;
            }
        }
        return total;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mantém o painel legível enquanto revela qual fala
     * está sendo processada ou acabou de ser corrigida.
     *
     * <p>INVARIANTES DO DOMÍNIO: quebras ASS aparecem como espaço e a prévia tem
     * no máximo 180 caracteres, sem alterar o conteúdo persistido.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo é representado por marcador
     * explícito em vez de provocar exceção.
     */
    private String resumirParaPainel(String texto) {
        if (texto == null) return "<texto ausente>";
        String limpo = texto.replace("\\N", " ").replaceAll("\\s+", " ").strip();
        return limpo.length() <= 180 ? limpo : limpo.substring(0, 177) + "...";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: finaliza a contingência com métricas confiáveis.
     *
     * <p>INVARIANTES DO DOMÍNIO: status e falhas constam na telemetria e no
     * relatório; correções contam somente respostas aplicadas.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve o resultado mesmo se o relatório
     * não puder ser persistido.
     */
    private ResultadoManutencaoCache finalizar(Path pasta, long inicioMs, Contadores c) {
        ResultadoManutencaoCache r = c.resultado();
        long duracao = System.currentTimeMillis() - inicioMs;
        OperacaoTelemetria op = TelemetriaService.criarOperacao(
            "Correção Google (cache)", "status=" + r.status() + "; pendentes="
                + r.itensPendentes() + "; falhas=" + r.falhas(), duracao,
            r.arquivosAnalisados(), r.itensDetectados(), r.itensCorrigidos());
        String relatorio = """
            CORREÇÃO ONLINE DO CACHE VIA GOOGLE
            ===================================
            Pasta: %s
            Status: %s
            Arquivos analisados: %d
            Arquivos alterados: %d
            Entradas pendentes: %d
            Entradas corrigidas: %d
            Entradas pendentes: %d
            Entradas não corrigidas/ignoradas: %d
            Falhas: %d
            Cancelado: %s
            Observação: execute novamente a Tradução Local para regenerar o ASS/SRT a partir do cache corrigido.
            """.formatted(pasta.toAbsolutePath(), r.status(), r.arquivosAnalisados(), r.arquivosAlterados(),
            r.itensDetectados(), r.itensCorrigidos(), r.itensPendentes(), r.itensIgnorados(),
            r.falhas(), r.cancelado());
        telemetriaService.finalizarOperacao(op, pasta, "correcao_google_cache", relatorio);
        out("Resultado da correção Google: " + r.status() + " | corrigidas=" + r.itensCorrigidos()
            + " pendentes=" + r.itensPendentes() + " falhas=" + r.falhas());
        return r;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: envia uma única linha operacional ao terminal e ao
     * SSE do painel de correção por meio do redirecionador global.
     *
     * <p>INVARIANTES DO DOMÍNIO: não replica a mesma mensagem no SLF4J, evitando
     * duplicidade visual e no arquivo {@code console-web.log}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code System.out} mantém o comportamento
     * padrão do runtime; não há dependência direta de cliente SSE conectado.
     */
    private void out(String mensagem) {
        System.out.println(mensagem);
    }

    /** Estado mutável restrito à execução serializada na fila do pipeline. */
    private static final class Contadores {
        int arquivosAnalisados;
        int arquivosAlterados;
        int itensDetectados;
        int itensCorrigidos;
        int itensIgnorados;
        int itensPendentes;
        int falhas;
        boolean cancelado;

        /**
         * PROPÓSITO DE NEGÓCIO: congela os totais da contingência Google no
         * contrato agregado exibido pela interface.
         * <p>INVARIANTES DO DOMÍNIO: pendências são independentes de falhas técnicas.
         * <p>COMPORTAMENTO EM CASO DE FALHA: o record normaliza valores negativos.
         */
        ResultadoManutencaoCache resultado() {
            return new ResultadoManutencaoCache(arquivosAnalisados, arquivosAlterados, itensDetectados,
                itensCorrigidos, itensIgnorados, itensPendentes, falhas, cancelado);
        }
    }
}
