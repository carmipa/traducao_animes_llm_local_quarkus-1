package org.traducao.projeto.cachetraducao.infrastructure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.core.util.ArquivoAtomicoUtil;
import org.traducao.projeto.legenda.domain.ArquivoLegendaException;
import org.traducao.projeto.cachetraducao.domain.CacheDocumento;
import org.traducao.projeto.cachetraducao.domain.EntradaCache;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: persiste, por arquivo de legenda, o par (texto original →
 * texto traduzido) em JSON, no formato versionado {@link CacheDocumento}. Serve para
 * (1) permitir revisão/correção manual do cache editando o JSON e (2) evitar chamar o
 * LLM de novo para falas já traduzidas numa execução anterior.
 *
 * <p>INVARIANTES DO DOMÍNIO: cada arquivo carrega a {@link ProvenienciaCache}
 * (lore/hash/modelo/idiomas) que o gerou; uma proveniência divergente NÃO é reutilizada
 * — a geração anterior é COPIADA para um arquivo datado ao lado e o episódio é retraduzido
 * com o lore atual, mas o cache ativo permanece no lugar até a nova geração ser gravada.
 * O caminho ativo nunca fica vazio durante uma tradução: a escrita é atômica (temporário +
 * {@code ArquivoAtomicoUtil}) e é o único momento em que o conteúdo ativo troca. A leitura
 * aceita tanto o documento versionado quanto a lista JSON histórica.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: um JSON ilegível é preservado (renomeado
 * {@code .corrompido_<ts>.json}) em vez de ignorado/sobrescrito; falha de gravação
 * propaga {@link ArquivoLegendaException} sem deixar o destino truncado.
 */
@Component
public class CacheTraducaoService {

