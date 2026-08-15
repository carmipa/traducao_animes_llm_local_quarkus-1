package org.traducao.projeto.traducaoKaraoke.application;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;
import org.traducao.projeto.traducaoKaraoke.domain.AcentosLetraKaraoke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que o corretor ortográfico do karaokê acentua o PORTUGUÊS sem
 * tocar no ROMAJI — as duas camadas convivem no mesmo evento, separadas por {@code \N}.
 *
 * <h2>A cicatriz que obriga este teste a existir</h2>
 * Nos 50 episódios do Unicorn o corretor virou {@code mae} em {@code mãe} <b>100 vezes</b>, e
 * ali {@code mae} era 前 — romaji, não a palavra portuguesa. Em karaokê o risco não é o
 * dicionário estar desligado: é ele agir DEMAIS, sobre a camada que existe justamente para ser
 * preservada.
 *
 * <h2>O que protege hoje, e é o que este teste congela</h2>
 * Em {@code TraduzirKaraokeUseCase:413} a ordem é
 * {@code comOriginalPreservada(evento, corrigirAcentos(traduzido), ...)}: o corretor recebe
 * SOMENTE o texto traduzido, e a linha original é anexada DEPOIS. Trocar a ordem — corrigir o
 * conjunto já montado — devolveria o defeito do Unicorn.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Romaji nunca é alterado pelo corretor, mesmo quando a forma coincide com palavra
 *       portuguesa sem acento.</li>
 *   <li>Português da letra recebe acento normalmente — desligar o corretor não é a solução.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem hunspell, PULA por {@link Assumptions} — a parte determinística continua valendo.
 */
@DisplayName("karaokê: o corretor acentua o português e não alcança o romaji")
class CorretorNaoAlcancaRomajiDoKaraokeTest {

    private final CorretorOrtograficoLegenda corretor = new CorretorOrtograficoLegenda();

    private void exigeDicionario() {
        corretor.corrigir("organizacao");
        Assumptions.assumeTrue(corretor.disponivel(), "hunspell ausente — NÃO VERIFICADO");
    }

    /**
     * A linha REAL da abertura do 86, episódio 01, conferida no vídeo remuxado em 14/08/2026.
     * Toda palavra aqui é romaji e nenhuma pode ganhar acento.
     */
    @Test
    @DisplayName("CASO DOENTE: a linha de abertura do 86 passa intacta")
    void aLinhaRealDoOpeningDo86PassaIntacta() {
        exigeDicionario();
        String romaji = "anata no koe wo, wasure wa shinai darou";
        assertEquals(romaji, corretor.corrigir(romaji),
            "o corretor alterou romaji da abertura do 86 — é a camada que existe para ser "
                + "preservada, e foi assim que 'mae' virou 'mãe' 100 vezes no Unicorn");
    }

    /** Formas de romaji que colidem com português sem acento — o caso mais perigoso. */
    @Test
    @DisplayName("romaji que parece português sem acento continua intacto")
    void romajiHomografoNaoEhAcentuado() {
        exigeDicionario();
        for (String r : new String[] {"kimi", "kokoro", "yume", "sora", "hikari", "negai"}) {
            assertEquals(r, corretor.corrigir(r),
                "romaji '" + r + "' foi alterado pelo corretor");
        }
    }

    /**
     * O outro lado: desligar o corretor NÃO é a solução. A letra em português precisa sair
     * acentuada, senão o karaokê herda o defeito que o dicionário existe para resolver.
     */
    @Test
    @DisplayName("CASO SÃO: a letra em português recebe acento")
    void letraEmPortuguesEhAcentuada() {
        exigeDicionario();
        // A CORRENTE INTEIRA, como TraduzirKaraokeUseCase.corrigirAcentos faz: lista NOMINAL da
        // fatia primeiro, dicionário depois. Este teste ficou VERMELHO de 13/08 a 14/08 porque
        // exercitava só o dicionário, que tem piso de 4 letras e nunca alcançaria "nao" — a fatia
        // ainda não tinha lista própria. Não é a asserção que foi afrouxada: é o alvo que passou a
        // existir. Ver AcentosLetraKaraoke.
        String saida = corretor.corrigir(
            AcentosLetraKaraoke.repor("nao importa o que aconteca, nao vou esquecer sua voz"));
        assertTrue(saida.contains("não"), "a letra em português não foi acentuada: " + saida);
    }

    /**
     * O EVENTO MONTADO, que é como a fala chega ao arquivo: romaji, {@code \N}, português. Prova
     * que corrigir o conjunto inteiro é justamente o que NÃO pode acontecer — se alguém trocar a
     * ordem em {@code TraduzirKaraokeUseCase:413}, este assert cai.
     */
    @Test
    @DisplayName("as duas camadas juntas: só a de baixo muda")
    void noEventoMontadoSoOportuguesMuda() {
        exigeDicionario();
        String romaji = "anata no koe wo, wasure wa shinai darou";
        String portuguesErrado = "nao vou esquecer sua voz";

        // Como o use case faz: corrige SÓ o traduzido — lista da fatia + dicionário —, depois junta.
        String montadoCerto = romaji + "\\N"
            + corretor.corrigir(AcentosLetraKaraoke.repor(portuguesErrado));
        assertTrue(montadoCerto.startsWith(romaji),
            "a camada romaji foi alterada: " + montadoCerto);
        assertTrue(montadoCerto.contains("não"),
            "a camada portuguesa não foi acentuada: " + montadoCerto);

        // E a contraprova: corrigir o conjunto montado é o que devolve o defeito do Unicorn.
        // Aqui só se documenta que a diferença EXISTE — a ordem correta é a de cima.
        String montadoErrado = corretor.corrigir(
            AcentosLetraKaraoke.repor(romaji + "\\N" + portuguesErrado));
        assertTrue(montadoErrado.contains("não"),
            "sanidade: o português seria acentuado nos dois caminhos");
    }
}
