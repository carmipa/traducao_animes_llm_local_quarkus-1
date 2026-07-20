package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore completa para Gundam The Origin —
 * nomenclatura canônica (Opção 7 / PT-only).
 *
 * <p>INVARIANTES DO DOMÍNIO: Casval/Edouard/Char; Artesia/Sayla; Zabi; Zeon ≠ Zion;
 * Black Tri-Stars; Munzo; Mobile Worker; One Year War; Newtype ≠ Novo Tipo.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreGundamOrigin implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam: The Origin (OVA / mangá) — Char, Zabi, Munzo, One Year War.
        - Papel: corrigir APENAS nomenclatura. Nao retraduzir dialogo.

        === Deikun / Mass / Char ===
        - Zeon Zum Deikun; Astraia Tor Deikun; Casval Rem Deikun → Edouard Mass / Édouard Mass
          → Char Aznable (NUNCA Shar/Xar); Artesia Som Deikun → Sayla Mass;
          Don Teabolo Mass / Teabolo Mass; Jimba Ral; Ramba Ral; Crowley Hamon.

        === Zabi ===
        - Degwin Sodo Zabi, Gihren Zabi, Dozle Zabi (nao Dozzle), Kycilia Zabi, Garma Zabi, Sasro Zabi.
        - Torenov Y. Minovsky.

        === Federacao / OYW (quando cruzar) ===
        - Amuro Ray, Bright Noa, Mirai Yashima, Tem Ray, Johann Ibrahim Revil, Lalah Sune.

        === Zeon / Tri-Stars ===
        - Black Tri-Stars: Gaia, Ortega, Mash (NUNCA Triangulo Negro como canonico).
        - M'Quve, Gadem, Clamp, Darcia Bakharov.

        === Lugares / faccoes ===
        - Autonomous Republic of Munzo; Principality of Zeon / Zeon (NUNCA Zion); Side 3; Munzo;
          Texas Colony; Loum / Battle of Loum; Granada; Solomon; A Baoa Qu; Jaburo; Side 7;
          White Base; Earth Federation; One Year War.

        === Mecha / UC ===
        - Mobile Worker; Bugu; Zaku I / Zaku II; Gouf; Dom; RX-78-02 Gundam; Guntank; Guncannon.
        - Newtype (NUNCA Novo Tipo); Oldtype; Minovsky; Mobile Suit / Mobile Armor.

        === Formas-ruim ===
        - Base Branca → White Base; Trabalhador Movel → Mobile Worker;
          Zion / Principado de Zion → Zeon / Principality of Zeon;
          Republica Autonoma de Munzo → Autonomous Republic of Munzo;
          Guerra de Um Ano → One Year War; Batalha de Loum → Battle of Loum;
          Triangulo Negro / Tres Estrelas Negras → Black Tri-Stars;
          Eduardo Mass / Edward Mass → Edouard Mass; Novo Tipo → Newtype.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override
    public String getId() {
        return "gundam_origin";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam: The Origin - Revisao de Lore";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico Origin na Opção 7 (núcleo UC + extras).
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho da Tradução Local Origin (sem import cruzado).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUcRevisao.comExtras(Map.ofEntries(
            Map.entry("Base Branca", "White Base"),
            Map.entry("Trabalhador Móvel", "Mobile Worker"),
            Map.entry("Trabalhador Movel", "Mobile Worker"),
            Map.entry("Zion", "Zeon"),
            Map.entry("Principado de Zion", "Principality of Zeon"),
            Map.entry("Principado de Zeon", "Principality of Zeon"),
            Map.entry("República Autônoma de Munzo", "Autonomous Republic of Munzo"),
            Map.entry("Republica Autonoma de Munzo", "Autonomous Republic of Munzo"),
            Map.entry("Guerra de Um Ano", "One Year War"),
            Map.entry("Batalha de Loum", "Battle of Loum"),
            Map.entry("Triângulo Negro", "Black Tri-Stars"),
            Map.entry("Triangulo Negro", "Black Tri-Stars"),
            Map.entry("Três Estrelas Negras", "Black Tri-Stars"),
            Map.entry("Tres Estrelas Negras", "Black Tri-Stars"),
            Map.entry("Eduardo Mass", "Edouard Mass"),
            Map.entry("Edward Mass", "Edouard Mass")
        ));
    }
}