    private static final Logger log = LoggerFactory.getLogger(CacheTraducaoService.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ObjectMapper objectMapper;

    /**
     * PROPÓSITO DE NEGÓCIO: marca {@link ProvenienciaCache} para que campos nulos NÃO sejam
     * escritos no JSON — hoje isso vale só para {@code modeloHerdado}, presente apenas nos caches
     * que herdaram traduções de outro modelo.
     *
     * <p>INVARIANTES DO DOMÍNIO: é um MIXIN, e não uma anotação no record, porque
     * {@code cachetraducao.domain} é puro por contrato de arquitetura e não conhece Jackson —
     * {@code FronteiraCacheTraducaoArchTest} reprova a anotação lá. Aplicado só a esta classe: um
     * {@code NON_NULL} global no mapper mudaria também a serialização de {@link EntradaCache},
     * cujos campos de texto aceitam nulo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem o mixin, o JSON ganha {@code "modeloHerdado":null} em
     * todo cache regravado e deixa de ser estruturalmente idêntico ao legado —
     * {@code CompatibilidadeCacheJsonLegadoTest} reprova.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    abstract static class ProvenienciaSemCamposNulos {
    }

    public CacheTraducaoService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
            .addMixIn(ProvenienciaCache.class, ProvenienciaSemCamposNulos.class);
    }

    /**
     * Resultado da carga versionada: o mapa reaproveitavel (original -> traduzido),
     * quantas entradas foram invalidadas por mudanca de proveniencia e se o
     * arquivo veio do formato antigo (lista pura) e sera migrado ao salvar.
     */
    public record ResultadoCarga(Map<String, String> mapa, int invalidadas, boolean migrado,
            String modeloHerdado) {

        /** A forma de sempre: sem herança entre modelos. */
        public ResultadoCarga(Map<String, String> mapa, int invalidadas, boolean migrado) {
            this(mapa, invalidadas, migrado, null);
        }

        public static ResultadoCarga vazio() {
            return new ResultadoCarga(new HashMap<>(), 0, false, null);
        }

        /**
         * Diz se estas entradas vieram de OUTRO modelo. Quem salvar precisa carimbar
         * {@code herdandoDe(modeloHerdado)}, senão o cache passa a afirmar que o modelo atual
         * traduziu o que ele apenas herdou.
         */
        public boolean herdouDeOutroModelo() {
            return modeloHerdado != null && !modeloHerdado.isBlank();
        }
    }

    /**
     * PROPOSITO DE NEGOCIO: Carrega o cache reaproveitavel apenas quando a
     * proveniencia atual (lore/modelo/idiomas) bate com a que gerou o arquivo.
     *
     * <p>INVARIANTES DO DOMINIO: proveniencia divergente nunca e reutilizada
     * (arquiva a geracao anterior e devolve mapa vazio + contagem invalidada);
     * formato antigo sem cabecalho e assumido compativel nesta migracao e
     * versionado a partir do proximo salvamento; arquivo ilegivel e preservado,
     * nunca lido como vazio-e-sobrescrito.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lanca; devolve
     * {@link ResultadoCarga#vazio()} (mapa vazio) e, quando aplicavel, move o
     * arquivo problematico para o lado antes de retornar.
     */
    public ResultadoCarga carregar(Path arquivoCache, ProvenienciaCache provenienciaAtual) {
        return carregar(arquivoCache, provenienciaAtual, false);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mesma carga, podendo AUTORIZAR que o modelo atual reaproveite o
     * trabalho de outro modelo quando lore, prompt, idiomas e schema são idênticos.
     *
     * <h2>Para que serve, com o número que a justifica</h2>
     * Exercitar 6 falas pendentes do Zeta custava retraduzir os 50 episódios — 17.090 falas — só
     * porque o titular mudou de mistral-nemo para aya. Com o reuso autorizado, o experimento manda
     * ao LLM apenas o que falta, e comparar modelos numa obra grande deixa de custar uma noite.
     *
     * <h2>Invariantes do domínio</h2>
     * <ul>
     *   <li>DESLIGADO por padrão: a sobrecarga de dois argumentos passa {@code false}. Falha
     *       fechada — nenhum fluxo herda tradução de outro modelo sem alguém ter pedido.</li>
     *   <li>Só o MODELO pode divergir. Lore ou prompt diferentes continuam invalidando, porque aí a
     *       tradução correta é outra.</li>
     *   <li>O resultado carrega {@code modeloHerdado} preenchido, e quem salvar DEVE carimbá-lo com
     *       {@link ProvenienciaCache#herdandoDe}. Sem isso o cache passa a afirmar que o modelo
     *       atual produziu o que ele apenas herdou — e a comparação entre modelos, que depende da
     *       proveniência, morre em silêncio.</li>
     *   <li>A geração anterior é arquivada do mesmo jeito: reuso não dispensa o histórico.</li>
     * </ul>
     *
     * <h2>Comportamento em caso de falha</h2>
     * Idêntico à sobrecarga simples: nunca lança, preserva arquivo ilegível, devolve vazio.
     *
     * @param permitirReusoEntreModelos autoriza herdar as traduções de outro modelo
     */
    public ResultadoCarga carregar(Path arquivoCache, ProvenienciaCache provenienciaAtual,
            boolean permitirReusoEntreModelos) {
        if (!Files.exists(arquivoCache)) {
            return ResultadoCarga.vazio();
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(arquivoCache.toFile());
        } catch (IOException e) {
            log.error("Cache ilegivel/corrompido em {}: {}", arquivoCache, e.getMessage());
            preservarCorrompido(arquivoCache);
            return ResultadoCarga.vazio();
        }

        if (root == null || root.isNull()) {
            preservarCorrompido(arquivoCache);
            return ResultadoCarga.vazio();
        }

        // Formato antigo: lista pura de entradas, sem cabecalho de proveniencia.
        if (root.isArray()) {
            try {
                List<EntradaCache> entradas = objectMapper.convertValue(root,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, EntradaCache.class));
                log.warn("Cache sem proveniencia em {} — assumindo compativel nesta migracao; sera versionado a partir de agora.",
                    arquivoCache);
                return new ResultadoCarga(montarMapa(entradas), 0, true);
            } catch (IllegalArgumentException e) {
                preservarCorrompido(arquivoCache);
                return ResultadoCarga.vazio();
            }
        }

        // Formato novo: objeto com proveniencia + entradas.
        if (root.isObject()) {
            CacheDocumento doc;
            try {
                doc = objectMapper.treeToValue(root, CacheDocumento.class);
            } catch (Exception e) {
                preservarCorrompido(arquivoCache);
                return ResultadoCarga.vazio();
            }
            List<EntradaCache> entradas = doc.entradas() != null ? doc.entradas() : List.of();
            if (!provenienciaAtual.mesmaProveniencia(doc.proveniencia())) {
                // Antes de invalidar: o operador autorizou herdar de outro modelo, e a divergência
                // é SÓ o modelo? Então o trabalho anterior é reaproveitado — e o cache resultante
                // vai dizer de quem herdou, porque quem salva carimba com herdandoDe().
                if (permitirReusoEntreModelos
                    && provenienciaAtual.divergeSomenteNoModelo(doc.proveniencia())) {
                    arquivarGeracao(arquivoCache, doc.proveniencia());
                    Map<String, String> herdado = montarMapa(entradas);
                    String modeloAnterior = doc.proveniencia().modeloLlm();
                    log.warn("REUSO ENTRE MODELOS autorizado em {}: {} entrada(s) de \"{}\" "
                            + "reaproveitada(s) por \"{}\". O cache resultante sera carimbado como "
                            + "herdado — nao use este arquivo para comparar os dois modelos.",
                        arquivoCache, herdado.size(), modeloAnterior, provenienciaAtual.modeloLlm());
                    System.out.println("[CACHE] REUSO ENTRE MODELOS: " + herdado.size()
                        + " fala(s) de \"" + modeloAnterior + "\" reaproveitada(s) por \""
                        + provenienciaAtual.modeloLlm() + "\". So o que faltar vai ao LLM.");
                    return new ResultadoCarga(herdado, 0, false, modeloAnterior);
                }
                arquivarGeracao(arquivoCache, doc.proveniencia());
                log.warn("Proveniencia do cache mudou (lore/modelo) em {} — {} entrada(s) arquivada(s) e NAO reutilizada(s).",
                    arquivoCache, entradas.size());
                return new ResultadoCarga(new HashMap<>(), entradas.size(), false);
            }
            Map<String, String> mapa = montarMapa(entradas);
            log.info("Cache carregado de {} ({} entradas reaproveitaveis, contexto {}, modelo {})",
                arquivoCache, mapa.size(),
                doc.proveniencia() != null ? doc.proveniencia().contextoId() : "?",
                doc.proveniencia() != null ? doc.proveniencia().modeloLlm() : "?");
            return new ResultadoCarga(mapa, 0, false);
        }

        // Escalar/booleano/etc. — nao e um cache valido.
        preservarCorrompido(arquivoCache);
        return ResultadoCarga.vazio();
    }

    public void salvar(Path arquivoCache, ProvenienciaCache proveniencia, List<EntradaCache> entradas) {
        try {
            Path pasta = arquivoCache.toAbsolutePath().getParent();
            if (pasta != null) {
                Files.createDirectories(pasta);
            }
            // Mesmo padrão do EscritorLegendaAss: escreve num temporário e só
            // substitui o destino com move atômico. Uma queda no meio da
            // escrita não pode corromper o cache — ele guarda horas de
            // tradução via LLM.
            CacheDocumento documento = new CacheDocumento(proveniencia, entradas);
            Path temp = Files.createTempFile(pasta, arquivoCache.getFileName().toString(), ".tmp");
            try {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), documento);
                ArquivoAtomicoUtil.substituirAtomico(temp, arquivoCache);
            } finally {
                Files.deleteIfExists(temp);
            }
            log.info("Cache de traducao salvo em {} ({} entradas, contexto {}, modelo {})",
                arquivoCache, entradas.size(),
                proveniencia != null ? proveniencia.contextoId() : "?",
                proveniencia != null ? proveniencia.modeloLlm() : "?");
        } catch (IOException e) {
            throw new ArquivoLegendaException("Falha ao salvar cache de traducao: " + arquivoCache, e);
        }
    }

