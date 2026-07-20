package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

@Component
public class ContextoRevisaoLoreGundamCCA implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam: Char's Counterattack, Universal Century U.C. 0093.
        - Regra central: manter nomes oficiais de personagens, faccoes, locais, eventos, mobile suits, tecnologias e termos UC.
        - Personagens: Amuro Ray, Char Aznable, Bright Noa, Chan Agi, Beltorchika Irma, Hathaway Noa, Quess Paraya, Gyunei Guss, Nanai Miguel, Adenaur Paraya.
        - Faccao/forcas: Londo Bell, Neo Zeon, Earth Federation, Federation, Anaheim Electronics.
        - Lugares/eventos: Axis, Torrington Base, Luna II, Axis Shock.
        - Mobile suits/armors: Nu Gundam, Sazabi, Re-GZ, Jegan, Geara Doga, Jagd Doga, Alpha Azieru.
        - Termos UC: Newtype, psycho-frame, psycommu, funnel, mobile suit, mobile armor, beam rifle, beam saber.
        - Alertas: Char's Counterattack nao deve virar "Contra-ataque do Char" quando for titulo; Axis nao vira "Eixo"; Nu Gundam nao vira "Novo Gundam"; funnel nao vira funil quando for arma.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "gundam_cca"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam: Char's Counterattack - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }

    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUcRevisao.comExtras(Map.ofEntries(
            Map.entry("Eixo", "Axis"),
            Map.entry("Novo Gundam", "Nu Gundam"),
            Map.entry("Moldura Psíquica", "Psycho-Frame"),
            Map.entry("Moldura Psiquica", "Psycho-Frame"),
            Map.entry("Contra-ataque do Char", "Char's Counterattack"),
            Map.entry("Contraataque do Char", "Char's Counterattack")
        ));
    }
}
