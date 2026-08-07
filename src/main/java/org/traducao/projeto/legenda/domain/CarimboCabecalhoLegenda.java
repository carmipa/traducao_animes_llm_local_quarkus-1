package org.traducao.projeto.legenda.domain;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: escreve, no cabeçalho da própria legenda, o que o KRONOS fez com ela —
 * para que quem abrir o arquivo meses depois descubra pelo arquivo, e não por dedução, que houve
 * achatamento, descarte de camada musical ou preservação de estilo.
 *
 * <h2>O prejuízo que originou</h2>
 * Auditando o acervo em 07/08/2026, CINCO conclusões erradas saíram da mesma causa: eu recalculei
 * por fora o que o KRONOS já tinha decidido por dentro, e o arquivo não contava nada.
 * <ul>
 *   <li>18.431 falas classificadas como resíduo de tradução — eram letra de música cujo estilo o
 *       achatamento apagou;</li>
 *   <li>2.898 falas dadas como perdidas no Gundam Unicorn — eram as palavras soltas do karaokê
 *       {@code OPL2}, descartadas de propósito para não virarem 138 legendas piscando uma
 *       palavra por vez;</li>
 *   <li>364 "defeitos" de tradução no mesmo Unicorn — eram estilos que o
 *       {@code application.yml} manda ignorar.</li>
 * </ul>
 * Em nenhum dos três o dado estava perdido. Perdida estava a DESCOBERTA: o arquivo não apontava
 * para lugar nenhum, e quem o encontrasse sozinho tinha de adivinhar.
 *
 * <h2>Por que mora no peer {@code legenda}</h2>
 * Carimbar é mecânica de CABEÇALHO ASS — inserir comentário depois de {@code [Script Info]},
 * preservar a quebra de linha do arquivo, substituir o carimbo anterior em vez de empilhar. O
 * dono do formato é este peer. A mecânica nasceu privada dentro de
 * {@code AchatadorEstilosDecorativosService}; deixá-la lá e copiá-la para a tradução criaria a
 * SEGUNDA implementação da mesma regra — exatamente a classe de defeito que
 * {@code CatracaRegraDuplicadaEntreFatiasTest} existe para contar.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Escreve como COMENTÁRIO do formato ({@code ;}) dentro de {@code [Script Info]}. Player e
 *       libass ignoram; o Aegisub já usa a mesma convenção no cabeçalho que gera.</li>
 *   <li><b>Idempotente:</b> um carimbo anterior é substituído, nunca empilhado. Carimbar duas
 *       vezes não pode produzir cabeçalho que cresce a cada execução.</li>
 *   <li>Preserva a quebra de linha do arquivo ({@code \r\n} ou {@code \n}) — trocar o final de
 *       linha de um {@code .ass} no meio do acervo é mudança silenciosa que nenhum teste de
 *       conteúdo pegaria.</li>
 *   <li>Não registra data nem caminho absoluto: data envelhece dentro do arquivo e caminho
 *       vazaria a máquina de quem rodou.</li>
 *   <li><b>Toda</b> linha de carimbo começa por {@link #PREFIXO}. É esse prefixo, e só ele, que
 *       torna a remoção do carimbo anterior possível sem tocar em comentário de terceiros.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Cabeçalho nulo vira vazio; cabeçalho sem {@code [Script Info]} recebe o carimbo no início;
 * lista de linhas vazia devolve o cabeçalho apenas limpo do carimbo anterior. Nunca lança e
 * nunca devolve {@code null}.
 */
public final class CarimboCabecalhoLegenda {

    /**
     * Prefixo de TODA linha de carimbo. É o que permite remover o bloco anterior sem apagar
     * comentário que não seja nosso — o operador e o Aegisub também escrevem linhas com {@code ;}.
     */
    public static final String PREFIXO = "; KRONOS ";

    private CarimboCabecalhoLegenda() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aplica o carimbo ao cabeçalho, substituindo qualquer carimbo anterior.
     *
     * <p>INVARIANTES DO DOMÍNIO: cada item de {@code linhas} vira uma linha de comentário
     * prefixada por {@link #PREFIXO}; itens que já começam com o prefixo não são prefixados de
     * novo, para que quem já monta a linha inteira não gere {@code "; KRONOS ; KRONOS ..."}.
     * A inserção é logo após {@code [Script Info]}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nulos degradam para vazio; nunca lança.
     *
     * @param cabecalho cabeçalho atual do documento
     * @param linhas conteúdo do carimbo, uma linha por item, sem o {@code ;}
     * @return cabeçalho com o carimbo aplicado
     */
    public static String aplicar(String cabecalho, List<String> linhas) {
        String limpo = remover(cabecalho);
        if (linhas == null || linhas.isEmpty()) {
            return limpo;
        }
        String quebra = limpo.contains("\r\n") ? "\r\n" : "\n";

        StringBuilder bloco = new StringBuilder();
        for (String linha : linhas) {
            if (linha == null || linha.isBlank()) {
                continue;
            }
            bloco.append(linha.startsWith(PREFIXO) ? linha : PREFIXO + linha).append(quebra);
        }
        if (bloco.length() == 0) {
            return limpo;
        }

        int marcador = limpo.indexOf("[Script Info]");
        if (marcador < 0) {
            return bloco + limpo;
        }
        int fimDaLinha = limpo.indexOf('\n', marcador);
        if (fimDaLinha < 0) {
            return limpo + quebra + bloco;
        }
        return limpo.substring(0, fimDaLinha + 1) + bloco + limpo.substring(fimDaLinha + 1);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: remove um bloco de carimbo anterior. Sem isto, cada execução somaria
     * linhas de comentário e o cabeçalho cresceria sem limite — e o carimbo mais antigo, já falso,
     * continuaria lá dizendo números de outra execução.
     *
     * <p>INVARIANTES DO DOMÍNIO: remove APENAS linhas iniciadas por {@link #PREFIXO}. Comentário
     * do operador ou do Aegisub sobrevive.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nulo devolve vazio; cabeçalho sem carimbo volta
     * inalterado.
     */
    public static String remover(String cabecalho) {
        if (cabecalho == null) {
            return "";
        }
        if (!cabecalho.contains(PREFIXO)) {
            return cabecalho;
        }
        StringBuilder saida = new StringBuilder(cabecalho.length());
        for (String linha : cabecalho.split("\n", -1)) {
            if (linha.startsWith(PREFIXO)) {
                continue;
            }
            if (saida.length() > 0) {
                saida.append('\n');
            }
            saida.append(linha);
        }
        return saida.toString();
    }
}
