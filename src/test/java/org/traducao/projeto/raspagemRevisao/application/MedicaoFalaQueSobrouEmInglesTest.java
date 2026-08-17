package org.traducao.projeto.raspagemRevisao.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.domain.EventoLegenda;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: descobrir POR QUE seis falas do Zeta continuam em inglês depois de a
 * tela 3.1 rodar — perguntando às classes de PRODUÇÃO, uma etapa de cada vez, em vez de deduzir
 * do código.
 *
 * <h2>De onde vieram estas oito falas</h2>
 * Medição de 17/08/2026 sobre o acervo: das 27.987 falas do Zeta com espelho inglês, 642 seguem
 * idênticas ao original em estilo {@code Dialogue}. Destas, <b>634 são nome próprio</b>
 * ({@code Kamille!}, {@code Matosh!}) e são corretas. Sobram estas oito, em três classes:
 * quatro com {@code \clip} de recorte vetorial, duas com itálico atravessando {@code \N}, e
 * duas que também são nome próprio ({@code The O!}, {@code Gate of Zedan?}).
 *
 * <h2>Invariantes do domínio</h2>
 * Este harness NÃO corrige nada e NÃO afirma defeito: ele imprime o veredito de cada etapa para
 * que o conserto (se houver) mire a etapa certa. Consertar antes de saber onde cai é como as 14
 * medições erradas de 07/08 — número certo, alvo errado.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Só imprime; nunca reprova por conta do conteúdo. Reprova apenas se o filtro lançar.
 */
@QuarkusTest
class MedicaoFalaQueSobrouEmInglesTest {

    @Inject
    FiltroAuditoriaLinha filtro;

    @Inject
    org.traducao.projeto.qualidadeTraducao.application.MascaradorTags mascarador;

    @Inject
    org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService detectorKaraoke;

    @Inject
    org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService protecaoAss;

    /** As oito, exatamente como estão no acervo em 17/08/2026. */
    private static final List<String> FALAS = List.of(
        "{\\i1}Four could be to me{\\i0}\\N{\\i1}what that person was to Amuro.{\\i0}",
        "{\\clip(m 884.625 1072 l 891.375 868 332.625 805.375 195.375 1017.375)}That might not be a bad idea.",
        "{\\clip(m 755.375 1065.375 l 756.625 868 312.625 828 150 1017.375)}That might not be a bad idea.",
        "{\\i1}So, you're saying the Titans went to{\\i0}\\N{\\i1}Side Four to prepare a colony drop?{\\i0}",
        "Gate of Zedan?",
        "The O!");

    /** O contra-caso: uma fala inglesa comum, que TEM de passar pelo filtro e ser auditada. */
    private static final String CONTROLE = "We leave at 1600 hours tomorrow. That is all.";

    @Test
    @DisplayName("MEDICAO: em que etapa cada fala sobrevivente e descartada")
    void medeOndeAsFalasSaoDescartadas() {
        System.out.println("\n===== FALAS QUE SOBRARAM EM INGLES NO ZETA =====");
        System.out.println("etapas de FiltroAuditoriaLinha.deveIgnorar, na ordem em que rodam:");
        System.out.printf("%-6s %-6s %-8s %-8s  %s%n",
            "IGNOR", "traduz", "karaoke", "protecao", "FALA");
        System.out.println("-".repeat(110));

        for (String fala : FALAS) {
            imprimir(fala);
        }
        System.out.println("-".repeat(110));
        imprimir(CONTROLE);
        System.out.println("   ^ CONTROLE: a primeira coluna tem de ser 'nao'");
        System.out.println("================================================\n");
    }

    /** Pergunta a MESMA coisa que o filtro pergunta, etapa por etapa, na mesma ordem. */
    private void imprimir(String fala) {
        EventoLegenda evento = new EventoLegenda(1, "Dialogue", "Dialogue", "", fala);
        boolean ignorada = filtro.deveIgnorar(evento, fala);
        boolean temTextoTraduzivel = mascarador.contemTextoTraduzivel(fala);
        boolean karaokeBarrou = detectorKaraoke.eEfeitoKaraoke(fala)
            && !detectorKaraoke.eKaraokeOuMusicaTraduzivel("Dialogue", fala);
        boolean protecaoBarrou = protecaoAss.deveIgnorarIntervencaoIa("Dialogue", fala);
        System.out.printf("%-6s %-6s %-8s %-8s  %s%n",
            ignorada ? "SIM" : "nao",
            temTextoTraduzivel ? "sim" : "NAO",
            karaokeBarrou ? "BARROU" : "-",
            protecaoBarrou ? "BARROU" : "-",
            recortar(fala));
    }

    private static String recortar(String texto) {
        return texto.length() <= 88 ? texto : texto.substring(0, 88) + "…";
    }
}
