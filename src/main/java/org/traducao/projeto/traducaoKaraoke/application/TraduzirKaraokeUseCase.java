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
    LlmPort llmPort;

    @Inject
    GerenciadorContexto gerenciadorContexto;

    @Inject
    ClassificadorLetraKaraokeService classificador;

    /**
     * Os TRÊS caminhos de envio ao LLM. Saíram daqui em 2026-08-19: eram 780 bytecodes e cinco
     * dependências dentro de um objeto que também classifica, grava cache e escreve arquivo.
     */
    @Inject
    TradutorDeLetraKaraoke tradutorDeLetra;

    /** Manifesto, relatorio, telemetria e console. Saiu daqui em 2026-08-19. */
    @Inject
    RegistroDaExecucao registro;

    /** Carregar e salvar o cache JSON por arquivo. Saiu daqui em 2026-08-19. */
    @Inject
    CacheDoArquivo cacheDoArquivo;

    /** Acento e empilhamento da letra original. Saiu daqui em 2026-08-19. */
    @Inject
    MontadorEventoFinal montador;


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
                registro.registrar(
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
        if (!houveLinhaCorrigivel) {
            return DesfechoKaraoke.EstadoDicionario.NAO_CONSULTADO;
        }
        return montador.dicionarioDisponivel()
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
        Path arquivoCache = cacheDoArquivo.arquivoDe(arquivo);
        Map<String, String> cacheExistente =
            cacheDoArquivo.carregar(arquivoCache, gravar, proveniencia, cacheIgnorado);

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
                    logStream.publicarLog(CANAL_LOG, "   [MÚSICA-JP] mantida: " + RegistroDaExecucao.visivelResumido(evento.texto()));
                }
                case JA_PORTUGUES -> {
                    jaPt++;
                    eventosFinais.add(evento);
                    logStream.publicarLog(CANAL_LOG, "   [MÚSICA-PT] já traduzida: " + RegistroDaExecucao.visivelResumido(evento.texto()));
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
                                        + RegistroDaExecucao.visivelResumido(traduzido));
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
                            traduzido = tradutorDeLetra.traduzirViaLlm(
                                original, avisos, sequencialLote, contextoJob.promptSistema());
                        }
                    }
                    if (traduzido == null) {
                        eventosFinais.add(evento);
                        if (gravar) {
                            semTraducao++;
                        } else {
                            logStream.publicarLog(CANAL_LOG, "   [MÚSICA-EN → LLM] será traduzida: " + RegistroDaExecucao.visivelResumido(original));
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
                    String comAcento = montador.corrigirAcentos(traduzido);
                    if (!comAcento.equals(traduzido)) {
                        acentosRepostos++;
                    }
                    eventosFinais.add(evento.comTexto(
                        montador.comOriginalPreservada(evento, comAcento, plano)));
                    if (veioDoCache) {
                        doCache++;
                        logStream.publicarLog(CANAL_LOG, "   [CACHE] reaproveitada: " + RegistroDaExecucao.visivelResumido(traduzido));
                    } else {
                        traduzidas++;
                        logStream.publicarLog(CANAL_LOG, "   [LLM] " + RegistroDaExecucao.visivelResumido(original)
                            + "  =>  " + RegistroDaExecucao.visivelResumido(traduzido));
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
            cacheDoArquivo.salvar(arquivoCache, documento, traducoes, proveniencia);
            logStream.publicarLog(CANAL_LOG, "   [GRAVADO] " + destino);
        }

        logStream.publicarLog(CANAL_LOG, String.format(Locale.ROOT,
            "   Resumo: %d evento(s) | KFX preservado: %d | letra original: %d | já PT: %d | traduzível: %d (LLM %d, cache %d, sem tradução %d) | acento reposto: %s",
            documento.eventos().size(), kfx, originais, jaPt, paraTraduzir, traduzidas, doCache, semTraducao,
            // Três estados, nunca dois: sem hunspell, "0 corrigidas" e "não pude verificar" são
            // coisas diferentes e não podem imprimir o mesmo sinal.
            !montador.dicionarioDisponivel()
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
     * PROPÓSITO DE NEGÓCIO: repõe acento na linha que ESTA fatia acabou de traduzir.
     *
     * <p>INVARIANTES DO DOMÍNIO: sem o corretor injetado — construção manual em teste, mesmo
     * caso de {@code SeletorEventosTraduziveis} — devolve o texto INTACTO. Preservação é o
     * default do karaokê, então a ausência do corretor não pode virar exceção no meio de uma
     * tradução que já custou chamadas ao LLM.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer problema devolve o texto recebido.
     */


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
}
