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
 * PROPÓSITO DE NEGÓCIO: prova que o nome {@code Four} deixou de comer os nomes curtos de
 * personagem vizinhos. O espectador via o nome ERRADO na tela, e nenhuma regra de texto
 * enxergava: {@code "Four!"} é português impecável, é termo canônico da obra, preserva a
 * estrutura ASS e passa no dicionário.
 *
 * <h2>O prejuízo MEDIDO — 2026-09-09, no acervo PUBLICADO</h2>
 * Varredura de 32.430 pares de diálogo, original contra o {@code .ass} entregue:
 * <pre>
 * "Four" no publicado COM "Four" no original (legitimo) ... 158
 * "Four" no publicado SEM  "Four" no original (INJETADO) ...  24
 * </pre>
 * O padrão dominante é {@code "Fa!"} virando {@code "Four!"}. <b>Fa Yuiry e Four Murasame são
 * personagens diferentes.</b> No cache do ZZ o mesmo ímã come outros três nomes curtos:
 * <pre>
 * "Fa! He's found us!"                  -> "Four! Ele nos encontrou!"
 * "Iino! Qum! You okay?"                -> "Iino! Four! Voce esta bem?"
 * "P-Ple?! Why?!"                       -> "P-Four?! Por que?!"
 * "Clear out of Mistress Chara's seat"  -> "Saia do lugar de Four"
 * "QUATTRO BAJEENA" (cartao de nome)    -> "Four Murasame."
 * </pre>
 *
 * <h2>Por que o par Four x Quattro nao bastava</h2>
 * Ele já existia e por isso {@code Quattro} era pego. As outras quatro personagens nunca foram
 * DECLARADAS como inconfundíveis, então a mesma guarda ficava cega para elas. O mecanismo estava
 * pronto; faltava a declaração — e declaração ausente é exatamente o {@code 2} da regra 23:
 * "não achei nada" com a cara de "não tinha nada".
 *
 * <h2>Por que nao se resolve tirando o exemplo do prompt</h2>
 * MEDIDO no aya-expanse-8b em 2026-09-09, três formas da mesma cláusula de lore:
 * <pre>
 *                              injecao (12 falas sem Four)   recall (8 falas com Four)
 * tabela "Four -> Four"                    0                          0
 * prosa COM exemplo de frase               1 a 2                      7
 * prosa SEM o exemplo                      0                          1
 * </pre>
 * O exemplo <b>é</b> o mecanismo: sem ele o recall colapsa. Então a cláusula fica, e o dano se
 * fecha aqui, no portão determinístico, que é onde ele pode ser fechado sem custo de qualidade.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A troca só é acusada quando o nome do original <b>SUMIU</b> da tradução. Fala que
 *       menciona os dois nomes passa intacta — é o que os casos-controle fixam.</li>
 *   <li>Os pares valem nas duas direções.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * {@link AlucinacaoDetectadaException} com os dois nomes no diagnóstico.
 */
@DisplayName("O ima do nome Four nao come mais Fa, Qum, Ple, Chara nem Quattro")
class ImaDoNomeFourNaoComeOsVizinhosTest {

