package org.traducao.projeto.traducaoKaraoke.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.core.util.DuracaoUtil;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.llm.domain.Lote;
import org.traducao.projeto.llm.domain.StatusLlm;
import org.traducao.projeto.llm.domain.TraducaoLote;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.llm.domain.LlmPort;
import org.traducao.projeto.cachetraducao.infrastructure.CacheTraducaoService;
import org.traducao.projeto.cachetraducao.domain.EntradaCache;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.core.presentation.web.LogStreamService;
import org.traducao.projeto.traducaoKaraoke.domain.ClasseLinhaKaraoke;
import org.traducao.projeto.traducaoKaraoke.domain.ResultadoTraducaoKaraoke;
import org.traducao.projeto.traducaoKaraoke.domain.TraducaoKaraokeException;
import org.traducao.projeto.traducaoKaraoke.infrastructure.TraducaoKaraokePersistencia;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Traduz as LETRAS DE MÚSICA de legendas .ass para PT-BR mantendo a letra
 * original junto na tela: a camada japonesa/romaji é preservada intacta
 * (mesmo com inglês misturado pelo cantor) e apenas a camada de tradução em
 * inglês vai ao LLM — resultado final: romaji em cima, PT-BR embaixo, nos
 * mesmos tempos do arquivo original.
 * <p>
 * Garantias (mesmo contrato do Karaokê Simples):
 * <ul>
 *   <li>Os arquivos de entrada NUNCA são alterados — a saída vai para a pasta
 *       irmã {@code <entrada>-karaoke-ptbr}, com o mesmo nome de arquivo.</li>
 *   <li>Diálogo/placas/efeitos KFX são reemitidos sem nenhuma alteração.</li>
 *   <li>Falha ou alucinação do LLM numa linha mantém a linha original e
 *       sinaliza aviso — nunca derruba o arquivo inteiro.</li>
 *   <li>Traduções ficam em cache JSON por arquivo (editável manualmente),
 *       reaproveitado em reexecuções, no mesmo padrão da Tradução Local.</li>
 * </ul>
 */
@ApplicationScoped
public class TraduzirKaraokeUseCase {

    private static final Logger log = LoggerFactory.getLogger(TraduzirKaraokeUseCase.class);

    public static final String CANAL_LOG = "traducao-karaoke";
    static final String SUFIXO_PASTA_SAIDA = "-karaoke-ptbr";
    private static final String SUBPASTA_CACHE = "karaoke";

    @Inject
    LeitorLegendaAss leitor;

    @Inject
    EscritorLegendaAss escritor;

    @Inject
    MascaradorTags mascarador;

    @Inject
    ValidadorTraducaoService validador;

    @Inject
    CacheTraducaoService cacheService;

    @Inject
    LlmPort llmPort;

    @Inject
    GerenciadorContexto gerenciadorContexto;

    @Inject
    ClassificadorLetraKaraokeService classificador;

    @Inject
    LogStreamService logStream;

    @Inject
    TelemetriaService telemetriaService;

    @Inject
    TraducaoKaraokePersistencia persistencia;

    @ConfigProperty(name = "tradutor.idioma-original")
    Optional<String> idiomaOriginal;

    @ConfigProperty(name = "tradutor.idioma-traduzido")
    Optional<String> idiomaTraduzido;

    // E3b/Opção A: ausência e vazio colapsam em "cache"; branco de idioma cai no default via filtro isBlank.
    @ConfigProperty(name = "tradutor.diretorio-cache")
    Optional<String> diretorioCache;

    public List<ResultadoTraducaoKaraoke> simular(Path pastaOrigem, String contextoId) {
        return executar(pastaOrigem, contextoId, false);
    }

