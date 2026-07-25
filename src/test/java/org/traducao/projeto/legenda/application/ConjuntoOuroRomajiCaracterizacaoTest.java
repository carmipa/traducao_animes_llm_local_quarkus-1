package org.traducao.projeto.legenda.application;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela, em números, o que o KRONOS decide HOJE sobre cada classe de linha
 * musical — a fotografia obrigatória antes de mexer na proteção de romaji (Fase 0 do Plano-Mestre
 * de 2026-07-25). Não afirma o que é certo; afirma o que É. Quando uma fase futura mudar um destes
 * números, a mudança aparece aqui explicitamente, em vez de passar despercebida.
 *
 * <p>O conjunto-ouro foi extraído do <b>cache versionado do próprio projeto</b> (62.409 entradas em
 * 237 caches) e o rótulo de verdade é o <b>nome do estilo</b> gravado pelo fansub no arquivo .ass.
 *
 * <p>INVARIANTES DO DOMÍNIO: o arquivo de apoio é imutável — regerá-lo muda a régua e exige revisar
 * estas asserções conscientemente. Cada linha tem classe, estilo, arquivo de origem e o texto
 * original como estava no cache, com as tags preservadas.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: ausência do recurso reprova o teste imediatamente — sem
 * conjunto-ouro não há Fase 0.
 */
class ConjuntoOuroRomajiCaracterizacaoTest {

    private static final String RECURSO = "/legenda/conjunto-ouro-romaji.tsv";

    /** Romaji legítimo, como o fansub escreveu. É o que JAMAIS pode ser traduzido. */
    private static final String ROMAJI = "ROMAJI";
    /** Estilo diz romaji, mas o texto já foi destruído antes (PT-BR colado). Alvo da limpeza de cache. */
    private static final String ROMAJI_CORROMPIDO = "ROMAJI_CORROMPIDO";
    /** Música em inglês: DEVE seguir traduzível. */
    private static final String MUSICA_EN = "MUSICA_EN";
    /** Fala comum: o controle do experimento. */
    private static final String DIALOGO = "DIALOGO";

    private static List<Linha> conjuntoOuro;

    private final DetectorEfeitoKaraokeService detector = new DetectorEfeitoKaraokeService();

    private record Linha(String classe, String estilo, String origem, String texto) {
    }

    @BeforeAll
    static void carregar() {
        conjuntoOuro = new ArrayList<>();
        try (InputStream in = ConjuntoOuroRomajiCaracterizacaoTest.class.getResourceAsStream(RECURSO)) {
            assertTrue(in != null, "conjunto-ouro ausente no classpath: " + RECURSO);
            try (BufferedReader leitor = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String linha;
                while ((linha = leitor.readLine()) != null) {
                    if (linha.isBlank() || linha.startsWith("#")) {
                        continue;
                    }
                    String[] campos = linha.split("\t", 4);
                    assertEquals(4, campos.length, "linha malformada no conjunto-ouro: " + linha);
                    conjuntoOuro.add(new Linha(campos[0], campos[1], campos[2], campos[3]));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<Linha> daClasse(String classe) {
        return conjuntoOuro.stream().filter(l -> l.classe().equals(classe)).toList();
    }

    private long preservadas(String classe) {
        return daClasse(classe).stream()
            .filter(l -> detector.devePreservarKaraokeOriginal(l.estilo(), l.texto()))
            .count();
    }

    @Test
    @DisplayName("composição do conjunto-ouro está congelada")
    void composicaoDoConjuntoOuroEstaCongelada() {
        assertEquals(535, conjuntoOuro.size());
        assertEquals(31, daClasse(ROMAJI).size());
        assertEquals(4, daClasse(ROMAJI_CORROMPIDO).size());
        assertEquals(250, daClasse(MUSICA_EN).size());
        assertEquals(250, daClasse(DIALOGO).size());
    }

    @Test
    @DisplayName("HOJE 29 das 31 linhas de romaji vazam para a tradução (o bug, medido)")
    void hojeVinteENoveLinhasDeRomajiVazamParaTraducao() {
        long preservadas = preservadas(ROMAJI);

        assertEquals(2, preservadas,
            "só o estilo com ESPAÇO (\"ED Roma L1\") é reconhecido como música hoje");
        assertEquals(29, daClasse(ROMAJI).size() - preservadas,
            "as 29 desprotegidas são exatamente as de estilo com SUBLINHADO (ED_S2_roma/OP_S2_roma): "
                + "o padrão de nome de estilo musical usa \\b, e sublinhado é caractere de palavra, "
                + "então a linha nem chega a ser tratada como música. A Fase 1 do Plano-Mestre deve "
                + "virar este número para 31/0 — e ESTE teste é quem prova.");
    }

    @Test
    @DisplayName("nenhuma fala nem música em inglês é preservada por engano")
    void nenhumaFalaNemMusicaEmInglesEPreservadaPorEngano() {
        assertEquals(0, preservadas(MUSICA_EN),
            "música em inglês precisa continuar traduzível");
        assertEquals(0, preservadas(DIALOGO),
            "diálogo não tem indicador de música e nunca chega à heurística de romaji");
    }

    @Test
    @DisplayName("o escore de romaji tem recall total no conjunto-ouro")
    void escoreDeRomajiTemRecallTotalNoConjuntoOuro() {
        List<Linha> romaji = daClasse(ROMAJI);
        long acimaDoLimiar = romaji.stream().filter(l -> detector.proporcaoRomaji(l.texto()) >= 70).count();

        assertEquals(romaji.size(), acimaDoLimiar,
            "o limiar de 70 precisa capturar TODO romaji legítimo do conjunto-ouro");
        assertTrue(romaji.stream().allMatch(l -> detector.proporcaoRomaji(l.texto()) >= 80),
            "medido em 2026-07-25: o pior caso do romaji real ainda pontua 80");
    }

    @Test
    @DisplayName("o escore quase não confunde música em inglês nem diálogo")
    void escoreQuaseNaoConfundeMusicaEmInglesNemDialogo() {
        long musica = daClasse(MUSICA_EN).stream().filter(l -> detector.proporcaoRomaji(l.texto()) >= 70).count();
        long diaologo = daClasse(DIALOGO).stream().filter(l -> detector.proporcaoRomaji(l.texto()) >= 70).count();

        assertEquals(2, musica, "falso positivo medido: 2 em 250 (0,8%)");
        assertEquals(2, diaologo, "falso positivo medido: 2 em 250 — inofensivo, pois diálogo não "
            + "tem indicador de música e não chega à heurística");
    }

    @Test
    @DisplayName("linhas já corrompidas não pontuam como romaji (alvo da limpeza de cache)")
    void linhasJaCorrompidasNaoPontuamComoRomaji() {
        List<Linha> corrompidas = daClasse(ROMAJI_CORROMPIDO);

        assertTrue(corrompidas.stream().allMatch(l -> detector.proporcaoRomaji(l.texto()) == 0),
            "são PT-BR colado sem espaço (\"quemtechamadementira\"): o romaji já foi destruído por "
                + "processamento anterior e nenhuma correção de código traz de volta — é a Fase 5");
        assertEquals(corrompidas.size(), preservadas(ROMAJI_CORROMPIDO),
            "hoje são preservadas pelo NOME do estilo, o que congela o dano dentro do cache");
    }
}
