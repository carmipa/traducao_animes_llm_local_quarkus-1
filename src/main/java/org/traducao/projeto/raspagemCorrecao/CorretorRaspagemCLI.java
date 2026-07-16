package org.traducao.projeto.raspagemCorrecao;

import org.traducao.projeto.config.ExecucaoCli;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.traducao.projeto.raspagemCorrecao.application.CorrigirComGoogleUseCase;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.traducao.projeto.core.presentation.ui.AnsiCores;

import java.nio.file.Path;
import java.util.Optional;

/**
 * CommandLineRunner que realiza a tradução das falas residuais pendentes em inglês
 * utilizando raspagem na API gratuita e sem chaves do Google Translate.
 * Ativado quando a propriedade app.modo é configurada como "RASPAGEM_CORRECAO".
 */
@Component
@ConditionalOnProperty(name = "app.modo", havingValue = "RASPAGEM_CORRECAO")
public class CorretorRaspagemCLI implements ExecucaoCli {

    private final CorrigirComGoogleUseCase corrigirComGoogleUseCase;

    // E3b: chave crua; ausência/branco tratados pelo fallback de domínio local ("cache").
    @ConfigProperty(name = "tradutor.diretorio-entrada")
    Optional<String> diretorioEntrada;

    public CorretorRaspagemCLI(CorrigirComGoogleUseCase corrigirComGoogleUseCase) {
        this.corrigirComGoogleUseCase = corrigirComGoogleUseCase;
    }

    @Override
    public void executar() throws Exception {
        System.out.println(AnsiCores.CYAN + "==========================================================" + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "      CORRETOR DE CACHE VIA GOOGLE TRANSLATE (RASPAGEM)   " + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "==========================================================" + AnsiCores.RESET);

        String entradaUsuario = diretorioEntrada.orElse(null);
        Path diretorioCache = Path.of(entradaUsuario != null && !entradaUsuario.isBlank() ? entradaUsuario : "cache");

        corrigirComGoogleUseCase.executar(diretorioCache);

        System.out.println(AnsiCores.CYAN + "==========================================================" + AnsiCores.RESET);
        System.out.println("Agora rode a opção de Traduzir novamente para compilar as legendas finais instantaneamente.");
    }
}
