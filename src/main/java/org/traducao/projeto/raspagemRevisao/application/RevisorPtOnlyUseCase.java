package org.traducao.projeto.raspagemRevisao.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
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
import java.util.Set;
import java.util.stream.Stream;

/**
 * PROPÓSITO DE NEGÓCIO: revisa legendas que só existem em português (o {@code .ass} PT-BR, SEM
 * o inglês original e SEM o cache bilíngue) aplicando correções determinísticas PT-side. Atende
 * o pedido do Paulo — "às vezes só tenho a tradução PT-BR", como na revisão de lore. É o modo
 * "correção do português sem inglês e sem legendas": não retraduz nada; só arruma o que dá para
 * arrumar olhando apenas o PT (acentos, {@code \N} órfão, concordância PT-only) e SINALIZA
 * asteriscos de censura para revisão manual.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>NUNCA sobrescreve sem backup: cada arquivo alterado é copiado para uma subpasta de
 *       backup timestampada antes de ser regravado ("olhar antes de destruir").</li>
 *   <li>{@code aplicar=false} é dry-run: só conta e reporta, não escreve nada.</li>
 *   <li>Só reescreve arquivos que realmente mudaram; eventos sem texto passam intactos; a
 *       estrutura do documento (cabeçalho, quebra de linha, BOM) é preservada.</li>
 *   <li>Não usa inglês nem cache: nenhuma correção depende do original.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Pasta inexistente devolve resultado vazio; falha de I/O ao ler/gravar um arquivo é registrada
 * e o arquivo é pulado (os demais seguem). Falha ao criar o backup ABORTA a gravação daquele
 * arquivo (preserva o original).
 */
@Service
public class RevisorPtOnlyUseCase {

    private static final Logger log = LoggerFactory.getLogger(RevisorPtOnlyUseCase.class);
    private static final Set<String> EXTENSOES = Set.of(".ass", ".ssa");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final String PASTA_BACKUP = "backup_revisao_ptonly";

    private final LeitorLegendaAss leitor;
    private final EscritorLegendaAss escritor;
    private final RevisorPtOnlyService revisor;
    private final TelemetriaService telemetriaService;

    public RevisorPtOnlyUseCase(LeitorLegendaAss leitor, EscritorLegendaAss escritor,
            RevisorPtOnlyService revisor, TelemetriaService telemetriaService) {
        this.leitor = leitor;
        this.escritor = escritor;
        this.telemetriaService = telemetriaService;
        this.revisor = revisor;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: resumo da revisão PT-only de uma pasta — quantos arquivos foram
     * vistos/alterados, quais falas ficaram com asterisco (para revisão manual) e os backups.
     * <p>INVARIANTES DO DOMÍNIO: {@code falasComAsterisco} é só para relatório; nunca some fala.
     * <p>COMPORTAMENTO EM CASO DE FALHA: portador de dados puro.
     */
    public record ResultadoPtOnly(
        int arquivosAnalisados,
        int arquivosAlterados,
        int falasAlteradas,
        List<String> falasComAsterisco,
        List<Path> backups,
        boolean aplicado
    ) {}

    /**
     * PROPÓSITO DE NEGÓCIO: revisa todos os {@code .ass}/{@code .ssa} de uma pasta no modo PT-only.
     * <p>INVARIANTES DO DOMÍNIO: com {@code aplicar=false} nada é gravado; com {@code true} cada
     * arquivo alterado é copiado para backup antes de ser regravado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: pasta ausente devolve resultado zerado; erro por arquivo
     * é logado e o arquivo é pulado.
     *
     * @param pasta pasta com as legendas PT-BR
     * @param aplicar false = dry-run (só relatório); true = grava com backup
     * @return {@link ResultadoPtOnly} com contagens, falas com asterisco e backups
     */
    public ResultadoPtOnly revisarPasta(Path pasta, boolean aplicar) {
        long inicioMs = System.currentTimeMillis();
        if (pasta == null || !Files.isDirectory(pasta)) {
            return new ResultadoPtOnly(0, 0, 0, List.of(), List.of(), aplicar);
        }
        List<Path> arquivos;
        try (Stream<Path> stream = Files.list(pasta)) {
            arquivos = stream.filter(Files::isRegularFile).filter(this::temExtensaoSuportada).sorted().toList();
        } catch (IOException e) {
            log.warn("Falha ao listar a pasta {} para revisão PT-only: {}", pasta, e.getMessage());
            return new ResultadoPtOnly(0, 0, 0, List.of(), List.of(), aplicar);
        }

        int analisados = 0;
        int alterados = 0;
        int falasAlteradas = 0;
        List<String> comAsterisco = new ArrayList<>();
        List<Path> backups = new ArrayList<>();

        for (Path arquivo : arquivos) {
            analisados++;
            try {
                DocumentoLegenda documento = leitor.ler(arquivo);
                List<EventoLegenda> novos = new ArrayList<>(documento.eventos().size());
                int alteradasNoArquivo = 0;
                for (EventoLegenda evento : documento.eventos()) {
                    if (!evento.temTexto()) {
                        novos.add(evento);
                        continue;
                    }
                    RevisorPtOnlyService.ResultadoFala rf = revisor.revisarFala(evento.texto());
                    if (rf.temAsterisco()) {
                        comAsterisco.add(arquivo.getFileName() + ": " + evento.texto());
                    }
                    if (rf.alterado()) {
                        alteradasNoArquivo++;
                        novos.add(evento.comTexto(rf.texto()));
                    } else {
                        novos.add(evento);
                    }
                }
                if (alteradasNoArquivo == 0) {
                    continue;
                }
                falasAlteradas += alteradasNoArquivo;
                alterados++;
                if (aplicar) {
                    Path backup = criarBackup(pasta, arquivo);
                    backups.add(backup);
                    DocumentoLegenda revisado = new DocumentoLegenda(
                        documento.cabecalho(), novos, documento.quebraDeLinha(), documento.comBom());
                    escritor.escrever(arquivo, revisado);
                }
            } catch (IOException | RuntimeException e) {
                log.warn("Revisão PT-only pulou {} por erro: {}", arquivo, e.getMessage());
            }
        }
        telemetriaService.registrarOperacao(new OperacaoTelemetria(
            "Revisão PT-only",
            "Pasta: " + pasta.getFileName() + (aplicar ? " (aplicado)" : " (simulado)"),
            System.currentTimeMillis() - inicioMs,
            analisados,
            falasAlteradas,
            falasAlteradas,
            Instant.now().toString()));
        return new ResultadoPtOnly(analisados, alterados, falasAlteradas, List.copyOf(comAsterisco),
            List.copyOf(backups), aplicar);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: copia o arquivo para uma subpasta de backup timestampada antes de
     * regravá-lo, para nenhuma correção destruir a versão anterior.
     * <p>INVARIANTES DO DOMÍNIO: o backup preserva os atributos; a subpasta é criada se faltar.
     * <p>COMPORTAMENTO EM CASO DE FALHA: falha de cópia lança {@link IOException}, abortando a
     * gravação do arquivo (o original fica intacto).
     */
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
