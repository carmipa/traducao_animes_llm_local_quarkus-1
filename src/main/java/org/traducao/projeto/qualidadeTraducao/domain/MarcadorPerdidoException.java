package org.traducao.projeto.qualidadeTraducao.domain;

/**
 * PROPÓSITO DE NEGÓCIO: distinguir <b>"o modelo inventou conteúdo"</b> de <b>"o modelo traduziu
 * certo mas não repetiu o marcador de controle"</b>. São causas diferentes, com correções
 * diferentes, e até 08/08/2026 as duas saíam com a mesma palavra no console: <i>alucinação</i>.
 *
 * <h2>O prejuízo que originou</h2>
 * Paulo, olhando o console do corretor de karaokê: <i>"vi escrito alucinação várias vezes"</i>. No
 * manifesto daquela execução do 08th MS Team eram <b>1.258 avisos de "Alucinação detectada"</b> —
 * e NENHUM era alucinação. O que o modelo tinha devolvido:
 * <pre>
 *   "Você vê o sonho brilhando dentro da tempestade."
 *   "Aguenta firme agora! Não solta isso."
 *   "O sonho que brilha dentro do seu coração!"
 * </pre>
 * Traduções corretas do encerramento, descartadas por falta de um {@code [[TAG0]]} — e anunciadas
 * como delírio do modelo. O custo não é estético: enquanto a mensagem disser "alucinação", ninguém
 * procura a causa certa. Eu mesmo li esse log em 07/08 e não fui até o fim da frase.
 *
 * <p>Alucinação DE VERDADE, para efeito de comparação, é o que aconteceu no Guilty Crown: a linha
 * {@code Kizuite} (uma palavra romaji) virou <i>"Crow é o nome de palco"</i> — conteúdo que não
 * existe no original, puxado da lore da obra. Aquilo destrói a legenda; isto aqui só perde
 * formatação.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>É subclasse de {@link AlucinacaoDetectadaException} DE PROPÓSITO: todo
 *       {@code catch (AlucinacaoDetectadaException)} que já existia continua tratando este caso
 *       exatamente como antes. A mudança é de VOCABULÁRIO e de diagnóstico, não de fluxo — o
 *       texto segue sendo recusado, porque publicar sem a tag quebraria o layout.</li>
 *   <li>Carrega o texto que o modelo devolveu ({@link #traducaoRecusada()}), para que o relato
 *       possa mostrar que a tradução existia. Sem isso, o operador não tem como saber se perdeu
 *       lixo ou trabalho bom.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Propaga como {@code RuntimeException}; tradução nula é aceita e vira string vazia.
 */
public class MarcadorPerdidoException extends AlucinacaoDetectadaException {

    private final transient String traducaoRecusada;

    /**
     * PROPÓSITO DE NEGÓCIO: cria a falha nomeando o que de fato ocorreu — marcador ausente — e
     * guardando a tradução que foi descartada por causa disso.
     *
     * <p>INVARIANTES DO DOMÍNIO: a mensagem NÃO usa a palavra "alucinação", para não voltar a
     * mascarar a causa; quem a lê descobre em uma linha o que procurar.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entradas nulas são aceitas; nunca lança do construtor.
     */
    public MarcadorPerdidoException(int esperados, String traducaoRecusada) {
        super("Marcador de formatacao [[TAGn]] ausente na resposta do LLM (esperado(s) "
            + esperados + "). A TRADUCAO pode estar correta e mesmo assim e recusada, porque "
            + "publicar sem a tag quebraria o layout da legenda. Recebido: "
            + (traducaoRecusada == null ? "" : traducaoRecusada));
        this.traducaoRecusada = traducaoRecusada == null ? "" : traducaoRecusada;
    }

    /** O texto que o modelo devolveu e foi descartado — normalmente uma tradução aproveitável. */
    public String traducaoRecusada() {
        return traducaoRecusada;
    }
}
