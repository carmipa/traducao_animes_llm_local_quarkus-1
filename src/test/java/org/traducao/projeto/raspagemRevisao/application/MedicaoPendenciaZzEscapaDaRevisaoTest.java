package org.traducao.projeto.raspagemRevisao.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.domain.EventoLegenda;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: descobrir por que as falas que a TRADUÇÃO deixou pendentes no Gundam ZZ
 * não são recuperadas pela tela 3.1 — perguntando às classes de PRODUÇÃO, etapa por etapa.
 *
 * <h2>De onde vieram estas falas</h2>
 * O cabeçalho de 26 dos 47 {@code .ass} do ZZ carrega o carimbo
 * {@code ; KRONOS pendentes: N fala(s) mantida(s) no original}, somando <b>38</b>. O carimbo é
 * escrito por {@code ProcessarArquivoUseCase} a partir de {@code falhasDistintas}, no momento da
 * tradução. As falas em si estão no cache, como entradas cujo {@code traduzido} está VAZIO —
 * distinto de {@code traduzido == original}, que é nome próprio preservado e está correto.
 *
 * <p>Três classes saíram da leitura das entradas vazias:
 * <ol>
 *   <li><b>cartão de próximo episódio</b> — {@code \h} de enchimento, muitos {@code \N} e o
 *       título do episódio entre aspas;</li>
 *   <li><b>aspas internas</b> — a mesma cicatriz registrada no Zeta;</li>
 *   <li><b>frase comum</b> — sem lore, sem música, sem nome próprio. É a que o Paulo viu.</li>
 * </ol>
 *
 * <h2>Invariantes do domínio</h2>
 * Este harness NÃO corrige nada e NÃO afirma defeito: imprime o veredito de cada etapa para o
 * conserto mirar a etapa certa. Consertar antes de saber onde cai foi o erro das 14 medições de
 * 07/08/2026 — número certo, alvo errado.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Só imprime. O contra-caso no fim garante que o instrumento não está aprovando tudo.
 */
@QuarkusTest
class MedicaoPendenciaZzEscapaDaRevisaoTest {

    @Inject
    FiltroAuditoriaLinha filtro;

    @Inject
    org.traducao.projeto.qualidadeTraducao.application.MascaradorTags mascarador;

    @Inject
    org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService detectorKaraoke;

    @Inject
    org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService protecaoAss;

    /** Classe 1 — cartão de próximo episódio, como está no cache do ZZ. */
    private static final List<String> CARTAO_PROXIMO_EP = List.of(
        "Next Episode \\N\\N\\N\\N\\N\\N\\N\\N\\h\\h\\h\\h\\h\\h\\h\\hJudau in Space",
        "\\h\\h\\hNext Episode \\N\\N\\N\\N\\N\\N\\N\\N\\h\\h\\h\\h\\h\\h\\h\\h\\h\\h\\hJudau, Launch!!",
        "Next time, on Gundam ZZ: \"Ple and Axis.\"",
        "Next time on Gundam ZZ:\\N\"The Burning Earth.\"");

    /** Classe 2 — aspas internas. */
    private static final List<String> ASPAS_INTERNAS = List.of(
        "I tell them, \"Sorry, but no.\"",
        "And you ask, \"Is Leina here?\"",
        "Sir. It keeps repeating that they'll show\\Nus a \"safe location\" for 5,000 gillas.");

    /** Classe 3 — frase comum. Nenhuma tem lore, música ou nome próprio isolado. */
    private static final List<String> FRASE_COMUM = List.of(
        "Big Brother, I think you did the\\Nright thing today, but...",
        "It's impossible! Astonaige, don't be a\\Nfool!",
        "She's been alone for three years,\\Never since that guy Tag died.",
        "Lady Haman?! But why did\\Nyou leave her alone?!",
        "Well, I don't actually know which of\\Nthe three ships is the Sadalahn.",
        "When you're ready to see me, just go\\Nto Port Blanc and say your name is Candy.",
        "State your assignment and ID number!",
        "I... have... had... ENOUGH!");

    /** O CONTRA-CASO: fala inglesa banal, que TEM de passar pelo filtro e ser auditada. */
    private static final String CONTROLE = "We leave at 1600 hours tomorrow. That is all.";

    @Test
    @DisplayName("MEDICAO: em que etapa a pendencia do ZZ escapa da revisao")
    void medeOndeAsPendenciasEscapam() {
        System.out.println("\n===== PENDENCIAS DO GUNDAM ZZ x FiltroAuditoriaLinha =====");
        System.out.printf("%-6s %-7s %-8s %-9s  %s%n",
            "IGNOR", "traduz", "karaoke", "protecao", "FALA");

        imprimirGrupo("CLASSE 1 — cartao de proximo episodio", CARTAO_PROXIMO_EP);
        imprimirGrupo("CLASSE 2 — aspas internas", ASPAS_INTERNAS);
        imprimirGrupo("CLASSE 3 — frase comum", FRASE_COMUM);

        System.out.println("\n  -- CONTROLE (a primeira coluna tem de ser 'nao') --");
        imprimir(CONTROLE);
        System.out.println("==========================================================\n");
    }

    private void imprimirGrupo(String titulo, List<String> falas) {
        System.out.println("\n  -- " + titulo + " --");
        falas.forEach(this::imprimir);
    }

    /** Pergunta a MESMA coisa que o filtro pergunta, etapa por etapa, na mesma ordem. */
    private void imprimir(String fala) {
        EventoLegenda evento = new EventoLegenda(1, "Dialogue", "Dialogue", "", fala);
        System.out.printf("%-6s %-7s %-8s %-9s  %s%n",
            filtro.deveIgnorar(evento, fala) ? "SIM" : "nao",
            mascarador.contemTextoTraduzivel(fala) ? "sim" : "NAO",
            detectorKaraoke.eEfeitoKaraoke(fala)
                && !detectorKaraoke.eKaraokeOuMusicaTraduzivel("Dialogue", fala) ? "BARROU" : "-",
            protecaoAss.deveIgnorarIntervencaoIa("Dialogue", fala) ? "BARROU" : "-",
            fala.length() <= 84 ? fala : fala.substring(0, 84) + "…");
    }
}
