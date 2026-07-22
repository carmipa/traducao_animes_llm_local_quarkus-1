package org.traducao.projeto.trocaTipoLegenda.application;

import org.traducao.projeto.core.io.DiretorioBaseKronos;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.core.presentation.ui.AnsiCores;
import org.traducao.projeto.core.util.DuracaoUtil;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.trocaTipoLegenda.domain.ResultadoTrocaFonte;
import org.traducao.projeto.trocaTipoLegenda.domain.exceptions.TrocaTipoLegendaException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

/**
 * PROPÓSITO DE NEGÓCIO: aplica em lote o "achatamento" de estilos decorativos numa
 * pasta de legendas .ass — transforma letras de abertura/encerramento e placas
 * (fontes decorativas como Dash Horizon/Althea/Androgyne, com {@code \pos}/{@code \fad})
 * em legendas brancas legíveis no estilo Default. É o passo que faltava na tela de
 * Troca de Legenda: a substituição de fontes só conserta fontes ANSI quebradas, e a
 * cura de tags PRESERVA a formatação — nenhum dos dois removia a frescura visual.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Nunca sobrescreve sem antes copiar o arquivo original para
 *       {@code backups/achatar_estilos_<timestamp>/} — a operação é reversível.</li>
 *   <li>Só grava arquivos em que houve achatamento efetivo; arquivos sem estilo
 *       decorativo são pulados byte a byte.</li>
 *   <li>A pasta de destino da gravação é a MESMA de origem (edição in-place com
 *       backup); a extensão e o conteúdo não-decorativo são preservados pelo par
 *       {@link LeitorLegendaAss}/{@link EscritorLegendaAss}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Diretório inválido lança {@link TrocaTipoLegendaException}. Falha ao ler/gravar um
 * arquivo específico é registrada no console e contabilizada, sem abortar os demais;
 * o backup já feito permanece para reversão manual. Pasta sem legendas devolve um
 * resultado zerado, sem exceção.
 */
@Service
public class AchatarEstilosUseCase {

    private static final Logger log = LoggerFactory.getLogger(AchatarEstilosUseCase.class);
    private static final DateTimeFormatter TIMESTAMP_DIR = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final LeitorLegendaAss leitor;
    private final EscritorLegendaAss escritor;
    private final AchatadorEstilosDecorativosService achatador;
    private final TelemetriaService telemetriaService;
    private final Path raizBackups;

    @Inject
    public AchatarEstilosUseCase(
        LeitorLegendaAss leitor,
        EscritorLegendaAss escritor,
        AchatadorEstilosDecorativosService achatador,
        TelemetriaService telemetriaService
    ) {
        this(leitor, escritor, achatador, telemetriaService, DiretorioBaseKronos.resolver("backups"));
    }

