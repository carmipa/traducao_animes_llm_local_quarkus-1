package org.traducao.projeto.core.texto.gramatica;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: a porta por onde o KRONOS pergunta "isto está gramaticalmente certo em
 * português do Brasil?" — a única pergunta que nenhum instrumento do projeto sabia responder.
 *
 * <h2>O buraco que esta porta fecha, medido</h2>
 * O hunspell responde "esta palavra existe?". Para {@code noticia}, {@code orbita}, {@code premio}
 * e {@code milicia} a resposta é SIM, porque são conjugações legítimas de {@code noticiar},
 * {@code orbitar}, {@code premiar} e {@code miliciar}. Quando o modelo escreve <i>"a milicia
 * ordenou um blackout de noticias"</i>, o dicionário aprova cada palavra — e a frase está errada.
 * Paulo nomeou a classe em 23/08/2026: <i>"muitas vezes ficou invertido o verbo e substantivo"</i>.
 *
 * <p>Responder isso exige saber a CLASSE GRAMATICAL da palavra no contexto, não a existência dela.
 * É POS tagging, e é o que existe do outro lado desta porta.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li><b>Só ACUSA, nunca aplica.</b> A porta devolve achados; quem decide se viram texto é a
 *       fatia que chamou, depois dos portões dela.</li>
 *   <li>Recebe TEXTO VISÍVEL — sem tag {@code {...}} de override e sem a quebra {@code \\N} do
 *       ASS. Entregar marcação a um revisor de gramática mede o ruído da marcação.</li>
 *   <li><b>Falha FECHADA.</b> Revisor indisponível devolve lista vazia, e {@link #disponivel()}
 *       responde {@code false} — as duas coisas juntas, para que "não achei nada" e "não tinha
 *       como olhar" nunca produzam o mesmo sinal. É a invariante 12 do projeto.</li>
 * </ul>
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança. Texto nulo ou em branco devolve lista vazia.
 */
public interface RevisorGramaticalPort {

    /**
     * PROPÓSITO DE NEGÓCIO: revisa uma fala e devolve o que há de errado nela.
     *
     * <p>INVARIANTES DO DOMÍNIO: só devolve achados das categorias que a configuração deixou
     * ligadas; nunca altera o texto recebido.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve lista vazia — nunca lança, e nunca inventa.
     *
     * @param texto a fala JÁ VISÍVEL, sem tags e sem a quebra do ASS
     */
    List<AchadoGramatical> revisar(String texto);

    /**
     * PROPÓSITO DE NEGÓCIO: separa "revisei e está limpo" de "não tive como revisar".
     *
     * <p>INVARIANTES DO DOMÍNIO: só devolve {@code true} depois que o motor carregou de verdade.
     * Quem consome é obrigado a distinguir os dois casos no relatório — zero achados com o
     * revisor fora do ar é <b>NÃO VERIFICADO</b>, e não aprovação.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code false}; nunca lança.
     */
    boolean disponivel();

    /**
     * PROPÓSITO DE NEGÓCIO: o motivo, em português, de o revisor estar indisponível — para o
     * operador ler na tela em vez de olhar um zero sem explicação.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@code null} quando está tudo bem.
     */
    String motivoDaIndisponibilidade();
}
