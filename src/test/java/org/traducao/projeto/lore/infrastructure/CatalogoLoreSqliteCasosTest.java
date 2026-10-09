package org.traducao.projeto.lore.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.lore.domain.ProvedorContexto;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: casos-controle do carregador da lore em SQL — um por classe de defeito que
 * um arquivo de obra pode trazer, cada um ao lado do vizinho LEGÍTIMO que carrega o mesmo sinal.
 * Carregador que recusa tudo passaria em metade destes testes; o vizinho é o que prova que ele
 * discrimina.
 *
 * <h2>Os sinais e os seus dois lados</h2>
 * <pre>
 *   apóstrofo ........ dobrado dentro do prompt passa; esquecido sem dobrar reprova
 *   ";" e "--" ....... dentro do literal não cortam nem comentam; fora, separam e comentam
 *   apóstrofo ........ dentro de comentário não abre literal
 *   INSERT ........... de literais, em várias tuplas, passa; com função, subconsulta ou ON CONFLICT reprova
 *   id ............... do próprio arquivo passa; de outra obra reprova (L4)
 *   quebra de linha .. LF passa; CRLF reprova
 * </pre>
 *
 * <h2>Invariantes do domínio</h2>
 * Todo arquivo de teste usa o {@code esquema.sql} REAL: um esquema copiado para cá divergiria do de
 * produção sem ninguém notar.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Cada caso confere a MENSAGEM, não só a exceção: recusa pela causa errada não prova a guarda.
 */
class CatalogoLoreSqliteCasosTest {

    private static final String PROMPT_LEGITIMO =
        "Você é um tradutor; preserve Gauna -- isto não é comentário. It''s fine.\nSegunda linha.";

    private static final String OBRA_LEGITIMA = """
        -- cicatriz: o nome de Char's Counterattack tem apóstrofo, e aqui não abre literal
        INSERT INTO obra VALUES ('teste_a');
        INSERT INTO termo_protegido VALUES ('teste_a', 'Gauna'), ('teste_a', 'Kabizashi');  -- dois de uma vez
        INSERT INTO correcao_terminologia VALUES ('teste_a', 'Gaunna', 'Gauna');
        INSERT INTO par_inconfundivel VALUES ('teste_a', -1, 'Kanata', 'Tsumugi');
        INSERT INTO lore_traducao VALUES ('teste_a', 'Obra A', 1,
        '%s');
        INSERT INTO lore_revisao VALUES ('teste_a', 'Obra A - Revisão', 'Revise.');
        INSERT INTO equivalencia_aceita VALUES ('teste_a', 'Mobile Suit', 0, 'Traje MÓVEL');
        """.formatted(PROMPT_LEGITIMO);

