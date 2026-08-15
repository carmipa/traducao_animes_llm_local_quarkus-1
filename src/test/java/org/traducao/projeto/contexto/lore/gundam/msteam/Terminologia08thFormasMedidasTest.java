package org.traducao.projeto.contexto.lore.gundam.msteam;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.qualidadeTraducao.application.EnforcadorTermosLore;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela as formas-ruim REAIS medidas nas 3.621 falas de
 * Gundam The 08th MS Team — a obra com a PIOR taxa do catálogo (36,2 perdas por mil).
 *
 * <p>Aqui o ímã aparece em estado puro: {@code termosProtegidos()} declara DUAS grafias
 * do mesmo nome e o modelo escolhe sempre a que a legenda não usa, com consistência
 * total — {@code Apsaras}→"Apsalus" em 26 de 26, {@code Ginius}→"Ginias" em 15 de 15,
 * {@code Sakhalin}→"Sahalin" em 9 de 9.
 *
 * <p>INVARIANTES DO DOMÍNIO: como o {@link EnforcadorTermosLore} só age quando o texto
 * ORIGINAL contém o canônico, a regra adotada é restaurar o que a FONTE diz. Isso
 * convive com as entradas antigas que apontam para a outra grafia, porque nenhuma fala
 * traz as duas no inglês.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer forma-ruim medida que volte a passar
 * reprova a suíte.
 */
class Terminologia08thFormasMedidasTest {

    private final EnforcadorTermosLore enforcador = new EnforcadorTermosLore();
    private final Map<String, String> correcoes =
        org.traducao.projeto.contexto.LoreDeTeste.obra("gundam_08ms").correcoesTerminologia();

    private String reforcar(String original, String traduzido) {
        return enforcador.reforcar(original, traduzido, correcoes);
    }

    @Test
    @DisplayName("Apsaras: perdido em 26 de 26 — o mobile armor virava a outra grafia")
    void restauraApsaras() {
        assertEquals("Minha Apsaras, você quer dizer?",
            reforcar("My Apsaras, you mean?", "Minha Apsalus, você quer dizer?"));
        assertEquals("O dia do término do Apsaras está próximo!",
            reforcar("The day of the Apsaras's completion is near!",
                "O dia do término do Apsalus está próximo!"));
    }

    @Test
    @DisplayName("Ginius: perdido em 15 de 15")
    void restauraGinius() {
        assertEquals("Onde está o Ginius?",
            reforcar("Where's Ginius?!", "Onde está o Ginias?"));
    }

    @Test
    @DisplayName("Sakhalin: perdido em 9 de 9, inclusive no nome completo")
    void restauraSakhalin() {
        assertEquals("Meu nome é Aina Sakhalin!",
            reforcar("My name is Aina Sakhalin!", "Meu nome é Aina Sahalin!"));
        assertEquals("Para o bem da família Sakhalin.",
            reforcar("It's for the sake of the Sakhalin family.",
                "Para o bem da família Sahalin."));
    }

    @Test
    @DisplayName("A grafia antiga continua valendo quando é ELA que está no inglês")
    void respeitaAGrafiaDaFonte() {
        String fala = "O Apsalus decolou.";
        assertEquals(fala, reforcar("The Apsalus took off.", fala),
            "sem 'Apsaras' no inglês, o enforcer não troca — a fonte manda");
    }

    @Test
    @DisplayName("Ball é um pod de combate, não um brinquedo — 6 de 9")
    void restauraBall() {
        assertEquals("Vi a Ball quando embarquei!",
            reforcar("The Ball! I saw one when I came aboard!", "Vi a bola quando embarquei!"));
    }

    @Test
    @DisplayName("bola continua bola quando o inglês não falava do Ball")
    void naoTocaBolaComum() {
        String fala = "A criança chutou a bola.";
        assertEquals(fala, reforcar("The child kicked the ball.", fala),
            "o canônico é 'Ball' maiúsculo e o enforcer exige grafia exata");
    }

    @Test
    @DisplayName("Beam Rifle: o núcleo UC cobria 'Rifle de Feixe', não esta forma")
    void restauraBeamRifle() {
        assertEquals("Levanta tua Beam Rifle!",
            reforcar("Get your beam rifle up!", "Levanta tua rifle de raio!"));
    }

    @Test
    @DisplayName("Kellerne: sobrenome corrompido em 2 de 4")
    void restauraKellerne() {
        assertEquals("Almirante Yuri Kellerne.",
            reforcar("Admiral Yuri Kellerne.", "Almirante Yuri Kellarny."));
    }

    @Test
    @DisplayName("Patente traduzida NÃO entra no mapa — 'Capitão Norris' está correto")
    void patenteTraduzidaPermanece() {
        String fala = "Capitão Norris, nossas forças estarão acabando...";
        assertEquals(fala,
            reforcar("Captain Norris, our forces will be finishing off...", fala));
        assertTrue(!correcoes.containsKey("Capitão Norris"),
            "traduzir o posto é correto; o termo protegido é que embute a patente");
    }
}
