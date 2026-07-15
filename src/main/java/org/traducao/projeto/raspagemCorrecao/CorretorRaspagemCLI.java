package org.traducao.projeto.raspagemCorrecao;

import org.traducao.projeto.config.ExecucaoCli;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.traducao.projeto.raspagemCorrecao.application.CorrigirComGoogleUseCase;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.core.presentation.ui.AnsiCores;

import java.nio.file.Path;

/**
 * CommandLineRunner que realiza a tradução das falas residuais pendentes em inglês
 * utilizando raspagem na API gratuita e sem chaves do Google Translate.
 * Ativado quando a propriedade app.modo é configurada como "RASPAGEM_CORRECAO".
 */
@Component
@ConditionalOnProperty(name = "app.modo", havingValue = "RASPAGEM_CORRECAO")
public class CorretorRaspagemCLI implements ExecucaoCli {

    private final TradutorProperties propriedades;
    private final CorrigirComGoogleUseCase corrigirComGoogleUseCase;

    public CorretorRaspagemCLI(TradutorProperties propriedades, CorrigirComGoogleUseCase corrigirComGoogleUseCase) {
        this.propriedades = propriedades;
        this.corrigirComGoogleUseCase = corrigirComGoogleUseCase;
    }

    @Override
    public void executar() throws Exception {
        System.out.println(AnsiCores.CYAN + "==========================================================" + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "      CORRETOR DE CACHE VIA GOOGLE TRANSLATE (RASPAGEM)   " + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "==========================================================" + AnsiCores.RESET);

        String entradaUsuario = propriedades.diretorioEntrada();
        Path diretorioCache = Path.of(entradaUsuario != null && !entradaUsuario.isBlank() ? entradaUsuario : "cache");

        corrigirComGoogleUseCase.executar(diretorioCache);

        System.out.println(AnsiCores.CYAN + "==========================================================" + AnsiCores.RESET);
        System.out.println("Agora rode a opção de Traduzir novamente para compilar as legendas finais instantaneamente.");
    }
}
