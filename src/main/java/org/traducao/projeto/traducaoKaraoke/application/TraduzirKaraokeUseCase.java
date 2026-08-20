package org.traducao.projeto.traducaoKaraoke.application;

import io.quarkus.runtime.Startup;
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
import org.traducao.projeto.qualidadeTraducao.domain.MarcadorPerdidoException;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.llm.domain.LlmPort;
import org.traducao.projeto.cachetraducao.infrastructure.CacheTraducaoService;
import org.traducao.projeto.cachetraducao.domain.EntradaCache;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.traducao.projeto.lore.domain.SnapshotContexto;
import org.traducao.projeto.lore.infrastructure.GerenciadorContexto;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.core.presentation.web.LogStreamService;
import org.traducao.projeto.traducaoKaraoke.domain.AcentosLetraKaraoke;
import org.traducao.projeto.traducaoKaraoke.domain.ClasseLinhaKaraoke;
import org.traducao.projeto.traducaoKaraoke.domain.DesfechoKaraoke;
import org.traducao.projeto.traducaoKaraoke.domain.FalhaArquivoKaraoke;
import org.traducao.projeto.traducaoKaraoke.domain.StatusExecucaoKaraoke;
import org.traducao.projeto.core.texto.TextoSemTags;
import org.traducao.projeto.traducaoKaraoke.domain.GradienteKaraoke;
import org.traducao.projeto.traducaoKaraoke.domain.ResultadoTraducaoKaraoke;
import org.traducao.projeto.traducaoKaraoke.domain.SinaisDeKaraoke;
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
/*
 * CRIAÇÃO ANTECIPADA (@Startup), e não preferência de estilo: um bean
 * @ApplicationScoped só é instanciado na PRIMEIRA chamada, e é nesse momento que os
 * @ConfigProperty abaixo são resolvidos. O endpoint /simular despacha por
 * CompletableFuture.runAsync SEM executor, ou seja, no ForkJoinPool.commonPool — cujas
 * threads são globais da JVM e não carregam o class loader do Quarkus. Numa aplicação
 * recém-iniciada, o Dry-Run era a primeira chamada, a instanciação caía naquela thread e
 * morria com "SRCFG00015: No configuration is available for this class loader".
 *
 * O defeito só aparecia no Dry-Run: /traduzir passa pela FilaExecucaoPipeline, cujas
 * threads têm o contexto certo, então bastava traduzir uma vez para o bean nascer bom e o
 * simulador passar a funcionar — o que fazia o erro parecer intermitente.
 *
 * Criar no boot resolve na origem: a instanciação acontece na thread principal do Quarkus,
 * com configuração disponível, e nenhuma thread de execução volta a instanciar nada. O
 * construtor é vazio (só injeção de campo), então antecipar não executa trabalho nenhum.
 *
 * Isto NÃO revoga a decisão de rodar o /simular fora da fila: ela foi auditada e mantida
 * por ser read-only. Aquela auditoria perguntou se era SEGURO, não se FUNCIONAVA.
 */
