package org.traducao.projeto.traducaoKaraoke.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.traducaoKaraoke.domain.ClasseLinhaKaraoke;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: guarda as invariantes que passaram a morar no
 * {@link PlanoDeClassificacao} quando ele saiu de dentro do use case — a ORDEM dos dois passes,
 * a leitura do campo {@code Effect}, e a indexação por posição.
 *
 * <h2>Por que esta classe existe separada</h2>
 * A decisão do arquivo vivia num método de 1.107 bytecodes que também gravava cache e escrevia
 * o {@code .ass}. Mexer no critério de música exigia tocar em tudo isso. E o método percorria o
 * documento DUAS vezes chamando o classificador nas duas — no acervo, 3,95 milhões de
 * classificações onde 1,98 milhão basta.
 */
class PlanoDeClassificacaoTest {

    private final ClassificadorLetraKaraokeService classificador =
        new ClassificadorLetraKaraokeService(new DetectorEfeitoKaraokeService());
    private final LeitorLegendaAss leitor = new LeitorLegendaAss();

    private DocumentoLegenda documento(Path pasta, String... linhasDialogo) throws IOException {
        StringBuilder sb = new StringBuilder("[Script Info]\r\nTitle: Teste\r\n\r\n[Events]\r\n")
            .append("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\r\n");
        for (String l : linhasDialogo) {
            sb.append(l).append("\r\n");
        }
        Path arquivo = pasta.resolve("plano.ass");
        Files.writeString(arquivo, sb.toString(), StandardCharsets.UTF_8);
        return leitor.ler(arquivo);
    }

    /**
     * A INVARIANTE DE ORDEM, e é a razão de o pré-passe existir separado: ele descobre o romaji
     * usando SÓ o campo {@code Effect}. Se usasse também "romaji no mesmo instante", a regra se
     * alimentaria da própria conclusão e uma linha puxaria a vizinha para dentro da música.
     *
     * <p>Aqui as duas linhas estão no MESMO instante: uma é romaji (e o estilo declara), a outra
     * é inglês num estilo que não declara nada. A segunda só é karaokê porque a primeira existe.
     */
    @Test
    @DisplayName("a camada romaji no mesmo instante puxa a camada inglesa para dentro da musica")
    void pareamentoPorInstante(@org.junit.jupiter.api.io.TempDir Path pasta) throws IOException {
        DocumentoLegenda doc = documento(pasta,
            "Dialogue: 0,0:01:00.00,0:01:05.00,OP - Romaji,,0,0,0,,kimi no koe wo wasure wa shinai",
            "Dialogue: 0,0:01:00.00,0:01:05.00,Camada2,,0,0,0,,I will never forget your voice");

        PlanoDeClassificacao plano = PlanoDeClassificacao.montar(doc, classificador);

        assertEquals(ClasseLinhaKaraoke.ORIGINAL_JAPONES, plano.classeNaPosicao(0));
        assertEquals(ClasseLinhaKaraoke.TRADUZIVEL_INGLES, plano.classeNaPosicao(1),
            "o estilo 'Camada2' nao declara nada e o campo Effect esta vazio — a UNICA evidencia "
                + "e a camada romaji no mesmo instante");
    }

    /** Sem a camada romaji, a mesma linha inglesa não tem evidência nenhuma e fica de fora. */
    @Test
    @DisplayName("CONTRA-TESTE: sem a camada romaji, a linha inglesa nao e karaoke")
    void semPareamentoNaoEhKaraoke(@org.junit.jupiter.api.io.TempDir Path pasta) throws IOException {
        DocumentoLegenda doc = documento(pasta,
            "Dialogue: 0,0:01:00.00,0:01:05.00,Camada2,,0,0,0,,I will never forget your voice");

        PlanoDeClassificacao plano = PlanoDeClassificacao.montar(doc, classificador);

        assertEquals(ClasseLinhaKaraoke.FORA_DE_MUSICA, plano.classeNaPosicao(0),
            "sem evidencia nenhuma a linha nao e assunto do karaoke");
    }

    /**
     * O campo {@code Effect} é o NONO da linha, e o {@code split} precisa de limite negativo:
     * campo vazio no meio é o caso normal, e sem isso o índice escorrega.
     */
    @Test
    @DisplayName("campo Effect=fx e lido do nono campo e vale como evidencia")
    void campoEfeitoEhLidoDoNonoCampo(@org.junit.jupiter.api.io.TempDir Path pasta) throws IOException {
        DocumentoLegenda doc = documento(pasta,
            "Dialogue: 0,0:01:37.00,0:01:39.80,OPL2,,0,0,0,fx,{\\fad(200,200)\\pos(960,1050)}Do you feel alone",
            "Dialogue: 0,0:01:37.00,0:01:39.80,OPL2,,0,0,0,,{\\fad(200,200)\\pos(960,1050)}Do you feel alone");

        PlanoDeClassificacao plano = PlanoDeClassificacao.montar(doc, classificador);

        assertEquals(ClasseLinhaKaraoke.TRADUZIVEL_INGLES, plano.classeNaPosicao(0),
            "com Effect=fx o OPL2 e karaoke — sao 258 linhas assim no acervo");
        assertEquals(ClasseLinhaKaraoke.FORA_DE_MUSICA, plano.classeNaPosicao(1),
            "a MESMA linha sem o campo Effect nao tem evidencia — o nono campo e o que decide");
    }

