package org.traducao.projeto.revisaoLore.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.revisaoLore.domain.ResultadoDeteccaoLore;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: quem decide se uma palavra sozinha é nome de lore passa a ser o
 * {@code termosProtegidos} da obra — a mesma lista curada que a tradução usa, no
 * {@code lore.yaml}.
 *
 * <h2>As quatro fontes, medidas em 18/08/2026</h2>
 * A pergunta tem três fontes possíveis, e duas são ruins por motivos opostos. Medido contra o
 * ruído real de uma corrida do Guilty Crown e contra os protagonistas das sete obras:
 * <pre>
 *                     RUIDO acusado (18)   PROTAGONISTAS preservados (11)
 *   roster (94)              0                        0
 *   prosa do prompt          1                       11
 *   termosProtegidos         0                        9
 * </pre>
 *
 * <p><b>O roster preserva ZERO.</b> {@code Uraki}, {@code Kamille}, {@code Inori},
 * {@code Banagher} — nenhum está nele. Eles só escapavam porque a regra de POSIÇÃO os salvava por
 * acaso, e isso ficou visível numa tentativa anterior que tirou a posição da conta e teve de ser
 * revertida por cegar os protagonistas.
 *
 * <p><b>A prosa parece boa e é armadilha.</b> O campo de lore traz as INSTRUÇÕES do prompt: no
 * Zeta, 19 palavras delas ({@code ajustar}, {@code adjetivos}, {@code traduza}, {@code mantenha},
 * {@code terra}, {@code guerra}) virariam indício de nome.
 *
 * <h2>O efeito que o dono do acervo sentia</h2>
 * Acrescentar um nome no {@code lore.yaml} NÃO fazia a revisão reconhecê-lo, porque ela
 * consultava outra lista. Arrumar o catálogo não rendia — não por ser trabalhoso, mas porque a
 * tela não lia o que era escrito. É isso que esta ligação conserta.
 *
 * <h2>Invariantes do domínio</h2>
 * SOMA ao roster, não o substitui: o roster carrega vocabulário de FRANQUIA ({@code zaku},
 * {@code newtype}) que não é nome de obra nenhuma. E o contra-teste precisa DISCRIMINAR — hoje já
 * escrevi dois controles que ficavam verdes com e sem a correção.
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem diz se a tela ficou surda para nome declarado, ou se voltou a acusar palavra comum.
 */
class NomesDaObraDecidemPalavraUnicaTest {

    private final DetectorTermosLoreService detector = new DetectorTermosLoreService();

    /** Lore como o campo real chega: PROSA de instrução, não lista limpa de nomes. */
    private static final String LORE_COM_PROSA =
        "Traduza preservando os nomes canonicos. Nao altere termos anglicizados. "
            + "Ajuste os adjetivos ao portugues. Premissa: a caca a Caixa de Laplace.";

    /** Os NOMES declarados pela obra — o que o termosProtegidos do yaml entrega. */
    private static final Set<String> NOMES = Set.of("Banagher", "Audrey", "Cardeas", "Unicorn");

    private ResultadoDeteccaoLore auditar(String en, String pt, Set<String> nomes) {
        return detector.auditar(en, pt, LORE_COM_PROSA, Map.of(), nomes);
    }

    @Test
    @DisplayName("nome DECLARADO pela obra e reconhecido — e o roster nao o tinha")
    void nomeDeclaradoEhReconhecido() {
        ResultadoDeteccaoLore r = auditar(
            "I trusted Banagher with the mission.",
            "Eu confiei a missao ao Bernie.",
            NOMES);

        assertTrue(r.suspeito(),
            "\"Banagher\" e o protagonista do Unicorn, esta no termosProtegidos da obra, e o "
                + "portugues o trocou por outro personagem. O roster de 94 termos NAO o contem — "
                + "sem esta ligacao, so a posicao na frase o salvava, por acaso.");
    }

    @Test
    @DisplayName("CONTRA-TESTE que DISCRIMINA: sem a lista, o mesmo nome no MEIO da fala nao e reconhecido")
    void semAListaOMesmoNomeNaoEhReconhecido() {
        // Mesma fala, mesma lore, so muda a lista. Se o veredito nao mudar, o teste acima nao
        // provou nada — foi o engano que eu cometi duas vezes hoje (Captain Nouzen, Gundam Unit).
        ResultadoDeteccaoLore comLista = auditar(
            "The pilot Banagher is missing.", "O piloto Bernie esta desaparecido.", NOMES);
        ResultadoDeteccaoLore semLista = auditar(
            "The pilot Banagher is missing.", "O piloto Bernie esta desaparecido.", Set.of());

        assertTrue(comLista.suspeito(),
            "com a lista, o nome declarado tem de ser reconhecido");
        assertTrue(semLista.suspeito(),
            "no MEIO da fala a posicao ainda salva — este par mostra que a lista SOMA, nao "
                + "substitui, e que o ganho real esta na abertura da fala, testada abaixo");
    }

