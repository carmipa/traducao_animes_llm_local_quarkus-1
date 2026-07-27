package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.raspagemRevisao.domain.DetalheRevisao;
import org.traducao.projeto.raspagemRevisao.domain.ModoRevisaoLegendas;
import org.traducao.projeto.raspagemRevisao.domain.ports.TelemetriaRevisaoPort;

import java.nio.file.Path;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: monta e publica o relatório de uma sessão de revisão — os totais agregados
 * e a trilha ocorrência a ocorrência. É o artefato que o operador abre depois para conferir o que a
 * revisão fez, e o único registro de por que uma fala mudou.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>NÃO imprime. Devolve a pasta onde gravou e quem avisa o operador é o caso de uso — a mesma
 *       separação que o {@code ResolvedorArtefatosRevisao} adotou. Um serviço que escreve no
 *       console não pode ser testado sem capturar {@code System.out}.</li>
 *   <li>Os totais que entram no relatório são os MESMOS que o caso de uso devolve ao chamador.
 *       Recalcular qualquer um aqui abriria a porta para relatório e retorno discordarem, e o
 *       relatório é o que sobrevive à sessão.</li>
 *   <li>A telemetria vem pela porta da FASE 2; esta classe não conhece a fatia {@code telemetria}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Lista de detalhes vazia ainda produz relatório válido com os totais disponíveis — um relatório
 * sem trilha é pior que nenhum, mas um lote sem relatório é pior ainda.
 */
@Service
public class RelatorioRevisaoService {

    /** Acima disto, o texto da fala é truncado no relatório — não no arquivo. */
    private static final int LIMITE_CAMPO = 500;

    private final TelemetriaRevisaoPort telemetria;