    /**
     * Indexação por POSIÇÃO, não por igualdade de evento. No 86 a mesma frase aparece 650 vezes
     * em instantes diferentes; um mapa por conteúdo colapsaria todas numa decisão só.
     */
    @Test
    @DisplayName("a decisao e por POSICAO: linhas identicas em instantes diferentes sao eventos diferentes")
    void indexadoPorPosicao(@org.junit.jupiter.api.io.TempDir Path pasta) throws IOException {
        DocumentoLegenda doc = documento(pasta,
            "Dialogue: 0,0:01:00.00,0:01:05.00,OP - Romaji,,0,0,0,,kimi no koe wo wasure wa shinai",
            "Dialogue: 0,0:01:00.00,0:01:05.00,Camada2,,0,0,0,,A flower blooms only to be crushed",
            "Dialogue: 0,0:09:00.00,0:09:05.00,Camada2,,0,0,0,,A flower blooms only to be crushed");

        PlanoDeClassificacao plano = PlanoDeClassificacao.montar(doc, classificador);

        assertEquals(3, plano.total());
        assertEquals(ClasseLinhaKaraoke.TRADUZIVEL_INGLES, plano.classeNaPosicao(1),
            "no instante 0:01:00 ha camada romaji — esta e karaoke");
        assertEquals(ClasseLinhaKaraoke.FORA_DE_MUSICA, plano.classeNaPosicao(2),
            "TEXTO IDENTICO, instante diferente e sem camada romaji: decisao diferente. "
                + "Indexar por conteudo colapsaria as duas.");
    }

