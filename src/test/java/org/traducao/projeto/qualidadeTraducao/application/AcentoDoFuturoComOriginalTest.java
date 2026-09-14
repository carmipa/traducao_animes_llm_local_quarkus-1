package org.traducao.projeto.qualidadeTraducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: prova que o futuro do presente escrito sem acento é reposto quando o
 * ORIGINAL INGLÊS prova que a fala está no futuro, e que o mais-que-perfeito legítimo continua
 * intocado. {@code acabara} sem acento é palavra válida em português — nenhum corretor
 * ortográfico a acusa, porque não há nada de errado com ela; está errado o TEMPO.
 *
 * <h2>Por que nenhum dicionário resolve</h2>
 * {@code NormalizadorAcentosComuns} só age sobre forma cuja grafia sem acento NUNCA é palavra, e
 * recusa esta classe com razão — tocá-la sem contexto estragaria todo mais-que-perfeito. O
 * contexto que falta está no original, e o marcador de futuro do inglês o fornece de forma
 * verificável. Nesta máquina o dicionário hunspell está indisponível
 * ({@code disponivel() == false}), o que torna a rota do dicionário <b>não verificada</b> — e o
 * desenho não depende dela.
 *
 * <h2>O prejuízo MEDIDO — 2026-09-14, acervo publicado</h2>
 * 134 pares casados pela chave completa do evento ASS (9 campos). Denominador: <b>20</b> falas
 * publicadas com uma das formas da lista.
 * <pre>
 * corrige ........................... 19
 * abstem, sem marcador de futuro ..... 1   ("he'd probably die" e condicional, nao futuro)
 * abstem por "had" ................... 0   (nao existe no acervo)
 * </pre>
 *
 * <h2>O defeito do meu próprio instrumento, que vale registrar</h2>
 * A primeira versão do padrão de futuro exigia fronteira à esquerda e por isso perdia
 * {@code you'll}, {@code We'll} e {@code he'll} — a forma mais comum do futuro na fala. Três das
 * quatro abstenções eram isso, e eu quase declarei "sem marcador" onde o marcador estava.
 *
 * <h2>Caso-controle de fronteira (A1)</h2>
 * Os controles legítimos carregam o MESMO sinal superficial do defeito:
 * <ul>
 *   <li><b>Reais, do acervo:</b> as duas ocorrências de {@code vira}, que estão corretas. Uma
 *       delas tem {@code we'll} numa oração vizinha, e é ela que prova por que {@code vira} ficou
 *       fora da lista: {@code vira} é presente de <i>virar</i>, {@code virá} é futuro de
 *       <i>vir</i> — verbos diferentes.</li>
 *   <li><b>Sintético e declarado:</b> o mais-que-perfeito com {@code had}. Não existe nenhuma
 *       ocorrência no acervo, então o caso é construído, e o gabarito vem da gramática — que é
 *       fundamento fora da implementação, como a A3 exige.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Falha de asserção mostrando o texto obtido.
 */
@DisplayName("Acento do futuro reposto pelo original ingles -- e o mais-que-perfeito intocado")
class AcentoDoFuturoComOriginalTest {

    private final CorretorHomografoComOriginal corretor = new CorretorHomografoComOriginal();

    @ParameterizedTest(name = "[{index}] {1} -> {2}")
    @CsvSource(delimiter = '|', value = {
        "This war will end someday.                       | Um dia, esta guerra acabara.                  | Um dia, esta guerra acabará.",
        "Their counterattack will be here soon!            | Seu contra-ataque chegara em breve!           | Seu contra-ataque chegará em breve!",
        "Today, at 2100 hours, the Albion will set out...  | Hoje, o Albion partira...                     | Hoje, o Albion partirá...",
        "Really? Your mother will be so happy.             | Realmente? Sua mae ficara tao feliz.          | Realmente? Sua mae ficará tao feliz.",
        "If the worst happens, you'll die with us!         | Se o pior acontecer, voce morrera conosco!    | Se o pior acontecer, voce morrerá conosco!",
        "We'll start moving out right before dawn.         | Nossa movimentacao começara antes do amanhecer| Nossa movimentacao começará antes do amanhecer",
        "The quarantine won't last that long.              | A quarentena nao durara tanto tempo.          | A quarentena nao durará tanto tempo.",
        "I'm sure he'll be mad as hell about it.           | Tenho certeza de que ele ficara furioso.      | Tenho certeza de que ele ficará furioso.",
    })
    @DisplayName("CORRIGE: marcador de futuro no original repoe o acento")
    void futuroNoOriginalRepoeOAcento(String original, String traduzido, String esperado) {
        assertEquals(esperado, corretor.corrigir(original, traduzido),
            "o marcador de futuro do ingles prova que a forma sem acento esta no tempo errado");
    }

    @Test
    @DisplayName("CORRIGE os DOIS verbos da mesma fala")
    void doisVerbosNaMesmaFala() {
        assertEquals("Fido ficará e se esconderá.",
            corretor.corrigir("Fido will stay and hide.", "Fido ficara e se escondera."),
            "a fala tem duas formas alvo e as duas estao no futuro");
    }

    @Test
    @DisplayName("CORRIGE preservando a CAIXA: o acento herda a caixa da letra que substitui")
    void caixaAltaPreservada() {
        assertEquals("A GUERRA ACABARÁ UM DIA",
            corretor.corrigir("THE WAR WILL END SOMEDAY", "A GUERRA ACABARA UM DIA"),
            "legenda em caixa alta existe, e 'ACABARá' seria pior que o defeito");
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @CsvSource(delimiter = '|', value = {
        // SINTETICO e declarado: nao existe fala com "had" no acervo. Gabarito pela gramatica.
        "He said the siege had lasted three days.                 | Ele disse que o cerco durara tres dias.",
        "She had already arrived when the alarm rang.             | Ela ja chegara quando o alarme soou.",
        // REAL, do acervo: condicional, nao futuro. A traducao certa seria 'morreria'.
        "If he used it again, he'd probably die.                  | Se ele o usar novamente, provavelmente morrera.",
    })
    @DisplayName("ABSTEM: anterioridade e condicional nao sao futuro")
    void semFuturoNoOriginalNaoToca(String original, String traduzido) {
        assertEquals(traduzido, corretor.corrigir(original, traduzido),
            "sem marcador de futuro, ou com 'had', a forma sem acento pode estar CORRETA");
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @CsvSource(delimiter = '|', value = {
        // REAIS, do acervo, e os dois estao CORRETOS. O primeiro tem "we'll" numa oracao vizinha,
        // e e ele que prova por que "vira" nao pode entrar na lista.
        "This thing can't turn on a dime, so we'll have problems. | Essa maquina nao vira rapidamente, entao teremos problemas.",
        "You turn around to pursue the Endlave and end up here.   | Voce vira para perseguir o Endlave e acaba aqui.",
    })
    @DisplayName("CASO-CONTROLE do acervo: 'vira' e presente de virar, nao futuro de vir")
    void viraNaoEhFuturoDeVir(String original, String traduzido) {
        assertEquals(traduzido, corretor.corrigir(original, traduzido),
            "'vira' e 'vira' de virar; acentuar aqui trocaria o VERBO e corromperia traducao boa");
    }

    @Test
    @DisplayName("a correcao do futuro nao depende do verbo 'to be' no original")
    void futuroNaoDependeDoVerboSer() {
        // "This war will end someday." nao tem is/are/was. Se a correcao do futuro estivesse
        // atras do portao do VERBO_SER_INGLES, esta fala sairia sem correcao.
        String original = "This war will end someday.";
        assertEquals("Um dia, a guerra acabará.",
            corretor.corrigir(original, "Um dia, a guerra acabara."),
            "o portao do 'to be' serve ao 'e'/'esta', nao ao futuro");
    }
}
