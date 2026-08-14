package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: impede que fala em FRANCÊS seja dada por "já traduzida" e gravada como
 * está — o pior desfecho do pipeline, porque não vira nem pendência.
 *
 * <h2>O prejuízo medido que originou esta guarda</h2>
 * Em 14/08/2026, nas faixas francesas dos três filmes do {@code Memories (1995)}:
 * <pre>
 *   FR  Magnetic Rose    423 falas   208 no total dos três seriam PULADAS
 *   FR  Stink Bomb       376 falas   -> 21,6% do filme ficaria em francês
 *   FR  Cannon Fodder    164 falas
 *   EN  (os três)       2402 falas   0 puladas (0,0%)  <- caso-controle
 * </pre>
 * O inglês dando zero é o que prova que o furo era do francês, não do detector em geral.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Fala francesa NUNCA é "já no idioma-alvo".</li>
 *   <li>Fala portuguesa legítima CONTINUA sendo — o dano oposto seria retraduzir português bom,
 *       gastando LLM e arriscando eco. Metade dos testes abaixo existe só para isso.</li>
 *   <li>Nenhum homógrafo entre francês e português entra na lista de sinal francês.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Reprovar aqui significa que uma das duas classes voltou: francês gravado como português, ou
 * português mandado de volta ao LLM.
 */
@DisplayName("fonte francesa: não é português, e o português continua sendo")
class FonteFrancesaNaoEhPortuguesTest {

    private final DetectorIdiomaFonteService detector = new DetectorIdiomaFonteService();

    /** As falas REAIS que a medição pegou sendo classificadas como português. */
    @Test
    @DisplayName("CASO DOENTE: as falas do Memories que eram dadas por traduzidas")
    void falasFrancesasReaisNaoSaoPortugues() {
        for (String fala : new String[] {
                "Nous rentrons à la base.",
                "Je reviens bientôt, Cécile.",
                "La gravité l'a rattrapé avant\\Nqu'il puisse s'enfoncer dans l'espace.",
                "Le récepteur est\\Nla seule chose qui soit récupérable.",
                "L'objet dérivant sur la trajectoire\\NPZ-302 a été détruit.",
                "On a détecté des débris d'astéroïdes\\Nà la dérive, là-bas.",
                "On est fatigués.\\NLe Corona est fatigué aussi.",
                "Bien sûr, encore faut-il trouver\\Nune épave de nos jours.",
                "Heintz, parle des épaves à Aoshima.",
                "C'est à cause de notre boulot\\Net parce qu'on est bloqués ici."}) {
            assertFalse(detector.jaNoIdiomaAlvo(fala, "pt-br"),
                "FRANCÊS DADO POR PORTUGUÊS — esta fala seria gravada como está, em francês, "
                    + "sem virar pendência: " + fala);
        }
    }

    /**
     * A elisão sozinha tem de bastar. É o que pega a fala curta sem nenhuma stopword da lista —
     * e o português não elide com apóstrofo, então não há colisão a temer.
     */
    @Test
    @DisplayName("elisão com apóstrofo basta, mesmo sem stopword na fala")
    void elisaoSozinhaBasta() {
        assertFalse(detector.jaNoIdiomaAlvo("L'épave n'était qu'illusion.", "pt-br"));
        assertFalse(detector.jaNoIdiomaAlvo("C'était magnifique.", "pt-br"));
        assertFalse(detector.jaNoIdiomaAlvo("D'accord, madame.", "pt-br"));
    }

    /**
     * A elisão COLADA NA QUEBRA, que foi o furo apontado pela {@code CatracaFronteiraQuebraAssTest}
     * na primeira versão deste detector: {@code \N} termina em {@code N}, que é LETRA, então um
     * lookbehind comum enxerga o {@code qu'} de {@code "avant\Nqu'il"} como continuação de palavra
     * e não dispara. E é exatamente assim que a fala aparece na legenda real do Memories.
     */
    @Test
    @DisplayName("elisão colada na quebra \\N não fica invisível")
    void elisaoColadaNaQuebra() {
        assertFalse(detector.jaNoIdiomaAlvo("La gravité l'a rattrapé avant\\Nqu'il puisse fuir.", "pt-br"),
            "elisão colada em \\N ficou invisível — usar lookbehind próprio em vez de "
                + "FronteiraTermoAss.INICIO reintroduz este furo");
        assertFalse(detector.jaNoIdiomaAlvo("Transmettez\\Nc'est urgent.", "pt-br"));
    }

