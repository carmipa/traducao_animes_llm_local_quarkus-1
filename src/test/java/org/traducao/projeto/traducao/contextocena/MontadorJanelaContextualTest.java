package org.traducao.projeto.traducao.contextocena;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.traducao.application.ClassificadorPendenciaTelemetria;
import org.traducao.projeto.traducao.application.contextocena.MontadorJanelaContextual;
import org.traducao.projeto.traducao.domain.contextocena.JanelaContextual;
import org.traducao.projeto.traducao.domain.contextocena.LinhaAlvoContextual;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: subfase 3 do Plano-Mestre — prova que o {@link MontadorJanelaContextual}
 * monta a janela de vizinhança SÓ com diálogo narrativo elegível, pulando Comment, karaokê,
 * letreiro e falas vazias, e devolvendo as vizinhas anteriores em ordem cronológica. Usa o
 * classificador real do pipeline (sem CDI: os serviços são instanciados à mão).
 *
 * <p>INVARIANTES DO DOMÍNIO: teste puro e determinístico; nenhuma vizinha não elegível pode
 * entrar na janela; cada lado respeita o {@code tamanhoJanela}.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer vizinha indevida ou ordem trocada reprova.
 */
class MontadorJanelaContextualTest {

    private final MontadorJanelaContextual montador =
        new MontadorJanelaContextual(new ClassificadorPendenciaTelemetria(new DetectorEfeitoKaraokeService()));

    private static EventoLegenda dialogo(int i, String texto) {
        return new EventoLegenda(i, "Dialogue", "Default", "Dialogue: ,", texto);
    }

    private static EventoLegenda comentario(int i, String texto) {
        return new EventoLegenda(i, "Comment", "Default", "Comment: ,", texto);
    }

    private static EventoLegenda comEstilo(int i, String estilo, String texto) {
        return new EventoLegenda(i, "Dialogue", estilo, "Dialogue: ,", texto);
    }

    private static List<String> textos(List<LinhaAlvoContextual> linhas) {
        return linhas.stream().map(LinhaAlvoContextual::texto).toList();
    }

    @Test
    @DisplayName("janela: pula Comment/karaoke/letreiro/vazio e pega so dialogo elegivel em ordem")
    void janelaSoDialogoElegivel() {
        List<EventoLegenda> eventos = List.of(
            dialogo(0, "Line A"),
            comentario(1, "linha comentada"),
            dialogo(2, "Line B"),
            comEstilo(3, "Karaoke Simples", "la la la la"),
            comEstilo(4, "Signs", "PLACA NA PAREDE"),
            dialogo(5, "   "),
            dialogo(6, "Line C"),
            dialogo(7, "Line D"));

        JanelaContextual janela = montador.montar(eventos, 6, 2);

        assertEquals("Line C", janela.alvo().texto());
        // antes: pula vazio(5), letreiro(4), karaoke(3), comentario(1); pega B(2) e A(0), cronologico.
        assertEquals(List.of("Line A", "Line B"), textos(janela.antes()));
        // depois: so Line D(7); fim da lista.
        assertEquals(List.of("Line D"), textos(janela.depois()));
    }

    @Test
    @DisplayName("janela: tamanho 0 devolve so a fala-alvo, sem vizinhas")
    void janelaZeroSoAlvo() {
        List<EventoLegenda> eventos = List.of(
            dialogo(0, "Line A"), dialogo(1, "Line B"), dialogo(2, "Line C"));
        JanelaContextual janela = montador.montar(eventos, 1, 0);
        assertEquals("Line B", janela.alvo().texto());
        assertTrue(janela.antes().isEmpty());
        assertTrue(janela.depois().isEmpty());
    }

    @Test
    @DisplayName("janela: no inicio da lista nao ha vizinhas anteriores")
    void janelaNoInicioSemAntes() {
        List<EventoLegenda> eventos = List.of(
            dialogo(0, "Line A"), dialogo(1, "Line B"), dialogo(2, "Line C"));
        JanelaContextual janela = montador.montar(eventos, 0, 2);
        assertTrue(janela.antes().isEmpty());
        assertEquals(List.of("Line B", "Line C"), textos(janela.depois()));
    }

    @Test
    @DisplayName("janela: indiceAlvo fora da faixa lanca IndexOutOfBounds")
    void indiceForaDaFaixa() {
        List<EventoLegenda> eventos = List.of(dialogo(0, "Line A"));
        assertThrows(IndexOutOfBoundsException.class, () -> montador.montar(eventos, 3, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> montador.montar(eventos, -1, 2));
    }
}
