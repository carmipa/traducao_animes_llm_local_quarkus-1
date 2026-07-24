package org.traducao.projeto.apiDadosAnime.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.apiDadosAnime.domain.model.AnimeMetadata;
import org.traducao.projeto.apiDadosAnime.infrastructure.adapters.AniListApiClientAdapter;
import org.traducao.projeto.apiDadosAnime.infrastructure.config.ApiDadosAnimeHttpProperties;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObterMetadataAnimeUseCaseTest {

    private final ObterMetadataAnimeUseCase useCase = new ObterMetadataAnimeUseCase(
        null,
        null,
        null,
        new ObjectMapper()
    );

    @Test
    void removeSufixoRevisaoDeLoreDoTermoDeBusca() {
        assertEquals("86 Eighty Six", useCase.extrairNomeTermoBusca("86 (Eighty-Six) - Revisao de Lore"));
        assertEquals("DanMachi", useCase.extrairNomeTermoBusca("DanMachi S5 - Revisao de Lore"));
        assertEquals(
            "Mobile Suit Gundam: The 08th MS Team",
            useCase.extrairNomeTermoBusca("Mobile Suit Gundam: The 08th MS Team - Revisao de Lore")
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante que o contexto Gundam Narrative mantenha o
     * alias usado pelas APIs de capa tanto na tradução quanto na revisão de lore.
     * <p>
     * INVARIANTES DO DOMÍNIO: parênteses são apenas delimitadores; a palavra
     * {@code Narrative} nunca pode ser descartada do termo de busca.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: qualquer regressão reprova a suíte com a
     * diferença entre o termo esperado e o termo sanitizado.
     */
    /**
     * PROPÓSITO DE NEGÓCIO: prefixo de CATÁLOGO do operador não é título de obra. As pastas são
     * organizadas por linha do tempo ({@code UC 0079 - ...}), mas a AniList/Jikan/TMDB não conhecem
     * esse prefixo — mandá-lo na busca devolve HTTP 404 e a capa não aparece.
     *
     * <p>CASO REAL (2026-07-23 07:25): a pasta {@code UC 0079 - Mobile Suit Gundam: The 08th MS
     * Team} virava a busca {@code "UC 0079 Mobile Suit Gundam: The 08th MS Team"} e a AniList
     * respondia 404, enquanto o MESMO anime já tinha metadata em cache sob a chave
     * {@code mobile_suit_gundam_the_08th_ms_team}. O filtro de ano não pegava o {@code 0079}
     * porque só remove {@code 19xx}/{@code 20xx}. O defeito ficou anos escondido atrás do cache:
     * só apareceu quando a pasta foi renomeada e a chave de cache mudou junto.
     *
     * <h2>Invariantes do domínio</h2>
     * <ul>
     *   <li>Só sai prefixo com a forma sigla curta + número + hífen NO COMEÇO.</li>
     *   <li>Número que PERTENCE ao título sobrevive: {@code Mobile Suit Gundam 0083 - Stardust
     *       Memory} e {@code 0080 - War in the Pocket} são obras reais com metadata em cache.</li>
     * </ul>
     *
     * <h2>Comportamento em caso de falha</h2>
     * Remover demais quebra a busca das obras cujo número é parte do nome; remover de menos
     * mantém a capa sumida em toda pasta organizada por linha do tempo.
     */
    @Test
    void removePrefixoDeCatalogoSemDestruirNumeroQuePertenceAoTitulo() {
        assertEquals("Mobile Suit Gundam: The 08th MS Team",
            useCase.extrairNomeTermoBusca("UC 0079 - Mobile Suit Gundam: The 08th MS Team"),
            "o prefixo de linha do tempo nao pode ir para a API");
        assertEquals("Mobile Suit Gundam: The 08th MS Team",
            useCase.extrairNomeTermoBusca("G:/animes/UC 0079 - Mobile Suit Gundam: The 08th MS Team"),
            "mesmo caso vindo como caminho completo");

        // O numero E o titulo: nao pode ser confundido com prefixo de catalogo.
        assertTrue(useCase.extrairNomeTermoBusca("Mobile Suit Gundam 0083 - Stardust Memory").contains("0083"),
            "0083 pertence ao titulo da obra");
        assertTrue(useCase.extrairNomeTermoBusca("86 - Eighty Six").contains("86"),
            "86 e o proprio nome da obra");
    }

    @Test
    void preservaAliasNarrativeEntreParentesesParaBuscaDeCapa() {
        assertEquals(
            "Mobile Suit Gundam NT Narrative",
            useCase.extrairNomeTermoBusca("Mobile Suit Gundam NT (Narrative)")
        );
        assertEquals(
            "Mobile Suit Gundam NT Narrative",
            useCase.extrairNomeTermoBusca("Mobile Suit Gundam NT (Narrative) - Revisao de Lore")
        );
    }

    @Test
    void mantemLimpezaPadraoDeCaminhoDeArquivo() {
        assertEquals(
            "Mobile Suit Gundam The 08th MS Team",
            useCase.extrairNomeTermoBusca("C:\\animes\\[Joseki] Mobile Suit Gundam The 08th MS Team COMPLETE (1996)(BD AV1 1080p)\\Mobile.Suit.Gundam.The.08th.MS.Team.S01E02_Track3_PT-BR.ass")
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante que dois formulários pedindo simultaneamente
     * a mesma capa compartilhem uma única consulta externa.
     *
     * <p>INVARIANTES DO DOMÍNIO: ambos recebem o mesmo resultado vazio e o adapter
     * é chamado exatamente uma vez.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: timeout curto reprova o teste em vez de
     * deixá-lo bloqueado.
     */
    @Test
    void consolidaBuscasConcorrentesDaMesmaObra() throws Exception {
        AniListContador adapter = new AniListContador();
        ObterMetadataAnimeUseCase concorrente = new ObterMetadataAnimeUseCase(
            null, adapter, null, new ObjectMapper());
        String termo = "Obra Inexistente Concorrente 987654321";

        CompletableFuture<Optional<AnimeMetadata>> primeira = CompletableFuture.supplyAsync(
            () -> concorrente.executar(termo));
        assertTrue(adapter.iniciada.await(2, TimeUnit.SECONDS));
        CompletableFuture<Optional<AnimeMetadata>> segunda = CompletableFuture.supplyAsync(
            () -> concorrente.executar(termo));
        adapter.liberar.countDown();

        assertTrue(primeira.get(2, TimeUnit.SECONDS).isEmpty());
        assertTrue(segunda.get(2, TimeUnit.SECONDS).isEmpty());
        assertEquals(1, adapter.chamadas.get());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: simula deterministicamente uma fonte lenta e sem
     * resultado para exercitar a consolidação concorrente.
     *
     * <p>INVARIANTES DO DOMÍNIO: nenhuma rede real é acessada; os latches controlam
     * quando consumidores concorrentes encontram a busca em andamento.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: interrupção restaura o sinal da thread e
     * devolve vazio.
     */
    private static final class AniListContador extends AniListApiClientAdapter {
        private final AtomicInteger chamadas = new AtomicInteger();
        private final CountDownLatch iniciada = new CountDownLatch(1);
        private final CountDownLatch liberar = new CountDownLatch(1);

        /**
         * PROPÓSITO DE NEGÓCIO: prepara o adapter falso sem comunicação externa.
         * <p>INVARIANTES DO DOMÍNIO: propriedades e mapper existem apenas em memória.
         * <p>COMPORTAMENTO EM CASO DE FALHA: erro de construção reprova o teste.
         */
        AniListContador() {
            super(new ApiDadosAnimeHttpProperties(), new ObjectMapper());
        }

        /**
         * PROPÓSITO DE NEGÓCIO: bloqueia a fonte falsa até a segunda requisição
         * entrar no caso de uso.
         * <p>INVARIANTES DO DOMÍNIO: contador cresce uma vez por chamada real.
         * <p>COMPORTAMENTO EM CASO DE FALHA: interrupção devolve ausência segura.
         */
        @Override
        public Optional<AnimeMetadata> buscarPorNome(String termoBusca) {
            chamadas.incrementAndGet();
            iniciada.countDown();
            try {
                liberar.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }
}
