package org.traducao.projeto.traducaoKaraoke.domain;

import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorAcentoPorDicionario;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: repõe acento na LETRA DE MÚSICA traduzida — a camada em português que
 * convive, no mesmo arquivo, com a camada em romaji. É a rede determinística do karaokê, irmã da
 * que a tradução de diálogo já tem, mas com curadoria PRÓPRIA.
 *
 * <h2>Por que uma lista própria, e não a de {@code qualidadeTraducao.NormalizadorAcentosComuns}</h2>
 * <b>DUPLICAÇÃO DECLARADA, e a medição é o motivo.</b> Aquela lista tem 162 entradas e resolve o
 * problema DELA: diálogo em português puro. Das 162, <b>quatro também são romaji válido</b> —
 * medido em 2026-08-14 contra o dicionário {@code ja_ROMAJI} do próprio projeto (129.745 formas):
 *
 * <pre>
 *   ate    mae    nao    sao
 * </pre>
 *
 * No diálogo essas quatro nunca fizeram mal, porque ali não existe camada japonesa. Reusar a lista
 * inteira aqui arrastaria o dano conhecido para dentro do karaokê — {@code mae} é 前, e virou
 * {@code mãe} <b>100 vezes</b> nos 50 episódios do Unicorn. Camada de desacoplamento resolve o
 * problema da sua fatia e não o empurra para as outras; foi a regra que Paulo enunciou em
 * 2026-08-14, e este arquivo é a aplicação dela.
 *
 * <p>Por isso a lista aqui NÃO é herdada nem copiada: é montada do que foi <b>medido na saída do
 * karaokê</b>. Na tradução do 86 de 2026-08-14, a camada portuguesa saiu com {@code nao} sem acento
 * em <b>5 falas distintas</b>, presentes em 918 linhas do arquivo (o gradiente de {@code \clip}
 * repete a mesma fala centenas de vezes). {@code esta} e {@code sera} apareceram e ficaram DE FORA
 * pela invariante de sempre — são palavras válidas e dependem de sentido.
 *
 * <p>As outras três colisões ({@code mae}, {@code ate}, {@code sao}) ficam fora por MEDIÇÃO, não por
 * precaução: nenhuma teve ocorrência a corrigir no karaokê. Entram no dia em que forem medidas — e
 * aí com caso-controle de romaji junto.
 *
 * <h2>A mecânica não é duplicada</h2>
 * A substituição em si — fronteira de termo que trata o {@code \N} do ASS, preservação de caixa,
 * não entrar em {@code {tag}} — é de {@code core.texto.dicionarioOrtografia}, já testada. Esta
 * classe é dona apenas da DECISÃO (quais formas), que é o que tem versão por fatia. A mecânica de
 * FORMATO não tem, e por isso continua num lugar só.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só entra forma cuja grafia sem acento NUNCA é palavra portuguesa válida.</li>
 *   <li>Roda exclusivamente sobre o texto JÁ TRADUZIDO, antes de a linha original ser anexada
 *       ({@code TraduzirKaraokeUseCase}). O romaji é inalcançável por construção, não por sorte —
 *       {@code CorretorNaoAlcancaRomajiDoKaraokeTest} congela essa ordem.</li>
 *   <li>Substituição preserva a caixa: {@code Nao} vira {@code Não}, {@code NAO} vira {@code NÃO}.</li>
 * </ul>
 *
 * <h2>Risco residual DECLARADO</h2>
 * {@code Nao} é nome próprio japonês. Numa obra que tenha uma personagem Nao, esta lista a
 * renomearia. É o mesmo caso de {@code irma → irmã}, que a lista do diálogo excluiu por "Irma" ser
 * personagem no 86, apontando o dono certo: o enforçador de termos de lore, que conhece a obra.
 * Não há Nao no acervo atual; se entrar, a proteção é de lá, não daqui.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Texto nulo ou em branco volta como veio. Nunca lança.
 */
public final class AcentosLetraKaraoke {

    /**
     * Forma-sem-acento -> forma acentuada, medida na camada portuguesa do karaokê.
     *
     * <p>Deliberadamente MÍNIMA. Cada entrada nova exige a ocorrência medida que a justifica e,
     * quando colidir com romaji, o caso-controle correspondente.
     */
    private static final Map<String, String> CORRECOES = Map.of("nao", "não");

    /**
     * Sufixos de tratamento japoneses. Uma forma da lista SEGUIDA de um deles é NOME, não palavra.
     *
     * <h2>Nasceu de caso-controle que reprovou, em 2026-08-14</h2>
     * O teste afirmava que {@code Nao-chan} passava intacto e <b>caiu</b>: a fronteira de termo do
     * ASS trata o hífen como fim de palavra, então a correção alcançava o nome e devolvia
     * {@code Não-chan}. É o mesmo dano que fez {@code irma → irmã} ser excluída da lista do
     * diálogo, porque "Irma" é personagem no 86 — só que aqui, numa camada que convive com romaji,
     * a chance de o nome aparecer é muito maior.
     *
     * <p>A saída NÃO foi remover {@code nao} da lista (as 918 linhas do 86 continuariam erradas)
     * nem afrouxar a asserção (isso seria fazer a guarda concordar com o defeito): foi proteger o
     * caso específico, que é o que a fatia pode fazer sem mexer em ninguém.
     */
    private static final java.util.Set<String> HONORIFICOS = java.util.Set.of(
        "chan", "kun", "san", "sama", "senpai", "sempai", "sensei", "dono", "tan");

    /** Marca temporária para tirar o nome do caminho da substituição; nunca sobra no resultado. */
    private static final String SENTINELA = "\u0001";

    private static final Pattern ANTES_DE_HONORIFICO = Pattern.compile(
        "(?i)(" + String.join("|", CORRECOES.keySet()) + ")(?=-(?:"
            + String.join("|", HONORIFICOS) + ")\\b)");

    private AcentosLetraKaraoke() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve a letra traduzida com os acentos que o modelo não pôs.
     *
     * <p>INVARIANTES DO DOMÍNIO: altera apenas as formas da lista; tags, quebras {@code \N} e o
     * resto do texto voltam byte a byte. Delega a substituição a
     * {@link CorretorAcentoPorDicionario#aplicar}, dono único dessa mecânica.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve o texto recebido.
     */
    public static String repor(String texto) {
        if (texto == null || texto.isBlank()) {
            return texto;
        }
        // Tira os nomes do caminho ANTES da substituição — mesmo recurso que o projeto já usa para
        // a quebra \N: sentinela primeiro, regex de palavra depois, restauração no fim.
        Map<String, String> protegidos = new java.util.LinkedHashMap<>();
        Matcher m = ANTES_DE_HONORIFICO.matcher(texto);
        StringBuilder mascarado = new StringBuilder();
        while (m.find()) {
            String marca = SENTINELA + protegidos.size() + SENTINELA;
            protegidos.put(marca, m.group(1));
            m.appendReplacement(mascarado, Matcher.quoteReplacement(marca));
        }
        m.appendTail(mascarado);

        String corrigido = CorretorAcentoPorDicionario.aplicar(mascarado.toString(), CORRECOES);
        for (Map.Entry<String, String> e : protegidos.entrySet()) {
            corrigido = corrigido.replace(e.getKey(), e.getValue());
        }
        return corrigido;
    }
}
