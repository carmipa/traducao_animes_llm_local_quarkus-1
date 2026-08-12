package org.traducao.projeto.legenda.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela o que os detectores REALMENTE decidem sobre os nomes de estilo
 * que o acervo tem hoje, separando o que já está coberto do que ainda escapa. Existe porque a
 * anotação de 2026-08-11 afirmava que três nomes escapavam dos três detectores, e a verificação
 * de 2026-08-12 mostrou que <b>um deles não escapa</b> — o padrão de romaji casa a forma curta
 * "roma" dentro de "Romaji". Anotação não confere com código; teste confere.
 *
 * <h2>O inventário que sustenta os casos</h2>
 * Medido em 2026-08-12 sobre {@code C:\animes}: 1.022 arquivos {@code .ass}, 2.346.132 linhas
 * {@code Dialogue}, 132 estilos distintos. Apenas <b>dois</b> estilos trazem tag de karaokê
 * {@code \k} no corpo — {@code Paradise} (864 falas) e {@code Dungeons} (2) —, e nenhum dos dois
 * tem vocabulário musical no nome. Eles não vazam para o LLM porque a tag no CONTEÚDO já é
 * indicador de música; é isso que este teste trava.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Tag {@code \k} no conteúdo basta: o nome do estilo é irrelevante quando o corpo se
 *       declara karaokê.</li>
 *   <li>Nome com "romaji" é reconhecido pelas duas vias — vocabulário musical e declaração de
 *       romaji.</li>
 *   <li>Nome próprio de canção SEM tag no corpo continua invisível. Está aqui documentado como
 *       lacuna CONHECIDA, não como comportamento desejado: fechá-la por nome exigiria lista
 *       nominal por obra, que é remendo; o mecanismo que a torna inofensiva é o pipeline não
 *       derrubar o episódio por uma fala ({@code ProcessarEpisodioUseCaseRecusaDoLlmTest}).</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Testes puros sobre os detectores; sem I/O, sem acervo em disco, sem rede.
 */
@DisplayName("estilos musicais do acervo: o que os detectores cobrem de fato")
class EstiloMusicalDoAcervoTest {

    private final DetectorEfeitoKaraokeService detector = new DetectorEfeitoKaraokeService();

    /** Karaokê cru como está no acervo: uma tag de timing por sílaba. */
    private static final String CORPO_COM_K = "{\\k21}Pa{\\k18}ra{\\k24}di{\\k30}se";
    /** Mesma letra depois do template: sem \k, uma transformação animada por letra. */
    private static final String CORPO_KFX = "{\\r\\pos(369,23)\\t(1160,1450,\\frx-50)}P";

    @Nested
    @DisplayName("tag no conteúdo vence o nome do estilo")
    class TagNoConteudo {

        @Test
        @DisplayName("Paradise e Dungeons: nome sem vocabulário musical, mas o corpo tem \\k")
        void estiloDeNomeProprioComTagDeKaraokeEhMusica() {
            for (String estilo : new String[] {"Paradise", "Dungeons"}) {
                assertFalse(detector.eEstiloDeMusica(estilo),
                    "o NOME de \"" + estilo + "\" não declara música — é o nome da canção");
                assertTrue(detector.podeSerCamadaMusical(estilo, CORPO_COM_K),
                    "a tag \\k no corpo de \"" + estilo + "\" é indicador suficiente: sem isso as 866 "
                        + "falas de karaokê do acervo iriam ao LLM");
            }
        }
    }

    @Nested
    @DisplayName("o que a anotação de 2026-08-11 dizia escapar, e o que escapa de fato")
    class CoberturaPorNome {

        @Test
        @DisplayName("\"Hey World Romaji\" NÃO escapa: o padrão casa a forma curta \"roma\"")
        void estiloComRomajiNoNomeEhCoberto() {
            assertTrue(detector.eEstiloDeRomaji("Hey World Romaji"),
                "\"romaji\" no nome é declaração explícita de camada original");
            assertTrue(detector.eEstiloDeMusica("Hey World Romaji"),
                "\"romaji\" também está entre as substrings musicais desde 2026-08-07");
            assertTrue(detector.podeSerCamadaMusical("Hey World Romaji", CORPO_KFX),
                "mesmo sem \\k no corpo, o nome basta — este caso já estava fechado");
        }

        @Test
        @DisplayName("lacuna conhecida: nome próprio de canção sem tag no corpo segue invisível")
        void nomeProprioSemTagNoCorpoSegueInvisivel() {
            for (String estilo : new String[] {"RISE LIGHT RISE English", "Logo", "Paradise"}) {
                assertFalse(detector.podeSerCamadaMusical(estilo, CORPO_KFX),
                    "lacuna DECLARADA: \"" + estilo + "\" sem \\k no corpo não é reconhecido. "
                        + "Se este teste ficar vermelho, o mecanismo melhorou — atualize o inventário "
                        + "e mova o caso para a classe de cobertura acima");
            }
        }

        @Test
        @DisplayName("contraprova: estilo de diálogo não pode ser capturado como música")
        void estiloDeDialogoNaoEhCapturado() {
            for (String estilo : new String[] {"Default", "Dialogue", "Char's Counterattack", "Mask", "Signs"}) {
                assertFalse(detector.podeSerCamadaMusical(estilo, "Bom dia, tenente."),
                    "estilo de diálogo \"" + estilo + "\" jamais pode ser tratado como música");
            }
        }
    }
}
