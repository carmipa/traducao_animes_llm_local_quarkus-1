package org.traducao.projeto.lore;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.traducao.projeto.lore.domain.ProvedorContexto;
import org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore;
import org.traducao.projeto.lore.infrastructure.CatalogoLoreYaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.comments.CommentLine;
import org.yaml.snakeyaml.comments.CommentType;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * PROPÓSITO DE NEGÓCIO: FASE 2 da lore em SQLite — gera, a partir do catálogo VIVO, os arquivos SQL
 * que vão substituir o {@code lore.yaml} ({@code esquema.sql}, {@code obras.lst} e um
 * {@code obras/<id>.sql} por obra), migra para eles TODA a cicatriz escrita em comentário no YAML e
 * prova, no mesmo experimento, que os arquivos relidos num SQLite em memória reproduzem cada obra
 * campo a campo.
 *
 * <h2>Por que gerar em vez de escrever</h2>
 * São 69 obras e 994 KB. Transcrever à mão é como as duas cópias da terminologia divergiram em
 * agosto. O dado nasce da fonte viva; o que é humano — a cicatriz — é carregado pelo parser do próprio
 * YAML, com a posição de cada comentário.
 *
 * <h2>Os comentários, medidos em 09/10/2026</h2>
 * <pre>
 *   linhas de comentário inteiras ...... 521 (140 no preâmbulo, 12 antes de "revisao:", 369 em obras)
 *   comentários na linha do dado ....... 152 (a catraca de cicatriz do YAML não os contava)
 *   vistos pelo parser (SnakeYAML) ..... 673 = 521 + 152, nenhuma linha sem correspondente
 * </pre>
 * Cada um vai para perto do dado que explica: bloco antes do {@code INSERT}, comentário de linha no
 * fim da linha do {@code INSERT}, e a linha inteira que o parser anexou ao nó anterior logo depois
 * dele. Sem âncora correspondente no SQL, vai para o topo do arquivo da obra com a linha de origem.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li><b>Ida e volta antes de escrever.</b> Os arquivos são carregados num SQLite em memória com
 *       chave estrangeira imposta, rematerializados por uma implementação PRÓPRIA deste teste e
 *       comparados com o catálogo vivo pelo manifesto completo. Qualquer diferença aborta sem
 *       escrever arquivo aproveitável.</li>
 *   <li><b>Nenhum comentário perdido.</b> O arquivo é relido depois de gravado e os comentários
 *       migrados são conferidos por texto, um a um, contra os do YAML (identidade, não contagem).</li>
 *   <li>A ordem que o consumidor enxerga é preservada pela coluna {@code ordem} (pares e
 *       equivalências: medido que 4 obras têm pares fora de ordem e 11 listas de equivalência não
 *       estão ordenadas). Conjuntos e mapas saem ordenados.</li>
 *   <li>Saída em {@code build/tmp/lore/}. Promover para {@code src/main/resources/lore/} é ato
 *       deliberado de quem migra, nunca efeito de rodar a suíte.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Catálogo vazio reprova como NÃO VERIFICADO. Divergência na ida e volta, comentário perdido ou
 * duplicado, ou restrição do esquema recusando o dado reprovam nomeando obra e campo, e os arquivos
 * não são gravados.
 */
@QuarkusTest
@DisplayName("FASE 2 (lore em SQLite): gera os arquivos SQL com a cicatriz e prova a ida e volta")
class GeradorLoreSqlIT {

    static final Path SAIDA = Path.of("build", "tmp", "lore");
    static final Path VERSIONADO = Path.of("src", "main", "resources", "lore");
    static final String FLAG_REGRAVAR = "kronos.lore.sql.regravar";
    static final String GERADO = "-- [gerado] ";

