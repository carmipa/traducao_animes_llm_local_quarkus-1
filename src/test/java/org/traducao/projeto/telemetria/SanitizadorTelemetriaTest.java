package org.traducao.projeto.telemetria;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.traducao.projeto.telemetria.FixtureCaminhoWindows.c;
import static org.traducao.projeto.telemetria.FixtureCaminhoWindows.de;
import static org.traducao.projeto.telemetria.FixtureCaminhoWindows.driveSemBarra;
import static org.traducao.projeto.telemetria.FixtureCaminhoWindows.marcadorDrive;

/**
 * PROPÓSITO DE NEGÓCIO: prova que o texto publicado no dataset perde o que
 * identifica máquina e pessoa, e MANTÉM o que serve a quem for usar o dado.
 *
 * <h2>Os casos não são inventados</h2>
 * As entradas sujas abaixo foram lidas da telemetria real do acervo em
 * 06/08/2026, quando 4.532 das 6.601 operações carregavam caminho absoluto e
 * 2.843 carregavam pasta de usuário.
 */
class SanitizadorTelemetriaTest {

    /**
     * PROPÓSITO DE NEGÓCIO: o caso mais comum do acervo — caminho colado num
     * detalhe que vale ouro. O caminho sai, a medição fica.
     */
    @Test
    @DisplayName("caminho sai, medicao e nome da obra ficam")
    void caminhoSaiMedicaoFica() {
        String sujo = c("animes", "zeta- double zeta", "Mobile Suit Zeta Gundam")
            + " | ASS | faixas: 50, extraidas: 50, sem faixa: 0";

        String limpo = SanitizadorTelemetria.sanitizar(sujo);

        assertFalse(limpo.contains(marcadorDrive('C')), "letra de drive tem de sair");
        assertTrue(limpo.contains("faixas: 50"), "a medicao e o valor do dataset e nao pode sair junto");
        assertTrue(limpo.contains("Mobile Suit Zeta Gundam"),
            "o nome da obra FICA — e o que permite comparar medicoes entre pessoas");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: grupo de release é informação de comunidade, não de
     * privacidade. Perder isso tornaria o dataset incomparável.
     */
    @Test
    @DisplayName("grupo de release sobrevive a limpeza")
    void grupoDeReleaseSobrevive() {
        String limpo = SanitizadorTelemetria.sanitizar(
            c("animes", "[Joseki] Mobile Suit Gundam The 08th MS Team COMPLETE (1996)", "traducao_ptbr"));

        assertTrue(limpo.contains("[Joseki]"), "grupo de release e dado publico util");
        assertTrue(limpo.contains("traducao_ptbr"));
        assertFalse(limpo.contains("animes"), "o segmento de raiz do acervo nao precisa ir junto");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o caso GRAVE. Pasta de usuário nunca pode sair
     * publicada, e aqui a cauda de dois segmentos por si só ainda a deixaria
     * passar — quem barra é a reconferência posterior.
     */
    @Test
    @DisplayName("pasta de usuario e REDIGIDA, nao aparada")
    void pastaDeUsuarioEhRedigida() {
        assertEquals(SanitizadorTelemetria.REDIGIDO,
            SanitizadorTelemetria.sanitizar(c("Users", "Paulo", "Documents", "legenda.ass")));
        assertEquals(SanitizadorTelemetria.REDIGIDO,
            SanitizadorTelemetria.sanitizar("/home/paulo/kronos/saida.ass"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a reconferência é o que separa este sanitizador de
     * uma limpeza que se auto-aprova. Um caminho que a transformação não conheça
     * tem de virar redigido, não passar cru.
     */
    @Test
    @DisplayName("caminho que a transformacao NAO conhece cai na reconferencia")
    void formaDesconhecidaCaiNaReconferencia() {
        // Sem barra depois do drive: a regra de caminho não casa, e é justamente
        // por isso que a verificação final existe.
        String limpo = SanitizadorTelemetria.sanitizar(
            "erro ao abrir " + driveSemBarra('D', "relatorio.txt"));

        assertEquals(SanitizadorTelemetria.REDIGIDO, limpo);
    }

    /** Caminho de contêiner também é caminho de máquina. */
    @Test
    @DisplayName("caminho do conteiner tambem e aparado")
    void caminhoDeConteinerEhAparado() {
        String limpo = SanitizadorTelemetria.sanitizar("/acervo/86/86 Part 1/legendas_extraidas_ass");

        assertFalse(limpo.startsWith("/acervo"), "a raiz de montagem nao vai para o dataset");
        assertTrue(limpo.contains("legendas_extraidas_ass"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CONTRA-TESTE. Sem ele, "sanitizar" poderia virar
     * "apagar tudo" e o dataset ficaria tecnicamente seguro e completamente
     * inútil — que é exatamente o defeito da publicação por omissão que este
     * sanitizador veio substituir.
     */
    @Test
    @DisplayName("texto SEM caminho atravessa intacto")
    void textoLimpoAtravessaIntacto() {
        String puro = "Auditoria de Conteudo | regras: 12, anomalias: 117, severidade alta: 3";

        assertEquals(puro, SanitizadorTelemetria.sanitizar(puro));
        assertEquals("NOVO_KARAOKE", SanitizadorTelemetria.sanitizar("NOVO_KARAOKE"));
    }

    /** Entradas degeneradas não podem virar exceção no meio de uma publicação. */
    @Test
    @DisplayName("entrada nula ou vazia nao lanca")
    void entradaDegenerada() {
        assertEquals(null, SanitizadorTelemetria.sanitizar(null));
        assertEquals("", SanitizadorTelemetria.sanitizar(""));
        assertEquals("   ", SanitizadorTelemetria.sanitizar("   "));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: separador normalizado. Sem isto, cada linha do
     * dataset denunciaria o sistema operacional de origem.
     */
    @Test
    @DisplayName("separador sai normalizado em barra")
    void separadorNormalizado() {
        String limpo = SanitizadorTelemetria.sanitizar(de('D', "animes", "Guilty Crown", "traducao_ptbr"));

        assertFalse(limpo.contains("\\"), "barra invertida denuncia o sistema de origem");
        assertTrue(limpo.contains("Guilty Crown/traducao_ptbr"));
    }
}
