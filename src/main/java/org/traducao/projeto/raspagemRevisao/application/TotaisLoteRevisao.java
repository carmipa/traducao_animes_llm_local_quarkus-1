package org.traducao.projeto.raspagemRevisao.application;

/**
 * PROPÓSITO DE NEGÓCIO: soma o que cada arquivo produziu para formar o resultado da varredura — os
 * números que aparecem no painel, no relatório e no retorno da operação.
 *
 * <h2>Por que existe</h2>
 * Antes, sete {@code int[]} de um elemento eram criados aqui e passados como parâmetro de saída
 * para o processamento de cada arquivo, que os incrementava em treze lugares. Era o contorno estilo
 * C para simular retorno múltiplo em Java, e tinha um custo real: bastava um {@code return} novo no
 * meio do processamento para a contagem daquele arquivo sumir sem erro nenhum.
 *
 * <p>Agora cada arquivo DEVOLVE sua {@link SessaoRevisaoArquivo} e este acumulador a soma. O
 * compilador cobra um valor em cada saída, e o que já foi contado vai junto por construção.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só soma. Nunca recalcula, nunca deriva um número de outro — relatório e retorno leem daqui,
 *       e é isso que os impede de discordar.</li>
 *   <li>Um arquivo bloqueado também é somado: ele foi analisado, e omiti-lo faria a varredura
 *       relatar menos do que viu.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Não lança. Sessão nula é ignorada — um arquivo que nem chegou a ser processado não altera totais.
 */
public class TotaisLoteRevisao {

    private int arquivos;
    private int corrigidas;
    private int problemas;
    private int auditadas;
    private int semOriginal;
    private int pendentes;
    private int semReferenciaSegura;
    private int italicoRemovido;

    /**
     * Quantos arquivos saíram CEGOS — tinham fala para auditar e nenhuma foi comparada.
     *
     * <p>O aviso já existia por arquivo (amarelo, "Nenhuma fala auditada"), mas morria ali: o
     * total do lote não o carregava e o desfecho saía {@code CONCLUIDO} verde. Medido em
     * 17/08/2026 com a pasta de inglês apontada para o lugar errado: 4 falas não olhadas,
     * {@code [SUCESSO]}, indistinguível de uma obra limpa.
     */
    private int arquivosCegos;

    /**
     * PROPÓSITO DE NEGÓCIO: incorpora o resultado de um arquivo ao total da varredura.
     * <p>INVARIANTES DO DOMÍNIO: soma campo a campo, sem interpretar nenhum.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sessão nula não altera nada.
     */
    public void somar(SessaoRevisaoArquivo sessao) {
        if (sessao == null) {
            return;
        }
        arquivos += sessao.arquivos();
        corrigidas += sessao.corrigidas();
        problemas += sessao.problemas();
        auditadas += sessao.auditadas();
        semOriginal += sessao.semOriginal();
        pendentes += sessao.pendentes();
        semReferenciaSegura += sessao.semReferenciaSegura();
        italicoRemovido += sessao.italicoRemovido();
        // A única interpretação desta soma, e ela é DELEGADA: quem define cegueira é a sessão.
        if (sessao.ficouCego()) {
            arquivosCegos++;
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: quantos arquivos do lote saíram sem enxergar nada.
     * <p>INVARIANTES DO DOMÍNIO: maior que zero significa que o desfecho do lote NÃO pode ser
     * apresentado como sucesso limpo — houve arquivo que ninguém comparou.
     * <p>COMPORTAMENTO EM CASO DE FALHA: contador simples; nunca lança.
     */
    public int arquivosCegos() {
        return arquivosCegos;
    }

    public int arquivos() {
        return arquivos;
    }

    public int corrigidas() {
        return corrigidas;
    }

    public int problemas() {
        return problemas;
    }

    public int auditadas() {
        return auditadas;
    }

    public int semOriginal() {
        return semOriginal;
    }

    public int pendentes() {
        return pendentes;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: quantas falas do LOTE tiveram o itálico removido pela regra de
     * 22/08/2026 — é o número que vai ao relatório e à telemetria da operação.
     * <p>INVARIANTES DO DOMÍNIO: consulta pura; a soma acontece em {@link #somar}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    public int italicoRemovido() {
        return italicoRemovido;
    }

    public int semReferenciaSegura() {
        return semReferenciaSegura;
    }
}
