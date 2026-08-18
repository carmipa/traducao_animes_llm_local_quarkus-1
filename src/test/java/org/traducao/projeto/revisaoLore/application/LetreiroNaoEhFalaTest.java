package org.traducao.projeto.revisaoLore.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.domain.EventoLegenda;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: letreiro — título de episódio, placa, cartaz — não é fala, e a tela 3.2
 * corrige lore em DIÁLOGO. Este teste congela o veto.
 *
 * <h2>O prejuízo, medido no acervo em 17/08/2026</h2>
 * A corrida no Gundam 08th MS Team acusou <b>10 vezes</b> esta linha:
 * <pre>
 *   EN: {\fs200\blur1\pos(720,650)}NEXT EPISODE
 *   PT: {\fs200\blur1\pos(720,650)}Proximo episodio.
 * </pre>
 * Corpo 200, blur, posicionamento — letreiro, e a tradução está CERTA. Varrendo os 232 arquivos
 * PT do acervo com o {@link AlcanceRevisaoLore} de produção: <b>114 linhas</b> de {@code \fs>=100}
 * chegavam à tela, todas letreiro já traduzido ({@code [Sign] "Elas chamaram isso de Gundam"},
 * {@code [Sign] "A cacada de Full Frontal."}). Ruído puro, e ruído esconde achado.
 *
 * <h2>Por que o veto mora AQUI e não no dono da regra</h2>
 * O {@code ProtecaoLegendaAssService.deveBloquearAntesDoLlm} exige {@code clip} longo na última
 * porta — nasceu do karaokê do Zeta — e por isso deixa passar cartaz sem clip. Tirar o
 * {@code clip} de lá <b>quebraria a TRADUÇÃO</b>, que precisa mandar cartaz ao modelo: é por isso
 * que os letreiros do acervo estão em português. Aquela regra serve a quem traduz; esta a quem
 * revisa lore.
 *
 * <h2>Invariantes do domínio</h2>
 * O veto exige a CONJUNÇÃO de estilo técnico e typesetting pesado. Os contra-testes cobrem os
 * dois lados: diálogo com {@code \pos} continua no alcance, e estilo {@code Sign} sem corpo
 * grande também — senão o veto viraria alarme falso, que é como guarda morre.
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem diz qual lado quebrou e quantas linhas do acervo aquilo representa.
 */
@QuarkusTest
class LetreiroNaoEhFalaTest {

    @Inject
    AlcanceRevisaoLore alcance;

    private static EventoLegenda linha(String estilo, String texto) {
        return new EventoLegenda(1, "Dialogue", estilo,
            "Dialogue: 0,0:00:01.00,0:00:03.00," + estilo + ",,0,0,0,,", texto);
    }

    /**
     * O estilo é <b>{@code Titles}</b>, no PLURAL, copiado do arquivo real do 08th. A primeira
     * versão deste teste usava {@code "Sign"} e passava — enquanto a produção continuava mandando
     * os 20 letreiros ao modelo, porque {@code \btitle\b} não casa {@code "Titles"}: o {@code s}
     * impede a fronteira. Fixture escolhida por conveniência esconde exatamente o defeito que o
     * acervo tem.
     */
    @Test
    @DisplayName("o NEXT EPISODE do 08th sai do alcance da 3.2")
    void letreiroDeProximoEpisodioSaiDoAlcance() {
        EventoLegenda cartaz = linha("Titles",
            "{\\fs200\\blur1\\pos(720,650)\\c&HDEDEE3&}Proximo episodio.");

        assertFalse(alcance.estaNoAlcance(cartaz),
            "letreiro continua no alcance da tela. No 08th esta linha rendeu 10 acusacoes de uma "
                + "traducao CORRETA, e no acervo inteiro sao 114 linhas assim.");
    }

    @Test
    @DisplayName("titulo de episodio com estilo Sign tambem sai")
    void tituloDeEpisodioSaiDoAlcance() {
        EventoLegenda titulo = linha("Sign",
            "{\\blur0.75\\fs140\\fade(500,500)\\pos(1120,813.11)}Elas chamaram isso de Gundam");

        assertFalse(alcance.estaNoAlcance(titulo),
            "titulo de episodio e letreiro: o Unicorn sozinho tem dezenas deles, todos ja "
                + "traduzidos");
    }

    @Test
    @DisplayName("CONTRA-TESTE: dialogo com \\pos continua sendo olhado")
    void dialogoPosicionadoContinuaNoAlcance() {
        EventoLegenda fala = linha("Default", "{\\pos(960,1000)}Nosso Ceifador estava solitario.");

        assertTrue(alcance.estaNoAlcance(fala),
            "fala legitima saiu do alcance. Dialogo usa \\pos o tempo todo, e vetar por tag "
                + "sozinha cegaria a tela — alarme falso ensina a desligar o alarme.");
    }

    @Test
    @DisplayName("CONTRA-TESTE: estilo Sign SEM corpo grande continua sendo olhado")
    void sinalSemCorpoGrandeContinuaNoAlcance() {
        EventoLegenda fala = linha("Sign", "{\\pos(500,500)}Base da Republica de San Magnolia");

        assertTrue(alcance.estaNoAlcance(fala),
            "o veto exige a CONJUNCAO de estilo tecnico E typesetting pesado. Nome de estilo e "
                + "convencao de fansub e varia; vetar so por ele deixaria de fora placa com nome "
                + "de lore escrito em corpo normal");
    }
    /**
     * O 0083 mostrou a SEGUNDA porta do mesmo defeito: o corpo da fonte mora na DEFINIÇÃO do
     * estilo, não na linha.
     * <pre>
     *   Style: Titles, Narkisim, 120, ...
     *   Dialogue: ...,Titles,,0,0,0,,{\blur1\c&H141316&\pos(720,610)}NEXT EPISODE
     * </pre>
     * O veto por {@code \fs} inline é cego para ela, e 12 letreiros do 0083 continuaram indo ao
     * modelo DEPOIS do veto entrar. Consertar a forma vista não conserta a classe.
     */
    @Test
    @DisplayName("cartao de episodio SEM \\fs na tag tambem sai (o corpo esta no estilo)")
    void letreiroComCorpoNoEstiloSaiDoAlcance() {
        EventoLegenda cartaz = linha("Titles",
            "{\\blur1\\c&H141316&\\pos(720,610)}Proximo episodio.");

        assertFalse(alcance.estaNoAlcance(cartaz),
            "letreiro sem \\fs na tag continua no alcance. No 0083 o estilo Titles declara corpo "
                + "120 no cabecalho do arquivo, e sao 12 acusacoes de uma traducao CORRETA.");
    }

    @Test
    @DisplayName("CONTRA-TESTE: placa 'Sign' com nome de lore em corpo normal continua sendo olhada")
    void placaComNomeDeLoreContinuaNoAlcance() {
        EventoLegenda placa = linha("Signs",
            "{\\pos(500,500)\\blur1}Base da Republica de San Magnolia");

        assertTrue(alcance.estaNoAlcance(placa),
            "a dispensa de corpo de fonte vale so para CARTAO DE EPISODIO. Se 'Signs' entrasse "
                + "junto, placa com nome de lore em corpo normal sairia da conferencia — e nome "
                + "de lore em placa e exatamente o que a 3.2 existe para conferir.");
    }
}
