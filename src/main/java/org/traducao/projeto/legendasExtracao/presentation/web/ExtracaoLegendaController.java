package org.traducao.projeto.legendasExtracao.presentation.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.traducao.projeto.legendasExtracao.application.ExtrairLegendaUseCase;
import org.traducao.projeto.legendasExtracao.domain.FormatoLegenda;
import org.traducao.projeto.legendasExtracao.domain.RelatorioExtracao;
import org.traducao.projeto.legendasExtracao.domain.exceptions.FormatoLegendaInvalidoException;
import org.traducao.projeto.legendasExtracao.presentation.ui.TabelaExtracaoRenderer;
import org.traducao.projeto.traducao.presentation.web.ExtracaoRequest;
import org.traducao.projeto.traducao.presentation.web.PipelineWebSupport;
import org.traducao.projeto.traducao.presentation.web.RespostaPadrao;

import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: expõe a extração de legendas (Opção 2) à interface web,
 * validando o formato-alvo e enfileirando o processamento pesado que percorre a
 * pasta de vídeos e extrai as faixas de legenda no formato escolhido.
 *
 * <p>Fronteira arquitetural: este endpoint pertence ao módulo
 * {@code legendasExtracao} (Opção 2) e reside na sua camada de apresentação
 * própria. Não importa nenhuma regra funcional da Tradução Local (Opção 4): usa
 * apenas o use case e os tipos do próprio módulo. As dependências
 * {@link PipelineWebSupport}, {@link RespostaPadrao} e {@link ExtracaoRequest}
 * são <b>glue técnico de apresentação</b> (fila única e contratos de transporte
 * HTTP) hoje em {@code traducao.presentation.web}; é dívida técnica temporária
 * reservada para saneamento na FASE E — não é acoplamento funcional.
 *
 * <p>INVARIANTES DO DOMÍNIO: usa a MESMA fila compartilhada via
 * {@link PipelineWebSupport}; o formato é validado antes de enfileirar; caminhos
 * são normalizados; a rota {@code POST /api/extrair}, o status e os campos de DTO
 * são contrato público preservado exatamente como antes da movimentação.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: entrada em branco ou formato inválido
 * retorna HTTP 400; falhas do job de background são registradas no log e no
 * console SSE, sem derrubar a fila.
 */
@RestController
@RequestMapping("/api")
public class ExtracaoLegendaController {

    private static final Logger log = LoggerFactory.getLogger(ExtracaoLegendaController.class);

    private final PipelineWebSupport pipelineWebSupport;
    private final ExtrairLegendaUseCase extrairLegendaUseCase;

    public ExtracaoLegendaController(
            PipelineWebSupport pipelineWebSupport,
            ExtrairLegendaUseCase extrairLegendaUseCase) {
        this.pipelineWebSupport = pipelineWebSupport;
        this.extrairLegendaUseCase = extrairLegendaUseCase;
    }

    /**
     * 2. EXTRAÇÃO DE LEGENDAS
     */
    @PostMapping("/extrair")
    public ResponseEntity<RespostaPadrao> extrair(@RequestBody ExtracaoRequest req) {
        if (req.entrada() == null || req.entrada().isBlank()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao("Caminho da pasta de vídeos obrigatório."));
        }

