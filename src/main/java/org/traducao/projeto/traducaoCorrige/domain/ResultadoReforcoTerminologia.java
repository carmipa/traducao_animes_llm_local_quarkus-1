package org.traducao.projeto.traducaoCorrige.domain;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * PROPÓSITO DE NEGÓCIO: totais de uma passada de reforço de terminologia sobre cache JÁ GRAVADO,
 * com o contador POR TERMO que é a razão de esta operação existir.
 *
 * <p>O contador resolve uma medição que até aqui era circular. O reforço determinístico roda
 * ANTES da gravação do cache, então "zero formas-ruim no cache" nunca conseguiu distinguir
 * <i>"o modelo acertou"</i> de <i>"o enforcer consertou"</i> — os dois produzem exatamente o mesmo
 * arquivo. Contar quantas vezes CADA termo canônico foi restaurado separa as duas hipóteses pela
 * primeira vez.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code aplicado} distingue o ensaio da execução real. Em ensaio ({@code false}) os números
 *       descrevem o que ACONTECERIA e nenhum byte foi tocado no disco — ler um relatório de ensaio
 *       como se fosse execução é o erro que este campo existe para impedir.</li>
 *   <li>{@code restauracoesPorTermo} é indexado pelo termo CANÔNICO (o destino), não pela
 *       forma-ruim: várias formas-ruim convergem para o mesmo canônico ("Sabre de Raio",
 *       "Espada de Raio" e "Lâmina de Energia" são todas Beam Saber) e o que interessa medir é a
 *       conformidade do termo oficial.</li>
 *   <li>Totais negativos são normalizados para zero; o mapa nunca é {@code null}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Record imutável, sem I/O. Arquivos que falharam entram em {@code falhas} e ficam intactos no
 * disco; não são contados como alterados.
 *
 * @param arquivosAnalisados arquivos de cache abertos e julgados
 * @param arquivosAlterados arquivos com ao menos uma fala mudada (zero em ensaio)
 * @param falasAlteradas falas em que ao menos um termo foi restaurado
 * @param restauracoesPorTermo termo canônico → quantas restaurações
 * @param falhas arquivos que não puderam ser processados
 * @param aplicado {@code false} para ensaio (dry-run); {@code true} quando o disco foi escrito
 */
public record ResultadoReforcoTerminologia(
    int arquivosAnalisados,
    int arquivosAlterados,
    int falasAlteradas,
    Map<String, Integer> restauracoesPorTermo,
    int falhas,
    boolean aplicado
) {

    /**
     * PROPÓSITO DE NEGÓCIO: normaliza o record na construção.
     * <p>INVARIANTES DO DOMÍNIO: totais nunca negativos; mapa nunca nulo e sempre imutável.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança para entrada malformada; corrige.
     */
    public ResultadoReforcoTerminologia {
        arquivosAnalisados = Math.max(0, arquivosAnalisados);
        arquivosAlterados = Math.max(0, arquivosAlterados);
        falasAlteradas = Math.max(0, falasAlteradas);
        falhas = Math.max(0, falhas);
        restauracoesPorTermo = restauracoesPorTermo == null
            ? Map.of() : Map.copyOf(restauracoesPorTermo);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: soma de todas as restaurações, para a linha de resumo.
     * <p>INVARIANTES DO DOMÍNIO: igual à soma dos valores do mapa.
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa vazio devolve zero.
     */
    public int totalRestauracoes() {
        return restauracoesPorTermo.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: status agregado fiel — em particular, ENSAIO nunca se parece com uma
     * execução concluída, porque confundir os dois faria o operador acreditar que o acervo foi
     * corrigido quando nada foi escrito.
     *
     * <p>INVARIANTES DO DOMÍNIO: falha tem precedência sobre tudo; ensaio tem precedência sobre
     * os desfechos de sucesso; sem arquivos, o status diz isso em vez de fingir sucesso.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: função pura; nunca lança.
     */
    public String status() {
        if (falhas > 0) {
            return "CONCLUIDO_COM_FALHAS";
        }
        if (arquivosAnalisados == 0) {
            return "NENHUM_CACHE_ENCONTRADO";
        }
        if (!aplicado) {
            return totalRestauracoes() > 0 ? "ENSAIO_COM_PENDENCIAS" : "ENSAIO_SEM_PENDENCIAS";
        }
        return totalRestauracoes() > 0 ? "CONCLUIDO" : "CONCLUIDO_SEM_ALTERACOES";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: ordena o contador do termo mais restaurado para o menos, que é como
     * o operador lê ("Beam Saber 41×, Normal Suit 19×, Axis 8×") para decidir onde a lore ainda
     * não está pegando.
     *
     * <p>INVARIANTES DO DOMÍNIO: empate desempata por nome do termo, para o relatório ser
     * determinístico entre execuções.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa vazio devolve mapa vazio.
     */
    public Map<String, Integer> porFrequencia() {
        Map<String, Integer> ordenado = new LinkedHashMap<>();
        new TreeMap<>(restauracoesPorTermo).entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()))
            .forEach(e -> ordenado.put(e.getKey(), e.getValue()));
        return ordenado;
    }
}
