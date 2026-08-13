package org.traducao.projeto.raspagemRevisao.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fecha a metade que {@code CorrecaoChegaAoArquivoTest} não alcançou. Aquele
 * prova o caminho DETERMINÍSTICO; este prova o caminho do LLM — que é a única rota para a maioria
 * das classes de defeito, porque a regra local só cobre parentesco, "graças a Deus", insulto forte
 * e o artigo de mobile suit.
 *
 * <h2>O defeito escolhido é o que NÃO tem conserto local</h2>
 * {@code "Ela está muito cansado"} é detectada pela concordância nominal e nenhuma regra
 * determinística a resolve — foi exatamente o caso que o teste anterior deixou registrado como
 * limite, preservando a fala sem corrigi-la. Aqui ela chega ao modelo.
 *
 * <h2>O que este teste prova, e o que continua aberto</h2>
 * Prova o MECANISMO: detectar → mascarar → pedir ao modelo → desmascarar → julgar na
 * {@code GuardaCorrecaoSegura} → gravar no {@code .ass}. Não prova, e não tem como provar sem
 * infraestrutura, se a aya ou o mistral REAIS escrevem português melhor — isso é medição de
 * modelo, e depende do LM Studio no ar.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A correção do modelo só entra no arquivo depois de aprovada pela guarda.</li>
 *   <li>Resposta que não melhora a auditoria é RECUSADA, e a fala original é preservada — não
 *       basta o modelo responder.</li>
 *   <li>Fala sã não pode chegar ao modelo: consultar custa segundos e cota.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem rede: {@link LlmCorretorDublado} responde localmente aplicando a troca SOBRE o texto
 * recebido, para não perder os marcadores de tag e de lore.
 */
@QuarkusTest
@TestProfile(CorrecaoViaLlmChegaAoArquivoTest.PerfilComLlmDublado.class)
class CorrecaoViaLlmChegaAoArquivoTest {