    /**
     * PROPÓSITO DE NEGÓCIO: dublê com os pares que a lore de Zeta e ZZ passou a declarar em
     * 2026-09-09, para o teste provar o comportamento sem subir CDI nem ler o YAML.
     * <p>INVARIANTES DO DOMÍNIO: os mesmos pares do arquivo; divergir daqui torna o teste uma
     * ficção que aprova o que a produção reprova.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    private static ValidadorTraducaoService comOsParesDaLore() {
        Set<List<String>> reais = paresDeclaradosEmProducao();
        return new ValidadorTraducaoService(new org.traducao.projeto.qualidadeTraducao.domain.LoreAtivaPort() {
            @Override
            public Set<String> termosProtegidosAtivos() {
                return Set.of("Four", "Fa", "Qum", "Ple", "Chara", "Quattro");
            }

            @Override
            public String obterLoreAtiva() {
                return "";
            }

            @Override
            public Set<List<String>> paresInconfundiveisAtivos() {
                return reais;
            }
        });
    }

    /**
     * PROPÓSITO DE NEGÓCIO: lê do CATÁLOGO REAL os pares declarados nas obras onde o ímã do
     * {@code Four} foi medido, para o dublê nunca ser mais generoso que a produção.
     *
     * <h2>A cicatriz que obrigou isto — 2026-09-14</h2>
     * A primeira versão deste dublê escrevia os pares à mão e incluía {@code Four x Quattro}. No
     * ZZ esse par <b>não estava declarado</b>, só no Zeta. Resultado: este teste passava verde
     * enquanto a produção publicava, pela segunda vez, o cartão de nome {@code "QUATTRO BAJEENA"}
     * como {@code "Four Murasame"} — e só uma retradução do episódio 1 do zero mostrou isso.
     * <b>Dublê mais generoso que a realidade aprova por cegueira</b>, que é a mesma forma do
     * defeito que a guarda existe para impedir.
     *
     * <p>INVARIANTES DO DOMÍNIO: usa a união de {@code gundam_zz} e {@code gundam_zeta}, que são
     * as duas obras onde as trocas deste teste foram medidas. Obra ausente do catálogo faz o
     * teste falhar em vez de silenciar — conjunto vazio aprovaria tudo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@link IllegalStateException} nomeando a obra ausente.
     */
    private static Set<List<String>> paresDeclaradosEmProducao() {
        var catalogo = new org.traducao.projeto.lore.infrastructure.CatalogoLoreYaml();
        Set<List<String>> uniao = new java.util.LinkedHashSet<>();
        for (String obra : List.of("gundam_zz", "gundam_zeta")) {
            var provedor = catalogo.obras().stream()
                .filter(o -> obra.equals(o.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                    "obra ausente do catalogo de lore: " + obra
                        + " — sem ela o duble ficaria sem pares e aprovaria tudo"));
            uniao.addAll(provedor.paresInconfundiveis());
        }
        return uniao;
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource(delimiter = '|', value = {
        "Fa!                                  | Four!",
        "Fa! He's found us!                   | Four! Ele nos encontrou!",
        "Iino! Qum! You okay?                 | Iino! Four! Voce esta bem?",
        "P-Ple?! Why?!                        | P-Four?! Por que?!",
        "Clear out of Mistress Chara's seat!   | Saia do lugar de Four!",
        "QUATTRO BAJEENA                      | Four Murasame.",
    })
    @DisplayName("nome curto de personagem trocado por Four e RECUSADO")
    void nomeTrocadoPorFourEhRecusado(String original, String traduzido) {
        assertThrows(AlucinacaoDetectadaException.class,
            () -> comOsParesDaLore().validarPar(original, traduzido),
            "estas seis foram GRAVADAS na legenda: o espectador leu o nome de outra personagem");
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource(delimiter = '|', value = {
        // A troca e o nome SUMIR. Fala que preserva o nome do original passa.
        "Four!                     | Four!",
        "Fa!                       | Fa!",
        "Fa, this is Four.         | Fa, esta e a Four.",
        "Fa and Four are here.     | Fa e Four estao aqui.",
        "Where is Four Murasame?   | Onde esta Four Murasame?",
        "Qum, get down!            | Qum, abaixe-se!",
    })
    @DisplayName("CASO-CONTROLE: fala que PRESERVA o nome, ou que cita os dois, passa intacta")
    void falaQuePreservaONomePassa(String original, String traduzido) {
        assertDoesNotThrow(() -> comOsParesDaLore().validarPar(original, traduzido),
            "guarda que reprova o correto e pior que guarda nenhuma: aqui nada foi trocado");
    }

    @Test
    @DisplayName("o diagnostico nomeia AS DUAS entidades, para o operador saber o que virou o que")
    void diagnosticoNomeiaAsDuasEntidades() {
        var erro = assertThrows(AlucinacaoDetectadaException.class,
            () -> comOsParesDaLore().validarPar("Fa!", "Four!"));
        String m = erro.getMessage();
        org.junit.jupiter.api.Assertions.assertTrue(m.contains("Fa") && m.contains("Four"),
            "o diagnostico tem de citar as duas entidades, e disse: " + m);
    }
}
