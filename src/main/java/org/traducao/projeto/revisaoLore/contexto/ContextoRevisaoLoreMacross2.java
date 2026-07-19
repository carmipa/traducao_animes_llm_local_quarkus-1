package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore excepcional para Macross II: Lovers Again.
 *
 * <p>INVARIANTES DO DOMÍNIO: Emulator (cargo Marduk); Minmay Attack; Marduk; Valkyrie;
 * anti-Veritech.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreMacross2 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Macross II: Lovers Again (OVA — continuidade alternativa pos-Space War I).
        - Regra: corrigir APENAS nomenclatura.

        === Roster ===
        - Hibiki Kanzaki (m), Ishtar (f — Emulator Marduk), Silvie Gena (f),
          Nexx Gilbert (m), Lord Feff (m), Ingues (m), Mash Broodwell (m).

        === Termos canonicos ===
        - Marduk; Emulator (NUNCA tratar como "emulador" de software); Minmay Attack;
          UN Spacy; VF-2SS Valkyrie II; Metal Siren; Gigamesh; Macross Cannon;
          Valkyrie / Variable Fighter; GERWALK / Battroid; Overtechnology; Protoculture.
        - PROIBIDO Veritech / Robotech.

        === Formas-ruim ===
        - Emulador → Emulator; Ataque Minmay → Minmay Attack; Marduque → Marduk;
          Valquíria → Valkyrie; Protocultura → Protoculture.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override
    public String getId() {
        return "macross_2";
    }

    @Override
    public String getNomeExibicao() {
        return "Macross II: Lovers Again - Revisao de Lore";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico Macross II na Opção 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho da Tradução Local.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaMacross2Revisao.mapa();
    }
}
