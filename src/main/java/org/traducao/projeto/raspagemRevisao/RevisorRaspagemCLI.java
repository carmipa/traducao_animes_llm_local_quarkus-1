package org.traducao.projeto.raspagemRevisao;

import org.traducao.projeto.config.ExecucaoCli;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.traducao.projeto.raspagemRevisao.application.RevisarCacheUseCase;
import org.traducao.projeto.llm.domain.StatusLlm;
import org.traducao.projeto.llm.domain.LlmPort;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.traducao.projeto.core.presentation.ui.AnsiCores;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Revisa falas já traduzidas no cache, corrigindo concordância de gênero,
 * pronomes e adjetivos — erros comuns quando o LLM traduz literalmente do inglês.
 * Ativado quando {@code app.modo=RASPAGEM_REVISAO}.
 */
@Component
@ConditionalOnProperty(name = "app.modo", havingValue = "RASPAGEM_REVISAO")
public class RevisorRaspagemCLI implements ExecucaoCli {

    private final RevisarCacheUseCase revisarCacheUseCase;
    private final LlmPort llmPort;

    // E3b: chave crua; ausência/branco tratados pelo fallback de domínio local ("cache").
    @ConfigProperty(name = "tradutor.diretorio-entrada")
    Optional<String> diretorioEntrada;

    public RevisorRaspagemCLI(
        RevisarCacheUseCase revisarCacheUseCase,
        LlmPort llmPort
    ) {
        this.revisarCacheUseCase = revisarCacheUseCase;
        this.llmPort = llmPort;
    }

    @Override
    public void executar() {
        System.out.println(AnsiCores.CYAN + "==========================================================" + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "   REVISÃO GRAMATICAL DO CACHE (CONCORDÂNCIA PT-BR / LLM)  " + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "==========================================================" + AnsiCores.RESET);

        StatusLlm status = llmPort.verificarDisponibilidade();
        if (!status.modeloCarregado()) {
            System.out.println(AnsiCores.RED + "[FAIL] " + status.mensagem() + AnsiCores.RESET);
            System.out.println("Inicie o servidor LLM local e carregue o modelo antes de revisar.");
            return;
        }
        System.out.println(AnsiCores.GREEN + "[OK] " + status.mensagem() + AnsiCores.RESET);

        String entradaUsuario = diretorioEntrada.orElse(null);
        Path diretorioCache = Path.of(
            entradaUsuario != null && !entradaUsuario.isBlank() ? entradaUsuario : "cache");

        revisarCacheUseCase.executar(diretorioCache);

        System.out.println(AnsiCores.CYAN + "==========================================================" + AnsiCores.RESET);
        System.out.println("Rode Traduzir novamente para recompilar os arquivos .ass a partir do cache revisado.");
    }
}
