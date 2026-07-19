package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore excepcional para Do You Remember Love?
 * (id de Tradução: macross_filme1).
 *
 * <p>INVARIANTES DO DOMÍNIO: Protoculture ≠ Protocultura; Meltrandi; Valkyrie; Minmay Attack.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreMacrossFilme1 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Macross: Do You Remember Love? (filme 1984 — releitura Space War I).
        - Regra: corrigir APENAS nomenclatura. Walküre Delta NAO se aplica aqui.

        === Roster ===
        - Hikaru Ichijyo, Lynn Minmay, Misa Hayase, Roy Focker, Max Jenius, Milia Fallyna,
          Exsedol Folmo, Britai Kridanik, Boddole Zer.

        === Termos canonicos ===
        - SDF-1 Macross; Zentradi; Meltrandi; Protoculture (NUNCA Protocultura como canonico);
          Minmay Attack; Valkyrie / VF-1 / Strike Valkyrie; Queadluun-Rau; Nousjadeul-Ger;
          GERWALK / Battroid / Fighter Mode; UN Spacy; Overtechnology.
        - PROIBIDO Veritech / Robotech.

        === Formas-ruim ===
        - Protocultura → Protoculture; Valquíria → Valkyrie; Zentraedi → Zentradi;
          Meltrandy → Meltrandi; Ataque Minmay → Minmay Attack.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override
    public String getId() {
        return "macross_filme1";
    }

    @Override
    public String getNomeExibicao() {
        return "Macross: Do You Remember Love? - Revisao de Lore";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico DYRL na Opção 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho da Tradução Local.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaMacrossDyrlRevisao.mapa();
    }
}
