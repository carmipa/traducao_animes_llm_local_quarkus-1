package org.traducao.projeto.raspagemRevisao;

import org.traducao.projeto.config.ExecucaoCli;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.core.presentation.ui.AnsiCores;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Revisa arquivos .ass/.ssa já traduzidos, detecta resíduos em inglês e erros
 * de concordância, e corrige via Google Translate.
 */
@Component
@ConditionalOnProperty(name = "app.modo", havingValue = "RASPAGEM_REVISAO_LEGENDAS")
public class RevisorLegendasCLI implements ExecucaoCli {

    private final TradutorProperties propriedades;
    private final RevisarLegendasUseCase revisarLegendasUseCase;

    public RevisorLegendasCLI(TradutorProperties propriedades, RevisarLegendasUseCase revisarLegendasUseCase) {
        this.propriedades = propriedades;
        this.revisarLegendasUseCase = revisarLegendasUseCase;
    }

    @Override
    public void executar() {
        System.out.println(AnsiCores.CYAN + "==========================================================" + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "  REVISÃO DE LEGENDAS TRADUZIDAS (GOOGLE + AUDITORIA PT)   " + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "==========================================================" + AnsiCores.RESET);

        String pastaPt = propriedades.diretorioEntrada();

        if (pastaPt == null || pastaPt.isBlank()) {
            System.out.println(AnsiCores.RED + "Informe a pasta de legendas traduzidas (entrada)." + AnsiCores.RESET);
            return;
        }

        Optional<String> erro = revisarLegendasUseCase.validarPastaEntrada(Path.of(pastaPt));
        if (erro.isPresent()) {
            System.out.println(AnsiCores.RED + erro.get() + AnsiCores.RESET);
            return;
        }

        revisarLegendasUseCase.executar(Path.of(pastaPt), null, Path.of("cache"), null);

        System.out.println(AnsiCores.CYAN + "==========================================================" + AnsiCores.RESET);
    }
}
