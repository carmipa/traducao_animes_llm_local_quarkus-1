package org.traducao.projeto.revisaoLore.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.revisaoLore.domain.StatusRevisaoLoreLlm;
import org.traducao.projeto.revisaoLore.domain.ports.RevisorLoreLlmPort;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * PROPÓSITO DE NEGÓCIO: revisa a terminologia de lore de legendas que só existem em português
 * (o {@code .ass} PT-BR, SEM o inglês original), para o caso em que Paulo só tem a tradução —
 * é a aba "Revisão de Lore PT-only". Sem o EN não há como desambiguar homógrafo de uma palavra,
 * então trabalha em duas camadas: (1) DETERMINÍSTICA — aplica os termos INEQUÍVOCOS (forma-ruim
 * multi-palavra) via {@link CorretorLoreDeterministico#corrigirPtOnly}; (2) LLM PT-only opcional
 * — só nas falas que contêm um homógrafo de UMA palavra do mapa (onde o determinístico não
 * arrisca), deixando o modelo julgar com a lore do contexto.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Nunca usa o inglês: a camada determinística ignora o gate de EN (só termos inequívocos)
 *       e a chamada ao LLM passa original vazio, guiada pela lore do contexto.</li>
 *   <li>NUNCA sobrescreve sem backup: cada arquivo alterado é copiado para subpasta timestampada
 *       antes de regravar; {@code aplicar=false} é dry-run (não escreve).</li>
 *   <li>Toda proposta (determinística ou do LLM) passa pela validação de resíduo/recusa antes de
 *       ser aceita; proposta reprovada é DESCARTADA e a fala fica como está.</li>
 *   <li>O LLM só é acionado quando disponível e habilitado, e apenas nas falas com homógrafo do
 *       mapa — evita uma chamada por fala.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Pasta/contexto ausentes devolvem resultado zerado; erro por arquivo é logado e o arquivo é
 * pulado; LLM indisponível apenas desliga a camada 2 (a determinística segue).
 */
@Service
public class RevisarLorePtOnlyUseCase {

    private static final Logger log = LoggerFactory.getLogger(RevisarLorePtOnlyUseCase.class);
    private static final Set<String> EXTENSOES = Set.of(".ass", ".ssa");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final String PASTA_BACKUP = "backup_revisao_lore_ptonly";

    private final LeitorLegendaAss leitor;
    private final EscritorLegendaAss escritor;
    private final MascaradorTags mascarador;
    private final CorretorLoreDeterministico corretorLore;
    private final RevisorLoreLlmPort revisorLoreLlm;
    private final GerenciadorPromptRevisaoLore gerenciadorPrompt;
    private final ValidadorTraducaoService validador;
    private final TelemetriaService telemetriaService;

    public RevisarLorePtOnlyUseCase(
        LeitorLegendaAss leitor,
        EscritorLegendaAss escritor,
        MascaradorTags mascarador,
        CorretorLoreDeterministico corretorLore,
        RevisorLoreLlmPort revisorLoreLlm,
        GerenciadorPromptRevisaoLore gerenciadorPrompt,
        ValidadorTraducaoService validador,
        TelemetriaService telemetriaService
    ) {
        this.leitor = leitor;
        this.escritor = escritor;
        this.mascarador = mascarador;
        this.corretorLore = corretorLore;
        this.revisorLoreLlm = revisorLoreLlm;
        this.gerenciadorPrompt = gerenciadorPrompt;
        this.validador = validador;
        this.telemetriaService = telemetriaService;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: resumo da revisão de lore PT-only de uma pasta.
     * <p>INVARIANTES DO DOMÍNIO: contagens refletem o que realmente mudou/foi descartado; nunca
     * some fala.
     * <p>COMPORTAMENTO EM CASO DE FALHA: portador de dados puro.
     */
    public record ResultadoLorePtOnly(
        int arquivosAnalisados,
        int arquivosAlterados,
        int falasCorrigidas,
        int falasDescartadas,
        List<Path> backups,
        boolean aplicado,
        boolean usouLlm
    ) {}

    /**
     * PROPÓSITO DE NEGÓCIO: revisa a lore de todos os {@code .ass}/{@code .ssa} de uma pasta PT-BR
     * sem o inglês.
     * <p>INVARIANTES DO DOMÍNIO: determinístico sempre; LLM só se {@code usarLlm} e disponível, e
     * só nas falas com homógrafo do mapa; backup antes de qualquer gravação.
     * <p>COMPORTAMENTO EM CASO DE FALHA: pasta/contexto ausentes devolvem zero; erro por arquivo
     * é pulado.
     *
     * @param pastaPt pasta com as legendas PT-BR
     * @param contextoId obra/contexto (carrega o mapa de terminologia e o prompt de lore)
     * @param usarLlm habilita a camada 2 (LLM PT-only nos homógrafos)
     * @param aplicar false = dry-run (só relatório); true = grava com backup
     * @return {@link ResultadoLorePtOnly} com contagens e backups
     */
    public ResultadoLorePtOnly executar(Path pastaPt, String contextoId, boolean usarLlm, boolean aplicar) {
        long inicioMs = System.currentTimeMillis();
        if (pastaPt == null || !Files.isDirectory(pastaPt) || contextoId == null || contextoId.isBlank()) {
            return new ResultadoLorePtOnly(0, 0, 0, 0, List.of(), aplicar, false);
        }
        Map<String, String> correcoes = gerenciadorPrompt.correcoesTerminologia(contextoId);
        Pattern homografosUmaPalavra = compilarHomografosUmaPalavra(correcoes);

        String promptSistema = null;
        boolean llmAtivo = false;
        if (usarLlm) {
            StatusRevisaoLoreLlm status = revisorLoreLlm.verificarDisponibilidade();
            llmAtivo = status.modeloCarregado();
            if (llmAtivo) {
                promptSistema = gerenciadorPrompt.obterPromptSistema(contextoId);
            } else {
                log.warn("Revisão de lore PT-only: LLM indisponível ({}); seguindo só com o determinístico.",
                    status.mensagem());
            }
        }

        List<Path> arquivos;
        try (Stream<Path> stream = Files.walk(pastaPt)) {
            arquivos = stream.filter(Files::isRegularFile).filter(this::temExtensaoSuportada).sorted().toList();
        } catch (IOException e) {
            log.warn("Falha ao varrer {} para revisão de lore PT-only: {}", pastaPt, e.getMessage());
            return new ResultadoLorePtOnly(0, 0, 0, 0, List.of(), aplicar, llmAtivo);
        }

        int analisados = 0;
        int alterados = 0;
        int falasCorrigidas = 0;
        int falasDescartadas = 0;
        List<Path> backups = new ArrayList<>();

        for (Path arquivo : arquivos) {
            analisados++;
            try {
                DocumentoLegenda documento = leitor.ler(arquivo);
                List<EventoLegenda> novos = new ArrayList<>(documento.eventos().size());
                int corrigidasArq = 0;
                for (EventoLegenda evento : documento.eventos()) {
                    if (!evento.temTexto()) {
                        novos.add(evento);
                        continue;
                    }
                    MascaradorTags.Mascarado m = mascarador.mascarar(evento.texto());
                    Optional<String> proposta = proporRevisao(m.texto(), correcoes, homografosUmaPalavra,
                        llmAtivo, promptSistema);
                    if (proposta.isEmpty() || proposta.get().equals(m.texto())) {
                        novos.add(evento);
                        continue;
                    }
                    String revisada = mascarador.desmascarar(proposta.get(), m.tags());
                    try {
                        validador.validarFala(revisada);
                    } catch (RuntimeException e) {
                        falasDescartadas++;
                        novos.add(evento);
                        continue;
                    }
                    novos.add(evento.comTexto(revisada));
                    corrigidasArq++;
                }
                if (corrigidasArq == 0) {
                    continue;
                }
                falasCorrigidas += corrigidasArq;
                alterados++;
                if (aplicar) {
                    backups.add(criarBackup(pastaPt, arquivo));
                    DocumentoLegenda revisado = new DocumentoLegenda(
                        documento.cabecalho(), novos, documento.quebraDeLinha(), documento.comBom());
                    escritor.escrever(arquivo, revisado);
                }
            } catch (IOException | RuntimeException e) {
                log.warn("Revisão de lore PT-only pulou {} por erro: {}", arquivo, e.getMessage());
            }
        }
        telemetriaService.registrarOperacao(new OperacaoTelemetria(
            "Revisão de Lore PT-only",
            "Pasta: " + pastaPt.getFileName() + (aplicar ? " (aplicado)" : " (simulado)")
                + (llmAtivo ? " + LLM" : ""),
            System.currentTimeMillis() - inicioMs,
            analisados,
            falasCorrigidas,
            falasCorrigidas,
            Instant.now().toString()));
        return new ResultadoLorePtOnly(analisados, alterados, falasCorrigidas, falasDescartadas,
            List.copyOf(backups), aplicar, llmAtivo);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: propõe a fala revisada — primeiro o determinístico inequívoco, e só
     * se ele não agir E a fala tiver um homógrafo do mapa, o LLM PT-only.
     * <p>INVARIANTES DO DOMÍNIO: o LLM só entra quando ativo e há homógrafo; original vazio.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem proposta devolve {@link Optional#empty()}.
     */
    private Optional<String> proporRevisao(String masc, Map<String, String> correcoes,
            Pattern homografos, boolean llmAtivo, String promptSistema) {
        Optional<String> deterministica = corretorLore.corrigirPtOnly(masc, correcoes);
        if (deterministica.isPresent()) {
            return deterministica;
        }
        if (llmAtivo && homografos != null && homografos.matcher(masc).find()) {
            return revisorLoreLlm.revisar(promptSistema, "", masc, List.of());
        }
        return Optional.empty();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: compila o padrão dos homógrafos de UMA palavra do mapa — as
     * forma-ruim que o determinístico PT-only pula e que direcionam o LLM.
     * <p>INVARIANTES DO DOMÍNIO: só chaves de uma palavra; fronteira de palavra; ignora caixa.
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa sem homógrafo de uma palavra devolve {@code null}.
     */
    private Pattern compilarHomografosUmaPalavra(Map<String, String> correcoes) {
        if (correcoes == null || correcoes.isEmpty()) {
            return null;
        }
        String alternancia = correcoes.keySet().stream()
            .filter(k -> k != null && !k.isBlank() && k.trim().indexOf(' ') < 0)
            .map(Pattern::quote)
            .collect(Collectors.joining("|"));
        if (alternancia.isEmpty()) {
            return null;
        }
        return Pattern.compile("(?<![\\p{L}\\p{N}])(?:" + alternancia + ")(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    private Path criarBackup(Path pasta, Path arquivo) throws IOException {
        Path dirBackup = pasta.resolve(PASTA_BACKUP);
        Files.createDirectories(dirBackup);
        String nome = arquivo.getFileName().toString();
        Path backup = dirBackup.resolve(nome + "." + LocalDateTime.now().format(TS) + ".bak");
        Files.copy(arquivo, backup, StandardCopyOption.COPY_ATTRIBUTES);
        return backup;
    }

    private boolean temExtensaoSuportada(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase();
        return EXTENSOES.stream().anyMatch(nome::endsWith);
    }
}