    private static String esquemaReal() {
        try (InputStream in = CatalogoLoreSqliteCasosTest.class.getResourceAsStream("/lore/esquema.sql")) {
            assertTrue(in != null, "NÃO VERIFICADO: /lore/esquema.sql ausente do classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static CatalogoLoreSqlite carregar(String lista, Map<String, String> obras) {
        Map<String, String> arquivos = new HashMap<>();
        arquivos.put("esquema.sql", esquemaReal());
        arquivos.put("obras.lst", lista);
        obras.forEach((id, sql) -> arquivos.put("obras/" + id + ".sql", sql));
        return new CatalogoLoreSqlite(arquivos::get);
    }

    private static void reprova(String lista, Map<String, String> obras, String trechoDaMensagem) {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> carregar(lista, obras));
        assertTrue(e.getMessage().contains(trechoDaMensagem),
            () -> "recusou pela causa errada. Esperado \"" + trechoDaMensagem + "\", veio: " + e.getMessage());
    }

    @Test
    @DisplayName("o arquivo legítimo carrega: ';' e '--' no literal, apóstrofo dobrado e em comentário, várias tuplas")
    void legitimoCarrega() {
        CatalogoLoreSqlite c = carregar("teste_a\n", Map.of("teste_a", OBRA_LEGITIMA));
        assertEquals(1, c.obras().size());
        ProvedorContexto a = c.obras().getFirst();
        assertEquals("Você é um tradutor; preserve Gauna -- isto não é comentário. It's fine.\nSegunda linha.",
            a.obterPromptSistema());
        assertEquals(Set.of("Gauna", "Kabizashi"), a.termosProtegidos());
        assertEquals(Map.of("Gaunna", "Gauna"), a.correcoesTerminologia());
        assertEquals(Set.of(List.of("Kanata", "Tsumugi")), a.paresInconfundiveis());
        assertEquals(1, c.obrasRevisao().size());
        assertEquals(Set.of("Gauna", "Kabizashi"), c.obrasRevisao().getFirst().termosProtegidos(),
            "a revisão recebe os termos protegidos da tradução, como no leitor do YAML");
        assertEquals(Map.of("mobile suit", List.of("traje móvel")), c.obrasRevisao().getFirst().equivalenciasAceitas(),
            "equivalência sai em minúsculas, como no leitor do YAML");
    }

    @Test
    @DisplayName("apóstrofo sem dobrar no prompt reprova nomeando o arquivo")
    void apostrofoSemDobrar() {
        reprova("teste_a\n", Map.of("teste_a", OBRA_LEGITIMA.replace("It''s", "It's")), "Literal não fechado em obras/teste_a.sql");
    }

    @Test
    @DisplayName("arquivo que escreve linha de outra obra reprova (L4)")
    void linhaDeOutraObra() {
        String b = "INSERT INTO obra VALUES ('teste_b');\n"
            + "INSERT INTO lore_traducao VALUES ('teste_b', 'Obra B', 1, 'Traduza.');\n";
        String aEscreveEmB = OBRA_LEGITIMA + "INSERT INTO termo_protegido VALUES ('teste_b', 'Intruso');\n";
        assertEquals(2, carregar("teste_b\nteste_a\n", Map.of("teste_a", OBRA_LEGITIMA, "teste_b", b)).obras().size(),
            "o par legítimo de duas obras tem de carregar");
        reprova("teste_b\nteste_a\n", Map.of("teste_a", aEscreveEmB, "teste_b", b),
            "O arquivo obras/teste_a.sql escreveu 1 linha(s) de OUTRA obra");
    }

    @Test
    @DisplayName("id repetido reprova: na lista e na chave primária")
    void idRepetido() {
        reprova("teste_a\nteste_a\n", Map.of("teste_a", OBRA_LEGITIMA), "id repetido \"teste_a\"");
        String bComIdDeA = "INSERT INTO obra VALUES ('teste_a');\n";
        reprova("teste_a\nteste_b\n", Map.of("teste_a", OBRA_LEGITIMA, "teste_b", bComIdDeA), "UNIQUE constraint failed: obra.id");
    }

    @Test
    @DisplayName("CRLF reprova; o mesmo arquivo em LF passa")
    void retornoDeCarro() {
        reprova("teste_a\n", Map.of("teste_a", OBRA_LEGITIMA.replace("\n", "\r\n")), "quebra de linha CRLF");
    }

    @Test
    @DisplayName("BOM no início reprova")
    void bom() {
        reprova("teste_a\n", Map.of("teste_a", "﻿" + OBRA_LEGITIMA), "começa com BOM");
    }

    @Test
    @DisplayName("obra listada e sem arquivo reprova (L5)")
    void listadaEAusente() {
        reprova("teste_a\nteste_c\n", Map.of("teste_a", OBRA_LEGITIMA), "/lore/obras/teste_c.sql (listado em obras.lst)");
    }

    @Test
    @DisplayName("obras.lst sem nenhuma obra reprova, em vez de subir catálogo vazio")
    void listaVazia() {
        reprova("\n# só comentário\n\n", Map.of("teste_a", OBRA_LEGITIMA), "obras.lst não lista nenhuma obra");
    }

    @Test
    @DisplayName("esquema ausente reprova")
    void esquemaAusente() {
        Map<String, String> arquivos = new HashMap<>();
        arquivos.put("obras.lst", "teste_a\n");
        arquivos.put("obras/teste_a.sql", OBRA_LEGITIMA);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> new CatalogoLoreSqlite(arquivos::get));
        assertTrue(e.getMessage().contains("/lore/esquema.sql"), () -> "recusou pela causa errada: " + e.getMessage());
    }

    @Test
    @DisplayName("só INSERT de literais: DROP, UPDATE, PRAGMA, ATTACH, ON CONFLICT, função e tabela de fora reprovam")
    void soInsertDeLiterais() {
        List<String> proibidas = List.of(
            "DROP TABLE termo_protegido;",
            "UPDATE lore_traducao SET prompt = 'x';",
            "PRAGMA foreign_keys = OFF;",
            "ATTACH DATABASE 'x.db' AS x;",
            "INSERT OR REPLACE INTO termo_protegido VALUES ('teste_a', 'Gauna');",
            "INSERT INTO lore_traducao VALUES ('teste_a', 'A', 1, 'p') ON CONFLICT(obra_id) DO UPDATE SET prompt = ('x');",
            "INSERT INTO termo_protegido VALUES ('teste_a', lower('X'));",
            "INSERT INTO termo_protegido VALUES ('teste_a', (SELECT prompt FROM lore_traducao));",
            "INSERT INTO sqlite_master VALUES ('teste_a');");
        for (String instrucao : proibidas) {
            reprova("teste_a\n", Map.of("teste_a", OBRA_LEGITIMA + instrucao + "\n"), "Instrução não permitida em obras/teste_a.sql");
        }
    }

