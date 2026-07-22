package org.traducao.projeto.traducao.application;

import org.springframework.stereotype.Component;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;
import org.traducao.projeto.traducao.infrastructure.config.LlmProperties;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.traducao.presentation.ui.PastasExecucao;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: resolve a IDENTIDADE de cache de um episódio — o arquivo de
 * cache no disco, o carimbo de proveniência que decide se traduções anteriores podem
 * ser reusadas e os rótulos de agrupamento (anime, temporada) derivados do caminho —,
 * isolando essa derivação da orquestração de {@link ProcessarArquivoUseCase}
 * (FASE F, R2).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O arquivo de cache mora em {@code <diretorioCache>/<anime>/<base>.cache.json},
 *       preservando a extensão do formato via {@link ResolvedorSaidaLegenda} e o mesmo
 *       nome-base do episódio.</li>
 *   <li>A proveniência carimba os seis campos canônicos (schema, contexto ativo, hash
 *       do prompt ativo, modelo, idiomas) — qualquer troca de lore/modelo/idioma muda o
 *       carimbo e invalida o cache antigo.</li>
 *   <li>O nome do anime vem da pasta-avó do arquivo ({@code <Anime>/legendas_originais/
 *       arquivo.ass}); a temporada é extraída desse nome quando presente.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Não lança: sem contexto ativo, {@code contextoId} vem nulo e o hash do prompt padrão
 * ainda é calculado (a comparação de proveniência trata nulos como divergência); um
 * caminho sem pasta-avó reconhecível resolve o anime como {@code "Desconhecido"} e a
 * temporada como {@code "Temporada Única"}.
 */
@Component
public class ResolvedorCacheTraducao {

    private static final Pattern PADRAO_TEMPORADA =
        Pattern.compile("(?i)(?:season|temporada|\\bs)\\s*0*(\\d{1,2})\\b");

    private final PastasExecucao pastasExecucao;
    private final ResolvedorSaidaLegenda resolvedorSaida;
    private final GerenciadorContexto gerenciadorContexto;
    private final LlmProperties llmPropriedades;
    private final TradutorProperties propriedades;

    /**
     * PROPÓSITO DE NEGÓCIO: injeta as fontes que compõem a identidade de cache do episódio —
     * diretórios, extensão do formato, contexto/lore ativo, modelo e idiomas.
     *
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas; não as substitui nem cria
     * implementação própria.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não valida os argumentos; a injeção CDI garante os beans.
     *
     * @param pastasExecucao raiz de cache/saída resolvidas para a execução
     * @param resolvedorSaida provê a extensão canônica do formato de legenda
     * @param gerenciadorContexto fonte do contexto/lore e do prompt ativos
     * @param llmPropriedades configuração de onde vem o modelo efetivamente ativo
     * @param propriedades idiomas de origem/destino do carimbo de proveniência
     */
    public ResolvedorCacheTraducao(
        PastasExecucao pastasExecucao,
        ResolvedorSaidaLegenda resolvedorSaida,
        GerenciadorContexto gerenciadorContexto,
        LlmProperties llmPropriedades,
        TradutorProperties propriedades
    ) {
        this.pastasExecucao = pastasExecucao;
        this.resolvedorSaida = resolvedorSaida;
        this.gerenciadorContexto = gerenciadorContexto;
        this.llmPropriedades = llmPropriedades;
        this.propriedades = propriedades;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: resolve o caminho do arquivo de cache do episódio, dentro
     * da pasta do anime, para que cada episódio tenha seu banco bilíngue próprio.
     *
     * <p>INVARIANTES DO DOMÍNIO: o cache mora em {@code <diretorioCache>/<anime>/
     * <base>.cache.json}, preservando a extensão do formato e o nome-base do episódio.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: função pura, sem I/O; caminho sem pasta-avó
     * reconhecível usa {@code "Desconhecido"} como anime.
     */
    public Path resolverArquivoCache(Path entrada) {
        String nome = entrada.getFileName().toString();
        String extensao = resolvedorSaida.extensaoLegenda(nome);
        String base = nome.substring(0, nome.length() - extensao.length());
        String animeNome = animeAPartirDoArquivo(entrada);
        return pastasExecucao.diretorioCache().resolve(animeNome).resolve(base + ".cache.json");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: carimbo de origem da tradução em cache — qual lore, hash do
     * prompt de sistema, modelo e idiomas estão em vigor nesta execução. É o que impede
     * o cache de reusar traduções feitas com um lore diferente.
     *
     * <p>INVARIANTES DO DOMÍNIO: o hash reflete o prompt ativo inteiro; qualquer mudança
     * de lore/regra o altera e invalida o cache antigo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; se não houver contexto ativo,
     * {@code contextoId} vem nulo e o {@code hashDe} do prompt padrão ainda é calculado —
     * a comparação de proveniência trata nulos como divergência.
     */
    public ProvenienciaCache provenienciaAtual() {
        return new ProvenienciaCache(
            ProvenienciaCache.SCHEMA_ATUAL,
            gerenciadorContexto.obterIdContextoAtivo(),
            ProvenienciaCache.hashDe(gerenciadorContexto.obterPromptAtivo()),
            llmPropriedades.model(),
            propriedades.idiomaOriginal(),
            propriedades.idiomaTraduzido()
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: deriva o nome do anime para agrupar cache e telemetria por
     * obra, a partir da pasta-avó do arquivo de legenda ({@code <Anime>/
     * legendas_originais/arquivo.ass}) — mesma convenção de duas pastas acima usada por
     * {@code TradutorProperties.resolverDiretorioCache()}.
     *
     * <p>INVARIANTES DO DOMÍNIO: prefere a pasta-avó; sem ela, cai para a pasta-mãe.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem pasta reconhecível, devolve
     * {@code "Desconhecido"} em vez de lançar.
     */
    public String animeAPartirDoArquivo(Path arquivoEntrada) {
        Path pastaEntrada = arquivoEntrada.getParent();
        Path pastaAnime = pastaEntrada != null ? pastaEntrada.getParent() : null;
        if (pastaAnime != null && pastaAnime.getFileName() != null) {
            return pastaAnime.getFileName().toString();
        }
        if (pastaEntrada != null && pastaEntrada.getFileName() != null) {
            return pastaEntrada.getFileName().toString();
        }
        return "Desconhecido";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: extrai o rótulo de temporada (ex.: "Season 04", "S04") do
     * nome da pasta do anime para agrupar a telemetria por temporada.
     *
     * <p>INVARIANTES DO DOMÍNIO: reconhece {@code season}/{@code temporada}/{@code s}
     * seguidos do número; sem marcador, agrupa como {@code "Temporada Única"}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nome nulo ou sem marcador devolve
     * {@code "Temporada Única"} em vez de lançar.
     */
    public String temporadaAPartirDoNome(String animeNome) {
        if (animeNome == null) {
            return "Temporada Única";
        }
        Matcher matcher = PADRAO_TEMPORADA.matcher(animeNome);
        if (matcher.find()) {
            return "Temporada " + Integer.parseInt(matcher.group(1));
        }
        return "Temporada Única";
    }
}
