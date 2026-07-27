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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fecha a lacuna que o teste de interrupção declarou — o DRENO do laço de
 * falas e o REUSO da memória de sessão. As duas revisões externas marcaram estes cenários como o
 * mínimo obrigatório antes de decompor o laço, e nenhum deles tinha teste.
 *
 * <h2>Como o dublê torna isso determinístico</h2>
 * A {@link RecuperacaoExternaContadora} substitui o tradutor externo por uma alternativa CDI
 * escopada a ESTE perfil de teste — não há mock global trocando o bean para a suíte inteira. Ela
 * conta as chamadas e sabe interromper a thread numa fala específica, o que é a única forma de
 * observar a interrupção acontecendo NO MEIO da varredura.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Sem rede: o dublê responde localmente.</li>
 *   <li>A flag de interrupção é limpa no {@code finally} — deixá-la marcada contaminaria os testes
 *       seguintes.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Uma decomposição que transforme o dreno em abort, ou que perca a memória de sessão, reprova aqui
 * — que é exatamente o que estes testes existem para impedir.
 */
@QuarkusTest
@TestProfile(DrenoLacoFalasCaracterizacaoTest.PerfilComTradutorDublado.class)
class DrenoLacoFalasCaracterizacaoTest {

    /** Escopa a alternativa a este teste; um {@code @Mock} global mudaria a suíte inteira. */
    public static class PerfilComTradutorDublado implements QuarkusTestProfile {
        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(RecuperacaoExternaContadora.class);
        }
    }

    @Inject
    RevisarLegendasUseCase useCase;

    @Inject
    RecuperacaoExternaContadora tradutor;

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
        tradutor.reiniciar();
    }

    private static long contarDialogos(String conteudo) {
        return conteudo.lines().filter(l -> l.startsWith("Dialogue:")).count();
    }

    /**
     * Monta uma pasta com uma legenda cujas falas REGREDIRAM ao inglês (logo são auditáveis e vão
     * ao tradutor) e o cache com os originais correspondentes.
     */
    private Path montar(Path temp, List<String> textos) throws IOException {
        Path pastaPt = Files.createDirectory(temp.resolve("pt"));
        Path pastaCache = Files.createDirectory(temp.resolve("cache"));

        StringBuilder ass = new StringBuilder(CABECALHO);
        List<EntradaCache> entradas = new ArrayList<>();
        for (int i = 0; i < textos.size(); i++) {
            ass.append("Dialogue: 0,0:00:0").append(i).append(".00,0:00:09.00,Default,,0,0,0,,")
               .append(textos.get(i)).append('\n');
            entradas.add(new EntradaCache(i, "Default", textos.get(i), textos.get(i), "en", "pt"));
        }
        Files.writeString(pastaPt.resolve("show_PT-BR.ass"), ass.toString(), StandardCharsets.UTF_8);
        mapper.writerWithDefaultPrettyPrinter().writeValue(
            pastaCache.resolve("show_ENG.cache.json").toFile(),
            new CacheDocumento(new ProvenienciaCache(
                ProvenienciaCache.SCHEMA_ATUAL, "danmachi", "h", "m", "en", "pt"), entradas));
        return pastaPt;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: interromper NO MEIO de um arquivo não pode deixá-lo pela metade.
     *
     * <p>É o cenário que o teste de interrupção anterior declarou como lacuna. A thread é
     * interrompida durante a terceira chamada ao tradutor; a partir dali o laço só copia. O arquivo
     * de saída tem de conter TODAS as falas da entrada.
     */
    @Test
    @DisplayName("Dreno: interrompido na 3ª fala, a saída ainda tem todas as falas")
    void interrupcaoNoMeioDrenaOArquivo(@TempDir Path temp) throws IOException {
        List<String> textos = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            textos.add("This line is still in english number " + i + ".");
        }
        Path pastaPt = montar(temp, textos);
        Path pastaSaida = Files.createDirectory(temp.resolve("saida"));
        tradutor.interromperNaChamada(3);

        try {
            useCase.executar(pastaPt, null, temp.resolve("cache"), pastaSaida,
                ModoRevisaoLegendas.GOOGLE, "danmachi", ModoReferenciaRevisao.AMBOS);
        } finally {
            Thread.interrupted();
        }

        assertEquals(3, tradutor.chamadas(),
            "a varredura para de chamar o tradutor assim que a flag aparece");
        Path destino = pastaSaida.resolve("show_PT-BR.ass");
        assertTrue(Files.exists(destino), "o que foi corrigido antes da parada é gravado");
        assertEquals(textos.size(), contarDialogos(Files.readString(destino, StandardCharsets.UTF_8)),
            "DRENO: as falas restantes são copiadas intactas. Menos falas na saída significa que "
                + "alguém trocou o dreno por um break e o cancelamento passou a destruir legenda");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a memória de sessão evita pagar duas vezes pela mesma fala.
     *
     * <p>Duas falas com texto IDÊNTICO só podem ir uma vez ao tradutor externo — a segunda reusa o
     * que a primeira trouxe. Elos sem estado compartilhado redisparariam a chamada, e o custo
     * apareceria como lentidão e cota, não como erro.
     */
    @Test
    @DisplayName("Sessão: falas idênticas no mesmo arquivo chamam o tradutor UMA vez")
    void memoriaDeSessaoEvitaChamadaRepetida(@TempDir Path temp) throws IOException {
        String repetida = "This exact line appears twice in the file.";
        Path pastaPt = montar(temp, List.of(repetida, "Another english line here.", repetida));
        Path pastaSaida = Files.createDirectory(temp.resolve("saida"));

        useCase.executar(pastaPt, null, temp.resolve("cache"), pastaSaida,
            ModoRevisaoLegendas.GOOGLE, "danmachi", ModoReferenciaRevisao.AMBOS);

        assertEquals(2, tradutor.chamadas(),
            "três falas, dois textos distintos: a repetida reusa a memória de sessão. "
                + "Três chamadas significam que a memória entre falas se perdeu");
        assertEquals(2, tradutor.pedidos().stream().distinct().count(),
            "nenhum texto foi pedido duas vezes");
    }
}
