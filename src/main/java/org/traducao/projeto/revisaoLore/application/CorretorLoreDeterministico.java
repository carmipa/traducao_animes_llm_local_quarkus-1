package org.traducao.projeto.revisaoLore.application;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: reforça DETERMINISTICAMENTE a terminologia canônica de lore na
 * revisão (Opção 7), SEM LLM. Cobre casos específicos herdados (nome "Shin" traduzido
 * como "Canela"; "dud rounds" como "rodadas aleatórias") e o mapa genérico da obra ativa
 * (forma-ruim PT → canônico). É o complemento determinístico do prompt de lore: o prompt
 * PEDE ao LLM; esta classe GARANTE nas formas-ruim conhecidas. Espelha, na fatia de
 * revisão de lore, o {@code EnforcadorTermosLore} da fatia de tradução — reimplementado
 * aqui porque a arquitetura proíbe uma fatia importar a outra.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Opera sobre texto MASCARADO (tags viram {@code [[TAGn]]} e ficam intactas).</li>
 *   <li>O mapa só restaura uma forma-ruim quando o original EN contém o termo canônico
 *       na grafia EXATA (nome próprio maiúsculo "Titans" ≠ comum "titans").</li>
 *   <li>Comparações por fronteira de palavra; forma-ruim casa ignorando caixa; canônico
 *       inserido literalmente. Nunca deixa a linha PIOR: classe sem estado.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Entradas nulas/tradução vazia ou nenhuma substituição aplicável devolvem
 * {@link Optional#empty()} (mantém o texto original); não lança.
 */
@Component
public class CorretorLoreDeterministico {

    private static final Pattern PADRAO_SHIN = Pattern.compile("(?<![\\p{L}\\p{N}])Shin(?![\\p{L}\\p{N}])");
    private static final Pattern PADRAO_CANELA = Pattern.compile("(?<![\\p{L}\\p{N}])[Cc]anela(?![\\p{L}\\p{N}])");
    private static final Pattern PADRAO_DUD_ROUNDS = Pattern.compile("(?i)(?<![\\p{L}\\p{N}])dud\\s+rounds?(?![\\p{L}\\p{N}])");
    private static final Pattern PADRAO_RODADAS_ALEATORIAS = Pattern.compile(
        "(?i)(?<![\\p{L}\\p{N}])rodadas\\s+(?:aleat[oó]rias|fracassadas|falsas|dud)(?![\\p{L}\\p{N}])");

    /**
     * PROPÓSITO DE NEGÓCIO: restaura terminologia canônica de lore na fala mascarada,
     * combinando os casos específicos herdados com o mapa genérico da obra.
     *
     * <p>INVARIANTES DO DOMÍNIO: o mapa só aplica quando o original EN contém o canônico
     * na grafia exata; forma-ruim casa por fronteira de palavra, ignorando caixa.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entradas nulas/tradução vazia ou nenhuma
     * substituição aplicável devolvem {@link Optional#empty()}.
     *
     * @param originalMascarado o texto original (EN) já mascarado
     * @param traducaoMascarada a fala traduzida (PT) já mascarada
     * @param correcoesTerminologia mapa forma-ruim (PT) → canônico da obra ativa
     * @return a fala corrigida quando houve alteração; caso contrário {@link Optional#empty()}
     */
    public Optional<String> corrigir(
            String originalMascarado, String traducaoMascarada, Map<String, String> correcoesTerminologia) {
        if (originalMascarado == null || traducaoMascarada == null || traducaoMascarada.isBlank()) {
            return Optional.empty();
        }

        String corrigida = traducaoMascarada;
        if (PADRAO_SHIN.matcher(originalMascarado).find()
            && PADRAO_CANELA.matcher(corrigida).find()) {
            corrigida = PADRAO_CANELA.matcher(corrigida).replaceAll("Shin");
        }

        if (PADRAO_DUD_ROUNDS.matcher(originalMascarado).find()
            && PADRAO_RODADAS_ALEATORIAS.matcher(corrigida).find()) {
            corrigida = PADRAO_RODADAS_ALEATORIAS.matcher(corrigida).replaceAll("munições falhas");
        }

        corrigida = aplicarCorrecoesTerminologia(originalMascarado, corrigida, correcoesTerminologia);

        if (corrigida.equals(traducaoMascarada)) {
            return Optional.empty();
        }
        return Optional.of(corrigida);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aplica o mapa de terminologia da obra, restaurando cada
     * forma-ruim para o canônico quando o original EN contém o canônico.
     *
     * <p>INVARIANTES DO DOMÍNIO: canônico verificado no original por fronteira de palavra
     * e SENSÍVEL à caixa; forma-ruim substituída por fronteira de palavra IGNORANDO caixa.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa nulo/vazio ou pares inválidos devolvem o
     * texto inalterado.
     */
    private String aplicarCorrecoesTerminologia(String original, String traducao, Map<String, String> correcoes) {
        if (correcoes == null || correcoes.isEmpty()) {
            return traducao;
        }
        String resultado = traducao;
        for (Map.Entry<String, String> par : correcoes.entrySet()) {
            String formaRuim = par.getKey();
            String canonico = par.getValue();
            if (formaRuim == null || formaRuim.isBlank() || canonico == null) {
                continue;
            }
            if (!contemTermoCanonico(original, canonico)) {
                continue;
            }
            Pattern formaRuimPat = Pattern.compile(
                "(?<![\\p{L}\\p{N}])" + Pattern.quote(formaRuim) + "(?![\\p{L}\\p{N}])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            if (formaRuimPat.matcher(resultado).find()) {
                resultado = formaRuimPat.matcher(resultado).replaceAll(Matcher.quoteReplacement(canonico));
            }
        }
        return resultado;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: confirma que o original EN contém o termo canônico na grafia
     * exata — nome próprio maiúsculo, distinto da palavra comum minúscula.
     * <p>INVARIANTES DO DOMÍNIO: fronteira de palavra; comparação SENSÍVEL à caixa.
     * <p>COMPORTAMENTO EM CASO DE FALHA: termo em branco devolve {@code false}.
     */
    private boolean contemTermoCanonico(String texto, String termo) {
        if (termo.isBlank()) {
            return false;
        }
        return Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(termo) + "(?![\\p{L}\\p{N}])")
            .matcher(texto).find();
    }
}
