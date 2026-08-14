package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * PROPÓSITO DE NEGÓCIO: garante que a tradução SEM LORE receba as mesmas correções determinísticas
 * que a tradução com lore. Sem isso, comparar as duas seria comparar pipelines diferentes, e todo
 * número medido numa obra sem lore ficaria sem sentido.
 *
 * <h2>O padrão perigoso que esta catraca procura</h2>
 * Oito linhas acima do corretor, no mesmo método, existe este bloco — legítimo:
 * <pre>{@code
 * Map<String, String> correcoesLore = contexto.correcoesTerminologia();
 * if (!correcoesLore.isEmpty()) { ... enforcadorTermosLore.reforcar ... }
 * }</pre>
 * O risco é a proximidade: envolver o laço de normalização num {@code if} igual a esse parece
 * simetria e desliga o dicionário justamente na obra que mais precisa dele — a que não tem lista
 * de termos para proteger nada. {@code ContextoSemLore} devolve
 * {@code correcoesTerminologia()} e {@code termosProtegidos()} VAZIOS por definição.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code normalizadorAcentos.normalizar} e {@code corretorOrtografico.corrigir} rodam para
 *       toda tradução validada, sem condicional de lore no caminho.</li>
 *   <li>Alvo ausente é NÃO VERIFICADO, nunca aprovação: se o arquivo ou a chamada sumirem, esta
 *       catraca REPROVA em vez de passar por não achar nada.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Reprova apontando a linha e o termo de lore encontrado no caminho.
 */
@DisplayName("catraca: o dicionário não pode depender de haver lore")
class CatracaCorretorIndependeDeLoreTest {

    private static final Path ALVO = Path.of(
        "src/main/java/org/traducao/projeto/traducao/application/ProcessarArquivoUseCase.java");

    /** Marcas de que uma condicional decide por lore. */
    private static final List<String> MARCAS_DE_LORE = List.of(
        "correcoeslore", "correcoesterminologia", "termosprotegidos", "paresinconfundiveis",
        "semlore", "sem_lore", "temlore");

    private static final List<String> CHAMADAS_OBRIGATORIAS = List.of(
        "normalizadorAcentos.normalizar(", "corretorOrtografico.corrigir(");

    @Test
    @DisplayName("as duas correções existem no use case — alvo vazio é NÃO VERIFICADO")
    void asChamadasExistem() throws IOException {
        List<String> linhas = lerAlvo();
        for (String chamada : CHAMADAS_OBRIGATORIAS) {
            assertTrue(linhas.stream().anyMatch(l -> l.contains(chamada)),
                "NÃO VERIFICADO: '" + chamada + "' não existe mais em " + ALVO + ". Ou a correção "
                    + "saiu do pipeline, ou foi renomeada — nos dois casos esta catraca precisa "
                    + "ser reapontada antes de voltar a valer.");
        }
    }

    /**
     * O TESTE DE VERDADE: para CADA {@code if} de lore no arquivo, calcula o trecho que ele
     * governa e prova que o corretor está fora dele.
     *
     * <h3>Por que não basta olhar entre o laço e a chamada</h3>
     * A primeira versão desta catraca subia do corretor até o {@code for} e varria só esse
     * intervalo. Na calibração ela passou verde com o arquivo ADULTERADO: um {@code if} escrito na
     * linha ANTERIOR ao {@code for} — que é a forma natural de escrever a adulteração — governa o
     * laço inteiro e ficava fora da janela examinada. Guarda cega aprova por não enxergar, e foi
     * exatamente o que aconteceu.
     */
    @Test
    @DisplayName("nenhum if de lore governa o trecho onde o corretor roda")
    void semCondicionalDeLoreGovernandoOcorretor() throws IOException {
        List<String> linhas = lerAlvo();
        int corretor = indiceDe(linhas, "corretorOrtografico.corrigir(");

        for (int i = 0; i < corretor; i++) {
            if (!ehIfDeLore(linhas.get(i))) {
                continue;
            }
            int fim = fimDoEscopoGovernado(linhas, i);
            assertFalse(corretor <= fim,
                "CONDICIONAL DE LORE governando o dicionário. O if da linha " + (i + 1)
                    + " alcança até a linha " + (fim + 1) + ", e o corretor está na linha "
                    + (corretor + 1) + ":\n    " + linhas.get(i).trim()
                    + "\nIsto desliga a correção ortográfica na tradução SEM LORE, que é "
                    + "exatamente a obra sem nenhuma outra rede de proteção — ContextoSemLore "
                    + "devolve correcoesTerminologia() e termosProtegidos() VAZIOS por definição. "
                    + "Se a intenção for mesmo condicionar, o teste que prova a nova intenção "
                    + "vem junto.");
        }
    }

