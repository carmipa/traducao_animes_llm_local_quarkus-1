package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore completa de Macross II: Lovers Again (OVA) — continuidade
 * alternativa pós-Space War I, Marduk/Emulator e doutrina Minmay Attack.
 *
 * <p>INVARIANTES DO DOMÍNIO: Hibiki Kanzaki; Ishtar; Silvie Gena; Marduk; Emulator
 * (cargo, NÃO software); Minmay Attack; VF-2SS; anti-Veritech/Robotech; Protoculture.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoMacross2 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross II: Lovers Again (OVA 1992) — continuidade alternativa pos-Space War I
          (distinta da linha Frontier/Delta em varios pontos).
        - Premissa: humanidade usa Minmay Attack (cultura/cancao) como arma; reporter Hibiki
          Kanzaki encontra Ishtar (Emulator Marduk); UN Spacy vs hierarquia Marduk
          (Lord Feff / Ingues).
        - Tom: romance idol/reporter em guerra espacial; Hibiki informal, Silvie objetiva,
          Ishtar ritualistica. Evitar girias modernas.

        === Nucleo Macross ===
        - Valkyrie / Variable Fighter; Fighter / GERWALK / Battroid;
          Overtechnology; Reaction Weaponry; Fold / space fold; Protoculture;
          Zentradi (legado Space War I); Macross Cannon.
        - PROIBIDO Veritech / Robotech.

        === Faccoes / doutrina ===
        - UN Spacy / U.N. Spacy; Marduk (invasores); Emulator (cargo cultural-militar Marduk —
          NUNCA "emulador" de software); Minmay Attack (doutrina/tatica — NUNCA so "ataque musical");
          Song Energy quando o dialogo trouxer.

        === Roster — Terra / UN Spacy / midia ===
        - Hibiki Kanzaki (m) — reporter;
          Silvie Gena (f) — oficial UN Spacy / Metal Siren;
          Nexx Gilbert (m); Mash Broodwell (m); Saori (f) quando aparecer;
          Dennis Lone (m) quando aparecer.

        === Roster — Marduk ===
        - Ishtar (f) — Emulator;
          Lord Feff (m); Ingues (m) — lider espiritual/politico Marduk.

        === Mecha / naves ===
        - VF-2SS Valkyrie II; Metal Siren; Gigamesh;
          Macross Cannon / ativos estilo SDF-1 Macross quando a continuidade trouxer.

        === Regras duras ===
        - Emulator nao vira "emulador" de TI; Marduk nao vira Marduque;
          Minmay Attack nao vira Ataque Minmay; Valkyrie nao vira Valquiria;
          Protoculture nao vira Protocultura; Lovers Again como subtitulo oficial.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross II: Lovers Again", LORE);

    @Override
    public String getId() {
        return "macross_2";
    }

    @Override
    public String getNomeExibicao() {
        return "Macross II: Lovers Again";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege elenco Macross II, Marduk/Emulator e mecha oficiais.
     *
     * <p>INVARIANTES DO DOMÍNIO: Emulator nunca tratado como palavra comum de TI.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Hibiki Kanzaki", "Ishtar", "Silvie Gena", "Nexx Gilbert", "Lord Feff",
            "Ingues", "Mash Broodwell", "Saori", "Dennis Lone",
            "Marduk", "Emulator", "Minmay Attack", "Song Energy",
            "VF-2SS Valkyrie II", "Valkyrie II", "Metal Siren", "Gigamesh",
            "Macross Cannon", "SDF-1", "Macross", "Lovers Again",
            "UN Spacy", "U.N. Spacy", "Variable Fighter", "Valkyrie",
            "GERWALK", "Battroid", "Fighter", "Overtechnology", "Reaction Weaponry",
            "Protoculture", "Zentradi", "Space War I", "Fold"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: restaura Emulator/Minmay Attack/Marduk + núcleo franquia Macross.
     *
     * <p>INVARIANTES DO DOMÍNIO: mapa de {@link CorrecoesTerminologiaMacross2}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaMacross2.mapa();
    }
}
