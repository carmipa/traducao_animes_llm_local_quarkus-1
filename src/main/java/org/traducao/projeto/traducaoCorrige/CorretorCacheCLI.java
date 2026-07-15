package org.traducao.projeto.traducaoCorrige;

import org.traducao.projeto.config.ExecucaoCli;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.traducao.presentation.ui.AnsiCores;
import org.traducao.projeto.traducaoCorrige.application.LimparCacheUseCase;

import java.nio.file.Path;

/**
 * CommandLineRunner que realiza a limpeza do cache de tradução integrado ao fluxo do Spring.
 * Ativado quando a propriedade app.modo é configurada como "CORRIGIR_CACHE".
 */
@Component
@ConditionalOnProperty(name = "app.modo", havingValue = "CORRIGIR_CACHE")
public class CorretorCacheCLI implements ExecucaoCli {

    private final TradutorProperties propriedades;
    private final LimparCacheUseCase limparCacheUseCase;

    public CorretorCacheCLI(TradutorProperties propriedades, LimparCacheUseCase limparCacheUseCase) {
        this.propriedades = propriedades;
        this.limparCacheUseCase = limparCacheUseCase;
    }

    @Override
    public void executar() throws Exception {
        System.out.println(AnsiCores.CYAN + "==========================================================" + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "         CORRETOR DE CACHE DE TRADUÇÃO DE ANIMES          " + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "==========================================================" + AnsiCores.RESET);

        String entradaUsuario = propriedades.diretorioEntrada();
        Path diretorioCache = Path.of(entradaUsuario != null && !entradaUsuario.isBlank() ? entradaUsuario : "cache");

        limparCacheUseCase.executar(diretorioCache);
        
        System.out.println(AnsiCores.CYAN + "==========================================================" + AnsiCores.RESET);
        System.out.println("Agora as linhas corrigidas serão reenviadas ao LLM em uma nova tradução.");
    }
}
