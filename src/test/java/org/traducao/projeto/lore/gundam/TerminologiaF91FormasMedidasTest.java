package org.traducao.projeto.lore.gundam;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.qualidadeTraducao.application.EnforcadorTermosLore;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela as formas-ruim REAIS que o LLM produziu ao traduzir
 * Mobile Suit Gundam F91 em 2026-07-30 (2.069 falas). Cada caso saiu de uma fala do
 * filme — nenhum foi deduzido.
 *
 * <p>Existe pela mesma razão do equivalente de Char's Counterattack:
 * {@code termosProtegidos()} é PERMISSIVO (os validadores removem o termo antes de
 * procurar resíduo em inglês) e não impede a localização. Quem garante a grafia canônica
 * é o {@link EnforcadorTermosLore} lendo {@code correcoesTerminologia()}.
 *
 * <p>INVARIANTES DO DOMÍNIO: o enforcer só restaura quando o texto ORIGINAL (EN) contém
 * o canônico — por isso "insetos" só vira "Bugs" numa fala cujo inglês tinha "Bugs".
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer forma-ruim medida que volte a passar
 * reprova a suíte.
 */
class TerminologiaF91FormasMedidasTest {

    private final EnforcadorTermosLore enforcador = new EnforcadorTermosLore();
    private final Map<String, String> correcoes = org.traducao.projeto.lore.LoreDeTeste.obra("gundam_f91").correcoesTerminologia();

    private String reforcar(String original, String traduzido) {
        return enforcador.reforcar(original, traduzido, correcoes);
    }

    @Test
    @DisplayName("Bugs: as armas autônomas viravam 'insetos' em 5 de 6 falas")
    void restauraBugs() {
        assertEquals("Isso precisa ser feito antes que os Bugs sejam ativados?",
            reforcar("...be finished before the Bugs are activated?",
                "Isso precisa ser feito antes que os insetos sejam ativados?"));
        assertEquals("São esses os Bugs?",
            reforcar("Are these the Bugs?", "São esses os insetos?"));
    }

    @Test
    @DisplayName("insetos continua inseto quando o inglês não falava de Bugs")
    void naoTocaInsetosForaDeContexto() {
        String fala = "A colônia está cheia de insetos.";
        assertEquals(fala, reforcar("The colony is full of insects.", fala),
            "sem 'Bugs' no inglês, o enforcer não pode tocar 'insetos'");
    }

    @Test
    @DisplayName("Space Ark: perdido em 11 de 12, e ainda com gênero errado")
    void restauraSpaceArk() {
        assertEquals("Se você tem tempo para correr, lute no Space Ark!",
            reforcar("If you have time to run, fight in the Space Ark!",
                "Se você tem tempo para correr, lute no Arca Espacial!"));
    }

    @Test
    @DisplayName("Colônias Frontier: 'Fronteira I' e a forma invertida 'IV Fronteira'")
    void restauraColoniasFrontier() {
        assertEquals("Ele está em uma operação na Frontier I.",
            reforcar("He's in an operation on Frontier I.",
                "Ele está em uma operação na Fronteira I."));
        assertEquals("Se eu não proteger esta colônia, a Frontier IV,",
            reforcar("If I don't protect this space colony, Frontier IV,",
                "Se eu não proteger esta colônia, a IV Fronteira,"));
    }

    @Test
    @DisplayName("Zamouth Garr: nave confundida com parte do corpo")
    void restauraZamouthGarr() {
        assertEquals("Que alguém do seu posto não saiba tudo sobre a Zamouth Garr.",
            reforcar("...that someone of your rank doesn't know everything about the Zamouth Garr.",
                "Que alguém do seu posto não saiba tudo sobre a Garra de Zamouth."));
    }

