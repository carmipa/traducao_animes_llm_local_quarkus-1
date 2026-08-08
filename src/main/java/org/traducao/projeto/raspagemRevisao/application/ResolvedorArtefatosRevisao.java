package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.raspagemRevisao.domain.FrescorCache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * PROPÓSITO DE NEGÓCIO: descobre, a partir de uma legenda PT-BR, quais são os DEMAIS artefatos do
 * mesmo episódio — o arquivo de cache que a originou e a legenda inglesa de referência. É o
 * pareamento que sustenta toda a revisão: sem o par certo, compara-se a fala de um episódio com o
 * original de outro.
 *
 * <p>O problema é de NOMES, não de conteúdo. Os arquivos chegam do pipeline com sufixos que variam
 * conforme a origem ({@code _PT-BR}, {@code _PTBR_Track3}, {@code _ENG}, {@code _Track2}), e o
 * mesmo episódio aparece com grafias diferentes em pastas diferentes. Esta classe concentra todas
 * as regras de normalização e de preferência que antes viviam espalhadas em doze métodos privados
 * do {@code RevisarLegendasUseCase} — a maior massa coesa daquele arquivo, e a primeira a sair na
 * FASE 4 do Plano-Mestre justamente por ser a de menor risco: lógica de nomes, sem regra de
 * negócio de tradução.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>NÃO imprime nada e NÃO decide nada sobre a revisão. Devolve caminhos e desfechos; quem
 *       avisa o operador é o caso de uso. Foi por isso que a comparação de datas virou
 *       {@link FrescorCache} em vez de {@code boolean}: o caso "não deu para ler as datas" precisa
 *       de aviso, e um booleano o esconderia dentro de {@code false}.</li>
 *   <li>Zero dependências injetadas. Toca o disco só para PERGUNTAR (existe? é arquivo? qual a
 *       data?) — nunca para escrever.</li>
 *   <li>Candidatos DIRETOS têm prioridade sobre a varredura recursiva, e a varredura é ordenada.
 *       Sem essa ordem, a mesma pasta devolveria pares diferentes em execuções diferentes, e a
 *       revisão deixaria de ser reproduzível.</li>
 *   <li>Nunca devolve um caminho fora da raiz informada, e nunca devolve o próprio arquivo PT como
 *       se fosse o original inglês.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Falha de varredura NÃO lança: devolve o caminho esperado por convenção, que o leitor tratará como
 * artefato ausente. Devolver um palpite explícito é melhor que interromper o lote inteiro por causa
 * de um episódio cujo par não foi encontrado.
 */
@Service
public class ResolvedorArtefatosRevisao {

    private static final Set<String> EXTENSOES = Set.of(".ass", ".ssa");
    private static final Pattern CODIGO_EPISODIO = Pattern.compile("(?i)(S\\d{1,2}E\\d{1,3})");
    private static final Pattern SUFIXO_PTBR_TRACK = Pattern.compile("(?i)_PT-?BR(_Track\\d+)?$");
    private static final String SUFIXO_CACHE = ".cache.json";

    /**
     * PROPÓSITO DE NEGÓCIO: localiza deterministicamente o cache correspondente à legenda PT-BR
     * mesmo quando a raiz contém subpastas por obra.
     *
     * <p>INVARIANTES DO DOMÍNIO: candidatos diretos têm prioridade; a busca recursiva é ordenada e
     * nunca seleciona arquivo fora da raiz informada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: erro de varredura devolve o caminho esperado, que será
     * tratado como cache ausente pelo leitor.
     *
     * @param arquivoPt legenda traduzida cujo cache se procura
     * @param cacheDir raiz da busca
     * @return o cache encontrado, ou o caminho convencional quando não há par
     */
    public Path resolverArquivoCache(Path arquivoPt, Path cacheDir) {
        String baseLegenda = nomeBaseLegenda(arquivoPt);
        String baseMidia = normalizarBaseLegenda(baseLegenda);
        String codigoEpisodio = extrairCodigoEpisodio(baseLegenda);

        for (String candidato : candidatosNomeCache(baseLegenda, baseMidia)) {
            Path direto = cacheDir.resolve(candidato);
            if (Files.isRegularFile(direto)) {
                return direto;
            }
        }

        if (Files.isDirectory(cacheDir)) {
            try (Stream<Path> stream = Files.walk(cacheDir)) {
                return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> correspondeCache(p.getFileName().toString(), baseMidia, codigoEpisodio))
                    .sorted()
                    .findFirst()
                    .orElse(cacheDir.resolve(baseMidia + "_ENG" + SUFIXO_CACHE));
            } catch (IOException e) {
                return cacheDir.resolve(baseMidia + "_ENG" + SUFIXO_CACHE);
            }
        }

