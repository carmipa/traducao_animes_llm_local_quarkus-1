package org.traducao.projeto.traducaoCorrige.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.traducao.application.DetectorTraducaoIdenticaService;
import org.traducao.projeto.traducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.traducao.application.ValidadorTraducaoService;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        new DetectorTraducaoIdenticaService(contexto),
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
