package org.traducao.projeto.revisaoConcordancia.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

    public RevisarConcordanciaUseCase(
        LeitorLegendaAss leitor, EscritorLegendaAss escritor, CorretorConcordanciaGeneroService corretor) {
        this.leitor = leitor;
        this.escritor = escritor;
        this.corretor = corretor;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: resumo da revisão de concordância de uma pasta.
     * <p>INVARIANTES DO DOMÍNIO: contagens refletem o que realmente mudou; nunca some fala.
     * <p>COMPORTAMENTO EM CASO DE FALHA: portador de dados puro.
     */
    public record ResultadoConcordancia(
        int arquivosAnalisados,
        int arquivosAlterados,
        int falasCorrigidas,
        List<Path> backups,
        boolean aplicado
    ) {}

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
                    Optional<String> corrigida = corretor.corrigir(evento.texto());
                    if (corrigida.isPresent()) {
                        corrigidasArq++;
                        novos.add(evento.comTexto(corrigida.get()));
                    } else {
                        novos.add(evento);
                    }
                }
                if (corrigidasArq == 0) {
                    continue;
                }
                falasCorrigidas += corrigidasArq;
                alterados++;
                if (aplicar) {
                    backups.add(criarBackup(pasta, arquivo));
                    DocumentoLegenda revisado = new DocumentoLegenda(
                        documento.cabecalho(), novos, documento.quebraDeLinha(), documento.comBom());
                    escritor.escrever(arquivo, revisado);
                }
            } catch (IOException | RuntimeException e) {
                log.warn("Revisão de concordância pulou {} por erro: {}", arquivo, e.getMessage());
            }
        }
        return new ResultadoConcordancia(analisados, alterados, falasCorrigidas, List.copyOf(backups), aplicar);
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
