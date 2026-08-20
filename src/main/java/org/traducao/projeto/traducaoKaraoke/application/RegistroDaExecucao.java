package org.traducao.projeto.traducaoKaraoke.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.core.presentation.web.LogStreamService;
import org.traducao.projeto.lore.domain.SnapshotContexto;
import org.traducao.projeto.telemetria.SanitizadorTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducaoKaraoke.domain.DesfechoKaraoke;
import org.traducao.projeto.traducaoKaraoke.domain.FalhaArquivoKaraoke;
import org.traducao.projeto.traducaoKaraoke.domain.ResultadoTraducaoKaraoke;
import org.traducao.projeto.traducaoKaraoke.domain.TelemetriaKaraoke;
import org.traducao.projeto.traducaoKaraoke.infrastructure.TelemetriaKaraokeDataset;
import org.traducao.projeto.traducaoKaraoke.infrastructure.TraducaoKaraokePersistencia;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * PROPOSITO DE NEGOCIO: e o dono de tudo que a execucao de karaoke DEIXA para tras — manifesto
 * de auditoria, relatorio de telemetria e as linhas de console. Se nada disto for escrito, a
 * execucao aconteceu e ninguem consegue provar o que ela fez.
 *
 * <h2>A invariante que esta classe carrega, e o prejuizo dela</h2>
 * <b>Registra SEMPRE</b>, inclusive — e principalmente — quando a execucao deu errado. Ate
 * 2026-08-14 o registro era condicionado a haver resultado, e a consequencia foi medida na
 * auditoria: LLM fora do ar, destino nao criavel ou TODOS os arquivos falhando produziam ZERO
 * artefato. O pior desfecho possivel saia indistinguivel de "nao havia nada a fazer".
 *
 * <h2>Por que saiu do use case</h2>
 * Eram 660 bytecodes ({@code registrarArtefatos} 231 e {@code montarRelatorio} 429) e tres
 * dependencias dentro de um objeto que tambem classificava, traduzia e gravava cache. E o
 * {@code visivelResumido} vem junto: ele estava package-private compartilhado justamente
 * esperando este dono, e o Javadoc dele dizia isso.
 *
 * <h2>Invariantes do dominio</h2>
 * <ul>
 *   <li>Contexto e proveniencia podem ser NULOS — o aborto por LLM fora do ar acontece antes do
 *       congelamento, e exigi-los faria o registro falhar exatamente quando importa.</li>
 *   <li>Falha ao salvar o manifesto e ERRO, nao aviso: e o artefato que prova todo o resto.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nunca lanca. Falha de escrita vira {@code log.error} mais linha no console dizendo, com todas
 * as letras, que a execucao ficou sem auditoria.
 */
@ApplicationScoped
public class RegistroDaExecucao {

    private static final Logger log = LoggerFactory.getLogger(RegistroDaExecucao.class);

    static final String CANAL_LOG = TraduzirKaraokeUseCase.CANAL_LOG;

    @Inject
    TraducaoKaraokePersistencia persistencia;

    @Inject
    TelemetriaService telemetriaService;

    /**
     * A escritora do acervo PROPRIO desta fatia — o que faz o karaoke existir no dataset publico.
     */
    @Inject
    TelemetriaKaraokeDataset acervoDataset;

    @Inject
    LogStreamService logStream;








