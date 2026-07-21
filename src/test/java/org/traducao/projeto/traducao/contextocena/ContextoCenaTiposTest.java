package org.traducao.projeto.traducao.contextocena;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.traducao.domain.contextocena.JanelaContextual;
import org.traducao.projeto.traducao.domain.contextocena.LinhaAlvoContextual;
import org.traducao.projeto.traducao.infrastructure.contextocena.ContextoCenaProperties;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: subfase 2 do Plano-Mestre da correção de gênero por contexto de
 * cena — prova que os tipos internos de produção (pacotes contextocena da fatia traducao)
 * estão bem formados e, sobretudo, que o motor nasce DESLIGADO ({@code ativo=false}): enquanto a flag
 * não é ligada, nada no runtime muda. Estes tipos ainda não estão ligados ao pipeline; este
 * teste fixa o contrato deles antes de qualquer fiação.
 *
 * <p>INVARIANTES DO DOMÍNIO: teste puro (sem CDI/@QuarkusTest); a flag default é OFF; a
 * janela faz cópia defensiva das listas e trata nulo como vazio.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer desvio do default OFF ou da imutabilidade da
 * janela reprova.
 */
class ContextoCenaTiposTest {

    @Test
    @DisplayName("flag: o motor de contexto de cena nasce DESLIGADO, janela default 2")
    void motorNasceDesligado() {
        ContextoCenaProperties props = new ContextoCenaProperties();
        assertFalse(props.ativo(), "o motor de contexto de cena DEVE nascer desligado");
        assertFalse(props.isAtivo());
        assertEquals(2, props.tamanhoJanela());
    }

    @Test
    @DisplayName("flag: tamanho de janela negativo e normalizado para 0 (so a fala-alvo)")
    void tamanhoJanelaNaoNegativo() {
        ContextoCenaProperties props = new ContextoCenaProperties(true, -5);
        assertTrue(props.ativo());
        assertEquals(0, props.tamanhoJanela());
        props.setTamanhoJanela(-1);
        assertEquals(0, props.tamanhoJanela());
        props.setTamanhoJanela(3);
        assertEquals(3, props.tamanhoJanela());
    }

    @Test
    @DisplayName("janela: copia defensiva das vizinhas e nulo vira lista vazia")
    void janelaImutavelEToleraNulo() {
        LinhaAlvoContextual alvo = new LinhaAlvoContextual(10, "Dialogue", "I'm sure...");
        List<LinhaAlvoContextual> antes = new ArrayList<>();
        antes.add(new LinhaAlvoContextual(9, "Dialogue", "Karen, are you ready?"));

        JanelaContextual janela = new JanelaContextual(alvo, antes, null);
        // nulo -> vazio
        assertTrue(janela.depois().isEmpty());
        // copia defensiva: mutar a lista de origem nao afeta a janela
        antes.add(new LinhaAlvoContextual(8, "Dialogue", "intruso"));
        assertEquals(1, janela.antes().size());
        // imutavel: nao aceita mutacao
        assertThrows(UnsupportedOperationException.class,
            () -> janela.antes().add(new LinhaAlvoContextual(0, "x", "y")));
        assertEquals("I'm sure...", janela.alvo().texto());
        assertEquals(10, janela.alvo().indice());
    }
}
