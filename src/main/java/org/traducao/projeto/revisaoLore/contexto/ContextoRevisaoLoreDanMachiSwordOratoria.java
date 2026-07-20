package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore para Sword Oratoria (spin-off Loki Familia / Aiz).
 *
 * <p>INVARIANTES DO DOMÍNIO: Aiz Wallenstein (não Ais); Sword Princess; Loki Familia.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreDanMachiSwordOratoria implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Sword Oratoria: DanMachi On the Side — Loki Familia / Aiz Wallenstein.
        - Regra: corrigir APENAS nomenclatura. Aiz Wallenstein = Sword Princess.
        - Personagens: Aiz Wallenstein, Lefiya Viridis, Finn Deimne, Riveria Ljos Alf,
          Gareth Landrock, Tiona Hiryute, Tione Hiryute, Bete Loga, Loki, Bell Cranel.
        - Termos: Familia, Falna, Dungeon, Monster Rex, Irregular, Excelia, Sword Princess.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override
    public String getId() {
        return "danmachi_so";
    }

    @Override
    public String getNomeExibicao() {
        return "DanMachi: Sword Oratoria - Revisao de Lore";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo + extras SO (Ais→Aiz, Princesa Espadachim).
     *
     * <p>INVARIANTES DO DOMÍNIO: Aiz é a grafia canônica deste projeto.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaDanMachiRevisao.comExtras(Map.ofEntries(
            Map.entry("Ais Wallenstein", "Aiz Wallenstein"),
            Map.entry("Princesa Espadachim", "Sword Princess"),
            Map.entry("Sino Cranel", "Bell Cranel")
        ));
    }
}
