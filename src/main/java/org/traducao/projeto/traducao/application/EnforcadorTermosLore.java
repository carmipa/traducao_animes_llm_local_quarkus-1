package org.traducao.projeto.traducao.application;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: reforça DETERMINISTICAMENTE a terminologia oficial de uma obra
 * na legenda já traduzida. Quando o LLM traduz um termo de mundo que a lore manda manter
 * no idioma original (ex.: "Legion" virou "Legião", "Undertaker" virou "Coveiro"), este
 * serviço restaura a grafia canônica — sem marcador, sem rede e sem depender do modelo.
 * É o complemento pós-tradução do prompt de lore: o prompt PEDE ao LLM; este serviço
 * GARANTE nas formas-ruim conhecidas.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só restaura uma forma-ruim quando o texto ORIGINAL (EN) contém o termo canônico
 *       correspondente — nunca altera uma tradução legítima que não veio daquele termo.</li>
 *   <li>Comparações por fronteira de palavra, ignorando caixa; o termo canônico é inserido
 *       exatamente como definido na lore.</li>
 *   <li>Nunca pode deixar a linha PIOR: mapa vazio ou sem casamento devolve o texto
 *       traduzido inalterado (pior caso = comportamento de hoje). Classe sem estado.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Argumentos nulos ou mapa vazio devolvem o texto traduzido como recebido; não lança.
 */
@Component
public class EnforcadorTermosLore {

    /**
     * PROPÓSITO DE NEGÓCIO: restaura os termos canônicos da lore na fala traduzida.
     *
     * <p>INVARIANTES DO DOMÍNIO: para cada par (forma-ruim → canônico), só substitui se o
     * original contém o canônico e a tradução contém a forma-ruim; substituição por
     * fronteira de palavra, ignorando caixa.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code original}, {@code traduzido} ou {@code correcoes}
     * nulos/vazios devolvem {@code traduzido} inalterado.
     *
     * @param original o texto original (EN) da fala
     * @param traduzido a fala já traduzida (PT)
     * @param correcoes mapa forma-ruim (PT) → termo canônico a restaurar
     * @return a fala traduzida com os termos canônicos restaurados quando aplicável
     */
    public String reforcar(String original, String traduzido, Map<String, String> correcoes) {
        if (original == null || traduzido == null || correcoes == null || correcoes.isEmpty()) {
            return traduzido;
        }
        String resultado = traduzido;
        for (Map.Entry<String, String> par : correcoes.entrySet()) {
            String formaRuim = par.getKey();
            String canonico = par.getValue();
            if (formaRuim == null || formaRuim.isBlank() || canonico == null) {
                continue;
            }
            if (!contemCanonico(original, canonico)) {
                continue; // o original não tinha o termo canônico (grafia exata): não mexe
            }
            Pattern formaRuimPat = padraoFormaRuim(formaRuim);
            if (formaRuimPat.matcher(resultado).find()) {
                resultado = formaRuimPat.matcher(resultado).replaceAll(Matcher.quoteReplacement(canonico));
            }
        }
        return resultado;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: confirma que o ORIGINAL contém o termo canônico na grafia
     * EXATA — os termos de lore são nomes próprios maiúsculos ("Legion"), distinguindo-os
     * da palavra comum minúscula ("legion") que NÃO deve disparar a restauração.
     * <p>INVARIANTES DO DOMÍNIO: fronteira de palavra; comparação SENSÍVEL à caixa.
     * <p>COMPORTAMENTO EM CASO DE FALHA: termo vazio devolve {@code false}.
     */
    private boolean contemCanonico(String texto, String termo) {
        if (termo.isBlank()) {
            return false;
        }
        return Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(termo) + "(?![\\p{L}\\p{N}])")
            .matcher(texto).find();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: compila o padrão da forma-ruim em PT — que pode aparecer em
     * qualquer caixa (ex.: "a legião" minúsculo depois de artigo).
     * <p>INVARIANTES DO DOMÍNIO: fronteira de palavra; {@code CASE_INSENSITIVE + UNICODE_CASE}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: termo é escapado; não injeta regex.
     */
    private Pattern padraoFormaRuim(String termo) {
        return Pattern.compile(
            "(?<![\\p{L}\\p{N}])" + Pattern.quote(termo) + "(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }
}
