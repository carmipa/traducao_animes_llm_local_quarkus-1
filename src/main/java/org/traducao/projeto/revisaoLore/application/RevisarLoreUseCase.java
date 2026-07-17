package org.traducao.projeto.revisaoLore.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.revisaoLore.domain.LogEventoRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.RevisaoLoreRelatorioJson;
import org.traducao.projeto.revisaoLore.domain.EntradaAuditoriaRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ResultadoDeteccaoLore;
import org.traducao.projeto.revisaoLore.domain.ResultadoRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.StatusRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.exceptions.RevisaoLoreException;
import org.traducao.projeto.revisaoLore.infrastructure.RevisaoLoreAuditoriaCache;
import org.traducao.projeto.revisaoLore.infrastructure.RevisaoLoreLogPersistencia;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.core.io.DiretorioBaseKronos;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.revisaoLore.domain.StatusRevisaoLoreLlm;
import org.traducao.projeto.revisaoLore.domain.ports.RevisorLoreLlmPort;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.core.presentation.ui.AnsiCores;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * PROPÓSITO DE NEGÓCIO: revisa terminologia canônica de uma obra comparando
 * legendas EN/PT-BR, preservando estrutura ASS e produzindo auditoria,
 * telemetria, backup e status operacional verdadeiro.
 *
 * <p>INVARIANTES DO DOMÍNIO: pares precisam estar estruturalmente alinhados;
 * somente propostas estruturalmente válidas e semanticamente livres dos
 * indícios originais de lore podem ser gravadas; toda sobrescrita tem backup.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: falhas de entrada interrompem a operação;
 * falhas isoladas por arquivo preservam o original, entram no relatório e
 * resultam em status com pendências.
 */
@Service
public class RevisarLoreUseCase {

    private static final Logger log = LoggerFactory.getLogger(RevisarLoreUseCase.class);
    private static final DateTimeFormatter UTC_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private static final DateTimeFormatter TS_BACKUP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final int TAMANHO_TRECHO_LOG = 120;
    // Tolerância de tempo (ms) ao verificar se EN[i] e PT[i] são a MESMA fala.
    // Traduções fiéis mantêm o tempo idêntico; fontes externas podem ter jitter
    // de arredondamento. 500ms tolera jitter sem deixar passar reordenação real
    // (que desloca os tempos em segundos).
    private static final long ALINHAMENTO_TOLERANCIA_MS = 500;
    private static final Pattern PADRAO_TEMPO_ASS = Pattern.compile("(\\d+):(\\d{2}):(\\d{2})[.,](\\d{2})");
    private static final Pattern PADRAO_DESENHO_VETORIAL = Pattern.compile("\\\\p[1-9]\\d*");
    private static final Pattern PADRAO_TAG_ASS = Pattern.compile("\\{[^}]*}");
    private static final Pattern PADRAO_INVISIVEIS = Pattern.compile("[\\u200B\\u200C\\u200D\\uFEFF]");
    private static final Pattern PADRAO_SHIN = Pattern.compile("(?<![\\p{L}\\p{N}])Shin(?![\\p{L}\\p{N}])");
    private static final Pattern PADRAO_CANELA = Pattern.compile("(?<![\\p{L}\\p{N}])[Cc]anela(?![\\p{L}\\p{N}])");
    private static final Pattern PADRAO_DUD_ROUNDS = Pattern.compile("(?i)(?<![\\p{L}\\p{N}])dud\\s+rounds?(?![\\p{L}\\p{N}])");
    private static final Pattern PADRAO_RODADAS_ALEATORIAS = Pattern.compile(
        "(?i)(?<![\\p{L}\\p{N}])rodadas\\s+(?:aleat[oó]rias|fracassadas|falsas|dud)(?![\\p{L}\\p{N}])");

    private final LeitorLegendaAss leitor;
    private final EscritorLegendaAss escritor;
    private final MascaradorTags mascarador;
    private final DetectorTermosLoreService detector;
    private final ValidadorTraducaoService validador;
    private final RevisorLoreLlmPort revisorLoreLlm;
    private final GerenciadorPromptRevisaoLore gerenciadorPromptRevisaoLore;
    private final TelemetriaService telemetriaService;
    private final RevisaoLoreLogPersistencia logPersistencia;
    private final RevisaoLoreAuditoriaCache auditoriaCache;
    private final PoliticaEstiloMusical politicaEstiloMusical;
    private final DetectorEfeitoKaraokeService detectorKaraoke;
    private final ProtecaoLegendaAssService protecaoAss;

    /**
     * Estado de UMA execução de revisão (log de eventos + relógio da sessão).
     * Vive num objeto local, nunca em campos do bean: este use case é um
     * singleton e campos de instância seriam compartilhados — e corrompidos —
     * entre execuções.
     */
    private static final class SessaoRevisao {
        final List<LogEventoRevisaoLore> eventos = new ArrayList<>();
        final long inicioMs = System.currentTimeMillis();

        // O console web carimba a hora local no navegador, então a linha vai
        // para System.out sem prefixo de relógio. O prefixo UTC + tempo
        // decorrido permanece no log do servidor e nos eventos persistidos.
        void out(String msg) {
            String limpo = removerAnsi(msg);
            String prefixo = prefixoLog();
            String limpoComPrefixo = prefixo + " " + limpo;
            System.out.println(msg);
            log.info(limpoComPrefixo);
            eventos.add(new LogEventoRevisaoLore(
                Instant.now().toString(),
                inferirNivel(limpoComPrefixo),
                limpoComPrefixo
            ));
        }

