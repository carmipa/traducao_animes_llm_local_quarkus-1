package org.traducao.projeto.revisaoConcordancia.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.core.presentation.ui.AnsiCores;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.revisaoConcordancia.domain.ResultadoConcordancia;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * PROPÓSITO DE NEGÓCIO: revisa a CONCORDÂNCIA DE GÊNERO de legendas que só existem em português
 * (o {@code .ass} PT-BR, sem inglês e sem cache), aplicando o corretor determinístico. Atende o
 * relato do Paulo de ver erros de gênero sobrando no fim dos animes: é o motor do menu "Revisão
 * de Concordância", que roda separado da revisão de lore e da tradução.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só correções determinísticas de gênero inequívoco (via {@link CorretorConcordanciaGeneroService});
 *       não usa inglês nem LLM.</li>
 *   <li><b>Estilo musical é veto ABSOLUTO.</b> Música e karaokê pertencem à fatia
 *       {@code traducaoKaraoke}, que sabe lidar com KFX, camadas e tempo por sílaba. Aqui não é
 *       "não consigo revisar", é "não é meu trabalho" — a mesma regra de escopo que
 *       {@code traducao.SeletorEventosTraduziveis} e {@code raspagemRevisao.FiltroAuditoriaLinha}
 *       já aplicam. Medido em 2026-08-16 no 86, ANTES desta guarda existir: esta tela via
 *       <b>22.568 de 26.524</b> eventos na Part 1 (85,1%) e <b>49.458 de 53.175</b> na Part 2
 *       (93,0%) — quase tudo sílaba solta de karaokê, e a tela mexe em GÊNERO, que é onde a
 *       heurística mais erra.</li>
 *   <li>NUNCA sobrescreve sem backup: cada arquivo alterado é copiado para subpasta timestampada
 *       antes de regravar; {@code aplicar=false} é dry-run (não escreve).</li>
 *   <li>Só reescreve arquivos que mudaram; eventos sem texto passam intactos; a estrutura do
 *       documento é preservada.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Pasta inexistente devolve resultado zerado; erro por arquivo é logado e o arquivo é pulado;
 * falha ao criar backup aborta a gravação daquele arquivo (preserva o original).
 */
@Service
public class RevisarConcordanciaUseCase {

    private static final Logger log = LoggerFactory.getLogger(RevisarConcordanciaUseCase.class);
    private static final Set<String> EXTENSOES = Set.of(".ass", ".ssa");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final String PASTA_BACKUP = "backup_revisao_concordancia";

    private final LeitorLegendaAss leitor;
    private final EscritorLegendaAss escritor;
    private final CorretorConcordanciaGeneroService corretor;
    private final TelemetriaService telemetriaService;

    /**
     * O MESMO juiz de estilo musical que as outras telas usam. É peer ({@code legenda.domain}), e
     * consultá-lo é o oposto de reescrever a regra aqui: a forma do nome musical tem UM dono desde
     * 07/08/2026, e uma segunda lista divergiria em silêncio no dia em que um estilo novo entrasse.
     */
    private final PoliticaEstiloMusical politicaEstiloMusical;

    /**
     * PROPÓSITO DE NEGÓCIO: responde se a fala é música/karaokê — e portanto NÃO é trabalho desta
     * tela.
     *
     * <p>INVARIANTES DO DOMÍNIO: pergunta ao dono da regra ({@link PoliticaEstiloMusical}), que é
     * peer, em vez de reimplementar a forma do nome musical. Uma segunda lista aqui divergiria em
     * silêncio no dia em que um estilo novo entrasse no acervo — e o sinal disso só apareceria numa
     * legenda estragada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: estilo {@code null} devolve {@code false} e a fala segue
     * para o corretor, como sempre seguiu — a guarda não inventa veto onde não há nome de estilo.
     */
    private boolean eMusica(EventoLegenda evento) {
        return evento.estilo() != null && politicaEstiloMusical.estiloIgnorado(evento.estilo());
    }

