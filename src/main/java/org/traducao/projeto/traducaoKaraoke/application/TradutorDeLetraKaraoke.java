package org.traducao.projeto.traducaoKaraoke.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.traducao.projeto.core.presentation.web.LogStreamService;
import org.traducao.projeto.core.texto.TextoSemTags;
import org.traducao.projeto.llm.domain.LlmPort;
import org.traducao.projeto.llm.domain.Lote;
import org.traducao.projeto.llm.domain.TraducaoLote;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;
import org.traducao.projeto.qualidadeTraducao.domain.MarcadorPerdidoException;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducaoKaraoke.domain.GradienteKaraoke;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PROPÃSITO DE NEGÃCIO: leva UMA linha de letra ao LLM e devolve a traduÃ§Ã£o vestida com a
 * moldura original. Ã o dono dos TRÃS caminhos de envio, e a ordem entre eles nÃ£o Ã© preferÃªncia:
 * cada um nasceu de um prejuÃ­zo medido.
 *
 * <h2>Os trÃªs caminhos, e o que cada um custou para existir</h2>
 * <ol>
 *   <li><b>Gradiente</b> â karaokÃª pintado letra a letra. Guilty Crown, 07/08/2026: 28 de 31
 *       recusas eram marcador intercalado que nenhum modelo devolve na ordem.</li>
 *   <li><b>Texto puro</b> â tags sÃ³ na borda. 08th MS Team, 08/08/2026: 1.258 de 1.258 avisos
 *       pelo mesmo motivo, com portuguÃªs perfeito sendo descartado.</li>
 *   <li><b>Mascarador</b> â o caminho antigo, sÃ³ para o que nÃ£o couber nos dois primeiros.</li>
 * </ol>
 *
 * <h2>Por que isto saiu do use case</h2>
 * Eram 780 bytecodes e CINCO dependÃªncias ({@code llmPort}, {@code mascarador},
 * {@code validador}, {@code telemetriaService}, {@code logStream}) dentro de um objeto que
 * tambÃ©m classificava, gravava cache e escrevia arquivo. Aqui a mesma lÃ³gica Ã© testÃ¡vel sem
 * disco, sem cache e sem manifesto.
 *
 * <h2>Invariantes do domÃ­nio</h2>
 * <ul>
 *   <li>Falha, resposta invÃ¡lida ou alucinaÃ§Ã£o devolvem {@code null} â a linha fica no idioma
 *       original e um aviso Ã© registrado. NUNCA derruba o arquivo.</li>
 *   <li>A moldura devolvida Ã© a do ORIGINAL, nunca a que o modelo imaginou.</li>
 *   <li>O {@code sequencialLote} Ã© o contador LOCAL da execuÃ§Ã£o, recebido por parÃ¢metro â nunca
 *       campo de instÃ¢ncia, para nÃ£o ser perturbado por execuÃ§Ã£o concorrente deste bean.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nunca lanÃ§a. Todo caminho de erro devolve {@code null} e escreve o motivo em {@code avisos}.
 */
@ApplicationScoped
public class TradutorDeLetraKaraoke {

    static final String CANAL_LOG = TraduzirKaraokeUseCase.CANAL_LOG;

    @Inject
    LlmPort llmPort;

    @Inject
    MascaradorTags mascarador;

    @Inject
    ValidadorTraducaoService validador;

    @Inject
    TelemetriaService telemetriaService;

    @Inject
    LogStreamService logStream;

