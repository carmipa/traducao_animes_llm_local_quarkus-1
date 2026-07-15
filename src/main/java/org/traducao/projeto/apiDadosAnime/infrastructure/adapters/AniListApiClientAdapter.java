package org.traducao.projeto.apiDadosAnime.infrastructure.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.traducao.projeto.apiDadosAnime.domain.model.AnimeMetadata;
import org.traducao.projeto.traducao.infrastructure.config.LlmProperties;
import org.traducao.projeto.traducao.infrastructure.http.JsonHttpClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PROPÓSITO DE NEGÓCIO: consulta a API pública GraphQL da AniList para manter
 * capas e dados das obras disponíveis quando a fonte principal estiver fora.
 * <p>
 * INVARIANTES DO DOMÍNIO: pesquisa somente mídia do tipo ANIME; não exige chave
 * ou autenticação; converte a nota percentual da AniList para a escala de 0 a 10.
 * <p>
 * COMPORTAMENTO EM CASO DE FALHA: registra a causa e devolve
 * {@link Optional#empty()}, permitindo que o use case tente a próxima fonte.
 */
@Component
public class AniListApiClientAdapter {

    private static final Logger log = LoggerFactory.getLogger(AniListApiClientAdapter.class);
    private static final String ANILIST_BASE_URL = "https://graphql.anilist.co";
    private static final String CONSULTA = """
        query ($search: String!) {
          Media(search: $search, type: ANIME) {
            title { romaji english native }
            coverImage { extraLarge large }
            seasonYear
            episodes
            averageScore
            description(asHtml: false)
            genres
          }
        }
        """;

    private final JsonHttpClient httpClient;
    private final ObjectMapper mapper;

    /**
     * PROPÓSITO DE NEGÓCIO: prepara o cliente público da AniList com a mesma
     * política de rede configurada para o KRONOS.
     * <p>
     * INVARIANTES DO DOMÍNIO: a URL base é HTTPS e o mapper é o gerenciado pela
     * aplicação para manter o contrato JSON uniforme.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: erros de construção são propagados pelo CDI,
     * pois sem cliente válido o adapter não pode cumprir sua responsabilidade.
     */
    @Autowired
    public AniListApiClientAdapter(LlmProperties propriedades, ObjectMapper mapper) {
        this(new JsonHttpClient(propriedades, ANILIST_BASE_URL, mapper), mapper);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: permite validar deterministicamente o mapeamento da
     * AniList sem realizar chamadas externas durante a suíte automatizada.
     * <p>
     * INVARIANTES DO DOMÍNIO: usa exatamente o mesmo mapper do construtor CDI;
     * o cliente pode ser nulo somente em testes que não executam a busca remota.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: uma chamada remota com cliente nulo falha e
     * é convertida em retorno vazio por {@link #buscarPorNome(String)}.
     */
    AniListApiClientAdapter(JsonHttpClient httpClient, ObjectMapper mapper) {
        this.httpClient = httpClient;
        this.mapper = mapper;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: procura uma obra pelo título e devolve seus metadados
     * visuais para os banners dos formulários.
     * <p>
     * INVARIANTES DO DOMÍNIO: termos vazios nunca geram chamadas externas e apenas
     * o objeto {@code data.Media} de uma resposta GraphQL válida é aceito.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: devolve vazio para resposta sem mídia, erro
     * HTTP, timeout ou JSON incompatível, sem interromper a requisição web.
     */
    public Optional<AnimeMetadata> buscarPorNome(String termoBusca) {
        if (termoBusca == null || termoBusca.isBlank()) {
            return Optional.empty();
        }

        try {
            JsonNode resposta = httpClient.post("", new GraphQlRequest(
                CONSULTA, Map.of("search", termoBusca)), JsonNode.class);
            return mapearResposta(resposta);
        } catch (Exception e) {
            log.warn("Falha ao buscar metadados na AniList para \"{}\": {}", termoBusca, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: converte a resposta GraphQL da AniList para o modelo
     * único usado pelo frontend e pelo cache de metadados.
     * <p>
     * INVARIANTES DO DOMÍNIO: título romaji é a identificação principal; capa usa
     * a maior resolução disponível; nota percentual é dividida por dez.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: devolve vazio quando {@code data.Media} ou o
     * título principal não existem, evitando gravar cache inválido.
     */
    Optional<AnimeMetadata> mapearResposta(JsonNode raiz) {
        JsonNode media = raiz == null ? null : raiz.path("data").path("Media");
        if (media == null || media.isMissingNode() || media.isNull()) {
            return Optional.empty();
        }

        JsonNode titulos = media.path("title");
        String titulo = textoOuNull(titulos.path("romaji"));
        if (titulo == null) {
            titulo = textoOuNull(titulos.path("english"));
        }
        if (titulo == null) {
            return Optional.empty();
        }

        String posterUrl = textoOuNull(media.path("coverImage").path("extraLarge"));
        if (posterUrl == null) {
            posterUrl = textoOuNull(media.path("coverImage").path("large"));
        }
        Integer ano = media.path("seasonYear").isInt() ? media.path("seasonYear").asInt() : null;
        Integer episodios = media.path("episodes").isInt() ? media.path("episodes").asInt() : null;
        Double score = media.path("averageScore").isNumber()
            ? Math.round(media.path("averageScore").asDouble()) / 10.0
            : null;

        List<String> generos = new ArrayList<>();
        media.path("genres").forEach(genero -> {
            String valor = textoOuNull(genero);
            if (valor != null) {
                generos.add(valor);
            }
        });

        return Optional.of(new AnimeMetadata(
            titulo,
            textoOuNull(titulos.path("english")),
            textoOuNull(titulos.path("native")),
            posterUrl,
            ano,
            episodios,
            score,
            limparSinopse(textoOuNull(media.path("description"))),
            generos
        ));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: transforma a descrição da AniList em texto legível no
     * banner HTML cuja renderização escapa marcação externa por segurança.
     * <p>
     * INVARIANTES DO DOMÍNIO: quebras {@code br} viram linhas e as demais tags são
     * removidas; conteúdo textual é preservado.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: entrada nula permanece nula.
     */
    private String limparSinopse(String sinopse) {
        if (sinopse == null) {
            return null;
        }
        return sinopse
            .replaceAll("(?i)<br\\s*/?>", "\n")
            .replaceAll("<[^>]+>", "")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&amp;", "&")
            .replaceAll("\n{3,}", "\n\n")
            .trim();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: diferencia texto realmente informado de valores JSON
     * ausentes ao montar o modelo de anime.
     * <p>
     * INVARIANTES DO DOMÍNIO: nós nulos, ausentes e textos em branco viram nulo.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: nunca lança por nó nulo e devolve nulo.
     */
    private String textoOuNull(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        String valor = node.asText().trim();
        return valor.isEmpty() ? null : valor;
    }

    private record GraphQlRequest(String query, Map<String, String> variables) {}
}
