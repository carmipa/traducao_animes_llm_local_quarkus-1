package org.traducao.projeto.arquitetura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: impede que uma tela volte a responder "iniciado" para um
 * trabalho que não tem como acontecer.
 *
 * <h2>O prejuízo, medido em 11/08/2026</h2>
 * Sondando as bordas com uma pasta que não existe em ambiente nenhum, <b>sete
 * rotas</b> responderam HTTP 200/202 "iniciada": revisão de lore (2), revisão de
 * concordância, correção de legendas, os dois karaokês e — as piores, porque
 * GRAVAM no acervo — {@code /troca-legenda/aplicar} e
 * {@code /troca-legenda/achatar-estilos}.
 *
 * <p>A correção de legendas foi até o fim: criou {@code relatorios/<pasta
 * inexistente>/}, gravou o relatório e registrou na telemetria canônica
 * {@code {"arquivosProcessados": 1, "itensCorrigidos": 0}}. Ali "0 corrigidos
 * porque a pasta não existe" virou idêntico a "0 corrigidos porque estava tudo
 * certo" — e o {@code ConsolidadorTelemetriaPorFatia} lê esse arquivo.
 *
 * <h2>Por que o discriminador é ASSÍNCRONO, e não a fatia</h2>
 * As três rotas que já recusavam com 400 — renomeador, {@code /troca-legenda/
 * escanear} e auditoria de conteúdo — são SÍNCRONAS: a exceção do caso de uso
 * ainda alcança a resposta HTTP. Assim que o trabalho vai para
 * {@code filaExecucao.submeter} ou {@code CompletableFuture.runAsync}, a resposta
 * já saiu, e o único canal que resta é o log — que ninguém está lendo no instante
 * do clique. O defeito não é da fatia; é da forma.
 *
 * <p>Vale igual no Windows: um caminho digitado errado produz o mesmo silêncio.
 * Nada aqui é sobre contêiner.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: nomeia o arquivo e diz a forma esperada.
 */
class CatracaBordaAssincronaConfereCaminhoTest {

    private static final Path FONTE = Path.of("src", "main", "java");

    /**
     * Disparo de trabalho em segundo plano: depois disto a resposta HTTP já foi.
     *
     * <p>{@code submeterJobComRelatorio} entrou em 2026-08-12, e a omissão dele custou caro: a
     * catraca nasceu enxergando só {@code filaExecucao.submeter} — a forma das 7 rotas
     * consertadas — e ficou <b>cega para os 8 controllers que enfileiram pelo
     * PipelineWebSupport</b>, entre eles a Tradução Local, que é a rota principal do projeto.
     * O defeito reapareceu em runtime: {@code POST /api/traduzir} com pasta inexistente
     * devolveu 200 "Tradução via LLM iniciada" e só falhou depois, no log.
     * <b>Guarda que não reconhece a forma aprova por cegueira.</b>
     */
    private static final Pattern DISPARO_ASSINCRONO = Pattern.compile(
        "filaExecucao\\.submeter|CompletableFuture\\.runAsync|submeterJobComRelatorio");

    /**
     * Caminho construído a partir de texto que veio da requisição. Cobre as
     * grafias das três pilhas que convivem no projeto — JAX-RS com {@code record
     * Request}, Spring com {@code Map<String,String> payload}, e o
     * {@code pipelineWebSupport.normalizarCaminho(req...)} do caminho compartilhado.
     */
    private static final Pattern CAMINHO_DO_USUARIO = Pattern.compile(
        "(Paths\\.get|Path\\.of|normalizarCaminho)\\(\\s*(req|request|payload|diretorio|caminho|informado|limpo)");

    /**
     * Quem confere que a pasta EXISTE. Três formas legítimas convivem no projeto, e exigir
     * apenas a primeira faria a catraca reprovar código correto:
     * <ul>
     *   <li>{@code GuardaCaminhoEntrada} — a porta compartilhada, que ainda ORIENTA (sugere o
     *       equivalente sob a raiz montada quando o caminho é do host);</li>
     *   <li>{@code Files.isDirectory} à mão, como o Remuxer;</li>
     *   <li>uma validação do próprio caso de uso que faz isso, como
     *       {@code validarPastaEntrada} na revisão de legendas.</li>
     * </ul>
     *
     * <p>Exigir a classe em vez do COMPORTAMENTO acusou dois controllers corretos em
     * 2026-08-12. <b>Guarda que reprova o certo ensina a desligar o alarme</b> — e aí ela deixa
     * de proteger os que estão errados de verdade.
     */
    private static final Pattern CONFERE = Pattern.compile(
        "GuardaCaminhoEntrada|Files\\.isDirectory|validarPastaEntrada");

