package org.traducao.projeto.contexto;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que a pasta Joseki de Char's Counterattack identifica
 * exatamente a lore {@code gundam_cca} — nem zero (portão cego), nem várias (AMBIGUO).
 *
 * <p>INVARIANTES DO DOMÍNIO: o catálogo injetado é o COMPLETO (todas as lores CDI),
 * não um subset. A pasta de teste NÃO carrega o nome de exibição inteiro
 * ({@code Mobile Suit Gundam: Char's Counterattack}); sem o apelido específico, o
 * reconhecimento falha. Mutar o apelido para um genérico ({@code "Gundam"}) tem de
 * reprovar este teste.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: assertiva no Set exato de ids.
 */
@QuarkusTest
class ApelidoPastaCharsCounterattackTest {

    /**
     * Pasta derivada do inventário do acervo (07/08/2026): contém a frase
     * {@code Char's Counterattack} e NÃO contém o nome de exibição completo —
     * isola o apelido como causa do reconhecimento.
     */
    static final String PASTA_CCA =
        "[Joseki] Char's Counterattack COMPLETE (1988)(BD AV1 1080p Opus)[Sub Eng]";

    @Inject
    GerenciadorContexto gerenciador;

    /**
     * PROPÓSITO DE NEGÓCIO: a pasta CCA resolve para exatamente uma lore, a correta.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code idsQueReconhecem} no catálogo completo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: Set diferente de {@code gundam_cca} reprova.
     */
    @Test
    @DisplayName("pasta Char's Counterattack resolve para EXATAMENTE gundam_cca no catálogo completo")
    void pastaCcaResolveParaUmaLore() {
        assertEquals(Set.of("gundam_cca"), gerenciador.idsQueReconhecem(PASTA_CCA),
            "pasta CCA deve identificar só gundam_cca; vazio = portão cego; "
                + "mais de um = AMBIGUO que BLOQUEIA");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: contra-teste — a pasta não pode ser reivindicada por
     * mais de uma lore com a mesma especificidade.
     *
     * <p>INVARIANTES DO DOMÍNIO: tamanho do Set == 1.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: size != 1 reprova com os ids empatados.
     */
    @Test
    @DisplayName("pasta Char's Counterattack nao e ambigua no catálogo completo")
    void pastaCcaNaoEhAmbigua() {
        Set<String> ids = gerenciador.idsQueReconhecem(PASTA_CCA);
        assertEquals(1, ids.size(),
            "pasta \"" + PASTA_CCA + "\" reivindicada por " + ids
                + " — mais de uma lore significa veredicto AMBIGUO, que BLOQUEIA");
        assertTrue(ids.contains("gundam_cca"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o nome de exibição completo continua resolvendo; o apelido
     * não quebra o caminho já coberto por id/nome.
     *
     * <p>INVARIANTES DO DOMÍNIO: nome canônico derivado segue único.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: Set diferente reprova.
     */
    @Test
    @DisplayName("nome de exibicao completo de CCA segue resolvendo sozinho")
    void nomeExibicaoCompletoContinuaResolvendo() {
        assertEquals(Set.of("gundam_cca"),
            gerenciador.idsQueReconhecem("Mobile Suit Gundam: Char's Counterattack"));
    }
}
