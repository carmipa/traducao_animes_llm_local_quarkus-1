package org.traducao.projeto.medicao;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.lore.domain.ProvedorContexto;
import org.traducao.projeto.lore.domain.SnapshotContexto;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: gravar no acervo <b>exatamente as retraduções que foram lidas</b> no
 * relatório do ensaio — nem uma linha a mais, nem um texto diferente.
 *
 * <h2>Por que este harness existe separado do que retraduz</h2>
 * O LLM é não-determinístico. Rodar de novo com a autorização ligada produziria traduções
 * DIFERENTES das que eu li par a par — e a leitura seria decorativa. Medido em 26/08/2026: entre
 * duas execuções seguidas do mesmo alvo, {@code sorriro} saiu uma vez consertado e outra vez
 * intacto, e {@code Pezo} saiu {@code Peça} numa e {@code Pezinho} na outra.
 *
 * <p>Então a ordem é: o {@code RetraduzirFalaComDefeitoIT} traduz e ESCREVE o relatório; alguém lê
 * o relatório; este harness grava o que está lá.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Sem {@code -Dkronos.aplicar.retraducao=SIM-ESCREVER-NO-ACERVO} nada é gravado, e o harness
 *       DIZ o que recebeu.</li>
 *   <li>Só linha {@code MELHOROU}. As outras são a razão de o relatório existir.</li>
 *   <li>Cada linha passa DE NOVO pelas mesmas guardas — texto no disco idêntico ao medido, moldura
 *       de tags preservada, nenhuma palavra declarada defeituosa no texto novo, nenhum termo de
 *       lore perdido. Relatório é dado, e dado de arquivo não se obedece sem conferir.</li>
 *   <li>Backup por arquivo, nunca sobrescrito: a segunda execução guardaria o estado já alterado
 *       e o desfazer deixaria de existir.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Relatório ausente, vazio ou sem nenhuma linha aceita termina DECLARANDO, sem gravar. Linha cujo
 * texto no disco divergiu é pulada e contada — nunca gravada por cima.
 *
 * <p>Uso: {@code gradlew test --tests "*AplicarRetraducaoLidaIT*" "-Dkronos.medicao=true"
 * "-Dkronos.acervo=C:\animes\ANIMES-TESTES"
 * "-Dkronos.aplicar.retraducao=SIM-ESCREVER-NO-ACERVO"}
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class AplicarRetraducaoLidaIT {

    private static final String AUTORIZACAO = "SIM-ESCREVER-NO-ACERVO";
    private static final String CHAVE_ESCRITA = "kronos.aplicar.retraducao";
    private static final String LISTA_DECLARADA = "medicao/palavras-defeituosas.txt";
    private static final Path RELATORIO = Path.of("relatorios", "retraducao-desfecho.csv");
    private static final Pattern TAG_ASS = Pattern.compile("\\{[^{}]*}");
    private static final String SUFIXO_BACKUP = ".antes-da-retraducao";

    @Inject
    LeitorLegendaAss leitor;

    @Inject
    EscritorLegendaAss escritor;

    @Inject
    List<ProvedorContexto> provedores;

    /** Uma linha aceita do relatório, já separada em campos. */
    private record Linha(String arquivo, int indice, String palavra, String lore,
                         String antes, String depois) {}

    @Test
    @DisplayName("grava no acervo as retraducoes MELHOROU do relatorio, reconferindo cada guarda")
    void aplicar() throws Exception {
        String recebida = System.getProperty(CHAVE_ESCRITA);
        boolean escrever = AUTORIZACAO.equals(recebida);
        System.out.printf("%n=== APLICAR A RETRADUCAO LIDA — modo %s ===%n",
            escrever ? "!! ESCRITA !!" : "ENSAIO (nada e gravado)");
        if (!escrever) {
            System.out.printf("  autorizacao: -D%s=%s%n", CHAVE_ESCRITA,
                recebida == null ? "(NAO CHEGOU ao JVM de teste)"
                    : "'" + recebida + "' — DIFERENTE do exigido '" + AUTORIZACAO + "'");
        }
        if (!Files.isRegularFile(RELATORIO)) {
            System.out.printf("NAO VERIFICADO: relatorio ausente em %s. Rode antes o "
                + "RetraduzirFalaComDefeitoIT.%n", RELATORIO.toAbsolutePath());
            return;
        }
        List<Path> pastas = AlcanceDaMedicao.pastasDeTraducao();
        if (pastas.isEmpty()) {
            return;
        }
        Set<String> declaradas = carregarDeclaradas();
        if (declaradas.isEmpty()) {
            System.out.printf("NAO VERIFICADO: lista declarada '%s' ausente — sem ela nao ha como "
                + "reconferir se o texto novo continua limpo.%n", LISTA_DECLARADA);
            return;
        }
        Pattern padraoDeclarado = padraoDe(declaradas);
        if (!guardasCalibradas(padraoDeclarado)) {
            return;
        }

        List<Linha> aceitas = lerRelatorio();
        System.out.printf("  relatorio: %s%n  linhas MELHOROU: %d%n",
            RELATORIO.toAbsolutePath(), aceitas.size());
        if (aceitas.isEmpty()) {
            System.out.println("NADA A GRAVAR — o relatorio nao tem uma linha aceita. Isso e "
                + "resultado do julgamento, e nao falta de dado: o arquivo foi lido e tem linhas.");
            return;
        }

        Map<String, SnapshotContexto> loreporId = new LinkedHashMap<>();
        for (ProvedorContexto p : provedores) {
            loreporId.putIfAbsent(p.getId(), SnapshotContexto.de(p));
        }

        // Indexa por NOME de arquivo: o relatorio guarda o nome, e a raiz do acervo pode ter
        // mudado de lugar entre o ensaio e a gravacao.
        Map<String, Path> caminhoPorNome = new LinkedHashMap<>();
        for (Path pasta : pastas) {
            for (Path arquivo : AlcanceDaMedicao.arquivosEntregues(pasta)) {
                caminhoPorNome.putIfAbsent(arquivo.getFileName().toString(), arquivo);
            }
        }

        Map<Path, List<Linha>> porArquivo = new LinkedHashMap<>();
        Map<String, Integer> recusas = new TreeMap<>();
        for (Linha l : aceitas) {
            Path caminho = caminhoPorNome.get(l.arquivo());
            if (caminho == null) {
                recusas.merge("arquivo do relatorio nao esta no acervo atual", 1, Integer::sum);
                continue;
            }
            porArquivo.computeIfAbsent(caminho, k -> new ArrayList<>()).add(l);
        }

        int gravadas = 0;
        int arquivosGravados = 0;
        for (Map.Entry<Path, List<Linha>> e : porArquivo.entrySet()) {
            DocumentoLegenda doc;
            try {
                doc = leitor.ler(e.getKey());
            } catch (RuntimeException ex) {
                recusas.merge("arquivo ilegivel agora", e.getValue().size(), Integer::sum);
                continue;
            }
            List<EventoLegenda> eventos = new ArrayList<>(doc.eventos());
            int trocadas = 0;
            for (Linha l : e.getValue()) {
                String motivo = motivoParaNaoGravar(l, eventos, padraoDeclarado,
                    loreporId.get(l.lore()));
                if (motivo != null) {
                    recusas.merge(motivo, 1, Integer::sum);
                    continue;
                }
                eventos.set(l.indice(), eventos.get(l.indice()).comTexto(l.depois()));
                trocadas++;
            }
            if (trocadas == 0) {
                continue;
            }
            if (escrever) {
                Path backup = e.getKey().resolveSibling(
                    e.getKey().getFileName().toString() + SUFIXO_BACKUP);
                if (!Files.exists(backup)) {
                    Files.copy(e.getKey(), backup);
                }
                escritor.escrever(e.getKey(), new DocumentoLegenda(
                    doc.cabecalho(), eventos, doc.quebraDeLinha(), doc.comBom()));
            }
            arquivosGravados++;
            gravadas += trocadas;
        }

        System.out.printf("%n  falas %s: %d em %d arquivos%n",
            escrever ? "GRAVADAS" : "que SERIAM gravadas", gravadas, arquivosGravados);
        if (recusas.isEmpty()) {
            System.out.println("  recusadas na reconferencia: nenhuma");
        } else {
            System.out.println("  recusadas na reconferencia:");
            recusas.forEach((m, n) -> System.out.printf("   %5d  %s%n", n, m));
        }
        if (escrever) {
            System.out.printf("  backup por arquivo, sufixo '%s' — desfazer e copiar de volta%n",
                SUFIXO_BACKUP);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CASO-CONTROLE (regra 9) das quatro guardas deste gravador, com os
     * casos REAIS que originaram cada uma. Guarda exercitada só no arquivo são pode estar
     * aprovando por não enxergar nada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: imprime qual guarda está cega e devolve {@code false};
     * nenhuma fala é gravada.
     */
    private boolean guardasCalibradas(Pattern padraoDeclarado) {
        SnapshotContexto loreDeControle = new SnapshotContexto("controle", "controle", "", "",
            Set.of("Eighty-Six"), Map.of(), Set.of(), Map.of());

        boolean moldura = !blocosDeTag("{\\blur1.5\\move(1,2,3,4,5,6)}Mechanismo")
            .equals(blocosDeTag("{\\pos(294.8,550.2)}Mecha"))
            && blocosDeTag("{\\an8}A").equals(blocosDeTag("{\\an8}B"));
        boolean declarada = padraoDeclarado.matcher("O assento braos aqui").find()
            && !padraoDeclarado.matcher("O assento e fragil").find();
        boolean disfarce = alvoSobreviveu("efragil", "O assento efrágil, mas eu sou resistente.")
            && !alvoSobreviveu("efragil", "O assento e frágil, mas eu sou resistente.");
        boolean loreOk = "Eighty-Six".equals(termoDeLorePerdido(
                "missao para o Eighty-Six.", "missao para o Oitenta e Seis.", loreDeControle))
            && termoDeLorePerdido("missao para o Eighty-Six.",
                "outra missao para o Eighty-Six.", loreDeControle) == null;

        if (moldura && declarada && disfarce && loreOk) {
            System.out.println("  controle das guardas: moldura · palavra declarada · disfarce "
                + "por acento · termo de lore — as quatro acusam o caso doente e calam no sao");
            return true;
        }
        System.out.printf("GUARDAS REPROVADAS NO CONTROLE — moldura=%s declarada=%s disfarce=%s "
            + "lore=%s. Nada e gravado.%n", moldura, declarada, disfarce, loreOk);
        return false;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reconfere, para uma linha do relatório, TODAS as guardas do ensaio.
     *
     * <p>INVARIANTES DO DOMÍNIO: o relatório é dado de arquivo, e dado de arquivo não se obedece
     * sem conferir. Entre o ensaio e a gravação o acervo pode ter sido alterado por outra tela.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve o motivo da recusa; {@code null} libera.
     */
    private String motivoParaNaoGravar(Linha l, List<EventoLegenda> eventos,
                                       Pattern padraoDeclarado, SnapshotContexto lore) {
        if (l.indice() < 0 || l.indice() >= eventos.size()) {
            return "indice fora do arquivo";
        }
        EventoLegenda ev = eventos.get(l.indice());
        if (!ev.temTexto() || !l.antes().equals(ev.texto())) {
            return "texto no disco ja nao e o que foi medido";
        }
        if (l.depois() == null || l.depois().isBlank()) {
            return "texto novo vazio";
        }
        if (!blocosDeTag(l.antes()).equals(blocosDeTag(l.depois()))) {
            return "moldura de tags diferente";
        }
        Matcher m = padraoDeclarado.matcher(visivel(l.depois()));
        if (m.find()) {
            return "texto novo ainda tem palavra declarada defeituosa";
        }
        if (alvoSobreviveu(l.palavra(), l.depois())) {
            return "palavra alvo sobreviveu, so ganhou acento";
        }
        String perdido = termoDeLorePerdido(l.antes(), l.depois(), lore);
        if (perdido != null) {
            return "perdeu termo de lore";
        }
        return null;
    }

    private List<Linha> lerRelatorio() throws Exception {
        List<Linha> aceitas = new ArrayList<>();
        List<String> linhas = Files.readAllLines(RELATORIO, StandardCharsets.UTF_8);
        for (int i = 1; i < linhas.size(); i++) {
            String[] c = linhas.get(i).split(";", -1);
            if (c.length < 9 || !"MELHOROU".equals(c[4])) {
                continue;
            }
            try {
                aceitas.add(new Linha(c[1], Integer.parseInt(c[2].trim()), c[3], c[5],
                    c[7], c[8]));
            } catch (NumberFormatException e) {
                // Linha malformada e DEFEITO, nao ruido: descartar em silencio aprovaria por
                // cegueira. Ela aparece no placar como recusa.
                System.out.printf("   LINHA MALFORMADA no relatorio (linha %d): %s%n",
                    i + 1, linhas.get(i).substring(0, Math.min(70, linhas.get(i).length())));
            }
        }
        return aceitas;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: pega o disfarce — a palavra defeituosa que continua ali, só com acento.
     *
     * <p>O prejuízo, medido em 26/08/2026: {@code "O assento efragil"} voltou como
     * {@code "O assento efrágil"}. Não é conserto, é a mesma palavra inventada com um acento em
     * cima. Passou por todas as outras guardas porque {@code efrágil} não está na lista declarada
     * (que tem {@code efragil}) e porque o dicionário aceita a forma acentuada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: argumento nulo devolve {@code false} — a linha segue para
     * as outras guardas e nunca é liberada por este caminho.
     */
    private static boolean alvoSobreviveu(String palavra, String novo) {
        if (palavra == null || novo == null || palavra.isBlank()) {
            return false;
        }
        return Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(semDiacritico(palavra))
                + "(?![\\p{L}\\p{N}])")
            .matcher(semDiacritico(visivel(novo)))
            .find();
    }

    private static String semDiacritico(String texto) {
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").toLowerCase(java.util.Locale.ROOT);
    }

    private static String visivel(String texto) {
        return TAG_ASS.matcher(texto).replaceAll(" ")
            .replace("\\N", " ").replace("\\n", " ").replace("\\h", " ");
    }

    private static List<String> blocosDeTag(String texto) {
        List<String> blocos = new ArrayList<>();
        if (texto == null) {
            return blocos;
        }
        Matcher m = TAG_ASS.matcher(texto);
        while (m.find()) {
            blocos.add(m.group());
        }
        return blocos;
    }

    private static String termoDeLorePerdido(String antes, String depois, SnapshotContexto lore) {
        if (lore == null || antes == null || depois == null) {
            return null;
        }
        Set<String> termos = new LinkedHashSet<>();
        if (lore.termosProtegidos() != null) {
            termos.addAll(lore.termosProtegidos());
        }
        if (lore.correcoesTerminologia() != null) {
            termos.addAll(lore.correcoesTerminologia().values());
        }
        for (String termo : termos) {
            if (termo != null && termo.length() >= 3
                && antes.contains(termo) && !depois.contains(termo)) {
                return termo;
            }
        }
        return null;
    }

    private Set<String> carregarDeclaradas() throws Exception {
        Set<String> palavras = new LinkedHashSet<>();
        try (InputStream entrada =
                 getClass().getClassLoader().getResourceAsStream(LISTA_DECLARADA)) {
            if (entrada == null) {
                return palavras;
            }
            try (BufferedReader leitura =
                     new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
                String linha;
                while ((linha = leitura.readLine()) != null) {
                    String limpa = linha.trim();
                    if (!limpa.isEmpty() && !limpa.startsWith("#")) {
                        palavras.add(limpa);
                    }
                }
            }
        }
        return palavras;
    }

    private static Pattern padraoDe(Set<String> declaradas) {
        String alternativa = declaradas.stream().map(Pattern::quote)
            .collect(java.util.stream.Collectors.joining("|"));
        return Pattern.compile("(?<![\\p{L}\\p{N}])(" + alternativa + ")(?![\\p{L}\\p{N}])");
    }
}