@Startup
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

    /**
     * Repõe acento nas linhas que ESTA fatia traduziu para português.
     *
     * <p>Vem de {@code core}, que é consumo livre por contrato — não cria aresta para a fatia
     * {@code traducao}. É o mesmo precedente de {@code TextoSemTags}, que nasceu em
     * {@code traducao.domain} e mudou-se para o core justamente quando o karaokê precisou dele.
     * Ortografia é mecânica de idioma; nem a tradução nem o karaokê são donos do português.
     */
    @Inject
    org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda corretorOrtografico;

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
     * <p>INVARIANTES DO DOMÍNIO: só o modo {@code gravar} verifica o LLM, congela o contexto
     * escolhido e escreve saída/cache; a simulação não toca estado global nem a GPU/LLM. O
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

        List<ResultadoTraducaoKaraoke> resultados = new ArrayList<>();
        List<FalhaArquivoKaraoke> falhas = new ArrayList<>();
        // Holder porque a informação nasce lá dentro, em carregarCache, e precisa subir até o
        // manifesto: proveniência divergente significa que TUDO foi retraduzido, e sem esse sinal
        // um pico de tempo e de custo fica sem explicação no histórico.
        java.util.concurrent.atomic.AtomicBoolean cacheIgnorado =
            new java.util.concurrent.atomic.AtomicBoolean(false);
        StatusExecucaoKaraoke status = StatusExecucaoKaraoke.COMPLETA;
        String motivo = null;
        SnapshotContexto contextoJob = null;
        ProvenienciaCache proveniencia = null;
        boolean houveLinhaCorrigivel = false;

        try {
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
                StatusLlm statusLlm = llmPort.verificarDisponibilidade();
                if (statusLlm == null || !statusLlm.modeloCarregado()) {
                    throw new TraducaoKaraokeException("Servidor LLM indisponível: "
                        + (statusLlm != null ? statusLlm.mensagem() : "sem resposta"));
                }
                logStream.publicarLog(CANAL_LOG, "[OK] Servidor LLM ativo.");
                contextoJob = congelarContexto(contextoId);
                proveniencia = provenienciaDe(contextoJob);
                logStream.publicarLog(CANAL_LOG, "[CONTEXTO CONGELADO] " + contextoJob.nomeExibicao()
                    + " | id=" + contextoJob.id() + " | hash=" + proveniencia.contextoHash());
            }

            // Contador de lotes local à execução: um use case @ApplicationScoped é
            // singleton, então um campo de instância seria compartilhado entre
            // execuções concorrentes (simular fora da fila + aplicar na fila).
            AtomicInteger sequencialLote = new AtomicInteger();
            for (Path arquivo : arquivos) {
                if (Thread.currentThread().isInterrupted()) {
                    status = StatusExecucaoKaraoke.INTERROMPIDA;
                    motivo = "Execução cancelada entre arquivos; o que já foi gravado está preservado.";
                    logStream.publicarLog(CANAL_LOG, "[INTERROMPIDO] " + motivo);
                    break;
                }
                try {
                    ResultadoTraducaoKaraoke r = processarArquivo(
                        arquivo, pastaDestino, gravar, sequencialLote, contextoJob, proveniencia,
                        cacheIgnorado);
                    resultados.add(r);
                    houveLinhaCorrigivel = houveLinhaCorrigivel
                        || r.traduzidas() + r.reaproveitadasCache() > 0;
                } catch (Exception e) {
                    // O arquivo que quebra NÃO pode sumir do manifesto: até 14/08/2026 ele virava
                    // só um contador e uma linha de console, e quem auditasse depois via 23 de 25
                    // sem nenhum vestígio dos outros dois.
                    falhas.add(new FalhaArquivoKaraoke(
                        arquivo.getFileName().toString(),
                        e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
                    log.error("Falha ao processar {}", arquivo, e);
                    logStream.publicarLog(CANAL_LOG, "[ERRO] " + arquivo.getFileName() + ": " + e.getMessage());
                }
            }
        } catch (RuntimeException e) {
            // ABORTADA é o "não verificou" desta fatia. A exceção segue para o chamador — mas
            // agora deixa artefato antes, no finally, em vez de existir só numa linha de log.
            status = StatusExecucaoKaraoke.ABORTADA;
            motivo = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw e;
        } finally {
            long duracaoMs = System.currentTimeMillis() - inicioMs;
            int musicas = resultados.stream().mapToInt(ResultadoTraducaoKaraoke::paraTraduzir).sum();
            int traduzidas = resultados.stream().mapToInt(ResultadoTraducaoKaraoke::traduzidas).sum();
            int doCache = resultados.stream().mapToInt(ResultadoTraducaoKaraoke::reaproveitadasCache).sum();
            int preservadas = resultados.stream().mapToInt(ResultadoTraducaoKaraoke::preservadasOriginalJapones).sum();

            logStream.publicarLog(CANAL_LOG, "==============================================================");
            logStream.publicarLog(CANAL_LOG, String.format(Locale.ROOT,
                "[%s] %s %s: %d arquivo(s), %d falha(s) | letras originais preservadas: %d | traduzíveis: %d (LLM: %d, cache: %d)",
                status == StatusExecucaoKaraoke.COMPLETA && falhas.isEmpty() ? "SUCESSO" : "ATENÇÃO",
                modo, status.name().toLowerCase(Locale.ROOT), resultados.size(), falhas.size(),
                preservadas, musicas, traduzidas, doCache));
            for (FalhaArquivoKaraoke f : falhas) {
                logStream.publicarLog(CANAL_LOG, "   [FALHOU] " + f.arquivo() + " — " + f.motivo());
            }
            logStream.publicarLog(CANAL_LOG, "==============================================================");
            logStream.publicarLog(CANAL_LOG, DuracaoUtil.linhaRelatorioFinal(
                gravar ? "Tradução de Karaokê (LLM)" : "Tradução de Karaokê (simulação)", inicioMs));

            // SEMPRE, e não só quando houve resultado: execução abortada com zero arquivo é
            // justamente a que mais precisa deixar rastro.
            if (gravar) {
                registrarArtefatos(
                    pastaOrigem, pastaDestino, resultados, duracaoMs, musicas, traduzidas + doCache,
                    contextoJob, proveniencia,
                    new DesfechoKaraoke(status, motivo, falhas, cacheIgnorado.get(),
                        estadoDoDicionario(houveLinhaCorrigivel)));
            }
        }
        return resultados;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: responde em TRÊS estados se a ortografia foi conferida nesta execução.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code disponivel()} do corretor é {@code false} tanto quando o
     * hunspell falta quanto quando ninguém perguntou nada — fundir os dois faria "0 acento
     * reposto" parecer sempre bom resultado. Por isso só vira AUSENTE/DISPONIVEL quando ALGUMA
     * linha chegou a passar pelo corretor.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem corretor injetado, devolve NAO_CONSULTADO.
     */
    private DesfechoKaraoke.EstadoDicionario estadoDoDicionario(boolean houveLinhaCorrigivel) {
        if (corretorOrtografico == null || !houveLinhaCorrigivel) {
            return DesfechoKaraoke.EstadoDicionario.NAO_CONSULTADO;
        }
        return corretorOrtografico.disponivel()
            ? DesfechoKaraoke.EstadoDicionario.DISPONIVEL
            : DesfechoKaraoke.EstadoDicionario.AUSENTE;
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
                                                       AtomicInteger sequencialLote,
                                                       SnapshotContexto contextoJob,
                                                       ProvenienciaCache proveniencia,
                                                       java.util.concurrent.atomic.AtomicBoolean cacheIgnorado) {
        String nome = arquivo.getFileName().toString();
        logStream.publicarLog(CANAL_LOG, "");
        logStream.publicarLog(CANAL_LOG, ">> " + nome);

        DocumentoLegenda documento = leitor.ler(arquivo);
        Path arquivoCache = resolverArquivoCache(arquivo);
        Map<String, String> cacheExistente = carregarCache(arquivoCache, gravar, proveniencia, cacheIgnorado);

        int kfx = 0;
        int originais = 0;
        int jaPt = 0;
        int paraTraduzir = 0;
        int doCache = 0;
        int traduzidas = 0;
        int semTraducao = 0;
        // TELEMETRIA DA FATIA: o defeito do acento tem de aparecer NA HORA, na fatia onde nasce.
        // Sem este número, as 5 falas do 86 que saíram com "nao" sem acento em 14/08/2026 só
        // apareceriam para quem lesse o .ass — e apareceram mesmo, semanas depois.
        int acentosRepostos = 0;
        List<String> avisos = new ArrayList<>();

        // Traduções desta execução, deduplicadas por texto original (refrão
        // repetido gasta uma chamada de LLM só).
        Map<String, String> traducoes = new HashMap<>();
        // Dedup por texto VISÍVEL, para o caso em que a MESMA letra aparece em camadas com tags
        // diferentes. Sem isto a chave é o texto COM tags, e {\fad(100,100)}You old Earthlings
        // e {\fad(200,200)}You old Earthlings viram duas chaves — duas chamadas ao LLM e, pior,
        // duas traduções DIVERGENTES na tela. Observado em arquivo ZZ que estava na pasta de
        // saída usada pela execução de karaokê em 09/08/2026:
        //     You old Earthlings, => Vocês, velhos terráqueos,
        //     You old Earthlings, => Vocês, terrestres velhos
        // Só vale quando as tags estão na BORDA (TextoSemTags): com tag no meio a tradução de
        // uma camada não tem onde reencaixar os marcadores da outra.
        Map<String, String> traducaoPorTextoVisivel = new HashMap<>();
        List<EventoLegenda> eventosFinais = new ArrayList<>(documento.eventos().size());

        // Quais instantes JÁ têm uma camada original preservada (japonês/romaji). Precisa ser
        // calculado ANTES de emitir, porque a decisão de cada linha depende do arquivo inteiro.
        //
        // O PREJUÍZO que obriga isto, medido nos 22 do Unicorn em 13/08: nos episódios 13-22 o
        // encerramento tem UMA camada só (ED2, 23 linhas). Traduzir substituía o evento e a letra
        // original DESAPARECIA da tela — 22 de 23 linhas, em 10 episódios. Nos episódios 01-12
        // havia duas camadas (ED romaji + ED - EN inglês) e sobrava a outra por acaso, o que
        // mascarou o defeito: parecia preservação, era substituição com sorte.
        //
        // A DECISÃO inteira do arquivo é tomada de uma vez, e por outro objeto. Antes, este
        // método percorria o documento DUAS vezes chamando o classificador nas duas — no acervo,
        // 3,95 milhões de classificações onde 1,98 milhão basta. E o ganho maior nem é esse:
        // mexer no critério de música deixou de exigir tocar num método que também grava cache e
        // escreve arquivo. Ver PlanoDeClassificacao, inclusive quanto à ORDEM dos dois passes.
        PlanoDeClassificacao plano = PlanoDeClassificacao.montar(documento, classificador);

        List<EventoLegenda> eventos = documento.eventos();
        for (int posicao = 0; posicao < eventos.size(); posicao++) {
            EventoLegenda evento = eventos.get(posicao);
            ClasseLinhaKaraoke classe = plano.classeNaPosicao(posicao);

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

                    // Mesma letra, moldura diferente: reaproveita a tradução já obtida e veste-a
                    // com a tag DESTA camada. Evita a segunda chamada ao LLM e, sobretudo, evita
                    // que duas camadas simultâneas mostrem textos diferentes na tela.
                    if (traduzido == null) {
                        Optional<TextoSemTags> molde = TextoSemTags.decompor(original);
                        if (molde.isPresent()) {
                            String jaTraduzido = traducaoPorTextoVisivel.get(molde.get().textoLimpo());
                            if (jaTraduzido != null) {
                                traduzido = molde.get().recompor(jaTraduzido);
                                logStream.publicarLog(CANAL_LOG,
                                    "   [CAMADA] mesma letra ja traduzida nesta execucao: "
                                        + visivelResumido(traduzido));
                            }
                        }
                    }

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
                            traduzido = traduzirViaLlm(
                                original, avisos, sequencialLote, contextoJob.promptSistema());
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
                    // PRESERVACAO E O DEFAULT AQUI: so chega neste ponto o que JA foi traduzido — romaji e
                    // os fragmentos de KFX nem entram no mapa. E o corretor so aceita a sugestao que e A
                    // MESMA PALAVRA ACENTUADA, entao 'gonna', 'kieta' e 'Nordlicht' passam intactos: nao
                    // existe versao acentuada deles para o dicionario oferecer.
                    //
                    // Medido no Unicorn em 13/08: 88 ocorrencias de acento faltando em 372 linhas ja
                    // traduzidas (ED - EN com 11, ED2 com 77) — 'tras', 'mascara', 'nao', 'voce'.
                    traducoes.put(original, traduzido);
                    // Registra pelo texto VISÍVEL para que a próxima camada com a mesma letra
                    // reaproveite em vez de gastar outra chamada e divergir.
                    TextoSemTags.decompor(traduzido).ifPresent(t ->
                        traducaoPorTextoVisivel.putIfAbsent(
                            TextoSemTags.decompor(original).map(TextoSemTags::textoLimpo).orElse(original),
                            t.textoLimpo()));
                    // AQUI e nao no traducoes.put(): por este ponto passa TUDO que vai para o arquivo,
                    // inclusive o que veio do CACHE. Plugado na origem, a rodada de 13/08 17:53 saiu sem
                    // correcao nenhuma — 40x 'nao', 13x 'voce', 7x 'tras' — porque o karaoke reaproveitou
                    // cache e a traducao nem passou pelo ponto que eu tinha escolhido.
                    String comAcento = corrigirAcentos(traduzido);
                    if (!comAcento.equals(traduzido)) {
                        acentosRepostos++;
                    }
                    eventosFinais.add(evento.comTexto(
                        comOriginalPreservada(evento, comAcento, plano)));
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
            salvarCache(arquivoCache, documento, traducoes, proveniencia);
            logStream.publicarLog(CANAL_LOG, "   [GRAVADO] " + destino);
        }

        logStream.publicarLog(CANAL_LOG, String.format(Locale.ROOT,
            "   Resumo: %d evento(s) | KFX preservado: %d | letra original: %d | já PT: %d | traduzível: %d (LLM %d, cache %d, sem tradução %d) | acento reposto: %s",
            documento.eventos().size(), kfx, originais, jaPt, paraTraduzir, traduzidas, doCache, semTraducao,
            // Três estados, nunca dois: sem hunspell, "0 corrigidas" e "não pude verificar" são
            // coisas diferentes e não podem imprimir o mesmo sinal.
            corretorOrtografico != null && !corretorOrtografico.disponivel()
                ? acentosRepostos + " (dicionário AUSENTE — NÃO VERIFICADO)"
                : String.valueOf(acentosRepostos)));

        // O que estava no cache e NAO foi usado some na regravacao — e some certo, porque a regua
        // de evidencia positiva tornou inalcançavel o que o classificador antigo criou por engano
        // (258 entradas de estilo Signs e 8 de romaji, medidas no cache real em 19/08). Mas cache
        // que encolhe sem numero e indistinguivel de perda de dado, entao o numero vai ao relatorio.
        int entradasCacheDescartadas = 0;
        for (String original : cacheExistente.keySet()) {
            if (!traducoes.containsKey(original)) {
                entradasCacheDescartadas++;
            }
        }
        if (entradasCacheDescartadas > 0) {
            logStream.publicarLog(CANAL_LOG, String.format(Locale.ROOT,
                "   [CACHE] %d entrada(s) do cache nao sao mais alcancaveis e serao descartadas na "
                    + "regravacao — o criterio de musica mudou em 19/08/2026.",
                entradasCacheDescartadas));
        }

        return new ResultadoTraducaoKaraoke(
            nome, nomeDestino, documento.eventos().size(), kfx, originais, jaPt,
            paraTraduzir, doCache, traduzidas, semTraducao, acentosRepostos,
            entradasCacheDescartadas, List.copyOf(avisos));
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
    private String traduzirViaLlm(String original, List<String> avisos, AtomicInteger sequencialLote,
                                  String promptSistemaCongelado) {
        // Karaokê pintado LETRA A LETRA: o mascarador comum produziria uma dezena de [[TAGn]]
        // intercalados e o LLM não os devolve na ordem — medido no Guilty Crown em 07/08/2026,
        // 28 das 31 recusas de uma execução foram exatamente isso, e a única que passou saiu
        // como "So, eu e evidentementereithyingthathingthatmakes mea whole wholed".
        // Aqui a linha é decomposta em paleta + texto: o LLM recebe a frase limpa e as MESMAS
        // cores voltam distribuídas sobre a tradução. Ver GradienteKaraoke.
        Optional<GradienteKaraoke> gradiente = GradienteKaraoke.decompor(original);
        if (gradiente.isPresent()) {
            return traduzirGradiente(
                gradiente.get(), avisos, sequencialLote, promptSistemaCongelado);
        }

        // TAG NA BORDA (o caso do 08th MS Team, 08/08/2026): a linha tem UMA tag de prefixo,
        // vira UM marcador [[TAG0]], e o LLM simplesmente nao o repete. A traducao vinha CERTA e
        // era jogada fora. Do manifesto daquela execucao — 1.258 de 1.258 avisos, todos iguais:
        //
        //   Esperado 1 marcador(es) [0], recebido: Voce ve o sonho brilhando dentro da tempestade
        //   Esperado 1 marcador(es) [0], recebido: Aguenta firme agora! Nao solta isso.
        //
        // Portugues perfeito, descartado por falta de um marcador de controle. Resultado: de
        // 1.636 linhas detectadas, apenas 378 (23%) chegavam a legenda.
        //
        // A saida e a mesma do gradiente e a mesma que Paulo propos em 07/08: NAO mascarar,
        // SEPARAR. O LLM recebe a frase pura — sem marcador nenhum para perder — e a moldura e
        // recolocada aqui. TextoSemTags e o dono desse criterio, ja usado pela fatia traducao.
        Optional<TextoSemTags> semTags = TextoSemTags.decompor(original);
        if (semTags.isPresent()) {
            return traduzirTextoPuro(
                semTags.get(), avisos, sequencialLote, promptSistemaCongelado);
        }

        MascaradorTags.Mascarado mascarado = mascarador.mascarar(original);
        TraducaoLote resposta;
        try {
            resposta = llmPort.traduzir(
                new Lote(sequencialLote.incrementAndGet(), List.of(mascarado.texto())),
                null,
                promptSistemaCongelado);
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
        } catch (MarcadorPerdidoException e) {
            // NAO e alucinacao, e o console nao pode dizer que e (08/08/2026): o modelo traduziu
            // e so nao repetiu o marcador. Mostrar a TRADUCAO RECUSADA e o que permite ao
            // operador ver, na hora, que perdeu trabalho bom — e nao lixo.
            telemetriaService.registrarAlucinacaoPrevenida();
            avisos.add("Marcador perdido (" + e.getMessage() + "); linha mantida: " + original);
            logStream.publicarLog(CANAL_LOG, "   [MARCADOR PERDIDO] traducao DESCARTADA por falta de tag: \""
                + e.traducaoRecusada() + "\"");
            return null;
        } catch (AlucinacaoDetectadaException e) {
            telemetriaService.registrarAlucinacaoPrevenida();
            avisos.add("Alucinação detectada (" + e.getMessage() + "); linha mantida: " + original);
            logStream.publicarLog(CANAL_LOG, "   [AVISO] Alucinação interceptada — linha mantida sem tradução: "
                + visivelResumido(original));
            return null;
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: traduz uma linha de karaokê cujas tags estão todas na BORDA, enviando
     * ao LLM só a frase e recolocando a moldura na volta.
     *
     * <h2>O prejuízo que originou</h2>
     * Execução real no 08th MS Team em 08/08/2026: <b>1.636 linhas detectadas, 378 corrigidas
     * (23%)</b>. Os 1.258 avisos do manifesto são TODOS o mesmo motivo — marcador
     * {@code [[TAG0]]} não devolvido pelo modelo — e o texto recusado estava correto em
     * português. O sistema descartava tradução boa por causa de um marcador de controle.
     *
     * <p>INVARIANTES DO DOMÍNIO: o texto que sai daqui rumo ao LLM não contém tag ASS nem
     * marcador, então não existe marcador a perder; a moldura devolvida é a do ORIGINAL, nunca a
     * que o modelo tenha imaginado. A validação de alucinação roda sobre o texto puro, ANTES de
     * recompor — validar depois faria a própria tag disparar o detector de resíduo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: falha de comunicação, resposta inválida ou alucinação
     * devolvem {@code null} e a linha fica no idioma original, com aviso. Nunca linha meio montada.
     */
    private String traduzirTextoPuro(TextoSemTags semTags, List<String> avisos,
                                     AtomicInteger sequencialLote, String promptSistemaCongelado) {
        TraducaoLote resposta;
        try {
            resposta = llmPort.traduzir(
                new Lote(sequencialLote.incrementAndGet(), List.of(semTags.textoLimpo())),
                null,
                promptSistemaCongelado);
        } catch (Exception e) {
            avisos.add("Falha de comunicação com o LLM; letra mantida: " + semTags.textoLimpo());
            logStream.publicarLog(CANAL_LOG, "   [AVISO] LLM falhou nesta linha (mantida): " + e.getMessage());
            return null;
        }
        if (resposta == null || !resposta.sucesso()
            || resposta.linhasTraduzidas() == null || resposta.linhasTraduzidas().isEmpty()) {
            avisos.add("LLM não retornou tradução; letra mantida: " + semTags.textoLimpo());
            logStream.publicarLog(CANAL_LOG, "   [AVISO] LLM sem resposta válida — letra mantida.");
            return null;
        }
        String traduzido = resposta.linhasTraduzidas().getFirst();
        try {
            validador.validarFala(traduzido);
        } catch (AlucinacaoDetectadaException e) {
            telemetriaService.registrarAlucinacaoPrevenida();
            avisos.add("Alucinação detectada (" + e.getMessage() + "); letra mantida: "
                + semTags.textoLimpo());
            logStream.publicarLog(CANAL_LOG,
                "   [AVISO] Alucinação interceptada na letra — mantida sem tradução: "
                    + semTags.textoLimpo());
            return null;
        }
        return semTags.recompor(traduzido);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: traduz uma linha de karaokê com gradiente de cor por letra, enviando
     * ao LLM apenas o texto que o espectador lê e devolvendo a tradução vestida com as MESMAS
     * cores — o efeito visual do fansub sobrevive à tradução.
     *
     * <p>INVARIANTES DO DOMÍNIO: o texto enviado ao LLM não tem nenhuma tag ASS, portanto não há
     * marcador para o modelo perder; a paleta é reposicionada, nunca alterada. A validação de
     * alucinação roda sobre o TEXTO PURO, antes de recompor — validar depois faria as tags de cor
     * dispararem o detector de resíduo, que foi o outro motivo de recusa observado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: falha de comunicação, resposta inválida ou alucinação
     * devolvem {@code null} e a linha permanece no idioma original, com aviso — exatamente como no
     * caminho comum. Nunca devolve linha meio montada.
     */
    private String traduzirGradiente(GradienteKaraoke gradiente, List<String> avisos,
                                     AtomicInteger sequencialLote, String promptSistemaCongelado) {
        TraducaoLote resposta;
        try {
            resposta = llmPort.traduzir(
                new Lote(sequencialLote.incrementAndGet(), List.of(gradiente.textoVisivel())),
                null,
                promptSistemaCongelado);
        } catch (Exception e) {
            avisos.add("Falha de comunicação com o LLM; letra mantida: " + gradiente.textoVisivel());
            logStream.publicarLog(CANAL_LOG, "   [AVISO] LLM falhou nesta linha de karaokê (mantida): "
                + e.getMessage());
            return null;
        }
        if (resposta == null || !resposta.sucesso()
            || resposta.linhasTraduzidas() == null || resposta.linhasTraduzidas().isEmpty()) {
            avisos.add("LLM não retornou tradução; letra mantida: " + gradiente.textoVisivel());
            logStream.publicarLog(CANAL_LOG, "   [AVISO] LLM sem resposta válida — letra mantida.");
            return null;
        }
        String traduzido = resposta.linhasTraduzidas().getFirst();
        try {
            validador.validarFala(traduzido);
        } catch (AlucinacaoDetectadaException e) {
            telemetriaService.registrarAlucinacaoPrevenida();
            avisos.add("Alucinação detectada (" + e.getMessage() + "); letra mantida: "
                + gradiente.textoVisivel());
            logStream.publicarLog(CANAL_LOG,
                "   [AVISO] Alucinação interceptada na letra — mantida sem tradução: "
                    + gradiente.textoVisivel());
            return null;
        }
        return gradiente.recompor(traduzido);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: repõe acento na linha que ESTA fatia acabou de traduzir.
     *
     * <p>INVARIANTES DO DOMÍNIO: sem o corretor injetado — construção manual em teste, mesmo
     * caso de {@code SeletorEventosTraduziveis} — devolve o texto INTACTO. Preservação é o
     * default do karaokê, então a ausência do corretor não pode virar exceção no meio de uma
     * tradução que já custou chamadas ao LLM.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer problema devolve o texto recebido.
     */
    /**
     * PROPÓSITO DE NEGÓCIO: garante que a letra ORIGINAL continue na tela quando não existe uma
     * camada irmã para preservá-la — empilhando original em cima e tradução embaixo, com a quebra
     * {@code \N}, que é a promessa deste modo ("romaji em cima, PT-BR embaixo").
     *
     * <h2>Por que \N e não um segundo evento</h2>
     * Dois eventos no MESMO instante desenham um por cima do outro, a menos que se mexa em
     * {@code MarginV} ou {@code \pos} — e mexer em posição de karaokê é como o timing se perde.
     * A quebra resolve dentro da própria linha, sem tocar em posicionamento nem em timing.
     *
     * <h2>Invariantes do domínio</h2>
     * <ul>
     *   <li>Se JÁ existe camada original preservada neste instante, NÃO empilha — senão a letra
     *       apareceria duas vezes. É o caso dos episódios 01-12 do Unicorn, que têm ED romaji
     *       ao lado do ED - EN.</li>
     *   <li>A moldura de tags do evento é preservada: o texto original entra como está.</li>
     *   <li>Tradução igual ao original não empilha — mostrar a mesma frase duas vezes é ruído.</li>
     * </ul>
     *
     * <h2>Comportamento em caso de falha</h2>
     * Qualquer entrada nula devolve o traduzido sozinho — o comportamento anterior.
     */


    private String comOriginalPreservada(EventoLegenda evento, String traduzido,
            PlanoDeClassificacao plano) {
        if (evento == null || traduzido == null || traduzido.isBlank()) {
            return traduzido;
        }
        if (plano.temOriginalPreservadaNoInstante(evento)) {
            return traduzido;
        }
        String original = evento.texto();
        if (original == null || original.isBlank()) {
            return traduzido;
        }
        String visivelOriginal = TextoSemTags.decompor(original)
            .map(TextoSemTags::textoLimpo).orElse(original).trim();
        String visivelTraduzido = TextoSemTags.decompor(traduzido)
            .map(TextoSemTags::textoLimpo).orElse(traduzido).trim();
        if (visivelOriginal.isEmpty() || visivelOriginal.equalsIgnoreCase(visivelTraduzido)) {
            return traduzido;
        }
        return original + "\\N" + traduzido;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: repõe acento na camada portuguesa da letra, em duas redes que se
     * completam — a lista NOMINAL desta fatia e, depois, o dicionário do sistema.
     *
     * <h2>Por que a lista é da FATIA e não a de {@code qualidadeTraducao}</h2>
     * Regra de Paulo, 2026-08-14: <i>camada de desacoplamento resolve o problema dela e não o
     * atravessa para as outras</i>. A lista do diálogo tem 162 entradas e QUATRO delas são romaji
     * válido — {@code ate}, {@code mae}, {@code nao}, {@code sao}, medidas contra o dicionário
     * {@code ja_ROMAJI}. No diálogo isso é inofensivo, porque lá não existe camada japonesa;
     * arrastá-la para cá traria de volta o dano do Unicorn ({@code mae} = 前 virou {@code mãe}
     * 100 vezes). Ver {@link AcentosLetraKaraoke}.
     *
     * <p>INVARIANTES DO DOMÍNIO: recebe SÓ o texto traduzido — a linha original é anexada depois,
     * em {@link #comOriginalPreservada}. Trocar essa ordem devolve o defeito do Unicorn, e
     * {@code CorretorNaoAlcancaRomajiDoKaraokeTest} congela isso.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem o corretor injetado — construção manual em teste — a
     * lista nominal ainda vale e o texto volta sem exceção. Preservação é o default do karaokê.
     */
    private String corrigirAcentos(String traduzido) {
        String pelaListaDaFatia = AcentosLetraKaraoke.repor(traduzido);
        return corretorOrtografico == null
            ? pelaListaDaFatia
            : corretorOrtografico.corrigir(pelaListaDaFatia);
    }
    /**
     * Persiste TODAS as traduções aplicadas (novas e reaproveitadas) no cache
     * do arquivo, preservando o fluxo de correção manual: o usuário edita o
     * JSON e a reexecução respeita a edição.
     */
    private void salvarCache(Path arquivoCache, DocumentoLegenda documento, Map<String, String> traducoes,
                             ProvenienciaCache proveniencia) {
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
        cacheService.salvar(arquivoCache, proveniencia, new ArrayList<>(porOriginal.values()));
    }

    private void registrarArtefatos(
        Path pastaOrigem,
        Path pastaDestino,
        List<ResultadoTraducaoKaraoke> resultados,
        long duracaoMs,
        int detectadas,
        int corrigidas,
        SnapshotContexto contexto,
        ProvenienciaCache proveniencia,
        DesfechoKaraoke desfecho
    ) {
        try {
            Path manifesto = persistencia.salvarManifesto(
                pastaOrigem, pastaDestino, resultados, duracaoMs, contexto, proveniencia, desfecho);
            if (manifesto != null) {
                logStream.publicarLog(CANAL_LOG, "Manifesto de auditoria salvo em: " + manifesto);
            }
        } catch (IOException | RuntimeException e) {
            // ERROR, não WARN, e também no console: este é o artefato que prova todo o resto.
            // Perdê-lo em silêncio é ficar sem auditoria justamente na execução que deu errado.
            log.error("Falha ao salvar o manifesto da tradução de karaokê", e);
            logStream.publicarLog(CANAL_LOG,
                "[ERRO] MANIFESTO NÃO SALVO — esta execução ficou SEM auditoria: " + e.getMessage());
        }
        // Contexto é nulo quando a execução abortou antes de congelá-lo (LLM fora do ar). O
        // registro tem de sobreviver a isso: era exatamente a execução sem rastro nenhum.
        String descricaoContexto = contexto == null
            ? "Contexto NÃO congelado (execução " + desfecho.status() + ")"
            : "Contexto: " + contexto.nomeExibicao() + " (" + contexto.id() + ") | hash="
                + (proveniencia == null ? "-" : proveniencia.contextoHash());
        telemetriaService.finalizarOperacao(
            TelemetriaService.criarOperacao(
                "Tradução de Karaokê (LLM)",
                "[" + desfecho.status() + "] " + descricaoContexto
                    + " | Legendas: " + pastaOrigem + " → " + pastaDestino
                    + (desfecho.falhas().isEmpty() ? "" : " | falhas: " + desfecho.falhas().size()),
                duracaoMs,
                resultados.size(),
                detectadas,
                corrigidas),
            pastaOrigem,
            "traducao_karaoke",
            montarRelatorio(pastaOrigem, pastaDestino, resultados, duracaoMs, desfecho));
    }

    private String montarRelatorio(
        Path pastaOrigem, Path pastaDestino, List<ResultadoTraducaoKaraoke> resultados, long duracaoMs,
        DesfechoKaraoke desfecho) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tradução de Karaokê — letras originais preservadas + tradução PT-BR\n");
        // O desfecho encabeça o relatório: é a primeira pergunta de quem o abre.
        sb.append("Status: ").append(desfecho.status());
        if (desfecho.motivo() != null) {
            sb.append(" — ").append(desfecho.motivo());
        }
        sb.append('\n');
        sb.append("Dicionário: ").append(desfecho.estadoDicionario()).append('\n');
        if (desfecho.cacheIgnorado()) {
            sb.append("Cache IGNORADO nesta execução (proveniência divergente): tudo retraduzido\n");
        }
        if (!desfecho.falhas().isEmpty()) {
            sb.append("Arquivos com FALHA: ").append(desfecho.falhas().size()).append('\n');
            for (FalhaArquivoKaraoke f : desfecho.falhas()) {
                sb.append("  - ").append(f.arquivo()).append(" — ").append(f.motivo()).append('\n');
            }
        }
        sb.append("Origem: ").append(pastaOrigem.toAbsolutePath()).append('\n');
        sb.append("Destino: ").append(pastaDestino.toAbsolutePath()).append('\n');
        sb.append("Duração: ").append(duracaoMs).append(" ms\n\n");
        for (ResultadoTraducaoKaraoke r : resultados) {
            sb.append(r.arquivo())
                .append(" | letra original: ").append(r.preservadasOriginalJapones())
                .append(" | traduzidas (LLM): ").append(r.traduzidas())
                .append(" | cache: ").append(r.reaproveitadasCache())
                .append(" | sem tradução: ").append(r.mantidasSemTraducao())
                .append(" | acento reposto: ").append(r.acentosRepostos())
                .append(" | avisos: ").append(r.avisos().size())
                .append('\n');
        }
        return sb.toString();
    }

    /** Congela exatamente a obra escolhida; a pasta não participa da decisão de lore. */
    private SnapshotContexto congelarContexto(String contextoId) {
        if (gerenciadorContexto == null) {
            throw new TraducaoKaraokeException(
                "Gerenciador de contexto indisponível; a lore selecionada não pode ser congelada.");
        }
        return gerenciadorContexto.snapshotPorId(contextoId);
    }

    private ProvenienciaCache provenienciaDe(SnapshotContexto contexto) {
        String modelo = llmPort.modeloAtivo();
        return new ProvenienciaCache(
            ProvenienciaCache.SCHEMA_ATUAL,
            contexto.id(),
            ProvenienciaCache.hashDe(contexto.promptSistema()),
            modelo == null || modelo.isBlank() ? "desconhecido" : modelo,
            idiomaOriginal.filter(s -> !s.isBlank()).orElse("en"),
            idiomaTraduzido.filter(s -> !s.isBlank()).orElse("pt-br"));
    }

    private Map<String, String> carregarCache(
        Path arquivoCache, boolean gravar, ProvenienciaCache proveniencia,
        java.util.concurrent.atomic.AtomicBoolean cacheIgnorado
    ) {
        if (!gravar) {
            return Map.of();
        }
        CacheTraducaoService.ResultadoCarga carga = cacheService.carregar(arquivoCache, proveniencia);
        if (carga.migrado()) {
            cacheService.arquivarGeracaoSemProveniencia(arquivoCache);
            // Sobe para o manifesto: descartar o cache multiplica tempo e chamadas ao LLM, e sem
            // esse sinal o pico aparece no histórico sem explicação nenhuma.
            cacheIgnorado.set(true);
            logStream.publicarLog(CANAL_LOG,
                "   [CACHE IGNORADO] cache antigo sem contexto/lore foi preservado; linhas serão retraduzidas e carimbadas.");
            return Map.of();
        }
        return carga.mapa();
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



