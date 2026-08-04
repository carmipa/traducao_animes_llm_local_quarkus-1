package org.traducao.projeto.novoKaraoke.domain;

/**
 * PROPÓSITO DE NEGÓCIO: o que a simplificação de karaokê fez com UM estilo de UM arquivo. É a
 * unidade que responde "onde exatamente o problema acontece" — a pergunta que os defeitos de
 * 2026-08-03 não conseguiram responder sem varredura manual de disco.
 *
 * <h2>Por que por ESTILO e não só por arquivo</h2>
 * O defeito que motivou este registro foi silencioso e localizado: a régua de nome musical usava
 * {@code \b}, e sublinhado é caractere de palavra, então {@code OP_S2} e {@code ED_S2} não eram
 * reconhecidos. No Guilty Crown isso fez <b>10 dos 23 episódios passarem com delta ZERO</b> — 392
 * eventos entrando, 392 saindo. Sem erro, sem aviso, sem contador. O sistema não fez nada e
 * reportou sucesso, e o defeito viveu nove dias.
 *
 * <p>Um total por arquivo não teria pego: o arquivo "processou 392 eventos" e isso parece certo.
 * O que denuncia é o RECORTE — estilo {@code OP_S2}, 17 eventos, <b>reconhecido como música:
 * não</b>. Esse é o número que grita no painel.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code reconhecidoComoMusica} falso com {@code eventos} alto é o sinal de régua estreita
 *       demais; falso com valor baixo é diálogo normal, o caso esperado.</li>
 *   <li>{@code removidos} e {@code preservadosPorSeguranca} sempre somam com as linhas geradas —
 *       evento que some sem entrar em nenhum dos dois é perda silenciosa.</li>
 *   <li>Registro puro, imutável, sem I/O e sem dependência de fatia nenhuma.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Não valida nem lança: é portador de dados. Quem constrói garante a coerência.
 *
 * @param estilo nome do estilo ASS, exatamente como está no arquivo
 * @param eventos quantos eventos daquele estilo o arquivo tinha
 * @param reconhecidoComoMusica se a régua de nome/tag classificou o estilo como musical
 * @param linhasGeradas linhas simples reconstruídas a partir dele
 * @param removidos eventos KFX descartados por já estarem cobertos por uma linha gerada
 * @param preservadosPorSeguranca eventos mantidos intactos por não caberem em nenhuma linha
 */
public record MedicaoEstiloKaraoke(
    String estilo,
    int eventos,
    boolean reconhecidoComoMusica,
    int linhasGeradas,
    int removidos,
    int preservadosPorSeguranca
) {

    /**
     * PROPÓSITO DE NEGÓCIO: o estilo tem cara de música mas foi recusado pela régua?
     *
     * <p>INVARIANTES DO DOMÍNIO: é heurística de ALERTA, não de decisão — serve para o painel
     * destacar o caso, nunca para mudar classificação. O limiar de 5 eventos separa uma placa
     * isolada de uma faixa musical inteira; abaixo disso, estilo não reconhecido é o normal.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: função pura; não lança.
     */
    public boolean suspeitoDeReguaEstreita() {
        return !reconhecidoComoMusica && eventos >= 5;
    }
}
