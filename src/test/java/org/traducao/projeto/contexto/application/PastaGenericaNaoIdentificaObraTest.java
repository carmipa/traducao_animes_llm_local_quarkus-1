package org.traducao.projeto.contexto.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.contexto.domain.VeredictoObraContexto;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que o portão distingue "obra ainda sem lore cadastrada" de
 * "a pasta não diz que obra é esta" — dois casos que caíam no MESMO aviso e pedem consertos
 * opostos (cadastrar a lore × renomear a pasta).
 *
 * <h2>O prejuízo que originou</h2>
 * Medido em 07/08/2026: metade das pastas de legenda do acervo tinha um nível de temporada
 * entre a obra e o arquivo, e a obra derivada do caminho saía {@code "Season 05"}. O aviso
 * dizia "não é reconhecida por nenhum contexto registrado — confirme que é a lore certa",
 * mandando o operador procurar uma lore chamada "Season 05" que nunca vai existir. E calava
 * sobre o dano real: o cache ia para {@code cache/Season 5/}, onde já havia 22 arquivos do
 * Gundam Unicorn.
 *
 * <h2>Por que o contra-teste é a parte que importa</h2>
 * Um padrão largo demais transformaria {@code "Guilty Crown OVA"} e {@code "DanMachi Season 05"}
 * — nomes LEGÍTIMOS, que o catálogo reconhece — em "pasta genérica", e o operador passaria a
 * ver aviso de renomear pasta em obra corretamente organizada. Por isso o casamento é do nome
 * INTEIRO normalizado, e por isso metade deste teste são nomes reais do acervo que NÃO podem
 * casar.
 */
class PastaGenericaNaoIdentificaObraTest {

    private final ValidadorCompatibilidadeObraContexto validador =
        new ValidadorCompatibilidadeObraContexto();

    /**
     * PROPÓSITO DE NEGÓCIO: os rótulos estruturais que apareceram no acervo real, mais as
     * variações previsíveis da mesma convenção.
     */
    @Test
    @DisplayName("rotulo estrutural e reconhecido como pasta generica")
    void rotuloEstruturalEhGenerico() {
        for (String nome : new String[]{
            "Season 05", "Season 5", "season 1", "SEASON 12",
            "Temporada 3", "Temp 2", "S01", "S1",
            "Movie", "Movies", "Filme", "Filmes", "Movie 2",
            "OVA", "OVAs", "OAD", "Especiais", "Specials", "Special",
            "Extras", "Extra", "Bonus", "Part 2", "Parte 1",
            "Disc 1", "Disco 2", "CD1", "Vol 3", "Volume 1"
        }) {
            assertTrue(validador.pastaGenerica(nome),
                "\"" + nome + "\" e rotulo estrutural: qualquer obra do acervo pode ter uma "
                    + "pasta com esse nome, entao ele nao identifica obra alguma");
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CONTRA-TESTE. Todos estes são nomes REAIS de pastas do acervo em
     * 07/08/2026, e todos identificam obra. Se algum casar, o operador passa a ver aviso de
     * "renomeie a pasta" justamente onde a pasta já está certa.
     */
    @Test
    @DisplayName("nome real de obra do acervo NUNCA e generico, mesmo contendo a palavra")
    void nomeRealDeObraNaoEhGenerico() {
        for (String nome : new String[]{
            "DanMachi Season 00", "DanMachi Season 01", "DanMachi Season 05",
            "DanMachi Sword Oratoria", "DanMachi Arrow of the Orion",
            "Guilty Crown OVA", "Gundam Unicorn Season 1",
            "Break Blade 1", "Break Blade 6",
            "Memories (1995)", "Mobile Suit Gundam ZZ", "Mobile Suit Zeta Gundam",
            "86 Part 1",
            "[Joseki] Mobile Suit Gundam Char's Counterattack (1988)(BD AV1 Opus)[Eng Sub]",
            "[Sokudo] DanMachi",
            "[P9] Mobile Police Patlabor - Early Days (BD 1080p HEVC FLAC) [Dual-Audio]"
        }) {
            assertFalse(validador.pastaGenerica(nome),
                "\"" + nome + "\" identifica obra e NAO pode ser tratado como generico — "
                    + "o casamento e do nome INTEIRO, nao de pedaco");
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o aviso da pasta genérica precisa ser OUTRO TEXTO, senão a
     * distinção não chega a quem lê o console — que é o único ponto onde ela serve para algo.
     */
    @Test
    @DisplayName("as duas mensagens sao diferentes e cada uma indica o seu conserto")
    void mensagensSaoDistintasEIndicamOConserto() {
        String generica = validador.mensagemDePastaGenerica("Season 05", "danmachi_s5");
        String semLore = validador.mensagemDeIndeterminacao("Memories (1995)", "danmachi_s5");

        assertNotEquals(semLore, generica);

        assertTrue(generica.contains("cache/Season 05/"),
            "o aviso tem de nomear o diretorio de cache: a colisao e o dano silencioso, e e a "
                + "unica parte que o operador nao enxerga sozinho. Veio: " + generica);
        assertTrue(generica.contains("Renomeie a pasta"),
            "o conserto da pasta generica e RENOMEAR, nao cadastrar lore. Veio: " + generica);
        assertTrue(semLore.contains("Confirme que é a lore certa"),
            "o conserto da obra sem lore continua sendo conferir a lore. Veio: " + semLore);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: pasta genérica AVISA, não bloqueia. Bloquear pararia a tradução de
     * quem organiza o acervo assim — e a guarda existe para impedir lore errada, não para impor
     * convenção de nome de pasta.
     */
    @Test
    @DisplayName("pasta generica segue INDETERMINADO: avisa, nao bloqueia")
    void pastaGenericaNaoBloqueia() {
        assertEquals(VeredictoObraContexto.INDETERMINADO,
            validador.avaliar("Season 05", "danmachi_s5", Set.of()),
            "nenhum contexto reconhece \"Season 05\", entao o veredicto e INDETERMINADO e a "
                + "traducao SEGUE — a mudanca e so de mensagem");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: entrada ausente não pode virar acusação. Sem nome não há como
     * afirmar que a pasta é genérica, e o aviso comum já cobre o caso.
     */
    @Test
    @DisplayName("nulo e branco nao sao genericos")
    void nuloEBrancoNaoSaoGenericos() {
        assertFalse(validador.pastaGenerica(null));
        assertFalse(validador.pastaGenerica(""));
        assertFalse(validador.pastaGenerica("   "));
    }
}
