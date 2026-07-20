package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.qualidadeTraducao.application.NormalizadorAcentosComuns;

import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: revisa UMA fala de legenda em português usando SÓ correções
 * determinísticas PT-side — sem o inglês original e sem o cache bilíngue. Atende o caso em
 * que só existe a tradução PT-BR (como a revisão de lore): repõe acentos inequívocos, tira o
 * {@code \N} órfão antes de pontuação e aplica a concordância determinística que não exige o
 * original. Asterisco (censura/markdown do LLM) é SINALIZADO, não corrigido — sem o original
 * não dá para retraduzir a palavra danificada.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Nenhuma correção depende do inglês: só transformações válidas olhando apenas o PT.</li>
 *   <li>Reusa os blocos já testados: {@link NormalizadorAcentosComuns} (acentos inequívocos) e
 *       {@link CorretorDeterministicoConcordanciaService} chamado com {@code original = null}
 *       (aplica só o subconjunto PT-only: "graças a Deus", possessivos de parentesco).</li>
 *   <li>Asterisco no texto visível é só reportado ({@code temAsterisco}); a fala não é
 *       reescrita para escondê-lo. Serviço sem estado/sem I/O.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Fala {@code null}/vazia devolve {@link ResultadoFala} inalterado com {@code alterado=false};
 * nunca lança.
 */
@Service
public class RevisorPtOnlyService {

    /** {@code \N} imediatamente antes de pontuação — quebra que orfaniza o sinal na linha. */
    private static final Pattern QUEBRA_ANTES_PONTUACAO = Pattern.compile("\\\\N\\s*([,.;:!?])");
    private static final Pattern ASTERISCO = Pattern.compile("\\*");
    private static final Pattern TAG_ASS = Pattern.compile("\\{[^}]*}");

    private final NormalizadorAcentosComuns acentos;
    private final CorretorDeterministicoConcordanciaService concordancia;

    public RevisorPtOnlyService(
        NormalizadorAcentosComuns acentos,
        CorretorDeterministicoConcordanciaService concordancia
    ) {
        this.acentos = acentos;
        this.concordancia = concordancia;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: resultado da revisão PT-only de uma fala — o texto (corrigido ou
     * não), se houve mudança e se sobrou asterisco a sinalizar.
     * <p>INVARIANTES DO DOMÍNIO: {@code alterado} só é true quando {@code texto} difere da
     * entrada; {@code temAsterisco} reflete o texto FINAL.
     * <p>COMPORTAMENTO EM CASO DE FALHA: portador de dados puro.
     */
    public record ResultadoFala(String texto, boolean alterado, boolean temAsterisco) {}

    /**
     * PROPÓSITO DE NEGÓCIO: aplica a cadeia de correções determinísticas PT-only a uma fala.
     * <p>INVARIANTES DO DOMÍNIO: acentos → move {@code \N} para depois da pontuação →
     * concordância PT-only; nada que dependa do inglês é aplicado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada vazia devolve resultado inalterado.
     */
    public ResultadoFala revisarFala(String pt) {
        if (pt == null || pt.isBlank()) {
            return new ResultadoFala(pt, false, false);
        }
        String r = acentos.normalizar(pt);
        r = QUEBRA_ANTES_PONTUACAO.matcher(r).replaceAll("$1\\\\N");
        r = concordancia.corrigir(null, r).orElse(r);
        String visivel = TAG_ASS.matcher(r).replaceAll("");
        boolean temAsterisco = ASTERISCO.matcher(visivel).find();
        return new ResultadoFala(r, !r.equals(pt), temAsterisco);
    }
}