    @Test
    @DisplayName("prompt só de espaços e quebras reprova, como no leitor do YAML")
    void promptEmBranco() {
        reprova("teste_a\n", Map.of("teste_a", OBRA_LEGITIMA.replace(PROMPT_LEGITIMO, "   \n  ")), "com prompt em branco");
    }

    @Test
    @DisplayName("lore sem obra de tradução reprova")
    void semTraducao() {
        String soRevisao = "INSERT INTO obra VALUES ('teste_a');\n"
            + "INSERT INTO lore_revisao VALUES ('teste_a', 'A', 'Revise.');\n";
        reprova("teste_a\n", Map.of("teste_a", soRevisao), "nenhuma obra do lado da TRADUÇÃO");
    }

    @Test
    @DisplayName("instrução sem ponto e vírgula no fim reprova")
    void semPontoEVirgula() {
        reprova("teste_a\n", Map.of("teste_a", OBRA_LEGITIMA + "INSERT INTO termo_protegido VALUES ('teste_a', 'Lem')"),
            "Instrução sem ponto e vírgula");
    }

    /**
     * L7: o catálogo materializado não guarda conexão nem fonte de dados. Confere os campos da classe
     * e dos tipos aninhados — banco que sobrevivesse à construção poria código nativo no meio da
     * tradução.
     */
    @Test
    @DisplayName("nenhum campo guarda conexão (L7)")
    void naoGuardaConexao() {
        List<Class<?>> tipos = Stream.concat(Stream.of(CatalogoLoreSqlite.class),
            Stream.of(CatalogoLoreSqlite.class.getDeclaredClasses())).toList();
        for (Class<?> tipo : tipos) {
            for (Field f : tipo.getDeclaredFields()) {
                assertTrue(!java.sql.Connection.class.isAssignableFrom(f.getType())
                        && !javax.sql.DataSource.class.isAssignableFrom(f.getType())
                        && !java.sql.Statement.class.isAssignableFrom(f.getType()),
                    () -> tipo.getSimpleName() + "." + f.getName() + " guarda " + f.getType().getName());
            }
        }
    }

    /**
     * L5, a metade que o carregador não vê: arquivo presente em {@code obras/} e fora da lista. No jar
     * não dá para listar a pasta, então quem acusa é este teste, sobre a árvore versionada.
     */
    @Test
    @DisplayName("todo arquivo em src/main/resources/lore/obras está em obras.lst, e vice-versa (L5)")
    void pastaIgualALista() throws IOException {
        Path raiz = Path.of("src", "main", "resources", "lore");
        Set<String> lista = new TreeSet<>(CatalogoLoreSqlite.lista(Files.readString(raiz.resolve("obras.lst"), StandardCharsets.UTF_8)));
        Set<String> arquivos;
        try (Stream<Path> s = Files.list(raiz.resolve("obras"))) {
            arquivos = s.map(p -> p.getFileName().toString()).collect(Collectors.toCollection(TreeSet::new));
        }
        assertTrue(arquivos.size() > 0, "NÃO VERIFICADO: pasta obras/ vazia");
        Set<String> semLista = new TreeSet<>(arquivos);
        semLista.removeIf(n -> n.endsWith(".sql") && lista.contains(n.substring(0, n.length() - 4)));
        Set<String> semArquivo = new TreeSet<>(lista);
        semArquivo.removeIf(id -> arquivos.contains(id + ".sql"));
        assertTrue(semLista.isEmpty(), () -> "arquivo em obras/ que obras.lst não lista (não carregaria): " + semLista);
        assertTrue(semArquivo.isEmpty(), () -> "obras.lst lista obra sem arquivo: " + semArquivo);
    }

    @Test
    @DisplayName("o analisador de tuplas aceita literais e recusa o resto")
    void analisadorDeTuplas() {
        assertEquals(-1, CatalogoLoreSqlite.primeiroInvalidoNasTuplas("('a', 1, NULL), ('b''c', -2, null)"));
        assertEquals(-1, CatalogoLoreSqlite.primeiroInvalidoNasTuplas("(\n'várias\nlinhas'\n)"));
        assertTrue(CatalogoLoreSqlite.primeiroInvalidoNasTuplas("('a') ON CONFLICT DO NOTHING") >= 0);
        assertTrue(CatalogoLoreSqlite.primeiroInvalidoNasTuplas("(lower('a'))") >= 0);
        assertTrue(CatalogoLoreSqlite.primeiroInvalidoNasTuplas("('a'") >= 0);
        assertTrue(CatalogoLoreSqlite.primeiroInvalidoNasTuplas("('a' || 'b')") >= 0);
        assertTrue(CatalogoLoreSqlite.primeiroInvalidoNasTuplas("(NULLX)") >= 0);
    }
}