        final FormatoLegenda formatoSelecionado;
        try {
            formatoSelecionado = FormatoLegenda.fromString(req.formato());
        } catch (FormatoLegendaInvalidoException e) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(e.getMessage()));
        }

        pipelineWebSupport.submeterJobComRelatorio("extracao", "Extração de Legendas", () -> {
            try {
                Path pathEntrada = pipelineWebSupport.normalizarCaminho(req.entrada());
                if (pathEntrada == null) {
                    log.error("Caminho de entrada inválido informado para extração: {}", req.entrada());
                    return;
                }
                Path pathSaida = pipelineWebSupport.normalizarCaminho(req.saida());
                FormatoLegenda formato = formatoSelecionado;
                RelatorioExtracao rel = extrairLegendaUseCase.executar(pathEntrada, pathSaida, formato);

                String tabela = TabelaExtracaoRenderer.render(rel);
                if (!tabela.isBlank()) {
                    System.out.print("\u001B[36m" + tabela + "\u001B[0m");
                }

                if (rel.getArquivosDetectados() == 0) {
                    System.out.println("\n\u001B[33m========================================================================\u001B[0m");
                    System.out.println("\u001B[33m  ⚠️ [AVISO] NENHUM ARQUIVO DE VÍDEO SUPORTADO FOI ENCONTRADO!\u001B[0m");
                    System.out.println("\u001B[33m========================================================================\u001B[0m");
                    System.out.println("\u001B[36m  • Caminho informado : " + pathEntrada + "\u001B[0m");
                    System.out.println("\u001B[33m  • Formatos suportados: .mkv/.webm (MKVToolNix) e .mp4/.mov/.avi/.ts/.m2ts/.flv/.wmv (ffmpeg).\u001B[0m");
                    System.out.println("\u001B[33m========================================================================\n\u001B[0m");
                    log.warn("[AVISO] Nenhum arquivo de vídeo suportado foi encontrado no caminho: {}", pathEntrada);
                } else if (rel.getLegendasExtraidas() == 0) {
                    System.out.println("\n\u001B[33m========================================================================\u001B[0m");
                    System.out.println("\u001B[33m  ⚠️ [ALERTA] NENHUMA LEGENDA [" + formato.name() + "] FOI EXTRAÍDA!\u001B[0m");
                    System.out.println("\u001B[33m========================================================================\u001B[0m");
                    System.out.println("\u001B[36m  • Arquivos de Vídeo Analisados : " + rel.getArquivosDetectados() + "\u001B[0m");
                    System.out.println("\u001B[31m  • Faixas Extraídas com Sucesso : 0 [" + formato.name() + "]\u001B[0m");
                    System.out.println("\u001B[33m  • Vídeos sem Faixa " + formato.name() + "        : " + rel.getArquivosSemLegenda() + "\u001B[0m");
                    if (rel.getFalhasInesperadas() > 0) {
                        System.out.println("\u001B[31m  • Falhas de Processamento     : " + rel.getFalhasInesperadas() + "\u001B[0m");
                    }
                    System.out.println("\u001B[33m  💡 Dica: Verifique se o vídeo possui legendas em outro formato (ex: PGS ou SRT) ou se a legenda está queimada na imagem (Hardsub).\u001B[0m");
                    System.out.println("\u001B[33m========================================================================\n\u001B[0m");
                    log.warn("[ALERTA] Extração finalizada sem faixas geradas. 0 de {} vídeos possuíam faixa {}", rel.getArquivosDetectados(), formato.name());
                } else {
                    System.out.println("\n\u001B[32m========================================================================\u001B[0m");
                    System.out.println("\u001B[32m  🎉 [SUCESSO] EXTRAÇÃO DE LEGENDAS FINALIZADA COM SUCESSO!\u001B[0m");
                    System.out.println("\u001B[32m========================================================================\u001B[0m");
                    System.out.println("\u001B[36m  • Arquivos de Vídeo Analisados : " + rel.getArquivosDetectados() + "\u001B[0m");
                    System.out.println("\u001B[32m  • Faixas Extraídas com Sucesso : " + rel.getLegendasExtraidas() + " [" + formato.name() + "]\u001B[0m");
                    if (rel.getArquivosSemLegenda() > 0) {
                        System.out.println("\u001B[33m  • Vídeos sem Faixa " + formato.name() + "        : " + rel.getArquivosSemLegenda() + "\u001B[0m");
                    }
                    if (rel.getFalhasInesperadas() > 0) {
                        System.out.println("\u001B[31m  • Falhas de Processamento     : " + rel.getFalhasInesperadas() + "\u001B[0m");
                    }
                    System.out.println("\u001B[32m========================================================================\n\u001B[0m");
                    log.info("[SUCESSO] Extração de legendas finalizada. Extraídas: {} de {}", rel.getLegendasExtraidas(), rel.getArquivosDetectados());
                }
            } catch (Exception e) {
                log.error("Erro na extração de legendas em background", e);
                System.out.println("\u001B[31m[ERRO] Falha na extração: " + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(new RespostaPadrao("Extração de legendas iniciada no servidor."));
    }
}
