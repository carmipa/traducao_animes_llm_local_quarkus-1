package org.traducao.projeto.traducao.infrastructure.adapters;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: sela a ADJACÊNCIA da tag ASS no round-trip pelo Google — o espaço que
 * existia em volta da tag no original tem que voltar, e o que NÃO existia não pode aparecer.
 *
 * <h2>O defeito medido</h2>
 * O round-trip perdia informação nas duas pontas: ao mascarar, o marcador saía sempre com espaço
 * dos dois lados e {@code replaceAll("\\s+", " ")} colapsava o resto; ao restaurar,
 * {@code \s*\[Tn\]\s*} comia todo espaço em volta. Resultado gravado no cache:
 * <pre>
 *   EN "She let {\i1}you{\i0} use the Void Genome."
 *   PT "Ela deixou{\i1}você{\i0}usar o Void Genome."   -> renderiza "Ela deixouvocêusar"
 * </pre>
 * Medição de 2026-07-28 sobre 22.854 falas de dois acervos: das falas com tag NO MEIO da frase,
 * ~70% saíram coladas (Guilty Crown 72 de 110; Zeta 15 de 20). Os totais diferem só porque
 * Guilty Crown usa ênfase no meio da fala cinco vezes mais.
 *
 * <h2>Invariantes do domínio</h2>
 * O conserto NÃO é "devolver o espaço": em {@code "President Ou{ba}ma"} a tag está no meio da
 * palavra e precisa continuar colada — ali o comportamento antigo acertava. Só a adjacência do
 * ORIGINAL separa os dois casos, e por isso cada teste que exige espaço vem com o vizinho que
 * exige a ausência dele.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem rede — o transporte HTTP é substituído pelo seam
 * {@code executarGet}, como no {@link GoogleFallbackAdapterTest}.
 */
class GoogleFallbackAdjacenciaTagTest {

    /** Adapter com transporte HTTP fixo — devolve o corpo canado, sem rede. */
    private static final class AdapterFake extends GoogleFallbackAdapter {
        private final String corpo;

        AdapterFake(String corpo) {
            super(new ObjectMapper());
            this.corpo = corpo;
        }

        @Override
        protected RespostaHttp executarGet(String url) {
            return new RespostaHttp(200, corpo);
        }
    }

    /** Monta o JSON no formato do endpoint translate_a/single para um único segmento. */
    private static String jsonGoogle(String traducao) {
        return "[[[\"" + traducao + "\",\"orig\",null,null,10]],null,\"en\"]";
    }

    private static String traduzir(String original, String respostaDoGoogle) {
        Optional<String> r = new AdapterFake(jsonGoogle(respostaDoGoogle))
            .traduzir(original).traducaoOpcional();
        assertTrue(r.isPresent(), "a tradução deveria ser aceita");
        return r.get();
    }

    @Test
    @DisplayName("tag cercada de espaço no original volta cercada de espaço")
    void tagComEspacoNoOriginalVoltaComEspaco() {
        // A fala real gravada com defeito no cache de Guilty Crown (02_Track4, evento 81).
        String pt = traduzir(
            "She let {\\i1}you{\\i0} use the Void Genome.",
            "Ela deixou [T0] você [T1] usar o Void Genome.");

        assertEquals("Ela deixou {\\i1}você{\\i0} usar o Void Genome.", pt);
    }

    @Test
    @DisplayName("tag NO MEIO DA PALAVRA continua sem espaço — o caso que proíbe o conserto ingênuo")
    void tagColadaNoOriginalContinuaColada() {
        // Guilty Crown 16_Track4, evento 60: "Ou{ba}ma" é o sobrenome partido por uma tag.
        // Se o conserto fosse "sempre devolver espaço", esta fala viraria "Ou {ba} ma".
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

    @Test
    @DisplayName("ênfase no meio da frase — a forma mais comum do defeito")
    void enfaseNoMeioDaFrase() {
        // Guilty Crown 17_Track4, evento 264: gravado como "O{\i1}rei{\i0}sempre fui eu."
        String pt = traduzir(
            "The {\\i1}king{\\i0} has always been me.",
            "O [T0] rei [T1] sempre fui eu.");

        assertEquals("O {\\i1}rei{\\i0} sempre fui eu.", pt);
    }
}