    AchatarEstilosUseCase(
        LeitorLegendaAss leitor,
        EscritorLegendaAss escritor,
        AchatadorEstilosDecorativosService achatador,
        TelemetriaService telemetriaService,
        Path raizBackups
    ) {
        this.leitor = leitor;
        this.escritor = escritor;
        this.achatador = achatador;
        this.telemetriaService = telemetriaService;
        this.raizBackups = raizBackups.toAbsolutePath().normalize();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: varre a pasta, achata os estilos decorativos de cada
     * legenda e grava as alteradas, produzindo o resumo consumido pela tela.
     *
     * <p>INVARIANTES DO DOMÍNIO: cada arquivo gravado tem backup prévio; contadores
     * (arquivos alterados, falas achatadas) refletem apenas gravações concluídas.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: diretório inexistente lança exceção antes de
     * qualquer escrita; falha por arquivo não impede os demais.
     */
    public ResultadoTrocaFonte aplicar(Path diretorio) {
        long inicioMs = System.currentTimeMillis();
        validarDiretorio(diretorio);

        out(AnsiCores.CYAN + "\n=== Iniciando Achatamento de Estilos Decorativos ===" + AnsiCores.RESET);
        out("Pasta alvo: " + diretorio.toAbsolutePath());

        List<Path> arquivos = listarLegendas(diretorio);
        if (arquivos.isEmpty()) {
            out(AnsiCores.YELLOW + "[AVISO] Nenhum arquivo .ass/.ssa encontrado." + AnsiCores.RESET);
            out(DuracaoUtil.linhaRelatorioFinal("Achatar Estilos Decorativos", inicioMs));
            return new ResultadoTrocaFonte(0, 0, 0, LocalDateTime.now().toString(), "N/A", null);
        }

        String timestamp = TIMESTAMP_DIR.format(LocalDateTime.now());
        Path pastaBackup = raizBackups.resolve("achatar_estilos_" + timestamp).normalize();
        try {
            Files.createDirectories(pastaBackup);
            out("Diretório de backup criado: " + pastaBackup);
        } catch (IOException e) {
            throw new TrocaTipoLegendaException("Falha ao criar diretório de backup: " + pastaBackup, e);
        }

        int totalAlterados = 0;
        int totalFalas = 0;
        for (Path arq : arquivos) {
            if (Thread.currentThread().isInterrupted()) {
                out(AnsiCores.YELLOW + "[AVISO] Execução interrompida — arquivos já gravados foram preservados." + AnsiCores.RESET);
                break;
            }
            try {
                DocumentoLegenda doc = leitor.ler(arq);
                AchatadorEstilosDecorativosService.Resultado r = achatador.achatar(doc);
                if (!r.houveAchatamento()) {
                    out("Arquivo " + arq.getFileName() + " [OK] — sem estilos decorativos. Pulando.");
                    continue;
                }
                Files.copy(arq, pastaBackup.resolve(arq.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                escritor.escrever(arq, r.documento());
                totalAlterados++;
                totalFalas += r.falasAchatadas();
                out(AnsiCores.GREEN + "  [ACHATADO] " + arq.getFileName() + ": " + r.falasAchatadas()
                    + " fala(s); estilos " + r.estilosDecorativos() + " -> Default." + AnsiCores.RESET);
            } catch (Exception e) {
                out(AnsiCores.RED + "  [ERRO] Falha ao achatar " + arq.getFileName() + ": " + e.getMessage() + AnsiCores.RESET);
                log.error("Erro ao achatar estilos de {}", arq, e);
            }
        }

        out(AnsiCores.GREEN + "\n========================================================================" + AnsiCores.RESET);
        out(AnsiCores.GREEN + "  [SUCESSO] ACHATAMENTO DE ESTILOS DECORATIVOS FINALIZADO!" + AnsiCores.RESET);
        out("  • Arquivos analisados : " + arquivos.size());
        out("  • Arquivos alterados  : " + totalAlterados);
        out("  • Falas achatadas     : " + totalFalas);
        out("  • Pasta de Backup     : " + pastaBackup);
        out(AnsiCores.GREEN + "========================================================================" + AnsiCores.RESET);

        long duracaoMs = System.currentTimeMillis() - inicioMs;
        telemetriaService.registrarOperacao(TelemetriaService.criarOperacao(
            "Achatar Estilos Decorativos",
            "Diretório: " + diretorio.getFileName() + "; alterados=" + totalAlterados + "; falas=" + totalFalas,
            duracaoMs,
            arquivos.size(),
            totalAlterados,
            totalFalas));
        out(DuracaoUtil.linhaRelatorioFinal("Achatar Estilos Decorativos", inicioMs));

        return new ResultadoTrocaFonte(
            arquivos.size(), totalAlterados, totalFalas, LocalDateTime.now().toString(), pastaBackup.toString(), null);
    }

    private void validarDiretorio(Path dir) {
        if (dir == null) {
            throw new TrocaTipoLegendaException("Diretório de legendas não pode ser nulo.");
        }
        if (!Files.isDirectory(dir)) {
            throw new TrocaTipoLegendaException("O caminho fornecido não é uma pasta existente: " + dir);
        }
    }

    private List<Path> listarLegendas(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(p -> {
                    String nome = p.toString().toLowerCase();
                    return nome.endsWith(".ass") || nome.endsWith(".ssa");
                })
                .sorted()
                .toList();
        } catch (IOException e) {
            throw new TrocaTipoLegendaException("Falha ao listar arquivos de legenda na pasta: " + dir, e);
        }
    }

    // System.out é redirecionado ao console SSE quando o controller define o canal
    // "troca-tipo-legenda" e submete à fila; log.* segue para o terminal do servidor.
    private void out(String mensagem) {
        System.out.println(mensagem);
        log.info(mensagem.replaceAll("\\u001B\\[[0-9;]*m", ""));
    }
}