    public List<ResultadoTraducaoKaraoke> aplicar(Path pastaOrigem, String contextoId) {
        return executar(pastaOrigem, contextoId, true);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: motor único da tradução de karaokê, compartilhado por
     * {@code simular} (dry-run, read-only) e {@code aplicar} (grava e chama o LLM) — lista as
     * legendas, classifica cada linha e produz o resumo por arquivo.
     *
     * <p>INVARIANTES DO DOMÍNIO: só o modo {@code gravar} verifica o LLM, ativa o contexto
     * (lore) e escreve saída/cache; a simulação não toca estado global nem a GPU/LLM. O
     * contador de lotes ({@code sequencialLote}) é local a esta execução, nunca campo de
     * instância, para não ser perturbado por uma execução concorrente deste bean singleton.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: pasta inexistente lança
     * {@link TraducaoKaraokeException}; falha por arquivo é contabilizada e não interrompe os
     * demais; interrupção cooperativa entre arquivos preserva o que já foi gravado.
     */
    private List<ResultadoTraducaoKaraoke> executar(Path pastaOrigem, String contextoId, boolean gravar) {
        long inicioMs = System.currentTimeMillis();
        String modo = gravar ? "Tradução" : "Simulação (Dry-Run)";
        logStream.publicarLog(CANAL_LOG, "==== Tradução de Karaokê — " + modo + " ====");
        logStream.publicarLog(CANAL_LOG, "Pasta das legendas: " + pastaOrigem);

        if (pastaOrigem == null || !Files.isDirectory(pastaOrigem)) {
            throw new TraducaoKaraokeException("A pasta informada não existe ou não é um diretório: " + pastaOrigem);
        }
        Path pastaDestino = resolverPastaSaida(pastaOrigem);
        logStream.publicarLog(CANAL_LOG, "Pasta de destino (criada automaticamente): " + pastaDestino);
        if (gravar) {
            try {
                Files.createDirectories(pastaDestino);
            } catch (IOException e) {
                throw new TraducaoKaraokeException("Não foi possível criar a pasta de destino: " + pastaDestino, e);
            }
        }

        List<Path> arquivos = listarLegendas(pastaOrigem);
        if (arquivos.isEmpty()) {
            logStream.publicarLog(CANAL_LOG, "Nenhum arquivo .ass/.ssa encontrado na pasta.");
            return List.of();
        }
        logStream.publicarLog(CANAL_LOG, "Arquivos de legenda encontrados: " + arquivos.size());

        if (gravar) {
            StatusLlm status = llmPort.verificarDisponibilidade();
            if (status == null || !status.modeloCarregado()) {
                throw new TraducaoKaraokeException("Servidor LLM indisponível: "
                    + (status != null ? status.mensagem() : "sem resposta"));
            }
            logStream.publicarLog(CANAL_LOG, "[OK] Servidor LLM ativo.");
            ativarContexto(contextoId);
        }

        // Contador de lotes local à execução: um use case @ApplicationScoped é
        // singleton, então um campo de instância seria compartilhado entre
        // execuções concorrentes (simular fora da fila + aplicar na fila).
        AtomicInteger sequencialLote = new AtomicInteger();
        List<ResultadoTraducaoKaraoke> resultados = new ArrayList<>();
        int falhas = 0;
        for (Path arquivo : arquivos) {
            if (Thread.currentThread().isInterrupted()) {
                logStream.publicarLog(CANAL_LOG, "[INTERROMPIDO] Execução cancelada; arquivos já gravados foram preservados.");
                break;
            }
            try {
                resultados.add(processarArquivo(arquivo, pastaDestino, gravar, sequencialLote));
            } catch (Exception e) {
                falhas++;
                log.error("Falha ao processar {}", arquivo, e);
                logStream.publicarLog(CANAL_LOG, "[ERRO] " + arquivo.getFileName() + ": " + e.getMessage());
            }
        }

        long duracaoMs = System.currentTimeMillis() - inicioMs;
        int musicas = resultados.stream().mapToInt(ResultadoTraducaoKaraoke::paraTraduzir).sum();
        int traduzidas = resultados.stream().mapToInt(ResultadoTraducaoKaraoke::traduzidas).sum();
        int doCache = resultados.stream().mapToInt(ResultadoTraducaoKaraoke::reaproveitadasCache).sum();
        int preservadas = resultados.stream().mapToInt(ResultadoTraducaoKaraoke::preservadasOriginalJapones).sum();

        logStream.publicarLog(CANAL_LOG, "==============================================================");
        logStream.publicarLog(CANAL_LOG, String.format(Locale.ROOT,
            "[%s] %s concluída: %d arquivo(s), %d falha(s) | letras originais preservadas: %d | traduzíveis: %d (LLM: %d, cache: %d)",
            falhas == 0 ? "SUCESSO" : "ATENÇÃO", modo, resultados.size(), falhas, preservadas, musicas, traduzidas, doCache));
        logStream.publicarLog(CANAL_LOG, "==============================================================");
        logStream.publicarLog(CANAL_LOG, DuracaoUtil.linhaRelatorioFinal(
            gravar ? "Tradução de Karaokê (LLM)" : "Tradução de Karaokê (simulação)", inicioMs));

        if (gravar && !resultados.isEmpty()) {
            registrarArtefatos(pastaOrigem, pastaDestino, resultados, duracaoMs, musicas, traduzidas + doCache);
        }
        return resultados;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: processa uma legenda .ass — preserva letra original/japonesa e
     * KFX, reaproveita cache e (só em modo gravar) traduz a camada inglesa via LLM, montando
     * o resumo de classificação do arquivo.
     *
     * <p>INVARIANTES DO DOMÍNIO: o arquivo de entrada nunca é alterado; falas repetidas gastam
     * uma só tradução (dedup por texto original); em dry-run nenhuma linha vai ao LLM e nada é
     * gravado. O {@code sequencialLote} recebido é o contador local da execução, repassado ao
     * LLM para numerar os lotes desta run.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: falha/alucinação do LLM numa linha mantém o texto
     * original e registra aviso, sem derrubar o arquivo; interrupção cooperativa encerra no
     * próximo ponto seguro preservando o que já foi resolvido.
     */
    private ResultadoTraducaoKaraoke processarArquivo(Path arquivo, Path pastaDestino, boolean gravar,
                                                      AtomicInteger sequencialLote) {
        String nome = arquivo.getFileName().toString();
        logStream.publicarLog(CANAL_LOG, "");
        logStream.publicarLog(CANAL_LOG, ">> " + nome);

        DocumentoLegenda documento = leitor.ler(arquivo);
        Path arquivoCache = resolverArquivoCache(arquivo);
        Map<String, String> cacheExistente = cacheService.carregar(arquivoCache);

        int kfx = 0;
        int originais = 0;
        int jaPt = 0;
        int paraTraduzir = 0;
        int doCache = 0;
        int traduzidas = 0;
        int semTraducao = 0;
        List<String> avisos = new ArrayList<>();

        // Traduções desta execução, deduplicadas por texto original (refrão
        // repetido gasta uma chamada de LLM só).
        Map<String, String> traducoes = new HashMap<>();
        List<EventoLegenda> eventosFinais = new ArrayList<>(documento.eventos().size());

        for (EventoLegenda evento : documento.eventos()) {
            ClasseLinhaKaraoke classe = evento.isDialogo() && evento.temTexto()
                ? classificador.classificar(evento.estilo(), evento.texto())
                : ClasseLinhaKaraoke.FORA_DE_MUSICA;

            switch (classe) {
                case FORA_DE_MUSICA -> eventosFinais.add(evento);
                case EFEITO_KFX -> {
                    kfx++;
                    eventosFinais.add(evento);
                }
                case ORIGINAL_JAPONES -> {
                    originais++;
                    eventosFinais.add(evento);
                    logStream.publicarLog(CANAL_LOG, "   [MÚSICA-JP] mantida: " + visivelResumido(evento.texto()));
                }
                case JA_PORTUGUES -> {
                    jaPt++;
                    eventosFinais.add(evento);
                    logStream.publicarLog(CANAL_LOG, "   [MÚSICA-PT] já traduzida: " + visivelResumido(evento.texto()));
                }
                case TRADUZIVEL_INGLES -> {
                    paraTraduzir++;
                    String original = evento.texto();
                    String traduzido = traducoes.get(original);
                    boolean veioDoCache = false;
                    if (traduzido == null) {
                        String cacheado = cacheExistente.get(original);
                        if (cacheado != null && !cacheado.isBlank()) {
                            traduzido = cacheado;
                            veioDoCache = true;
                        } else if (gravar) {
                            if (Thread.currentThread().isInterrupted()) {
                                eventosFinais.add(evento);
                                semTraducao++;
                                continue;
                            }
                            traduzido = traduzirViaLlm(original, avisos, sequencialLote);
                        }
                    }
                    if (traduzido == null) {
                        eventosFinais.add(evento);
                        if (gravar) {
                            semTraducao++;
                        } else {
                            logStream.publicarLog(CANAL_LOG, "   [MÚSICA-EN → LLM] será traduzida: " + visivelResumido(original));
                        }
                        continue;
                    }
                    traducoes.put(original, traduzido);
                    eventosFinais.add(evento.comTexto(traduzido));
                    if (veioDoCache) {
                        doCache++;
                        logStream.publicarLog(CANAL_LOG, "   [CACHE] reaproveitada: " + visivelResumido(traduzido));
                    } else {
                        traduzidas++;
                        logStream.publicarLog(CANAL_LOG, "   [LLM] " + visivelResumido(original)
                            + "  =>  " + visivelResumido(traduzido));
                    }
                }
            }
        }

        String nomeDestino = null;
        if (gravar) {
            Path destino = pastaDestino.resolve(nome);
            escritor.escrever(destino, new DocumentoLegenda(
                documento.cabecalho(), eventosFinais, documento.quebraDeLinha(), documento.comBom()));
            nomeDestino = destino.toString();
            salvarCache(arquivoCache, documento, traducoes);
            logStream.publicarLog(CANAL_LOG, "   [GRAVADO] " + destino);
        }

        logStream.publicarLog(CANAL_LOG, String.format(Locale.ROOT,
            "   Resumo: %d evento(s) | KFX preservado: %d | letra original: %d | já PT: %d | traduzível: %d (LLM %d, cache %d, sem tradução %d)",
            documento.eventos().size(), kfx, originais, jaPt, paraTraduzir, traduzidas, doCache, semTraducao));

        return new ResultadoTraducaoKaraoke(
            nome, nomeDestino, documento.eventos().size(), kfx, originais, jaPt,
            paraTraduzir, doCache, traduzidas, semTraducao, List.copyOf(avisos));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: traduz uma única linha de letra via LLM (uma linha por lote — a
     * letra é curta e o lote unitário é o padrão do projeto), mascarando as tags antes e
     * restaurando-as depois.
     *
     * <p>INVARIANTES DO DOMÍNIO: o {@code sequencialLote} é o contador LOCAL da execução (ver
     * {@link #executar}), incrementado atomicamente para numerar o lote; nunca é campo de
     * instância, evitando estado compartilhado entre execuções concorrentes deste bean
     * singleton. A saída passa por desmascaramento e validação antes de ser aceita.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: falha de comunicação, resposta inválida ou
     * {@link AlucinacaoDetectadaException} devolve {@code null} (mantém a linha original) e
     * registra um aviso — nunca propaga para derrubar o arquivo.
     */
    private String traduzirViaLlm(String original, List<String> avisos, AtomicInteger sequencialLote) {
        MascaradorTags.Mascarado mascarado = mascarador.mascarar(original);
        TraducaoLote resposta;
        try {
            resposta = llmPort.traduzir(new Lote(sequencialLote.incrementAndGet(), List.of(mascarado.texto())));
        } catch (Exception e) {
            avisos.add("Falha de comunicação com o LLM; linha mantida sem tradução: " + original);
            logStream.publicarLog(CANAL_LOG, "   [AVISO] LLM falhou nesta linha (mantida no idioma original): " + e.getMessage());
            return null;
        }
        if (resposta == null || !resposta.sucesso()
            || resposta.linhasTraduzidas() == null || resposta.linhasTraduzidas().isEmpty()) {
            avisos.add("LLM não retornou tradução; linha mantida: " + original);
            logStream.publicarLog(CANAL_LOG, "   [AVISO] LLM sem resposta válida — linha mantida sem tradução.");
            return null;
        }
        try {
            String traduzido = mascarador.desmascarar(resposta.linhasTraduzidas().getFirst(), mascarado.tags());
            validador.validarFala(traduzido);
            return traduzido;
        } catch (AlucinacaoDetectadaException e) {
            telemetriaService.registrarAlucinacaoPrevenida();
            avisos.add("Alucinação detectada (" + e.getMessage() + "); linha mantida: " + original);
            logStream.publicarLog(CANAL_LOG, "   [AVISO] Alucinação interceptada — linha mantida sem tradução: "
                + visivelResumido(original));
            return null;
        }
    }

    /**
     * Persiste TODAS as traduções aplicadas (novas e reaproveitadas) no cache
     * do arquivo, preservando o fluxo de correção manual: o usuário edita o
     * JSON e a reexecução respeita a edição.
     */
    private void salvarCache(Path arquivoCache, DocumentoLegenda documento, Map<String, String> traducoes) {
        if (traducoes.isEmpty()) {
            return;
        }
        Map<String, EntradaCache> porOriginal = new LinkedHashMap<>();
        for (EventoLegenda evento : documento.eventos()) {
            String traduzido = evento.temTexto() ? traducoes.get(evento.texto()) : null;
            if (traduzido != null) {
                porOriginal.putIfAbsent(evento.texto(), new EntradaCache(
                    evento.indice(), evento.estilo(), evento.texto(), traduzido,
                    idiomaOriginal.filter(s -> !s.isBlank()).orElse("en"),
                    idiomaTraduzido.filter(s -> !s.isBlank()).orElse("pt-br")));
            }
        }
        cacheService.salvar(arquivoCache, new ArrayList<>(porOriginal.values()));
    }

    private void registrarArtefatos(
        Path pastaOrigem,
        Path pastaDestino,
        List<ResultadoTraducaoKaraoke> resultados,
        long duracaoMs,
        int detectadas,
        int corrigidas
    ) {
        try {
            Path manifesto = persistencia.salvarManifesto(pastaOrigem, pastaDestino, resultados, duracaoMs);
            if (manifesto != null) {
                logStream.publicarLog(CANAL_LOG, "Manifesto de auditoria salvo em: " + manifesto);
            }
        } catch (IOException e) {
            log.warn("Falha ao salvar manifesto da tradução de karaokê: {}", e.getMessage());
        }
        telemetriaService.finalizarOperacao(
            TelemetriaService.criarOperacao(
                "Tradução de Karaokê (LLM)",
                "Legendas: " + pastaOrigem + " → " + pastaDestino,
                duracaoMs,
                resultados.size(),
                detectadas,
                corrigidas),
            pastaOrigem,
            "traducao_karaoke",
            montarRelatorio(pastaOrigem, pastaDestino, resultados, duracaoMs));
    }

    private String montarRelatorio(
        Path pastaOrigem, Path pastaDestino, List<ResultadoTraducaoKaraoke> resultados, long duracaoMs) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tradução de Karaokê — letras originais preservadas + tradução PT-BR\n");
        sb.append("Origem: ").append(pastaOrigem.toAbsolutePath()).append('\n');
        sb.append("Destino: ").append(pastaDestino.toAbsolutePath()).append('\n');
        sb.append("Duração: ").append(duracaoMs).append(" ms\n\n");
        for (ResultadoTraducaoKaraoke r : resultados) {
            sb.append(r.arquivo())
                .append(" | letra original: ").append(r.preservadasOriginalJapones())
                .append(" | traduzidas (LLM): ").append(r.traduzidas())
                .append(" | cache: ").append(r.reaproveitadasCache())
                .append(" | sem tradução: ").append(r.mantidasSemTraducao())
                .append(" | avisos: ").append(r.avisos().size())
                .append('\n');
        }
        return sb.toString();
    }

    /**
     * O contexto de lore ativo é estado global lido pelo adapter do LLM ao
     * montar o prompt — por isso este use case só roda pela fila do pipeline.
     * (Nulo em testes unitários, onde não há CDI nem chamada real de LLM.)
     */
    private void ativarContexto(String contextoId) {
        if (gerenciadorContexto == null) {
            return;
        }
        gerenciadorContexto.definirContextoAtivo(contextoId);
        logStream.publicarLog(CANAL_LOG, "[CONTEXTO] Obra ativa: " + gerenciadorContexto.obterNomeContextoAtivo());
    }

    private List<Path> listarLegendas(Path pasta) {
        try (Stream<Path> stream = Files.list(pasta)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(p -> {
                    String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                    return n.endsWith(".ass") || n.endsWith(".ssa");
                })
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .toList();
        } catch (IOException e) {
            throw new TraducaoKaraokeException("Falha ao listar as legendas em " + pasta, e);
        }
    }

    /**
     * Saída sempre em pasta irmã {@code <entrada>-karaoke-ptbr}: preserva os
     * originais para auditoria sem pedir um segundo campo na UI. A pasta só é
     * criada de fato na aplicação — a simulação não toca o disco.
     */
    static Path resolverPastaSaida(Path pastaOrigem) {
        Path absoluta = pastaOrigem.toAbsolutePath().normalize();
        String nome = absoluta.getFileName() != null ? absoluta.getFileName().toString() : "legendas";
        Path pai = absoluta.getParent();
        return pai != null ? pai.resolve(nome + SUFIXO_PASTA_SAIDA) : absoluta.resolve(nome + SUFIXO_PASTA_SAIDA);
    }

    private Path resolverArquivoCache(Path arquivo) {
        String nome = arquivo.getFileName().toString();
        String base = nome.replaceFirst("(?i)\\.(ass|ssa)$", "");
        String dirCache = diretorioCache.orElse("cache");
        return Path.of(dirCache, SUBPASTA_CACHE, base + ".cache.json");
    }

    private static String visivelResumido(String texto) {
        String visivel = ClassificadorLetraKaraokeService.extrairTextoVisivel(texto);
        return visivel.length() > 90 ? visivel.substring(0, 87) + "..." : visivel;
    }
}