    /**
     * O CASO-CONTROLE que protege o outro lado. Estas falas são português de verdade e precisam
     * continuar sendo reconhecidas; classificá-las como estrangeiras mandaria fala boa de volta
     * ao LLM, com custo e risco de eco.
     */
    @Test
    @DisplayName("CASO SÃO: português legítimo CONTINUA sendo reconhecido")
    void portuguesContinuaSendoPortugues() {
        for (String fala : new String[] {
                "Você não viu o que aconteceu aqui.",
                "A situação da colônia é crítica.",
                "Então vamos embora agora mesmo.",
                "Isso é tudo o que eu sei, obrigado.",
                "Ela está muito cansada, coitada.",
                "Nós já falamos sobre isso, você lembra?"}) {
            assertTrue(detector.jaNoIdiomaAlvo(fala, "pt-br"),
                "PORTUGUÊS DEIXOU DE SER RECONHECIDO — esta fala seria mandada ao LLM de novo, "
                    + "com custo e risco de eco: " + fala);
        }
    }

    /**
     * Português com acentuação farta é onde a colisão seria mais provável, porque o ramo de
     * diacríticos é o mesmo que o francês aciona. Estas falas não têm stopword francesa e
     * precisam continuar passando pelo ramo de acentos.
     */
    @Test
    @DisplayName("português acentuado sem stopword francesa continua passando pelos diacríticos")
    void portuguesAcentuadoNaoColide() {
        assertTrue(detector.jaNoIdiomaAlvo("Trê s décadas de história.".replace(" s", "s"), "pt-br"));
        assertTrue(detector.jaNoIdiomaAlvo("Missão concluída com êxito.", "pt-br"));
    }

    /** O inglês já dava zero na medição e não pode mudar por causa desta alteração. */
    @Test
    @DisplayName("inglês continua não sendo português")
    void inglesContinuaNaoSendoPortugues() {
        assertFalse(detector.jaNoIdiomaAlvo("Gravity well caught it before it could get away.", "pt-br"));
        assertFalse(detector.jaNoIdiomaAlvo("Hell of an old satellite.", "pt-br"));
        assertFalse(detector.jaNoIdiomaAlvo("Want some too, Heinz?", "pt-br"));
    }

    /**
     * Os homógrafos que FICARAM DE FORA da lista francesa, fixados como decisão. Cada um destes é
     * palavra portuguesa comum; se algum entrar na lista, estes asserts caem e a mudança precisa
     * vir com a medição que a autoriza.
     */
    /**
     * AS QUATRO QUE JÁ ENTRARAM POR ENGANO. Não são hipóteses: {@code jamais}, {@code mes} e
     * {@code sera} foram pegas medindo contra legendas PORTUGUESAS do acervo em 14/08/2026, com
     * as falas reais citadas abaixo. Cada uma custou quatro falas boas.
     *
     * <p>As duas do meio ensinam a regra que vale para qualquer adição futura: neste acervo falta
     * acento em parte das traduções, então <b>a candidata precisa ser conferida SEM acento
     * também</b> — {@code mês} vira {@code mes} e {@code será} vira {@code sera}, e as duas formas
     * desacentuadas são francês legítimo.
     */
    @Test
    @DisplayName("as quatro palavras que entraram por engano não podem voltar")
    void asQuatroQueEntraramPorEnganoNaoVoltam() {
        assertTrue(detector.jaNoIdiomaAlvo(
            "E acima de tudo, a Federação jamais abandonará aqueles que lutam ao seu lado.", "pt-br"),
            "'jamais' é português corrente e voltou para a lista francesa");
        assertTrue(detector.jaNoIdiomaAlvo("Nós vamos partir daqui agora mesmo.", "pt-br"),
            "'partir' é português corrente e voltou para a lista francesa");
        assertTrue(detector.jaNoIdiomaAlvo("Em breve, você sera enviado para a batalha.", "pt-br"),
            "'sera' é 'será' sem acento — e o acervo TEM acento faltando");
        assertTrue(detector.jaNoIdiomaAlvo("Eu vos digo que isso não é verdade.", "pt-br"),
            "'vos' é pronome português");
    }

    @Test
    @DisplayName("limite: homógrafos FR/PT ficam fora da lista, e por isso o PT sobrevive")
    void homografosNaoEntramNaLista() {
        assertTrue(detector.jaNoIdiomaAlvo("Vou te dizer o que é isso.", "pt-br"),
            "'te' e 'que' são francês E português — acusá-los mataria a detecção de PT");
        assertTrue(detector.jaNoIdiomaAlvo("Ele se foi, mas você não.", "pt-br"),
            "'se' e 'mais' são homógrafos; a fala é português puro");
    }
}
