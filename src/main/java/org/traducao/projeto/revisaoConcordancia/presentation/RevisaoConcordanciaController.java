package org.traducao.projeto.revisaoConcordancia.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.traducao.projeto.core.execucao.FilaExecucaoPipeline;
import org.traducao.projeto.core.presentation.ui.AnsiCores;
import org.traducao.projeto.core.presentation.web.LogStreamService;
import org.traducao.projeto.core.util.DuracaoUtil;
import org.traducao.projeto.revisaoConcordancia.application.RevisarConcordanciaUseCase;
import org.traducao.projeto.revisaoConcordancia.domain.ResultadoConcordancia;

import java.nio.file.Path;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: expõe a Revisão de Concordância à interface local — corrige gênero
 * inequívoco numa pasta de legendas PT-BR, sem inglês, sem cache e sem LLM. Enfileira o trabalho
 * na fila única do pipeline e reporta o desfecho real no console.
 * <p>INVARIANTES DO DOMÍNIO: só a pasta PT-BR é obrigatória; usa a MESMA fila do pipeline para não
 * rodar em paralelo com tradução/revisão.
 * <p>COMPORTAMENTO EM CASO DE FALHA: entrada inválida retorna HTTP 400; falha assíncrona vira
 * banner vermelho no console.
 */
@RestController
@RequestMapping("/api")
public class RevisaoConcordanciaController {

    private static final String LINHA = "========================================================================";

    private final FilaExecucaoPipeline filaExecucao;
    private final RevisarConcordanciaUseCase revisarConcordanciaUseCase;
    private final LogStreamService logStreamService;
    private final org.traducao.projeto.core.io.GuardaCaminhoEntrada guardaCaminho;

    public RevisaoConcordanciaController(
        FilaExecucaoPipeline filaExecucao,
        RevisarConcordanciaUseCase revisarConcordanciaUseCase,
        LogStreamService logStreamService,
        org.traducao.projeto.core.io.GuardaCaminhoEntrada guardaCaminho
    ) {
        this.filaExecucao = filaExecucao;
        this.revisarConcordanciaUseCase = revisarConcordanciaUseCase;
        this.logStreamService = logStreamService;
        this.guardaCaminho = guardaCaminho;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: solicitação da Revisão de Concordância — a pasta PT-BR e se deve
     * gravar ({@code aplicar}) ou só simular.
     * <p>INVARIANTES DO DOMÍNIO: {@code aplicar=false} é dry-run.
     * <p>COMPORTAMENTO EM CASO DE FALHA: portador de dados puro; validação é do endpoint.
     */
    public record RevisaoConcordanciaRequest(String diretorioTraduzido, boolean aplicar) {}

    /**
     * PROPÓSITO DE NEGÓCIO: valida e enfileira a revisão de concordância de uma pasta PT-BR.
     * <p>INVARIANTES DO DOMÍNIO: a pasta PT-BR é obrigatória; usa a fila única do pipeline.
     * <p>COMPORTAMENTO EM CASO DE FALHA: validação retorna HTTP 400; exceção da tarefa vira banner
     * FALHOU no console.
     */
    @PostMapping("/revisar-concordancia")
    public ResponseEntity<Map<String, Object>> iniciarRevisaoConcordancia(@RequestBody RevisaoConcordanciaRequest req) {
        if (req.diretorioTraduzido() == null || req.diretorioTraduzido().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "erro", "Pasta com legendas traduzidas em portugues nao informada."));
        }

