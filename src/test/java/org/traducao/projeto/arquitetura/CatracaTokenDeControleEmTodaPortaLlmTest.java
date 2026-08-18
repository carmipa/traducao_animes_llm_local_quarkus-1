package org.traducao.projeto.arquitetura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: garantir que TODA porta que lê a resposta crua do modelo tire o token de
 * template antes de entregá-la ao domínio. É catraca de arquitetura: não verifica comportamento,
 * verifica que uma forma de bug conhecida não voltou por uma porta nova.
 *
 * <h2>O prejuízo, medido DUAS vezes — e a segunda porque a primeira correção não varreu a classe</h2>
 * <b>11/08/2026, fatia {@code traducao}:</b> {@code <|END_OF_TURN_TOKEN|>} colado no fim da
 * resposta quebrava a checagem de marcadores e derrubava a fala para o tradutor de máquina. 115
 * das 116 falas perdidas naquela execução — 99% — eram este token.
 *
 * <p><b>18/08/2026, fatia {@code revisaoLore}:</b> o MESMO token, por uma porta que ninguém
 * contou. O Javadoc da correção anterior afirmava cobrir "os DOIS pontos que leem
 * {@code message.content()}" — e a fatia da Revisão de Lore tem o próprio cliente. Medido na
 * auditoria das sete obras rodadas naquele dia:
 * <pre>
 *   4.903 propostas recusadas por escopo
 *   4.903 (100%)   traziam o token na resposta
 *   2.616 (53,4%)  o token era a UNICA diferenca — o modelo nao mudara nada
 * </pre>
 * As sete obras fecharam com "Falas corrigidas: 0". A tela auditava, sinalizava e nunca
 * consertava, e a causa não era o modelo.
 *
 * <p>Contar portas na cabeça já falhou uma vez. Por isso a checagem é executável.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A checagem é sobre a CHAMADA, não sobre o arquivo. A primeira versão desta catraca só
 *       exigia que o arquivo citasse {@code TokenDeControleLlm} — e o {@code import} sozinho já
 *       satisfazia isso: apagar a chamada e deixar o import passava verde. Vista aprovando o
 *       caso doente, foi apertada.</li>
 *   <li>Comentários saem antes da varredura: comentário que cite a limpeza não é limpeza.</li>
 *   <li>A limpeza vale na linha da leitura ou até duas linhas acima — as duas formas reais no
 *       código, o ternário de uma linha e a chamada quebrada em três.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem nomeia arquivo e linha e diz o que fazer. Se a varredura não achar NENHUMA porta, o
 * teste também reprova: alvo vazio é cegueira do instrumento, não aprovação.
 */
class CatracaTokenDeControleEmTodaPortaLlmTest {

    private static final Path RAIZ = Path.of("src", "main", "java");
    private static final String LEITURA_DE_RESPOSTA = ".content()";
    private static final Pattern LIMPEZA = Pattern.compile(
        "TokenDeControleLlm\\s*\\.\\s*limpar\\s*\\(|limparTokensDeControle\\s*\\(");
    private static final Pattern COMENTARIO = Pattern.compile("//[^\\n]*|/\\*.*?\\*/", Pattern.DOTALL);
    private static final int LINHAS_DE_FOLGA = 2;

    @Test
    @DisplayName("toda porta que le a resposta do modelo tira o token de template")
    void todaPortaLimpaOTokenDeControle() {
        List<String> portas = new ArrayList<>();
        List<String> semLimpeza = new ArrayList<>();

        try (Stream<Path> arquivos = Files.walk(RAIZ)) {
            arquivos.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                // Comentario fora: citar a limpeza nao e limpar.
                String[] linhas = COMENTARIO.matcher(ler(p)).replaceAll("").split("\n", -1);
                for (int i = 0; i < linhas.length; i++) {
                    if (!linhas[i].contains(LEITURA_DE_RESPOSTA)) {
                        continue;
                    }
                    String onde = p.getFileName() + ":" + (i + 1);
                    portas.add(onde);
                    if (!limpaNaJanela(linhas, i)) {
                        semLimpeza.add(onde);
                    }
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // Alvo vazio sai reprovado, nunca aprovado: se a varredura nao achou porta nenhuma, o
        // instrumento cegou (pacote movido, metodo renomeado) e o verde seria mentira.
        assertTrue(portas.size() >= 3, () ->
            "a catraca encontrou " + portas.size() + " leitura(s) de resposta do LLM, e o codigo "
                + "tem pelo menos 3. Isso NAO e aprovacao: alguem renomeou o metodo ou moveu o "
                + "pacote e a catraca ficou cega. Conserte a varredura antes de confiar no verde. "
                + "Achadas: " + portas);

        assertTrue(semLimpeza.isEmpty(), () ->
            "porta que le a resposta do modelo SEM tirar o token de template: " + semLimpeza
                + ". Envolva a leitura com TokenDeControleLlm.limpar(...). Sem isso, "
                + "\"<|END_OF_TURN_TOKEN|>\" entra no texto e o dominio o le como conteudo — em "
                + "18/08/2026 isso recusou 4.903 propostas de revisao de lore e fechou sete obras "
                + "com zero correcoes.");
    }

    /** A limpeza pode estar na propria linha ou nas duas acima — as duas formas reais no codigo. */
    private static boolean limpaNaJanela(String[] linhas, int leitura) {
        for (int i = Math.max(0, leitura - LINHAS_DE_FOLGA); i <= leitura; i++) {
            if (LIMPEZA.matcher(linhas[i]).find()) {
                return true;
            }
        }
        return false;
    }

    private static String ler(Path arquivo) {
        try {
            return Files.readString(arquivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
