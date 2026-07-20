package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

@Component
public class ContextoRevisaoLoreGundam0080 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam 0080: War in the Pocket, Universal Century 0079/0080.
        - Regra central: manter nomes oficiais de personagens, faccoes, colonias, mobile suits, mobile armors e termos UC.
        - Personagens: Alfred "Al" Izuruha, Bernard "Bernie" Wiseman, Christina "Chris" Mackenzie, Mikhail "Misha" Kaminsky, Hardy Steiner, Gabriel Ramirez Garcia, Andy Strauss.
        - Faccao/locais: Earth Federation, Federation, Principality of Zeon, Zeon, Side 6, Republic of Riah, Libot colony, Antarctic Base.
        - Unidades: Cyclops Team, Zeon Special Forces.
        - Mobile suits: RX-78NT-1 Gundam Alex, Gundam Alex, Zaku II Kai, Kampfer, Hygogg, Z'Gok-E, GM Cold Districts Type, GM Sniper II.
        - Termos UC: mobile suit, mobile armor, beam rifle, beam saber, colony, Side, Newtype, One Year War, Universal Century, U.C.
        - Alertas: War in the Pocket nao deve virar "Guerra no Bolso" quando for titulo; Gundam Alex nao vira "Alexandre"; Kampfer/Hygogg/Z'Gok-E mantem grafia oficial.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "gundam_0080"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam 0080: War in the Pocket - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }

    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUcRevisao.comExtras(Map.ofEntries(
            Map.entry("Gundam Alexandre", "Gundam Alex"),
            Map.entry("Kämpfer", "Kampfer"),
            Map.entry("Guerra no Bolso", "War in the Pocket")
        ));
    }
}