        // ANTES do submeter(): depois da fila a resposta HTTP ja saiu como "revisao
        // iniciada" e a recusa so viveria no log. Medido em 11/08/2026 — pasta
        // inexistente devolvia 200 "iniciada".
        var recusa = guardaCaminho
            .conferirDiretorio("Pasta com legendas traduzidas em portugues", req.diretorioTraduzido());
        if (recusa.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("erro", recusa.get().mensagem()));
        }

        Path pastaTraduzida = Path.of(req.diretorioTraduzido().trim());
        boolean aplicar = req.aplicar();

        filaExecucao.submeter(() -> {
            logStreamService.definirCanalAtual("revisao-concordancia");
            long inicioMs = System.currentTimeMillis();
            try {
                ResultadoConcordancia resultado =
                    revisarConcordanciaUseCase.revisarPasta(pastaTraduzida, aplicar);
                imprimirBanner(resultado, pastaTraduzida);
            } catch (Exception e) {
                imprimirFalha("Falha inesperada: " + e.getMessage());
            } finally {
                System.out.println(DuracaoUtil.linhaRelatorioFinal("Revisão de Concordância", inicioMs));
            }
        });

        return ResponseEntity.ok(Map.of(
            "mensagem", "Revisao de concordancia iniciada no servidor. Acompanhe os logs em tempo real."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: banner de fechamento deixando claro dry-run vs aplicado e as contagens.
     * <p>INVARIANTES DO DOMÍNIO: sempre imprime arquivos/falas e o modo.
     * <p>COMPORTAMENTO EM CASO DE FALHA: só escreve em {@code System.out}; não lança.
     */
    private void imprimirBanner(ResultadoConcordancia r, Path alvo) {
        // A MESMA regra de cores das linhas por arquivo, agora no fecho: verde só quando algo foi
        // GRAVADO, amarelo quando há fala a corrigir e nada foi escrito (a simulação), e o cinza
        // neutro reservado ao caso em que realmente não havia nada. Antes o banner era verde
        // sempre — inclusive numa simulação com 90 falas pendentes, que é justamente o engano
        // que a regra da 3.1 existe para impedir.
        boolean houveTrabalho = r.falasCorrigidas() > 0;
        String cor = !houveTrabalho ? AnsiCores.DIM : (r.aplicado() ? AnsiCores.GREEN : AnsiCores.YELLOW);
        String modo = !houveTrabalho
            ? (r.aplicado() ? "OK — NADA A CORRIGIR" : "OK — NADA A CORRIGIR (dry-run)")
            : (r.aplicado() ? "APLICADO" : "PENDENTE — SIMULADO (dry-run, nada gravado)");
        // Em dry-run nenhuma fala foi corrigida: elas MUDARIAM. Chamar as duas coisas pelo mesmo
        // nome é o que faz "arquivo limpo" e "arquivo com 90 pendências" saírem iguais na tela.
        String rotuloFalas = r.aplicado() ? "Falas corrigidas     : " : "Falas que mudariam   : ";
        System.out.println("\n" + cor + LINHA + AnsiCores.RESET);
        System.out.println(cor + "  [" + modo + "] REVISAO DE CONCORDANCIA (genero PT-BR)" + AnsiCores.RESET);
        // O ALVO no banner, e ele nasceu de um susto REAL em 24/08/2026. Paulo pediu uma
        // SIMULACAO do Macross II enquanto um lote de gravacao rodava, e o painel mostrou, logo
        // abaixo do cabecalho "simular (dry-run)" que o NAVEGADOR imprimiu, um banner [APLICADO]
        // com 11 arquivos — que era de OUTRA pasta, de OUTRA execucao, vinda do SERVIDOR.
        //
        // O console e um canal so: cabecalho do cliente e saida do servidor se intercalam, e quem
        // opera atribui o que ve ao que acabou de clicar. Sem o alvo, o banner nao tem como ser
        // conferido — e um [APLICADO] alheio embaixo do proprio "dry-run" e a pior leitura
        // possivel: o operador acredita que gravou o que mandou simular.
        System.out.println(cor + "  alvo: " + alvo + AnsiCores.RESET);
        System.out.println(cor + LINHA + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "  • Arquivos revisados   : " + r.arquivosAnalisados() + AnsiCores.RESET);
        // Linha separada, e ela NUNCA some do relatório: somar o que a tela nem abriu ao que ela
        // revisou faria o número parecer prova de cobertura que não houve.
        if (r.arquivosForaDoAlcance() > 0) {
            System.out.println(AnsiCores.DIM + "  • Fora do alcance      : " + r.arquivosForaDoAlcance()
                + " (.parcial — tradução incompleta)" + AnsiCores.RESET);
        }
        System.out.println(AnsiCores.CYAN + "  • Arquivos alterados   : " + r.arquivosAlterados() + AnsiCores.RESET);
        System.out.println(cor + "  • " + rotuloFalas + r.falasCorrigidas() + AnsiCores.RESET);
        // AS DUAS LINHAS "por genero" E "por acento" SAIRAM DAQUI EM 24/08/2026.
        //
        // Elas nasceram quando a tela tinha DOIS corretores e resumiam bem. Com CINCO elos
        // passaram a mentir por omissao: a fala consertada so pelo corretor de caractere entra no
        // total e nao entra em nenhuma das duas, entao o operador lia "6 corrigidas · por genero
        // 0 · por acento 0" e nao tinha de onde tirar o 6.
        //
        // O placar abaixo diz a mesma coisa de forma completa, uma linha por elo, e leva junto o
        // NAO VERIFICADO de quem nao pode rodar — que era a unica coisa que estas linhas faziam e
        // o placar nao fizesse. Resumo incompleto ao lado do detalhe completo e so ruido que
        // contradiz o que esta logo abaixo.

        // O PLACAR POR CORRETOR. Um total unico nao diz de onde veio o ganho. Mais importante: o
        // elo que NAO PODE RODAR aparece como NAO VERIFICADO, e nao como zero — "nao achei nada"
        // e "nem olhei" nunca podem sair iguais.
        if (!r.porCorretor().isEmpty()) {
            System.out.println(AnsiCores.DIM + "  • Por corretor:" + AnsiCores.RESET);
            for (var c : r.porCorretor()) {
                String corDoElo = !c.disponivel() ? AnsiCores.YELLOW
                    : (c.falhou() > 0 ? AnsiCores.RED : AnsiCores.DIM);
                System.out.println(corDoElo + "      " + c.linhaDeRelatorio() + AnsiCores.RESET);
            }
        }
        // O PRECO DAS GUARDAS, e nao so o que elas deixaram passar. Guarda cujo custo ninguem
        // mede vira dogma, e dogma nao se revisa quando o acervo muda. Estes dois numeros sao o
        // que o operador precisa para saber se a regra do nome proprio esta larga demais.
        if (r.barradasPorMaiuscula() > 0 || r.barradasPorIdioma() > 0) {
            System.out.println(AnsiCores.DIM + "  • Guardas do dicionario: "
                + r.barradasPorMaiuscula() + " fala(s) barrada(s) por MAIUSCULA (nome proprio), "
                + r.barradasPorIdioma() + " por IDIOMA (fala nao portuguesa)" + AnsiCores.RESET);
        }
        System.out.println(AnsiCores.CYAN + "  • Backups              : " + r.backups().size() + AnsiCores.RESET);
        System.out.println(cor + LINHA + "\n" + AnsiCores.RESET);
    }

    private void imprimirFalha(String mensagem) {
        System.out.println("\n" + AnsiCores.RED + LINHA + AnsiCores.RESET);
        System.out.println(AnsiCores.RED + "  [FALHOU] REVISAO DE CONCORDANCIA" + AnsiCores.RESET);
        System.out.println(AnsiCores.RED + "  " + mensagem + AnsiCores.RESET);
        System.out.println(AnsiCores.RED + LINHA + "\n" + AnsiCores.RESET);
    }
}