    /** Preserva um cache legado antes de um fluxo estrito recusá-lo por não ter proveniência. */
    public void arquivarGeracaoSemProveniencia(Path arquivoCache) {
        if (arquivoCache != null && Files.exists(arquivoCache)) {
            arquivarGeracao(arquivoCache, null);
        }
    }

    // --- Formato antigo (lista pura), mantido para os fluxos que ainda nao
    // --- versionam o cache (ex.: traducao de karaoke). ---

    public Map<String, String> carregar(Path arquivoCache) {
        if (!Files.exists(arquivoCache)) {
            return new HashMap<>();
        }
        try {
            List<EntradaCache> entradas = objectMapper.readValue(arquivoCache.toFile(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, EntradaCache.class));
            Map<String, String> mapa = montarMapa(entradas);
            log.info("Cache carregado de {} ({} entradas reaproveitaveis)", arquivoCache, mapa.size());
            return mapa;
        } catch (IOException e) {
            // PRESERVA antes de devolver vazio. O fluxo versionado sempre fez isso; este,
            // usado pelo karaokê, apenas ignorava — e como o mapa vazio faz o chamador
            // traduzir do zero e GRAVAR por cima, o arquivo ilegível era destruído na mesma
            // execução, sem cópia. Um cache guarda horas de GPU: JSON quebrado ainda pode ser
            // recuperado à mão, sobrescrito não pode.
            log.error("Cache legado ilegível em {}: {}. Preservando antes de traduzir do zero.",
                arquivoCache, e.getMessage());
            preservarCorrompido(arquivoCache);
            return new HashMap<>();
        }
    }

