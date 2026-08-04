package org.traducao.projeto.arquitetura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: toda fronteira de INÍCIO de termo do projeto trata a quebra {@code \N} do
 * ASS como separador. A regra vive hoje em <b>12 arquivos de 7 fatias</b>, e este é o único ponto
 * que a cobra por inteiro.
 *
 * <h2>Por que uma catraca, e não doze testes</h2>
 * {@code \N} ocupa DOIS caracteres — contrabarra e a letra {@code N} — e o {@code N} é letra para
 * {@code \p{L}}. Um lookbehind {@code (?<![\p{L}\p{N}])} sozinho conclui que o termo colado na
 * quebra é sufixo de outra palavra, e o termo fica invisível.
 *
 * <p><b>MEDIDO em 2026-08-04</b>, mutando a constante e rodando a suíte: dos 12 arquivos, apenas
 * DOIS quebravam algum teste — {@code ValidadorTraducaoService} (4 testes) e
 * {@code EnforcadorTermosLore} (3). <b>Os outros 10 podiam perder a alternativa com o repo
 * inteiro verde.</b> Escrever dez testes de comportamento significaria dez fixtures diferentes,
 * cada uma com a semântica da sua fatia; esta catraca cobre os dez e também o décimo terceiro
 * arquivo, que ainda não existe.
 *
 * <p>O efeito é real onde foi medido: no cache do acervo (60.891 falas, 24,6% com a quebra), das 12
 * formas sem acento do {@code NormalizadorAcentosComuns}, <b>0 sobreviveram soltas e 11
 * sobreviveram coladas na quebra</b> — forma solta foi sempre corrigida, forma colada nunca foi.
 * O mesmo experimento no {@code EnforcadorTermosLore} deu 74 termos canônicos colados à quebra.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Toda ocorrência do lookbehind de letra/dígito em {@code src/main} vem acompanhada da
 *       alternativa da quebra na MESMA declaração. Não há número congelado: o invariante é
 *       universal, e por isso não pode ser "ajustado para baixo" quando alguém o violar — que foi
 *       como a catraca de duplicação foi contornada no mesmo dia.</li>
 *   <li>Todas as declarações usam a forma IDÊNTICA. Variante de grafia é deriva começando.</li>
 *   <li>Só o lado ESQUERDO precisa da alternativa: à direita do termo o caractere da quebra é a
 *       contrabarra, que já não é letra nem dígito. {@code FIM_DE_TERMO} fica de fora de propósito.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem nomeia arquivo e linha, e mostra a forma esperada pronta para colar.
 */
class CatracaFronteiraQuebraAssTest {

    private static final Path RAIZ = Path.of("src/main/java/org/traducao/projeto");

    /** Uma contrabarra. As constantes abaixo descrevem o TEXTO do fonte, não uma regex. */
    private static final String C = "\\";
    private static final String DUAS = C + C;
    private static final String QUATRO = DUAS + DUAS;

    /** Como aparece no fonte: {@code (?<![\\p{L}\\p{N}])}. */
    private static final String LOOKBEHIND_LETRA = "(?<![" + DUAS + "p{L}" + DUAS + "p{N}])";

    /** Como aparece no fonte: {@code (?<=\\\\N)}. */
    private static final String ALTERNATIVA_QUEBRA = "(?<=" + QUATRO + "N)";

    /** A forma canônica inteira, para a mensagem de falha ser copiável. */
    private static final String FORMA_CANONICA =
        "\"(?:" + ALTERNATIVA_QUEBRA + "|" + LOOKBEHIND_LETRA + ")\"";

    @Test
    @DisplayName("toda fronteira de inicio de termo trata a quebra \\N do ASS")
    void todaFronteiraDeInicioTrataAQuebra() throws IOException {
        List<String> desprotegidas = new ArrayList<>();
        varrer((arquivo, linhas) -> {
            for (int i = 0; i < linhas.size(); i++) {
                String linha = linhas.get(i);
                if (linha.contains(LOOKBEHIND_LETRA) && !linha.contains(ALTERNATIVA_QUEBRA)) {
                    desprotegidas.add("  " + RAIZ.relativize(arquivo) + ":" + (i + 1)
                        + System.lineSeparator() + "      " + linha.strip());
                }
            }
        });

        assertTrue(desprotegidas.isEmpty(), () -> """
            FRONTEIRA DE INICIO SEM TRATAR A QUEBRA \\N DO ASS.

            \\N ocupa DOIS caracteres e o N e letra para \\p{L}. Sozinho, o lookbehind conclui que
            o termo colado na quebra e sufixo de outra palavra, e o termo fica INVISIVEL. Medido no
            acervo: 24,6% das falas tem a quebra; das 12 formas sem acento do dicionario, 0
            sobreviveram soltas e 11 sobreviveram coladas nela.

            Use a forma canonica:
              """ + FORMA_CANONICA + """


            Ocorrencias sem a alternativa:
            """ + String.join(System.lineSeparator(), desprotegidas));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: as declarações são cópia consciente entre fatias — o projeto prefere
     * duplicar a acoplar —, mas cópia que muda de grafia deixa de ser cópia e vira deriva.
     *
     * <p>INVARIANTES DO DOMÍNIO: um único texto para todas as declarações de {@code INICIO_DE_TERMO}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista as variantes encontradas, com quem as declara.
     */
    @Test
    @DisplayName("todas as declaracoes de INICIO_DE_TERMO usam a forma identica")
    void declaracoesNaoDivergemNaGrafia() throws IOException {
        Set<String> formas = new TreeSet<>();
        List<String> onde = new ArrayList<>();
        varrer((arquivo, linhas) -> {
            for (String linha : linhas) {
                int igual = linha.indexOf("INICIO_DE_TERMO =");
                if (igual < 0 || !linha.contains("\"")) {
                    continue;
                }
                String valor = linha.substring(igual).strip();
                formas.add(valor);
                onde.add("  " + RAIZ.relativize(arquivo) + "  ->  " + valor);
            }
        });

        assertTrue(onde.size() >= 2, "as declaracoes de INICIO_DE_TERMO sumiram do codigo");
        assertEquals(1, formas.size(), () -> """
            AS DECLARACOES DE INICIO_DE_TERMO DIVERGIRAM.

            Sao copia consciente entre fatias, e o projeto prefere copia a acoplamento — mas copia
            que muda de grafia deixa de ser copia e vira deriva silenciosa. Em 2026-08-03 tres
            defeitos do acervo tiveram essa assinatura: a regra existia em dois lugares, um foi
            corrigido e o outro ficou.

            Formas encontradas:
            """ + String.join(System.lineSeparator(), onde));
    }

    private interface Visita {
        void aceitar(Path arquivo, List<String> linhas);
    }

    private static void varrer(Visita visita) throws IOException {
        try (Stream<Path> arquivos = Files.walk(RAIZ)) {
            for (Path arquivo : arquivos.filter(p -> p.toString().endsWith(".java")).toList()) {
                visita.aceitar(arquivo, Files.readAllLines(arquivo, StandardCharsets.UTF_8));
            }
        }
    }
}
