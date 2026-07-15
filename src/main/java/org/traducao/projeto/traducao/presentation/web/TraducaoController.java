package org.traducao.projeto.traducao.presentation.web;
import org.traducao.projeto.core.presentation.web.PipelineWebSupport;
import org.traducao.projeto.core.presentation.web.OperacaoRequest;
import org.traducao.projeto.core.presentation.web.RespostaPadrao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.traducao.projeto.traducao.domain.TelemetriaTraducao;
import org.traducao.projeto.traducao.domain.ports.TelemetriaTraducaoPort;
import org.traducao.projeto.traducao.application.ProcessarArquivoUseCase;
import org.traducao.projeto.traducao.domain.StatusLlm;
import org.traducao.projeto.traducao.domain.ports.MistralPort;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto;
import org.traducao.projeto.traducao.presentation.ui.PastasExecucao;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * PROPÓSITO DE NEGÓCIO: expõe a tradução local via LLM (Opção 3) à interface
 * web, verificando a disponibilidade do servidor LLM, configurando as pastas de
 * execução, aplicando o contexto de lore selecionado e traduzindo em lote os
 * arquivos de legenda encontrados.
 *
 * <p>INVARIANTES DO DOMÍNIO: usa a MESMA fila compartilhada via
 * {@link PipelineWebSupport}; contexto de lore é obrigatório e validado (sem
 * fallback silencioso); apenas extensões suportadas ({@code .ass/.ssa/.srt})
 * são traduzidas; nenhuma URL, código HTTP ou nome de campo de DTO é alterado em
 * relação ao controller monolítico original.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: entrada em branco ou contexto ausente/
 * inválido retorna HTTP 400; falhas por arquivo são contabilizadas, registradas
 * na telemetria e reportadas no console SSE, sem derrubar a fila.
 */
@RestController
@RequestMapping("/api")
public class TraducaoController {

    private static final Logger log = LoggerFactory.getLogger(TraducaoController.class);
    private static final Set<String> EXTENSOES_SUPORTADAS = Set.of(".ass", ".ssa", ".srt");

    private final PipelineWebSupport pipelineWebSupport;
    private final ProcessarArquivoUseCase processarArquivoUseCase;
    private final MistralPort mistralPort;
    private final GerenciadorContexto gerenciadorContexto;
    private final PastasExecucao pastasExecucao;
    private final TradutorProperties propriedades;
    private final TelemetriaTraducaoPort telemetriaTraducao;

    public TraducaoController(
            PipelineWebSupport pipelineWebSupport,
            ProcessarArquivoUseCase processarArquivoUseCase,
            MistralPort mistralPort,
            GerenciadorContexto gerenciadorContexto,
            PastasExecucao pastasExecucao,
            TradutorProperties propriedades,
            TelemetriaTraducaoPort telemetriaTraducao) {
        this.pipelineWebSupport = pipelineWebSupport;
        this.processarArquivoUseCase = processarArquivoUseCase;
        this.mistralPort = mistralPort;
        this.gerenciadorContexto = gerenciadorContexto;
        this.pastasExecucao = pastasExecucao;
        this.propriedades = propriedades;
        this.telemetriaTraducao = telemetriaTraducao;
    }

