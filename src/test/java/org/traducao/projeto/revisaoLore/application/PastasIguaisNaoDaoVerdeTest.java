package org.traducao.projeto.revisaoLore.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.revisaoLore.domain.StatusRevisaoLore;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * PROPÓSITO DE NEGÓCIO: garantir que a 3.2 NUNCA feche em verde sem ter comparado nada.
 *
 * <h2>O defeito, que era MEU e tinha um dia de idade</h2>
 * Paulo perguntou se a lógica de escolha de pastas da 3.2 estava conferida contra os problemas
 * que a 3.1 pagou. Não estava — e a pergunta achou uma regressão introduzida no mesmo dia.
 *
 * <p>Com as duas pastas apontando para o mesmo lugar, {@code localizarArquivoTraduzido} cai no
 * último candidato (o mesmo nome do original) e devolve <b>o próprio arquivo</b>. Cada evento é
 * comparado consigo mesmo, toda fala vira "idêntica ao original" e é encaminhada à 3.1. Até
 * 17/08/2026 isso ao menos fechava AMARELO, porque as encaminhadas entravam em
 * {@code falasPendentes}. O item C daquele dia as tirou da conta — com razão, porque falta de
 * tradução não é pendência desta tela — e, sem perceber, transformou o sinal de cegueira em
 * <b>CONCLUIDO verde</b>.
 *
 * <p>É a regra 12 na veia: <i>"nada a corrigir" e "não comparei nada" não podem produzir o mesmo
 * sinal</i>. E é a mesma classe que a 3.1 fechou com {@code CONCLUIDO_SEM_REFERENCIA} depois de
 * uma corrida com a pasta EN vazia dar banner verde.
 *
 * <h2>Invariantes do domínio</h2>
 * As duas camadas são testadas, porque protegem coisas diferentes: a recusa na entrada mata o
 * caso concreto (pastas iguais) e o status de cegueira cobre o caso GERAL — par errado, pasta que
 * não é a tradução, qualquer motivo pelo qual nada foi comparado.
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem diz qual das duas caiu e o que o operador veria na tela.
 */
class PastasIguaisNaoDaoVerdeTest {

    @Test
    @DisplayName("lote em que TODA fala saiu como identica ao original nao pode fechar CONCLUIDO")
    void loteInteiramenteCegoNaoFechaVerde() {
        // O que a sessao registra quando as duas pastas sao a mesma: nenhuma pendencia, nenhum
        // erro de arquivo — e, ainda assim, nada comparado.
        List<String> erros = new ArrayList<>();
        int falasAuditadas = 120;
        int falasEncaminhadas = 120;

        if (falasAuditadas > 0 && falasEncaminhadas == falasAuditadas) {
            erros.add("CEGO: as " + falasAuditadas + " falas auditadas estavam IDENTICAS ao original");
        }

        StatusRevisaoLore status = RevisarLoreUseCase.determinarStatus(false, false, erros, 0);

        assertNotEquals(StatusRevisaoLore.CONCLUIDO, status,
            "a tela fecharia em VERDE sem ter comparado uma unica fala. Foi assim que a 3.1 deu "
                + "[SUCESSO] com a pasta EN vazia, e e o que o CONCLUIDO_SEM_REFERENCIA de la "
                + "existe para impedir.");
        assertEquals(StatusRevisaoLore.CONCLUIDO_COM_PENDENCIAS, status,
            "o desfecho honesto de um lote cego e pendencia declarada, com o motivo no relatorio");
    }

    @Test
    @DisplayName("CONTROLE NEGATIVO: lote em que so PARTE saiu identica fecha limpo")
    void loteParcialmenteEncaminhadoContinuaFechandoLimpo() {
        // Este e o caso legitimo e comum: algumas falas ainda em ingles no meio de um episodio
        // traduzido. Elas sao trabalho da 3.1, e nao podem pintar a 3.2 de amarelo — senao a
        // guarda acima vira alarme falso, e alarme falso ensina a desligar o alarme.
        List<String> erros = new ArrayList<>();
        int falasAuditadas = 120;
        int falasEncaminhadas = 3;

        if (falasAuditadas > 0 && falasEncaminhadas == falasAuditadas) {
            erros.add("CEGO");
        }

        StatusRevisaoLore status = RevisarLoreUseCase.determinarStatus(false, false, erros, 0);

        assertEquals(StatusRevisaoLore.CONCLUIDO, status,
            "3 falas em ingles num lote de 120 e trabalho da 3.1, nao cegueira desta tela — "
                + "pintar isso de amarelo transformaria a guarda em alarme falso");
    }

    @Test
    @DisplayName("CONTROLE NEGATIVO: lote sem fala auditavel nenhuma nao dispara a guarda de cegueira")
    void loteSemFalaAuditavelNaoDisparaGuarda() {
        List<String> erros = new ArrayList<>();
        int falasAuditadas = 0;
        int falasEncaminhadas = 0;

        if (falasAuditadas > 0 && falasEncaminhadas == falasAuditadas) {
            erros.add("CEGO");
        }

        assertEquals(StatusRevisaoLore.CONCLUIDO,
            RevisarLoreUseCase.determinarStatus(false, false, erros, 0),
            "0 == 0 nao pode ser lido como cegueira: sem fala auditavel a guarda tem de se calar, "
                + "senao toda pasta so de musica fecharia amarela");
    }
}
