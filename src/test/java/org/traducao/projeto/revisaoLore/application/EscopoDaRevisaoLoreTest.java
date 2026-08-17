package org.traducao.projeto.revisaoLore.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.revisaoLore.domain.ResultadoDeteccaoLore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congelar O QUE A TELA 3.2 ACUSA. Escopo fechado por Paulo em 17/08/2026:
 * <i>"corrigir nomes, locais etc da lore da animação que estamos trabalhando, mas nada além
 * disso! é um escopo bem fechado! o resto tiramos tudo!"</i>
 *
 * <h2>O prejuízo, medido antes do corte</h2>
 * {@code MedicaoEscopoDaRevisaoLoreIT} sobre 22 obras e 75.419 falas contou 10.080 motivos. Duas
 * regras que não eram lore respondiam por <b>2.422 deles — 24,0% de todo o ruído da tela</b>:
 * <ul>
 *   <li><b>1.606 (15,9%)</b> "possível nome/termo em inglês remanescente" — acusava QUALQUER
 *       palavra inglesa de 4+ letras que sobrasse no português. Isso é FALTA DE TRADUÇÃO, que é
 *       a tela 3.1; acusar aqui fazia a 3.2 ficar amarela por trabalho alheio;</li>
 *   <li><b>816 (8,1%)</b> "sigla ou termo todo em maiúsculas" — acusava qualquer palavra em CAIXA
 *       ALTA de 3+ letras. Um grito {@code "PARE!"} virava motivo de lore.</li>
 * </ul>
 *
 * <p><b>E nenhuma das duas tinha um único teste.</b> Foi por isso que o corte passou verde de
 * primeira: não havia o que quebrar. Este arquivo é a cobertura que faltava, escrita no sentido
 * contrário — ele reprova se elas VOLTAREM.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Caso-controle nos DOIS sentidos, no mesmo arquivo: as falas que saíram do escopo têm de
 *       sair limpas, <b>e</b> as três regras que ficaram têm de continuar acusando. Só o primeiro
 *       grupo ficaria verde com um detector completamente cego — que é o modo de falha real de
 *       "remover regra".</li>
 *   <li>Cada fala exercita UMA regra por vez: fala que dispara duas não distingue qual sobreviveu.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem nomeia a regra e o volume que ela produzia no acervo, para que quem a reintroduzir
 * saiba o preço antes de decidir.
 */
class EscopoDaRevisaoLoreTest {

    private final DetectorTermosLoreService detector = new DetectorTermosLoreService();

    @Test
    @DisplayName("FORA DO ESCOPO: palavra em CAIXA ALTA no portugues nao e mais motivo de lore")
    void caixaAltaNaoEMaisAcusada() {
        ResultadoDeteccaoLore r = detector.auditar("Stop right there.", "PARE agora.");

        assertFalse(r.suspeito(), () ->
            "a regra 'sigla ou termo todo em maiusculas' VOLTOU. Ela produzia 816 motivos (8,1%) "
                + "no acervo acusando gritos como PARE — palpite sobre formatacao, nao nome nem "
                + "local. Motivos: " + r.motivos());
    }

    @Test
    @DisplayName("FORA DO ESCOPO: palavra inglesa que sobrou no PT e trabalho da 3.1, nao da 3.2")
    void residuoEmInglesNaoEMaisAcusado() {
        ResultadoDeteccaoLore r = detector.auditar("Our colony is falling.", "Nossa colony esta caindo.");

        assertFalse(r.suspeito(), () ->
            "a regra 'possivel nome/termo em ingles remanescente' VOLTOU. Ela produzia 1.606 "
                + "motivos (15,9%) no acervo acusando FALTA DE TRADUCAO, que e a tela 3.1. "
                + "Motivos: " + r.motivos());
    }

    @Test
    @DisplayName("CONTROLE POSITIVO: nome proprio composto quebrado continua sendo acusado")
    void nomeProprioCompostoContinuaAcusado() {
        ResultadoDeteccaoLore r = detector.auditar("Char Aznable is here.", "Cha Aznable esta aqui.");

        assertTrue(r.suspeito(),
            "o detector ficou CEGO: nome proprio composto preservado pela metade e o coracao da "
                + "3.2 (33,5% dos motivos do acervo) e tem de continuar acusando. Sem esta "
                + "assercao, os dois testes acima passariam com o detector inteiro apagado.");
    }

    @Test
    @DisplayName("CONTROLE POSITIVO: nome canonico traduzido ao pe da letra continua sendo acusado")
    void traducaoLiteralDeNomeCanonicoContinuaAcusada() {
        ResultadoDeteccaoLore r = detector.auditar("The Unicorn is moving.", "O Unicornio esta se movendo.");

        assertTrue(r.suspeito(),
            "o detector parou de acusar traducao literal de nome canonico (Unicorn -> Unicornio), "
                + "que e exatamente o que a 3.2 existe para pegar");
    }

    @Test
    @DisplayName("CONTROLE POSITIVO: termo de faccao que ficou em ingles continua sendo acusado")
    void termoDeFaccaoEmInglesContinuaAcusado() {
        ResultadoDeteccaoLore r = detector.auditar(
            "The Earth Federation forces arrived.", "As forcas da Earth Federation chegaram.");

        assertTrue(r.suspeito(),
            "o detector parou de acusar termo de faccao com equivalente PT-BR cadastrado "
                + "(Earth Federation -> Federacao Terrestre)");
    }
}
