package org.traducao.projeto.legenda.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: no karaokê de fansub a mesma frase aparece em DUAS camadas simultâneas —
 * o romaji (a língua original, que nunca se traduz) e a tradução (inglês no release, PT-BR depois).
 * Esta classe é a regra pura que reconhece esse par pelo TEMPO e diz qual das duas linhas é a
 * original a preservar. É o sinal PRIMÁRIO da proteção de romaji, acima do conteúdo e muito acima
 * do nome do estilo ou da fonte — que Paulo normaliza para Arial de propósito e portanto não podem
 * sustentar a decisão.
 *
 * <p>Medido no arquivo real (Guilty Crown ep4): {@code OP Roma} (10 linhas) tem parceira no tempo
 * em 10/10, {@code ED Roma L1} (11) em 11/11, e o diálogo {@code Default} (253) em 0/253. O
 * pareamento separa música de diálogo com precisão e — diferente do conteúdo — continua valendo
 * quando a letra é deliberadamente bilíngue (caso Sawano), em que as duas camadas pontuam parecido.
 *
 * <p>INVARIANTES DO DOMÍNIO: classe pura, sem framework, sem I/O e sem estado — recebe fatos já
 * medidos ({@code estiloMarcadoRomaji}, {@code escoreRomaji}) e devolve pares. Cada camada entra em
 * no máximo UM par. Sobreposição é medida contra a MENOR das duas janelas, então uma linha longa
 * não "engole" uma curta que apenas a tangencia. Quando os dois lados são indistinguíveis a decisão
 * é {@link Original#INDECISO} — nunca um chute: errar aqui ou deixa o karaokê sem tradução ou
 * destrói o romaji, e o viés do projeto é preservar.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: entradas nulas viram lista vazia; camadas sem janela válida
 * são ignoradas pelo pareamento; nenhum método lança.
 */
public final class PareamentoCamadasMusicais {

    /** Fração mínima de sobreposição (sobre a menor janela) para duas linhas serem o mesmo verso. */
    public static final double SOBREPOSICAO_MINIMA = 0.6;

    /**
     * Diferença mínima de escore de romaji para eleger a original pelo conteúdo. Abaixo disso as
     * camadas são consideradas empatadas — é o caso da letra bilíngue, onde chutar custa caro.
     */
    public static final int MARGEM_MINIMA_ESCORE = 20;

    private static final Pattern TEMPO_ASS_PATTERN = Pattern.compile("(\\d+):([0-5]\\d):([0-5]\\d)[.,](\\d{2})");

    private PareamentoCamadasMusicais() {
    }

    /**
     * Uma linha musical candidata a formar par, com a janela de tempo e os dois sinais já medidos
     * por quem chamou (o detector de karaokê vive na camada de aplicação; o domínio não o conhece).
     *
     * @param indice posição ordinal do evento na legenda
     * @param estilo nome do estilo, guardado só para diagnóstico
     * @param texto texto do evento, com tags
     * @param inicioCs início em centésimos de segundo
     * @param fimCs fim em centésimos de segundo
     * @param estiloMarcadoRomaji o NOME do estilo declara romaji/japonês (sinal de reforço)
     * @param escoreRomaji proporção de palavras romaji do texto visível, de 0 a 100
     */
    public record Camada(
        int indice,
        String estilo,
        String texto,
        long inicioCs,
        long fimCs,
        boolean estiloMarcadoRomaji,
        int escoreRomaji
    ) {
        /** Duração da janela; nunca negativa. */
        public long duracaoCs() {
            return Math.max(0, fimCs - inicioCs);
        }
    }

    /** Qual lado do par é a linha original (romaji) a preservar. */
    public enum Original {
        /** A primeira camada do par é a original. */
        PRIMEIRA,
        /** A segunda camada do par é a original. */
        SEGUNDA,
        /** Os dois lados são indistinguíveis — não decidir (viés de preservar as duas). */
        INDECISO
    }

    /**
     * Duas linhas simultâneas reconhecidas como o mesmo verso.
     *
     * @param primeira camada que começa antes (empate resolvido pelo índice)
     * @param segunda a outra camada do par
     * @param sobreposicao fração de sobreposição medida sobre a menor janela
     * @param original qual das duas é o romaji, ou {@link Original#INDECISO}
     */
    public record Par(Camada primeira, Camada segunda, double sobreposicao, Original original) {

        /** A camada a PRESERVAR (romaji), vazia quando indeciso. */
        public Optional<Camada> camadaPreservar() {
            return switch (original) {
                case PRIMEIRA -> Optional.of(primeira);
                case SEGUNDA -> Optional.of(segunda);
                case INDECISO -> Optional.empty();
            };
        }

        /** A camada alvo de TRADUÇÃO, vazia quando indeciso. */
        public Optional<Camada> camadaTraduzir() {
            return switch (original) {
                case PRIMEIRA -> Optional.of(segunda);
                case SEGUNDA -> Optional.of(primeira);
                case INDECISO -> Optional.empty();
            };
        }
    }

    /** Janela de tempo de um evento, em centésimos de segundo. */
    public record Janela(long inicioCs, long fimCs) {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: extrai a janela de tempo do prefixo estrutural de um evento ASS, o
     * único lugar onde ela existe hoje — {@code EventoLegenda} guarda o prefixo cru
     * ({@code "Dialogue: 0,0:01:23.45,0:01:26.78,ED_S2_roma,,0,0,0,,"}) e não expõe tempo.
     *
     * <p>INVARIANTES DO DOMÍNIO: lê os DOIS primeiros carimbos {@code H:MM:SS.cc} na ordem em que
     * aparecem, o que independe da ordem das colunas declarada no {@code Format:} e funciona também
     * no dialeto SSA ({@code Marked=0}). Vírgula é aceita como separador decimal.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: prefixo nulo, sem dois carimbos, ou com fim anterior ao
     * início devolve {@link Optional#empty()} — nunca lança e nunca inventa janela.
     *
     * @param prefixo prefixo estrutural da linha, como preservado pelo leitor ASS
     * @return a janela, ou vazio quando não há dois tempos válidos
     */
    public static Optional<Janela> janelaDoPrefixo(String prefixo) {
        if (prefixo == null) {
            return Optional.empty();
        }
        Matcher m = TEMPO_ASS_PATTERN.matcher(prefixo);
        if (!m.find()) {
            return Optional.empty();
        }
        long inicio = paraCentisegundos(m);
        if (!m.find()) {
            return Optional.empty();
        }
        long fim = paraCentisegundos(m);
        if (fim < inicio) {
            return Optional.empty();
        }
        return Optional.of(new Janela(inicio, fim));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mede o quanto duas linhas ocupam o mesmo instante da tela — a evidência
     * de que são o mesmo verso em duas línguas.
     *
     * <p>INVARIANTES DO DOMÍNIO: a fração é calculada sobre a MENOR das duas durações, então uma
     * linha curta totalmente contida numa longa dá 1,0 (é o padrão real do karaokê, em que a camada
     * traduzida costuma ter janela um pouco maior que a romaji).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: camada nula ou de duração zero devolve {@code 0}.
     *
     * @return fração de 0 a 1
     */
    public static double sobreposicao(Camada a, Camada b) {
        if (a == null || b == null) {
            return 0;
        }
        long inicio = Math.max(a.inicioCs(), b.inicioCs());
        long fim = Math.min(a.fimCs(), b.fimCs());
        long intersecao = Math.max(0, fim - inicio);
        long menor = Math.min(a.duracaoCs(), b.duracaoCs());
        if (menor <= 0) {
            return 0;
        }
        return (double) intersecao / menor;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: agrupa as camadas musicais em pares romaji × tradução, resolvendo para
     * cada par quem é a original. É a função que uma futura ligação no seletor de traduzíveis vai
     * consultar; nesta fase ela é pura e não está ligada a nenhum caminho de execução.
     *
     * <p>INVARIANTES DO DOMÍNIO: percorre em ordem de tempo e casa cada camada com a parceira de
     * MAIOR sobreposição ainda livre, acima de {@link #SOBREPOSICAO_MINIMA} — cada camada entra em
     * no máximo um par, e linha sem parceira (diálogo, verso solo) simplesmente não aparece no
     * resultado. A escolha da original segue a ordem de confiança da decisão de 2026-07-25: nome do
     * estilo quando apenas UM lado o declara, senão o escore de conteúdo com margem, senão
     * {@link Original#INDECISO}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista nula ou com menos de duas camadas devolve lista
     * vazia; elementos nulos são ignorados; nunca lança.
     *
     * @param camadas candidatas já filtradas como musicais por quem chamou
     * @return pares encontrados, em ordem de tempo
     */
    public static List<Par> parear(List<Camada> camadas) {
        if (camadas == null || camadas.size() < 2) {
            return List.of();
        }
        List<Camada> ordenadas = new ArrayList<>();
        for (Camada c : camadas) {
            if (c != null) {
                ordenadas.add(c);
            }
        }
        ordenadas.sort(Comparator.comparingLong(Camada::inicioCs).thenComparingInt(Camada::indice));

        boolean[] usada = new boolean[ordenadas.size()];
        List<Par> pares = new ArrayList<>();
        for (int i = 0; i < ordenadas.size(); i++) {
            if (usada[i]) {
                continue;
            }
            Camada atual = ordenadas.get(i);
            int melhor = -1;
            double melhorSobreposicao = 0;
            for (int j = i + 1; j < ordenadas.size(); j++) {
                if (usada[j]) {
                    continue;
                }
                Camada candidata = ordenadas.get(j);
                // Ordenado por início: quem começa depois do fim da atual não pode sobrepor, e
                // nenhum candidato seguinte poderá — corta a varredura.
                if (candidata.inicioCs() >= atual.fimCs()) {
                    break;
                }
                double s = sobreposicao(atual, candidata);
                if (s >= SOBREPOSICAO_MINIMA && s > melhorSobreposicao) {
                    melhor = j;
                    melhorSobreposicao = s;
                }
            }
            if (melhor < 0) {
                continue;
            }
            usada[i] = true;
            usada[melhor] = true;
            Camada parceira = ordenadas.get(melhor);
            pares.add(new Par(atual, parceira, melhorSobreposicao, decidirOriginal(atual, parceira)));
        }
        return List.copyOf(pares);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: dentro de um par já reconhecido, decide qual linha é o romaji original.
     *
     * <p>INVARIANTES DO DOMÍNIO: nome de estilo só decide quando exatamente UM dos lados o declara
     * (nos dois lados, ou em nenhum, ele não discrimina). O conteúdo só decide com margem de pelo
     * menos {@link #MARGEM_MINIMA_ESCORE} pontos — abaixo disso é empate declarado, não chute.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: camada nula devolve {@link Original#INDECISO}.
     */
    public static Original decidirOriginal(Camada a, Camada b) {
        if (a == null || b == null) {
            return Original.INDECISO;
        }
        if (a.estiloMarcadoRomaji() != b.estiloMarcadoRomaji()) {
            return a.estiloMarcadoRomaji() ? Original.PRIMEIRA : Original.SEGUNDA;
        }
        int diferenca = a.escoreRomaji() - b.escoreRomaji();
        if (Math.abs(diferenca) < MARGEM_MINIMA_ESCORE) {
            return Original.INDECISO;
        }
        return diferenca > 0 ? Original.PRIMEIRA : Original.SEGUNDA;
    }

    private static long paraCentisegundos(Matcher m) {
        long horas = Long.parseLong(m.group(1));
        long minutos = Long.parseLong(m.group(2));
        long segundos = Long.parseLong(m.group(3));
        long centesimos = Long.parseLong(m.group(4));
        return ((horas * 60 + minutos) * 60 + segundos) * 100 + centesimos;
    }
}
