package org.traducao.projeto.lore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.traducao.projeto.lore.domain.ProvedorContexto;
import org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore;
import org.traducao.projeto.lore.infrastructure.CatalogoLoreSqlite;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela a decisão de que a lore tem FONTE ÚNICA — quem traduz e quem
 * revisa enxergam exatamente a mesma terminologia.
 *
 * <h2>O prejuízo que originou</h2>
 * Ordem de Paulo em 2026-08-15: <i>"a lore tem de ser compartilhada, é a única exceção. Se
 * não, temos problemas que não valem a pena."</i> O gatilho foi {@code Spearhead} chegar à
 * legenda final do 86 como {@code Esquadroe de Ponta}, com a revisão já conhecendo formas que
 * a tradução ignorava.
 *
 * <p>Medido em duas etapas, e a segunda é o motivo desta catraca existir:
 * <pre>
 *   antes de juntar os arquivos ..... 17 de 68 obras divergentes, 18 só na tradução, 65 só na revisão
 *   com UM arquivo, duas seções ..... 17 de 68 — IDÊNTICO. Mudou o LUGAR, não a VERDADE.
 *   com a união no carregamento ..... 0 de 68
 * </pre>
 * Juntar num arquivo só não bastou porque as duas seções continuavam sendo lidas em separado.
 *
 * <h2>Desde 2026-10-09 (lore em SQL)</h2>
 * A terminologia é UMA tabela por obra ({@code correcao_terminologia}), lida pelos dois lados. O
 * conflito virou violação de chave primária. A catraca continua porque os dois perigos continuam
 * possíveis no CARREGADOR: entregar a um lado um mapa diferente do declarado, ou perder entrada.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Para toda obra presente nos DOIS lados, o mapa efetivo é o MESMO.</li>
 *   <li>O mapa efetivo é EXATAMENTE o declarado na fonte — nem perde, nem inventa —, conferido
 *       contra os arquivos SQL lidos por um SQLite cru, fora do carregador sob teste.</li>
 *   <li>Conflito — mesma forma-ruim com canônicos diferentes — falha FECHADO no carregamento,
 *       nomeando a obra, a forma e OS DOIS canônicos.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Reprovar aqui significa que a lore voltou a ter dois donos, ou que o carregador passou a engolir
 * ou inventar entrada. Termina do mesmo jeito: termo perdido na legenda final.
 */
@DisplayName("catraca: a terminologia de lore tem fonte única")
class CatracaTerminologiaDeLoreUnificadaTest {

    private final CatalogoLoreSqlite catalogo = new CatalogoLoreSqlite();

    @Test
    @DisplayName("toda obra nos dois lados tem o MESMO mapa de terminologia")
    void osDoisLadosVeemAMesmaCoisa() {
        Map<String, Map<String, String>> daTraducao = new LinkedHashMap<>();
        for (ProvedorContexto p : catalogo.obras()) {
            daTraducao.put(p.getId(), p.correcoesTerminologia());
        }

        List<String> divergentes = new ArrayList<>();
        int comparadas = 0;
        for (ProvedorPromptRevisaoLore r : catalogo.obrasRevisao()) {
            Map<String, String> t = daTraducao.get(r.getId());
            if (t == null) {
                continue; // obra só do lado da revisão: não há par para comparar
            }
            comparadas++;
            if (!t.equals(r.correcoesTerminologia())) {
                var soT = new TreeSet<>(t.keySet());
                soT.removeAll(r.correcoesTerminologia().keySet());
                var soR = new TreeSet<>(r.correcoesTerminologia().keySet());
                soR.removeAll(t.keySet());
                divergentes.add(r.getId() + "  só na tradução=" + soT + "  só na revisão=" + soR);
            }
        }

        assertTrue(comparadas > 0,
            "NÃO VERIFICADO: nenhuma obra existe nos dois lados — a catraca não teve o que comparar");
        assertTrue(divergentes.isEmpty(),
            () -> "a lore voltou a ter dois donos em " + divergentes.size() + " obra(s):\n"
                + String.join("\n", divergentes));
    }