    @Test
    @DisplayName("borda ASSINCRONA que recebe caminho do usuario tem de conferir ANTES de enfileirar")
    void bordaAssincronaConfereCaminho() throws IOException {
        List<String> desprotegidos = new ArrayList<>();
        int assincronasComCaminho = 0;

        try (Stream<Path> arquivos = Files.walk(FONTE)) {
            for (Path p : arquivos.filter(f -> f.toString().endsWith("Controller.java")).toList()) {
                String fonte = Files.readString(p);
                var disparo = DISPARO_ASSINCRONO.matcher(fonte);
                if (!disparo.find() || !CAMINHO_DO_USUARIO.matcher(fonte).find()) {
                    continue;
                }
                assincronasComCaminho++;
                // ANTES, não "em algum lugar": a conferência que roda DEPOIS do disparo já
                // perdeu a resposta HTTP, e foi exatamente esse o defeito do TraducaoController
                // — ele tinha Files.isDirectory, só que dentro do job.
                if (!CONFERE.matcher(fonte.substring(0, disparo.start())).find()) {
                    desprotegidos.add(p.getFileName().toString());
                }
            }
        }

        // Caso-controle embutido: se o padrão parar de casar qualquer coisa, o teste
        // não pode passar por não ter encontrado nada para examinar.
        assertTrue(assincronasComCaminho > 0,
            "instrumento cego: nenhuma borda assincrona com caminho de usuario foi encontrada em " + FONTE);

        assertTrue(desprotegidos.isEmpty(),
            "estas bordas enfileiram trabalho a partir de um caminho do usuario SEM conferir antes: "
                + desprotegidos
                + " — injete org.traducao.projeto.core.io.GuardaCaminhoEntrada e recuse com HTTP 400 "
                + "ANTES do submeter()/runAsync(), como em RevisaoConcordanciaController");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: provar que a catraca acima enxerga o defeito.
     *
     * <p>Guarda exercitada só no código são pode estar aprovando por não enxergar
     * nada — foi exatamente assim que a conferência de hash do
     * {@code checar-portao.ps1} nasceu aprovando um hash adulterado.
     */
    @Test
    @DisplayName("caso-controle: os tres padroes reprovam e aprovam o que devem")
    void padroesEnxergamOCasoDoente() {
        String doente = """
            @PostMapping("/revisar")
            public ResponseEntity<Map<String,Object>> iniciar(@RequestBody Req req) {
                Path pasta = Path.of(req.diretorioTraduzido().trim());
                filaExecucao.submeter(() -> useCase.executar(pasta));
                return ResponseEntity.ok(Map.of("mensagem", "iniciada"));
            }
            """;
        assertTrue(DISPARO_ASSINCRONO.matcher(doente).find(), "nao viu o submeter()");
        assertTrue(CAMINHO_DO_USUARIO.matcher(doente).find(), "nao viu o Path.of(req...)");
        assertFalse(CONFERE.matcher(doente).find(), "acusou conferencia onde nao ha");

        String saudavel = doente.replace("Path pasta =",
            "var r = guardaCaminho.conferirDiretorio(\"Pasta\", req.diretorioTraduzido());\n"
                + "    if (r.isPresent()) return ResponseEntity.badRequest().build();\n"
                + "    // org.traducao.projeto.core.io.GuardaCaminhoEntrada\n"
                + "    Path pasta =");
        assertTrue(CONFERE.matcher(saudavel).find(), "nao reconheceu a forma CORRETA");

        // A POSIÇÃO é o que decide, e este par prova que a catraca enxerga a diferença. As duas
        // versões abaixo contêm exatamente o mesmo Files.isDirectory; muda só o lado do disparo.
        String confereDepoisDoDisparo = """
            @PostMapping("/traduzir")
            public ResponseEntity<Resposta> traduzir(@RequestBody Req req) {
                submeterJobComRelatorio("traducao", "Traducao", () -> {
                    Path p = normalizarCaminho(req.entrada());
                    if (!Files.isDirectory(p)) { System.out.println("[FAIL]"); return; }
                    useCase.executar(p);
                });
                return ResponseEntity.ok(new Resposta("iniciada"));
            }
            """;
        var d = DISPARO_ASSINCRONO.matcher(confereDepoisDoDisparo);
        assertTrue(d.find(), "nao viu o submeterJobComRelatorio");
        assertFalse(CONFERE.matcher(confereDepoisDoDisparo.substring(0, d.start())).find(),
            "conferencia DEPOIS do disparo nao pode contar: a resposta HTTP ja saiu");

        String confereAntesDoDisparo = """
            @PostMapping("/remuxar")
            public ResponseEntity<Resposta> remuxar(@RequestBody Req req) {
                Path p = normalizarCaminho(req.entrada());
                if (p == null || !Files.isDirectory(p)) return ResponseEntity.badRequest().build();
                submeterJobComRelatorio("remuxer", "Remuxer", () -> useCase.executar(p));
                return ResponseEntity.ok(new Resposta("iniciada"));
            }
            """;
        var a = DISPARO_ASSINCRONO.matcher(confereAntesDoDisparo);
        assertTrue(a.find(), "nao viu o submeterJobComRelatorio");
        assertTrue(CONFERE.matcher(confereAntesDoDisparo.substring(0, a.start())).find(),
            "conferencia a mao ANTES do disparo e legitima — reprova-la seria alarme falso");

        // A outra metade do invariante: rota SINCRONA nao entra na regra, porque a
        // excecao do caso de uso ainda alcanca a resposta HTTP. Se esta assercao
        // cair, a catraca passou a cobrar guarda de quem nao precisa — e guarda que
        // reprova o certo e pior que guarda nenhuma.
        String sincrona = """
            @PostMapping("/escanear")
            public ResponseEntity<Map<String,Object>> escanear(@RequestBody Req req) {
                Path pasta = Path.of(req.diretorioLegendas().trim());
                return ResponseEntity.ok(useCase.escanear(pasta));
            }
            """;
        assertFalse(DISPARO_ASSINCRONO.matcher(sincrona).find(),
            "rota sincrona nao pode ser tratada como assincrona");
    }
}
