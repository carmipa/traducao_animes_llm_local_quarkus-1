package org.traducao.projeto.traducao.presentation.ui;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;

import java.nio.file.Path;

/**
 * Pastas efetivas da execução atual. Preenchidas pelo {@code TradutorCLI} a
 * partir do diálogo Swing ou das propriedades/linha de comando.
 */
@Component
public class PastasExecucao {

    private Path diretorioEntrada;
    private Path diretorioSaida;
    private Path diretorioCache;

    public void configurar(String entrada, String saida, String cache, TradutorProperties propriedades) {
        if (entrada == null || entrada.isBlank()) {
            throw new IllegalArgumentException("Pasta de entrada é obrigatória");
        }
        this.diretorioEntrada = Path.of(entrada.trim());

        TradutorProperties efetivas = new TradutorProperties(
            entrada.trim(),
            saida != null ? saida.trim() : "",
            cache != null ? cache.trim() : "",
            propriedades.tamanhoLote(),
            propriedades.estilosIgnorados(),
            propriedades.idiomaOriginal(),
            propriedades.idiomaTraduzido()
        );
        this.diretorioSaida = efetivas.resolverDiretorioSaida();
        this.diretorioCache = efetivas.resolverDiretorioCache();
    }

    public Path diretorioEntrada() {
        return diretorioEntrada;
    }

    public Path diretorioSaida() {
        return diretorioSaida;
    }

    public Path diretorioCache() {
        return diretorioCache;
    }

    public boolean configurado() {
        return diretorioEntrada != null;
    }
}
