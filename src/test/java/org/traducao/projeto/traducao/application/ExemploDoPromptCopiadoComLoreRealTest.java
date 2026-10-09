package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.traducao.projeto.lore.domain.ProvedorContexto;
import org.traducao.projeto.lore.domain.SnapshotContexto;
import org.traducao.projeto.lore.infrastructure.CatalogoLoreSqlite;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;
import org.traducao.projeto.qualidadeTraducao.domain.LoreAtivaPort;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova a reprovação do exemplo do prompt copiado com a lore REAL dos
 * arquivos SQL da lore, congelada do mesmo jeito que o job congela ({@link SnapshotContexto#de}), e não
 * com um dublê que devolve o prompt. Nasceu da retradução de 08/10/2026 no acervo: a entrada do cache
 * foi recusada fora do job, mas dentro do job o modelo respondeu "Psyco Gundam?" de novo e o portão
 * aceitou — só a releitura A6 acusou.
 *
 * <p>INVARIANTES DO DOMÍNIO: os casos RUIM são os medidos no acervo publicado do Zeta e do ZZ; os BOM
 * carregam o mesmo sinal (igual a exemplo) e têm de passar.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: RUIM aprovado é o vazamento passando dentro do job.
 */
@DisplayName("exemplo do prompt copiado: reprova com a lore real congelada como no job")
class ExemploDoPromptCopiadoComLoreRealTest {

    private static final CatalogoLoreSqlite CATALOGO = new CatalogoLoreSqlite();

    private static ValidadorTraducaoService validadorDaObra(String id) {
        ProvedorContexto provedor = CATALOGO.obras().stream()
            .filter(p -> id.equals(p.getId())).findFirst().orElseThrow();
        SnapshotContexto congelado = SnapshotContexto.de(provedor);
        return new ValidadorTraducaoService(new LoreAtivaPort() {
            @Override public Set<String> termosProtegidosAtivos() { return congelado.termosProtegidos(); }
            @Override public String obterLoreAtiva() { return congelado.lore(); }
            @Override public Set<List<String>> paresInconfundiveisAtivos() { return congelado.paresInconfundiveis(); }
        });
    }

    @ParameterizedTest(name = "[{index}] RUIM {0}: {1} -> {2}")
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
        "gundam_zz|Z-G...?|Psyco Gundam?",
        "gundam_zz|Catl?|Psyco Gundam?",
        "gundam_zz|Z-G...?|\"Psyco Gundam?\"",
        "gundam_zz|Catl?|\"Psyco Gundam?\"",
        "gundam_zeta|G3?!|Psyco Gundam?",
        "gundam_zeta|All right, do as you wish.|Como quiser, mas vou confiar o Psyco Gundam a você.",
        "gundam_zeta|...through the armor of a mobile suit,\\Nlike a Newtype?|Você precisa sair do cockpit\\Ndo Psyco Gundam! Rápido!"
    })
    void exemploCopiadoReprovaComALoreReal(String obra, String original, String traduzido) {
        AlucinacaoDetectadaException e = assertThrows(AlucinacaoDetectadaException.class,
            () -> validadorDaObra(obra).validarPar(original, traduzido));
        assertTrue(e.getMessage().startsWith("Exemplo do prompt copiado"), e.getMessage());
    }

    @ParameterizedTest(name = "[{index}] BOM {0}: {1} -> {2}")
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
        "gundam_zz|Psyco Gundam?|Psyco Gundam?",
        "gundam_zeta|The Psyco Gundam!|Psyco Gundam!",
        "gundam_zeta|It's Four!|Four!"
    })
    void falaIgualAExemploLegitimaPassaComALoreReal(String obra, String original, String traduzido) {
        assertDoesNotThrow(() -> validadorDaObra(obra).validarPar(original, traduzido));
    }
}