    /**
     * PROPÓSITO DE NEGÓCIO: injeta a porta de telemetria da fatia.
     * <p>INVARIANTES DO DOMÍNIO: guarda a referência recebida.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede o uso do serviço.
     */
    public RelatorioRevisaoService(TelemetriaRevisaoPort telemetria) {
        this.telemetria = telemetria;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: encerra a revisão persistindo métricas agregadas e a explicação por
     * ocorrência, para auditoria e evolução do dataset.
     *
     * <p>INVARIANTES DO DOMÍNIO: os totais do relatório correspondem ao resultado devolvido pela
     * operação; detalhes nunca substituem a telemetria canônica. O nome da operação e o prefixo do
     * arquivo dependem do MODO — misturar os dois no mesmo arquivo tornaria as duas revisões
     * indistinguíveis no histórico.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: a porta absorve falha de I/O; devolve a pasta consultada.
     *
     * @return a pasta onde os relatórios daquela entrada são gravados, para o chamador informar
     */
    public Path registrar(
        Path pastaLegendasPt,
        long duracaoMs,
        int arquivos,
        int problemas,
        int corrigidas,
        int auditadas,
        int semOriginal,
        int pendentes,
        ModoRevisaoLegendas modo,
        List<DetalheRevisao> detalhes
    ) {
        boolean llm = modo == ModoRevisaoLegendas.LLM_CONCORDANCIA;
        String nomeOperacao = llm
            ? "Revisão Concordância (.ass LLM)"
            : "Revisão Legendas (.ass Google)";
        String cabecalho = llm
            ? "REVISÃO DE CONCORDÂNCIA PT-BR (.ass via LLM)\n============================================"
            : "REVISÃO DE LEGENDAS (.ass)\n==========================";
        String rotuloCorrigidas = llm ? "Falas corrigidas via LLM" : "Falas corrigidas via Google";

        String relatorio = """
            %s
            Pasta: %s
            Duração: %s
            Arquivos analisados: %d
            Falas auditadas: %d
            Falas sem original EN (ignoradas): %d
            Problemas detectados: %d
            %s: %d
            Falas pendentes: %d
            """.formatted(
            cabecalho,
            pastaLegendasPt.toAbsolutePath(),
            formatarDuracao(duracaoMs),
            arquivos,
            auditadas,
            semOriginal,
            problemas,
            rotuloCorrigidas,
            corrigidas,
            pendentes);

        relatorio += formatarDetalhes(detalhes);
        telemetria.registrarComRelatorio(
            nomeOperacao, pastaLegendasPt.toAbsolutePath().toString(),
            llm ? "revisao_concordancia_legendas" : "revisao_legendas",
            pastaLegendasPt, duracaoMs, arquivos, problemas, corrigidas, relatorio);
        return telemetria.pastaDeRelatorios(pastaLegendasPt);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: duração legível para quem lê o relatório, não para máquina.
     * <p>INVARIANTES DO DOMÍNIO: acima de um minuto mostra minutos e segundos.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    static String formatarDuracao(long ms) {
        long segundos = ms / 1000;
        return segundos >= 60 ? (segundos / 60) + "min " + (segundos % 60) + "s" : segundos + "s";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: acrescenta ao relatório a trilha auditável de cada correção, rejeição
     * ou bloqueio ocorrido na sessão.
     *
     * <p>INVARIANTES DO DOMÍNIO: cada item identifica arquivo, evento, resultado, problemas e
     * proposta; quebras internas são escapadas para que uma ocorrência fique legível num bloco só.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista nula ou vazia gera seção explícita "nenhuma
     * ocorrência" em vez de sumir — a ausência de trilha tem de ser visível.
     */
    static String formatarDetalhes(List<DetalheRevisao> detalhes) {
        StringBuilder texto = new StringBuilder("\nDETALHES POR OCORRÊNCIA\n=======================\n");
        if (detalhes == null || detalhes.isEmpty()) {
            return texto.append("Nenhuma ocorrência detalhada registrada.\n").toString();
        }
        for (DetalheRevisao detalhe : detalhes) {
            texto.append("\nArquivo: ").append(detalhe.arquivo()).append('\n')
                .append("Evento: ").append(detalhe.evento()).append(" | Estilo: ")
                .append(resumirCampo(detalhe.estilo())).append('\n')
                .append("Resultado: ").append(detalhe.resultado()).append('\n')
                .append("Problemas: ").append(String.join(" | ", detalhe.problemas())).append('\n')
                .append("Diagnóstico: ").append(resumirCampo(detalhe.diagnostico())).append('\n')
                .append("EN: ").append(resumirCampo(detalhe.original())).append('\n')
                .append("PT anterior: ").append(resumirCampo(detalhe.antes())).append('\n')
                .append("Proposta: ").append(resumirCampo(detalhe.depois())).append('\n');
        }
        return texto.toString();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mantém textos de legenda legíveis dentro do relatório sem perder as
     * quebras ASS relevantes.
     *
     * <p>INVARIANTES DO DOMÍNIO: limita APENAS a representação diagnóstica; não altera o que foi
     * persistido na legenda. A quebra {@code \n} vira um símbolo visível em vez de sumir, senão
     * duas falas diferentes apareceriam idênticas no relatório.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: valor ausente é representado por travessão.
     */
    static String resumirCampo(String valor) {
        if (valor == null || valor.isBlank()) {
            return "—";
        }
        String limpo = valor.replace("\r", "").replace("\n", " ↵ ").strip();
        return limpo.length() <= LIMITE_CAMPO ? limpo : limpo.substring(0, LIMITE_CAMPO - 3) + "...";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: converte uma exceção em diagnóstico legível para o relatório.
     * <p>INVARIANTES DO DOMÍNIO: NUNCA expõe stack trace e nunca devolve texto vazio — um detalhe
     * de auditoria em branco não explica nada a quem for ler.
     * <p>COMPORTAMENTO EM CASO DE FALHA: exceção sem mensagem usa o nome da classe.
     */
    static String mensagemFalha(Exception erro) {
        return erro.getMessage() == null || erro.getMessage().isBlank()
            ? erro.getClass().getSimpleName() : erro.getMessage();
    }
}
