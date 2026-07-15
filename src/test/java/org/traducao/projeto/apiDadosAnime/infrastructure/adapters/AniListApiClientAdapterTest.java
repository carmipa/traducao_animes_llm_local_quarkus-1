package org.traducao.projeto.apiDadosAnime.infrastructure.adapters;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.apiDadosAnime.domain.model.AnimeMetadata;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: protege o contrato entre a resposta pública da AniList
 * e o banner de capa exibido em todos os formulários do KRONOS.
 * <p>
 * INVARIANTES DO DOMÍNIO: título, capa, escala da nota, episódios e descrição
 * limpa devem permanecer compatíveis com {@link AnimeMetadata}.
 * <p>
 * COMPORTAMENTO EM CASO DE FALHA: qualquer mudança incompatível no mapeamento
 * reprova a suíte antes de produzir banners vazios em execução.
 */
class AniListApiClientAdapterTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final AniListApiClientAdapter adapter = new AniListApiClientAdapter(
        (org.traducao.projeto.traducao.infrastructure.http.JsonHttpClient) null, mapper);

    /**
     * PROPÓSITO DE NEGÓCIO: confirma que uma obra encontrada produz os dados e a
     * capa esperados pela interface.
     * <p>
     * INVARIANTES DO DOMÍNIO: nota 83 vira 8.3 e tags HTML não vazam na sinopse.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: o teste falha com divergência explícita.
     */
    @Test
    void mapeiaRespostaGraphQlParaMetadataComCapa() throws Exception {
        String json = """
            {"data":{"Media":{
              "title":{"romaji":"86: Eighty Six","english":"86 EIGHTY-SIX","native":"86－エイティシックス－"},
              "coverImage":{"extraLarge":"https://s4.anilist.co/capa.jpg","large":"https://s4.anilist.co/capa-media.jpg"},
              "seasonYear":2021,"episodes":11,"averageScore":83,
              "description":"Primeira linha.<br><br>Segunda linha.",
              "genres":["Action","Drama","Mecha"]
            }}}
            """;

        Optional<AnimeMetadata> resultado = adapter.mapearResposta(mapper.readTree(json));

        assertTrue(resultado.isPresent());
        AnimeMetadata metadata = resultado.orElseThrow();
        assertEquals("86: Eighty Six", metadata.titulo());
        assertEquals("https://s4.anilist.co/capa.jpg", metadata.posterUrl());
        assertEquals(2021, metadata.ano());
        assertEquals(11, metadata.episodios());
        assertEquals(8.3, metadata.score());
        assertEquals("Primeira linha.\n\nSegunda linha.", metadata.sinopse());
        assertEquals(3, metadata.generos().size());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: impede que respostas GraphQL sem mídia sejam tratadas
     * como capas válidas.
     * <p>
     * INVARIANTES DO DOMÍNIO: {@code data.Media=null} representa busca sem resultado.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: o teste exige retorno vazio, sem exceção.
     */
    @Test
    void devolveVazioQuandoAniListNaoEncontraMidia() throws Exception {
        Optional<AnimeMetadata> resultado = adapter.mapearResposta(
            mapper.readTree("{\"data\":{\"Media\":null}}"));

        assertTrue(resultado.isEmpty());
    }
}
