package org.traducao.projeto.qualidadeTraducao.application;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: tira o itálico da legenda, de vez. Decisão do Paulo em 2026-08-22:
 * <i>"num filme normal não tem itálico, é frescura"</i>. Deixa de ser formatação a preservar e
 * passa a ser ruído a remover, antes de a fala chegar ao tradutor.
 *
 * <h2>Por que remover ANTES de traduzir resolve um defeito, e não só estética</h2>
 * O pipeline mascara cada {@code {...}} como {@code [[TAGn]]} e devolve por índice. Isso
 * garante que a tag VOLTE, não que ela continue envolvendo a palavra que realçava — e o
 * português reordena as palavras:
 * <pre>
 * vai   : there's no [[TAG0]]love[[TAG1]] in this kind of killing.
 * volta : não há amor [[TAG0]][[TAG1]] neste tipo de assassinato.
 * </pre>
 * Os dois marcadores voltaram, na ordem, sem duplicar — o desmascarador aceita e está certo em
 * aceitar. O modelo não perdeu a tag: moveu a palavra. Sem o par, o problema não existe.
 *
 * <p>Medido no acervo em 22/08/2026: das 76.699 falas de diálogo, <b>6.179 (8,1%) têm
 * itálico</b> — 5.725 de narração (abre a fala) e 441 de ênfase (par no meio da frase).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Remove o TOKEN {@code \i}, nunca o bloco. Dos 8.309 blocos com itálico no acervo,
 *       <b>340 vêm misturados</b> com outra coisa ({@code {\q2\i1}},
 *       {@code {\fade(...)\an7\i1\pos(...)}}); apagar o bloco destruiria quebra automática,
 *       posição e cor.</li>
 *   <li>Bloco que fica VAZIO depois da remoção é descartado — {@code {}} não é ASS válido e
 *       vira lixo na tela.</li>
 *   <li>Não toca em mais nada: negrito, sublinhado, cor, posição e quebra seguem intactos.</li>
 *   <li>Fala cujo PRIMEIRO token de itálico é {@code \i0} fica INTACTA: o itálico dela vem
 *       do {@code Style:}, e remover o desliga acenderia o itálico. 48 falas no acervo.</li>
 *   <li>Idempotente e sem estado; só JDK + Spring.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Texto {@code null} devolve {@code null}; texto sem itálico é devolvido intacto. Nunca lança.
 */
@Component
public class RemovedorItalico {

    /**
     * O token de itálico do ASS: {@code \i1}, {@code \i0} ou {@code \i} solto. O lookahead
     * impede casar o começo de outro override que comece com {@code i} — hoje não há, e a
     * guarda é barata.
     */
    private static final Pattern TOKEN_ITALICO =
        Pattern.compile("\\\\i[01]?(?![0-9A-Za-z])");

    /** Um bloco de override do ASS. */
    private static final Pattern BLOCO = Pattern.compile("\\{[^}]*\\}");

    /**
     * PROPÓSITO DE NEGÓCIO: devolve a fala sem nenhum itálico, preservando todo o resto.
     *
     * <p>INVARIANTES DO DOMÍNIO: opera bloco a bloco; bloco que sobra vazio é removido; texto
     * visível não é tocado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code null} entra, {@code null} sai.
     *
     * @param texto a fala como está na legenda
     */
    public String remover(String texto) {
        if (texto == null || texto.indexOf('{') < 0) {
            return texto;
        }
        if (dependeDoItalicoDoEstilo(texto)) {
            return texto;
        }
        Matcher m = BLOCO.matcher(texto);
        StringBuilder saida = new StringBuilder();
        int fim = 0;
        while (m.find()) {
            saida.append(texto, fim, m.start());
            String bloco = m.group();
            String limpo = TOKEN_ITALICO.matcher(bloco).replaceAll("");
            if (!"{}".equals(limpo)) {
                saida.append(limpo);
            }
            fim = m.end();
        }
        saida.append(texto.substring(fim));
        return saida.toString();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: responde se a fala tem itálico, para quem precisa CONTAR sem
     * alterar — a telemetria e os harness de medição.
     *
     * <p>Existe para o padrão ter DONO ÚNICO. A primeira versão da telemetria carregava a
     * própria cópia do regex, e a catraca de regra duplicada entre fatias pegou: cópia não
     * declarada foi a causa de três defeitos em 03/08/2026, um deles vivo por nove dias.
     * Contador que diverge do removedor é pior que contador nenhum, porque mente com número.
     *
     * <p>INVARIANTES DO DOMÍNIO: pergunta ao MESMO padrão que o {@link #remover} usa. Enxerga
     * o token onde ele estiver, inclusive na fala de que o removedor SE ABSTÉM — é justamente
     * essa combinação (tem itálico E não mudou) que identifica a abstenção.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code null} devolve {@code false}; nunca lança.
     */
    public boolean temItalico(String texto) {
        return texto != null && TOKEN_ITALICO.matcher(texto).find();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reconhece a fala cujo itálico vem do {@code Style:} do cabeçalho e
     * não da tag — ali um {@code \i0} é o que MANTÉM o texto reto, e tirá-lo LIGARIA o
     * itálico. O oposto exato do que a regra existe para fazer.
     *
     * <p>O sinal está no próprio texto e não exige ler o cabeçalho: se o PRIMEIRO token de
     * itálico da fala é um {@code \i0}, não há {@code \i1} anterior para ele fechar, logo
     * ele fecha algo que veio de fora. Falha fechada: na dúvida a fala fica intacta.
     *
     * <p>Medido no acervo em 22/08/2026: das 9.696 falas com token de itálico, <b>48 (0,5%)</b>
     * estão nesta forma — 29 {@code Dialogue}, 11 {@code EG}, 8 {@code Default}. Contraprova pelo
     * cabeçalho: só 10 falas do acervo têm {@code \i0} sob um {@code Style:} de fato itálico,
     * todas de {@code "Other songs"} — música, que a regra já não toca.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança; texto sem token devolve {@code false}.
     */
    private boolean dependeDoItalicoDoEstilo(String texto) {
        Matcher blocos = BLOCO.matcher(texto);
        while (blocos.find()) {
            Matcher tokens = TOKEN_ITALICO.matcher(blocos.group());
            if (tokens.find()) {
                return tokens.group().endsWith("0");
            }
        }
        return false;
    }
}
