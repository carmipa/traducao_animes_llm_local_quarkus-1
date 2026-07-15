package org.traducao.projeto.raspagemRevisao;

import org.traducao.projeto.config.ExecucaoCli;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.traducao.projeto.raspagemRevisao.application.RevisarCacheUseCase;
import org.traducao.projeto.traducao.domain.StatusLlm;
import org.traducao.projeto.traducao.domain.ports.MistralPort;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.core.presentation.ui.AnsiCores;

import java.nio.file.Path;

/**
 * Revisa falas já traduzidas no cache, corrigindo concordância de gênero,
 * pronomes e adjetivos — erros comuns quando o LLM traduz literalmente do inglês.
 * Ativado quando {@code app.modo=RASPAGEM_REVISAO}.
 */
@Component
@ConditionalOnProperty(name = "app.modo", havingValue = "RASPAGEM_REVISAO")
public class RevisorRaspagemCLI implements ExecucaoCli {

    private final TradutorProperties propriedades;
    private final RevisarCacheUseCase revisarCacheUseCase;
    private final MistralPort mistralPort;

    public RevisorRaspagemCLI(
        TradutorProperties propriedades,
        RevisarCacheUseCase revisarCacheUseCase,
        MistralPort mistralPort
    ) {
        this.propriedades = propriedades;
        this.revisarCacheUseCase = revisarCacheUseCase;
        this.mistralPort = mistralPort;
    }

    @Override
    public void executar() {
        System.out.println(AnsiCores.CYAN + "==========================================================" + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "   REVISÃO GRAMATICAL DO CACHE (CONCORDÂNCIA PT-BR / LLM)  " + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "==========================================================" + AnsiCores.RESET);

        StatusLlm status = mistralPort.verificarDisponibilidade();
        if (!status.modeloCarregado()) {
            System.out.println(AnsiCores.RED + "[FAIL] " + status.mensagem() + AnsiCores.RESET);
            System.out.println("Inicie o servidor LLM local e carregue o modelo antes de revisar.");
            return;
        }
        System.out.println(AnsiCores.GREEN + "[OK] " + status.mensagem() + AnsiCores.RESET);

        String entradaUsuario = propriedades.diretorioEntrada();
        Path diretorioCache = Path.of(
            entradaUsuario != null && !entradaUsuario.isBlank() ? entradaUsuario : "cache");

        revisarCacheUseCase.executar(diretorioCache);

        System.out.println(AnsiCores.CYAN + "==========================================================" + AnsiCores.RESET);
        System.out.println("Rode Traduzir novamente para recompilar os arquivos .ass a partir do cache revisado.");
    }
}
