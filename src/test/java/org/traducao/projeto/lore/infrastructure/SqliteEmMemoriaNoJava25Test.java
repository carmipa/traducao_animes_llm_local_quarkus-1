package org.traducao.projeto.lore.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteJDBCLoader;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: portão de viabilidade da FASE 0 do plano da lore em SQLite. A lore vai sair
 * do {@code lore.yaml} para arquivos SQL por obra, executados num SQLite em memória no arranque. Antes
 * de qualquer linha do carregador existir, este teste prova que o driver nativo
 * ({@code org.xerial:sqlite-jdbc}) funciona no JVM do Java 25 e que o motor aplica as restrições das
 * quais o esquema vai depender para falhar FECHADO.
 *
 * <h2>O que foi medido em 09/10/2026, e fica congelado aqui</h2>
 * <pre>
 *   execute("CREATE ...; INSERT ...; INSERT ...")     -> só a 1ª roda; os INSERT somem SEM ERRO
 *   executeUpdate(as mesmas três)                       -> as três rodam
 *   FOREIGN KEY sem enforceForeignKeys(true)            -> órfão ACEITO em silêncio
 * </pre>
 * Cada linha é um jeito de a lore chegar incompleta ao pipeline sem que nada reclame. O carregador
 * da Fase 3 é escrito contra estes fatos; se uma versão nova do driver mudar algum deles, este teste
 * reprova e a mudança é avaliada antes de virar lore perdida.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Cada restrição é provada nos DOIS lados (A1): o defeito reprova e o vizinho legítimo, com o
 *       mesmo sinal superficial, passa. Restrição que só foi vista reprovando pode estar reprovando
 *       tudo.</li>
 *   <li>O banco nasce e morre com a conexão. Nada fica em disco além do nativo extraído pelo driver
 *       no {@code java.io.tmpdir}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Cada teste reprova com mensagem que nomeia a causa — driver sem nativo, instrução descartada,
 * restrição que deixou de valer. Não há
 * caminho que pule o teste: SQLite indisponível é falha, nunca "nada a verificar".
 */
class SqliteEmMemoriaNoJava25Test {

    /** Versão mínima com tabelas STRICT, que o esquema da lore usa em todas as tabelas. */
    private static final int[] VERSAO_MINIMA_STRICT = {3, 37};

    /**
     * PROPÓSITO DE NEGÓCIO: a fonte de conexões que o carregador vai usar — direto pelo
     * {@link SQLiteDataSource}, sem {@code DriverManager} nem pool, porque o banco vive só no arranque.
     * <p>INVARIANTES DO DOMÍNIO: banco em memória; integridade referencial ligada só quando pedido,
     * para o teste poder exibir os dois lados.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; quem lança é {@code getConnection()}.
     */
    private static SQLiteDataSource fonte(boolean imporChaveEstrangeira) {
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(imporChaveEstrangeira);
        SQLiteDataSource fonte = new SQLiteDataSource(config);
        fonte.setUrl("jdbc:sqlite::memory:");
        return fonte;
    }

