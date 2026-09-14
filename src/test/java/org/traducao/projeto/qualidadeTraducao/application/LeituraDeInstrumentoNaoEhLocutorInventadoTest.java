package org.traducao.projeto.qualidadeTraducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * PROPÓSITO DE NEGÓCIO: prova que a guarda de locutor inventado deixou de recusar LEITURA DE
 * INSTRUMENTO — rumo, altitude, distância, horário —, e continua recusando o rumo que o modelo
 * INVENTA. As duas formas são superficialmente idênticas: rótulo curto, dois-pontos, conteúdo
 * depois. O que as separa é se o número veio do original.
 *
 * <h2>O prejuízo MEDIDO — 2026-09-14</h2>
 * Recusa aqui devolve a fala ao INGLÊS na legenda. Quatro ocorrências independentes, três delas
 * sem nenhuma relação com alteração de prompt:
 * <pre>
 * "Heading 2-8-0. Distance 5,000."  -> "Direção: 2-8-0. Distância: 5.000."   era RECUSADA
 * "Heading 2-8-0. Distance 5,000."  -> "Título: 2-8-0. Distância: 5.000."    era RECUSADA
 * "Current altitude 4000! ..."      -> "Altitude atual: 4.000 metros! ..."   era RECUSADA
 * horário lido como prefixo de locutor                                       era RECUSADA
 * </pre>
 * E o dano passou do inglês: no acervo publicado, {@code "Heading 2-8-0. Distance 5,000."} saiu
 * como {@code "Distancia 5.000."} — <b>o rumo sumiu do arquivo</b>. A explicação que os dados
 * sustentam é que a resposta completa foi recusada e a retentativa entregou uma resposta
 * truncada, que passou. Falso positivo em guarda bloqueante não só devolve inglês: ele empurra o
 * pipeline para aceitar a próxima resposta, que pode ser pior.
 *
 * <h2>O denominador (A8)</h2>
 * 438 pares {@code (original, resposta do modelo)} gerados no aya-expanse-8b sobre o corpus da
 * frente de proa, validados antes e depois da correção:
 * <pre>
 *                        antes    depois
 * leituras de instrumento   6         0     <- falsos positivos removidos
 * rumos INVENTADOS          3         3     <- continuam recusados, como devem
 * controles legitimos       0         0
 * alucinacao desproporcional 3        3     <- outra regra, intocada
 * </pre>
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A isenção exige número LOGO APÓS os dois-pontos: narração inventada é prosa.</li>
 *   <li>Exige que TODO número da tradução exista no original, com o separador de grupo
 *       normalizado ({@code 4000} do inglês é {@code 4.000} do português).</li>
 *   <li>Não há lista de rótulos permitidos. Lista de palavra é frágil por construção, e este
 *       arquivo já pagou por isso mais de uma vez.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * {@link AlucinacaoDetectadaException} com o par no diagnóstico.
 */
@DisplayName("Leitura de instrumento nao e locutor inventado -- e rumo inventado continua sendo")
class LeituraDeInstrumentoNaoEhLocutorInventadoTest {

    /**
     * PROPÓSITO DE NEGÓCIO: validador sem lore, porque esta regra não depende de terminologia —
     * o que decide é a origem do número.
     * <p>INVARIANTES DO DOMÍNIO: conjuntos vazios, para nenhum termo protegido interferir.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    private static ValidadorTraducaoService validador() {
        return new ValidadorTraducaoService(
            new org.traducao.projeto.qualidadeTraducao.domain.LoreAtivaPort() {
                @Override public Set<String> termosProtegidosAtivos() { return Set.of(); }
                @Override public String obterLoreAtiva() { return ""; }
                @Override public Set<List<String>> paresInconfundiveisAtivos() { return Set.of(); }
            });
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource(delimiter = '|', value = {
        // Todos os numeros da traducao existem no original: e leitura, nao invencao.
        "Heading 2-8-0. Distance 5,000.        | Direção: 2-8-0. Distância: 5.000.",
        "Heading 2-8-0. Distance 5,000.        | Título: 2-8-0. Distância: 5.000.",
        "Current altitude 4000!                | Altitude atual: 4.000 metros!",
        "Current altitude 4000!                | Altura atual: 4.000 metros!",
        "Heading 030. Four rooftop units.      | Rumo: 030. Quatro unidades no telhado.",
        "Bearing 771, range 8000.              | Marcação: 771, alcance 8000.",
    })
    @DisplayName("ACEITA: rotulo de leitura cujo numero veio do original")
    void leituraDeInstrumentoPassa(String original, String traduzido) {
        assertDoesNotThrow(() -> validador().validarPar(original, traduzido),
            "recusar isto devolve a fala ao ingles na legenda, e o numero veio do proprio original");
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource(delimiter = '|', value = {
        // MESMO sinal superficial -- rotulo curto, dois-pontos, numero depois -- e o numero
        // NAO existe no original. Foi medido no modelo real: a instrucao de proa faz o aya
        // confabular um rumo nesta fala.
        "Heading 060, distance 800.            | Direção: 180 graus.",
        "Heading 060, distance 800.            | Direção: 315 graus. Distância: 800 metros.",
    })
    @DisplayName("RECUSA: rumo INVENTADO, com o mesmo sinal superficial da leitura legitima")
    void rumoInventadoContinuaRecusado(String original, String traduzido) {
        assertThrows(AlucinacaoDetectadaException.class,
            () -> validador().validarPar(original, traduzido),
            "numero que nao existe no original e invencao, e publicar rumo errado e pior que ingles");
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource(delimiter = '|', value = {
        // A isencao nao pode abrir a porta para a invencao que a regra nasceu para pegar.
        "Done!                                 | Linha 1:",
        "Haruhime View                         | Haruhime disse: Estou aqui.",
        "Go!                                   | Narrador: Ele avança pelo campo de batalha.",
    })
    @DisplayName("CASO-CONTROLE: a invencao classica continua recusada -- a isencao nao a alcanca")
    void invencaoClassicaContinuaRecusada(String original, String traduzido) {
        assertThrows(AlucinacaoDetectadaException.class,
            () -> validador().validarPar(original, traduzido),
            "sem numero do original logo apos os dois-pontos, a isencao nao vale");
    }

    @Test
    @DisplayName("numero SEM nada depois dos dois-pontos nao ativa a isencao")
    void prefixoNumericoSemConteudoNaoIsenta() {
        assertThrows(AlucinacaoDetectadaException.class,
            () -> validador().validarPar("Done!", "Linha 1:"),
            "a isencao exige conteudo numerico DEPOIS dos dois-pontos");
    }
}
