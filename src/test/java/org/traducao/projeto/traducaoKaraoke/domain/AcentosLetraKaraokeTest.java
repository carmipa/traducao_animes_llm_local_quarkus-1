package org.traducao.projeto.traducaoKaraoke.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: prova que a lista de acentos do KARAOKÊ resolve o problema do karaokê e
 * <b>não herda</b> o da tradução de diálogo — a regra de desacoplamento enunciada por Paulo em
 * 2026-08-14: <i>o problema novo se resolve só na fatia onde apareceu, e não atravessa para o
 * módulo comum</i>.
 *
 * <h2>As duas cicatrizes que este teste guarda ao mesmo tempo</h2>
 * <ul>
 *   <li><b>Acento faltando</b>: na tradução do 86 de 14/08/2026 a camada portuguesa saiu com
 *       {@code nao} sem acento em 5 falas distintas, presentes em 918 linhas do arquivo.</li>
 *   <li><b>Acento a mais</b>: {@code mae} é 前 em romaji e virou {@code mãe} 100 vezes nos 50
 *       episódios do Unicorn. {@code mae} está na lista do DIÁLOGO — e é por isso que aquela lista
 *       não pode ser reusada aqui.</li>
 * </ul>
 *
 * <p>Medido contra o dicionário {@code ja_ROMAJI} do projeto: das 162 entradas da lista comum,
 * quatro são romaji válido — {@code ate}, {@code mae}, {@code nao}, {@code sao}.
 */
class AcentosLetraKaraokeTest {

    /** A fala real do OP do 86, como o aya a devolveu em 14/08/2026. */
    @Test
    @DisplayName("CASO DOENTE: a fala real do 86 sai acentuada")
    void acentuaAFalaRealDo86() {
        assertEquals("Não se preocupe, relaxe, por que não?",
            AcentosLetraKaraoke.repor("Nao se preocupe, relaxe, por que nao?"),
            "918 linhas do 86 saíram assim em 14/08/2026");
        assertEquals("Eu não consigo ver o futuro nos seus olhos sem reflexo.",
            AcentosLetraKaraoke.repor("Eu nao consigo ver o futuro nos seus olhos sem reflexo."));
    }

    /**
     * O caso-controle que dá sentido à fatia ter lista própria. Se alguém "simplificar" reusando o
     * {@code NormalizadorAcentosComuns} do peer {@code qualidadeTraducao}, estas três passam a ser
     * corrigidas e este teste cai — que é exatamente o alarme desejado.
     */
    @Test
    @DisplayName("CASO SÃO: as colisões com romaji da lista do diálogo NAO entram aqui")
    void naoHerdaAsColisoesComRomajiDaListaDoDialogo() {
        for (String romaji : new String[] {"mae", "ate", "sao"}) {
            assertEquals(romaji, AcentosLetraKaraoke.repor(romaji),
                "'" + romaji + "' é romaji válido e está na lista do DIÁLOGO; reusá-la aqui "
                    + "devolveria o dano do Unicorn (mae=前 virou mãe 100 vezes)");
        }
    }

    /** A linha que pagou a cicatriz: {@code mae} no meio de romaji corrido. */
    @Test
    @DisplayName("linha de romaji do acervo passa byte a byte")
    void linhaDeRomajiPassaIntacta() {
        String romaji = "mae wo muite arukou, namida ga koborenai you ni";
        assertEquals(romaji, AcentosLetraKaraoke.repor(romaji));
    }

    @Test
    @DisplayName("preserva a caixa e a fronteira da palavra")
    void preservaCaixaEFronteira() {
        assertEquals("NÃO", AcentosLetraKaraoke.repor("NAO"));
        assertEquals("Nao-chan", AcentosLetraKaraoke.repor("Nao-chan"),
            "hífen não é fronteira de fim aqui — nome composto não pode ser renomeado");
    }

    @Test
    @DisplayName("FALHA FECHADA: nulo e vazio voltam como vieram")
    void falhaFechada() {
        assertEquals(null, AcentosLetraKaraoke.repor(null));
        assertEquals("   ", AcentosLetraKaraoke.repor("   "));
    }
}
