package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Macross 7 (Série TV).
 *
 * <p>INVARIANTES DO DOMÍNIO: Valkyrie/Zentradi oficiais; sem Veritech/Valquíria.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreMacross7 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Macross 7 (Série TV).
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Basara Nekki, Mylene Flare Jenius, Ray Lovelock, Veffidas Feaze, Gamlin Kizaki, Fire Bomber, Protodeviln, Sound Force, VF-19 Custom Fire Valkyrie, City 7.
        - Premissa: a frota Macross 7 enfrenta os Protodeviln; Basara Nekki da banda Fire Bomber
        - Personagens (gênero): Basara Nekki (m), Mylene Flare Jenius (f), Ray Lovelock (m),
        - Bandas/unidades: Fire Bomber, Sound Force, Diamond Force, Jamming Birds.
        - Termos: Protodeviln, Anima Spiritia, Song Energy, Spiritia, Zentradi, NUNS/UN Spacy era Macross 7,
        - Mecha: VF-19 Custom Fire Valkyrie (Basara), VF-11 Thunderbolt, VF-17 Nightmare,
        - Regras: Fire Bomber e nomes dos músicos oficiais; Protodeviln não traduzir;
        - Alertas: Valkyrie nao vira Valquiria/Valquíria; Zentradi grafia oficial; proibido Veritech;
          GERWALK/Battroid/Fighter Mode — nao traduzir nomes dos modos.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "macross_7"; }
    @Override public String getNomeExibicao() { return "Macross 7 (Série TV) - Revisao de Lore"; }
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
        return CorrecoesTerminologiaMacrossRevisao.comExtras(Map.ofEntries(
            Map.entry("Protodevilns", "Protodeviln"),
            Map.entry("Protodemonios", "Protodeviln"),
            Map.entry("Protodemônios", "Protodeviln"),
            Map.entry("Energia da Canção", "Song Energy"),
            Map.entry("Energia da Cancao", "Song Energy")
        ));
    }
}
