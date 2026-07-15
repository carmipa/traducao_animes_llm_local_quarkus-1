package org.traducao.projeto.apiDadosAnime.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.apiDadosAnime.domain.model.AnimeMetadata;
import org.traducao.projeto.apiDadosAnime.infrastructure.adapters.AniListApiClientAdapter;
import org.traducao.projeto.apiDadosAnime.infrastructure.adapters.JikanApiClientAdapter;
import org.traducao.projeto.apiDadosAnime.infrastructure.adapters.TmdbApiClientAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PROPÓSITO DE NEGÓCIO: fornece capa e dados oficiais da obra selecionada aos
 * formulários, reutilizando cache e fontes externas redundantes.
 * <p>
 * INVARIANTES DO DOMÍNIO: cache válido tem prioridade; TMDB autenticado é a fonte
 * preferencial, AniList é o fallback público primário e Jikan o último fallback.
 * <p>
 * COMPORTAMENTO EM CASO DE FALHA: fontes indisponíveis resultam em tentativa da
 * próxima integração; sem resultado em todas elas, devolve {@link Optional#empty()}.
 */
@Service
public class ObterMetadataAnimeUseCase {

    private static final Logger log = LoggerFactory.getLogger(ObterMetadataAnimeUseCase.class);
    private static final Path PASTA_CACHE_METADATA = Path.of("cache", "metadata");
    private static final long TTL_AUSENCIA_MS = 5 * 60 * 1000L;

    private final TmdbApiClientAdapter tmdbAdapter;
    private final AniListApiClientAdapter aniListAdapter;
    private final JikanApiClientAdapter jikanAdapter;
    private final ObjectMapper mapper;
    private final ConcurrentHashMap<String, CompletableFuture<Optional<AnimeMetadata>>> buscasEmAndamento =
        new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> ausenciasRecentes = new ConcurrentHashMap<>();

    /**
     * PROPÓSITO DE NEGÓCIO: compõe cache e provedores de metadados na ordem de
     * confiança usada pelos banners do KRONOS.
     * <p>
     * INVARIANTES DO DOMÍNIO: o mapper é copiado com saída indentada para manter os
     * arquivos de cache legíveis; adapters CDI de produção não são nulos.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: configuração inválida do mapper impede a
     * criação do use case; adapters nulos são tolerados apenas pelos testes unitários.
     */
    public ObterMetadataAnimeUseCase(
            TmdbApiClientAdapter tmdbAdapter,
            AniListApiClientAdapter aniListAdapter,
            JikanApiClientAdapter jikanAdapter,
            ObjectMapper mapper) {
        this.tmdbAdapter = tmdbAdapter;
        this.aniListAdapter = aniListAdapter;
        this.jikanAdapter = jikanAdapter;
        this.mapper = mapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: resolve o nome de uma obra em dados visuais para o
     * banner, evitando chamadas repetidas por meio do cache local.
     * <p>
     * INVARIANTES DO DOMÍNIO: entrada é sanitizada antes de formar chave de cache;
     * somente metadados encontrados são persistidos.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: cache ilegível é ignorado; cada fonte vazia
     * conduz à próxima e a ausência total devolve {@link Optional#empty()}.
     */
    public Optional<AnimeMetadata> executar(String caminhoOuNome) {
        if (caminhoOuNome == null || caminhoOuNome.isBlank()) {
            return Optional.empty();
        }

        String nomeSanitizado = extrairNomeTermoBusca(caminhoOuNome);
        if (nomeSanitizado.isBlank()) {
            return Optional.empty();
        }

        Path arquivoCache = resolverArquivoCache(nomeSanitizado);
        if (Files.isRegularFile(arquivoCache)) {
            try {
                AnimeMetadata metadata = mapper.readValue(arquivoCache.toFile(), AnimeMetadata.class);
                return Optional.of(metadata);
            } catch (IOException e) {
                log.warn("Falha ao ler cache de metadata em {}: {}", arquivoCache, e.getMessage());
            }
        }

        Long ausenciaAte = ausenciasRecentes.get(nomeSanitizado);
        long agora = System.currentTimeMillis();
        if (ausenciaAte != null && ausenciaAte > agora) {
            return Optional.empty();
        }
        if (ausenciaAte != null) {
            ausenciasRecentes.remove(nomeSanitizado, ausenciaAte);
        }

        CompletableFuture<Optional<AnimeMetadata>> novaBusca = new CompletableFuture<>();
        CompletableFuture<Optional<AnimeMetadata>> buscaExistente =
            buscasEmAndamento.putIfAbsent(nomeSanitizado, novaBusca);
        if (buscaExistente != null) {
            try {
                return buscaExistente.join();
            } catch (CompletionException e) {
                log.warn("Busca concorrente de metadata falhou para {}: {}", nomeSanitizado, e.getMessage());
                return Optional.empty();
            }
        }

        try {
            Optional<AnimeMetadata> resultado = buscarRemoto(nomeSanitizado, arquivoCache);
            if (resultado.isEmpty()) {
                ausenciasRecentes.put(nomeSanitizado, agora + TTL_AUSENCIA_MS);
            } else {
                ausenciasRecentes.remove(nomeSanitizado);
            }
            novaBusca.complete(resultado);
            return resultado;
        } catch (RuntimeException e) {
            novaBusca.completeExceptionally(e);
            log.warn("Falha inesperada ao obter metadata para {}: {}", nomeSanitizado, e.getMessage());
            return Optional.empty();
        } finally {
            buscasEmAndamento.remove(nomeSanitizado, novaBusca);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: executa uma única cadeia de provedores para todos os
     * consumidores concorrentes que pediram a mesma obra.
     *
     * <p>INVARIANTES DO DOMÍNIO: TMDB, AniList e Jikan mantêm a ordem de
     * preferência; somente resultado presente é persistido.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: ausência em todas as fontes devolve
     * {@link Optional#empty()} e ativa o cache negativo temporário do chamador.
     */
    private Optional<AnimeMetadata> buscarRemoto(String nomeSanitizado, Path arquivoCache) {
        Optional<AnimeMetadata> obtidoOpt = Optional.empty();
        if (tmdbAdapter != null && tmdbAdapter.isConfigurado()) {
            obtidoOpt = tmdbAdapter.buscarPorNome(nomeSanitizado);
        }

        if (obtidoOpt.isEmpty() && aniListAdapter != null) {
            obtidoOpt = aniListAdapter.buscarPorNome(nomeSanitizado);
        }

        if (obtidoOpt.isEmpty() && jikanAdapter != null) {
            obtidoOpt = jikanAdapter.buscarPorNome(nomeSanitizado);
        }

        if (obtidoOpt.isPresent()) {
            salvarEmCache(arquivoCache, obtidoOpt.get());
        }

        return obtidoOpt;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: transforma nomes de contextos e caminhos de releases
     * em termos reconhecíveis pelas APIs de anime sem perder partes do título.
     * <p>
     * INVARIANTES DO DOMÍNIO: conteúdo semântico entre parênteses é preservado
     * (por exemplo, {@code Narrative} e {@code Eighty-Six}); tags de fansub,
     * resolução, codec, temporada técnica e ano isolado são removidos.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: uma entrada sem conteúdo útil resulta em
     * texto vazio, levando {@link #executar(String)} a devolver {@link Optional#empty()}.
     */
    public String extrairNomeTermoBusca(String entrada) {
        String texto = entrada.replace('\\', '/');
        if (texto.contains("/")) {
            String[] partes = texto.split("/");
            for (int i = partes.length - 1; i >= 0; i--) {
                String p = partes[i].trim();
                if (!p.isBlank() && !p.equalsIgnoreCase("cache") && !p.startsWith("Season") && !p.startsWith("season")) {
                    texto = p;
                    break;
                }
            }
        }

        // Remove extensao(es) do nome do arquivo (ex.: ".cache.json", ".mkv", ".ass")
        // antes de tokenizar, senao "cache"/"json"/"mkv" sobram como ruido na busca.
        texto = texto.replaceAll("(?:\\.[A-Za-z0-9]{1,5})+$", "");

        texto = texto.replaceAll("\\[[^\\]]*\\]", " ")
                     // Parenteses em nomes de contexto frequentemente carregam
                     // aliases essenciais: "NT (Narrative)", "86 (Eighty-Six)".
                     // Remove apenas os delimitadores; o filtro de ruido abaixo
                     // descarta codecs/temporadas que estavam dentro deles.
                     .replaceAll("\\(([^\\)]*)\\)", " $1 ")
                     .replaceAll("(?i)\\s*-?\\s*Revis[aã]o\\s+de\\s+Lore\\s*$", " ")
                     // Separadores primeiro: "_ENG" so e removido pela lista de ruido
                     // abaixo se o "_" já tiver virado espaço (\b não separa "_E").
                     .replaceAll("[_.-]", " ")
                     .replaceAll("(?i)\\bS\\d{1,2}E\\d{1,3}\\b|\\b(Season|S)\\s*\\d+\\b|\\bE\\d{1,3}\\b", " ")
                     .replaceAll("(?i)\\b(1080p|720p|4k|BD|AV1|HEVC|x264|x265|Dual Audio|Multi-Audio|ENG|PTBR|PT\\s*BR|Track\\d+)\\b", " ")
                     .replaceAll("\\b(?:19|20)\\d{2}\\b", " ")
                     .replaceAll("\\s+", " ")
                     .trim();

        return texto;
    }

    private Path resolverArquivoCache(String nomeSanitizado) {
        String chaveValida = nomeSanitizado.toLowerCase().replaceAll("[^a-z0-9]", "_").replaceAll("_+", "_");
        if (chaveValida.isBlank()) {
            chaveValida = "anime_desconhecido";
        }
        return PASTA_CACHE_METADATA.resolve(chaveValida + ".json");
    }

    private void salvarEmCache(Path arquivoCache, AnimeMetadata metadata) {
        try {
            Files.createDirectories(PASTA_CACHE_METADATA);
            mapper.writeValue(arquivoCache.toFile(), metadata);
        } catch (IOException e) {
            log.warn("Falha ao salvar cache de metadata em {}: {}", arquivoCache, e.getMessage());
        }
    }
}
