package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore completa para Gundam 0083: Stardust Memory —
 * Opção 7 alinhada à Tradução Local, sem importar {@code contexto.lore}.
 *
 * <p>INVARIANTES DO DOMÍNIO: Stardust Memory ≠ Memória de Poeira Estelar; Delaz Fleet;
 * Dendrobium; Neue Ziel; Titans ≠ Titãs; Physalis / Zephyranthes oficiais.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreGundam0083 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam 0083: Stardust Memory, Universal Century U.C. 0083.
        - Regra: corrigir APENAS nomenclatura. Neue Ziel e Mobile Armor.

        === Roster — Federacao / Albion ===
        - Kou Uraki; Nina Purpleton; South Burning; Eiphar Synapse; Chuck Keith;
          Mora Bascht; Bernard Monsha; Chap Adel; Alpha A. Bate; John Kowen;
          Adelheid Bernard; Jamitov Hymem; Bask Om (Titans).

        === Roster — Delaz Fleet / Zeon ===
        - Anavel Gato (Nightmare of Solomon quando o EN trouxer); Aiguille Delaz;
          Cima Garahau; Kelly Layzner.

        === Orgs / naves / operacoes ===
        - Delaz Fleet (NUNCA Frota Delaz); Anaheim Electronics; Titans (NUNCA Titas);
          Earth Federation; Principality of Zeon.
        - Albion; La Vie en Rose; Gwaden; Birmingham.
        - Operation Stardust; Colony Drop; Solar System II; Naval Review.
        - Stardust Memory (NUNCA Memoria de Poeira Estelar como titulo).

        === Mecha ===
        - Gundam GP01 Zephyranthes; Full Burnern; Gundam GP02A Physalis;
          GP03 Dendrobium / Dendrobium Orchis; Gerbera Tetra; Neue Ziel;
          GM Custom; GM Cannon II; Gelgoog Marine; Dom Tropen; Dra-C; Val-Walo.

        === Formas-ruim ===
        - Frota Delaz → Delaz Fleet; Dendróbio → Dendrobium; Novo Alvo → Neue Ziel;
          Titãs → Titans; Operação Stardust → Operation Stardust;
          Queda de Colônia → Colony Drop; Físalis → Physalis;
          Pesadelo de Solomon → Nightmare of Solomon.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override
    public String getId() {
        return "gundam_0083";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam 0083: Stardust Memory - Revisao de Lore";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo UC + extras 0083 na Opção 7 (espelho da Tradução Local).
     *
     * <p>INVARIANTES DO DOMÍNIO: sem import cruzado de {@code contexto.lore}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUcRevisao.comExtras(Map.ofEntries(
            Map.entry("Frota Delaz", "Delaz Fleet"),
            Map.entry("Dendróbio", "Dendrobium"),
            Map.entry("Dendrobio", "Dendrobium"),
            Map.entry("Novo Alvo", "Neue Ziel"),
            Map.entry("Memória de Poeira Estelar", "Stardust Memory"),
            Map.entry("Memoria de Poeira Estelar", "Stardust Memory"),
            Map.entry("Titãs", "Titans"),
            Map.entry("Titas", "Titans"),
            Map.entry("Operação Stardust", "Operation Stardust"),
            Map.entry("Operacao Stardust", "Operation Stardust"),
            Map.entry("Queda de Colônia", "Colony Drop"),
            Map.entry("Queda de Colonia", "Colony Drop"),
            Map.entry("Sistema Solar II", "Solar System II"),
            Map.entry("Pesadelo de Solomon", "Nightmare of Solomon"),
            Map.entry("Físalis", "Physalis"),
            Map.entry("Fisalis", "Physalis"),
            Map.entry("Principado de Zeon", "Principality of Zeon")
        ));
    }
}
