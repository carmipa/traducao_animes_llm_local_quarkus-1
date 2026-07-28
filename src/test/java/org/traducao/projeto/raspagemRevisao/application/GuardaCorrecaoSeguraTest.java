package org.traducao.projeto.raspagemRevisao.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.raspagemRevisao.domain.ContextoRevisao;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa o portão único de correção. Ele decide o que entra numa legenda já
 * publicada, e as três origens de proposta (regra determinística, memória do arquivo, IA/Google)
 * passam pela MESMA régua — este teste é o que impede a régua de se soltar de uma delas.
 *
 * <p>INVARIANTES DO DOMÍNIO: o portão só aprova com melhora mensurável; termo canônico alterado e
 * proposta idêntica à fala atual são vetos incondicionais.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer aprovação indevida reprova o teste.
 */
@QuarkusTest
class GuardaCorrecaoSeguraTest {

    @Inject
    GuardaCorrecaoSegura guarda;

    private static final ContextoRevisao SEM_LORE = new ContextoRevisao("teste", "", Set.of());

    private ContextoRevisao comTermo(String termo) {
        return new ContextoRevisao("teste", "", Set.of(termo));
    }

    private ResultadoDeteccaoConcordancia suspeitaCom(String... motivos) {
        return new ResultadoDeteccaoConcordancia(true, List.of(motivos));
    }

    private boolean aprovou(GuardaCorrecaoSegura.Veredicto veredicto) {
        return veredicto instanceof GuardaCorrecaoSegura.Veredicto.Aprovada;
    }

    private List<String> avisos(GuardaCorrecaoSegura.Veredicto veredicto) {
        return assertInstanceOf(GuardaCorrecaoSegura.Veredicto.Rejeitada.class, veredicto)
            .avisosAoOperador();
    }

    @Test
    void propostaVaziaOuNulaEhRejeitadaEmSilencio() {
        for (String candidata : new String[]{null, "", "   "}) {
            GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
                "He is tired.", "Ele está cansada.", candidata,
                suspeitaCom("concordância de gênero"), SEM_LORE);

            assertFalse(aprovou(veredicto), "candidata=[" + candidata + "] não podia passar");
            assertTrue(avisos(veredicto).isEmpty(),
                "rejeição por texto vazio não fala com o operador: o desfecho já aparece como pendente");
        }
    }

    /**
     * Não é purismo: aprovar uma proposta igual à fala atual contaria uma correção que não houve,
     * marcaria o arquivo como modificado e dispararia regravação e backup de um .ass idêntico.
     */
    @Test
    void propostaIdenticaAFalaAtualEhRejeitada() {
        String atual = "Ele está cansada.";

        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            "He is tired.", atual, atual, suspeitaCom("concordância de gênero"), SEM_LORE);

        assertFalse(aprovou(veredicto));
    }

    /**
     * O veto de lore é o único que o operador PRECISA ver: a proposta parecia boa e foi barrada por
     * um termo da obra, e é ele quem decide se o termo está certo no catálogo.
     */
    @Test
    void alterarTermoCanonicoRejeitaEAvisaOperador() {
        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            "The Hyaku Shiki is ready.", "O Hyaku Shiki está pronta.", "O Cem Tipos está pronto.",
            suspeitaCom("concordância de gênero"), comTermo("Hyaku Shiki"));

        assertFalse(aprovou(veredicto), "trocar termo canônico não pode passar");
        assertEquals(1, avisos(veredicto).size());
        assertTrue(avisos(veredicto).get(0).contains("Hyaku Shiki"),
            "o aviso precisa nomear o termo, senão o operador não sabe o que revisar");
    }

    /** A concordância corrigida é o caso central da Opção 6: sai suspeita, entra limpa. */
    @Test
    void correcaoQueResolveOProblemaEhAprovada() {
        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            "He is tired.", "Ele está cansada.", "Ele está cansado.",
            suspeitaCom("concordância de gênero"), SEM_LORE);

        assertTrue(aprovou(veredicto), "correção válida foi barrada pelo portão");
    }

    /**
     * A pergunta que dá sentido às outras quatro: gastar uma chamada externa, um backup e uma
     * regravação para trocar um defeito por outro do mesmo tamanho não é melhora.
     */
    @Test
    void propostaQueNaoMelhoraAAuditoriaEhRejeitada() {
        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            "He is tired.", "Ele está cansada.", "Ela está cansado.",
            suspeitaCom("concordância de gênero"), SEM_LORE);

        assertFalse(aprovou(veredicto),
            "proposta ainda suspeita e sem redução de motivos não pode substituir a fala");
    }

    /**
     * A lista de avisos é entregue ao laço, que a imprime. Se fosse mutável, um chamador distraído
     * poderia alterar a narração de uma decisão já tomada.
     */
    @Test
    void avisosSaoImutaveis() {
        List<String> lista = avisos(guarda.avaliar(
            "The Hyaku Shiki is ready.", "O Hyaku Shiki está pronta.", "O Cem Tipos está pronto.",
            suspeitaCom("concordância de gênero"), comTermo("Hyaku Shiki")));

        assertThrowsUnsupported(() -> lista.add("intruso"));
    }

    private void assertThrowsUnsupported(Runnable acao) {
        try {
            acao.run();
        } catch (UnsupportedOperationException esperado) {
            return;
        }
        throw new AssertionError("a lista de avisos aceitou mutação");
    }
}
