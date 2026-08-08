package org.traducao.projeto.traducaoKaraoke.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.traducaoKaraoke.domain.ClasseLinhaKaraoke;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: a regra de Paulo para letra de música, em 2026-08-07 —
 * <b>"jamais mexe no japonês, só no inglês"</b>.
 *
 * <h2>O prejuízo que originou</h2>
 * O corretor rodou em duas obras no mesmo dia:
 * <ul>
 *   <li><b>86</b> — letra em frases inteiras: 2.156 linhas alteradas, <b>0 erros</b>;</li>
 *   <li><b>Guilty Crown</b> — letra quebrada UMA PALAVRA por linha: 15 alterações,
 *       <b>15 erradas</b>. {@code yasashikatta} virou "era legal" e {@code Kizuite} virou
 *       "Crow é o nome de palco" — alucinação puxada da lore da própria obra.</li>
 * </ul>
 * A causa: o desempate por proporção exige {@code MINIMO_PALAVRAS_DESEMPATE} palavras, e a
 * linha curta caía no ramo que TRADUZ. Em dúvida, o classificador mexia no japonês — o
 * inverso da regra, e o inverso de falhar fechado.
 *
 * <h2>Por que o critério é a LÍNGUA, e só ela</h2>
 * Medido nas quatro obras do acervo em 07/08/2026, e nenhum outro sinal sobrevive:
 *
 * <table>
 *   <tr><th>obra</th><th>romaji</th><th>inglês</th></tr>
 *   <tr><td>Guilty Crown</td><td>{@code OP Roma}, TOPO</td><td>{@code OP}, BASE</td></tr>
 *   <tr><td>Gundam Unicorn</td><td>{@code ED}, BASE</td><td>{@code ED - EN}, TOPO</td></tr>
 *   <tr><td>Gundam 08th MS Team</td><td>{@code Song JP}, TOPO</td><td>{@code Song ENG}, BASE</td></tr>
 *   <tr><td>86</td><td>—</td><td>{@code Opening}, TOPO</td></tr>
 * </table>
 *
 * A posição está <b>invertida</b> entre Guilty Crown e Unicorn; o nome {@code ED} é romaji no
 * Unicorn e inglês no Guilty Crown. Decidir por posição inverteria uma obra inteira; decidir
 * por nome de estilo inverteria outra. Só o CONTEÚDO vale — é a mesma conclusão da decisão de
 * 2026-07-25 ("romaji é LÍNGUA, não estilo nem fonte").
 */
class JamaisMexerNoJaponesTest {

    private final ClassificadorLetraKaraokeService classificador =
        new ClassificadorLetraKaraokeService(new DetectorEfeitoKaraokeService());

