package org.traducao.projeto.arquitetura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.telemetria.FatiaTelemetria;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela a cobertura do mapa que decide em que aba do
 * painel — e em que dataset publicado — cada operação aparece.
 *
 * <h2>O prejuízo que originou</h2>
 * Medido no acervo em 06/08/2026: das <b>6.601 operações registradas, 6.488
 * estavam presas em 21 pastas de relatório</b> que o publicador nunca varreu, e
 * apenas 85 chegaram ao repositório público — 1,3%. Agrupar por fatia é o que
 * torna o resto publicável de forma útil; um tipo que perde o mapeamento volta a
 * ser invisível, só que agora dentro de {@code outros}.
 *
 * <h2>Por que lista nominal, e não varredura de fonte</h2>
 * Tentado primeiro e descartado por medição: dos tipos usados no código, um é
 * montado por concatenação ({@code "Extracao de Legendas (" + formato + ")"}),
 * dois vêm de constante e pelo menos um chega como valor de runtime
 * ({@code resumo.tipo()} no adaptador de correção de legendas). Varredura de
 * fonte seria cega exatamente onde precisaria enxergar — e guarda cega aprova
 * por não ver nada.
 *
 * <p>Esta catraca protege o que dá para provar: os tipos que EXISTEM no acervo
 * continuam mapeados. Ela não detecta tipo novo surgindo em runtime — para isso
 * existe o fluxo {@code outros}, que é visível no painel de propósito.
 */
class CatracaCoberturaFatiaTelemetriaTest {

    /**
     * Os 22 tipos medidos no acervo em 06/08/2026, com a contagem de ocorrências.
     * Não é lista de desejos: é inventário do que foi encontrado em
     * {@code relatorios/} e {@code logs/}.
     */
    private static final Map<String, String> TIPOS_DO_ACERVO = Map.ofEntries(
        Map.entry("Auditoria de Conteudo de Legendas", "auditoria"),   // 2134
        Map.entry("Auditoria de Conteudo (.ass)", "auditoria"),        //  796
        Map.entry("Limpeza de Cache", "cache"),                        //  785
        Map.entry("Correção Google (cache)", "cache"),                 //  654
        Map.entry("Revisão Legendas (.ass Google)", "revisao"),        //  342
        Map.entry("Extracao de Legendas (ASS)", "extracao"),           //  324
        Map.entry("Revisão Concordância (.ass LLM)", "revisao"),       //  215
        Map.entry("Renomear Arquivos", "arquivos"),                    //  180
        Map.entry("Revisão Gramatical (cache LLM)", "cache"),          //  171
        Map.entry("Reforço de Terminologia (ensaio)", "terminologia"), //  157
        Map.entry("Remux (mkvmerge)", "extracao"),                     //  140
        Map.entry("NOVO_KARAOKE", "karaoke"),                          //  113
        Map.entry("Revisao de Lore (.ass LLM)", "revisao"),            //  103
        Map.entry("Tradução de Karaokê (LLM)", "karaoke"),             //   98
        Map.entry("Revisão de Concordância", "revisao"),               //   89
        Map.entry("Troca de Fontes ASS", "legenda"),                   //   86
        Map.entry("Achatar Estilos Decorativos", "legenda"),           //   66
        Map.entry("Revisão de Lore PT-only", "revisao"),               //   60
        Map.entry("Correcao de Legendas (.ass original->traduzida)", "legenda"), // 33
        Map.entry("Analise de Midia", "extracao"),                     //   32
        Map.entry("Reforço de Terminologia", "terminologia"),          //   20
        Map.entry("Karaokê Simples", "karaoke")                        //    3
    );

    /**
     * PROPÓSITO DE NEGÓCIO: nenhum tipo do acervo pode cair em {@code outros}.
     *
     * <p>Cair em {@code outros} não perde o dado — o fluxo recebe igual —, mas
     * tira o registro da aba onde ele deveria aparecer e do dataset que ele
     * deveria alimentar. Silencioso, e é isso que a catraca impede.
     */
    @Test
    @DisplayName("todo tipo medido no acervo tem fatia — nenhum cai em outros")
    void nenhumTipoDoAcervoCaiEmOutros() {
        List<String> semFatia = new ArrayList<>();
        for (String tipo : TIPOS_DO_ACERVO.keySet()) {
            if (FatiaTelemetria.OUTROS.equals(FatiaTelemetria.de(tipo))) {
                semFatia.add(tipo);
            }
        }
        assertTrue(semFatia.isEmpty(),
            "tipos do acervo sem fatia mapeada (sumiriam da aba e do dataset): " + semFatia);
    }

    /** A fatia tem de ser a que foi decidida, não apenas alguma. */
    @Test
    @DisplayName("cada tipo cai na fatia decidida, nao em qualquer uma")
    void cadaTipoNaFatiaDecidida() {
        List<String> divergentes = new ArrayList<>();
        TIPOS_DO_ACERVO.forEach((tipo, esperada) -> {
            String obtida = FatiaTelemetria.de(tipo);
            if (!esperada.equals(obtida)) {
                divergentes.add(tipo + ": esperado " + esperada + ", obtido " + obtida);
            }
        });
        assertTrue(divergentes.isEmpty(), String.join("\n", divergentes));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: as oito fatias com volume real precisam existir. Uma
     * fatia que some do mapa leva junto a aba e o dataset dela.
     */
    @Test
    @DisplayName("as oito fatias de volume real seguem no inventario")
    void asOitoFatiasSeguemNoInventario() {
        Set<String> presentes = new TreeSet<>(FatiaTelemetria.inventario().values());

        assertEquals(
            Set.of("arquivos", "auditoria", "cache", "extracao", "karaoke", "legenda",
                "revisao", "terminologia"),
            presentes,
            "o conjunto de fatias mudou; abas e datasets dependem dele");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CONTRA-TESTE. Sem ele, "todo tipo tem fatia" poderia
     * ser satisfeito devolvendo uma fatia fixa para qualquer entrada, e as duas
     * asserções acima passariam num mapa que não mapeia nada.
     */
    @Test
    @DisplayName("tipo inexistente REALMENTE cai em outros")
    void tipoInexistenteCaiEmOutros() {
        assertEquals(FatiaTelemetria.OUTROS,
            FatiaTelemetria.de("Operacao Inventada Que Nunca Existiu No Acervo"));
    }
}
