package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore completa para Macross II: Lovers Again —
 * Opção 7 alinhada à Tradução Local, sem importar {@code contexto.lore}.
 *
 * <p>INVARIANTES DO DOMÍNIO: Emulator (cargo Marduk); Minmay Attack; Marduk; Valkyrie;
 * Protoculture; anti-Veritech.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreMacross2 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Macross II: Lovers Again (OVA — continuidade alternativa pos-Space War I).
        - Regra: corrigir APENAS nomenclatura.

        === Roster — Terra / UN Spacy ===
        - Hibiki Kanzaki (m); Silvie Gena (f); Nexx Gilbert (m); Mash Broodwell (m);
          Saori (f); Dennis Lone (m) quando aparecerem.

        === Roster — Marduk ===
        - Ishtar (f — Emulator); Lord Feff (m); Ingues (m).

        === Faccoes / doutrina ===
        - Marduk; Emulator (NUNCA "emulador" de software); Minmay Attack; Song Energy;
          UN Spacy / U.N. Spacy; Protoculture; Zentradi (legado).

        === Mecha / naves ===
        - VF-2SS Valkyrie II; Metal Siren; Gigamesh; Macross Cannon;
          Valkyrie / Variable Fighter; GERWALK / Battroid; Overtechnology.
        - PROIBIDO Veritech / Robotech.

        === Formas-ruim ===
        - Emulador → Emulator; Ataque Minmay → Minmay Attack; Marduque → Marduk;
          Valquíria → Valkyrie; Protocultura → Protoculture;
          Energia da Canção → Song Energy; Sereia de Metal → Metal Siren;
          Canhão Macross → Macross Cannon; Amantes de Novo → Lovers Again.
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
     * <p>INVARIANTES DO DOMÍNIO: espelho da Tradução Local (sem import cruzado).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaMacross2Revisao.mapa();
    }
}
