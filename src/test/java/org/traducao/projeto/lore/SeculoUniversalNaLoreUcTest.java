package org.traducao.projeto.lore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.lore.domain.ProvedorContexto;
import org.traducao.projeto.lore.infrastructure.CatalogoLoreYaml;
import org.traducao.projeto.qualidadeTraducao.application.EnforcadorTermosLore;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: {@code "Universal Century"} passa a sair como
 * {@code "Século Universal"} nas obras do Universal Century. Decisão de Paulo em 22/08/2026,
 * sobre as <b>7 falas do Unicorn</b> que a medição de prontidão encontrou publicadas como
 * {@code "Universal Century 0096."}.
 *
 * <h2>Por que este teste existe — o hash não cobria</h2>
 * A catraca {@code ProtecaoConteudoLoreTest} congela <b>nome, prompt e termos protegidos</b>.
 * O mapa {@code traducoesObrigatorias} fica FORA dela: dá para adicionar, mudar ou APAGAR uma
 * entrada e a catraca continuar verde. Sem este teste, a decisão do Paulo viraria uma linha de
 * YAML sem nada que a defendesse.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A troca vale nas NOVE obras UC do acervo, não só no Unicorn — a mesma expressão aparece
 *       em Zeta, ZZ, 0080, 0083, 08th, CCA, F91 e Narrative.</li>
 *   <li>{@code "Universal Century"} saiu de {@code termosProtegidos}: protegê-lo o mantinha em
 *       inglês, que é exatamente o que a decisão desfaz.</li>
 *   <li>A abreviação {@code "U.C."} NÃO é tocada. É forma consagrada, aparece em 24 linhas da
 *       própria lore como parte de datas ({@code "U.C. 0087"}), e trocá-la por {@code "S.U."}
 *       mudaria o que Paulo não pediu.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Se a entrada sumir do YAML, a fala volta a ser publicada em inglês e a catraca de hash não
 * acusa nada — este teste é a única coisa entre a decisão e o esquecimento.
 */
class SeculoUniversalNaLoreUcTest {

    /** As obras do Universal Century que existem no acervo de Paulo. */
    private static final List<String> OBRAS_UC = List.of(
        "gundam_0080", "gundam_0083", "gundam_08ms", "gundam_cca", "gundam_f91",
        "gundam_nt", "gundam_unicorn", "gundam_zeta", "gundam_zz");

    private final CatalogoLoreYaml catalogo = new CatalogoLoreYaml();
    private final EnforcadorTermosLore enforcador = new EnforcadorTermosLore();

    private ProvedorContexto obra(String id) {
        Optional<ProvedorContexto> achada = catalogo.obras().stream()
            .filter(o -> id.equals(o.getId()))
            .findFirst();
        assertTrue(achada.isPresent(), "obra ausente na lore: " + id);
        return achada.get();
    }

    @Test
    @DisplayName("as nove obras UC mapeiam Universal Century -> Seculo Universal")
    void asObrasUcMapeiamOTermo() {
        for (String id : OBRAS_UC) {
            Map<String, String> obrigatorias = obra(id).traducoesObrigatorias();
            assertEquals("Século Universal", obrigatorias.get("Universal Century"),
                "a obra " + id + " precisa mapear o termo");
        }
    }

    /** O EFEITO, e não só a presença no mapa: o enforcador de produção tem de trocar. */
    @Test
    @DisplayName("o enforcador troca de fato na fala do Unicorn")
    void oEnforcadorTrocaNaFalaReal() {
        Map<String, String> obrigatorias = obra("gundam_unicorn").traducoesObrigatorias();

        String saida = enforcador.traduzirObrigatorios(
            "Universal Century 0096.", "Universal Century 0096.", obrigatorias);

        assertEquals("Século Universal 0096.", saida,
            "é a fala que a medição de prontidão encontrou 7 vezes no acervo");
    }

    /**
     * A abreviação continua intacta. Sem este caso, alguém "completaria" a regra trocando
     * {@code U.C.} por {@code S.U.} e quebraria 24 linhas da própria lore.
     */
    @Test
    @DisplayName("a abreviacao U.C. NAO e tocada")
    void abreviacaoNaoEtocada() {
        Map<String, String> obrigatorias = obra("gundam_zeta").traducoesObrigatorias();

        String saida = enforcador.traduzirObrigatorios(
            "U.C. 0087, Gryps Conflict.", "U.C. 0087, conflito de Gryps.", obrigatorias);

        assertTrue(saida.contains("U.C. 0087"),
            "a abreviação é forma consagrada e fica: " + saida);
        assertFalse(saida.contains("S.U."), "não existe S.U. nesta decisão: " + saida);
    }

    /**
     * CONTRA-CASO da remoção: o termo saiu de {@code termosProtegidos}. Se voltar para lá, ele
     * é mantido em inglês e a decisão do Paulo é desfeita em silêncio — protegido e traduzido
     * são estados que se cancelam.
     */
    @Test
    @DisplayName("Universal Century NAO pode voltar a termosProtegidos")
    void termoNaoPodeVoltarAserProtegido() {
        for (ProvedorContexto o : catalogo.obras()) {
            assertFalse(o.termosProtegidos().contains("Universal Century"),
                "a obra " + o.getId() + " voltou a proteger o termo, e proteger é mantê-lo "
                    + "em inglês — o oposto da decisão");
        }
    }
}
