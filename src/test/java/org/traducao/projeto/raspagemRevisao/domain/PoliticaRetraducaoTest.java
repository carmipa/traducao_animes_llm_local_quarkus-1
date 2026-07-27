package org.traducao.projeto.raspagemRevisao.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: trava as três decisões que definem QUANDO uma fala é retraduzida do zero.
 * Errar para o lado permissivo gasta GPU refazendo trabalho pronto; errar para o restritivo deixa
 * fala em inglês na legenda publicada.
 *
 * <h2>O que estes testes protegem, e que antes não era testável</h2>
 * As três regras eram um método privado, um {@code anyMatch} solto no meio de um método de 71
 * linhas e um {@code static} package-private. Só a última tinha teste. As duas primeiras leem o
 * MESMO vocabulário de motivos com conjuntos DIFERENTES, e estavam a 350 linhas de distância uma da
 * outra — a distância era o que fazia a diferença entre elas parecer acidente.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer mudança nos trechos procurados reprova aqui, que é o único lugar onde as três decisões
 * aparecem lado a lado.
 */
class PoliticaRetraducaoTest {

    // ---------- decisão do Google ----------

    @Test
    @DisplayName("Google: só falhas OBJETIVAS de tradução — cinco motivos")
    void googleAceitaFalhasObjetivas() {
        assertTrue(PoliticaRetraducao.exigeRetraducaoPeloGoogle(
            List.of("Resíduo gringo detectado: whatever")));
        assertTrue(PoliticaRetraducao.exigeRetraducaoPeloGoogle(
            List.of("Fala não traduzida (idêntica ao original em inglês): Help!")));
        assertTrue(PoliticaRetraducao.exigeRetraducaoPeloGoogle(
            List.of("Idioma incorreto detectado (não é PT-BR): algo")));
        assertTrue(PoliticaRetraducao.exigeRetraducaoPeloGoogle(
            List.of("Preâmbulo detectado: Claro, aqui está")));
        assertTrue(PoliticaRetraducao.exigeRetraducaoPeloGoogle(
            List.of("Marcador de erro de tradução detectado: [ERRO]")));
    }

    @Test
    @DisplayName("Google: concordância e gênero NUNCA vão para o tradutor sem lore")
    void googleRecusaConcordancia() {
        assertFalse(PoliticaRetraducao.exigeRetraducaoPeloGoogle(
            List.of("Concordância de gênero suspeita: ela/ele")),
            "mandar concordância para um tradutor sem lore devolve nome próprio traduzido");
        assertFalse(PoliticaRetraducao.exigeRetraducaoPeloGoogle(List.of()));
        assertFalse(PoliticaRetraducao.exigeRetraducaoPeloGoogle(null), "lista nula não retraduz");
    }

    // ---------- decisão do LLM ----------

    @Test
    @DisplayName("LLM: só resíduo e não-traduzida justificam refazer o texto inteiro")
    void llmRefazSoQuandoValeAPena() {
        assertTrue(PoliticaRetraducao.exigeRetraducaoCompletaPeloLlm(
            List.of("Resíduo gringo detectado: whatever")));
        assertTrue(PoliticaRetraducao.exigeRetraducaoCompletaPeloLlm(
            List.of("Fala não traduzida (idêntica ao original em inglês): Help!")));
        assertFalse(PoliticaRetraducao.exigeRetraducaoCompletaPeloLlm(
            List.of("Preâmbulo detectado: Claro")),
            "preâmbulo vira revisão pontual, que preserva mais do trabalho já feito");
        assertFalse(PoliticaRetraducao.exigeRetraducaoCompletaPeloLlm(null));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: documenta que as duas decisões NÃO são a mesma, e que a diferença é
     * deliberada.
     *
     * <p>Um motivo de "Idioma incorreto" manda a fala para o Google, mas dentro do LLM recebe
     * revisão pontual em vez de retradução completa. Quem quiser unificar as duas regras tem de
     * saber que isso muda qual método do LLM atende quais falas — não é limpeza.
     */
    @Test
    @DisplayName("As duas decisões divergem DE PROPÓSITO: idioma incorreto vai ao Google, mas não refaz no LLM")
    void asDuasDecisoesNaoSaoAMesma() {
        List<String> idiomaIncorreto = List.of("Idioma incorreto detectado (não é PT-BR): algo");

        assertTrue(PoliticaRetraducao.exigeRetraducaoPeloGoogle(idiomaIncorreto));
        assertFalse(PoliticaRetraducao.exigeRetraducaoCompletaPeloLlm(idiomaIncorreto));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: fixa a diferença LATENTE entre os dois trechos procurados, para que ela
     * seja uma escolha visível e não uma surpresa.
     *
     * <p>A decisão do LLM procura {@code "não traduzida"} (largo); a do Google, {@code "Fala não
     * traduzida"} (estreito). Hoje nenhum produtor emite uma frase que case só com o largo, então a
     * diferença não se manifesta no acervo. Se um dia alguém emitir "Legenda não traduzida", este
     * teste mostra qual das duas decisões muda de comportamento.
     */
    @Test
    @DisplayName("Diferença latente: o LLM casa 'não traduzida' solto; o Google exige 'Fala não traduzida'")
    void diferencaLatenteEntreOsTrechosProcurados() {
        List<String> outraFrase = List.of("Legenda não traduzida por falha do provedor");

        assertTrue(PoliticaRetraducao.exigeRetraducaoCompletaPeloLlm(outraFrase),
            "o trecho largo casa");
        assertFalse(PoliticaRetraducao.exigeRetraducaoPeloGoogle(outraFrase),
            "o estreito não — preservado como estava, porque estreitar o outro mudaria o pipeline");
    }

    // ---------- limiar de retradução em massa ----------

    @Test
    @DisplayName("Massa: exige SIMULTANEAMENTE 20 falas e um décimo do auditável")
    void limiarExigeAsDuasCondicoes() {
        assertTrue(PoliticaRetraducao.excedeLimiarRetraducaoEmMassa(1230, 360),
            "360 falas em 1230 passa nas duas condições");
        assertFalse(PoliticaRetraducao.excedeLimiarRetraducaoEmMassa(1230, 3),
            "3 falas não atinge o mínimo absoluto, por menor que seja o arquivo");
        assertFalse(PoliticaRetraducao.excedeLimiarRetraducaoEmMassa(100, 10),
            "10 em 100 atinge a proporção mas não os 20 absolutos");
        assertTrue(PoliticaRetraducao.excedeLimiarRetraducaoEmMassa(200, 20),
            "exatamente no limite das duas condições: bloqueia");
        assertFalse(PoliticaRetraducao.excedeLimiarRetraducaoEmMassa(201, 20),
            "um auditável a mais e a proporção não fecha");
    }

    @Test
    @DisplayName("Massa: contagens inválidas não bloqueiam — não se barra por falta de dados")
    void limiarComContagensInvalidas() {
        assertFalse(PoliticaRetraducao.excedeLimiarRetraducaoEmMassa(0, 50));
        assertFalse(PoliticaRetraducao.excedeLimiarRetraducaoEmMassa(-1, 50));
        assertFalse(PoliticaRetraducao.excedeLimiarRetraducaoEmMassa(100, -1));
    }
}
