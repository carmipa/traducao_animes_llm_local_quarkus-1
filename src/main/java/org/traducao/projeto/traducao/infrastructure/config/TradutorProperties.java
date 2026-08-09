package org.traducao.projeto.traducao.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.traducao.projeto.core.io.DiretorioBaseKronos;

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
    /**
     * Proteção de romaji pelo PAREAMENTO das duas camadas de karaokê (romaji e tradução no mesmo
     * tempo). DESLIGADA por padrão: com {@code false} o seletor recebe proteção vazia e a decisão
     * do que traduzir fica byte-idêntica à anterior. Ligar exige rodar o A/B do Plano-Mestre.
     */
    private boolean protecaoRomajiPareamento = false;
    /**
     * Agrupa numa MESMA chamada ao LLM as falas que formam uma frase partida entre eventos
     * consecutivos (ver {@code DetectorCorrenteFrasePartida}). DESLIGADA por padrão: com
     * {@code false} o fatiamento em lotes é byte-idêntico ao anterior. Ligar troca ~30% de
     * conector errado na junção por ~4% de risco de deslocamento — que a
     * {@code GuardaCorrenteTraduzida} intercepta devolvendo a corrente ao fluxo normal.
     */
    private boolean agruparFrasePartida = false;

    /**
     * Envia ao LLM o TEXTO PURO das falas cujas tags estão todas na BORDA, em vez do texto com
     * marcadores {@code [[TAGn]]} — a moldura é recolocada aqui na volta (ver
     * {@code core.texto.TextoSemTags}). DESLIGADA por padrão: com {@code false} o caminho é
     * byte-idêntico ao anterior, e os testes que caracterizam o mascaramento continuam válidos.
     *
     * <p>O QUE SE GANHA LIGANDO: o marcador é a causa isolada das falas perdidas. Medido em
     * 2026-07-22, <b>393 das 412 perdas (95%)</b> foram marcador ausente com tradução aproveitável
     * — o que motivou o remendo {@code ReparadorMarcadoresLlm}. No corretor de karaokê, em
     * 07/08/2026, eliminar o marcador levou a taxa de sucesso de <b>34% para 100%</b> nas linhas
     * que passaram a viajar limpas.
     *
     * <p>O QUE ESTÁ EM JOGO: alcança 8,3% das falas de diálogo (488 de 5.869 no Guilty Crown). As
     * 87,6% sem tag nenhuma não passam por este caminho nem com a flag ligada, e as 4,0% com tag
     * NO MEIO ficam de fora por decisão — ali a tag marca uma palavra específica e recolocá-la
     * exigiria alinhamento palavra a palavra entre os dois idiomas.
     *
     * <p>Ligada, a fala de borda deixa de passar por {@code marcadoresPreservados} (não há
     * marcador a preservar) e por {@code desmascarar}; a proteção equivalente passa a ser o
     * {@code recompor}, que devolve o original intacto quando a resposta é inútil.
     */
    private boolean textoPuroAoLlm = false;

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

    public boolean protecaoRomajiPareamento() { return protecaoRomajiPareamento; }
    public boolean isProtecaoRomajiPareamento() { return protecaoRomajiPareamento; }
    public void setProtecaoRomajiPareamento(boolean protecaoRomajiPareamento) { this.protecaoRomajiPareamento = protecaoRomajiPareamento; }

    public boolean agruparFrasePartida() { return agruparFrasePartida; }
    public boolean isAgruparFrasePartida() { return agruparFrasePartida; }
    public void setAgruparFrasePartida(boolean agruparFrasePartida) { this.agruparFrasePartida = agruparFrasePartida; }

    public boolean textoPuroAoLlm() { return textoPuroAoLlm; }
    public boolean isTextoPuroAoLlm() { return textoPuroAoLlm; }
    public void setTextoPuroAoLlm(boolean textoPuroAoLlm) { this.textoPuroAoLlm = textoPuroAoLlm; }

    public String idiomaTraduzido() { return idiomaTraduzido; }
    public String getIdiomaTraduzido() { return idiomaTraduzido; }
    public void setIdiomaTraduzido(String idiomaTraduzido) { if (idiomaTraduzido != null && !idiomaTraduzido.isBlank()) this.idiomaTraduzido = idiomaTraduzido; }

    /**
     * PROPÓSITO DE NEGÓCIO: resolve a pasta onde as legendas traduzidas são gravadas —
     * o caminho configurado, ou, na ausência dele, a pasta {@code traducao_ptbr} irmã da
     * pasta de legendas originais (ao lado dos vídeos, onde o remuxer também procura).
     *
     * <p>INVARIANTES DO DOMÍNIO: o caminho passa por {@link DiretorioBaseKronos}, então um
     * valor ABSOLUTO é usado como está e um valor RELATIVO (o default {@code saida}) fica
     * sob a raiz operacional efetiva. Em produção a raiz é o diretório corrente, e o
     * resultado é idêntico ao histórico; na suíte de testes a raiz é redirecionada, o que
     * impede a gravação em pastas versionadas pelo Git.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança exceção própria; um caminho
     * sintaticamente inválido propaga {@link java.nio.file.InvalidPathException}. Entrada
     * em raiz (sem pai) mantém o comportamento antigo.
     */
    public Path resolverDiretorioSaida() {
        if (diretorioSaida != null && !diretorioSaida.isBlank()) {
            return DiretorioBaseKronos.resolver(diretorioSaida);
        }
        Path entrada = DiretorioBaseKronos.resolver(diretorioEntrada);
        // Por padrão, cria 'traducao_ptbr' NO MESMO NÍVEL da pasta de legendas originais
        // (irmã dela, dentro da pasta da mídia/temporada), não dentro dela — assim o
        // artefato fica ao lado dos vídeos, onde o remuxer também procura por ele.
        // Guarda contra entrada em raiz (sem pai): mantém o comportamento antigo.
        Path pai = entrada.getParent();
        return (pai != null ? pai : entrada).resolve("traducao_ptbr");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: resolve onde mora o banco bilíngue (cache) do episódio. Se não
     * for informado (nem por config nem pelo console), fica em
     * {@code cache/<pasta-do-anime>/<subpasta>} sob a raiz operacional — mesma convenção
     * relativa de {@code logs/tradutor.log} — em vez de pedir o caminho a cada execução.
     *
     * <p>INVARIANTES DO DOMÍNIO: o caminho passa por {@link DiretorioBaseKronos}, então um
     * valor ABSOLUTO é usado como está e um valor RELATIVO (o default {@code cache}) fica
     * sob a raiz operacional efetiva. Isto é o que impede a SUÍTE DE TESTES de escrever no
     * {@code cache/} versionado pelo Git: uma execução de teste chegou a esvaziar o campo
     * {@code traduzido} de 28 caches reais (86, Gundam 0083/ZZ/08th), perda silenciosa de
     * tradução já paga ao LLM. Em produção a raiz é o diretório corrente e o resultado é
     * byte-idêntico ao histórico.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança exceção própria; um caminho
     * sintaticamente inválido propaga {@link java.nio.file.InvalidPathException}.
     */
    public Path resolverDiretorioCache() {
        if (diretorioCache != null && !diretorioCache.isBlank()) {
            return DiretorioBaseKronos.resolver(diretorioCache);
        }
        return DiretorioBaseKronos.resolver("cache").resolve(nomeAnimeAPartirDaEntrada());
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

