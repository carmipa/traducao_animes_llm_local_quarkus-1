package org.traducao.projeto.contexto.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.contexto.domain.VeredictoObraContexto;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa a régua do veredicto obra×contexto — a decisão que faltava
 * quando 15 caches de Gundam 0083 foram gravados com {@code contextoId = "guilty_crown"}.
 * Mora no peer {@code contexto} porque identidade de obra é assunto deste peer.
 *
 * <p>INVARIANTES DO DOMÍNIO: divergência exige PROVA POSITIVA (alguém reconheceu a pasta e
 * o ativo não está entre eles); ausência de reconhecimento é sempre indeterminação, nunca
 * divergência; a decisão é determinística e não depende de I/O.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer entrada nula/em branco deve degradar para
 * {@link VeredictoObraContexto#INDETERMINADO} — falhar fechado aqui pararia toda obra que
 * ainda não declarou vocabulário de pasta.
 */
class ValidadorCompatibilidadeObraContextoTest {

    private final ValidadorCompatibilidadeObraContexto validador = new ValidadorCompatibilidadeObraContexto();

    @Test
    void obraReconhecidaPeloContextoAtivoCasa() {
        assertEquals(VeredictoObraContexto.CASA,
            validador.avaliar("Mobile Suit Gundam 0083", "gundam_0083", Set.of("gundam_0083")));
    }

    @Test
    void obraReconhecidaSoPorOutroContextoDiverge() {
        // O caso real: pasta de 0083, combo em Guilty Crown.
        assertEquals(VeredictoObraContexto.DIVERGENTE,
            validador.avaliar("Mobile Suit Gundam 0083", "guilty_crown", Set.of("gundam_0083")));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o conjunto que chega aqui JÁ vem resolvido por especificidade pelo
     * catálogo — franquia e temporada não empatam. Dois ids, portanto, significam empate REAL:
     * duas obras distintas reivindicando a pasta com a mesma precisão. Nesse estado a identidade
     * não resolve e não há o que provar, então bloqueia.
     *
     * <p>INVARIANTES DO DOMÍNIO: bloqueia MESMO com o ativo entre os empatados. Aceitar o ativo
     * como desempate é exatamente tomar a seleção da UI por prova — o mecanismo que produziu o
     * incidente. O conserto é de catálogo (apelido mais específico), não de combo.
     */
    @Test
    void obraReivindicadaPorVariasObrasEhAmbiguaMesmoComOAtivoEntreElas() {
        assertEquals(VeredictoObraContexto.AMBIGUO,
            validador.avaliar("Gundam Alguma Coisa", "gundam_0083", Set.of("gundam_0079", "gundam_0083")));
        assertEquals(VeredictoObraContexto.AMBIGUO,
            validador.avaliar("Gundam Alguma Coisa", "guilty_crown", Set.of("gundam_0079", "gundam_0083")));
    }

    @Test
    void obraQueNinguemReconheceEhIndeterminadaEnuncaDivergente() {
        assertEquals(VeredictoObraContexto.INDETERMINADO,
            validador.avaliar("Pasta Qualquer", "guilty_crown", Set.of()));
        assertEquals(VeredictoObraContexto.INDETERMINADO,
            validador.avaliar("Pasta Qualquer", "guilty_crown", null));
    }

    @Test
    void semContextoAtivoNaoHaBaseParaJulgar() {
        assertEquals(VeredictoObraContexto.INDETERMINADO,
            validador.avaliar("Mobile Suit Gundam 0083", null, Set.of("gundam_0083")));
        assertEquals(VeredictoObraContexto.INDETERMINADO,
            validador.avaliar("Mobile Suit Gundam 0083", "  ", Set.of("gundam_0083")));
    }

    @Test
    void obraSemNomeNaoJulga() {
        assertEquals(VeredictoObraContexto.INDETERMINADO,
            validador.avaliar(null, "gundam_0083", Set.of("gundam_0083")));
        assertEquals(VeredictoObraContexto.INDETERMINADO,
            validador.avaliar("   ", "gundam_0083", Set.of("gundam_0083")));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a mensagem de bloqueio é o único artefato que o operador lê para
     * se corrigir; precisa nomear os três fatos (arquivo, obra do caminho, contexto esperado
     * vs. ativo).
     */
    @Test
    void mensagemDeBloqueioNomeiaArquivoObraContextoEsperadoEAtivo() {
        String msg = validador.mensagemDeBloqueio(
            Path.of("animes", "Gundam 0083", "legendas_originais", "ep01.ass").toString(),
            "Mobile Suit Gundam 0083", "guilty_crown", Set.of("gundam_0083"));

        assertTrue(msg.contains("ep01.ass"), msg);
        assertTrue(msg.contains("Mobile Suit Gundam 0083"), msg);
        assertTrue(msg.contains("gundam_0083"), msg);
        assertTrue(msg.contains("guilty_crown"), msg);
        assertTrue(msg.contains("BLOQUEADA"), msg);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o aviso do caso indeterminado precisa dizer que a checagem foi
     * PULADA — silenciar daria a falsa impressão de que o arquivo foi conferido.
     */
    @Test
    void mensagemDeIndeterminacaoDeixaExplicitoQueAChecagemFoiPulada() {
        String msg = validador.mensagemDeIndeterminacao("Pasta Qualquer", "danmachi");

        assertTrue(msg.contains("Pasta Qualquer"), msg);
        assertTrue(msg.contains("danmachi"), msg);
        assertTrue(msg.contains("PULADA"), msg);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a mensagem de ambiguidade é endereçada a quem MANTÉM as lores, não a
     * quem clica no combo: precisa nomear as obras empatadas e dizer que o conserto é declarar um
     * apelido de pasta mais específico. Reaproveitar a redação de divergência mandaria o operador
     * procurar no combo uma opção que não resolveria nada.
     */
    @Test
    void mensagemDeAmbiguidadeNomeiaAsObrasEmpatadasEApontaOConsertoNoCatalogo() {
        String msg = validador.mensagemDeAmbiguidade(
            Path.of("animes", "Break Blade Movies", "legendas_originais", "ep01.ass").toString(),
            "Break Blade Movies", "break_blade_1", Set.of("break_blade_1", "break_blade_2"));

        assertTrue(msg.contains("ep01.ass"), msg);
        assertTrue(msg.contains("Break Blade Movies"), msg);
        assertTrue(msg.contains("break_blade_1"), msg);
        assertTrue(msg.contains("break_blade_2"), msg);
        assertTrue(msg.contains("AMBÍGUA"), msg);
        assertTrue(msg.contains("BLOQUEADA"), msg);
        assertTrue(msg.contains("apelido de pasta"), msg);
    }
}