    /**
     * 3. TRADUÇÃO LOCAL
     */
    @PostMapping("/traduzir")
    public ResponseEntity<RespostaPadrao> traduzir(@RequestBody OperacaoRequest req) {
        if (req.entrada() == null || req.entrada().isBlank()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao("Pasta de legendas de entrada obrigatória."));
        }
        if (req.contextoId() == null || req.contextoId().isBlank()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                    "Contexto de tradução (lore) obrigatório: selecione o contexto antes de traduzir. "
                    + "Não há fallback silencioso para a obra usada na execução anterior."));
        }
        if (!gerenciadorContexto.existeContexto(req.contextoId())) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                    "Contexto de tradução desconhecido: \"" + req.contextoId() + "\". Recarregue a página e selecione um contexto válido."));
        }

        pipelineWebSupport.submeterJobComRelatorio("traducao", "Tradução Local via LLM", () -> {
            try {
                Path pathEntrada = pipelineWebSupport.normalizarCaminho(req.entrada());
                if (pathEntrada == null) {
                    log.error("Caminho de entrada inválido informado para tradução: {}", req.entrada());
                    System.out.println("[FAIL] Caminho de entrada inválido: " + req.entrada());
                    return;
                }
                if (!Files.isDirectory(pathEntrada)) {
                    System.out.println("\u001B[31m[FAIL] Pasta de entrada inválida: " + pathEntrada + "\u001B[0m");
                    return;
                }

                // Verifica LLM
                System.out.println("Verificando se o servidor LLM local está online...");
                StatusLlm status = mistralPort.verificarDisponibilidade();
                if (!status.modeloCarregado()) {
                    System.out.println("\u001B[31m[FAIL] Servidor LLM indisponível: " + status.mensagem() + "\u001B[0m");
                    return;
                }
                System.out.println("\u001B[32m[OK] Servidor LLM ativo.\u001B[0m");

                // Configura as pastas compartilhadas
                String saida = req.saida() != null && !req.saida().isBlank() ? req.saida() : "";
                pastasExecucao.configurar(req.entrada(), saida, propriedades.diretorioCache(), propriedades);

                // Define o contexto de tradução selecionado na UI
                gerenciadorContexto.definirContextoAtivo(req.contextoId());
                System.out.println("\u001B[34m[CONTEXTO] Utilizando contexto: " + gerenciadorContexto.obterNomeContextoAtivo() + "\u001B[0m");

                List<Path> arquivos;
                try (Stream<Path> stream = Files.list(pathEntrada)) {
                    arquivos = stream
                            .filter(Files::isRegularFile)
                            .filter(this::temExtensaoSuportada)
                            .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                            .toList();
                }

                if (arquivos.isEmpty()) {
                    System.out.println("\u001B[33mNenhum arquivo de legenda .ass/.ssa/.srt encontrado.\u001B[0m");
                    return;
                }

                System.out.println("Iniciando tradução de " + arquivos.size() + " arquivo(s)...");

                java.util.List<org.traducao.projeto.traducao.domain.ResultadoTraducaoArquivo> resultados = new java.util.ArrayList<>();
                boolean permitir = Boolean.TRUE.equals(req.permitirRetraducao());
                String loreNome = gerenciadorContexto.obterNomeContextoAtivo();
                for (int i = 0; i < arquivos.size(); i++) {
                    Path arquivo = arquivos.get(i);
                    System.out.println("\n--------------------------------------------------------------");
                    System.out.println("Processando arquivo [" + (i + 1) + "/" + arquivos.size() + "]: " + arquivo.getFileName());
                    System.out.println("--------------------------------------------------------------");
                    try {
                        var resultado = processarArquivoUseCase.processar(arquivo, permitir);
                        resultados.add(resultado);
                        if (resultado.status() == org.traducao.projeto.traducao.domain.StatusArquivoTraducao.CONCLUIDO) {
                            System.out.println("[OK] Traduzido: " + arquivo.getFileName());
                        } else {
                            System.out.println("[PARCIAL] " + arquivo.getFileName()
                                + ": saída parcial em " + resultado.arquivoSaida()
                                + "; corrija o cache e execute novamente.");
                        }
                    } catch (org.traducao.projeto.traducao.domain.exceptions.EntradaJaTraduzidaException ex) {
                        resultados.add(org.traducao.projeto.traducao.domain.ResultadoTraducaoArquivo.bloqueado(arquivo.getFileName().toString(), loreNome));
                        registrarTelemetriaFalhaTraducao(arquivo, loreNome, org.traducao.projeto.traducao.domain.StatusArquivoTraducao.BLOQUEADO, ex.getMessage());
                        System.out.println("[BLOQUEADO] " + arquivo.getFileName() + ": " + ex.getMessage());
                    } catch (org.traducao.projeto.traducao.domain.exceptions.TraducaoParcialException ex) {
                        // Abortou antes de escrever a legenda de saída: é FALHA deste run
                        // (nenhum _PT-BR gerado), mesmo que N linhas tenham sido salvas no
                        // cache para retomar depois. Não pode contar como "ok" no lote.
                        int salvas = ex.getDicionarioParcial() != null ? ex.getDicionarioParcial().size() : 0;
                        resultados.add(org.traducao.projeto.traducao.domain.ResultadoTraducaoArquivo.falha(arquivo.getFileName().toString(), loreNome));
                        registrarTelemetriaFalhaTraducao(arquivo, loreNome, org.traducao.projeto.traducao.domain.StatusArquivoTraducao.FALHOU, ex.getMessage());
                        System.out.println("[FALHA] " + arquivo.getFileName() + " abortado sem gerar saída (" + salvas + " linha(s) salvas no cache para retomar).");
                    } catch (Exception ex) {
                        resultados.add(org.traducao.projeto.traducao.domain.ResultadoTraducaoArquivo.falha(arquivo.getFileName().toString(), loreNome));
                        registrarTelemetriaFalhaTraducao(arquivo, loreNome, org.traducao.projeto.traducao.domain.StatusArquivoTraducao.FALHOU, ex.getMessage());
                        System.out.println("[FAIL] " + arquivo.getFileName() + ": " + ex.getMessage());
                    }
                }

                String tabelaTraducao = org.traducao.projeto.traducao.presentation.ui.TabelaTraducaoRenderer.render(resultados);
                if (!tabelaTraducao.isBlank()) {
                    System.out.println(tabelaTraducao);
                }

                org.traducao.projeto.traducao.domain.StatusLoteTraducao statusLote =
                    org.traducao.projeto.traducao.domain.StatusLoteTraducao.consolidar(resultados);
                long okCount = resultados.stream().filter(r ->
                    r.status() == org.traducao.projeto.traducao.domain.StatusArquivoTraducao.CONCLUIDO).count();
                long parcialCount = resultados.stream().filter(r ->
                    r.status() == org.traducao.projeto.traducao.domain.StatusArquivoTraducao.PARCIAL).count();
                long falhaCount = resultados.size() - okCount - parcialCount;
                System.out.println("\n========================================================================");
                System.out.println("  [" + statusLote.getRotulo().toUpperCase() + "] TRADUCAO LOCAL VIA LLM: "
                    + okCount + " concluído(s), " + parcialCount + " parcial(is), " + falhaCount
                    + " com falha/bloqueio de " + resultados.size() + " arquivo(s).");
                System.out.println("========================================================================\n");
                log.info("[{}] Traducao via LLM finalizada. {} concluido(s), {} parcial(is), {} falha/bloqueio de {}.",
                    statusLote.name(), okCount, parcialCount, falhaCount, resultados.size());

            } catch (Exception e) {
                log.error("Erro na tradução em background", e);
                System.out.println("\u001B[31m[ERRO] Falha geral no tradutor: " + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(new RespostaPadrao("Tradução via LLM iniciada no servidor."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: registra na telemetria um arquivo que FALHOU/foi
     * BLOQUEADO na tradução — sem isso, o dataset perdia justamente os casos mais
     * úteis (falhas). Carrega o lore e o status final para dar proveniência ao
     * registro.
     *
     * <p>INVARIANTES DO DOMÍNIO: o anime é inferido pela pasta avó do arquivo; o
     * status registrado é sempre o status final de falha/bloqueio informado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: motivo nulo é substituído pelo rótulo do
     * status; a origem indeterminada vira {@code "Desconhecido"}.
     */
    private void registrarTelemetriaFalhaTraducao(Path arquivo, String lore,
            org.traducao.projeto.traducao.domain.StatusArquivoTraducao status, String motivo) {
        String nome = arquivo.getFileName().toString();
        Path pai = arquivo.getParent();
        String anime = (pai != null && pai.getParent() != null && pai.getParent().getFileName() != null)
            ? pai.getParent().getFileName().toString() : "Desconhecido";
        telemetriaTraducao.registrarTraducao(new TelemetriaTraducao(
            nome, null, 0, 0, 0, 0L,
            java.util.List.of(motivo != null ? motivo : status.getRotulo()),
            anime, "Temporada Única", java.time.Instant.now().toString(),
            lore, status.name()));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: filtra os arquivos de legenda elegíveis para tradução
     * pela extensão suportada.
     *
     * <p>INVARIANTES DO DOMÍNIO: apenas {@code .ass/.ssa/.srt} são elegíveis; a
     * comparação ignora a caixa do nome do arquivo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer arquivo com extensão fora do
     * conjunto suportado retorna {@code false}.
     */
    private boolean temExtensaoSuportada(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase();
        return EXTENSOES_SUPORTADAS.stream().anyMatch(nome::endsWith);
    }
}
