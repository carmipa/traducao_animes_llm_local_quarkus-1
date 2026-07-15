package org.traducao.projeto.apiDadosAnime.infrastructure.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.apiDadosAnime.domain.model.AnimeMetadata;
import org.traducao.projeto.traducao.infrastructure.config.LlmProperties;
import org.traducao.projeto.traducao.infrastructure.http.JsonHttpClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class JikanApiClientAdapter {

    private static final Logger log = LoggerFactory.getLogger(JikanApiClientAdapter.class);
    private static final String JIKAN_BASE_URL = "https://api.jikan.moe/v4";

    private final JsonHttpClient httpClient;
    private final ObjectMapper mapper;

    public JikanApiClientAdapter(LlmProperties llmProperties, ObjectMapper mapper) {
        this.httpClient = new JsonHttpClient(llmProperties, JIKAN_BASE_URL, mapper);
        this.mapper = mapper;
    }

    public Optional<AnimeMetadata> buscarPorNome(String termoBusca) {
        if (termoBusca == null || termoBusca.isBlank()) {
            return Optional.empty();
        }

        try {
            String encoded = URLEncoder.encode(termoBusca, StandardCharsets.UTF_8);
            String responseBody = httpClient.getString("/anime?q=" + encoded + "&limit=1");

            if (responseBody == null || responseBody.isBlank()) {
                return Optional.empty();
            }

            JsonNode root = mapper.readTree(responseBody);
            JsonNode data = root.path("data");
            if (data.isArray() && !data.isEmpty()) {
                JsonNode item = data.get(0);
                return Optional.of(mapearNodeParaMetadata(item));
            }

        } catch (Exception e) {
            log.warn("Falha ao buscar metadados no Jikan para \"{}\": {}", termoBusca, e.getMessage());
        }

        return Optional.empty();
    }

    private AnimeMetadata mapearNodeParaMetadata(JsonNode item) {
        String titulo = item.path("title").asText(null);
        String tituloIngles = item.path("title_english").asText(null);
        String tituloJapones = item.path("title_japanese").asText(null);

        String posterUrl = item.path("images").path("jpg").path("large_image_url").asText(
            item.path("images").path("jpg").path("image_url").asText(null)
        );

        Integer ano = item.path("year").isNumber() ? item.path("year").asInt() : null;
        if (ano == null) {
            String airedFrom = item.path("aired").path("from").asText(null);
            if (airedFrom != null && airedFrom.length() >= 4) {
                try {
                    ano = Integer.parseInt(airedFrom.substring(0, 4));
                } catch (NumberFormatException ignored) {}
            }
        }

        Integer episodios = item.path("episodes").isNumber() ? item.path("episodes").asInt() : null;
        Double score = item.path("score").isNumber() ? item.path("score").asDouble() : null;
        String sinopse = item.path("synopsis").asText(null);

        List<String> generos = new ArrayList<>();
        JsonNode genresNode = item.path("genres");
        if (genresNode.isArray()) {
            for (JsonNode g : genresNode) {
                String nomeG = g.path("name").asText(null);
                if (nomeG != null) {
                    generos.add(nomeG);
                }
            }
        }

        return new AnimeMetadata(
            titulo != null ? titulo : "Desconhecido",
            tituloIngles,
            tituloJapones,
            posterUrl,
            ano,
            episodios,
            score,
            sinopse,
            generos
        );
    }
}
