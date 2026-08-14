package org.traducao.projeto.core.texto.dicionarioOrtografia;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.qualidadeTraducao.application.NormalizadorAcentosComuns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: garante o acento de {@code são} e {@code vão} — as duas formas de três
 * letras que escapavam de TODOS os mecanismos — e congela a razão de o conserto ter sido feito na
 * lista nominal e não no filtro do dicionário.
 *
 * <h2>O prejuízo medido</h2>
 * Tradução do 86 em 14/08/2026, contra a de 07/08, só no diálogo:
 * <pre>
 *   sao (por "são")     nova = 48   anterior = 1
 *   vao (por "vão")     nova = 11   anterior = -
 *   nao, voce, entao    nova =  0   anterior = 0   (já estavam na lista nominal)
 * </pre>
 *
 * <h2>A tentativa que a suíte REPROVOU, e por isso está registrada aqui</h2>
 * O caminho óbvio era baixar o filtro de {@code CorretorAcentoPorDicionario} de quatro para três
 * letras. A medição de risco parecia autorizar: das 151 formas de três letras do diálogo, o
 * hunspell só desconhece 17, e a regra de identidade rejeitaria as perigosas
 * ({@code san->sã} — é <i>San Magnólia</i> —, {@code six->sic}, {@code two->tão}).
 *
 * <p>A suíte derrubou em um assert: {@code mae} vira {@code mãe}, e {@code mae} é <b>前</b> em
 * romaji — dano já medido em 100 ocorrências nos 50 episódios do Unicorn. <b>A medição de risco
 * tinha olhado só o português</b>, e este acervo é bilíngue por natureza. Lista nominal não tem
 * esse problema: ela não alcança palavra que não está nela.
 *
 * <h2>Comportamento em caso de falha</h2>
 * A parte do dicionário PULA sem hunspell; a da lista nominal roda sempre, por ser determinística.
 */
@DisplayName("acento de são/vão: lista nominal, não filtro de comprimento")
class PalavraDeTresLetrasTest {

    private final NormalizadorAcentosComuns normalizador = new NormalizadorAcentosComuns();

    /** As duas formas REAIS que escaparam no 86, com as contagens medidas. */
    @Test
    @DisplayName("CASO DOENTE: as 59 ocorrências do 86 — sao -> são e vao -> vão")
    void asDuasFormasQueEscaparamNo86() {
        assertEquals("As forças inimigas são unidades.",
            normalizador.normalizar("As forças inimigas sao unidades."),
            "'sao' não foi acentuada — 48 ocorrências chegaram assim à legenda do 86");
        assertEquals("Eles vão atacar o setor.",
            normalizador.normalizar("Eles vao atacar o setor."),
            "'vao' não foi acentuada — 11 ocorrências no 86");
    }

    @Test
    @DisplayName("a caixa é preservada: Sao -> São")
    void caixaPreservada() {
        assertTrue(normalizador.normalizar("Sao os últimos.").startsWith("São"),
            normalizador.normalizar("Sao os últimos."));
    }

    /**
     * O CASO-CONTROLE do nome da obra. {@code San} de <i>San Magnólia</i> NÃO está na lista e não
     * pode ser tocado — o hunspell sugeriria {@code sã} se alguém voltasse a mexer no filtro de
     * comprimento.
     */
    @Test
    @DisplayName("CASO SÃO: San Magnólia intacta — 'san' não está na lista")
    void nomeDaObraIntacto() {
        String fala = "A República de San Magnólia resiste.";
        assertEquals(fala, normalizador.normalizar(fala),
            "'san' foi alterada — é o cenário do 86 e apareceria corrompida em toda ocorrência");
    }

    /**
     * O ROMAJI QUE A LISTA NÃO ALCANÇA. Note o que NÃO está aqui: {@code mae}.
     *
     * <p>{@code mae -> mãe} JÁ está na lista nominal, de propósito, e é correto no diálogo. O
     * dano de 100 ocorrências no Unicorn veio de aplicá-lo em faixa de MÚSICA, onde {@code mae} é
     * 前 — e a solução de lá foi pular estilo musical, não tirar a palavra da lista.
     *
     * <p>É exatamente por isso que baixar o filtro do dicionário para três letras foi reprovado:
     * lá o alcance é o vocabulário inteiro do hunspell, sem lista para auditar. Aqui o conjunto é
     * nominal e cada entrada foi decidida uma a uma.
     */
    @Test
    @DisplayName("romaji fora da lista continua intacto")
    void romajiForaDaListaIntacto() {
        String fala = "kimi kokoro yume kai";
        assertEquals(fala, normalizador.normalizar(fala),
            "romaji que não está na lista nominal foi alterado — a lista passou a alcançar o que "
                + "não deve: " + normalizador.normalizar(fala));
    }

    /** O que a lista já cobria continua valendo. */
    @Test
    @DisplayName("as formas antigas da lista continuam funcionando")
    void semRegressaoNaLista() {
        String saida = normalizador.normalizar("voce nao viu, entao tambem nao sabe.");
        assertTrue(saida.contains("você") && saida.contains("não")
            && saida.contains("então") && saida.contains("também"), saida);
    }

    /** E o dicionário, que cuida das longas, não pode ter mudado de comportamento. */
    @Test
    @DisplayName("o dicionário continua cuidando das palavras longas")
    void dicionarioIntactoNasLongas() {
        var c = new CorretorOrtograficoLegenda();
        String saida = c.corrigir("Um evento fatidico no espaco aereo.");
        Assumptions.assumeTrue(c.disponivel(), "hunspell ausente — NÃO VERIFICADO");
        assertTrue(saida.contains("fatídico"), saida);
        assertTrue(saida.contains("aéreo"), saida);
    }
}
