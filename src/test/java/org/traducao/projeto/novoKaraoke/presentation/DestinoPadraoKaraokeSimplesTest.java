package org.traducao.projeto.novoKaraoke.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: o operador não deveria ter de digitar (nem lembrar) o caminho da pasta de
 * saída da simplificação de karaokê. O acervo segue uma convenção: uma pasta por etapa, TODAS
 * irmãs dentro da pasta da obra — {@code legendas_extraidas_ass}, {@code traducao_ptbr}. A saída
 * padrão entra nesse mesmo nível como {@code legenda-simplificada}.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Caminho informado pelo operador VENCE sempre e é usado como está — o padrão é
 *       conveniência, não política.</li>
 *   <li>O padrão é IRMÃO da entrada, nunca filho: gravar dentro da pasta de entrada faria a
 *       próxima execução varrer a própria saída.</li>
 *   <li>Entrada em raiz (sem pai) resolve dentro dela mesma — degradação segura, porque não há
 *       nível acima para onde ir.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * A função é pura: não cria pasta nem toca disco. Quem cria é o use case, na gravação — em
 * simulação nada é criado.
 */
class DestinoPadraoKaraokeSimplesTest {

    @Test
    @DisplayName("destino vazio cria 'legenda-simplificada' IRMA da pasta de entrada")
    void destinoVazioResolveIrmaoDaEntrada() {
        Path entrada = Paths.get("C:", "animes", "Guilty Crown", "traducao_ptbr");

        Path destino = NovoKaraokeController.resolverDestino(entrada, null);

        assertEquals(Paths.get("C:", "animes", "Guilty Crown", "legenda-simplificada"), destino);
        assertEquals(entrada.getParent(), destino.getParent(),
            "a saida tem de ficar no MESMO nivel das outras pastas de legenda da obra");
    }

    @Test
    @DisplayName("string em branco conta como vazio")
    void brancoContaComoVazio() {
        Path entrada = Paths.get("C:", "animes", "Guilty Crown", "traducao_ptbr");

        assertEquals(NovoKaraokeController.resolverDestino(entrada, null),
            NovoKaraokeController.resolverDestino(entrada, "   "));
    }

    @Test
    @DisplayName("caminho informado pelo operador vence o padrao")
    void informadoVenceOPadrao() {
        Path entrada = Paths.get("C:", "animes", "Guilty Crown", "traducao_ptbr");
        String escolhido = Paths.get("D:", "saida", "minha-pasta").toString();

        assertEquals(Paths.get(escolhido),
            NovoKaraokeController.resolverDestino(entrada, escolhido));
    }

    @Test
    @DisplayName("o padrao NUNCA fica dentro da entrada — senao a proxima execucao varre a saida")
    void padraoNaoEhFilhoDaEntrada() {
        Path entrada = Paths.get("C:", "animes", "Guilty Crown", "traducao_ptbr");

        Path destino = NovoKaraokeController.resolverDestino(entrada, null);

        assertTrue(!destino.startsWith(entrada.toAbsolutePath().normalize()),
            "destino dentro da entrada faria a proxima execucao ler os proprios arquivos: " + destino);
    }

    @Test
    @DisplayName("entrada em raiz degrada para dentro dela mesma, sem lancar")
    void entradaEmRaizNaoQuebra() {
        Path raiz = Paths.get("C:").toAbsolutePath().getRoot();

        Path destino = NovoKaraokeController.resolverDestino(raiz, null);

        assertTrue(destino.toString().contains("legenda-simplificada"), destino::toString);
    }
}
