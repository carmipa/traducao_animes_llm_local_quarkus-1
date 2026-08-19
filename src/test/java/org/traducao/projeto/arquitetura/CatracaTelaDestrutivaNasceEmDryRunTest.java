package org.traducao.projeto.arquitetura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: a tela que reescreve arquivo do acervo tem de ABRIR em dry-run. Se o
 * padrão for gravar, um clique distraído já é dano — e o dano aqui é o {@code .ass} entregue.
 *
 * <h2>O prejuízo que originou</h2>
 * Até 19/08/2026 o checkbox "Apenas simular" da 3.3 nascia <b>desmarcado</b>, e o JS calcula
 * {@code aplicar = !simular}: abrir a tela, informar a pasta e apertar "Revisar" GRAVAVA. As
 * telas irmãs do projeto (novoKaraoke, renomearArquivos, traducaoKaraoke) nunca tiveram esse
 * desenho — nelas simular e aplicar são BOTÕES distintos, e a ação destrutiva exige escolher.
 * A 3.3 era a exceção, e a exceção era a insegura.
 *
 * <p>É a regra de falha fechada aplicada à interface, e a lente de boa-fé em uma linha: <i>como
 * uma pessoa honesta, seguindo uma interpretação razoável da tela, causaria dano sem perceber?</i>
 * Apertando o único botão da tela, que é o que qualquer um faz.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Todo {@code <input type="checkbox">} cujo id termina em {@code -simular} nasce com o
 *       atributo {@code checked}.</li>
 *   <li><b>Alvo vazio é instrumento CEGO, não aprovação:</b> se nenhum checkbox de simulação for
 *       encontrado, o teste reprova — ou a convenção do id mudou, ou a varredura quebrou, e as
 *       duas pedem olhar humano.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nomeia o id exato do checkbox que nasce desmarcado. Não afirma nada sobre o backend: quem
 * prova que o dry-run não escreve é {@code RevisarConcordanciaUseCaseTest#dryRunNaoEscreve}.
 */
class CatracaTelaDestrutivaNasceEmDryRunTest {

    private static final Path RAIZ_ESTATICOS = Path.of("src", "main", "resources", "static");

    /**
     * O checkbox de simulação, com o atributo {@code checked} opcional em qualquer posição. O
     * {@code [^>]*} dos dois lados é o que torna a busca robusta à ordem dos atributos — casar
     * uma sequência fixa deixaria a catraca verde no dia em que alguém reordenasse o HTML.
     */
    private static final Pattern CHECKBOX_SIMULAR = Pattern.compile(
        "<input([^>]*\\btype=\"checkbox\"[^>]*\\bid=\"([a-z0-9-]+-simular)\"[^>]*)>",
        Pattern.CASE_INSENSITIVE);

    @Test
    @DisplayName("todo checkbox de simulacao nasce MARCADO — a tela destrutiva abre em dry-run")
    void telaQueEscreveNasceEmDryRun() throws IOException {
        assertTrue(Files.isDirectory(RAIZ_ESTATICOS),
            "NAO VERIFICADO: " + RAIZ_ESTATICOS.toAbsolutePath() + " nao existe");

        int encontrados = 0;
        StringBuilder desmarcados = new StringBuilder();

        try (var caminhos = Files.walk(RAIZ_ESTATICOS)) {
            for (Path html : caminhos.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".html")).toList()) {
                Matcher m = CHECKBOX_SIMULAR.matcher(Files.readString(html, StandardCharsets.UTF_8));
                while (m.find()) {
                    encontrados++;
                    if (!m.group(1).toLowerCase().contains("checked")) {
                        desmarcados.append("\n  ").append(m.group(2))
                            .append("  em ").append(html.getFileName());
                    }
                }
            }
        }

        assertTrue(encontrados > 0,
            "NAO VERIFICADO: nenhum checkbox '*-simular' encontrado nas telas. Ou a convencao do "
                + "id mudou, ou a varredura quebrou — as duas pedem olhar, nao verde.");

        assertTrue(desmarcados.isEmpty(), () -> """
            Checkbox de simulacao nascendo DESMARCADO: %s

            O JS calcula aplicar = !simular. Desmarcado por padrao significa que abrir a tela e
            apertar o botao GRAVA no .ass do acervo — um clique distraido basta.

            Acrescente o atributo checked, ou separe simular e aplicar em botoes distintos, como
            as telas irmas ja fazem.
            """.formatted(desmarcados));
    }

    /**
     * CASO-CONTROLE: a varredura precisa ser vista separando o marcado do desmarcado. Sem isto o
     * verde acima pode ser cegueira do regex — e um regex de HTML erra fácil.
     */
    @Test
    @DisplayName("caso-controle: o regex enxerga o checked em qualquer ordem e acusa a falta dele")
    void regexDiscriminaOChecked() {
        Matcher marcado = CHECKBOX_SIMULAR.matcher(
            "<input type=\"checkbox\" id=\"x-simular\" checked>");
        assertTrue(marcado.find() && marcado.group(1).contains("checked"), "nao viu o checked no fim");

        Matcher marcadoNoMeio = CHECKBOX_SIMULAR.matcher(
            "<input type=\"checkbox\" checked id=\"y-simular\" class=\"z\">");
        assertTrue(marcadoNoMeio.find() && marcadoNoMeio.group(1).contains("checked"),
            "a ordem dos atributos nao pode mudar o veredito");

        Matcher semChecked = CHECKBOX_SIMULAR.matcher(
            "<input type=\"checkbox\" id=\"w-simular\">");
        assertTrue(semChecked.find(), "nao achou o checkbox desmarcado");
        assertFalse(semChecked.group(1).contains("checked"), "acusou checked onde nao ha");
    }
}
