package org.traducao.projeto.remuxer.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.legendasExtracao.application.ValidadorSaidaExtracao;
import org.traducao.projeto.legendasExtracao.domain.ExtratorException;
import org.traducao.projeto.legendasExtracao.domain.FormatoLegenda;
import org.traducao.projeto.remuxer.domain.PlanoRemux;
import org.traducao.projeto.remuxer.domain.RelatorioRemux;
import org.traducao.projeto.remuxer.domain.RemuxTarefa;
import org.traducao.projeto.remuxer.domain.RemuxerException;
import org.traducao.projeto.remuxer.domain.SaidaRemuxJaExisteException;
import org.traducao.projeto.remuxer.infrastructure.adapters.MkvmergeAdapter;
import org.traducao.projeto.remuxer.presentation.ui.ConsoleRemuxerLogger;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * PROPÓSITO DE NEGÓCIO: orquestra o remux em lote, da validação das entradas à
 * telemetria final, sem reencodar vídeo/áudio.
 *
 * <p>INVARIANTES DO DOMÍNIO: somente legenda textual válida chega ao mkvmerge;
 * cada sucesso representa temporário validado e publicado; cancelamento é
 * observado entre arquivos.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: o lote preserva sucessos anteriores,
 * classifica falhas/pendências e sempre tenta registrar status final no dataset.
 */
@Service
public class RemuxarLoteUseCase {
    private static final Logger log = LoggerFactory.getLogger(RemuxarLoteUseCase.class);

    private final MkvmergeAdapter mkvmergeAdapter;
    private final MapeadorMidiaService mapeadorMidiaService;
    private final ConsoleRemuxerLogger console;
    private final TelemetriaService telemetriaService;