    /**
     * PROPÓSITO DE NEGÓCIO: o caso que produziu as 15 alterações erradas. Linha de UMA palavra
     * romaji, num estilo que o detector reconhece como musical.
     */
    @Test
    @DisplayName("palavra romaji SOZINHA e japones — nao se traduz")
    void palavraRomajiSozinhaEhJapones() {
        for (String verso : new String[]{"yasashikatta", "Kizuite", "kieta", "tsunagu", "kibou"}) {
            assertEquals(ClasseLinhaKaraoke.ORIGINAL_JAPONES,
                classificador.classificar("ED Roma L1", verso),
                "\"" + verso + "\" e japones: linha curta NAO pode cair no ramo que traduz. "
                    + "Foi assim que 'yasashikatta' virou 'era legal' no Guilty Crown.");
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CONTRA-TESTE. A guarda não pode engolir letra original em inglês —
     * senão o corretor para de traduzir justamente o que existe para traduzir.
     */
    @Test
    @DisplayName("letra em ingles continua traduzivel, curta ou longa")
    void letraInglesaContinuaTraduzivel() {
        assertEquals(ClasseLinhaKaraoke.TRADUZIVEL_INGLES,
            classificador.classificar("Song ENG", "You see the dream shining within the storm."),
            "verso do OP do Gundam 08th MS Team");
        assertEquals(ClasseLinhaKaraoke.TRADUZIVEL_INGLES,
            classificador.classificar("Opening", "No matter how hard I wish, nothing ever changes"),
            "verso do OP do 86");
        assertEquals(ClasseLinhaKaraoke.TRADUZIVEL_INGLES,
            classificador.classificar("ED", "Then I can breathe in so deep"),
            "verso do ED do Unicorn — o estilo se chama ED e mesmo assim e ingles");
        assertEquals(ClasseLinhaKaraoke.TRADUZIVEL_INGLES,
            classificador.classificar("ED2", "Behind your mask"),
            "tres palavras, ingles curto");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: caracteriza um comportamento que JÁ EXISTIA e que a guarda nova
     * NÃO alterou — "One more time, one more chance" é preservado.
     *
     * <p>Parece contraintuitivo: a linha é inglês e mesmo assim não vai ao LLM. Mas
     * {@code ORIGINAL_JAPONES} aqui quer dizer <b>letra original, preserva</b>, e essa letra é
     * a canção em si — não a camada de tradução que o fansub acrescentou. Traduzi-la seria
     * reescrever a música, não legendá-la.
     *
     * <p>Todas as palavras são silabáveis em Hepburn ({@code o-ne}, {@code mo-re},
     * {@code ti-me}), e nenhuma está em {@code INGLES_FORTE} — então o desempate por fração já
     * a mandava para cá antes de 07/08/2026. Escrevi este teste esperando o contrário e ele
     * reprovou, o que impediu de eu "consertar" um comportamento correto.
     *
     * <p>CONSEQUÊNCIA MEDIDA: como nenhuma dessas palavras é sinal de inglês, uma delas SOZINHA
     * numa linha também é preservada. {@code MedicaoLinhaCurtaKaraokeIT} encontrou o caso real:
     * {@code One,} do ED do 86, que o corretor traduzia como "Um" e hoje mantém em inglês. É a
     * única perda entre as 1.658 entradas do cache — o preço aceito por Paulo para nunca mexer
     * no japonês. Ver {@link #letraInglesaContinuaTraduzivel}: com duas palavras ou mais e
     * qualquer sinal gramatical de inglês, a tradução volta a acontecer.
     */
    @Test
    @DisplayName("letra ORIGINAL em ingles silabavel e preservada — comportamento anterior, intacto")
    void letraOriginalInglesaSilabavelEhPreservada() {
        assertEquals(ClasseLinhaKaraoke.ORIGINAL_JAPONES,
            classificador.classificar("Insert Song", "One more time, one more chance"),
            "ORIGINAL_JAPONES aqui significa 'letra original, preserva' — traduzir seria "
                + "reescrever a cancao. A guarda nova nao pode mudar isto.");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: caracteriza o CUSTO da regra, para que ele seja uma decisão visível e
     * não uma surpresa. Palavra inglesa curta e silabável, sozinha na linha, é preservada.
     *
     * <p>Isto NÃO é defeito a corrigir: é o preço que Paulo aceitou em 07/08/2026 — <i>"ainda que
     * ganhemos partes do karaokê japonês em inglês"</i>. Com uma palavra só não existe evidência
     * suficiente para distinguir {@code One} de {@code Kizuite}, e a assimetria de dano decide:
     * inglês preservado por engano fica em inglês (recuperável); japonês traduzido por engano vira
     * "Crow é o nome de palco" (letra destruída com alucinação de lore).
     *
     * <p>MEDIDO: {@code MedicaoLinhaCurtaKaraokeIT} sobre as 1.658 entradas do cache de karaokê
     * achou <b>5</b> textos distintos que a guarda passou a preservar, e {@code One,} é o único
     * que era acerto antes. Quem quiser recuperá-lo precisa de um sinal novo (o par de camadas
     * por tempo, por exemplo), NÃO de baixar a guarda.
     */
    @Test
    @DisplayName("CUSTO ACEITO: palavra inglesa curta e silabavel sozinha fica sem traduzir")
    void palavraInglesaCurtaSilabavelSozinhaEhPreservada() {
        for (String palavra : new String[]{"One,", "Shine", "take", "name"}) {
            assertEquals(ClasseLinhaKaraoke.ORIGINAL_JAPONES,
                classificador.classificar("Ending", palavra),
                "\"" + palavra + "\" fica sem traducao — preco aceito da regra. Se este teste "
                    + "reprovar, alguem baixou a guarda e o romaji de uma palavra voltou a ser "
                    + "traduzido: confira JamaisMexerNoJaponesTest.palavraRomajiSozinhaEhJapones.");
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: verso do Sawano que MISTURA as duas línguas. Pela regra, qualquer
     * japonês na linha manda preservar — inglês solto dentro de verso romaji faz parte do
     * verso original.
     */
    @Test
    @DisplayName("verso MISTO com japones preserva")
    void versoMistoComJaponesPreserva() {
        assertEquals(ClasseLinhaKaraoke.ORIGINAL_JAPONES,
            classificador.classificar("ED", "Kiraina otona no kage ga kasanaru you ni kieta"),
            "verso do ED do Unicorn, romaji puro");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: música NO MEIO do episódio — insert song, não abertura nem
     * encerramento. Levantado por Paulo em 07/08/2026: <i>"pode aparecer karaokê no meio do
     * filme ou animação, então temos de ter uma guarda para não traduzir de forma alguma o
     * japonês"</i>.
     *
     * <p>INVARIANTE QUE ESTE TESTE TRAVA: a decisão é por ESTILO e CONTEÚDO, nunca por tempo,
     * posição no arquivo ou índice do evento. Uma insert song no minuto 12 recebe exatamente o
     * mesmo tratamento do ED — não existe, e não pode passar a existir, nenhum atalho do tipo
     * "só protege o que está no começo/fim". Os nomes abaixo são os que o acervo realmente usa
     * ({@code Insert Song} no DanMachi, {@code Other songs} no Guilty Crown).
     */
    @Test
    @DisplayName("insert song NO MEIO do episodio: japones preservado igual ao OP/ED")
    void insertSongNoMeioDoEpisodioPreservaJapones() {
        for (String estilo : new String[]{"Insert Song", "Other songs", "Song", "Theme", "Music"}) {
            assertEquals(ClasseLinhaKaraoke.ORIGINAL_JAPONES,
                classificador.classificar(estilo, "kimi no koe ga kikoeru"),
                "romaji em \"" + estilo + "\" tem de ser preservado — a guarda nao pode "
                    + "depender de o estilo se chamar OP/ED nem de a musica estar no comeco");
            assertEquals(ClasseLinhaKaraoke.ORIGINAL_JAPONES,
                classificador.classificar(estilo, "遠くまで届くように"),
                "kana/kanji em \"" + estilo + "\" tem de ser preservado");
            assertEquals(ClasseLinhaKaraoke.ORIGINAL_JAPONES,
                classificador.classificar(estilo, "tsutaetai"),
                "UMA palavra romaji em \"" + estilo + "\" — o caso que quebrou no Guilty Crown");
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: escrita japonesa de verdade (kana/kanji) nunca esteve em risco,
     * mas o teste trava o caminho mais óbvio.
     */
    @Test
    @DisplayName("kana e kanji sao japones")
    void kanaEKanjiSaoJapones() {
        assertEquals(ClasseLinhaKaraoke.ORIGINAL_JAPONES,
            classificador.classificar("Song JP", "嵐の中で輝いて"));
    }
}
