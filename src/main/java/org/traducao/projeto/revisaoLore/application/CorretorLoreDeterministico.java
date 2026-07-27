package org.traducao.projeto.revisaoLore.application;

import org.springframework.stereotype.Component;
import org.traducao.projeto.qualidadeTraducao.application.EnforcadorTermosLore;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: reforça DETERMINISTICAMENTE a terminologia canônica de lore na
 * revisão (Opção 7), SEM LLM. Cobre casos específicos herdados (nome "Shin" traduzido
 * como "Canela"; "dud rounds" como "rodadas aleatórias") e delega o mapa genérico da obra
 * ativa ao {@link EnforcadorTermosLore}. É o complemento determinístico do prompt de lore:
 * o prompt PEDE ao LLM; esta classe GARANTE nas formas-ruim conhecidas.
 *
 * <h2>Por que o mapa genérico é delegado, e não reimplementado</h2>
 * Esta classe carregava uma SEGUNDA cópia do algoritmo do enforcer, justificada no
 * comentário original por "a arquitetura proíbe uma fatia importar a outra". A justificativa
 * valia para fatia, não para PEER — e o custo apareceu: as duas cópias divergiram em três
 * propriedades, todas medidas antes desta mudança.
 * <ul>
 *   <li><b>Teto de ocorrências.</b> A cópia daqui usava {@code replaceAll}, trocando TODAS as
 *       ocorrências da forma-ruim. Com {@code "Quatro" → "Quattro"} no mapa de Zeta/ZZ, a fala
 *       "Quattro, há quatro inimigos" virava "Quattro, há Quattro inimigos" — o NÚMERO quatro
 *       corrompido. O enforcer restaura no máximo tantas quantas o canônico aparece no EN.</li>
 *   <li><b>Ordem por comprimento.</b> Sem ordenar as chaves da mais longa para a mais curta,
 *       {@code "Vazio"→"Void"} destruía {@code "Genoma do Vazio"→"Void Genome"}.</li>
 *   <li><b>Canônico multi-palavra.</b> A checagem era sensível à caixa para todo termo, então
 *       o EN "two mobile suits" não reconhecia o canônico "Mobile Suits" e a restauração nunca
 *       disparava.</li>
 * </ul>
 * O mapa forma-ruim→canônico continua sendo do peer {@code contexto}; o ALGORITMO passa a ter
 * uma implementação só, no peer {@code qualidadeTraducao}, que age sobre o texto produzido.
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

    private final EnforcadorTermosLore enforcadorTermosLore;

    /**
     * PROPÓSITO DE NEGÓCIO: injeta a implementação ÚNICA do reforço de terminologia.
     * <p>INVARIANTES DO DOMÍNIO: guarda a referência recebida; nenhuma cópia local do algoritmo.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede o uso do serviço.
     */
    public CorretorLoreDeterministico(EnforcadorTermosLore enforcadorTermosLore) {
        this.enforcadorTermosLore = enforcadorTermosLore;
    }

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
        // Só restaura "Shin" quando o nome NÃO está presente na grafia correta no PT: no
        // erro real o nome virou "canela" (some do PT); se "Shin" já está no PT, o "canela"
        // é a especiaria (falso-positivo) e não pode ser tocado.
        if (PADRAO_SHIN.matcher(originalMascarado).find()
            && !PADRAO_SHIN.matcher(corrigida).find()
            && PADRAO_CANELA.matcher(corrigida).find()) {
            corrigida = PADRAO_CANELA.matcher(corrigida).replaceAll("Shin");
        }

        if (PADRAO_DUD_ROUNDS.matcher(originalMascarado).find()
            && PADRAO_RODADAS_ALEATORIAS.matcher(corrigida).find()) {
            corrigida = PADRAO_RODADAS_ALEATORIAS.matcher(corrigida).replaceAll("munições falhas");
        }

        corrigida = enforcadorTermosLore.reforcar(originalMascarado, corrigida, correcoesTerminologia);

        if (corrigida.equals(traducaoMascarada)) {
            return Optional.empty();
        }
        return Optional.of(corrigida);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: variante PT-only (sem o inglês) da restauração de terminologia —
     * para a revisão de lore quando só existe a legenda PT-BR. Sem o EN não há como desambiguar
     * homógrafo de uma palavra ({@code Vazio}=Void vs {@code vazio}=empty); por isso aplica SÓ os
     * termos INEQUÍVOCOS: forma-ruim MULTI-PALAVRA (contém espaço), improvável de colidir com
     * palavra comum. Homógrafos de uma palavra ficam para o LLM PT-only.
     *
     * <p>INVARIANTES DO DOMÍNIO: só aplica forma-ruim com espaço; fronteira de palavra;
     * forma-ruim casa ignorando caixa; canônico inserido literalmente; nunca deixa pior.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: tradução vazia, mapa vazio ou nenhuma substituição
     * aplicável devolve {@link Optional#empty()}; não lança.
     *
     * @param traducaoMascarada a fala PT já mascarada
     * @param correcoesTerminologia mapa forma-ruim (PT) → canônico da obra ativa
     * @return a fala corrigida quando houve alteração; caso contrário {@link Optional#empty()}
     */
    public Optional<String> corrigirPtOnly(String traducaoMascarada, Map<String, String> correcoesTerminologia) {
        if (traducaoMascarada == null || traducaoMascarada.isBlank()
            || correcoesTerminologia == null || correcoesTerminologia.isEmpty()) {
            return Optional.empty();
        }
        String resultado = traducaoMascarada;
        // Frases longas primeiro: sem isto "Traje Móvel"→"Mobile Suit" mutila
        // "Traje Móvel de Combate" antes de a entrada completa ser tentada. Mesma regra que o
        // EnforcadorTermosLore aplica no caminho com o inglês; aqui não dá para delegar porque
        // sem o EN não existe o portão "o original contém o canônico".
        var pares = correcoesTerminologia.entrySet().stream()
            .sorted(Comparator.comparingInt((Map.Entry<String, String> e) ->
                e.getKey() == null ? 0 : e.getKey().length()).reversed())
            .toList();
        for (Map.Entry<String, String> par : pares) {
            String formaRuim = par.getKey();
            String canonico = par.getValue();
            if (formaRuim == null || formaRuim.isBlank() || canonico == null) {
                continue;
            }
            // Só termos INEQUÍVOCOS sem o EN: forma-ruim multi-palavra (contém espaço). Homógrafo
            // de uma palavra ("Vazio") é pulado — sem o original não dá para separar do comum.
            if (formaRuim.trim().indexOf(' ') < 0) {
                continue;
            }
            Pattern formaRuimPat = Pattern.compile(
                "(?<![\\p{L}\\p{N}])" + Pattern.quote(formaRuim) + "(?![\\p{L}\\p{N}])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            if (formaRuimPat.matcher(resultado).find()) {
                resultado = formaRuimPat.matcher(resultado).replaceAll(Matcher.quoteReplacement(canonico));
            }
        }
        return resultado.equals(traducaoMascarada) ? Optional.empty() : Optional.of(resultado);
    }

}
