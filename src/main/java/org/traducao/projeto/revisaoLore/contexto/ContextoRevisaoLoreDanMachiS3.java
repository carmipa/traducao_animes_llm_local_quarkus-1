package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore para DanMachi III (3ª temporada).
 *
 * <p>INVARIANTES DO DOMÍNIO: Liliruca Arde; Familia; Dungeon.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreDanMachiS3 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: DanMachi III (Season 3).
        - Regra: corrigir APENAS nomenclatura. Liliruca Arde NUNCA Liriruca/Lilisuka.
        - Personagens: Bell Cranel, Hestia, Liliruca Arde, Welf Crozzo, Aiz Wallenstein,
          Freya, Ottar, Syr Flover, Ryu Lion.
        - Termos: Dungeon, Familia, Falna, Status, Level, Excelia, Magic Stone.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override
    public String getId() {
        return "danmachi_s3";
    }

    @Override
    public String getNomeExibicao() {
        return "DanMachi S3 - Revisao de Lore";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo + extras S3 (Lilisuka/Liriruca).
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho da temporada na Tradução (sem import).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaDanMachiRevisao.comExtras(Map.ofEntries(
            Map.entry("Lilisuka", "Liliruca Arde"),
            Map.entry("Liriruca", "Liliruca Arde"),
            Map.entry("Sino Cranel", "Bell Cranel")
        ));
    }
}
