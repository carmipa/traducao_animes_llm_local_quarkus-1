package org.traducao.projeto.raspagemCorrecao.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.raspagemCorrecao.domain.ResultadoRaspagem;
import org.traducao.projeto.raspagemCorrecao.domain.StatusRaspagem;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: sela a ADJACÊNCIA da tag ASS neste scraper, o gêmeo do
 * {@code traducao.infrastructure.adapters.GoogleFallbackAdapter}.
 *
 * <p>Os dois adaptadores são deliberadamente separados — divergem no marcador residual e na
 * política de retry, e o Javadoc do scraper registra que unificá-los mudaria desfecho em falas
 * reais. Vale anotar o que a medição de 2026-07-28 mostrou: eles divergiram no que era
 * intencional e CONVERGIRAM no mesmo defeito de espaçamento, com a mesma linha
 * {@code replaceAll("(?i)\\s*\\[t" + i + "\\]\\s*", ...)} comendo o espaço em volta da tag em
 * ambos. Por isso o conserto — e este teste — existem duas vezes.
 *
 * <p>INVARIANTES DO DOMÍNIO: o espaço que existia em volta da tag no ORIGINAL volta; o que não
 * existia não aparece. Cada caso que exige espaço vem com o vizinho que exige a ausência dele,
 * porque tag no meio de palavra ({@code "Ou{ba}ma"}) precisa continuar colada.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem rede — transporte e espera são substituídos pelos seams
 * {@code executarGet} e {@code dormir}, como no {@link GoogleTranslateScraperTest}.
 */
class GoogleScraperAdjacenciaTagTest {

    /** Scraper com transporte fixo e sem espera real. */
    private static final class ScraperFalso extends GoogleTranslateScraper {
        private final String corpo;

        ScraperFalso(String corpo) {
            super(new ObjectMapper());
            this.corpo = corpo;
        }

        @Override
        protected RespostaHttp executarGet(String url) {
            return new RespostaHttp(200, corpo, 0);
        }

        @Override
        protected void dormir(long ms) {
            // não dorme
        }
    }

    private static String traduzir(String original, String respostaDoGoogle) {
        ResultadoRaspagem r = new ScraperFalso("[[[\"" + respostaDoGoogle + "\",\"orig\"]]]")
            .traduzir(original);
        assertEquals(StatusRaspagem.SUCESSO, r.status(), "a tradução deveria ser aceita");
        return r.texto();
    }

    @Test
    @DisplayName("tag cercada de espaço no original volta cercada de espaço")
    void tagComEspacoNoOriginalVoltaComEspaco() {
        String pt = traduzir(
            "She let {\\i1}you{\\i0} use the Void Genome.",
            "Ela deixou [T0] você [T1] usar o Void Genome.");

        assertEquals("Ela deixou {\\i1}você{\\i0} usar o Void Genome.", pt);
    }

    @Test
    @DisplayName("tag NO MEIO DA PALAVRA continua sem espaço — o caso que proíbe o conserto ingênuo")
    void tagColadaNoOriginalContinuaColada() {
        String pt = traduzir(
            "If you swear your allegiance to President Ou{ba}ma,",
            "Se você jurar lealdade ao presidente Ou [T0] ma,");

        assertEquals("Se você jurar lealdade ao presidente Ou{ba}ma,", pt);
    }

    @Test
    @DisplayName("tag no início e no fim da fala não ganha espaço inventado")
    void tagNasBordasNaoGanhaEspaco() {
        String pt = traduzir("{\\i1}You go!{\\i0}", "[T0] Você vai! [T1]");

        assertEquals("{\\i1}Você vai!{\\i0}", pt);
    }
}
