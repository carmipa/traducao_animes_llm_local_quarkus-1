package org.traducao.projeto.auditorConteudoLegendas.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.AuditoriaConteudoRelatorioJson;
import org.traducao.projeto.auditorConteudoLegendas.domain.RelatorioAuditoriaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.infrastructure.AuditoriaConteudoPersistencia;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: transforma cada Análise de Legenda em telemetria e
 * dataset JSON pesquisável, incluindo os formatos efetivamente processados.
 * <p>INVARIANTES DO DOMÍNIO: métricas e relatório persistido descrevem a mesma
 * execução e os mesmos arquivos.
 * <p>COMPORTAMENTO EM CASO DE FALHA: falha de persistência é registrada, mas
 * não invalida o resultado em memória da auditoria.
 */
@ApplicationScoped
public class TelemetriaAuditoriaService {

    public static final String TIPO_OPERACAO = "Auditoria de Conteudo de Legendas";

    private static final Logger log = LoggerFactory.getLogger(TelemetriaAuditoriaService.class);

    private final TelemetriaService telemetriaService;
    private final AuditoriaConteudoPersistencia persistencia;

    /**
     * Persistência canônica: em produção grava o relatório em {@code relatorios/} e
     * registra a telemetria canônica. No perfil de teste (%test) fica {@code false}:
     * o JSON vai só para a pasta de entrada (tipicamente um {@code @TempDir}) e a
     * telemetria canônica NÃO é escrita, evitando contaminar o repositório.
     */
    @org.eclipse.microprofile.config.inject.ConfigProperty(
        name = "kronos.auditoria.persistencia-canonica", defaultValue = "true")
    boolean persistenciaCanonica;

    @Inject
    public TelemetriaAuditoriaService(
        TelemetriaService telemetriaService,
        AuditoriaConteudoPersistencia persistencia
    ) {
        this.telemetriaService = telemetriaService;
        this.persistencia = persistencia;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: registra resultado, modo, formatos e anomalias para
     * acompanhamento operacional e melhoria futura das regras.
     * <p>INVARIANTES DO DOMÍNIO: o JSON e a operação agregada compartilham os
     * mesmos contadores e formatos; {@code caminhoPrincipal} é o arquivo que
     * define a pasta de relatórios (traduzido no modo comparativo, o próprio
     * arquivo nos modos de arquivo único).
     * <p>COMPORTAMENTO EM CASO DE FALHA: retorna {@code null} se o JSON não
     * puder ser salvo, mantendo a telemetria em memória quando possível.
     */
    public String registrar(
        RelatorioAuditoriaConteudo relatorio,
        Path caminhoPrincipal,
        long duracaoMs
    ) {
        int totalAnomalias = relatorio.getAnomalias().size();
        long criticas = relatorio.getAnomalias().stream()
            .filter(a -> a.severidade() == AnomaliaConteudo.TipoSeveridade.CRITICAL)
            .count();

        String detalhe = caminhoPrincipal.toAbsolutePath()
            + " | modo=" + relatorio.getModo()
            + " | formatoOriginal=" + (relatorio.getFormatoOriginal() == null ? "—" : relatorio.getFormatoOriginal())
            + " | formatoTraduzido=" + (relatorio.getFormatoTraduzido() == null ? "—" : relatorio.getFormatoTraduzido())
            + " | anomalias=" + totalAnomalias
            + " | criticas=" + criticas;

        OperacaoTelemetria operacao = TelemetriaService.criarOperacao(
            TIPO_OPERACAO,
            detalhe,
            duracaoMs,
            1,
            totalAnomalias,
            0
        );

        AuditoriaConteudoRelatorioJson json = new AuditoriaConteudoRelatorioJson(
            "auditoria_conteudo",
            operacao,
            relatorio.getModo(),
            relatorio.getArquivoOriginal(),
            relatorio.getArquivoTraduzido(),
            relatorio.getFormatoOriginal(),
            relatorio.getFormatoTraduzido(),
            relatorio.isLimpo(),
            totalAnomalias,
            duracaoMs,
            List.copyOf(relatorio.getAnomalias())
        );

        Path pastaEntrada = resolverPastaRelatorios(caminhoPrincipal);
        // Produção: relatorios/<pasta>. Teste: a própria pasta de entrada (@TempDir),
        // sem tocar em relatorios/ nem na telemetria canônica.
        Path pastaDestino = persistenciaCanonica
            ? TelemetriaService.resolverPastaRelatorios(pastaEntrada)
            : pastaEntrada;
        String caminhoJson = null;

        try {
            Path arquivo = persistencia.salvarRelatorioJson(pastaDestino, json);
            caminhoJson = arquivo.toString();
            if (persistenciaCanonica) {
                telemetriaService.registrarOperacao(operacao);
                telemetriaService.salvar(pastaDestino);
            }
        } catch (IOException e) {
            log.warn("Falha ao salvar relatorio JSON da auditoria de conteudo: {}", e.getMessage());
            if (persistenciaCanonica) {
                telemetriaService.registrarOperacao(operacao);
            }
        }

        String arquivoAuditado = nomeArquivoAuditado(relatorio);
        if (relatorio.isLimpo()) {
            log.info("Auditoria de conteudo limpa: {} ({} ms)", arquivoAuditado, duracaoMs);
            System.out.println("[Auditoria Conteudo] Arquivo limpo: " + arquivoAuditado);
        } else {
            log.warn("Auditoria de conteudo detectou {} anomalia(s) em {} ({} ms)",
                totalAnomalias, arquivoAuditado, duracaoMs);
            System.out.println("[Auditoria Conteudo] " + totalAnomalias + " anomalia(s) em "
                + arquivoAuditado);
            for (AnomaliaConteudo anomalia : relatorio.getAnomalias()) {
                System.out.println("  [" + anomalia.severidade() + "] " + anomalia.regra()
                    + " — " + anomalia.descricao());
            }
        }

        if (caminhoJson != null) {
            System.out.println("[Auditoria Conteudo] Relatorio JSON: " + caminhoJson);
        }

        return caminhoJson;
    }

    static Path resolverPastaRelatorios(Path caminhoPrincipal) {
        Path pai = caminhoPrincipal.getParent();
        return pai != null ? pai : Path.of("auditoria_conteudo");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: escolhe o nome de arquivo que representa a auditoria
     * nas mensagens de log, independente do modo.
     * <p>INVARIANTES DO DOMÍNIO: no comparativo e no modo traduzido usa o
     * traduzido; no modo original usa o original.
     * <p>COMPORTAMENTO EM CASO DE FALHA: se ambos forem nulos devolve rótulo
     * genérico "(arquivo)".
     */
    private static String nomeArquivoAuditado(RelatorioAuditoriaConteudo relatorio) {
        if (relatorio.getArquivoTraduzido() != null) {
            return relatorio.getArquivoTraduzido();
        }
        if (relatorio.getArquivoOriginal() != null) {
            return relatorio.getArquivoOriginal();
        }
        return "(arquivo)";
    }
}