    static final String ESQUEMA = """
        PRAGMA foreign_keys = ON;

        -- [gerado] Identidade da obra. 70 ids: sem_lore só existe na tradução e macross_dyrl só na revisão.
        CREATE TABLE obra (
          id TEXT PRIMARY KEY CHECK (length(id) > 0 AND id NOT GLOB '*[^a-z0-9_]*')
        ) STRICT;

        -- [gerado] Lado da TRADUÇÃO. O prompt vira o contextoHash do cache: \\r proibido, porque o literal SQL
        -- [gerado] não normaliza quebra de linha como o YAML normalizava.
        CREATE TABLE lore_traducao (
          obra_id          TEXT PRIMARY KEY REFERENCES obra(id),
          nome             TEXT NOT NULL CHECK (length(trim(nome)) > 0),
          aparece_na_lista INTEGER NOT NULL CHECK (aparece_na_lista IN (0, 1)),
          prompt           TEXT NOT NULL CHECK (length(prompt) > 0 AND instr(prompt, char(13)) = 0)
        ) STRICT;

        %s
        -- [gerado] Lado da REVISÃO. O prompt é diferente do da tradução de propósito.
        CREATE TABLE lore_revisao (
          obra_id TEXT PRIMARY KEY REFERENCES obra(id),
          nome    TEXT NOT NULL CHECK (length(trim(nome)) > 0),
          prompt  TEXT NOT NULL CHECK (length(prompt) > 0 AND instr(prompt, char(13)) = 0)
        ) STRICT;

        -- [gerado] Terminologia UMA vez por obra: tradução e revisão recebem o mesmo conjunto e o mesmo mapa.
        CREATE TABLE termo_protegido (
          obra_id TEXT NOT NULL REFERENCES obra(id),
          termo   TEXT NOT NULL CHECK (length(trim(termo)) > 0),
          PRIMARY KEY (obra_id, termo)
        ) STRICT, WITHOUT ROWID;

        CREATE TABLE correcao_terminologia (
          obra_id    TEXT NOT NULL REFERENCES obra(id),
          forma_ruim TEXT NOT NULL,
          canonico   TEXT NOT NULL,
          CHECK (forma_ruim <> canonico),
          PRIMARY KEY (obra_id, forma_ruim)
        ) STRICT, WITHOUT ROWID;

        -- [gerado] Só tradução.
        CREATE TABLE traducao_obrigatoria (
          obra_id TEXT NOT NULL REFERENCES obra(id),
          origem  TEXT NOT NULL,
          destino TEXT NOT NULL,
          PRIMARY KEY (obra_id, origem)
        ) STRICT, WITHOUT ROWID;

        -- [gerado] A ordem dos pares é a que o consumidor itera; "ordem" a preserva.
        CREATE TABLE par_inconfundivel (
          obra_id TEXT NOT NULL REFERENCES obra(id),
          ordem   INTEGER NOT NULL,
          a       TEXT NOT NULL,
          b       TEXT NOT NULL,
          CHECK (a <> b),
          PRIMARY KEY (obra_id, ordem)
        ) STRICT, WITHOUT ROWID;

        -- [gerado] O mesmo apelido em duas obras já derruba o arranque (colisão de identidade); UNIQUE antecipa.
        CREATE TABLE apelido_pasta (
          obra_id TEXT NOT NULL REFERENCES obra(id),
          apelido TEXT NOT NULL UNIQUE,
          PRIMARY KEY (obra_id, apelido)
        ) STRICT, WITHOUT ROWID;

        -- [gerado] Só revisão.
        CREATE TABLE equivalencia_aceita (
          obra_id TEXT NOT NULL REFERENCES obra(id),
          termo   TEXT NOT NULL,
          ordem   INTEGER NOT NULL,
          forma   TEXT NOT NULL,
          PRIMARY KEY (obra_id, termo, ordem)
        ) STRICT, WITHOUT ROWID;
        """;

    @Inject
    List<ProvedorContexto> catalogoTraducao;

    @Inject
    List<ProvedorPromptRevisaoLore> catalogoRevisao;

    /** Onde um comentário vai em relação à instrução: antes, no fim da linha, ou logo depois. */
    enum Posicao { ANTES, FIM_DA_LINHA, DEPOIS }