    /** Linha que é um {@code if} e cuja condição menciona lore. */
    private static boolean ehIfDeLore(String linha) {
        String baixa = linha.toLowerCase(Locale.ROOT);
        String podado = baixa.trim();
        boolean temIf = podado.startsWith("if (") || podado.startsWith("if(")
            || baixa.contains(" if (") || baixa.contains(" if(");
        if (!temIf) {
            return false;
        }
        return MARCAS_DE_LORE.stream().anyMatch(baixa::contains);
    }

    /**
     * Índice da última linha governada pelo {@code if} que começa em {@code inicio}.
     *
     * <p>Cobre as duas formas: com bloco ({@code if (...) { ... }}) e sem bloco, em que o
     * {@code if} governa a instrução seguinte — e essa instrução pode ser um {@code for} inteiro,
     * que foi justamente o furo achado na calibração. Procura a primeira chave de abertura antes
     * do primeiro {@code ;}; achando-a, conta até o balanceamento fechar.
     */
    private static int fimDoEscopoGovernado(List<String> linhas, int inicio) {
        int profundidade = 0;
        boolean abriu = false;
        for (int i = inicio; i < linhas.size(); i++) {
            String linha = semComentario(linhas.get(i));
            for (int c = 0; c < linha.length(); c++) {
                char ch = linha.charAt(c);
                if (ch == '{') {
                    profundidade++;
                    abriu = true;
                } else if (ch == '}') {
                    profundidade--;
                    if (abriu && profundidade <= 0) {
                        return i;
                    }
                } else if (ch == ';' && !abriu && i > inicio) {
                    // if sem bloco governando uma instrução simples: acaba aqui.
                    return i;
                }
            }
        }
        return linhas.size() - 1;
    }

    /** Corta {@code //} para chave dentro de comentário não desbalancear a contagem. */
    private static String semComentario(String linha) {
        int i = linha.indexOf("//");
        return i < 0 ? linha : linha.substring(0, i);
    }

    /**
     * O laço de normalização itera {@code traducoesValidadas} — TODAS as traduções aproveitáveis,
     * independentemente de contexto. Trocar a coleção iterada é a outra forma de excluir o sem
     * lore sem escrever um {@code if}.
     */
    @Test
    @DisplayName("o laço itera traducoesValidadas, não uma coleção filtrada por lore")
    void oLacoIteraTodasAsTraducoes() throws IOException {
        List<String> linhas = lerAlvo();
        int corretor = indiceDe(linhas, "corretorOrtografico.corrigir(");
        String assinatura = linhas.get(subirAteOlaco(linhas, corretor));

        assertTrue(assinatura.contains("traducoesValidadas"),
            "o laço que contém o corretor passou a iterar outra coleção:\n    " + assinatura.trim()
                + "\nSe ela for filtrada por lore, a tradução sem lore deixa de ser corrigida sem "
                + "que nenhum 'if' apareça no caminho.");
    }

    private static List<String> lerAlvo() throws IOException {
        if (!Files.exists(ALVO)) {
            fail("NÃO VERIFICADO: " + ALVO + " não existe. Esta catraca não tem como olhar, e "
                + "'não olhei' nunca é 'está certo'.");
        }
        return Files.readAllLines(ALVO);
    }

    private static int indiceDe(List<String> linhas, String agulha) {
        for (int i = 0; i < linhas.size(); i++) {
            if (linhas.get(i).contains(agulha)) {
                return i;
            }
        }
        return fail("NÃO VERIFICADO: '" + agulha + "' não encontrado em " + ALVO);
    }

    /** Sobe até o {@code for} mais próximo acima da linha dada. */
    private static int subirAteOlaco(List<String> linhas, int partida) {
        for (int i = partida; i >= 0; i--) {
            String t = linhas.get(i).trim();
            if (t.startsWith("for (") || t.startsWith("for(")) {
                return i;
            }
        }
        return fail("NÃO VERIFICADO: o corretor não está dentro de nenhum laço em " + ALVO
            + " — a estrutura mudou e esta catraca precisa ser reescrita.");
    }
}
