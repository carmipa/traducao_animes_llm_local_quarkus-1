package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore (Opção 7) completa para Guilty Crown — roster,
 * locais e mapa alinhados à Tradução Local, sem importar {@code contexto.lore}.
 *
 * <p>INVARIANTES DO DOMÍNIO: Funeral Parlor ≠ Funerária; Void ≠ vazio; Undertaker ≠ grupo;
 * Guilty Crown ≠ Coroa Culpada; Apocalypse Virus / Lost Christmas / Void Genome oficiais;
 * Anti Bodies ≠ Anticorpos; Crow = persona de Inori.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreGuiltyCrown implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Guilty Crown (serie TV Production I.G).
        - Regra: corrigir APENAS nomenclatura de lore. Nomes canonicos NAO sao localizados.

        === Faccoes (NUNCA fundir) ===
        - Funeral Parlor (NUNCA Funeraria / Empresa Funeraria) — resistencia de Gai.
        - Undertaker — codinome de Gai Tsutsugami; NAO e o nome do grupo.
        - GHQ; Anti Bodies (NUNCA Anticorpos); Da'ath; Sephirah Genomics; Kuhouin Group.

        === Roster — Funeral Parlor ===
        - Shu Ouma; Inori Yuzuriha (tambem Crow no palco); Gai Tsutsugami (Undertaker);
          Ayase Shinomiya; Tsugumi; Yahiro Samukawa; Hare Menjou; Souta Tamadate;
          Shibungi; Kenji Kido; Argo Tsukishima; Oogumo.

        === Roster — GHQ / politicos ===
        - Shuichiro Keido (Keido); Makoto Waltz Segai; Daryl Yan; Major General Yan;
          Dan Eagleman; Arisa Kuhouin; Okina Kuhouin.

        === Roster — familia / virus ===
        - Mana Ouma (Eve); Kurosu Ouma; Haruka Ouma; Jun Samukawa.

        === Locais ===
        - Tennouzu High; Second Hand; Loop 7; Roppongi Fort.

        === Termos canonicos ===
        - Void / Voids (NUNCA "vazio"/"vazios" quando for poder/arma).
        - Void Genome (NUNCA Genoma do Vazio / Genoma Void); King's Power / Power of the King.
        - Apocalypse Virus (NUNCA Virus do Apocalipse como nome proprio).
        - Lost Christmas (NUNCA Natal Perdido como titulo canonico).
        - Endlave / Endlaves (NUNCA Endslave); Leukocyte; Genomic Resonance; Crystallization;
          Norma Gene; Guilty Crown (NUNCA Coroa Culpada).

        === Alertas / formas-ruim ===
        - Void nao vira "vazio"; Funeral Parlor nao vira "funeraria"; GHQ fica como sigla;
          Undertaker nao vira "Coveiro"; Anti Bodies nao vira "Anticorpos";
          Second Hand nao vira "Segunda Mao"; Genomic Resonance nao vira "Ressonancia Genomica".
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override
    public String getId() {
        return "guilty_crown";
    }

    @Override
    public String getNomeExibicao() {
        return "Guilty Crown - Revisao de Lore";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: espelho determinístico da Tradução Local de Guilty Crown
     * (fatia revisaoLore, sem importar {@code contexto.lore}).
     *
     * <p>INVARIANTES DO DOMÍNIO: chave = forma-ruim PT; valor = canônico; longas antes de curtas.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Genoma do Vazio", "Void Genome");
        m.put("Genoma Vazio", "Void Genome");
        m.put("Genoma Void", "Void Genome");
        m.put("Empresa Funerária", "Funeral Parlor");
        m.put("Empresa Funeraria", "Funeral Parlor");
        m.put("Vírus do Apocalipse", "Apocalypse Virus");
        m.put("Virus do Apocalipse", "Apocalypse Virus");
        m.put("Natal Perdido", "Lost Christmas");
        m.put("Coroa Culpada", "Guilty Crown");
        m.put("Poder do Rei", "King's Power");
        m.put("Poder dos Reis", "King's Power");
        m.put("Ressonância Genômica", "Genomic Resonance");
        m.put("Ressonancia Genomica", "Genomic Resonance");
        m.put("Gene Norma", "Norma Gene");
        m.put("Funerária", "Funeral Parlor");
        m.put("Funeraria", "Funeral Parlor");
        m.put("Anticorpos", "Anti Bodies");
        m.put("Corpos Anti", "Anti Bodies");
        m.put("Cristalização", "Crystallization");
        m.put("Cristalizacao", "Crystallization");
        m.put("Segunda Mão", "Second Hand");
        m.put("Segunda Mao", "Second Hand");
        m.put("Forte Roppongi", "Roppongi Fort");
        m.put("Endslave", "Endlave");
        m.put("Escravo Final", "Endlave");
        m.put("Coveiro", "Undertaker");
        m.put("Vazios", "Voids");
        m.put("Vazio", "Void");
        return Collections.unmodifiableMap(m);
    }
}
