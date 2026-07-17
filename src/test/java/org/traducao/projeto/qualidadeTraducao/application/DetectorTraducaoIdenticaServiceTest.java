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
}
