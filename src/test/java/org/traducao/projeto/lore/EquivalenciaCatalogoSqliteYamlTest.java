package org.traducao.projeto.lore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.lore.domain.ProvedorContexto;
import org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore;
import org.traducao.projeto.lore.infrastructure.CatalogoLoreSqlite;
import org.traducao.projeto.lore.infrastructure.CatalogoLoreYaml;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: FASE 3 da lore em SQLite — prova, com os DOIS leitores vivos ao mesmo tempo,
 * que o {@link CatalogoLoreSqlite} entrega exatamente o que o {@link CatalogoLoreYaml} entrega: obra a
 * obra, lado a lado, campo a campo, com o hash de cada prompt. É a condição para a virada da fase 4
 * trocar a fonte sem que os 40 consumidores da lore percebam.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A comparação é a do manifesto completo ({@link ManifestoCompletoLoreIT#linhasDoCatalogo}),
 *       a mesma régua que congela a lore — não uma segunda régua escrita para este teste.</li>
 *   <li>Os ids saem na mesma ordem nos dois leitores.</li>
 *   <li>Vale enquanto os dois existem. Depois que o YAML sai (fase 5), quem continua provando o
 *       conteúdo é o manifesto completo, comparando o vivo com a fotografia congelada.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Reprova listando cada campo divergente por obra e lado. Imprime o tempo de carga dos dois, para a
 * medição do arranque.
 */
class EquivalenciaCatalogoSqliteYamlTest {

    @Test
    @DisplayName("SQL e YAML entregam a mesma lore, obra a obra, campo a campo")
    void mesmaLoreNosDoisLeitores() {
        long t0 = System.nanoTime();
        CatalogoLoreYaml yaml = new CatalogoLoreYaml();
        long t1 = System.nanoTime();
        CatalogoLoreSqlite sql = new CatalogoLoreSqlite();
        long t2 = System.nanoTime();

        List<String> divergencias = ManifestoCompletoLoreIT.divergencias(
            ManifestoCompletoLoreIT.linhasDoCatalogo(yaml.obras(), yaml.obrasRevisao()),
            ManifestoCompletoLoreIT.linhasDoCatalogo(sql.obras(), sql.obrasRevisao()));
        assertTrue(divergencias.isEmpty(), () -> "o leitor SQL diverge do YAML em " + divergencias.size()
            + " campo(s):\n  " + String.join("\n  ", divergencias));

        assertEquals(yaml.obras().stream().map(ProvedorContexto::getId).toList(),
            sql.obras().stream().map(ProvedorContexto::getId).toList(), "ordem das obras da tradução");
        assertEquals(yaml.obrasRevisao().stream().map(ProvedorPromptRevisaoLore::getId).toList(),
            sql.obrasRevisao().stream().map(ProvedorPromptRevisaoLore::getId).toList(), "ordem das obras da revisão");

        System.out.println("[equivalencia-lore] YAML " + (t1 - t0) / 1_000_000 + " ms | SQL " + (t2 - t1) / 1_000_000
            + " ms | " + sql.obras().size() + " + " + sql.obrasRevisao().size() + " obras, 0 divergência");
    }
}
