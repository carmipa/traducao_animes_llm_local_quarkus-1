package org.traducao.projeto.traducao.domain.fallback;

import java.util.EnumMap;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: desfecho agregado de uma rodada de recuperação de pendências — as falas
 * efetivamente recuperadas E a contagem de tentativas por CAUSA. Existe porque "recuperou 12 de
 * 80" não permite decidir nada: sem saber se as 68 restantes caíram por provedor fora do ar, por
 * marcador corrompido ou por guarda de lore, não há como julgar se vale trocar de provedor,
 * ajustar a guarda ou subir um container. É o relatório que transforma atividade em diagnóstico.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code recuperadas} contém apenas os pares original→tradução aprovados; a soma de
 *       {@code porCausa} cobre TODAS as tentativas, inclusive as bem-sucedidas
 *       ({@link StatusFallback#RECUPERADA}).</li>
 *   <li>Ambos os mapas são cópias defensivas e imutáveis — o chamador não altera o relatório.</li>
 *   <li>Record puro de domínio: só JDK, sem framework e sem I/O.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Mapas nulos viram vazios; não lança. Um relatório vazio é um resultado legítimo (modo
 * desligado ou nenhuma pendência), não um erro.
 *
 * @param recuperadas pares original→tradução que passaram por todas as verificações
 * @param porCausa quantas tentativas terminaram em cada {@link StatusFallback}
 */
public record ResultadoRecuperacao(
    Map<String, String> recuperadas,
    Map<StatusFallback, Integer> porCausa
) {

    public ResultadoRecuperacao {
        recuperadas = recuperadas == null ? Map.of() : Map.copyOf(recuperadas);
        porCausa = porCausa == null || porCausa.isEmpty()
            ? Map.of()
            : Map.copyOf(new EnumMap<>(porCausa));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: relatório de uma rodada que não aconteceu (modo desligado ou nenhuma
     * pendência), para o chamador não precisar tratar {@code null}.
     * <p>INVARIANTES DO DOMÍNIO: ambos os mapas vazios.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    public static ResultadoRecuperacao vazio() {
        return new ResultadoRecuperacao(Map.of(), Map.of());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: linha única e legível para o console/telemetria, listando só as
     * causas que de fato ocorreram — um relatório que imprime dez zeros esconde o que importa.
     * <p>INVARIANTES DO DOMÍNIO: ordem estável (a do enum); causas com zero são omitidas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem causas devolve texto indicando nenhuma tentativa.
     */
    public String resumoPorCausa() {
        if (porCausa.isEmpty()) {
            return "nenhuma tentativa";
        }
        StringBuilder sb = new StringBuilder();
        for (StatusFallback status : StatusFallback.values()) {
            Integer n = porCausa.get(status);
            if (n != null && n > 0) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(status).append('=').append(n);
            }
        }
        return sb.toString();
    }
}
