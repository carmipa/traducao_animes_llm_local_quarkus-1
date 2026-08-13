package org.traducao.projeto.core.texto.dicionarioOrtografia;

import java.util.Collection;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: diz quais palavras NÃO existem no idioma — o julgamento que uma regra de
 * terminação não alcança e que um LLM erra por construção. É a porta para um verificador
 * ortográfico de verdade, com centenas de milhares de formas, em vez de um dicionário escrito à
 * mão palavra a palavra.
 *
 * <h2>O prejuízo que originou</h2>
 * O {@code NormalizadorAcentosComuns} corrige por lista, e a lista é montada com o que foi MEDIDO
 * em cada obra. Em 13/08/2026, sobre o Zeta recém-traduzido pela aya, a lista resolvia <b>zero</b>
 * das 119 falas com acento faltando: o vocabulário de uma obra nova não é o da anterior. A regra de
 * terminação {@code -ção} fechou 105; as ~200 formas restantes medidas no acervo
 * ({@code colonia} 53, {@code sera} 32, {@code area} 14, {@code opiniao} 10, {@code assembléia} —
 * grafia pré-acordo) só um dicionário completo alcança.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Pergunta em LOTE, nunca palavra a palavra: a implementação paga um custo fixo de processo
 *       por chamada, e uma chamada por fala tornaria o episódio inviável.</li>
 *   <li>Responde só o que NÃO conhece. Silêncio sobre uma palavra significa "esta existe", nunca
 *       "não consegui verificar" — para isso existe {@link #disponivel()}.</li>
 *   <li>NÃO corrige e NÃO sugere: diz apenas o que é desconhecido. Quem decide o que fazer com
 *       isso é a camada que conhece a lore, e essa ordem não é negociável — {@code Gundam},
 *       {@code Argama} e {@code Reccoa} somam 797 ocorrências só no Zeta e são desconhecidas de
 *       qualquer dicionário de português.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Verificador ausente ou quebrado NÃO reprova nada e devolve {@code false} em
 * {@link #disponivel()} — os dois sinais juntos, porque "nenhuma palavra desconhecida" e "não
 * consegui olhar" não podem ser o mesmo fato (regra 12). Nunca lança.
 */
public interface DicionarioOrtograficoPort {

    /**
     * PROPÓSITO DE NEGÓCIO: das palavras recebidas, quais o dicionário do idioma não reconhece.
     *
     * <p>INVARIANTES DO DOMÍNIO: o conjunto devolvido é subconjunto do recebido, com a grafia
     * exatamente como veio; entrada vazia devolve vazio sem consultar nada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve conjunto VAZIO — falha fechada, nada é acusado.
     *
     * @param palavras formas a verificar, já sem tags e sem pontuação
     * @return as que o dicionário não conhece
     */
    Set<String> desconhecidas(Collection<String> palavras);

    /**
     * PROPÓSITO DE NEGÓCIO: além de dizer o que não conhece, oferece as formas que o dicionário
     * julga próximas — é o que permite CORRIGIR e não apenas acusar.
     *
     * <p>INVARIANTES DO DOMÍNIO: só aparecem no mapa as palavras desconhecidas; a ordem das
     * sugestões é a do verificador, da mais provável para a menos. Quem consome decide o que
     * aceitar, e o critério seguro está em {@code CorretorAcentoPorDicionario}: só vale a
     * sugestão que, sem acentos, é a MESMA palavra — o que impede {@code colonia} de virar
     * {@code colonial}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa VAZIO — falha fechada, nenhuma correção proposta.
     *
     * @param palavras formas a verificar
     * @return desconhecida -> sugestões, na ordem do verificador
     */
    java.util.Map<String, Set<String>> sugestoes(Collection<String> palavras);

    /**
     * PROPÓSITO DE NEGÓCIO: se há um verificador utilizável agora.
     *
     * <p>INVARIANTES DO DOMÍNIO: é o que separa "o texto está limpo" de "não foi verificado" —
     * o estado 2 da guarda de três estados. Quem relata ao operador DEVE consultar isto antes de
     * afirmar que a ortografia foi conferida.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code false}; nunca lança.
     */
    boolean disponivel();

    /** Como o verificador se identifica no relatório — idioma e origem. Nunca nulo. */
    String descricao();
}



