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

    /**
     * Pastas de SAÍDA que são BASELINE DE COMPARAÇÃO — traduções mantidas lado a lado para comparar
     * modelos ({@code traducao_aya}, {@code traducao_mistral}) ou variantes de experimento. Doem se
     * sobrescritas em silêncio (Achado 0, 2026-09-16). NÃO inclui {@code traducao_ptbr}/{@code ptbr}
     * genéricos, que são o alvo NORMAL da revisão — avisar neles seria o alarme falso que ensina a
     * ignorar o aviso. Subconjunto de {@link #PASTAS_DE_SAIDA}; Paulo ajusta quais nomes entram.
     */
    private static final java.util.Set<String> BASELINES_DE_COMPARACAO = java.util.Set.of(
        "traducao_mistral", "traducao_aya", "traducao_ptbr_aya", "traducao_ptbr_achatado",
        "traducao_ptbr_sem_lore", "legendas_ptbr_corrigidas", "legenda-simplificada");

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
        return conferir(rotulo, caminho, false);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mesma recusa na porta, para as telas que aceitam PASTA OU ARQUIVO de
     * vídeo (a 1.1 Análise de Mídia promete os dois na tela, na documentação e no MCP).
     *
     * <p>INVARIANTES DO DOMÍNIO: aceita diretório existente ou arquivo comum existente; tudo o
     * mais é recusado com o mesmo motivo de {@link #conferirDiretorio(String, String)}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve a {@link Recusa}; nunca lança.
     */
    public Optional<Recusa> conferirDiretorioOuArquivo(String rotulo, String caminho) {
        return conferir(rotulo, caminho, true);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: regra única por trás das duas portas públicas — recusa caminho vazio,
     * inválido ou inexistente antes do enfileiramento.
     *
     * <p>INVARIANTES DO DOMÍNIO: aspas envolventes saem antes de validar; arquivo só é aceito
     * quando {@code aceitaArquivo}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve a {@link Recusa} com motivo e mensagem; nunca lança.
     */
    private Optional<Recusa> conferir(String rotulo, String caminho, boolean aceitaArquivo) {
        if (caminho == null || caminho.isBlank()) {
            return Optional.of(new Recusa(Motivo.NAO_INFORMADO,
                rotulo + " não foi informada."));
        }

        // O "Copiar como caminho" do Explorer põe aspas; quem consome o caminho depois
        // (PipelineWebSupport.normalizarCaminho) já as tira, mas a guarda validava o texto
        // com aspas e recusava como "caminho inválido" (auditoria de 08/10/2026, achado E7).
        String limpo = semAspasEnvolventes(caminho.trim());
        Path alvo;
        try {
            alvo = caminhoDaInterface(caminho);
        } catch (InvalidPathException e) {
            return Optional.of(new Recusa(Motivo.CAMINHO_INVALIDO,
                rotulo + ": o texto informado não forma um caminho válido neste sistema (" + limpo + ")."));
        }

        if (Files.isDirectory(alvo)) {
            return Optional.empty();
        }
        if (aceitaArquivo && Files.isRegularFile(alvo)) {
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
     * PROPÓSITO DE NEGÓCIO: o {@link Path} que a operação vai usar, montado do texto da tela do
     * MESMO jeito que esta guarda o confere — dono único dessa conversão para as rotas que recebem
     * pasta da interface.
     *
     * <h2>O prejuízo que obrigou a existir, medido em 09/10/2026</h2>
     * Três controllers faziam {@code Path.of(texto.trim())} depois de a guarda aprovar o texto:
     * <ul>
     *   <li>sem tirar as aspas que a guarda tira: o "Copiar como caminho" do Explorer passava na
     *       guarda e quebrava no {@code Path.of} ({@code "} não é caractere de caminho no
     *       Windows);</li>
     *   <li>sem ancorar o relativo na raiz operacional ({@link DiretorioBaseKronos}), como o
     *       {@code PipelineWebSupport.normalizarCaminho} já fazia: na suíte, {@code "cache"} virava
     *       o cache REAL da árvore de trabalho, e o job da 3.3 sobre ele prendia a fila por mais de
     *       30 s ({@code ApiEndpointsTest.telemetriaExportarRetornaArquivoJson}).</li>
     * </ul>
     * E dentro desta própria classe, {@link #conferirEntradaNaoEhSaidaDeTraducao} e
     * {@link #avisoRevisaoSobrescreveBaseline} recebiam o texto com aspas, caíam na
     * {@link InvalidPathException} e respondiam "nada a recusar" — o caminho colado do Explorer
     * pulava a recusa de traduzir a partir da pasta de SAÍDA.
     *
     * <p>INVARIANTES DO DOMÍNIO: tira espaços e o par de aspas envolvente; caminho absoluto passa
     * intocado; relativo fica sob a raiz operacional — em produção a raiz é o diretório corrente e
     * o resultado é idêntico a {@code Path.of(texto)}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto que não forma caminho propaga
     * {@link InvalidPathException}; nulo propaga {@link NullPointerException}. Quem chama depois de
     * {@link #conferirDiretorio} já recebeu a recusa para esses casos.
     */
    public static Path caminhoDaInterface(String caminho) {
        return DiretorioBaseKronos.resolver(semAspasEnvolventes(caminho.trim()));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: tira o par de aspas que envolve o caminho inteiro, como o Explorer
     * do Windows entrega ao copiar.
     *
     * <p>INVARIANTES DO DOMÍNIO: só remove quando a MESMA aspa (dupla ou simples) abre e fecha o
     * texto; aspas no meio do caminho ficam, e o caminho continua sendo recusado se for inválido.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto curto demais ou sem o par devolve o próprio texto.
     */
    static String semAspasEnvolventes(String texto) {
        if (texto.length() >= 2) {
            char primeiro = texto.charAt(0);
            char ultimo = texto.charAt(texto.length() - 1);
            if ((primeiro == '"' || primeiro == '\'') && primeiro == ultimo) {
                return texto.substring(1, texto.length() - 1).trim();
            }
        }
        return texto;
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
            pastaEntrada = caminhoDaInterface(entrada).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            return Optional.empty();
        }

        if (saida != null && !saida.isBlank()) {
            try {
                Path pastaSaida = caminhoDaInterface(saida).toAbsolutePath().normalize();
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

    /**
     * PROPÓSITO DE NEGÓCIO: AVISA (não bloqueia) quando a REVISÃO vai sobrescrever no lugar uma pasta
     * que é BASELINE DE COMPARAÇÃO. A revisão lê e grava português de propósito — por isso não é
     * recusa como em {@link #conferirEntradaNaoEhSaidaDeTraducao} —, mas sobrescrever um baseline de
     * modelo em silêncio apaga a referência de um experimento de dias.
     *
     * <h2>O gap que originou (Achado 0, 2026-09-16)</h2>
     * A {@link #conferirEntradaNaoEhSaidaDeTraducao} isenta a revisão ("revisão lê PT de propósito"),
     * decisão anterior aos baselines {@code traducao_aya}/{@code traducao_mistral} (12/08). Mas a 3.1
     * sobrescreve a pasta que lê, no lugar, e NÃO chamava guarda de caminho nenhuma. Apontar a revisão
     * para um baseline o sobrescrevia e a tela mostrava {@code [SUCESSO]} — sem aviso.
     *
     * <p>INVARIANTES DO DOMÍNIO: só o NOME da pasta decide, e só os baselines de comparação disparam —
     * o alvo genérico ({@code traducao_ptbr}) NÃO, senão o aviso viraria ruído no uso normal. NÃO
     * bloqueia: o backup por execução continua sendo a rede de segurança; isto só torna o dano visível.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada nula/inválida devolve {@link Optional#empty()}.
     *
     * @param entrada caminho que a revisão vai LER e SOBRESCREVER
     * @return o aviso pronto para o console, quando a pasta é um baseline de comparação
     */
    public Optional<String> avisoRevisaoSobrescreveBaseline(String entrada) {
        if (entrada == null || entrada.isBlank()) {
            return Optional.empty();
        }
        Path pasta;
        try {
            pasta = caminhoDaInterface(entrada).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
        Path nome = pasta.getFileName();
        if (nome == null) {
            return Optional.empty();
        }
        if (BASELINES_DE_COMPARACAO.contains(nome.toString().toLowerCase(java.util.Locale.ROOT))) {
            return Optional.of(
                "[ATENÇÃO] A pasta \"" + nome + "\" é um BASELINE de comparação: a revisão vai "
                    + "sobrescrevê-la NO LUGAR (há backup por execução). Confira se era mesmo esta a "
                    + "pasta que você queria revisar.");
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
