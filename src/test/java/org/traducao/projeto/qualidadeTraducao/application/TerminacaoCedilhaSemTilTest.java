package org.traducao.projeto.qualidadeTraducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: fecha o furo da regra de terminação encontrado na tradução completa do
 * Unicorn de 13/08/2026 às 20:30 — o modelo acerta a CEDILHA e erra o TIL.
 *
 * <h2>O que a saída trouxe</h2>
 * Cinco formas escaparam da regra, que só casava {@code -cao}/{@code -coes}:
 * {@code atenuaçao}, {@code admiraçao}, {@code evacuaçao}, {@code salvaçao},
 * {@code recuperaçao}. São 6 ocorrências — pequeno em número, e trivial de fechar.
 *
 * <h2>O limite que a contraprova guarda</h2>
 * O padrão exige C ou Ç. Terminação {@code -ao} solta fica FORA, e isso é deliberado: o risco
 * medido cobriu 151 formas em {@code -cao}/{@code -coes} do acervo, todas legítimas. Terminação
 * {@code -ao} genérica é outro conjunto, que ninguém mediu — e alargar sem medir é exatamente
 * como se produz alarme falso em massa.
 */
@DisplayName("acentos: terminação com cedilha e sem til")
class TerminacaoCedilhaSemTilTest {

    private final NormalizadorAcentosComuns normalizador = new NormalizadorAcentosComuns();

    @Test
    @DisplayName("cedilha sem til é corrigida — as 5 formas medidas na saída de 20:30")
    void cedilhaSemTilEhCorrigida() {
        assertEquals("atenuação", normalizador.normalizar("atenuaçao"));
        assertEquals("admiração", normalizador.normalizar("admiraçao"));
        assertEquals("evacuação", normalizador.normalizar("evacuaçao"));
        assertEquals("salvação", normalizador.normalizar("salvaçao"));
        assertEquals("recuperação", normalizador.normalizar("recuperaçao"));
    }

    @Test
    @DisplayName("a forma sem cedilha continua funcionando, singular e plural")
    void semCedilhaContinuaFuncionando() {
        assertEquals("organização", normalizador.normalizar("organizacao"));
        assertEquals("ambições", normalizador.normalizar("ambicoes"));
        assertEquals("observação", normalizador.normalizar("observacao"));
    }

    @Test
    @DisplayName("plural COM cedilha: condiçoes -> condições")
    void pluralComCedilha() {
        assertEquals("condições", normalizador.normalizar("condiçoes"));
        assertEquals("reparações", normalizador.normalizar("reparaçoes"));
    }

    /**
     * CONTRAPROVA do alargamento. Se um dia alguém trocar {@code [cç]} por {@code ç?}, este teste
     * reprova — e é o que impede a regra de sair mexendo em terminação {@code -ao} que nunca foi
     * medida.
     */
    @Test
    @DisplayName("contraprova: terminação -ao sem C/Ç NÃO é tocada")
    void terminacaoAoSoltaNaoEhTocada() {
        assertEquals("Macao", normalizador.normalizar("Macao"),
            "topônimo com -ao viraria -ção se o padrão fosse afrouxado");
        assertEquals("bilbao", normalizador.normalizar("bilbao"));
    }

    /**
     * O FALSO POSITIVO QUE EXISTE, fixado como fato em vez de ficar como surpresa.
     *
     * <p>{@code Curaçao} — a ilha — tem C e casa a regra, virando {@code Curação}. A medição de
     * 13/08 varreu 151 formas em {@code -cao}/{@code -coes} no acervo inteiro e NÃO encontrou
     * nenhum estrangeirismo desse tipo, por isso a regra foi adotada. Mas "não está no acervo de
     * hoje" não é "não existe".
     *
     * <p>Se aparecer numa obra futura, o conserto é exceção NOMINAL — uma lista de formas que a
     * regra não toca —, jamais afrouxar o padrão: afrouxar devolveria as 105 correções que ela
     * fechou no Zeta.
     */
    @Test
    @DisplayName("limite conhecido: Curaçao vira Curação, e o conserto é exceção nominal")
    void oFalsoPositivoQueExisteEstaDeclarado() {
        assertEquals("curação", normalizador.normalizar("curacao"),
            "se este teste mudar, a regra mudou — e a mudança precisa de medição nova");
    }

    @Test
    @DisplayName("caixa preservada nas duas formas")
    void caixaPreservada() {
        assertEquals("Atenuação", normalizador.normalizar("Atenuaçao"));
        assertEquals("Organização", normalizador.normalizar("Organizacao"));
    }
}
