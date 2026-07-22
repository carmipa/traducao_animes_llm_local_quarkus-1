package org.traducao.projeto.qualidadeTraducao.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.qualidadeTraducao.domain.LoreAtivaPort;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: impede que nomes próprios legítimos sejam enviados ao
 * revisor apenas porque são idênticos no inglês e no PT-BR.
 * <p>INVARIANTES DO DOMÍNIO: hesitação e pontuação não descaracterizam nomes;
 * palavras conversacionais inglesas continuam pendentes.
 * <p>COMPORTAMENTO EM CASO DE FALHA: falso nome ou falso inglês reprova o teste.
 */
class DetectorTraducaoIdenticaServiceTest {

    /**
     * PROPÓSITO DE NEGÓCIO: fake de {@link LoreAtivaPort} fiel ao estado que o detector
     * enxergava antes da E8c.1 com {@code new GerenciadorContexto(List.of())} — nenhum
     * provedor de contexto, logo nenhum termo protegido e a lore neutra do prompt padrão.
     * <p>INVARIANTES DO DOMÍNIO: reproduz exatamente os retornos daquele cenário para que
     * a inversão de dependência não altere o comportamento observado nestes casos.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; retornos fixos e determinísticos.
     */
    private static final class LoreAtivaVazia implements LoreAtivaPort {
        @Override
        public Set<String> termosProtegidosAtivos() {
            return Set.of();
        }

        @Override
        public String obterLoreAtiva() {
            return "Voce e um tradutor especialista. Traduza fielmente.";
        }
    }

    private final DetectorTraducaoIdenticaService detector =
        new DetectorTraducaoIdenticaService(new LoreAtivaVazia());

    /**
     * PROPÓSITO DE NEGÓCIO: cobre os nomes que o Nemo recebeu indevidamente na execução real.
     * <p>INVARIANTES DO DOMÍNIO: uma a quatro palavras capitalizadas são preservadas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer nome não reconhecido reprova o teste.
     */
    @Test
    void preservaNomesSimplesGaguejadosESequenciais() {
        assertTrue(detector.deveManterIdentico("Maria?"));
        assertTrue(detector.deveManterIdentico("E-Eledore..."));
        assertTrue(detector.deveManterIdentico("Rob... Sally... Mike..."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mantém saudações inglesas na fila de tradução.
     * <p>INVARIANTES DO DOMÍNIO: capitalização isolada não protege vocabulário comum.
     * <p>COMPORTAMENTO EM CASO DE FALHA: classificação como nome reprova o teste.
     */
    @Test
    void naoConfundePalavraInglesaComNome() {
        assertFalse(detector.deveManterIdentico("Hello!"));
    }

    /** Lore com a terminologia UC de Zeon, como as obras da Guerra de Um Ano declaram. */
    private static final class LoreZeon implements LoreAtivaPort {
        @Override
        public Set<String> termosProtegidosAtivos() {
            return Set.of("Sieg Zeon", "Zeon", "Principality of Zeon", "Earth Federation",
                "Minovsky", "Gundam");
        }

        @Override
        public String obterLoreAtiva() {
            return "Universal Century; Principality of Zeon vs Earth Federation.";
        }
    }

    private final DetectorTraducaoIdenticaService detectorZeon =
        new DetectorTraducaoIdenticaService(new LoreZeon());

    /**
     * PROPÓSITO DE NEGÓCIO: o brado do Principado de Zeon é canônico em toda a linha do
     * tempo UC e deve permanecer no original — traduzi-lo para "Salve Zeon!" perde o
     * paralelo histórico deliberado da obra. Caso REAL: na corrida de 2026-07-22 a fala
     * "Sieg Zeon! Sieg Zeon! Sieg Zeon! Sieg Zeon!" virou pendência por "modelo devolveu o
     * texto original", porque o reconhecimento exigia o texto inteiro igual ao termo e não
     * enxergava a repetição.
     * <p>INVARIANTES DO DOMÍNIO: repetição do termo canônico continua canônica.
     * <p>COMPORTAMENTO EM CASO DE FALHA: se voltar a reprovar, o brado vira pendência a
     * cada execução de qualquer obra UC.
     */
    @Test
    void bradoCanonicoRepetidoPermaneceNoOriginal() {
        assertTrue(detectorZeon.deveManterIdentico("Sieg Zeon! Sieg Zeon! Sieg Zeon! Sieg Zeon!"));
        assertTrue(detectorZeon.deveManterIdentico("{\\i1}Sieg Zeon! Sieg Zeon!{\\i0}"));
        assertTrue(detectorZeon.deveManterIdentico("Sieg Zeon!"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante que a regra nova NÃO virou porta dos fundos — basta
     * sobrar uma palavra traduzível para a fala continuar exigindo tradução.
     * <p>INVARIANTES DO DOMÍNIO: só texto INTEIRAMENTE composto de termos declarados passa.
     * <p>COMPORTAMENTO EM CASO DE FALHA: aceitar frase inglesa como tradução.
     */
    @Test
    void fraseComPalavraTraduzivelContinuaPendente() {
        assertFalse(detectorZeon.deveManterIdentico("Zeon attacks the colony"),
            "sobrou \"attacks the colony\": a fala não foi traduzida");
        assertFalse(detectorZeon.deveManterIdentico("The Gundam is here"),
            "sobrou \"is here\": a fala não foi traduzida");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: sem obra selecionada, a regra nova não pode inventar proteção.
     * <p>INVARIANTES DO DOMÍNIO: lore vazia degrada para as heurísticas globais.
     * <p>COMPORTAMENTO EM CASO DE FALHA: aceitação cega sem terminologia declarada.
     */
    @Test
    void semLoreAtivaOBradoNaoEhProtegidoPelaRegraNova() {
        assertFalse(detector.deveManterIdentico("Sieg Zeon! Sieg Zeon! Sieg Zeon! Sieg Zeon!"),
            "sem termos declarados, nada autoriza manter a fala em inglês");
    }
}
