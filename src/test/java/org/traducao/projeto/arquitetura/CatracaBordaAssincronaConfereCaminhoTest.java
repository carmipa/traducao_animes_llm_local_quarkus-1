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

    /** Disparo de trabalho em segundo plano: depois disto a resposta HTTP já foi. */
    private static final Pattern DISPARO_ASSINCRONO =
        Pattern.compile("filaExecucao\\.submeter|CompletableFuture\\.runAsync");

    /**
     * Caminho construído a partir de texto que veio da requisição. Cobre as
     * grafias das duas pilhas que convivem no projeto — JAX-RS com {@code record
     * Request} e Spring com {@code Map<String,String> payload}.
     */
    private static final Pattern CAMINHO_DO_USUARIO = Pattern.compile(
        "(Paths\\.get|Path\\.of)\\(\\s*(req|request|payload|diretorio|caminho|informado|limpo)");

    /** Quem confere. Basta a referência: a injeção não compila sem o tipo. */
    private static final Pattern CONFERE = Pattern.compile("GuardaCaminhoEntrada");

    @Test
    @DisplayName("borda ASSINCRONA que recebe caminho do usuario tem de conferir antes de enfileirar")
    void bordaAssincronaConfereCaminho() throws IOException {
        List<String> desprotegidos = new ArrayList<>();
        int assincronasComCaminho = 0;

        try (Stream<Path> arquivos = Files.walk(FONTE)) {
            for (Path p : arquivos.filter(f -> f.toString().endsWith("Controller.java")).toList()) {
                String fonte = Files.readString(p);
                if (!DISPARO_ASSINCRONO.matcher(fonte).find()
                    || !CAMINHO_DO_USUARIO.matcher(fonte).find()) {
                    continue;
                }
                assincronasComCaminho++;
                if (!CONFERE.matcher(fonte).find()) {
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
