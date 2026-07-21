package org.traducao.projeto.traducao.contextocena;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.traducao.application.AvaliadorTraducaoCache;
import org.traducao.projeto.traducao.application.ClassificadorPendenciaTelemetria;
import org.traducao.projeto.traducao.application.IsoladorQuebraDialogo;
import org.traducao.projeto.traducao.application.contextocena.ChaveadorContextual;
import org.traducao.projeto.traducao.application.contextocena.MontadorJanelaContextual;
import org.traducao.projeto.traducao.application.contextocena.TradutorContextualEpisodio;
import org.traducao.projeto.traducao.domain.contextocena.TradutorContextualPort;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.traducao.infrastructure.contextocena.ContextoCenaProperties;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * PROPÓSITO DE NEGÓCIO: subfase 6c-ii-b — prova que o orquestrador contextual reusa as
 * máquinas por-fala SEM corromper a legenda: mascara a tag ASS antes de chamar o modelo e a
 * restaura depois (round-trip), reaproveita o cache por assinatura, e degrada para "manter
 * original" quando o modelo corrompe o marcador (alucinação). Usa porta e avaliador stub.
 *
 * <p>INVARIANTES DO DOMÍNIO: teste puro; a tag {@code {\i1}} sobrevive à tradução; o modelo
 * nunca vê a sintaxe de estilo; alucinação de marcador nunca vira legenda corrompida.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer corrupção de tag, colapso ou contagem errada reprova.
 */
class TradutorContextualEpisodioTest {

    private final MascaradorTags mascarador = new MascaradorTags();
    private final IsoladorQuebraDialogo isolador = new IsoladorQuebraDialogo();
    private final MontadorJanelaContextual montadorJanela =
        new MontadorJanelaContextual(new ClassificadorPendenciaTelemetria(new DetectorEfeitoKaraokeService()));
    private final ChaveadorContextual chaveador = new ChaveadorContextual();
    private final TradutorProperties propriedades = new TradutorProperties();

    /** Avaliador stub: válido quando a tradução difere do original; pendente se igual/branco. */
    private static AvaliadorTraducaoCache avaliadorStub() {
        return new AvaliadorTraducaoCache(null, null, null) {
            @Override
            public boolean isCacheReaproveitavel(String original, String traduzido) {
                return traduzido != null && !traduzido.isBlank();
            }
            @Override
            public String motivoFalhaFinal(String original, String traduzido) {
                if (traduzido == null || traduzido.isBlank()) {
                    return "resposta vazia";
                }
                return original.equals(traduzido) ? "modelo devolveu o texto original sem traducao" : null;
            }
        };
    }

    private TradutorContextualEpisodio comPorta(TradutorContextualPort porta) {
        return new TradutorContextualEpisodio(montadorJanela, chaveador, mascarador, isolador,
            porta, avaliadorStub(), new ContextoCenaProperties(true, 2), propriedades);
    }

    @Test
    @DisplayName("mascara: a tag ASS sobrevive ao round-trip (modelo nunca ve a sintaxe de estilo)")
    void tagSobreviveAoRoundTrip() {
        List<EventoLegenda> eventos = List.of(
            new EventoLegenda(0, "Dialogue", "Default", "Dialogue: ,", "{\\i1}Hello"));

        // O modelo recebe a fala MASCARADA e devolve a traducao preservando o marcador.
        TradutorContextualEpisodio orq = comPorta(req -> {
            String alvo = req.janela().alvo().texto();
            // garante que o modelo NAO viu a tag crua, so o marcador
            assertEquals("[[TAG0]]Hello", alvo, "o alvo deve chegar mascarado ao modelo");
            return "[[TAG0]]Olá";
        });

        TradutorContextualEpisodio.ResultadoContextual r =
            orq.traduzir(eventos, Set.of(0), "PROMPT", Map.of());

        assertEquals("{\\i1}Olá", r.traducaoPorIndice().get(0), "a tag deve ser restaurada na saida");
        assertEquals(1, r.resumo().falasContextualizadas());
        assertEquals(1, r.entradas().size());
        assertNotNull(r.entradas().get(0).assinaturaContexto(), "a entrada deve carregar a assinatura");
        assertEquals("{\\i1}Olá", r.entradas().get(0).traduzido());
    }

    @Test
    @DisplayName("cache: assinatura batendo reaproveita sem chamar o modelo")
    void reaproveitaCachePorAssinatura() {
        List<EventoLegenda> eventos = List.of(
            new EventoLegenda(0, "Dialogue", "Default", "Dialogue: ,", "Hi"));
        String assinatura = chaveador.assinatura(0, "Hi", "Default", List.of(),
            TradutorContextualEpisodio.POLITICA_VERSAO);

        TradutorContextualEpisodio orq = comPorta(req -> {
            throw new AssertionError("a porta NAO deveria ser chamada num cache hit");
        });

        TradutorContextualEpisodio.ResultadoContextual r =
            orq.traduzir(eventos, Set.of(0), "PROMPT", Map.of(assinatura, "Oi (cache)"));

        assertEquals("Oi (cache)", r.traducaoPorIndice().get(0));
        assertEquals(1, r.resumo().reaproveitadasCache());
        assertEquals(0, r.resumo().falasContextualizadas());
    }

    @Test
    @DisplayName("alucinacao: marcador corrompido pelo modelo mantem o original, nao corrompe a legenda")
    void alucinacaoDeMarcadorMantemOriginal() {
        List<EventoLegenda> eventos = List.of(
            new EventoLegenda(0, "Dialogue", "Default", "Dialogue: ,", "{\\i1}Hello"));

        // Modelo devolve SEM o marcador [[TAG0]] -> desmascarar falha -> mantem original.
        TradutorContextualEpisodio orq = comPorta(req -> "Olá");

        TradutorContextualEpisodio.ResultadoContextual r =
            orq.traduzir(eventos, Set.of(0), "PROMPT", Map.of());

        assertEquals("{\\i1}Hello", r.traducaoPorIndice().get(0), "deve manter o original intacto");
        assertEquals(1, r.resumo().pendentes());
        assertEquals("", r.entradas().get(0).traduzido(), "pendente grava tradução em branco no cache");
    }
}
