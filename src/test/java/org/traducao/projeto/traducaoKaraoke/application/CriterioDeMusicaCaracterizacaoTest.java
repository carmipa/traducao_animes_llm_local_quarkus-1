package org.traducao.projeto.traducaoKaraoke.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.traducaoKaraoke.domain.ClasseLinhaKaraoke;
import org.traducao.projeto.traducaoKaraoke.domain.SinaisDeKaraoke;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: congela o critério "isto é música?" com linhas REAIS do acervo, para que
 * qualquer mudança nele apareça como diferença explícita neste arquivo — nunca em silêncio.
 *
 * <h2>Este arquivo nasceu congelando o DEFEITO, e o diff é o registro do conserto</h2>
 * Na F1 do Plano Mestre de 2026-08-19 ele descrevia o que o código fazia: três casos
 * <b>errados de propósito</b>, assim documentados. Na F3 as três asserções foram invertidas,
 * cada uma com o motivo ao lado. Quem quiser saber o que a régua mudou lê o histórico deste
 * arquivo — a mudança não aconteceu em silêncio, que era o ponto.
 *
 * <h2>O prejuízo medido que originou</h2>
 * Rodando o classificador de produção sobre o acervo inteiro (726 arquivos, 0 erro de leitura):
 * <b>165.827 linhas</b> classificadas TRADUZIVEL_INGLES, e <b>133.951 delas (80,8%) entram só
 * pela assinatura de efeito</b> — nem o estilo declara música, nem existe tag {@code \k}. O que
 * entra por engano: {@code Char's Counterattack} 106.692 (o estilo de DIÁLOGO do filme),
 * {@code Signs} 9.213, {@code Zeta Episode Title} 6.923, {@code Main Title} 4.129,
 * {@code Logo} 1.961.
 *
 * <p>A causa é {@code DetectorEfeitoKaraokeService.eSaidaDeTemplateKaraoke}, que trata
 * "posicionamento complexo + alta densidade de tags" como prova de karaokê. Isso é assinatura de
 * TIPOGRAFIA, e o fansub usa exatamente as mesmas tags para mascarar diálogo.
 */
class CriterioDeMusicaCaracterizacaoTest {

    private final ClassificadorLetraKaraokeService classificador =
        new ClassificadorLetraKaraokeService(new DetectorEfeitoKaraokeService());

    /**
     * Linha real de {@code Mobile.Suit.Gundam.CCA_Track2.ass}, estilo {@code Char's Counterattack}
     * — que é o estilo de DIÁLOGO do filme, não de música. Campo {@code Effect} vazio.
     *
     * <p>O filme tem 55.983 eventos com texto para 1.786 textos distintos (repetição de 31×), e
     * 106.692 deles caem como traduzíveis hoje.
     */
    @Test
    @DisplayName("CASO DOENTE: dialogo do CCA com clip retangular NAO e karaoke")
    void dialogoDoCcaNaoEntraComoKaraoke() {
        String real = "{=1}{\\alpha&D2&\\blur1.15\\c&H604C96&\\pos(1142.1,935.42)"
            + "\\clip(601,835,1685,924)}CHAR'S COUNTERATTACK";

        // MUDOU EM 2026-08-19, e a mudanca e o conserto: ate a F2 isto devolvia
        // TRADUZIVEL_INGLES, e eram 106.692 eventos so neste filme. O estilo e o de DIALOGO,
        // o campo Effect esta vazio, nao ha tag \k e nao ha camada romaji no instante —
        // nenhuma das quatro evidencias. Densidade de tag deixou de valer como prova.
        assertEquals(ClasseLinhaKaraoke.FORA_DE_MUSICA,
            classificador.classificar("Char's Counterattack", real),
            "dialogo do filme voltou a ser tratado como letra de musica");
    }

    /**
     * Linha real do 86, estilo {@code Signs}, campo {@code Effect} vazio. É cartaz de data, não
     * letra. São 9.213 no acervo, e 258 já foram efetivamente traduzidas pelo módulo — estão no
     * cache de karaokê.
     */
    @Test
    @DisplayName("CASO DOENTE: cartaz de data (Signs) NAO e karaoke")
    void cartazDeDataNaoEntraComoKaraoke() {
        String real = "{=11}{\\an2\\pos(960,930)\\fnNewCinemaB Std D\\fscx90\\fs80\\fsp2"
            + "\\bord2\\1a&HFF&\\3a&H30&\\blur5\\b0}May 13th, Stellar Year 2148";

        // MUDOU EM 2026-08-19. Sao 9.213 no acervo, e 258 ja tinham sido efetivamente
        // traduzidas pelo modulo — estao no cache de karaoke, com estilo "Signs" gravado.
        // Cartaz e trabalho da traducao de dialogo, nao desta fatia.
        assertEquals(ClasseLinhaKaraoke.FORA_DE_MUSICA,
            classificador.classificar("Signs", real),
            "cartaz de data voltou a entrar na fatia de karaoke");
    }

    /**
     * Linha real do Zeta, estilo {@code Zeta Episode Title}, com máscara vetorial
     * ({@code \clip(m ... l ...)}) — tipografia pura. São 6.923 no acervo.
     */
    @Test
    @DisplayName("CASO DOENTE: titulo de episodio com mascara vetorial NAO e karaoke")
    void tituloDeEpisodioNaoEntraComoKaraoke() {
        String real = "{=10}{\\fscx100\\fscy100\\fs130\\blur1.5\\t(3603,4353,\\c&H46340A&\\1a&H00&)"
            + "\\fade(255,0,255,0,558,5688,6180)\\pos(-178,570.8)"
            + "\\clip(m 1 0 l 1491 0 1491 1080 1 1080)}Mobile Suit";

        // MUDOU EM 2026-08-19. Sao 6.923 no acervo. Repare que aqui HA a transformacao \t( —
        // e mesmo assim nao e musica: \t( anima titulo, logo e placa exatamente como anima
        // letra. Foi a hipotese que a medicao derrubou.
        assertEquals(ClasseLinhaKaraoke.FORA_DE_MUSICA,
            classificador.classificar("Zeta Episode Title", real),
            "titulo de episodio voltou a entrar na fatia de karaoke");
    }

    /**
     * CASO SÃO que precisa continuar funcionando depois da régua: a letra inglesa real do OP do
     * 86. O estilo {@code Opening} declara música, então ela sobrevive pela evidência do ESTILO
     * mesmo com o campo {@code Effect} vazio — que é como o 86 inteiro vem (159.398 linhas).
     */
    @Test
    @DisplayName("CASO SAO: letra inglesa do OP do 86 e traduzivel, e tem de continuar sendo")
    void letraInglesaDo86ContinuaTraduzivel() {
        String real = "{=27}{\\an2\\pos(830,170)\\c&H002F68&\\2c&HC4B2AD&\\blur3.5"
            + "\\clip(0,107,1920,123.5)\\t(-301,-300,\\3c&HAAA618&)}A flower blooms only to be crushed";

        assertEquals(ClasseLinhaKaraoke.TRADUZIVEL_INGLES,
            classificador.classificar("Opening", real),
            "esta e a linha que o conserto do marcador perdido recuperou 650 vezes; nao regride");
    }

    /**
     * CASO SÃO: romaji continua preservado. É a invariante mais antiga da fatia — "jamais mexer
     * no japonês" — e o dano medido quando ela falhou foi {@code mae} (前) virando {@code mãe}
     * 100 vezes nos 50 episódios do Unicorn.
     */
    @Test
    @DisplayName("CASO SAO: romaji do 86 continua preservado")
    void romajiContinuaPreservado() {
        assertEquals(ClasseLinhaKaraoke.ORIGINAL_JAPONES,
            classificador.classificar("Opening",
                "{\\pos(798,40)\\bord0\\blur0.5\\clip(0,37,1920,53.5)}"
                    + "aigan shitemo kongan shitemo kawaranai ya, mou"));
    }

    /**
     * CASO SÃO: fala de diálogo comum, sem tag pesada, nunca foi assunto desta fatia.
     */
    @Test
    @DisplayName("CASO SAO: dialogo simples continua fora de musica")
    void dialogoSimplesContinuaFora() {
        assertEquals(ClasseLinhaKaraoke.FORA_DE_MUSICA,
            classificador.classificar("Default", "Banagher, você está bem?"));
    }

    /**
     * A EVIDÊNCIA (c), que é contribuição de Paulo: o campo {@code Effect} do ASS.
     *
     * <p>Linha real do Unicorn. O estilo {@code OPL2} <b>não</b> é reconhecido como música pelo
     * nome — a fronteira de letra do padrão musical não alcança o {@code L2} colado. Sem o campo
     * {@code Effect} esta linha sairia da fatia e as 258 do acervo virariam eco. Com ele, fica.
     *
     * <p>Medido: das 133.951 linhas que a régua remove, exatamente <b>258</b> tinham
     * {@code Effect=fx} — e são todas estas.
     */
    @Test
    @DisplayName("EVIDENCIA (c): campo Effect=fx salva o OPL2, que o nome do estilo perde")
    void campoEfeitoSalvaOpl2() {
        String real = "{\\fad(200,200)\\bord0\\c&HFFFFFF&\\1a&H00&\\3c&H000000&"
            + "\\pos(960,1050)}Do you feel alone";

        assertEquals(ClasseLinhaKaraoke.FORA_DE_MUSICA,
            classificador.classificar("OPL2", real),
            "sem nenhuma evidencia o OPL2 sai — e e por isso que o campo Effect importa");

        assertEquals(ClasseLinhaKaraoke.TRADUZIVEL_INGLES,
            classificador.classificar("OPL2", real, new SinaisDeKaraoke("fx", false)),
            "com Effect=fx, que e o que o arquivo REAL traz, a letra do OPL2 volta a ser traduzida");
    }

    /**
     * A EVIDÊNCIA (d): camada romaji no mesmo instante. É a que cobre as obras em que o campo
     * {@code Effect} vem vazio — o 86 inteiro, 159.398 linhas, e o Guilty Crown.
     *
     * <p>Medido: ela salva 283 linhas que o campo {@code Effect} sozinho perderia. Por isso as
     * duas evidências convivem em vez de uma substituir a outra.
     */
    @Test
    @DisplayName("EVIDENCIA (d): camada romaji no mesmo instante prova que a linha e musica")
    void romajiNoMesmoInstanteProvaMusica() {
        String semEstiloMusical = "{\\an2\\pos(830,170)\\blur3.5\\clip(0,107,1920,123.5)}"
            + "Swept up and scattered by the wind";

        assertEquals(ClasseLinhaKaraoke.FORA_DE_MUSICA,
            classificador.classificar("Layer2", semEstiloMusical),
            "sem evidencia nenhuma, sai");

        assertEquals(ClasseLinhaKaraoke.TRADUZIVEL_INGLES,
            classificador.classificar("Layer2", semEstiloMusical, new SinaisDeKaraoke(null, true)),
            "havendo romaji no MESMO instante, a linha inglesa ao lado e a camada de traducao");
    }

    /**
     * A EVIDÊNCIA (e): o nome do estilo declara PAPEL DE CAMADA — {@code English}, {@code Romaji},
     * {@code Kanji}, {@code Lyrics}. Nasceu porque a régua removia junto a letra de obras cujo
     * estilo é o NOME DA MÚSICA: {@code Hey World English} 1.150 e {@code RISE LIGHT RISE
     * English} 927, ambas letra de verdade. São 2.179 eventos no acervo e nenhum estilo de
     * diálogo casa o padrão.
     */
    @Test
    @DisplayName("EVIDENCIA (e): estilo que declara PAPEL DE CAMADA e karaoke")
    void papelDeCamadaNoEstiloProvaKaraoke() {
        assertEquals(ClasseLinhaKaraoke.TRADUZIVEL_INGLES,
            classificador.classificar("Hey World English",
                "{\\blur2}My body's totally exhausted, and yet"),
            "letra real do Guilty Crown — 1.150 eventos que a regua levaria junto");

        assertEquals(ClasseLinhaKaraoke.TRADUZIVEL_INGLES,
            classificador.classificar("RISE LIGHT RISE English",
                "{\\fad(200,200)}In my own part of the world"));
    }

    /**
     * CASO DOENTE que a evidência (e) TRARIA de volta se viesse sozinha: no estilo
     * {@code English} há 10 eventos cujo "texto" é um traçado vetorial. Coordenada não é frase, e
     * mandá-la ao LLM é alucinação garantida — é a mesma classe dos 2.062 "cartazes" que o
     * projeto já pagou.
     */
    @Test
    @DisplayName("CASO DOENTE: comando de desenho vetorial NUNCA e letra")
    void comandoDeDesenhoNaoEhLetra() {
        assertEquals(ClasseLinhaKaraoke.EFEITO_KFX,
            classificador.classificar("English", "{\\p1}m 0 476 l 2000 476 2000 576 0 576"),
            "tracado vetorial entrou como letra — foi a evidencia (e) criando o problema que a "
                + "regua acabou de resolver");

        // O estilo "EG" NAO casa o papel de camada — e "E-G", nao "eng". Esta linha e preservada
        // por um motivo DIFERENTE: falta de evidencia nenhuma. Duas rotas ate o mesmo desfecho, e
        // o teste diz qual e qual — asserir "qualquer coisa menos traduzivel" aceitaria as duas
        // e nao guardaria nada.
        assertEquals(ClasseLinhaKaraoke.FORA_DE_MUSICA,
            classificador.classificar("EG", "{\\p1}m 240 0 l 1680 0 1680 1080 240 1080"),
            "sem nenhuma das cinco evidencias, a linha nem chega a ser assunto do karaoke");
    }

    /**
     * CONTRA-TESTE da guarda de desenho: ela não pode engolir letra de verdade. Uma letra com
     * número é comum ("One more time"), e uma com as letras de comando também.
     */
    @Test
    @DisplayName("CONTRA-TESTE: letra com numero e com as letras m/n/l/b/s/p/c continua letra")
    void guardaDeDesenhoNaoEngoleLetra() {
        assertEquals(ClasseLinhaKaraoke.TRADUZIVEL_INGLES,
            classificador.classificar("Opening", "{\\blur2}2 souls, 1 heart"),
            "numero na letra e comum; a guarda so pode pegar tracado, nao verso");
        assertEquals(ClasseLinhaKaraoke.ORIGINAL_JAPONES,
            classificador.classificar("OP - Romaji", "{\\blur2}mabuta no ura ni 100 no hoshi"),
            "romaji com numero tambem nao pode virar desenho");
    }

    /**
     * CONTRA-TESTE das duas evidências novas: elas não podem virar um "sim" universal.
     * {@link SinaisDeKaraoke#nenhum()} é o estado honesto de "não perguntei", e ele é o
     * RESTRITIVO — nunca aprova por omissão.
     */
    @Test
    @DisplayName("CONTRA-TESTE: sinal ausente NAO vira evidencia de musica")
    void sinalAusenteNaoAprovaPorOmissao() {
        String dialogo = "{\\clip(601,835,1685,924)\\pos(1142,935)\\blur1.15}Did you get Gyunei?!";

        assertEquals(ClasseLinhaKaraoke.FORA_DE_MUSICA,
            classificador.classificar("Dialogue", dialogo, SinaisDeKaraoke.nenhum()));
        assertEquals(ClasseLinhaKaraoke.FORA_DE_MUSICA,
            classificador.classificar("Dialogue", dialogo, new SinaisDeKaraoke("", false)),
            "campo Effect VAZIO e ausencia de evidencia, nao evidencia de karaoke");
        assertEquals(ClasseLinhaKaraoke.FORA_DE_MUSICA,
            classificador.classificar("Dialogue", dialogo, new SinaisDeKaraoke("Fixed", false)),
            "'Fixed' e anotacao de typesetter, nao carimbo do Kara Templater");
        assertEquals(ClasseLinhaKaraoke.FORA_DE_MUSICA,
            classificador.classificar("Dialogue", dialogo, null),
            "sinais nulos caem no lado restritivo, nunca no permissivo");
    }
}
