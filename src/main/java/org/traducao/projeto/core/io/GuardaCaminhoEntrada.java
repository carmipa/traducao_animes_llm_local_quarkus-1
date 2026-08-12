package org.traducao.projeto.core.io;

import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: recusa, na porta de entrada, o trabalho que não tem como
 * dar certo — pasta que não existe, texto que não forma caminho, arquivo passado
 * onde se espera diretório. É o que impede a interface responder "iniciado" para
 * uma operação impossível e deixar a pessoa esperando um resultado que nunca vem.
 *
 * <h2>O prejuízo que originou a guarda</h2>
 * Medido em 11/08/2026, sondando as bordas com uma pasta que não existe em
 * ambiente nenhum. Das seis rotas testadas, <b>cinco responderam HTTP 200/202
 * "iniciada"</b>: revisão de lore, revisão de concordância, correção de legendas
 * e as duas de karaokê. Só o renomeador recusou com 400, e só porque é síncrono.
 *
 * <p>A correção de legendas foi até o fim: criou
 * {@code relatorios/PASTA-INEXISTENTE.../}, gravou o relatório JSON e registrou
 * na telemetria canônica {@code {"arquivosProcessados": 1, "itensCorrigidos": 0}}
 * — para uma pasta inexistente. Ali "0 corrigidos porque a pasta não existe"
 * virou idêntico a "0 corrigidos porque estava tudo certo", e o
 * {@code ConsolidadorTelemetriaPorFatia} lê esse arquivo.
 *
 * <p>Não é defeito de contêiner: um caminho digitado errado no Windows produz
 * exatamente o mesmo silêncio.
 *
 * <h2>INVARIANTES DO DOMÍNIO</h2>
 * <ul>
 *   <li><b>Falha fechada.</b> Caminho em branco, inválido ou inexistente é
 *       recusa, nunca "seguir e ver no que dá".</li>
 *   <li><b>Recusa é ANTES do enfileiramento.</b> Depois que a operação entra na
 *       fila, a resposta HTTP já saiu e o único canal que resta é o log — que
 *       ninguém está lendo no instante do clique.</li>
 *   <li><b>Recusa ORIENTA, não adivinha.</b> Caminho do Windows recebido num
 *       sistema que não é Windows devolve o equivalente sob a raiz montada
 *       <i>como sugestão de texto</i>. Converter em silêncio seria pior que
 *       recusar: um palpite errado aponta para conteúdo errado, e aí o dano é
 *       gravado no acervo em vez de barrado na porta.</li>
 *   <li><b>Nada é lido nem escrito aqui.</b> A guarda só decide.</li>
 * </ul>
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@link Optional} com a
 * {@link Recusa} e o {@link Motivo}; nunca lança, nunca devolve {@code null}.
 * {@link Optional#empty()} significa — e só significa — que o caminho existe e é
 * um diretório neste sistema de arquivos, agora.
 */
@Component
public class GuardaCaminhoEntrada {

    /**
     * Onde o acervo é montado dentro do contêiner. Usado apenas para SUGERIR o
     * caminho equivalente na mensagem de recusa; nada é convertido por conta
     * própria.
     */
    private static final String RAIZ_ACERVO_CONTAINER = "/acervo";

    /** {@code C:\...}, {@code d:/...} — letra de unidade seguida de barra. */
    private static final Pattern CAMINHO_WINDOWS = Pattern.compile("^[A-Za-z]:[\\\\/].*");

    /** Por que o caminho foi recusado. Cada valor tem uma orientação diferente. */
    public enum Motivo {
        /** Campo vazio ou só espaços. */
        NAO_INFORMADO,
        /** Texto que sequer forma um caminho para este sistema de arquivos. */
        CAMINHO_INVALIDO,
        /** O caminho não existe aqui. */
        NAO_ENCONTRADO,
        /** Existe, mas é arquivo onde se esperava pasta. */
        NAO_E_DIRETORIO,
        /** Existe e é pasta, mas é a pasta de SAÍDA — traduzir dali destrói o já traduzido. */
        SAIDA_COMO_ENTRADA
    }

    /**
     * Nomes de pasta de SAÍDA em uso no acervo. A lista nasceu de MEDIÇÃO, não de suposição:
     * a primeira versão dela, no harness de auditoria, trazia só os três primeiros e deixou
     * QUATRO obras fora do inventário — Memories, Break Blade e Patlabor usam
     * {@code traducao_ptbr_sem_lore} (saída da rota 2.2) e Macross II usa
     * {@code legendas_ptbr_corrigidas}.
     *
     * <p>{@code traducao_mistral} e {@code traducao_aya} entraram em 12/08/2026, quando o
     * confronto de modelos passou a manter várias traduções da mesma obra lado a lado. São
     * exatamente as pastas que doem mais se forem sobrescritas: são baseline de comparação.
     */
    private static final java.util.Set<String> PASTAS_DE_SAIDA = java.util.Set.of(
        "traducao_ptbr", "legendas_ptbr", "ptbr", "traducao_ptbr_sem_lore",
        "legendas_ptbr_corrigidas", "legenda-simplificada",
        "traducao_mistral", "traducao_aya", "traducao_ptbr_aya", "traducao_ptbr_achatado");

    /** Recusa com motivo e mensagem pronta para a tela. */
    public record Recusa(Motivo motivo, String mensagem) {}

    /**
     * PROPÓSITO DE NEGÓCIO: confere um único campo de pasta vindo da interface.
     *
     * <p>INVARIANTES DO DOMÍNIO: o rótulo entra na mensagem para a pessoa saber
     * QUAL dos campos da tela está errado — "a pasta não existe" numa tela com
     * dois campos não diz nada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@link Optional} com a recusa. Vazio
     * significa que o diretório existe agora.
     *
     * @param rotulo  como o campo se chama na tela ("Pasta traduzida (PT-BR)")
     * @param caminho o texto exatamente como veio da interface
     */
    public Optional<Recusa> conferirDiretorio(String rotulo, String caminho) {
        if (caminho == null || caminho.isBlank()) {
            return Optional.of(new Recusa(Motivo.NAO_INFORMADO,
                rotulo + " não foi informada."));
        }

        String limpo = caminho.trim();
        Path alvo;
        try {
            alvo = Path.of(limpo);
        } catch (InvalidPathException e) {
            return Optional.of(new Recusa(Motivo.CAMINHO_INVALIDO,
                rotulo + ": o texto informado não forma um caminho válido neste sistema (" + limpo + ")."));
        }

        if (Files.isDirectory(alvo)) {
            return Optional.empty();
        }
        if (Files.exists(alvo)) {
            return Optional.of(new Recusa(Motivo.NAO_E_DIRETORIO,
                rotulo + ": " + limpo + " existe, mas é um arquivo. Informe a PASTA que o contém."));
        }
        return Optional.of(new Recusa(Motivo.NAO_ENCONTRADO,
            rotulo + ": a pasta " + limpo + " não existe" + orientacao(limpo) + "."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: impede que uma tradução LEIA a pasta onde traduções são GRAVADAS.
     * É a lente de boa-fé aplicada à porta: ninguém faz isso de propósito, e quem faz não
     * percebe até o arquivo bom já ter virado o arquivo ruim.
     *
     * <h2>O prejuízo que originou</h2>
     * 06/08/2026: uma tradução apontou para {@code legenda-simplificada}, que é pasta de
     * SAÍDA, e <b>sobrescreveu 17 arquivos limpos</b>. O código fez exatamente o que foi
     * mandado — a interface é que permitiu mandar. A anotação da época registra a lição sem
     * meias palavras: <i>"revisão adversarial acha ataque e é cega para engano de boa-fé"</i>.
     *
     * <p>O risco não passou: em 12/08/2026 o mesmo acervo tinha {@code traducao_mistral},
     * {@code traducao_aya} e {@code traducao_ptbr} lado a lado na mesma obra, sendo duas
     * delas backup de comparação. Um clique na pasta errada apaga o baseline de um
     * experimento de dias.
     *
     * <h2>Invariantes do domínio</h2>
     * <ul>
     *   <li>Entrada e saída no MESMO caminho é sempre recusa: a tradução leria o que acabou
     *       de escrever.</li>
     *   <li>Pasta cujo nome é de saída conhecida é recusa. A lista nasceu de MEDIÇÃO do
     *       acervo, não de suposição — a primeira versão dela, com três nomes, deixou quatro
     *       obras fora de um inventário.</li>
     *   <li>Recusa ORIENTA: diz qual pasta o operador provavelmente queria.</li>
     *   <li>Só vale para TRADUZIR. Revisão e correção leem português de propósito, e cobrar
     *       esta guarda delas seria reprovar o uso correto.</li>
     * </ul>
     *
     * <h2>Comportamento em caso de falha</h2>
     * {@link Optional#empty()} quando a entrada não parece pasta de saída. Nunca lança.
     *
     * @param entrada caminho de onde as legendas serão LIDAS
     * @param saida caminho onde serão gravadas, ou {@code null}/vazio se for o padrão
     */
    public Optional<Recusa> conferirEntradaNaoEhSaidaDeTraducao(String entrada, String saida) {
        if (entrada == null || entrada.isBlank()) {
            return Optional.empty();
        }
        Path pastaEntrada;
        try {
            pastaEntrada = Path.of(entrada.trim()).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            return Optional.empty();
        }

        if (saida != null && !saida.isBlank()) {
            try {
                Path pastaSaida = Path.of(saida.trim()).toAbsolutePath().normalize();
                if (pastaEntrada.equals(pastaSaida)) {
                    return Optional.of(new Recusa(Motivo.SAIDA_COMO_ENTRADA,
                        "A pasta de entrada e a de saída são a MESMA (" + pastaEntrada + "). "
                            + "A tradução leria o que ela mesma acabou de gravar e sobrescreveria "
                            + "o original. Informe uma pasta de saída diferente."));
                }
            } catch (InvalidPathException ignorada) {
                // Caminho de saída inválido é problema de outra checagem, não desta.
            }
        }

        Path nome = pastaEntrada.getFileName();
        if (nome == null) {
            return Optional.empty();
        }
        String pasta = nome.toString().toLowerCase(java.util.Locale.ROOT);
        for (String saidaConhecida : PASTAS_DE_SAIDA) {
            if (pasta.equals(saidaConhecida)) {
                return Optional.of(new Recusa(Motivo.SAIDA_COMO_ENTRADA,
                    "A pasta escolhida (" + nome + ") é uma pasta de SAÍDA de tradução — ela contém"
                        + " o português já traduzido, não o original. Traduzir a partir dela"
                        + " sobrescreveria o trabalho pronto. A entrada costuma ser"
                        + " \"legendas_extraidas_ass\" ou \"legendas_eng\", ao lado desta."));
            }
        }
        return Optional.empty();
    }

    // NÃO existe um conferirDiretorios(Map). A primeira versão tinha, e a tela com
    // dois campos a chamava com Map.of(...) — que NÃO garante ordem de iteração.
    // A mensagem apontaria ora o campo de cima, ora o de baixo, sem nada mudar no
    // código. Quem tem mais de um campo encadeia com Optional.or(), que é ordenado
    // e ainda por cima só avalia o segundo se o primeiro passar.

    /**
     * Acrescenta a orientação que transforma "não existe" em algo acionável.
     *
     * <p>Caminho do Windows num sistema que não é Windows é o caso do contêiner:
     * {@code Path.of("C:/animes/86")} no Linux vira um caminho RELATIVO e acaba
     * resolvido como {@code /app/C:/animes/86}. A mensagem crua ("não existe")
     * está certa e não ajuda ninguém; a sugestão diz onde o acervo realmente
     * está montado — e continua sendo sugestão, porque quem escolhe é quem sabe.
     */
    private String orientacao(String caminho) {
        boolean sistemaWindows = File.separatorChar == '\\';
        if (sistemaWindows || !CAMINHO_WINDOWS.matcher(caminho).matches()) {
            return "";
        }
        // NÃO se monta aqui o caminho equivalente. Seria um palpite: a segunda
        // parte de "C:/animes/86" só é o acervo porque a montagem de HOJE diz
        // isso, e "D:/PROJETOS/x" viraria "/acervo/x", que aponta para lugar
        // nenhum com cara de resposta. Diz-se onde o acervo está e devolve-se a
        // escolha a quem sabe — é a diferença entre orientar e adivinhar.
        return " — este é um caminho do Windows e o KRONOS está rodando em Linux (contêiner),"
            + " onde o acervo do host é montado em " + RAIZ_ACERVO_CONTAINER
            + ". Use o botão Procurar para escolher a pasta a partir de " + RAIZ_ACERVO_CONTAINER;
    }
}