    @Test
    @DisplayName("Crossbone Vanguard: ordem invertida e a invenção 'Cruz Branca'")
    void restauraCrossboneVanguard() {
        assertEquals("Crossbone Vanguard é o nome que aqueles piratas usam!",
            reforcar("Crossbone Vanguard is the name those pirates use!",
                "Vanguard Crossbone é o nome que aqueles piratas usam!"));
        assertEquals("mas depois de ver o Crossbone Vanguard, acho que sei.",
            reforcar("but after seeing the Crossbone Vanguard, I think I know.",
                "mas depois de ver o Vanguard da Cruz Branca, acho que sei."));
    }

    @Test
    @DisplayName("Cosmo Aristocracy (doutrina) não se confunde com Cosmo Babylonia (Estado)")
    void restauraCosmoAristocracy() {
        assertEquals("que defende a Cosmo Aristocracy.",
            reforcar("who are proponents of the Cosmo Aristocracy.",
                "que defende a Aristocracia Cósmica."));
        assertEquals("Mas a Cosmo Aristocracy que você defende também discrimina...",
            reforcar("But the Cosmo Aristocracy you advocate also discriminates...",
                "Mas a Cosmo Aristocracia que você defende também discrimina..."));
    }

    @Test
    @DisplayName("Newtype no feminino: o mapa base do UC não cobria 'Nova Tipo'")
    void restauraNewtypeFeminino() {
        assertEquals("É um Newtype?", reforcar("Is it a Newtype?", "É um Nova Tipo?"));
    }

    @Test
    @DisplayName("bio-computer minúsculo é substantivo comum — 'bio-computador' fica")
    void naoForcaBioComputerMinusculo() {
        String fala = "sua velha senhora projetou seu bio-computador.";
        assertEquals(fala, reforcar("your old lady designed its bio-computer.", fala),
            "as 2 falas do filme escrevem em minúscula; o canônico Bio-Computer não casa");
        assertTrue(!correcoes.containsKey("bio-computador"),
            "entrada seria inerte e a tradução minúscula é legítima");
    }

    @Test
    @DisplayName("Iron Mask: o codinome não pode ser trocado pelo nome real")
    void restauraIronMask() {
        assertEquals("Iron Mask!", reforcar("Iron Mask!", "Carozzo Ronah!"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: congela um LIMITE do mecanismo, não um comportamento desejado.
     *
     * <p>{@code "Dorel Ronah?"} também saiu como {@code "Carozzo Ronah"}. Como a chave do
     * mapa é a FORMA-RUIM, não dá para mapear {@code "Carozzo Ronah"} para dois canônicos
     * ({@code Iron Mask} e {@code Dorel Ronah}) — {@code Map.ofEntries} lançaria com chave
     * duplicada. Ficou a de maior frequência (Iron Mask, 2 falas contra 1).
     *
     * <p>A guarda do enforcer torna isso INÓCUO em vez de perigoso: como ela exige que o
     * inglês contenha o canônico, a fala {@code "Dorel Ronah?"} — que não tem "Iron Mask"
     * no original — simplesmente não é tocada. O erro sobrevive, mas não é agravado, que
     * foi exatamente o que aconteceu no CCA com {@code "Londo Bell" -> "Londenion"}.
     */
    @Test
    @DisplayName("LIMITE: Dorel Ronah trocado por Carozzo Ronah sobrevive, mas não piora")
    void trocaDorelPorCarozzoNaoEhCorrigivel() {
        assertEquals("Carozzo Ronah", reforcar("Dorel Ronah?", "Carozzo Ronah"),
            "sem 'Iron Mask' no inglês, a guarda impede a troca — o erro fica como está");
        assertEquals("Iron Mask", correcoes.get("Carozzo Ronah"),
            "cobrir Dorel Ronah exigiria uma segunda entrada com a MESMA chave");
    }

    @Test
    @DisplayName("Federação continua traduzida — decisão de produto, igual ao CCA e ao Unicorn")
    void federacaoPermaneceTraduzida() {
        assertTrue(!correcoes.containsKey("Federação"),
            "'Federação' é a forma usada em todo o acervo; não é defeito");
    }
}
