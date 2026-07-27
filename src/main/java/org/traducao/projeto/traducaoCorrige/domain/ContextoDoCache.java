package org.traducao.projeto.traducaoCorrige.domain;

import org.traducao.projeto.contexto.domain.SnapshotContexto;

/**
 * PROPÓSITO DE NEGÓCIO: o contexto resolvido para um arquivo de cache, junto com QUANTO ele pode
 * ser confiado. Guardar as duas coisas separadas é o que permite uma operação decidir se o grau de
 * certeza é suficiente para o estrago que ela é capaz de fazer.
 *
 * <h2>Por que "verificável" é uma pergunta em si</h2>
 * A identidade da obra de um cache pode vir de duas testemunhas independentes: o CARIMBO gravado
 * na tradução ({@code proveniencia.contextoId}) e a PASTA em que o arquivo mora. Isso dá três
 * situações reais, medidas no acervo:
 * <ul>
 *   <li>as duas concordam — caso normal, verificado dos dois lados;</li>
 *   <li>só o carimbo existe (pasta que nenhuma lore reconhece) — ainda é um FATO registrado no
 *       momento da tradução, fraco mas real;</li>
 *   <li><b>nenhuma das duas</b> — cache legado, sem carimbo, em pasta irreconhecível, com a obra
 *       escolhida à mão na tela. Aqui não há testemunha alguma: a lore é um palpite.</li>
 * </ul>
 * O terceiro caso é o de uma obra que o catálogo ainda não reivindica pelo nome da pasta guardando
 * cache anterior à proveniência. A guarda obra×contexto deixa passar, corretamente, porque falhar
 * fechado ali pararia obras que ainda não declararam vocabulário de pasta. Mas "deixar passar"
 * não pode significar "é seguro escrever": quem reescreve texto já pronto tem de olhar este campo.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Descreve, não decide. O que fazer com uma identidade não verificável é da operação: a
 *       limpeza de entradas defeituosas quase não depende de lore, o reforço de terminologia
 *       depende inteiramente dela.</li>
 *   <li>{@code contexto} nunca é nulo: se a resolução falhasse, a guarda já teria lançado.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Record imutável, sem I/O e sem lançar.
 *
 * @param contexto fotografia congelada da lore resolvida para este arquivo
 * @param veioDaProveniencia {@code true} quando o contexto saiu do carimbo do próprio cache, e não
 *        da seleção da tela
 * @param obraReconhecida {@code true} quando alguma lore do catálogo reconhece a pasta do arquivo
 */
public record ContextoDoCache(
    SnapshotContexto contexto,
    boolean veioDaProveniencia,
    boolean obraReconhecida
) {

    /**
     * PROPÓSITO DE NEGÓCIO: responde se ALGUMA testemunha sustenta a lore deste arquivo.
     *
     * <p>INVARIANTES DO DOMÍNIO: basta UMA. Exigir as duas bloquearia todo cache de obra sem
     * vocabulário de pasta declarado, e todo cache legado de obra reconhecida — nos dois casos
     * existe evidência suficiente. É a ausência das DUAS que caracteriza o palpite.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: função pura; nunca lança.
     */
    public boolean identidadeVerificavel() {
        return veioDaProveniencia || obraReconhecida;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: atalho para o id da lore, que é o que a auditoria e os logs registram.
     * <p>INVARIANTES DO DOMÍNIO: mesmo id do snapshot; nunca deriva um segundo.
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança.
     */
    public String id() {
        return contexto.id();
    }
}
