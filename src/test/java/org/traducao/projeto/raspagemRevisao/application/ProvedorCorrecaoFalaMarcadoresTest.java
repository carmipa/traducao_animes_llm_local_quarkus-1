package org.traducao.projeto.raspagemRevisao.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.llm.domain.LlmPort;
import org.traducao.projeto.llm.domain.Lote;
import org.traducao.projeto.llm.domain.StatusLlm;
import org.traducao.projeto.llm.domain.TraducaoLote;
import org.traducao.projeto.qualidadeTraducao.application.IsoladorQuebraDialogo;
import org.traducao.projeto.qualidadeTraducao.application.LoreAtivaFake;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.qualidadeTraducao.domain.LoreAtivaPort;
import org.traducao.projeto.raspagemCorrecao.application.ProtetorTermosLoreService;
import org.traducao.projeto.raspagemRevisao.domain.ContextoRevisao;
import org.traducao.projeto.raspagemRevisao.domain.ModoRevisaoLegendas;
import org.traducao.projeto.raspagemRevisao.domain.PoliticaRetraducao;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** Regressões do contrato assimétrico: o inglês referencia; somente o PT define a estrutura. */
class ProvedorCorrecaoFalaMarcadoresTest {

    private static final ContextoRevisao SEM_LORE =
        new ContextoRevisao("teste", "", Set.of());
    private static final ContextoRevisao LORE_ZETA =
        new ContextoRevisao("gundam_zeta", "", Set.of("Argama", "Mont Blanc"));
    private static final LoreAtivaPort LORE_VAZIA = new LoreAtivaPort() {
        @Override
        public Set<String> termosProtegidosAtivos() {
            return Set.of();
        }

        @Override
        public String obterLoreAtiva() {
            return "";
        }
    };

    @Test
    void removeTagsDoOriginalSemInventarMarcadorNaTraducaoSemTags() {
        LlmCapturador llm = executar(
            "{\\blur2\\bord0.5\\fad(200,200)\\c&HFFFFFF&\\1a&HFF&}"
                + "It has the color of vividly blazing flames...",
            "It has the color of vividly blazing flames...");

        assertEquals("It has the color of vividly blazing flames...", llm.originalRecebido);
        assertEquals("It has the color of vividly blazing flames...", llm.traducaoRecebida);
    }

    /**
     * ESTE TESTE VIROU O CONTRA-TESTE DO ESPELHO em 2026-08-16, e o papel novo merece registro
     * porque ele pegou um defeito de desenho meu.
     *
     * <p>A rota do espelho ({@code ProvedorCorrecaoFala}) retraduz a linha INTEIRA a partir do
     * inglês quando o marcador se perde. Eu a havia liberado pelo MOTIVO da auditoria, que inclui
     * "resíduo gringo" — e resíduo aparece em fala <b>já traduzida</b>. Aqui o PT é
     * {@code "{\i1}O piloto voltou."}, uma tradução legítima: com o portão frouxo, o espelho
     * disparava e o modelo passava a receber {@code "The pilot returned."} em vez de
     * {@code "[[TAG0]]O piloto voltou."} — ou seja, a tradução boa seria jogada fora para consertar
     * uma palavra. Este teste reprovou exatamente assim.
     *
     * <p>O portão passou a ser a COMPARAÇÃO com o original (ideia de Paulo): o espelho só entra
     * quando o texto visível do PT ainda é, letra por letra, o texto visível do inglês.
     */
    @Test
    void preservaComoMarcadorSomenteATagExistenteNoPt() {
        LlmCapturador llm = executar(
            "{\\blur2}The pilot returned.",
            "{\\i1}O piloto voltou.");

        assertEquals("The pilot returned.", llm.originalRecebido);
        assertEquals("[[TAG0]]O piloto voltou.", llm.traducaoRecebida);
    }

    @Test
    void isolaQuebraInternaDoArgamaEReaplicaDepoisDaTraducao() {
        String original = "Presently on the Argama, we have\\Nthe GM pilots from the Mont Blanc...";
        LlmCapturador llm = new LlmCapturador(
            "Atualmente no ZXQLORE1QXZ, temos os pilotos do GM da ZXQLORE0QXZ...");
        ProvedorCorrecaoFala provedor = novoProvedor(
            llm, new ValidadorTraducaoService(LoreAtivaFake.com("Argama", "Mont Blanc")));

        ProvedorCorrecaoFala.Resultado resultado = provedor.obter(
            ModoRevisaoLegendas.LLM_CONCORDANCIA,
            original,
            original,
            List.of(PoliticaRetraducao.NAO_TRADUZIDA),
            LORE_ZETA);

        ProvedorCorrecaoFala.Resultado.Obtida obtida =
            assertInstanceOf(ProvedorCorrecaoFala.Resultado.Obtida.class, resultado, resultado.toString());
        assertFalse(llm.traducaoRecebida.contains("[[TAG"),
            "a quebra interna não pode virar marcador no prompt");
        assertEquals(
            "Presently on the ZXQLORE1QXZ, we have the GM pilots from the ZXQLORE0QXZ...",
            llm.traducaoRecebida,
            "os nomes canônicos devem atravessar o LLM protegidos");
        assertEquals(1, contarQuebras(obtida.texto()));
        assertEquals("Atualmente no Argama, temos os pilotos do GM da Mont Blanc...",
            obtida.texto().replace("\\N", " "));
    }

    private LlmCapturador executar(String original, String traducao) {
        LlmCapturador llm = new LlmCapturador();
        ProvedorCorrecaoFala provedor = novoProvedor(llm);

        provedor.obter(
            ModoRevisaoLegendas.LLM_CONCORDANCIA,
            original,
            traducao,
            List.of(PoliticaRetraducao.NAO_TRADUZIDA),
            SEM_LORE);
        return llm;
    }

    private ProvedorCorrecaoFala novoProvedor(LlmCapturador llm) {
        return novoProvedor(llm, new ValidadorTraducaoService(LORE_VAZIA));
    }

    private ProvedorCorrecaoFala novoProvedor(
        LlmCapturador llm, ValidadorTraducaoService validador) {
        return new ProvedorCorrecaoFala(
            llm,
            null,
            new ProtetorTermosLoreService(),
            new MascaradorTags(),
            new IsoladorQuebraDialogo(),
            validador,
            new ProtecaoLegendaAssService());
    }

    private static int contarQuebras(String texto) {
        return (texto.length() - texto.replace("\\N", "").length()) / 2;
    }

    private static final class LlmCapturador implements LlmPort {
        private String originalRecebido;
        private String traducaoRecebida;
        private final Optional<String> resposta;

        private LlmCapturador() {
            this.resposta = Optional.empty();
        }

        private LlmCapturador(String resposta) {
            this.resposta = Optional.of(resposta);
        }

        @Override
        public Optional<String> corrigirTraducao(String original, String traducao, String motivo) {
            originalRecebido = original;
            traducaoRecebida = traducao;
            return resposta;
        }

        @Override
        public Optional<String> revisarConcordancia(
                String original, String traducao, List<String> problemas) {
            originalRecebido = original;
            traducaoRecebida = traducao;
            return resposta;
        }

        @Override
        public TraducaoLote traduzir(Lote lote) {
            throw new UnsupportedOperationException("fora do escopo deste teste");
        }

        @Override
        public StatusLlm verificarDisponibilidade() {
            throw new UnsupportedOperationException("fora do escopo deste teste");
        }
    }
}
