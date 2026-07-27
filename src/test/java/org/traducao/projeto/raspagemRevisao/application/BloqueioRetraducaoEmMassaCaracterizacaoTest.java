package org.traducao.projeto.raspagemRevisao.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.cachetraducao.domain.CacheDocumento;
import org.traducao.projeto.cachetraducao.domain.EntradaCache;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.raspagemRevisao.domain.ModoRevisaoLegendas;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: caracteriza o BLOQUEIO POR RETRADUÇÃO EM MASSA — a proteção que impede a
 * revisão de assumir silenciosamente o trabalho da Tradução Local quando a legenda chega quase
 * inteira em inglês.
 *
 * <h2>Por que este teste existe agora</h2>
 * O bloqueio tinha teste da FÓRMULA do limiar, não do COMPORTAMENTO. E o comportamento tem uma
 * sutileza que nenhum teste cobria: quando o cache já havia restaurado alguma fala antes do
 * diagnóstico, o arquivo é <b>gravado assim mesmo</b>, com as recuperações, e só depois bloqueado.
 * "Bloqueou" NÃO é sinônimo de "não escreveu".
 *
 * <p>É a caracterização exigida antes de a FASE 4 extrair esta fase para um colaborador. Quem
 * extrair vai olhar para um método que devolve "bloquear: sim/não" e concluir que é um predicado
 * puro — não é: ele escreve no disco, cria backup, incrementa três contadores e registra um
 * detalhe de auditoria. Transformá-lo em predicado perderia as traduções recuperadas.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A pasta de SAÍDA é separada da de entrada, então "escreveu" é simplesmente "o arquivo
 *       existe lá" — e, como origem e destino diferem, nenhum backup é criado no projeto.</li>
 *   <li>Os dois casos afirmam que o LAÇO por fala não rodou ({@code falasCorrigidas == 0}): o
 *       bloqueio acontece antes dele, e é por isso que estes testes não tocam a rede.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Se o bloqueio deixar de gravar as recuperações, um operador que rodar a Opção 6 sobre um arquivo
 * mal traduzido perde as falas que o cache já sabia consertar, sem nenhum aviso.
 */
@QuarkusTest
class BloqueioRetraducaoEmMassaCaracterizacaoTest {

    /** Acima de 20 falas idênticas ao inglês E acima de 1/10 do auditável, o arquivo é bloqueado. */
    private static final int FALAS_EM_INGLES = 20;

    @Inject
    RevisarLegendasUseCase useCase;

    @Inject
    ObjectMapper mapper;

    private static final String CABECALHO = """
        [Script Info]
        ScriptType: v4.00+
        PlayResX: 1920
        PlayResY: 1080

        [V4+ Styles]
        Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
        Style: Default,Arial,20,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,0,2,10,10,10,1

        [Events]
        Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
        """;

    /** Frases inglesas distintas — texto repetido poderia ser deduplicado e falsear a contagem. */
    private static String fraseInglesa(int i) {
        return "The commander said we should move out at once, number " + i + ".";
    }

    private void escreverAss(Path arquivo, List<String> textos) throws IOException {
        StringBuilder sb = new StringBuilder(CABECALHO);
        for (String texto : textos) {
            sb.append("Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,").append(texto).append('\n');
        }
        Files.writeString(arquivo, sb.toString(), StandardCharsets.UTF_8);
    }

    private void escreverCache(Path arquivo, List<EntradaCache> entradas) throws IOException {
        ProvenienciaCache proveniencia = new ProvenienciaCache(
            ProvenienciaCache.SCHEMA_ATUAL, "danmachi", "hash", "modelo", "en", "pt");
        mapper.writerWithDefaultPrettyPrinter()
            .writeValue(arquivo.toFile(), new CacheDocumento(proveniencia, entradas));
    }

