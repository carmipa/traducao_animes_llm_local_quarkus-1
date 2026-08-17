package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Component;
import org.traducao.projeto.core.presentation.ui.AnsiCores;
import org.traducao.projeto.raspagemRevisao.domain.PoliticaRetraducao;

import java.util.ArrayList;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: montar as linhas que o operador LÊ quando a 3.1 acha uma fala suspeita,
 * de forma que o olho separe <b>referência</b> de <b>estado</b> sem precisar ler o motivo.
 *
 * <h2>O prejuízo MEDIDO que originou — 2026-08-17</h2>
 * A tela imprimia {@code EN:} e {@code PT:} <b>as duas em AMARELO</b>, com peso visual idêntico.
 * Numa corrida do Zeta, 8 dos 10 achados eram concordância — o português estava correto e o
 * inglês ali era só referência — e 2 eram fala realmente em inglês. Os dois formatos saíam
 * <b>indistinguíveis</b>:
 * <pre>
 * EN: Seeing Amuro's heroics, Beltorchika willingly opened her heart to him.
 * PT: Vendo as proezas de Amuro, Beltorchika abriu o coração para ele.   &lt;- CERTO
 *
 * EN: So, you're saying the Titans went to Side Four to prepare a colony drop?
 * PT: So, you're saying the Titans went to Side Four to prepare a colony drop?  &lt;- ERRADO
 * </pre>
 * Paulo passou o dia convencido de que a ferramenta não estava traduzindo, porque rolando a tela
 * o olho pega as linhas em inglês. <b>Ele leu certo o que a tela mostrou; a tela é que mostrava
 * errado.</b> Auditoria linha a linha depois: 10 de 10 achados batiam com o disco — o dado nunca
 * esteve errado, só a apresentação.
 *
 * <p>É falha de BOA-FÉ no sentido da regra 15: um operador honesto, lendo a interface como ela
 * foi feita, conclui errado sobre o próprio acervo. Achado de boa-fé vira mecanismo, não
 * comentário.
 *
 * <h2>Convenção de cor — padrão internacional, pedido do Paulo (17/08/2026)</h2>
 * <ul>
 *   <li><b>VERDE</b> — a fala está traduzida (é o texto português sob análise, e o
 *       {@code PT corrigido});</li>
 *   <li><b>AMARELO</b> — a fala NÃO está traduzida (o selo {@code [NÃO TRADUZIDA]} e a linha
 *       {@code na legenda} quando ela ainda é o inglês);</li>
 *   <li><b>VERMELHO</b> — erro, e só erro. Gastar vermelho no que não é erro tira dele o poder de
 *       parar o olho quando o erro aparecer de verdade.</li>
 * </ul>
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O inglês é sempre REFERÊNCIA e sai apagado ({@code DIM}) com rótulo que diz isso; o
 *       português é o ESTADO e fica em destaque. Peso visual segue o que o operador decide.</li>
 *   <li>"Ainda em inglês" NÃO é recalculado aqui: pergunta-se ao veredito que o auditor já
 *       emitiu ({@link PoliticaRetraducao#NAO_TRADUZIDA}). Uma segunda implementação do critério
 *       divergiria da primeira — é a cicatriz das regras duplicadas entre fatias.</li>
 *   <li>Um lugar só: a tela da revisão e a do reaproveitamento de cache usam este mesmo
 *       formatador, senão as duas divergem com o tempo.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Lista de motivos nula é tratada como vazia; nunca lança.
 */
@Component
public class ApresentacaoFalaSuspeita {

    /** Marca que o olho pega antes de ler qualquer motivo. */
    private static final String SELO_EM_INGLES = "  [NÃO TRADUZIDA]";

    /**
     * PROPÓSITO DE NEGÓCIO: a fala continua no idioma de origem — e isso é o defeito, não a
     * referência ao lado.
     * <p>INVARIANTES DO DOMÍNIO: consulta o veredito do auditor pela constante de domínio, sem
     * reimplementar a comparação.
     * <p>COMPORTAMENTO EM CASO DE FALHA: motivos nulos devolvem {@code false}.
     */
    public boolean aindaEmIngles(List<String> motivos) {
        return motivos != null
            && motivos.stream().anyMatch(m -> m != null && m.startsWith(PoliticaRetraducao.NAO_TRADUZIDA));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: as linhas do achado, prontas para o console.
     *
     * <p>INVARIANTES DO DOMÍNIO: o cabeçalho leva o selo quando a fala está em inglês; a linha do
     * inglês é sempre {@code DIM} e rotulada como referência; a do português muda de cor conforme
     * seja o defeito ou apenas o texto sob análise.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: textos nulos viram vazio; nunca lança.
     *
     * @param indice índice do evento na legenda
     * @param estilo estilo do evento
     * @param originalEn o inglês de referência
     * @param traducaoAtual a fala como está na legenda
     * @param motivos os motivos que o auditor emitiu
     */
    public List<String> linhas(int indice, String estilo, String originalEn,
            String traducaoAtual, List<String> motivos) {
        boolean emIngles = aindaEmIngles(motivos);
        List<String> saida = new ArrayList<>();
        saida.add("  -> Linha " + indice + " [" + estilo + "]:"
            + (emIngles ? AnsiCores.YELLOW + SELO_EM_INGLES + AnsiCores.RESET : ""));
        saida.add("     " + AnsiCores.DIM + "referência EN: " + texto(originalEn) + AnsiCores.RESET);
        // Convenção de cor pedida pelo Paulo em 17/08/2026, e ela é a internacional:
        // VERDE traduzida · AMARELO não traduzida · VERMELHO erro. A versão anterior desta classe
        // usava vermelho para "não traduzida", e gastar o vermelho no que não é erro tira dele o
        // poder de parar o olho quando o erro aparecer de verdade.
        saida.add("     na legenda  : " + (emIngles ? AnsiCores.YELLOW : AnsiCores.GREEN)
            + texto(traducaoAtual) + AnsiCores.RESET);
        if (motivos != null) {
            for (String m : motivos) {
                saida.add("     " + AnsiCores.DIM + "• " + m + AnsiCores.RESET);
            }
        }
        return List.copyOf(saida);
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }
}
