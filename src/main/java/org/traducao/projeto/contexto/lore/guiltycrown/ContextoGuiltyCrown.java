package org.traducao.projeto.contexto.lore.guiltycrown;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore de Guilty Crown (biologia distópica + Voids).
 *
 * <p>INVARIANTES DO DOMÍNIO: Funeral Parlor ≠ Undertaker; Void Genome; Lost Christmas;
 * Apocalypse Virus; Endlave; Crystallization.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoGuiltyCrown implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Guilty Crown.
        - Premissa: Toquio sob ocupacao da GHQ apos o colapso biologico do Apocalypse Virus;
          Shu Ouma herda o Void Genome e se alia ao Funeral Parlor.

        === Apocalipse biologico ===
        - Apocalypse Virus: virus do apocalipse. Nao traduzir o nome proprio.
        - Lost Christmas (Natal Perdido, 2029): evento historico que marca o surto/ocupacao.
          Manter "Lost Christmas" como nome canonico; nao inventar outro titulo.
        - Crystallization: estagio avancado da infeccao (corpo cristaliza). Termo tecnico da obra;
          nao reduzir a "virar cristal" generico quando for o nome do fenomeno.

        === Tecnologia de almas (Voids) ===
        - Void Genome: poder/capacidade de extrair Voids. NUNCA traduzir.
        - Void: materializacao da psique, traumas e personalidade de pessoa menor de 17 anos
          em arma ou ferramenta fisica. Nao traduzir Void como "vazio" filosofico generico.
        - Endlave: mecha de combate controlado remotamente via interface neural. Manter Endlave.

        === Faccoes (NUNCA fundir) ===
        - GHQ: forca militar internacional de ocupacao; trata infectados com logica eugenica/totalitaria.
        - Funeral Parlor (葬儀社): organizacao guerrilheira de resistencia liderada por Gai Tsutsugami.
        - Undertaker: codinome/identidade de combate de Gai (ligada ao Void) — NAO e o nome do grupo.
        - King of Funeral Parlor: titulo/papel quando aparecer; nao confundir com Undertaker.

        === Pessoas ===
        - Shu Ouma (m), Inori Yuzuriha (f), Gai Tsutsugami (m) — Undertaker,
          Ayase Shinomiya (f), Tsugumi (f), Yahiro Samukawa (m), Hare Menjou (f),
          Souta Tamadate (m), Daryl Yan (m), Arisa Kuhouin (f), Keido (m).

        === Regras ===
        - Funeral Parlor ≠ Undertaker; Void Genome / Void / Endlave / Lost Christmas oficiais.
        - Tom: acao, idol/cancao (Inori), drama adolescente, opressao politica.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Guilty Crown", LORE);

    @Override
    public String getId() {
        return "guilty_crown";
    }

    @Override
    public String getNomeExibicao() {
        return "Guilty Crown";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Shu Ouma", "Inori Yuzuriha", "Gai Tsutsugami",
            "Mana Ouma", "Ayase Shinomiya", "Tsugumi",
            "Yahiro Samukawa", "Hare Menjou", "Souta Tamadate",
            "Arisa Kuhouin", "Daryl Yan", "Makoto Waltz Segai",
            "Shuichiro Keido", "Keido", "Funeral Parlor",
            "Undertaker", "GHQ", "Anti Bodies",
            "Da'ath", "Void", "Voids",
            "Void Genome", "King's Power", "Apocalypse Virus",
            "Apocalypse", "Lost Christmas", "Endlave",
            "Leukocyte", "Genomic Resonance", "Crystallization",
            "Guilty Crown"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reforço determinístico de terminologia própria da obra
     * (nomes/termos que a lore manda manter no original).
     * <p>INVARIANTES DO DOMÍNIO: forma-ruim PT → canônico; só aplica se o EN contém o canônico.
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return Map.ofEntries(
            Map.entry("Vazio", "Void"),
            Map.entry("Vazios", "Voids"),
            Map.entry("Genoma do Vazio", "Void Genome"),
            Map.entry("Funerária", "Funeral Parlor"),
            Map.entry("Vírus do Apocalipse", "Apocalypse Virus"),
            Map.entry("Natal Perdido", "Lost Christmas"),
            Map.entry("Coroa Culpada", "Guilty Crown")
        );
    }
}
