package org.traducao.projeto.raspagemRevisao.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.cachetraducao.domain.CacheDocumento;
import org.traducao.projeto.cachetraducao.domain.EntradaCache;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: cobre o fluxo completo da Opção 6 no modo Cache
 * (endpoint → sincronização → gravação), garantindo que cache seguro corrige o
 * ASS e cache ausente/insegurо nunca produz sucesso silencioso.
 * <p>INVARIANTES DO DOMÍNIO: o vídeo/legenda EN nunca é obrigatório; a
 * proveniência e o vínculo por índice/estilo/texto governam qualquer escrita.
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem cache correspondente o arquivo fica
 * pendente; qualquer alteração indevida do ASS reprova o teste.
 */
@QuarkusTest
class RevisarLegendasCacheIntegracaoTest {

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

    private void escreverAss(Path arquivo, String texto) throws IOException {
        String linha = "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,," + texto + "\n";
        Files.writeString(arquivo, CABECALHO + linha, StandardCharsets.UTF_8);
    }

    private void escreverCache(Path arquivo, String contextoId, List<EntradaCache> entradas) throws IOException {
        ProvenienciaCache proveniencia = new ProvenienciaCache(
            ProvenienciaCache.SCHEMA_ATUAL, contextoId, "hash", "modelo", "en", "pt");
        CacheDocumento documento = new CacheDocumento(proveniencia, entradas);
        mapper.writerWithDefaultPrettyPrinter().writeValue(arquivo.toFile(), documento);
    }

    private void tornarCacheMaisNovo(Path cache, Path legenda) throws IOException {
        FileTime tempoLegenda = Files.getLastModifiedTime(legenda);
        Files.setLastModifiedTime(cache, FileTime.from(tempoLegenda.toInstant().plusSeconds(30)));
    }

    /**
     * Test 9 + sincronização segura: só com cache (sem legenda EN), uma fala que
     * regrediu ao inglês é restaurada para o português do cache.
     */
    @Test
    void modoCacheSemLegendaEnRestauraTraducaoSegura(@TempDir Path tempDir) throws IOException {
        Path pastaPt = Files.createDirectory(tempDir.resolve("pt"));
        Path pastaCache = Files.createDirectory(tempDir.resolve("cache"));

        Path ass = pastaPt.resolve("show_PT-BR.ass");
        escreverAss(ass, "Good morning"); // regrediu ao inglês

        Path cache = pastaCache.resolve("show_ENG.cache.json");
        escreverCache(cache, "danmachi", List.of(
            new EntradaCache(0, "Default", "Good morning", "Bom dia", "en", "pt")));
        tornarCacheMaisNovo(cache, ass);

        ResultadoRevisaoLegendas resultado = useCase.executar(
            pastaPt, null, pastaCache, null,
            RevisarLegendasUseCase.ModoRevisaoLegendas.GOOGLE, "danmachi",
            RevisarLegendasUseCase.ModoReferenciaRevisao.CACHE);

        assertEquals(1, resultado.arquivosAnalisados());
        String conteudo = Files.readString(ass, StandardCharsets.UTF_8);
        assertTrue(conteudo.contains("Bom dia"), "a tradução do cache deveria ter sido restaurada");
        assertFalse(conteudo.contains("Good morning"), "o inglês regredido deveria ter sido substituído");
    }

    /**
     * Test 6 — modo Cache sem cache correspondente para a legenda: o arquivo fica
     * BLOQUEADO/PENDENTE (status não é conclusão normal) e o ASS não é alterado.
     */
    @Test
    void modoCacheSemCacheCorrespondenteFicaPendente(@TempDir Path tempDir) throws IOException {
        Path pastaPt = Files.createDirectory(tempDir.resolve("pt"));
        Path pastaCache = Files.createDirectory(tempDir.resolve("cache"));

        Path ass = pastaPt.resolve("showA_PT-BR.ass");
        escreverAss(ass, "Uma fala qualquer");
        String antes = Files.readString(ass, StandardCharsets.UTF_8);

        // cache existe na pasta, mas é de OUTRA obra/episódio (não casa com showA)
        escreverCache(pastaCache.resolve("showB_ENG.cache.json"), "danmachi", List.of(
            new EntradaCache(0, "Default", "Whatever", "Qualquer", "en", "pt")));

        ResultadoRevisaoLegendas resultado = useCase.executar(
            pastaPt, null, pastaCache, null,
            RevisarLegendasUseCase.ModoRevisaoLegendas.GOOGLE, "danmachi",
            RevisarLegendasUseCase.ModoReferenciaRevisao.CACHE);

        assertEquals(1, resultado.arquivosAnalisados());
        assertTrue(resultado.falasPendentes() >= 1, "arquivo sem cache deve ficar pendente");
        assertEquals("CONCLUIDO_COM_PENDENCIAS", resultado.status());
        assertEquals(antes, Files.readString(ass, StandardCharsets.UTF_8), "o ASS não pode ser alterado");
    }

    /**
     * Buraco residual do bug 4 — um cache que é RESOLVIDO (mesmo código de episódio,
     * outra obra) mas que não casa com segurança com nenhuma fala não pode concluir
     * com sucesso silencioso: deve ficar pendente e não alterar o ASS.
     */
    @Test
    void modoCacheResolvidoQueNaoCasaNaoConcluiComSucesso(@TempDir Path tempDir) throws IOException {
        Path pastaPt = Files.createDirectory(tempDir.resolve("pt"));
        Path pastaCache = Files.createDirectory(tempDir.resolve("cache"));

        Path ass = pastaPt.resolve("MY_SHOW_S01E05_PT-BR.ass");
        escreverAss(ass, "Some english line still here");
        String antes = Files.readString(ass, StandardCharsets.UTF_8);

        // Mesmo código de episódio (S01E05) mas obra diferente e textos que não casam.
        escreverCache(pastaCache.resolve("OTHER_SHOW_S01E05_ENG.cache.json"), "danmachi", List.of(
            new EntradaCache(0, "Default", "Totally different EN", "Tradução diferente", "en", "pt")));

        ResultadoRevisaoLegendas resultado = useCase.executar(
            pastaPt, null, pastaCache, null,
            RevisarLegendasUseCase.ModoRevisaoLegendas.GOOGLE, "danmachi",
            RevisarLegendasUseCase.ModoReferenciaRevisao.CACHE);

        assertEquals(1, resultado.arquivosAnalisados());
        assertTrue(resultado.falasPendentes() >= 1, "cache que não casa não pode virar sucesso silencioso");
        assertEquals("CONCLUIDO_COM_PENDENCIAS", resultado.status());
        assertEquals(antes, Files.readString(ass, StandardCharsets.UTF_8), "o ASS não pode ser alterado");
    }
}
