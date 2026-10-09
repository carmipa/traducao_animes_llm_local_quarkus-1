package org.traducao.projeto.lore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * PROPÓSITO DE NEGÓCIO: impede que a CICATRIZ da lore — a medição real escrita em comentário ao lado
 * de cada termo — seja apagada em silêncio. Nenhuma outra guarda perceberia: todas comparam DADO, e
 * comentário não é dado. A lore continuaria passando no manifesto e nos baselines, verde e vazia de
 * história.
 *
 * <h2>De onde vem o piso</h2>
 * Até 2026-10-09 a lore era o {@code lore.yaml}, e esta catraca contava as linhas que começavam com
 * {@code #} (piso 377, de agosto). Na migração para SQL o parser do YAML achou 673 comentários: as
 * 521 linhas inteiras e mais 152 comentários na MESMA linha do dado ({@code Esquadroe de Ponta:
 * Spearhead # 1 <- o defeito que Paulo viu na legenda}), que a contagem por linha nunca enxergou. Os
 * 673 foram migrados e conferidos por texto, um a um. O piso agora é esse.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li><b>Catraca: o número só SOBE.</b> Escrever cicatriz nova é livre; perder reprova. Quando
 *       subir de verdade, atualize {@link #PISO} no mesmo commit.</li>
 *   <li>Conta comentário {@code --} FORA de literal: um {@code --} dentro do prompt é texto e não
 *       conta, e um comentário no fim da linha do {@code INSERT} conta. Não conta o que leva a marca
 *       {@code [gerado]}, que é explicação do formato e não medição.</li>
 *   <li>Mede o que o KRONOS carrega: {@code esquema.sql} e cada arquivo de {@code obras.lst}, do
 *       classpath.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Arquivo ausente reprova como {@code NÃO VERIFICADO} — "não achei comentário" e "não achei arquivo"
 * não podem dar o mesmo sinal. Contagem abaixo do piso reprova dizendo quantos sumiram.
 */
@DisplayName("CATRACA: a cicatriz da lore só pode CRESCER")
class CatracaCicatrizNaLoreSqlTest {

    /** Comentários migrados do lore.yaml em 2026-10-09 (521 linhas + 152 na linha do dado). Só sobe. */
    private static final int PISO = 673;

    private static final String MARCA_GERADO = "-- [gerado]";

    @Test
    @DisplayName("a lore não perdeu comentário de cicatriz")
    void cicatrizNaoEncolheu() {
        int total = contar(ler("/lore/esquema.sql"));
        for (String id : ler("/lore/obras.lst").lines().map(String::strip)
                .filter(l -> !l.isEmpty() && !l.startsWith("#")).toList()) {
            total += contar(ler("/lore/obras/" + id + ".sql"));
        }
        final int comentarios = total;
        assertTrue(comentarios >= PISO,
            () -> "A CICATRIZ da lore encolheu: " + comentarios + " comentários, piso é " + PISO + " (sumiram "
                + (PISO - comentarios) + ").\nAlguém regravou um arquivo de obra sem os comentários, ou apagou "
                + "a linha de dado junto com a medição que a explicava. Recuperar pelo histórico do arquivo "
                + "(git log -p -- src/main/resources/lore/obras/<id>.sql) e, se o DADO precisava mudar, "
                + "mudar só o dado.");
    }

    /** Calibração do contador: os casos que ele tem de separar, com resposta conhecida. */
    @Test
    @DisplayName("calibração: '--' em literal e [gerado] não contam; fim de linha e apóstrofo em comentário contam certo")
    void contadorDiscrimina() {
        String sql = """
            -- [gerado] explicação do formato
            -- cicatriz de linha inteira, com o apóstrofo de Char's que não abre literal
            INSERT INTO obra VALUES ('x');  -- cicatriz no fim da linha
            INSERT INTO lore_traducao VALUES ('x', 'n', 1, 'prompt com -- dentro e It''s ok');
            --
            """;
        assertEquals(3, contar(sql));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: conta os comentários de cicatriz de um arquivo SQL.
     * <p>INVARIANTES DO DOMÍNIO: anda caractere a caractere respeitando aspas simples com {@code ''};
     * {@code --} fora de literal abre comentário até o fim da linha.
     * <p>COMPORTAMENTO EM CASO DE FALHA: literal não fechado reprova — o arquivo não carregaria.
     */
    static int contar(String sql) {
        int n = 0;
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
                int fim = sql.indexOf('\n', i);
                if (fim < 0) {
                    fim = sql.length();
                }
                if (!sql.startsWith(MARCA_GERADO, i)) {
                    n++;
                }
                i = fim;
            } else {
                i++;
            }
        }
        if (emLiteral) {
            fail("literal não fechado — o arquivo não carregaria");
        }
        return n;
    }

    private static String ler(String recurso) {
        try (InputStream in = CatracaCicatrizNaLoreSqlTest.class.getResourceAsStream(recurso)) {
            if (in == null) {
                fail("NÃO VERIFICADO: " + recurso + " não encontrado — a catraca não pôde medir, e isso não é aprovação.");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
