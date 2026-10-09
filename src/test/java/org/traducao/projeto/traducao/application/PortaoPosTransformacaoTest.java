package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova o portão depois da última transformação
 * ({@link ProcessarArquivoUseCase#desfazerTransformacoesQueReprovam}). Até 08/10/2026 a fala que
 * passava no portão e ficava reprovável depois das transformações era gravada assim, e a releitura
 * A6 só registrava — 8 casos no log desde 17/09.
 *
 * <p>INVARIANTES DO DOMÍNIO: desfaz para a versão APROVADA (nunca para o inglês), só quando a
 * transformada reprova e a aprovada passa; não mexe em fala boa, em pendente nem na que já
 * reprovava antes.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: transformada reprovável gravada é a regressão; aprovada boa
 * trocada é o falso positivo.
 */
@DisplayName("portão pós-transformação: fala estragada depois do portão volta à versão aprovada")
class PortaoPosTransformacaoTest {

    /** O portão de estrutura, como no caso de 17/09: a quebra do original tem de estar na tradução. */
    private static final BiFunction<String, String, String> PORTAO = (original, traduzido) ->
        contarQuebras(original) != contarQuebras(traduzido) ? "tags ASS/SSA ou quebras de linha divergentes do original" : null;

    private static int contarQuebras(String s) {
        return s.split("\\\\N", -1).length - 1;
    }

    @Test
    @DisplayName("a troca de termo comeu a quebra: volta a versão aprovada, com aviso que diz o porquê")
    void transformacaoQueReprovaEDesfeita() {
        String original = "We can grab some\\Nnormal suits on the way out!";
        String aprovada = "Podemos pegar alguns trajes\\Nnormais no caminho!";
        Map<String, String> validadas = new HashMap<>(Map.of(original, "Podemos pegar alguns Normal Suits no caminho!"));
        Map<String, String> aprovadas = Map.of(original, aprovada);

        List<String> avisos = ProcessarArquivoUseCase.desfazerTransformacoesQueReprovam(validadas, aprovadas, PORTAO);

        assertEquals(aprovada, validadas.get(original), "publica a versão que o portão aprovou, nunca o inglês");
        assertEquals(1, avisos.size());
        assertTrue(avisos.getFirst().contains("quebras de linha divergentes"), avisos.getFirst());
    }

    @Test
    @DisplayName("A1: transformação que mantém a fala aprovável fica (o acento, o termo restaurado)")
    void transformacaoBoaFica() {
        String original = "Deploy the Mobile\\NSuit.";
        Map<String, String> validadas = new HashMap<>(Map.of(original, "Lance o Mobile\\NSuit."));
        Map<String, String> aprovadas = Map.of(original, "Lance o traje\\Nmóvel.");

        List<String> avisos = ProcessarArquivoUseCase.desfazerTransformacoesQueReprovam(validadas, aprovadas, PORTAO);

        assertEquals("Lance o Mobile\\NSuit.", validadas.get(original));
        assertTrue(avisos.isEmpty());
    }

    @Test
    @DisplayName("não desfaz quando a aprovada também reprova agora, nem mexe em pendente")
    void semVersaoBoaOuPendenteNaoMexe() {
        String a = "Line\\Nbreak";
        String b = "Pending\\Nline";
        Map<String, String> validadas = new HashMap<>(Map.of(a, "Linha quebrada", b, ""));
        Map<String, String> aprovadas = Map.of(a, "Linha sem a quebra", b, "");

        List<String> avisos = ProcessarArquivoUseCase.desfazerTransformacoesQueReprovam(validadas, aprovadas, PORTAO);

        assertEquals("Linha quebrada", validadas.get(a), "sem versão aprovável não há para onde voltar");
        assertEquals("", validadas.get(b));
        assertTrue(avisos.isEmpty());
        assertTrue(ProcessarArquivoUseCase.desfazerTransformacoesQueReprovam(null, aprovadas, PORTAO).isEmpty());
    }
}