    /**
     * PROPÓSITO DE NEGÓCIO: um comentário do YAML com o endereço da instrução SQL que ele explica.
     * <p>INVARIANTES DO DOMÍNIO: {@code linha} é a linha de origem no YAML (1-based) e ordena os
     * comentários que caem na mesma instrução; {@code texto} é tudo depois do {@code #}, intacto.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    record Comentario(int linha, String texto, Posicao posicao, String destino) {
    }

    @Test
    @DisplayName("gera esquema, lista e uma obra por arquivo; relê no SQLite e confere com o catálogo vivo")
    void gerarEProvarIdaEVolta() throws Exception {
        assertFalse(catalogoTraducao.isEmpty(), "NÃO VERIFICADO: nenhum ProvedorContexto no CDI");
        assertFalse(catalogoRevisao.isEmpty(), "NÃO VERIFICADO: nenhum ProvedorPromptRevisaoLore no CDI");

        String yaml = lerRecurso(CatalogoLoreYaml.RECURSO);
        List<Comentario> comentarios = comentariosDoYaml(yaml);
        conferirCoberturaDoParser(yaml, comentarios);

        Map<String, ProvedorContexto> traducao = new TreeMap<>();
        catalogoTraducao.forEach(p -> traducao.put(p.getId(), p));
        Map<String, ProvedorPromptRevisaoLore> revisao = new TreeMap<>();
        catalogoRevisao.forEach(r -> revisao.put(r.getId(), r));
        Set<String> ids = new TreeSet<>(traducao.keySet());
        ids.addAll(revisao.keySet());

        Map<String, List<Comentario>> porDestino = new HashMap<>();
        for (Comentario c : comentarios) {
            porDestino.computeIfAbsent(c.destino(), k -> new ArrayList<>()).add(c);
        }
        porDestino.values().forEach(l -> l.sort(Comparator.comparingInt(Comentario::linha)));

        String esquema = GERADO + "ESQUEMA DA LORE DO KRONOS — gerado por GeradorLoreSqlIT a partir do catálogo vivo.\n"
            + GERADO + "Ordem de carga: este arquivo, depois cada id de obras.lst (obras/<id>.sql), numa transação.\n"
            + GERADO + "Os comentários sem a marca [gerado] são CICATRIZ migrada do lore.yaml, com a medição que a criou.\n\n"
            + bloco(porDestino.remove("esquema:preambulo"))
            + ESQUEMA.formatted(bloco(porDestino.remove("esquema:revisao")));
        Map<String, String> arquivos = new LinkedHashMap<>();
        for (String id : ids) {
            arquivos.put(id, sqlDaObra(id, traducao.get(id), revisao.get(id), porDestino));
        }
        StringBuilder lista = new StringBuilder();
        ids.forEach(id -> lista.append(id).append('\n'));

        // Comentário que ficou em porDestino apontava para instrução que não existe: vai para o topo
        // da obra, com a linha de origem. Se nem a obra existir, é defeito do gerador.
        List<String> semObra = new ArrayList<>();
        for (Map.Entry<String, List<Comentario>> e : porDestino.entrySet()) {
            semObra.add(e.getKey() + " <- linhas " + e.getValue().stream().map(Comentario::linha).toList());
        }
        assertTrue(semObra.isEmpty(), () -> "comentários sem obra de destino:\n  " + String.join("\n  ", semObra));

        // IDA E VOLTA no SQLite, com uma rematerialização própria deste teste.
        List<String> divergencias = new ArrayList<>();
        int[] contagem = new int[1];
        try (Connection c = fonte().getConnection()) {
            c.setAutoCommit(false);
            executar(c, "esquema.sql", esquema);
            for (Map.Entry<String, String> e : arquivos.entrySet()) {
                executar(c, "obras/" + e.getKey() + ".sql", e.getValue());
            }
            c.commit();
            verificarIntegridade(c);
            List<ProvedorContexto> relidaTraducao = materializarTraducao(c);
            List<ProvedorPromptRevisaoLore> relidaRevisao = materializarRevisao(c);
            divergencias.addAll(ManifestoCompletoLoreIT.divergencias(
                ManifestoCompletoLoreIT.linhasDoCatalogo(catalogoTraducao, catalogoRevisao),
                ManifestoCompletoLoreIT.linhasDoCatalogo(relidaTraducao, relidaRevisao)));
            contagem[0] = relidaTraducao.size() + relidaRevisao.size();
        }
        assertTrue(divergencias.isEmpty(), () -> "o SQL relido NÃO reproduz o catálogo vivo — nada foi gravado.\n  "
            + String.join("\n  ", divergencias));

        // Grava e RELÊ: a conferência dos comentários é feita sobre o que está em disco.
        Files.createDirectories(SAIDA.resolve("obras"));
        try (var antigos = Files.list(SAIDA.resolve("obras"))) {
            for (Path p : antigos.toList()) {
                Files.delete(p);
            }
        }
        Files.writeString(SAIDA.resolve("esquema.sql"), esquema, StandardCharsets.UTF_8);
        Files.writeString(SAIDA.resolve("obras.lst"), lista.toString(), StandardCharsets.UTF_8);
        for (Map.Entry<String, String> e : arquivos.entrySet()) {
            Files.writeString(SAIDA.resolve("obras").resolve(e.getKey() + ".sql"), e.getValue(), StandardCharsets.UTF_8);
        }
        List<String> relidos = new ArrayList<>(comentariosMigradosEm(Files.readString(SAIDA.resolve("esquema.sql"), StandardCharsets.UTF_8)));
        for (String id : ids) {
            relidos.addAll(comentariosMigradosEm(Files.readString(SAIDA.resolve("obras").resolve(id + ".sql"), StandardCharsets.UTF_8)));
        }
        Map<String, Integer> doYaml = multiconjunto(comentarios.stream().map(Comentario::texto).toList());
        Map<String, Integer> doSql = multiconjunto(relidos);
        assertEquals(comentarios.size(), relidos.size(), "quantidade de comentários migrados difere do YAML");
        assertEquals(doYaml, doSql, "os TEXTOS dos comentários migrados não batem com os do YAML");

        long bytes = Files.size(SAIDA.resolve("esquema.sql"));
        for (String id : ids) {
            bytes += Files.size(SAIDA.resolve("obras").resolve(id + ".sql"));
        }

        // Enquanto o YAML e o SQL convivem (fases 2 a 4), o versionado TEM de ser exatamente o que o
        // catálogo vivo gera: duas fontes da mesma lore só não divergem se alguém acusar a diferença.
        Map<String, String> gerados = new TreeMap<>();
        gerados.put("esquema.sql", esquema);
        gerados.put("obras.lst", lista.toString());
        arquivos.forEach((id, sql) -> gerados.put("obras/" + id + ".sql", sql));
        if (Boolean.getBoolean(FLAG_REGRAVAR)) {
            promover(gerados);
        } else {
            List<String> diferencas = diferencasDoVersionado(gerados);
            assertTrue(diferencas.isEmpty(), () -> "o SQL versionado em " + VERSIONADO + " não é o que o catálogo vivo "
                + "gera (" + diferencas.size() + " arquivo(s)). Se a lore mudou de propósito, regrave com -D"
                + FLAG_REGRAVAR + "=true no mesmo commit.\n  " + String.join("\n  ", diferencas));
        }
        System.out.println();
        System.out.println("=== LORE EM SQL (FASE 2) ===");
        System.out.println("  obras ............ " + ids.size() + " (provedores relidos: " + contagem[0] + ")");
        System.out.println("  comentários ...... " + relidos.size() + " migrados e relidos do disco");
        System.out.println("  bytes ............ " + bytes);
        System.out.println("  ida e volta ...... OK pelo manifesto completo");
        System.out.println("  escrito em ....... " + SAIDA.toAbsolutePath());
    }

    // ------------------------------------------------------------------ comentários do YAML

    /**
     * PROPÓSITO DE NEGÓCIO: lê os comentários do YAML pelo parser, com o endereço do nó a que cada
     * um pertence, e o traduz para o destino no SQL.
     * <p>INVARIANTES DO DOMÍNIO: linha em branco não é comentário; o texto vai intacto.
     * <p>COMPORTAMENTO EM CASO DE FALHA: arquivo que o parser não entende lança.
     */
    static List<Comentario> comentariosDoYaml(String yaml) {
        LoaderOptions opcoes = new LoaderOptions();
        opcoes.setProcessComments(true);
        opcoes.setCodePointLimit(50 * 1024 * 1024);
        Node raiz = new Yaml(opcoes).compose(new StringReader(yaml));
        String[] linhas = yaml.split("\n", -1);
        List<Comentario> saida = new ArrayList<>();
        if (!(raiz instanceof MappingNode topo)) {
            fail("lore.yaml não é um mapa");
            return saida;
        }
        for (NodeTuple secao : topo.getValue()) {
            String nomeSecao = ((ScalarNode) secao.getKeyNode()).getValue();
            String lado = "obras".equals(nomeSecao) ? "traducao" : "revisao";
            String destinoSecao = "obras".equals(nomeSecao) ? "esquema:preambulo" : "esquema:revisao";
            colher(secao.getKeyNode(), destinoSecao, linhas, saida);
            colher(secao.getValueNode(), destinoSecao, linhas, saida);
            if (!(secao.getValueNode() instanceof SequenceNode obras)) {
                continue;
            }
            for (Node no : obras.getValue()) {
                MappingNode obra = (MappingNode) no;
                String id = escalar(obra, "id");
                colher(obra, destino(id, lado, "id", null), linhas, saida);
                for (NodeTuple campo : obra.getValue()) {
                    String nome = ((ScalarNode) campo.getKeyNode()).getValue();
                    colher(campo.getKeyNode(), destino(id, lado, nome, null), linhas, saida);
                    Node valor = campo.getValueNode();
                    if (valor instanceof ScalarNode) {
                        colher(valor, destino(id, lado, nome, null), linhas, saida);
                    } else if (valor instanceof SequenceNode lista) {
                        colher(lista, destino(id, lado, nome, null), linhas, saida);
                        int i = 0;
                        for (Node item : lista.getValue()) {
                            String chave = "paresInconfundiveis".equals(nome) ? String.valueOf(i)
                                : item instanceof ScalarNode s ? s.getValue() : String.valueOf(i);
                            colherProfundo(item, destino(id, lado, nome, chave), linhas, saida);
                            i++;
                        }
                    } else if (valor instanceof MappingNode mapa) {
                        colher(mapa, destino(id, lado, nome, null), linhas, saida);
                        for (NodeTuple entrada : mapa.getValue()) {
                            String chave = ((ScalarNode) entrada.getKeyNode()).getValue();
                            if ("equivalenciasAceitas".equals(nome)) {
                                chave = chave.toLowerCase(Locale.ROOT);
                            }
                            colherProfundo(entrada.getKeyNode(), destino(id, lado, nome, chave), linhas, saida);
                            colherProfundo(entrada.getValueNode(), destino(id, lado, nome, chave), linhas, saida);
                        }
                    }
                }
            }
        }
        return saida;
    }

