package org.traducao.projeto.traducaoKaraoke.domain;

import java.util.Locale;

/**
 * PROPÓSITO DE NEGÓCIO: carrega as duas evidências de karaokê que NÃO cabem no par
 * (estilo, texto) — o campo {@code Effect} da linha do ASS e a existência de uma camada romaji
 * no mesmo instante do arquivo. Sem elas, a decisão "isto é música?" só tem o nome do estilo e a
 * densidade de tags, e a densidade de tags é assinatura de TIPOGRAFIA, não de música.
 *
 * <h2>O prejuízo que originou, medido em 2026-08-19</h2>
 * O classificador rodado sobre o acervo inteiro (726 arquivos) devolve <b>165.827</b> linhas
 * TRADUZIVEL_INGLES. Destas, <b>133.951 (80,8%) entram só pela assinatura de efeito</b>:
 * {@code Char's Counterattack} 106.692 — o estilo de DIÁLOGO do filme —, {@code Signs} 9.213,
 * {@code Zeta Episode Title} 6.923, {@code Main Title} 4.129, {@code Logo} 1.961. Se o módulo
 * rodasse ali, mandaria o diálogo do filme ao LLM com o prompt de karaokê e o empilharia como
 * {@code inglês\Nportuguês} na tela.
 *
 * <h2>Por que DUAS evidências, e não a melhor delas</h2>
 * Elas se cobrem em obras diferentes, não só em linhas diferentes — foi medido:
 * <ul>
 *   <li>o campo {@code Effect} está <b>vazio nas 159.398 linhas do 86</b> e também no Guilty
 *       Crown; o {@code fx} vem de DanMachi, Zeta e ZZ. Lá ele salva o {@code OPL2} (258 linhas
 *       de música real) que a evidência por instante perde;</li>
 *   <li>a evidência por instante salva outras 283 que o {@code Effect} perde.</li>
 * </ul>
 * Juntas salvam 541 linhas que qualquer uma sozinha descartaria.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@link #nenhum()} é o estado HONESTO de "não perguntei", e é o mais restritivo: sem
 *       evidência externa, a decisão fica só com estilo e tag {@code \k}. Nunca inventa sinal.</li>
 *   <li>O campo {@code Effect} é comparado em minúsculas e por conteúdo, porque o Kara Templater
 *       do Aegisub escreve tanto {@code fx} quanto {@code Effector [fx]}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Campo nulo ou em branco devolve {@code false} em {@link #efeitoDeclaraKaraoke()}. Nunca lança.
 *
 * @param campoEfeito            o 9º campo da linha {@code Dialogue:} do ASS, como veio
 * @param romajiNoMesmoInstante  existe camada {@code ORIGINAL_JAPONES} no mesmo início/fim
 */
public record SinaisDeKaraoke(String campoEfeito, boolean romajiNoMesmoInstante) {

    private static final SinaisDeKaraoke NENHUM = new SinaisDeKaraoke(null, false);

    /**
     * PROPÓSITO DE NEGÓCIO: o estado de quem não tem o arquivo em mãos — teste de unidade,
     * chamador antigo, consulta avulsa.
     *
     * <p>INVARIANTES DO DOMÍNIO: é o mais RESTRITIVO de propósito. "Não perguntei" não pode
     * virar "é música": seria a guarda aprovando por cegueira.
     */
    public static SinaisDeKaraoke nenhum() {
        return NENHUM;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o Kara Templater do Aegisub carimba as linhas que GERA. É a evidência
     * mais barata e mais confiável de que a linha é karaokê, e o classificador nunca a olhou.
     *
     * <p>INVARIANTES DO DOMÍNIO: medido no acervo — {@code fx} aparece em 1.412.289 linhas, das
     * quais 838.021 são romaji e 566.479 são fragmento de KFX; só 1.344 são letra traduzível.
     * Nos 133.951 falsos positivos ele aparece em <b>258</b>, e essas 258 são o {@code OPL2}, que
     * é música de verdade. Ou seja: onde o campo está preenchido, ele acerta.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nulo ou em branco devolve {@code false}.
     */
    public boolean efeitoDeclaraKaraoke() {
        if (campoEfeito == null || campoEfeito.isBlank()) {
            return false;
        }
        String x = campoEfeito.toLowerCase(Locale.ROOT);
        return x.equals("fx") || x.contains("[fx]") || x.contains("karaoke") || x.contains("template");
    }

    /** Alguma das duas evidências externas afirma que esta linha é karaokê. */
    public boolean algumaEvidencia() {
        return efeitoDeclaraKaraoke() || romajiNoMesmoInstante;
    }
}