        return cacheDir.resolve(baseMidia + "_ENG" + SUFIXO_CACHE);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: localiza a legenda INGLESA que serve de original para a revisão.
     *
     * <p>INVARIANTES DO DOMÍNIO: nunca devolve o próprio arquivo PT nem outra legenda traduzida —
     * comparar uma tradução com outra tradução produziria "correções" sem original. Entre vários
     * candidatos do mesmo episódio, a preferência é estável: {@code _Track2}, {@code _ENG},
     * {@code _Track1}, e só então o resto.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: erro de varredura devolve o caminho convencional.
     *
     * @param arquivoPt legenda traduzida cujo original se procura
     * @param pastaLegendasEn pasta onde procurar
     * @return o original encontrado, ou o caminho convencional quando não há par
     */
    public Path resolverArquivoOriginal(Path arquivoPt, Path pastaLegendasEn) {
        String nome = arquivoPt.getFileName().toString();
        String ext = extensaoLegenda(nome);
        String baseSemPt = normalizarBaseLegenda(nome.substring(0, nome.length() - ext.length()));
        String codigoEpisodio = extrairCodigoEpisodio(baseSemPt);

        Set<String> candidatos = new LinkedHashSet<>();
        candidatos.add(baseSemPt + ext);
        candidatos.add(baseSemPt + "_ENG" + ext);
        candidatos.add(baseSemPt + "_Eng" + ext);
        for (int track = 1; track <= 9; track++) {
            candidatos.add(baseSemPt + "_Track" + track + ext);
        }

        Matcher ptbrTrack = SUFIXO_PTBR_TRACK.matcher(nome.substring(0, nome.length() - ext.length()));
        if (ptbrTrack.find()) {
            String baseMidia = nome.substring(0, ptbrTrack.start());
            candidatos.add(baseMidia + "_Track2" + ext);
            candidatos.add(baseMidia + "_Track1" + ext);
            candidatos.add(baseMidia + ext);
        }

        for (String candidato : candidatos) {
            Path path = pastaLegendasEn.resolve(candidato);
            if (Files.isRegularFile(path) && !path.equals(arquivoPt) && !eLegendaTraduzida(path)) {
                return path;
            }
        }

        if (codigoEpisodio != null && Files.isDirectory(pastaLegendasEn)) {
            try (Stream<Path> stream = Files.list(pastaLegendasEn)) {
                return stream
                    .filter(Files::isRegularFile)
                    .filter(this::temExtensaoSuportada)
                    .filter(p -> !p.equals(arquivoPt))
                    .filter(p -> !eLegendaTraduzida(p))
                    .filter(p -> p.getFileName().toString().toUpperCase().contains(codigoEpisodio))
                    .min(Comparator.comparingInt(p -> preferenciaArquivoOriginal(p.getFileName().toString())))
                    .orElse(pastaLegendasEn.resolve(baseSemPt + ext));
            } catch (IOException e) {
                return pastaLegendasEn.resolve(baseSemPt + ext);
            }
        }

        return pastaLegendasEn.resolve(baseSemPt + ext);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: informa se o cache foi mantido DEPOIS da legenda, autorizando a ponte
     * 5→6 a sincronizar o ASS.
     *
     * <p>INVARIANTES DO DOMÍNIO: arquivo ausente ou metadados ilegíveis devolvem
     * {@link FrescorCache#INDETERMINADO}, e o chamador precisa AVISAR — a sincronização automática
     * ficou desligada sem o operador pedir. Empate de data é {@link FrescorCache#LEGENDA_ATUAL}:
     * não autoriza sobrescrita.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; devolve {@link FrescorCache#INDETERMINADO}.
     *
     * @param cache arquivo de cache do episódio
     * @param legenda legenda PT-BR publicada
     * @return o desfecho da comparação
     */
    public FrescorCache compararFrescor(Path cache, Path legenda) {
        if (!Files.isRegularFile(cache) || !Files.isRegularFile(legenda)) {
            return FrescorCache.INDETERMINADO;
        }
        try {
            return Files.getLastModifiedTime(cache).compareTo(Files.getLastModifiedTime(legenda)) > 0
                ? FrescorCache.CACHE_MAIS_NOVO
                : FrescorCache.LEGENDA_ATUAL;
        } catch (IOException e) {
            return FrescorCache.INDETERMINADO;
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reconhece os formatos de legenda que a revisão sabe ler.
     * <p>INVARIANTES DO DOMÍNIO: comparação sem diferenciar maiúsculas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    public boolean temExtensaoSuportada(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase();
        return EXTENSOES.stream().anyMatch(nome::endsWith);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: distingue uma legenda JÁ traduzida de um original inglês, pelo sufixo.
     * <p>INVARIANTES DO DOMÍNIO: é o que impede a revisão de tomar uma tradução como original e
     * "corrigir" uma fala contra ela mesma.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    public boolean eLegendaTraduzida(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase();
        return nome.contains("_ptbr") || nome.contains("_pt-br");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: nomes de cache que valem tentar diretamente, do mais provável ao menos.
     * <p>INVARIANTES DO DOMÍNIO: sem repetição e em ordem estável — a ordem É a política de
     * desempate.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    static List<String> candidatosNomeCache(String baseLegenda, String baseMidia) {
        Set<String> candidatos = new LinkedHashSet<>();
        candidatos.add(baseMidia + "_ENG" + SUFIXO_CACHE);
        candidatos.add(baseMidia + SUFIXO_CACHE);
        candidatos.add(baseLegenda + SUFIXO_CACHE);
        candidatos.add(baseLegenda + "_ENG" + SUFIXO_CACHE);
        return List.copyOf(candidatos);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se um arquivo encontrado na varredura é o cache DESTE episódio
     * DESTA obra.
     *
     * <h2>O prejuízo que originou (2026-08-08)</h2>
     * Até esta data o segundo caminho pedia apenas <b>código de episódio + {@code _ENG}</b>. Como
     * a busca é um {@code Files.walk} sobre a raiz de {@code cache/}, ela enxerga as subpastas de
     * TODAS as obras — e {@code S01E02} existe em qualquer série de TV do acervo. Medido no log de
     * produção do Gundam 0083:
     * <pre>
     *   Analisando: ...0083 - Stardust Memory - S01E02_5_Track3_PT-BR.ass
     *   Cache carregado: Mobile.Suit.Gundam.ZZ.S01E02_ENG.cache.json   (289 entradas)
     *   [CONTEXTO] Seleção manual "gundam_0083" ignorada: a proveniência exige "gundam_zz"
     * </pre>
     * Quatro arquivos do 0083 foram revisados com o cache do <b>Double Zeta</b>. E o efeito não
     * para no cache: {@code AtivadorContextoRevisao} dá precedência à proveniência sobre a seleção
     * manual — corretamente —, então a LORE ativa também virou a da obra errada. Nenhum termo de ZZ
     * chegou a vazar naquela execução (conferido por Paulo, página a página), mas o caminho para
     * isso estava aberto: bastaria o cache alheio conter um termo canônico.
     *
     * <h2>Invariantes do domínio</h2>
     * <ul>
     *   <li>Casa pela base normalizada (caminho exato, inalterado); OU</li>
     *   <li>pelo código de episódio <b>E</b> marca {@code _ENG} <b>E</b> afinidade de OBRA. O
     *       terceiro requisito é o que nasceu aqui: sem ele, episódio igual em série diferente
     *       casa, e o acervo tem dezenas de {@code S01E02}.</li>
     *   <li>Na dúvida, NÃO casa: um cache a menos deixa a fala pendente (recuperável); um cache de
     *       outra obra injeta lore errada na legenda publicada (não recuperável sem auditoria).</li>
     * </ul>
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; entrada nula devolve {@code false}.
     */
    static boolean correspondeCache(String nomeArquivo, String baseMidia, String codigoEpisodio) {
        if (!nomeArquivo.toLowerCase().endsWith(SUFIXO_CACHE)) {
            return false;
        }
        String stem = nomeArquivo.substring(0, nomeArquivo.length() - SUFIXO_CACHE.length());
        if (normalizarBaseLegenda(stem).equalsIgnoreCase(baseMidia)) {
            return true;
        }
        return codigoEpisodio != null
            && nomeArquivo.toUpperCase().contains(codigoEpisodio)
            && nomeArquivo.toUpperCase().contains("_ENG")
            && mesmaObra(stem, baseMidia);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: responde "estes dois nomes de arquivo falam da MESMA obra?", para que
     * o casamento por episódio não atravesse séries.
     *
     * <p>INVARIANTES DO DOMÍNIO: compara os tokens do nome IGNORANDO o que é comum a todo o acervo
     * — o código do episódio, marcas de faixa/idioma e as palavras de franquia ({@code mobile},
     * {@code suit}, {@code gundam}), que sozinhas fariam 0083, ZZ, Unicorn e 08th parecerem a mesma
     * coisa. Sobra o que distingue: {@code 0083}/{@code stardust} contra {@code zz}. Exige ao menos
     * um token distintivo em comum.
     *
     * <p>Quando um dos lados não tem NENHUM token distintivo (nome genérico como
     * {@code S01E02_ENG}), devolve {@code false} — não há evidência de que seja a mesma obra, e o
     * viés é recusar.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada nula ou vazia devolve {@code false}; nunca lança.
     */
    static boolean mesmaObra(String stemCache, String baseMidia) {
        Set<String> tokensCache = tokensDistintivos(stemCache);
        Set<String> tokensMidia = tokensDistintivos(baseMidia);
        if (tokensCache.isEmpty() || tokensMidia.isEmpty()) {
            return false;
        }
        return tokensCache.stream().anyMatch(tokensMidia::contains);
    }

    /** Palavras que aparecem em quase toda obra do acervo e por isso não identificam nada. */
    private static final Set<String> TOKENS_GENERICOS = Set.of(
        "mobile", "suit", "gundam", "the", "of", "and", "season", "complete",
        "eng", "ptbr", "pt", "br", "track", "bd", "tv", "ova", "movie", "part");

    private static Set<String> tokensDistintivos(String nome) {
        if (nome == null || nome.isBlank()) {
            return Set.of();
        }
        Set<String> tokens = new LinkedHashSet<>();
        for (String bruto : nome.toLowerCase(Locale.ROOT).split("[^0-9a-z]+")) {
            if (bruto.isBlank() || TOKENS_GENERICOS.contains(bruto)) {
                continue;
            }
            // Codigo de episodio (s01e02, e13) nao identifica obra — e justamente o que confundia.
            if (bruto.matches("s\\d{1,2}e\\d{1,3}") || bruto.matches("e\\d{1,3}")
                || bruto.matches("s\\d{1,2}") || bruto.matches("\\d{1,2}")) {
                continue;
            }
            tokens.add(bruto);
        }
        return tokens;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o nome do arquivo sem a extensão de legenda.
     * <p>INVARIANTES DO DOMÍNIO: preserva os sufixos de idioma/faixa — quem os remove é
     * {@link #normalizarBaseLegenda(String)}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    static String nomeBaseLegenda(Path arquivoPt) {
        String nome = arquivoPt.getFileName().toString();
        String ext = extensaoLegenda(nome);
        return nome.substring(0, nome.length() - ext.length());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: ordem de preferência entre originais candidatos do mesmo episódio.
     * <p>INVARIANTES DO DOMÍNIO: menor vence. {@code _Track2} primeiro porque, no acervo, é onde a
     * faixa inglesa costuma estar; {@code _ENG} em seguida por ser explícito.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; desconhecido recebe o peso mais alto.
     */
    static int preferenciaArquivoOriginal(String nome) {
        String n = nome.toLowerCase();
        if (n.contains("_track2")) {
            return 0;
        }
        if (n.contains("_eng")) {
            return 1;
        }
        if (n.contains("_track1")) {
            return 2;
        }
        return 10;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reduz o nome à base da MÍDIA, sem sufixo de idioma nem de faixa — é o
     * que faz a legenda PT e o cache inglês do mesmo episódio se encontrarem.
     * <p>INVARIANTES DO DOMÍNIO: a ordem das remoções importa; {@code _PT-BR_Track3} sai inteiro
     * antes de a regra de faixa ser aplicada.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    static String normalizarBaseLegenda(String base) {
        return base
            .replaceFirst("(?i)_PT-?BR(_Track\\d+)?$", "")
            .replaceFirst("(?i)_Track\\d+$", "")
            .replaceFirst("(?i)_ENG$", "");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a extensão de legenda do arquivo.
     * <p>INVARIANTES DO DOMÍNIO: {@code .ass} é o padrão quando não é {@code .ssa}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    static String extensaoLegenda(String nome) {
        return nome.toLowerCase().endsWith(".ssa") ? ".ssa" : ".ass";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: extrai o código {@code SxxEyy} do nome, quando existe.
     * <p>INVARIANTES DO DOMÍNIO: devolve em maiúsculas, para as comparações seguintes não
     * dependerem da grafia do arquivo.
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@code null} quando não há código.
     */
    static String extrairCodigoEpisodio(String nome) {
        Matcher matcher = CODIGO_EPISODIO.matcher(nome);
        return matcher.find() ? matcher.group(1).toUpperCase() : null;
    }
}
