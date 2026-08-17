package org.traducao.projeto.revisaoLore.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.revisaoLore.domain.ResultadoDeteccaoLore;
import org.traducao.projeto.revisaoLore.domain.ResultadoRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.StatusRevisaoLoreLlm;
import org.traducao.projeto.revisaoLore.domain.ports.RevisorLoreLlmPort;
import org.traducao.projeto.revisaoLore.infrastructure.RevisaoLoreAuditoriaCache;
import org.traducao.projeto.revisaoLore.infrastructure.RevisaoLoreLogPersistencia;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: provar, com o caso de uso REAL rodando, que a chamada ao LLM feita pelo
 * modo "Revisar todas as falas" numa fala <b>sem indício de lore</b> é <b>incapaz de alterar a
 * legenda</b> — a proposta é sempre ignorada. É a evidência que autoriza remover essa chamada
 * sem perder capacidade nenhuma.
 *
 * <h2>Por que este teste existe antes da remoção</h2>
 * O escopo da 3.2 foi fechado por Paulo em 17/08/2026: <i>"corrigir nomes, locais etc da lore da
 * animação que estamos trabalhando, mas nada além disso"</i>. O modo "todas as falas" manda ao
 * modelo local toda fala com diálogo, inclusive as que a heurística julgou limpas — e o
 * {@code RevisarLoreUseCase} descarta a proposta quando {@code deteccao.motivos()} está vazio.
 * Como {@link ResultadoDeteccaoLore} só é {@code suspeito} quando há motivo, <b>fala limpa
 * ⟺ motivos vazio</b>, e a proposta nunca chega ao arquivo.
 *
 * <p>Afirmar isso por leitura seria inferência. Aqui a fala roda pelo caso de uso inteiro, com um
 * LLM de mentira que devolve uma proposta <b>que passa por todos os portões anteriores</b>
 * (dentro do escopo de lore: troca UM token, e o token inserido existe no inglês E na lore da
 * obra) — justamente para que o desfecho não possa ser creditado a outra trava.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O discriminador é {@code falasSemAlteracao == 1}: o ramo que ignora a proposta preventiva
 *       incrementa ESSE contador. Se a proposta tivesse morrido no validador ou no portão de
 *       escopo, o contador seria {@code falasDescartadas} — o teste separa os caminhos em vez de
 *       aceitar "não gravou" como prova de qualquer coisa.</li>
 *   <li>Calibração antes de medir: o teste EXIGE que a fala escolhida seja considerada limpa pelo
 *       detector de produção e que o LLM tenha sido REALMENTE chamado. Fala suspeita por acidente,
 *       ou modelo nunca acionado, tornariam o resultado vazio de significado.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Reprova apontando qual das três coisas quebrou: a fala deixou de ser limpa, o LLM não foi
 * chamado, ou a proposta chegou ao arquivo.
 */
@QuarkusTest
class LlmEmFalaSemIndicioDeLoreEInerteTest {

    private static final String CONTEXTO = "gundam_zeta";

    /** Fala limpa: os dois nomes de lore aparecem certos nos dois lados. */
    private static final String EN = "Char and Amuro are here.";
    private static final String PT = "Char e Amuro estao aqui.";

    /**
     * Proposta desenhada para ATRAVESSAR o portão de escopo de lore: troca um único token, e o
     * token inserido ({@code Amuro}) existe no inglês e na lore de {@code gundam_zeta}. Sem isso,
     * ela morreria antes e o teste creditaria a trava errada.
     */
    private static final String PROPOSTA_DO_LLM = "Amuro e Amuro estao aqui.";

    private static final String CABECALHO = """
        [Script Info]
        ScriptType: v4.00+

        [V4+ Styles]
        Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
        Style: Default,Arial,20,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,0,2,10,10,10,1

        [Events]
        Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
        """;

    @Inject LeitorLegendaAss leitor;
    @Inject EscritorLegendaAss escritor;
    @Inject MascaradorTags mascarador;
    @Inject DetectorTermosLoreService detector;
    @Inject ValidadorTraducaoService validador;
    @Inject GerenciadorPromptRevisaoLore gerenciadorPromptRevisaoLore;
    @Inject TelemetriaService telemetriaService;
    @Inject RevisaoLoreLogPersistencia logPersistencia;
    @Inject RevisaoLoreAuditoriaCache auditoriaCache;
    @Inject AlcanceRevisaoLore alcance;
    @Inject ProtecaoLegendaAssService protecaoAss;
    @Inject CorretorLoreDeterministico corretorLore;

