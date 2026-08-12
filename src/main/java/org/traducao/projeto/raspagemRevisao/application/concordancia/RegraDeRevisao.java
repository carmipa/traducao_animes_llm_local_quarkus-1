package org.traducao.projeto.raspagemRevisao.application.concordancia;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: contrato de UMA família de regras da revisão de legenda. Existe para
 * que o detector principal seja um LAÇO sobre uma lista, em vez de uma sequência de chamadas
 * que cresce a cada regra nova — que foi como o método de 124 linhas se formou.
 *
 * <h2>Por que interface e não classe-base</h2>
 * Herança aqui só serviria para reusar campo, e reuso por herança é acoplamento com outro
 * nome: a subclasse passa a depender do formato interno da mãe, e mudar a mãe quebra filhas
 * que ninguém lembrava que existiam. A interface dá o contrato sem dar o estado — cada regra
 * carrega os próprios padrões, e o vocabulário compartilhado vem de {@link LexicoGenero},
 * que é dado, não comportamento.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A implementação ACRESCENTA motivos ao conjunto recebido e não remove nenhum: a decisão
 *       de descartar suspeita pertence ao portão da revisão, não à regra que a levantou.</li>
 *   <li>Nunca reescreve o texto. Detectar e corrigir são etapas distintas, e misturá-las faria
 *       uma regra silenciosamente desfazer o achado de outra.</li>
 *   <li>Recebe o original mesmo quando não precisa dele — a concordância nominal, por exemplo,
 *       é interna ao português. Assinatura única é o que permite a lista.</li>
 *   <li>Sem estado entre chamadas: a mesma instância atende falas de episódios diferentes.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Ausência de evidência não acrescenta motivo — e isso NÃO significa "está correto", significa
 * "esta regra não viu nada". Nenhuma implementação deve lançar: uma fala estranha não pode
 * derrubar a auditoria das outras.
 */
public interface RegraDeRevisao {

    /**
     * @param original fala em inglês, já sem tags ASS; pode ser ignorado pelas regras internas
     *        ao português
     * @param texto tradução PT-BR, já sem tags ASS
     * @param motivos conjunto acumulador — a implementação só acrescenta
     */
    void detectar(String original, String texto, Set<String> motivos);
}
