package org.traducao.projeto.traducaoKaraoke.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.core.presentation.web.LogStreamService;
import org.traducao.projeto.lore.domain.SnapshotContexto;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducaoKaraoke.domain.DesfechoKaraoke;
import org.traducao.projeto.traducaoKaraoke.domain.FalhaArquivoKaraoke;
import org.traducao.projeto.traducaoKaraoke.domain.ResultadoTraducaoKaraoke;
import org.traducao.projeto.traducaoKaraoke.infrastructure.TraducaoKaraokePersistencia;

import java.io.IOException;
import java.nio.file.Path;
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
