package org.traducao.projeto.medicao;

import org.traducao.projeto.revisaoConcordancia.application.CorretorAcentoDeDicionarioNaFalaService;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: dono único de <i>"esta palavra é nome próprio?"</i> quando a resposta
 * precisa vir do ACERVO INTEIRO, e não de uma fala só.
 *
 * <h2>O prejuízo que obrigou isto a existir</h2>
 * Em 25/08/2026 uma medição do alcance da tela 3.3 reportou <b>6.315 falas</b> com "palavra que o
 * dicionário reprova". A lista era dominada por nome de personagem — {@code Gundam} sozinho
 * aparecia <b>3.573</b> vezes, {@code Judau} 498, {@code Banagher} 145.
 *
 * <p>O filtro usado ali era o de UMA fala: capitalizada fora do início de frase. Ele é o certo
 * para proteger lore na hora de corrigir, e é insuficiente para MEDIR — porque nome de personagem
 * abre fala o tempo todo ({@code "Gundam!"}, {@code "Judau, cuidado!"}), e ali ele fica
 * desprotegido.
 *
 * <h2>A evidência que resolve, e por que ela é melhor</h2>
 * Um nome de personagem aparece capitalizado <b>no MEIO</b> de alguma fala, mais cedo ou mais
 * tarde — {@code "o Gundam está aqui"}. Uma palavra quebrada, nunca. Então a pergunta se responde
 * uma vez, contra o corpus todo, e não fala a fala.
 *
 * <p>Medido: no 0080, isso levou a lista de palavras suspeitas de 45 para 13, <b>sem esconder</b>
 * {@code Impeda}, que é defeito de verdade e abre a fala.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Quem decide "capitalizada fora do início de frase" é a PRODUÇÃO
 *       ({@link CorretorAcentoDeDicionarioNaFalaService#nomesPropriosNoMeioDaFala}). Aqui só se
 *       acumula o que ela responde, fala após fala.</li>
 *   <li>A resposta só vale depois de o corpus INTEIRO ter passado — perguntar no meio da
 *       varredura devolve um conjunto incompleto, e a diferença aparece como defeito inventado.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Texto nulo não contribui nada; nunca lança.
 */
public final class NomesConfirmadosPeloCorpus {

    private final Set<String> confirmados = new LinkedHashSet<>();

    /**
     * PROPÓSITO DE NEGÓCIO: acumula o que ESTA fala prova sobre nomes próprios.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo é ignorado.
     */
    public void observar(String texto) {
        if (texto == null) {
            return;
        }
        confirmados.addAll(
            CorretorAcentoDeDicionarioNaFalaService.nomesPropriosNoMeioDaFala(texto));
    }

    /**
     * Diz se o corpus já provou que esta palavra é nome próprio.
     *
     * <p><b>Só vale depois de o corpus inteiro ter passado por {@link #observar(String)}.</b>
     */
    public boolean eNomeProprio(String palavra) {
        return confirmados.contains(palavra);
    }

    /** Quantos nomes o corpus confirmou — para o relatório dizer sobre o que ele fala. */
    public int quantidade() {
        return confirmados.size();
    }
}