    /** Escopa a alternativa a este teste; um dublê global mudaria a suíte inteira. */
    public static class PerfilComLlmDublado implements QuarkusTestProfile {
        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(LlmCorretorDublado.class);
        }
    }

    @Inject
    RevisarLegendasUseCase useCase;

    @Inject
    LlmCorretorDublado llm;

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

    @BeforeEach
    void limparDuble() {
        llm.reiniciar();
    }

    private record Fala(String ingles, String portugues) {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: monta a legenda PT a auditar e o cache com o inglês de referência.
     * <p>INVARIANTES DO DOMÍNIO: índice, estilo e ordem casam entre o .ass e o cache.
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga {@link IOException}.
     */
    private Path montar(Path temp, List<Fala> falas) throws IOException {
        Path pastaPt = Files.createDirectory(temp.resolve("pt"));
        Path pastaCache = Files.createDirectory(temp.resolve("cache"));
        StringBuilder ass = new StringBuilder(CABECALHO);
        List<EntradaCache> entradas = new ArrayList<>();
        for (int i = 0; i < falas.size(); i++) {
            ass.append("Dialogue: 0,0:00:0").append(i).append(".00,0:00:09.00,Default,,0,0,0,,")
               .append(falas.get(i).portugues()).append('\n');
            entradas.add(new EntradaCache(
                i, "Default", falas.get(i).ingles(), falas.get(i).portugues(), "en", "pt"));
        }
        Files.writeString(pastaPt.resolve("show_PT-BR.ass"), ass.toString(), StandardCharsets.UTF_8);
        mapper.writerWithDefaultPrettyPrinter().writeValue(
            pastaCache.resolve("show_ENG.cache.json").toFile(),
            new CacheDocumento(new ProvenienciaCache(
                ProvenienciaCache.SCHEMA_ATUAL, "danmachi", "h", "m", "en", "pt"), entradas));
        return pastaPt;
    }

    /** Roda a revisão em modo LLM e devolve o arquivo de saída, se houve gravação. */
    private Optional<String> revisar(Path temp, Path pastaPt) throws IOException {
        Path pastaSaida = Files.createDirectory(temp.resolve("saida"));
        useCase.executar(pastaPt, null, temp.resolve("cache"), pastaSaida,
            ModoRevisaoLegendas.LLM_CONCORDANCIA, "danmachi", ModoReferenciaRevisao.AMBOS);
        Path destino = pastaSaida.resolve("show_PT-BR.ass");
        return Files.exists(destino)
            ? Optional.of(Files.readString(destino, StandardCharsets.UTF_8))
            : Optional.empty();
    }

    private static long contarDialogos(String conteudo) {
        return conteudo.lines().filter(l -> l.startsWith("Dialogue:")).count();
    }

    @Test
    @DisplayName("o defeito sem conserto local vai ao LLM e VOLTA corrigido no .ass")
    void concordanciaNominalCorrigidaPeloLlmChegaAoArquivo(@TempDir Path temp) throws IOException {
        llm.ensinar("cansado", "cansada");
        Path pastaPt = montar(temp, List.of(
            new Fala("She is very tired.", "Ela está cansado."),
            new Fala("The weather is nice today.", "O tempo está bom hoje.")));

        String saida = revisar(temp, pastaPt).orElseThrow(() -> new AssertionError(
            "O CAMINHO DO LLM NÃO GRAVOU NADA. O defeito é detectado, o modelo respondeu com a "
                + "correção certa, e mesmo assim nada chegou ao arquivo — é assim que a fatia "
                + "falharia se a guarda rejeitasse correção legítima ou a escrita fosse cegada."));

        assertTrue(saida.contains("Ela está cansada."),
            "a correção do modelo não chegou ao arquivo:\n" + saida);
        assertTrue(saida.contains("O tempo está bom hoje."),
            "a fala sã ao lado foi alterada");
        assertEquals(2, contarDialogos(saida), "nenhuma fala pode sumir na gravação");
        assertEquals(1, llm.chamadas(),
            "só a fala defeituosa pode ir ao modelo — consultar a sã custa segundos e cota");
    }

    /**
     * CONTROLE que separa "o pipeline não chamou" de "o pipeline chamou e o modelo não resolveu".
     * O modelo responde, mas devolve o texto igual: a resposta é recusada por ausência de melhoria
     * e a fala original é preservada. Sem este teste, o anterior poderia estar passando por um
     * caminho que aceita qualquer resposta.
     */
    @Test
    @DisplayName("controle: modelo que responde sem melhorar é RECUSADO e a fala é preservada")
    void respostaSemMelhoriaNaoEntraNoArquivo(@TempDir Path temp) throws IOException {
        llm.responderSemAlterar();
        Path pastaPt = montar(temp, List.of(
            new Fala("She is very tired.", "Ela está cansado.")));

        Optional<String> saida = revisar(temp, pastaPt);

        assertTrue(llm.chamadas() >= 1,
            "o modelo TEM de ter sido consultado — senão este não é o controle que eu penso que é");
        String texto = saida.orElse(Files.readString(
            pastaPt.resolve("show_PT-BR.ass"), StandardCharsets.UTF_8));
        assertTrue(texto.contains("Ela está cansado."),
            "resposta sem melhoria não pode entrar no arquivo, e a fala original tem de sobreviver:\n"
                + texto);
        assertEquals(1, contarDialogos(texto), "a fala não pode sumir por não ter sido corrigida");
    }

    @Test
    @DisplayName("arquivo são não consulta o modelo nem regrava")
    void arquivoSaoNaoChegaAoModelo(@TempDir Path temp) throws IOException {
        llm.ensinar("cansado", "cansada");
        Path pastaPt = montar(temp, List.of(
            new Fala("She is very tired.", "Ela está cansada."),
            new Fala("The weather is nice today.", "O tempo está bom hoje.")));

        Optional<String> saida = revisar(temp, pastaPt);

        assertEquals(0, llm.chamadas(),
            "fala correta chegou ao modelo: em um episódio de 400 falas isso é custo puro");
        assertTrue(saida.isEmpty(),
            "arquivo são foi regravado sem nada ter mudado:\n" + saida.orElse(""));
    }
}
