package org.traducao.projeto.telemetria;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.core.io.DiretorioBaseKronos;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova, exercitando o caminho real de persistência de
 * relatório e telemetria de operação (o mesmo usado por revisão, correção,
 * lore etc.), que uma execução sob o perfil de teste NÃO grava nos diretórios
 * operacionais versionados ({@code relatorios/}, {@code logs/}) e sim na árvore
 * descartável redirecionada por {@link DiretorioBaseKronos}. É o guard que
 * impede a reaparição dos resíduos {@code relatorios/junit-*}.
 *
 * <p>INVARIANTES DO DOMÍNIO: a suíte roda com {@code kronos.dir.base} apontando
 * para {@code build/tmp/kronos-tests} (ver build.gradle), portanto os caminhos
 * relativos crus ({@code Path.of("relatorios")}, {@code Path.of("logs")})
 * continuam apontando para os diretórios reais e servem de referência do que
 * NÃO pode ser tocado.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer escrita real dispara asserção
 * JUnit, sinalizando regressão do isolamento.
 */
class IsolamentoArtefatosTest {

    @Test
    void finalizarOperacaoNaoGravaNosDiretoriosReais(@TempDir Path pastaEntrada) throws IOException {
        // Pré-condição: o redirecionamento de teste está ativo.
        assertTrue(System.getProperty(DiretorioBaseKronos.PROPRIEDADE_BASE) != null
                && !System.getProperty(DiretorioBaseKronos.PROPRIEDADE_BASE).isBlank(),
            "Testes devem rodar com kronos.dir.base redirecionado (ver build.gradle)");

        Path relatoriosReal = Path.of("relatorios", pastaEntrada.getFileName().toString());
        Path telemetriaReal = Path.of("logs", "telemetria_compartilhada.json");
        byte[] telemetriaRealAntes = Files.exists(telemetriaReal)
            ? Files.readAllBytes(telemetriaReal) : null;

        TelemetriaService telemetria = new TelemetriaService();
        OperacaoTelemetria operacao = TelemetriaService.criarOperacao(
            "Teste de Isolamento", "detalhe", 10L, 1, 0, 0);

        telemetria.finalizarOperacao(operacao, pastaEntrada, "isolamento", "conteudo do relatorio");

        // 1. Nada foi criado na árvore real de relatorios.
        assertFalse(Files.exists(relatoriosReal),
            "Não deve criar relatorios/<tempdir> real: " + relatoriosReal.toAbsolutePath());

        // 2. A telemetria canônica real permanece byte-a-byte intacta (ou ausente).
        if (telemetriaRealAntes == null) {
            assertFalse(Files.exists(telemetriaReal),
                "Telemetria canônica real não deve ser criada por um teste");
        } else {
            assertArrayEquals(telemetriaRealAntes, Files.readAllBytes(telemetriaReal),
                "Telemetria canônica real não pode ser modificada por um teste");
        }

        // 3. A escrita ocorreu, porém na árvore redirecionada (prova que não foi
        //    silenciosamente ignorada).
        Path relatoriosRedirecionado =
            DiretorioBaseKronos.resolver("relatorios", pastaEntrada.getFileName().toString());
        assertTrue(Files.isDirectory(relatoriosRedirecionado),
            "O relatório deveria ter sido gravado na árvore redirecionada: " + relatoriosRedirecionado);
        Path telemetriaRedirecionada =
            DiretorioBaseKronos.resolver("logs", "telemetria_compartilhada.json");
        assertTrue(Files.exists(telemetriaRedirecionada),
            "A telemetria de teste deveria persistir na árvore redirecionada: " + telemetriaRedirecionada);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: estende o guard de isolamento ao {@code cache/} e ao
     * {@code backups/} — os diretórios que guardam horas de tradução já pagas ao LLM e
     * são versionados pelo Git. É regressão de um dano REAL: uma execução da suíte
     * esvaziou o campo {@code traduzido} de 28 caches de produção (86, Gundam 0083/ZZ/
     * 08th), porque {@code TradutorProperties} resolvia o caminho com {@code Path.of}
     * cru, ignorando o redirecionamento de teste.
     *
     * <p>INVARIANTES DO DOMÍNIO: todo caminho operacional RELATIVO ({@code cache},
     * {@code saida}, {@code backups}) resolve SOB a raiz redirecionada; nenhum deles pode
     * cair na árvore real do repositório. Um caminho ABSOLUTO informado pelo usuário
     * continua intocado — é o que preserva o comportamento de produção.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer caminho que escape da raiz de teste
     * reprova, apontando o valor resolvido — sinaliza que o vazamento voltou.
     */
    @Test
    void caminhosDeCacheSaidaEBackupNaoEscapamParaOsDiretoriosReais() {
        Path base = DiretorioBaseKronos.base().toAbsolutePath().normalize();

        TradutorProperties propriedades = new TradutorProperties(
            "entrada", "saida", "cache", 20, null, "en", "pt-br");

        assertTrue(propriedades.resolverDiretorioCache().toAbsolutePath().normalize().startsWith(base),
            "cache configurado como relativo deve ficar sob a raiz de teste, nunca em ./cache versionado. Resolvido: "
                + propriedades.resolverDiretorioCache().toAbsolutePath());
        assertTrue(propriedades.resolverDiretorioSaida().toAbsolutePath().normalize().startsWith(base),
            "saida configurada como relativa deve ficar sob a raiz de teste. Resolvido: "
                + propriedades.resolverDiretorioSaida().toAbsolutePath());

        // Sem cache configurado: cai no default "cache/<anime>" — o caminho que causou o dano.
        TradutorProperties semCache = new TradutorProperties(
            "Anime/legendas_originais", "saida", null, 20, null, "en", "pt-br");
        assertTrue(semCache.resolverDiretorioCache().toAbsolutePath().normalize().startsWith(base),
            "o default cache/<anime> deve ficar sob a raiz de teste. Resolvido: "
                + semCache.resolverDiretorioCache().toAbsolutePath());

        // Backups da Tradução Local: mesma classe de falha, mesmo guard.
        assertTrue(DiretorioBaseKronos.resolver("backups", "traducao-cache")
                .toAbsolutePath().normalize().startsWith(base),
            "backups/traducao-cache deve ficar sob a raiz de teste");

        // Caminho ABSOLUTO do usuário é preservado: prova que o isolamento não quebrou produção.
        Path absoluto = Path.of("/kronos-caminho-absoluto-do-usuario").toAbsolutePath();
        TradutorProperties comAbsoluto = new TradutorProperties(
            "entrada", absoluto.toString(), absoluto.toString(), 20, null, "en", "pt-br");
        assertEquals(absoluto.normalize(), comAbsoluto.resolverDiretorioCache().normalize(),
            "caminho absoluto informado pelo usuário não pode ser reancorado na raiz operacional");
        assertEquals(absoluto.normalize(), comAbsoluto.resolverDiretorioSaida().normalize(),
            "caminho absoluto de saída informado pelo usuário não pode ser reancorado");
    }

    private static final java.util.regex.Pattern LITERAL_OPERACIONAL =
        java.util.regex.Pattern.compile("\"(?:cache|logs|relatorios|backups)\"");
    private static final java.util.regex.Pattern VARIAVEL_DE_CACHE =
        java.util.regex.Pattern.compile("^\\s*(?:dirCache|diretorioCache)\\b");

    /**
     * Verdadeiro quando algum {@code Path.of(...)} do código monta caminho operacional cru: os
     * ARGUMENTOS dele (lidos até o parêntese que fecha, não até o fim do statement) contêm um literal
     * da árvore versionada, ou começam por variável de diretório de cache. Ler até o fim do
     * statement acusava {@code executar(Path.of(pastaPt), DiretorioBaseKronos.resolver("cache"))}.
     */
    private static boolean montaCaminhoOperacionalCru(String codigo) {
        int i = codigo.indexOf("Path.of(");
        while (i >= 0) {
            int inicio = i + "Path.of(".length();
            int nivel = 1;
            int j = inicio;
            while (j < codigo.length() && nivel > 0) {
                char c = codigo.charAt(j);
                if (c == '(') {
                    nivel++;
                } else if (c == ')') {
                    nivel--;
                }
                j++;
            }
            String argumentos = codigo.substring(inicio, Math.max(inicio, j - 1));
            if (LITERAL_OPERACIONAL.matcher(argumentos).find() || VARIAVEL_DE_CACHE.matcher(argumentos).find()) {
                return true;
            }
            i = codigo.indexOf("Path.of(", j);
        }
        return false;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: catraca da CLASSE de falha que já custou dois danos reais — a suíte
     * esvaziando 28 caches de produção ({@code TradutorProperties}) e, em 08/10/2026, uma medição
     * isolada regravando o cache real do karaokê ({@code CacheDoArquivo}). Varre o código de
     * produção e reprova qualquer {@code Path.of} que monte {@code cache}/{@code logs}/
     * {@code relatorios}/{@code backups} sem passar por {@link DiretorioBaseKronos}.
     *
     * <p>INVARIANTES DO DOMÍNIO: a varredura junta o statement inteiro (o {@code Path.of} pode
     * quebrar linha) e ignora comentários; o próprio {@code DiretorioBaseKronos} é o único dono
     * autorizado a montar a raiz.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: calibração antes de varrer — a regex TEM de casar as duas
     * formas doentes reais e NÃO pode casar a forma ancorada nem um {@code Path.of} de caminho do
     * usuário; zero arquivos lidos reprova (alvo vazio não é aprovação).
     */
    @Test
    void nenhumCaminhoOperacionalNasceComPathOfCruNoCodigoDeProducao() throws IOException {
        assertTrue(montaCaminhoOperacionalCru(
            "Path diretorioCache = Path.of(entradaUsuario != null && !entradaUsuario.isBlank() ? entradaUsuario : \"cache\");"),
            "calibração: a forma doente das CLIs tem de casar");
        assertTrue(montaCaminhoOperacionalCru("return Path.of(dirCache, SUBPASTA, base + \".cache.json\");"),
            "calibração: a forma doente do CacheDoArquivo tem de casar");
        assertFalse(montaCaminhoOperacionalCru(
            "return DiretorioBaseKronos.resolver(dirCache, SUBPASTA, base + \".cache.json\");"),
            "calibração: a forma ancorada não pode casar");
        assertFalse(montaCaminhoOperacionalCru("Path entrada = Path.of(request.caminhoOrigem());"),
            "calibração: caminho escolhido pelo usuário não é caminho operacional");
        assertFalse(montaCaminhoOperacionalCru(
            "executar(Path.of(pastaPt), null, DiretorioBaseKronos.resolver(\"cache\"), null)"),
            "calibração: literal FORA dos argumentos do Path.of não é violação (fronteira medida em 08/10)");

        Path raizFontes = Path.of("src", "main", "java");
        java.util.List<String> violacoes = new java.util.ArrayList<>();
        int lidos = 0;
        try (java.util.stream.Stream<Path> arquivos = Files.walk(raizFontes)) {
            for (Path arquivo : (Iterable<Path>) arquivos.filter(p -> p.toString().endsWith(".java"))::iterator) {
                if (arquivo.getFileName().toString().equals("DiretorioBaseKronos.java")) {
                    continue;
                }
                lidos++;
                String semComentarios = Files.readString(arquivo)
                    .replaceAll("(?s)/\\*.*?\\*/", " ")
                    .replaceAll("(?m)//.*$", " ");
                for (String statement : semComentarios.split(";")) {
                    if (montaCaminhoOperacionalCru(statement)) {
                        violacoes.add(raizFontes.relativize(arquivo) + ": " + statement.strip().replaceAll("\\s+", " "));
                    }
                }
            }
        }
        // Piso medido em 08/10/2026: 494 fontes. Abaixo de 400 a varredura não leu a árvore.
        assertTrue(lidos > 400, "alvo vazio não é aprovação: arquivos .java lidos = " + lidos);
        assertTrue(violacoes.isEmpty(),
            "caminho operacional montado com Path.of cru (use DiretorioBaseKronos.resolver):\n  "
                + String.join("\n  ", violacoes));
    }
}
