package org.traducao.projeto.apiDadosAnime.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.apiDadosAnime.domain.model.AnimeMetadata;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: o cartão da tela afirma QUAL obra está sendo trabalhada. Afirmar a obra
 * errada é pior que não afirmar nada, porque o operador age sobre a afirmação.
 *
 * <h2>A cicatriz, vista na tela por Paulo em 22/08/2026</h2>
 * Ele apontou a pasta do <b>Zeta</b>, escolheu a lore do <b>Zeta</b>, e o cartão exibiu
 * <b>Mobile Suit Gundam ZZ</b> — outra obra, de outro ano, com outra sinopse. O provedor
 * devolvia o primeiro resultado da busca e ninguém conferia; pior, o erro era PERSISTIDO em
 * {@code cache/metadata/mobile_suit_zeta_gundam.json} e voltava idêntico a cada abertura.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Se esta guarda parar de recusar, o cartão volta a poder afirmar a obra errada — e a afirmar
 * de forma persistente, que é o que torna o defeito difícil de perceber.
 */
class GuardaIdentidadeMetadataTest {

    // O construtor faz mapper.copy(): um ObjectMapper de verdade, e nada mais. Os tres
    // adapters ficam nulos de proposito — esta guarda nao chama provedor nenhum.
    private final ObterMetadataAnimeUseCase useCase = new ObterMetadataAnimeUseCase(
        null, null, null, new com.fasterxml.jackson.databind.ObjectMapper());

    private static Optional<AnimeMetadata> obra(String titulo, String ingles, String japones) {
        return Optional.of(new AnimeMetadata(
            titulo, ingles, japones, null, 1985, 50, 8.0, "sinopse", List.of()));
    }

    /** O CASO REAL. Três das quatro palavras batem — e é a quarta que decide. */
    @Test
    @DisplayName("Zeta nao aceita ZZ: falta a palavra que distingue as obras")
    void zetaNaoAceitaZZ() {
        Optional<AnimeMetadata> r = useCase.aceitarSeForDaObra(
            "mobile_suit_zeta_gundam",
            obra("Mobile Suit Gundam ZZ", "Mobile Suit Gundam ZZ", "機動戦士ガンダムZZ"),
            "TMDB");

        assertTrue(r.isEmpty(),
            "\"zeta\" não aparece em nenhum título do ZZ: o cartão tem de ficar vazio");
    }

    /**
     * CONTRA-CASO obrigatório: a obra CERTA continua sendo aceita. Sem ele, "recusou" passaria
     * como se fosse a correção — e uma guarda que recusa tudo deixa a tela sem cartão nenhum.
     */
    @Test
    @DisplayName("Zeta aceita Zeta")
    void zetaAceitaZeta() {
        Optional<AnimeMetadata> r = useCase.aceitarSeForDaObra(
            "mobile_suit_zeta_gundam",
            obra("Mobile Suit Zeta Gundam", "Mobile Suit Zeta Gundam", "機動戦士Ζガンダム"),
            "TMDB");

        assertFalse(r.isEmpty(), "a obra certa não pode ser recusada");
    }

    /**
     * O ROMAJI é a razão de os TRÊS títulos entrarem na conferência. A AniList responde
     * {@code "Kidou Senshi Z Gundam"} para esta busca — nenhuma palavra do termo aparece ali.
     * Sem o título inglês junto, o resultado CERTO seria recusado.
     */
    @Test
    @DisplayName("romaji sozinho nao recusa: o titulo ingles conta junto")
    void romajiNaoRecusaQuandoOIinglesConfere() {
        Optional<AnimeMetadata> r = useCase.aceitarSeForDaObra(
            "mobile_suit_zeta_gundam",
            obra("Kidou Senshi Z Gundam", "Mobile Suit Zeta Gundam", "機動戦士Ζガンダム"),
            "AniList");

        assertFalse(r.isEmpty(),
            "o título romaji não bate, mas o inglês sim — e isso basta");
    }

    /**
     * O SEGUNDO envenenado do cache: uma busca por {@code "traducao"} — nome de PASTA, não de
     * obra — trouxe o filme "Amor em Tradução", de 2023.
     */
    @Test
    @DisplayName("nome de pasta nao vira obra")
    void nomeDePastaNaoViraObra() {
        Optional<AnimeMetadata> r = useCase.aceitarSeForDaObra(
            "traducao",
            obra("Amor em Tradução", "Lost in Translation", "恋するトランスレーター"),
            "TMDB");

        assertTrue(r.isEmpty(), "\"traducao\" não aparece nos títulos: não é essa obra");
    }

    /**
     * As siglas curtas ({@code ZZ}, {@code NT}, {@code F91}) ficam FORA da conferência de
     * propósito: sozinhas não bastam para afirmar identidade, e exigi-las recusaria o resultado
     * certo quando a API abrevia diferente.
     */
    @Test
    @DisplayName("sigla curta nao e exigida")
    void siglaCurtaNaoEExigida() {
        Optional<AnimeMetadata> r = useCase.aceitarSeForDaObra(
            "gundam_narrative_nt",
            obra("Mobile Suit Gundam Narrative", "Mobile Suit Gundam Narrative", "機動戦士ガンダムNT"),
            "TMDB");

        assertFalse(r.isEmpty(), "\"nt\" tem 2 letras e não entra na exigência");
    }

    /** Ausência continua sendo ausência: a guarda não inventa resultado. */
    @Test
    @DisplayName("vazio continua vazio")
    void vazioContinuaVazio() {
        assertTrue(useCase.aceitarSeForDaObra("qualquer", Optional.empty(), "TMDB").isEmpty());
        assertTrue(useCase.aceitarSeForDaObra("qualquer", null, "TMDB").isEmpty());
    }
}
