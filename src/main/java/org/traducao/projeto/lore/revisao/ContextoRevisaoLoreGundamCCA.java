package org.traducao.projeto.lore.revisao;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: espelho de lore para a revisão de Char's Counterattack (UC 0093).
 *
 * <p>INVARIANTES DO DOMÍNIO: Amuro Ray; Char Aznable; Nu Gundam; Sazabi;
 * Londo Bell; Axis; psycho-frame / Psyco-frame; Londenion distinto de Londo Bell.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt montado em compile-time.
 */
@Component
public class ContextoRevisaoLoreGundamCCA implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam: Char's Counterattack, Universal Century U.C. 0093.
        - Regra central: manter nomes oficiais de personagens, faccoes, locais, eventos,
          naves, mobile suits, tecnologias e termos UC.
        - Personagens: Amuro Ray, Char Aznable, Casval Deikun, Bright Noa, Chan Agi,
          Beltorchika Irma, Hathaway Noa, Quess Paraya, Gyunei Guss, Nanai Miguel,
          Adenauer Paraya, Kayra Su, Lalah Sune, Cheimin, Mirai, John Bauer,
          Cameron Bloom, Meran, Rezin, Astonaige, Christina, Zeon Deikun, Artesia,
          Haman, Zabi.
        - Faccao/forcas: Londo Bell, Neo Zeon, Earth Federation, Federation,
          Anaheim Electronics, Anti-Earth United Government, Titans, Audit Bureau.
        - Lugares/eventos: Axis, Torrington Base, Luna II, Sweetwater, Londenion,
          Fifth Luna, Side 1, Side 2, Side 3, Lhasa, Hong Kong, Earthsphere, Axis Shock.
        - Naves: Ra Cailum, Ra-Cailum, Rewloola, Ra-Chutter, Ra-Kiem, Ra-Zyme, Clop, Musaka.
        - Mobile suits/armors: Nu Gundam, Sazabi, Re-GZ, Jegan, Geara Doga, Jagd Doga,
          Alpha Azieru.
        - Termos UC: Newtype, psycho-frame, Psyco-frame, psycommu, funnel, Haro,
          mobile suit, mobile armor, beam rifle, beam saber.
        - Alertas: Char's Counterattack nao deve virar "Contra-ataque do Char" quando for
          titulo; Axis nao vira "Eixo"; Nu Gundam nao vira "Novo Gundam"; funnel nao vira
          funil quando for arma; Londenion NUNCA "Londo Bell"; Fifth Luna NUNCA "Quinta Lua";
          Side 1/2/3 NUNCA "lado N"; Ra-Chutter / Rezin / Christina grafias oficiais.
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