    public RevisarConcordanciaUseCase(
        LeitorLegendaAss leitor, EscritorLegendaAss escritor,
        CorretorConcordanciaGeneroService corretor, TelemetriaService telemetriaService,
        PoliticaEstiloMusical politicaEstiloMusical) {
        this.leitor = leitor;
        this.escritor = escritor;
        this.corretor = corretor;
        this.telemetriaService = telemetriaService;
        this.politicaEstiloMusical = politicaEstiloMusical;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: revisa a concordância de gênero de todos os {@code .ass}/{@code .ssa}
     * de uma pasta PT-BR.
     * <p>INVARIANTES DO DOMÍNIO: com {@code aplicar=false} nada é gravado; com {@code true} cada
     * arquivo alterado é copiado para backup antes de ser regravado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: pasta ausente devolve zero; erro por arquivo é pulado.
     *
     * @param pasta pasta com as legendas PT-BR
     * @param aplicar false = dry-run (só relatório); true = grava com backup
     * @return {@link ResultadoConcordancia} com contagens e backups
     */
    public ResultadoConcordancia revisarPasta(Path pasta, boolean aplicar) {
        long inicioMs = System.currentTimeMillis();
        if (pasta == null || !Files.isDirectory(pasta)) {
            return new ResultadoConcordancia(0, 0, 0, List.of(), aplicar);
        }
        List<Path> arquivos;
        try (Stream<Path> stream = Files.walk(pasta)) {
            arquivos = stream.filter(Files::isRegularFile).filter(this::temExtensaoSuportada).sorted().toList();
        } catch (IOException e) {
            log.warn("Falha ao varrer {} para revisão de concordância: {}", pasta, e.getMessage());
            return new ResultadoConcordancia(0, 0, 0, List.of(), aplicar);
        }

        int analisados = 0;
        int alterados = 0;
        int falasCorrigidas = 0;
        int foraDoAlcance = 0;
        List<Path> backups = new ArrayList<>();

        for (Path arquivo : arquivos) {
            if (eParcial(arquivo)) {
                foraDoAlcance++;
                imprimir(AnsiCores.DIM + "  [Ignorado] " + arquivo.getFileName()
                    + " (.parcial — tradução incompleta, não é entrega)" + AnsiCores.RESET);
                continue;
            }
            analisados++;
            try {
                DocumentoLegenda documento = leitor.ler(arquivo);
                List<EventoLegenda> novos = new ArrayList<>(documento.eventos().size());
                int corrigidasArq = 0;
                for (EventoLegenda evento : documento.eventos()) {
                    if (!evento.temTexto() || eMusica(evento)) {
                        novos.add(evento);
                        continue;
                    }
                    Optional<String> corrigida = corretor.corrigir(evento.texto());
                    if (corrigida.isPresent()) {
                        corrigidasArq++;
                        novos.add(evento.comTexto(corrigida.get()));
                    } else {
                        novos.add(evento);
                    }
                }
                if (corrigidasArq == 0) {
                    imprimir(AnsiCores.DIM + "  [OK]       " + arquivo.getFileName()
                        + " (concordancia conforme)" + AnsiCores.RESET);
                    continue;
                }
                if (aplicar) {
                    // As contagens sobem DEPOIS da gravação, não antes. Falha ao criar backup ou
                    // ao escrever cai no catch e o arquivo fica intacto — se os contadores já
                    // tivessem subido, o banner final diria "corrigidas" para uma fala que
                    // continua errada no disco. Estado-alvo não é estado-atual.
                    Path backup = criarBackup(pasta, arquivo);
                    DocumentoLegenda revisado = new DocumentoLegenda(
                        documento.cabecalho(), novos, documento.quebraDeLinha(), documento.comBom());
                    escritor.escrever(arquivo, revisado);
                    backups.add(backup);
                    falasCorrigidas += corrigidasArq;
                    alterados++;
                    imprimir(AnsiCores.GREEN + "  [Revisado] " + arquivo.getFileName()
                        + " (" + corrigidasArq + " fala(s) corrigida(s))" + AnsiCores.RESET);
                    imprimir(AnsiCores.CYAN + "  Backup anterior: " + backup + AnsiCores.RESET);
                } else {
                    falasCorrigidas += corrigidasArq;
                    alterados++;
                    imprimir(AnsiCores.YELLOW + "  [Pendente] " + arquivo.getFileName()
                        + " (" + corrigidasArq + " fala(s) mudariam — nada gravado, simulacao)"
                        + AnsiCores.RESET);
                }
            } catch (IOException | RuntimeException e) {
                imprimir(AnsiCores.RED + "  [Erro]     " + arquivo.getFileName()
                    + " — " + e.getMessage() + AnsiCores.RESET);
                log.warn("Revisão de concordância pulou {} por erro: {}", arquivo, e.getMessage());
            }
        }
        ResultadoConcordancia resultado = new ResultadoConcordancia(
            analisados, alterados, falasCorrigidas, List.copyOf(backups), aplicar, foraDoAlcance);
        telemetriaService.registrarOperacao(new OperacaoTelemetria(
            "Revisão de Concordância",
            "Pasta: " + pasta.getFileName() + (aplicar ? " (aplicado)" : " (simulado)"),
            System.currentTimeMillis() - inicioMs,
            analisados,
            falasCorrigidas,
            falasCorrigidas,
            Instant.now().toString()));
        return resultado;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: escreve UMA linha por arquivo no console do operador, seguindo a
     * mesma regra de cores das telas 3.1 e 3.2 — verde só para o que foi gravado, amarelo para o
     * que ficou por resolver, cinza para o arquivo que realmente não tinha nada, vermelho só
     * para erro.
     *
     * <h2>Por que uma linha por ARQUIVO, e nunca por fala</h2>
     * Medido na 3.2 em 18/08/2026: imprimindo por fala, 10.013 das 10.563 linhas do console
     * (94,8%) eram "auditando" e "limpo" — ruído que empurra o sinal para fora da tela. Aqui o
     * risco seria pior: esta fatia percorre 332.545 falas do acervo, e nenhuma delas interessa ao
     * operador enquanto não muda nada.
     *
     * <p>INVARIANTES DO DOMÍNIO: escreve em {@code System.out} (que o console web captura) e no
     * log do servidor SEM os códigos ANSI — cor é para o olho, não para o arquivo de log.
     *
     * <h2>Por que aqui NÃO há batimento de progresso, e isso está medido</h2>
     * A 3.2 precisou de um: {@code System.out} alimenta o mesmo log que o
     * {@code pode-compilar.ps1} lê, e ele trata <b>90s sem linha</b> como "job terminado" —
     * liberando uma compilação que dispara live reload e mata o job em curso (o acidente do
     * episódio Stink Bomb, 14/08/2026). Lá o silêncio é real porque cada fala pode virar uma
     * chamada ao LLM com retentativa.
     *
     * <p>Aqui não há LLM nem rede. Medido em 18/08/2026 sobre o acervo inteiro: a pasta mais
     * lenta ({@code DanMachi}, 1.614.552 eventos em 260 arquivos) levou <b>12,8s no total</b> —
     * 7x abaixo do limite do portão. E como sai uma linha por ARQUIVO, o silêncio máximo é o de
     * um arquivo só, na casa das dezenas de milissegundos. Copiar o batimento da 3.2 para cá
     * seria guarda que não protege nada — e guarda que não protege nada ensina a ignorar as que
     * protegem.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança; console indisponível não pode derrubar uma
     * revisão em andamento.
     */
    private void imprimir(String linhaColorida) {
        System.out.println(linhaColorida);
        log.info(linhaColorida.replaceAll((char) 27 + "\\[[0-9;]*m", "").strip());
    }

    private Path criarBackup(Path pasta, Path arquivo) throws IOException {
        Path dirBackup = pasta.resolve(PASTA_BACKUP);
        Files.createDirectories(dirBackup);
        String nome = arquivo.getFileName().toString();
        Path backup = dirBackup.resolve(nome + "." + LocalDateTime.now().format(TS) + ".bak");
        Files.copy(arquivo, backup, StandardCopyOption.COPY_ATTRIBUTES);
        return backup;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: diz se o arquivo é uma tradução INCOMPLETA, que o pipeline isolou com
     * o sufixo {@code .parcial} — e que por isso não é entrega e não se reescreve.
     *
     * <h2>Por que o sufixo é critério confiável aqui</h2>
     * Ele não é convenção de quem nomeia arquivo à mão: quem o coloca é o próprio pipeline
     * ({@code traducao.ResolvedorSaidaLegenda}), quando a tradução terminou com pendências. O
     * arquivo existe justamente para NÃO ser confundido com o PT-BR final usado no remux.
     *
     * <p>A constante é local de propósito. O dono da regra é outra FATIA, e fatia não fala com
     * fatia: duplicar oito caracteres conscientemente custa menos que a dependência — é a mesma
     * escolha que o projeto já faz em toda parte. Se o pipeline mudar o sufixo, esta linha muda
     * junto.
     *
     * <p>INVARIANTES DO DOMÍNIO: compara em minúsculas, porque o disco do Windows não distingue
     * caixa e o acervo tem as duas formas.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nome vazio devolve {@code false} — na dúvida o arquivo
     * segue para a revisão normal, que tem backup e veto de música.
     */
    private boolean eParcial(Path arquivo) {
        return arquivo.getFileName().toString().toLowerCase().contains(".parcial.");
    }

    private boolean temExtensaoSuportada(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase();
        return EXTENSOES.stream().anyMatch(nome::endsWith);
    }
}
