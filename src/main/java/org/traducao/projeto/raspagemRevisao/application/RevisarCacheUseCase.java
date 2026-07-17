package org.traducao.projeto.raspagemRevisao.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.traducao.application.ValidadorTraducaoService;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;
import org.traducao.projeto.traducao.domain.ports.MistralPort;
import org.traducao.projeto.cachetraducao.infrastructure.CacheManutencaoService;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.core.presentation.ui.AnsiCores;
import org.traducao.projeto.traducaoCorrige.application.ClassificadorEntradaCacheService;
import org.traducao.projeto.traducaoCorrige.application.ContextoManutencaoCacheService;
import org.traducao.projeto.traducaoCorrige.domain.EntradaAuditoriaCorrecaoCache;
import org.traducao.projeto.traducaoCorrige.domain.ResultadoManutencaoCache;
import org.traducao.projeto.traducaoCorrige.infrastructure.CorrecaoCacheAuditoria;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PROPÓSITO DE NEGÓCIO: revisa concordância, gênero e resíduos em traduções
 * válidas já persistidas, usando a lore vinculada a cada arquivo da pasta cache.
 *
 * <p>INVARIANTES DO DOMÍNIO: entradas vazias/inválidas ficam para tradução ou
 * contingência, não para revisão; uma pasta com vários animes nunca compartilha
 * a mesma lore por engano; tags, karaokê e linhas gráficas são preservados;
 * toda alteração possui backup, escrita atômica e auditoria.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: resposta ausente, alucinada ou que não
 * reduz o problema é descartada; falhas por arquivo não destroem o cache nem
 * são anunciadas como sucesso total.
 */
@Service
public class RevisarCacheUseCase {

    private static final Logger log = LoggerFactory.getLogger(RevisarCacheUseCase.class);

    private final CacheManutencaoService cacheService;
    private final ClassificadorEntradaCacheService classificador;
    private final ContextoManutencaoCacheService contextoService;
    private final DetectorConcordanciaService detector;
    private final MistralPort mistralPort;
    private final ValidadorTraducaoService validador;
    private final MascaradorTags mascaradorTags;
    private final ProtecaoLegendaAssService protecaoAss;
    private final CorrecaoCacheAuditoria auditoria;
    private final TelemetriaService telemetriaService;

