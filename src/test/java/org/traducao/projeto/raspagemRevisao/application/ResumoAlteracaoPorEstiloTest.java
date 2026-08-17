package org.traducao.projeto.raspagemRevisao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que o resumo por estilo enxerga o dano que passou despercebido em
 * 17/08/2026 — música reescrita pela ponte do cache, com o console dizendo {@code corrigidas=0}.
 *
 * <p>INVARIANTES DO DOMÍNIO: música alterada tem de aparecer marcada; contagem de eventos
 * diferente é {@code NaoComparavel}, nunca zero.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: se este teste ficar verde com a marcação musical desligada,
 * o resumo virou enfeite e o veto volta a depender de alguém olhar o backup à mão.
 */
class ResumoAlteracaoPorEstiloTest {

    private final ResumoAlteracaoPorEstilo resumo =
        new ResumoAlteracaoPorEstilo(new PoliticaEstiloMusical(List.of()));

    private static EventoLegenda evento(int indice, String estilo, String texto) {
        return new EventoLegenda(indice, "Dialogue", estilo, "", texto);
    }

    private static ResumoAlteracaoPorEstilo.Resumo.Comparado comparado(
        ResumoAlteracaoPorEstilo.Resumo r) {
        return assertInstanceOf(ResumoAlteracaoPorEstilo.Resumo.Comparado.class, r);
    }

    /**
     * O CASO REAL, reduzido: a ponte restaurou letra de ED do cache e uma fala de diálogo. O
     * console diria {@code corrigidas=0} porque a ponte escreve fora do laço de correção.
     */
    @Test
    @DisplayName("musica reescrita aparece marcada e contada a parte")
    void acusaMusicaReescritaPelaPonteDoCache() {
        List<EventoLegenda> antes = List.of(
            evento(1, "Song ENG", "I was watching you as you,"),
            evento(2, "Song ENG", "were watching the sun rise."),
            evento(3, "Default", "Texto antigo"));
        List<EventoLegenda> depois = List.of(
            evento(1, "Song ENG", "Eu estava observando você"),
            evento(2, "Song ENG", "Estamos assistindo o sol nascer."),
            evento(3, "Default", "Texto novo"));

        var c = comparado(resumo.comparar(antes, depois));

        assertEquals(3, c.total(), "as três mudaram");
        assertEquals(2, c.totalMusical(),
            "as duas de Song ENG têm de ser contadas como MÚSICA — foi assim que 687 linhas do "
                + "08th passaram com o console dizendo corrigidas=0");
        var songEng = c.porEstilo().stream().filter(l -> l.estilo().equals("Song ENG")).findFirst();
        assertTrue(songEng.isPresent() && songEng.get().musical(), "Song ENG é música");
        var padrao = c.porEstilo().stream().filter(l -> l.estilo().equals("Default")).findFirst();
        assertTrue(padrao.isPresent() && !padrao.get().musical(), "Default não é música");
    }

    /** O CONTRA-CASO: corrida honesta de diálogo não pode acender o alarme. */
    @Test
    @DisplayName("corrida so de dialogo nao acusa musica")
    void corridaSoDeDialogoNaoAcusaMusica() {
        List<EventoLegenda> antes = List.of(
            evento(1, "Default", "Nao vou tambem."),
            evento(2, "nextep", "Proximo episodio"));
        List<EventoLegenda> depois = List.of(
            evento(1, "Default", "Não vou também."),
            evento(2, "nextep", "Próximo episódio"));

        var c = comparado(resumo.comparar(antes, depois));

        assertEquals(2, c.total());
        assertEquals(0, c.totalMusical(),
            "alarme falso ensina a desligar o alarme: diálogo não pode ser acusado de música");
        assertTrue(c.porEstilo().stream().noneMatch(ResumoAlteracaoPorEstilo.LinhaEstilo::musical));
    }

    /** Nada mudou É um resultado, e é diferente de não ter conseguido comparar. */
    @Test
    @DisplayName("arquivo intacto devolve total zero, nao NaoComparavel")
    void arquivoIntactoDevolveZeroComparado() {
        List<EventoLegenda> iguais = List.of(evento(1, "Default", "Igual"));

        var c = comparado(resumo.comparar(iguais, iguais));

        assertEquals(0, c.total());
        assertTrue(c.porEstilo().isEmpty());
    }

    /**
     * O TERCEIRO ESTADO. Tamanho diferente quebra o pareamento por posição; devolver zero aqui
     * seria "aprovar por cegueira" — a regra 23 em uma linha.
     */
    @Test
    @DisplayName("contagem de eventos diferente e NAO COMPARAVEL, nunca zero")
    void tamanhoDiferenteNaoPodeVirarZero() {
        List<EventoLegenda> antes = List.of(
            evento(1, "Default", "A"), evento(2, "Default", "B"));
        List<EventoLegenda> depois = List.of(evento(1, "Default", "A"));

        var r = resumo.comparar(antes, depois);

        var nc = assertInstanceOf(ResumoAlteracaoPorEstilo.Resumo.NaoComparavel.class, r,
            "tamanho diferente NAO pode sair como 'nada mudou'");
        assertTrue(nc.motivo().contains("2 -> 1"), "o motivo diz o que houve: " + nc.motivo());
    }

    /** Nulo também é não comparável — e o motivo vai junto. */
    @Test
    @DisplayName("nulo e NAO COMPARAVEL com motivo")
    void nuloNaoViraZero() {
        var r = resumo.comparar(null, List.of(evento(1, "Default", "A")));
        var nc = assertInstanceOf(ResumoAlteracaoPorEstilo.Resumo.NaoComparavel.class, r);
        assertFalse(nc.motivo().isBlank(), "guarda sem motivo é guarda muda");
    }
}
