package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore de Macross II: Lovers Again para tradução fiel de
 * nomes, Marduk/Emulator e mecha da continuidade alternativa.
 *
 * <p>INVARIANTES DO DOMÍNIO: Hibiki Kanzaki; Ishtar; Silvie Gena; Marduk;
 * Emulator; Minmay Attack; VF-2SS.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoMacross2 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross II: Lovers Again (OVA / filme — continuidade alternativa pós-Space War I,
          distinta da linha Frontier/Delta/DYRL canônica principal em vários pontos).
        - Premissa: a humanidade usa o "Minmay Attack" (cultura/canção) como arma estratégica;
          o repórter Hibiki Kanzaki encontra Ishtar, Emulator do povo invasor Marduk;
          conflito com a UN Spacy e a hierarquia Marduk (Lord Feff, Ingues).
        - Personagens (gênero): Hibiki Kanzaki (m) — repórter curioso/informal;
          Ishtar (f) — Emulator Marduk, inocência + reverência musical;
          Silvie Gena (f) — oficial UN Spacy, militar direta;
          Nexx Gilbert (m); Lord Feff (m); Ingues (m) — líder espiritual/político Marduk;
          Mash / Mash Broodwell (m) conforme créditos.
        - Facções/termos: Marduk, Emulator (cargo cultural-militar Marduk — NÃO traduzir como
          "emulador" de software), Minmay Attack, UN Spacy / U.N. Spacy, Macross Cannon,
          Valkyrie, Variable Fighter, Song Energy / cultura como arma.
        - Mecha/naves: VF-2SS Valkyrie II, Metal Siren, Gigamesh, Macross Cannon / SDF-era assets.
        - Nucleo Macross: PROIBIDO Veritech/Robotech; Fighter/GERWALK/Battroid; Overtechnology;
          Reaction Weaponry; musica/Emulator = doutrina cultural de guerra, nao OST.
        - Regras: Marduk e Emulator oficiais; Ishtar/Hibiki/Silvie sem adaptação;
          "songstress" como cantora quando for função comum, mas Emulator permanece termo Marduk;
          Minmay Attack como nome de doutrina/tática.
        - Tom: romance idol/repórter em guerra espacial; Hibiki informal, Silvie objetiva, Ishtar ritualística.
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
     * PROPÓSITO DE NEGÓCIO: protege nomes e termos Marduk/Emulator de Macross II.
     * <p>INVARIANTES DO DOMÍNIO: Emulator nunca tratado como palavra comum de TI.
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Hibiki Kanzaki", "Ishtar", "Silvie Gena", "Nexx Gilbert", "Lord Feff",
            "Ingues", "Marduk", "Emulator", "Minmay Attack", "VF-2SS Valkyrie II",
            "Metal Siren", "UN Spacy", "GERWALK", "Battroid", "Overtechnology", "Valkyrie"
        );
    }
}
