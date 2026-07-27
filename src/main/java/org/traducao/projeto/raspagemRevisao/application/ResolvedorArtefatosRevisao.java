package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.raspagemRevisao.domain.FrescorCache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
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
     * PROPÓSITO DE NEGÓCIO: decide se um arquivo encontrado na varredura é o cache do episódio.
     * <p>INVARIANTES DO DOMÍNIO: casa pela base normalizada OU pelo código de episódio somado à
     * marca {@code _ENG} — o segundo caminho existe porque nem toda obra nomeia o cache pela mídia.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
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
            && nomeArquivo.toUpperCase().contains("_ENG");
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
