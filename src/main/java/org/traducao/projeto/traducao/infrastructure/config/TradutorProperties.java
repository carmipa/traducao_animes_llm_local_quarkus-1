package org.traducao.projeto.traducao.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.List;

@ConfigurationProperties(prefix = "tradutor")
public class TradutorProperties {
    private String diretorioEntrada;
    private String diretorioSaida;
    private String diretorioCache;
    private int tamanhoLote = 20;
    private List<String> estilosIgnorados = List.of("Song JP");
    private String idiomaOriginal = "en";
    private String idiomaTraduzido = "pt-br";

    public TradutorProperties() {
    }

    public TradutorProperties(String diretorioEntrada, String diretorioSaida, String diretorioCache, int tamanhoLote, List<String> estilosIgnorados, String idiomaOriginal, String idiomaTraduzido) {
        this.diretorioEntrada = diretorioEntrada;
        this.diretorioSaida = diretorioSaida;
        this.diretorioCache = diretorioCache;
        this.tamanhoLote = tamanhoLote <= 0 ? 20 : tamanhoLote;
        this.estilosIgnorados = estilosIgnorados == null ? List.of("Song JP") : estilosIgnorados;
        this.idiomaOriginal = (idiomaOriginal == null || idiomaOriginal.isBlank()) ? "en" : idiomaOriginal;
        this.idiomaTraduzido = (idiomaTraduzido == null || idiomaTraduzido.isBlank()) ? "pt-br" : idiomaTraduzido;
    }

    public String diretorioEntrada() { return diretorioEntrada; }
    public String getDiretorioEntrada() { return diretorioEntrada; }
    public void setDiretorioEntrada(String diretorioEntrada) { this.diretorioEntrada = diretorioEntrada; }

    public String diretorioSaida() { return diretorioSaida; }
    public String getDiretorioSaida() { return diretorioSaida; }
    public void setDiretorioSaida(String diretorioSaida) { this.diretorioSaida = diretorioSaida; }

    public String diretorioCache() { return diretorioCache; }
    public String getDiretorioCache() { return diretorioCache; }
    public void setDiretorioCache(String diretorioCache) { this.diretorioCache = diretorioCache; }

    public int tamanhoLote() { return tamanhoLote; }
    public int getTamanhoLote() { return tamanhoLote; }
    public void setTamanhoLote(int tamanhoLote) { if (tamanhoLote > 0) this.tamanhoLote = tamanhoLote; }

    public List<String> estilosIgnorados() { return estilosIgnorados; }
    public List<String> getEstilosIgnorados() { return estilosIgnorados; }
    public void setEstilosIgnorados(List<String> estilosIgnorados) { if (estilosIgnorados != null) this.estilosIgnorados = estilosIgnorados; }

    public String idiomaOriginal() { return idiomaOriginal; }
    public String getIdiomaOriginal() { return idiomaOriginal; }
    public void setIdiomaOriginal(String idiomaOriginal) { if (idiomaOriginal != null && !idiomaOriginal.isBlank()) this.idiomaOriginal = idiomaOriginal; }

    public String idiomaTraduzido() { return idiomaTraduzido; }
    public String getIdiomaTraduzido() { return idiomaTraduzido; }
    public void setIdiomaTraduzido(String idiomaTraduzido) { if (idiomaTraduzido != null && !idiomaTraduzido.isBlank()) this.idiomaTraduzido = idiomaTraduzido; }

    public Path resolverDiretorioSaida() {
        if (diretorioSaida != null && !diretorioSaida.isBlank()) {
            return Path.of(diretorioSaida);
        }
        Path entrada = Path.of(diretorioEntrada);
        // Por padrão, cria automaticamente a subpasta 'traducao_ptbr' dentro da pasta da mídia informada
        return entrada.resolve("traducao_ptbr");
    }

    /**
     * Se nao for informado (nem por config nem pelo console), o cache fica em
     * "cache/<pasta-do-anime>/<subpasta>" na raiz do projeto — mesma convenção
     * relativa usada por logging.file.name (logs/tradutor.log) — em vez de
     * pedir esse caminho ao usuário a cada execução.
     */
    public Path resolverDiretorioCache() {
        if (diretorioCache != null && !diretorioCache.isBlank()) {
            return Path.of(diretorioCache);
        }
        return Path.of("cache").resolve(nomeAnimeAPartirDaEntrada());
    }

    private Path nomeAnimeAPartirDaEntrada() {
        Path entrada = Path.of(diretorioEntrada);
        Path pai = entrada.getParent();
        if (pai == null || pai.getFileName() == null) {
            return Path.of(entrada.getFileName() != null ? entrada.getFileName().toString() : "default");
        }
        Path avo = pai.getParent();
        if (avo == null || avo.getFileName() == null) {
            return Path.of(pai.getFileName().toString());
        }
        return Path.of(avo.getFileName().toString(), pai.getFileName().toString());
    }
}

