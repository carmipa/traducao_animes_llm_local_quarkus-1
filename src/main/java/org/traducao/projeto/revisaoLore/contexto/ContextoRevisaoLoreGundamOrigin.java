package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore excepcional para Gundam The Origin.
 *
 * <p>INVARIANTES DO DOMÍNIO: Char/Casval; Sayla/Artesia; Zabi; Zeon ≠ Zion; White Base;
 * Mobile Worker; Newtype ≠ Novo Tipo.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreGundamOrigin implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam: The Origin (OVA — origem Char / Zeon / Zabi).
        - Papel: corrigir APENAS nomenclatura. Nao retraduzir dialogo.

        === Personagens ===
        - Casval Rem Deikun / Char Aznable (m); Artesia Som Deikun / Sayla Mass (f);
          Zeon Zum Deikun (m); Amuro Ray (m).
        - Zabi: Degwin Sodo Zabi, Gihren Zabi, Dozle Zabi, Kycilia Zabi, Garma Zabi, Sasro Zabi.
        - Ramba Ral (m), Crowley Hamon (f), Jimba Ral, Astraia Tor Deikun.

        === Faccoes / lugares / mecha ===
        - Principality of Zeon / Zeon (NUNCA Zion); Side 3; Munzo; Earth Federation.
        - Mobile Worker; Zaku I / Zaku II; Bugu; Gundam; White Base.
        - Newtype (NUNCA Novo Tipo); Oldtype; Minovsky; Mobile Suit / Mobile Armor.

        === Formas-ruim ===
        - Base Branca → White Base; Trabalhador Movel → Mobile Worker;
          Zion / Principado de Zion → Zeon / Principality of Zeon; Novo Tipo → Newtype.
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
     * PROPÓSITO DE NEGÓCIO: mapa determinístico Origin na Opção 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho da Tradução Local Origin.
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
            Map.entry("Principado de Zeon", "Principality of Zeon")
        ));
    }
}