        private String prefixoLog() {
            long decorridoMs = Math.max(0, System.currentTimeMillis() - inicioMs);
            return "[UTC " + UTC_FORMATTER.format(Instant.now()) + " | +" + formatarDuracaoDetalhada(decorridoMs) + "]";
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reúne os componentes que detectam, revisam,
     * validam, persistem e auditam correções de lore.
     * <p>INVARIANTES DO DOMÍNIO: leitores, validadores, contexto e persistências
     * são obrigatórios e permanecem imutáveis durante a execução.
     * <p>COMPORTAMENTO EM CASO DE FALHA: a injeção interrompe a inicialização
     * quando alguma dependência obrigatória não está disponível.
     */
    public RevisarLoreUseCase(
        LeitorLegendaAss leitor,
        EscritorLegendaAss escritor,
        MascaradorTags mascarador,
        DetectorTermosLoreService detector,
        ValidadorTraducaoService validador,
        RevisorLoreLlmPort revisorLoreLlm,
        GerenciadorPromptRevisaoLore gerenciadorPromptRevisaoLore,
        TelemetriaService telemetriaService,
        RevisaoLoreLogPersistencia logPersistencia,
        RevisaoLoreAuditoriaCache auditoriaCache,
        PoliticaEstiloMusical politicaEstiloMusical,
        DetectorEfeitoKaraokeService detectorKaraoke,
        ProtecaoLegendaAssService protecaoAss
    ) {
        this.leitor = leitor;
        this.escritor = escritor;
        this.mascarador = mascarador;
        this.detector = detector;
        this.validador = validador;
        this.revisorLoreLlm = revisorLoreLlm;
        this.gerenciadorPromptRevisaoLore = gerenciadorPromptRevisaoLore;
        this.telemetriaService = telemetriaService;
        this.logPersistencia = logPersistencia;
        this.auditoriaCache = auditoriaCache;
        this.politicaEstiloMusical = politicaEstiloMusical;
        this.detectorKaraoke = detectorKaraoke;
        this.protecaoAss = protecaoAss;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: executa uma sessão completa de revisão de lore e
     * devolve o desfecho real com contadores e caminho do dataset detalhado.
     * <p>INVARIANTES DO DOMÍNIO: contexto válido e pastas existentes são
     * exigidos; a sessão não compartilha estado mutável com outra execução.
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada inválida lança
     * {@link RevisaoLoreException}; falhas por arquivo viram pendências.
     */
    public ResultadoRevisaoLore executar(
        Path pastaOriginal,
        Path pastaTraduzida,
        String contextoId,
        boolean revisarTodasFalas
    ) {
        SessaoRevisao sessao = new SessaoRevisao();

        validarEntrada(pastaOriginal, pastaTraduzida, contextoId);

        StatusRevisaoLoreLlm status = revisorLoreLlm.verificarDisponibilidade();
        if (!status.modeloCarregado()) {
            throw new RevisaoLoreException("LLM indisponivel para revisao de lore: " + status.mensagem());
        }

        String nomePromptRevisao = gerenciadorPromptRevisaoLore.obterNome(contextoId);
        String promptSistemaRevisaoLore = gerenciadorPromptRevisaoLore.obterPromptSistema(contextoId);
        String loreCanonica = PromptRevisaoLore.extrairLoreCanonica(promptSistemaRevisaoLore);

        sessao.out(AnsiCores.CYAN + "\n=== Revisao de Lore (nomes, locais e terminologia) ===" + AnsiCores.RESET);
        sessao.out("Inicio UTC: " + UTC_FORMATTER.format(Instant.now()));
        sessao.out("Pasta original (EN): " + pastaOriginal.toAbsolutePath());
        sessao.out("Pasta traduzida (PT-BR): " + pastaTraduzida.toAbsolutePath());
        sessao.out("Prompt de revisao de lore ativo: " + nomePromptRevisao + " (" + contextoId + ")");
        sessao.out(revisarTodasFalas
            ? "Modo: revisar TODAS as falas com dialogo"
            : "Modo: revisar apenas falas sinalizadas pela heuristica");

        int[] arquivosAnalisados = {0};
        int[] arquivosAlterados = {0};
        int[] falasAuditadas = {0};
        int[] falasSinalizadas = {0};
        int[] falasCorrigidas = {0};
        int[] falasSemAlteracao = {0};
        int[] falasSemResposta = {0};
        int[] falasDescartadas = {0};
        int[] falasEncaminhadasOpcao6 = {0};
        boolean[] cancelado = {false};
        boolean semArquivos = false;
        List<String> erros = new ArrayList<>();

        // Toda sobrescrita cria backup (como as opções 5/6): uma correção
        // semanticamente errada nunca destrói a legenda anterior sem cópia.
        Path pastaBackup = DiretorioBaseKronos.resolver("backups", "revisao-lore",
            "revisao_" + LocalDateTime.now().format(TS_BACKUP)).toAbsolutePath().normalize();

        try (Stream<Path> stream = Files.walk(pastaOriginal)) {
            List<Path> originais = stream
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().toLowerCase().endsWith(".ass"))
                .toList();

            sessao.out("Arquivos .ass encontrados na pasta original: " + originais.size());
            if (originais.isEmpty()) {
                semArquivos = true;
                String msg = "Nenhum arquivo .ass encontrado na pasta original; revisao de lore sem trabalho real.";
                erros.add(msg);
                sessao.out(AnsiCores.YELLOW + "  [Aviso] " + msg + AnsiCores.RESET);
            }

            int totalFalasGlobais = contarDialogosAuditaveisNoLote(originais, pastaTraduzida);
            sessao.out("Falas auditaveis no lote: " + totalFalasGlobais);

            for (int indiceArquivo = 0; indiceArquivo < originais.size(); indiceArquivo++) {
                Path arqOriginal = originais.get(indiceArquivo);
                processarArquivo(
                    sessao, arqOriginal, pastaTraduzida, contextoId, nomePromptRevisao,
                    revisarTodasFalas, promptSistemaRevisaoLore, loreCanonica, pastaBackup,
                    indiceArquivo + 1, originais.size(), totalFalasGlobais,
                    arquivosAnalisados, arquivosAlterados, falasAuditadas, falasSinalizadas,
                    falasCorrigidas, falasSemAlteracao, falasSemResposta, falasDescartadas,
                    falasEncaminhadasOpcao6, cancelado, erros
                );
            }
        } catch (IOException e) {
            throw new RevisaoLoreException("Falha ao percorrer pasta original: " + pastaOriginal, e);
        }

        long duracaoMs = System.currentTimeMillis() - sessao.inicioMs;

        // Pendentes = falas sinalizadas cujo problema NÃO foi resolvido (LLM sem
        // resposta ou correção proposta barrada por trava). Ficam como estavam,
        // ainda merecendo revisão humana — distinto de "conforme" e "corrigida".
        int falasPendentes = falasSemResposta[0] + falasDescartadas[0] + falasEncaminhadasOpcao6[0];
        StatusRevisaoLore statusFinal = determinarStatus(semArquivos, cancelado[0], erros, falasPendentes);

        sessao.out("Arquivos analisados: " + arquivosAnalisados[0]);
        sessao.out("Arquivos alterados: " + arquivosAlterados[0]);
        sessao.out("Falas auditadas: " + falasAuditadas[0]);
        sessao.out("Falas sinalizadas (heuristica/LLM): " + falasSinalizadas[0]);
        sessao.out("Falas corrigidas: " + falasCorrigidas[0]);
        sessao.out("Falas ja conformes: " + falasSemAlteracao[0]);
        sessao.out("Falas sem resposta do LLM: " + falasSemResposta[0]);
        sessao.out("Falas descartadas (travas): " + falasDescartadas[0]);
        sessao.out("Falas encaminhadas para a Opção 6: " + falasEncaminhadasOpcao6[0]);
        sessao.out("Falas pendentes (sinalizadas sem correcao): " + falasPendentes);
        sessao.out("Status: " + statusFinal.rotulo());

        if (statusFinal == StatusRevisaoLore.CONCLUIDO) {
            sessao.out(AnsiCores.GREEN + "\nRevisao de lore concluida com sucesso." + AnsiCores.RESET);
        } else if (statusFinal == StatusRevisaoLore.CONCLUIDO_COM_PENDENCIAS) {
            sessao.out(AnsiCores.YELLOW + "\nRevisao de lore concluida com " + falasPendentes
                + " fala(s) pendente(s) e " + erros.size() + " aviso(s)/erro(s)." + AnsiCores.RESET);
        } else {
            sessao.out(AnsiCores.YELLOW + "\nRevisao de lore finalizada com status: "
                + statusFinal.rotulo() + "." + AnsiCores.RESET);
        }
        if (!erros.isEmpty()) {
            for (String erro : erros) {
                sessao.out(AnsiCores.YELLOW + "  - " + erro + AnsiCores.RESET);
            }
        }

        String caminhoRelatorioJson = persistirRelatorioJson(
            sessao, contextoId, revisarTodasFalas, statusFinal,
            pastaOriginal, pastaTraduzida, duracaoMs,
            arquivosAnalisados[0], arquivosAlterados[0],
            falasAuditadas[0], falasSinalizadas[0], falasCorrigidas[0],
            falasSemAlteracao[0], falasSemResposta[0], falasDescartadas[0],
            falasEncaminhadasOpcao6[0],
            falasPendentes, erros
        );

        if (caminhoRelatorioJson != null) {
            sessao.out("Relatorio JSON (log + telemetria) salvo em: " + caminhoRelatorioJson);
        }

        return new ResultadoRevisaoLore(
            statusFinal,
            arquivosAnalisados[0],
            arquivosAlterados[0],
            falasAuditadas[0],
            falasSinalizadas[0],
            falasCorrigidas[0],
            falasSemAlteracao[0],
            falasSemResposta[0],
            falasDescartadas[0],
            falasEncaminhadasOpcao6[0],
            falasPendentes,
            erros.size(),
            erros,
            caminhoRelatorioJson
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: traduz os contadores de uma execução no status final
     * exibido ao operador, para que o desfecho não seja mais um "[SUCESSO]"
     * incondicional.
     *
     * <p>INVARIANTES DO DOMÍNIO: retorna exatamente um status para retornos
     * normais do use case. {@link StatusRevisaoLore#FALHOU} nunca sai daqui — é
     * responsabilidade do controller ao capturar exceção propagada. Precedência:
     * sem arquivos &gt; cancelado &gt; pendências (erros ou falas pendentes) &gt;
     * concluído.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: método puro; não lança exceção.
     */
    static StatusRevisaoLore determinarStatus(
        boolean semArquivos, boolean cancelado, List<String> erros, int falasPendentes) {
        if (semArquivos) {
            return StatusRevisaoLore.SEM_ARQUIVOS;
        }
        if (cancelado) {
            return StatusRevisaoLore.CANCELADO;
        }
        if (!erros.isEmpty() || falasPendentes > 0) {
            return StatusRevisaoLore.CONCLUIDO_COM_PENDENCIAS;
        }
        return StatusRevisaoLore.CONCLUIDO;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: preserva a legenda PT-BR anterior antes de a revisão
     * de lore sobrescrever o arquivo, evitando perda por correção equivocada.
     *
     * <p>INVARIANTES DO DOMÍNIO: o backup fica dentro de {@code pastaBackup}
     * (validado contra path traversal) e a primeira cópia da sessão nunca é
     * substituída.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link RevisaoLoreException} e
     * bloqueia a escrita da nova legenda (o arquivo original permanece intacto).
     */
    static Path criarBackup(Path arquivo, Path pastaBackup) {
        Path backup = pastaBackup.resolve(arquivo.getFileName()).normalize();
        if (!backup.startsWith(pastaBackup)) {
            throw new RevisaoLoreException("Caminho de backup invalido para: " + arquivo);
        }
        try {
            Files.createDirectories(backup.getParent());
            if (Files.notExists(backup)) {
                Files.copy(arquivo, backup, StandardCopyOption.COPY_ATTRIBUTES);
            }
            return backup;
        } catch (IOException e) {
            throw new RevisaoLoreException("Falha ao criar backup da legenda: " + arquivo, e);
        }
    }

    private void validarEntrada(Path pastaOriginal, Path pastaTraduzida, String contextoId) {
        if (contextoId == null || contextoId.isBlank()) {
            throw new RevisaoLoreException(
                "Contexto da obra obrigatorio. Selecione o anime/filme no menu para carregar a lore oficial.");
        }
        if (!gerenciadorPromptRevisaoLore.existePrompt(contextoId)) {
            throw new RevisaoLoreException(
                "Prompt de revisao de lore desconhecido: \"" + contextoId
                    + "\". Recarregue a pagina e selecione uma obra valida.");
        }
        if (!Files.isDirectory(pastaOriginal) || !Files.isDirectory(pastaTraduzida)) {
            throw new RevisaoLoreException(
                "Pastas nao encontradas — esperava original em " + pastaOriginal + " e traduzida em " + pastaTraduzida);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: revisa um par EN/PT-BR, preservando falas que não
     * podem ser corrigidas com segurança e acumulando métricas da sessão.
     * <p>INVARIANTES DO DOMÍNIO: o par precisa ter estrutura alinhada; somente
     * candidato validado estrutural e semanticamente altera o documento.
     * <p>COMPORTAMENTO EM CASO DE FALHA: registra o erro do arquivo, mantém a
     * legenda anterior e permite que os demais pares continuem.
     */
    private void processarArquivo(
        SessaoRevisao sessao,
        Path arqOriginal,
        Path pastaTraduzida,
        String contextoId,
        String nomePromptRevisao,
        boolean revisarTodasFalas,
        String promptSistemaRevisaoLore,
        String loreCanonica,
        Path pastaBackup,
        int indiceArquivo,
        int totalArquivos,
        int totalFalasGlobais,
        int[] arquivosAnalisados,
        int[] arquivosAlterados,
        int[] falasAuditadas,
        int[] falasSinalizadas,
        int[] falasCorrigidas,
        int[] falasSemAlteracao,
        int[] falasSemResposta,
        int[] falasDescartadas,
        int[] falasEncaminhadasOpcao6,
        boolean[] cancelado,
        List<String> erros
    ) {
        String nomeOriginal = arqOriginal.getFileName().toString();
        Path arqTraduzido = localizarArquivoTraduzido(arqOriginal, pastaTraduzida);
        if (!Files.exists(arqTraduzido)) {
            String msg = "Sem par traduzido para: " + nomeOriginal;
            erros.add(msg);
            sessao.out(AnsiCores.YELLOW + "  [Pulado] " + msg + AnsiCores.RESET);
            return;
        }

        arquivosAnalisados[0]++;
        sessao.out("\n[Arquivo] Analisando: " + arqTraduzido.getFileName());

        try {
            DocumentoLegenda docOriginal = leitor.ler(arqOriginal);
            DocumentoLegenda docTraduzido = leitor.ler(arqTraduzido);
            sessao.out("[Arquivo] Eventos original/traduzido: " + docOriginal.eventos().size()
                + "/" + docTraduzido.eventos().size());

            if (docOriginal.eventos().size() != docTraduzido.eventos().size()) {
                String msg = arqTraduzido.getFileName() + ": contagem de eventos divergente ("
                    + docOriginal.eventos().size() + " vs " + docTraduzido.eventos().size() + ") — arquivo pulado.";
                sessao.out(AnsiCores.YELLOW + "  [Pulado] " + msg + AnsiCores.RESET);
                erros.add(msg);
                return;
            }

            // Contagem igual NÃO garante que EN[i] e PT[i] sejam a mesma fala: um
            // Comment reposicionado ou diálogos reordenados alinham por índice mas
            // trocam o contexto. Verifica tempo/tipo por evento antes de auditar —
            // sem isso, o LLM corrigiria comparando falas trocadas em silêncio.
            Optional<String> divergencia = primeiraDivergenciaEstrutural(
                docOriginal, docTraduzido, ALINHAMENTO_TOLERANCIA_MS);
            if (divergencia.isPresent()) {
                String msg = arqTraduzido.getFileName() + ": pares EN/PT desalinhados ("
                    + divergencia.get() + ") — arquivo pulado para nao corrigir com contexto trocado.";
                sessao.out(AnsiCores.YELLOW + "  [Pulado] " + msg + AnsiCores.RESET);
                erros.add(msg);
                return;
            }

            boolean houveModificacao = false;
            int corrigidasNoArquivo = 0;
            int totalDialogos = contarDialogosAuditaveis(docOriginal, docTraduzido);
            int dialogoAtual = 0;
            List<EventoLegenda> novosEventos = new ArrayList<>(docTraduzido.eventos().size());

            boolean interrompido = false;
            for (int i = 0; i < docOriginal.eventos().size(); i++) {
                EventoLegenda evtOriginal = docOriginal.eventos().get(i);
                EventoLegenda evtTraduzido = docTraduzido.eventos().get(i);

                // Parada cooperativa (botão "Parar" da UI): falas restantes
                // entram sem alteração e o já revisado é gravado normalmente.
                if (interrompido || Thread.currentThread().isInterrupted()) {
                    if (!interrompido) {
                        sessao.out(AnsiCores.YELLOW
                            + "[STOP] Revisão de lore interrompida pelo usuário — falas restantes mantidas."
                            + AnsiCores.RESET);
                        interrompido = true;
                        cancelado[0] = true;
                    }
                    novosEventos.add(evtTraduzido);
                    continue;
                }

                if (!ehEventoAuditavelLore(evtOriginal, evtTraduzido)) {
                    novosEventos.add(evtTraduzido);
                    continue;
                }

                String textoEn = evtOriginal.texto();
                String textoPt = evtTraduzido.texto();
                falasAuditadas[0]++;
                dialogoAtual++;

                String marcadorFala = formatarMarcadorProgresso(
                    indiceArquivo, totalArquivos, falasAuditadas[0], totalFalasGlobais, i + 1);
                sessao.out(AnsiCores.DIM + marcadorFala + " auditando lore | EN: "
                    + trecho(textoEn) + " | PT: " + trecho(textoPt) + AnsiCores.RESET);

                MascaradorTags.Mascarado mascaraEn = mascarador.mascarar(textoEn);
                MascaradorTags.Mascarado mascaraPt = mascarador.mascarar(textoPt);

                if (ehFalaNaoTraduzida(mascaraEn.texto(), mascaraPt.texto())) {
                    falasSinalizadas[0]++;
                    falasEncaminhadasOpcao6[0]++;
                    sessao.out(AnsiCores.YELLOW + marcadorFala
                        + " [FORA DO ESCOPO DE LORE] PT-BR está idêntico ao original EN; "
                        + "nenhuma chamada ao LLM foi feita. Execute a Opção 6 antes da Opção 7."
                        + AnsiCores.RESET);
                    registrarAuditoria(
                        contextoId, nomePromptRevisao, revisarTodasFalas, arqTraduzido, i + 1,
                        dialogoAtual, totalDialogos, "ENCAMINHADA_OPCAO_6", List.of("Fala não traduzida"),
                        textoEn, textoPt, null, textoPt,
                        "Fala integralmente em inglês; correção linguística pertence à Opção 6"
                    );
                    novosEventos.add(evtTraduzido);
                    continue;
                }

                // O prompt de lore da obra ativa contextualiza a heurística: regras
                // de outra franquia (ex.: "freedom"→"liberdade" do SEED) não disparam.
                ResultadoDeteccaoLore deteccao = detector.auditar(
                    mascaraEn.texto(), mascaraPt.texto(), loreCanonica);
                if (!revisarTodasFalas && !deteccao.suspeito()) {
                    falasSemAlteracao[0]++;
                    sessao.out(AnsiCores.DIM + marcadorFala + " limpo pela heuristica" + AnsiCores.RESET);
                    novosEventos.add(evtTraduzido);
                    continue;
                }

                falasSinalizadas[0]++;
                Optional<String> correcaoDeterministica = corrigirLoreDeterministica(mascaraEn.texto(), mascaraPt.texto());
                if (correcaoDeterministica.isPresent()) {
                    String revisada = mascarador.desmascarar(correcaoDeterministica.get(), mascaraPt.tags());
                    try {
                        validador.validarFala(revisada);
                    } catch (Exception e) {
                        log.warn("Correcao deterministica de lore descartada (validacao): {}", e.getMessage());
                        sessao.out(AnsiCores.YELLOW + marcadorFala + " correcao deterministica descartada pela validacao: "
                            + e.getMessage() + AnsiCores.RESET);
                        falasDescartadas[0]++;
                        novosEventos.add(evtTraduzido);
                        continue;
                    }

                    ResultadoDeteccaoLore deteccaoPosterior = detector.auditar(
                        mascaraEn.texto(), mascarador.mascarar(revisada).texto(), loreCanonica);
                    if (!problemaLoreFoiResolvido(deteccao, deteccaoPosterior)) {
                        falasDescartadas[0]++;
                        sessao.out(AnsiCores.YELLOW + marcadorFala
                            + " pendente: correção determinística não eliminou todos os indícios de lore | motivos: "
                            + formatarMotivos(deteccaoPosterior.motivos()) + AnsiCores.RESET);
                        registrarAuditoria(
                            contextoId, nomePromptRevisao, revisarTodasFalas, arqTraduzido, i + 1,
                            dialogoAtual, totalDialogos, "PENDENTE_REGRA_LORE_INCOMPLETA",
                            deteccaoPosterior.motivos(), textoEn, textoPt,
                            correcaoDeterministica.get(), textoPt,
                            "Correção determinística não eliminou todos os indícios de lore"
                        );
                        novosEventos.add(evtTraduzido);
                        continue;
                    }

                    novosEventos.add(evtTraduzido.comTexto(revisada));
                    houveModificacao = true;
                    corrigidasNoArquivo++;
                    falasCorrigidas[0]++;
                    sessao.out(AnsiCores.GREEN + marcadorFala + " corrigida por regra de lore | Antes: "
                        + trecho(textoPt) + " | Depois: " + trecho(revisada) + AnsiCores.RESET);
                    registrarAuditoria(
                        contextoId, nomePromptRevisao, revisarTodasFalas, arqTraduzido, i + 1,
                        dialogoAtual, totalDialogos, "CORRIGIDA_REGRA_LORE", deteccao.motivos(),
                        textoEn, textoPt, correcaoDeterministica.get(), revisada, null
                    );
                    continue;
                }

                sessao.out(AnsiCores.YELLOW + marcadorFala + " enviada ao LLM | motivos: "
                    + formatarMotivos(deteccao.motivos()) + AnsiCores.RESET);

                Optional<String> revisadaOpt = revisorLoreLlm.revisar(
                    promptSistemaRevisaoLore,
                    mascaraEn.texto(),
                    mascaraPt.texto(),
                    deteccao.motivos()
                );

                if (revisadaOpt.isEmpty()) {
                    falasSemResposta[0]++;
                    sessao.out(AnsiCores.YELLOW + marcadorFala + " LLM sem resposta valida; mantendo traducao atual" + AnsiCores.RESET);
                    registrarAuditoria(
                        contextoId, nomePromptRevisao, revisarTodasFalas, arqTraduzido, i + 1,
                        dialogoAtual, totalDialogos, "SEM_RESPOSTA", deteccao.motivos(),
                        textoEn, textoPt, null, textoPt, "LLM sem resposta valida"
                    );
                    novosEventos.add(evtTraduzido);
                    continue;
                }

                String revisada;
                try {
                    revisada = mascarador.desmascarar(revisadaOpt.get(), mascaraPt.tags());
                    if (protecaoAss.respostaSuspeita(textoEn, revisada)) {
                        falasDescartadas[0]++;
                        sessao.out(AnsiCores.YELLOW + marcadorFala
                            + " revisão descartada por resposta suspeita em linha ASS pesada"
                            + AnsiCores.RESET);
                        registrarAuditoria(
                            contextoId, nomePromptRevisao, revisarTodasFalas, arqTraduzido, i + 1,
                            dialogoAtual, totalDialogos, "DESCARTADA_ASS_PESADO", deteccao.motivos(),
                            textoEn, textoPt, revisadaOpt.get(), textoPt,
                            "Resposta suspeita para linha ASS pesada"
                        );
                        novosEventos.add(evtTraduzido);
                        continue;
                    }
                    if (mesmaFalaVisivel(revisada, textoPt)) {
                        if (deteccao.suspeito()) {
                            falasDescartadas[0]++;
                            sessao.out(AnsiCores.YELLOW + marcadorFala
                                + " pendente: LLM nao alterou a fala suspeita" + AnsiCores.RESET);
                            registrarAuditoria(
                                contextoId, nomePromptRevisao, revisarTodasFalas, arqTraduzido, i + 1,
                                dialogoAtual, totalDialogos, "PENDENTE_SEM_MELHORIA", deteccao.motivos(),
                                textoEn, textoPt, revisadaOpt.get(), textoPt,
                                "Resposta manteve os indícios originais de lore"
                            );
                        } else {
                            falasSemAlteracao[0]++;
                            sessao.out(AnsiCores.DIM + marcadorFala + " conforme apos revisao LLM" + AnsiCores.RESET);
                            registrarAuditoria(
                                contextoId, nomePromptRevisao, revisarTodasFalas, arqTraduzido, i + 1,
                                dialogoAtual, totalDialogos, "CONFORME", deteccao.motivos(),
                                textoEn, textoPt, revisadaOpt.get(), textoPt, null
                            );
                        }
                        novosEventos.add(evtTraduzido);
                        continue;
                    }
                } catch (Exception e) {
                    log.warn("Revisao de lore descartada (falha de tags/alucinacao): {}", e.getMessage());
                    falasDescartadas[0]++;
                    sessao.out(AnsiCores.YELLOW + marcadorFala + " revisao descartada por falha de tags: "
                        + e.getMessage() + AnsiCores.RESET);
                    registrarAuditoria(
                        contextoId, nomePromptRevisao, revisarTodasFalas, arqTraduzido, i + 1,
                        dialogoAtual, totalDialogos, "DESCARTADA_TAGS", deteccao.motivos(),
                        textoEn, textoPt, revisadaOpt.get(), textoPt, e.getMessage()
                    );
                    novosEventos.add(evtTraduzido);
                    continue;
                }

                try {
                    validador.validarFala(revisada);
                } catch (Exception e) {
                    log.warn("Revisao de lore descartada (validacao): {}", e.getMessage());
                    falasDescartadas[0]++;
                    sessao.out(AnsiCores.YELLOW + marcadorFala + " revisao descartada pela validacao: "
                        + e.getMessage() + AnsiCores.RESET);
                    registrarAuditoria(
                        contextoId, nomePromptRevisao, revisarTodasFalas, arqTraduzido, i + 1,
                        dialogoAtual, totalDialogos, "DESCARTADA_VALIDACAO", deteccao.motivos(),
                        textoEn, textoPt, revisadaOpt.get(), revisada, e.getMessage()
                    );
                    novosEventos.add(evtTraduzido);
                    continue;
                }

                Optional<String> violacaoEscopo = ValidadorCandidatoLoreService.validar(
                    mascaraEn.texto(), mascaraPt.texto(), mascarador.mascarar(revisada).texto(), loreCanonica);
                if (violacaoEscopo.isPresent()) {
                    falasDescartadas[0]++;
                    sessao.out(AnsiCores.YELLOW + marcadorFala
                        + " pendente: proposta fora do escopo seguro de lore — "
                        + violacaoEscopo.get() + AnsiCores.RESET);
                    registrarAuditoria(
                        contextoId, nomePromptRevisao, revisarTodasFalas, arqTraduzido, i + 1,
                        dialogoAtual, totalDialogos, "PENDENTE_ESCOPO_LORE", deteccao.motivos(),
                        textoEn, textoPt, revisadaOpt.get(), textoPt, violacaoEscopo.get()
                    );
                    novosEventos.add(evtTraduzido);
                    continue;
                }

                if (deteccao.suspeito()) {
                    String revisadaMascarada = mascarador.mascarar(revisada).texto();
                    ResultadoDeteccaoLore deteccaoPosterior = detector.auditar(
                        mascaraEn.texto(), revisadaMascarada, loreCanonica);
                    if (!problemaLoreFoiResolvido(deteccao, deteccaoPosterior)) {
                        falasDescartadas[0]++;
                        sessao.out(AnsiCores.YELLOW + marcadorFala
                            + " pendente: proposta do LLM ainda contém indícios de lore | motivos: "
                            + formatarMotivos(deteccaoPosterior.motivos()) + AnsiCores.RESET);
                        registrarAuditoria(
                            contextoId, nomePromptRevisao, revisarTodasFalas, arqTraduzido, i + 1,
                            dialogoAtual, totalDialogos, "PENDENTE_LORE_NAO_RESOLVIDA",
                            deteccaoPosterior.motivos(), textoEn, textoPt, revisadaOpt.get(), textoPt,
                            "Proposta não eliminou os indícios de lore"
                        );
                        novosEventos.add(evtTraduzido);
                        continue;
                    }
                }

                if (deteccao.motivos().isEmpty()) {
                    falasSemAlteracao[0]++;
                    sessao.out(AnsiCores.YELLOW + marcadorFala
                        + " conforme; proposta preventiva ignorada por não haver indício de lore"
                        + AnsiCores.RESET);
                    registrarAuditoria(
                        contextoId, nomePromptRevisao, revisarTodasFalas, arqTraduzido, i + 1,
                        dialogoAtual, totalDialogos, "CONFORME_PROPOSTA_PREVENTIVA_IGNORADA", deteccao.motivos(),
                        textoEn, textoPt, revisadaOpt.get(), textoPt,
                        "Alteracao proposta em fala sem motivo heuristico de lore"
                    );
                    novosEventos.add(evtTraduzido);
                    continue;
                }

                novosEventos.add(evtTraduzido.comTexto(revisada));
                houveModificacao = true;
                corrigidasNoArquivo++;
                falasCorrigidas[0]++;
                sessao.out(AnsiCores.GREEN + marcadorFala + " corrigida | Antes: "
                    + trecho(textoPt) + " | Depois: " + trecho(revisada) + AnsiCores.RESET);
                registrarAuditoria(
                    contextoId, nomePromptRevisao, revisarTodasFalas, arqTraduzido, i + 1,
                    dialogoAtual, totalDialogos, "CORRIGIDA", deteccao.motivos(),
                    textoEn, textoPt, revisadaOpt.get(), revisada, null
                );
            }

            if (houveModificacao) {
                DocumentoLegenda revisado = new DocumentoLegenda(
                    docTraduzido.cabecalho(),
                    novosEventos,
                    docTraduzido.quebraDeLinha(),
                    docTraduzido.comBom()
                );
                Path backup = criarBackup(arqTraduzido, pastaBackup);
                escritor.escrever(arqTraduzido, revisado);
                arquivosAlterados[0]++;
                sessao.out(AnsiCores.GREEN + "  [Revisado] " + arqTraduzido.getFileName()
                    + " (" + corrigidasNoArquivo + " fala(s) corrigida(s))" + AnsiCores.RESET);
                if (backup != null) {
                    sessao.out(AnsiCores.CYAN + "  Backup anterior: " + backup + AnsiCores.RESET);
                }
            } else {
                sessao.out(AnsiCores.DIM + "  [OK]     " + arqTraduzido.getFileName() + " (lore conforme)" + AnsiCores.RESET);
            }

        } catch (Exception e) {
            String msg = "Falha ao revisar lore em " + arqTraduzido.getFileName() + ": " + e.getMessage();
            log.error(msg, e);
            sessao.out(AnsiCores.RED + "  [Erro] " + msg + AnsiCores.RESET);
            erros.add(msg);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: persiste o dataset detalhado da sessão e registra
     * seu resumo na telemetria canônica compartilhada.
     * <p>INVARIANTES DO DOMÍNIO: status e contadores no detalhe canônico são os
     * mesmos do relatório JSON; sessão sem trabalho não polui o agregado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: registra aviso, mantém a operação em
     * memória quando possível e retorna {@code null} para o caminho do JSON.
     */
    private String persistirRelatorioJson(
        SessaoRevisao sessao,
        String contextoId,
        boolean revisarTodasFalas,
        StatusRevisaoLore status,
        Path pastaOriginal,
        Path pastaTraduzida,
        long duracaoMs,
        int arquivosAnalisados,
        int arquivosAlterados,
        int falasAuditadas,
        int falasSinalizadas,
        int falasCorrigidas,
        int falasSemAlteracao,
        int falasSemResposta,
        int falasDescartadas,
        int falasEncaminhadasOpcao6,
        int falasPendentes,
        List<String> erros
    ) {
        String detalhe = montarDetalheTelemetria(
            pastaTraduzida,
            gerenciadorPromptRevisaoLore.obterNome(contextoId),
            status,
            falasPendentes,
            falasSemResposta,
            falasDescartadas,
            falasEncaminhadasOpcao6,
            erros.size()
        );

        OperacaoTelemetria operacao = TelemetriaService.criarOperacao(
            "Revisao de Lore (.ass LLM)",
            detalhe,
            duracaoMs,
            arquivosAnalisados,
            falasSinalizadas,
            falasCorrigidas
        );

        RevisaoLoreRelatorioJson relatorio = new RevisaoLoreRelatorioJson(
            "revisao_lore",
            operacao,
            new RevisaoLoreRelatorioJson.ContextoObra(
                contextoId,
                gerenciadorPromptRevisaoLore.obterNome(contextoId)
            ),
            new RevisaoLoreRelatorioJson.PastasOperacao(
                pastaOriginal.toAbsolutePath().toString(),
                pastaTraduzida.toAbsolutePath().toString()
            ),
            revisarTodasFalas ? "todas_as_falas" : "apenas_sinalizadas",
            new RevisaoLoreRelatorioJson.MetricasRevisaoLore(
                status,
                duracaoMs,
                formatarDuracaoMs(duracaoMs),
                arquivosAnalisados,
                arquivosAlterados,
                falasAuditadas,
                falasSinalizadas,
                falasCorrigidas,
                falasSemAlteracao,
                falasSemResposta,
                falasDescartadas,
                falasEncaminhadasOpcao6,
                falasPendentes,
                erros.size()
            ),
            List.copyOf(erros),
            List.copyOf(sessao.eventos)
        );

        try {
            Path arquivo = logPersistencia.salvarRelatorioJson(pastaTraduzida, relatorio);
            if (deveRegistrarTelemetria(arquivosAnalisados, falasAuditadas, falasSinalizadas, falasCorrigidas)) {
                telemetriaService.registrarOperacao(operacao);
                telemetriaService.salvar(TelemetriaService.resolverPastaRelatorios(pastaTraduzida));
            } else {
                sessao.out(AnsiCores.YELLOW
                    + "Telemetria canonica nao registrada: revisao de lore sem arquivos .ass analisados."
                    + AnsiCores.RESET);
            }
            return arquivo.toString();
        } catch (IOException e) {
            log.warn("Falha ao salvar relatorio JSON da revisao de lore: {}", e.getMessage());
            sessao.out(AnsiCores.YELLOW + "Aviso: nao foi possivel salvar o relatorio JSON em disco." + AnsiCores.RESET);
            telemetriaService.registrarOperacao(operacao);
            return null;
        }
    }

    private boolean deveRegistrarTelemetria(
        int arquivosAnalisados,
        int falasAuditadas,
        int falasSinalizadas,
        int falasCorrigidas
    ) {
        return arquivosAnalisados > 0 || falasAuditadas > 0 || falasSinalizadas > 0 || falasCorrigidas > 0;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: serializa no resumo canônico os campos mínimos que
     * permitem descobrir sessões falhas ou incompletas sem abrir o relatório.
     * <p>INVARIANTES DO DOMÍNIO: status e contadores pertencem à mesma sessão e
     * usam chaves estáveis para mineração posterior.
     * <p>COMPORTAMENTO EM CASO DE FALHA: valores são convertidos diretamente;
     * caminho e prompt nulos são representados textualmente sem exceção.
     */
    static String montarDetalheTelemetria(
        Path pastaTraduzida,
        String nomePrompt,
        StatusRevisaoLore status,
        int falasPendentes,
        int falasSemResposta,
        int falasDescartadas,
        int falasEncaminhadasOpcao6,
        int totalErros
    ) {
        return String.valueOf(pastaTraduzida != null ? pastaTraduzida.toAbsolutePath() : null)
            + " | promptRevisaoLore=" + nomePrompt
            + " | status=" + (status != null ? status.name() : "DESCONHECIDO")
            + " | pendentes=" + falasPendentes
            + " | semResposta=" + falasSemResposta
            + " | descartadas=" + falasDescartadas
            + " | encaminhadasOpcao6=" + falasEncaminhadasOpcao6
            + " | erros=" + totalErros;
    }

    private void registrarAuditoria(
        String contextoId,
        String contextoNome,
        boolean revisarTodasFalas,
        Path arquivo,
        int indiceEvento,
        int falaAtual,
        int totalFalas,
        String resultado,
        List<String> motivos,
        String originalEn,
        String traducaoAntes,
        String respostaLlm,
        String traducaoDepois,
        String detalhe
    ) {
        auditoriaCache.registrar(new EntradaAuditoriaRevisaoLore(
            Instant.now().toString(),
            contextoId,
            contextoNome,
            revisarTodasFalas ? "todas_as_falas" : "apenas_sinalizadas",
            arquivo != null ? arquivo.toAbsolutePath().toString() : null,
            indiceEvento,
            falaAtual,
            totalFalas,
            resultado,
            motivos != null ? List.copyOf(motivos) : List.of(),
            originalEn,
            traducaoAntes,
            respostaLlm,
            traducaoDepois,
            detalhe
        ));
    }

    private String formatarDuracaoMs(long ms) {
        long segundos = ms / 1000;
        return segundos >= 60 ? (segundos / 60) + "min " + (segundos % 60) + "s" : segundos + "s";
    }

    private static String formatarDuracaoDetalhada(long ms) {
        long totalSegundos = ms / 1000;
        long minutos = totalSegundos / 60;
        long segundos = totalSegundos % 60;
        long millis = ms % 1000;
        if (minutos > 0) {
            return "%02d:%02d.%03d".formatted(minutos, segundos, millis);
        }
        return "%02d.%03ds".formatted(segundos, millis);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: localiza a legenda PT-BR correspondente ao arquivo
     * inglês respeitando as três convenções de nome aceitas pelo pipeline.
     * <p>INVARIANTES DO DOMÍNIO: a prioridade é {@code _PT-BR}, depois
     * {@code _PTBR} e por último o mesmo nome do original.
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve o último candidato mesmo que
     * não exista, permitindo que o chamador produza o diagnóstico operacional.
     */
    private Path localizarArquivoTraduzido(Path arqOriginal, Path pastaTraduzida) {
        String nomeOriginal = arqOriginal.getFileName().toString();
        String nomeBase = nomeOriginal.substring(0, nomeOriginal.lastIndexOf('.'));
        Path candidato = pastaTraduzida.resolve(nomeBase + "_PT-BR.ass");
        if (Files.exists(candidato)) {
            return candidato;
        }
        candidato = pastaTraduzida.resolve(nomeBase + "_PTBR.ass");
        if (Files.exists(candidato)) {
            return candidato;
        }
        return pastaTraduzida.resolve(nomeOriginal);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: calcula antes do processamento o denominador global
     * mostrado no console para o operador acompanhar o lote inteiro.
     * <p>INVARIANTES DO DOMÍNIO: somente pares existentes, com mesma quantidade
     * de eventos e alinhamento temporal válido entram no total.
     * <p>COMPORTAMENTO EM CASO DE FALHA: um par ilegível é ignorado na prévia e
     * será diagnosticado normalmente quando chegar sua vez de processamento.
     */
    private int contarDialogosAuditaveisNoLote(List<Path> originais, Path pastaTraduzida) {
        int total = 0;
        for (Path arqOriginal : originais) {
            Path arqTraduzido = localizarArquivoTraduzido(arqOriginal, pastaTraduzida);
            if (!Files.exists(arqTraduzido)) {
                continue;
            }
            try {
                DocumentoLegenda original = leitor.ler(arqOriginal);
                DocumentoLegenda traduzido = leitor.ler(arqTraduzido);
                if (original.eventos().size() == traduzido.eventos().size()
                    && primeiraDivergenciaEstrutural(
                        original, traduzido, ALINHAMENTO_TOLERANCIA_MS).isEmpty()) {
                    total += contarDialogosAuditaveis(original, traduzido);
                }
            } catch (Exception e) {
                log.debug("Par ignorado na pré-contagem global de lore: {}", arqOriginal, e);
            }
        }
        return total;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: apresenta simultaneamente a posição do arquivo e da
     * fala no lote completo, sem reiniciar a percepção de progresso a cada ASS.
     * <p>INVARIANTES DO DOMÍNIO: posições nunca são exibidas abaixo de um e o
     * denominador de falas nunca fica menor que a posição atual.
     * <p>COMPORTAMENTO EM CASO DE FALHA: totais ausentes ou zero são ajustados
     * conservadoramente à posição corrente.
     */
    static String formatarMarcadorProgresso(
        int indiceArquivo, int totalArquivos, int falaGlobal, int totalFalasGlobais, int indiceEvento) {
        int arquivoAtualSeguro = Math.max(1, indiceArquivo);
        int totalArquivosSeguro = Math.max(arquivoAtualSeguro, totalArquivos);
        int falaAtualSegura = Math.max(1, falaGlobal);
        int totalFalasSeguro = Math.max(falaAtualSegura, totalFalasGlobais);
        return "[Arquivo " + arquivoAtualSeguro + "/" + totalArquivosSeguro
            + " | Fala " + falaAtualSegura + "/" + totalFalasSeguro
            + " | evento " + Math.max(1, indiceEvento) + "]";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: separa tradução linguística pendente de revisão de
     * lore para evitar chamadas caras ao modelo no módulo errado.
     * <p>INVARIANTES DO DOMÍNIO: somente falas visivelmente idênticas entre EN
     * e PT-BR são encaminhadas à Opção 6; tags ASS e diferenças de caixa não
     * alteram essa decisão.
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto vazio não é classificado como
     * fala não traduzida e permanece sob as validações existentes.
     */
    static boolean ehFalaNaoTraduzida(String originalIngles, String traducaoPt) {
        String originalVisivel = normalizarFalaVisivel(originalIngles);
        String traducaoVisivel = normalizarFalaVisivel(traducaoPt);
        return !originalVisivel.isBlank() && originalVisivel.equalsIgnoreCase(traducaoVisivel);
    }

    private int contarDialogosAuditaveis(DocumentoLegenda docOriginal, DocumentoLegenda docTraduzido) {
        int total = 0;
        int limite = Math.min(docOriginal.eventos().size(), docTraduzido.eventos().size());
        for (int i = 0; i < limite; i++) {
            EventoLegenda original = docOriginal.eventos().get(i);
            EventoLegenda traduzido = docTraduzido.eventos().get(i);
            if (ehEventoAuditavelLore(original, traduzido)) {
                total++;
            }
        }
        return total;
    }

    private boolean ehEventoAuditavelLore(EventoLegenda original, EventoLegenda traduzido) {
        if (!original.isDialogo() || !traduzido.isDialogo()
            || !original.temTexto() || !traduzido.temTexto()) {
            return false;
        }

        String textoOriginal = original.texto();
        if (politicaEstiloMusical.estiloIgnorado(original.estilo())) {
            return false;
        }
        if (PADRAO_DESENHO_VETORIAL.matcher(textoOriginal).find()) {
            return false;
        }
        if (protecaoAss.deveIgnorarIntervencaoIa(original.estilo(), textoOriginal)) {
            return false;
        }
        if (detectorKaraoke.eEfeitoKaraoke(textoOriginal)
            && !detectorKaraoke.eKaraokeOuMusicaTraduzivel(original.estilo(), textoOriginal)) {
            return false;
        }
        return mascarador.contemTextoTraduzivel(textoOriginal);
    }

    private static boolean mesmaFalaVisivel(String revisada, String atual) {
        return normalizarFalaVisivel(revisada).equals(normalizarFalaVisivel(atual));
    }

    private static String normalizarFalaVisivel(String texto) {
        if (texto == null) {
            return "";
        }
        return PADRAO_INVISIVEIS.matcher(PADRAO_TAG_ASS.matcher(texto).replaceAll(""))
            .replaceAll("")
            .replace("\\N", " ")
            .replace("\\n", " ")
            .replace("\\h", " ")
            .replaceAll("\\s+", " ")
            .strip();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se uma proposta eliminou de fato os
     * indícios de lore que motivaram sua revisão.
     * <p>INVARIANTES DO DOMÍNIO: uma fala inicialmente suspeita só é resolvida
     * quando a auditoria posterior está limpa; fala inicialmente limpa não
     * exige redução de indícios.
     * <p>COMPORTAMENTO EM CASO DE FALHA: resultados ausentes são conservadores
     * e nunca autorizam gravar uma proposta para fala suspeita.
     */
    static boolean problemaLoreFoiResolvido(
        ResultadoDeteccaoLore antes,
        ResultadoDeteccaoLore depois
    ) {
        if (antes == null) return false;
        if (!antes.suspeito()) return true;
        return depois != null && !depois.suspeito();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: confirma que a legenda EN e a PT descrevem a MESMA
     * sequência de falas antes de a revisão de lore parear evento por evento.
     * Contagem igual de eventos não basta — um Comment reposicionado ou diálogos
     * reordenados alinham por índice, mas trocam o contexto que o LLM recebe.
     *
     * <p>INVARIANTES DO DOMÍNIO: compara tipo (Dialogue/Comment) e os tempos
     * Start/End por evento, com tolerância {@code tolMs} (traduções fiéis mantêm
     * o tempo idêntico; jitter de arredondamento é aceito). Estilo é ignorado de
     * propósito: restyle da PT é legítimo e não indica desalinhamento. Eventos
     * com tempo ilegível não bloqueiam (evita falso positivo por formato exótico).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: retorna {@link Optional#empty()} se os
     * pares estão alinhados; caso contrário, um texto descrevendo a PRIMEIRA
     * divergência (para o chamador pular o arquivo, nunca reescrevê-lo).
     */
    static Optional<String> primeiraDivergenciaEstrutural(
        DocumentoLegenda en, DocumentoLegenda pt, long tolMs) {
        int[] posEn = posicoesTempoAss(en.cabecalho());
        int[] posPt = posicoesTempoAss(pt.cabecalho());
        int limite = Math.min(en.eventos().size(), pt.eventos().size());
        for (int i = 0; i < limite; i++) {
            EventoLegenda a = en.eventos().get(i);
            EventoLegenda b = pt.eventos().get(i);
            boolean aTempo = temTempoAss(a);
            boolean bTempo = temTempoAss(b);
            if (aTempo != bTempo) {
                return Optional.of("evento " + (i + 1) + ": estrutura divergente (EN "
                    + rotuloTipo(a) + " vs PT " + rotuloTipo(b) + ")");
            }
            if (!aTempo) {
                continue;
            }
            if (!a.tipoLinha().equals(b.tipoLinha())) {
                return Optional.of("evento " + (i + 1) + ": tipo divergente (EN "
                    + a.tipoLinha() + " vs PT " + b.tipoLinha() + ")");
            }
            String iniA = campoPrefixo(a, posEn[0]);
            String fimA = campoPrefixo(a, posEn[1]);
            String iniB = campoPrefixo(b, posPt[0]);
            String fimB = campoPrefixo(b, posPt[1]);
            long msIniA = tempoAssParaMs(iniA);
            long msFimA = tempoAssParaMs(fimA);
            long msIniB = tempoAssParaMs(iniB);
            long msFimB = tempoAssParaMs(fimB);
            if (msIniA < 0 || msFimA < 0 || msIniB < 0 || msFimB < 0) {
                continue;
            }
            if (Math.abs(msIniA - msIniB) > tolMs || Math.abs(msFimA - msFimB) > tolMs) {
                return Optional.of("evento " + (i + 1) + ": tempos divergentes (EN "
                    + iniA + "->" + fimA + " | PT " + iniB + "->" + fimB + ")");
            }
        }
        return Optional.empty();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: identifica eventos ASS que possuem intervalo de
     * exibição comparável entre original e tradução.
     * <p>INVARIANTES DO DOMÍNIO: somente Dialogue e Comment possuem tempo.
     * <p>COMPORTAMENTO EM CASO DE FALHA: tipo ausente ou desconhecido retorna falso.
     */
    private static boolean temTempoAss(EventoLegenda e) {
        return "Dialogue".equals(e.tipoLinha()) || "Comment".equals(e.tipoLinha());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: produz descrição didática do tipo de evento para o
     * diagnóstico de desalinhamento.
     * <p>INVARIANTES DO DOMÍNIO: tipo vazio representa linha não-evento.
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca retorna nulo.
     */
    private static String rotuloTipo(EventoLegenda e) {
        String t = e.tipoLinha();
        return (t == null || t.isEmpty()) ? "linha-nao-evento" : t;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: localiza Start e End no formato ASS para comparar
     * a mesma fala mesmo quando a ordem das colunas não é canônica.
     * <p>INVARIANTES DO DOMÍNIO: a linha Format da seção Events é a autoridade;
     * na ausência dela, usa as posições canônicas 1 e 2.
     * <p>COMPORTAMENTO EM CASO DE FALHA: cabeçalho ausente conserva o fallback
     * seguro sem lançar exceção.
     */
    private static int[] posicoesTempoAss(String cabecalho) {
        int start = 1;
        int end = 2;
        if (cabecalho != null) {
            for (String linha : cabecalho.split("\r\n|\n")) {
                String t = linha.trim();
                if (t.regionMatches(true, 0, "Format:", 0, 7)) {
                    String[] nomes = t.substring(t.indexOf(':') + 1).split(",");
                    for (int i = 0; i < nomes.length; i++) {
                        String nome = nomes[i].trim();
                        if (nome.equalsIgnoreCase("Start")) {
                            start = i;
                        } else if (nome.equalsIgnoreCase("End")) {
                            end = i;
                        }
                    }
                }
            }
        }
        return new int[]{start, end};
    }

    /**
     * PROPÓSITO DE NEGÓCIO: lê um campo estrutural do prefixo ASS para validar
     * o pareamento temporal sem tocar no texto traduzido.
     * <p>INVARIANTES DO DOMÍNIO: preserva campos vazios e não lê além do índice.
     * <p>COMPORTAMENTO EM CASO DE FALHA: prefixo ausente ou índice inválido
     * retorna texto vazio, que será tratado como tempo ilegível.
     */
    private static String campoPrefixo(EventoLegenda e, int idx) {
        String prefixo = e.prefixo();
        if (prefixo == null) {
            return "";
        }
        int sep = prefixo.indexOf(": ");
        String corpo = sep >= 0 ? prefixo.substring(sep + 2) : prefixo;
        String[] campos = corpo.split(",", -1);
        return (idx >= 0 && idx < campos.length) ? campos[idx].trim() : "";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: converte timestamp ASS em milissegundos para a
     * verificação tolerante de alinhamento EN/PT.
     * <p>INVARIANTES DO DOMÍNIO: interpreta a fração como centésimos.
     * <p>COMPORTAMENTO EM CASO DE FALHA: valor ausente ou ilegível retorna -1.
     */
    private static long tempoAssParaMs(String tempo) {
        if (tempo == null) {
            return -1;
        }
        Matcher m = PADRAO_TEMPO_ASS.matcher(tempo.strip());
        if (!m.matches()) {
            return -1;
        }
        long horas = Long.parseLong(m.group(1));
        long minutos = Long.parseLong(m.group(2));
        long segundos = Long.parseLong(m.group(3));
        long centesimos = Long.parseLong(m.group(4));
        return ((((horas * 60 + minutos) * 60 + segundos) * 100) + centesimos) * 10;
    }

    static Optional<String> corrigirLoreDeterministica(String originalMascarado, String traducaoMascarada) {
        if (originalMascarado == null || traducaoMascarada == null || traducaoMascarada.isBlank()) {
            return Optional.empty();
        }

        String corrigida = traducaoMascarada;
        if (PADRAO_SHIN.matcher(originalMascarado).find()
            && PADRAO_CANELA.matcher(corrigida).find()) {
            corrigida = PADRAO_CANELA.matcher(corrigida).replaceAll("Shin");
        }

        if (PADRAO_DUD_ROUNDS.matcher(originalMascarado).find()
            && PADRAO_RODADAS_ALEATORIAS.matcher(corrigida).find()) {
            corrigida = PADRAO_RODADAS_ALEATORIAS.matcher(corrigida).replaceAll("munições falhas");
        }

        if (corrigida.equals(traducaoMascarada)) {
            return Optional.empty();
        }
        return Optional.of(corrigida);
    }

    private String formatarMotivos(List<String> motivos) {
        if (motivos == null || motivos.isEmpty()) {
            return "revisao preventiva";
        }
        return String.join(" | ", motivos.stream().map(this::trecho).toList());
    }

    private String trecho(String texto) {
        if (texto == null || texto.isBlank()) {
            return "(vazio)";
        }
        String normalizado = texto
            .replace("\\N", " ")
            .replace("\\n", " ")
            .replaceAll("\\s+", " ")
            .strip();
        if (normalizado.length() <= TAMANHO_TRECHO_LOG) {
            return normalizado;
        }
        return normalizado.substring(0, TAMANHO_TRECHO_LOG - 3).stripTrailing() + "...";
    }

    private static String inferirNivel(String mensagem) {
        if (mensagem.contains("[Erro]") || mensagem.contains("Falha")) {
            return "ERROR";
        }
        if (mensagem.contains("[Pulado]") || mensagem.contains("Aviso:")) {
            return "WARN";
        }
        if (mensagem.contains("[Revisado]") || mensagem.contains("concluida com sucesso")) {
            return "SUCCESS";
        }
        return "INFO";
    }

    private static String removerAnsi(String texto) {
        return texto.replaceAll((char) 27 + "\\[[0-9;]*m", "");
    }
}
