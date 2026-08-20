package org.traducao.projeto.traducaoKaraoke.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.traducaoKaraoke.domain.TelemetriaKaraoke;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: o backfill dos manifestos antigos só é legítimo se ele souber dizer o que
 * NÃO mediu. Este teste congela essa distinção.
 *
 * <h2>O que estava em jogo, medido em 2026-08-20</h2>
 * 19 manifestos, 383 arquivos, nove contadores reais por arquivo. Mas <b>zero dos 19</b> tem
 * {@code statusFinal}, {@code estadoDicionario}, {@code acentosRepostos} ou
 * {@code entradasCacheDescartadas}. Importar sem marcar a origem faria
 * {@code acentosRepostos = 0} significar ao mesmo tempo "medi e deu zero" e "não havia medição" —
 * a saída vazia ambígua, num dataset público onde ninguém pode conferir a diferença.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Se a marca de origem cair, o backfill vira fábrica de dado falso e nada no build avisa.
 */
class ImportadorManifestoKaraokeTest {

    @TempDir
    Path pasta;

    private final ImportadorManifestoKaraoke importador =
        new ImportadorManifestoKaraoke(new ObjectMapper());

    @Test
    @DisplayName("linha importada carrega os contadores REAIS e se declara histórica")
    void importaContadoresEMarcaOrigem() throws IOException {
        gravarManifesto("m1.json", ANTIGO);

        List<TelemetriaKaraoke> linhas = importador.importar(pasta, List.of());

        assertEquals(2, linhas.size());
        TelemetriaKaraoke primeira = linhas.get(0);
        assertEquals(TelemetriaKaraoke.Origem.MANIFESTO_HISTORICO.name(), primeira.origemDoRegistro(),
            "sem esta marca, os campos vazios do schema antigo viram dado falso");
        assertEquals("ep01.ass", primeira.arquivo());
        assertEquals(TelemetriaKaraoke.DesfechoDoArquivo.TRADUZIDO.name(), primeira.desfechoArquivo());

        // Os contadores que o manifesto TINHA chegam inteiros — e o teste usa numeros distintos,
        // porque com todos iguais uma troca de campo passaria verde.
        assertEquals(900, primeira.eventosTotais());
        assertEquals(400, primeira.efeitosKfxPreservados());
        assertEquals(120, primeira.preservadasOriginalJapones());
        assertEquals(5, primeira.jaEmPortugues());
        assertEquals(60, primeira.paraTraduzir());
        assertEquals(20, primeira.reaproveitadasCache());
        assertEquals(40, primeira.traduzidas());
        assertEquals(3, primeira.mantidasSemTraducao());
        assertEquals(List.of("marcador perdido"), primeira.avisos());
        assertEquals(2, primeira.arquivosNaExecucao());
        assertEquals(1234L, primeira.duracaoExecucaoMs());
    }

    /**
     * O invariante que separa backfill honesto de dado inventado: o que o manifesto não tinha sai
     * VAZIO, e nunca preenchido com um palpite plausível.
     */
    @Test
    @DisplayName("o que o schema antigo não media sai vazio, nunca adivinhado")
    void oQueNaoExistiaSaiVazio() throws IOException {
        gravarManifesto("m1.json", ANTIGO);

        TelemetriaKaraoke linha = importador.importar(pasta, List.of()).get(0);

        assertNull(linha.statusExecucao(),
            "afirmar que a execucao foi COMPLETA e justamente o que nao se sabe: o manifesto "
                + "antigo nao registrava falha por arquivo");
        assertNull(linha.estadoDicionario());
        assertNull(linha.motivoExecucao());
        assertEquals(0, linha.acentosRepostos());
        assertEquals(0, linha.entradasCacheDescartadas());
        // Contexto e modelo EXISTEM neste manifesto e tem de sobreviver — senao "nao media" e
        // "media e eu joguei fora" ficariam indistinguiveis.
        assertEquals("eight_six", linha.contextoId());
        assertEquals("aya-expanse-8b", linha.modeloLlm());
    }

    @Test
    @DisplayName("importar duas vezes não duplica: chave já no acervo é pulada")
    void naoDuplicaOQueJaEstaNoAcervo() throws IOException {
        gravarManifesto("m1.json", ANTIGO);

        List<TelemetriaKaraoke> primeira = importador.importar(pasta, List.of());
        List<String> chaves = primeira.stream()
            .map(l -> l.registradoEm() + '|' + l.arquivo()).toList();

        assertEquals(List.of(), importador.importar(pasta, chaves),
            "reimportar duplicaria o acervo local");
    }

    @Test
    @DisplayName("manifesto ilegível é pulado — não custa os outros")
    void manifestoIlegivelNaoDerrubaOsOutros() throws IOException {
        gravarManifesto("m0.json", "{ isto nao e json");
        gravarManifesto("m1.json", ANTIGO);

        assertEquals(2, importador.importar(pasta, List.of()).size());
    }

    @Test
    @DisplayName("pasta inexistente devolve lista vazia, sem lançar")
    void pastaInexistenteDevolveVazio() throws IOException {
        assertEquals(List.of(), importador.importar(pasta.resolve("nao-existe"), List.of()));
    }

    /**
     * O carimbo do manifesto é {@code LocalDateTime} sem fuso; o acervo trabalha em instante UTC.
     * Sem a conversão, a chave não ordena junto com as linhas novas e o acervo perde a cronologia.
     */
    @Test
    @DisplayName("carimbo local vira instante UTC; ilegível devolve nulo em vez de inventar")
    void carimboConvertidoOuNulo() {
        String convertido = ImportadorManifestoKaraoke.carimboDe("2026-08-14T21:34:21.123");
        assertNotNull(convertido);
        assertTrue(convertido.endsWith("Z"), "o carimbo nao virou instante UTC: " + convertido);

        assertNull(ImportadorManifestoKaraoke.carimboDe("ontem de tarde"));
        assertNull(ImportadorManifestoKaraoke.carimboDe(null));
        assertNull(ImportadorManifestoKaraoke.carimboDe("  "));
    }

    private void gravarManifesto(String nome, String conteudo) throws IOException {
        Files.writeString(pasta.resolve(nome), conteudo, StandardCharsets.UTF_8);
    }

    /** Manifesto no formato de 09/08/2026: tem contexto e modelo, não tem desfecho. */
    private static final String ANTIGO = """
        {
          "executadoEm": "2026-08-09T19:33:48.123",
          "duracaoMs": 1234,
          "contextoId": "eight_six",
          "contextoNome": "86 (Eighty-Six)",
          "contextoHash": "abc123",
          "modeloLlm": "aya-expanse-8b",
          "arquivos": [
            {
              "arquivo": "ep01.ass",
              "eventosTotais": 900,
              "efeitosKfxPreservados": 400,
              "preservadasOriginalJapones": 120,
              "jaEmPortugues": 5,
              "paraTraduzir": 60,
              "reaproveitadasCache": 20,
              "traduzidas": 40,
              "mantidasSemTraducao": 3,
              "avisos": ["marcador perdido"]
            },
            {
              "arquivo": "ep02.ass",
              "eventosTotais": 100,
              "efeitosKfxPreservados": 0,
              "preservadasOriginalJapones": 0,
              "jaEmPortugues": 0,
              "paraTraduzir": 10,
              "reaproveitadasCache": 10,
              "traduzidas": 0,
              "mantidasSemTraducao": 0,
              "avisos": []
            }
          ]
        }
        """;
}
