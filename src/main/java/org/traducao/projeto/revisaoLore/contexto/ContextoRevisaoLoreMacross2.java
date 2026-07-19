package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Macross II: Lovers Again.
 *
 * <p>INVARIANTES DO DOMÍNIO: Valkyrie/Zentradi oficiais; sem Veritech/Valquíria.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreMacross2 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Macross II: Lovers Again.
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Hibiki Kanzaki, Ishtar, Silvie Gena, Nexx Gilbert, Lord Feff, Ingues, Marduk, Emulator, Minmay Attack, VF-2SS Valkyrie II, Metal Siren, UN Spacy, GERWALK, Battroid, Overtechnology, Valkyrie.
        - Premissa: a humanidade usa o "Minmay Attack" (cultura/canção) como arma estratégica;
        - Personagens (gênero): Hibiki Kanzaki (m) — repórter curioso/informal;
        - Facções/termos: Marduk, Emulator (cargo cultural-militar Marduk — NÃO traduzir como
        - Mecha/naves: VF-2SS Valkyrie II, Metal Siren, Gigamesh, Macross Cannon / SDF-era assets.
        - Alertas: Valkyrie nao vira Valquiria/Valquíria; Zentradi grafia oficial; proibido Veritech;
          GERWALK/Battroid/Fighter Mode — nao traduzir nomes dos modos.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "macross_2"; }
    @Override public String getNomeExibicao() { return "Macross II: Lovers Again - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico Macross na Opção 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho da Tradução Local.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaMacrossRevisao.mapa();
    }
}