    private static int inteiro(Connection c, String consulta) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery(consulta)) {
            assertTrue(r.next(), "a consulta não devolveu linha: " + consulta);
            return r.getInt(1);
        }
    }

    private static String texto(Connection c, String consulta) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery(consulta)) {
            assertTrue(r.next(), "a consulta não devolveu linha: " + consulta);
            return r.getString(1);
        }
    }

    private static void atualizar(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.executeUpdate(sql);
        }
    }

    /**
     * A liberação do acesso nativo NÃO é conferida por asserção, de propósito: o JDK marca o módulo
     * como liberado logo depois de emitir o primeiro aviso, então {@code isNativeAccessEnabled()} lido
     * depois da carga dá verdadeiro com ou sem a flag (medido em 09/10/2026). Quem guarda a flag é o
     * {@code --illegal-native-access=deny} do bloco {@code test} do {@code build.gradle}: sem a
     * liberação, a primeira conexão daqui lança {@link IllegalCallerException} e este teste reprova.
     */
    @Test
    @DisplayName("o nativo carrega e o motor tem tabelas STRICT")
    void nativoCarregaEMotorTemTabelaStrict() throws Exception {
        try (Connection c = fonte(true).getConnection()) {
            assertTrue(SQLiteJDBCLoader.isNativeMode(), "o sqlite-jdbc não carregou a biblioteca nativa");

            String versao = texto(c, "select sqlite_version()");
            String[] partes = versao.split("\\.");
            int maior = Integer.parseInt(partes[0]);
            int menor = Integer.parseInt(partes[1]);
            assertTrue(maior > VERSAO_MINIMA_STRICT[0]
                    || (maior == VERSAO_MINIMA_STRICT[0] && menor >= VERSAO_MINIMA_STRICT[1]),
                "SQLite " + versao + " não tem tabela STRICT (exige 3.37+)");
        }
    }

    /**
     * A armadilha que decide como o carregador executa um arquivo SQL. {@code execute} compila só a
     * primeira instrução e descarta o resto sem erro: a tabela nasce e os dois {@code INSERT} somem.
     * Um carregador escrito assim subiria o KRONOS com as obras sem nenhuma terminologia, e a
     * contagem de obras continuaria certa.
     */
    @Test
    @DisplayName("executeUpdate roda todas as instruções; execute roda só a primeira, em silêncio")
    void executeUpdateRodaTodasEExecuteDescartaORestoEmSilencio() throws Exception {
        try (Connection c = fonte(true).getConnection()) {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE a (x INTEGER); INSERT INTO a VALUES (1); INSERT INTO a VALUES (2);");
            }
            assertEquals(0, inteiro(c, "select count(*) from a"),
                "Statement.execute passou a rodar as instruções seguintes: a armadilha mudou com a versão "
                    + "do driver — reavaliar o carregador antes de confiar nisso");

            atualizar(c, "CREATE TABLE b (x INTEGER); INSERT INTO b VALUES (1); INSERT INTO b VALUES (2);");
            assertEquals(2, inteiro(c, "select count(*) from b"),
                "executeUpdate deixou de rodar todas as instruções do script");
        }
    }

    /**
     * No SQLite a integridade referencial nasce DESLIGADA. O mesmo órfão é recusado com
     * {@code enforceForeignKeys(true)} e aceito sem ela — o que prova que quem recusa é a
     * configuração, e não outra coisa.
     */
    @Test
    @DisplayName("chave estrangeira só é imposta com enforceForeignKeys(true)")
    void chaveEstrangeiraSoValeComAImposicaoLigada() throws Exception {
        String esquema = "CREATE TABLE obra (id TEXT PRIMARY KEY) STRICT;"
            + " CREATE TABLE termo (obra_id TEXT NOT NULL REFERENCES obra(id), termo TEXT NOT NULL) STRICT;"
            + " INSERT INTO obra VALUES ('sidonia_movie');";

        try (Connection c = fonte(true).getConnection()) {
            atualizar(c, esquema);
            assertDoesNotThrow(() -> atualizar(c, "INSERT INTO termo VALUES ('sidonia_movie', 'Gauna')"),
                "o filho com pai existente foi recusado — a restrição reprova o legítimo");
            SQLException orfao = assertThrows(SQLException.class,
                () -> atualizar(c, "INSERT INTO termo VALUES ('gundam_zeta', 'Gauna')"));
            assertTrue(orfao.getMessage().contains("FOREIGN KEY constraint failed"),
                "o órfão foi recusado por outra causa: " + orfao.getMessage());
        }

        try (Connection c = fonte(false).getConnection()) {
            atualizar(c, esquema);
            assertDoesNotThrow(() -> atualizar(c, "INSERT INTO termo VALUES ('gundam_zeta', 'Gauna')"),
                "sem enforceForeignKeys o SQLite deveria aceitar o órfão — se recusou, o controle não "
                    + "distingue mais a configuração da causa");
            assertEquals(1, inteiro(c, "select count(*) from termo where obra_id = 'gundam_zeta'"));
        }
    }

    @Test
    @DisplayName("tabela STRICT recusa tipo errado que a tabela comum aceitaria")
    void tabelaStrictRecusaTipoErrado() throws Exception {
        try (Connection c = fonte(true).getConnection()) {
            atualizar(c, "CREATE TABLE estrita (aparece_na_lista INTEGER NOT NULL) STRICT;"
                + " CREATE TABLE comum (aparece_na_lista INTEGER NOT NULL);");

            assertDoesNotThrow(() -> atualizar(c, "INSERT INTO estrita VALUES (1)"));
            SQLException tipo = assertThrows(SQLException.class,
                () -> atualizar(c, "INSERT INTO estrita VALUES ('sim')"));
            assertTrue(tipo.getMessage().contains("cannot store TEXT value in INTEGER column"),
                "a recusa veio por outra causa: " + tipo.getMessage());

            assertDoesNotThrow(() -> atualizar(c, "INSERT INTO comum VALUES ('sim')"),
                "a tabela comum deveria aceitar — é o que torna o STRICT necessário");
        }
    }

    /**
     * A proteção do hash do prompt (invariante L1 do plano). O YAML normalizava a quebra de linha; o
     * literal SQL não normaliza, e um {@code \r} enfiado pelo {@code autocrlf} trocaria o
     * {@code contextoHash} e jogaria fora o cache da obra. Os dois lados carregam o mesmo sinal — uma
     * quebra de linha no meio do prompt — e só o {@code \r} separa.
     */
    @Test
    @DisplayName("CHECK sem \\r aceita LF e recusa CRLF no mesmo prompt")
    void checkSemRetornoDeCarroSeparaLfDeCrlf() throws Exception {
        try (Connection c = fonte(true).getConnection()) {
            atualizar(c, "CREATE TABLE lore (prompt TEXT NOT NULL CHECK (instr(prompt, char(13)) = 0)) STRICT;");

            assertDoesNotThrow(() -> atualizar(c,
                "INSERT INTO lore VALUES ('Você é um tradutor.' || char(10) || 'Preserve Gauna.')"));
            SQLException crlf = assertThrows(SQLException.class, () -> atualizar(c,
                "INSERT INTO lore VALUES ('Você é um tradutor.' || char(13) || char(10) || 'Preserve Gauna.')"));
            assertTrue(crlf.getMessage().contains("CHECK constraint failed"),
                "o CRLF foi recusado por outra causa: " + crlf.getMessage());
        }
    }

    /**
     * O prompt volta do banco igual ao que entrou: acento, aspas tipográficas, apóstrofo dobrado no
     * literal, quebra de linha e caractere fora do plano básico. Um byte diferente aqui é um
     * {@code contextoHash} diferente e o cache da obra inteira descartado.
     */
    @Test
    @DisplayName("texto com acento, apóstrofo e caractere de 4 bytes volta igual")
    void textoVoltaIgualAoQueEntrou() throws Exception {
        String prompt = "Você é um tradutor de \"Knights of Sidonia\".\nNão traduza Gauna; "
            + "Tanikaze's Garde é nome próprio — “aspas” e 𝄞 também.";
        String literal = "'" + prompt.replace("'", "''") + "'";

        try (Connection c = fonte(true).getConnection()) {
            atualizar(c, "CREATE TABLE lore (prompt TEXT NOT NULL) STRICT;");
            atualizar(c, "INSERT INTO lore VALUES (" + literal + ")");
            assertEquals(prompt, texto(c, "select prompt from lore"));
        }
    }

    /**
     * O banco não sobrevive à conexão. É a base da invariante L7 (o caminho quente da tradução não
     * segura banco nenhum): fechar a conexão depois de materializar devolve tudo.
     */
    @Test
    @DisplayName("o banco em memória morre com a conexão")
    void bancoEmMemoriaMorreComAConexao() throws Exception {
        SQLiteDataSource fonte = fonte(true);
        try (Connection c = fonte.getConnection()) {
            atualizar(c, "CREATE TABLE obra (id TEXT PRIMARY KEY) STRICT; INSERT INTO obra VALUES ('x');");
            assertEquals(1, inteiro(c, "select count(*) from sqlite_master where name = 'obra'"));
        }
        try (Connection c = fonte.getConnection()) {
            assertEquals(0, inteiro(c, "select count(*) from sqlite_master where name = 'obra'"),
                "uma conexão nova enxergou a tabela da anterior: o banco não é mais efêmero");
        }
    }
}
