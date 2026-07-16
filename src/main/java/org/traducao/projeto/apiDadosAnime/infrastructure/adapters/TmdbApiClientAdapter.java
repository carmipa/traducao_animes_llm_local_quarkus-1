package org.traducao.projeto.apiDadosAnime.infrastructure.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.traducao.projeto.apiDadosAnime.domain.model.AnimeMetadata;
import org.traducao.projeto.apiDadosAnime.infrastructure.config.ApiDadosAnimeHttpProperties;
import org.traducao.projeto.core.infrastructure.http.JsonHttpClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class TmdbApiClientAdapter {

    private static final Logger log = LoggerFactory.getLogger(TmdbApiClientAdapter.class);
    private static final String TMDB_BASE_URL = "https://api.themoviedb.org/3";
    private static final String TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";

    private final JsonHttpClient httpClient;
    private final ObjectMapper mapper;
    private final String apiKey;

    public TmdbApiClientAdapter(
            ApiDadosAnimeHttpProperties http,
            ObjectMapper mapper,
            @Value("${tmdb.api-key:}") String apiKey) {
        this.httpClient = new JsonHttpClient(http.connectTimeout(), http.readTimeout(), TMDB_BASE_URL, mapper);
        this.mapper = mapper;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
    }

    public boolean isConfigurado() {
        return !apiKey.isBlank()
            && !apiKey.equalsIgnoreCase("dummy_key")
            && !apiKey.equalsIgnoreCase("INSIRA_SUA_CHAVE_DO_TMDB_AQUI")
            && !apiKey.equalsIgnoreCase("sua-chave-tmdb-aqui")
            && !apiKey.equalsIgnoreCase("sua-chave-tmdb-real-aqui");
    }

    public Optional<AnimeMetadata> buscarPorNome(String termoBusca) {
        if (!isConfigurado() || termoBusca == null || termoBusca.isBlank()) {
            return Optional.empty();
        }

        Optional<AnimeMetadata> tvOpt = buscarEmEndpoint("/search/tv", termoBusca, true);
        if (tvOpt.isPresent()) {
            return tvOpt;
        }

        return buscarEmEndpoint("/search/movie", termoBusca, false);
    }

    private Optional<AnimeMetadata> buscarEmEndpoint(String endpoint, String termoBusca, boolean eSerieTv) {
        try {
            String encoded = URLEncoder.encode(termoBusca, StandardCharsets.UTF_8);
            String url = String.format("%s%s?api_key=%s&query=%s&language=pt-BR&include_adult=false",
                TMDB_BASE_URL, endpoint, apiKey, encoded);

            String responseBody = httpClient.getAbsolute(url);
            if (responseBody == null || responseBody.isBlank()) {
                return Optional.empty();
            }

            JsonNode root = mapper.readTree(responseBody);
            JsonNode results = root.path("results");
            if (results.isArray() && !results.isEmpty()) {
                JsonNode item = results.get(0);
                return Optional.of(mapearNodeParaMetadata(item, eSerieTv));
            }

        } catch (Exception e) {
            log.warn("Falha ao buscar no TMDB ({}) para \"{}\": {}", endpoint, termoBusca, e.getMessage());
        }

        return Optional.empty();
    }

    private AnimeMetadata mapearNodeParaMetadata(JsonNode item, boolean eSerieTv) {
        String titulo = eSerieTv ? item.path("name").asText(null) : item.path("title").asText(null);
        String tituloOriginal = eSerieTv ? item.path("original_name").asText(null) : item.path("original_title").asText(null);

        String posterPath = item.path("poster_path").asText(null);
        String posterUrl = (posterPath != null && !posterPath.isBlank() && !posterPath.equals("null"))
                ? TMDB_IMAGE_BASE_URL + posterPath
                : null;

        String dateStr = eSerieTv ? item.path("first_air_date").asText(null) : item.path("release_date").asText(null);
        Integer ano = null;
        if (dateStr != null && dateStr.length() >= 4) {
            try {
                ano = Integer.parseInt(dateStr.substring(0, 4));
            } catch (NumberFormatException ignored) {}
        }

        Double score = item.path("vote_average").isNumber() ? item.path("vote_average").asDouble() : null;
        if (score != null) {
            score = Math.round(score * 10.0) / 10.0;
        }

        String sinopse = item.path("overview").asText(null);

        List<String> generos = new ArrayList<>();
        generos.add(eSerieTv ? "Série / Anime" : "Filme");

        return new AnimeMetadata(
                titulo != null ? titulo : "Desconhecido",
                tituloOriginal,
                null,
                posterUrl,
                ano,
                null,
                score,
                sinopse,
                generos
        );
    }
}
