package org.traducao.projeto.raspagemRevisao.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.raspagemRevisao.domain.ContextoRevisao;
import org.traducao.projeto.raspagemRevisao.domain.DecisaoFala;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa a pergunta "esta fala está errada?" — a última antes de a linha custar
 * uma chamada externa. O caso que este teste protege de verdade é o da lore: numa obra cheia de nome
 * próprio, a fala CERTA é idêntica ao inglês, e sem a exceção ela seria mandada para tradução.
 *
 * <p>INVARIANTES DO DOMÍNIO: dispensar é sempre manter; suspeitar entrega a auditoria junto.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: dispensar uma fala errada ou suspeitar de uma certa reprova.
 */
@QuarkusTest
class TriagemFalaSuspeitaTest {

    @Inject
    TriagemFalaSuspeita triagem;

    private EventoLegenda fala(String texto) {
        return new EventoLegenda(7, "Dialogue", "Default",
            "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,", texto);
    }

    private ContextoRevisao contextoCom(String... termos) {
        return new ContextoRevisao("teste", "", Set.of(termos));
    }

    private DecisaoFala dispensa(TriagemFalaSuspeita.Resultado resultado) {
        return assertInstanceOf(TriagemFalaSuspeita.Resultado.Dispensada.class, resultado).decisao();
    }

    /**
     * O coração da exceção: "Hyaku Shiki" em PT é "Hyaku Shiki". Idêntico ao inglês aqui é CERTO,
     * não é tradução que faltou.
     */
    @Test
    void falaIdenticaAoInglesPorSerSoTermoCanonicoEhDispensada() {
        TriagemFalaSuspeita.Resultado resultado = triagem.triar(
            fala("Hyaku Shiki"), "Hyaku Shiki", "Hyaku Shiki", true, contextoCom("Hyaku Shiki"));

        DecisaoFala decisao = dispensa(resultado);
        assertInstanceOf(DecisaoFala.Manter.class, decisao, "dispensar é sempre manter");
        assertEquals(1, decisao.avisosAoOperador().size(),
            "o operador precisa ver por que uma fala igual ao inglês não foi tocada");
        assertTrue(decisao.avisosAoOperador().get(0).contains("[LORE]"));
    }

    /**
     * As três condições valem JUNTAS. Aqui há termo canônico, mas há também texto em volta que
     * precisa de tradução — dispensar esta fala deixaria inglês na tela.
     */
    @Test
    void termoCanonicoComTextoEmVoltaNaoEhDispensadoPelaLore() {
        TriagemFalaSuspeita.Resultado resultado = triagem.triar(
            fala("Hyaku Shiki is falling"), "Hyaku Shiki is falling", "Hyaku Shiki is falling",
            true, contextoCom("Hyaku Shiki"));

        assertFalse(resultado instanceof TriagemFalaSuspeita.Resultado.Dispensada dispensada
                && dispensada.decisao().avisosAoOperador().stream().anyMatch(a -> a.contains("[LORE]")),
            "a exceção de lore não pode cobrir uma fala que tem texto a traduzir em volta");
    }

    /** Fala idêntica ao inglês SEM termo canônico é tradução que não aconteceu: tem de ser pega. */
    @Test
    void falaIdenticaAoInglesSemTermoCanonicoEhSuspeita() {
        TriagemFalaSuspeita.Resultado resultado = triagem.triar(
            fala("Get out of there!"), "Get out of there!", "Get out of there!", true,
            contextoCom());

        assertInstanceOf(TriagemFalaSuspeita.Resultado.Suspeita.class, resultado,
            "português idêntico ao inglês, sem lore que justifique, é falha de tradução");
    }

    /** Tradução correta não vira trabalho: sem problema não há o que relatar nem o que corrigir. */
    @Test
    void traducaoCorretaEhDispensadaEmSilencio() {
        TriagemFalaSuspeita.Resultado resultado = triagem.triar(
            fala("Ele está cansado."), "He is tired.", "Ele está cansado.", true, contextoCom());

        DecisaoFala decisao = dispensa(resultado);
        assertInstanceOf(DecisaoFala.Manter.class, decisao);
        assertTrue(decisao.avisosAoOperador().isEmpty(),
            "fala sem problema não gera linha no console — o operador acompanha um lote inteiro");
    }

    /**
     * A auditoria vem JUNTO da suspeita porque é a base de comparação do portão de segurança.
     * Se viesse vazia, qualquer proposta pareceria melhoria.
     */
    @Test
    void suspeitaEntregaAAuditoriaComOsMotivos() {
        TriagemFalaSuspeita.Resultado resultado = triagem.triar(
            fala("Ele está cansada."), "He is tired.", "Ele está cansada.", true, contextoCom());

        var suspeita = assertInstanceOf(TriagemFalaSuspeita.Resultado.Suspeita.class, resultado);
        assertTrue(suspeita.auditoria().suspeito());
        assertFalse(suspeita.auditoria().motivos().isEmpty(),
            "sem motivos não há como medir se uma correção melhorou a fala");
    }
}