    /**
     * PROPÓSITO DE NEGÓCIO: compõe revisão local, validações, persistência e dataset.
     * <p>INVARIANTES DO DOMÍNIO: nenhuma proposta LLM contorna classificador ou proteção ASS.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede criação do caso de uso.
     */
    public RevisarCacheUseCase(
        CacheManutencaoService cacheService,
        ClassificadorEntradaCacheService classificador,
        ContextoManutencaoCacheService contextoService,
        DetectorConcordanciaService detector,
        MistralPort mistralPort,
        ValidadorTraducaoService validador,
        MascaradorTags mascaradorTags,
        ProtecaoLegendaAssService protecaoAss,
        CorrecaoCacheAuditoria auditoria,
        TelemetriaService telemetriaService
    ) {
        this.cacheService = cacheService;
        this.classificador = classificador;
        this.contextoService = contextoService;
        this.detector = detector;
        this.mistralPort = mistralPort;
        this.validador = validador;
        this.mascaradorTags = mascaradorTags;
        this.protecaoAss = protecaoAss;
        this.auditoria = auditoria;
        this.telemetriaService = telemetriaService;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mantém o modo CLI compatível com caches que já
     * carregam sua proveniência.
     *
     * <p>INVARIANTES DO DOMÍNIO: cache legado não herda contexto global antigo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: legado sem contexto é contabilizado e
     * preservado sem alteração.
     */
    public ResultadoManutencaoCache executar(Path diretorioCache) {
        return executar(diretorioCache, null);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: revisa a árvore persistente usando contexto por
     * arquivo e fallback explícito somente para o formato legado.
     *
     * <p>INVARIANTES DO DOMÍNIO: processamento é determinístico e serializado;
     * cancelamento encerra no próximo ponto seguro e salva o progresso do arquivo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: continua nos demais arquivos e produz
     * status agregado fiel.
     */
    public ResultadoManutencaoCache executar(Path diretorioCache, String contextoFallback) {
        long inicioMs = System.currentTimeMillis();
        out("Iniciando revisão gramatical do cache em: " + diretorioCache.toAbsolutePath());
        CacheManutencaoService.Sessao sessao = cacheService.iniciarSessao(diretorioCache, "revisao_llm");
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
     * PROPÓSITO DE NEGÓCIO: revisa apenas falas suspeitas de um arquivo usando a
     * lore registrada em sua proveniência.
     *
     * <p>INVARIANTES DO DOMÍNIO: somente entrada classificada como válida chega
     * ao detector; uma proposta só é aplicada se passar validação e reduzir os
     * indícios; o arquivo é salvo uma vez.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: registra descarte/falha no dataset,
     * mantém o texto anterior e preserva o arquivo original.
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
            List<CandidatoRevisao> candidatos = new ArrayList<>();
            for (JsonNode no : doc.entradas()) {
                if (!(no instanceof ObjectNode entrada)) {
                    c.itensIgnorados++;
                    continue;
                }
                ClassificadorEntradaCacheService.Classificacao cls = classificador.classificar(entrada);
                if (cls.status() != ClassificadorEntradaCacheService.Status.VALIDA) continue;

                String original = ClassificadorEntradaCacheService.texto(entrada, "original");
                String traduzido = ClassificadorEntradaCacheService.texto(entrada, "traduzido");
                ResultadoDeteccaoConcordancia deteccao = detector.analisar(original, traduzido);
                if (!deteccao.suspeito()) continue;

                candidatos.add(new CandidatoRevisao(entrada, original, traduzido, deteccao));
            }

            c.itensDetectados += candidatos.size();
            out("[ARQUIVO] " + arquivo.getFileName() + " — " + candidatos.size()
                + " fala(s) suspeita(s) para revisão.");
            for (int posicao = 0; posicao < candidatos.size(); posicao++) {
                if (Thread.currentThread().isInterrupted()) {
                    c.cancelado = true;
                    break;
                }
                CandidatoRevisao candidato = candidatos.get(posicao);
                ObjectNode entrada = candidato.entrada();
                String original = candidato.original();
                String traduzido = candidato.traduzido();
                ResultadoDeteccaoConcordancia deteccao = candidato.deteccao();
                int atual = posicao + 1;
                int total = candidatos.size();
                int indice = entrada.path("indice").asInt(-1);
                String progresso = atual + "/" + total;
                String motivos = String.join("; ", deteccao.motivos());

                out(AnsiCores.CYAN + "[REVISANDO " + progresso + "] Evento " + indice
                    + " | Motivo: " + motivos + AnsiCores.RESET);
                out("  Original: " + resumirFala(original));
                out("  Tradução atual: " + resumirFala(traduzido));

                TentativaRevisao tentativa = tentarRevisar(original, traduzido, deteccao.motivos());
                if (tentativa.revisado().isEmpty()) {
                    c.itensIgnorados++;
                    c.itensPendentes++;
                    out(AnsiCores.YELLOW + "[PENDENTE " + progresso + "] Evento " + indice
                        + " | " + tentativa.detalhe() + AnsiCores.RESET);
                    auditar(arquivo, entrada, prov, contextoId, "DESCARTADA", motivos,
                        traduzido, traduzido, tentativa.detalhe());
                    continue;
                }

                String revisado = tentativa.revisado().get();
                if (revisado.equals(traduzido)) {
                    c.itensIgnorados++;
                    out(AnsiCores.CYAN + "[CONFORME " + progresso + "] Evento " + indice
                        + " | O LLM manteve a tradução atual." + AnsiCores.RESET);
                    auditar(arquivo, entrada, prov, contextoId, "CONFORME", motivos,
                        traduzido, traduzido, "LLM manteve a tradução");
                    continue;
                }
                ResultadoDeteccaoConcordancia pos = detector.analisar(original, revisado);
                if (pos.suspeito() && pos.motivos().size() >= deteccao.motivos().size()) {
                    c.itensIgnorados++;
                    c.itensPendentes++;
                    out(AnsiCores.YELLOW + "[PENDENTE " + progresso + "] Evento " + indice
                        + " | A proposta não reduziu os indícios: "
                        + String.join("; ", pos.motivos()) + AnsiCores.RESET);
                    out("  Proposta descartada: " + resumirFala(revisado));
                    auditar(arquivo, entrada, prov, contextoId, "DESCARTADA_VALIDACAO",
                        motivos, traduzido, traduzido,
                        "A proposta não reduziu os indícios de concordância");
                    continue;
                }

                entrada.put("traduzido", revisado);
                alteradas++;
                c.itensCorrigidos++;
                out(AnsiCores.GREEN + "[CORRIGIDA " + progresso + "] Evento " + indice + AnsiCores.RESET);
                out("  Antes: " + resumirFala(traduzido));
                out("  Depois: " + resumirFala(revisado));
                auditar(arquivo, entrada, prov, contextoId, "CORRIGIDA", motivos,
                    traduzido, revisado, null);
            }
            if (alteradas > 0) {
                Path backup = cacheService.salvarAtomico(doc, sessao);
                c.arquivosAlterados++;
                out(AnsiCores.GREEN + "[OK] " + arquivo.getFileName() + ": " + alteradas
                    + " revisão(ões) aplicadas; backup em " + backup + AnsiCores.RESET);
            }
        } catch (Exception e) {
            c.falhas++;
            log.error("Falha na revisão LLM do cache {}: {}", arquivo, e.getMessage());
            out(AnsiCores.RED + "[FALHA] " + arquivo.getFileName() + " | "
                + mensagemFalha(e) + AnsiCores.RESET);
            auditoria.registrar(new EntradaAuditoriaCorrecaoCache(
                Instant.now().toString(), "revisao_llm", arquivo.toAbsolutePath().toString(), -1, null,
                contextoFallback, null, null, "FALHA_ARQUIVO", e.getMessage(), null, null, null, e.toString()));
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: solicita uma correção pontual preservando os
     * marcadores ASS da tradução atual.
     *
     * <p>INVARIANTES DO DOMÍNIO: resíduos usam o prompt de correção; demais
     * problemas usam o prompt restrito de concordância; tags são restauradas e
     * validadas antes do retorno.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@link Optional#empty()} para
     * resposta ausente, tags incompatíveis, alucinação ou linha ASS suspeita.
     */
    private TentativaRevisao tentarRevisar(String original, String traduzido, List<String> motivos) {
        MascaradorTags.Mascarado mascOriginal = mascaradorTags.mascarar(original);
        MascaradorTags.Mascarado mascTraduzido = mascaradorTags.mascarar(traduzido);
        boolean temResiduo = motivos.stream().anyMatch(m -> m.contains("Resíduo gringo"));
        Optional<String> resposta = temResiduo
            ? mistralPort.corrigirTraducao(mascOriginal.texto(), mascTraduzido.texto(), String.join(", ", motivos))
            : mistralPort.revisarConcordancia(mascOriginal.texto(), mascTraduzido.texto(), motivos);
        if (resposta.isEmpty()) {
            return TentativaRevisao.pendente("O LLM não retornou uma proposta válida.");
        }
        try {
            String desmascarado = mascaradorTags.desmascarar(resposta.get(), mascTraduzido.tags());
            validador.validarFala(desmascarado);
            if (protecaoAss.respostaSuspeita(original, desmascarado)) {
                return TentativaRevisao.pendente(
                    "A proposta alterou a estrutura ou os marcadores da legenda ASS.");
            }
            return TentativaRevisao.sucesso(desmascarado);
        } catch (AlucinacaoDetectadaException e) {
            log.warn("Revisão de cache descartada por validação: {}", e.getMessage());
            return TentativaRevisao.pendente("Validação rejeitou a proposta: " + mensagemFalha(e));
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reduz uma fala a uma linha curta e legível para o
     * console operacional acompanhar a revisão sem perder o conteúdo essencial.
     *
     * <p>INVARIANTES DO DOMÍNIO: não altera o valor persistido; quebras ASS são
     * exibidas como separadores e textos longos são limitados a 220 caracteres.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto ausente é exibido explicitamente
     * como {@code <vazio>}.
     */
    private String resumirFala(String texto) {
        if (texto == null || texto.isBlank()) return "<vazio>";
        String limpo = texto.replace("\\N", " / ").replace("\\n", " / ")
            .replaceAll("\\s+", " ").strip();
        return limpo.length() <= 220 ? limpo : limpo.substring(0, 217) + "...";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: transforma exceções técnicas em explicações úteis
     * para quem acompanha a manutenção pelo console web.
     *
     * <p>INVARIANTES DO DOMÍNIO: nunca devolve texto vazio nem expõe stack trace
     * no painel; os detalhes completos continuam no log técnico.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: usa o nome da exceção quando não existe
     * uma mensagem descritiva.
     */
    private String mensagemFalha(Exception e) {
        return e.getMessage() == null || e.getMessage().isBlank()
            ? e.getClass().getSimpleName() : e.getMessage();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: persiste a decisão granular de revisão para
     * rastreabilidade e treinamento futuro dos detectores.
     *
     * <p>INVARIANTES DO DOMÍNIO: evento contém lore/proveniência e tradução
     * anterior/posterior.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: a auditoria não interrompe a revisão.
     */
    private void auditar(
        Path arquivo, ObjectNode entrada, ProvenienciaCache prov, String contextoId,
        String resultado, String motivo, String antes, String depois, String detalhe
    ) {
        auditoria.registrar(new EntradaAuditoriaCorrecaoCache(
            Instant.now().toString(), "revisao_llm", arquivo.toAbsolutePath().toString(),
            entrada.path("indice").asInt(-1), ClassificadorEntradaCacheService.texto(entrada, "estilo"), contextoId,
            prov != null ? prov.contextoHash() : null, prov != null ? prov.modeloLlm() : null,
            resultado, motivo, ClassificadorEntradaCacheService.texto(entrada, "original"), antes, depois, detalhe));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: encerra a revisão com resultado verdadeiro e
     * orientação para regenerar a legenda final.
     *
     * <p>INVARIANTES DO DOMÍNIO: falhas, descartes e cancelamento aparecem nas
     * métricas; somente correções persistidas contam como corrigidas.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve o resultado ainda que a escrita
     * do relatório operacional não esteja disponível.
     */
    private ResultadoManutencaoCache finalizar(Path pasta, long inicioMs, Contadores c) {
        ResultadoManutencaoCache r = c.resultado();
        long duracao = System.currentTimeMillis() - inicioMs;
        OperacaoTelemetria op = TelemetriaService.criarOperacao(
            "Revisão Gramatical (cache LLM)", "status=" + r.status() + "; falhas=" + r.falhas(), duracao,
            r.arquivosAnalisados(), r.itensDetectados(), r.itensCorrigidos());
        String relatorio = """
            REVISÃO GRAMATICAL DO CACHE (LLM LOCAL)
            =======================================
            Pasta: %s
            Status: %s
            Arquivos analisados: %d
            Arquivos alterados: %d
            Falas suspeitas: %d
            Falas corrigidas: %d
            Falas descartadas/conformes: %d
            Falhas: %d
            Cancelado: %s
            Falas pendentes: %d
            Observação: avance para a Opção 6; ela sincroniza o cache mais novo no ASS antes da revisão.
            """.formatted(pasta.toAbsolutePath(), r.status(), r.arquivosAnalisados(), r.arquivosAlterados(),
            r.itensDetectados(), r.itensCorrigidos(), r.itensIgnorados(), r.falhas(), r.cancelado(),
            r.itensPendentes());
        telemetriaService.finalizarOperacao(op, pasta, "revisao_gramatical_cache", relatorio);
        out("Resultado da revisão LLM: " + r.status() + " | corrigidas=" + r.itensCorrigidos()
            + " pendentes=" + r.itensPendentes() + " falhas=" + r.falhas());
        return r;
    }

    private void out(String mensagem) {
        System.out.println(mensagem);
        log.info(mensagem);
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
         * PROPÓSITO DE NEGÓCIO: congela os totais da revisão LLM do cache para
         * apresentação, relatório e telemetria.
         * <p>INVARIANTES DO DOMÍNIO: propostas inválidas permanecem pendentes.
         * <p>COMPORTAMENTO EM CASO DE FALHA: o record normaliza valores negativos.
         */
        ResultadoManutencaoCache resultado() {
            return new ResultadoManutencaoCache(arquivosAnalisados, arquivosAlterados, itensDetectados,
                itensCorrigidos, itensIgnorados, itensPendentes, falhas, cancelado);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mantém juntos os dados de uma fala já classificada
     * para apresentar progresso total antes de chamar o LLM.
     * <p>INVARIANTES DO DOMÍNIO: representa somente entrada válida e suspeita.
     * <p>COMPORTAMENTO EM CASO DE FALHA: record imutável sem efeitos colaterais.
     */
    private record CandidatoRevisao(
        ObjectNode entrada,
        String original,
        String traduzido,
        ResultadoDeteccaoConcordancia deteccao
    ) {}

    /**
     * PROPÓSITO DE NEGÓCIO: transporta uma proposta aceita ou a razão didática
     * pela qual uma fala permaneceu pendente.
     * <p>INVARIANTES DO DOMÍNIO: sucesso contém texto; pendência contém detalhe.
     * <p>COMPORTAMENTO EM CASO DE FALHA: fábricas normalizam o estado retornado.
     */
    private record TentativaRevisao(Optional<String> revisado, String detalhe) {
        static TentativaRevisao sucesso(String texto) {
            return new TentativaRevisao(Optional.of(texto), null);
        }

        static TentativaRevisao pendente(String detalhe) {
            return new TentativaRevisao(Optional.empty(), detalhe);
        }
    }
}