    private static String escalar(MappingNode m, String chave) {
        for (NodeTuple t : m.getValue()) {
            if (t.getKeyNode() instanceof ScalarNode k && chave.equals(k.getValue())
                && t.getValueNode() instanceof ScalarNode v) {
                return v.getValue();
            }
        }
        fail("obra sem \"" + chave + "\" no lore.yaml");
        return null;
    }

    /** Destino no SQL: "obra|lado|campo|chave". Chave nula = a primeira instrução daquele campo. */
    private static String destino(String id, String lado, String campo, String chave) {
        return id + "|" + lado + "|" + campo + "|" + (chave == null ? "" : chave);
    }

    private static void colherProfundo(Node n, String destino, String[] linhas, List<Comentario> saida) {
        colher(n, destino, linhas, saida);
        if (n instanceof SequenceNode s) {
            s.getValue().forEach(x -> colherProfundo(x, destino, linhas, saida));
        } else if (n instanceof MappingNode m) {
            m.getValue().forEach(t -> {
                colherProfundo(t.getKeyNode(), destino, linhas, saida);
                colherProfundo(t.getValueNode(), destino, linhas, saida);
            });
        }
    }

    private static void colher(Node n, String destino, String[] linhas, List<Comentario> saida) {
        juntar(n.getBlockComments(), Posicao.ANTES, destino, linhas, saida);
        juntar(n.getInLineComments(), Posicao.FIM_DA_LINHA, destino, linhas, saida);
        juntar(n.getEndComments(), Posicao.DEPOIS, destino, linhas, saida);
    }