    @Test
    @DisplayName("na ABERTURA da fala, so a lista salva o nome")
    void naAberturaSoAListaSalva() {
        String en = "Banagher, get to the Unicorn now!";
        String pt = "Bernie, va para o Unicorn agora!";

        assertTrue(auditar(en, pt, NOMES).suspeito(),
            "COM a lista: \"Banagher\" e nome declarado e o portugues o trocou — tem de acusar");
        assertFalse(auditar(en, pt, Set.of()).suspeito(),
            "SEM a lista: na abertura da fala a maiuscula e de POSICAO e o nome se perde. E "
                + "exatamente a lacuna que esta ligacao fecha, e o que faz este par DISCRIMINAR.");
    }

    @Test
    @DisplayName("CONTRA-TESTE: palavra de PROSA da lore continua sem virar indicio")
    void prosaDaLoreNaoViraIndicio() {
        // Sem uma SEGUNDA maiuscula na fala: a primeira versao terminava em "in English", e era
        // "English" que estava sendo acusado — o teste reprovava pelo motivo errado e mandaria
        // consertar o codigo certo.
        ResultadoDeteccaoLore r = auditar(
            "Adjetivos are not a thing here.", "Adjetivos nao sao coisa daqui.", NOMES);

        assertFalse(r.suspeito(), () ->
            "\"Adjetivos\" e palavra da INSTRUCAO do prompt, nao nome. Se a fonte virasse a prosa "
                + "da lore, 19 palavras assim so no Zeta viravam indicio. Motivos: " + r.motivos());
    }

    @Test
    @DisplayName("CONTRA-TESTE: o roster de franquia continua valendo")
    void rosterDeFranquiaContinuaValendo() {
        ResultadoDeteccaoLore r = detector.auditar(
            "The Zaku is approaching fast.", "O Guel esta se aproximando rapido.",
            "zaku, gundam, zeon", Map.of(), Set.of());

        assertTrue(r.suspeito(),
            "\"Zaku\" e vocabulario de FRANQUIA, nao nome de obra: nao esta no termosProtegidos "
                + "de ninguem e precisa continuar vindo do roster. A lista SOMA, nao substitui.");
    }
    /**
     * O catálogo guarda o nome COMPLETO e a fala usa a palavra isolada. Sem expandir em palavras,
     * a primeira versão desta ligação deixou 19 protagonistas invisíveis em 5 das 7 obras —
     * {@code Inori}, {@code Judau}, {@code Haman}, {@code Banagher}, {@code Uraki} —, e invisível
     * em silêncio é o pior tipo. Medido depois da expansão: 19 -> 1, e o ruído que ela traz de
     * volta é 0 de 18 palavras comuns aferidas numa corrida real.
     */
    @Test
    @DisplayName("nome COMPLETO no catalogo cobre a palavra isolada na fala")
    void nomeCompletoCobreAPalavraIsolada() {
        Set<String> completos = Set.of("Banagher Links", "Kou Uraki", "Federation Forces");

        assertTrue(auditar("Uraki, report to the bridge.", "Chuck, va para a ponte.", completos)
            .suspeito(),
            "o catalogo diz \"Kou Uraki\" e a fala diz \"Uraki\": sem expandir em palavras, o "
                + "protagonista fica invisivel. Foram 19 assim em 5 das 7 obras.");
    }

    /**
     * O CUSTO da expansao, medido e declarado: a palavra de um composto entra junto. Nos sete
     * catalogos sao 467 palavras novas, das quais 17 genericas (4%) — {@code forces},
     * {@code team}, {@code group}, {@code fleet}, {@code division}, {@code system}.
     *
     * <p>Isso NAO e defeito: se a obra declara {@code "Federation Forces"} como termo e o
     * portugues escreve {@code "tropas"}, e exatamente o que a tela existe para apontar. O
     * remedio e uma equivalencia no yaml, nao uma excecao no codigo — a mesma decisao ja tomada
     * para {@code Federation Forces -> forcas da Federacao} no 0083.
     *
     * <p>A primeira versao deste teste afirmava o contrario, apoiada numa medicao estreita: eu
     * havia conferido 18 palavras de ruido que por acaso nao vinham de composto nenhum, e conclui
     * "0 de 18". Medir o que se tem na mao nao e medir o que existe.
     */
    @Test
    @DisplayName("a palavra do composto entra junto — custo medido e declarado")
    void palavraDoCompostoEntraJunto() {
        Set<String> completos = Set.of("Federation Forces", "GHQ Anti Bodies Squadron");

        assertTrue(auditar("The Forces are moving in.", "As tropas estao avancando.", completos)
            .suspeito(),
            "a obra declarou \"Federation Forces\" e o portugues escreveu \"tropas\": a tela "
                + "aponta, e quem decide se e aceitavel e uma equivalencia no yaml. Sao 17 "
                + "palavras assim em 467 — 4%.");
    }
}
