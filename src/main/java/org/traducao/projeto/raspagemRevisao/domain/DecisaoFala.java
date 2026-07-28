package org.traducao.projeto.raspagemRevisao.domain;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: o desfecho de UMA fala na revisão. Só existem três, e o tipo é a prova
 * disso: a fala é mantida como está, fica pendente de revisão humana, ou é substituída por um texto
 * novo. Tudo o mais — de onde veio a proposta, quem a vetou, quantas chamadas custou — é histórico
 * que não muda o que acontece com a linha na legenda.
 *
 * <h2>Não existe "PARAR O ARQUIVO"</h2>
 * A tentação é acrescentar um quarto desfecho para abortar o arquivo no meio. Ele seria realizado
 * como {@code break} no laço de falas, e aí as falas seguintes NÃO entrariam no documento gravado:
 * o {@code .ass} sairia truncado, com tempo e estilo preservados e o resto do episódio faltando.
 * A parada existe — é cooperativa, mora um nível acima, no laço de ARQUIVOS — e dentro do arquivo
 * ela DRENA: as falas restantes entram sem alteração. Se um dia alguém precisar interromper a partir
 * de uma fala, o caminho é sinalizar o laço de arquivos, nunca um desfecho aqui.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Os avisos ao operador viajam JUNTO da decisão, na ordem em que devem sair. Quem decide não
 *       imprime — se imprimisse, a ordem das mensagens viraria efeito colateral de quem avalia, e
 *       o console é a única janela que o operador tem para o que está sendo feito com a legenda.</li>
 *   <li>{@code Corrigir} carrega o texto FINAL, já aprovado pelo portão de segurança. Nada rio
 *       abaixo re-valida: quem constrói um {@code Corrigir} está afirmando que ele pode ser
 *       gravado.</li>
 *   <li>Tipo selado e sem {@code default} nos {@code switch} do laço: um quarto desfecho vira erro
 *       de compilação em todos os pontos que precisam tratá-lo, não um caso silenciosamente
 *       ignorado.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Não faz I/O e não lança. Listas de avisos são copiadas para imutáveis na construção.
 */
public sealed interface DecisaoFala {

    /** Mensagens a imprimir antes de aplicar a decisão, na ordem; vazia quando não há o que narrar. */
    List<String> avisosAoOperador();

    /**
     * A fala entra no documento como está.
     *
     * @param avisosAoOperador narração da decisão; normalmente vazia
     */
    record Manter(List<String> avisosAoOperador) implements DecisaoFala {

        /** PROPÓSITO DE NEGÓCIO: blinda a lista contra mutação depois da decisão tomada. */
        public Manter {
            avisosAoOperador = List.copyOf(avisosAoOperador);
        }
    }

    /**
     * A fala tem problema conhecido e NENHUMA correção confiável: entra como está e é contada como
     * pendente, para aparecer no relatório e voltar como trabalho humano.
     *
     * @param avisosAoOperador por que nenhuma proposta serviu
     */
    record Pendente(List<String> avisosAoOperador) implements DecisaoFala {

        /** PROPÓSITO DE NEGÓCIO: blinda a lista contra mutação depois da decisão tomada. */
        public Pendente {
            avisosAoOperador = List.copyOf(avisosAoOperador);
        }
    }

    /**
     * A fala é substituída pelo texto aprovado.
     *
     * @param texto o texto final, já validado pelo portão de segurança
     * @param avisosAoOperador narração do antes/depois
     */
    record Corrigir(String texto, List<String> avisosAoOperador) implements DecisaoFala {

        /** PROPÓSITO DE NEGÓCIO: blinda a lista contra mutação depois da decisão tomada. */
        public Corrigir {
            avisosAoOperador = List.copyOf(avisosAoOperador);
        }
    }
}