    @Test
    @DisplayName("no modo todas as falas, a proposta do LLM em fala limpa NAO chega ao arquivo")
    void propostaEmFalaSemIndicioDeLoreNuncaEGravada(@TempDir Path base) throws IOException {
        // CALIBRACAO 1: a fala precisa ser limpa para o detector de PRODUCAO. Se ela for suspeita,
        // o caminho exercitado passa a ser outro e o teste nao prova nada.
        String lore = org.traducao.projeto.lore.domain.PromptRevisaoLore.extrairLoreCanonica(
            gerenciadorPromptRevisaoLore.obterPromptSistema(CONTEXTO));
        ResultadoDeteccaoLore deteccao = detector.auditar(EN, PT, lore);
        assertFalse(deteccao.suspeito(),
            "fixture invalida: o detector de producao considerou a fala SUSPEITA ("
                + deteccao.motivos() + "), entao ela nao exercita o ramo preventivo");

        Path pastaEn = Files.createDirectory(base.resolve("en"));
        Path pastaPt = Files.createDirectory(base.resolve("pt"));
        Files.writeString(pastaEn.resolve("ep.ass"),
            CABECALHO + "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,," + EN + "\n",
            StandardCharsets.UTF_8);
        Path arquivoPt = pastaPt.resolve("ep_PT-BR.ass");
        Files.writeString(arquivoPt,
            CABECALHO + "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,," + PT + "\n",
            StandardCharsets.UTF_8);
        String antes = Files.readString(arquivoPt, StandardCharsets.UTF_8);

        AtomicInteger chamadas = new AtomicInteger();
        RevisorLoreLlmPort llmQuePropoe = new RevisorLoreLlmPort() {
            @Override
            public StatusRevisaoLoreLlm verificarDisponibilidade() {
                return new StatusRevisaoLoreLlm(true, true, "dublê disponível");
            }

            @Override
            public Optional<String> revisar(String promptSistema, String originalMascarado,
                                            String traducaoMascarada, List<String> problemas) {
                chamadas.incrementAndGet();
                return Optional.of(PROPOSTA_DO_LLM);
            }
        };

        RevisarLoreUseCase useCase = new RevisarLoreUseCase(
            leitor, escritor, mascarador, detector, validador,
            llmQuePropoe, gerenciadorPromptRevisaoLore, telemetriaService,
            logPersistencia, auditoriaCache, alcance, protecaoAss, corretorLore);

        ResultadoRevisaoLore resultado = useCase.executar(pastaEn, pastaPt, CONTEXTO, true);

        // CALIBRACAO 2: se o LLM nunca foi chamado, "nao gravou" seria trivial e nao provaria que
        // a chamada e inerte — provaria que ela nao aconteceu.
        assertEquals(1, chamadas.get(),
            "o LLM tinha de ter sido chamado UMA vez no modo 'todas as falas'; foi chamado "
                + chamadas.get() + " vez(es)");

        assertEquals(antes, Files.readString(arquivoPt, StandardCharsets.UTF_8),
            "A PROPOSTA CHEGOU AO ARQUIVO. O modo 'todas as falas' passaria a alterar legenda em "
                + "fala sem indicio de lore, que e exatamente o que o escopo da 3.2 proibe.");
        assertEquals(0, resultado.arquivosAlterados(), "nenhum arquivo devia ter sido reescrito");
        assertEquals(0, resultado.falasCorrigidas(), "nenhuma fala devia ter sido corrigida");

        // O DISCRIMINADOR: o ramo que ignora proposta preventiva conta em falasSemAlteracao.
        // Descarte por validador ou por portao de escopo contaria em falasDescartadas.
        assertEquals(1, resultado.falasSemAlteracao(),
            "esperava a fala fechando como CONFORME (proposta preventiva ignorada). Se ela caiu em "
                + "falasDescartadas=" + resultado.falasDescartadas() + ", a proposta morreu numa "
                + "trava ANTERIOR e este teste nao provou o ramo preventivo");
        assertEquals(0, resultado.falasDescartadas(),
            "a proposta devia ter atravessado as travas anteriores e morrido no ramo preventivo");

        assertTrue(resultado.falasAuditadas() >= 1, "a fala devia ter sido auditada");
    }
}