    /**
     * PROPÓSITO DE NEGÓCIO: recebe as fronteiras responsáveis por planejamento,
     * execução externa, console e observabilidade.
     *
     * <p>INVARIANTES DO DOMÍNIO: dependências são compartilhadas pelo container e
     * o relatório permanece local a cada execução.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente é erro de bootstrap.
     */
    public RemuxarLoteUseCase(MkvmergeAdapter mkvmergeAdapter, MapeadorMidiaService mapeadorMidiaService,
            ConsoleRemuxerLogger console, TelemetriaService telemetriaService) {
        this.mkvmergeAdapter = mkvmergeAdapter;
        this.mapeadorMidiaService = mapeadorMidiaService;
        this.console = console;
        this.telemetriaService = telemetriaService;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: executa lote padrão sem offset e substituindo as
     * legendas originais pela faixa PT-BR final.
     *
     * <p>INVARIANTES DO DOMÍNIO: delega ao fluxo completo com opções explícitas.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve relatório classificado.
     */
    public RelatorioRemux executar(Path pastaVideos, Path pastaLegendas) {
        return executar(pastaVideos, pastaLegendas, 0, false);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mantém compatibilidade com offset manual e política
     * histórica de substituir legendas originais.
     *
     * <p>INVARIANTES DO DOMÍNIO: offset é aplicado igualmente a todo o lote.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve relatório classificado.
     */
    public RelatorioRemux executar(Path pastaVideos, Path pastaLegendas, long sincronismoMs) {
        return executar(pastaVideos, pastaLegendas, sincronismoMs, false);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: executa o lote com offset e política explícita para
     * preservar ou substituir legendas existentes no MKV de origem.
     *
     * <p>INVARIANTES DO DOMÍNIO: telemetria é emitida em sucesso, pendência,
     * cancelamento e falha; exceção inesperada não produz sucesso falso.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: captura a fronteira do lote, registra
     * erro inesperado, finaliza o relatório e retorna ao controller.
     */
    public RelatorioRemux executar(Path pastaVideos, Path pastaLegendas, long sincronismoMs,
                                   boolean preservarLegendasOriginais) {
        long inicioMs = System.currentTimeMillis();
        RelatorioRemux relatorio = new RelatorioRemux();
        try {
            executarInterno(pastaVideos, pastaLegendas, sincronismoMs, preservarLegendasOriginais, relatorio);
        } catch (Exception e) {
            log.error("Falha inesperada no lote de remux", e);
            console.erro("Falha inesperada no lote de remux: " + e.getMessage());
            relatorio.registrarErroInesperado();
        } finally {
            relatorio.finalizar();
            registrarTelemetria(pastaVideos, pastaLegendas, sincronismoMs,
                preservarLegendasOriginais, inicioMs, relatorio);
        }
        return relatorio;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: realiza validação, planejamento e processamento
     * sequencial de cada tarefa do lote.
     *
     * <p>INVARIANTES DO DOMÍNIO: pasta de saída é filha de vídeos; avisos do plano
     * entram no relatório antes do primeiro processo; interrupção impede iniciar
     * nova tarefa.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: problemas de infraestrutura encerram;
     * problemas por arquivo não impedem os próximos, salvo cancelamento.
     */
    private void executarInterno(Path pastaVideos, Path pastaLegendas, long sincronismoMs,
                                 boolean preservarLegendasOriginais, RelatorioRemux relatorio) {
        if (!Files.isDirectory(pastaVideos) || !Files.isDirectory(pastaLegendas)) {
            console.erro("Pasta de vídeos ou legendas não encontrada. Vídeos=" + pastaVideos
                + " | Legendas=" + pastaLegendas);
            relatorio.registrarErroInfra();
            return;
        }
        try {
            mkvmergeAdapter.validarInfraestrutura();
        } catch (RemuxerException e) {
            console.erro("Falha na validação do MKVToolNix: " + e.getMessage());
            relatorio.registrarErroInfra();
            return;
        }

        Path pastaSaida = pastaVideos.resolve("mkv_final_ptbr");
        try {
            Files.createDirectories(pastaSaida);
        } catch (IOException e) {
            console.erro("Não foi possível criar pasta de saída: " + pastaSaida + " — " + e.getMessage());
            relatorio.registrarErroInfra();
            return;
        }

        PlanoRemux plano;
        try {
            plano = mapeadorMidiaService.construirPlano(pastaVideos, pastaLegendas, pastaSaida);
        } catch (RemuxerException e) {
            console.erro("Falha ao mapear vídeos e legendas: " + e.getMessage());
            relatorio.registrarErroInfra();
            return;
        }
        relatorio.registrarDeteccao(plano.videosDetectados(), plano.tarefas().size());
        for (int i = 0; i < plano.videosSemLegenda(); i++) relatorio.registrarSemLegenda();
        for (int i = 0; i < plano.pareamentosAmbiguos(); i++) relatorio.registrarPareamentoAmbiguo();
        plano.avisos().forEach(console::aviso);

        console.info("Vídeos=" + plano.videosDetectados() + " | Legendas=" + plano.legendasDetectadas()
            + " | Pareados=" + plano.tarefas().size() + " | Sem legenda=" + plano.videosSemLegenda()
            + " | Ambíguos=" + plano.pareamentosAmbiguos());
        console.info(preservarLegendasOriginais
            ? "Política de faixas: preservar legendas originais e adicionar PT-BR como padrão."
            : "Política de faixas: remover legendas originais e manter somente a nova PT-BR.");
        if (sincronismoMs != 0) console.info("Sincronismo manual do lote: " + sincronismoMs + "ms");

        for (int indice = 0; indice < plano.tarefas().size(); indice++) {
            if (Thread.currentThread().isInterrupted()) {
                relatorio.registrarCancelamento();
                console.aviso("Cancelamento detectado antes do próximo arquivo; nenhum novo remux será iniciado.");
                break;
            }
            RemuxTarefa tarefa = plano.tarefas().get(indice);
            console.info("[REMUX " + (indice + 1) + "/" + plano.tarefas().size() + "] "
                + tarefa.nomeVideo() + " + " + tarefa.caminhoLegenda().getFileName()
                + " -> " + tarefa.caminhoSaida().getFileName());
            try {
                validarLegenda(tarefa.caminhoLegenda());
                mkvmergeAdapter.executarRemux(tarefa, sincronismoMs, preservarLegendasOriginais);
                long bytes = Files.size(tarefa.caminhoSaida());
                relatorio.registrarSucesso(bytes);
                console.sucesso("[OK " + (indice + 1) + "/" + plano.tarefas().size()
                    + "] MKV validado: " + tarefa.caminhoSaida().getFileName());
            } catch (SaidaRemuxJaExisteException e) {
                relatorio.registrarSaidaJaExistente();
                console.aviso("[PRESERVADO] " + e.getMessage());
            } catch (ExtratorException e) {
                relatorio.registrarErroLegendaInvalida();
                console.erro("[LEGENDA INVÁLIDA] " + tarefa.caminhoLegenda().getFileName() + " — " + e.getMessage());
            } catch (RemuxerException e) {
                if (Thread.currentThread().isInterrupted()) {
                    relatorio.registrarCancelamento();
                    console.aviso("Remux cancelado durante " + tarefa.nomeVideo() + "; parcial descartado.");
                    break;
                }
                relatorio.registrarErroRuntime();
                console.erro("[FALHA MKVMERGE] " + tarefa.nomeVideo() + " — " + e.getMessage());
            } catch (IOException e) {
                relatorio.registrarErroIo();
                console.erro("[FALHA I/O] " + tarefa.nomeVideo() + " — " + e.getMessage());
            } catch (Exception e) {
                relatorio.registrarErroInesperado();
                console.erro("[FALHA INESPERADA] " + tarefa.nomeVideo() + " — " + e.getMessage());
            }
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: recusa arquivo vazio ou com conteúdo incompatível
     * com a extensão ASS/SRT antes de invocar o mkvmerge.
     *
     * <p>INVARIANTES DO DOMÍNIO: apenas ASS e SRT textuais são aceitos.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: formato desconhecido ou assinatura
     * inválida lança {@link ExtratorException} e a tarefa é pulada.
     */
    private void validarLegenda(Path legenda) {
        String nome = legenda.getFileName().toString().toLowerCase(Locale.ROOT);
        FormatoLegenda formato;
        if (nome.endsWith(".ass")) {
            formato = FormatoLegenda.ASS;
        } else if (nome.endsWith(".srt")) {
            formato = FormatoLegenda.SRT;
        } else {
            throw new ExtratorException("Formato não suportado no remux: " + nome);
        }
        ValidadorSaidaExtracao.validar(legenda, formato);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: registra o lote como dataset com status, política de
     * faixas, offset, pendências, falhas e volume final.
     *
     * <p>INVARIANTES DO DOMÍNIO: itens corrigidos equivalem a MKVs efetivamente
     * publicados; detalhe contém o status real.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: telemetria é secundária e não altera os
     * arquivos já concluídos.
     */
    private void registrarTelemetria(Path videos, Path legendas, long sincronismoMs,
                                     boolean preservar, long inicioMs, RelatorioRemux relatorio) {
        String detalhe = "status=" + relatorio.getStatusFinal()
            + "; videos=" + videos + "; legendas=" + legendas
            + "; syncMs=" + sincronismoMs + "; preservarOriginais=" + preservar
            + "; semLegenda=" + relatorio.getVideosSemLegenda()
            + "; ambiguos=" + relatorio.getPareamentosAmbiguos()
            + "; existentes=" + relatorio.getSaidasJaExistentes()
            + "; falhas=" + relatorio.getTotalErros()
            + "; bytes=" + relatorio.getBytesMkvGeradosTotal();
        try {
            telemetriaService.registrarOperacao(TelemetriaService.criarOperacao(
                "Remux (mkvmerge)", detalhe, System.currentTimeMillis() - inicioMs,
                relatorio.getMkvDetectados(), relatorio.getLegendasPareadas(),
                relatorio.getMkvProcessadosSucesso()));
        } catch (RuntimeException e) {
            log.warn("Remux concluído, mas a telemetria não pôde ser registrada: {}", e.getMessage());
        }
    }
}
