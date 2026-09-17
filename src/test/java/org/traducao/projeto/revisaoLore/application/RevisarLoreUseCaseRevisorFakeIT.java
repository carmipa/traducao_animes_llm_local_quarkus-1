package org.traducao.projeto.revisaoLore.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.revisaoLore.domain.StatusRevisaoLoreLlm;
import org.traducao.projeto.revisaoLore.domain.exceptions.RevisaoLoreException;
import org.traducao.projeto.revisaoLore.domain.ports.RevisorLoreLlmPort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que o {@code RevisarLoreUseCase} está integrado à
 * porta LLM própria da Revisão de Lore — usando um fake da {@link RevisorLoreLlmPort} —,
 * validando o portão de disponibilidade sem depender do LM Studio real.
 * <p>INVARIANTES DO DOMÍNIO: o {@code GerenciadorPromptRevisaoLore} real (com os
 * contextos registrados) valida o contexto; a porta fake decide a disponibilidade.
 * <p>COMPORTAMENTO EM CASO DE FALHA: LLM indisponível deve abortar a sessão com
 * {@link RevisaoLoreException} antes de qualquer processamento de arquivo.
 */
@QuarkusTest
class RevisarLoreUseCaseRevisorFakeIT {

    @Inject
    GerenciadorPromptRevisaoLore gerenciadorPromptRevisaoLore;

    @Inject
    org.traducao.projeto.lore.infrastructure.GerenciadorContexto gerenciadorContexto;

    @Inject
    org.traducao.projeto.lore.application.ValidadorCompatibilidadeObraContexto validadorObraContexto;

    private RevisorLoreLlmPort fake(StatusRevisaoLoreLlm status) {
        return new RevisorLoreLlmPort() {
            @Override
            public StatusRevisaoLoreLlm verificarDisponibilidade() {
                return status;
            }

            @Override
            public Optional<String> revisar(String promptSistemaRevisaoLore, String originalInglesMascarado,
                                            String traducaoPtMascarada, List<String> problemasDetectados) {
                return Optional.empty();
            }
        };
    }

    @Test
    @DisplayName("LLM indisponível pela porta própria aborta a sessão antes de processar arquivos")
    void portaIndisponivelAbortaSessao(@TempDir Path tempDir) throws IOException {
        Path pastaOriginal = Files.createDirectory(tempDir.resolve("en"));
        Path pastaTraduzida = Files.createDirectory(tempDir.resolve("pt"));

        RevisorLoreLlmPort fakeIndisponivel = fake(new StatusRevisaoLoreLlm(true, false, "nenhum modelo carregado"));
        // Colaboradores não usados até o portão de disponibilidade permanecem nulos; o gerenciador
        // de prompts (validação de contexto), a guarda obra×contexto (pasta temp = INDETERMINADO,
        // segue) e a porta LLM são exercitados. A obra da pasta temp não é reconhecida, então a
        // guarda apenas avisa e deixa a sessão chegar ao portão de disponibilidade.
        RevisarLoreUseCase useCase = new RevisarLoreUseCase(
            null, null, null, null, null,
            fakeIndisponivel, gerenciadorPromptRevisaoLore, null, null, null, null, null, null,
            gerenciadorContexto, validadorObraContexto);

        RevisaoLoreException excecao = assertThrows(RevisaoLoreException.class,
            () -> useCase.executar(pastaOriginal, pastaTraduzida, "eight_six", false));
        assertTrue(excecao.getMessage().toLowerCase().contains("indisponivel"),
            "A sessão deve abortar citando LLM indisponível: " + excecao.getMessage());
    }
}