    public void salvar(Path arquivoCache, List<EntradaCache> entradas) {
        try {
            Path pasta = arquivoCache.toAbsolutePath().getParent();
            if (pasta != null) {
                Files.createDirectories(pasta);
            }
            Path temp = Files.createTempFile(pasta, arquivoCache.getFileName().toString(), ".tmp");
            try {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), entradas);
                ArquivoAtomicoUtil.substituirAtomico(temp, arquivoCache);
            } finally {
                Files.deleteIfExists(temp);
            }
            log.info("Cache de traducao salvo em {} ({} entradas)", arquivoCache, entradas.size());
        } catch (IOException e) {
            throw new ArquivoLegendaException("Falha ao salvar cache de traducao: " + arquivoCache, e);
        }
    }

    private Map<String, String> montarMapa(List<EntradaCache> entradas) {
        Map<String, String> mapa = new HashMap<>();
        if (entradas == null) {
            return mapa;
        }
        for (EntradaCache entrada : entradas) {
            if (entrada.traduzido() != null && !entrada.traduzido().isBlank()) {
                mapa.put(entrada.original(), entrada.traduzido());
            }
        }
        return mapa;
    }

    private void preservarCorrompido(Path arquivoCache) {
        try {
            Path destino = arquivoCache.resolveSibling(
                arquivoCache.getFileName().toString() + ".corrompido_" + LocalDateTime.now().format(TS) + ".json");
            Files.move(arquivoCache, destino);
            log.error("Cache corrompido preservado em {} (nao sobrescrito). A traducao recomeca do zero.", destino);
        } catch (IOException e) {
            log.error("Falha ao preservar o cache corrompido {}: {}", arquivoCache, e.getMessage());
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: guarda uma cópia datada da geração anterior quando a proveniência
     * muda (troca de lore/modelo/schema), para que o trabalho de LLM daquela geração continue
     * recuperável mesmo depois de o episódio ser retraduzido com o contexto novo.
     *
     * <p>INVARIANTES DO DOMÍNIO: COPIA e NUNCA remove o cache do caminho ativo. A geração
     * anterior deixa de ser reutilizada por decisão EM MEMÓRIA — {@link #carregar} devolve
     * mapa vazio e a contagem de invalidadas —, não por o arquivo sumir do disco. O caminho
     * ativo só muda quando {@link #salvar} conclui a substituição atômica da nova geração.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança nem propaga; falha de I/O na cópia é
     * registrada em WARN e a carga prossegue — o cache ativo permanece intacto de qualquer
     * forma, então nenhuma tradução é perdida por causa do arquivamento.
     *
     * <p>HISTÓRICO: até 2026-07-22 este método fazia {@code Files.move}, tirando o cache do
     * caminho ativo ANTES das ~25 minutos de tradução seguintes. Uma interrupção nessa janela
     * deixava o episódio sem cache algum — foi o que aconteceu com o S00E02 do 08th MS Team,
     * recuperado na mão a partir de {@code backups/traducao-cache}.
     */
    private void arquivarGeracao(Path arquivoCache, ProvenienciaCache anterior) {
        try {
            String tag = anterior != null && anterior.contextoHash() != null && anterior.contextoHash().length() >= 8
                ? anterior.contextoHash().substring(0, 8) : "anterior";
            Path destino = arquivoCache.resolveSibling(
                arquivoCache.getFileName().toString() + ".geracao_" + tag + "_" + LocalDateTime.now().format(TS) + ".json");
            Files.copy(arquivoCache, destino, StandardCopyOption.COPY_ATTRIBUTES);
            log.info("Geracao anterior do cache copiada para {} (o cache ativo permanece ate a nova geracao ser gravada).",
                destino);
        } catch (IOException e) {
            // ERROR, e com a CONSEQUÊNCIA escrita: o Javadoc acima garante que nada se perde "por
            // causa do arquivamento", e isso é verdade AQUI — mas só até o `salvar` seguinte, que
            // substitui o caminho ativo. Sem a cópia, aquela substituição apaga a geração anterior
            // para sempre. Um WARN dizendo apenas "falha ao arquivar" não deixa isso visível, e o
            // gatilho é real nesta máquina: a suíte de 12/08 registrou "not enough space on the
            // disk" em outro fluxo.
            log.error("NAO foi possivel arquivar a geracao anterior de {}: {}. O cache ativo segue "
                    + "intacto AGORA, mas a proxima gravacao vai substitui-lo e esta geracao sera "
                    + "PERDIDA — copie o arquivo a mao antes de deixar a retraducao terminar.",
                arquivoCache, e.getMessage());
            System.out.println("[CACHE] ATENCAO: falha ao arquivar a geracao anterior de "
                + arquivoCache.getFileName() + " (" + e.getMessage() + "). Se a retraducao "
                + "terminar, a geracao anterior sera perdida.");
        }
    }
}
