package org.traducao.projeto.traducaoCorrige.application;

import org.traducao.projeto.traducao.application.ContextoCongeladoDaExecucao;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.qualidadeTraducao.application.DetectorTraducaoIdenticaService;
import org.traducao.projeto.traducao.infrastructure.adapters.LoreAtivaContextoAdapter;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: cobre a fronteira que impede o menu de apagar termos
 * legítimos da lore e garante que lacunas/fallbacks reais sejam reparáveis.
 *
 * <p>INVARIANTES DO DOMÍNIO: a decisão deriva da lore ativa e não de uma lista
 * fixa de um anime; expressões inglesas em Title Case continuam sendo falhas.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: cada cenário retorna status explícito, sem
 * depender de exceção ou igualdade ambígua.
 */
class ClassificadorEntradaCacheServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final GerenciadorContexto contexto = new GerenciadorContexto(List.of(new ContextoTeste()));
    private final ClassificadorEntradaCacheService service = new ClassificadorEntradaCacheService(
        new DetectorTraducaoIdenticaService(new LoreAtivaContextoAdapter(contexto, new ContextoCongeladoDaExecucao())),
        new ValidadorTraducaoService(),
        new PoliticaEstiloMusical(List.of("Song JP")),
        new DetectorEfeitoKaraokeService(),
        new ProtecaoLegendaAssService()
    );

    @Test
    void termoPresenteNaLorePodePermanecerIdentico() {
        assertEquals(ClassificadorEntradaCacheService.Status.VALIDA,
            service.classificar(entrada("Bell Cranel", "Bell Cranel")).status());
    }

    @Test
    void fraseInglesaTitleCaseNaoEhConfundidaComNome() {
        assertEquals(ClassificadorEntradaCacheService.Status.NAO_TRADUZIDA,
            service.classificar(entrada("Good Morning", "Good Morning")).status());
    }

    @Test
    void palavraInglesaCurtaETraducaoVaziaSaoCandidatos() {
        assertEquals(ClassificadorEntradaCacheService.Status.NAO_TRADUZIDA,
            service.classificar(entrada("Run!", "Run!")).status());
        assertEquals(ClassificadorEntradaCacheService.Status.VAZIA,
            service.classificar(entrada("Help!", "")).status());
    }

    @Test
    void romajiComTraducaoGravadaEDanoConsumadoENaoConteudoProtegido() {
        // Linha real do ED do Guilty Crown ep13: o cache guardava "Sonhonaa sekai..." como
        // tradução do romaji e reaplicava a salada a cada reprocessamento.
        ObjectNode envenenada = entrada("Sonna sekai ni nokosareta boku wa", "Sonhonaa sekai ni nokosareta");
        envenenada.put("estilo", "ED_S2_roma");

        ClassificadorEntradaCacheService.Classificacao cls = service.classificar(envenenada);

        assertEquals(ClassificadorEntradaCacheService.Status.ROMAJI_TRADUZIDO, cls.status());
        assertTrue(cls.precisaCorrecao(),
            "sem isto o cache devolve PROTEGIDA e CONGELA o estrago dentro do arquivo");
    }

    @Test
    void romajiIntactoNoCacheContinuaProtegidoSemCorrecao() {
        ObjectNode preservada = entrada("Sonna sekai ni nokosareta boku wa", "Sonna sekai ni nokosareta boku wa");
        preservada.put("estilo", "ED_S2_roma");

        ClassificadorEntradaCacheService.Classificacao cls = service.classificar(preservada);

        assertEquals(ClassificadorEntradaCacheService.Status.PROTEGIDA, cls.status());
        assertFalse(cls.precisaCorrecao(), "preservar não é dano: nada a corrigir");
    }

    @Test
    void naoApagaCacheComBaseNaHeuristicaDeSilabas() {
        // "E eu, assim, sozinha" pontua 75% na heurística de romaji porque português também
        // decompõe em sílabas abertas. Preservar por causa disso é barato; APAGAR destruiria
        // tradução legítima, então a limpeza exige estilo declarado ou escrita japonesa.
        ObjectNode portuguesQuePareceRomaji = entrada("E eu, assim, sozinha...", "Eu, assim, tão sozinha...");
        portuguesQuePareceRomaji.put("estilo", "Karaoke Simples");

        ClassificadorEntradaCacheService.Classificacao cls = service.classificar(portuguesQuePareceRomaji);

        assertEquals(ClassificadorEntradaCacheService.Status.PROTEGIDA, cls.status());
        assertFalse(cls.precisaCorrecao(), "apagar cache erra para o lado de NÃO apagar");
    }

    @Test
    void escritaJaponesaComTraducaoGravadaTambemEDano() {
        ObjectNode kana = entrada("そんな世界に残された僕は", "Neste mundo em que fui deixado");
        kana.put("estilo", "Song JP");

        assertEquals(ClassificadorEntradaCacheService.Status.ROMAJI_TRADUZIDO,
            service.classificar(kana).status());
    }

    @Test
    void musicaEmInglesComTraducaoLegitimaNaoEConfundidaComDano() {
        ObjectNode legitima = entrada("But no matter how bad a fight we'd have", "Não importa o quanto brigássemos");
        legitima.put("estilo", "Opening");

        assertEquals(ClassificadorEntradaCacheService.Status.VALIDA, service.classificar(legitima).status());
    }

    private ObjectNode entrada(String original, String traduzido) {
        ObjectNode no = mapper.createObjectNode();
        no.put("indice", 1);
        no.put("estilo", "Default");
        no.put("original", original);
        no.put("traduzido", traduzido);
        return no;
    }

    /** Provedor mínimo cuja lore permite provar proteção sem listas hardcoded. */
    private static final class ContextoTeste implements ProvedorContexto {
        private static final String PROMPT = ContextoPrompt.montar("Teste", "Principais nomes: Bell Cranel, Hestia.");
        @Override public String getId() { return "danmachi"; }
        @Override public String getNomeExibicao() { return "Teste"; }
        @Override public String obterPromptSistema() { return PROMPT; }
    }
}