    private void tornarCacheMaisNovo(Path cache, Path legenda) throws IOException {
        FileTime tempoLegenda = Files.getLastModifiedTime(legenda);
        Files.setLastModifiedTime(cache, FileTime.from(tempoLegenda.toInstant().plusSeconds(30)));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o caso simples — nada foi recuperado, então o bloqueio não escreve nada
     * e o arquivo de saída sequer é criado.
     */
    @Test
    @DisplayName("Bloqueio SEM recuperação prévia: nenhum arquivo é escrito no destino")
    void bloqueiaSemEscreverQuandoNadaFoiRecuperado(@TempDir Path temp) throws IOException {
        Path pastaPt = Files.createDirectory(temp.resolve("pt"));
        Path pastaCache = Files.createDirectory(temp.resolve("cache"));
        Path pastaSaida = Files.createDirectory(temp.resolve("saida"));

        List<String> textos = new ArrayList<>();
        List<EntradaCache> entradas = new ArrayList<>();
        for (int i = 0; i < FALAS_EM_INGLES; i++) {
            textos.add(fraseInglesa(i));
            // traduzido == original: o cache não tem nada melhor a oferecer, logo não sincroniza
            entradas.add(new EntradaCache(i, "Default", fraseInglesa(i), fraseInglesa(i), "en", "pt"));
        }
        Path ass = pastaPt.resolve("show_PT-BR.ass");
        escreverAss(ass, textos);
        escreverCache(pastaCache.resolve("show_ENG.cache.json"), entradas);

        ResultadoRevisaoLegendas resultado = useCase.executar(
            pastaPt, null, pastaCache, pastaSaida,
            ModoRevisaoLegendas.GOOGLE, "danmachi",
            RevisarLegendasUseCase.ModoReferenciaRevisao.AMBOS);

        assertFalse(Files.exists(pastaSaida.resolve("show_PT-BR.ass")),
            "sem nada recuperado, o bloqueio não pode produzir arquivo de saída");
        assertEquals(0, resultado.falasCorrigidas(),
            "o laço por fala não roda: o bloqueio acontece antes dele");
        assertTrue(resultado.falasPendentes() >= FALAS_EM_INGLES,
            "as falas em inglês contam como pendentes, não como concluídas");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o caso que quase se perde numa extração — o cache restaurou uma fala
     * ANTES do diagnóstico, e essa recuperação é gravada apesar do bloqueio.
     *
     * <p>É o único caminho do método em que "bloqueado" e "arquivo alterado no destino" acontecem
     * juntos. Sem este teste, um colaborador extraído como predicado puro passaria em toda a suíte
     * e faria o operador perder a fala recuperada.
     */
    @Test
    @DisplayName("Bloqueio COM recuperação prévia: o arquivo É escrito, com a fala restaurada")
    void bloqueiaMasGravaOqueOCacheJaHaviaRecuperado(@TempDir Path temp) throws IOException {
        Path pastaPt = Files.createDirectory(temp.resolve("pt"));
        Path pastaCache = Files.createDirectory(temp.resolve("cache"));
        Path pastaSaida = Files.createDirectory(temp.resolve("saida"));

        List<String> textos = new ArrayList<>();
        List<EntradaCache> entradas = new ArrayList<>();
        for (int i = 0; i < FALAS_EM_INGLES; i++) {
            textos.add(fraseInglesa(i));
            entradas.add(new EntradaCache(i, "Default", fraseInglesa(i), fraseInglesa(i), "en", "pt"));
        }
        // A fala extra REGREDIU ao inglês, mas o cache guarda a tradução boa: será restaurada.
        textos.add("Good morning");
        entradas.add(new EntradaCache(
            FALAS_EM_INGLES, "Default", "Good morning", "Bom dia", "en", "pt"));

        Path ass = pastaPt.resolve("show_PT-BR.ass");
        escreverAss(ass, textos);
        Path cache = pastaCache.resolve("show_ENG.cache.json");
        escreverCache(cache, entradas);
        tornarCacheMaisNovo(cache, ass);

        ResultadoRevisaoLegendas resultado = useCase.executar(
            pastaPt, null, pastaCache, pastaSaida,
            ModoRevisaoLegendas.GOOGLE, "danmachi",
            RevisarLegendasUseCase.ModoReferenciaRevisao.AMBOS);

        Path destino = pastaSaida.resolve("show_PT-BR.ass");
        assertTrue(Files.exists(destino),
            "o bloqueio grava o que o cache recuperou — 'bloqueou' NÃO é 'não escreveu'");

        String conteudo = Files.readString(destino, StandardCharsets.UTF_8);
        assertTrue(conteudo.contains("Bom dia"),
            "a tradução que o cache já sabia consertar tem de sobreviver ao bloqueio");
        assertTrue(conteudo.contains(fraseInglesa(0)),
            "as falas em inglês continuam intactas: o bloqueio não as retraduz");
        assertEquals(0, resultado.falasCorrigidas(),
            "o que salvou a fala foi a SINCRONIZAÇÃO com o cache, não o laço de revisão");

        assertFalse(Files.readString(ass, StandardCharsets.UTF_8).contains("Bom dia"),
            "a legenda de ENTRADA não é tocada quando há pasta de saída separada");
    }
}