    public void registrar(
        Path pastaOrigem,
        Path pastaDestino,
        List<ResultadoTraducaoKaraoke> resultados,
        long duracaoMs,
        int detectadas,
        int corrigidas,
        SnapshotContexto contexto,
        ProvenienciaCache proveniencia,
        DesfechoKaraoke desfecho
    ) {
        try {
            Path manifesto = persistencia.salvarManifesto(
                pastaOrigem, pastaDestino, resultados, duracaoMs, contexto, proveniencia, desfecho);
            if (manifesto != null) {
                logStream.publicarLog(CANAL_LOG, "Manifesto de auditoria salvo em: " + manifesto);
            }
        } catch (IOException | RuntimeException e) {
            // ERROR, não WARN, e também no console: este é o artefato que prova todo o resto.
            // Perdê-lo em silêncio é ficar sem auditoria justamente na execução que deu errado.
            log.error("Falha ao salvar o manifesto da tradução de karaokê", e);
            logStream.publicarLog(CANAL_LOG,
                "[ERRO] MANIFESTO NÃO SALVO — esta execução ficou SEM auditoria: " + e.getMessage());
        }
        // O ACERVO do dataset vem ANTES da operação genérica: é ele que carrega os números, e a
        // operação genérica só carrega três contadores e uma string de detalhe.
        acrescentarAoDataset(resultados, duracaoMs, contexto, proveniencia, desfecho);
        // Contexto é nulo quando a execução abortou antes de congelá-lo (LLM fora do ar). O
        // registro tem de sobreviver a isso: era exatamente a execução sem rastro nenhum.
        String descricaoContexto = contexto == null
            ? "Contexto NÃO congelado (execução " + desfecho.status() + ")"
            : "Contexto: " + contexto.nomeExibicao() + " (" + contexto.id() + ") | hash="
                + (proveniencia == null ? "-" : proveniencia.contextoHash());
        telemetriaService.finalizarOperacao(
            TelemetriaService.criarOperacao(
                "Tradução de Karaokê (LLM)",
                "[" + desfecho.status() + "] " + descricaoContexto
                    + " | Legendas: " + pastaOrigem + " → " + pastaDestino
                    + (desfecho.falhas().isEmpty() ? "" : " | falhas: " + desfecho.falhas().size()),
                duracaoMs,
                resultados.size(),
                detectadas,
                corrigidas),
            pastaOrigem,
            "traducao_karaoke",
            montarRelatorio(pastaOrigem, pastaDestino, resultados, duracaoMs, desfecho));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: transforma a execução que acabou nas linhas do DATASET público —
     * uma por arquivo, e uma sintética quando não houve arquivo nenhum.
     *
     * <h2>O buraco que este método fecha, medido</h2>
     * Em 2026-08-20 o acervo estruturado do projeto tinha 2.266 execuções e <b>zero de
     * karaokê</b>. Tudo que esta fatia mede terminava em manifesto e relatório, que o publicador
     * do dataset nunca leu; o que chegava lá era uma linha genérica de sete campos por execução
     * INTEIRA, com status, dicionário, falhas e contadores espremidos numa string de detalhe.
     *
     * <h2>Invariantes do domínio</h2>
     * <ul>
     *   <li><b>Sempre sai ao menos uma linha.</b> Zero resultado e zero falha é o desfecho mais
     *       grave que existe aqui (LLM fora do ar), e é justamente o que sairia mudo. A linha
     *       {@code NAO_ALCANCADO} é a terceira saída da regra das guardas aplicada ao dataset.</li>
     *   <li>Resultados e falhas são conjuntos DISJUNTOS por construção: um arquivo que falhou não
     *       produz {@code ResultadoTraducaoKaraoke}. As duas listas somadas são o universo de
     *       arquivos que a execução tocou.</li>
     *   <li>O carimbo de tempo é UM só para a execução inteira — é ele que agrupa as linhas de
     *       volta numa run. A chave do acervo é {@code registradoEm + arquivo}, única porque
     *       nome de arquivo não repete dentro de uma pasta.</li>
     *
     *   <li><b>Os dois campos de MOTIVO passam pelo sanitizador.</b> Eles nascem de
     *       {@code e.getMessage()} de uma exceção qualquer, e a mensagem de
     *       {@code NoSuchFileException} é o caminho absoluto. O dataset é público: sem essa
     *       passagem, a primeira legenda ilegível publicaria a árvore de pastas da máquina.</li>
     * </ul>
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança. A escritora do acervo já engole os erros
     * dela; aqui o {@code catch} protege contra falha na própria montagem, para que telemetria
     * jamais derrube o registro do manifesto que veio antes.
     */
    private void acrescentarAoDataset(
        List<ResultadoTraducaoKaraoke> resultados, long duracaoMs, SnapshotContexto contexto,
        ProvenienciaCache proveniencia, DesfechoKaraoke desfecho) {
        try {
            List<ResultadoTraducaoKaraoke> feitos = resultados == null ? List.of() : resultados;
            List<FalhaArquivoKaraoke> falhas = desfecho.falhas();
            String registradoEm = Instant.now().toString();
            TelemetriaKaraoke.ExecucaoKaraoke execucao = new TelemetriaKaraoke.ExecucaoKaraoke(
                desfecho.status().name(),
                SanitizadorTelemetria.sanitizar(desfecho.motivo()),
                contexto == null ? null : contexto.id(),
                contexto == null ? null : contexto.nomeExibicao(),
                proveniencia == null ? null : proveniencia.contextoHash(),
                proveniencia == null ? null : proveniencia.modeloLlm(),
                desfecho.cacheIgnorado(),
                desfecho.estadoDicionario().name(),
                duracaoMs,
                feitos.size() + falhas.size());

            List<TelemetriaKaraoke> linhas = new ArrayList<>(feitos.size() + falhas.size() + 1);
            for (ResultadoTraducaoKaraoke r : feitos) {
                linhas.add(TelemetriaKaraoke.deArquivo(registradoEm, r, execucao));
            }
            for (FalhaArquivoKaraoke f : falhas) {
                // O motivo é e.getMessage() de uma exceção qualquer — e a mensagem de
                // NoSuchFileException/AccessDeniedException É o caminho absoluto. Sem esta
                // passagem, C:\animes\... iria direto para um repositório PÚBLICO. O nome do
                // arquivo não passa pelo sanitizador: ele já é só o nome, e é o que identifica
                // a legenda para quem for estudar a falha.
                linhas.add(TelemetriaKaraoke.deFalha(registradoEm,
                    new FalhaArquivoKaraoke(f.arquivo(), SanitizadorTelemetria.sanitizar(f.motivo())),
                    execucao));
            }
            if (linhas.isEmpty()) {
                linhas.add(TelemetriaKaraoke.semArquivo(registradoEm, execucao));
            }
            acervoDataset.registrar(linhas);
        } catch (RuntimeException e) {
            log.warn("Falha ao montar as linhas do dataset de karaoke: {}", e.toString());
        }
    }

    private String montarRelatorio(
        Path pastaOrigem, Path pastaDestino, List<ResultadoTraducaoKaraoke> resultados, long duracaoMs,
        DesfechoKaraoke desfecho) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tradução de Karaokê — letras originais preservadas + tradução PT-BR\n");
        // O desfecho encabeça o relatório: é a primeira pergunta de quem o abre.
        sb.append("Status: ").append(desfecho.status());
        if (desfecho.motivo() != null) {
            sb.append(" — ").append(desfecho.motivo());
        }
        sb.append('\n');
        sb.append("Dicionário: ").append(desfecho.estadoDicionario()).append('\n');
        if (desfecho.cacheIgnorado()) {
            sb.append("Cache IGNORADO nesta execução (proveniência divergente): tudo retraduzido\n");
        }
        if (!desfecho.falhas().isEmpty()) {
            sb.append("Arquivos com FALHA: ").append(desfecho.falhas().size()).append('\n');
            for (FalhaArquivoKaraoke f : desfecho.falhas()) {
                sb.append("  - ").append(f.arquivo()).append(" — ").append(f.motivo()).append('\n');
            }
        }
        sb.append("Origem: ").append(pastaOrigem.toAbsolutePath()).append('\n');
        sb.append("Destino: ").append(pastaDestino.toAbsolutePath()).append('\n');
        sb.append("Duração: ").append(duracaoMs).append(" ms\n\n");
        for (ResultadoTraducaoKaraoke r : resultados) {
            sb.append(r.arquivo())
                .append(" | letra original: ").append(r.preservadasOriginalJapones())
                .append(" | traduzidas (LLM): ").append(r.traduzidas())
                .append(" | cache: ").append(r.reaproveitadasCache())
                .append(" | sem tradução: ").append(r.mantidasSemTraducao())
                .append(" | acento reposto: ").append(r.acentosRepostos())
                .append(" | avisos: ").append(r.avisos().size())
                .append('\n');
        }
        return sb.toString();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: encurta a linha para o console, sem tags.
     *
     * <p>Chegou ao dono certo em 2026-08-19. Ela viveu package-private no use case esperando
     * esta classe existir, e o Javadoc de la dizia exatamente isso — a promessa foi cumprida no
     * mesmo dia em que foi escrita, e nao virou divida.
     */
    static String visivelResumido(String texto) {
        String visivel = ClassificadorLetraKaraokeService.extrairTextoVisivel(texto);
        return visivel.length() > 90 ? visivel.substring(0, 87) + "..." : visivel;
    }
}