    /**
     * Conferido contra a FONTE, não contra o outro lado já carregado. A primeira versão desta
     * catraca comparava tradução com revisão e passava por CONSTRUÇÃO: um carregador que entregasse
     * o mesmo mapa errado aos dois deixaria os dois "iguais". A mutação provou isso em agosto, com 65
     * entradas sumindo e o teste verde.
     */
    @Test
    @DisplayName("o mapa efetivo é exatamente o declarado nos arquivos SQL (conferido fora do carregador)")
    void efetivoIgualAoDeclarado() throws SQLException {
        Map<String, Map<String, String>> declarado = terminologiaDeclaradaNosArquivos();
        assertTrue(!declarado.isEmpty(), "NÃO VERIFICADO: nenhuma correção declarada nos arquivos SQL");

        List<String> erros = new ArrayList<>();
        int conferidas = 0;
        for (ProvedorContexto p : catalogo.obras()) {
            conferidas += comparar(erros, "tradução", p.getId(), declarado.getOrDefault(p.getId(), Map.of()),
                p.correcoesTerminologia());
        }
        for (ProvedorPromptRevisaoLore r : catalogo.obrasRevisao()) {
            conferidas += comparar(erros, "revisão", r.getId(), declarado.getOrDefault(r.getId(), Map.of()),
                r.correcoesTerminologia());
        }
        final int total = conferidas;
        assertTrue(total > 0, "NÃO VERIFICADO: nenhuma entrada conferida");
        assertTrue(erros.isEmpty(), () -> erros.size() + " divergência(s) entre o declarado e o efetivo, em "
            + total + " entrada(s):\n" + String.join("\n", erros.subList(0, Math.min(15, erros.size()))));
    }

    private static int comparar(List<String> erros, String lado, String id, Map<String, String> declarado,
                                Map<String, String> efetivo) {
        for (Map.Entry<String, String> e : declarado.entrySet()) {
            if (!e.getValue().equals(efetivo.get(e.getKey()))) {
                erros.add("declarada em obras/" + id + ".sql (\"" + e.getKey() + "\" -> \"" + e.getValue()
                    + "\") NÃO chegou ao lado " + lado + " (veio " + efetivo.get(e.getKey()) + ")");
            }
        }
        for (String chave : efetivo.keySet()) {
            if (!declarado.containsKey(chave)) {
                erros.add("o lado " + lado + " de " + id + " tem \"" + chave + "\", que nenhum arquivo declara");
            }
        }
        return declarado.size();
    }

    /** Lê a terminologia como os ARQUIVOS a declaram, por um SQLite cru — sem o carregador sob teste. */
    private static Map<String, Map<String, String>> terminologiaDeclaradaNosArquivos() throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        SQLiteDataSource fonte = new SQLiteDataSource(config);
        fonte.setUrl("jdbc:sqlite::memory:");
        Map<String, Map<String, String>> porObra = new TreeMap<>();
        try (Connection c = fonte.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate(recurso("/lore/esquema.sql"));
            for (String id : recurso("/lore/obras.lst").lines().map(String::strip).filter(l -> !l.isEmpty()).toList()) {
                s.executeUpdate(recurso("/lore/obras/" + id + ".sql"));
            }
            try (ResultSet r = s.executeQuery("SELECT obra_id, forma_ruim, canonico FROM correcao_terminologia")) {
                while (r.next()) {
                    porObra.computeIfAbsent(r.getString(1), k -> new LinkedHashMap<>()).put(r.getString(2), r.getString(3));
                }
            }
        }
        return porObra;
    }

    private static String recurso(String caminho) {
        try (InputStream in = CatracaTerminologiaDeLoreUnificadaTest.class.getResourceAsStream(caminho)) {
            assertTrue(in != null, "NÃO VERIFICADO: recurso ausente " + caminho);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * O CASO-CONTROLE. Sem ele a catraca poderia estar passando por cegueira: um carregador que
     * escolhesse um dos canônicos ao acaso também deixaria os dois lados "iguais".
     */
    @Test
    @DisplayName("CASO DOENTE: canônicos diferentes para a mesma forma derrubam o carregamento")
    void conflitoFalhaFechado() {
        Map<String, String> arquivos = new HashMap<>();
        arquivos.put("esquema.sql", recurso("/lore/esquema.sql"));
        arquivos.put("obras.lst", "obra_de_teste\n");
        arquivos.put("obras/obra_de_teste.sql", recurso("/lore/lore-conflito-terminologia.sql"));
        IllegalStateException erro = assertThrows(IllegalStateException.class,
            () -> new CatalogoLoreSqlite(arquivos::get),
            "conflito de terminologia passou: um dos canônicos venceria em silêncio e ninguém saberia qual");

        String msg = String.valueOf(erro.getMessage());
        assertTrue(msg.contains("obra_de_teste") && msg.contains("Lanca-Flanco"),
            "a mensagem precisa nomear a obra e a forma em conflito, senão não se acha o cadastro errado: " + msg);
        assertTrue(msg.contains("Spearhead") && msg.contains("Ponta de Lanca"),
            "a mensagem precisa mostrar OS DOIS canônicos, senão não se sabe qual está errado: " + msg);
    }

    /** A lore de produção carrega sem conflito — se um entrar, o boot para. */
    @Test
    @DisplayName("a lore de produção carrega sem conflito")
    void producaoCarregaLimpo() {
        assertEquals(catalogo.obras().size(), new CatalogoLoreSqlite().obras().size());
        assertTrue(!catalogo.obras().isEmpty(), "NÃO VERIFICADO: catálogo de produção vazio");
    }
}
