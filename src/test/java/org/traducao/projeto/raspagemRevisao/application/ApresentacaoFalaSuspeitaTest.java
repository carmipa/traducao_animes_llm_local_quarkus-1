package org.traducao.projeto.raspagemRevisao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.core.presentation.ui.AnsiCores;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que o operador consegue distinguir, <b>pelo olho</b>, uma fala que
 * está em inglês de uma fala correta cujo inglês é só referência.
 *
 * <h2>O prejuízo MEDIDO que originou — 2026-08-17</h2>
 * Numa corrida do Zeta, 8 dos 10 achados eram concordância (português correto, inglês só de
 * referência) e 2 eram fala realmente em inglês. A tela imprimia os dois casos com o MESMO
 * formato e a MESMA cor. Paulo passou o dia convencido de que a ferramenta não estava
 * traduzindo. Auditoria linha a linha depois: <b>10 de 10 batiam com o disco</b> — o dado nunca
 * esteve errado, só a apresentação.
 *
 * <p>INVARIANTES DO DOMÍNIO: os dois casos NUNCA podem render o mesmo texto de tela; a linha do
 * inglês é sempre referência apagada; o selo de "ainda em inglês" vem do veredito do auditor.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: se este teste ficar verde com as duas saídas iguais, a tela
 * voltou a enganar quem a lê — e é o tipo de defeito que nenhuma outra guarda pega, porque o
 * dado continua certo.
 */
class ApresentacaoFalaSuspeitaTest {

    private final ApresentacaoFalaSuspeita apresentacao = new ApresentacaoFalaSuspeita();

    private static final String EN_CONCORDANCIA =
        "Seeing Amuro's heroics, Beltorchika willingly opened her heart to him.";
    private static final String PT_CONCORDANCIA =
        "Vendo as proezas de Amuro, Beltorchika abriu o coração para ele.";
    private static final String EN_NAO_TRADUZIDA =
        "So, you're saying the Titans went to Side Four to prepare a colony drop?";

    private String tela(List<String> linhas) {
        return String.join("\n", linhas);
    }

    /**
     * O caso REAL do Zeta S01E17 #3: concordância. O português está certo e o inglês é só
     * referência — a tela não pode dar a entender que a fala ficou em inglês.
     */
    @Test
    @DisplayName("achado de concordancia NAO leva selo de ingles")
    void concordanciaNaoLevaSeloDeIngles() {
        String saida = tela(apresentacao.linhas(3, "Dialogue", EN_CONCORDANCIA, PT_CONCORDANCIA,
            List.of("Original usa 'her', mas tradução aponta para masculino")));

        assertFalse(saida.contains("AINDA EM INGLÊS"),
            "o português está CORRETO aqui; marcar como inglês é alarme falso");
        assertTrue(saida.contains(AnsiCores.DIM + "referência EN: "),
            "o inglês tem de sair rotulado como referência e apagado, para o olho pular");
    }

    /** O caso REAL do Zeta S01E25 #9: a fala está mesmo em inglês. */
    @Test
    @DisplayName("fala realmente em ingles leva selo e destaque")
    void falaEmInglesLevaSelo() {
        String saida = tela(apresentacao.linhas(9, "Dialogue", EN_NAO_TRADUZIDA, EN_NAO_TRADUZIDA,
            List.of("Fala não traduzida (idêntica ao original em inglês): " + EN_NAO_TRADUZIDA)));

        assertTrue(saida.contains("AINDA EM INGLÊS"),
            "esta é a que o operador precisa enxergar sem ler o motivo");
        assertTrue(saida.contains(AnsiCores.RED),
            "a linha da legenda tem de destoar quando ELA é o defeito");
    }

    /**
     * A ASSERÇÃO QUE IMPORTA, e a que faltava: os dois casos têm de sair DIFERENTES. Enquanto
     * saíam iguais, nenhuma outra guarda acusava — porque o dado estava certo nos dois.
     */
    @Test
    @DisplayName("os dois casos NAO podem render a mesma tela")
    void osDoisCasosSaoVisualmenteDistintos() {
        String concordancia = tela(apresentacao.linhas(3, "Dialogue",
            EN_CONCORDANCIA, PT_CONCORDANCIA,
            List.of("Original usa 'her', mas tradução aponta para masculino")));
        String emIngles = tela(apresentacao.linhas(3, "Dialogue",
            EN_NAO_TRADUZIDA, EN_NAO_TRADUZIDA,
            List.of("Fala não traduzida (idêntica ao original em inglês): " + EN_NAO_TRADUZIDA)));

        assertNotEquals(marcacaoDe(concordancia), marcacaoDe(emIngles),
            "a MARCAÇÃO dos dois casos tem de diferir, não só o texto: era exatamente por saírem "
                + "com a mesma cor e o mesmo formato que o operador leu 8 achados corretos como "
                + "se fossem falas não traduzidas");
    }

    /**
     * Só a marcação (selo e cores), sem o texto — é isso que o olho pega ao rolar a tela.
     *
     * <p>A PRIMEIRA versão deste extrator usava {@code [^\[\];0-9m]}, que preserva o
     * {@code m} e os dígitos DO TEXTO. As duas saídas diferiam por causa das letras de
     * "masculino" e "Amuro", não da cor — e o teste passava por motivo errado. A mutação de
     * 17/08/2026 (selo desligado) expôs isso: só um dos três testes reprovou, quando os dois
     * deviam. <b>Extrair de menos é o mesmo que não medir.</b>
     */
    private static String marcacaoDe(String saida) {
        StringBuilder codigos = new StringBuilder();
        java.util.regex.Matcher m =
            java.util.regex.Pattern.compile("\\u001B\\[[0-9;]*m").matcher(saida);
        while (m.find()) {
            codigos.append(m.group());
        }
        return codigos + (saida.contains("AINDA EM INGLÊS") ? "|SELO" : "|");
    }

    /** Motivos nulos não podem derrubar a tela nem inventar selo. */
    @Test
    @DisplayName("motivos nulos nao inventam selo nem lancam")
    void motivosNulosNaoInventamSelo() {
        String saida = tela(apresentacao.linhas(1, "Default", "A", "B", null));

        assertFalse(saida.contains("AINDA EM INGLÊS"));
        assertFalse(apresentacao.aindaEmIngles(null));
    }
}
