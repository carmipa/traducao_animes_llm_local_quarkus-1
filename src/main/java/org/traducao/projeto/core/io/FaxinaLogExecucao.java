package org.traducao.projeto.core.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * PROPÓSITO DE NEGÓCIO: manter o disco limpo sem que ninguém precise lembrar disso. O KRONOS
 * grava um log por execução; esta classe apaga os que passaram da validade, para que rodar o
 * sistema todo dia não vire um gargalo de espaço meses depois.
 *
 * <h2>O prejuízo que originou</h2>
 * Pedido de Paulo em 08/08/2026, e a medição do mesmo dia dá o tamanho: {@code logs/} tinha
 * <b>~80 MB</b> em arquivos sem dono nem rotação — {@code claude-run.log} 30 MB,
 * {@code console-web.log} 28,8 MB, {@code quarkusdev-start.log} 10,1 MB — e uma única sessão de
 * {@code quarkusDev} produziu <b>26 MB em 10 horas</b>. Nenhuma linha de código no projeto
 * apagava log: {@code grep} por {@code deleteIfExists|retencao|rotac} devolvia zero.
 *
 * <p>O segundo motivo é de AUDITORIA, não de espaço: log de execuções misturadas responde
 * errado. Está registrado neste projeto que "auditar feature pelo tradutor.log deu 3 resultados
 * errados no mesmo dia". Um arquivo por execução é o que torna a pergunta "o que aconteceu
 * naquela run?" respondível.
 *
 * <h2>Invariantes do domínio (regra 16 — dano · camadas · prova)</h2>
 * <table>
 *   <tr><th>ID</th><th>invariante</th><th>dano se quebrado</th><th>o que protege</th></tr>
 *   <tr><td>LOG-1</td><td>só apaga DENTRO de {@code logs/execucoes}</td>
 *       <td>apagar cache, backup ou legenda do acervo</td>
 *       <td>pasta fixa no código + caminho real ({@code toRealPath}) conferido contra a raiz</td></tr>
 *   <tr><td>LOG-2</td><td>só apaga arquivo que casa {@value #PADRAO_NOME_TEXTO}</td>
 *       <td>apagar arquivo de outro dono largado na mesma pasta</td>
 *       <td>regex ancorada no nome, não em extensão</td></tr>
 *   <tr><td>LOG-3</td><td>NUNCA apaga o log da execução corrente</td>
 *       <td>o processo perde o próprio log enquanto escreve nele</td>
 *       <td>comparação por caminho absoluto normalizado</td></tr>
 *   <tr><td>LOG-4</td><td>idade vem do RELÓGIO do arquivo, nunca do nome</td>
 *       <td>nome com data forjada apagaria arquivo recente</td>
 *       <td>{@code Files.getLastModifiedTime}</td></tr>
 *   <tr><td>LOG-5</td><td>não segue link simbólico</td>
 *       <td>um link dentro da pasta faria a faxina sair dela</td>
 *       <td>{@code NOFOLLOW_LINKS} + recusa de não-arquivo-regular</td></tr>
 * </table>
 * Cada linha tem teste com caso-controle em {@code FaxinaLogExecucaoTest}.
 *
 * <h2>Revisão de erro de boa-fé (regra 15)</h2>
 * Isto é ferramenta DESTRUTIVA, e a pergunta obrigatória é: <i>como uma pessoa honesta causaria
 * dano sem perceber?</i> A resposta encontrada foi <b>apontar o log para uma pasta que já tem
 * outras coisas</b> — e é por isso que a faxina NÃO lê o caminho configurado do log. Ela tem
 * pasta própria, fixa, que só ela usa. Mudar a configuração do log não muda o que ela apaga.
 * A cicatriz que ensinou isso: em 06/08/2026 uma tradução apontou para {@code legenda-simplificada},
 * que é pasta de SAÍDA de outra ferramenta, e sobrescreveu 17 arquivos limpos — o código fez o
 * que foi mandado, e a interface é que permitiu mandar.
 *
 * <h2>Falha operacional (regra 18)</h2>
 * <ul>
 *   <li><b>pasta ausente</b> — não é erro: nada a fazer, devolve resultado zerado;</li>
 *   <li><b>sem permissão / arquivo travado pelo Windows</b> — o arquivo é PULADO e CONTADO em
 *       {@code naoRemovidos}; a faxina nunca derruba o boot da aplicação por causa de log;</li>
 *   <li><b>relógio para trás</b> — arquivo com data futura tem idade negativa e não é apagado;</li>
 *   <li><b>duas instâncias ao mesmo tempo</b> — cada uma protege o próprio arquivo por LOG-3, e
 *       apagar um arquivo que a outra já apagou é silencioso ({@code deleteIfExists}).</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nunca lança para o chamador. Devolve o {@link Resultado} com o que apagou e o que não
 * conseguiu apagar — "não apaguei nada" e "não consegui apagar" são estados DIFERENTES e
 * distinguíveis, nunca o mesmo silêncio (regra 12).
 */
public final class FaxinaLogExecucao {

    /** Subpasta dedicada. Fixa no código de propósito — ver a revisão de boa-fé acima. */
    public static final String SUBPASTA = "execucoes";

    /** Documentado no Javadoc de LOG-2; mantido em texto para aparecer na tabela. */
    public static final String PADRAO_NOME_TEXTO = "kronos-<carimbo>.log";

    /**
     * Nome que a faxina reconhece como seu. Ancorado nas duas pontas: um
     * {@code relatorio-kronos-2026.log.bak} largado na pasta NÃO casa e sobrevive.
     */
    private static final Pattern NOME_DE_LOG_DE_EXECUCAO =
        Pattern.compile("^kronos-[0-9A-Za-z_.\\-]+\\.log$");

    private FaxinaLogExecucao() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o que a faxina fez, em números que o operador possa conferir.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code removidos + preservados + naoRemovidos} é o total de
     * arquivos inspecionados; nenhum é contado duas vezes.
     */
    public record Resultado(int removidos, int preservados, int naoRemovidos, long bytesLiberados,
                            List<String> motivosDeFalha) {

        public Resultado {
            motivosDeFalha = List.copyOf(motivosDeFalha);
        }

        /** Distingue "pasta vazia/inexistente" de "havia coisa e não consegui" (regra 12). */
        public boolean houveImpedimento() {
            return naoRemovidos > 0;
        }

        public String resumo() {
            return "faxina de log: %d removido(s) (%.1f MB), %d preservado(s), %d impedido(s)"
                .formatted(removidos, bytesLiberados / 1048576.0, preservados, naoRemovidos);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: apaga os logs de execução mais velhos que {@code retencao},
     * preservando o da execução atual.
     *
     * <p>INVARIANTES DO DOMÍNIO: os cinco invariantes da tabela do Javadoc da classe valem em
     * TODA chamada. Retenção nula ou negativa é tratada como "não apagar nada" — desligar a
     * faxina jamais pode virar apagar tudo (falha fechada, regra 11).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança. Pasta ausente devolve resultado zerado;
     * arquivo que não pôde ser apagado entra em {@code naoRemovidos} com o motivo.
     *
     * @param raizLogs      a pasta {@code logs} do KRONOS (normalmente {@code DiretorioBaseKronos.base().resolve("logs")})
     * @param retencao      idade máxima de um log; {@code null}, zero ou negativa não apaga nada
     * @param logDaExecucao o arquivo em uso agora, que jamais é apagado; pode ser {@code null}
     */
    public static Resultado limpar(Path raizLogs, Duration retencao, Path logDaExecucao) {
        List<String> falhas = new ArrayList<>();
        if (raizLogs == null || retencao == null || retencao.isZero() || retencao.isNegative()) {
            return new Resultado(0, 0, 0, 0L, falhas);
        }
        Path pasta = raizLogs.resolve(SUBPASTA);
        if (!Files.isDirectory(pasta)) {
            return new Resultado(0, 0, 0, 0L, falhas);
        }

        Path pastaReal;
        Path atualReal;
        try {
            pastaReal = pasta.toRealPath();
            atualReal = logDaExecucao == null ? null : caminhoRealOuAbsoluto(logDaExecucao);
        } catch (IOException e) {
            falhas.add("nao foi possivel resolver a pasta de logs: " + e.getMessage());
            return new Resultado(0, 0, 1, 0L, falhas);
        }

        Instant limite = Instant.now().minus(retencao);
        int removidos = 0;
        int preservados = 0;
        int impedidos = 0;
        long bytes = 0L;

        try (Stream<Path> arquivos = Files.list(pasta)) {
            for (Path arquivo : arquivos.toList()) {
                // LOG-5: nada de link simbolico, nada de diretorio.
                if (!Files.isRegularFile(arquivo, LinkOption.NOFOLLOW_LINKS)) {
                    preservados++;
                    continue;
                }
                // LOG-2: nome tem de ser nosso.
                if (!NOME_DE_LOG_DE_EXECUCAO.matcher(arquivo.getFileName().toString()).matches()) {
                    preservados++;
                    continue;
                }
                try {
                    Path real = arquivo.toRealPath(LinkOption.NOFOLLOW_LINKS);
                    // LOG-1: o caminho REAL tem de continuar dentro da pasta — texto nao basta,
                    // "pasta/../fora" e link seriam filhos so na string.
                    if (!real.getParent().equals(pastaReal)) {
                        preservados++;
                        continue;
                    }
                    // LOG-3: o log de agora nunca cai.
                    if (atualReal != null && real.equals(atualReal)) {
                        preservados++;
                        continue;
                    }
                    // LOG-4: idade pelo relogio do arquivo.
                    Instant modificado = Files.getLastModifiedTime(arquivo).toInstant();
                    if (!modificado.isBefore(limite)) {
                        preservados++;
                        continue;
                    }
                    long tamanho = Files.size(arquivo);
                    Files.deleteIfExists(arquivo);
                    removidos++;
                    bytes += tamanho;
                } catch (IOException e) {
                    // Arquivo travado pelo Windows, permissao negada, sumiu no meio do caminho:
                    // conta e segue. Log nao derruba aplicacao.
                    impedidos++;
                    falhas.add(arquivo.getFileName() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            falhas.add("nao foi possivel listar " + pasta + ": " + e.getMessage());
            impedidos++;
        }
        return new Resultado(removidos, preservados, impedidos, bytes, falhas);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: monta o nome do log desta execução, único por run.
     *
     * <p>INVARIANTES DO DOMÍNIO: o nome casa {@link #NOME_DE_LOG_DE_EXECUCAO}, senão a própria
     * faxina não o reconheceria como seu e ele viveria para sempre. O carimbo entra pronto de
     * quem chama — esta classe não lê relógio, para ser testável sem depender da hora.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: carimbo nulo, em branco ou com caractere fora de
     * {@code [0-9A-Za-z_.-]} é substituído por {@code "sem-carimbo"}, que é um nome válido —
     * nunca produz nome que a faxina não reconheça.
     */
    public static String nomeDoArquivo(String carimbo) {
        String limpo = carimbo == null ? "" : carimbo.replaceAll("[^0-9A-Za-z_.\\-]", "");
        if (limpo.isBlank()) {
            limpo = "sem-carimbo";
        }
        return "kronos-" + limpo + ".log";
    }

    private static Path caminhoRealOuAbsoluto(Path p) {
        try {
            return p.toRealPath(LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            // Ainda nao existe em disco (o handler pode nao ter criado): normaliza e segue.
            return p.toAbsolutePath().normalize();
        }
    }
}
