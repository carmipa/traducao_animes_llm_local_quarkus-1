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
import org.traducao.projeto.raspagemRevisao.domain.ModoReferenciaRevisao;
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

/**
 * PROPÓSITO DE NEGÓCIO: caracteriza a PARADA COOPERATIVA da revisão — o que acontece quando o
 * operador aperta "Parar" no painel.
 *
 * <h2>São DOIS níveis, com semânticas opostas de propósito</h2>
 * <ul>
 *   <li><b>Laço de ARQUIVOS</b> ({@code RevisarLegendasUseCase:272}): {@code break}. Os arquivos
 *       restantes não são tocados; os já revisados ficaram salvos. Parar significa parar.</li>
 *   <li><b>Laço de FALAS</b> ({@code RevisarLegendasUseCase:436}): {@code continue} com flag. O
 *       arquivo CORRENTE é drenado — cada fala restante é copiada intacta para o documento de
 *       saída, e o que já havia sido corrigido é gravado. Parar no meio de um arquivo não pode
 *       deixá-lo pela metade.</li>
 * </ul>
 *
 * <p>O laço de falas tem 17 {@code continue} e ZERO {@code break}, e isso não é estilo: quem for
 * decompô-lo numa cadeia de elos vai querer um veredicto "pare o arquivo", e realizá-lo como
 * {@code break} produziria um {@code .ass} com falas FALTANDO. O operador que cancelou perderia
 * legenda em vez de apenas parar.
 *
 * <h2>O que este teste ainda NÃO cobre, e por quê</h2>
 * O dreno do laço interno exige interromper a thread NO MEIO de um arquivo, e não há costura para
 * isso: a porta de recuperação externa é injetada pelo contêiner e o projeto não tem infraestrutura
 * de mock (sem {@code quarkus-junit5-mockito}). O caminho seria um {@code @TestProfile} com uma
 * alternativa CDI contando chamadas e interrompendo na primeira. Fica declarado como lacuna, não
 * disfarçado por um teste que afirma menos do que o nome sugere.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Se um cancelamento passar a alterar arquivos, o operador perde a capacidade de abortar com
 * segurança — que é a única coisa que o botão "Parar" promete.
 */
@QuarkusTest
class InterrupcaoRevisaoCaracterizacaoTest {

    private static final int TOTAL_FALAS = 6;

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

    /**
     * PROPÓSITO DE NEGÓCIO: cancelar antes de o arquivo ser tocado não pode alterá-lo.
     *
     * <p>O cenário é montado para que a revisão TERIA trabalho a fazer: o cache é mais novo e traz
     * tradução para falas que regrediram ao inglês. Sem a interrupção, o arquivo seria sincronizado
     * e gravado. Com ela, nada acontece — é isso que separa "parou" de "parou pela metade".
     */
    @Test
    @DisplayName("Parada no laço de ARQUIVOS: nada é processado e a legenda fica intacta")
    void interrupcaoAntesDoArquivoNaoAlteraNada(@TempDir Path temp) throws IOException {
        Path pastaPt = Files.createDirectory(temp.resolve("pt"));
        Path pastaCache = Files.createDirectory(temp.resolve("cache"));
        Path pastaSaida = Files.createDirectory(temp.resolve("saida"));

        StringBuilder ass = new StringBuilder(CABECALHO);
        List<EntradaCache> entradas = new ArrayList<>();
        for (int i = 0; i < TOTAL_FALAS; i++) {
            ass.append("Dialogue: 0,0:00:0").append(i)
               .append(".00,0:00:09.00,Default,,0,0,0,,Good morning number ").append(i).append('\n');
            entradas.add(new EntradaCache(i, "Default",
                "Good morning number " + i, "Bom dia número " + i, "en", "pt"));
        }
        Path arquivo = pastaPt.resolve("show_PT-BR.ass");
        Files.writeString(arquivo, ass.toString(), StandardCharsets.UTF_8);
        String antes = Files.readString(arquivo, StandardCharsets.UTF_8);

        Path cache = pastaCache.resolve("show_ENG.cache.json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(cache.toFile(), new CacheDocumento(
            new ProvenienciaCache(ProvenienciaCache.SCHEMA_ATUAL, "danmachi", "h", "m", "en", "pt"),
            entradas));
        Files.setLastModifiedTime(cache, FileTime.from(
            Files.getLastModifiedTime(arquivo).toInstant().plusSeconds(30)));

        ResultadoRevisaoLegendas resultado;
        try {
            Thread.currentThread().interrupt();
            resultado = useCase.executar(pastaPt, null, pastaCache, pastaSaida,
                ModoRevisaoLegendas.GOOGLE, "danmachi", ModoReferenciaRevisao.AMBOS);
        } finally {
            // Limpa a flag: deixá-la marcada contaminaria os testes seguintes da mesma thread.
            Thread.interrupted();
        }

        assertEquals(0, resultado.arquivosAnalisados(),
            "o laço de arquivos para ANTES de abrir o primeiro");
        assertFalse(Files.exists(pastaSaida.resolve("show_PT-BR.ass")),
            "nenhum arquivo de saída é produzido por uma execução cancelada de saída");
        assertEquals(antes, Files.readString(arquivo, StandardCharsets.UTF_8),
            "a legenda de entrada continua byte a byte igual — cancelar não escreve");
        assertEquals(0, resultado.falasCorrigidas());
    }
}