    /**
     * A pergunta que decide o empilhamento com {@code \N}. Errar aqui apagou a letra da tela em
     * 22 de 23 linhas, em 10 episódios do Unicorn.
     */
    @Test
    @DisplayName("temOriginalPreservadaNoInstante responde pelo instante, nao pelo estilo")
    void originalPreservadaPorInstante(@org.junit.jupiter.api.io.TempDir Path pasta) throws IOException {
        DocumentoLegenda doc = documento(pasta,
            "Dialogue: 0,0:01:00.00,0:01:05.00,OP - Romaji,,0,0,0,,kimi no koe wo wasure wa shinai",
            "Dialogue: 0,0:01:00.00,0:01:05.00,Camada2,,0,0,0,,I will never forget your voice",
            "Dialogue: 0,0:09:00.00,0:09:05.00,ED2,,0,0,0,,Behind your mask");

        PlanoDeClassificacao plano = PlanoDeClassificacao.montar(doc, classificador);

        assertTrue(plano.temOriginalPreservadaNoInstante(doc.eventos().get(1)),
            "ha camada romaji simultanea: a traducao NAO precisa empilhar");
        assertFalse(plano.temOriginalPreservadaNoInstante(doc.eventos().get(2)),
            "camada UNICA: sem empilhar, a letra original desaparece da tela — foi o defeito "
                + "dos episodios 13-22 do Unicorn");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: uma SÍLABA de fill do KFX que por acaso parece romaji (a letra
     * {@code "I"} → {@code "i"}, que casa o padrão de sílaba japonesa) NÃO pode marcar o instante
     * como "tem original preservada" e roubar o empilhamento do inglês da frase que está no MESMO
     * instante.
     *
     * <h2>O prejuízo que originou (medido 2026-09-21, Unicorn OPL2)</h2>
     * As frases {@code "I know that all the lies..."} e {@code "I wonder how long..."} saíram só em
     * PT na abertura (2 de 16), porque o fragmento {@code "I"} — que compartilha o instante exato da
     * frase — era classificado {@code ORIGINAL_JAPONES} no PRÉ-PASSE (que ainda não tem o sinal de
     * sílaba) e marcava o instante. A original de verdade (romaji do ED) é FRASE, nunca fragmento —
     * ver {@link #originalPreservadaPorInstante} (o caso-controle SÃO que precisa continuar TRUE).
     *
     * <h2>Caso-controle de fronteira (A1)</h2>
     * A sílaba romaji-aparente no mesmo instante NÃO marca (aqui, FALSE); a FRASE romaji real no
     * mesmo instante MARCA (em {@link #originalPreservadaPorInstante}, TRUE). Os dois carregam o
     * mesmo sinal superficial ("parece romaji"); a régua é ser sílaba de fill ou frase.
     */
    @Test
    @DisplayName("silaba que parece romaji NAO marca original no instante (frase inglesa mantem o ingles)")
    void silabaRomajiAparenteNaoMarcaOriginalNoInstante(@org.junit.jupiter.api.io.TempDir Path pasta)
            throws IOException {
        DocumentoLegenda doc = documento(pasta,
            "Dialogue: 0,0:01:00.00,0:01:05.00,OPL2,,0,0,0,fx,{\\pos(640,60)}I know",
            "Dialogue: 0,0:01:00.00,0:01:05.00,OPL2,,0,0,0,fx,{\\pos(560,60)}I",
            "Dialogue: 0,0:01:02.00,0:01:05.00,OPL2,,0,0,0,fx,{\\pos(600,60)}know");

        PlanoDeClassificacao plano = PlanoDeClassificacao.montar(doc, classificador);

        assertEquals(ClasseLinhaKaraoke.TRADUZIVEL_INGLES, plano.classeNaPosicao(0),
            "a frase inglesa e karaoke traduzivel (Effect=fx)");
        assertFalse(plano.temOriginalPreservadaNoInstante(doc.eventos().get(0)),
            "a silaba 'I' que parece romaji NAO pode marcar o instante — senao a frase inglesa perde "
                + "o empilhamento do original e sai so em PT (o defeito das 2 frases da abertura OPL2)");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o outro lado da fronteira do fix #3 — a original ROMAJI de verdade,
     * mesmo quando o karaokê a pinta em SÍLABAS, tem de continuar marcando o instante. O fix pula
     * a SÍLABA (fragmento) do {@code comRomaji}, não a FRASE: a frase romaji inteira não é sílaba de
     * ninguém, então preserva. Sem esta garantia, "consertar" o #3 excluindo demais reabriria o
     * scar das 22/23 linhas do ED.
     *
     * <p>Aqui a frase romaji {@code "kimi no koe"} convive com suas sílabas ({@code ki},{@code mi},
     * {@code no},{@code koe}, que reconstroem {@code kiminokoe}) e com uma camada inglesa no MESMO
     * instante. A inglesa TEM de ver original preservada (a frase romaji), apesar de as sílabas
     * serem puladas.
     */
    @Test
    @DisplayName("fronteira #3: frase romaji com silabas KFX ainda marca o instante (nao sobre-exclui)")
    void fraseRomajiComSilabasAindaMarcaOInstante(@org.junit.jupiter.api.io.TempDir Path pasta)
            throws IOException {
        DocumentoLegenda doc = documento(pasta,
            "Dialogue: 0,0:01:00.00,0:01:05.00,OP - Romaji,,0,0,0,,kimi no koe",
            "Dialogue: 0,0:01:00.00,0:01:05.00,OP - Romaji,,0,0,0,,ki",
            "Dialogue: 0,0:01:01.00,0:01:05.00,OP - Romaji,,0,0,0,,mi",
            "Dialogue: 0,0:01:02.00,0:01:05.00,OP - Romaji,,0,0,0,,no",
            "Dialogue: 0,0:01:03.00,0:01:05.00,OP - Romaji,,0,0,0,,koe",
            "Dialogue: 0,0:01:00.00,0:01:05.00,Camada2,,0,0,0,,your voice");

        PlanoDeClassificacao plano = PlanoDeClassificacao.montar(doc, classificador);

        assertEquals(ClasseLinhaKaraoke.ORIGINAL_JAPONES, plano.classeNaPosicao(0),
            "a FRASE romaji e a original — nao e silaba de ninguem");
        assertTrue(plano.temOriginalPreservadaNoInstante(doc.eventos().get(5)),
            "a camada inglesa no mesmo instante TEM de ver a original romaji preservada, mesmo com "
                + "as silabas puladas — senao o fix #3 reabriria o scar 22/23 do ED");
    }

    /** FALHA FECHADA: documento nulo e posição fora da faixa não podem virar exceção. */
    @Test
    @DisplayName("FALHA FECHADA: nulo e posicao invalida devolvem FORA_DE_MUSICA")
    void falhaFechada() {
        PlanoDeClassificacao vazio = PlanoDeClassificacao.montar(null, classificador);
        assertEquals(0, vazio.total());
        assertEquals(ClasseLinhaKaraoke.FORA_DE_MUSICA, vazio.classeNaPosicao(0));
        assertEquals(ClasseLinhaKaraoke.FORA_DE_MUSICA, vazio.classeNaPosicao(-1));
        assertEquals(ClasseLinhaKaraoke.FORA_DE_MUSICA, vazio.classeNaPosicao(999));
        assertFalse(vazio.temOriginalPreservadaNoInstante(null));
    }
}