    /**
     * PROPÓSITO DE NEGÓCIO: traduz uma única linha de letra via LLM (uma linha por lote — a
     * letra é curta e o lote unitário é o padrão do projeto), mascarando as tags antes e
     * restaurando-as depois.
     *
     * <p>INVARIANTES DO DOMÍNIO: o {@code sequencialLote} é o contador LOCAL da execução (ver
     * {@link #executar}), incrementado atomicamente para numerar o lote; nunca é campo de
     * instância, evitando estado compartilhado entre execuções concorrentes deste bean
     * singleton. A saída passa por desmascaramento e validação antes de ser aceita.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: falha de comunicação, resposta inválida ou
     * {@link AlucinacaoDetectadaException} devolve {@code null} (mantém a linha original) e
     * registra um aviso — nunca propaga para derrubar o arquivo.
     */
    String traduzirViaLlm(String original, List<String> avisos, AtomicInteger sequencialLote,
                                  String promptSistemaCongelado) {
        // Karaokê pintado LETRA A LETRA: o mascarador comum produziria uma dezena de [[TAGn]]
        // intercalados e o LLM não os devolve na ordem — medido no Guilty Crown em 07/08/2026,
        // 28 das 31 recusas de uma execução foram exatamente isso, e a única que passou saiu
        // como "So, eu e evidentementereithyingthathingthatmakes mea whole wholed".
        // Aqui a linha é decomposta em paleta + texto: o LLM recebe a frase limpa e as MESMAS
        // cores voltam distribuídas sobre a tradução. Ver GradienteKaraoke.
        Optional<GradienteKaraoke> gradiente = GradienteKaraoke.decompor(original);
        if (gradiente.isPresent()) {
            return traduzirGradiente(
                gradiente.get(), avisos, sequencialLote, promptSistemaCongelado);
        }

        // TAG NA BORDA (o caso do 08th MS Team, 08/08/2026): a linha tem UMA tag de prefixo,
        // vira UM marcador [[TAG0]], e o LLM simplesmente nao o repete. A traducao vinha CERTA e
        // era jogada fora. Do manifesto daquela execucao — 1.258 de 1.258 avisos, todos iguais:
        //
        //   Esperado 1 marcador(es) [0], recebido: Voce ve o sonho brilhando dentro da tempestade
        //   Esperado 1 marcador(es) [0], recebido: Aguenta firme agora! Nao solta isso.
        //
        // Portugues perfeito, descartado por falta de um marcador de controle. Resultado: de
        // 1.636 linhas detectadas, apenas 378 (23%) chegavam a legenda.
        //
        // A saida e a mesma do gradiente e a mesma que Paulo propos em 07/08: NAO mascarar,
        // SEPARAR. O LLM recebe a frase pura — sem marcador nenhum para perder — e a moldura e
        // recolocada aqui. TextoSemTags e o dono desse criterio, ja usado pela fatia traducao.
        Optional<TextoSemTags> semTags = TextoSemTags.decompor(original);
        if (semTags.isPresent()) {
            return traduzirTextoPuro(
                semTags.get(), avisos, sequencialLote, promptSistemaCongelado);
        }

        MascaradorTags.Mascarado mascarado = mascarador.mascarar(original);
        TraducaoLote resposta;
        try {
            resposta = llmPort.traduzir(
                new Lote(sequencialLote.incrementAndGet(), List.of(mascarado.texto())),
                null,
                promptSistemaCongelado);
        } catch (Exception e) {
            avisos.add("Falha de comunicação com o LLM; linha mantida sem tradução: " + original);
            logStream.publicarLog(CANAL_LOG, "   [AVISO] LLM falhou nesta linha (mantida no idioma original): " + e.getMessage());
            return null;
        }
        if (resposta == null || !resposta.sucesso()
            || resposta.linhasTraduzidas() == null || resposta.linhasTraduzidas().isEmpty()) {
            avisos.add("LLM não retornou tradução; linha mantida: " + original);
            logStream.publicarLog(CANAL_LOG, "   [AVISO] LLM sem resposta válida — linha mantida sem tradução.");
            return null;
        }
        try {
            String traduzido = mascarador.desmascarar(resposta.linhasTraduzidas().getFirst(), mascarado.tags());
            validador.validarFala(traduzido);
            return traduzido;
        } catch (MarcadorPerdidoException e) {
            // NAO e alucinacao, e o console nao pode dizer que e (08/08/2026): o modelo traduziu
            // e so nao repetiu o marcador. Mostrar a TRADUCAO RECUSADA e o que permite ao
            // operador ver, na hora, que perdeu trabalho bom — e nao lixo.
            telemetriaService.registrarAlucinacaoPrevenida();
            avisos.add("Marcador perdido (" + e.getMessage() + "); linha mantida: " + original);
            logStream.publicarLog(CANAL_LOG, "   [MARCADOR PERDIDO] traducao DESCARTADA por falta de tag: \""
                + e.traducaoRecusada() + "\"");
            return null;
        } catch (AlucinacaoDetectadaException e) {
            telemetriaService.registrarAlucinacaoPrevenida();
            avisos.add("Alucinação detectada (" + e.getMessage() + "); linha mantida: " + original);
            logStream.publicarLog(CANAL_LOG, "   [AVISO] Alucinação interceptada — linha mantida sem tradução: "
                + TraduzirKaraokeUseCase.visivelResumido(original));
            return null;
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: traduz uma linha de karaokê cujas tags estão todas na BORDA, enviando
     * ao LLM só a frase e recolocando a moldura na volta.
     *
     * <h2>O prejuízo que originou</h2>
     * Execução real no 08th MS Team em 08/08/2026: <b>1.636 linhas detectadas, 378 corrigidas
     * (23%)</b>. Os 1.258 avisos do manifesto são TODOS o mesmo motivo — marcador
     * {@code [[TAG0]]} não devolvido pelo modelo — e o texto recusado estava correto em
     * português. O sistema descartava tradução boa por causa de um marcador de controle.
     *
     * <p>INVARIANTES DO DOMÍNIO: o texto que sai daqui rumo ao LLM não contém tag ASS nem
     * marcador, então não existe marcador a perder; a moldura devolvida é a do ORIGINAL, nunca a
     * que o modelo tenha imaginado. A validação de alucinação roda sobre o texto puro, ANTES de
     * recompor — validar depois faria a própria tag disparar o detector de resíduo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: falha de comunicação, resposta inválida ou alucinação
     * devolvem {@code null} e a linha fica no idioma original, com aviso. Nunca linha meio montada.
     */
    String traduzirTextoPuro(TextoSemTags semTags, List<String> avisos,
                                     AtomicInteger sequencialLote, String promptSistemaCongelado) {
        TraducaoLote resposta;
        try {
            resposta = llmPort.traduzir(
                new Lote(sequencialLote.incrementAndGet(), List.of(semTags.textoLimpo())),
                null,
                promptSistemaCongelado);
        } catch (Exception e) {
            avisos.add("Falha de comunicação com o LLM; letra mantida: " + semTags.textoLimpo());
            logStream.publicarLog(CANAL_LOG, "   [AVISO] LLM falhou nesta linha (mantida): " + e.getMessage());
            return null;
        }
        if (resposta == null || !resposta.sucesso()
            || resposta.linhasTraduzidas() == null || resposta.linhasTraduzidas().isEmpty()) {
            avisos.add("LLM não retornou tradução; letra mantida: " + semTags.textoLimpo());
            logStream.publicarLog(CANAL_LOG, "   [AVISO] LLM sem resposta válida — letra mantida.");
            return null;
        }
        String traduzido = resposta.linhasTraduzidas().getFirst();
        try {
            validador.validarFala(traduzido);
        } catch (AlucinacaoDetectadaException e) {
            telemetriaService.registrarAlucinacaoPrevenida();
            avisos.add("Alucinação detectada (" + e.getMessage() + "); letra mantida: "
                + semTags.textoLimpo());
            logStream.publicarLog(CANAL_LOG,
                "   [AVISO] Alucinação interceptada na letra — mantida sem tradução: "
                    + semTags.textoLimpo());
            return null;
        }
        return semTags.recompor(traduzido);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: traduz uma linha de karaokê com gradiente de cor por letra, enviando
     * ao LLM apenas o texto que o espectador lê e devolvendo a tradução vestida com as MESMAS
     * cores — o efeito visual do fansub sobrevive à tradução.
     *
     * <p>INVARIANTES DO DOMÍNIO: o texto enviado ao LLM não tem nenhuma tag ASS, portanto não há
     * marcador para o modelo perder; a paleta é reposicionada, nunca alterada. A validação de
     * alucinação roda sobre o TEXTO PURO, antes de recompor — validar depois faria as tags de cor
     * dispararem o detector de resíduo, que foi o outro motivo de recusa observado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: falha de comunicação, resposta inválida ou alucinação
     * devolvem {@code null} e a linha permanece no idioma original, com aviso — exatamente como no
     * caminho comum. Nunca devolve linha meio montada.
     */
    String traduzirGradiente(GradienteKaraoke gradiente, List<String> avisos,
                                     AtomicInteger sequencialLote, String promptSistemaCongelado) {
        TraducaoLote resposta;
        try {
            resposta = llmPort.traduzir(
                new Lote(sequencialLote.incrementAndGet(), List.of(gradiente.textoVisivel())),
                null,
                promptSistemaCongelado);
        } catch (Exception e) {
            avisos.add("Falha de comunicação com o LLM; letra mantida: " + gradiente.textoVisivel());
            logStream.publicarLog(CANAL_LOG, "   [AVISO] LLM falhou nesta linha de karaokê (mantida): "
                + e.getMessage());
            return null;
        }
        if (resposta == null || !resposta.sucesso()
            || resposta.linhasTraduzidas() == null || resposta.linhasTraduzidas().isEmpty()) {
            avisos.add("LLM não retornou tradução; letra mantida: " + gradiente.textoVisivel());
            logStream.publicarLog(CANAL_LOG, "   [AVISO] LLM sem resposta válida — letra mantida.");
            return null;
        }
        String traduzido = resposta.linhasTraduzidas().getFirst();
        try {
            validador.validarFala(traduzido);
        } catch (AlucinacaoDetectadaException e) {
            telemetriaService.registrarAlucinacaoPrevenida();
            avisos.add("Alucinação detectada (" + e.getMessage() + "); letra mantida: "
                + gradiente.textoVisivel());
            logStream.publicarLog(CANAL_LOG,
                "   [AVISO] Alucinação interceptada na letra — mantida sem tradução: "
                    + gradiente.textoVisivel());
            return null;
        }
        return gradiente.recompor(traduzido);
    }
}