    private static void juntar(List<CommentLine> cs, Posicao pedida, String destino, String[] linhas,
                               List<Comentario> saida) {
        if (cs == null) {
            return;
        }
        for (CommentLine c : cs) {
            if (c.getCommentType() == CommentType.BLANK_LINE) {
                continue;
            }
            int linha = c.getStartMark().getLine() + 1;
            Posicao posicao = pedida;
            // O parser anexa ao nó ANTERIOR, como "inline", linhas de comentário INTEIRAS que vêm logo
            // depois dele (medido: 24). No YAML elas estavam abaixo do dado — vão para depois da instrução.
            if (posicao == Posicao.FIM_DA_LINHA && linhas[linha - 1].stripLeading().startsWith("#")) {
                posicao = Posicao.DEPOIS;
            }
            saida.add(new Comentario(linha, c.getValue(), posicao, destino));
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: prova que o parser viu TODA linha de comentário do texto — sem isto a
     * migração poderia perder cicatriz que o parser não associou a nó nenhum.
     * <p>INVARIANTES DO DOMÍNIO: toda linha cujo primeiro caractere não branco é {@code #} tem de
     * estar entre as linhas de origem colhidas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: reprova listando as linhas perdidas.
     */
    private static void conferirCoberturaDoParser(String yaml, List<Comentario> colhidos) {
        Set<Integer> vistas = new TreeSet<>();
        colhidos.forEach(c -> vistas.add(c.linha()));
        String[] linhas = yaml.split("\n", -1);
        List<Integer> perdidas = new ArrayList<>();
        int total = 0;
        for (int i = 0; i < linhas.length; i++) {
            if (linhas[i].stripLeading().startsWith("#")) {
                total++;
                if (!vistas.contains(i + 1)) {
                    perdidas.add(i + 1);
                }
            }
        }
        assertTrue(total > 0, "NÃO VERIFICADO: nenhuma linha de comentário no lore.yaml");
        assertTrue(perdidas.isEmpty(), "o parser não colheu " + perdidas.size() + " linha(s) de comentário: " + perdidas);
    }

    // ------------------------------------------------------------------ SQL por obra

    /**
     * PROPÓSITO DE NEGÓCIO: o texto SQL de UMA obra, na ordem em que o YAML declara os campos, com a
     * cicatriz de cada dado ao lado dele.
     * <p>INVARIANTES DO DOMÍNIO: uma instrução por linha de dado; o prompt vai inteiro num literal,
     * com o apóstrofo dobrado e nada mais escapado. Os comentários consumidos saem de
     * {@code porDestino}; o que sobra da obra vai para o topo, com a linha de origem.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; a ida e volta é quem julga o resultado.
     */
    static String sqlDaObra(String id, ProvedorContexto t, ProvedorPromptRevisaoLore r,
                            Map<String, List<Comentario>> porDestino) {
        List<String> corpo = new ArrayList<>();
        instrucao(corpo, porDestino, List.of(destino(id, "traducao", "id", null), destino(id, "revisao", "id", null)),
            "INSERT INTO obra VALUES (" + q(id) + ");");
        if (t != null) {
            for (String apelido : new TreeSet<>(t.apelidosPasta())) {
                instrucao(corpo, porDestino, alvos(id, "apelidosPasta", apelido, primeira(corpo, "apelido_pasta")),
                    "INSERT INTO apelido_pasta VALUES (" + q(id) + ", " + q(apelido) + ");");
            }
            for (String termo : new TreeSet<>(t.termosProtegidos())) {
                instrucao(corpo, porDestino, alvos(id, "termosProtegidos", termo, primeira(corpo, "termo_protegido")),
                    "INSERT INTO termo_protegido VALUES (" + q(id) + ", " + q(termo) + ");");
            }
            int ordem = 0;
            for (List<String> par : t.paresInconfundiveis()) {
                instrucao(corpo, porDestino, alvos(id, "paresInconfundiveis", String.valueOf(ordem), primeira(corpo, "par_inconfundivel")),
                    "INSERT INTO par_inconfundivel VALUES (" + q(id) + ", " + ordem + ", " + q(par.get(0)) + ", " + q(par.get(1)) + ");");
                ordem++;
            }
            for (Map.Entry<String, String> e : new TreeMap<>(t.traducoesObrigatorias()).entrySet()) {
                instrucao(corpo, porDestino, alvos(id, "traducoesObrigatorias", e.getKey(), primeira(corpo, "traducao_obrigatoria")),
                    "INSERT INTO traducao_obrigatoria VALUES (" + q(id) + ", " + q(e.getKey()) + ", " + q(e.getValue()) + ");");
            }
        }
        Map<String, String> correcoes = t != null ? t.correcoesTerminologia() : r.correcoesTerminologia();
        for (Map.Entry<String, String> e : new TreeMap<>(correcoes).entrySet()) {
            boolean primeiraCorrecao = primeira(corpo, "correcao_terminologia");
            List<String> alvos = new ArrayList<>(alvosLado(id, "traducao", "correcoesTerminologia", e.getKey(), primeiraCorrecao));
            alvos.addAll(alvosLado(id, "revisao", "correcoesTerminologia", e.getKey(), primeiraCorrecao));
            instrucao(corpo, porDestino, alvos,
                "INSERT INTO correcao_terminologia VALUES (" + q(id) + ", " + q(e.getKey()) + ", " + q(e.getValue()) + ");");
        }
        if (t != null) {
            instrucao(corpo, porDestino, List.of(destino(id, "traducao", "nome", null),
                    destino(id, "traducao", "apareceNaLista", null), destino(id, "traducao", "prompt", null)),
                "INSERT INTO lore_traducao VALUES (" + q(id) + ", " + q(t.getNomeExibicao()) + ", "
                    + (t.apareceNaListaDeObras() ? 1 : 0) + ",\n" + q(t.obterPromptSistema()) + ");");
        }
        if (r != null) {
            instrucao(corpo, porDestino, List.of(destino(id, "revisao", "nome", null), destino(id, "revisao", "prompt", null)),
                "INSERT INTO lore_revisao VALUES (" + q(id) + ", " + q(r.getNomeExibicao()) + ",\n"
                    + q(r.obterPromptSistema()) + ");");
            for (Map.Entry<String, List<String>> e : new TreeMap<>(r.equivalenciasAceitas()).entrySet()) {
                int ordem = 0;
                for (String forma : e.getValue()) {
                    boolean primeiroDoTermo = ordem == 0;
                    List<String> alvos = new ArrayList<>();
                    if (primeira(corpo, "equivalencia_aceita")) {
                        alvos.add(destino(id, "revisao", "equivalenciasAceitas", null));
                    }
                    if (primeiroDoTermo) {
                        alvos.add(destino(id, "revisao", "equivalenciasAceitas", e.getKey()));
                    }
                    instrucao(corpo, porDestino, alvos, "INSERT INTO equivalencia_aceita VALUES (" + q(id) + ", "
                        + q(e.getKey()) + ", " + ordem + ", " + q(forma) + ");");
                    ordem++;
                }
            }
        }

        // O que sobrou para esta obra não achou instrução: topo do arquivo, com a linha de origem.
        List<String> topo = new ArrayList<>();
        topo.add(GERADO + "obra " + id);
        for (Map.Entry<String, List<Comentario>> e : new ArrayList<>(porDestino.entrySet())) {
            if (e.getKey().startsWith(id + "|")) {
                for (Comentario c : e.getValue()) {
                    topo.add(GERADO + "cicatriz sem âncora (lore.yaml:" + c.linha() + ", " + e.getKey() + ")");
                    topo.add("--" + c.texto());
                }
                porDestino.remove(e.getKey());
            }
        }
        topo.addAll(corpo);
        return String.join("\n", topo) + "\n";
    }

    private static List<String> alvos(String id, String campo, String chave, boolean primeiraDoCampo) {
        return alvosLado(id, "traducao", campo, chave, primeiraDoCampo);
    }

    private static List<String> alvosLado(String id, String lado, String campo, String chave, boolean primeiraDoCampo) {
        List<String> a = new ArrayList<>();
        if (primeiraDoCampo) {
            a.add(destino(id, lado, campo, null));
        }
        a.add(destino(id, lado, campo, chave));
        return a;
    }

    /** Verdadeiro se nenhuma instrução daquela tabela foi escrita ainda — o comentário do CAMPO vai nela. */
    private static boolean primeira(List<String> corpo, String tabela) {
        String marca = "INSERT INTO " + tabela + " ";
        return corpo.stream().noneMatch(l -> l.startsWith(marca));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: escreve uma instrução cercada dos comentários que a explicam.
     * <p>INVARIANTES DO DOMÍNIO: ANTES vira linhas acima; FIM_DA_LINHA vai no fim da última linha da
     * instrução; DEPOIS vira linhas abaixo. Ordem pela linha de origem.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    private static void instrucao(List<String> corpo, Map<String, List<Comentario>> porDestino, List<String> destinos,
                                  String sql) {
        List<Comentario> meus = new ArrayList<>();
        for (String d : destinos) {
            List<Comentario> l = porDestino.remove(d);
            if (l != null) {
                meus.addAll(l);
            }
        }
        meus.sort(Comparator.comparingInt(Comentario::linha));
        for (Comentario c : meus) {
            if (c.posicao() == Posicao.ANTES) {
                corpo.add("--" + c.texto());
            }
        }
        StringBuilder fim = new StringBuilder();
        for (Comentario c : meus) {
            if (c.posicao() == Posicao.FIM_DA_LINHA) {
                fim.append("  --").append(c.texto());
            }
        }
        corpo.add(sql + fim);
        for (Comentario c : meus) {
            if (c.posicao() == Posicao.DEPOIS) {
                corpo.add("--" + c.texto());
            }
        }
    }

    private static String bloco(List<Comentario> cs) {
        if (cs == null || cs.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        cs.stream().sorted(Comparator.comparingInt(Comentario::linha)).forEach(c -> sb.append("--").append(c.texto()).append('\n'));
        return sb.toString();
    }

    static String q(String s) {
        return "'" + s.replace("'", "''") + "'";
    }

    // ------------------------------------------------------------------ versionado

    /**
     * PROPÓSITO DE NEGÓCIO: promove os arquivos gerados para o recurso versionado — o único jeito de
     * o SQL da lore mudar enquanto o YAML existe.
     * <p>INVARIANTES DO DOMÍNIO: só escreve depois da ida e volta e da conferência de comentários
     * (chamado no fim do teste); apaga de {@code obras/} só os {@code .sql} que não estão mais na lista.
     * <p>COMPORTAMENTO EM CASO DE FALHA: erro de escrita propaga {@link IOException}.
     */
    private static void promover(Map<String, String> gerados) throws IOException {
        Path obras = VERSIONADO.resolve("obras");
        Files.createDirectories(obras);
        try (var existentes = Files.list(obras)) {
            for (Path p : existentes.toList()) {
                String nome = "obras/" + p.getFileName();
                if (nome.endsWith(".sql") && !gerados.containsKey(nome)) {
                    Files.delete(p);
                }
            }
        }
        for (Map.Entry<String, String> e : gerados.entrySet()) {
            Files.writeString(VERSIONADO.resolve(e.getKey()), e.getValue(), StandardCharsets.UTF_8);
        }
        System.out.println("[lore-sql] promovido para " + VERSIONADO + " (" + gerados.size() + " arquivos)");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: lista os arquivos em que o versionado difere do gerado.
     * <p>INVARIANTES DO DOMÍNIO: compara texto exato; arquivo de obra a mais no disco também é diferença.
     * <p>COMPORTAMENTO EM CASO DE FALHA: versionado ausente aparece como diferença, nunca como aprovação.
     */
    private static List<String> diferencasDoVersionado(Map<String, String> gerados) throws IOException {
        List<String> d = new ArrayList<>();
        for (Map.Entry<String, String> e : gerados.entrySet()) {
            Path p = VERSIONADO.resolve(e.getKey());
            if (!Files.exists(p)) {
                d.add("AUSENTE " + e.getKey());
            } else if (!Files.readString(p, StandardCharsets.UTF_8).equals(e.getValue())) {
                d.add("DIFERENTE " + e.getKey());
            }
        }
        Path obras = VERSIONADO.resolve("obras");
        if (Files.isDirectory(obras)) {
            try (var existentes = Files.list(obras)) {
                for (Path p : existentes.toList()) {
                    if (!gerados.containsKey("obras/" + p.getFileName())) {
                        d.add("A MAIS " + "obras/" + p.getFileName());
                    }
                }
            }
        }
        return d;
    }

    // ------------------------------------------------------------------ releitura

    /**
     * PROPÓSITO DE NEGÓCIO: extrai de um arquivo SQL GRAVADO os comentários migrados (os que não
     * levam a marca {@code [gerado]}), ignorando o que está dentro de literal.
     * <p>INVARIANTES DO DOMÍNIO: anda caractere a caractere respeitando aspas simples (com {@code ''}
     * dobrado); um {@code --} fora de literal começa comentário até o fim da linha.
     * <p>COMPORTAMENTO EM CASO DE FALHA: literal não fechado reprova — o arquivo não carregaria.
     */
    static List<String> comentariosMigradosEm(String sql) {
        List<String> saida = new ArrayList<>();
        boolean emLiteral = false;
        int i = 0;
        while (i < sql.length()) {
            char ch = sql.charAt(i);
            if (emLiteral) {
                if (ch == '\'') {
                    if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                        i += 2;
                        continue;
                    }
                    emLiteral = false;
                }
                i++;
            } else if (ch == '\'') {
                emLiteral = true;
                i++;
            } else if (ch == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                int fimLinha = sql.indexOf('\n', i);
                if (fimLinha < 0) {
                    fimLinha = sql.length();
                }
                String comentario = sql.substring(i, fimLinha);
                if (!comentario.startsWith(GERADO.strip())) {
                    saida.add(comentario.substring(2));
                }
                i = fimLinha;
            } else {
                i++;
            }
        }
        assertFalse(emLiteral, "literal SQL não fechado no fim do arquivo");
        return saida;
    }

    private static Map<String, Integer> multiconjunto(List<String> textos) {
        Map<String, Integer> m = new TreeMap<>();
        textos.forEach(t -> m.merge(t, 1, Integer::sum));
        return m;
    }

    // ------------------------------------------------------------------ SQLite

    static SQLiteDataSource fonte() {
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        SQLiteDataSource fonte = new SQLiteDataSource(config);
        fonte.setUrl("jdbc:sqlite::memory:");
        return fonte;
    }

    private static void executar(Connection c, String arquivo, String sql) {
        try (Statement s = c.createStatement()) {
            s.executeUpdate(sql);
        } catch (SQLException e) {
            fail("o SQLite recusou " + arquivo + ": " + e.getMessage());
        }
    }

    private static void verificarIntegridade(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("PRAGMA foreign_key_check")) {
            assertFalse(r.next(), "violação de chave estrangeira depois da carga");
        }
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("PRAGMA integrity_check")) {
            assertTrue(r.next());
            assertEquals("ok", r.getString(1), "integrity_check");
        }
    }

    private static List<ProvedorContexto> materializarTraducao(Connection c) throws SQLException {
        List<ProvedorContexto> saida = new ArrayList<>();
        try (Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT obra_id, nome, aparece_na_lista, prompt FROM lore_traducao ORDER BY obra_id")) {
            while (r.next()) {
                String id = r.getString(1);
                saida.add(new TraducaoRelida(id, r.getString(2), r.getString(4),
                    conjunto(c, "SELECT apelido FROM apelido_pasta WHERE obra_id = ? ORDER BY apelido", id),
                    conjunto(c, "SELECT termo FROM termo_protegido WHERE obra_id = ? ORDER BY termo", id),
                    pares(c, id),
                    mapa(c, "SELECT forma_ruim, canonico FROM correcao_terminologia WHERE obra_id = ? ORDER BY forma_ruim", id),
                    mapa(c, "SELECT origem, destino FROM traducao_obrigatoria WHERE obra_id = ? ORDER BY origem", id),
                    r.getInt(3) == 1));
            }
        }
        return saida;
    }

    private static List<ProvedorPromptRevisaoLore> materializarRevisao(Connection c) throws SQLException {
        List<ProvedorPromptRevisaoLore> saida = new ArrayList<>();
        try (Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT obra_id, nome, prompt FROM lore_revisao ORDER BY obra_id")) {
            while (r.next()) {
                String id = r.getString(1);
                Map<String, List<String>> eq = new LinkedHashMap<>();
                try (PreparedStatement p = c.prepareStatement(
                    "SELECT termo, forma FROM equivalencia_aceita WHERE obra_id = ? ORDER BY termo, ordem")) {
                    p.setString(1, id);
                    try (ResultSet x = p.executeQuery()) {
                        while (x.next()) {
                            eq.computeIfAbsent(x.getString(1), k -> new ArrayList<>()).add(x.getString(2));
                        }
                    }
                }
                saida.add(new RevisaoRelida(id, r.getString(2), r.getString(3),
                    conjunto(c, "SELECT termo FROM termo_protegido WHERE obra_id = ? ORDER BY termo", id),
                    mapa(c, "SELECT forma_ruim, canonico FROM correcao_terminologia WHERE obra_id = ? ORDER BY forma_ruim", id),
                    eq));
            }
        }
        return saida;
    }

    private static Set<String> conjunto(Connection c, String sql, String id) throws SQLException {
        Set<String> s = new LinkedHashSet<>();
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, id);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    s.add(r.getString(1));
                }
            }
        }
        return s;
    }

    private static Map<String, String> mapa(Connection c, String sql, String id) throws SQLException {
        Map<String, String> m = new LinkedHashMap<>();
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, id);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    m.put(r.getString(1), r.getString(2));
                }
            }
        }
        return m;
    }

    private static Set<List<String>> pares(Connection c, String id) throws SQLException {
        Set<List<String>> s = new LinkedHashSet<>();
        try (PreparedStatement p = c.prepareStatement("SELECT a, b FROM par_inconfundivel WHERE obra_id = ? ORDER BY ordem")) {
            p.setString(1, id);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    s.add(List.of(r.getString(1), r.getString(2)));
                }
            }
        }
        return s;
    }

    private static String lerRecurso(String recurso) throws IOException {
        try (InputStream in = GeradorLoreSqlIT.class.getResourceAsStream(recurso)) {
            assertTrue(in != null, "NÃO VERIFICADO: recurso " + recurso + " ausente");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Obra da tradução rematerializada do SQL por este teste — implementação independente do carregador. */
    private record TraducaoRelida(String id, String nome, String prompt, Set<String> apelidos, Set<String> termos,
                                  Set<List<String>> pares, Map<String, String> correcoes,
                                  Map<String, String> obrigatorias, boolean lista) implements ProvedorContexto {
        @Override public String getId() { return id; }
        @Override public String getNomeExibicao() { return nome; }
        @Override public String obterPromptSistema() { return prompt; }
        @Override public Set<String> apelidosPasta() { return apelidos; }
        @Override public Set<String> termosProtegidos() { return termos; }
        @Override public Set<List<String>> paresInconfundiveis() { return pares; }
        @Override public Map<String, String> correcoesTerminologia() { return correcoes; }
        @Override public Map<String, String> traducoesObrigatorias() { return obrigatorias; }
        @Override public boolean apareceNaListaDeObras() { return lista; }
    }

    /** Obra da revisão rematerializada do SQL por este teste. */
    private record RevisaoRelida(String id, String nome, String prompt, Set<String> termos,
                                 Map<String, String> correcoes, Map<String, List<String>> equivalencias)
        implements ProvedorPromptRevisaoLore {
        @Override public String getId() { return id; }
        @Override public String getNomeExibicao() { return nome; }
        @Override public String obterPromptSistema() { return prompt; }
        @Override public Set<String> termosProtegidos() { return termos; }
        @Override public Map<String, String> correcoesTerminologia() { return correcoes; }
        @Override public Map<String, List<String>> equivalenciasAceitas() { return equivalencias; }
    }
}
